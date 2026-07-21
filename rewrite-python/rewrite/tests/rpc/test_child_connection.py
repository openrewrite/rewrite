import os
import sys
import sysconfig
from importlib import metadata
from pathlib import Path

import rewrite
from rewrite.rpc import child_connection, venv_manager
from rewrite.rpc.child_connection import (
    ChildConnection,
    _child_env,
    _engine_roots,
    child_command,
)

ENGINE_ROOT = str(Path(rewrite.__file__).resolve().parent.parent)


class _FakeDist:
    def __init__(self, requires=None, location=None):
        self.requires = requires
        self._location = location

    def locate_file(self, _):
        return self._location

# A minimal stub child: read one Content-Length framed JSON-RPC request, reply with a canned result
# echoing the request id. Proves ChildConnection's spawn + framing + round-trip without needing a
# full venv + rewrite install (that end-to-end lives with the cutover).
STUB = r'''
import sys, json
buf = sys.stdin.buffer
cl = None
while True:
    line = buf.readline()
    if not line:
        sys.exit(0)
    s = line.decode("ascii").strip()
    if s == "":
        break
    if s.lower().startswith("content-length:"):
        cl = int(s.split(":", 1)[1])
body = b""
while len(body) < cl:
    body += buf.read(cl - len(body))
req = json.loads(body.decode("utf-8"))
resp = {"jsonrpc": "2.0", "id": req["id"], "result": [{"descriptor": {"name": "stub.recipe"}}]}
data = json.dumps(resp).encode("utf-8")
out = sys.stdout.buffer
out.write(("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii"))
out.write(data)
out.flush()
'''


def test_child_connection_round_trips_a_request(tmp_path):
    stub = tmp_path / "stub.py"
    stub.write_text(STUB)
    conn = ChildConnection.spawn([sys.executable, str(stub)])
    try:
        result = conn.request("GetMarketplace", {})
    finally:
        conn.close()
    assert result == [{"descriptor": {"name": "stub.recipe"}}]


def test_child_command_targets_the_venv_python_and_child_bundle(tmp_path):
    venv_dir = tmp_path / "b1"
    cmd = child_command(venv_dir, "my-recipes")
    assert cmd == [
        str(venv_manager.venv_python(venv_dir)),
        "-m", "rewrite.rpc.server",
        "--child-bundle", "my-recipes",
    ]


def test_child_command_carries_an_attribution_name_when_given(tmp_path):
    venv_dir = tmp_path / "b1"
    cmd = child_command(venv_dir, "my-recipes", attribution_name="/abs/path")
    assert cmd == [
        str(venv_manager.venv_python(venv_dir)),
        "-m", "rewrite.rpc.server",
        "--child-bundle", "my-recipes",
        "--attribution-name", "/abs/path",
    ]


# A stub child that, mid-request, emits a GetObject callback toward the facade, consumes the
# callback response, then answers the routed request echoing what the callback returned. Exercises
# the pump: relay child->Java callbacks and feed Java's response back to the child.
STUB_WITH_CALLBACK = r'''
import sys, json
buf = sys.stdin.buffer
out = sys.stdout.buffer

def read():
    cl = None
    while True:
        line = buf.readline()
        if not line:
            return None
        s = line.decode("ascii").strip()
        if s == "":
            break
        if s.lower().startswith("content-length:"):
            cl = int(s.split(":", 1)[1])
    if cl is None:
        return None
    body = b""
    while len(body) < cl:
        body += buf.read(cl - len(body))
    return json.loads(body.decode("utf-8"))

def write(msg):
    data = json.dumps(msg).encode("utf-8")
    out.write(("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii"))
    out.write(data)
    out.flush()

req = read()                                                        # routed request from facade
write({"jsonrpc": "2.0", "id": 99, "method": "GetObject", "params": {"id": "tree-1"}})
cb = read()                                                         # callback response from facade
write({"jsonrpc": "2.0", "id": req["id"], "result": {"got": cb["result"]}})
'''


def test_engine_roots_supply_the_engine_and_its_runtime_dependency_locations():
    # A recipe running in a child may parse a Python template, which reaches `parso`. The child's
    # venv has no engine and no engine deps, so both must travel on PYTHONPATH.
    roots = _engine_roots()
    assert roots[0] == ENGINE_ROOT
    assert str(metadata.distribution("parso").locate_file("")) in roots


def test_engine_roots_collapse_to_one_entry_when_deps_sit_beside_the_engine(monkeypatch):
    # Production: `pip install --target <engineDir>` flattens engine + deps into one directory,
    # so nothing beyond the engine root is exposed to children.
    def dist(name):
        if name == "openrewrite":
            return _FakeDist(requires=["parso>=0.7.1,<0.8", "more_itertools>=10.0.0"])
        return _FakeDist(location=ENGINE_ROOT)

    monkeypatch.setattr(child_connection.metadata, "distribution", dist)
    assert _engine_roots() == [ENGINE_ROOT]


