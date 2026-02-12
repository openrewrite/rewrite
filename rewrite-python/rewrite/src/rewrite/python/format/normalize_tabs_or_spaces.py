from __future__ import annotations

from typing import cast, Optional, TypeVar, List

from rewrite import Tree, P, Cursor, list_map
from rewrite.java import J, Space
from rewrite.python import PythonVisitor, PyComment, TabsAndIndentsStyle
from rewrite.visitor import T

J2 = TypeVar('J2', bound=J)


# TODO consider supporting multiline string literals
class NormalizeTabsOrSpacesVisitor(PythonVisitor):

    def __init__(self, style: TabsAndIndentsStyle, stop_after: Tree = None):
        self._stop_after = stop_after
        self._style = style
        self._stop = False

    def visit_space(self, space: Space, p):
        if not space or space is Space.EMPTY:
            return space

        s = space.replace(whitespace=self.normalize_after_first_newline(space.whitespace))
        return s.replace(comments=list_map(self.process_comment, cast(List[PyComment], s.comments)))

    def process_comment(self, comment: PyComment) -> PyComment:
        return comment.replace(suffix=self.normalize_after_first_newline(comment.suffix))

    def normalize_after_first_newline(self, text: str) -> str:
        first_newline = text.find('\n')
        if first_newline >= 0 and first_newline != len(text) - 1:
            return text[:first_newline + 1] + self.normalize(text[first_newline + 1:], False)
        return text

    def normalize(self, text: str, is_comment: bool) -> str:
        if not text:
            return text

        if ' ' not in text if self._style.use_tab_character else '\t' not in text:
            return text

        text_builder = []
        consecutive_spaces = 0
        in_margin = True
        i = 0

        while i < len(text):
            c = text[i]

            if c in '\n\r':
                in_margin = True
                consecutive_spaces = 0

            elif in_margin:
                if self._style.use_tab_character and c == ' ' and (
                        not is_comment or (i + 1 < len(text) and text[i + 1] != '*')):
                    end_idx = i + self._style.tab_size
                    if text[i:end_idx] == ' ' * self._style.tab_size:
                        text_builder.append('\t')
                        i += self._style.tab_size
                        continue

                elif not self._style.use_tab_character and c == '\t':
                    text_builder.append(' ' * (self._style.tab_size - (consecutive_spaces % self._style.tab_size)))
                    consecutive_spaces = 0
                    i += 1
                    continue

            text_builder.append(c)
            in_margin = in_margin and c.isspace()
            consecutive_spaces = consecutive_spaces + 1 if c.isspace() else 0
            i += 1

        return ''.join(text_builder)

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        if self._stop_after and tree == self._stop_after:
            self._stop = True
        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return tree if self._stop else super().visit(tree, p, parent)
