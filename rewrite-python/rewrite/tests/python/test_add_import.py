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

import pytest

from rewrite import ExecutionContext, InMemoryExecutionContext
from rewrite.java import J
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor

from ._java_import_arm import java_add_import


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


def _add_import_visitor(arm, module, name=None, alias=None, only_if_referenced=False):
    """Build a visitor that registers a single add-import request."""
    if arm == 'java':
        return java_add_import(module, name, alias, only_if_referenced)

    class _V(PythonVisitor[ExecutionContext]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
            maybe_add_import(self, AddImportOptions(
                module=module,
                name=name,
                alias=alias,
                only_if_referenced=only_if_referenced,
            ))
            return super().visit_compilation_unit(cu, p)

    return _V()


class TestMaybeAddImport:
    """Tests for maybe_add_import scheduling via _after_visit."""

    def test_add_from_import(self, arm):
        """Add 'from os.path import join' to a file that uses join."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'join')))
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

    def test_add_direct_import(self, arm):
        """Add 'import os' to a file."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os')))
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

    def test_no_duplicate_import(self, arm):
        """Don't add an import that already exists."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os')))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
            )
        )

    def test_builtin_name_is_not_imported(self, arm):
        """A bare builtin name (e.g. from ChangeType retargeting to `list`) is not a module;
        adding an import for it is a no-op."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'list')))
        spec.rewrite_run(
            python(
                """
                x: list[int] = []
                """,
            )
        )

    def test_only_if_referenced(self, arm):
        """Don't add import when the name is not referenced and only_if_referenced=True."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
            )
        )

    def test_only_if_referenced_ignores_identifiers_in_imports(self, arm):
        """``pathlib`` occurs only as the module of the existing aliased import."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'pathlib', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                import pathlib as o

                x = o.sep
                """,
            )
        )

    def test_only_if_referenced_adds_when_referenced(self, arm):
        """The name is used, so the import is added."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                x = join('a', 'b')
                """,
                """
                from os.path import join
                x = join('a', 'b')
                """,
            )
        )

    def test_only_if_referenced_finds_a_reference_in_a_comprehension(self, arm):
        """The only reference is inside a comprehension, a Python-specific node."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                paths = [join(p) for p in ps]
                """,
                """
                from os.path import join
                paths = [join(p) for p in ps]
                """,
            )
        )

    def test_merge_into_existing_from_import(self, arm):
        """Merge a new name into an existing 'from X import ...' statement.

        The new member is inserted in case-insensitive alphabetical position
        ('exists' < 'join'), not appended.
        """
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
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

    def test_merge_into_existing_import_keeps_ids_accessible(self, arm):
        """Merging into an existing from-import rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
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
                after_recipe=_assert_ids_accessible,
            )
        )

    def test_merge_inserts_member_in_sorted_position(self, arm):
        """Insert before an existing member when it sorts earlier (the reported bug)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'ClassVar')))
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

    def test_merge_inserts_member_in_middle(self, arm):
        """Insert a member between two existing, already-sorted members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'ClassVar')))
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

    def test_merge_appends_when_alphabetically_last(self, arm):
        """Append a member when it sorts after all existing members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Final')))
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

    def test_merge_is_case_insensitive(self, arm):
        """Ordering is case-insensitive: 'cast' sorts before 'Optional'."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'cast')))
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

    def test_merge_into_unsorted_list_still_inserts_sorted(self, arm):
        """When existing members are unsorted, the new member is still placed at
        its first sorted position; existing members are not reordered."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
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

    def test_merge_aliased_member_sorted_by_alias(self, arm):
        """An aliased member is sorted by its alias (the bound name)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Final', alias='abc')))
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

    def test_new_from_import_added_after_existing_imports(self, arm):
        """A brand-new 'from' import is placed after existing imports; import
        statements are not reordered among themselves."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'mmm', 'w')))
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

    def test_skips_builtins_import(self, arm):
        """Names from the 'builtins' module are always available, so no import
        is added (e.g. 'from builtins import list' is redundant)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins', 'list')))
        spec.rewrite_run(
            python(
                """
                x: list = []
                """,
            )
        )

    def test_skips_builtins_direct_import(self, arm):
        """A direct 'import builtins' is likewise not added."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
            )
        )

    def test_adds_aliased_builtins_import(self, arm):
        """An explicit alias makes a builtins import meaningful, so it is added."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins', 'list', '_list')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                from builtins import list as _list
                x = 1
                """,
            )
        )


class TestCanonicalAddImportDedup:
    """An existing import already satisfies a requested (module, name) when
    its canonical FQN matches (``os.path.join`` is canonically
    ``posixpath.join``), not just when its written path does."""

    def test_canonical_request_matches_written_from_import(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'posixpath', 'join'))).rewrite_run(
            python(
                """
                from os.path import join
                x = join('a', 'b')
                """,
            )
        )

    def test_canonical_request_matches_written_class_import(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Iterable'))).rewrite_run(
            python(
                """
                from collections.abc import Iterable
                def f(x: Iterable): ...
                """,
            )
        )

    def test_canonical_match_requires_same_bound_name(self, arm):
        """An aliased binding does not satisfy a request for the plain name."""
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'posixpath', 'join'))).rewrite_run(
            python(
                """
                from os.path import join as j
                x = 1
                """,
                """
                from os.path import join as j
                from posixpath import join
                x = 1
                """,
            )
        )

    def test_canonical_mismatch_still_adds(self, arm):
        """os.path.exists is canonically genericpath.exists, so a posixpath
        request is a different symbol and gets its own import."""
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'posixpath', 'exists'))).rewrite_run(
            python(
                """
                from os.path import exists
                x = 1
                """,
                """
                from os.path import exists
                from posixpath import exists
                x = 1
                """,
            )
        )
