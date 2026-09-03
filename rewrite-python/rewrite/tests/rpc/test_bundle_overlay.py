import sys
import textwrap

from rewrite.rpc.bundle_overlay import Bundle


def json_module():
    import json
    return json


def _bundle(tmp_path, name, body):
    """A bundle venv holding one module, laid out the way `venv_manager.site_packages` finds it."""
    site_packages = tmp_path / name / "lib" / "python3.12" / "site-packages"
    site_packages.mkdir(parents=True)
    (site_packages / f"{name}.py").write_text(textwrap.dedent(body))
    return Bundle(name, site_packages)


def test_each_bundle_resolves_its_own_copy_of_a_shared_module_name(tmp_path):
    """The point of the private table: one name, two versions, neither visible to the other."""
    for name, version in (("pkg_a", "1"), ("pkg_b", "2")):
        site_packages = tmp_path / name / "lib" / "python3.12" / "site-packages"
        site_packages.mkdir(parents=True)
        (site_packages / "shared.py").write_text(f"VERSION = {version}\n")
        (site_packages / f"{name}.py").write_text("import shared\n\ndef probe():\n    return shared.VERSION\n")

    a = Bundle("pkg_a", tmp_path / "pkg_a" / "lib" / "python3.12" / "site-packages")
    b = Bundle("pkg_b", tmp_path / "pkg_b" / "lib" / "python3.12" / "site-packages")

    for _ in range(2):  # entering twice: the swap has to survive re-entry, not just first import
        with a.active():
            assert __import__("pkg_a").probe() == 1
        with b.active():
            assert __import__("pkg_b").probe() == 2

    assert a.modules["shared"] is not b.modules["shared"]
    assert "shared" not in sys.modules      # and neither copy is left behind in the host


def test_a_module_from_outside_the_venv_stays_shared(tmp_path):
    """The engine lives outside every bundle venv, so it is never captured and every bundle sees
    the one instance."""
    bundle = _bundle(tmp_path, "pkg_c", """
        import json

        def probe():
            return json
        """)

    with bundle.active():
        assert __import__("pkg_c").probe() is json_module()
    assert "json" not in bundle.modules
    assert sys.modules["json"] is json_module()


def test_a_lazy_import_inside_a_call_resolves_to_the_calling_bundle(tmp_path):
    """A recipe imports at call time, not import time, so ownership has to hold then too."""
    for name, version in (("pkg_d", "10"), ("pkg_e", "20")):
        site_packages = tmp_path / name / "lib" / "python3.12" / "site-packages"
        site_packages.mkdir(parents=True)
        (site_packages / "lazy.py").write_text(f"VERSION = {version}\n")
        (site_packages / f"{name}.py").write_text(
            "def probe():\n    import lazy\n    return lazy.VERSION\n")

    d = Bundle("pkg_d", tmp_path / "pkg_d" / "lib" / "python3.12" / "site-packages")
    e = Bundle("pkg_e", tmp_path / "pkg_e" / "lib" / "python3.12" / "site-packages")

    with d.active():
        probe_d = __import__("pkg_d").probe
        assert probe_d() == 10
    with e.active():
        assert __import__("pkg_e").probe() == 20
    with d.active():
        assert probe_d() == 10


def test_the_host_keeps_the_path_and_modules_it_started_with(tmp_path):
    bundle = _bundle(tmp_path, "pkg_f", "VALUE = 1\n")
    before_path, before_modules = sys.path[:], set(sys.modules)

    with bundle.active():
        __import__("pkg_f")

    assert sys.path == before_path
    assert set(sys.modules) - before_modules == set()


def test_bundles_interleaved_across_files_each_keep_their_own_dependency(tmp_path):
    """The scheduler runs recipe A then B on one file before moving to the next, so the tables are
    swapped per visit rather than per file and have to survive arbitrary alternation."""
    bundles = []
    for name, version in (("pkg_g", 100), ("pkg_h", 200)):
        site_packages = tmp_path / name / "lib" / "python3.12" / "site-packages"
        site_packages.mkdir(parents=True)
        (site_packages / "dep.py").write_text(f"VERSION = {version}\n")
        (site_packages / f"{name}.py").write_text(
            "def visit(file):\n    import dep\n    return (file, dep.VERSION)\n")
        bundles.append((Bundle(name, site_packages), name, version))

    seen = []
    for file in ("F1", "F2", "F3"):
        for bundle, name, _ in bundles:
            with bundle.active():
                seen.append(__import__(name).visit(file))

    assert seen == [("F1", 100), ("F1", 200),
                    ("F2", 100), ("F2", 200),
                    ("F3", 100), ("F3", 200)]
