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

"""
Test utilities for OpenRewrite Python recipes.

This module provides a pytest-friendly test harness for testing
OpenRewrite recipes using the before/after pattern.

Example usage:

    from rewrite.test import RecipeSpec, python

    def test_my_recipe():
        spec = RecipeSpec(recipe=MyRecipe())
        spec.rewrite_run(
            python(
                '''
                import os
                ''',
                '''
                import sys
                '''
            )
        )

For testing visitors directly without a Recipe:

    from rewrite.test import RecipeSpec, python, from_visitor

    def test_my_visitor():
        spec = RecipeSpec(recipe=from_visitor(MyVisitor()))
        spec.rewrite_run(python("x = 1", "x = 2"))
"""

from .spec import SourceSpec, AfterRecipeText, python, dedent
from .rewrite_test import RecipeSpec, NoopRecipe, AdHocRecipe, from_visitor

__all__ = [
    # Core types
    "SourceSpec",
    "AfterRecipeText",
    "RecipeSpec",
    # Recipe helpers
    "NoopRecipe",
    "AdHocRecipe",
    "from_visitor",
    # Source spec helpers
    "python",
    "dedent",
]
