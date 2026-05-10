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
