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

import pytest

from rewrite import ExecutionContext, InMemoryExecutionContext
from rewrite.java import J
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor

from ._java_import_arm import java_remove_import


def _assert_ids_accessible(source_file):
    """Touch every node's public `.id`; raises if any `_id` holds a UUID instead of an int."""
    class IdWalker(PythonVisitor[ExecutionContext]):
        def pre_visit(self, tree, p):
            assert tree.id is not None
            return tree

    IdWalker().visit(source_file, InMemoryExecutionContext())


@pytest.fixture(params=[
    pytest.param('native'),
    pytest.param('java', marks=pytest.mark.requires_java_rpc),
])
def arm(request):
    """Run each case against the Python visitor in-process and against the Java visitor over RPC."""
    if request.param == 'java':
        request.getfixturevalue('java_rpc')
    return request.param


def _remove_import_visitor(arm, module, name=None, only_if_unused=True):
    """Build a visitor that registers a single remove-import request."""
    if arm == 'java':
        return java_remove_import(module, name, only_if_unused)

    class _V(PythonVisitor[ExecutionContext]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
            maybe_remove_import(self, RemoveImportOptions(
                module=module,
                name=name,
                only_if_unused=only_if_unused,
            ))
            return super().visit_compilation_unit(cu, p)

    return _V()


class TestMaybeRemoveImport:
    """Tests for maybe_remove_import scheduling via _after_visit."""

    def test_remove_unused_from_import(self, arm):
        """Remove 'from os.path import join' when join is not used."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os.path', 'join', only_if_unused=False)))
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

    def test_remove_entire_direct_import(self, arm):
        """Remove 'import os' entirely."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os', only_if_unused=False)))
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

    def test_remove_one_name_from_multi_import(self, arm):
        """Remove 'join' from 'from os.path import join, exists'."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os.path', 'join', only_if_unused=False)))
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

    def test_keep_import_when_used(self, arm):
        """Don't remove an import when the name is still used and only_if_unused=True."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'os.path', 'join')))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = join("a", "b")
                """,
            )
        )

    def test_remove_import_when_only_shadowed_in_function(self, arm):
        """Remove import when the name is only used as a local variable shadowing it."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'os.path', 'join')),
                          type_attribution=True)
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

    def test_keep_import_when_used_at_module_level(self, arm):
        """Keep import when it's used at module level even if also shadowed in a function."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'os.path', 'join')),
                          type_attribution=True)
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

    def test_remove_direct_import_when_only_shadowed(self, arm):
        """Remove 'import os' when 'os' is only used as a local variable name."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'os')),
                          type_attribution=True)
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

    def test_no_change_when_import_not_present(self, arm):
        """No change when the import to remove doesn't exist."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os.path', 'join', only_if_unused=False)))
        spec.rewrite_run(
            python(
                """
                import sys
                x = 1
                """,
            )
        )

    def test_keep_aliased_from_import_when_alias_used(self, arm):
        """Keep 'from typing import List as L' while L is used."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'typing', 'List')))
        spec.rewrite_run(
            python(
                """
                from typing import List as L
                x = L
                """,
            )
        )

    def test_remove_aliased_from_import_when_alias_unused(self, arm):
        """Remove 'from typing import List as L' when L is not used."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'typing', 'List')))
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

    def test_remove_aliased_from_import_even_when_imported_name_used(self, arm):
        """A use of the bare imported name is some other binding and must not
        keep the aliased import alive."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'typing', 'List')))
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

    def test_mixed_multi_import_removes_only_unreferenced_bindings(self, arm):
        """Only the entry whose bound name is unreferenced is removed."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'typing', 'List')))
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

    def test_keep_aliased_direct_import_when_alias_used(self, arm):
        """Keep 'import numpy as np' while np is used."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'numpy')))
        spec.rewrite_run(
            python(
                """
                import numpy as np
                x = np
                """,
            )
        )

    def test_remove_aliased_direct_import_when_alias_unused(self, arm):
        """Remove 'import numpy as np' when np is not used."""
        spec = RecipeSpec(recipe=from_visitor(_remove_import_visitor(arm, 'numpy')))
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

    def test_partial_from_import_removal_keeps_ids_accessible(self, arm):
        """Removing one name from a from-import rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os.path', 'join', only_if_unused=False)))
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

    def test_partial_direct_import_removal_keeps_ids_accessible(self, arm):
        """Removing one module from 'import X, Y' rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
        spec = RecipeSpec(recipe=from_visitor(
            _remove_import_visitor(arm, 'os', only_if_unused=False)))
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
    def _remover(arm, module, name=None):
        return _remove_import_visitor(arm, module, name, only_if_unused=False)

    def test_remove_reexported_function_by_canonical_fqn(self, arm):
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'posixpath', 'join'))).rewrite_run(
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

    def test_remove_reexported_class_by_canonical_fqn(self, arm):
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'typing', 'Iterable'))).rewrite_run(
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

    def test_canonical_removal_keeps_other_names(self, arm):
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'posixpath', 'join'))).rewrite_run(
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

    def test_remove_module_binding_by_canonical_fqn(self, arm):
        """`from os import path` binds the module canonically named os.path."""
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'os.path'))).rewrite_run(
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

    def test_canonical_mismatch_is_not_removed(self, arm):
        """os.path.exists is canonically genericpath.exists, not posixpath.exists."""
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'posixpath', 'exists'))).rewrite_run(
            python(
                """
                from os.path import exists
                x = 1
                """,
            )
        )

    def test_whole_module_removal_spares_canonically_related_members(self, arm):
        """`typing` is the canonical home of many re-exports, so a whole-module
        request must match only bindings of the module itself — not every member
        that happens to be declared there."""
        RecipeSpec(recipe=from_visitor(self._remover(arm, 'typing'))).rewrite_run(
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
    def _remove(arm, module, name):
        return from_visitor(_remove_import_visitor(arm, module, name))

    def test_keep_import_referenced_in_function_annotations(self, arm):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove(arm, 'typing', 'Deque'),
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

    def test_keep_import_referenced_in_function_body(self, arm):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove(arm, 'os.path', 'join'),
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

    def test_keep_import_referenced_outside_the_shadowing_function(self, arm):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove(arm, 'os.path', 'join'),
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

    def test_remove_import_shadowed_in_every_function(self, arm):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=self._remove(arm, 'os.path', 'join'),
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
    def _keeps(arm, source):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=TestRemoveImportUsageScoping._remove(arm, 'time', 'clock'),
                              type_attribution=type_attribution)
            spec.rewrite_run(python(source))

    def test_class_attribute_does_not_hide_method_usage(self, arm):
        self._keeps(
            arm,
            """\
            from time import clock


            class C:
                clock = 1

                def m(self):
                    return clock()
            """
        )

    def test_attribute_assignment_does_not_hide_usage(self, arm):
        self._keeps(
            arm,
            """\
            from time import clock


            class C:
                def m(self):
                    self.clock = 1
                    return clock()
            """
        )

    def test_decorator_usage_counts(self, arm):
        self._keeps(
            arm,
            """\
            from time import clock


            @clock
            def f():
                clock = 1
                return clock
            """
        )

    def test_parameter_default_usage_counts(self, arm):
        self._keeps(
            arm,
            """\
            from time import clock


            def f(x=clock):
                clock = 1
                return clock, x
            """
        )
