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

"""Structural tests for Py2 simple statements: raise / assert / del / yield / global,
and the bare-keyword shortcuts that parso emits for ``pass`` / ``break`` etc."""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor


def _stmt(source: str):
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()
    return cu.statements[0]


class TestBareKeywords:
    @pytest.mark.parametrize("src,cls", [
        ("pass\n",     py.Pass),
        ("break\n",    j.Break),
        ("continue\n", j.Continue),
        ("return\n",   j.Return),
    ])
    def test_bare(self, src, cls):
        assert isinstance(_stmt(src), cls)

    def test_bare_raise(self):
        stmt = _stmt("raise\n")
        assert isinstance(stmt, j.Throw)
        assert isinstance(stmt.exception, j.Empty)

    def test_bare_yield(self):
        stmt = _stmt("yield\n")
        assert isinstance(stmt, py.StatementExpression)
        assert isinstance(stmt.statement, j.Yield)


class TestRaise:
    def test_raise_with_name(self):
        stmt = _stmt("raise E\n")
        assert isinstance(stmt, j.Throw)
        assert isinstance(stmt.exception, j.Identifier)
        assert stmt.exception.simple_name == "E"

    def test_raise_with_call(self):
        stmt = _stmt("raise E()\n")
        assert isinstance(stmt, j.Throw)
        assert isinstance(stmt.exception, j.MethodInvocation)

    def test_py2_multi_arg_raise(self):
        # Py2 `raise E, v, tb`. Wrapped in a tuple tagged with the
        # RaiseTuple marker so the printer renders the legacy syntax.
        stmt = _stmt("raise E, v, tb\n")
        assert isinstance(stmt, j.Throw)
        marker_names = [type(m).__name__ for m in stmt.markers.markers]
        assert "RaiseTuple" in marker_names
        assert isinstance(stmt.exception, py.CollectionLiteral)


class TestAssert:
    def test_simple(self):
        stmt = _stmt("assert x\n")
        assert isinstance(stmt, j.Assert)
        assert isinstance(stmt.condition, j.Identifier)
        assert stmt.detail is None

    def test_with_message(self):
        stmt = _stmt('assert x, "boom"\n')
        assert isinstance(stmt, j.Assert)
        assert stmt.detail is not None


class TestDel:
    def test_single(self):
        stmt = _stmt("del x\n")
        assert isinstance(stmt, py.Del)
        assert len(stmt.targets) == 1

    def test_multi(self):
        stmt = _stmt("del x, y, z\n")
        assert isinstance(stmt, py.Del)
        assert len(stmt.targets) == 3
        names = [t.simple_name for t in stmt.targets if isinstance(t, j.Identifier)]
        assert names == ["x", "y", "z"]


class TestGlobal:
    def test_single(self):
        stmt = _stmt("global x\n")
        assert isinstance(stmt, py.VariableScope)
        assert stmt.kind == py.VariableScope.Kind.GLOBAL
        assert [n.simple_name for n in stmt.names] == ["x"]

    def test_multi(self):
        stmt = _stmt("global x, y, z\n")
        assert isinstance(stmt, py.VariableScope)
        assert [n.simple_name for n in stmt.names] == ["x", "y", "z"]


class TestYield:
    def test_yield_value(self):
        stmt = _stmt("yield x\n")
        assert isinstance(stmt, py.StatementExpression)
        inner = stmt.statement
        assert isinstance(inner, j.Yield)
        assert isinstance(inner.value, j.Identifier)
        assert inner.value.simple_name == "x"


class TestSimpleStmtRegressions:
    @pytest.mark.parametrize("src", [
        "raise E\n",
        "raise SomeError()\n",
        "assert x > 0\n",
        "assert items, 'no items'\n",
        "del a, b\n",
        "global counter\n",
        "yield value\n",
    ])
    def test_no_placeholders(self, src):
        from tests.python.py27.expressions_test import _collect_placeholders
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        assert list(_collect_placeholders(stmt)) == [], src
