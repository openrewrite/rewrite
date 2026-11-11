"""Facade orchestration: bundles isolated from each other.

Installed bundles are keyed by distribution name, attached ones by the path of the venv their caller
built. An installed bundle is hosted in the facade's own process when it agrees on the version of
every distribution it shares with what this process has already imported. Every other bundle runs
behind its own child, spawned on demand. Recipe ownership is first-wins across installed bundles,
and an attached bundle overrides it.

``activate`` hosts a bundle in this process, and ``None`` gives every bundle a child. It is
injected rather than imported: the server runs as ``__main__`` under ``python -m rewrite.rpc.server``,
so importing it by name here binds a second module object with its own, empty marketplace.
``venv_ops`` and ``spawn`` are injectable for testing.
"""
import os
from pathlib import Path

from rewrite.discovery import _normalize_package_name
from rewrite.rpc import venv_manager
from rewrite.rpc.child_connection import ChildConnection, child_command


class BundleChildren:
    def __init__(self, python_executable, venvs_root, upstream, *, activate, spawn=None,
                 venv_ops=None, on_child_replaced=None):
        self._python = python_executable
        self._venvs_root = Path(venvs_root)
        self._upstream = upstream
        # A fresh child starts with empty ref maps, so whatever mirrors the old one has to go.
        self._on_child_replaced = on_child_replaced or (lambda bundle_dist: None)
        self._spawn = spawn or ChildConnection.spawn
        self._venv_ops = venv_ops or venv_manager
        self._activate = activate
        self._hosted = set()    # the bundles this process runs itself
        # bundle -> version of each distribution it brought into this process. An entry outlives the
        # bundle's hosting, since its modules stay imported.
        self._joined = {}
        self._shared = None     # names of the distributions this process's own path provides
        # A bundle is a distribution name or an attached venv's path.
        self._children = {}     # bundle -> child connection
        self._descriptors = {}  # bundle -> list[marketplace row]
        self._owner = {}        # recipe name -> bundle (see _claim_owners)
        self._versions = {}     # bundle -> resolved version
        self._attribution = {}  # bundle -> attribution name (a local install's supplied path)
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
        """Create/reuse the bundle's venv, install ``spec``, activate its recipes, cache them.

        ``attribution_name`` labels the recipes with the identity the host keys the bundle by (a
        local install's supplied path); by default they carry the distribution's own name.
        """
        bundle_dist = _normalize_package_name(bundle_dist)
        if attribution_name:
            self._forget_attached(attribution_name)
        if force or self._attribution.get(bundle_dist) != attribution_name:
            # A forced re-install may change files under modules already imported. Activation
            # attributes only the recipes new to the marketplace.
            self._hosted.discard(bundle_dist)
        self._attribution[bundle_dist] = attribution_name   # None for a registry spec
        venv_dir = self._venv_dir(bundle_dist)
        if not self._venv_ops.is_usable_venv(venv_dir):
            # its modules were imported from the venv being replaced
            self._hosted.discard(bundle_dist)
            stale = self._children.pop(bundle_dist, None)
            if stale is not None:
                stale.close()
            self._on_child_replaced(bundle_dist)
            self._venv_ops.create_venv(self._python, venv_dir, clear=venv_dir.exists())
        # Venvs built at different times can hold different versions both bundles' ranges allow.
        # Settling on the ones other bundles imported lets this bundle be hosted beside them.
        prefer = self._imported(excluding=bundle_dist)
        self._venv_ops.install_into_venv(venv_dir, spec, force=force, prefer=prefer)
        return self._load(bundle_dist, self._venv_ops.installed_version(venv_dir, bundle_dist))

    def attach(self, bundle_dist: str, venv: str, attribution_name: str):
        """Spawn the bundle's child on ``venv``, a venv the caller built and keeps current.

        Every attach starts a fresh child, because the caller may have rebuilt the venv in place. Any
        other bundle under the same ``attribution_name`` is let go.
        """
        if not os.path.isabs(venv) or not self._venv_ops.is_usable_venv(Path(venv)):
            raise ValueError(f"'{venv}' is not a usable venv")
        key = self._key(venv)
        bundle_dist = _normalize_package_name(bundle_dist)
        version = self._venv_ops.installed_version(Path(key), bundle_dist)
        if version is None:
            raise ValueError(f"'{bundle_dist}' is not installed in '{venv}'")
        for previous in {key, *(k for k, a in self._attribution.items() if a == attribution_name)}:
            self._forget(previous)
        self._attached[key] = bundle_dist
        self._attribution[key] = attribution_name
        try:
            return self._load(key, version)
        except BaseException:
            self._forget(key)
            raise

    def _forget_attached(self, attribution_name: str) -> None:
        for key in [k for k in self._attached if self._attribution.get(k) == attribution_name]:
            self._forget(key)

    def _load(self, key: str, version):
        self._versions[key] = version
        self._descriptors[key] = self._discover(key)
        self._claim_owners()
        return self._descriptors[key]

    def _discover(self, key: str):
        """Bring a bundle's recipes within reach and return its marketplace rows.

        A bundle is hosted here while every distribution it shares with what this process imported
        is at one version. Joining its venv to the path then equals installing it beside what is here.

        Imports are permanent, so a process hosts a bundle once, and only while the modules it
        imported are current. A hosted bundle stays hosted when more bundles arrive, since the host
        may already hold recipes prepared here. An attached venv may be rebuilt in place, so an
        attached bundle is never hosted.
        """
        venv_dir = self._venv_dir(key)
        if (self._activate is not None and key not in self._attached
                and self._venv_ops.built_by_running_interpreter(venv_dir)):
            versions = self._own_distributions(venv_dir)
            current = key in self._hosted and self._joined[key] == versions
            if current or (key not in self._joined and self._agrees(versions)):
                self._joined[key] = versions  # even an activation that fails may have imported part of it
                rows = self._activate(venv_dir, key, self._attribution.get(key))
                self._hosted.add(key)
                return rows
        self._hosted.discard(key)
        return self._ensure_child(key).request("GetMarketplace", {})

    def _own_distributions(self, venv_dir: Path) -> dict:
        """The venv's distributions this process would import from the venv rather than its own path."""
        if self._shared is None:
            self._shared = self._venv_ops.shared_distributions(exclude=(str(self._venvs_root),))
        return {name: version for name, version in self._venv_ops.installed_distributions(venv_dir).items()
                if name not in self._shared}

    def _imported(self, excluding=None) -> dict:
        return {name: version for key, versions in self._joined.items() if key != excluding
                for name, version in versions.items()}

    def _agrees(self, versions: dict) -> bool:
        imported = self._imported()
        return all(imported.get(name, version) == version for name, version in versions.items())

    def hosts(self, bundle: str) -> bool:
        return self._key(bundle) in self._hosted

    def hosting(self) -> bool:
        return bool(self._hosted)

    def hosts_every_bundle(self) -> bool:
        """True when every installed bundle is hosted, so no request needs routing."""
        return bool(self._hosted) and self._descriptors.keys() == self._hosted

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
        self._hosted.discard(key)  # _joined keeps it, since its modules stay here
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
