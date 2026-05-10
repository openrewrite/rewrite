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

"""Precondition wrappers for recipe editors and scanners.

Mirrors Java's ``org.openrewrite.Preconditions``. A recipe author writes::

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        return Preconditions.check(uses_method("array tostring()"), MyVisitor())

The framework introspects the wrapper at PrepareRecipe time and emits the
``check`` visitor's identity in the ``editPreconditions`` slot of the
``PrepareRecipeResponse``. The Java side then evaluates the precondition
against the local source file *before* dispatching the visit RPC, skipping
the entire RPC round trip when the precondition does not match.

When a recipe is supplied as the ``check``, its ``editor()`` is used as the
condition visitor. ``ScanningRecipe`` is rejected because its scan phase
needs accumulator state that a stateless precondition check cannot provide.
"""

from __future__ import annotations

from typing import Any, Optional, Union, TYPE_CHECKING, cast

from rewrite.execution import DelegatingExecutionContext, ExecutionContext
from rewrite.tree import SourceFile, Tree
from rewrite.visitor import Cursor, TreeVisitor

if TYPE_CHECKING:
    from rewrite.recipe import Recipe


class _DataTableSuppressingExecutionContext(DelegatingExecutionContext):
    """Suppresses data-table writes from precondition visitors.

    Mirrors Java's ``Preconditions.DataTableSuppressingExecutionContextView``.
    Precondition visitors are typically search recipes (``HasMethod``,
    ``HasType``) that would otherwise emit data-table rows on every file
    they're checked against. We want to keep those rows scoped to actual
    recipe runs, not preconditions.
    """

    def __init__(self, delegate: ExecutionContext) -> None:
        super().__init__(delegate)


def _suppressing(ctx: ExecutionContext) -> ExecutionContext:
    if isinstance(ctx, _DataTableSuppressingExecutionContext):
        return ctx
    return _DataTableSuppressingExecutionContext(ctx)


class Check(TreeVisitor[Tree, ExecutionContext]):
    """Wrapper visitor that gates ``v`` on whether ``check`` mutates the tree.

    Semantics mirror Java's ``Preconditions.Check``: the precondition runs
    first; if it returns a different tree (i.e. it added a ``SearchResult``
    marker), the wrapped visitor runs. Otherwise the wrapped visitor is
    skipped and the original tree is returned unchanged.

    For non-``SourceFile`` trees the precondition is bypassed — preconditions
    are designed to evaluate at the root and may assume the root layout.

    The framework also introspects this wrapper at PrepareRecipe time and
    promotes ``check`` to the ``editPreconditions`` wire slot so that the
    Java host can evaluate the precondition locally and skip the visit RPC
    entirely. In that case the wrapped ``v`` is what runs Python-side, so
    the ``visit`` method below is the fallback for in-process callers (tests,
    direct invocation) and for non-RPC dispatch paths.
    """

    def __init__(
        self,
        check: TreeVisitor[Any, ExecutionContext],
        v: TreeVisitor[Any, ExecutionContext],
    ) -> None:
        self._check = check
        self._v = v

    @property
    def check(self) -> TreeVisitor[Any, ExecutionContext]:
        return self._check

    @property
    def wrapped(self) -> TreeVisitor[Any, ExecutionContext]:
        return self._v

    def is_acceptable(self, source_file: SourceFile, p: ExecutionContext) -> bool:
        return self._check.is_acceptable(source_file, p) and self._v.is_acceptable(
            source_file, p
        )

    def visit(
        self,
        tree: Optional[Tree],
        p: ExecutionContext,
        parent: Optional[Cursor] = None,
    ) -> Optional[Tree]:
        if not isinstance(tree, SourceFile):
            return self._v.visit(tree, p, parent)
        condition_after = self._check.visit(tree, _suppressing(p), parent)
        if condition_after is not tree:
            return self._v.visit(tree, p, parent)
        return tree


class RecipeCheck(Check):
    """A ``Check`` whose precondition visitor is a recipe's editor.

    Stores the originating ``Recipe`` so the framework can resolve the
    precondition's wire identity (the recipe's ``edit:<id>`` visitor name)
    at PrepareRecipe time without re-running ``recipe.editor()``.
    """

    def __init__(
        self,
        recipe: "Recipe",
        v: TreeVisitor[Any, ExecutionContext],
    ) -> None:
        # Local import: Recipe imports from rewrite.preconditions for the
        # forward reference, so we resolve the editor at construction time.
        from rewrite.recipe import ScanningRecipe

        if isinstance(recipe, ScanningRecipe):
            raise ValueError(
                "ScanningRecipe is not supported as a precondition: scan-phase "
                "recipes accumulate cross-file state, which a stateless "
                "precondition check cannot provide."
            )
        super().__init__(recipe.editor(), v)
        self._recipe = recipe

    @property
    def recipe(self) -> "Recipe":
        return self._recipe


class Preconditions:
    """Static helpers for wrapping editors with precondition checks.

    Usage::

        from rewrite.preconditions import Preconditions
        from rewrite.python.preconditions import uses_method

        class ReplaceArrayTostring(Recipe):
            def editor(self):
                return Preconditions.check(
                    uses_method("array tostring()"),
                    self._tostring_visitor(),
                )
    """

    @staticmethod
    def check(
        check: Union[TreeVisitor[Any, ExecutionContext], "Recipe"],
        v: TreeVisitor[Any, ExecutionContext],
    ) -> Check:
        """Wrap ``v`` with a precondition check.

        Accepts either a ``TreeVisitor`` or a ``Recipe`` for ``check``. When a
        ``Recipe`` is supplied, its ``editor()`` is used as the precondition
        visitor (``ScanningRecipe`` is rejected — see :class:`RecipeCheck`).

        The return is always a :class:`Check` instance. Recipe-form returns a
        :class:`RecipeCheck` so the framework can recover the originating
        recipe at wire-serialization time.
        """
        from rewrite.recipe import Recipe

        if isinstance(check, Recipe):
            return RecipeCheck(check, v)
        return Check(cast(TreeVisitor[Any, ExecutionContext], check), v)
