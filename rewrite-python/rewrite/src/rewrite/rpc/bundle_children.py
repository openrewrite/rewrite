"""Facade orchestration: one isolated child per bundle.

Installed bundles are keyed by distribution name, attached ones by the path of the venv their caller
built. Recipe ownership is first-wins across installed bundles, and an attached bundle overrides it.
"""
import os
from pathlib import Path

from rewrite.discovery import _normalize_package_name
from rewrite.rpc import venv_manager
from rewrite.rpc.child_connection import ChildConnection, child_command


class BundleChildren:
    def __init__(self, python_executable, venvs_root, upstream, *, spawn=None, venv_ops=None,
                 on_child_replaced=None):
        self._python = python_executable
        self._venvs_root = Path(venvs_root)
        self._upstream = upstream
        # A fresh child starts with empty ref maps, so whatever mirrors the old one has to go.
        self._on_child_replaced = on_child_replaced or (lambda bundle_dist: None)
        self._spawn = spawn or ChildConnection.spawn
        self._venv_ops = venv_ops or venv_manager
        self._children = {}     # bundle_dist -> child connection
        self._descriptors = {}  # bundle_dist -> list[marketplace row]
        self._owner = {}        # recipe name -> bundle_dist (first-wins)
        self._versions = {}     # bundle_dist -> resolved version (what pip actually installed)
        self._attribution = {}  # bundle_dist -> attribution name (a local install's supplied path)
        self._attached = {}     # attached venv path -> its normalized distribution name
        self._data_table_store = None  # cached SetDataTableStore params, broadcast to every child

    @staticmethod
    def _key(bundle: str) -> str:
        return os.path.normpath(bundle) if os.path.isabs(bundle) else _normalize_package_name(bundle)

    def _venv_dir(self, bundle_dist: str) -> Path:
        return Path(bundle_dist) if bundle_dist in self._attached else self._venvs_root / bundle_dist

    def _ensure_child(self, bundle_dist: str):
        child = self._children.get(bundle_dist)
        if child is None:
            cmd = child_command(self._venv_dir(bundle_dist), self._attached.get(bundle_dist, bundle_dist),
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

    def broadcast_reset(self, params: dict) -> None:
        for child in self._children.values():
            child.request("Reset", params)

    def install(self, bundle_dist: str, spec: str, force: bool = False, attribution_name=None):
        """Create/reuse the bundle's venv, install ``spec``, spawn its child, cache its recipes.

        ``attribution_name`` labels the recipes with the identity the host keys the bundle by (a
        local install's supplied path); by default they carry the distribution's own name.
        """
        bundle_dist = _normalize_package_name(bundle_dist)
        if attribution_name:
            self._forget_attached(attribution_name)
        self._attribution[bundle_dist] = attribution_name   # None for a registry spec
        venv_dir = self._venv_dir(bundle_dist)
        if not self._venv_ops.is_usable_venv(venv_dir):
            stale = self._children.pop(bundle_dist, None)
            if stale is not None:
                stale.close()
            self._on_child_replaced(bundle_dist)
            self._venv_ops.create_venv(self._python, venv_dir, clear=venv_dir.exists())
        self._venv_ops.install_into_venv(venv_dir, spec, force=force)
        return self._load(bundle_dist, venv_dir)

    def attach(self, bundle_dist: str, venv: str, attribution_name: str):
        """Spawn the bundle's child on ``venv``, a venv the caller built and keeps current.

        Every attach starts a fresh child, because the caller may have rebuilt the venv in place. An
        earlier venv attached under the same ``attribution_name`` is let go.
        """
        if not os.path.isabs(venv) or not self._venv_ops.is_usable_venv(Path(venv)):
            raise ValueError(f"'{venv}' is not a usable venv")
        key = self._key(venv)
        bundle_dist = _normalize_package_name(bundle_dist)
        if self._venv_ops.installed_version(Path(key), bundle_dist) is None:
            raise ValueError(f"'{bundle_dist}' is not installed in '{venv}'")
        self._forget_attached(attribution_name)
        self._forget(key)
        self._attached[key] = bundle_dist
        self._attribution[key] = attribution_name
        return self._load(key, Path(key))

    def _forget_attached(self, attribution_name: str) -> None:
        for key in [k for k in self._attached if self._attribution.get(k) == attribution_name]:
            self._forget(key)

    def _load(self, key: str, venv_dir: Path):
        try:
            self._versions[key] = self._venv_ops.installed_version(venv_dir, self._attached.get(key, key))
            self._descriptors[key] = self._ensure_child(key).request("GetMarketplace", {})
        except BaseException:
            self._forget(key)
            raise
        self._claim_owners()
        return self._descriptors[key]

    def _claim_owners(self) -> None:
        self._owner = {}
        for key, rows in self._descriptors.items():
            for row in rows:
                name = row["descriptor"]["name"]
                if key in self._attached:
                    self._owner[name] = key
                else:
                    self._owner.setdefault(name, key)

    def _forget(self, key: str) -> None:
        child = self._children.pop(key, None)
        if child is not None:
            child.close()
            self._on_child_replaced(key)
        self._descriptors.pop(key, None)
        self._versions.pop(key, None)
        self._attribution.pop(key, None)
        self._attached.pop(key, None)
        self._claim_owners()

    def marketplace(self):
        """Installed bundles list a recipe once, first-wins. An attached bundle's rows are always
        listed, so the host-side reader keyed by its source path finds them."""
        merged, seen = [], set()
        for key, rows in self._descriptors.items():
            for row in rows:
                name = row["descriptor"]["name"]
                identity = (name, row.get("packageName")) if key in self._attached else name
                if identity in seen:
                    continue
                seen.add(identity)
                merged.append(row)
        return merged

    def owner(self, recipe_name: str):
        return self._owner.get(recipe_name)

    def resolved_version(self, bundle: str):
        return self._versions.get(self._key(bundle))

    def request(self, bundle: str, method: str, params: dict):
        key = self._key(bundle)
        if key not in self._descriptors:
            raise ValueError(f"No bundle '{bundle}' is installed")
        return self._ensure_child(key).request(method, params)

    def uninstall(self, bundle: str) -> None:
        """Drop the bundle and its child. An attached venv belongs to its caller, so it stays on disk."""
        key = self._key(bundle)
        attached = key in self._attached
        venv_dir = self._venv_dir(key)
        self._forget(key)
        if not attached:
            self._venv_ops.remove_venv(venv_dir)

    def shutdown(self) -> None:
        for child in self._children.values():
            child.close()
        self._children.clear()
