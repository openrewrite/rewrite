"""Facade request handling: route RPC operations across per-bundle children.

In facade mode (``--recipe-install-dir`` set, not a child) the server holds no recipes itself: each
bundle gets its own venv and child, and recipe execution routes to the owning child.

``Print``/``GetObject`` are not routed — the facade owns the working tree and answers those itself.
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
            # The CLI pins the bundle to the resolved version, so later resolves arrive exact.
            return {"recipesInstalled": len(rows), "version": self._children.resolved_version(package)}
        raise ValueError(f"Invalid recipes parameter: {recipes!r}")

    def _install_local(self, local_path: str) -> dict:
        # A local install arrives as a path, but the venv and child are keyed by distribution name.
        dist = distribution_name_from_source(Path(local_path))
        if not dist:
            raise ValueError(f"Could not determine the distribution name for local path '{local_path}'")
        # Discovery filters on the distribution name; attribution is the supplied path, which is
        # the identity the host keys a local bundle by.
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
        # A precondition may name one of this child's own visitors, and appears in neither
        # editVisitor nor recipeList — unregistered, the host's Visit for it would find no owner.
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
        # The facade answers Java's later Print/GetObject from its own tree, so an edit left in the
        # child is lost — silently, since the run still succeeds and reports no changes.
        if self._hub_pull is not None and tree_id is not None and result.get("modified"):
            self._hub_pull(self._children, bundle, tree_id, params.get("sourceFileType"))
        return result

    def batch_visit(self, params: dict) -> dict:
        """Route a BatchVisit's visitors to their owning children, preserving sequence.

        The host batches a tree's visitors across every recipe in a run cycle, so one BatchVisit can
        span bundles. Each same-owner run is served the facade's current tree over that child's ref
        table, and its edit is pulled back before the next run starts — so the second bundle sees the
        first one's result rather than the original."""
        groups = []  # [(owner, [visitor, ...]), ...]
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
            # Pull this bundle's edit in so the next bundle starts from it.
            if (self._hub_pull is not None and tree_id is not None and
                    any(r.get("modified") for r in sub_results)):
                self._hub_pull(self._children, owner, tree_id, source_file_type)
        return {"results": results}

    def evict(self, params: dict) -> bool:
        # Each child rolls back its own ref map, keeping ref numbering aligned with the host
        # across files.
        self._children.broadcast_evict(params)
        return True

    def set_data_table_store(self, params: dict) -> bool:
        # Recipes emit rows from the children, so the store is broadcast and cached for children
        # spawned later.
        self._children.set_data_table_store(params)
        return True

    def generate(self, params: dict) -> dict:
        recipe_id = params.get("id")
        bundle = self._bundle_by_recipe_id.get(recipe_id)
        if bundle is None:
            raise ValueError(f"No child owns prepared recipe '{recipe_id}'")
        return self._children.request(bundle, "Generate", params)
