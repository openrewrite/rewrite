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
when a visitor replaces the child of one of these wrappers with a node that
no longer satisfies the wrapper's declared child type, the base visitor must
swap to the matching wrapper instead of silently constructing an invalid
tree (which only fails later, e.g. with a ClassCastException on RPC receive
in Java).
"""

from typing import Any, Optional

from rewrite import ExecutionContext, Markers, random_id
from rewrite.java.support_types import Expression, Space, Statement
from rewrite.java.tree import Yield
from rewrite.python.tree import Await, ExpressionStatement, StatementExpression, YieldFrom
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


class _YieldFromToAwaitVisitor(PythonVisitor[ExecutionContext]):
    """Mimics recipes that migrate `yield from expr` to `await expr`.

    `Py.Await` is an `Expression` but not a `Statement`, so the enclosing
    `Py.StatementExpression` produced by the parser for a bare `yield from`
    statement can no longer hold it.
    """

    def visit_yield(self, yield_stmt: Yield, p: ExecutionContext) -> Optional[Any]:
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


def _assert_no_invalid_wrappers(tree):
    for node in _walk(tree):
        if isinstance(node, StatementExpression):
            assert isinstance(node.statement, Statement), \
                f"StatementExpression wraps non-Statement {type(node.statement).__name__}"
        elif isinstance(node, ExpressionStatement):
            assert isinstance(node.expression, Expression), \
                f"ExpressionStatement wraps non-Expression {type(node.expression).__name__}"


def _walk(tree):
    import dataclasses
    from rewrite.tree import Tree

    def walk_val(val):
        if isinstance(val, Tree):
            yield val
            for f in dataclasses.fields(val):
                yield from walk_val(getattr(val, f.name, None))
        elif isinstance(val, (list, tuple)):
            for v in val:
                yield from walk_val(v)
        elif dataclasses.is_dataclass(val) and not isinstance(val, type):
            for f in dataclasses.fields(val):
                yield from walk_val(getattr(val, f.name, None))

    yield from walk_val(tree)


def test_statement_expression_rewrapped_when_child_becomes_expression():
    """A bare `yield from` statement whose Yield is replaced by Await must not
    leave an Await inside a StatementExpression."""
    spec = RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor()))
    spec.rewrite_run(
        python(
            """\
            def coro():
                yield from task()
            """,
            """\
            def coro():
                await task()
            """,
            after_recipe=_assert_no_invalid_wrappers,
        )
    )


def test_statement_expression_in_expression_position_rewrapped():
    """`x = yield from expr` puts the StatementExpression in expression
    position; the replacement must stay a valid Expression there too."""
    spec = RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor()))
    spec.rewrite_run(
        python(
            """\
            def coro():
                x = yield from task()
            """,
            """\
            def coro():
                x = await task()
            """,
            after_recipe=_assert_no_invalid_wrappers,
        )
    )


class _AwaitToYieldFromVisitor(PythonVisitor[ExecutionContext]):
    """The mirror case: replaces `await expr` with `yield from expr`.

    `J.Yield` is a `Statement` but not an `Expression`, so the enclosing
    `Py.ExpressionStatement` produced by the parser for a bare `await`
    statement can no longer hold it.
    """

    def visit_await(self, await_: Await, p: ExecutionContext) -> Optional[Any]:
        await_ = super().visit_await(await_, p)
        if isinstance(await_, Await):
            return Yield(
                random_id(),
                await_.prefix,
                await_.markers,
                False,
                YieldFrom(
                    random_id(),
                    Space([], ' '),
                    Markers.EMPTY,
                    await_.expression,
                    await_.type,
                ),
            )
        return await_


def test_expression_statement_rewrapped_when_child_becomes_statement():
    """A bare `await` statement whose Await is replaced by Yield must not
    leave a Yield inside an ExpressionStatement."""
    spec = RecipeSpec(recipe=from_visitor(_AwaitToYieldFromVisitor()))
    spec.rewrite_run(
        python(
            """\
            async def coro():
                await task()
            """,
            """\
            async def coro():
                yield from task()
            """,
            after_recipe=_assert_no_invalid_wrappers,
        )
    )


def test_plain_yield_untouched():
    """Plain `yield` (no `yield from`) keeps its StatementExpression wrapper."""
    spec = RecipeSpec(recipe=from_visitor(_YieldFromToAwaitVisitor()))
    spec.rewrite_run(
        python(
            """\
            def gen():
                yield 1
            """,
            after_recipe=_assert_no_invalid_wrappers,
        )
    )
