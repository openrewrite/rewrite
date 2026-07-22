"""Facade orchestration: one isolated child per bundle, keyed by distribution name.

Children are spawned on demand. Recipe ownership is first-wins across bundles. ``venv_ops`` and
``spawn`` are injectable for testing.
"""
from pathlib import Path

from rewrite.discovery import _normalize_package_name
from rewrite.rpc import venv_manager
from rewrite.rpc.child_connection import ChildConnection, child_command



class BundleChildren:
    def __init__(self, python_executable, venvs_root, upstream, *, spawn=None, venv_ops=None):
        self._python = python_executable
        self._venvs_root = Path(venvs_root)
        self._upstream = upstream
        self._spawn = spawn or ChildConnection.spawn
        self._venv_ops = venv_ops or venv_manager
        self._children = {}     # bundle_dist -> child connection
        self._descriptors = {}  # bundle_dist -> list[marketplace row]
        self._owner = {}        # recipe name -> bundle_dist (first-wins)
        self._versions = {}     # bundle_dist -> resolved version (what pip actually installed)
        self._attribution = {}  # bundle_dist -> attribution name (a local install's supplied path)
        self._data_table_store = None  # cached SetDataTableStore params, broadcast to every child

    def _venv_dir(self, bundle_dist: str) -> Path:
        return self._venvs_root / bundle_dist

    def _ensure_child(self, bundle_dist: str):
        child = self._children.get(bundle_dist)
        if child is None:
            cmd = child_command(self._venv_dir(bundle_dist), bundle_dist,
                                attribution_name=self._attribution.get(bundle_dist))
            child = self._spawn(cmd,
                                upstream=lambda m, p, b=bundle_dist: self._upstream(m, p, b),
                                exclude_paths=(str(self._venvs_root),))
            self._children[bundle_dist] = child
            if self._data_table_store is not None:
                child.request("SetDataTableStore", self._data_table_store)
        return child

    def set_data_table_store(self, params: dict) -> None:
        self._data_table_store = params
        for child in self._children.values():
            child.request("SetDataTableStore", params)

    def broadcast_evict(self, params: dict) -> None:
        for child in self._children.values():
            child.request("Evict", params)

    def install(self, bundle_dist: str, spec: str, force: bool = False, attribution_name=None):
        """Create/reuse the bundle's venv, install ``spec``, spawn its child, cache its recipes.

        ``attribution_name`` labels the recipes with the identity the host keys the bundle by (a
        local install's supplied path); by default they carry the distribution's own name.
        """
        bundle_dist = _normalize_package_name(bundle_dist)
        self._attribution[bundle_dist] = attribution_name   # None for a registry spec
        venv_dir = self._venv_dir(bundle_dist)
        if not self._venv_ops.is_usable_venv(venv_dir):
            stale = self._children.pop(bundle_dist, None)
            if stale is not None:
                stale.close()
            self._venv_ops.create_venv(self._python, venv_dir, clear=venv_dir.exists())
        self._venv_ops.install_into_venv(venv_dir, spec, force=force)
        self._versions[bundle_dist] = self._venv_ops.installed_version(venv_dir, bundle_dist)
        rows = self._ensure_child(bundle_dist).request("GetMarketplace", {})
        self._descriptors[bundle_dist] = rows
        for row in rows:
            self._owner.setdefault(row["descriptor"]["name"], bundle_dist)  # first-wins
        return rows

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

    def request(self, bundle_dist: str, method: str, params: dict):
        return self._ensure_child(_normalize_package_name(bundle_dist)).request(method, params)

    def uninstall(self, bundle_dist: str) -> None:
        bundle_dist = _normalize_package_name(bundle_dist)
        child = self._children.pop(bundle_dist, None)
        if child is not None:
            child.close()
        self._descriptors.pop(bundle_dist, None)
        self._versions.pop(bundle_dist, None)
        self._attribution.pop(bundle_dist, None)
        self._owner = {name: b for name, b in self._owner.items() if b != bundle_dist}
        self._venv_ops.remove_venv(self._venv_dir(bundle_dist))

    def shutdown(self) -> None:
        for child in self._children.values():
            child.close()
        self._children.clear()
