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

"""Decorators for recipe registration and discovery."""

from __future__ import annotations

from typing import List, Optional, Type, TYPE_CHECKING

if TYPE_CHECKING:
    from rewrite.category import CategoryDescriptor
    from rewrite.recipe import Recipe

_RECIPE_CATEGORY_KEY = "__recipe_category__"


def categorize(category: List[CategoryDescriptor]):
    """
    Decorator to mark a Recipe class with category information for discovery.

    The category path specifies where in the marketplace hierarchy this
    recipe should appear. Categories are specified from shallowest to
    deepest (e.g., [Python, Cleanup] means Python > Cleanup).

    Args:
        category: List of CategoryDescriptor objects forming the category path.
                  Must contain at least one category.

    Returns:
        A class decorator that stores the category path on the class.

    Raises:
        ValueError: If category is empty.

    Example:
        from rewrite import categorize
        from rewrite.python.recipes import Cleanup

        @categorize(Cleanup)
        class RemovePass(Recipe):
            ...
    """

    def decorator(cls: Type[Recipe]) -> Type[Recipe]:
        if not category:
            raise ValueError(
                f"@categorize decorator requires at least one category, "
                f"but none provided for {cls.__name__}"
            )
        setattr(cls, _RECIPE_CATEGORY_KEY, category)
        return cls

    return decorator


def get_recipe_category(recipe_class: Type[Recipe]) -> Optional[List[CategoryDescriptor]]:
    """
    Get the category path for a decorated recipe class.

    Args:
        recipe_class: A Recipe subclass that may have been decorated with @recipe

    Returns:
        The category path if the class was decorated, None otherwise.
        An empty list indicates the class was decorated but with no category path.
    """
    if hasattr(recipe_class, _RECIPE_CATEGORY_KEY):
        return getattr(recipe_class, _RECIPE_CATEGORY_KEY)
    return None
