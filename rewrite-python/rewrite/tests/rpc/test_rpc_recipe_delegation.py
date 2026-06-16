# Copyright 2025 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Wire-level tests for a Python composite recipe that references a Java recipe.

These exercise the ``RpcRecipe`` reference type and the ``handle_prepare_recipe``
delegation path WITHOUT a running JVM. They prove the contract the JVM relies on:

  * A pure ``recipe_list()`` composite (no ``editor()``) can list a ``RpcRecipe``.
  * The composite's serialized descriptor carries the Java recipe id as a child.
  * When the JVM round-trips ``PrepareRecipe`` for that id, the Python peer
    answers with ``delegatesTo: {recipeName, options}`` so the JVM instantiates
    the real Java recipe natively (full ScanningRecipe lifecycle).

The end-to-end proof that a Java ScanningRecipe actually edits a non-Python file
lives in a JVM-orchestrated JUnit test (see rewrite-python/src/integTest/java).
"""

import pytest

from rewrite import Recipe, RecipeMarketplace
from rewrite.marketplace import Python
from rewrite.rpc.rpc_recipe import RpcRecipe

JAVA_ID = "org.openrewrite.python.UpgradeDependencyVersion"


class TestRpcRecipeReference:
    def test_options_are_passed_through_verbatim(self):
        # Options name the target recipe's own (here camelCase) options; there
        # is no name translation.
        recipe = RpcRecipe(JAVA_ID, packageName="pydantic", newVersion=">=2.11.0")

        assert recipe.name == JAVA_ID
        assert recipe.java_recipe_name == JAVA_ID
        assert recipe.delegates_to_options == {
            "packageName": "pydantic",
            "newVersion": ">=2.11.0",
        }

    def test_option_names_are_not_transformed(self):
        # A non-camelCase key is preserved exactly, so RpcRecipe can target
        # recipes whose options are not camelCase (e.g. another Python recipe).
        recipe = RpcRecipe(JAVA_ID, some_option="x")
        assert recipe.delegates_to_options == {"some_option": "x"}

    def test_no_options_yields_empty_delegation_options(self):
        recipe = RpcRecipe(JAVA_ID)
        assert recipe.delegates_to_options == {}

    def test_is_a_recipe_with_no_child_recipes_of_its_own(self):
        recipe = RpcRecipe(JAVA_ID)
        assert isinstance(recipe, Recipe)
        assert recipe.recipe_list() == []


class _UpgradePydantic(Recipe):
    """Pure composite: overrides only ``recipe_list()`` (no ``editor()``)."""

    @property
    def name(self) -> str:
        return "io.moderne.example.UpgradePydantic"

    @property
    def display_name(self) -> str:
        return "Upgrade Pydantic"

    @property
    def description(self) -> str:
        return "Composite referencing a Java recipe by id with kwargs options."

    def recipe_list(self) -> list[Recipe]:
        return [
            RpcRecipe(JAVA_ID, packageName="pydantic", newVersion=">=2.11.0"),
        ]


@pytest.fixture
def isolated_server():
    """Swap the global marketplace + state caches so tests don't leak."""
    import rewrite.rpc.server as server

    saved_marketplace = server._marketplace
    server._marketplace = RecipeMarketplace()
    server._prepared_recipes.clear()
    server._delegating_recipes.clear()
    try:
        yield server
    finally:
        server._marketplace = saved_marketplace
        server._prepared_recipes.clear()
        server._delegating_recipes.clear()


class TestPrepareRecipeDelegation:
    def test_composite_descriptor_lists_java_recipe_as_child(self, isolated_server):
        server = isolated_server
        server._marketplace.install(_UpgradePydantic, Python)

        response = server.handle_prepare_recipe(
            {"id": "io.moderne.example.UpgradePydantic"}
        )

        recipe_list = response["descriptor"]["recipeList"]
        assert [child["name"] for child in recipe_list] == [JAVA_ID]

    def test_preparing_the_java_child_returns_delegates_to(self, isolated_server):
        server = isolated_server
        server._marketplace.install(_UpgradePydantic, Python)

        # Preparing the composite registers its RpcRecipe children so the
        # subsequent round-trip for the Java id resolves.
        server.handle_prepare_recipe({"id": "io.moderne.example.UpgradePydantic"})

        # The JVM materializes the composite as an RpcRecipe and, in
        # getRecipeList(), round-trips PrepareRecipe for each child id.
        child_response = server.handle_prepare_recipe({"id": JAVA_ID})

        assert child_response["delegatesTo"] == {
            "recipeName": JAVA_ID,
            "options": {"packageName": "pydantic", "newVersion": ">=2.11.0"},
        }
