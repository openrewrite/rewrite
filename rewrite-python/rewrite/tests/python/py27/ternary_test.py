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

"""Conditional expressions (``a if b else c``).

parso emits the ternary as a flat ``test`` node with five children. The
visitor converts it to :class:`j.Ternary`, whose source order is
``true_part if condition else false_part``.
"""

import pytest

from rewrite.java import tree as j
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _expr(src: str):
    cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
    stmt = cu.statements[0]
    return stmt.assignment if isinstance(stmt, j.Assignment) else stmt


def _round_trip(src: str) -> str:
    return PythonPrinter().print(Py2ParserVisitor(src, "<test>", "2.7").parse())


def test_simple_ternary_shape():
    e = _expr("x = a if b else c\n")
    assert isinstance(e, j.Ternary)
    assert isinstance(e.true_part, j.Identifier)
    assert e.true_part.simple_name == "a"
    assert isinstance(e.condition, j.Identifier)
    assert e.condition.simple_name == "b"
    assert isinstance(e.false_part, j.Identifier)
    assert e.false_part.simple_name == "c"


@pytest.mark.parametrize("src", [
    "x = a if b else c\n",
    "x = 1 if cond else 0\n",
    "x = (a + 1) if b > 0 else (a - 1)\n",
    "x = f(a) if a is not None else f(b)\n",
])
def test_round_trip(src):
    assert _round_trip(src) == src


def test_nested_ternary():
    src = "x = a if c1 else b if c2 else d\n"
    e = _expr(src)
    assert isinstance(e, j.Ternary)
    # Right-associative: a if c1 else (b if c2 else d)
    assert isinstance(e.false_part, j.Ternary)
    assert _round_trip(src) == src


def test_ternary_as_call_arg():
    src = "x = f(a if cond else b)\n"
    assert _round_trip(src) == src
