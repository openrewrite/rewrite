"""Tests for the DependencyTypes RPC handler (a pip dependency's exported types).

Mirrors the JavaScript package-exported-types test: enumerate a dependency's own
public types into JavaType and confirm the classes come out with populated methods
(the gap the normal parse-time mapping path leaves empty). The enumeration tests
require the ty-types CLI; the coordinate-resolution helpers are unit-tested
without it.
"""
import os
import shutil
import sys
import tempfile
from pathlib import Path

import pytest

from rewrite.java import JavaType
from rewrite.python.ty_client import TyTypesClient
from rewrite.rpc.server import (
    _artifact_files,
    _dist_info_dir,
    _enumerate_artifact,
    _resolve_dist_artifacts,
    _richness,
    _typeshed_stdlib_dir,
    _write_stdlib_ty_config,
    handle_dependency_types,
)


def _ty_types_cli_available() -> bool:
    try:
        from ty_types.__main__ import find_ty_types_bin  # noqa: F401
        return True
    except Exception:
        return shutil.which('ty-types') is not None


requires_ty_types_cli = pytest.mark.skipif(
    not _ty_types_cli_available(),
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)",
)

FIXTURE = '''
class Greeter:
    def __init__(self, name: str) -> None:
        self.name = name

    def greet(self, greeting: str) -> str:
        return greeting + self.name

    def reset(self) -> None:
        self.name = ""


class Repository:
    def find(self, id: str):
        ...

    def save(self, entity) -> None:
        ...
'''


def _cls(fqn: str) -> JavaType.Class:
    c = JavaType.Class()
    c._flags_bit_map = 0
    c._kind = JavaType.FullyQualified.Kind.Class
    c._fully_qualified_name = fqn
    return c


class TestResolveDistArtifacts:
    """Filesystem-only; no ty binary required."""

    def _site_packages(self, tmp_path) -> Path:
        sp = tmp_path / "env" / "lib" / "python3.12" / "site-packages"
        sp.mkdir(parents=True)
        return sp

    def test_record_keeps_own_sources_stub_first(self, tmp_path):
        sp = self._site_packages(tmp_path)
        pkg = sp / "greetlib"
        pkg.mkdir()
        (pkg / "__init__.py").write_text("class A: ...\n")
        (pkg / "thing.py").write_text("class Thing: ...\n")
        (pkg / "thing.pyi").write_text("class Thing: ...\n")
        (pkg / "data.json").write_text("{}")
        di = sp / "greetlib-1.0.0.dist-info"
        di.mkdir()
        (di / "RECORD").write_text(
            "greetlib/__init__.py,,\n"
            "greetlib/thing.py,,\n"
            "greetlib/thing.pyi,,\n"
            "greetlib/data.json,,\n"          # not a source
            "greetlib/missing.py,,\n"         # listed but absent
            "../../../bin/greet.py,,\n")      # escapes site-packages
        files = _resolve_dist_artifacts("greetlib", "1.0.0", sp)
        assert files == [str(pkg / "__init__.py"), str(pkg / "thing.pyi"), str(pkg / "thing.py")]

    def test_dist_info_matches_on_normalized_name(self, tmp_path):
        sp = self._site_packages(tmp_path)
        di = sp / "my_pkg_name-2.1.dist-info"
        di.mkdir()
        assert _dist_info_dir(sp, "My-Pkg.Name", "2.1") == di

    def test_missing_dist_raises(self, tmp_path):
        sp = self._site_packages(tmp_path)
        with pytest.raises(ValueError):
            _dist_info_dir(sp, "absent", "1.0")

    def test_recordless_dist_falls_back_to_top_level(self, tmp_path):
        sp = self._site_packages(tmp_path)
        pkg = sp / "greet_impl"
        pkg.mkdir()
        (pkg / "__init__.py").write_text("")
        di = sp / "greetlib-1.0.0.dist-info"
        di.mkdir()
        (di / "top_level.txt").write_text("greet_impl\n")
        assert _resolve_dist_artifacts("greetlib", "1.0.0", sp) == [str(pkg)]

    def test_recordless_dist_without_top_level_uses_normalized_name(self, tmp_path):
        sp = self._site_packages(tmp_path)
        pkg = sp / "greet_lib"
        pkg.mkdir()
        (pkg / "__init__.py").write_text("")
        (sp / "greet_lib-1.0.0.dist-info").mkdir()
        assert _resolve_dist_artifacts("greet-lib", "1.0.0", sp) == [str(pkg)]


