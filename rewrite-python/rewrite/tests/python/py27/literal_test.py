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

"""Python 2-specific literal forms.

Covers the literal shapes that differ from Python 3:

* Long-integer suffix ``100L``
* Octal ``0777`` (Py2 form; Py3 requires ``0o777``)
* Unicode string prefix ``u"..."`` / ``ur"..."``
* Raw string ``r"..."`` and byte string ``b"..."`` (both also valid in Py3)
* Trailing-dot float ``1.`` / leading-dot float ``.5``
* Scientific notation ``1e10``
* Implicit string concatenation ``"a" "b"`` (also valid in Py3)
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


class TestNumericLiterals:
    @pytest.mark.parametrize("src,value_source", [
        ("x = 100L\n",   "100L"),
        ("x = 0777\n",   "0777"),
        ("x = 0xff\n",   "0xff"),
        ("x = 0b101\n",  "0b101"),
        ("x = 1.\n",     "1."),
        ("x = .5\n",     ".5"),
        ("x = 1e10\n",   "1e10"),
        ("x = 1.5e-3\n", "1.5e-3"),
        ("x = 42\n",     "42"),
    ])
    def test_value_source_preserved(self, src, value_source):
        e = _expr(src)
        assert isinstance(e, j.Literal), src
        assert e.value_source == value_source

    @pytest.mark.parametrize("src", [
        "x = 100L\n",
        "x = 0777\n",
        "x = 0xff\n",
        "x = 1.\n",
        "x = .5\n",
        "x = 1e10\n",
    ])
    def test_round_trip(self, src):
        assert _round_trip(src) == src


class TestStringLiterals:
    @pytest.mark.parametrize("src,value_source", [
        ('x = "hello"\n', '"hello"'),
        ("x = 'hello'\n", "'hello'"),
        ('x = u"unicode"\n', 'u"unicode"'),
        ('x = U"unicode"\n', 'U"unicode"'),
        ('x = r"raw"\n', 'r"raw"'),
        ('x = R"raw"\n', 'R"raw"'),
        ('x = ur"both"\n', 'ur"both"'),
        ('x = b"bytes"\n', 'b"bytes"'),
        ('x = """triple"""\n', '"""triple"""'),
        ("x = '''triple'''\n", "'''triple'''"),
    ])
    def test_string_literal_round_trips(self, src, value_source):
        e = _expr(src)
        assert isinstance(e, j.Literal), src
        assert e.value_source == value_source
        assert _round_trip(src) == src


class TestImplicitConcatenation:
    def test_two_pieces(self):
        e = _expr('x = "a" "b"\n')
        assert isinstance(e, j.Literal)
        assert e.value_source == '"a" "b"'

    def test_three_pieces(self):
        e = _expr('x = "a" "b" "c"\n')
        assert isinstance(e, j.Literal)
        assert e.value_source == '"a" "b" "c"'

    def test_mixed_styles(self):
        e = _expr('x = "a" \'b\'\n')
        assert isinstance(e, j.Literal)
        assert e.value_source == '"a" \'b\''

    @pytest.mark.parametrize("src", [
        'x = "a" "b"\n',
        'x = "a" "b" "c"\n',
        'x = u"a" u"b"\n',
        'x = r"a" "b"\n',
    ])
    def test_round_trip(self, src):
        assert _round_trip(src) == src


class TestTrailingCommas:
    """Trailing commas on call args, collection literals, and parenthesized
    tuples must round-trip — both the comma itself and the whitespace
    between it and the closing delimiter."""

    @pytest.mark.parametrize("src", [
        "x = (1, 2,)\n",
        "x = [1, 2,]\n",
        "x = {1: 2, 3: 4,}\n",
        "x = {1, 2,}\n",
        "x = f(a, b,)\n",
        "x = f(\n    a,\n    b,\n)\n",
        "result = foo(\\\n    a,\\\n    b,\\\n)\n",
    ])
    def test_trailing_comma_round_trips(self, src):
        assert _round_trip(src) == src
