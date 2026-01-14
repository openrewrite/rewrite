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
