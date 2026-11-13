from contextlib import contextmanager
from contextvars import ContextVar
from typing import Callable, Iterator, Optional, cast

from .blank_lines import BlankLinesVisitor
from .indentation import tabs_and_indents_style
from .line_breaks import general_format_style
from .minimum_viable_spacing import MinimumViableSpacingVisitor
from .normalize_format import NormalizeFormatVisitor
from .normalize_line_breaks_visitor import NormalizeLineBreaksVisitor
from .normalize_tabs_or_spaces import NormalizeTabsOrSpacesVisitor
from .remove_trailing_whitespace_visitor import RemoveTrailingWhitespaceVisitor
from .spaces_visitor import SpacesVisitor
from .tabs_and_indents_visitor import TabsAndIndentsVisitor
from .. import SpacesStyle, OtherStyle
from ..style import BlankLinesStyle, IntelliJ
from ..visitor import PythonVisitor
from ... import Recipe, SourceFile, Tree, Cursor
from ...java import JavaSourceFile
from ...visitor import P, T


class AutoFormat(Recipe):
    @property
    def name(self) -> str:
        return "org.openrewrite.python.format.AutoFormat"

    @property
    def display_name(self) -> str:
        return "Auto-format Python code"

    @property
    def description(self) -> str:
        return "Applies standard formatting to Python source code."

    def editor(self):
        return AutoFormatVisitor()


FormattingListener = Callable[[Tree, Optional[Tree], Cursor], None]

# Told of each request to auto-format a tree, with its stop_after and the cursor of its parent.
_formatting_listener: ContextVar[Optional[FormattingListener]] = ContextVar("formatting_listener", default=None)


@contextmanager
def formatting_listener(listener: FormattingListener) -> Iterator[None]:
    """Tells ``listener`` of each request to auto-format a tree in this context, so that a test harness can check
    that a recipe formats only what it changed."""
    token = _formatting_listener.set(listener)
    try:
        yield
    finally:
        _formatting_listener.reset(token)


class AutoFormatVisitor(PythonVisitor[P]):
    def __init__(self, stop_after: Optional[Tree] = None, keep_indent: bool = False,
                 replaced: Optional[Tree] = None):
        """``keep_indent`` is passed on to `TabsAndIndentsVisitor`, and ``replaced``, the tree the one formatted
        takes the place of, to `BlankLinesVisitor`."""
        self._stop_after = stop_after
        self._keep_indent = keep_indent
        self._replaced = replaced

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        if isinstance(tree, SourceFile) and not self.is_acceptable(tree, p):
            # Such as a file the parser failed on, which has no Python to format.
            return cast(T, tree)
        self._cursor = parent if parent is not None else Cursor(None, Cursor.ROOT_VALUE)
        cu = tree if isinstance(tree, JavaSourceFile) else self._cursor.first_enclosing_or_throw(JavaSourceFile)
        listener = _formatting_listener.get()
        if listener is not None and tree is not None:
            listener(tree, self._stop_after, self._cursor)

        tree = NormalizeFormatVisitor(self._stop_after).visit(tree, p, self._cursor.fork())

        tree = MinimumViableSpacingVisitor(self._stop_after).visit(tree, p, self._cursor.fork())

        tree = BlankLinesVisitor(cu.get_style(BlankLinesStyle) or IntelliJ.blank_lines(), self._stop_after,
                                 self._replaced).visit(tree, p, self._cursor.fork())

        tree = SpacesVisitor(cu.get_style(SpacesStyle) or IntelliJ.spaces(), self._stop_after).visit(tree, p, self._cursor.fork())

        tree = NormalizeTabsOrSpacesVisitor(tabs_and_indents_style(cu), self._stop_after).visit(
            tree, p, self._cursor.fork())

        tree = _tabs_and_indents(cu, self._stop_after, self._keep_indent).visit(tree, p, self._cursor.fork())

        tree = NormalizeLineBreaksVisitor(general_format_style(cu), self._stop_after).visit(
            tree, p, self._cursor.fork())

        tree = RemoveTrailingWhitespaceVisitor(self._stop_after).visit(tree, self._cursor.fork())

        return tree


def minimally_format(tree: T, p: P, cursor: Optional[Cursor] = None, keep_indent: bool = False) -> T:
    """`tree` with the whitespace it needs to be valid and the indentation of the file `cursor` leads to,
    and its other whitespace as it is. With `keep_indent`, its first line keeps the indentation it has."""
    try:
        root = cursor if cursor is not None else Cursor(None, Cursor.ROOT_VALUE)
        cu = tree if isinstance(tree, JavaSourceFile) else root.first_enclosing_or_throw(JavaSourceFile)
        tree = MinimumViableSpacingVisitor().visit(tree, p, root.fork())
        return _tabs_and_indents(cu, keep_indent=keep_indent).visit(tree, p, root.fork())
    except (ValueError, AttributeError):
        return tree


def _tabs_and_indents(cu: JavaSourceFile, stop_after: Optional[Tree] = None,
                      keep_indent: bool = False) -> TabsAndIndentsVisitor:
    return TabsAndIndentsVisitor(
        tabs_and_indents_style(cu),
        cu.get_style(OtherStyle) or IntelliJ.other(),
        stop_after,
        keep_indent
    )