class TestTypeshedResolution:
    """Filesystem-only; no ty binary required."""

    def _fake_typeshed(self, tmp_path) -> Path:
        stdlib = tmp_path / "typeshed" / "stdlib"
        (stdlib / "os").mkdir(parents=True)
        (stdlib / "VERSIONS").write_text("os: 3.0-\nbuiltins: 3.0-\n")
        (stdlib / "os" / "__init__.pyi").write_text("def getcwd() -> str: ...\n")
        (stdlib / "builtins.pyi").write_text("class int: ...\n")
        return stdlib

    def test_env_var_may_name_root_or_stdlib(self, tmp_path, monkeypatch):
        stdlib = self._fake_typeshed(tmp_path)
        monkeypatch.setenv("REWRITE_PYTHON_TYPESHED", str(stdlib.parent))
        assert _typeshed_stdlib_dir() == stdlib
        monkeypatch.setenv("REWRITE_PYTHON_TYPESHED", str(stdlib))
        assert _typeshed_stdlib_dir() == stdlib

    def test_unset_env_raises(self, monkeypatch):
        monkeypatch.delenv("REWRITE_PYTHON_TYPESHED", raising=False)
        with pytest.raises(ValueError):
            _typeshed_stdlib_dir()

    def test_handler_resolves_stdlib_module_artifact(self, tmp_path, monkeypatch):
        import rewrite.rpc.server as server
        stdlib = self._fake_typeshed(tmp_path)
        monkeypatch.setenv("REWRITE_PYTHON_TYPESHED", str(stdlib.parent))
        captured = {}

        def fake_build(own_artifacts, root, virtual_env, stdlib_dir, python_version=None):
            captured.update(own=own_artifacts, root=root, stdlib=stdlib_dir)
            return [{"state": "END_OF_OBJECT"}]

        monkeypatch.setattr(server, "_build_exported_types_data", fake_build)
        server._dependency_types_pending.clear()
        assert handle_dependency_types({"name": "os"})[-1]["state"] == "END_OF_OBJECT"
        assert captured["own"] == [str(stdlib / "os")]            # package-dir module
        assert captured["stdlib"] == stdlib
        assert handle_dependency_types({"name": "builtins"})[-1]["state"] == "END_OF_OBJECT"
        assert captured["own"] == [str(stdlib / "builtins.pyi")]  # single-stub module

    def test_stdlib_ty_config_carries_requested_version(self, tmp_path):
        stdlib = self._fake_typeshed(tmp_path)
        config_dir = _write_stdlib_ty_config(stdlib, "3.11")
        try:
            content = (Path(config_dir) / "ty.toml").read_text()
            assert 'python-version = "3.11"' in content
            assert f'typeshed = "{stdlib.parent}"' in content
        finally:
            shutil.rmtree(config_dir, ignore_errors=True)

    def test_stdlib_ty_config_falls_back_to_interpreter_minor(self, tmp_path):
        stdlib = self._fake_typeshed(tmp_path)
        interpreter = "%d.%d" % (sys.version_info.major, sys.version_info.minor)
        for requested in (None, "banana", "3", "3.12.1"):
            config_dir = _write_stdlib_ty_config(stdlib, requested)
            try:
                content = (Path(config_dir) / "ty.toml").read_text()
                assert f'python-version = "{interpreter}"' in content
            finally:
                shutil.rmtree(config_dir, ignore_errors=True)

    def test_python_version_reaches_stdlib_build_and_cache_key(self, tmp_path, monkeypatch):
        import rewrite.rpc.server as server
        stdlib = self._fake_typeshed(tmp_path)
        monkeypatch.setenv("REWRITE_PYTHON_TYPESHED", str(stdlib.parent))
        captured = {}

        def fake_build(own_artifacts, root, virtual_env, stdlib_dir, python_version=None):
            captured["python_version"] = python_version
            return [{"state": "CHANGE"}, {"state": "END_OF_OBJECT"}]

        monkeypatch.setattr(server, "_build_exported_types_data", fake_build)
        monkeypatch.setattr(server, "_DEPENDENCY_TYPES_BATCH_SIZE", 1)
        server._dependency_types_pending.clear()
        try:
            server.handle_dependency_types({"name": "os", "pythonVersion": "3.11"})
            assert captured["python_version"] == "3.11"
            assert ("os", None, "3.11") in server._dependency_types_pending

            # A different requested version must not drain 3.11's pending pages.
            server.handle_dependency_types({"name": "os", "pythonVersion": "3.12"})
            assert captured["python_version"] == "3.12"
            assert ("os", None, "3.12") in server._dependency_types_pending

            server.handle_dependency_types({"name": "os", "pythonVersion": "not-a-version"})
            assert captured["python_version"] is None  # malformed falls back
            assert ("os", None, None) in server._dependency_types_pending
        finally:
            server._dependency_types_pending.clear()

    def test_missing_stdlib_module_raises(self, tmp_path, monkeypatch):
        import rewrite.rpc.server as server
        stdlib = self._fake_typeshed(tmp_path)
        monkeypatch.setenv("REWRITE_PYTHON_TYPESHED", str(stdlib.parent))
        server._dependency_types_pending.clear()
        with pytest.raises(ValueError):
            handle_dependency_types({"name": "not_a_module"})

    def test_package_request_without_dependency_env_raises(self, monkeypatch):
        import rewrite.rpc.server as server
        monkeypatch.setattr(server, "_last_dependency_path", None)
        monkeypatch.delenv("VIRTUAL_ENV", raising=False)
        server._dependency_types_pending.clear()
        with pytest.raises(ValueError):
            handle_dependency_types({"name": "greetlib", "version": "1.0.0"})


