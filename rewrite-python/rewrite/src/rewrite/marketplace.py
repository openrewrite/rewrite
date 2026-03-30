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

from typing import Dict, List, Optional, Tuple, Type, Union, TYPE_CHECKING

from rewrite.category import CategoryDescriptor
from rewrite.recipe import Recipe, RecipeDescriptor

if TYPE_CHECKING:
    pass


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
            # Use recipe name as key since RecipeDescriptor contains unhashable Lists
            self._recipes: Dict[str, Tuple[RecipeDescriptor, Optional[Type[Recipe]]]] = {}

        @property
        def recipes(self) -> Dict[str, Tuple[RecipeDescriptor, Optional[Type[Recipe]]]]:
            """Get the recipes dict (name -> (descriptor, class))."""
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
                    # It's a Recipe class - instantiate to get descriptor
                    try:
                        recipe_inst = recipe()
                        desc = recipe_inst.descriptor()
                        self._recipes[desc.name] = (desc, recipe)
                    except Exception as e:
                        raise RuntimeError(
                            f"Failed to install recipe {recipe}. "
                            f"Ensure the constructor can be called without arguments."
                        ) from e
                else:
                    # It's already a RecipeDescriptor
                    self._recipes[recipe.name] = (recipe, None)
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
        ) -> Optional[Tuple[RecipeDescriptor, Optional[Type[Recipe]]]]:
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

        def all_recipes(self) -> List[RecipeDescriptor]:
            """Get all recipes in this category and subcategories."""
            result: List[RecipeDescriptor] = [desc for desc, _ in self._recipes.values()]
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
    ) -> Optional[Tuple[RecipeDescriptor, Optional[Type[Recipe]]]]:
        """
        Find a recipe by its fully qualified name.

        Args:
            name: The recipe name (e.g., "org.openrewrite.python.RemovePass")

        Returns:
            A tuple of (descriptor, recipe_class) if found, None otherwise.
        """
        return self.root.find_recipe(name)

    def all_recipes(self) -> List[RecipeDescriptor]:
        """Get all recipes in the marketplace."""
        return self.root.all_recipes()


# Pre-defined category constant for Python recipes
Python: List[CategoryDescriptor] = [CategoryDescriptor(display_name="Python")]
