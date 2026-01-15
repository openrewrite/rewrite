# Copyright 2025 the original author or authors.
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

"""Python printer for generating source code from Python LST nodes."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING, Any, Callable, List, Optional, TypeVar, Union

from rewrite import Cursor, Marker, Markers, Tree
from rewrite.java import (
    J,
    Space,
    Comment,
    TextComment,
    JRightPadded,
    JLeftPadded,
    JContainer,
    Expression,
    Statement,
    Loop,
)
from rewrite.java.markers import Semicolon, TrailingComma, OmitParentheses
from rewrite.python.support_types import Py, PySpace, PyRightPadded, PyLeftPadded, PyContainer, PyComment
from rewrite.python.markers import KeywordArguments, KeywordOnlyArguments, Quoted, SuppressNewline

if TYPE_CHECKING:
    from rewrite.python import tree as py
    from rewrite.java import tree as j

P = TypeVar("P")


class MarkerPrinter(ABC):
    """Interface for printing markers."""

    @abstractmethod
    def before_prefix(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        """Called before the prefix of a tree element."""
        ...

    @abstractmethod
    def before_syntax(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        """Called before the syntax of a tree element."""
        ...

    @abstractmethod
    def after_syntax(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        """Called after the syntax of a tree element."""
        ...


class DefaultMarkerPrinter(MarkerPrinter):
    """Default marker printer implementation."""

    def before_prefix(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        return ""

    def before_syntax(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        # Handle SearchResult and Markup markers
        kind = getattr(marker, 'kind', '') if hasattr(marker, 'kind') else type(marker).__name__
        if kind == 'SearchResult' or (isinstance(kind, str) and kind.startswith('org.openrewrite.marker.Markup$')):
            desc = getattr(marker, 'description', None)
            return comment_wrapper("" if desc is None else f"({desc})")
        return ""

    def after_syntax(self, marker: Marker, cursor: Cursor, comment_wrapper: Callable[[str], str]) -> str:
        return ""


class PrintOutputCapture:
    """Captures output during printing."""

    def __init__(self, marker_printer: Optional[MarkerPrinter] = None):
        self._out: List[str] = []
        self.marker_printer = marker_printer or DefaultMarkerPrinter()

    @property
    def out(self) -> str:
        return "".join(self._out)

    def append(self, text: Optional[str]) -> 'PrintOutputCapture':
        if text:
            self._out.append(text)
        return self

    def get_marker_printer(self) -> MarkerPrinter:
        return self.marker_printer

    def __len__(self) -> int:
        return sum(len(s) for s in self._out)

    def last_char(self) -> Optional[str]:
        """Return the last character in the output, or None if empty."""
        if not self._out:
            return None
        for s in reversed(self._out):
            if s:
                return s[-1]
        return None


class PythonPrinter:
    """
    Python printer for generating source code from LST.

    Uses a delegate pattern where:
    - Python-specific nodes (Py.*) are handled by this printer
    - Java nodes (J.*) are handled by the delegate PythonJavaPrinter
    """

    def __init__(self):
        self._cursor: Optional[Cursor] = None
        self._delegate = PythonJavaPrinter(self)

    def get_cursor(self) -> Cursor:
        if self._cursor is None:
            raise RuntimeError("Cursor not set")
        return self._cursor

    def set_cursor(self, cursor: Optional[Cursor]) -> None:
        self._cursor = cursor
        self._delegate.set_cursor(cursor)

    def print(self, tree: Tree, p: Optional[PrintOutputCapture] = None) -> str:
        """Print a tree to source code."""
        if p is None:
            p = PrintOutputCapture()
        self.set_cursor(Cursor(None, Cursor.ROOT_VALUE))
        self.visit(tree, p)
        return p.out

    def visit(self, tree: Optional[Tree], p: PrintOutputCapture) -> Optional[J]:
        """Visit a tree node."""
        if tree is None:
            return None

        # Set up cursor
        self.set_cursor(Cursor(self.get_cursor(), tree))

        try:
            if isinstance(tree, Py):
                return self._visit_python(tree, p)
            else:
                # Delegate to Java printer for non-Python nodes
                return self._delegate.visit(tree, p)
        finally:
            # Restore cursor
            parent = self.get_cursor().parent
            self.set_cursor(parent)

    def _visit_python(self, tree: Py, p: PrintOutputCapture) -> Optional[J]:
        """Visit a Python-specific node."""
        from rewrite.python import tree as py

        if isinstance(tree, py.CompilationUnit):
            return self.visit_compilation_unit(tree, p)
        elif isinstance(tree, py.Async):
            return self.visit_async(tree, p)
        elif isinstance(tree, py.Await):
            return self.visit_await(tree, p)
        elif isinstance(tree, py.Binary):
            return self.visit_binary(tree, p)
        elif isinstance(tree, py.ChainedAssignment):
            return self.visit_chained_assignment(tree, p)
        elif isinstance(tree, py.CollectionLiteral):
            return self.visit_collection_literal(tree, p)
        elif isinstance(tree, py.ComprehensionExpression):
            return self.visit_comprehension_expression(tree, p)
        elif isinstance(tree, py.ComprehensionExpression.Clause):
            return self.visit_comprehension_clause(tree, p)
        elif isinstance(tree, py.ComprehensionExpression.Condition):
            return self.visit_comprehension_condition(tree, p)
        elif isinstance(tree, py.Del):
            return self.visit_del(tree, p)
        elif isinstance(tree, py.DictLiteral):
            return self.visit_dict_literal(tree, p)
        elif isinstance(tree, py.ErrorFrom):
            return self.visit_error_from(tree, p)
        elif isinstance(tree, py.ExceptionType):
            return self.visit_exception_type(tree, p)
        elif isinstance(tree, py.ExpressionStatement):
            return self.visit_expression_statement(tree, p)
        elif isinstance(tree, py.ExpressionTypeTree):
            return self.visit_expression_type_tree(tree, p)
        elif isinstance(tree, py.FormattedString):
            return self.visit_formatted_string(tree, p)
        elif isinstance(tree, py.FormattedString.Value):
            return self.visit_formatted_string_value(tree, p)
        elif isinstance(tree, py.KeyValue):
            return self.visit_key_value(tree, p)
        elif isinstance(tree, py.LiteralType):
            return self.visit_literal_type(tree, p)
        elif isinstance(tree, py.MatchCase):
            return self.visit_match_case(tree, p)
        elif isinstance(tree, py.MatchCase.Pattern):
            return self.visit_match_case_pattern(tree, p)
        elif isinstance(tree, py.MultiImport):
            return self.visit_multi_import(tree, p)
        elif isinstance(tree, py.NamedArgument):
            return self.visit_named_argument(tree, p)
        elif isinstance(tree, py.Pass):
            return self.visit_pass(tree, p)
        elif isinstance(tree, py.Slice):
            return self.visit_slice(tree, p)
        elif isinstance(tree, py.SpecialParameter):
            return self.visit_special_parameter(tree, p)
        elif isinstance(tree, py.Star):
            return self.visit_star(tree, p)
        elif isinstance(tree, py.StatementExpression):
            return self.visit_statement_expression(tree, p)
        elif isinstance(tree, py.TrailingElseWrapper):
            return self.visit_trailing_else_wrapper(tree, p)
        elif isinstance(tree, py.TypeAlias):
            return self.visit_type_alias(tree, p)
        elif isinstance(tree, py.TypeHint):
            return self.visit_type_hint(tree, p)
        elif isinstance(tree, py.TypeHintedExpression):
            return self.visit_type_hinted_expression(tree, p)
        elif isinstance(tree, py.UnionType):
            return self.visit_union_type(tree, p)
        elif isinstance(tree, py.VariableScope):
            return self.visit_variable_scope(tree, p)
        elif isinstance(tree, py.YieldFrom):
            return self.visit_yield_from(tree, p)
        else:
            raise ValueError(f"Unknown Python node type: {type(tree)}")

    # -------------------------------------------------------------------------
    # Helper methods
    # -------------------------------------------------------------------------

    def _java_marker_wrapper(self, out: str) -> str:
        return f"/*~~{out}{'~~' if out else ''}>*/"

    def _before_syntax(self, tree: Union[Py, J], loc: Optional[Any], p: PrintOutputCapture) -> None:
        """Handle marker printing and space before syntax."""
        prefix = tree.prefix
        markers = tree.markers

        for marker in markers.markers:
            p.append(p.get_marker_printer().before_prefix(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

        if loc is not None:
            self._visit_space(prefix, loc, p)

        self._visit_markers(markers, p)

        for marker in markers.markers:
            p.append(p.get_marker_printer().before_syntax(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

    def _after_syntax(self, tree: Union[Py, J], p: PrintOutputCapture) -> None:
        """Handle marker printing after syntax."""
        markers = tree.markers
        for marker in markers.markers:
            p.append(p.get_marker_printer().after_syntax(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

    def _visit_space(self, space: Optional[Space], loc: Any, p: PrintOutputCapture) -> Space:
        """Visit whitespace and comments.

        Space structure:
        - whitespace: prefix before any comments
        - comments: list of comments, each with its own suffix (whitespace after)

        Output order: whitespace + (comment + suffix) for each comment
        """
        if space is None:
            return Space.EMPTY

        # Print whitespace prefix first
        if space.whitespace:
            p.append(space.whitespace)

        # Print comments with their suffixes
        for comment in space.comments:
            self._visit_comment(comment, p)

        return space

    def _visit_comment(self, comment: Comment, p: PrintOutputCapture) -> None:
        """Visit a comment."""
        # Handle both TextComment and PyComment (Python's single-line comments)
        if isinstance(comment, TextComment):
            if comment.multiline:
                # Multi-line comment (docstring)
                p.append('"""')
                p.append(comment.text)
                p.append('"""')
            else:
                # Single-line comment
                p.append('#')
                p.append(comment.text)
        elif isinstance(comment, PyComment):
            # Python single-line comment (not multiline)
            p.append('#')
            p.append(comment.text)

        # Print suffix (whitespace after comment)
        if comment.suffix:
            p.append(comment.suffix)

    def _visit_markers(self, markers: Markers, p: PrintOutputCapture) -> Markers:
        """Visit markers that need printing (like TrailingComma, Semicolon)."""
        for marker in markers.markers:
            if isinstance(marker, Semicolon):
                p.append(';')
            elif isinstance(marker, TrailingComma):
                p.append(',')
                self._visit_space(marker.suffix, Space.Location.LANGUAGE_EXTENSION, p)
        return markers

    def _visit_right_padded(self, padded: JRightPadded, loc: Any, p: PrintOutputCapture) -> None:
        """Visit a right-padded element."""
        self.visit(padded.element, p)
        self._visit_space(padded.after, loc, p)
        self._visit_markers(padded.markers, p)

    def _visit_right_padded_list(
        self,
        nodes: List[JRightPadded],
        loc: Any,
        suffix_between: str,
        p: PrintOutputCapture
    ) -> None:
        """Visit a list of right-padded elements."""
        for i, node in enumerate(nodes):
            self.visit(node.element, p)
            self._visit_space(node.after, loc, p)
            self._visit_markers(node.markers, p)
            if i < len(nodes) - 1:
                p.append(suffix_between)

    def _visit_left_padded(self, prefix: str, padded: JLeftPadded, loc: Any, p: PrintOutputCapture) -> None:
        """Visit a left-padded element."""
        self._visit_space(padded.before, loc, p)
        p.append(prefix)
        self.visit(padded.element, p)
        self._visit_markers(padded.markers, p)

    def _visit_container(
        self,
        before: str,
        container: Optional[JContainer],
        loc: Any,
        suffix_between: str,
        after: Optional[str],
        p: PrintOutputCapture
    ) -> None:
        """Visit a container of elements."""
        if container is None:
            return
        self._visit_space(container.before, loc, p)
        p.append(before)
        self._visit_right_padded_list(container.padding.elements, loc, suffix_between, p)
        if after:
            p.append(after)

    # -------------------------------------------------------------------------
    # Python-specific visit methods
    # -------------------------------------------------------------------------

    def visit_compilation_unit(self, cu: 'py.CompilationUnit', p: PrintOutputCapture) -> J:
        """Visit a Python compilation unit."""
        from rewrite.java.tree import Import
        self._before_syntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p)

        # Print imports
        for imp in cu.padding.imports:
            self._visit_right_padded(imp, PyRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p)

        # Print statements
        for stmt in cu.padding.statements:
            self._visit_right_padded(stmt, PyRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p)

        # Print EOF space
        self._visit_space(cu.eof, Space.Location.COMPILATION_UNIT_EOF, p)

        # Handle SuppressNewline marker
        if cu.markers.find_first(SuppressNewline):
            if p.last_char() == '\n':
                # Remove trailing newline
                p._out[-1] = p._out[-1][:-1]

        self._after_syntax(cu, p)
        return cu

    def visit_async(self, async_: 'py.Async', p: PrintOutputCapture) -> J:
        """Visit an async statement."""
        self._before_syntax(async_, PySpace.Location.ASYNC_PREFIX, p)
        p.append("async")
        self.visit(async_.statement, p)
        return async_

    def visit_await(self, await_: 'py.Await', p: PrintOutputCapture) -> J:
        """Visit an await expression."""
        self._before_syntax(await_, PySpace.Location.AWAIT_PREFIX, p)
        p.append("await")
        self.visit(await_.expression, p)
        return await_

    def visit_binary(self, binary: 'py.Binary', p: PrintOutputCapture) -> J:
        """Visit a Python-specific binary expression."""
        from rewrite.python.tree import Binary as PyBinary
        self._before_syntax(binary, PySpace.Location.BINARY_PREFIX, p)
        self.visit(binary.left, p)
        self._visit_space(binary.padding.operator.before, PySpace.Location.BINARY_OPERATOR, p)

        op = binary.operator
        if op == PyBinary.Type.NotIn:
            p.append("not")
            if binary.negation is not None:
                self._visit_space(binary.negation, PySpace.Location.BINARY_NEGATION, p)
            else:
                p.append(' ')
            p.append("in")
        elif op == PyBinary.Type.In:
            p.append("in")
        elif op == PyBinary.Type.Is:
            p.append("is")
        elif op == PyBinary.Type.IsNot:
            p.append("is")
            if binary.negation is not None:
                self._visit_space(binary.negation, PySpace.Location.BINARY_NEGATION, p)
            else:
                p.append(' ')
            p.append("not")
        elif op == PyBinary.Type.FloorDivision:
            p.append("//")
        elif op == PyBinary.Type.MatrixMultiplication:
            p.append("@")
        elif op == PyBinary.Type.Power:
            p.append("**")
        elif op == PyBinary.Type.StringConcatenation:
            pass  # empty

        self.visit(binary.right, p)
        self._after_syntax(binary, p)
        return binary

    def visit_chained_assignment(self, chained: 'py.ChainedAssignment', p: PrintOutputCapture) -> J:
        """Visit a chained assignment."""
        self._before_syntax(chained, PySpace.Location.CHAINED_ASSIGNMENT_PREFIX, p)
        self._visit_right_padded_list(
            chained.padding.variables,
            PyRightPadded.Location.CHAINED_ASSIGNMENT_VARIABLES,
            "=",
            p
        )
        p.append('=')
        self.visit(chained.assignment, p)
        self._after_syntax(chained, p)
        return chained

    def visit_collection_literal(self, coll: 'py.CollectionLiteral', p: PrintOutputCapture) -> J:
        """Visit a collection literal."""
        from rewrite.python.tree import CollectionLiteral
        self._before_syntax(coll, PySpace.Location.COLLECTION_LITERAL_PREFIX, p)

        elements = coll.padding.elements
        kind = coll.kind

        if kind == CollectionLiteral.Kind.LIST:
            self._visit_container("[", elements, PyContainer.Location.COLLECTION_LITERAL_ELEMENTS, ",", "]", p)
        elif kind == CollectionLiteral.Kind.SET:
            self._visit_container("{", elements, PyContainer.Location.COLLECTION_LITERAL_ELEMENTS, ",", "}", p)
        elif kind == CollectionLiteral.Kind.TUPLE:
            if elements.markers.find_first(OmitParentheses):
                self._visit_container("", elements, PyContainer.Location.COLLECTION_LITERAL_ELEMENTS, ",", "", p)
            else:
                self._visit_container("(", elements, PyContainer.Location.COLLECTION_LITERAL_ELEMENTS, ",", ")", p)

        self._after_syntax(coll, p)
        return coll

    def visit_comprehension_expression(self, comp: 'py.ComprehensionExpression', p: PrintOutputCapture) -> J:
        """Visit a comprehension expression."""
        from rewrite.python.tree import ComprehensionExpression
        self._before_syntax(comp, PySpace.Location.COMPREHENSION_EXPRESSION_PREFIX, p)

        kind = comp.kind
        if kind in (ComprehensionExpression.Kind.DICT, ComprehensionExpression.Kind.SET):
            open_char = "{"
            close_char = "}"
        elif kind == ComprehensionExpression.Kind.LIST:
            open_char = "["
            close_char = "]"
        elif kind == ComprehensionExpression.Kind.GENERATOR:
            if comp.markers.find_first(OmitParentheses):
                open_char = ""
                close_char = ""
            else:
                open_char = "("
                close_char = ")"
        else:
            raise ValueError(f"Unknown comprehension kind: {kind}")

        p.append(open_char)
        self.visit(comp.result, p)
        for clause in comp.clauses:
            self.visit(clause, p)
        self._visit_space(comp.suffix, PySpace.Location.COMPREHENSION_EXPRESSION_SUFFIX, p)
        p.append(close_char)

        self._after_syntax(comp, p)
        return comp

    def visit_comprehension_clause(self, clause: 'py.ComprehensionExpression.Clause', p: PrintOutputCapture) -> J:
        """Visit a comprehension clause."""
        self._before_syntax(clause, PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_PREFIX, p)
        if clause.async_:
            p.append("async")
            self._visit_space(clause.padding.async_.after, PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_ASYNC_SUFFIX, p)
        p.append("for")
        self.visit(clause.iterator_variable, p)
        self._visit_space(clause.padding.iterated_list.before, PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST, p)
        p.append("in")
        self.visit(clause.iterated_list, p)
        if clause.conditions:
            for condition in clause.conditions:
                self.visit(condition, p)
        return clause

    def visit_comprehension_condition(self, condition: 'py.ComprehensionExpression.Condition', p: PrintOutputCapture) -> J:
        """Visit a comprehension condition."""
        self._before_syntax(condition, PySpace.Location.COMPREHENSION_EXPRESSION_CONDITION_PREFIX, p)
        p.append("if")
        self.visit(condition.expression, p)
        return condition

    def visit_del(self, del_: 'py.Del', p: PrintOutputCapture) -> J:
        """Visit a del statement."""
        self._before_syntax(del_, PySpace.Location.DEL_PREFIX, p)
        p.append("del")
        self._visit_right_padded_list(
            del_.padding.targets,
            PyRightPadded.Location.DEL_TARGETS,
            ",",
            p
        )
        return del_

    def visit_dict_literal(self, dict_: 'py.DictLiteral', p: PrintOutputCapture) -> J:
        """Visit a dict literal."""
        self._before_syntax(dict_, PySpace.Location.DICT_LITERAL_PREFIX, p)
        self._visit_container("{", dict_.padding.elements, PyContainer.Location.DICT_LITERAL_ELEMENTS, ",", "}", p)
        self._after_syntax(dict_, p)
        return dict_

    def visit_error_from(self, expr: 'py.ErrorFrom', p: PrintOutputCapture) -> J:
        """Visit an error from expression."""
        self._before_syntax(expr, PySpace.Location.ERROR_FROM_PREFIX, p)
        self.visit(expr.error, p)
        self._visit_space(expr.padding.from_.before, PySpace.Location.ERROR_FROM_EXPRESSION_FROM_PREFIX, p)
        p.append("from")
        self.visit(expr.from_, p)
        return expr

    def visit_exception_type(self, type_: 'py.ExceptionType', p: PrintOutputCapture) -> J:
        """Visit an exception type."""
        self._before_syntax(type_, PySpace.Location.EXCEPTION_TYPE_PREFIX, p)
        if type_.exception_group:
            p.append("*")
        self.visit(type_.expression, p)
        return type_

    def visit_expression_statement(self, stmt: 'py.ExpressionStatement', p: PrintOutputCapture) -> J:
        """Visit an expression statement (Python-specific wrapper)."""
        # ExpressionStatement is a thin wrapper, just visit the expression
        self.visit(stmt.expression, p)
        return stmt

    def visit_expression_type_tree(self, expr: 'py.ExpressionTypeTree', p: PrintOutputCapture) -> J:
        """Visit an expression type tree."""
        self._before_syntax(expr, PySpace.Location.EXPRESSION_TYPE_TREE_PREFIX, p)
        self.visit(expr.reference, p)
        self._after_syntax(expr, p)
        return expr

    def visit_formatted_string(self, fstring: 'py.FormattedString', p: PrintOutputCapture) -> J:
        """Visit a formatted string (f-string)."""
        self._before_syntax(fstring, PySpace.Location.FORMATTED_STRING_PREFIX, p)
        p.append(fstring.delimiter)
        for part in fstring.parts:
            self.visit(part, p)
        if fstring.delimiter:
            # Find the quote character position
            idx = max(fstring.delimiter.find("'"), fstring.delimiter.find('"'))
            if idx >= 0:
                p.append(fstring.delimiter[idx:])
        return fstring

    def visit_formatted_string_value(self, value: 'py.FormattedString.Value', p: PrintOutputCapture) -> J:
        """Visit a formatted string value."""
        from rewrite.python.tree import FormattedString
        self._before_syntax(value, PySpace.Location.FORMATTED_STRING_VALUE_PREFIX, p)
        p.append('{')
        self._visit_right_padded(value.padding.expression, PyRightPadded.Location.FORMATTED_STRING_VALUE_EXPRESSION, p)
        if value.padding.debug is not None:
            p.append('=')
            self._visit_space(value.padding.debug.after, PySpace.Location.FORMATTED_STRING_VALUE_DEBUG_SUFFIX, p)
        if value.conversion is not None:
            p.append('!')
            conv = value.conversion
            if conv == FormattedString.Value.Conversion.STR:
                p.append('s')
            elif conv == FormattedString.Value.Conversion.REPR:
                p.append('r')
            elif conv == FormattedString.Value.Conversion.ASCII:
                p.append('a')
        if value.format is not None:
            p.append(':')
            self.visit(value.format, p)
        p.append('}')
        return value

    def visit_key_value(self, kv: 'py.KeyValue', p: PrintOutputCapture) -> J:
        """Visit a key-value pair."""
        self._before_syntax(kv, PySpace.Location.KEY_VALUE_PREFIX, p)
        self._visit_right_padded(kv.padding.key, PyRightPadded.Location.KEY_VALUE_KEY, p)
        p.append(':')
        self.visit(kv.value, p)
        self._after_syntax(kv, p)
        return kv

    def visit_literal_type(self, lit: 'py.LiteralType', p: PrintOutputCapture) -> J:
        """Visit a literal type."""
        self._before_syntax(lit, PySpace.Location.LITERAL_TYPE_PREFIX, p)
        self.visit(lit.literal, p)
        self._after_syntax(lit, p)
        return lit

    def visit_match_case(self, match: 'py.MatchCase', p: PrintOutputCapture) -> J:
        """Visit a match case."""
        self._before_syntax(match, PySpace.Location.MATCH_CASE_PREFIX, p)
        self.visit(match.pattern, p)
        if match.padding.guard is not None:
            self._visit_space(match.padding.guard.before, PySpace.Location.MATCH_CASE_GUARD, p)
            p.append("if")
            self.visit(match.guard, p)
        return match

    def visit_match_case_pattern(self, pattern: 'py.MatchCase.Pattern', p: PrintOutputCapture) -> J:
        """Visit a match case pattern."""
        from rewrite.python.tree import MatchCase
        self._before_syntax(pattern, PySpace.Location.MATCH_CASE_PATTERN_PREFIX, p)

        children = pattern.padding.children
        kind = pattern.kind

        if kind == MatchCase.Pattern.Kind.AS:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "as", "", p)
        elif kind in (MatchCase.Pattern.Kind.CAPTURE, MatchCase.Pattern.Kind.LITERAL):
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "", "", p)
        elif kind == MatchCase.Pattern.Kind.CLASS:
            self._visit_space(children.before, PySpace.Location.MATCH_CASE_PATTERN_CHILDREN_PREFIX, p)
            elements = children.padding.elements
            self._visit_right_padded(elements[0], PyRightPadded.Location.MATCH_CASE_PATTERN_CHILD, p)
            rest = JContainer(Space.EMPTY, elements[1:], Markers.EMPTY)
            self._visit_container("(", rest, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ",", ")", p)
        elif kind == MatchCase.Pattern.Kind.DOUBLE_STAR:
            self._visit_container("**", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "", "", p)
        elif kind == MatchCase.Pattern.Kind.KEY_VALUE:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ":", "", p)
        elif kind == MatchCase.Pattern.Kind.KEYWORD:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "=", "", p)
        elif kind == MatchCase.Pattern.Kind.MAPPING:
            self._visit_container("{", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ",", "}", p)
        elif kind == MatchCase.Pattern.Kind.OR:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "|", "", p)
        elif kind == MatchCase.Pattern.Kind.SEQUENCE:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ",", "", p)
        elif kind == MatchCase.Pattern.Kind.SEQUENCE_LIST:
            self._visit_container("[", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ",", "]", p)
        elif kind in (MatchCase.Pattern.Kind.GROUP, MatchCase.Pattern.Kind.SEQUENCE_TUPLE):
            self._visit_container("(", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, ",", ")", p)
        elif kind == MatchCase.Pattern.Kind.STAR:
            self._visit_container("*", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "", "", p)
        elif kind == MatchCase.Pattern.Kind.VALUE:
            self._visit_container("", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "", "", p)
        elif kind == MatchCase.Pattern.Kind.WILDCARD:
            self._visit_container("_", children, PyContainer.Location.MATCH_CASE_PATTERN_CHILDREN, "", "", p)

        return pattern

    def visit_multi_import(self, multi: 'py.MultiImport', p: PrintOutputCapture) -> J:
        """Visit a multi-import statement."""
        self._before_syntax(multi, PySpace.Location.MULTI_IMPORT_PREFIX, p)
        if multi.from_ is not None:
            p.append("from")
            self._visit_right_padded(multi.padding.from_, PyRightPadded.Location.MULTI_IMPORT_FROM, p)
        p.append("import")
        if multi.parenthesized:
            self._visit_container("(", multi.padding.names, PyContainer.Location.MULTI_IMPORT_NAMES, ",", ")", p)
        else:
            self._visit_container("", multi.padding.names, PyContainer.Location.MULTI_IMPORT_NAMES, ",", "", p)
        self._after_syntax(multi, p)
        return multi

    def visit_named_argument(self, arg: 'py.NamedArgument', p: PrintOutputCapture) -> J:
        """Visit a named argument."""
        self._before_syntax(arg, PySpace.Location.NAMED_ARGUMENT, p)
        self.visit(arg.name, p)
        self._visit_left_padded("=", arg.padding.value, PyLeftPadded.Location.NAMED_ARGUMENT_VALUE, p)
        return arg

    def visit_pass(self, pass_: 'py.Pass', p: PrintOutputCapture) -> J:
        """Visit a pass statement."""
        self._before_syntax(pass_, PySpace.Location.PASS_PREFIX, p)
        p.append("pass")
        self._after_syntax(pass_, p)
        return pass_

    def visit_slice(self, slice_: 'py.Slice', p: PrintOutputCapture) -> J:
        """Visit a slice expression.

        Slice can be:
        - [:] - no start, stop is Empty, no step
        - [::] - no start, stop is Empty, step is Empty
        - [1:2] - start=1, stop=2
        - [1:2:3] - start=1, stop=2, step=3
        """
        self._before_syntax(slice_, PySpace.Location.SLICE_PREFIX, p)

        # Start value (before first colon)
        if slice_.padding.start is not None:
            self._visit_right_padded(slice_.padding.start, PyRightPadded.Location.SLICE_START, p)

        p.append(':')

        # Stop value (after first colon)
        if slice_.padding.stop is not None:
            self._visit_right_padded(slice_.padding.stop, PyRightPadded.Location.SLICE_STOP, p)

        # Step value (after second colon)
        if slice_.padding.step is not None:
            p.append(':')
            self._visit_right_padded(slice_.padding.step, PyRightPadded.Location.SLICE_STEP, p)

        return slice_

    def visit_special_parameter(self, param: 'py.SpecialParameter', p: PrintOutputCapture) -> J:
        """Visit a special parameter (*args or **kwargs)."""
        from rewrite.python.tree import SpecialParameter
        self._before_syntax(param, PySpace.Location.SPECIAL_PARAMETER_PREFIX, p)
        if param.kind == SpecialParameter.Kind.ARGS:
            p.append("*")
        elif param.kind == SpecialParameter.Kind.KWARGS:
            p.append("**")
        self._after_syntax(param, p)
        return param

    def visit_star(self, star: 'py.Star', p: PrintOutputCapture) -> J:
        """Visit a star expression."""
        from rewrite.python.tree import Star
        self._before_syntax(star, PySpace.Location.STAR_PREFIX, p)
        if star.kind == Star.Kind.LIST:
            p.append("*")
        elif star.kind == Star.Kind.DICT:
            p.append("**")
        self.visit(star.expression, p)
        self._after_syntax(star, p)
        return star

    def visit_statement_expression(self, stmt: 'py.StatementExpression', p: PrintOutputCapture) -> J:
        """Visit a statement expression.

        StatementExpression is a thin wrapper around statements that are also expressions
        (like yield, assignment expressions, etc.). It doesn't need its own prefix/suffix
        handling since the contained statement already has the correct spacing.
        """
        self._visit_markers(stmt.markers, p)
        self.visit(stmt.statement, p)
        return stmt

    def visit_trailing_else_wrapper(self, wrapper: 'py.TrailingElseWrapper', p: PrintOutputCapture) -> J:
        """Visit a trailing else wrapper."""
        from rewrite.java.tree import Try
        self._before_syntax(wrapper, PySpace.Location.TRAILING_ELSE_WRAPPER_PREFIX, p)
        self.visit(wrapper.statement, p)
        if not isinstance(wrapper.statement, Try):
            self._visit_space(wrapper.padding.else_block.before, Space.Location.ELSE_PREFIX, p)
            p.append("else")
            self.visit(wrapper.else_block, p)
        self._after_syntax(wrapper, p)
        return wrapper

    def visit_type_alias(self, alias: 'py.TypeAlias', p: PrintOutputCapture) -> J:
        """Visit a type alias."""
        self._before_syntax(alias, PySpace.Location.UNION_TYPE_PREFIX, p)
        p.append("type")
        self.visit(alias.name, p)
        self._visit_left_padded("=", alias.padding.value, PyLeftPadded.Location.TYPE_ALIAS_VALUE, p)
        self._after_syntax(alias, p)
        return alias

    def visit_type_hint(self, hint: 'py.TypeHint', p: PrintOutputCapture) -> J:
        """Visit a type hint."""
        from rewrite.java.tree import MethodDeclaration
        self._before_syntax(hint, PySpace.Location.TYPE_HINT_PREFIX, p)
        parent = self.get_cursor().parent
        if parent and isinstance(parent.value, MethodDeclaration):
            p.append("->")
        else:
            p.append(':')
        self.visit(hint.type_tree, p)
        self._after_syntax(hint, p)
        return hint

    def visit_type_hinted_expression(self, expr: 'py.TypeHintedExpression', p: PrintOutputCapture) -> J:
        """Visit a type-hinted expression."""
        self._before_syntax(expr, PySpace.Location.TYPE_HINTED_EXPRESSION_PREFIX, p)
        self.visit(expr.expression, p)
        self.visit(expr.type_hint, p)
        self._after_syntax(expr, p)
        return expr

    def visit_union_type(self, union: 'py.UnionType', p: PrintOutputCapture) -> J:
        """Visit a union type."""
        self._before_syntax(union, PySpace.Location.UNION_TYPE_PREFIX, p)
        self._visit_right_padded_list(union.padding.types, PyRightPadded.Location.UNION_TYPE_TYPES, "|", p)
        self._after_syntax(union, p)
        return union

    def visit_variable_scope(self, scope: 'py.VariableScope', p: PrintOutputCapture) -> J:
        """Visit a variable scope statement (global/nonlocal)."""
        from rewrite.python.tree import VariableScope
        self._before_syntax(scope, PySpace.Location.VARIABLE_SCOPE_PREFIX, p)
        if scope.kind == VariableScope.Kind.GLOBAL:
            p.append("global")
        elif scope.kind == VariableScope.Kind.NONLOCAL:
            p.append("nonlocal")
        self._visit_right_padded_list(scope.padding.names, PyRightPadded.Location.VARIABLE_SCOPE_NAMES, ",", p)
        return scope

    def visit_yield_from(self, yield_: 'py.YieldFrom', p: PrintOutputCapture) -> J:
        """Visit a yield from expression."""
        self._before_syntax(yield_, PySpace.Location.YIELD_FROM_PREFIX, p)
        p.append("from")
        self.visit(yield_.expression, p)
        return yield_