def test_engine_roots_skip_optional_extras(monkeypatch):
    # dev/publish/profiling extras are not part of the platform a child needs.
    def dist(name):
        if name == "openrewrite":
            return _FakeDist(requires=[
                "parso>=0.7.1,<0.8",
                'pyroscope-io>=0.8.0; extra == "profiling"',
            ])
        return _FakeDist(location={"parso": "/deps"}.get(name, "/extras"))

    monkeypatch.setattr(child_connection.metadata, "distribution", dist)
    assert _engine_roots() == [ENGINE_ROOT, "/deps"]


def test_child_env_puts_the_engine_first_on_pythonpath(monkeypatch):
    monkeypatch.delenv("PYTHONPATH", raising=False)
    assert _child_env()["PYTHONPATH"].split(os.pathsep) == _engine_roots()


def test_child_env_prepends_the_engine_ahead_of_an_inherited_pythonpath(monkeypatch):
    monkeypatch.setenv("PYTHONPATH", "/already/there")
    # Engine first: it must win over anything the child venv or the host put on the path.
    assert _child_env()["PYTHONPATH"].split(os.pathsep) == [*_engine_roots(), "/already/there"]


def test_child_env_does_not_duplicate_an_already_present_engine(monkeypatch):
    monkeypatch.setenv("PYTHONPATH", os.pathsep.join([ENGINE_ROOT, "/other"]))
    assert _child_env()["PYTHONPATH"].split(os.pathsep) == [*_engine_roots(), "/other"]


def test_child_env_drops_excluded_inherited_paths(monkeypatch, tmp_path):
    # The Java host puts --recipe-install-dir (the venvs root) on the facade's PYTHONPATH. A child
    # must not inherit it: a stale flat package there would shadow the bundle's own copy.
    venvs_root = tmp_path / "recipes" / "pip"
    keep = str(tmp_path / "keep")
    monkeypatch.setenv("PYTHONPATH", os.pathsep.join([str(venvs_root), keep]))

    path = _child_env(exclude_paths=(str(venvs_root),))["PYTHONPATH"].split(os.pathsep)
    assert str(venvs_root) not in path
    assert path == [*_engine_roots(), keep]


def test_child_env_appends_the_facade_scripts_dir_to_path(monkeypatch):
    # PYTHONPATH carries importable code, not console binaries. `ty-types` ships both, and the
    # child's own interpreter has neither — so its binary is reachable only via PATH.
    scripts = sysconfig.get_path("scripts")
    monkeypatch.setenv("PATH", "/first")

    path = _child_env()["PATH"].split(os.pathsep)
    assert path[0] == "/first"       # appended, so the bundle's own python/pip stay ahead
    assert path[-1] == scripts


def test_child_env_does_not_duplicate_the_scripts_dir_already_on_path(monkeypatch):
    scripts = sysconfig.get_path("scripts")
    monkeypatch.setenv("PATH", os.pathsep.join([scripts, "/other"]))
    assert _child_env()["PATH"].split(os.pathsep) == [scripts, "/other"]


# The child runs on the *bundle venv's* interpreter, which has no engine of its own; only the
# environment is inherited. Prove the engine path actually reaches the spawned process.
STUB_ECHO_PYTHONPATH = r'''
import sys, json, os
buf = sys.stdin.buffer
cl = None
while True:
    line = buf.readline()
    if not line:
        sys.exit(0)
    s = line.decode("ascii").strip()
    if s == "":
        break
    if s.lower().startswith("content-length:"):
        cl = int(s.split(":", 1)[1])
body = b""
while len(body) < cl:
    body += buf.read(cl - len(body))
req = json.loads(body.decode("utf-8"))
resp = {"jsonrpc": "2.0", "id": req["id"], "result": os.environ.get("PYTHONPATH")}
data = json.dumps(resp).encode("utf-8")
out = sys.stdout.buffer
out.write(("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii"))
out.write(data)
out.flush()
'''


def test_spawn_hands_the_engine_pythonpath_to_the_child_process(tmp_path):
    stub = tmp_path / "stub.py"
    stub.write_text(STUB_ECHO_PYTHONPATH)
    conn = ChildConnection.spawn([sys.executable, str(stub)])
    try:
        seen = conn.request("GetMarketplace", {})
    finally:
        conn.close()
    assert seen.split(os.pathsep)[0] == ENGINE_ROOT


def test_child_connection_relays_callbacks_to_upstream(tmp_path):
    stub = tmp_path / "stub.py"
    stub.write_text(STUB_WITH_CALLBACK)
    seen = []

    def upstream(method, params):
        seen.append((method, params))
        return {"data": "from-java"}

    conn = ChildConnection.spawn([sys.executable, str(stub)], upstream=upstream)
    try:
        result = conn.request("Visit", {})
    finally:
        conn.close()

    assert seen == [("GetObject", {"id": "tree-1"})]        # child's callback relayed up
    assert result == {"got": {"data": "from-java"}}          # Java's response fed back to the child
