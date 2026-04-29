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
    Recipe,
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


class _UnregisteredRecipe(Recipe):
    @property
    def name(self): return "org.openrewrite.python.test.Unregistered"
    @property
    def display_name(self): return "Unregistered"
    @property
    def description(self): return "A recipe not registered in the marketplace."


class _CrossModuleRecipeList(Recipe):
    @property
    def name(self): return "org.openrewrite.python.test.CrossModuleRecipeList"
    @property
    def display_name(self): return "Cross-module recipe list"
    @property
    def description(self): return "A recipe that delegates to an unregistered sub-recipe."
    def recipe_list(self):
        return [_UnregisteredRecipe()]


class TestInstallSubRecipes:
    """Tests for cross-module sub-recipe installation during PrepareRecipe."""

    def test_sub_recipes_installed_on_prepare(self):
        """Preparing a recipe auto-installs its sub-recipes so they can be prepared later."""
        import rewrite.rpc.server as server

        # given
        marketplace = RecipeMarketplace()
        marketplace.install(_CrossModuleRecipeList, Python)
        saved = server._marketplace
        server._marketplace = marketplace
        try:
            # when
            server.handle_prepare_recipe({'id': 'org.openrewrite.python.test.CrossModuleRecipeList'})

            # then
            server.handle_prepare_recipe({'id': 'org.openrewrite.python.test.Unregistered'})
        finally:
            server._marketplace = saved


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


class _PkgARecipe(Recipe):
    @property
    def name(self): return "org.openrewrite.test.pkga.RecipeA"
    @property
    def display_name(self): return "Recipe A"
    @property
    def description(self): return "Owned by package A."


class _PkgBRecipe(Recipe):
    @property
    def name(self): return "org.openrewrite.test.pkgb.RecipeB"
    @property
    def display_name(self): return "Recipe B"
    @property
    def description(self): return "Owned by package B."


class TestPerPackageAttribution:
    """Regression tests for the over-attribution bug.

    Before the fix, a singleton marketplace was shared across all installs and
    GetMarketplace returned everything in it. Installing a non-recipe package
    (e.g., a visualization-only pip package) would still report every built-in
    ``org.openrewrite.python.*`` recipe as belonging to that package.
    These tests pin the per-distribution scoping so each package's install
    response carries only the recipes its own entry points activated.
    """

    def setup_method(self):
        import rewrite.rpc.server as server

        # Reset module state so tests don't bleed into each other.
        self._saved_marketplace = server._marketplace
        self._saved_package_recipes = dict(server._package_recipes)
        server._marketplace = RecipeMarketplace()
        server._package_recipes.clear()

    def teardown_method(self):
        import rewrite.rpc.server as server

        server._marketplace = self._saved_marketplace
        server._package_recipes.clear()
        server._package_recipes.update(self._saved_package_recipes)

    def test_install_response_returns_only_packages_own_recipes(self, monkeypatch):
        """Installing pkg-a then pkg-b: each response carries only its own recipe."""
        import rewrite.rpc.server as server

        def fake_import_and_activate(name, marketplace, local_path=None):
            # Simulate the activate(marketplace) call each entry point would make.
            if name == "pkg-a":
                marketplace.install(_PkgARecipe, Python)
            elif name == "pkg-b":
                marketplace.install(_PkgBRecipe, Python)

        monkeypatch.setattr(server, "_import_and_activate_package", fake_import_and_activate)

        a_response = server.handle_install_recipes({"recipes": {"packageName": "pkg-a"}})
        b_response = server.handle_install_recipes({"recipes": {"packageName": "pkg-b"}})

        a_names = {r["descriptor"]["name"] for r in a_response["recipes"]}
        b_names = {r["descriptor"]["name"] for r in b_response["recipes"]}

        assert a_names == {"org.openrewrite.test.pkga.RecipeA"}
        assert b_names == {"org.openrewrite.test.pkgb.RecipeB"}

    def test_install_response_for_package_with_no_recipes_is_empty(self, monkeypatch):
        """Visualization-only pip packages (no recipes) must not be assigned others' recipes.

        Reproduces the moderne-visualizations-misc case: pkg-a contributed
        recipes earlier; pkg-b is a no-op (no openrewrite.recipes entry point).
        Installing pkg-b must NOT inherit pkg-a's recipes.
        """
        import rewrite.rpc.server as server

        def fake_import_and_activate(name, marketplace, local_path=None):
            if name == "pkg-a":
                marketplace.install(_PkgARecipe, Python)
            # pkg-b is intentionally a no-op.

        monkeypatch.setattr(server, "_import_and_activate_package", fake_import_and_activate)

        server.handle_install_recipes({"recipes": {"packageName": "pkg-a"}})
        b_response = server.handle_install_recipes({"recipes": {"packageName": "pkg-b"}})

        assert b_response["recipes"] == []
        assert b_response["recipesInstalled"] == 0

    def test_get_marketplace_filters_by_package_name(self, monkeypatch):
        """GetMarketplace with packageName returns only that package's recipes."""
        import rewrite.rpc.server as server

        def fake_import_and_activate(name, marketplace, local_path=None):
            if name == "pkg-a":
                marketplace.install(_PkgARecipe, Python)
            elif name == "pkg-b":
                marketplace.install(_PkgBRecipe, Python)

        monkeypatch.setattr(server, "_import_and_activate_package", fake_import_and_activate)

        server.handle_install_recipes({"recipes": {"packageName": "pkg-a"}})
        server.handle_install_recipes({"recipes": {"packageName": "pkg-b"}})

        # Filter by pkg-a
        rows_a = server.handle_get_marketplace({"packageName": "pkg-a"})
        assert {r["descriptor"]["name"] for r in rows_a} == {"org.openrewrite.test.pkga.RecipeA"}

        # Filter by pkg-b
        rows_b = server.handle_get_marketplace({"packageName": "pkg-b"})
        assert {r["descriptor"]["name"] for r in rows_b} == {"org.openrewrite.test.pkgb.RecipeB"}

        # No filter still returns everything for callers that want the full marketplace.
        rows_all = server.handle_get_marketplace({})
        assert {r["descriptor"]["name"] for r in rows_all} == {
            "org.openrewrite.test.pkga.RecipeA",
            "org.openrewrite.test.pkgb.RecipeB",
        }

    def test_package_name_normalization(self, monkeypatch):
        """Hyphen/underscore variants resolve to the same package."""
        import rewrite.rpc.server as server

        def fake_import_and_activate(name, marketplace, local_path=None):
            if name in ("openrewrite-migrate-python", "openrewrite_migrate_python"):
                marketplace.install(_PkgARecipe, Python)

        monkeypatch.setattr(server, "_import_and_activate_package", fake_import_and_activate)

        # Install with hyphen form.
        server.handle_install_recipes({"recipes": {"packageName": "openrewrite-migrate-python"}})

        # Query with underscore form.
        rows = server.handle_get_marketplace({"packageName": "openrewrite_migrate_python"})
        assert {r["descriptor"]["name"] for r in rows} == {"org.openrewrite.test.pkga.RecipeA"}
