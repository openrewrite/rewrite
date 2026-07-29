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

"""Tests for ExpressionStatement/StatementExpression wrapper normalization.

Regression tests for https://github.com/openrewrite/rewrite/issues/8322:
when a visitor replaces the child of one of these wrappers with a node of a
different kind, the base visitor must re-derive the wrapping instead of
silently constructing an invalid tree (which only fails later, e.g. with a
ClassCastException on RPC receive in Java).
"""

from typing import Optional

from rewrite import ExecutionContext, InMemoryExecutionContext, Markers, random_id
from rewrite.java import J
from rewrite.java.support_types import Expression, JLeftPadded, Space, Statement
from rewrite.java.tree import Assignment, Identifier, Literal, Yield
from rewrite.python.tree import Await, ExpressionStatement, StatementExpression, YieldFrom
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


def _assert_wrappers_well_formed(source_file):
    """Every wrapper must hold a child of the kind it exists to adapt."""

    class WrapperWalker(PythonVisitor[ExecutionContext]):
        def pre_visit(self, tree, p):
            if isinstance(tree, StatementExpression):
                child = tree.statement
                assert isinstance(child, Statement), \
                    f"StatementExpression wraps non-Statement {type(child).__name__}"
            elif isinstance(tree, ExpressionStatement):
                child = tree.expression
                assert isinstance(child, Expression), \
                    f"ExpressionStatement wraps non-Expression {type(child).__name__}"
            else:
                return tree
            assert not isinstance(child, (StatementExpression, ExpressionStatement)), \
                f"{type(tree).__name__} redundantly wraps {type(child).__name__}"
            return tree

    WrapperWalker().visit(source_file, InMemoryExecutionContext())


class _YieldFromToAwaitVisitor(PythonVisitor[ExecutionContext]):
    """Mimics recipes that migrate `yield from expr` to `await expr`.

    `Py.Await` is an `Expression` but not a `Statement`, so the enclosing
    `Py.StatementExpression` produced by the parser for a bare `yield from`
    statement can no longer hold it.
    """

    def visit_yield(self, yield_stmt: Yield, p: ExecutionContext) -> Optional[J]:
        yield_stmt = super().visit_yield(yield_stmt, p)
        if isinstance(yield_stmt, Yield) and isinstance(yield_stmt.value, YieldFrom):
            yield_from = yield_stmt.value
            return Await(
                random_id(),
                yield_stmt.prefix,
                yield_stmt.markers,
                yield_from.expression,
                yield_from.type,
            )
        return yield_stmt


def test_statement_expression_rewrapped_when_child_becomes_expression():
    """A bare `yield from` statement whose Yield is replaced by Await must not
    leave an Await inside a StatementExpression."""
    RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor())).rewrite_run(
        python(
            """\
            def coro():
                yield from task()
            """,
            """\
            def coro():
                await task()
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


def test_statement_expression_in_expression_position_rewrapped():
    """`x = yield from expr` puts the StatementExpression in expression
    position; the replacement must stay a valid Expression there too."""
    RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor())).rewrite_run(
        python(
            """\
            def coro():
                x = yield from task()
            """,
            """\
            def coro():
                x = await task()
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


class _AwaitToYieldFromVisitor(PythonVisitor[ExecutionContext]):
    """The mirror case: replaces `await expr` with `yield from expr`.

    `J.Yield` is a `Statement` but not an `Expression`, so the enclosing
    `Py.ExpressionStatement` produced by the parser for a bare `await`
    statement can no longer hold it.
    """

    def visit_await(self, await_: Await, p: ExecutionContext) -> Optional[J]:
        await_ = super().visit_await(await_, p)
        if isinstance(await_, Await):
            return Yield(
                random_id(),
                await_.prefix,
                await_.markers,
                False,
                YieldFrom(
                    random_id(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    await_.expression,
                    await_.type,
                ),
            )
        return await_


def test_expression_statement_rewrapped_when_child_becomes_statement():
    """A bare `await` statement whose Await is replaced by Yield must not
    leave a Yield inside an ExpressionStatement."""
    RecipeSpec(recipe=from_visitor(_AwaitToYieldFromVisitor())).rewrite_run(
        python(
            """\
            async def coro():
                await task()
            """,
            """\
            async def coro():
                yield from task()
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


class _YieldToAssignmentVisitor(PythonVisitor[ExecutionContext]):
    """Replaces `yield <n>` with `a = <n>`.

    `J.Assignment` is both an `Expression` and a `Statement`, so it needs no
    wrapper at all. Left inside the `Py.StatementExpression`, the printer emits
    the walrus form `a := 1`, which is not valid in statement position.
    """

    def visit_yield(self, yield_stmt: Yield, p: ExecutionContext) -> Optional[J]:
        yield_stmt = super().visit_yield(yield_stmt, p)
        if isinstance(yield_stmt, Yield) and isinstance(yield_stmt.value, Literal):
            return Assignment(
                random_id(),
                yield_stmt.prefix,
                yield_stmt.markers,
                Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], 'a', None, None),
                JLeftPadded(Space.SINGLE_SPACE, yield_stmt.value.replace(prefix=Space.SINGLE_SPACE), Markers.EMPTY),
                None,
            )
        return yield_stmt


def test_dual_kind_replacement_drops_the_wrapper():
    """A replacement that is already both Expression and Statement needs no
    wrapper; keeping one makes the printer emit `a := 1`."""
    RecipeSpec(recipe=from_visitor(_YieldToAssignmentVisitor())).rewrite_run(
        python(
            """\
            def gen():
                yield 1
            """,
            """\
            def gen():
                a = 1
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


class _PreWrappedAwaitVisitor(_YieldFromToAwaitVisitor):
    """Returns an already-wrapped replacement, as a recipe reasonably might."""

    def visit_yield(self, yield_stmt: Yield, p: ExecutionContext) -> Optional[J]:
        replacement = super().visit_yield(yield_stmt, p)
        if isinstance(replacement, Await):
            return ExpressionStatement(random_id(), replacement)
        return replacement


def test_already_wrapped_replacement_is_not_double_wrapped():
    """An already-wrapped replacement must not gain a second wrapper."""
    RecipeSpec(recipe=from_visitor(_PreWrappedAwaitVisitor())).rewrite_run(
        python(
            """\
            def coro():
                yield from task()
            """,
            """\
            def coro():
                await task()
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


class _DeleteYieldVisitor(PythonVisitor[ExecutionContext]):
    """Deletes the wrapped child, which must take the wrapper with it."""

    def visit_yield(self, yield_stmt: Yield, p: ExecutionContext) -> Optional[J]:
        return None


def test_deleted_child_deletes_the_wrapper():
    """Returning None for the wrapped child removes the whole statement rather
    than leaving a wrapper holding None."""
    RecipeSpec(recipe=from_visitor(_DeleteYieldVisitor())).rewrite_run(
        python(
            """\
            def gen():
                yield 1
                print(2)
            """,
            """\
            def gen():
                print(2)
            """,
            after_recipe=_assert_wrappers_well_formed,
        )
    )


def test_plain_yield_untouched():
    """Plain `yield` (no `yield from`) keeps its StatementExpression wrapper."""
    RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor())).rewrite_run(
        python(
            """\
            def gen():
                yield 1
            """
        )
    )
