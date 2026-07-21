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
import logging
import re
import sys
from importlib.metadata import entry_points
from pathlib import Path
from typing import Dict, List, NewType, Optional, Set, Tuple, Type

from rewrite.category import CategoryDescriptor
from rewrite.decorators import get_recipe_category
from rewrite.marketplace import RecipeMarketplace
from rewrite.recipe import Recipe

logger = logging.getLogger(__name__)


# A recipe's fully qualified name (e.g., ``org.openrewrite.python.RemovePass``).
# NewType is a static-only distinction — at runtime these are plain ``str`` —
# but it lets type checkers refuse a bare string where a recipe-name set is
# expected, which catches the kind of "did I just lookup the wrong key" bug
# that motivated this module's per-distribution attribution in the first place.
RecipeName = NewType("RecipeName", str)

# A python distribution name in its PEP 503 normalized form (hyphens,
# underscores, dots all folded to ``_`` and lowercased). Used as the
# attribution map key. Construct only via :func:`_normalize_package_name`.
NormalizedDistName = NewType("NormalizedDistName", str)


class RecipeAttribution:
    """Tracks which distribution's entry point activated which recipes.

    Used to answer "which recipes came from package X?" so callers can scope
    a marketplace response to a specific distribution instead of returning
    the whole singleton. Distribution names are PEP 503 normalized (hyphen,
    underscore, and case folded together) on both write and read, so callers
    can use any common spelling.
    """

    def __init__(self) -> None:
        self._by_package: Dict[NormalizedDistName, Set[RecipeName]] = {}
        # recipe name -> the distribution name as recorded (first attribution wins).
        # Kept un-normalized so a GetMarketplace row carries the package spelling the
        # host installed, which is what its bundle filter matches against.
        self._by_recipe: Dict[RecipeName, str] = {}

    def record(self, distribution_name: str, recipe_names: Set[RecipeName]) -> None:
        """Attribute ``recipe_names`` to ``distribution_name``.

        No-op when ``recipe_names`` is empty; multiple calls for the same
        distribution accumulate.
        """
        if not recipe_names:
            return
        key = _normalize_package_name(distribution_name)
        self._by_package.setdefault(key, set()).update(recipe_names)
        for recipe_name in recipe_names:
            self._by_recipe.setdefault(recipe_name, distribution_name)

    def recipes_for(self, distribution_name: str) -> Set[RecipeName]:
        """Return the (possibly empty) set of recipe names attributed to a
        distribution. The returned set is a snapshot — mutating it does not
        change the attribution.
        """
        return set(self._by_package.get(_normalize_package_name(distribution_name), ()))

    def package_for(self, recipe_name: RecipeName) -> Optional[str]:
        """Return the distribution that contributed ``recipe_name`` (the spelling
        it was recorded with), or None if the recipe wasn't attributed. Used to tag
        each GetMarketplace row with its origin so the host attributes it to the
        right bundle instead of the single requested one."""
        return self._by_recipe.get(recipe_name)


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

    for ep in entry_points(group="openrewrite.recipes"):
        _activate_entry_point(ep, marketplace, attribution)

    return marketplace


def _activate_entry_point(ep, marketplace: RecipeMarketplace,
                          attribution: Optional[RecipeAttribution],
                          attribution_name: Optional[str] = None) -> None:
    """Load and run one ``openrewrite.recipes`` entry point into ``marketplace``.

    An entry point may name the module (``pkg = pkg``), so ``load()`` yields a module whose
    ``activate`` we want, or the function itself (``pkg = pkg:activate`` — the form the engine and
    the recipe bundles declare), in which case ``load()`` has already yielded ``activate``. A failed
    activation contributes nothing but is logged, never silently swallowed — otherwise a bundle's
    recipes vanish with no explanation.

    ``attribution_name`` records the recipes under a caller-supplied identity (a local install's
    supplied path, which the host keys the bundle by) instead of the distribution's own name.
    """
    dist_name = ep.dist.name if ep.dist is not None else None
    try:
        loaded = ep.load()
        activate = getattr(loaded, "activate", loaded)
        if not callable(activate):
            return
        before = recipe_name_set(marketplace)
        activate(marketplace)
        recorded = attribution_name or dist_name
        if attribution is not None and recorded:
            attribution.record(recorded, recipe_name_set(marketplace) - before)
    except Exception:
        logger.warning("Failed to activate recipes from distribution %r", dist_name, exc_info=True)


def recipe_name_set(marketplace: RecipeMarketplace) -> Set[RecipeName]:
    """Snapshot the set of recipe names currently in the marketplace."""
    return {RecipeName(r.name) for r in marketplace.all_recipes()}


def _normalize_package_name(name: str) -> NormalizedDistName:
    """Normalize a python distribution name for attribution lookup.

    PyPI/pip treats hyphens, underscores, and case as equivalent when
    resolving distribution identity.
    """
    return NormalizedDistName(name.replace("-", "_").replace(".", "_").lower())


def discover_root_recipes(
    root_dist_name: str,
    marketplace: Optional[RecipeMarketplace] = None,
    attribution: Optional[RecipeAttribution] = None,
    attribution_name: Optional[str] = None,
) -> RecipeMarketplace:
    """Activate only the entry point whose owning distribution is ``root_dist_name``.

    ``entry_points()`` is environment-wide, so a bundle venv that transitively
    contains other recipe packages would otherwise surface them. Restricting
    activation to the root distribution keeps a bundle's marketplace to its own
    direct recipes; transitive recipe packages remain importable
    for in-boundary composition but are never listed or attributed here.

    ``attribution_name`` labels the discovered recipes with a caller-supplied identity rather than
    ``root_dist_name`` — a local install is filtered by its resolved distribution name but must be
    attributed to the source path the host supplied and keys the bundle by.
    """
    if marketplace is None:
        marketplace = RecipeMarketplace()

    root_key = _normalize_package_name(root_dist_name)
    for ep in entry_points(group="openrewrite.recipes"):
        dist_name = ep.dist.name if ep.dist is not None else None
        if dist_name is None or _normalize_package_name(dist_name) != root_key:
            continue
        _activate_entry_point(ep, marketplace, attribution, attribution_name)
    return marketplace


def distribution_name_from_source(local_path: Path) -> Optional[str]:
    """The distribution name declared by a local package source, or None.

    A local install (``pip install ./recipes``) arrives as a path, but the venv and its child are
    keyed by distribution name — so it must be resolved before install, from ``pyproject.toml``
    (PEP 621 ``[project]`` or ``[tool.poetry]``) or a ``setup.py`` ``name=``.
    """
    if sys.version_info >= (3, 11):
        import tomllib
    else:
        try:
            import tomli as tomllib  # type: ignore[import-not-found]
        except ModuleNotFoundError:
            return None

    pyproject = local_path / "pyproject.toml"
    if pyproject.exists():
        try:
            with open(pyproject, "rb") as f:
                data = tomllib.load(f)
            project = data.get("project", {})
            if "name" in project:
                return project["name"]
            poetry = data.get("tool", {}).get("poetry", {})
            if "name" in poetry:
                return poetry["name"]
        except Exception as e:
            logger.warning("Failed to parse %s: %s", pyproject, e)

    setup_py = local_path / "setup.py"
    if setup_py.exists():
        try:
            match = re.search(r'name\s*=\s*["\']([^"\']+)["\']', setup_py.read_text())
            if match:
                return match.group(1)
        except Exception as e:
            logger.warning("Failed to parse %s: %s", setup_py, e)

    return None


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
