"""Facade orchestration: one isolated child per bundle, keyed by distribution name.

Ties ``venv_manager`` and ``child_connection`` together. Spawn-on-demand: ``install`` creates the
bundle's venv, installs the package, spawns its child, caches the child's (root-only) marketplace,
and records recipe ownership first-wins. ``marketplace`` serves the merged cache; recipe execution
is routed to the owning child. ``venv_ops`` and ``spawn`` are injectable for testing.
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
        # One venv per distribution under the shared root (reusing --recipe-install-dir), like the
        # C# server's <root>/<id> layout. No version dimension: pip upgrades a venv in place, so
        # (unlike C#'s immutable published assemblies + flaky ALC reload) there's no need for
        # per-version sibling directories. bundle_dist is already PEP 503 normalized (lowercase +
        # `_`) — the canonical distribution identity, and inherently filesystem-safe.
        return self._venvs_root / bundle_dist

    def _ensure_child(self, bundle_dist: str):
        child = self._children.get(bundle_dist)
        if child is None:
            # The venvs root is on the facade's PYTHONPATH (the Java host puts --recipe-install-dir
            # there); keep it off the child's, so a bundle imports only from its own venv.
            cmd = child_command(self._venv_dir(bundle_dist), bundle_dist,
                                attribution_name=self._attribution.get(bundle_dist))
            # Bind the bundle into the upstream: the facade keeps one ref table per child
            # connection, so it must know which child is calling back.
            child = self._spawn(cmd,
                                upstream=lambda m, p, b=bundle_dist: self._upstream(m, p, b),
                                exclude_paths=(str(self._venvs_root),))
            self._children[bundle_dist] = child
            # The recipe runs here, not on the facade, so the host's data-table store must be
            # installed on the child. SetDataTableStore often arrives before a bundle's child exists
            # (lazy spawn), so replay the cached config onto each child as it comes up.
            if self._data_table_store is not None:
                child.request("SetDataTableStore", self._data_table_store)
        return child

    def set_data_table_store(self, params: dict) -> None:
        """Cache the host's data-table store config and apply it to every child — live ones now, and
        ones spawned later at spawn time (see _ensure_child). Recipes emit rows from the children."""
        self._data_table_store = params
        for child in self._children.values():
            child.request("SetDataTableStore", params)

    def broadcast_evict(self, params: dict) -> None:
        """Fan a host Evict out to every spawned child so each rolls its own ref map back to the
        per-file checkpoint (in lockstep with the host's send-side rollback). A child that never
        fetched this file no-ops. Only live children matter — one not yet spawned holds no state."""
        for child in self._children.values():
            child.request("Evict", params)

    def install(self, bundle_dist: str, spec: str, force: bool = False, attribution_name=None):
        """Create/reuse the bundle's venv, install ``spec``, spawn its child, cache its recipes.

        ``force`` re-copies a mutable local source (see ``venv_manager.install_into_venv``).
        ``attribution_name`` labels the recipes with the identity the host keys the bundle by (a
        local install's supplied path); by default they carry the distribution's own name.
        """
        bundle_dist = _normalize_package_name(bundle_dist)
        self._attribution[bundle_dist] = attribution_name   # None for a registry spec
        venv_dir = self._venv_dir(bundle_dist)
        # Rebuild anything that isn't a venv with a live base interpreter: a half-created venv, a
        # leftover flat `pip install --target` package directory of the same name (from before
        # per-bundle venvs), or a venv orphaned by a Python upgrade. Testing the directory — or even
        # the interpreter file — would call all three usable, then fail on exec or on import.
        if not self._venv_ops.is_usable_venv(venv_dir):
            # Any child still bound to the old venv is about to lose it; reap it so the next
            # request spawns against the rebuilt one.
            stale = self._children.pop(bundle_dist, None)
            if stale is not None:
                stale.close()
            self._venv_ops.create_venv(self._python, venv_dir, clear=venv_dir.exists())
        # pip --upgrade in the venv resolves in place: a version-less install upgrades to the latest
        # (fixes #8180); an exact pin already present is a near no-op.
        self._venv_ops.install_into_venv(venv_dir, spec, force=force)
        # The install layer just ran pip, so it — not the child — is the authority on what version
        # landed. Report that resolved version rather than echoing the requested (maybe null) spec.
        self._versions[bundle_dist] = self._venv_ops.installed_version(venv_dir, bundle_dist)
        rows = self._ensure_child(bundle_dist).request("GetMarketplace", {})
        self._descriptors[bundle_dist] = rows
        for row in rows:
            self._owner.setdefault(row["descriptor"]["name"], bundle_dist)  # first-wins
        return rows

    def marketplace(self):
        """Merged descriptor cache across all bundles (first-wins on duplicate names)."""
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
        """The bundle that owns ``recipe_name``, or None."""
        return self._owner.get(recipe_name)

    def resolved_version(self, bundle_dist: str):
        """The version pip resolved for ``bundle_dist`` at install, or None."""
        return self._versions.get(_normalize_package_name(bundle_dist))

    def request(self, bundle_dist: str, method: str, params: dict):
        """Route an operation to the given bundle's child."""
        return self._ensure_child(_normalize_package_name(bundle_dist)).request(method, params)

    def uninstall(self, bundle_dist: str) -> None:
        """Reap the bundle's child, drop its cache/ownership, and remove its venv."""
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
        """Reap all children (session end)."""
        for child in self._children.values():
            child.close()
        self._children.clear()
