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

"""Tests for recipe marketplace and discovery."""

import pytest

from rewrite import (
    CategoryDescriptor,
    RecipeMarketplace,
    Python,
    categorize,
    get_recipe_category,
    activate,
)
from rewrite.python.recipes import RemovePass, Cleanup


class TestCategoryDescriptor:
    """Tests for CategoryDescriptor."""

    def test_create_category_descriptor(self):
        """Test creating a CategoryDescriptor."""
        descriptor = CategoryDescriptor(
            display_name="Test Category",
            package_name="org.openrewrite.test",
            description="A test category",
        )
        assert descriptor.display_name == "Test Category"
        assert descriptor.package_name == "org.openrewrite.test"
        assert descriptor.description == "A test category"
        assert descriptor.tags == frozenset()
        assert descriptor.root is False
        assert descriptor.priority == -1
        assert descriptor.synthetic is False

    def test_category_descriptor_immutable(self):
        """Test that CategoryDescriptor is immutable."""
        descriptor = CategoryDescriptor(display_name="Test")
        with pytest.raises(Exception):
            descriptor.display_name = "Changed"  # type: ignore


class TestCategorizeDecorator:
    """Tests for the @categorize decorator."""

    def test_get_recipe_category_decorated(self):
        """Test getting category from a decorated recipe."""
        category = get_recipe_category(RemovePass)
        assert category is not None
        assert len(category) == 2
        assert category[0].display_name == "Python"
        assert category[1].display_name == "Cleanup"

    def test_get_recipe_category_undecorated(self):
        """Test getting category from an undecorated class."""
        from rewrite import Recipe

        # Recipe base class is not decorated
        category = get_recipe_category(Recipe)
        assert category is None


class TestRecipeMarketplace:
    """Tests for RecipeMarketplace."""

    def test_install_recipe_class(self):
        """Test installing a recipe class into the marketplace."""
        marketplace = RecipeMarketplace()
        marketplace.install(RemovePass, Cleanup)

        found = marketplace.find_recipe("org.openrewrite.python.RemovePass")
        assert found is not None
        descriptor, recipe_class = found
        assert descriptor.name == "org.openrewrite.python.RemovePass"
        assert recipe_class is RemovePass

    def test_find_recipe_not_found(self):
        """Test finding a recipe that doesn't exist."""
        marketplace = RecipeMarketplace()
        found = marketplace.find_recipe("nonexistent.Recipe")
        assert found is None

    def test_all_recipes(self):
        """Test listing all recipes."""
        marketplace = RecipeMarketplace()
        marketplace.install(RemovePass, Cleanup)

        all_recipes = marketplace.all_recipes()
        assert len(all_recipes) == 1
        assert all_recipes[0].name == "org.openrewrite.python.RemovePass"

    def test_categories(self):
        """Test getting categories."""
        marketplace = RecipeMarketplace()
        marketplace.install(RemovePass, Cleanup)

        categories = marketplace.categories()
        assert len(categories) == 1
        assert categories[0].descriptor.display_name == "Python"

        # Check subcategory
        assert len(categories[0].categories) == 1
        assert categories[0].categories[0].descriptor.display_name == "Cleanup"

    def test_install_multiple_categories(self):
        """Test installing recipes in multiple categories."""
        marketplace = RecipeMarketplace()

        # Install in Python > Cleanup
        marketplace.install(RemovePass, Cleanup)

        # Check structure
        python_cat = marketplace.categories()[0]
        assert python_cat.descriptor.display_name == "Python"

        cleanup_cat = python_cat.categories[0]
        assert cleanup_cat.descriptor.display_name == "Cleanup"
        # recipes is now a dict keyed by recipe name
        assert len(cleanup_cat.recipes) == 1
        assert "org.openrewrite.python.RemovePass" in cleanup_cat.recipes


class TestActivate:
    """Tests for the activate function."""

    def test_activate_installs_recipes(self):
        """Test that activate() installs recipes into the marketplace."""
        marketplace = RecipeMarketplace()
        activate(marketplace)

        # Should find RemovePass
        found = marketplace.find_recipe("org.openrewrite.python.RemovePass")
        assert found is not None

    def test_activate_categories_correct(self):
        """Test that activate() uses correct category paths."""
        marketplace = RecipeMarketplace()
        activate(marketplace)

        categories = marketplace.categories()
        assert len(categories) >= 1

        # Find Python category
        python_cat = next(
            (c for c in categories if c.descriptor.display_name == "Python"), None
        )
        assert python_cat is not None


class TestPythonCategory:
    """Tests for the Python category constant."""

    def test_python_category(self):
        """Test the Python category constant."""
        assert len(Python) == 1
        assert Python[0].display_name == "Python"

    def test_cleanup_category(self):
        """Test the Cleanup category constant."""
        assert len(Cleanup) == 2
        assert Cleanup[0].display_name == "Python"
        assert Cleanup[1].display_name == "Cleanup"
