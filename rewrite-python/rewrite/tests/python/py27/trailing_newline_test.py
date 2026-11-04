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

"""Round-trip the presence (or absence) of a trailing newline.

Real-world Py2 files end with or without ``\\n``. The parser must preserve
the original layout byte-for-byte; a missing trailing newline cannot be
silently inserted.
"""

import pytest

from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _round_trip(src: str) -> str:
    return PythonPrinter().print(Py2ParserVisitor(src, "<test>", "2.7").parse())


@pytest.mark.parametrize("src", [
    "x = 1\n",
    "x = 1",
    "x = 1\n\n",
    "x = 1\ny = 2\n",
    "x = 1\ny = 2",
    "\nx = 1\n",
    "\n\nx = 1\n",
])
def test_trailing_newline_variants_round_trip(src):
    assert _round_trip(src) == src


def test_compound_statement_no_trailing_newline():
    src = "def f():\n    pass"
    assert _round_trip(src) == src


def test_blank_lines_between_statements_preserved():
    src = "x = 1\n\n\ny = 2\n"
    assert _round_trip(src) == src
