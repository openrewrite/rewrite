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

parso embeds comments inside leaf ``prefix`` strings (e.g.
``"  # comment\\n"``). :meth:`Py2ParserVisitor._parse_space` tokenizes
those strings into :class:`Space.whitespace` plus one
:class:`TextComment` per ``#`` line, so comments are visible to recipes
that query :attr:`Space.comments` — and round-trip output stays
byte-for-byte identical (the printer renders whitespace then iterates
comments).
"""

import pytest

from rewrite.java.support_types import TextComment
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.python.visitor import PythonVisitor


def _parse(src: str):
    return Py2ParserVisitor(src, "<test>", "2.7").parse()


def _round_trip(src: str) -> str:
    return PythonPrinter().print(_parse(src))


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


# ---------------------------------------------------------------------------
# Structural tests — recipes need to *see* comments, not just round-trip them.
# ---------------------------------------------------------------------------

def _all_comments(cu):
    """Collect every :class:`TextComment` reachable from ``cu`` via the
    visitor's ``visit_space`` hook, which the framework invokes on every
    :class:`Space` slot in the tree (prefixes, padding ``.before`` /
    ``.after``, block ends, ``cu.eof``, etc.). Using the visitor guarantees
    we see the same Space objects the printer would render — no
    handwritten field-list to drift out of date.
    """
    collected = []

    class _Collector(PythonVisitor):
        def visit_space(self, space, p):
            if space is not None:
                collected.extend(space.comments)
            return space

    _Collector().visit(cu, None)
    return collected


class TestCommentStructure:
    def test_top_line_comment_is_a_text_comment(self):
        cu = _parse("# header\nx = 1\n")
        comments = list(_all_comments(cu))
        texts = [c.text for c in comments if isinstance(c, TextComment)]
        assert " header" in texts

    def test_trailing_comment_is_a_text_comment(self):
        cu = _parse("x = 1  # trailing\n")
        comments = list(_all_comments(cu))
        texts = [c.text for c in comments if isinstance(c, TextComment)]
        assert " trailing" in texts

    def test_two_consecutive_comments_both_captured(self):
        cu = _parse("# one\n# two\nx = 1\n")
        comments = list(_all_comments(cu))
        texts = [c.text for c in comments if isinstance(c, TextComment)]
        assert " one" in texts
        assert " two" in texts

    def test_comment_inside_block_body(self):
        cu = _parse("def f():\n    # body comment\n    pass\n")
        comments = list(_all_comments(cu))
        texts = [c.text for c in comments if isinstance(c, TextComment)]
        assert " body comment" in texts
