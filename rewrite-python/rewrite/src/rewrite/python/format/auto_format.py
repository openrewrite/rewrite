from typing import Optional

from .blank_lines import BlankLinesVisitor
from .minimum_viable_spacing import MinimumViableSpacingVisitor
from .normalize_format import NormalizeFormatVisitor
from .normalize_line_breaks_visitor import NormalizeLineBreaksVisitor
from .normalize_tabs_or_spaces import NormalizeTabsOrSpacesVisitor
from .remove_trailing_whitespace_visitor import RemoveTrailingWhitespaceVisitor
from .. import TabsAndIndentsStyle
from ..style import BlankLinesStyle, IntelliJ
from ..visitor import PythonVisitor
from ... import Recipe, Tree, Cursor
from ...java import JavaSourceFile
from ...style import GeneralFormatStyle
from ...visitor import P, T


class AutoFormat(Recipe):
    def get_visitor(self):
        return AutoFormatVisitor()


class AutoFormatVisitor(PythonVisitor[P]):
    def __init__(self, stop_after: Optional[Tree] = None):
        self._stop_after = stop_after

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        self._cursor = parent if parent is not None else Cursor(None, Cursor.ROOT_VALUE)
        cu = tree if isinstance(tree, JavaSourceFile) else self._cursor.first_enclosing_or_throw(JavaSourceFile)

        tree = NormalizeFormatVisitor(self._stop_after).visit(tree, p, self._cursor.fork())

        tree = MinimumViableSpacingVisitor(self._stop_after).visit(tree, p, self._cursor.fork())

        tree = BlankLinesVisitor(cu.get_style(BlankLinesStyle) or IntelliJ.blank_lines(), self._stop_after).visit(tree, p, self._cursor.fork())

        # TODO: SpacesVisitor - blocked on base PythonVisitor passing `loc` to visit_space/visit_right_padded/visit_container

        tree = NormalizeTabsOrSpacesVisitor(
            cu.get_style(TabsAndIndentsStyle) or IntelliJ.tabs_and_indents(),
            self._stop_after
        ).visit(tree, p, self._cursor.fork())

        # TODO: TabsAndIndentsVisitor - blocked on base PythonVisitor passing `loc` to visit_space/visit_right_padded/visit_container

        tree = NormalizeLineBreaksVisitor(cu.get_style(GeneralFormatStyle) or GeneralFormatStyle(False),
                                          self._stop_after).visit(tree, p, self._cursor.fork())

        tree = RemoveTrailingWhitespaceVisitor(self._stop_after).visit(tree, self._cursor.fork())

        return tree
