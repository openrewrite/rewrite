from types import SimpleNamespace

import pytest

from rewrite.rpc import venv_manager
from rewrite.rpc.bundle_children import BundleChildren


def _fake_venv_python(venv_dir):
    return venv_dir / "bin" / "python"


def _make_venv(venv_dir):
    interpreter = _fake_venv_python(venv_dir)
    interpreter.parent.mkdir(parents=True, exist_ok=True)
    interpreter.touch()


def _fake_is_usable_venv(venv_dir):
    return _fake_venv_python(venv_dir).exists()


class _FakeChild:
    def __init__(self, rows):
        self.rows = rows
        self.requests = []
        self.closed = False

    def request(self, method, params):
        self.requests.append((method, params))
        return self.rows if method == "GetMarketplace" else {"ran": method}

    def close(self):
        self.closed = True


def test_install_marketplace_owner_route_uninstall_and_shutdown(tmp_path):
    ops = SimpleNamespace(created=[], installed=[], removed=[])
    ops.venv_python = _fake_venv_python
    ops.is_usable_venv = _fake_is_usable_venv
    ops.create_venv = lambda py, d, clear=False: (ops.created.append((py, d)), _make_venv(d))
    ops.install_into_venv = lambda d, spec, force=False: ops.installed.append((d, spec, force))
    ops.installed_version = lambda d, dist: f"{dist}-1.0"
    ops.remove_venv = lambda d: ops.removed.append(d)

    spawned = {}

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        bundle = cmd[cmd.index("--child-bundle") + 1]
        child = _FakeChild([{"descriptor": {"name": f"{bundle}.Recipe"}}])
        spawned[bundle] = child
        return child

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=ops)

    # install: venv created + package installed + child spawned + marketplace cached + ownership
    rows = bc.install("pkga", "pkga==1.0")
    assert rows == [{"descriptor": {"name": "pkga.Recipe"}}]
    assert ops.created == [("py", tmp_path / "venvs" / "pkga")]
    assert ops.installed == [(tmp_path / "venvs" / "pkga", "pkga==1.0", False)]  # registry spec: no force
    assert bc.owner("pkga.Recipe") == "pkga"
    assert bc.marketplace() == rows
    assert bc.resolved_version("pkga") == "pkga-1.0"   # what the install layer reported, not the spec

    # a second bundle; marketplace merges
    bc.install("pkgb", "pkgb")
    assert bc.owner("pkgb.Recipe") == "pkgb"
    assert len(bc.marketplace()) == 2

    # route recipe execution to the owning child
    assert bc.request("pkga", "Visit", {"x": 1}) == {"ran": "Visit"}
    assert spawned["pkga"].requests[-1] == ("Visit", {"x": 1})

    # uninstall reaps the child, removes the venv, and drops ownership + resolved version
    bc.uninstall("pkga")
    assert spawned["pkga"].closed
    assert ops.removed == [tmp_path / "venvs" / "pkga"]
    assert bc.owner("pkga.Recipe") is None
    assert bc.resolved_version("pkga") is None

    bc.shutdown()
    assert spawned["pkgb"].closed


def _ops_recording(created, installed):
    ops = SimpleNamespace()
    ops.venv_python = _fake_venv_python
    ops.is_usable_venv = _fake_is_usable_venv
    ops.create_venv = lambda py, d, clear=False: (created.append(d), _make_venv(d))
    ops.install_into_venv = lambda d, spec, force=False: installed.append(d)
    ops.installed_version = lambda d, dist: "1.0.0"
    return ops


