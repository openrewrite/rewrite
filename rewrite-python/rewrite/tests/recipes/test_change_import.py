# Copyright 2026 the original author or authors.
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

"""Tests for ChangeImport recipe."""

from rewrite.python.recipes.change_import import ChangeImport
from rewrite.test import RecipeSpec, python


class TestChangeImport:
    """Tests for the ChangeImport recipe."""

    def test_change_from_import_module_and_name(self):
        """Change: from collections import Mapping -> from collections.abc import Mapping"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
            new_name='Mapping',
        ))
        spec.rewrite_run(
            python(
                """
                from collections import Mapping
                x: Mapping = {}
                """,
                """
                from collections.abc import Mapping
                x: Mapping = {}
                """,
            )
        )

    def test_change_direct_import(self):
        """Change: import os -> import pathlib"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os',
            new_module='pathlib',
        ))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
                """
                import pathlib
                x = 1
                """,
            )
        )

    def test_no_change_when_import_not_present(self):
        """No change when the old import doesn't exist."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
            new_name='Mapping',
        ))
        spec.rewrite_run(
            python(
                """
                import sys
                x = 1
                """,
            )
        )

    def test_change_from_import_different_name(self):
        """Change: from os.path import join -> from pathlib import PurePath"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os.path',
            old_name='join',
            new_module='pathlib',
            new_name='PurePath',
        ))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from pathlib import PurePath
                x = 1
                """,
            )
        )

    def test_change_removes_one_name_from_multi_import(self):
        """Change one name from 'from os.path import join, exists' leaves exists."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os.path',
            old_name='join',
            new_module='pathlib',
            new_name='PurePath',
        ))
        spec.rewrite_run(
            python(
                """
                from os.path import join, exists
                x = 1
                """,
                """
                from os.path import exists
                from pathlib import PurePath
                x = 1
                """,
            )
        )
