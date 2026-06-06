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

"""Tests for AddImport / maybe_add_import."""

from rewrite import ExecutionContext
from rewrite.java import J
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor


def _add_import_visitor(module, name=None, alias=None):
    """Build a visitor that registers a single maybe_add_import (always added)."""
    class _V(PythonVisitor[ExecutionContext]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
            maybe_add_import(self, AddImportOptions(
                module=module,
                name=name,
                alias=alias,
                only_if_referenced=False,
            ))
            return super().visit_compilation_unit(cu, p)

    return _V()


class TestMaybeAddImport:
    """Tests for maybe_add_import scheduling via _after_visit."""

    def test_add_from_import(self):
        """Add 'from os.path import join' to a file that uses join."""
        class AddJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_add_import(self, AddImportOptions(
                    module='os.path',
                    name='join',
                    only_if_referenced=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(AddJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                from os.path import join
                x = 1
                """,
            )
        )

    def test_add_direct_import(self):
        """Add 'import os' to a file."""
        class AddOsVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_add_import(self, AddImportOptions(
                    module='os',
                    only_if_referenced=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(AddOsVisitor()))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                import os
                x = 1
                """,
            )
        )

    def test_no_duplicate_import(self):
        """Don't add an import that already exists."""
        class AddOsVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_add_import(self, AddImportOptions(
                    module='os',
                    only_if_referenced=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(AddOsVisitor()))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
            )
        )

    def test_only_if_referenced(self):
        """Don't add import when the name is not referenced and only_if_referenced=True."""
        class AddJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_add_import(self, AddImportOptions(
                    module='os.path',
                    name='join',
                    only_if_referenced=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(AddJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
            )
        )

    def test_merge_into_existing_from_import(self):
        """Merge a new name into an existing 'from X import ...' statement.

        The new member is inserted in case-insensitive alphabetical position
        ('exists' < 'join'), not appended.
        """
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('os.path', 'exists')))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from os.path import exists, join
                x = 1
                """,
            )
        )

    def test_merge_inserts_member_in_sorted_position(self):
        """Insert before an existing member when it sorts earlier (the reported bug)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('typing', 'ClassVar')))
        spec.rewrite_run(
            python(
                """
                from typing import Final
                x = 1
                """,
                """
                from typing import ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_inserts_member_in_middle(self):
        """Insert a member between two existing, already-sorted members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('typing', 'ClassVar')))
        spec.rewrite_run(
            python(
                """
                from typing import Any, Final
                x = 1
                """,
                """
                from typing import Any, ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_appends_when_alphabetically_last(self):
        """Append a member when it sorts after all existing members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('typing', 'Final')))
        spec.rewrite_run(
            python(
                """
                from typing import Any, ClassVar
                x = 1
                """,
                """
                from typing import Any, ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_is_case_insensitive(self):
        """Ordering is case-insensitive: 'cast' sorts before 'Optional'."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('typing', 'cast')))
        spec.rewrite_run(
            python(
                """
                from typing import Optional
                x = 1
                """,
                """
                from typing import cast, Optional
                x = 1
                """,
            )
        )

    def test_merge_into_unsorted_list_still_inserts_sorted(self):
        """When existing members are unsorted, the new member is still placed at
        its first sorted position; existing members are not reordered."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('os.path', 'exists')))
        spec.rewrite_run(
            python(
                """
                from os.path import join, abspath
                x = 1
                """,
                """
                from os.path import exists, join, abspath
                x = 1
                """,
            )
        )

    def test_merge_aliased_member_sorted_by_alias(self):
        """An aliased member is sorted by its alias (the bound name)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('typing', 'Final', alias='abc')))
        spec.rewrite_run(
            python(
                """
                from typing import Optional
                x = 1
                """,
                """
                from typing import Final as abc, Optional
                x = 1
                """,
            )
        )

    def test_new_from_import_added_after_existing_imports(self):
        """A brand-new 'from' import is placed after existing imports; import
        statements are not reordered among themselves."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('mmm', 'w')))
        spec.rewrite_run(
            python(
                """
                from aaa import x
                from zzz import y
                z = 1
                """,
                """
                from aaa import x
                from zzz import y
                from mmm import w
                z = 1
                """,
            )
        )
