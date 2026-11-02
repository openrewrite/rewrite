from __future__ import annotations

import sys
from typing import Optional, TypeVar, cast

from rewrite import Tree, P, Cursor
from rewrite.java import J, Space, Statement, JRightPadded, Block, ClassDeclaration, MethodDeclaration, Import
from rewrite.python import (PythonVisitor, BlankLinesStyle, CompilationUnit, ExpressionStatement, MultiImport,
                            StatementExpression)
from rewrite.visitor import T

J2 = TypeVar('J2', bound=J)


class BlankLinesVisitor(PythonVisitor):
    def __init__(self, style: BlankLinesStyle, stop_after: Optional[Tree] = None, replaced: Optional[Tree] = None):
        """With ``replaced``, a tree visited with a parent cursor takes its place, keeping the blank lines before it
        and gaining any more the style asks for where ``replaced`` stands."""
        self._style = style
        self._stop_after = stop_after
        self._stop = False
        self._replaced = replaced
        self._kept: Optional[Tree] = None

    def visit_compilation_unit(self, cu: CompilationUnit, p: P) -> J:
        if not cu.prefix.comments:
            cu = cu.replace(prefix=Space.EMPTY)
        return super().visit_compilation_unit(cu, p)

    def visit_statement(self, stmt: Statement, p: P) -> J:
        stmt = super().visit_statement(stmt, p)  # ty: ignore[invalid-assignment]  # visitor covariance

        parent_cursor = self.cursor.parent_tree_cursor()
        top_level = isinstance(parent_cursor.value, CompilationUnit)

        if isinstance(stmt, (Import, MultiImport)):
            parent_cursor.put_message('prev_import', True)
            prev_import = False
        else:
            prev_import = parent_cursor.get_message('prev_import', False)
            if prev_import:
                parent_cursor.put_message('prev_import', False)

        kept = self._starts_kept_tree()
        if kept and self.cursor.value is not self._kept:
            return stmt
        # The kept tree stands where the tree it replaced does, and gains the blank lines its own kind asks for there
        # without losing any it has.
        here = self._replaced if kept else stmt
        if top_level:
            if here == parent_cursor.value.statements[0]:
                if not kept:
                    stmt = stmt.replace(prefix=stmt.prefix.replace(whitespace=''))
            else:
                min_lines = max(self._style.minimum.around_top_level_classes_functions if isinstance(stmt, (ClassDeclaration, MethodDeclaration)) else 0,
                                self._style.minimum.after_top_level_imports if prev_import else 0)
                max_lines = sys.maxsize if kept else self._style.keep_maximum.in_declarations
                stmt = _adjusted_lines_for_tree(stmt, min_lines, max_lines)  # ty: ignore[invalid-assignment]  # visitor covariance
        else:
            in_block = isinstance(parent_cursor.value, Block)
            in_class = in_block and isinstance(parent_cursor.parent_tree_cursor().value, ClassDeclaration)
            min_lines = 0
            if in_class:
                is_first = cast(Block, parent_cursor.value).statements[0] == here
                if not is_first and isinstance(stmt, MethodDeclaration):
                    min_lines = max(min_lines, self._style.minimum.around_method)
                elif not is_first and isinstance(stmt, ClassDeclaration):
                    min_lines = max(min_lines, self._style.minimum.around_class)
                elif is_first and isinstance(stmt, MethodDeclaration):
                    min_lines = max(min_lines, self._style.minimum.before_first_method)

            # This seems to correspond to how IntelliJ interprets this configuration
            max_lines = self._style.keep_maximum.in_declarations if \
                isinstance(stmt, (ClassDeclaration, MethodDeclaration)) else \
                self._style.keep_maximum.in_code

            if prev_import:
                min_lines = max(min_lines, self._style.minimum.after_local_imports)
            if kept:
                max_lines = sys.maxsize

            stmt = _adjusted_lines_for_tree(stmt, min_lines, max_lines)  # ty: ignore[invalid-assignment]  # visitor covariance
        return stmt

    def _starts_kept_tree(self) -> bool:
        """Whether the cursor is at the tree whose blank lines are kept, or at what that tree's prefix belongs to."""
        if self._kept is None:
            return False
        cursor = self.cursor
        while cursor.value is not self._kept:
            cursor = cursor.parent_tree_cursor()
            if not isinstance(cursor.value, (ExpressionStatement, StatementExpression)):
                return False
        return True

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        if self._stop_after and tree == self._stop_after:
            self._stop = True
        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        if parent is not None and self._replaced is not None:
            self._kept = tree
        return cast(Optional[T], tree if self._stop else super().visit(tree, p, parent))


def _adjusted_lines_for_right_padded(tree: JRightPadded[J2], min_lines: int, max_lines: int) -> JRightPadded[J2]:
    return tree.replace(element=_adjusted_lines_for_tree(tree.element, min_lines, max_lines))


def _adjusted_lines_for_tree(tree: J, min_lines: int, max_lines: int) -> J:
    return tree.replace(prefix=_adjusted_lines_for_space(tree.prefix, min_lines, max_lines))


def _adjusted_lines_for_space(prefix: Space, min_lines: int, max_lines: int) -> Space:
    if not prefix.comments or \
            '\n' in prefix.whitespace or \
            (prefix.comments[0].multiline and '\n' in prefix.comments[0].text):
        return prefix.replace(whitespace=_adjusted_lines_for_string(prefix.whitespace, min_lines, max_lines))

    # the first comment is a trailing comment on the previous line
    c0 = prefix.comments[0].replace(suffix=_adjusted_lines_for_string(prefix.comments[0].suffix, min_lines, max_lines))
    return prefix if c0 is prefix.comments[0] else prefix.replace(comments=[c0] + prefix.comments[1:])


def _adjusted_lines_for_string(whitespace, min_lines: int, max_lines: int):
    existing_blank_lines = max(_count_line_breaks(whitespace) - 1, 0)
    max_lines = max(min_lines, max_lines)
    if min_lines <= existing_blank_lines <= max_lines:
        return whitespace
    elif existing_blank_lines < min_lines:
        return '\n' * (min_lines - existing_blank_lines) + whitespace
    else:
        return '\n' * max_lines + whitespace[whitespace.rfind('\n'):]


def _count_line_breaks(whitespace):
    return whitespace.count('\n')