class PythonJavaPrinter:
    """
    Java printer delegate for Python.

    Handles printing of J.* nodes with Python-specific syntax.
    Routes Py.* nodes back to PythonPrinter.
    """

    def __init__(self, python_printer: PythonPrinter):
        self._python_printer = python_printer
        self._cursor: Optional[Cursor] = None

    def get_cursor(self) -> Cursor:
        if self._cursor is None:
            raise RuntimeError("Cursor not set")
        return self._cursor

    def set_cursor(self, cursor: Optional[Cursor]) -> None:
        self._cursor = cursor
        # Sync cursor back to PythonPrinter to keep them in sync
        self._python_printer._cursor = cursor

    def visit(self, tree: Optional[Tree], p: PrintOutputCapture) -> Optional[J]:
        """Visit a tree node."""
        if tree is None:
            return None

        if isinstance(tree, Py):
            # Route back to Python printer (which handles cursor)
            return self._python_printer.visit(tree, p)

        # For J nodes, update cursor before visiting
        # Only update if cursor doesn't already have this tree (avoid duplication when delegated from PythonPrinter)
        current_cursor = self.get_cursor()

        if current_cursor.value is tree:
            # Already set by PythonPrinter.visit delegation
            return self._visit_java(tree, p)

        # Update cursor for internal visit calls
        self.set_cursor(Cursor(current_cursor, tree))
        try:
            return self._visit_java(tree, p)
        finally:
            # Restore cursor
            parent = self.get_cursor().parent
            self.set_cursor(parent)

    def _visit_java(self, tree: J, p: PrintOutputCapture) -> Optional[J]:
        """Visit a Java node with Python syntax."""
        from rewrite.java import tree as j

        if isinstance(tree, j.Annotation):
            return self.visit_annotation(tree, p)
        elif isinstance(tree, j.ArrayAccess):
            return self.visit_array_access(tree, p)
        elif isinstance(tree, j.ArrayDimension):
            return self.visit_array_dimension(tree, p)
        elif isinstance(tree, j.Assert):
            return self.visit_assert(tree, p)
        elif isinstance(tree, j.Assignment):
            return self.visit_assignment(tree, p)
        elif isinstance(tree, j.AssignmentOperation):
            return self.visit_assignment_operation(tree, p)
        elif isinstance(tree, j.Binary):
            return self.visit_binary(tree, p)
        elif isinstance(tree, j.Block):
            return self.visit_block(tree, p)
        elif isinstance(tree, j.Break):
            return self.visit_break(tree, p)
        elif isinstance(tree, j.Case):
            return self.visit_case(tree, p)
        elif isinstance(tree, j.Try.Catch):
            return self.visit_catch(tree, p)
        elif isinstance(tree, j.ClassDeclaration):
            return self.visit_class_declaration(tree, p)
        elif isinstance(tree, j.Continue):
            return self.visit_continue(tree, p)
        elif isinstance(tree, j.ControlParentheses):
            return self.visit_control_parentheses(tree, p)
        elif isinstance(tree, j.If.Else):
            return self.visit_else(tree, p)
        elif isinstance(tree, j.Empty):
            return self.visit_empty(tree, p)
        elif isinstance(tree, j.FieldAccess):
            return self.visit_field_access(tree, p)
        elif isinstance(tree, j.ForEachLoop.Control):
            return self.visit_for_each_control(tree, p)
        elif isinstance(tree, j.ForEachLoop):
            return self.visit_for_each_loop(tree, p)
        elif isinstance(tree, j.Identifier):
            return self.visit_identifier(tree, p)
        elif isinstance(tree, j.If):
            return self.visit_if(tree, p)
        elif isinstance(tree, j.Import):
            return self.visit_import(tree, p)
        elif isinstance(tree, j.Lambda):
            return self.visit_lambda(tree, p)
        elif isinstance(tree, j.Literal):
            return self.visit_literal(tree, p)
        elif isinstance(tree, j.MethodDeclaration):
            return self.visit_method_declaration(tree, p)
        elif isinstance(tree, j.MethodInvocation):
            return self.visit_method_invocation(tree, p)
        elif isinstance(tree, j.Modifier):
            return self.visit_modifier(tree, p)
        elif isinstance(tree, j.NewArray):
            return self.visit_new_array(tree, p)
        elif isinstance(tree, j.ParameterizedType):
            return self.visit_parameterized_type(tree, p)
        elif isinstance(tree, j.Parentheses):
            return self.visit_parentheses(tree, p)
        elif isinstance(tree, j.Return):
            return self.visit_return(tree, p)
        elif isinstance(tree, j.Switch):
            return self.visit_switch(tree, p)
        elif isinstance(tree, j.Ternary):
            return self.visit_ternary(tree, p)
        elif isinstance(tree, j.Throw):
            return self.visit_throw(tree, p)
        elif isinstance(tree, j.Try):
            return self.visit_try(tree, p)
        elif isinstance(tree, j.Try.Resource):
            return self.visit_try_resource(tree, p)
        elif isinstance(tree, j.Unary):
            return self.visit_unary(tree, p)
        elif isinstance(tree, j.VariableDeclarations.NamedVariable):
            return self.visit_variable(tree, p)
        elif isinstance(tree, j.VariableDeclarations):
            return self.visit_variable_declarations(tree, p)
        elif isinstance(tree, j.WhileLoop):
            return self.visit_while_loop(tree, p)
        elif isinstance(tree, j.Yield):
            return self.visit_yield(tree, p)
        else:
            raise ValueError(f"Unknown Java node type: {type(tree)}")

    # -------------------------------------------------------------------------
    # Helper methods
    # -------------------------------------------------------------------------

    def _java_marker_wrapper(self, out: str) -> str:
        return f"/*~~{out}{'~~' if out else ''}>*/"

    def _before_syntax(self, tree: J, loc: Optional[Any], p: PrintOutputCapture) -> None:
        """Handle marker printing and space before syntax."""
        prefix = tree.prefix
        markers = tree.markers

        for marker in markers.markers:
            p.append(p.get_marker_printer().before_prefix(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

        if loc is not None:
            self._visit_space(prefix, loc, p)

        self._visit_markers(markers, p)

        for marker in markers.markers:
            p.append(p.get_marker_printer().before_syntax(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

    def _after_syntax(self, tree: J, p: PrintOutputCapture) -> None:
        """Handle marker printing after syntax."""
        markers = tree.markers
        for marker in markers.markers:
            p.append(p.get_marker_printer().after_syntax(
                marker, Cursor(self.get_cursor(), marker), self._java_marker_wrapper
            ))

    def _visit_space(self, space: Optional[Space], loc: Any, p: PrintOutputCapture) -> Space:
        """Visit whitespace and comments.

        Space structure:
        - whitespace: prefix before any comments
        - comments: list of comments, each with its own suffix (whitespace after)

        Output order: whitespace + (comment + suffix) for each comment
        """
        if space is None:
            return Space.EMPTY

        # Print whitespace prefix first
        if space.whitespace:
            p.append(space.whitespace)

        # Print comments with their suffixes
        for comment in space.comments:
            self._visit_comment(comment, p)

        return space

    def _visit_comment(self, comment: Comment, p: PrintOutputCapture) -> None:
        """Visit a comment."""
        # Handle both TextComment and PyComment (Python's single-line comments)
        if isinstance(comment, TextComment):
            if comment.multiline:
                # Multi-line comment (docstring)
                p.append('"""')
                p.append(comment.text)
                p.append('"""')
            else:
                # Single-line comment
                p.append('#')
                p.append(comment.text)
        elif isinstance(comment, PyComment):
            # Python single-line comment (not multiline)
            p.append('#')
            p.append(comment.text)

        # Print suffix (whitespace after comment)
        if comment.suffix:
            p.append(comment.suffix)

    def _visit_markers(self, markers: Markers, p: PrintOutputCapture) -> Markers:
        """Visit markers that need printing (like TrailingComma, Semicolon)."""
        for marker in markers.markers:
            self._visit_marker(marker, p)
        return markers

    def _visit_marker(self, marker: Marker, p: PrintOutputCapture) -> Marker:
        """Visit a single marker."""
        if isinstance(marker, Semicolon):
            p.append(';')
        elif isinstance(marker, TrailingComma):
            p.append(',')
            self._visit_space(marker.suffix, Space.Location.LANGUAGE_EXTENSION, p)
        return marker

    def _visit_right_padded(self, padded: JRightPadded, loc: Any, p: PrintOutputCapture, suffix: str = "") -> None:
        """Visit a right-padded element."""
        self.visit(padded.element, p)
        self._visit_space(padded.after, loc, p)
        self._visit_markers(padded.markers, p)
        if suffix:
            p.append(suffix)

    def _visit_right_padded_list(
        self,
        nodes: List[JRightPadded],
        loc: Any,
        suffix_between: str,
        p: PrintOutputCapture
    ) -> None:
        """Visit a list of right-padded elements."""
        for i, node in enumerate(nodes):
            self.visit(node.element, p)
            self._visit_space(node.after, loc, p)
            self._visit_markers(node.markers, p)
            if i < len(nodes) - 1:
                p.append(suffix_between)

    def _visit_left_padded(self, prefix: str, padded: JLeftPadded, loc: Any, p: PrintOutputCapture) -> None:
        """Visit a left-padded element."""
        self._visit_space(padded.before, loc, p)
        p.append(prefix)
        self.visit(padded.element, p)
        self._visit_markers(padded.markers, p)

    def _visit_container(
        self,
        before: str,
        container: Optional[JContainer],
        loc: Any,
        suffix_between: str,
        after: Optional[str],
        p: PrintOutputCapture
    ) -> None:
        """Visit a container of elements."""
        if container is None:
            return
        self._visit_space(container.before, loc, p)
        p.append(before)
        self._visit_right_padded_list(container.padding.elements, loc, suffix_between, p)
        if after:
            p.append(after)

    def _visit_statements(self, statements: List[JRightPadded], loc: Any, p: PrintOutputCapture) -> None:
        """Visit a list of statements."""
        for stmt in statements:
            self.visit(stmt.element, p)
            self._visit_space(stmt.after, loc, p)
            # Handle statement markers like Semicolon
            for marker in stmt.markers.markers:
                self._visit_marker(marker, p)

    # -------------------------------------------------------------------------
    # Java node visit methods (with Python syntax)
    # -------------------------------------------------------------------------

    def visit_annotation(self, annotation: 'j.Annotation', p: PrintOutputCapture) -> J:
        """Visit an annotation (decorator in Python)."""
        self._before_syntax(annotation, Space.Location.ANNOTATION_PREFIX, p)
        p.append("@")
        self.visit(annotation.annotation_type, p)
        self._visit_container("(", annotation.padding.arguments, JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p)
        self._after_syntax(annotation, p)
        return annotation

    def visit_array_access(self, access: 'j.ArrayAccess', p: PrintOutputCapture) -> J:
        """Visit an array access (subscript in Python)."""
        self._before_syntax(access, Space.Location.ARRAY_ACCESS_PREFIX, p)
        self.visit(access.indexed, p)
        self.visit(access.dimension, p)
        self._after_syntax(access, p)
        return access

    def visit_array_dimension(self, dimension: 'j.ArrayDimension', p: PrintOutputCapture) -> J:
        """Visit an array dimension."""
        self._before_syntax(dimension, Space.Location.DIMENSION_PREFIX, p)
        p.append("[")
        self._visit_right_padded(dimension.padding.index, JRightPadded.Location.ARRAY_INDEX, p, "]")
        self._after_syntax(dimension, p)
        return dimension

    def visit_assert(self, assert_: 'j.Assert', p: PrintOutputCapture) -> J:
        """Visit an assert statement."""
        self._before_syntax(assert_, Space.Location.ASSERT_PREFIX, p)
        p.append("assert")
        self.visit(assert_.condition, p)
        if assert_.detail is not None:
            self._visit_left_padded(",", assert_.detail, JLeftPadded.Location.ASSERT_DETAIL, p)
        self._after_syntax(assert_, p)
        return assert_

    def visit_assignment(self, assignment: 'j.Assignment', p: PrintOutputCapture) -> J:
        """Visit an assignment."""
        from rewrite.python import tree as py
        from rewrite.java import tree as j

        # Determine if this is a walrus operator (:=) or regular assignment (=)
        parent = self.get_cursor().parent
        parent_value = parent.value if parent else None

        is_regular_assignment = (
            isinstance(parent_value, j.Block) or
            isinstance(parent_value, py.CompilationUnit) or
            (isinstance(parent_value, j.If) and parent_value.then_part == assignment) or
            (isinstance(parent_value, j.If.Else) and parent_value.body == assignment) or
            (isinstance(parent_value, Loop) and parent_value.body == assignment)
        )

        symbol = "=" if is_regular_assignment else ":="

        self._before_syntax(assignment, Space.Location.ASSIGNMENT_PREFIX, p)
        self.visit(assignment.variable, p)
        self._visit_left_padded(symbol, assignment.padding.assignment, JLeftPadded.Location.ASSIGNMENT, p)
        self._after_syntax(assignment, p)
        return assignment

    def visit_assignment_operation(self, assign_op: 'j.AssignmentOperation', p: PrintOutputCapture) -> J:
        """Visit an assignment operation (+=, -=, etc.)."""
        from rewrite.java.tree import AssignmentOperation

        op_map = {
            AssignmentOperation.Type.Addition: "+=",
            AssignmentOperation.Type.Subtraction: "-=",
            AssignmentOperation.Type.Multiplication: "*=",
            AssignmentOperation.Type.Division: "/=",
            AssignmentOperation.Type.Modulo: "%=",
            AssignmentOperation.Type.BitAnd: "&=",
            AssignmentOperation.Type.BitOr: "|=",
            AssignmentOperation.Type.BitXor: "^=",
            AssignmentOperation.Type.LeftShift: "<<=",
            AssignmentOperation.Type.RightShift: ">>=",
            AssignmentOperation.Type.UnsignedRightShift: ">>>=",
            AssignmentOperation.Type.Exponentiation: "**=",
            AssignmentOperation.Type.FloorDivision: "//=",
            AssignmentOperation.Type.MatrixMultiplication: "@=",
        }

        keyword = op_map.get(assign_op.operator, "")

        self._before_syntax(assign_op, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p)
        self.visit(assign_op.variable, p)
        self._visit_space(assign_op.padding.operator.before, Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p)
        p.append(keyword)
        self.visit(assign_op.assignment, p)
        self._after_syntax(assign_op, p)
        return assign_op

    def visit_binary(self, binary: 'j.Binary', p: PrintOutputCapture) -> J:
        """Visit a binary expression."""
        from rewrite.java.tree import Binary

        op_map = {
            Binary.Type.Addition: "+",
            Binary.Type.Subtraction: "-",
            Binary.Type.Multiplication: "*",
            Binary.Type.Division: "/",
            Binary.Type.Modulo: "%",
            Binary.Type.LessThan: "<",
            Binary.Type.GreaterThan: ">",
            Binary.Type.LessThanOrEqual: "<=",
            Binary.Type.GreaterThanOrEqual: ">=",
            Binary.Type.Equal: "==",
            Binary.Type.NotEqual: "!=",
            Binary.Type.BitAnd: "&",
            Binary.Type.BitOr: "|",
            Binary.Type.BitXor: "^",
            Binary.Type.LeftShift: "<<",
            Binary.Type.RightShift: ">>",
            Binary.Type.UnsignedRightShift: ">>>",
            Binary.Type.Or: "or",
            Binary.Type.And: "and",
        }

        keyword = op_map.get(binary.operator, "")

        self._before_syntax(binary, Space.Location.BINARY_PREFIX, p)
        self.visit(binary.left, p)
        self._visit_space(binary.padding.operator.before, Space.Location.BINARY_OPERATOR, p)
        p.append(keyword)
        self.visit(binary.right, p)
        self._after_syntax(binary, p)
        return binary

    def visit_block(self, block: 'j.Block', p: PrintOutputCapture) -> J:
        """Visit a block (indented suite in Python)."""
        self._before_syntax(block, Space.Location.BLOCK_PREFIX, p)
        p.append(':')
        self._visit_statements(block.padding.statements, JRightPadded.Location.BLOCK_STATEMENT, p)
        self._visit_space(block.end, Space.Location.BLOCK_END, p)
        self._after_syntax(block, p)
        return block

    def visit_break(self, break_: 'j.Break', p: PrintOutputCapture) -> J:
        """Visit a break statement."""
        self._before_syntax(break_, Space.Location.BREAK_PREFIX, p)
        p.append("break")
        self._after_syntax(break_, p)
        return break_

    def visit_case(self, case: 'j.Case', p: PrintOutputCapture) -> J:
        """Visit a case (match case in Python)."""
        from rewrite.java import tree as j
        self._before_syntax(case, Space.Location.CASE_PREFIX, p)
        elem = case.case_labels[0] if case.case_labels else None
        if not (isinstance(elem, j.Identifier) and elem.simple_name == "default"):
            p.append("case")
        self._visit_container("", case.padding.case_labels, JContainer.Location.CASE_EXPRESSION, ",", "", p)
        self._visit_space(case.padding.statements.before, Space.Location.CASE, p)
        self._visit_statements(case.padding.statements.padding.elements, JRightPadded.Location.CASE, p)
        if case.body and isinstance(case.body, Statement):
            self._visit_right_padded(case.padding.body, JRightPadded.Location.LANGUAGE_EXTENSION, p)
        elif case.body:
            self._visit_right_padded(case.padding.body, JRightPadded.Location.CASE_BODY, p, ";")
        self._after_syntax(case, p)
        return case

    def visit_catch(self, catch: 'j.Try.Catch', p: PrintOutputCapture) -> J:
        """Visit a catch clause (except in Python)."""
        from rewrite.java import tree as j

        self._before_syntax(catch, Space.Location.CATCH_PREFIX, p)
        p.append("except")

        multi_variable = catch.parameter.tree
        self._before_syntax(multi_variable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p)
        self.visit(multi_variable.type_expression, p)

        for padded_variable in multi_variable.padding.variables:
            variable = padded_variable.element
            if variable.name.simple_name:
                self._visit_space(padded_variable.after, Space.Location.LANGUAGE_EXTENSION, p)
                self._before_syntax(variable, Space.Location.VARIABLE_PREFIX, p)
                p.append("as")
                self.visit(variable.name, p)
                self._after_syntax(variable, p)

        self._after_syntax(multi_variable, p)
        self.visit(catch.body, p)
        self._after_syntax(catch, p)
        return catch

    def visit_class_declaration(self, class_decl: 'j.ClassDeclaration', p: PrintOutputCapture) -> J:
        """Visit a class declaration."""
        self._before_syntax(class_decl, Space.Location.CLASS_DECLARATION_PREFIX, p)
        self._visit_space(Space.EMPTY, Space.Location.ANNOTATIONS, p)

        # Visit leading annotations (decorators)
        for annotation in class_decl.leading_annotations:
            self.visit(annotation, p)

        # Visit kind annotations
        if class_decl.padding.kind.annotations:
            for annotation in class_decl.padding.kind.annotations:
                self.visit(annotation, p)

        self._visit_space(class_decl.padding.kind.prefix, Space.Location.CLASS_KIND, p)
        p.append("class")
        self.visit(class_decl.name, p)

        # Visit implements (base classes in Python)
        if class_decl.padding.implements:
            omit_parens = class_decl.padding.implements.markers.find_first(OmitParentheses)
            before = "" if omit_parens else "("
            after = "" if omit_parens else ")"
            self._visit_container(before, class_decl.padding.implements, JContainer.Location.IMPLEMENTS, ",", after, p)

        self.visit(class_decl.body, p)
        self._after_syntax(class_decl, p)
        return class_decl

    def visit_continue(self, continue_: 'j.Continue', p: PrintOutputCapture) -> J:
        """Visit a continue statement."""
        self._before_syntax(continue_, Space.Location.CONTINUE_PREFIX, p)
        p.append("continue")
        self._after_syntax(continue_, p)
        return continue_

    def visit_control_parentheses(self, control_parens: 'j.ControlParentheses', p: PrintOutputCapture) -> J:
        """Visit control parentheses (condition in if/while)."""
        self._before_syntax(control_parens, Space.Location.CONTROL_PARENTHESES_PREFIX, p)
        self._visit_right_padded(control_parens.padding.tree, JRightPadded.Location.PARENTHESES, p)
        self._after_syntax(control_parens, p)
        return control_parens

    def visit_else(self, else_: 'j.If.Else', p: PrintOutputCapture) -> J:
        """Visit an else clause."""
        from rewrite.java import tree as j

        self._before_syntax(else_, Space.Location.ELSE_PREFIX, p)
        parent = self.get_cursor().parent
        parent_value = parent.value if parent else None

        # Check if this is an elif (else with If body, where parent is also If)
        is_elif = isinstance(parent_value, j.If) and isinstance(else_.body, j.If)

        if is_elif:
            # elif - print "el" and then let the nested If print "if"
            p.append("el")
            self.visit(else_.body, p)
        elif isinstance(else_.body, j.Block):
            p.append("else")
            self.visit(else_.body, p)
        else:
            p.append("else")
            p.append(':')
            self.visit(else_.body, p)

        self._after_syntax(else_, p)
        return else_

    def visit_empty(self, empty: 'j.Empty', p: PrintOutputCapture) -> J:
        """Visit an empty element."""
        self._before_syntax(empty, Space.Location.EMPTY_PREFIX, p)
        self._after_syntax(empty, p)
        return empty

    def visit_field_access(self, field_access: 'j.FieldAccess', p: PrintOutputCapture) -> J:
        """Visit a field access (attribute access in Python)."""
        self._before_syntax(field_access, Space.Location.FIELD_ACCESS_PREFIX, p)
        self.visit(field_access.target, p)
        self._visit_left_padded(".", field_access.padding.name, JLeftPadded.Location.FIELD_ACCESS_NAME, p)
        self._after_syntax(field_access, p)
        return field_access

    def visit_for_each_control(self, control: 'j.ForEachLoop.Control', p: PrintOutputCapture) -> J:
        """Visit for-each control (for-in in Python)."""
        self._before_syntax(control, Space.Location.FOR_EACH_CONTROL_PREFIX, p)
        self._visit_right_padded(control.padding.variable, JRightPadded.Location.FOREACH_VARIABLE, p)
        p.append("in")
        self._visit_right_padded(control.padding.iterable, JRightPadded.Location.FOREACH_ITERABLE, p)
        self._after_syntax(control, p)
        return control

    def visit_for_each_loop(self, for_loop: 'j.ForEachLoop', p: PrintOutputCapture) -> J:
        """Visit a for-each loop."""
        self._before_syntax(for_loop, Space.Location.FOR_EACH_LOOP_PREFIX, p)
        p.append("for")
        self.visit(for_loop.control, p)
        self.visit(for_loop.body, p)
        self._after_syntax(for_loop, p)
        return for_loop

    def visit_identifier(self, ident: 'j.Identifier', p: PrintOutputCapture) -> J:
        """Visit an identifier."""
        self._before_syntax(ident, Space.Location.IDENTIFIER_PREFIX, p)
        quoted = ident.markers.find_first(Quoted)
        if quoted:
            p.append(quoted.style.quote)
        p.append(ident.simple_name)
        if quoted:
            p.append(quoted.style.quote)
        self._after_syntax(ident, p)
        return ident

    def visit_if(self, if_: 'j.If', p: PrintOutputCapture) -> J:
        """Visit an if statement."""
        from rewrite.java import tree as j

        self._before_syntax(if_, Space.Location.IF_PREFIX, p)
        p.append("if")
        self.visit(if_.if_condition, p)

        then_part = if_.padding.then_part
        if not isinstance(then_part.element, j.Block):
            p.append(":")

        self._visit_right_padded(then_part, JRightPadded.Location.IF_THEN, p)
        self.visit(if_.else_part, p)
        self._after_syntax(if_, p)
        return if_

    def visit_import(self, import_: 'j.Import', p: PrintOutputCapture) -> J:
        """Visit an import statement."""
        from rewrite.java import tree as j

        self._before_syntax(import_, Space.Location.IMPORT_PREFIX, p)

        if isinstance(import_.qualid.target, j.Empty):
            self.visit(import_.qualid.name, p)
        else:
            self.visit(import_.qualid, p)

        if import_.padding.alias:
            self._visit_left_padded("as", import_.padding.alias, JLeftPadded.Location.IMPORT_ALIAS_PREFIX, p)

        self._after_syntax(import_, p)
        return import_

    def visit_lambda(self, lambda_: 'j.Lambda', p: PrintOutputCapture) -> J:
        """Visit a lambda expression."""
        self._before_syntax(lambda_, Space.Location.LAMBDA_PREFIX, p)
        p.append("lambda")
        self._visit_space(lambda_.parameters.prefix, Space.Location.LAMBDA_PARAMETERS_PREFIX, p)
        self._visit_markers(lambda_.parameters.markers, p)
        self._visit_right_padded_list(lambda_.parameters.padding.parameters, JRightPadded.Location.LAMBDA_PARAM, ",", p)
        self._visit_space(lambda_.arrow, Space.Location.LAMBDA_ARROW_PREFIX, p)
        p.append(":")
        self.visit(lambda_.body, p)
        self._after_syntax(lambda_, p)
        return lambda_

    def visit_literal(self, literal: 'j.Literal', p: PrintOutputCapture) -> J:
        """Visit a literal."""
        value_source = literal.value_source

        if literal.value is None and value_source is None:
            value_source = "None"

        self._before_syntax(literal, Space.Location.LITERAL_PREFIX, p)

        unicode_escapes = literal.unicode_escapes
        if unicode_escapes is None:
            p.append(value_source)
        elif value_source:
            # Handle unicode escapes
            surrogate_iter = iter(unicode_escapes)
            surrogate = next(surrogate_iter, None)
            i = 0

            if surrogate and surrogate.value_source_index == 0:
                p.append(f"\\u{surrogate.code_point}")
                surrogate = next(surrogate_iter, None)

            for c in value_source:
                p.append(c)
                i += 1
                while surrogate and surrogate.value_source_index == i:
                    p.append(f"\\u{surrogate.code_point}")
                    surrogate = next(surrogate_iter, None)

        self._after_syntax(literal, p)
        return literal

    def visit_method_declaration(self, method: 'j.MethodDeclaration', p: PrintOutputCapture) -> J:
        """Visit a method declaration (function definition in Python)."""
        self._before_syntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p)
        self._visit_space(Space.EMPTY, Space.Location.ANNOTATIONS, p)

        # Visit leading annotations (decorators)
        for annotation in method.leading_annotations:
            self.visit(annotation, p)

        # Visit modifiers
        for mod in method.modifiers:
            self.visit_modifier(mod, p)

        self.visit(method.name, p)
        self._visit_container("(", method.padding.parameters, JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p)
        self.visit(method.return_type_expression, p)
        self.visit(method.body, p)
        self._after_syntax(method, p)
        return method

    def visit_method_invocation(self, method: 'j.MethodInvocation', p: PrintOutputCapture) -> J:
        """Visit a method invocation (function call)."""
        self._before_syntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p)

        # Visit select with appropriate separator
        if method.padding.select:
            suffix = "" if not method.name.simple_name else "."
            self._visit_right_padded(method.padding.select, JRightPadded.Location.METHOD_SELECT, p, suffix)

        # Visit type parameters
        self._visit_container("<", method.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, ",", ">", p)

        self.visit(method.name, p)

        # Visit arguments
        before = "("
        after = ")"
        if method.markers.find_first(OmitParentheses):
            before = ""
            after = ""
        self._visit_container(before, method.padding.arguments, JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", after, p)

        self._after_syntax(method, p)
        return method

    def visit_modifier(self, mod: 'j.Modifier', p: PrintOutputCapture) -> J:
        """Visit a modifier (def, async in Python)."""
        from rewrite.java.tree import Modifier

        keyword = None
        if mod.type == Modifier.Type.Default:
            keyword = "def"
        elif mod.type == Modifier.Type.Async:
            keyword = "async"

        if keyword:
            for annotation in mod.annotations:
                self.visit(annotation, p)
            self._before_syntax(mod, Space.Location.MODIFIER_PREFIX, p)
            p.append(keyword)
            self._after_syntax(mod, p)

        return mod

    def visit_new_array(self, new_array: 'j.NewArray', p: PrintOutputCapture) -> J:
        """Visit a new array (list literal in Python)."""
        self._before_syntax(new_array, Space.Location.NEW_ARRAY_PREFIX, p)
        self._visit_container("[", new_array.padding.initializer, JContainer.Location.NEW_ARRAY_INITIALIZER, ",", "]", p)
        self._after_syntax(new_array, p)
        return new_array

    def visit_parameterized_type(self, type_: 'j.ParameterizedType', p: PrintOutputCapture) -> J:
        """Visit a parameterized type (generic type in Python)."""
        self._before_syntax(type_, Space.Location.PARAMETERIZED_TYPE_PREFIX, p)
        self.visit(type_.clazz, p)
        self._visit_container("[", type_.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, ",", "]", p)
        self._after_syntax(type_, p)
        return type_

    def visit_parentheses(self, parens: 'j.Parentheses', p: PrintOutputCapture) -> J:
        """Visit parentheses."""
        self._before_syntax(parens, Space.Location.PARENTHESES_PREFIX, p)
        p.append("(")
        self._visit_right_padded(parens.padding.tree, JRightPadded.Location.PARENTHESES, p, ")")
        self._after_syntax(parens, p)
        return parens

    def visit_return(self, return_: 'j.Return', p: PrintOutputCapture) -> J:
        """Visit a return statement."""
        self._before_syntax(return_, Space.Location.RETURN_PREFIX, p)
        p.append("return")
        self.visit(return_.expression, p)
        self._after_syntax(return_, p)
        return return_

    def visit_switch(self, switch: 'j.Switch', p: PrintOutputCapture) -> J:
        """Visit a switch statement (match in Python)."""
        self._before_syntax(switch, Space.Location.SWITCH_PREFIX, p)
        p.append("match")
        self.visit(switch.selector, p)
        self.visit(switch.cases, p)
        self._after_syntax(switch, p)
        return switch

    def visit_ternary(self, ternary: 'j.Ternary', p: PrintOutputCapture) -> J:
        """Visit a ternary expression (conditional expression in Python)."""
        self._before_syntax(ternary, Space.Location.TERNARY_PREFIX, p)
        self.visit(ternary.true_part, p)
        self._visit_space(ternary.padding.true_part.before, Space.Location.TERNARY_TRUE, p)
        p.append("if")
        self.visit(ternary.condition, p)
        self._visit_left_padded("else", ternary.padding.false_part, JLeftPadded.Location.TERNARY_FALSE, p)
        self._after_syntax(ternary, p)
        return ternary

    def visit_throw(self, throw: 'j.Throw', p: PrintOutputCapture) -> J:
        """Visit a throw statement (raise in Python)."""
        self._before_syntax(throw, Space.Location.THROW_PREFIX, p)
        p.append("raise")
        self.visit(throw.exception, p)
        self._after_syntax(throw, p)
        return throw

    def visit_try(self, try_: 'j.Try', p: PrintOutputCapture) -> J:
        """Visit a try statement (or with statement in Python)."""
        from rewrite.python import tree as py
        from rewrite.java import tree as j

        is_with_statement = try_.resources and len(try_.resources) > 0

        self._before_syntax(try_, Space.Location.TRY_PREFIX, p)
        if is_with_statement:
            p.append("with")
        else:
            p.append("try")

        resources = try_.padding.resources
        if is_with_statement and resources:
            self._visit_space(resources.before, Space.Location.TRY_RESOURCES, p)
            omit_parens = resources.markers.find_first(OmitParentheses)
            if not omit_parens:
                p.append("(")

            first = True
            for resource in resources.padding.elements:
                if not first:
                    p.append(",")
                else:
                    first = False

                self._visit_space(resource.element.prefix, Space.Location.TRY_RESOURCE, p)
                self._visit_markers(resource.element.markers, p)

                decl = resource.element.variable_declarations
                if isinstance(decl, j.Assignment):
                    self.visit(decl.assignment, p)
                    if not isinstance(decl.variable, j.Empty):
                        self._visit_space(decl.padding.assignment.before, Space.Location.LANGUAGE_EXTENSION, p)
                        p.append("as")
                        self.visit(decl.variable, p)
                else:
                    self.visit(decl, p)

                self._visit_space(resource.after, Space.Location.TRY_RESOURCE_SUFFIX, p)
                self._visit_markers(resource.markers, p)

            self._visit_markers(resources.markers, p)
            if not omit_parens:
                p.append(")")

        try_body = try_.body
        parent = self.get_cursor().parent
        else_wrapper = parent.value if parent and isinstance(parent.value, py.TrailingElseWrapper) else None

        self.visit(try_body, p)

        # Visit catches
        for catch in try_.catches:
            self.visit(catch, p)

        # Handle else block from TrailingElseWrapper
        if else_wrapper:
            self._visit_space(else_wrapper.padding.else_block.before, Space.Location.ELSE_PREFIX, p)
            p.append("else")
            self.visit(else_wrapper.else_block, p)

        # Visit finally
        if try_.padding.finally_:
            self._visit_left_padded("finally", try_.padding.finally_, JLeftPadded.Location.TRY_FINALLY, p)

        self._after_syntax(try_, p)
        return try_

    def visit_try_resource(self, resource: 'j.Try.Resource', p: PrintOutputCapture) -> J:
        """Visit a try resource."""
        self._before_syntax(resource, Space.Location.TRY_RESOURCE, p)
        self.visit(resource.variable_declarations, p)
        self._after_syntax(resource, p)
        return resource

    def visit_unary(self, unary: 'j.Unary', p: PrintOutputCapture) -> J:
        """Visit a unary expression."""
        from rewrite.java.tree import Unary

        self._before_syntax(unary, Space.Location.UNARY_PREFIX, p)

        if unary.operator == Unary.Type.Not:
            p.append("not")
        elif unary.operator == Unary.Type.Positive:
            p.append("+")
        elif unary.operator == Unary.Type.Negative:
            p.append("-")
        elif unary.operator == Unary.Type.Complement:
            p.append("~")

        self.visit(unary.expression, p)
        self._after_syntax(unary, p)
        return unary

    def visit_variable(self, variable: 'j.VariableDeclarations.NamedVariable', p: PrintOutputCapture) -> J:
        """Visit a named variable."""
        from rewrite.python import tree as py

        self._before_syntax(variable, Space.Location.VARIABLE_PREFIX, p)

        parent_cursor = self.get_cursor().parent
        vd = parent_cursor.parent.value if parent_cursor and parent_cursor.parent else None
        padding = parent_cursor.value if parent_cursor else None

        type_expr = vd.type_expression if vd else None

        if isinstance(type_expr, py.SpecialParameter):
            special = type_expr
            self._python_printer.visit(special, p)
            type_expr = special.type_hint

        if not variable.name.simple_name:
            self.visit(variable.initializer, p)
        else:
            if vd and vd.varargs is not None:
                self._visit_space(vd.varargs, Space.Location.VARARGS, p)
                p.append('*')
            if vd and vd.markers.find_first(KeywordArguments):
                p.append("**")
            self.visit(variable.name, p)
            if type_expr is not None and padding:
                self._visit_space(padding.after, JRightPadded.Location.NAMED_VARIABLE.after_location, p)
                p.append(':')
                self.visit(type_expr, p)
            if variable.padding.initializer:
                self._visit_left_padded("=", variable.padding.initializer, JLeftPadded.Location.VARIABLE_INITIALIZER, p)

        self._after_syntax(variable, p)
        return variable

    def visit_variable_declarations(self, multi_variable: 'j.VariableDeclarations', p: PrintOutputCapture) -> J:
        """Visit variable declarations."""
        self._before_syntax(multi_variable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p)
        self._visit_space(Space.EMPTY, Space.Location.ANNOTATIONS, p)

        # Visit leading annotations
        for annotation in multi_variable.leading_annotations:
            self.visit(annotation, p)

        # Visit modifiers
        for mod in multi_variable.modifiers:
            self.visit_modifier(mod, p)

        # Handle keyword-only arguments marker
        if multi_variable.markers.find_first(KeywordOnlyArguments):
            p.append("*")

        # Visit variables
        nodes = multi_variable.padding.variables
        for i, node in enumerate(nodes):
            # Set cursor for context in visit_variable
            self.set_cursor(Cursor(self.get_cursor(), node))
            self.visit(node.element, p)
            self._visit_markers(node.markers, p)
            if i < len(nodes) - 1:
                p.append(",")
            # Restore cursor
            parent = self.get_cursor().parent
            self.set_cursor(parent)

        self._after_syntax(multi_variable, p)
        return multi_variable

    def visit_while_loop(self, while_loop: 'j.WhileLoop', p: PrintOutputCapture) -> J:
        """Visit a while loop."""
        self._before_syntax(while_loop, Space.Location.WHILE_PREFIX, p)
        p.append("while")
        self.visit(while_loop.condition, p)
        self.visit(while_loop.body, p)
        self._after_syntax(while_loop, p)
        return while_loop

    def visit_yield(self, yield_: 'j.Yield', p: PrintOutputCapture) -> J:
        """Visit a yield statement."""
        self._before_syntax(yield_, Space.Location.YIELD_PREFIX, p)
        p.append("yield")
        self.visit(yield_.value, p)
        self._after_syntax(yield_, p)
        return yield_
