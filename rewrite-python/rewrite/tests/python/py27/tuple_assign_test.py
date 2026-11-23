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

"""Tuple destructuring assignment.

In Python 2 ``a, b = 1, 2`` is a common idiom. The left-hand and
right-hand sides are both bare ``testlist`` nodes (no parentheses); the
LST represents each as a :class:`py.CollectionLiteral` of kind ``TUPLE``
with the ``OmitParentheses`` marker on its elements container, so the
printer doesn't reintroduce the ``()`` wrappers.
"""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _stmt(src: str):
    return Py2ParserVisitor(src, "<test>", "2.7").parse().statements[0]


def _round_trip(src: str) -> str:
    return PythonPrinter().print(Py2ParserVisitor(src, "<test>", "2.7").parse())


class TestShape:
    def test_lhs_is_tuple(self):
        stmt = _stmt("a, b = 1, 2\n")
        assert isinstance(stmt, j.Assignment)
        assert isinstance(stmt.variable, py.CollectionLiteral)
        assert stmt.variable.kind == py.CollectionLiteral.Kind.TUPLE

    def test_rhs_is_tuple(self):
        stmt = _stmt("a, b = 1, 2\n")
        assert isinstance(stmt.assignment, py.CollectionLiteral)
        assert stmt.assignment.kind == py.CollectionLiteral.Kind.TUPLE

    def test_three_way_destructure(self):
        stmt = _stmt("a, b, c = 1, 2, 3\n")
        assert isinstance(stmt.variable, py.CollectionLiteral)
        assert len(stmt.variable.elements) == 3
        assert len(stmt.assignment.elements) == 3


class TestRoundTrip:
    @pytest.mark.parametrize("src", [
        "a, b = 1, 2\n",
        "a, b, c = 1, 2, 3\n",
        "a, b = c, d\n",
        "(a, b) = (1, 2)\n",
        "(a, b, c) = (1, 2, 3)\n",
        "first, second = pair\n",
        "k, v = items[0]\n",
        "head, tail = xs[0], xs[1:]\n",
    ])
    def test_round_trip(self, src):
        assert _round_trip(src) == src
