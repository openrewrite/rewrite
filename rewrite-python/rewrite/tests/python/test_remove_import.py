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

from rewrite import ExecutionContext, InMemoryExecutionContext
from rewrite.java import J
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor


def _assert_ids_accessible(source_file):
    """Touch every node's public `.id`; raises if any `_id` holds a UUID instead of an int."""
    class IdWalker(PythonVisitor[ExecutionContext]):
        def pre_visit(self, tree, p):
            assert tree.id is not None
            return tree

    IdWalker().visit(source_file, InMemoryExecutionContext())


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

    def test_keep_aliased_from_import_when_alias_used(self):
        """Keep 'from typing import List as L' while L is used."""
        class RemoveListVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='typing',
                    name='List',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveListVisitor()))
        spec.rewrite_run(
            python(
                """
                from typing import List as L
                x = L
                """,
            )
        )

    def test_remove_aliased_from_import_when_alias_unused(self):
        """Remove 'from typing import List as L' when L is not used."""
        class RemoveListVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='typing',
                    name='List',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveListVisitor()))
        spec.rewrite_run(
            python(
                """
                from typing import List as L
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_remove_aliased_from_import_even_when_imported_name_used(self):
        """A use of the bare imported name is some other binding and must not
        keep the aliased import alive."""
        class RemoveListVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='typing',
                    name='List',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveListVisitor()))
        spec.rewrite_run(
            python(
                """
                from typing import List as L
                List = [1]
                """,
                """
                List = [1]
                """,
            )
        )

    def test_mixed_multi_import_removes_only_unreferenced_bindings(self):
        """Only the entry whose bound name is unreferenced is removed."""
        class RemoveListVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='typing',
                    name='List',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveListVisitor()))
        spec.rewrite_run(
            python(
                """
                from typing import List, List as L
                x = L
                """,
                """
                from typing import List as L
                x = L
                """,
            )
        )

    def test_keep_aliased_direct_import_when_alias_used(self):
        """Keep 'import numpy as np' while np is used."""
        class RemoveNumpyVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='numpy',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveNumpyVisitor()))
        spec.rewrite_run(
            python(
                """
                import numpy as np
                x = np
                """,
            )
        )

    def test_remove_aliased_direct_import_when_alias_unused(self):
        """Remove 'import numpy as np' when np is not used."""
        class RemoveNumpyVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module='numpy',
                    only_if_unused=True
                ))
                return super().visit_compilation_unit(cu, p)

        spec = RecipeSpec(recipe=from_visitor(RemoveNumpyVisitor()))
        spec.rewrite_run(
            python(
                """
                import numpy as np
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_partial_from_import_removal_keeps_ids_accessible(self):
        """Removing one name from a from-import rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
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
                after_recipe=_assert_ids_accessible,
            )
        )

    def test_partial_direct_import_removal_keeps_ids_accessible(self):
        """Removing one module from 'import X, Y' rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
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
                import os, sys
                x = 1
                """,
                """
                import sys
                x = 1
                """,
                after_recipe=_assert_ids_accessible,
            )
        )


class TestCanonicalRemoveImport:
    """A requested (module, name) also matches an import by its canonical FQN
    (``os.path.join`` is canonically ``posixpath.join``), not just by its
    written path."""

    @staticmethod
    def _remover(module, name=None):
        class RemoveVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module=module, name=name, only_if_unused=False))
                return super().visit_compilation_unit(cu, p)

        return RecipeSpec(recipe=from_visitor(RemoveVisitor()))

    def test_remove_reexported_function_by_canonical_fqn(self):
        self._remover('posixpath', 'join').rewrite_run(
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

    def test_remove_reexported_class_by_canonical_fqn(self):
        self._remover('typing', 'Iterable').rewrite_run(
            python(
                """
                from collections.abc import Iterable
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_canonical_removal_keeps_other_names(self):
        self._remover('posixpath', 'join').rewrite_run(
            python(
                """
                from os.path import exists, join
                x = 1
                """,
                """
                from os.path import exists
                x = 1
                """,
            )
        )

    def test_remove_module_binding_by_canonical_fqn(self):
        """`from os import path` binds the module canonically named os.path."""
        self._remover('os.path').rewrite_run(
            python(
                """
                from os import path
                x = 1
                """,
                """
                x = 1
                """,
            )
        )

    def test_canonical_mismatch_is_not_removed(self):
        """os.path.exists is canonically genericpath.exists, not posixpath.exists."""
        self._remover('posixpath', 'exists').rewrite_run(
            python(
                """
                from os.path import exists
                x = 1
                """,
            )
        )

    def test_whole_module_removal_spares_canonically_related_members(self):
        """`typing` is the canonical home of many re-exports, so a whole-module
        request must match only bindings of the module itself — not every member
        that happens to be declared there."""
        self._remover('typing').rewrite_run(
            python(
                """
                from collections.abc import Iterable
                x = 1
                """,
            )
        )


class TestRemoveImportUsageScoping:
    """``only_if_unused`` counts every reference the enclosing scopes do not
    rebind, including references that appear only in annotations."""

    @staticmethod
    def _remove(module, name):
        class RemoveVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_remove_import(self, RemoveImportOptions(
                    module=module, name=name, only_if_unused=True))
                return super().visit_compilation_unit(cu, p)

        return from_visitor(RemoveVisitor())

    def test_keep_import_referenced_in_function_annotations(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove('typing', 'Deque'),
                              type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from typing import Deque


                    def f(q: Deque[int]) -> Deque[int]:
                        local: Deque[int] = q
                        return local
                    """,
                )
            )

    def test_keep_import_referenced_in_function_body(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove('os.path', 'join'),
                              type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from os.path import join


                    def f():
                        return join("a", "b")
                    """,
                )
            )

    def test_keep_import_referenced_outside_the_shadowing_function(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove('os.path', 'join'),
                              type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from os.path import join


                    def shadows():
                        join = "not a function"
                        return join


                    def uses():
                        return join("a", "b")
                    """,
                )
            )

    def test_remove_import_shadowed_in_every_function(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove('os.path', 'join'),
                              type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from os.path import join


                    def f():
                        join = "not a function"
                        return join
                    """,
                    """\
                    def f():
                        join = "not a function"
                        return join
                    """,
                )
            )


class TestRemoveImportScopeRules:
    """Usage counting follows Python's scoping, so a reference the interpreter
    resolves to the import keeps that import alive."""

    @staticmethod
    def _keeps(source):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=TestRemoveImportUsageScoping._remove('time', 'clock'),
                              type_attribution=type_attribution)
            spec.rewrite_run(python(source))

    def test_class_attribute_does_not_hide_method_usage(self):
        self._keeps(
            """\
            from time import clock


            class C:
                clock = 1

                def m(self):
                    return clock()
            """
        )

    def test_attribute_assignment_does_not_hide_usage(self):
        self._keeps(
            """\
            from time import clock


            class C:
                def m(self):
                    self.clock = 1
                    return clock()
            """
        )

    def test_decorator_usage_counts(self):
        self._keeps(
            """\
            from time import clock


            @clock
            def f():
                clock = 1
                return clock
            """
        )

    def test_parameter_default_usage_counts(self):
        self._keeps(
            """\
            from time import clock


            def f(x=clock):
                clock = 1
                return clock, x
            """
        )
