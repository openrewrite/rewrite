from types import SimpleNamespace

from rewrite.rpc.bundles import Bundles


def _fake_venv_python(venv_dir):
    return venv_dir / "bin" / "python"


def _make_venv(venv_dir):
    interpreter = _fake_venv_python(venv_dir)
    interpreter.parent.mkdir(parents=True, exist_ok=True)
    interpreter.touch()


def _ops(created=None, installed=None, removed=None):
    ops = SimpleNamespace()
    ops.venv_python = _fake_venv_python
    ops.is_usable_venv = lambda d: _fake_venv_python(d).exists()
    ops.create_venv = lambda py, d, clear=False: (
        created.append((py, d, clear)) if created is not None else None, _make_venv(d))[0]
    ops.install_into_venv = lambda d, spec, force=False: (
        installed.append((d, spec, force)) if installed is not None else None)
    ops.installed_version = lambda d, dist: f"{dist}-1.0"
    ops.site_packages = lambda d: d / "lib" / "site-packages"
    ops.remove_venv = lambda d: (removed.append(d) if removed is not None else None)
    return ops


def _activating(rows_by_dist, seen=None):
    def activate(overlay, bundle_dist, attribution_name):
        if seen is not None:
            seen.append((overlay.dist, bundle_dist, attribution_name))
        return rows_by_dist(bundle_dist)
    return activate


def test_install_activates_the_bundle_and_records_what_it_contributed(tmp_path):
    created, installed, seen = [], [], []
    bc = Bundles("py", tmp_path / "venvs", venv_ops=_ops(created, installed),
                 activate=_activating(lambda d: [{"descriptor": {"name": f"{d}.R"}}], seen))

    rows = bc.install("pkga", "pkga==1.0")

    assert created == [("py", tmp_path / "venvs" / "pkga", False)]
    assert installed == [(tmp_path / "venvs" / "pkga", "pkga==1.0", False)]  # registry spec: no force
    assert seen == [("pkga", "pkga", None)]          # activated under its own module table
    assert rows == [{"descriptor": {"name": "pkga.R"}}]
    assert bc.owner("pkga.R") == "pkga"
    assert bc.resolved_version("pkga") == "pkga-1.0"  # what the install layer reported, not the spec


def test_every_bundle_gets_its_own_module_table(tmp_path):
    """The table is what keeps two bundles' dependencies apart now that they share a process."""
    bc = Bundles("py", tmp_path / "venvs", venv_ops=_ops(),
                 activate=_activating(lambda d: [{"descriptor": {"name": f"{d}.R"}}]))
    bc.install("pkga", "pkga")
    bc.install("pkgb", "pkgb")

    assert bc.overlay("pkga") is not bc.overlay("pkgb")
    assert len(bc.marketplace()) == 2
    assert bc.overlay("nobody") is None


def test_any_spelling_of_a_distribution_names_the_same_bundle(tmp_path):
    """PEP 503 folds hyphens, underscores and case, so one venv and one table serve all of them."""
    created = []
    bc = Bundles("py", tmp_path / "venvs", venv_ops=_ops(created),
                 activate=_activating(lambda d: [{"descriptor": {"name": "R"}}]))
    bc.install("Foo-Bar", "Foo-Bar")
    bc.install("foo_bar", "foo_bar")

    assert [d for _py, d, _clear in created] == [tmp_path / "venvs" / "foo_bar"]
    assert bc.overlay("FOO.BAR") is bc.overlay("foo-bar")


def test_a_venv_that_is_not_usable_is_rebuilt_before_the_bundle_installs(tmp_path):
    """A leftover flat package dir or a venv orphaned by an interpreter upgrade has to be
    populated rather than trusted."""
    created, installed = [], []
    ops = _ops(created, installed)
    ops.is_usable_venv = lambda d: False
    legacy = tmp_path / "venvs" / "pkg"
    legacy.mkdir(parents=True)

    bc = Bundles("py", tmp_path / "venvs", venv_ops=ops,
                 activate=_activating(lambda d: [{"descriptor": {"name": "R"}}]))
    bc.install("pkg", "pkg")

    assert created == [("py", legacy, True)]     # existed but unusable -> recreate, clearing it
    assert installed == [(legacy, "pkg", False)]


def test_uninstall_drops_the_bundle_its_recipes_and_its_modules(tmp_path):
    """Unlike an import, a module table can be discarded, so a removed bundle leaves nothing."""
    removed = []
    bc = Bundles("py", tmp_path / "venvs", venv_ops=_ops(removed=removed),
                 activate=_activating(lambda d: [{"descriptor": {"name": f"{d}.R"}}]))
    bc.install("pkga", "pkga")
    bc.install("pkgb", "pkgb")

    bc.uninstall("pkga")

    assert removed == [tmp_path / "venvs" / "pkga"]
    assert bc.overlay("pkga") is None
    assert bc.owner("pkga.R") is None
    assert bc.resolved_version("pkga") is None
    assert len(bc.marketplace()) == 1
