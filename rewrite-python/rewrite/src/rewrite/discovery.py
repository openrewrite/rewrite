# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Entry point discovery for recipe packages."""

from __future__ import annotations

import inspect
from dataclasses import dataclass, field
from importlib.metadata import entry_points
from typing import Dict, List, Optional, Set, Tuple, Type

from rewrite.category import CategoryDescriptor
from rewrite.decorators import get_recipe_category
from rewrite.marketplace import RecipeMarketplace
from rewrite.recipe import Recipe


@dataclass
class RecipeAttribution:
    """Tracks which distribution's entry point activated which recipes.

    Used to answer "which recipes came from package X?" so callers can scope
    a marketplace response to a specific distribution instead of returning
    the whole singleton. Distribution names are PEP 503 normalized (hyphen,
    underscore, and case folded together) on both write and read, so callers
    can use any common spelling.
    """

    _by_package: Dict[str, Set[str]] = field(default_factory=dict)

    def record(self, distribution_name: str, recipe_names: Set[str]) -> None:
        """Attribute ``recipe_names`` to ``distribution_name``.

        No-op when ``recipe_names`` is empty; multiple calls for the same
        distribution accumulate.
        """
        if not recipe_names:
            return
        key = _normalize_package_name(distribution_name)
        self._by_package.setdefault(key, set()).update(recipe_names)

    def recipes_for(self, distribution_name: str) -> Set[str]:
        """Return the (possibly empty) set of recipe names attributed to a
        distribution. The returned set is a snapshot — mutating it does not
        change the attribution.
        """
        return set(self._by_package.get(_normalize_package_name(distribution_name), ()))

    def clear(self) -> None:
        self._by_package.clear()


def discover_recipes(
    marketplace: Optional[RecipeMarketplace] = None,
    attribution: Optional[RecipeAttribution] = None,
) -> RecipeMarketplace:
    """
    Discover all recipes from installed packages via entry points.

    Looks for packages that declare entry points under
    [project.entry-points."openrewrite.recipes"] and calls their
    activate function to install recipes into the marketplace.

    Args:
        marketplace: Optional existing marketplace to install into; a new one
            is created when None.
        attribution: Optional sink that records which distribution activated
            which recipes. When supplied, each entry point's contribution is
            recorded against its distribution name. Recipes already in the
            marketplace before this call (e.g., deduped by an earlier entry
            point) are not re-recorded for the current entry point.

    Returns:
        The marketplace containing all discovered recipes.

    Example pyproject.toml entry point:
        [project.entry-points."openrewrite.recipes"]
        mypackage = "mypackage:activate"

    The activate function signature should be:
        def activate(marketplace: RecipeMarketplace) -> None:
            marketplace.install(MyRecipe, [Python, Cleanup])
    """
    if marketplace is None:
        marketplace = RecipeMarketplace()

    # Python 3.10+ uses select parameter via SelectableGroups
    eps = entry_points(group="openrewrite.recipes")

    for ep in eps:
        try:
            module = ep.load()
            if not hasattr(module, "activate") or not callable(module.activate):
                continue
            if attribution is None:
                module.activate(marketplace)
                continue
            dist_name = ep.dist.name if ep.dist is not None else None
            before = _recipe_name_set(marketplace)
            module.activate(marketplace)
            if dist_name:
                attribution.record(dist_name, _recipe_name_set(marketplace) - before)
        except Exception:
            # Log or handle the error - for now, skip failed activations
            pass

    return marketplace


def _recipe_name_set(marketplace: RecipeMarketplace) -> Set[str]:
    """Snapshot the set of recipe names currently in the marketplace."""
    return {r.name for r in marketplace.all_recipes()}


def _normalize_package_name(name: str) -> str:
    """Normalize a python distribution name for attribution lookup.

    PyPI/pip treats hyphens, underscores, and case as equivalent when
    resolving distribution identity.
    """
    return name.replace("-", "_").replace(".", "_").lower()


def discover_decorated_recipes_in_module(
    module,
) -> List[Tuple[Type[Recipe], List[CategoryDescriptor]]]:
    """
    Find all decorated Recipe subclasses in a module.

    This function inspects a module and finds all classes that:
    1. Are subclasses of Recipe
    2. Have been decorated with @recipe

    Args:
        module: A Python module to inspect

    Returns:
        A list of (recipe_class, category_path) tuples for each
        decorated Recipe found in the module.
    """
    results: List[Tuple[Type[Recipe], List[CategoryDescriptor]]] = []

    for _name, obj in inspect.getmembers(module, inspect.isclass):
        # Check if it's a Recipe subclass (but not Recipe itself)
        if issubclass(obj, Recipe) and obj is not Recipe:
            # Check if it has the @recipe decorator
            category = get_recipe_category(obj)
            if category is not None:
                results.append((obj, category))

    return results
