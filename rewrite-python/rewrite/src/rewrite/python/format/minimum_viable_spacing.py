from __future__ import annotations

from typing import cast, Optional

from rewrite import Tree, P, Cursor, PrintOutputCapture, list_find
from rewrite.java import Assert, Binary, Expression, Return, Statement, Block, Semicolon, Ternary, Unary
from rewrite.python import Binary as PyBinary, PythonVisitor, ExpressionStatement
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
                    tree = tree.replace(expression=tree.expression.replace(prefix=new_prefix))
                else:
                    tree = tree.replace(prefix=new_prefix)

        return tree

    def visit_assert(self, assert_: Assert, p: P) -> Optional[T]:
        a = cast(Assert, super().visit_assert(assert_, p))
        return cast(Optional[T], a.replace(condition=self._separate_from_keyword(a.condition)))

    def visit_binary(self, binary: Binary, p: P) -> Optional[T]:
        b = cast(Binary, super().visit_binary(binary, p))
        if b.operator in (Binary.Type.And, Binary.Type.Or):
            b = b.replace(right=self._separate_from_keyword(b.right))
        return cast(Optional[T], b)

    def visit_python_binary(self, binary: PyBinary, p: P) -> Optional[T]:
        b = cast(PyBinary, super().visit_python_binary(binary, p))
        if b.operator in (PyBinary.Type.In, PyBinary.Type.Is, PyBinary.Type.IsNot, PyBinary.Type.NotIn):
            b = b.replace(right=self._separate_from_keyword(b.right))
        return cast(Optional[T], b)

    def visit_return(self, return_: Return, p: P) -> Optional[T]:
        r = cast(Return, super().visit_return(return_, p))
        if r.expression is not None:
            r = r.replace(expression=self._separate_from_keyword(r.expression))
        return cast(Optional[T], r)

    def visit_ternary(self, ternary: Ternary, p: P) -> Optional[T]:
        t = cast(Ternary, super().visit_ternary(ternary, p))
        t = t.replace(condition=self._separate_from_keyword(t.condition))
        return cast(Optional[T], t.padding.replace(
            false_part=t.padding.false_part.replace(element=self._separate_from_keyword(t.false_part))))

    def visit_unary(self, unary: Unary, p: P) -> Optional[T]:
        u = cast(Unary, super().visit_unary(unary, p))
        if u.operator == Unary.Type.Not:
            u = u.replace(expression=self._separate_from_keyword(u.expression))
        return cast(Optional[T], u)

    def _separate_from_keyword(self, expression: Expression) -> Expression:
        """Adds a space where a preceding keyword and `expression` would otherwise lex as a single token."""
        prefix = expression.prefix
        if prefix.comments or prefix.whitespace:
            return expression
        first = expression.print(self.cursor, PrintOutputCapture(0))[:1]
        if not first.isalnum() and first != '_':
            return expression
        return expression.replace(prefix=prefix.replace(whitespace=' '))

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return cast(Optional[T], tree if self._stop else super().visit(tree, p, parent))