class TestRichness:
    """No ty binary required."""

    def test_supertypes_and_interfaces_count(self):
        method_rich = _cls("m.A")
        method_rich._methods = [None]  # _richness only measures length

        supertype_rich = _cls("m.B")
        supertype_rich._supertype = _cls("m.Base")
        supertype_rich._interfaces = [_cls("m.I1"), _cls("m.I2")]
        supertype_rich._type_parameters = [_cls("m.T")]

        assert _richness(method_rich) == 1
        assert _richness(supertype_rich) == 4  # supertype + 2 interfaces + 1 type param
        # The supertype-rich class must not be judged emptier than the method-rich one.
        assert _richness(supertype_rich) > _richness(method_rich)


class TestArtifactFiles:
    """No ty binary required."""

    def test_pyi_sorts_before_py_sibling(self, tmp_path):
        pkg = tmp_path / "pkg"
        pkg.mkdir()
        (pkg / "thing.py").write_text("class Thing: ...\n")
        (pkg / "thing.pyi").write_text("class Thing: ...\n")
        (pkg / "other.py").write_text("class Other: ...\n")
        names = [os.path.basename(f) for f in _artifact_files(pkg)]
        assert names.index("thing.pyi") < names.index("thing.py")
        assert set(names) == {"thing.pyi", "thing.py", "other.py"}


def _fake_dependency_env(root: Path, name: str = "greetlib", version: str = "1.0.0") -> Path:
    """A minimal installed-dist venv: pyvenv.cfg + site-packages holding the package
    files and a dist-info carrying METADATA/RECORD."""
    venv = root / "env"
    sp = venv / "lib" / f"python{sys.version_info.major}.{sys.version_info.minor}" / "site-packages"
    pkg = sp / name
    pkg.mkdir(parents=True)
    (venv / "pyvenv.cfg").write_text(
        f"home = /usr\nversion = {sys.version_info.major}.{sys.version_info.minor}.0\n")
    (pkg / "__init__.py").write_text(FIXTURE)
    dist_info = sp / f"{name}-{version}.dist-info"
    dist_info.mkdir()
    (dist_info / "METADATA").write_text(f"Metadata-Version: 2.1\nName: {name}\nVersion: {version}\n")
    (dist_info / "RECORD").write_text(
        f"{name}/__init__.py,,\n"
        f"{name}-{version}.dist-info/METADATA,,\n"
        f"{name}-{version}.dist-info/RECORD,,\n")
    return venv


@requires_ty_types_cli
class TestShallowReferenceWireTag:
    """A body-less reference streams with the ShallowClass value type so the
    receiver can tell a stub from a class the dependency defines."""

    def test_shallow_reference_streams_as_shallow_class(self):
        from rewrite.python.type_mapping import PythonTypeMapping
        from rewrite.rpc.python_sender import PythonRpcSender
        from rewrite.rpc.send_queue import RpcSendQueue

        full = _cls("greetlib.Greeter")
        full._supertype = PythonTypeMapping('x = 1')._create_class_type('missing_dep.Base')
        assert isinstance(full._supertype, JavaType.ShallowClass)

        q = RpcSendQueue('org.openrewrite.java.tree.JavaType$Class')
        sender = PythonRpcSender()
        q.send_list([full], None, sender._type_signature,
                    lambda t: sender._visit_type(t, q), as_ref=True)
        tags = {d.get('valueType') for d in q.q}
        assert 'org.openrewrite.java.tree.JavaType$Class' in tags
        assert 'org.openrewrite.java.tree.JavaType$ShallowClass' in tags


