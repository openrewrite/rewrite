# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""TyTypesClient protocol tests against a fake `ty-types --serve`.

The fake server answers per the requested file's name: "slow-<seconds>" delays
the response and "big" pads it to ~64KB. Every request also writes to stderr,
so an undrained stderr pipe would wedge it just like the real binary can.
"""
import os
import stat
import sys
import textwrap

import pytest

from rewrite.python.ty_client import TyTypesClient

FAKE_SERVER = textwrap.dedent('''\
    #!/usr/bin/env python3
    import json
    import re
    import sys
    import time

    for line in sys.stdin:
        req = json.loads(line)
        params = req.get("params") or {}
        print("handled " + req["method"] + " " * 512, file=sys.stderr, flush=True)
        if req["method"] == "shutdown":
            break
        if req["method"] == "initialize":
            result = {"ok": True}
        else:
            name = params.get("file", "")
            slow = re.search(r"slow-(\\d+(?:\\.\\d+)?)", name)
            if slow:
                time.sleep(float(slow.group(1)))
            padding = "x" * 65536 if "big" in name else ""
            result = {"types": {"1": {"kind": "class", "name": "str"}}, "pad": padding}
        sys.stdout.write(json.dumps({"jsonrpc": "2.0", "id": req["id"], "result": result}) + "\\n")
        sys.stdout.flush()
''')


@pytest.fixture
def client(tmp_path, monkeypatch):
    server = tmp_path / "fake-ty-types"
    server.write_text(FAKE_SERVER)
    server.chmod(server.stat().st_mode | stat.S_IEXEC)
    if os.name == 'nt':
        pytest.skip("fake server uses a shebang launcher")
    monkeypatch.setattr(TyTypesClient, '_find_binary', staticmethod(lambda: server))
    c = TyTypesClient()
    yield c
    c.shutdown()


def test_round_trip(client):
    assert client.initialize("/some/project")
    result = client.get_types("/some/project/app.py")
    assert result is not None
    assert result["types"]["1"]["name"] == "str"


def test_timeout_returns_none_then_recovers(client):
    assert client.initialize("/some/project")
    assert client.get_types("/some/project/slow-1.py", timeout=0.05) is None
    # The abandoned request's late response must be discarded, not returned here.
    result = client.get_types("/some/project/ok.py", timeout=10)
    assert result is not None
    assert result["types"]["1"]["name"] == "str"


def test_unread_big_responses_do_not_deadlock(client):
    # Windows-hang regression: abandoned ~64KB responses must be absorbed; interleaved successes keep the breaker from tripping.
    assert client.initialize("/some/project")
    for i in range(20):
        assert client.get_types(f"/some/project/big-slow-0.2-{i}.py", timeout=0.01) is None
        result = client.get_types("/some/project/ok.py", timeout=30)
        assert result is not None, f"client wedged after {i + 1} abandoned responses"


def test_consecutive_timeouts_shut_the_process_down(client):
    assert client.initialize("/some/project")
    for i in range(3):
        assert client.get_types(f"/some/project/slow-5-{i}.py", timeout=0.05) is None
    assert not client.is_available
    # Degrades fast instead of waiting on a dead process.
    assert client.get_types("/some/project/ok.py", timeout=5) is None
