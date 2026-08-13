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
the response, "big" pads it to ~64KB, "junk" precedes it with a non-object JSON
line, and "die" makes the server write to stderr and exit non-zero.
"""
import logging
import os
import stat
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
        if req["method"] == "shutdown":
            break
        if req["method"] == "initialize":
            result = {"ok": True}
        else:
            name = params.get("file", "")
            if "die" in name:
                print("ty-types panicked", file=sys.stderr, flush=True)
                sys.exit(3)
            slow = re.search(r"slow-(\\d+(?:\\.\\d+)?)", name)
            if slow:
                time.sleep(float(slow.group(1)))
            if "junk" in name:
                sys.stdout.write("null\\n")
                sys.stdout.write("[1, 2, 3]\\n")
                sys.stdout.flush()
            padding = "x" * 65536 if "big" in name else ""
            result = {"types": {"1": {"kind": "class", "name": "str"}}, "pad": padding}
        sys.stdout.write(json.dumps({"jsonrpc": "2.0", "id": req["id"], "result": result}) + "\\n")
        sys.stdout.flush()
''')


WEDGED_SERVER = textwrap.dedent('''\
    #!/usr/bin/env python3
    import json
    import sys

    for line in sys.stdin:
        req = json.loads(line)
        if req["method"] == "initialize":
            sys.stdout.write(json.dumps({"jsonrpc": "2.0", "id": req["id"], "result": {"ok": True}}) + "\\n")
            sys.stdout.flush()
''')


def _install_server(tmp_path, monkeypatch, source):
    server = tmp_path / "fake-ty-types"
    server.write_text(source)
    server.chmod(server.stat().st_mode | stat.S_IEXEC)
    if os.name == 'nt':
        pytest.skip("fake server uses a shebang launcher")
    monkeypatch.setattr(TyTypesClient, '_find_binary', staticmethod(lambda: server))


@pytest.fixture
def client(tmp_path, monkeypatch):
    _install_server(tmp_path, monkeypatch, FAKE_SERVER)
    c = TyTypesClient()
    yield c
    c.shutdown()


@pytest.fixture
def wedged_client(tmp_path, monkeypatch):
    _install_server(tmp_path, monkeypatch, WEDGED_SERVER)
    c = TyTypesClient()
    yield c
    c._kill()


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
    # Interleaved successes reset the breaker, so a failure here means the undrained pipe.
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
    assert client.get_types("/some/project/ok.py", timeout=5) is None


def test_junk_stdout_lines_are_skipped(client):
    assert client.initialize("/some/project")
    result = client.get_types("/some/project/junk.py", timeout=10)
    assert result is not None
    assert result["types"]["1"]["name"] == "str"


def test_process_death_marks_the_client_unavailable(client, caplog):
    assert client.initialize("/some/project")
    with caplog.at_level(logging.WARNING, logger="rewrite.python.ty_client"):
        assert client.get_types("/some/project/die.py", timeout=10) is None
    assert not client.is_available
    assert "ty-types panicked" in caplog.text


def test_initialize_revives_a_client_whose_process_died(client):
    assert client.initialize("/some/project")
    assert client.get_types("/some/project/die.py", timeout=10) is None
    assert not client.is_available

    assert client.initialize("/some/project")
    assert client.is_available
    assert client.get_types("/some/project/app.py", timeout=10) is not None


def test_initialize_revives_a_killed_client(wedged_client):
    assert wedged_client.initialize("/some/project")
    for i in range(TyTypesClient._MAX_CONSECUTIVE_TIMEOUTS):
        assert wedged_client.get_types(f"/some/project/f{i}.py", timeout=0.05) is None
    assert not wedged_client.is_available

    assert wedged_client.initialize("/some/project")
    assert wedged_client.is_available


def test_shutdown_when_its_own_request_trips_the_breaker(wedged_client):
    assert wedged_client.initialize("/some/project")
    # One short of the breaker, so the "shutdown" request supplies the last strike.
    for i in range(TyTypesClient._MAX_CONSECUTIVE_TIMEOUTS - 1):
        assert wedged_client.get_types(f"/some/project/f{i}.py", timeout=0.05) is None

    wedged_client.shutdown()
    assert not wedged_client.is_available
