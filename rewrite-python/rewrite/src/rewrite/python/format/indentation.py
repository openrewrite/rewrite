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


"""The indentation a file is written with, for the code a format indents."""

from typing import Dict, List, Optional, Tuple

from rewrite.java import J, JavaSourceFile
from rewrite.java import tree as j
from rewrite.python.style import IntelliJ, TabsAndIndentsStyle
from rewrite.python.visitor import PythonVisitor

# The file last detected and its style, since a format asks once for each tree it formats.
_last: Optional[Tuple[JavaSourceFile, TabsAndIndentsStyle]] = None


def tabs_and_indents_style(cu: JavaSourceFile) -> TabsAndIndentsStyle:
    """The style `cu` carries, or else IntelliJ's indented as most statements of `cu` are, as Java's `Autodetect`
    decides it: with tabs where they make up at least half of the levels those statements are indented by."""
    style = cu.get_style(TabsAndIndentsStyle)
    if style is not None:
        return style
    global _last
    last = _last
    if last is not None and last[0] is cu:
        return last[1]
    counts = _IndentCounts()
    counts.visit(cu, None)
    style = counts.style()
    _last = (cu, style)
    return style


class _IndentCounts(PythonVisitor[None]):
    def __init__(self):
        super().__init__()
        # The indentation of the statements of each enclosing block, the file's own first
        self._levels: List[str] = [""]
        self._levels_total = 0
        self._tabs = 0
        self._spaces_per_level: Dict[int, int] = {}

    def visit_block(self, block: j.Block, p: None) -> J:
        indents = [s.prefix.indent for s in block.statements if '\n' in s.prefix.last_whitespace]
        if not indents:
            return super().visit_block(block, p)
        outer, indent = self._levels[-1], indents[0]
        self._levels.append(indent)
        depth = len(self._levels) - 1
        for statement_indent in indents:
            self._levels_total += depth
            self._tabs += statement_indent.count('\t')
        if '\t' not in outer + indent and len(indent) > len(outer):
            added = len(indent) - len(outer)
            self._spaces_per_level[added] = self._spaces_per_level.get(added, 0) + len(indents)
        try:
            return super().visit_block(block, p)
        finally:
            self._levels.pop()

    def style(self) -> TabsAndIndentsStyle:
        style = IntelliJ.tabs_and_indents()
        if self._levels_total == 0:
            return style
        if self._tabs >= self._levels_total / 2:
            return style.with_use_tab_character(True).with_tab_size(style.indent_size)
        if self._spaces_per_level:
            # The most common, and the smaller of two as common
            size = min(self._spaces_per_level, key=lambda added: (-self._spaces_per_level[added], added))
            style = style.with_indent_size(size)
        return style
