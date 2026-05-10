# Copyright 2025 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Wire-level tests for Preconditions.check introspection at PrepareRecipe time.

These exercise ``handle_prepare_recipe`` and verify it correctly extracts
precondition wire entries from a ``Preconditions.check(...)`` wrapper around
``recipe.editor()``, registers the bare editor in the override cache, and
returns the wrapper-free editor on subsequent ``_instantiate_visitor`` calls.
"""

from typing import Any

import pytest

from rewrite import (
    InMemoryExecutionContext,
    Preconditions,
    Recipe,
    RecipeMarketplace,
    Tree,
    TreeVisitor,
)
from rewrite.marketplace import Python


class _Identity(TreeVisitor[Tree, Any]):
    def visit(self, tree, p, parent=None):
        return tree


class _BareEditor(TreeVisitor[Tree, Any]):
    """Sentinel editor; identity is what we assert in tests."""

    sentinel = "bare-editor-sentinel"


class _PreconditionRecipe(Recipe):
    """Recipe whose editor() returns Preconditions.check(condition_recipe, bare).

    The condition is itself a recipe so we exercise the RecipeCheck path —
    the Java side will resolve its wire identity via ``edit:<id>``.
    """

    @property
    def name(self):
        return "test.precondition.PreconditionRecipe"

    @property
    def display_name(self):
        return "Precondition recipe"

    @property
    def description(self):
        return "Wraps an editor in a precondition for testing wire serialization."

    def editor(self):
        return Preconditions.check(_ConditionRecipe(), _BareEditor())


class _ConditionRecipe(Recipe):
    @property
    def name(self):
        return "test.precondition.ConditionRecipe"

    @property
    def display_name(self):
        return "Condition recipe"

    @property
    def description(self):
        return "Plays the role of a search recipe used as a precondition."

    def editor(self):
        return _Identity()


class _PlainRecipe(Recipe):
    @property
    def name(self):
        return "test.precondition.PlainRecipe"

    @property
    def display_name(self):
        return "Plain"

    @property
    def description(self):
        return "Editor() returns a plain visitor (no Preconditions.check wrapper)."

    def editor(self):
        return _BareEditor()


@pytest.fixture
def isolated_server():
    """Swap the global marketplace + state caches so tests don't leak."""
    import rewrite.rpc.server as server

    saved_marketplace = server._marketplace
    server._marketplace = RecipeMarketplace()
    server._prepared_recipes.clear()
    server._prepared_editor_overrides.clear()
    server._prepared_edit_preconditions.clear()
    try:
        yield server
    finally:
        server._marketplace = saved_marketplace
        server._prepared_recipes.clear()
        server._prepared_editor_overrides.clear()
        server._prepared_edit_preconditions.clear()


def _install(server, recipe_cls):
    server._marketplace.install(recipe_cls, Python)


