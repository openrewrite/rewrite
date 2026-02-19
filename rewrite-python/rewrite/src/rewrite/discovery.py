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
from importlib.metadata import entry_points
from typing import List, Tuple, Type, TYPE_CHECKING

from rewrite.category import CategoryDescriptor
from rewrite.decorators import get_recipe_category
from rewrite.marketplace import RecipeMarketplace
from rewrite.recipe import Recipe

if TYPE_CHECKING:
    pass


def discover_recipes() -> RecipeMarketplace:
    """
    Discover all recipes from installed packages via entry points.

    Looks for packages that declare entry points under
    [project.entry-points."openrewrite.recipes"] and calls their
    activate function to install recipes into the marketplace.

    Returns:
        A RecipeMarketplace containing all discovered recipes.

    Example pyproject.toml entry point:
        [project.entry-points."openrewrite.recipes"]
        mypackage = "mypackage:activate"

    The activate function signature should be:
        def activate(marketplace: RecipeMarketplace) -> None:
            marketplace.install(MyRecipe, [Python, Cleanup])
    """
    marketplace = RecipeMarketplace()

    # Python 3.10+ uses select parameter via SelectableGroups
    eps = entry_points(group="openrewrite.recipes")

    for ep in eps:
        try:
            module = ep.load()
            if hasattr(module, "activate") and callable(module.activate):
                module.activate(marketplace)
        except Exception:
            # Log or handle the error - for now, skip failed activations
            pass

    return marketplace


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