class TestExportedTypes:

    def _write_fixture(self, root: str) -> str:
        pkg = os.path.join(root, "greetlib")
        os.makedirs(pkg, exist_ok=True)
        with open(os.path.join(pkg, "__init__.py"), "w", encoding="utf-8") as f:
            f.write(FIXTURE)
        return pkg

    def test_enumerates_own_types_with_methods(self):
        root = tempfile.mkdtemp(prefix="rewrite-exported-types-")
        try:
            pkg = self._write_fixture(root)
            client = TyTypesClient()
            assert client.initialize(root)
            by_fqn: dict = {}
            _enumerate_artifact(pkg, root, client, by_fqn, set())
            client.shutdown()

            greeter = by_fqn.get("greetlib.Greeter")
            assert isinstance(greeter, JavaType.Class)
            method_names = {m._name for m in (greeter._methods or [])}
            assert "greet" in method_names
            assert "reset" in method_names

            repo = by_fqn.get("greetlib.Repository")
            assert isinstance(repo, JavaType.Class)
            repo_methods = {m._name for m in (repo._methods or [])}
            assert {"find", "save"} <= repo_methods
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_own_module_filter_excludes_sibling(self):
        # "clickhouse" shares the "click" prefix but not the "click." boundary,
        # so a class it defines must not leak into click's own exported types even
        # when click imports it.
        root = tempfile.mkdtemp(prefix="rewrite-exported-types-")
        try:
            click = os.path.join(root, "click")
            os.makedirs(click, exist_ok=True)
            with open(os.path.join(click, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("from clickhouse import ClickhouseThing\n\n"
                        "class ClickThing:\n"
                        "    def run(self) -> None:\n        ...\n")
            house = os.path.join(root, "clickhouse")
            os.makedirs(house, exist_ok=True)
            with open(os.path.join(house, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("class ClickhouseThing:\n"
                        "    def run(self) -> None:\n        ...\n")

            client = TyTypesClient()
            assert client.initialize(root)
            by_fqn: dict = {}
            _enumerate_artifact(click, root, client, by_fqn, set())
            client.shutdown()

            assert "click.ClickThing" in by_fqn
            assert not any(k.startswith("clickhouse") for k in by_fqn)
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_namespace_package_scopes_to_own_dist(self):
        # A PEP 420 namespace: two dists share google/cloud/ with no __init__.py at google/ or
        # google/cloud/. Enumerating only one dist's files (as RECORD scopes it) must yield that
        # dist's types under their full FQN (site-packages rooting) and never the sibling's.
        root = tempfile.mkdtemp(prefix="rewrite-exported-types-")
        try:
            storage = os.path.join(root, "google", "cloud", "storage")
            os.makedirs(storage, exist_ok=True)
            with open(os.path.join(storage, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("class Blob:\n    def upload(self) -> None:\n        ...\n")
            bigquery = os.path.join(root, "google", "cloud", "bigquery")
            os.makedirs(bigquery, exist_ok=True)
            with open(os.path.join(bigquery, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("class Table:\n    def query(self) -> None:\n        ...\n")

            client = TyTypesClient()
            assert client.initialize(root)
            by_fqn: dict = {}
            _enumerate_artifact(os.path.join(storage, "__init__.py"), root, client, by_fqn, set())
            client.shutdown()

            assert "google.cloud.storage.Blob" in by_fqn
            assert not any(k.startswith("google.cloud.bigquery") for k in by_fqn)
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_dedup_keeps_single_class_for_py_and_pyi(self):
        root = tempfile.mkdtemp(prefix="rewrite-exported-types-")
        try:
            pkg = os.path.join(root, "stubbed")
            os.makedirs(pkg, exist_ok=True)
            with open(os.path.join(pkg, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("")
            with open(os.path.join(pkg, "thing.py"), "w", encoding="utf-8") as f:
                f.write("class Thing:\n"
                        "    def api(self):\n        return 0\n")
            with open(os.path.join(pkg, "thing.pyi"), "w", encoding="utf-8") as f:
                f.write("class Thing:\n"
                        "    def api(self) -> int: ...\n")

            client = TyTypesClient()
            assert client.initialize(root)
            by_fqn: dict = {}
            _enumerate_artifact(pkg, root, client, by_fqn, set())
            client.shutdown()

            things = [k for k in by_fqn if k.endswith("thing.Thing")]
            assert len(things) == 1  # the .py and .pyi collapse to one entry
            thing = by_fqn[things[0]]
            method_names = {m._name for m in (thing._methods or [])}
            assert "api" in method_names
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_multifile_package_does_not_remap(self):
        import rewrite.python.type_mapping as tm

        root = tempfile.mkdtemp(prefix="rewrite-exported-types-")
        try:
            pkg = os.path.join(root, "multi")
            os.makedirs(pkg, exist_ok=True)
            with open(os.path.join(pkg, "__init__.py"), "w", encoding="utf-8") as f:
                f.write("")
            with open(os.path.join(pkg, "a.py"), "w", encoding="utf-8") as f:
                f.write("class Alpha:\n"
                        "    def m(self, x: str) -> str:\n        return x\n")
            # b.py references Alpha, so ty back-fills Alpha into b.py's registry.
            with open(os.path.join(pkg, "b.py"), "w", encoding="utf-8") as f:
                f.write("from multi.a import Alpha\n\n"
                        "value: Alpha = Alpha()\n\n"
                        "class Beta:\n"
                        "    def n(self) -> None:\n        ...\n")

            client = TyTypesClient()
            assert client.initialize(root)
            by_fqn: dict = {}
            processed: set = set()
            _enumerate_artifact(pkg, root, client, by_fqn, processed)

            assert "multi.a.Alpha" in by_fqn
            assert "multi.b.Beta" in by_fqn
            first = set(processed)
            assert first  # something was enumerated

            # A second pass over the same session with the same processed_ids must
            # do no mapping work: every own type id is already recorded.
            calls: list = []
            original = tm.PythonTypeMapping._descriptor_to_java_type

            def counting(self, descriptor):
                calls.append(descriptor.get('kind'))
                return original(self, descriptor)

            tm.PythonTypeMapping._descriptor_to_java_type = counting
            try:
                _enumerate_artifact(pkg, root, client, by_fqn, processed)
            finally:
                tm.PythonTypeMapping._descriptor_to_java_type = original
            client.shutdown()

            assert calls == []         # no re-mapping on the second pass
            assert processed == first  # no new ids recorded
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_handler_resolves_coordinate_from_session_dependency_env(self, tmp_path, monkeypatch):
        import rewrite.rpc.server as server

        venv = _fake_dependency_env(tmp_path)
        monkeypatch.setattr(server, "_last_dependency_path", str(venv))
        server._dependency_types_pending.clear()
        batch = handle_dependency_types({"name": "greetlib", "version": "1.0.0"})

        assert batch[0]["state"] == "ADD"       # list opens
        assert batch[1]["state"] == "CHANGE"    # positions array
        assert batch[-1]["state"] == "END_OF_OBJECT"
        positions = batch[1].get("value") or []
        assert len(positions) >= 1
        # A method name reaches the wire, proving methods are populated.
        assert any(m.get("value") == "greet" for m in batch)

    def test_handler_pages_response_across_repeated_requests(self, tmp_path, monkeypatch):
        import rewrite.rpc.server as server

        venv = _fake_dependency_env(tmp_path)
        monkeypatch.setattr(server, "_last_dependency_path", str(venv))
        params = {"name": "greetlib", "version": "1.0.0"}
        key = ("greetlib", "1.0.0", None)
        server._dependency_types_pending.clear()

        # A single-shot run: one response carrying the whole answer.
        monkeypatch.setattr(server, "_DEPENDENCY_TYPES_BATCH_SIZE", 10 ** 9)
        full = server.handle_dependency_types(params)
        assert full[-1]["state"] == "END_OF_OBJECT"
        assert key not in server._dependency_types_pending

        # Same coordinate under a tiny batch size, drained across repeated calls.
        monkeypatch.setattr(server, "_DEPENDENCY_TYPES_BATCH_SIZE", 2)
        collected = []
        while True:
            batch = server.handle_dependency_types(params)
            assert len(batch) <= 2                        # (a) never exceeds batch_size
            collected.extend(batch)
            if key not in server._dependency_types_pending:     # final slice evicts the key
                break

        assert collected == full                          # (b) same items, same order
        assert collected[-1]["state"] == "END_OF_OBJECT"
        assert key not in server._dependency_types_pending      # (c) evicted after final slice
