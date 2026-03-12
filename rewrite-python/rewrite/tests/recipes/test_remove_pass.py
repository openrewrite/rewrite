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

"""Tests for the RemovePass recipe."""

import pytest

from rewrite.test import RecipeSpec, python
from rewrite.python.recipes import RemovePass


class TestRemovePass:
    """Tests for the RemovePass recipe."""

    def test_removes_pass_from_function_body(self):
        """Test that pass is removed from a function with other statements."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                def foo():
                    pass
                    x = 1
                """,
                """
                def foo():
                    x = 1
                """,
            )
        )

    def test_removes_standalone_pass(self):
        """Test that standalone pass statement is removed."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                x = 1
                pass
                y = 2
                """,
                """
                x = 1
                y = 2
                """,
            )
        )

    def test_no_change_when_no_pass(self):
        """Test that code without pass is unchanged."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                def foo():
                    x = 1
                """
            )
        )

    def test_keeps_pass_when_only_statement_in_function(self):
        """Test that pass is NOT removed when it's the only statement in a function."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                def foo():
                    pass
                """
            )
        )

    def test_keeps_pass_when_only_statement_in_class(self):
        """Test that pass is NOT removed when it's the only statement in a class body."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                class Foo:
                    pass
                """
            )
        )

    def test_removes_pass_from_class_method(self):
        """Test that pass is removed from a method inside a class when there are other statements."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                class Foo:
                    def bar(self):
                        pass
                        x = 1
                """,
                """
                class Foo:
                    def bar(self):
                        x = 1
                """,
            )
        )

    def test_removes_pass_from_if_block(self):
        """Test that pass is removed from an if block when there are other statements."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                if True:
                    pass
                    x = 1
                """,
                """
                if True:
                    x = 1
                """,
            )
        )

    def test_keeps_pass_when_only_statement_in_if(self):
        """Test that pass is NOT removed when it's the only statement in an if block."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                if True:
                    pass
                """
            )
        )

    def test_removes_pass_from_else_block(self):
        """Test that pass is removed from an else block when there are other statements."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                """
                if False:
                    x = 1
                else:
                    pass
                    y = 2
                """,
                """
                if False:
                    x = 1
                else:
                    y = 2
                """,
            )
        )

    def test_keeps_pass_when_only_docstring_in_function(self):
        """Test that pass is NOT removed when the only other statement is a docstring."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                '''
                def foo():
                    """This is a docstring."""
                    pass
                '''
            )
        )

    def test_keeps_pass_when_only_docstring_in_class(self):
        """Test that pass is NOT removed when the only other statement is a class docstring."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                '''
                class Foo:
                    """This is a class docstring."""
                    pass
                '''
            )
        )

    def test_removes_pass_when_docstring_plus_other_statement(self):
        """Test that pass IS removed when there is a docstring AND other statements."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                '''
                def foo():
                    """This is a docstring."""
                    pass
                    x = 1
                ''',
                '''
                def foo():
                    """This is a docstring."""
                    x = 1
                ''',
            )
        )

    def test_removes_pass_when_string_not_first_statement(self):
        """Test that pass IS removed when the string literal is not the first statement (not a docstring)."""
        spec = RecipeSpec(recipe=RemovePass())
        spec.rewrite_run(
            python(
                '''
                def foo():
                    x = 1
                    """Not a docstring."""
                    pass
                ''',
                '''
                def foo():
                    x = 1
                    """Not a docstring."""
                ''',
            )
        )
