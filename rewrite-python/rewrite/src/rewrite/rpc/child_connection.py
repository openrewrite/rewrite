"""Facade -> child transport.

Spawns a single-bundle child server (``server.py --child-bundle <dist>`` on the bundle's venv
interpreter) and exchanges JSON-RPC over its stdin/stdout using the same Content-Length framing
as the Java<->Python link. stdio is point-to-point, so the facade owns the child's pipes and speaks
to it directly here. The child inherits the facade's environment, which is how it receives the
engine itself (see ``_child_env``). A child's *outbound* callbacks toward Java (``GetObject`` etc.)
are relayed by handling inbound requests inside ``request``'s read loop.
"""
import json
import os
import re
import subprocess
import sysconfig
from importlib import metadata
from pathlib import Path

from rewrite.rpc import venv_manager

# The directory holding the `rewrite` package — i.e. this engine's own location on sys.path.
_ENGINE_ROOT = str(Path(__file__).resolve().parents[2])
_ENGINE_DIST = "openrewrite"
_REQUIREMENT_NAME = re.compile(r"[A-Za-z0-9._-]+")


def _path_key(path: str) -> str:
    """Comparison key for a sys.path/PATH entry, so the same directory spelled differently (case,
    relative) compares equal."""
    return os.path.normcase(os.path.abspath(path))


def _engine_roots() -> list:
    """Directories supplying the engine *and its runtime dependencies*.

    The platform a child needs is not just the ``rewrite`` package: a recipe may parse a Python
    template, which reaches ``parso``. A bundle venv has neither, so both must travel on the
    child's ``PYTHONPATH``.

    Resolving the dependencies' locations from metadata makes this self-tuning. In production the
    engine is a ``pip install --target <dir>`` tree, so ``parso`` and friends already sit beside
    ``rewrite`` and everything collapses to the single engine root — children see nothing else.
    Only in an editable/dev install do the dependencies live elsewhere (the parent's
    ``site-packages``), adding a second entry.
    """
    roots: list = []
    seen: set = set()

    def add(path: str) -> None:
        key = _path_key(path)
        if key not in seen:
            seen.add(key)
            roots.append(path)

    add(_ENGINE_ROOT)
    try:
        requires = metadata.distribution(_ENGINE_DIST).requires or []
    except metadata.PackageNotFoundError:
        return roots  # running straight from source with no installed metadata

    for requirement in requires:
        if "extra ==" in requirement:
            continue  # dev/publish/profiling extras are not part of a child's platform
        name = _REQUIREMENT_NAME.match(requirement.strip())
        if name is None:
            continue
        try:
            add(str(metadata.distribution(name.group(0)).locate_file("")))
        except metadata.PackageNotFoundError:
            continue  # declared but absent: contribute no path rather than fail the spawn
    return roots


def child_command(venv_dir: Path, bundle_dist: str, attribution_name=None) -> list:
    """Command to run a single-bundle child on the bundle's venv interpreter.

    ``attribution_name`` (a local install's supplied path) labels the child's recipes with the
    identity the host keys the bundle by, rather than the distribution name it filters on.
    """
    cmd = [
        str(venv_manager.venv_python(venv_dir)),
        "-m", "rewrite.rpc.server",
        "--child-bundle", bundle_dist,
    ]
    if attribution_name:
        cmd += ["--attribution-name", attribution_name]
    return cmd