class TestPrepareRecipeWithPreconditions:
    def test_plain_editor_yields_only_baseline_preconditions(self, isolated_server):
        server = isolated_server
        _install(server, _PlainRecipe)

        response = server.handle_prepare_recipe(
            {"id": "test.precondition.PlainRecipe"}
        )

        # Only the language-gate precondition (FindTreesOfType) is expected.
        precs = response["editPreconditions"]
        assert len(precs) == 1
        assert precs[0]["visitorName"] == "org.openrewrite.rpc.internal.FindTreesOfType"
        prepared_id = response["id"]
        # No override cached: editor() returned a plain visitor.
        assert prepared_id not in server._prepared_editor_overrides

    def test_check_wrapped_editor_emits_recipe_precondition(self, isolated_server):
        server = isolated_server
        _install(server, _ConditionRecipe)
        _install(server, _PreconditionRecipe)

        # PrepareRecipe the condition first so its wire identity is known
        # when the wrapping recipe's editor() is introspected.
        cond_resp = server.handle_prepare_recipe(
            {"id": "test.precondition.ConditionRecipe"}
        )
        cond_visitor = cond_resp["editVisitor"]

        # Make the same recipe instance available to RecipeCheck. The recipe
        # constructed inside _PreconditionRecipe.editor() is a fresh instance
        # of _ConditionRecipe; rebind the global so it matches the prepared
        # one. We do this by storing the prepared recipe as a sentinel and
        # making _PreconditionRecipe.editor() return Preconditions.check
        # against that exact instance.
        prepared_condition_recipe = server._prepared_recipes[cond_resp["id"]]

        class _PreconditionRecipeUsingPrepared(Recipe):
            @property
            def name(self):
                return "test.precondition.PreconditionRecipeUsingPrepared"

            @property
            def display_name(self):
                return "Precondition recipe using prepared"

            @property
            def description(self):
                return "Wires the precondition to a previously-prepared condition recipe."

            def editor(self):
                return Preconditions.check(prepared_condition_recipe, _BareEditor())

        _install(server, _PreconditionRecipeUsingPrepared)

        response = server.handle_prepare_recipe(
            {"id": "test.precondition.PreconditionRecipeUsingPrepared"}
        )

        precs = response["editPreconditions"]
        # Baseline language-gate + recipe precondition.
        assert len(precs) == 2
        # First entry is the language gate, second is the recipe-as-precondition.
        assert precs[0]["visitorName"] == (
            "org.openrewrite.rpc.internal.FindTreesOfType"
        )
        assert precs[1]["visitorName"] == cond_visitor

    def test_bare_editor_is_cached_and_returned_by_instantiate(
        self, isolated_server
    ):
        server = isolated_server
        _install(server, _ConditionRecipe)

        cond_resp = server.handle_prepare_recipe(
            {"id": "test.precondition.ConditionRecipe"}
        )
        prepared_condition_recipe = server._prepared_recipes[cond_resp["id"]]

        class _Wrapper(Recipe):
            @property
            def name(self):
                return "test.precondition.Wrapper"

            @property
            def display_name(self):
                return "Wrapper"

            @property
            def description(self):
                return "Editor returns Preconditions.check around a bare editor."

            def editor(self):
                return Preconditions.check(prepared_condition_recipe, _BareEditor())

        _install(server, _Wrapper)
        resp = server.handle_prepare_recipe({"id": "test.precondition.Wrapper"})
        prepared_id = resp["id"]

        # The override cache holds the BARE editor (not the Check wrapper),
        # so dispatch via _instantiate_visitor returns it directly.
        assert prepared_id in server._prepared_editor_overrides
        cached = server._prepared_editor_overrides[prepared_id]
        assert isinstance(cached, _BareEditor)

        ctx = InMemoryExecutionContext()
        instantiated = server._instantiate_visitor(f"edit:{prepared_id}", ctx)
        assert instantiated is cached

    def test_recipe_ref_emits_class_name_and_options(self, isolated_server):
        """uses_method/uses_type return a RecipeRef; the wire entry should
        carry the Java recipe class name and options directly so that the
        host's PreparedRecipeCache can construct the recipe via Jackson."""
        server = isolated_server

        from rewrite.python.preconditions import uses_method

        class _RecipeRefWrapper(Recipe):
            @property
            def name(self):
                return "test.precondition.RecipeRefWrapper"

            @property
            def display_name(self):
                return "Recipe ref wrapper"

            @property
            def description(self):
                return "Editor wraps in Preconditions.check around a RecipeRef from uses_method."

            def editor(self):
                return Preconditions.check(
                    uses_method("*..* tostring(..)"), _BareEditor()
                )

        _install(server, _RecipeRefWrapper)
        resp = server.handle_prepare_recipe(
            {"id": "test.precondition.RecipeRefWrapper"}
        )

        precs = resp["editPreconditions"]
        # Baseline language gate + the RecipeRef precondition.
        assert len(precs) == 2
        assert precs[0]["visitorName"] == (
            "org.openrewrite.rpc.internal.FindTreesOfType"
        )
        assert precs[1]["visitorName"] == "org.openrewrite.java.search.HasMethod"
        assert precs[1]["visitorOptions"] == {
            "methodPattern": "*..* tostring(..)",
            "matchOverrides": False,
        }

        # Bare editor cached so dispatch via _instantiate_visitor returns it.
        prepared_id = resp["id"]
        assert prepared_id in server._prepared_editor_overrides
        assert isinstance(server._prepared_editor_overrides[prepared_id], _BareEditor)

    def test_check_with_unserializable_visitor_falls_back(self, isolated_server):
        """A Check wrapping a generic TreeVisitor (no recipe identity, no
        java_recipe_name, no PreparedJavaRecipe) cannot be propagated to the
        wire — it would have to RPC back to Python which defeats the
        optimization. The introspection silently leaves the wrapper intact so
        the precondition still runs Python-side via the wrapper's visit()."""
        server = isolated_server

        class _OpaqueCheckRecipe(Recipe):
            @property
            def name(self):
                return "test.precondition.OpaqueCheck"

            @property
            def display_name(self):
                return "Opaque check"

            @property
            def description(self):
                return "Editor wraps in a Check whose condition has no wire identity."

            def editor(self):
                return Preconditions.check(_Identity(), _BareEditor())

        _install(server, _OpaqueCheckRecipe)
        resp = server.handle_prepare_recipe(
            {"id": "test.precondition.OpaqueCheck"}
        )
        prepared_id = resp["id"]

        # No wire entry beyond the baseline language gate.
        assert len(resp["editPreconditions"]) == 1
        # No override cached — the wrapper still runs Python-side as a fallback.
        assert prepared_id not in server._prepared_editor_overrides
