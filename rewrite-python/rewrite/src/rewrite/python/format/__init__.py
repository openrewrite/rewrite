__path__ = __import__('pkgutil').extend_path(__path__, __name__)

from typing import Optional

from .auto_format import AutoFormat, AutoFormatVisitor, minimally_format
from .blank_lines import BlankLinesVisitor
from .minimum_viable_spacing import MinimumViableSpacingVisitor
from .normalize_format import NormalizeFormatVisitor
from .normalize_tabs_or_spaces import NormalizeTabsOrSpacesVisitor
from .spaces_visitor import SpacesVisitor
from .tabs_and_indents_visitor import TabsAndIndentsVisitor

from ...tree import Tree
from ...visitor import Cursor


def auto_format(tree, p, stop_after=None, cursor: Optional[Cursor] = None, keep_indent: bool = False,
                replaced: Optional[Tree] = None):
    try:
        return AutoFormatVisitor(stop_after, keep_indent, replaced).visit(tree, p, cursor)
    except (ValueError, AttributeError):
        return tree


def maybe_auto_format(before, after, p, stop_after=None, cursor: Optional[Cursor] = None, keep_indent: bool = False):
    """With ``keep_indent``, ``after`` takes the place of ``before``, keeping its indentation and the blank lines
    before it."""
    if before is not after:
        return auto_format(after, p, stop_after, cursor, keep_indent, before if keep_indent else None)
    return after


__all__ = [
    'AutoFormat',
    'AutoFormatVisitor',
    'BlankLinesVisitor',
    'MinimumViableSpacingVisitor',
    'NormalizeFormatVisitor',
    'NormalizeTabsOrSpacesVisitor',
    'SpacesVisitor',
    'TabsAndIndentsVisitor',
    'auto_format',
    'maybe_auto_format',
    'minimally_format',
]
