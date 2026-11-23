# Copyright 2026 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""The line break a file is written with, for whitespace an edit synthesizes."""

from rewrite.java import JavaSourceFile, Space
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.style import GeneralFormatStyle


def general_format_style(cu: JavaSourceFile) -> GeneralFormatStyle:
    """The style `cu` carries, or else the line break most of its whitespace uses, as Java's
    `Autodetect` decides it: a tie goes to LF."""
    style = cu.get_style(GeneralFormatStyle)
    if style is not None:
        return style
    counts = _LineBreakCounts()
    if isinstance(cu, CompilationUnit):
        for statement in cu.statements:
            counts.add(statement.prefix)
        counts.add(cu.eof)
    if counts.lf == counts.crlf == 0:
        counts.visit(cu, None)
    return GeneralFormatStyle(counts.crlf > counts.lf)


def line_break(cu: JavaSourceFile) -> str:
    return '\r\n' if general_format_style(cu).use_crlf_new_lines else '\n'


class _LineBreakCounts(PythonVisitor):
    def __init__(self):
        super().__init__()
        self.lf = 0
        self.crlf = 0

    def add(self, space: Space) -> None:
        self._count(space.whitespace)
        for comment in space.comments:
            self._count(comment.suffix)

    def _count(self, text: str) -> None:
        crlf = text.count('\r\n')
        self.crlf += crlf
        self.lf += text.count('\n') - crlf

    def visit_space(self, space, p):
        if space is not None:
            self.add(space)
        return super().visit_space(space, p)
