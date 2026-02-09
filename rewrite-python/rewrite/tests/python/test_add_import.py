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
        """Merge a new name into an existing 'from X import ...' statement."""
        class AddExistsVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_add_import(self, AddImportOptions(
                    module='os.path',
                    name='exists',
                    only_if_referenced=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(AddExistsVisitor()))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from os.path import join, exists
                x = 1
                """,
            )
        )