def _child_env(exclude_paths=()) -> dict:
    """The child's environment, with the engine placed first on ``PYTHONPATH``.

    The engine is *platform*, not a bundle dependency: one pinned copy is shared by the facade and
    every child, the way Java delegates ``org.openrewrite.*`` to the parent classloader. A child
    runs on its bundle's venv interpreter, which has no engine of its own and inherits only the
    environment — so the engine can reach it by no route other than ``PYTHONPATH``.

    Placing it first also makes it win over ``site-packages``, so a copy pip may have dragged into
    the bundle's venv (as a recipe's transitive dependency) is shadowed rather than loaded.

    ``exclude_paths`` drops inherited entries a child must not import from — notably the shared
    recipe-install directory, which the Java host puts on the facade's ``PYTHONPATH``. Recipes now
    live in per-bundle venvs, and anything left directly in that root (a flat ``pip install
    --target`` layout from before per-bundle venvs) would otherwise be importable by *every* child,
    shadowing its own copy.

    ``PATH`` gains the facade's scripts directory, because ``PYTHONPATH`` carries importable code
    but not console binaries. ``ty-types`` ships both; a child's own interpreter has neither, and
    ``ty_types.find_ty_types_bin`` only locates the binary beside the package under a ``pip install
    --target`` layout. Appending (not prepending) leaves the bundle's own ``python``/``pip`` first
    for the subprocesses a recipe's template workspace spawns.
    """
    env = dict(os.environ)

    engine = _engine_roots()
    excluded = {_path_key(p) for p in (*exclude_paths, *engine)}
    inherited = [p for p in env.get("PYTHONPATH", "").split(os.pathsep)
                 if p and _path_key(p) not in excluded]
    env["PYTHONPATH"] = os.pathsep.join([*engine, *inherited])

    scripts = sysconfig.get_path("scripts")
    path = [p for p in env.get("PATH", "").split(os.pathsep) if p]
    if scripts and _path_key(scripts) not in {_path_key(p) for p in path}:
        path.append(scripts)
    env["PATH"] = os.pathsep.join(path)
    return env


class ChildConnection:
    """A spawned child RPC server addressed over its stdin/stdout with Content-Length framing."""

    @classmethod
    def spawn(cls, cmd: list, upstream=None, exclude_paths=()) -> "ChildConnection":
        proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                env=_child_env(exclude_paths))
        return cls(proc, upstream)

    def __init__(self, proc: subprocess.Popen, upstream=None):
        # upstream(method, params) -> result forwards a child's callback (e.g. GetObject) to Java.
        # In the facade this is server.send_request; injected so this module needn't import server.
        self._proc = proc
        self._upstream = upstream
        self._id = 0

    def request(self, method: str, params: dict):
        """Send a request and return its result, relaying any callbacks the child makes to Java."""
        self._id += 1
        rid = self._id
        self._write({"jsonrpc": "2.0", "id": rid, "method": method, "params": params})
        while True:
            msg = self._read()
            if msg is None:
                raise RuntimeError(f"child closed the connection during {method}")

            if msg.get("id") == rid and ("result" in msg or "error" in msg):
                if "error" in msg:
                    err = msg["error"]
                    tb = err.get("data")  # the child sends its full traceback here
                    detail = f"\nchild traceback:\n{tb}" if tb else ""
                    raise RuntimeError(f"child error for {method}: {err.get('message')}{detail}")
                return msg.get("result")

            callback_method = msg.get("method")
            if callback_method is not None:
                # Inbound callback request from the child (its GetObject/Print target the master
                # objects on Java). Relay to upstream and feed the response back with the child's id.
                child_id = msg.get("id")
                if self._upstream is None:
                    raise RuntimeError(
                        f"child issued a '{callback_method}' callback but no upstream is configured")
                try:
                    result = self._upstream(callback_method, msg.get("params", {}))
                    self._write({"jsonrpc": "2.0", "id": child_id, "result": result})
                except Exception as e:
                    self._write({"jsonrpc": "2.0", "id": child_id,
                                 "error": {"code": -32603, "message": str(e)}})

    def close(self) -> None:
        try:
            if self._proc.stdin:
                self._proc.stdin.close()
        except Exception:
            pass
        try:
            self._proc.terminate()
            self._proc.wait(timeout=5)
        except Exception:
            self._proc.kill()

    def _write(self, msg: dict) -> None:
        data = json.dumps(msg).encode("utf-8")
        header = ("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii")
        self._proc.stdin.write(header + data)
        self._proc.stdin.flush()

    def _read(self):
        content_length = None
        while True:
            line = self._proc.stdout.readline()
            if not line:
                return None  # EOF
            s = line.decode("ascii").strip()
            if s == "":
                break
            if s.lower().startswith("content-length:"):
                content_length = int(s.split(":", 1)[1])
        if content_length is None:
            return None
        body = b""
        while len(body) < content_length:
            chunk = self._proc.stdout.read(content_length - len(body))
            if not chunk:
                return None
            body += chunk
        return json.loads(body.decode("utf-8"))
