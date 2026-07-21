"""Facade request handling: route RPC operations across per-bundle children.

In facade mode (``--recipe-install-dir`` set, not a child), the server holds no recipes itself. It
installs each bundle into its own venv+child (``BundleChildren``), serves ``GetMarketplace`` from
the merged cache, and routes recipe execution to the owning child — tracking which child produced
each prepared recipe's visitors so later ``Visit``/``Generate`` calls route to the right child.

Scope note: precondition routing remains a follow-up to validate against a real CLI run.
"""
from pathlib import Path

from rewrite.discovery import distribution_name_from_source


class Facade:
    def __init__(self, children, hub_pull=None):
        self._children = children
        # (children, bundle, tree_id, source_file_type) -> pull that child's edit into the facade's tree
        self._hub_pull = hub_pull
        self._bundle_by_visitor = {}    # visitor name (edit:/scan:) -> bundle
        self._bundle_by_recipe_id = {}  # prepared recipe id -> bundle
        self._bundle_by_tree = {}       # visited tree id -> bundle that holds the modified tree
        self._active_bundle = None      # bundle of the most recent visit (fallback owner during a run)

    def install_recipes(self, params: dict) -> dict:
        recipes = params.get("recipes")
        if isinstance(recipes, str):
            return self._install_local(recipes)
        if isinstance(recipes, dict):
            package = recipes.get("packageName")
            version = recipes.get("version")
            if not package:
                raise ValueError("Package name is required")
            if version:
                spec = f"{package}{version}" if version[0] in "=<>!~" else f"{package}=={version}"
            else:
                spec = package
            rows = self._children.install(package, spec)
            # Report what pip resolved, not the requested spec (which is null for a version-less
            # install). The CLI pins the bundle to this so later resolves arrive with an exact spec.
            return {"recipesInstalled": len(rows), "version": self._children.resolved_version(package)}
        raise ValueError(f"Invalid recipes parameter: {recipes!r}")

    def _install_local(self, local_path: str) -> dict:
        # A local install arrives as a path but the venv/child are keyed by distribution name, so
        # resolve the name from the source before installing. force=True re-copies the mutable
        # source even at an unchanged version.
        dist = distribution_name_from_source(Path(local_path))
        if not dist:
            raise ValueError(f"Could not determine the distribution name for local path '{local_path}'")
        # Filter discovery by the resolved distribution name, but attribute the recipes to the
        # supplied path — the identity the host keys the local bundle by.
        rows = self._children.install(dist, local_path, force=True, attribution_name=local_path)
        return {"recipesInstalled": len(rows), "version": self._children.resolved_version(dist)}

    def get_marketplace(self, params: dict):
        return self._children.marketplace()

    def prepare_recipe(self, params: dict) -> dict:
        recipe_name = params.get("id")
        bundle = self._children.owner(recipe_name)
        if bundle is None:
            raise ValueError(f"No bundle owns recipe '{recipe_name}'")
        response = self._children.request(bundle, "PrepareRecipe", params)
        self._register(response, bundle)
        return response

    def _register(self, response: dict, bundle) -> None:
        rid = response.get("id")
        if rid is not None:
            self._bundle_by_recipe_id[rid] = bundle
        for key in ("editVisitor", "scanVisitor"):
            visitor = response.get(key)
            if visitor:
                self._bundle_by_visitor[visitor] = bundle
        # A precondition can name a Python visitor (`edit:<prepared id>`) that the host calls back
        # like any other. It was prepared inside this child but appears in neither editVisitor nor
        # recipeList, so without this the host's Visit for it finds no owner.
        for key in ("editPreconditions", "scanPreconditions"):
            for precondition in (response.get(key) or []):
                visitor = precondition.get("visitorName")
                if visitor:
                    self._bundle_by_visitor[visitor] = bundle
        for child in (response.get("recipeList") or []):
            self._register(child, bundle)  # same bundle: in-boundary composition

    def visit(self, params: dict) -> dict:
        visitor = params.get("visitor")
        bundle = self._bundle_by_visitor.get(visitor)
        if bundle is None:
            raise ValueError(f"No child owns visitor '{visitor}'")
        result = self._children.request(bundle, "Visit", params)
        tree_id = params.get("treeId")
        # The edit lives in the child; the facade answers Java's later Print/GetObject from its own
        # tree. Pull it back here or that answer is the unmodified input and the edit is lost --
        # silently, since the run still succeeds and simply reports no changes. `batch_visit` does
        # the same for each of its runs; a single-recipe run reaches this path instead.
        if self._hub_pull is not None and tree_id is not None and result.get("modified"):
            self._hub_pull(self._children, bundle, tree_id, params.get("sourceFileType"))
        if tree_id is not None:
            self._bundle_by_tree[tree_id] = bundle
        self._active_bundle = bundle
        return result

    def batch_visit(self, params: dict) -> dict:
        """Route a BatchVisit's visitors to their owning children, preserving sequence.

        The Java scheduler (RecipeRunCycle) batches a tree's visitors across every recipe in the
        run cycle, so one BatchVisit can span bundles. Split it into maximal consecutive same-owner
        runs and dispatch each as a BatchVisit to that child, concatenating the per-visitor results
        in order. A single-bundle composite collapses to one run — the whole batch to one child.

        Each run is a "sub-BatchVisit" against the facade's hub: the child is served the facade's
        current tree over that child's ref table, runs its visitors, and its edit is pulled back as a
        diff and applied to the facade's tree before the next bundle starts. So bundle B sees bundle
        A's result rather than the original, and the accumulated tree stays with the facade."""
        groups = []  # maximal consecutive same-owner runs: [(owner, [visitor, ...]), ...]
        for visitor in params.get("visitors", []):
            name = visitor.get("visitor")
            owner = self._bundle_by_visitor.get(name)
            if owner is None:
                raise ValueError(f"No child owns visitor '{name}'")
            if groups and groups[-1][0] == owner:
                groups[-1][1].append(visitor)
            else:
                groups.append((owner, [visitor]))

        tree_id = params.get("treeId")
        source_file_type = params.get("sourceFileType")
        results = []
        for owner, sub_visitors in groups:
            response = self._children.request(owner, "BatchVisit", {**params, "visitors": sub_visitors})
            sub_results = response.get("results", [])
            results.extend(sub_results)
            self._active_bundle = owner
            # Pull this bundle's edit into the facade's tree so the next bundle starts from it.
            # Only when it actually changed something -- an unmodified run has nothing to contribute.
            if (self._hub_pull is not None and tree_id is not None and
                    any(r.get("modified") for r in sub_results)):
                self._hub_pull(self._children, owner, tree_id, source_file_type)
        if tree_id is not None and self._active_bundle is not None:
            self._bundle_by_tree[tree_id] = self._active_bundle
        return {"results": results}

    def tree_owner(self, params: dict):
        """The child that should service a Print/GetObject, or None to fall back to the facade-local
        handler. Print sends 'treeId', GetObject sends 'id'. A modifying visit re-keys the tree under
        a new id (and Java also fetches the execution context and cursors by id), so an exact-id miss
        during a run still belongs to the child that just visited — the facade holds no trees then.
        Only before any visit (build time) is there no active bundle, leaving those objects local."""
        tree_id = params.get("treeId") or params.get("id")
        return self._bundle_by_tree.get(tree_id) or self._active_bundle

    def route_to_child(self, bundle, method: str, params: dict) -> dict:
        return self._children.request(bundle, method, params)

    def evict(self, params: dict) -> bool:
        # The facade runs no recipes and holds no source trees; the children do. Fan the host's
        # per-file Evict out to them so each rolls back its own ref map (bounding child memory and
        # keeping ref numbering aligned with the host across files).
        self._children.broadcast_evict(params)
        return True

    def set_data_table_store(self, params: dict) -> bool:
        # The facade runs no recipes, so it keeps no store of its own; it broadcasts the config to
        # the children (where recipes emit data-table rows) and caches it for children spawned later.
        self._children.set_data_table_store(params)
        return True

    def generate(self, params: dict) -> dict:
        recipe_id = params.get("id")
        bundle = self._bundle_by_recipe_id.get(recipe_id)
        if bundle is None:
            raise ValueError(f"No child owns prepared recipe '{recipe_id}'")
        return self._children.request(bundle, "Generate", params)
