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

"""Semicolon-separated simple_stmt round-trip and structural tests.

In Python 2 (and 3) the grammar allows several ``small_stmt`` nodes on the
same physical line, separated by ``;``::

    x = 1; y = 2
    x = 1;       # trailing ';'
    a = 1; b = 2; c = 3

Each ``small_stmt`` must surface as its own LST statement, with the
:class:`Semicolon` marker attached to every statement that is followed by
``;``. Without that, the printer cannot reproduce the source and any
trailing small_stmts are silently dropped.
"""

import pytest

from rewrite.java.markers import Semicolon
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter
from tests.python.py27.expressions_test import _collect_placeholders


def _parse(src: str):
    return Py2ParserVisitor(src, "<test>", "2.7").parse()


def _round_trip(src: str) -> str:
    return PythonPrinter().print(_parse(src))


@pytest.mark.parametrize("src", [
    # two small_stmts on one line
    "x = 1; y = 2\n",
    # three small_stmts on one line
    "a = 1; b = 2; c = 3\n",
    # trailing ';' with no following small_stmt
    "x = 1;\n",
    # mixed kinds: assignment + augmented
    "x = 1; y += 2\n",
    # mixed kinds: import + expression
    "import os; print os\n",
    # mixed kinds: print + assignment (Py2-only print statement)
    "print x; y = 1\n",
    # bare keywords mixed in
    "x = 1; pass\n",
    # del / global on the same line
    "del x; del y\n",
    # whitespace variations around ';'
    "x = 1 ; y = 2\n",
    "x = 1 ;y = 2\n",
    # nested inside a block — the suite body uses the same plumbing
    "def f():\n    a = 1; b = 2\n",
    "if True:\n    x = 1; y = 2\n",
    "for i in xs:\n    a = i; b = i + 1\n",
])
def test_semicolon_round_trip(src):
    assert _round_trip(src) == src


class TestSemicolonStructure:
    """Each small_stmt becomes its own LST statement with the correct marker."""

    def test_two_statements_emitted(self):
        cu = _parse("x = 1; y = 2\n")
        assert len(cu.statements) == 2

    def test_three_statements_emitted(self):
        cu = _parse("a = 1; b = 2; c = 3\n")
        assert len(cu.statements) == 3

    def test_trailing_semicolon_keeps_one_statement(self):
        cu = _parse("x = 1;\n")
        assert len(cu.statements) == 1

    def test_semicolon_marker_on_preceding_statements(self):
        cu = _parse("a = 1; b = 2; c = 3\n")
        padded = cu.padding.statements
        for p in padded[:-1]:
            assert p.markers.find_first(Semicolon) is not None, (
                "expected Semicolon marker on statement preceding ';'"
            )
        assert padded[-1].markers.find_first(Semicolon) is None, (
            "last statement on a line should not carry Semicolon"
        )

    def test_trailing_semicolon_marker(self):
        cu = _parse("x = 1;\n")
        padded = cu.padding.statements
        assert len(padded) == 1
        assert padded[0].markers.find_first(Semicolon) is not None

    def test_no_placeholders_in_semicolon_line(self):
        cu = _parse("a = 1; b = 2; c = 3\n")
        for stmt in cu.statements:
            assert list(_collect_placeholders(stmt)) == [], stmt
