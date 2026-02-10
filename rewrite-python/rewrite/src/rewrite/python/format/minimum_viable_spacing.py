from __future__ import annotations

from typing import cast, Optional, TypeVar, List

from rewrite import Tree, P, Cursor, list_map, list_find
from rewrite.java import J, Space, Statement, Block, Semicolon
from rewrite.python import PythonVisitor, PyComment, ExpressionStatement
from rewrite.visitor import T

J2 = TypeVar('J2', bound=J)


class MinimumViableSpacingVisitor(PythonVisitor):
    def __init__(self, stop_after: Optional[Tree] = None):
        self._stop_after = stop_after
        self._stop = False

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        if self._stop_after and tree == self._stop_after:
            self._stop = True

        owner = self.cursor.parent_tree_cursor().value
        if isinstance(tree, Statement) and isinstance(owner, Block) and not tree.prefix.comments and not '\n' in tree.prefix.whitespace:
            statement_index = list_find(owner.statements, tree)
            previous_statement = owner.padding.statements[statement_index - 1] if statement_index > 0 else None
            if not previous_statement or not previous_statement.markers.find_first(Semicolon):
                new_prefix = tree.prefix.replace(whitespace='\n' + tree.prefix.whitespace)
                if isinstance(tree, ExpressionStatement):
                    tree = tree.replace(expression=tree.expression.replace(prefix=new_prefix))
                else:
                    tree = tree.replace(prefix=new_prefix)

        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return tree if self._stop else super().visit(tree, p, parent)


def _common_margin(s1, s2):
    if s1 is None:
        s = str(s2)
        return s[s.rfind('\n') + 1:]

    min_length = min(len(s1), len(s2))
    for i in range(min_length):
        if s1[i] != s2[i] or not s1[i].isspace():
            return s1[:i]

    return s2 if len(s2) < len(s1) else s1


def _concatenate_prefix(j: J, prefix: Space) -> J2:
    shift = _common_margin(None, j.prefix.whitespace)

    def modify_comment(c: PyComment) -> PyComment:
        if len(shift) == 0:
            return c
        c = c.replace(text=c.text.replace('\n', '\n' + shift))
        if '\n' in c.suffix:
            c = c.replace(suffix=c.suffix.replace('\n', '\n' + shift))
        return c

    comments = j.prefix.comments + list_map(modify_comment, cast(List[PyComment], prefix.comments))

    new_prefix = j.prefix
    new_prefix = new_prefix.replace(whitespace=new_prefix.whitespace + prefix.whitespace)
    if comments:
        new_prefix = new_prefix.replace(comments=comments)

    return j.replace(prefix=new_prefix)
