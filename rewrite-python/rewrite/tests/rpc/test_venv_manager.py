import subprocess
from pathlib import Path

from rewrite.rpc import venv_manager


def _capture(monkeypatch):
    calls = []

    class _CP:
        returncode = 0
        stdout = ""
        stderr = ""

    def fake_run(cmd, capture_output=False, text=False):
        calls.append(cmd)
        return _CP()

    monkeypatch.setattr(subprocess, "run", fake_run)
    return calls


def test_create_venv_uses_stdlib_venv_of_given_interpreter(monkeypatch, tmp_path):
    calls = _capture(monkeypatch)
    venv_dir = tmp_path / "b1"
    venv_manager.create_venv("/usr/bin/python3.12", venv_dir)
    assert calls == [["/usr/bin/python3.12", "-m", "venv", str(venv_dir)]]


def test_venv_python_points_inside_the_venv(tmp_path):
    venv_dir = tmp_path / "b1"
    py = venv_manager.venv_python(venv_dir)
    # Interpreter lives under the venv, in Scripts (Windows) or bin (POSIX).
    assert venv_dir in py.parents
    assert py.parent.name in ("Scripts", "bin")


def test_install_into_venv_upgrades_via_the_venv_pip(monkeypatch, tmp_path):
    calls = _capture(monkeypatch)
    venv_dir = tmp_path / "b1"
    venv_manager.install_into_venv(venv_dir, "openrewrite-migrate-python")
    py = str(venv_manager.venv_python(venv_dir))
    assert calls == [[py, "-m", "pip", "install", "--upgrade", "openrewrite-migrate-python"]]


def test_install_into_venv_force_reinstalls_mutable_local_sources(monkeypatch, tmp_path):
    calls = _capture(monkeypatch)
    venv_dir = tmp_path / "b1"
    venv_manager.install_into_venv(venv_dir, "./local-recipes", force=True)
    py = str(venv_manager.venv_python(venv_dir))
    assert calls == [[py, "-m", "pip", "install", "--upgrade", "--force-reinstall", "./local-recipes"]]


def test_remove_venv_deletes_the_directory(tmp_path):
    venv_dir = tmp_path / "b1"
    (venv_dir / "sub").mkdir(parents=True)
    (venv_dir / "sub" / "f.txt").write_text("x")
    venv_manager.remove_venv(venv_dir)
    assert not venv_dir.exists()


def test_create_venv_can_clear_a_stale_directory(monkeypatch, tmp_path):
    calls = _capture(monkeypatch)
    venv_dir = tmp_path / "b1"
    venv_manager.create_venv("/usr/bin/python3.12", venv_dir, clear=True)
    assert calls == [["/usr/bin/python3.12", "-m", "venv", "--clear", str(venv_dir)]]


def _fake_venv(venv_dir: Path, home: Path) -> None:
    interpreter = venv_manager.venv_python(venv_dir)
    interpreter.parent.mkdir(parents=True, exist_ok=True)
    interpreter.touch()
    (venv_dir / "pyvenv.cfg").write_text(
        f"home = {home}\ninclude-system-site-packages = false\nversion = 3.12.11\n")


def test_usable_venv_requires_an_interpreter_whose_base_still_exists(tmp_path):
    base = tmp_path / "cpython-3.12.11"
    base.mkdir()
    venv_dir = tmp_path / "b1"
    _fake_venv(venv_dir, base)
    assert venv_manager.is_usable_venv(venv_dir)


def test_usable_venv_is_false_when_the_base_interpreter_was_upgraded_away(tmp_path):
    # uv encodes the patch version in the interpreter path, so a Python upgrade orphans the venv.
    # On Windows the copied python.exe survives, so existence alone would wrongly say "usable".
    venv_dir = tmp_path / "b1"
    _fake_venv(venv_dir, tmp_path / "cpython-3.12.11-pruned")
    assert venv_manager.venv_python(venv_dir).exists()
    assert not venv_manager.is_usable_venv(venv_dir)


