"""Tests for empty-diff detection in the test harness."""

from uuid import uuid4

import pytest

from rewrite import ExecutionContext, Markers
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


class _GhostChangeVisitor(PythonVisitor[ExecutionContext]):
    """A visitor that changes the AST markers without affecting printed output.

    By replacing the Markers object with one that has a new ID, the AST is
    technically modified (identity check fails) but the printed output remains
    identical. This simulates a recipe bug that modifies metadata without
    producing any visible change.
    """

    def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext):
        cu = super().visit_compilation_unit(cu, p)
        return cu.replace(markers=Markers(uuid4(), cu.markers.markers))


def test_empty_diff_raises_error():
    spec = RecipeSpec(recipe=from_visitor(_GhostChangeVisitor()))
    with pytest.raises(AssertionError, match="An empty diff was generated"):
        spec.rewrite_run(python("x = 1\n"))


def test_allow_empty_diff_suppresses_error():
    spec = RecipeSpec(
        recipe=from_visitor(_GhostChangeVisitor()),
        allow_empty_diff=True,
    )
    # Should not raise
    spec.rewrite_run(python("x = 1\n"))
