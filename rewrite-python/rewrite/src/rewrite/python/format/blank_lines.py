from __future__ import annotations

from typing import Optional, TypeVar, cast

from rewrite import Tree, P, Cursor
from rewrite.java import J, Space, Statement, JRightPadded, Block, ClassDeclaration, MethodDeclaration, Import
from rewrite.python import PythonVisitor, BlankLinesStyle, CompilationUnit, MultiImport
from rewrite.visitor import T

J2 = TypeVar('J2', bound=J)


class BlankLinesVisitor(PythonVisitor):
    def __init__(self, style: BlankLinesStyle, stop_after: Tree = None):
        self._style = style
        self._stop_after = stop_after
        self._stop = False

    def visit_compilation_unit(self, compilation_unit: CompilationUnit, p: P) -> J:
        if not compilation_unit.prefix.comments:
            compilation_unit = compilation_unit.replace(prefix=Space.EMPTY)
        return super().visit_compilation_unit(compilation_unit, p)

    def visit_statement(self, statement: Statement, p: P) -> J:
        statement = super().visit_statement(statement, p)

        parent_cursor = self.cursor.parent_tree_cursor()
        top_level = isinstance(parent_cursor.value, CompilationUnit)

        if isinstance(statement, (Import, MultiImport)):
            parent_cursor.put_message('prev_import', True)
            prev_import = False
        else:
            prev_import = parent_cursor.get_message('prev_import', False)
            if prev_import:
                parent_cursor.put_message('prev_import', False)

        if top_level:
            if statement == cast(CompilationUnit, parent_cursor.value).statements[0]:
                statement = statement.replace(prefix=statement.prefix.replace(whitespace=''))
            else:
                min_lines = max(self._style.minimum.around_top_level_classes_functions if isinstance(statement, (ClassDeclaration, MethodDeclaration)) else 0,
                                self._style.minimum.after_top_level_imports if prev_import else 0)
                statement = _adjusted_lines_for_tree(statement, min_lines, self._style.keep_maximum.in_declarations)
        else:
            in_block = isinstance(parent_cursor.value, Block)
            in_class = in_block and isinstance(parent_cursor.parent_tree_cursor().value, ClassDeclaration)
            min_lines = 0
            if in_class:
                is_first = cast(Block, parent_cursor.value).statements[0] is statement
                if not is_first and isinstance(statement, MethodDeclaration):
                    min_lines = max(min_lines, self._style.minimum.around_method)
                elif not is_first and isinstance(statement, ClassDeclaration):
                    min_lines = max(min_lines, self._style.minimum.around_class)
                elif is_first and isinstance(statement, MethodDeclaration):
                    min_lines = max(min_lines, self._style.minimum.before_first_method)

            # This seems to correspond to how IntelliJ interprets this configuration
            max_lines = self._style.keep_maximum.in_declarations if \
                isinstance(statement, (ClassDeclaration, MethodDeclaration)) else \
                self._style.keep_maximum.in_code

            if prev_import:
                min_lines = max(min_lines, self._style.minimum.after_local_imports)

            statement = _adjusted_lines_for_tree(statement, min_lines, max_lines)
        return statement

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        if self._stop_after and tree == self._stop_after:
            self._stop = True
        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return tree if self._stop else super().visit(tree, p, parent)


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
