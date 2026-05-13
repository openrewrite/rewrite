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

"""Comment preservation through parse-and-print.

Comments live in :class:`Space.whitespace` (parso stores them in leaf
``prefix`` strings, which we keep verbatim). The printer just emits
:class:`Space.whitespace`, so as long as the right LST slot holds the
comment, byte-for-byte round-trip works.
"""

import pytest

from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _round_trip(src: str) -> str:
    return PythonPrinter().print(Py2ParserVisitor(src, "<test>", "2.7").parse())


@pytest.mark.parametrize("src", [
    "# top-line comment\nx = 1\n",
    "# first\n# second\nx = 1\n",
    "x = 1  # trailing\n",
    "x = 1  # trailing\ny = 2\n",
    "# leading\nx = 1  # trailing\n",
    "def f():\n    # body comment\n    pass\n",
    "if x:\n    # inside if\n    pass\n",
    "class Foo:\n    # docstring slot\n    pass\n",
    "x = 1\n# between\ny = 2\n",
    "x = (1 +  # mid-expression\n     2)\n",
])
def test_comment_round_trip(src):
    assert _round_trip(src) == src


def test_comment_only_file_at_top():
    # A file with leading comments before any statement.
    src = "# license header line 1\n# license header line 2\n\nx = 1\n"
    assert _round_trip(src) == src


def test_trailing_comment_no_final_newline():
    src = "x = 1  # final comment"
    assert _round_trip(src) == src
