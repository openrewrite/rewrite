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

"""Backslash line continuation (``\\`` at end-of-line).

parso treats the backslash + newline + leading whitespace of the next
line as part of the following token's prefix. The LST stores it as
whitespace in the relevant :class:`Space`, and the printer emits it
verbatim — so as long as the parser threads the prefix through correctly,
round-trip works.
"""

import pytest

from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _round_trip(src: str) -> str:
    return PythonPrinter().print(Py2ParserVisitor(src, "<test>", "2.7").parse())


@pytest.mark.parametrize("src", [
    "x = 1 + \\\n    2\n",
    "x = a + b + \\\n    c + d\n",
    "if a == 1 and \\\n   b == 2:\n    pass\n",
    "result = foo(\\\n    a,\\\n    b,\\\n)\n",
])
def test_line_continuation_round_trips(src):
    assert _round_trip(src) == src


def test_continuation_with_comment_after_paren_open():
    # Inside parens, a true newline (no backslash) is allowed and stays.
    src = "x = (\n    1 +\n    2\n)\n"
    assert _round_trip(src) == src
