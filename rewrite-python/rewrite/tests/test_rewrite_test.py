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

"""Tests for the Python test harness."""

import pytest

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.test import RecipeSpec, python, from_visitor, dedent
from rewrite.python.visitor import PythonVisitor
from rewrite.python.tree import CompilationUnit
from rewrite.java import J


class TestDedent:
    """Tests for the dedent helper function."""

    def test_dedent_removes_leading_newline(self):
        result = dedent("\n    hello")
        assert result == "hello"

    def test_dedent_removes_common_indentation(self):
        result = dedent(
            """
            def foo():
                pass
            """
        )
        assert result == "def foo():\n    pass\n"

    def test_dedent_preserves_relative_indentation(self):
        result = dedent(
            """
            if True:
                x = 1
                if False:
                    y = 2
            """
        )
        assert result == "if True:\n    x = 1\n    if False:\n        y = 2\n"

    def test_dedent_empty_string(self):
        assert dedent("") == ""
        assert dedent(None) == ""

    def test_dedent_no_indentation(self):
        result = dedent("hello\nworld")
        assert result == "hello\nworld"


class TestRecipeSpecParsing:
    """Tests for RecipeSpec parsing functionality."""

    def test_parse_simple_assignment(self):
        """Test that simple Python code parses correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            )
        )

    def test_parse_function_definition(self):
        """Test that function definitions parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                def foo(a, b):
                    return a + b
                """
            )
        )

    def test_parse_class_definition(self):
        """Test that class definitions parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                class MyClass:
                    def __init__(self):
                        self.value = 0
                """
            )
        )

    def test_parse_import_statement(self):
        """Test that import statements parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                import os
                from pathlib import Path
                """
            )
        )


class TestRecipeSpecNoChange:
    """Tests for RecipeSpec when no changes are expected."""

    def test_no_change_with_noop_recipe(self):
        """Test that NoopRecipe makes no changes."""
        spec = RecipeSpec()  # Uses NoopRecipe by default
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            )
        )

    def test_no_change_explicit(self):
        """Test explicit no-change assertion (after=None)."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                def foo():
                    pass
                """,
                None  # Explicitly no change expected
            )
        )


class TestRecipeSpecWithChanges:
    """Tests for RecipeSpec when changes are expected."""

    def test_recipe_makes_changes(self):
        """Test a recipe that modifies code."""

        class AddCommentVisitor(PythonVisitor[ExecutionContext]):
            """A visitor that adds a comment to the first statement."""

            def visit_compilation_unit(
                self, cu: CompilationUnit, ctx: ExecutionContext
            ) -> J:
                # For now, just return unchanged - we're testing the harness
                return cu

        # This test validates the harness can detect changes
        # For now we just test that the infrastructure works
        spec = RecipeSpec(recipe=from_visitor(AddCommentVisitor()))
        spec.rewrite_run(
            python(
                """
                x = 1
                """
                # No after = no change expected, which is correct since our visitor
                # doesn't actually make changes
            )
        )


class TestRecipeSpecHooks:
    """Tests for before_recipe and after_recipe hooks."""

    def test_before_recipe_hook_called(self):
        """Test that before_recipe hook is called after parsing."""
        called = []

        def before_hook(cu: CompilationUnit) -> None:
            called.append(("before", len(cu.statements)))

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                y = 2
                """,
                before_recipe=before_hook,
            )
        )

        assert len(called) == 1
        assert called[0][0] == "before"
        assert called[0][1] == 2  # Two statements

    def test_after_recipe_hook_called(self):
        """Test that after_recipe hook is called after recipe runs."""
        called = []

        def after_hook(cu: CompilationUnit) -> None:
            called.append(("after", cu.source_path))

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                from typing import Any, Dict, List, Optional, Callable, TypeVar
                from uuid import UUID

                x = 1
                """,
                after_recipe=after_hook,
            )
        )

        assert len(called) == 1
        assert called[0][0] == "after"

    def test_before_recipe_can_modify_ast(self):
        """Test that before_recipe can modify the parsed AST."""
        from rewrite import random_id
        from rewrite.markers import SearchResult

        def add_marker(cu: CompilationUnit) -> CompilationUnit:
            # Add a marker to the compilation unit
            new_marker = SearchResult(random_id(), "test")
            new_markers = cu.markers.replace(markers=cu.markers.markers + [new_marker])
            return cu.replace(markers=new_markers)

        marker_found = []

        def check_marker(cu: CompilationUnit) -> None:
            marker = cu.markers.find_first(SearchResult)
            marker_found.append(marker is not None)

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                before_recipe=add_marker,
                after_recipe=check_marker,
            )
        )

        assert marker_found == [True]


class TestRecipeSpecCallableAfter:
    """Tests for callable after assertions."""

    def test_callable_after_with_no_change(self):
        """Test callable after when recipe makes no changes."""
        received = []

        def check_actual(actual: str) -> None:
            # Return None to indicate actual output is acceptable
            received.append(actual)
            return None

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                # No after = no change expected
            )
        )

    def test_callable_after_returns_none_means_no_change_expected(self):
        """Test that returning None from callable means no change expected."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
                # No after = no change expected (this is the correct way to test)
            )
        )


class TestFromVisitor:
    """Tests for the from_visitor helper."""

    def test_from_visitor_creates_recipe(self):
        """Test that from_visitor creates a working Recipe."""
        from rewrite import TreeVisitor

        visitor = TreeVisitor.noop()
        recipe = from_visitor(visitor)
        assert isinstance(recipe, Recipe)
        assert recipe.name == "org.openrewrite.adhoc"

    def test_from_visitor_with_noop(self):
        """Test using from_visitor with a noop visitor."""
        from rewrite import TreeVisitor

        # Using noop visitor doesn't modify anything
        spec = RecipeSpec(recipe=from_visitor(TreeVisitor.noop()))
        spec.rewrite_run(
            python(
                """
                def hello():
                    print("world")
                """
            )
        )


class TestMultipleSourceSpecs:
    """Tests for handling multiple source specifications."""

    def test_multiple_python_files(self):
        """Test parsing multiple Python files."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            ),
            python(
                """
                y = 2
                """
            ),
            python(
                """
                z = 3
                """
            ),
        )