def test_set_data_table_store_broadcasts_to_live_children_and_replays_on_spawn(tmp_path):
    created, installed = [], []

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        bundle = cmd[cmd.index("--child-bundle") + 1]
        return _FakeChild([{"descriptor": {"name": f"{bundle}.R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording(created, installed))

    bc.install("pkga", "pkga")                       # a live child before the store is configured
    live = bc._children["pkga"]

    store = {"kind": "CSV", "outputDir": "/out"}
    bc.set_data_table_store(store)
    assert ("SetDataTableStore", store) in live.requests   # broadcast to the live child now

    bc.install("pkgb", "pkgb")                        # a bundle whose child is spawned later
    assert ("SetDataTableStore", store) in bc._children["pkgb"].requests  # replayed at spawn


def test_broadcast_evict_fans_out_to_live_children(tmp_path):
    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        bundle = cmd[cmd.index("--child-bundle") + 1]
        return _FakeChild([{"descriptor": {"name": f"{bundle}.R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording([], []))
    bc.install("pkga", "pkga")
    bc.install("pkgb", "pkgb")

    bc.broadcast_evict({"id": "tree-1"})
    # every live child is told to evict the file so each rolls back its own ref map
    assert ("Evict", {"id": "tree-1"}) in bc._children["pkga"].requests
    assert ("Evict", {"id": "tree-1"}) in bc._children["pkgb"].requests


def test_install_reuses_an_existing_venv_but_still_upgrades(tmp_path):
    created, installed = [], []

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        return _FakeChild([{"descriptor": {"name": "pkg.R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording(created, installed))

    # a real venv from a prior session (it has an interpreter) is reused, not recreated...
    _make_venv(tmp_path / "venvs" / "pkg")
    bc.install("pkg", "pkg")

    assert created == []                              # create skipped: the interpreter is present
    assert installed == [tmp_path / "venvs" / "pkg"]  # ...but pip --upgrade still runs (fixes #8180)


def test_install_rebuilds_a_directory_that_is_not_a_venv(tmp_path):
    """A leftover flat `pip install --target` package dir (or a half-created venv) has no
    interpreter, so it must be populated rather than trusted and exec'd."""
    created, installed = [], []

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        return _FakeChild([{"descriptor": {"name": "pkg.R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording(created, installed))

    legacy = tmp_path / "venvs" / "pkg"          # e.g. ~/.moderne/recipes/pip/pkg/__init__.py
    legacy.mkdir(parents=True)
    (legacy / "__init__.py").write_text("# stale flat install\n")

    bc.install("pkg", "pkg")

    assert created == [legacy]                   # directory exists, but no interpreter -> create
    assert installed == [legacy]


def test_install_rebuilds_a_venv_orphaned_by_a_python_upgrade(tmp_path):
    ops = SimpleNamespace(created=[], installed=[])
    ops.venv_python = _fake_venv_python
    ops.is_usable_venv = lambda d: False          # base interpreter pruned away
    ops.create_venv = lambda py, d, clear=False: (ops.created.append((d, clear)), _make_venv(d))
    ops.install_into_venv = lambda d, spec, force=False: ops.installed.append(d)
    ops.installed_version = lambda d, dist: "2.0"

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        return _FakeChild([{"descriptor": {"name": "pkg.R"}}])

    venv_dir = tmp_path / "venvs" / "pkg"
    _make_venv(venv_dir)                           # looks like a venv; its base is gone

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=ops)
    orphaned_child = _FakeChild([])                # a child still bound to the dead venv
    bc._children["pkg"] = orphaned_child

    bc.install("pkg", "pkg")

    assert ops.created == [(venv_dir, True)]       # rebuilt, clearing the orphaned tree
    assert ops.installed == [venv_dir]             # and the bundle reinstalled into it
    assert orphaned_child.closed                   # the child holding the dead venv was reaped
    assert bc._children["pkg"] is not orphaned_child


def test_child_is_denied_the_shared_venvs_root_on_its_path(tmp_path):
    created, installed = [], []
    seen = {}

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        seen["exclude_paths"] = exclude_paths
        return _FakeChild([{"descriptor": {"name": "pkg.R"}}])

    venvs_root = tmp_path / "venvs"
    bc = BundleChildren("py", venvs_root, upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording(created, installed))
    bc.install("pkg", "pkg")

    assert seen["exclude_paths"] == (str(venvs_root),)


def test_local_install_spawns_the_child_with_the_supplied_attribution_name(tmp_path):
    created, installed = [], []
    spawned = {}

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        spawned["cmd"] = cmd
        return _FakeChild([{"descriptor": {"name": "pkg.R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=_ops_recording(created, installed))
    bc.install("my-local-recipes", "/abs/path/to/src", force=True,
               attribution_name="/abs/path/to/src")

    # the resolved dist name filters discovery (--child-bundle); the supplied path is the attribution
    assert "--child-bundle" in spawned["cmd"]
    assert spawned["cmd"][spawned["cmd"].index("--child-bundle") + 1] == "my_local_recipes"
    assert "--attribution-name" in spawned["cmd"]
    assert spawned["cmd"][spawned["cmd"].index("--attribution-name") + 1] == "/abs/path/to/src"


def test_install_normalizes_distribution_name_so_spellings_dedup(tmp_path):
    ops = SimpleNamespace(created=[], installed=[])
    ops.venv_python = _fake_venv_python
    ops.is_usable_venv = _fake_is_usable_venv

    def _create(py, d, clear=False):
        ops.created.append(d)
        _make_venv(d)  # so the second install sees a real venv and skips creation

    ops.create_venv = _create
    ops.install_into_venv = lambda d, spec, force=False: ops.installed.append(d)
    ops.installed_version = lambda d, dist: "9.9"

    spawned = []

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        spawned.append(cmd[cmd.index("--child-bundle") + 1])
        return _FakeChild([{"descriptor": {"name": "R"}}])

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None,
                        spawn=fake_spawn, venv_ops=ops)

    # PEP 503 treats these as the same distribution -> one venv, one child, one normalized key.
    bc.install("Foo-Bar", "Foo-Bar")
    bc.install("foo_bar", "foo_bar")

    assert ops.created == [tmp_path / "venvs" / "foo_bar"]        # created once, normalized dir
    assert spawned == ["foo_bar"]                                 # one child, spawned once
    assert bc.request("FOO.BAR", "Visit", {}) == {"ran": "Visit"}  # any spelling routes to it


def _spawning_children(tmp_path, spawned, replaced=None):
    """A BundleChildren whose children list one shared recipe, tagged with their attribution."""
    created, installed = [], []

    def fake_spawn(cmd, upstream=None, exclude_paths=()):
        origin = (cmd[cmd.index("--attribution-name") + 1] if "--attribution-name" in cmd
                  else cmd[cmd.index("--child-bundle") + 1])
        child = _FakeChild([{"descriptor": {"name": "pkg.R"}, "packageName": origin}])
        child.cmd = cmd
        spawned.append(child)
        return child

    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None, spawn=fake_spawn,
                        venv_ops=_ops_recording(created, installed),
                        on_child_replaced=(replaced.append if replaced is not None else None))
    return bc, created, installed


def test_attach_runs_the_bundle_on_the_callers_venv_beside_its_published_namesake(tmp_path):
    spawned = []
    bc, created, installed = _spawning_children(tmp_path, spawned)
    bc.install("pkg", "pkg==1.0")
    published = spawned[-1]
    bc.install("other", "other")                 # another installed bundle defining pkg.R

    venv = tmp_path / "prebuilt"
    _make_venv(venv)
    rows = bc.attach("pkg", str(venv), attribution_name="/src/pkg")
    attached = spawned[-1]

    # nothing created or installed for the attached venv, and its child runs on that venv
    assert created == installed == [tmp_path / "venvs" / "pkg", tmp_path / "venvs" / "other"]
    assert attached.cmd[0] == str(venv_manager.venv_python(venv))
    assert attached.cmd[attached.cmd.index("--child-bundle") + 1] == "pkg"
    assert rows == [{"descriptor": {"name": "pkg.R"}, "packageName": "/src/pkg"}]

    # the published bundle keeps its child. Installed bundles list a shared recipe once, first-wins,
    # and the attached bundle's row is listed for its own reader.
    assert not published.closed
    assert bc.request("pkg", "Visit", {}) == {"ran": "Visit"}
    assert published.requests[-1] == ("Visit", {})
    assert [r["packageName"] for r in bc.marketplace()] == ["pkg", "/src/pkg"]

    # a recipe both bundles define runs from the attached build
    assert bc.owner("pkg.R") == str(venv)
    assert bc.resolved_version(str(venv)) == "1.0.0"

    # letting go of the attached bundle hands its recipes back and leaves the caller's venv alone
    bc.uninstall(str(venv))
    assert attached.closed and venv.exists()
    assert bc.owner("pkg.R") == "pkg"


def test_every_attach_restarts_the_bundles_child(tmp_path):
    spawned, replaced = [], []
    bc, _, _ = _spawning_children(tmp_path, spawned, replaced)
    first, second = tmp_path / "v1", tmp_path / "v2"
    _make_venv(first)
    _make_venv(second)

    bc.attach("pkg", str(first), attribution_name="/src/pkg")
    bc.attach("pkg", str(first), attribution_name="/src/pkg")   # rebuilt in place
    assert spawned[0].closed and not spawned[1].closed
    assert replaced == [str(first)]

    bc.attach("pkg", str(second), attribution_name="/src/pkg")  # rebuilt elsewhere
    assert spawned[1].closed
    assert bc.owner("pkg.R") == str(second)
    assert len(bc.marketplace()) == 1
    with pytest.raises(ValueError):
        bc.request(str(first), "Visit", {})                      # never respawned on the old venv


def test_a_later_install_of_the_same_source_path_lets_go_of_its_attached_venv(tmp_path):
    spawned = []
    bc, _, _ = _spawning_children(tmp_path, spawned)
    venv = tmp_path / "prebuilt"
    _make_venv(venv)

    bc.attach("pkg", str(venv), attribution_name="/src/pkg")
    bc.install("pkg", "/src/pkg", force=True, attribution_name="/src/pkg")

    assert spawned[0].closed
    assert bc.owner("pkg.R") == "pkg"


def test_attach_rejects_a_venv_without_the_distribution_and_keeps_the_current_one(tmp_path):
    spawned = []
    bc, _, _ = _spawning_children(tmp_path, spawned)
    current, empty = tmp_path / "current", tmp_path / "empty"
    _make_venv(current)
    _make_venv(empty)
    bc.attach("pkg", str(current), attribution_name="/src/pkg")
    bc._venv_ops.installed_version = lambda d, dist: None if d == empty else "1.0.0"

    with pytest.raises(ValueError, match="not a usable venv"):
        bc.attach("pkg", str(tmp_path / "missing"), attribution_name="/src/pkg")

    with pytest.raises(ValueError, match="'pkg' is not installed"):
        bc.attach("pkg", str(empty), attribution_name="/src/pkg")

    assert not spawned[0].closed
    assert bc.owner("pkg.R") == str(current)


def test_a_failed_attach_reaps_its_child_and_registers_nothing(tmp_path):
    def failing_spawn(cmd, upstream=None, exclude_paths=()):
        child = _FakeChild([])
        child.request = lambda method, params: (_ for _ in ()).throw(RuntimeError("child died"))
        failed.append(child)
        return child

    failed = []
    bc = BundleChildren("py", tmp_path / "venvs", upstream=lambda m, p: None, spawn=failing_spawn,
                        venv_ops=_ops_recording([], []))
    venv = tmp_path / "prebuilt"
    _make_venv(venv)

    with pytest.raises(RuntimeError, match="child died"):
        bc.attach("pkg", str(venv), attribution_name="/src/pkg")

    assert failed[0].closed
    assert bc._children == {} and bc._attached == {}
