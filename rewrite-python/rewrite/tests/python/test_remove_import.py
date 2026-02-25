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

"""Tests for RemoveImport / maybe_remove_import."""

from rewrite import ExecutionContext
from rewrite.java import J
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor


class TestMaybeRemoveImport:
    """Tests for maybe_remove_import scheduling via _after_visit."""

    def test_remove_unused_from_import(self):
        """Remove 'from os.path import join' when join is not used."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_remove_entire_direct_import(self):
        """Remove 'import os' entirely."""
        class RemoveOsVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os',
                    only_if_unused=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveOsVisitor()))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_remove_one_name_from_multi_import(self):
        """Remove 'join' from 'from os.path import join, exists'."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                from os.path import join, exists
                x = 1
                """,
                """
                from os.path import exists
                x = 1
                """,
            )
        )

    def test_keep_import_when_used(self):
        """Don't remove an import when the name is still used and only_if_unused=True."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = join("a", "b")
                """,
            )
        )

    def test_remove_import_when_only_shadowed_in_function(self):
        """Remove import when the name is only used as a local variable shadowing it."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()), type_attribution=True)
        spec.rewrite_run(
            python(
                """\
                from os.path import join

                def foo():
                    join = "not a function"
                    return join
                """,
                """\
                def foo():
                    join = "not a function"
                    return join
                """,
            )
        )

    def test_keep_import_when_used_at_module_level(self):
        """Keep import when it's used at module level even if also shadowed in a function."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()), type_attribution=True)
        spec.rewrite_run(
            python(
                """\
                from os.path import join

                x = join("a", "b")

                def foo():
                    join = "shadowed"
                    return join
                """,
            )
        )

    def test_remove_direct_import_when_only_shadowed(self):
        """Remove 'import os' when 'os' is only used as a local variable name."""
        class RemoveOsVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveOsVisitor()), type_attribution=True)
        spec.rewrite_run(
            python(
                """\
                import os

                def foo():
                    os = "not a module"
                    return os
                """,
                """\
                def foo():
                    os = "not a module"
                    return os
                """,
            )
        )

    def test_no_change_when_import_not_present(self):
        """No change when the import to remove doesn't exist."""
        class RemoveJoinVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='os.path',
                    name='join',
                    only_if_unused=False
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveJoinVisitor()))
        spec.rewrite_run(
            python(
                """
                import sys
                x = 1
                """,
            )
        )
