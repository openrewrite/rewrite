"""Per-bundle virtual environment lifecycle.

Each recipe bundle gets its own venv so its dependency graph is fully isolated.
Creation uses the stdlib ``venv`` module of a caller-supplied, rewrite-capable
interpreter. Uninstall is directory removal — one bundle owns one venv, so
there is nothing to pip-uninstall.
"""
import os
import shutil
import subprocess
import sys
import tempfile
from importlib import metadata
from pathlib import Path
from typing import Optional

from rewrite.discovery import _normalize_package_name


def venv_python(venv_dir: Path) -> Path:
    if os.name == "nt":
        return venv_dir / "Scripts" / "python.exe"
    return venv_dir / "bin" / "python"


def site_packages(venv_dir: Path) -> Optional[Path]:
    if os.name == "nt":
        sp = venv_dir / "Lib" / "site-packages"
        return sp if sp.exists() else None
    return next(venv_dir.glob("lib/python*/site-packages"), None)


def installed_version(venv_dir: Path, dist: str) -> Optional[str]:
    """The resolved version of ``dist`` installed in the venv, or None.

    Read off disk, so the recipe is never imported.
    """
    return installed_distributions(venv_dir).get(_normalize_package_name(dist))


def installed_distributions(venv_dir: Path) -> dict:
    """The version of each distribution installed in the venv, by normalized name, read off disk."""
    packages = site_packages(venv_dir)
    return {} if packages is None else _distributions([str(packages)])


def shared_distributions(exclude=()) -> set:
    """The normalized names of the distributions on this interpreter's path, leaving out every
    entry under one of ``exclude``."""
    return set(_distributions(path_without(exclude)))


def path_without(exclude) -> list:
    """``sys.path`` less every entry at or under one of ``exclude``."""
    roots = [os.path.join(path_key(root), "") for root in exclude]
    return [p for p in sys.path if not os.path.join(path_key(p), "").startswith(tuple(roots))]


def path_key(path: str) -> str:
    return os.path.normcase(os.path.abspath(path))


def _distributions(paths) -> dict:
    found = {}
    for distribution in metadata.distributions(path=paths):
        name = distribution.metadata["Name"]
        if name:
            found.setdefault(_normalize_package_name(name), distribution.version)
    return found


def create_venv(python_executable: str, venv_dir: Path, clear: bool = False) -> None:
    """Create a venv at ``venv_dir``; ``clear`` empties the directory first."""
    cmd = [python_executable, "-m", "venv"]
    if clear:
        cmd.append("--clear")
    cmd.append(str(venv_dir))
    _run(cmd)


def is_usable_venv(venv_dir: Path) -> bool:
    """True when ``venv_dir`` is a venv whose base interpreter still exists.

    A venv is not self-contained: ``pyvenv.cfg``'s ``home`` points at the base installation it
    borrows its stdlib from. uv encodes the patch version in that path
    (``.../cpython-3.12.11-...``), so upgrading or pruning the interpreter orphans every venv
    built on it. Checking only for the interpreter file would miss this on Windows, where ``venv``
    *copies* ``python.exe`` — it outlives its base and the venv still looks intact.
    """
    if not venv_python(venv_dir).exists():
        return False
    home = _pyvenv_cfg(venv_dir).get("home")  # absent when interrupted before configuration
    return home is not None and Path(home).exists()


def built_by_running_interpreter(venv_dir: Path) -> bool:
    """True when this process can import the venv's packages.

    A venv's ``site-packages`` carries bytecode and extension modules for the one interpreter
    version it was built against. An unrecorded version reads as no match.
    """
    config = _pyvenv_cfg(venv_dir)
    version = config.get("version", config.get("version_info"))
    return version is not None and version.split(".")[:2] == [str(p) for p in sys.version_info[:2]]


def _pyvenv_cfg(venv_dir: Path) -> dict:
    config = venv_dir / "pyvenv.cfg"
    if not config.exists():
        return {}
    entries = {}
    for line in config.read_text().splitlines():
        key, separator, value = line.partition("=")
        if separator:
            entries.setdefault(key.strip(), value.strip())
    return entries


def install_into_venv(venv_dir: Path, spec: str, force: bool = False, constraints: Optional[dict] = None) -> None:
    """Install/upgrade ``spec`` (a PEP 440 requirement or a local path) into the venv.

    ``force`` re-copies a mutable local source whose version is unchanged. ``constraints`` maps
    distributions to the versions pip must settle on wherever ``spec`` installs them, including one
    the venv already holds at another version.
    """
    cmd = [str(venv_python(venv_dir)), "-m", "pip", "install", "--upgrade"]
    if force:
        cmd.append("--force-reinstall")
    if not constraints:
        _run([*cmd, spec])
        return
    with tempfile.TemporaryDirectory() as scratch:
        pins = Path(scratch) / "constraints.txt"
        pins.write_text("".join(f"{name}=={version}\n" for name, version in sorted(constraints.items())))
        _run([*cmd, "--constraint", str(pins), spec])


def remove_venv(venv_dir: Path) -> None:
    shutil.rmtree(venv_dir, ignore_errors=True)


def purge_non_venv_entries(root: Path) -> list:
    if not root.exists():
        return []
    removed = []
    for entry in sorted(root.iterdir()):
        if entry.is_dir() and (entry / "pyvenv.cfg").exists():
            continue
        removed.append(entry.name)
        if entry.is_dir():
            shutil.rmtree(entry, ignore_errors=True)
        else:
            entry.unlink(missing_ok=True)
    return removed


def _run(cmd: list) -> None:
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"command failed: {' '.join(cmd)}\n{result.stderr}")
