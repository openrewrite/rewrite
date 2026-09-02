"""The recipe bundles installed into this server, each isolated by its own venv and module table.

A bundle gets a venv so pip resolves its dependencies on their own, and a :class:`Bundle` overlay so
those dependencies stay its own at run time. Both live in this process: recipes visit the tree the
server already holds, so nothing has to be serialized to reach them.

``venv_ops`` and ``activate`` are injectable for testing.
"""
from pathlib import Path

from rewrite.discovery import _normalize_package_name
from rewrite.rpc import venv_manager
from rewrite.rpc.bundle_overlay import Bundle


class Bundles:
    def __init__(self, python_executable, venvs_root, *, activate, venv_ops=None):
        self._python = python_executable
        self._venvs_root = Path(venvs_root)
        self._venv_ops = venv_ops or venv_manager
        self._activate = activate
        self._overlays = {}     # bundle_dist -> Bundle
        self._descriptors = {}  # bundle_dist -> list[marketplace row]
        self._owner = {}        # recipe name -> bundle_dist (first-wins)
        self._versions = {}     # bundle_dist -> resolved version (what pip actually installed)

    def _venv_dir(self, bundle_dist: str) -> Path:
        return self._venvs_root / bundle_dist

    def install(self, bundle_dist: str, spec: str, force: bool = False, attribution_name=None):
        """Create/reuse the bundle's venv, install ``spec``, activate its recipes, cache them.

        ``attribution_name`` labels the recipes with the identity the host keys the bundle by (a
        local install's supplied path); by default they carry the distribution's own name.
        """
        bundle_dist = _normalize_package_name(bundle_dist)
        venv_dir = self._venv_dir(bundle_dist)
        if not self._venv_ops.is_usable_venv(venv_dir):
            self._venv_ops.create_venv(self._python, venv_dir, clear=venv_dir.exists())
        self._venv_ops.install_into_venv(venv_dir, spec, force=force)
        self._versions[bundle_dist] = self._venv_ops.installed_version(venv_dir, bundle_dist)

        overlay = Bundle(bundle_dist, self._venv_ops.site_packages(venv_dir))
        self._overlays[bundle_dist] = overlay
        rows = self._activate(overlay, bundle_dist, attribution_name)
        self._descriptors[bundle_dist] = rows
        for row in rows:
            self._owner.setdefault(row["descriptor"]["name"], bundle_dist)  # first-wins
        return rows

    def overlay(self, bundle_dist):
        """The module table to run ``bundle_dist``'s code under, or None for an unknown bundle."""
        if bundle_dist is None:
            return None
        return self._overlays.get(_normalize_package_name(bundle_dist))

    def marketplace(self):
        merged, seen = [], set()
        for rows in self._descriptors.values():
            for row in rows:
                name = row["descriptor"]["name"]
                if name in seen:
                    continue
                seen.add(name)
                merged.append(row)
        return merged

    def owner(self, recipe_name: str):
        return self._owner.get(recipe_name)

    def resolved_version(self, bundle_dist: str):
        return self._versions.get(_normalize_package_name(bundle_dist))

    def uninstall(self, bundle_dist: str) -> None:
        bundle_dist = _normalize_package_name(bundle_dist)
        self._overlays.pop(bundle_dist, None)  # its modules go with it, unlike an import
        self._descriptors.pop(bundle_dist, None)
        self._versions.pop(bundle_dist, None)
        self._owner = {name: b for name, b in self._owner.items() if b != bundle_dist}
        self._venv_ops.remove_venv(self._venv_dir(bundle_dist))
