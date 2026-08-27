"""Per-bundle virtual environment lifecycle.

Each recipe bundle gets its own venv so its dependency graph is fully isolated.
Creation uses the stdlib ``venv`` module of a caller-supplied, rewrite-capable
interpreter. Uninstall is directory removal — one bundle owns one venv, so
there is nothing to pip-uninstall.
"""
import os
import shutil
import subprocess
from importlib import metadata
from pathlib import Path
from typing import Optional

from rewrite.discovery import _normalize_package_name


def venv_python(venv_dir: Path) -> Path:
    if os.name == "nt":
        return venv_dir / "Scripts" / "python.exe"
    return venv_dir / "bin" / "python"


def _site_packages(venv_dir: Path) -> Optional[Path]:
    if os.name == "nt":
        sp = venv_dir / "Lib" / "site-packages"
        return sp if sp.exists() else None
    return next(venv_dir.glob("lib/python*/site-packages"), None)


def installed_version(venv_dir: Path, dist: str) -> Optional[str]:
    """The resolved version of ``dist`` installed in the venv, or None.

    Read off disk, so the recipe is never imported.
    """
    site_packages = _site_packages(venv_dir)
    if site_packages is None:
        return None
    target = _normalize_package_name(dist)
    for distribution in metadata.distributions(path=[str(site_packages)]):
        name = distribution.metadata["Name"]
        if name and _normalize_package_name(name) == target:
            return distribution.version
    return None


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
    config = venv_dir / "pyvenv.cfg"
    if not config.exists():
        return False  # interrupted before configuration; treat as unbuilt
    for line in config.read_text().splitlines():
        key, separator, value = line.partition("=")
        if separator and key.strip() == "home":
            return Path(value.strip()).exists()
    return False


def install_into_venv(venv_dir: Path, spec: str, force: bool = False) -> None:
    """Install/upgrade ``spec`` (a PEP 440 requirement or a local path) into the venv.

    ``force`` re-copies a mutable local source whose version is unchanged.
    """
    cmd = [str(venv_python(venv_dir)), "-m", "pip", "install", "--upgrade"]
    if force:
        cmd.append("--force-reinstall")
    cmd.append(spec)
    _run(cmd)


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