def test_usable_venv_is_false_for_a_missing_dir_a_flat_package_and_a_config_less_dir(tmp_path):
    assert not venv_manager.is_usable_venv(tmp_path / "absent")

    flat = tmp_path / "legacy"                       # pre-venv `pip install --target` leftovers
    flat.mkdir()
    (flat / "__init__.py").write_text("")
    assert not venv_manager.is_usable_venv(flat)

    half = tmp_path / "half"                         # interpreter, but interrupted before pyvenv.cfg
    venv_manager.venv_python(half).parent.mkdir(parents=True)
    venv_manager.venv_python(half).touch()
    assert not venv_manager.is_usable_venv(half)


def _write_dist_info(venv_dir: Path, dist_name: str, version: str) -> None:
    import os
    sp = venv_dir / ("Lib/site-packages" if os.name == "nt" else "lib/python3.12/site-packages")
    dist_info = sp / f"{dist_name.replace('-', '_')}-{version}.dist-info"
    dist_info.mkdir(parents=True)
    (dist_info / "METADATA").write_text(
        f"Metadata-Version: 2.1\nName: {dist_name}\nVersion: {version}\n")


def test_installed_version_reads_the_resolved_version_from_the_venv(tmp_path):
    venv = tmp_path / "b1"
    _write_dist_info(venv, "openrewrite-migrate-python", "0.9.3")
    assert venv_manager.installed_version(venv, "openrewrite-migrate-python") == "0.9.3"


def test_installed_version_matches_on_the_normalized_distribution_name(tmp_path):
    venv = tmp_path / "b1"
    _write_dist_info(venv, "openrewrite-migrate-python", "0.9.3")
    # bundle_dist arrives PEP 503 normalized; it must still match the canonical metadata Name.
    assert venv_manager.installed_version(venv, "openrewrite_migrate_python") == "0.9.3"


def test_installed_version_is_none_for_an_absent_dist_or_missing_venv(tmp_path):
    venv = tmp_path / "b1"
    _write_dist_info(venv, "openrewrite-migrate-python", "0.9.3")
    assert venv_manager.installed_version(venv, "some-other-package") is None
    assert venv_manager.installed_version(tmp_path / "nope", "openrewrite-migrate-python") is None


def test_purge_keeps_venvs_and_removes_pre_venv_target_debris(tmp_path):
    root = tmp_path / "pip"
    _fake_venv(root / "openrewrite_migrate_python", tmp_path / "base")   # a per-bundle venv — kept

    # pre-venv `pip install --target` residue: flat packages, dist-info, flattened deps, loose files
    flat = root / "openrewrite_code_quality_python"; flat.mkdir(parents=True)
    (flat / "__init__.py").write_text("")
    (root / "openrewrite_migrate_python-0.8.0.dist-info").mkdir()
    dep = root / "some_dep"; dep.mkdir(); (dep / "__init__.py").write_text("")
    (root / "loose_module.py").write_text("")
    (root / "bin").mkdir(); (root / "bin" / "tool").write_text("")
    (root / "__pycache__").mkdir()

    removed = venv_manager.purge_non_venv_entries(root)

    assert (root / "openrewrite_migrate_python" / "pyvenv.cfg").exists()      # venv untouched
    assert set(removed) == {
        "openrewrite_code_quality_python", "openrewrite_migrate_python-0.8.0.dist-info",
        "some_dep", "loose_module.py", "bin", "__pycache__",
    }
    assert [e.name for e in root.iterdir()] == ["openrewrite_migrate_python"]  # only the venv remains


def test_purge_keeps_an_orphaned_venv(tmp_path):
    # An orphaned venv (base pruned by a Python upgrade) is still a venv; fix 2 rebuilds it on
    # install. The sweep must not delete it just because it isn't currently usable.
    root = tmp_path / "pip"
    _fake_venv(root / "pkg", tmp_path / "pruned-base")
    assert not venv_manager.is_usable_venv(root / "pkg")
    assert venv_manager.purge_non_venv_entries(root) == []
    assert (root / "pkg" / "pyvenv.cfg").exists()


def test_purge_is_a_noop_on_a_missing_root_and_is_idempotent(tmp_path):
    assert venv_manager.purge_non_venv_entries(tmp_path / "absent") == []

    root = tmp_path / "pip"
    _fake_venv(root / "pkg", tmp_path / "base")
    (root / "junk").mkdir()
    assert venv_manager.purge_non_venv_entries(root) == ["junk"]
    assert venv_manager.purge_non_venv_entries(root) == []   # nothing left but the venv
