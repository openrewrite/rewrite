# Copyright 2026 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Structural tests for Py2 constructs added in Rounds 2-9:
classdef, try/with, atom literals, lambda, subscript slices, kwarg/star calls,
comprehensions, chained assignment, two-token comparison operators."""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.markers import TupleExceptClause


def _stmt(src: str):
    return Py2ParserVisitor(src, "<test>", "2.7").parse().statements[0]


def _expr(src: str):
    stmt = _stmt(src)
    if isinstance(stmt, py.ExpressionStatement):
        return stmt.expression
    if isinstance(stmt, j.Assignment):
        return stmt.assignment
    return stmt


# ---------------------------------------------------------------------------
# Classdef
# ---------------------------------------------------------------------------

class TestClassdef:
    def test_old_style_no_parens(self):
        s = _stmt("class Foo:\n    pass\n")
        assert isinstance(s, j.ClassDeclaration)
        assert s.name.simple_name == "Foo"
        assert s.implements is None

    def test_single_base(self):
        s = _stmt("class Foo(Base):\n    pass\n")
        assert isinstance(s, j.ClassDeclaration)
        assert s.implements is not None
        assert len(s.implements) == 1

    def test_multi_base(self):
        s = _stmt("class Foo(A, B):\n    pass\n")
        assert s.implements is not None
        assert len(s.implements) == 2

    def test_empty_parens(self):
        s = _stmt("class Foo():\n    pass\n")
        # Empty paren list still produces a JContainer (with Empty inside).
        assert s.implements is not None

    def test_decorated(self):
        s = _stmt("@d\nclass Foo:\n    pass\n")
        assert isinstance(s, j.ClassDeclaration)
        assert len(s.leading_annotations) == 1


# ---------------------------------------------------------------------------
# Try / With
# ---------------------------------------------------------------------------

class TestTry:
    def test_simple(self):
        s = _stmt("try:\n    a\nexcept:\n    b\n")
        assert isinstance(s, j.Try)
        assert len(s.catches) == 1

    def test_typed(self):
        s = _stmt("try:\n    a\nexcept E:\n    b\n")
        assert isinstance(s, j.Try)
        assert len(s.catches) == 1

    def test_as_form(self):
        s = _stmt("try:\n    a\nexcept E as e:\n    b\n")
        assert isinstance(s, j.Try)
        names = [type(m).__name__ for m in s.catches[0].markers.markers]
        # `as` form should NOT carry the TupleExceptClause marker.
        assert "TupleExceptClause" not in names

    def test_py2_comma_form(self):
        s = _stmt("try:\n    a\nexcept E, e:\n    b\n")
        assert isinstance(s, j.Try)
        names = [type(m).__name__ for m in s.catches[0].markers.markers]
        assert "TupleExceptClause" in names

    def test_multiple_catches(self):
        s = _stmt("try:\n    a\nexcept E:\n    b\nexcept F:\n    c\n")
        assert isinstance(s, j.Try)
        assert len(s.catches) == 2

    def test_finally_only(self):
        s = _stmt("try:\n    a\nfinally:\n    b\n")
        assert isinstance(s, j.Try)
        assert s.finally_ is not None or s.padding.finally_ is not None

    def test_full(self):
        s = _stmt(
            "try:\n    a\n"
            "except E:\n    b\n"
            "else:\n    c\n"
            "finally:\n    d\n"
        )
        # The else clause makes it a TrailingElseWrapper.
        assert isinstance(s, py.TrailingElseWrapper)


class TestWith:
    def test_single_context(self):
        s = _stmt("with foo:\n    pass\n")
        assert isinstance(s, j.Try)
        assert s.resources is not None

    def test_with_as(self):
        s = _stmt("with foo as f:\n    pass\n")
        assert isinstance(s, j.Try)
        assert len(s.resources) == 1

    def test_multi_with(self):
        s = _stmt("with foo as f, bar as b:\n    pass\n")
        assert isinstance(s, j.Try)
        assert len(s.resources) == 2


# ---------------------------------------------------------------------------
# Atom literals
# ---------------------------------------------------------------------------

class TestLiterals:
    def test_list(self):
        e = _expr("x = [1, 2, 3]\n")
        assert isinstance(e, py.CollectionLiteral)
        assert e.kind == py.CollectionLiteral.Kind.LIST
        assert len(e.elements) == 3

    def test_empty_list(self):
        e = _expr("x = []\n")
        assert isinstance(e, py.CollectionLiteral)
        assert e.kind == py.CollectionLiteral.Kind.LIST

    def test_dict(self):
        e = _expr("x = {1: 'a', 2: 'b'}\n")
        assert isinstance(e, py.DictLiteral)
        assert len(e.elements) == 2

    def test_empty_dict(self):
        e = _expr("x = {}\n")
        # Empty {} is a dict per Python convention.
        assert isinstance(e, py.DictLiteral)

    def test_set(self):
        e = _expr("x = {1, 2, 3}\n")
        assert isinstance(e, py.CollectionLiteral)
        assert e.kind == py.CollectionLiteral.Kind.SET

    def test_tuple(self):
        e = _expr("x = (1, 2, 3)\n")
        assert isinstance(e, py.CollectionLiteral)
        assert e.kind == py.CollectionLiteral.Kind.TUPLE

    def test_single_tuple(self):
        # `(1,)` is a 1-element tuple.
        e = _expr("x = (1,)\n")
        assert isinstance(e, py.CollectionLiteral)
        assert e.kind == py.CollectionLiteral.Kind.TUPLE


# ---------------------------------------------------------------------------
# Lambda
# ---------------------------------------------------------------------------

class TestLambda:
    def test_no_args(self):
        e = _expr("f = lambda: 1\n")
        assert isinstance(e, j.Lambda)

    def test_one_arg(self):
        e = _expr("f = lambda x: x + 1\n")
        assert isinstance(e, j.Lambda)
        assert isinstance(e.body, j.Binary)

    def test_multi_arg_with_default(self):
        e = _expr("f = lambda x, y=1: x * y\n")
        assert isinstance(e, j.Lambda)


# ---------------------------------------------------------------------------
# Subscript variants
# ---------------------------------------------------------------------------

class TestSlices:
    def test_basic_slice(self):
        e = _expr("x = a[1:5]\n")
        assert isinstance(e, j.ArrayAccess)
        # The index should be a py.Slice with start and stop set.
        dim = e.dimension
        idx = dim.index if hasattr(dim, 'index') else dim.padding.index.element
        assert isinstance(idx, py.Slice)
        assert idx.start is not None and idx.stop is not None

    def test_slice_with_step(self):
        e = _expr("x = a[::2]\n")
        assert isinstance(e, j.ArrayAccess)

    def test_multi_subscript(self):
        # ``a[i, j]`` is a TUPLE index.
        e = _expr("x = a[i, j]\n")
        assert isinstance(e, j.ArrayAccess)


# ---------------------------------------------------------------------------
# Call arg variants
# ---------------------------------------------------------------------------

class TestCallArgs:
    def test_keyword_arg(self):
        e = _expr("x = f(a=1)\n")
        assert isinstance(e, j.MethodInvocation)
        assert isinstance(e.arguments[0], py.NamedArgument)

    def test_mixed_args(self):
        e = _expr("x = f(a, b=2)\n")
        assert isinstance(e, j.MethodInvocation)
        assert isinstance(e.arguments[0], j.Identifier)
        assert isinstance(e.arguments[1], py.NamedArgument)

    def test_star_arg(self):
        e = _expr("x = f(*xs)\n")
        assert isinstance(e, j.MethodInvocation)
        assert isinstance(e.arguments[0], py.Star)
        assert e.arguments[0].kind == py.Star.Kind.LIST

    def test_double_star_arg(self):
        e = _expr("x = f(**kw)\n")
        assert isinstance(e, j.MethodInvocation)
        assert isinstance(e.arguments[0], py.Star)
        assert e.arguments[0].kind == py.Star.Kind.DICT


# ---------------------------------------------------------------------------
# Comprehensions
# ---------------------------------------------------------------------------

class TestComprehensions:
    def test_list_comp(self):
        e = _expr("x = [v for v in xs]\n")
        assert isinstance(e, py.ComprehensionExpression)
        assert e.kind == py.ComprehensionExpression.Kind.LIST
        assert len(e.clauses) == 1

    def test_list_comp_with_if(self):
        e = _expr("x = [v for v in xs if v > 0]\n")
        assert isinstance(e, py.ComprehensionExpression)
        assert e.clauses[0].conditions is not None
        assert len(e.clauses[0].conditions) == 1

    def test_set_comp(self):
        e = _expr("x = {v for v in xs}\n")
        assert isinstance(e, py.ComprehensionExpression)
        assert e.kind == py.ComprehensionExpression.Kind.SET

    def test_dict_comp(self):
        e = _expr("x = {k: v for k, v in items}\n")
        assert isinstance(e, py.ComprehensionExpression)
        assert e.kind == py.ComprehensionExpression.Kind.DICT

    def test_generator_in_parens(self):
        e = _expr("x = (v for v in xs)\n")
        assert isinstance(e, py.ComprehensionExpression)
        assert e.kind == py.ComprehensionExpression.Kind.GENERATOR

    def test_generator_as_call_arg(self):
        e = _expr("x = sum(v for v in xs)\n")
        assert isinstance(e, j.MethodInvocation)
        assert isinstance(e.arguments[0], py.ComprehensionExpression)


# ---------------------------------------------------------------------------
# Round 9 cleanup
# ---------------------------------------------------------------------------

class TestRound9:
    def test_chained_assign(self):
        s = _stmt("a = b = c = 0\n")
        assert isinstance(s, py.ChainedAssignment)

    def test_for_tuple_destructure(self):
        s = _stmt("for k, v in d:\n    pass\n")
        assert isinstance(s, j.ForEachLoop)

    def test_not_in(self):
        e = _expr("x = a not in b\n")
        assert isinstance(e, py.Binary)
        assert e.operator == py.Binary.Type.NotIn

    def test_is_not(self):
        e = _expr("x = a is not b\n")
        assert isinstance(e, py.Binary)
        assert e.operator == py.Binary.Type.IsNot
