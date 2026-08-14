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

"""Recipe marketplace for organizing and discovering recipes."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple, Type, Union, TYPE_CHECKING

from rewrite.category import CategoryDescriptor
from rewrite.recipe import Recipe, RecipeDescriptor

if TYPE_CHECKING:
    pass


@dataclass
class RecipeListing:
    """Listing-weight view the marketplace holds and serves for the InstallRecipes and
    GetMarketplace RPC commands, so listing never materializes the full recursive descriptor.
    The full tree is built lazily per recipe by PrepareRecipe. ``recipe_count`` is 1 + every
    transitive recipe_list entry, computed once at install time (the host uses it as a sort key)."""
    name: str
    display_name: str
    description: str
    estimated_effort_per_occurrence: Any = None
    options: Any = field(default_factory=list)
    data_tables: Any = field(default_factory=list)
    recipe_count: int = 1


def _count_recipes(descriptor) -> int:
    """The count of this recipe and all recipes nested transitively in its recipe_list."""
    count = 1
    for sub in descriptor.recipe_list:
        count += _count_recipes(sub)
    return count


def to_listing(descriptor) -> RecipeListing:
    """Derive the listing-weight view from a full descriptor, collapsing its recursive
    recipe_list to a count."""
    return RecipeListing(
        name=descriptor.name,
        display_name=descriptor.display_name,
        description=descriptor.description,
        estimated_effort_per_occurrence=descriptor.estimated_effort_per_occurrence,
        options=descriptor.options,
        data_tables=descriptor.data_tables,
        recipe_count=_count_recipes(descriptor),
    )


class RecipeMarketplace:
    """
    Registry that holds discovered recipes organized by category.

    The marketplace provides a hierarchical organization of recipes,
    similar to a file system. Recipes are installed into categories,
    and can be queried by name or browsed by category.

    Example:
        marketplace = RecipeMarketplace()
        marketplace.install(RemovePass, [Python, Cleanup])
        recipe = marketplace.find_recipe("org.openrewrite.python.RemovePass")
    """

    class Category:
        """A category in the marketplace hierarchy."""

        def __init__(self, descriptor: CategoryDescriptor):
            self.descriptor = descriptor
            self.categories: List[RecipeMarketplace.Category] = []
            self._recipes: Dict[str, Tuple[RecipeListing, Optional[Type[Recipe]]]] = {}

        @property
        def recipes(self) -> Dict[str, Tuple[RecipeListing, Optional[Type[Recipe]]]]:
            """Get the recipes dict (name -> (listing, class))."""
            return self._recipes

        def install(
            self,
            recipe: Union[Type[Recipe], RecipeDescriptor],
            category_path: List[CategoryDescriptor],
        ) -> None:
            """
            Install a recipe into this category or a subcategory.

            If a Recipe class is provided, it is instantiated to extract
            its descriptor. If a RecipeDescriptor is provided, it is used
            directly (for client-side hydration from RPC).

            Categories are specified top-down (shallowest to deepest).
            Intermediate categories are created as needed.

            Args:
                recipe: The recipe class or descriptor to install
                category_path: Category path from shallowest to deepest
            """
            if len(category_path) == 0:
                if isinstance(recipe, type) and issubclass(recipe, Recipe):
                    # It's a Recipe class - instantiate once to derive its listing (the class is
                    # retained so PrepareRecipe can later build the full tree).
                    try:
                        recipe_inst = recipe()
                        listing = to_listing(recipe_inst.descriptor())
                        # First-wins must match the host's name-keyed RecipeListing and
                        # RecipeAttribution.
                        self._recipes.setdefault(listing.name, (listing, recipe))
                    except Exception as e:
                        raise RuntimeError(
                            f"Failed to install recipe {recipe}. "
                            f"Ensure the constructor can be called without arguments."
                        ) from e
                else:
                    # It's already a RecipeDescriptor (client-side hydration) - derive its listing.
                    self._recipes.setdefault(recipe.name, (to_listing(recipe), None))
                return

            # Get the first category in the path
            first_category = category_path[0]
            target_category = self._find_or_create_category(first_category)

            # Recursively add to the child category
            target_category.install(recipe, category_path[1:])

        def _find_or_create_category(
            self, category_descriptor: CategoryDescriptor
        ) -> RecipeMarketplace.Category:
            """Find or create a subcategory with the given descriptor."""
            for category in self.categories:
                if category.descriptor.display_name == category_descriptor.display_name:
                    return category
            new_category = RecipeMarketplace.Category(category_descriptor)
            self.categories.append(new_category)
            return new_category

        def find_recipe(
            self, name: str
        ) -> Optional[Tuple[RecipeListing, Optional[Type[Recipe]]]]:
            """
            Find a recipe by its fully qualified name.

            Args:
                name: The recipe name (e.g., "org.openrewrite.python.RemovePass")

            Returns:
                A tuple of (descriptor, recipe_class) if found, None otherwise.
                recipe_class may be None if the recipe was installed as a descriptor.
            """
            if name in self._recipes:
                return self._recipes[name]
            for category in self.categories:
                found = category.find_recipe(name)
                if found:
                    return found
            return None

        def all_recipes(self) -> List[RecipeListing]:
            """Get all recipes in this category and subcategories."""
            result: List[RecipeListing] = [listing for listing, _ in self._recipes.values()]
            for category in self.categories:
                result.extend(category.all_recipes())
            return result

    def __init__(self):
        self.root = RecipeMarketplace.Category(
            CategoryDescriptor(
                display_name="Root",
                description="This is the root of all categories. "
                "When displaying the category hierarchy of a marketplace, "
                "this is typically not shown.",
            )
        )

    def install(
        self,
        recipe: Union[Type[Recipe], RecipeDescriptor],
        category_path: List[CategoryDescriptor],
    ) -> None:
        """
        Install a recipe into the marketplace under the specified category path.

        If a Recipe class is provided, it is instantiated to extract its descriptor.
        If a RecipeDescriptor is provided, it is used directly (for client-side hydration).
        Categories are specified top-down (shallowest to deepest).
        Intermediate categories are created as needed.

        Args:
            recipe: The recipe class or descriptor to install
            category_path: Category path from shallowest to deepest
                          (e.g., [Python, Cleanup] for Python > Cleanup)
        """
        self.root.install(recipe, category_path)

    def categories(self) -> List[Category]:
        """Get the top-level categories."""
        return self.root.categories

    def find_recipe(
        self, name: str
    ) -> Optional[Tuple[RecipeListing, Optional[Type[Recipe]]]]:
        """
        Find a recipe by its fully qualified name.

        Args:
            name: The recipe name (e.g., "org.openrewrite.python.RemovePass")

        Returns:
            A tuple of (descriptor, recipe_class) if found, None otherwise.
        """
        return self.root.find_recipe(name)

    def all_recipes(self) -> List[RecipeListing]:
        """Get all recipes in the marketplace."""
        return self.root.all_recipes()


# Pre-defined category constant for Python recipes
Python: List[CategoryDescriptor] = [CategoryDescriptor(display_name="Python")]
