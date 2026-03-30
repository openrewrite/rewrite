from __future__ import annotations

from typing import cast, Optional

from rewrite import Tree, P, Cursor, list_find
from rewrite.java import Statement, Block, Semicolon
from rewrite.python import PythonVisitor, ExpressionStatement
from rewrite.visitor import T


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
                    tree = tree.replace(expression=tree.expression.replace(prefix=new_prefix))  # ty: ignore[invalid-assignment]
                else:
                    tree = tree.replace(prefix=new_prefix)  # ty: ignore[invalid-assignment]

        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return cast(Optional[T], tree if self._stop else super().visit(tree, p, parent))
