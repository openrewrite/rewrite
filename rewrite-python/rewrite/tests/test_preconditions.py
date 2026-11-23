# Copyright 2025 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""In-process tests for the Preconditions wrapper.

These verify the wrapper semantics directly (no RPC). Wire-level tests for
the PrepareRecipe introspection live in tests/rpc/test_server.py.
"""

from typing import Any, List, Optional

import pytest

from rewrite import (
    Cursor,
    InMemoryExecutionContext,
    Preconditions,
    Recipe,
    SourceFile,
    Tree,
    TreeVisitor,
)
from rewrite.markers import SearchResult
from rewrite.preconditions import Check, RecipeCheck


class _RecordingVisitor(TreeVisitor[Tree, Any]):
    """Returns the same tree, recording each visit."""

    def __init__(self) -> None:
        self.calls: int = 0

    def visit(self, tree, p, parent=None):
        self.calls += 1
        return tree


class _MarkingVisitor(TreeVisitor[Tree, Any]):
    """Returns a sentinel different tree, simulating a precondition match."""

    def __init__(self) -> None:
        self.calls: int = 0
        self.last_tree = None

    def visit(self, tree, p, parent=None):
        self.calls += 1
        self.last_tree = tree
        # Return any non-identity object so the Check sees "modified".
        return object()


class _StubSourceFile(SourceFile):
    """Minimal SourceFile-like sentinel that bypasses the abstract API.

    SourceFile is abstract; we only need ``isinstance`` to succeed inside
    :meth:`Check.visit`. The Check wrapper does not call any of the abstract
    properties, so a bare subclass is safe for these tests.
    """

    def __init__(self):
        pass

    @property
    def id(self):
        raise NotImplementedError

    @property
    def markers(self):
        raise NotImplementedError

    @property
    def charset_name(self):
        return None

    @property
    def charset_bom_marked(self):
        return False

    @property
    def checksum(self):
        return None

    @property
    def file_attributes(self):
        return None

    @property
    def source_path(self):
        return None

    def is_acceptable(self, v, p):
        return True

    def printer(self, cursor):
        raise NotImplementedError


def _stub_source_file():
    return _StubSourceFile.__new__(_StubSourceFile)


def test_check_runs_editor_when_condition_marks_tree():
    condition = _MarkingVisitor()
    editor = _RecordingVisitor()

    wrapped = Preconditions.check(condition, editor)
    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()

    result = wrapped.visit(sf, ctx)

    assert condition.calls == 1
    assert editor.calls == 1
    # The editor returned the same tree, but the gate fired.
    assert result is sf


def test_check_skips_editor_when_condition_returns_identity():
    condition = _RecordingVisitor()  # returns same tree
    editor = _RecordingVisitor()

    wrapped = Preconditions.check(condition, editor)
    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()

    result = wrapped.visit(sf, ctx)

    assert condition.calls == 1
    assert editor.calls == 0, "editor should not run when precondition is identity"
    assert result is sf


def test_check_passes_through_for_non_source_file():
    """Preconditions only gate at the SourceFile root — nested trees pass through."""
    condition = _RecordingVisitor()
    editor = _RecordingVisitor()

    wrapped = Preconditions.check(condition, editor)
    not_a_source_file = object()
    ctx = InMemoryExecutionContext()

    wrapped.visit(not_a_source_file, ctx)

    assert condition.calls == 0
    assert editor.calls == 1


def test_recipe_check_uses_recipe_editor_as_condition():
    class _CheckRecipe(Recipe):
        @property
        def name(self):
            return "test.recipe.Check"

        @property
        def display_name(self):
            return "Check"

        @property
        def description(self):
            return "Marks every file."

        def editor(self):
            return _MarkingVisitor()

    editor = _RecordingVisitor()
    wrapped = Preconditions.check(_CheckRecipe(), editor)

    assert isinstance(wrapped, RecipeCheck)

    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()
    wrapped.visit(sf, ctx)

    assert editor.calls == 1


def test_recipe_check_rejects_scanning_recipe():
    from rewrite import ScanningRecipe

    class _ScanRecipe(ScanningRecipe):
        @property
        def name(self):
            return "test.recipe.Scan"

        @property
        def display_name(self):
            return "Scan"

        @property
        def description(self):
            return "Scan."

        def initial_value(self, ctx):
            return set()

    with pytest.raises(ValueError, match="ScanningRecipe is not supported"):
        Preconditions.check(_ScanRecipe(), _RecordingVisitor())


def test_or_short_circuits_on_first_match():
    matching = _MarkingVisitor()
    nonmatching = _RecordingVisitor()
    editor = _RecordingVisitor()

    composite = Preconditions.or_(matching, nonmatching)
    wrapped = Preconditions.check(composite, editor)

    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()
    wrapped.visit(sf, ctx)

    assert matching.calls == 1
    assert nonmatching.calls == 0, "OR should short-circuit after first match"
    assert editor.calls == 1


def test_or_skips_editor_when_no_operand_matches():
    a = _RecordingVisitor()
    b = _RecordingVisitor()
    editor = _RecordingVisitor()

    composite = Preconditions.or_(a, b)
    wrapped = Preconditions.check(composite, editor)

    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()
    result = wrapped.visit(sf, ctx)

    assert a.calls == 1
    assert b.calls == 1
    assert editor.calls == 0
    assert result is sf


def test_and_runs_editor_only_when_all_match():
    matching_a = _MarkingVisitor()
    matching_b = _MarkingVisitor()
    nonmatching = _RecordingVisitor()
    editor = _RecordingVisitor()

    # All match -> editor runs.
    wrapped = Preconditions.check(Preconditions.and_(matching_a, matching_b), editor)
    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()
    wrapped.visit(sf, ctx)
    assert editor.calls == 1

    # One non-matching short-circuits to false.
    editor2 = _RecordingVisitor()
    wrapped2 = Preconditions.check(
        Preconditions.and_(matching_a, nonmatching), editor2
    )
    wrapped2.visit(_stub_source_file(), ctx)
    assert editor2.calls == 0


def test_not_inverts_match():
    matching = _MarkingVisitor()
    nonmatching = _RecordingVisitor()
    editor = _RecordingVisitor()

    # not(match) -> editor skipped.
    Preconditions.check(Preconditions.not_(matching), editor).visit(
        _stub_source_file(), InMemoryExecutionContext()
    )
    assert editor.calls == 0

    # not(no-match) -> editor runs.
    editor2 = _RecordingVisitor()
    Preconditions.check(Preconditions.not_(nonmatching), editor2).visit(
        _stub_source_file(), InMemoryExecutionContext()
    )
    assert editor2.calls == 1


def test_or_with_recipe_ref_without_local_visitor_short_circuits():
    """A bare RecipeRef without a local_visitor short-circuits to "matches"
    in-process so the wrapped editor still runs."""
    from rewrite.preconditions import RecipeRef

    bare = RecipeRef("org.openrewrite.java.search.HasMethod", {"methodPattern": "*..* a()"})
    bare2 = RecipeRef("org.openrewrite.java.search.HasMethod", {"methodPattern": "*..* b()"})

    editor = _RecordingVisitor()
    wrapped = Preconditions.check(Preconditions.or_(bare, bare2), editor)

    wrapped.visit(_stub_source_file(), InMemoryExecutionContext())
    assert editor.calls == 1


def test_recipe_ref_with_local_visitor_evaluates_for_real():
    """Helpers like uses_method bundle a native local visitor so unit tests
    without an active RPC connection still see real filtering. The stub
    source file has no method invocations, so the gate fails and the
    editor is skipped."""
    from rewrite.python.preconditions import uses_method

    editor = _RecordingVisitor()
    wrapped = Preconditions.check(uses_method("*..* nope()"), editor)

    wrapped.visit(_stub_source_file(), InMemoryExecutionContext())
    assert editor.calls == 0


def test_helpers_populate_local_visitor():
    """Spot-check that helpers bundle a native visitor for offline eval."""
    from rewrite.python.preconditions import (
        find_methods,
        find_types,
        has_source_path,
        uses_method,
        uses_type,
    )

    assert has_source_path("**/*.py").local_visitor is not None
    assert uses_method("*..* a(..)").local_visitor is not None
    assert uses_type("foo.Bar").local_visitor is not None
    assert find_methods("*..* a(..)").local_visitor is not None
    assert find_types("foo.Bar").local_visitor is not None


def test_or_requires_at_least_two_operands():
    with pytest.raises(ValueError, match="at least two operands"):
        Preconditions.or_(_RecordingVisitor())


def test_check_is_acceptable_ands_both_legs():
    class _Reject(TreeVisitor[Tree, Any]):
        def is_acceptable(self, sf, p):
            return False

    accept = _RecordingVisitor()
    reject = _Reject()
    sf = _stub_source_file()
    ctx = InMemoryExecutionContext()

    # condition rejects -> wrapper rejects
    assert Preconditions.check(reject, accept).is_acceptable(sf, ctx) is False
    # editor rejects -> wrapper rejects
    assert Preconditions.check(accept, reject).is_acceptable(sf, ctx) is False
    # both accept -> wrapper accepts
    assert Preconditions.check(accept, accept).is_acceptable(sf, ctx) is True
