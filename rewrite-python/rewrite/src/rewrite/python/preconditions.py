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

"""Precondition helpers that delegate to Java search recipes.

A recipe author wraps an editor with a precondition like so::

    from rewrite import Preconditions
    from rewrite.python.preconditions import uses_method

    class ReplaceArrayTostring(Recipe):
        def editor(self):
            return Preconditions.check(uses_method("*..* tostring(..)"), Visitor())

These helpers return :class:`rewrite.preconditions.RecipeRef` placeholders
that record the Java recipe class name and options without firing an RPC.
The framework introspects the wrapper at PrepareRecipe time and emits the
recipe identity directly in ``editPreconditions``; the Java host's
``PreparedRecipeCache.instantiateVisitor`` constructs the recipe and uses
its visitor — no extra RPC round-trip needed. This keeps ``recipe.editor()``
callable in unit tests without an active RPC connection.
"""

from __future__ import annotations

from rewrite.preconditions import RecipeRef


def has_source_path(file_pattern: str) -> RecipeRef:
    """Match source files by path glob (delegates to ``org.openrewrite.FindSourceFiles``)."""
    return RecipeRef(
        "org.openrewrite.FindSourceFiles", {"filePattern": file_pattern}
    )


def uses_method(method_pattern: str, match_overrides: bool = False) -> RecipeRef:
    """Match files using a specific method (delegates to ``org.openrewrite.java.search.HasMethod``).

    ``method_pattern`` follows the OpenRewrite method-pattern syntax::

        <receiver-type> <method-name>(<args>)

    Use ``*..*`` to match any class in any package, ``(..)`` to match any
    arguments::

        uses_method("*..* tostring(..)")
        uses_method("java.util.Collections emptyList()")
    """
    return RecipeRef(
        "org.openrewrite.java.search.HasMethod",
        {"methodPattern": method_pattern, "matchOverrides": match_overrides},
    )


def uses_type(
    fully_qualified_type: str, check_assignability: bool = False
) -> RecipeRef:
    """Match files using a specific type (delegates to ``org.openrewrite.java.search.HasType``)."""
    return RecipeRef(
        "org.openrewrite.java.search.HasType",
        {
            "fullyQualifiedTypeName": fully_qualified_type,
            "checkAssignability": check_assignability,
        },
    )


def find_methods(
    method_pattern: str, match_overrides: bool = False
) -> RecipeRef:
    """Find and mark methods matching a pattern (delegates to ``org.openrewrite.java.search.FindMethods``)."""
    return RecipeRef(
        "org.openrewrite.java.search.FindMethods",
        {"methodPattern": method_pattern, "matchOverrides": match_overrides},
    )


def find_types(fully_qualified_type: str) -> RecipeRef:
    """Find and mark usages of a type (delegates to ``org.openrewrite.java.search.FindTypes``)."""
    return RecipeRef(
        "org.openrewrite.java.search.FindTypes",
        {"fullyQualifiedTypeName": fully_qualified_type},
    )
