"""
Python 2 Parser Visitor

This module provides a parser for Python 2 code using parso, converting the
parso CST (Concrete Syntax Tree) to OpenRewrite's LST (Lossless Semantic Tree).

Parso is used because Python's built-in ast module cannot parse Python 2 syntax
(e.g., print statements, exec statements, backtick repr) when running on Python 3.

Key differences from the Python 3 ParserVisitor:
- Uses parso instead of the ast module
- Parso's CST includes whitespace in node prefixes (simpler than tokenization)
- Handles Python 2-specific syntax (print statement, exec statement, backtick repr)
"""

from pathlib import Path
from typing import Optional, List, Tuple
from uuid import UUID

import parso
from parso.python import tree as parso_tree

from rewrite import random_id, Markers
from rewrite.java import Space, JRightPadded, JLeftPadded, JContainer, JavaType
from rewrite.java.support_types import TextComment
from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python.markers import (
    PrintSyntax, ExecSyntax, Quoted, KeywordArguments,
    TupleExceptClause, LegacyNotEqual, RaiseTuple,
)
from rewrite.java.markers import OmitParentheses, Semicolon, TrailingComma


class Py2ParserVisitor:
    """Converts parso CST to OpenRewrite LST for Python 2 code.

    Usage:
        visitor = Py2ParserVisitor(source, "path/to/file.py", "2.7")
        cu = visitor.parse()
    """

    # UTF-8 BOM character
    _BOM = '\ufeff'

    def __init__(self, source: str, file_path: Optional[str] = None, version: str = "2.7"):
        """Initialize the parser visitor.

        Args:
            source: The Python 2 source code to parse
            file_path: Optional path to the source file
            version: Python version string (e.g., "2.7", "2")
        """
        self._source = source
        self._file_path = file_path or "<unknown>"
        self._version = version if '.' in version else version + ".7"  # Default to 2.7

        # Detect and strip UTF-8 BOM if present
        if source.startswith(self._BOM):
            self._bom_marked = True
            source = source[1:]
        else:
            self._bom_marked = False

        self._source_without_bom = source

        # Parse with parso
        try:
            self._tree = parso.parse(source, version=self._version)
        except Exception as e:
            raise SyntaxError(f"Failed to parse Python {self._version} code: {e}")

    def parse(self) -> py.CompilationUnit:
        """Parse the source and return a CompilationUnit.

        Returns:
            A Py.CompilationUnit representing the parsed source code.
        """
        # Convert parso tree to LST statements
        statements, end_ws = self._convert_module(self._tree)

        # Combine block end-whitespace (from trailing ``;`` lines) with
        # the file's terminal whitespace recovered from the endmarker.
        trailing = self._trailing_whitespace()
        if end_ws is not Space.EMPTY:
            trailing = Space([], end_ws.whitespace + trailing.whitespace)

        # Build CompilationUnit
        cu = py.CompilationUnit(
            random_id(),
            Space.EMPTY,  # Prefix handled in statements
            Markers.EMPTY,
            Path(self._file_path),
            None,  # file_attributes
            None,  # charset_name
            self._bom_marked,
            None,  # checksum
            [],    # imports — Python imports live in `statements`, like the Py3 parser
            statements,
            trailing,
        )

        return cu

    def _convert_module(self, module: parso_tree.Module) -> Tuple[List[JRightPadded], Space]:
        """Convert a parso Module to a list of LST statements, preserving
        newlines so the printer can round-trip the source.

        parso emits newline leaves between top-level statements. We thread
        each one onto the *trailing* :class:`JRightPadded.after` of the
        statement that owns it (which is how the printer expects to find
        them). Returns the statement list plus any leftover end-whitespace
        that must be appended to ``cu.eof`` (e.g. the ``\\n`` after a
        trailing ``;`` line).
        """
        statements, end_ws = self._convert_stmt_block_children(module.children)
        if not statements:
            statements.append(JRightPadded(
                j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                Space.EMPTY,
                Markers.EMPTY
            ))
        return statements, end_ws

    def _convert_stmt_block_children(self, children) -> Tuple[list, Space]:
        """Walk parso suite/module children into ``JRightPadded`` statements.

        Newlines between statements live on the next statement's prefix.
        When a line ends with a trailing ``;``, the trailing newline can't
        live in ``JRightPadded.after`` (the printer renders ``.after``
        *before* markers); the helper returns it so the surrounding
        ``cu.eof`` or ``block.end`` slot absorbs it instead.
        """
        statements = []
        pending_prefix = ''
        for child in children:
            ctype = getattr(child, 'type', None)
            if isinstance(child, parso_tree.PythonLeaf):
                if ctype in ('newline', 'indent', 'dedent'):
                    pending_prefix += getattr(child, 'value', '')
                    continue
                if ctype == 'endmarker':
                    # endmarker.prefix is captured by _trailing_whitespace.
                    continue
            if ctype == 'simple_stmt':
                padded, trail = self._convert_simple_stmt_padded(child)
                if padded and pending_prefix:
                    padded[0] = padded[0].replace(
                        element=self._prepend_prefix(padded[0].element, pending_prefix),
                    )
                    pending_prefix = ''
                statements.extend(padded)
                pending_prefix += trail
                continue
            stmt = self._convert_node(child)
            if stmt is None:
                continue
            if pending_prefix:
                stmt = self._prepend_prefix(stmt, pending_prefix)
                pending_prefix = ''
            statements.append(self._pad_statement(stmt))

        if not pending_prefix:
            return statements, Space.EMPTY
        if statements:
            last = statements[-1]
            if last.markers.find_first(Semicolon) is None:
                # Last statement has no trailing ``;``: append the
                # leftover whitespace to its ``.after`` (the existing
                # convention — this is how the file's terminal ``\n``
                # normally round-trips).
                statements[-1] = last.replace(
                    after=Space([], last.after.whitespace + pending_prefix),
                )
                return statements, Space.EMPTY
        return statements, Space([], pending_prefix)

    @staticmethod
    def _prepend_prefix(stmt, leading: str):
        """Prepend a whitespace string to a node's prefix, preserving
        any comments already attached.
        """
        if not hasattr(stmt, 'prefix'):
            return stmt
        existing = stmt.prefix if stmt.prefix is not None else Space.EMPTY
        new_prefix = Space(existing.comments, leading + existing.whitespace)
        try:
            return stmt.replace(prefix=new_prefix)
        except (TypeError, AttributeError):
            return stmt

    def _convert_node(self, node) -> Optional[j.J]:
        """Convert a parso node to an LST node.

        Args:
            node: A parso tree node

        Returns:
            The corresponding LST node, or None if the node should be skipped.
        """
        # Handle leaf nodes (tokens)
        if isinstance(node, parso_tree.PythonLeaf):
            return self._convert_leaf(node)

        # Handle different node types
        node_type = node.type if hasattr(node, 'type') else type(node).__name__

        converters = {
            'file_input': lambda n: None,  # Handled by _convert_module
            # 'simple_stmt' is intentionally absent: it must be routed
            # through ``_convert_simple_stmt_padded`` by the block walker
            # so semicolon-separated small_stmts each surface as their
            # own LST statement.
            'expr_stmt': self._convert_expr_stmt,
            'print_stmt': self._convert_print_stmt,
            'exec_stmt': self._convert_exec_stmt,
            'pass_stmt': self._convert_pass_stmt,
            'break_stmt': self._convert_break_stmt,
            'continue_stmt': self._convert_continue_stmt,
            'return_stmt': self._convert_return_stmt,
            'raise_stmt': self._convert_raise_stmt,
            'assert_stmt': self._convert_assert_stmt,
            'del_stmt': self._convert_del_stmt,
            'global_stmt': self._convert_global_stmt,
            'yield_expr': self._convert_yield_expr,
            'funcdef': self._convert_funcdef,
            'classdef': self._convert_classdef,
            'decorated': self._convert_decorated,
            'if_stmt': self._convert_if_stmt,
            'while_stmt': self._convert_while_stmt,
            'for_stmt': self._convert_for_stmt,
            'try_stmt': self._convert_try_stmt,
            'with_stmt': self._convert_with_stmt,
            'import_name': self._convert_import_name,
            'import_from': self._convert_import_from,
            'suite': self._convert_suite,
            'atom': self._convert_atom,
            'power': self._convert_power,
            'testlist_star_expr': self._convert_testlist,
            'testlist': self._convert_testlist,
            'test': self._convert_test,
            'or_test': self._convert_or_test,
            'and_test': self._convert_and_test,
            'not_test': self._convert_not_test,
            'comparison': self._convert_comparison,
            'arith_expr': self._convert_arith_expr,
            'term': self._convert_term,
            'factor': self._convert_factor,
            'lambdef': self._convert_lambdef,
            'old_lambdef': self._convert_lambdef,
            'strings': self._convert_strings,
        }

        converter = converters.get(node_type)
        if converter:
            return converter(node)

        # For unhandled node types, try generic conversion
        return self._convert_generic(node)

    def _convert_leaf(self, leaf: parso_tree.PythonLeaf) -> Optional[j.J]:
        """Convert a parso leaf (token) to an LST node."""
        prefix = self._parse_space(leaf.prefix)

        # parso 0.7.x uses lowercase type names (e.g. 'name', 'number')
        leaf_type = leaf.type.upper()  # ty: ignore[unresolved-attribute]  # parso leaf.type is always str

        if leaf_type == 'KEYWORD':
            # Bare statement-keywords occur inside suites when the keyword
            # has no operands (e.g. `pass`, `break`, `continue`, `raise`,
            # `yield`). They are routed here rather than through their
            # `*_stmt` nodes because parso elides the wrapper for these
            # trivial forms.
            if leaf.value == 'pass':
                return py.Pass(random_id(), prefix, Markers.EMPTY)
            if leaf.value == 'break':
                return j.Break(random_id(), prefix, Markers.EMPTY, None)
            if leaf.value == 'continue':
                return j.Continue(random_id(), prefix, Markers.EMPTY, None)
            if leaf.value == 'raise':
                return j.Throw(random_id(), prefix, Markers.EMPTY,
                               j.Empty(random_id(), Space.EMPTY, Markers.EMPTY))
            if leaf.value == 'return':
                return j.Return(random_id(), prefix, Markers.EMPTY, None)
            if leaf.value == 'yield':
                yield_ = j.Yield(random_id(), prefix, Markers.EMPTY, False,
                                 j.Empty(random_id(), Space.EMPTY, Markers.EMPTY))
                return py.StatementExpression(random_id(), yield_)
            # Generic keyword used in an expression position (e.g. `True`,
            # `False`, `None`) — falls through to identifier shape.
            return j.Identifier(
                random_id(), prefix, Markers.EMPTY, [], leaf.value, None, None,
            )
        if leaf_type == 'NAME':
            return j.Identifier(
                random_id(),
                prefix,
                Markers.EMPTY,
                [],
                leaf.value,
                None,  # type
                None   # field_type
            )
        elif leaf_type == 'NUMBER':
            return j.Literal(
                random_id(),
                prefix,
                Markers.EMPTY,
                leaf.value,
                leaf.value,
                None,  # unicode_escapes
                self._number_type(leaf.value)
            )
        elif leaf_type == 'STRING':
            return self._convert_string_literal(leaf, prefix)
        elif leaf_type in ('NEWLINE', 'ENDMARKER', 'INDENT', 'DEDENT'):
            return None  # Skip whitespace tokens

        return None

    def _convert_string_literal(self, leaf: parso_tree.PythonLeaf, prefix: Space) -> j.Literal:
        """Convert a string literal, detecting quote style."""
        value = leaf.value
        quote_style = self._detect_quote_style(value)

        # Build markers with quote style
        markers = Markers.build(random_id(), [Quoted(random_id(), quote_style)])

        return j.Literal(
            random_id(),
            prefix,
            markers,
            value,
            value,
            None,  # unicode_escapes
            JavaType.Primitive.String
        )

    def _detect_quote_style(self, string_value: str) -> Quoted.Style:
        """Detect the quote style of a string literal."""
        # Remove any prefix (r, u, b, etc.)
        s = string_value.lstrip('rRuUbB')

        if s.startswith('"""'):
            return Quoted.Style.TRIPLE_DOUBLE
        elif s.startswith("'''"):
            return Quoted.Style.TRIPLE_SINGLE
        elif s.startswith('"'):
            return Quoted.Style.DOUBLE
        elif s.startswith("'"):
            return Quoted.Style.SINGLE
        elif s.startswith('`'):
            return Quoted.Style.BACKTICK
        else:
            return Quoted.Style.DOUBLE  # Default

    def _convert_print_stmt(self, node) -> j.MethodInvocation:
        """Convert Python 2 print statement to MethodInvocation with PrintSyntax marker.

        Python 2 print syntax:
            print                      # empty print (newline only)
            print "hello"              # simple print
            print >> stderr, "error"   # print to file
            print x,                   # trailing comma (no newline)
        """
        prefix = self._parse_space(node.children[0].prefix)  # 'print' keyword

        # Parse the print statement arguments
        has_destination = False
        trailing_comma = False
        arguments = []

        children = node.children[1:]  # Skip 'print' keyword
        i = 0

        # Check for >> (output destination)
        if i < len(children) and hasattr(children[i], 'value') and children[i].value == '>>':
            has_destination = True
            i += 1
            if i < len(children):
                dest = self._convert_node(children[i])
                if dest:
                    arguments.append(JRightPadded(dest, Space.EMPTY, Markers.EMPTY))
                i += 1
            # Skip comma after destination
            if i < len(children) and hasattr(children[i], 'value') and children[i].value == ',':
                i += 1

        # Collect remaining expressions
        while i < len(children):
            child = children[i]
            if hasattr(child, 'value') and child.value == ',':
                # Capture space before comma as 'after' of the preceding argument
                comma_prefix = self._parse_space(child.prefix)
                if arguments:
                    prev = arguments[-1]
                    arguments[-1] = JRightPadded(prev.element, comma_prefix, prev.markers)
                # Check if this is a trailing comma
                if i == len(children) - 1:
                    trailing_comma = True
                i += 1
                continue
            expr = self._convert_node(child)
            if expr:
                arguments.append(JRightPadded(expr, Space.EMPTY, Markers.EMPTY))
            i += 1

        # Build PrintSyntax marker
        print_marker = PrintSyntax(random_id(), has_destination, trailing_comma)
        markers = Markers.build(random_id(), [print_marker])

        # Create MethodInvocation for 'print'
        name = j.Identifier(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            [],
            "print",
            None,
            None
        )

        return j.MethodInvocation(
            random_id(),
            prefix,
            markers,
            None,  # select
            None,  # type_parameters
            name,
            JContainer(Space.EMPTY, arguments, Markers.EMPTY),
            None   # method_type
        )

    def _convert_exec_stmt(self, node) -> j.MethodInvocation:
        """Convert Python 2 exec statement to MethodInvocation with ExecSyntax marker.

        Python 2 exec syntax:
            exec code                    # simple form
            exec code in globals         # with globals dict
            exec code in globals, locals # with globals and locals dicts
        """
        prefix = self._parse_space(node.children[0].prefix)  # 'exec' keyword

        # Parse exec statement arguments
        arguments = []
        children = node.children[1:]  # Skip 'exec' keyword

        for child in children:
            if hasattr(child, 'value') and child.value in ('in', ','):
                # Capture space before 'in' or ',' as 'after' of the preceding argument
                delim_prefix = self._parse_space(child.prefix)
                if arguments:
                    prev = arguments[-1]
                    arguments[-1] = JRightPadded(prev.element, delim_prefix, prev.markers)
                continue
            expr = self._convert_node(child)
            if expr:
                arguments.append(JRightPadded(expr, Space.EMPTY, Markers.EMPTY))

        # Build ExecSyntax marker
        exec_marker = ExecSyntax(random_id())
        markers = Markers.build(random_id(), [exec_marker])

        # Create MethodInvocation for 'exec'
        name = j.Identifier(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            [],
            "exec",
            None,
            None
        )

        return j.MethodInvocation(
            random_id(),
            prefix,
            markers,
            None,  # select
            None,  # type_parameters
            name,
            JContainer(Space.EMPTY, arguments, Markers.EMPTY),
            None   # method_type
        )

    # --- Stub implementations for other node types ---
    # These will be implemented incrementally

    def _convert_simple_stmt_padded(self, node) -> Tuple[List[JRightPadded], str]:
        """Convert a ``simple_stmt`` to one padded LST statement per
        semicolon-separated ``small_stmt``.

        Grammar (parso): ``simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE``.

        Each ``;`` attaches a :class:`Semicolon` marker to the preceding
        statement; the whitespace before the ``;`` lives in that
        statement's :attr:`JRightPadded.after`. The trailing newline goes
        on the last small_stmt's ``.after`` — *unless* the line ends with
        a trailing ``;``, in which case the printer's ``element →
        after → markers`` order would put the newline before the ``;``.
        For that case the newline is returned as the second tuple element
        so the caller can route it to ``cu.eof`` / ``block.end``.
        """
        result: List[JRightPadded] = []
        trailing_after_marker = ''
        for child in node.children:
            ctype = getattr(child, 'type', None)
            cval = getattr(child, 'value', None)
            if ctype == 'newline':
                # parso stores any trailing comment in this leaf's
                # ``prefix`` (e.g. ``"  # comment"``), with the bare
                # ``\n`` as its ``value``. When there's a small_stmt to
                # absorb it the trailing whitespace/comment becomes its
                # ``.after``; otherwise (trailing ``;``) we return it.
                if result and result[-1].markers.find_first(Semicolon) is None:
                    last = result[-1]
                    trailing_space = self._parse_space(child.prefix + cval)
                    if last.after.whitespace:
                        trailing_space = Space(
                            trailing_space.comments,
                            last.after.whitespace + trailing_space.whitespace,
                        )
                    result[-1] = last.replace(after=trailing_space)
                else:
                    trailing_after_marker = child.prefix + cval
                continue
            if cval == ';':
                if result:
                    result[-1] = result[-1].replace(
                        after=self._parse_space(child.prefix),
                        markers=Markers.build(random_id(), [Semicolon(random_id())]),
                    )
                continue
            stmt = self._convert_node(child)
            if stmt is not None:
                result.append(self._pad_statement(stmt))
        return result, trailing_after_marker

    def _convert_expr_stmt(self, node) -> Optional[j.J]:
        """Convert an expression statement.

        Handles:
        * Bare expression (``f(x)``)
        * Simple assignment (``a = b``); chained ``a = b = c`` not yet covered.
        * Augmented assignment (``x += 1`` and the rest of the ``_aug_assign_map``).
        """
        if len(node.children) == 1:
            # Simple expression
            expr = self._convert_node(node.children[0])
            if expr:
                return py.ExpressionStatement(random_id(), expr)
        elif len(node.children) >= 5 and self._is_chained_assign(node.children):
            return self._build_chained_assignment(node.children)
        elif len(node.children) == 3 and hasattr(node.children[1], 'value'):
            op_value = node.children[1].value
            if op_value == '=':
                lhs = self._convert_node(node.children[0])
                eq_prefix = self._parse_space(node.children[1].prefix)
                rhs = self._convert_node(node.children[2])
                if lhs and rhs:
                    return j.Assignment(
                        random_id(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        lhs,
                        JLeftPadded(eq_prefix, rhs, Markers.EMPTY),
                        None  # type
                    )
            else:
                aug_type = self._aug_assign_map().get(op_value)
                if aug_type is not None:
                    lhs = self._convert_node(node.children[0])
                    op_prefix = self._parse_space(node.children[1].prefix)
                    rhs = self._convert_node(node.children[2])
                    if lhs and rhs:
                        return j.AssignmentOperation(
                            random_id(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            lhs,
                            JLeftPadded(op_prefix, aug_type, Markers.EMPTY),
                            rhs,
                            None,  # type
                        )
        return self._convert_generic(node)

    def _convert_pass_stmt(self, node) -> py.Pass:
        """Convert pass statement."""
        prefix = self._parse_space(node.children[0].prefix)
        return py.Pass(random_id(), prefix, Markers.EMPTY)

    def _convert_break_stmt(self, node) -> j.Break:
        """Convert break statement."""
        prefix = self._parse_space(node.children[0].prefix)
        return j.Break(random_id(), prefix, Markers.EMPTY, None)

    def _convert_continue_stmt(self, node) -> j.Continue:
        """Convert continue statement."""
        prefix = self._parse_space(node.children[0].prefix)
        return j.Continue(random_id(), prefix, Markers.EMPTY, None)

    def _convert_return_stmt(self, node) -> j.Return:
        """Convert return statement."""
        prefix = self._parse_space(node.children[0].prefix)
        expr = None
        if len(node.children) > 1:
            expr = self._convert_node(node.children[1])
        return j.Return(random_id(), prefix, Markers.EMPTY, expr)

    @staticmethod
    def _is_chained_assign(children) -> bool:
        """True when an ``expr_stmt`` is shaped as ``a = b = ... = expr`` —
        i.e. an alternating ``[target, '=', target, '=', ..., '=', value]``
        sequence with at least two ``=`` operators."""
        eq_count = sum(1 for c in children if getattr(c, 'value', None) == '=')
        return eq_count >= 2

    def _build_chained_assignment(self, children) -> Optional[j.J]:
        """Convert ``a = b = c`` into :class:`py.ChainedAssignment`."""
        variables = []
        i = 0
        while i + 2 < len(children):
            target = self._convert_node(children[i])
            eq = children[i + 1]
            if target is None or getattr(eq, 'value', None) != '=':
                return None
            variables.append(JRightPadded(target, self._parse_space(eq.prefix), Markers.EMPTY))
            i += 2
        value = self._convert_node(children[-1])
        if value is None:
            return None
        return py.ChainedAssignment(
            random_id(), Space.EMPTY, Markers.EMPTY,
            variables, value, None,
        )

    def _convert_raise_stmt(self, node) -> Optional[j.J]:
        """Convert ``raise`` / ``raise E`` / ``raise E, v[, tb]`` (Py2 form).

        The Py2 multi-argument form is wrapped in a :class:`py.CollectionLiteral`
        of kind ``TUPLE`` tagged with :class:`RaiseTuple`, so the printer can
        emit the legacy ``raise E, v[, tb]`` syntax without the tuple parens.
        """
        children = node.children
        prefix = self._parse_space(children[0].prefix)
        if len(children) == 1:
            return j.Throw(random_id(), prefix, Markers.EMPTY,
                           j.Empty(random_id(), Space.EMPTY, Markers.EMPTY))
        if len(children) == 2:
            exc = self._convert_node(children[1])
            if exc is None:
                return self._convert_generic(node)
            return j.Throw(random_id(), prefix, Markers.EMPTY, exc)
        # Py2 multi-arg `raise E, v[, tb]`. The remaining children are
        # `[exception, ',', value, (',', tb)?]`.
        items = self._pad_comma_list(children[1:], Space.EMPTY)
        if items is None:
            return self._convert_generic(node)
        tuple_lit = py.CollectionLiteral(
            random_id(), Space.EMPTY, Markers.EMPTY,
            py.CollectionLiteral.Kind.TUPLE,
            JContainer(Space.EMPTY, items, Markers.EMPTY),
            None,
        )
        return j.Throw(
            random_id(), prefix,
            Markers.build(random_id(), [RaiseTuple(random_id())]),
            tuple_lit,
        )

    def _convert_assert_stmt(self, node) -> Optional[j.J]:
        """Convert ``assert expr`` and ``assert expr, msg``."""
        children = node.children
        prefix = self._parse_space(children[0].prefix)
        if len(children) < 2:
            return self._convert_generic(node)
        condition = self._convert_node(children[1])
        if condition is None:
            return self._convert_generic(node)
        detail = None
        if len(children) >= 4 and getattr(children[2], 'value', None) == ',':
            msg = self._convert_node(children[3])
            if msg is None:
                return self._convert_generic(node)
            detail = JLeftPadded(self._parse_space(children[2].prefix), msg, Markers.EMPTY)
        return j.Assert(random_id(), prefix, Markers.EMPTY, condition, detail)

    def _convert_del_stmt(self, node) -> Optional[j.J]:
        """Convert ``del a`` / ``del a, b``."""
        children = node.children
        prefix = self._parse_space(children[0].prefix)
        if len(children) < 2:
            return self._convert_generic(node)
        target_node = children[1]
        targets = []
        if getattr(target_node, 'type', None) == 'exprlist':
            items = target_node.children
            i = 0
            while i < len(items):
                item = items[i]
                if getattr(item, 'value', None) == ',':
                    i += 1
                    continue
                expr = self._convert_node(item)
                if expr is None:
                    return self._convert_generic(node)
                if i + 1 < len(items) and getattr(items[i + 1], 'value', None) == ',':
                    comma = items[i + 1]
                    targets.append(JRightPadded(expr, self._parse_space(comma.prefix), Markers.EMPTY))
                    i += 2
                else:
                    targets.append(JRightPadded(expr, Space.EMPTY, Markers.EMPTY))
                    i += 1
        else:
            expr = self._convert_node(target_node)
            if expr is None:
                return self._convert_generic(node)
            targets = [JRightPadded(expr, Space.EMPTY, Markers.EMPTY)]
        return py.Del(random_id(), prefix, Markers.EMPTY, targets)

    def _convert_global_stmt(self, node) -> Optional[j.J]:
        """Convert ``global a`` / ``global a, b`` to :class:`py.VariableScope`."""
        children = node.children
        prefix = self._parse_space(children[0].prefix)
        names = []
        i = 1
        while i < len(children):
            child = children[i]
            if getattr(child, 'value', None) == ',':
                i += 1
                continue
            if not hasattr(child, 'value'):
                return self._convert_generic(node)
            ident = j.Identifier(random_id(),
                                 self._parse_space(child.prefix),
                                 Markers.EMPTY, [], child.value, None, None)
            if i + 1 < len(children) and getattr(children[i + 1], 'value', None) == ',':
                comma = children[i + 1]
                names.append(JRightPadded(ident, self._parse_space(comma.prefix), Markers.EMPTY))
                i += 2
            else:
                names.append(JRightPadded(ident, Space.EMPTY, Markers.EMPTY))
                i += 1
        return py.VariableScope(random_id(), prefix, Markers.EMPTY,
                                py.VariableScope.Kind.GLOBAL, names)

    def _convert_yield_expr(self, node) -> Optional[j.J]:
        """Convert ``yield`` / ``yield expr`` / ``yield x, y``.

        Wraps a :class:`j.Yield` in :class:`py.StatementExpression` so that
        it can act as a statement when it appears at the top of a
        ``simple_stmt`` body.
        """
        children = node.children
        prefix = self._parse_space(children[0].prefix)
        value = None
        if len(children) >= 2:
            value = self._convert_node(children[1])
            if value is None:
                return self._convert_generic(node)
        else:
            value = j.Empty(random_id(), Space.EMPTY, Markers.EMPTY)
        yield_ = j.Yield(random_id(), prefix, Markers.EMPTY, False, value)
        return py.StatementExpression(random_id(), yield_)

    def _convert_decorated(self, node) -> Optional[j.J]:
        """Convert a parso ``decorated`` wrapper around a ``funcdef`` (or
        ``classdef``).

        Each decorator becomes a :class:`j.Annotation`. The outer prefix of
        the resulting statement is the whitespace before the first ``@``;
        every decorator after the first has its prefix set to the newline /
        indentation between decorators.
        """
        children = node.children
        if len(children) < 2:
            return self._convert_generic(node)

        # parso emits a single decorator as a direct child, but groups
        # multiple decorators inside a `decorators` wrapper node.
        decorator_nodes = []
        body_idx = 0
        if getattr(children[0], 'type', None) == 'decorators':
            decorator_nodes = list(children[0].children)
            body_idx = 1
        else:
            while body_idx < len(children) and getattr(children[body_idx], 'type', None) == 'decorator':
                decorator_nodes.append(children[body_idx])
                body_idx += 1

        annotations = []
        outer_prefix = None
        trailing = ''  # accumulator of newlines emitted between decorators
        for dec_node in decorator_nodes:
            ann = self._convert_decorator(dec_node)
            if ann is None:
                return self._convert_generic(node)
            # Prepend any pending newline-between-decorators to this
            # annotation's prefix so the printer reproduces line breaks.
            if trailing:
                existing = ann.prefix.whitespace if ann.prefix else ''
                ann = ann.replace(_prefix=Space([], trailing + existing))
                trailing = ''
            if outer_prefix is None:
                outer_prefix = ann.prefix
                # The first annotation now carries the outer prefix, so the
                # printer would double-print it. Clear it on the annotation.
                annotations.append(ann.replace(_prefix=Space.EMPTY))
            else:
                annotations.append(ann)
            # parso emits the trailing newline as the decorator's last
            # child. Capture it so it can land on the next annotation /
            # def-modifier prefix.
            if dec_node.children:
                last = dec_node.children[-1]
                if (isinstance(last, parso_tree.PythonLeaf)
                        and getattr(last, 'type', None) == 'newline'):
                    trailing += last.value

        body = children[body_idx] if body_idx < len(children) else None
        if body is None:
            return self._convert_generic(node)

        if getattr(body, 'type', None) == 'funcdef':
            return self._convert_funcdef(body, leading_annotations=annotations,
                                         outer_prefix=outer_prefix,
                                         extra_modifier_prefix=trailing)
        if getattr(body, 'type', None) == 'classdef':
            return self._convert_classdef(body, leading_annotations=annotations,
                                          outer_prefix=outer_prefix,
                                          extra_modifier_prefix=trailing)
        return self._convert_generic(node)

    def _convert_decorator(self, decorator_node) -> Optional[j.Annotation]:
        """Convert a parso ``decorator`` node.

        Grammar: ``decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE``.
        """
        children = decorator_node.children
        if len(children) < 2:
            return None
        at_leaf = children[0]
        name_node = children[1]
        prefix = self._parse_space(at_leaf.prefix)

        # The decorator name slot expects a NameTree. For a single NAME we
        # use a bare :class:`j.Identifier` (so the printer doesn't emit a
        # leading dot); dotted names go through ``_build_qualid``.
        if isinstance(name_node, parso_tree.PythonLeaf):
            annotation_type = j.Identifier(
                random_id(),
                self._parse_space(name_node.prefix),
                Markers.EMPTY, [], name_node.value, None, None,
            )
        else:
            annotation_type = self._build_qualid(name_node)
            if annotation_type is None:
                return None

        args = None
        if len(children) >= 3 and getattr(children[2], 'value', None) == '(':
            # Decorator call: collect args between '(' and ')'.
            open_paren = children[2]
            close_paren_idx = None
            for k in range(3, len(children)):
                if getattr(children[k], 'value', None) == ')':
                    close_paren_idx = k
                    break
            if close_paren_idx is not None:
                inner = children[3:close_paren_idx]
                close_paren = children[close_paren_idx]
                args = self._convert_call_args(inner, close_paren,
                                               self._parse_space(open_paren.prefix))

        return j.Annotation(
            random_id(),
            prefix,
            Markers.EMPTY,
            annotation_type,
            args,
        )

    def _convert_funcdef(self, node, leading_annotations=None, outer_prefix=None,
                          extra_modifier_prefix='') -> Optional[j.J]:
        """Convert a function definition.

        parso shape: ``[def, NAME, parameters, ':', suite]``.

        Argument types currently handled: positional (with or without
        defaults), ``*args``, ``**kw``. Annotations and ``->`` return types
        don't exist in Py2 and are not handled.

        ``leading_annotations`` / ``outer_prefix`` are supplied by the
        ``decorated`` wrapper so the decorators precede the bare-def's prefix
        consistently.
        """
        children = node.children
        if len(children) < 5:
            return self._convert_generic(node)
        def_kw, name_leaf, params_node, colon, suite_node = children[0:5]

        # The `def` keyword's prefix is the space immediately before it. When
        # we're called from the decorated wrapper, the wrapper carries the
        # outer prefix (the whitespace before the first '@') and the def_kw
        # prefix is just the space between the last decorator and 'def'.
        def_prefix = self._parse_space(def_kw.prefix)
        prefix = outer_prefix if outer_prefix is not None else def_prefix
        modifier_prefix = Space.EMPTY if outer_prefix is not None else Space.EMPTY
        # When no decorators, the bare `def` keyword carries no leading-of-def
        # space (it's already on MethodDeclaration.prefix). When there are
        # decorators, the wrapper supplies the outer prefix and the def_kw's
        # parso prefix becomes the modifier prefix (space between decorator
        # newline and 'def').
        if outer_prefix is not None:
            modifier_prefix = def_prefix
        if extra_modifier_prefix:
            existing = modifier_prefix.whitespace if modifier_prefix is not None else ''
            modifier_prefix = Space([], extra_modifier_prefix + existing)

        modifiers = [j.Modifier(
            random_id(),
            modifier_prefix,
            Markers.EMPTY,
            'def',
            j.Modifier.Type.Default,
            [],
        )]

        name_ident = j.Identifier(
            random_id(),
            self._parse_space(name_leaf.prefix),
            Markers.EMPTY, [], name_leaf.value, None, None,
        )

        params_container = self._convert_parameters(params_node)
        if params_container is None:
            return self._convert_generic(node)

        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))

        return j.MethodDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            leading_annotations or [],
            modifiers,
            None,  # type_parameters
            None,  # return_type_expression (Py2 has none)
            [],    # name_annotations
            name_ident,
            params_container,
            None,  # throws
            body,
            None,  # default_value
            None,  # method_type
        )

    def _convert_parameters(self, params_node) -> Optional[JContainer]:
        """Convert a parso ``parameters`` node to a parameter ``JContainer``.

        Each parso ``param`` becomes a :class:`j.VariableDeclarations`
        whose single ``NamedVariable`` carries the parameter name and
        (when present) its default value. ``*args`` populates the
        ``varargs`` slot; ``**kw`` is marked with the
        :class:`KeywordArguments` marker so the printer emits ``**``.
        """
        children = params_node.children
        if len(children) < 2:
            return None
        open_paren = children[0]
        close_paren = children[-1]
        param_nodes = children[1:-1]
        leading = self._parse_space(open_paren.prefix)

        if not param_nodes:
            empty = j.Empty(random_id(), self._parse_space(close_paren.prefix), Markers.EMPTY)
            return JContainer(leading, [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)], Markers.EMPTY)

        padded_params = []
        for i, p in enumerate(param_nodes):
            converted = self._convert_param(p)
            if converted is None:
                return None
            is_last = (i == len(param_nodes) - 1)
            # parso `param` nodes carry their own trailing comma when not the
            # last; use it for the JRightPadded.after. For the final param,
            # use the space before `)`.
            after = self._extract_param_trailing_space(p)
            if is_last and after == Space.EMPTY:
                after = self._parse_space(close_paren.prefix)
            padded_params.append(JRightPadded(converted, after, Markers.EMPTY))

        return JContainer(leading, padded_params, Markers.EMPTY)

    @staticmethod
    def _extract_param_trailing_space(param_node) -> Space:
        """Return the space attached to a trailing comma inside a parso
        ``param`` node, or :attr:`Space.EMPTY` when there is no trailing
        comma. The comma itself is consumed implicitly by the printer."""
        last = param_node.children[-1]
        if getattr(last, 'value', None) == ',':
            # The space-before-comma belongs to the comma's own prefix; that
            # becomes the JRightPadded.after on this parameter.
            return Py2ParserVisitor._static_parse_space(last.prefix)
        return Space.EMPTY

    @staticmethod
    def _static_parse_space(text: str) -> Space:
        if not text:
            return Space.EMPTY
        return Space([], text)

    def _convert_param(self, param_node) -> Optional[j.VariableDeclarations]:
        """Convert one parso ``param`` node to :class:`j.VariableDeclarations`.

        Shapes handled:
        * ``NAME``                  — positional
        * ``NAME = expr``           — positional with default
        * ``'*' NAME``              — vararg
        * ``'**' NAME``             — kwargs
        Any trailing comma is consumed in :meth:`_extract_param_trailing_space`.
        """
        children = list(param_node.children)
        # Strip trailing comma if present — it's handled by the caller.
        if children and getattr(children[-1], 'value', None) == ',':
            children = children[:-1]
        if not children:
            return None

        markers = Markers.EMPTY
        varargs_space: Optional[Space] = None
        prefix = Space.EMPTY

        first = children[0]
        if getattr(first, 'value', None) == '*':
            prefix = self._parse_space(first.prefix)
            # The varargs slot stores the space immediately after `*`
            # (parso captures that as the next leaf's prefix).
            if len(children) >= 2:
                varargs_space = self._parse_space(children[1].prefix)
            name_leaf = children[1] if len(children) >= 2 else None
            rest = children[2:]
        elif getattr(first, 'value', None) == '**':
            prefix = self._parse_space(first.prefix)
            markers = Markers.build(random_id(), [KeywordArguments(random_id())])
            # `**` consumes the leading prefix; the name's parso prefix is the
            # space between `**` and the name (typically empty).
            name_leaf = children[1] if len(children) >= 2 else None
            rest = children[2:]
        else:
            prefix = self._parse_space(first.prefix)
            name_leaf = first
            rest = children[1:]

        if name_leaf is None or not hasattr(name_leaf, 'value'):
            return None

        # For *args/**kw the name's leading whitespace is captured in
        # varargs_space (vararg) or carried to the name prefix (kwargs).
        # For positional params, the prefix has already been consumed.
        name_prefix = Space.EMPTY
        if markers != Markers.EMPTY:  # kwargs
            name_prefix = self._parse_space(name_leaf.prefix)
        name_ident = j.Identifier(
            random_id(),
            name_prefix,
            Markers.EMPTY, [], name_leaf.value, None, None,
        )

        initializer = None
        if len(rest) >= 2 and getattr(rest[0], 'value', None) == '=':
            eq_leaf = rest[0]
            default_value = self._convert_node(rest[1])
            if default_value is None:
                return None
            initializer = JLeftPadded(
                self._parse_space(eq_leaf.prefix),
                default_value,
                Markers.EMPTY,
            )

        named_var = j.VariableDeclarations.NamedVariable(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            name_ident,
            [],          # dimensions_after_name
            initializer,
            None,        # variable_type
        )
        return j.VariableDeclarations(
            random_id(),
            prefix,
            markers,
            [],          # leading_annotations
            [],          # modifiers
            None,        # type_expression
            varargs_space,
            [],          # dimensions_before_name
            [JRightPadded(named_var, Space.EMPTY, Markers.EMPTY)],
        )

    def _convert_classdef(self, node, leading_annotations=None, outer_prefix=None,
                           extra_modifier_prefix='') -> Optional[j.J]:
        """Convert a class definition.

        Grammar:
            classdef: 'class' NAME ['(' [testlist] ')'] ':' suite

        The Py2-only old-style class form (``class Foo:`` with no parens)
        is preserved as ``implements=None`` and round-trips through the
        printer based on that distinction (vs. ``implements=JContainer(...)``
        for ``class Foo():`` or ``class Foo(Base):``).
        """
        children = node.children
        if len(children) < 4:
            return self._convert_generic(node)

        class_kw = children[0]
        name_leaf = children[1]
        class_prefix = self._parse_space(class_kw.prefix)
        prefix = outer_prefix if outer_prefix is not None else class_prefix
        kind_prefix = class_prefix if outer_prefix is not None else Space.EMPTY
        if extra_modifier_prefix:
            existing = kind_prefix.whitespace if kind_prefix is not None else ''
            kind_prefix = Space([], extra_modifier_prefix + existing)

        name_ident = j.Identifier(
            random_id(),
            self._parse_space(name_leaf.prefix),
            Markers.EMPTY, [], name_leaf.value, None, None,
        )

        # Skip the name to find the optional `(...)` bases list, then `:`.
        idx = 2
        interfaces = None
        if idx < len(children) and getattr(children[idx], 'value', None) == '(':
            open_paren = children[idx]
            close_idx = idx + 1
            while close_idx < len(children) and getattr(children[close_idx], 'value', None) != ')':
                close_idx += 1
            if close_idx >= len(children):
                return self._convert_generic(node)
            close_paren = children[close_idx]
            inside = children[idx + 1:close_idx]
            leading = self._parse_space(open_paren.prefix)
            interfaces = self._convert_class_bases(inside, close_paren, leading)
            idx = close_idx + 1

        if idx >= len(children) or getattr(children[idx], 'value', None) != ':':
            return self._convert_generic(node)
        colon = children[idx]
        suite_node = children[idx + 1] if idx + 1 < len(children) else None
        if suite_node is None:
            return self._convert_generic(node)

        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))

        kind = j.ClassDeclaration.Kind(
            random_id(), kind_prefix, Markers.EMPTY,
            [], j.ClassDeclaration.Kind.Type.Class,
        )
        return j.ClassDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            leading_annotations or [],
            [],
            kind,
            name_ident,
            None,   # type_parameters
            None,   # primary_constructor
            None,   # extends
            interfaces,
            None,   # permits
            body,
            None,   # type
        )

    def _convert_class_bases(self, inside, close_paren, leading) -> JContainer:
        """Convert the contents between ``(`` and ``)`` of a class header."""
        before_close = self._parse_space(close_paren.prefix)
        if not inside:
            empty = j.Empty(random_id(), before_close, Markers.EMPTY)
            return JContainer(leading,
                              [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)],
                              Markers.EMPTY)
        # `class Foo(Base):` — one base node; `class Foo(A, B):` — testlist.
        if len(inside) == 1 and getattr(inside[0], 'type', None) == 'testlist':
            items = inside[0].children
        else:
            items = inside
        result = []
        i = 0
        while i < len(items):
            child = items[i]
            if getattr(child, 'value', None) == ',':
                i += 1
                continue
            base = self._convert_node(child)
            if base is None:
                # Unknown shape — bail to a single Empty so we don't drop
                # any tokens.
                empty = j.Empty(random_id(), before_close, Markers.EMPTY)
                return JContainer(leading,
                                  [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)],
                                  Markers.EMPTY)
            if i + 1 < len(items) and getattr(items[i + 1], 'value', None) == ',':
                comma = items[i + 1]
                result.append(JRightPadded(base, self._parse_space(comma.prefix), Markers.EMPTY))
                i += 2
            else:
                result.append(JRightPadded(base, before_close, Markers.EMPTY))
                i += 1
        return JContainer(leading, result, Markers.EMPTY)

    def _convert_if_stmt(self, node) -> Optional[j.J]:
        """Convert ``if`` / ``elif`` / ``else`` chain.

        Parso emits a flat list
        ``[if, test, :, suite, (elif, test, :, suite)*, (else, :, suite)?]``.
        We render each ``elif`` as a nested :class:`j.If` inside the parent's
        :class:`j.If.Else` slot, matching the Py3 visitor convention.
        """
        result = self._build_if_chain(node.children, 0)
        if result is None:
            return self._convert_generic(node)
        return result

    def _build_if_chain(self, children, start) -> Optional[j.If]:
        if start + 3 >= len(children):
            return None
        kw = children[start]
        test_node = children[start + 1]
        colon = children[start + 2]
        suite_node = children[start + 3]

        prefix = self._parse_space(kw.prefix)
        test_expr = self._convert_node(test_node)
        if test_expr is None:
            return None

        condition = j.ControlParentheses(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            JRightPadded(test_expr, Space.EMPTY, Markers.EMPTY),
        )
        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))
        then_part = JRightPadded(body, Space.EMPTY, Markers.EMPTY)

        elze = None
        idx = start + 4
        if idx < len(children):
            tail_kw = children[idx]
            tail_val = getattr(tail_kw, 'value', None)
            if tail_val == 'elif':
                nested = self._build_if_chain(children, idx)
                if nested is None:
                    return None
                # Inner If's prefix already holds the space before 'elif'; we
                # wrap it bare and keep j.If.Else.prefix as Space.EMPTY so the
                # printer doesn't double-print the gap.
                elze = j.If.Else(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    JRightPadded(nested, Space.EMPTY, Markers.EMPTY),
                )
            elif tail_val == 'else':
                else_colon = children[idx + 1]
                else_suite = children[idx + 2]
                else_block = self._convert_suite_to_block(else_suite, self._parse_space(else_colon.prefix))
                elze = j.If.Else(
                    random_id(),
                    self._parse_space(tail_kw.prefix),
                    Markers.EMPTY,
                    JRightPadded(else_block, Space.EMPTY, Markers.EMPTY),
                )

        return j.If(
            random_id(), prefix, Markers.EMPTY,
            condition, then_part, elze,
        )

    def _convert_while_stmt(self, node) -> Optional[j.J]:
        """Convert ``while expr: suite [else: suite]``."""
        children = node.children
        if len(children) < 4:
            return self._convert_generic(node)
        while_kw, test_node, colon, suite_node = children[0:4]
        prefix = self._parse_space(while_kw.prefix)

        test_expr = self._convert_node(test_node)
        if test_expr is None:
            return self._convert_generic(node)

        condition = j.ControlParentheses(
            random_id(), Space.EMPTY, Markers.EMPTY,
            JRightPadded(test_expr, Space.EMPTY, Markers.EMPTY),
        )
        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))
        loop = j.WhileLoop(
            random_id(), prefix, Markers.EMPTY,
            condition,
            JRightPadded(body, Space.EMPTY, Markers.EMPTY),
        )
        return self._maybe_wrap_else(loop, children, 4)

    def _convert_for_stmt(self, node) -> Optional[j.J]:
        """Convert ``for target in iterable: suite [else: suite]``.

        Single-name and tuple-destructure (``for i, j in ...``) targets
        are both supported; the destructure form is wrapped in a
        :class:`py.CollectionLiteral` of kind ``TUPLE``.
        """
        children = node.children
        if len(children) < 6:
            return self._convert_generic(node)
        for_kw, target_node, in_kw, iter_node, colon, suite_node = children[0:6]

        prefix = self._parse_space(for_kw.prefix)
        if getattr(target_node, 'type', None) == 'exprlist':
            items = self._pad_comma_list(target_node.children, Space.EMPTY)
            if items is None:
                return self._convert_generic(node)
            target = py.CollectionLiteral(
                random_id(), Space.EMPTY, Markers.EMPTY,
                py.CollectionLiteral.Kind.TUPLE,
                JContainer(Space.EMPTY, items, Markers.EMPTY),
                None,
            )
        else:
            target = self._convert_node(target_node)
        iterable = self._convert_node(iter_node)
        if target is None or iterable is None:
            return self._convert_generic(node)

        target_stmt = py.ExpressionStatement(random_id(), target)
        control = j.ForEachLoop.Control(
            random_id(), Space.EMPTY, Markers.EMPTY,
            JRightPadded(target_stmt, self._parse_space(in_kw.prefix), Markers.EMPTY),
            JRightPadded(iterable, Space.EMPTY, Markers.EMPTY),
        )
        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))
        loop = j.ForEachLoop(
            random_id(), prefix, Markers.EMPTY,
            control, JRightPadded(body, Space.EMPTY, Markers.EMPTY),
        )
        return self._maybe_wrap_else(loop, children, 6)

    def _maybe_wrap_else(self, loop, children, idx) -> j.J:
        """Wrap a while/for loop in :class:`py.TrailingElseWrapper` when a
        trailing ``else:`` clause is present in the parso children."""
        if idx >= len(children) or getattr(children[idx], 'value', None) != 'else':
            return loop
        else_kw = children[idx]
        else_colon = children[idx + 1]
        else_suite = children[idx + 2]
        else_block = self._convert_suite_to_block(
            else_suite, self._parse_space(else_colon.prefix),
        )
        return py.TrailingElseWrapper(
            random_id(),
            loop.prefix,
            Markers.EMPTY,
            loop.replace(_prefix=Space.EMPTY),
            JLeftPadded(self._parse_space(else_kw.prefix), else_block, Markers.EMPTY),
        )

    def _convert_suite_to_block(self, suite_node, block_prefix) -> j.Block:
        """Convert a parso ``suite`` to a :class:`j.Block`, threading
        newlines onto statement prefixes / trailing-padding so the printer
        can faithfully reproduce the original line layout. Any leftover
        whitespace that must follow a trailing-``;`` marker is routed to
        :attr:`j.Block.end` (the printer renders ``.after`` before
        markers, so it can't live in the last statement's ``.after``).
        """
        statements, end_ws = self._convert_stmt_block_children(suite_node.children)
        return j.Block(
            random_id(),
            block_prefix,
            Markers.EMPTY,
            JRightPadded(False, Space.EMPTY, Markers.EMPTY),  # not static
            statements,
            end_ws,
        )

    def _convert_try_stmt(self, node) -> Optional[j.J]:
        """Convert ``try`` / ``except`` / ``else`` / ``finally`` blocks.

        Both Py3 (``except E as e:``) and Py2 (``except E, e:``) catch forms
        are accepted; the Py2 form is marked with
        :class:`TupleExceptClause` so a future printer change can emit the
        legacy syntax.
        """
        children = node.children
        if len(children) < 4:
            return self._convert_generic(node)
        try_kw = children[0]
        try_colon = children[1]
        try_suite = children[2]
        prefix = self._parse_space(try_kw.prefix)
        body = self._convert_suite_to_block(try_suite, self._parse_space(try_colon.prefix))

        catches = []
        else_block = None
        finally_block = None

        i = 3
        while i < len(children):
            current = children[i]
            ctype = getattr(current, 'type', None)
            cval = getattr(current, 'value', None)
            if ctype == 'except_clause':
                # except_clause + ':' + suite
                except_clause = current
                if i + 2 >= len(children):
                    return self._convert_generic(node)
                colon = children[i + 1]
                suite = children[i + 2]
                catch = self._build_catch(except_clause, colon, suite)
                if catch is None:
                    return self._convert_generic(node)
                catches.append(catch)
                i += 3
            elif cval == 'except':
                # Bare `except:` (no except_clause wrapper).
                if i + 2 >= len(children):
                    return self._convert_generic(node)
                colon = children[i + 1]
                suite = children[i + 2]
                catch = self._build_bare_catch(current, colon, suite)
                if catch is None:
                    return self._convert_generic(node)
                catches.append(catch)
                i += 3
            elif cval == 'else':
                if i + 2 >= len(children):
                    return self._convert_generic(node)
                colon = children[i + 1]
                suite = children[i + 2]
                else_block = JLeftPadded(
                    self._parse_space(current.prefix),
                    self._convert_suite_to_block(suite, self._parse_space(colon.prefix)),
                    Markers.EMPTY,
                )
                i += 3
            elif cval == 'finally':
                if i + 2 >= len(children):
                    return self._convert_generic(node)
                colon = children[i + 1]
                suite = children[i + 2]
                finally_block = JLeftPadded(
                    self._parse_space(current.prefix),
                    self._convert_suite_to_block(suite, self._parse_space(colon.prefix)),
                    Markers.EMPTY,
                )
                i += 3
            else:
                # Unrecognized — bail out cleanly.
                return self._convert_generic(node)

        try_ = j.Try(
            random_id(), prefix, Markers.EMPTY,
            None,  # resources
            body, catches, finally_block,
        )
        if else_block is None:
            return try_
        return py.TrailingElseWrapper(
            random_id(), try_.prefix, Markers.EMPTY,
            try_.replace(_prefix=Space.EMPTY),
            else_block,
        )

    def _build_bare_catch(self, except_kw, colon, suite) -> Optional[j.Try.Catch]:
        """Build a catch for the bare ``except:`` form (no exception type)."""
        prefix = self._parse_space(except_kw.prefix)
        empty_type = py.ExceptionType(
            random_id(), Space.EMPTY, Markers.EMPTY,
            None, False,
            j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
        )
        empty_name = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], '', None, None)
        named = j.VariableDeclarations.NamedVariable(
            random_id(), Space.EMPTY, Markers.EMPTY, empty_name, [], None, None,
        )
        var_decl = j.VariableDeclarations(
            random_id(), Space.EMPTY, Markers.EMPTY,
            [], [], empty_type, None, [],
            [JRightPadded(named, Space.EMPTY, Markers.EMPTY)],
        )
        return j.Try.Catch(
            random_id(), prefix, Markers.EMPTY,
            j.ControlParentheses(
                random_id(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(var_decl, Space.EMPTY, Markers.EMPTY),
            ),
            self._convert_suite_to_block(suite, self._parse_space(colon.prefix)),
        )

    def _build_catch(self, except_clause, colon, suite) -> Optional[j.Try.Catch]:
        """Build a catch from a parso ``except_clause`` node + ':' + suite."""
        ec_children = except_clause.children
        except_kw = ec_children[0]
        prefix = self._parse_space(except_kw.prefix)

        # except_clause shapes:
        #   ['except']                         (handled separately above)
        #   ['except', type]
        #   ['except', type, 'as', name]
        #   ['except', type, ',', name]        ← Py2 form
        type_node = ec_children[1]
        type_expr = self._convert_node(type_node)
        if type_expr is None:
            return None
        exception_type = py.ExceptionType(
            random_id(), Space.EMPTY, Markers.EMPTY,
            None, False, type_expr,
        )

        name_ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], '', None, None)
        before_as = Space.EMPTY
        catch_markers = Markers.EMPTY
        if len(ec_children) >= 4:
            sep = ec_children[2]
            name_leaf = ec_children[3]
            before_as = self._parse_space(sep.prefix)
            sep_val = getattr(sep, 'value', None)
            if sep_val == ',':
                catch_markers = Markers.build(random_id(), [TupleExceptClause(random_id())])
            # Otherwise it's `as` — the Py3-style form.
            if hasattr(name_leaf, 'value'):
                name_ident = j.Identifier(
                    random_id(),
                    self._parse_space(name_leaf.prefix),
                    Markers.EMPTY, [], name_leaf.value, None, None,
                )

        named = j.VariableDeclarations.NamedVariable(
            random_id(), Space.EMPTY, Markers.EMPTY, name_ident, [], None, None,
        )
        var_decl = j.VariableDeclarations(
            random_id(), Space.EMPTY, Markers.EMPTY,
            [], [], exception_type, None, [],
            [JRightPadded(named, before_as, Markers.EMPTY)],
        )
        return j.Try.Catch(
            random_id(), prefix, catch_markers,
            j.ControlParentheses(
                random_id(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(var_decl, Space.EMPTY, Markers.EMPTY),
            ),
            self._convert_suite_to_block(suite, self._parse_space(colon.prefix)),
        )

    def _convert_with_stmt(self, node) -> Optional[j.J]:
        """Convert ``with`` statements.

        Grammar:
            with_stmt: 'with' with_item (',' with_item)* ':' suite
            with_item: test ['as' expr]
        """
        children = node.children
        if len(children) < 4:
            return self._convert_generic(node)
        with_kw = children[0]
        prefix = self._parse_space(with_kw.prefix)

        # Find the ':' that ends the head.
        colon_idx = None
        for k in range(1, len(children)):
            if getattr(children[k], 'value', None) == ':' and isinstance(children[k], parso_tree.PythonLeaf):
                colon_idx = k
                break
        if colon_idx is None:
            return self._convert_generic(node)
        colon = children[colon_idx]
        suite_node = children[colon_idx + 1] if colon_idx + 1 < len(children) else None
        if suite_node is None:
            return self._convert_generic(node)

        # Collect items (with_item or bare expression) separated by commas.
        items_children = children[1:colon_idx]
        resources = []
        i = 0
        while i < len(items_children):
            child = items_children[i]
            if getattr(child, 'value', None) == ',':
                i += 1
                continue
            # Capture the leading whitespace of this item (space between
            # ``with``/``,`` and the first leaf) so it can go on the
            # Resource.prefix — the printer emits each resource's prefix
            # after the comma it follows.
            first_leaf = (child if isinstance(child, parso_tree.PythonLeaf)
                          else (child.children[0] if getattr(child, 'children', None) else None))
            item_leading = (self._parse_space(first_leaf.prefix)
                            if first_leaf is not None and hasattr(first_leaf, 'prefix')
                            else Space.EMPTY)
            resource = self._build_with_resource(child, is_first=False)
            if resource is None:
                return self._convert_generic(node)
            resource = resource.replace(_prefix=item_leading)
            after = Space.EMPTY
            if i + 1 < len(items_children) and getattr(items_children[i + 1], 'value', None) == ',':
                comma = items_children[i + 1]
                after = self._parse_space(comma.prefix)
                i += 2
            else:
                i += 1
            resources.append(JRightPadded(resource, after, Markers.EMPTY))

        if not resources:
            return self._convert_generic(node)

        body = self._convert_suite_to_block(suite_node, self._parse_space(colon.prefix))
        # Each Resource carries its own leading whitespace on .prefix; the
        # container's leading slot stays empty, and OmitParentheses tells
        # the printer to skip the ``(...)`` wrapping.
        return j.Try(
            random_id(), prefix, Markers.EMPTY,
            JContainer(Space.EMPTY, resources,
                       Markers.build(random_id(), [OmitParentheses(random_id())])),
            body, [], None,
        )

    def _build_with_resource(self, item, is_first) -> Optional[j.Try.Resource]:
        """Convert a parso ``with_item`` (or bare expression) to :class:`j.Try.Resource`.

        The printer's ``with``-statement branch (in :meth:`visit_try`) walks
        the inner ``j.Assignment`` directly (printing ``ctx as target``), so
        we use that shape rather than wrapping in a :class:`py.ExpressionTypeTree`.
        Bare expressions (``with foo:``) are wrapped to satisfy the TypedTree
        contract.
        """
        if getattr(item, 'type', None) == 'with_item':
            ctx_node = item.children[0]
            as_kw = item.children[1]
            target_leaf = item.children[2]
            ctx_expr = self._convert_node(ctx_node)
            target = self._convert_node(target_leaf)
            if ctx_expr is None or target is None:
                return None
            # Strip the leading whitespace from the ctx_expr — it is
            # captured separately (on JContainer.before for the first item
            # or on JRightPadded.after of the preceding item). Leaving it
            # in place produces a double space in the printer output.
            ctx_expr = self._strip_leading_space(ctx_expr)
            var = j.Assignment(
                random_id(), Space.EMPTY, Markers.EMPTY,
                target,
                JLeftPadded(self._parse_space(as_kw.prefix), ctx_expr, Markers.EMPTY),
                None,
            )
        else:
            expr = self._convert_node(item)
            if expr is None:
                return None
            expr = self._strip_leading_space(expr)
            var = py.ExpressionTypeTree(random_id(), Space.EMPTY, Markers.EMPTY, expr)
        return j.Try.Resource(random_id(), Space.EMPTY, Markers.EMPTY, var, False)

    @staticmethod
    def _strip_leading_space(node):
        """Return ``node`` with its outer prefix reset to :attr:`Space.EMPTY`.

        Used when the caller has already captured the leading whitespace on
        a different LST slot (e.g. a ``JContainer.before``) and would
        otherwise emit a duplicated space at print time.
        """
        if not hasattr(node, 'prefix') or node.prefix == Space.EMPTY:
            return node
        try:
            return node.replace(_prefix=Space.EMPTY)
        except (TypeError, AttributeError):
            return node

    def _convert_import_name(self, node) -> Optional[j.J]:
        """Convert ``import x``, ``import x.y``, ``import x as y``, ``import a, b``.

        Single-name imports produce a bare :class:`j.Import` to match the Py3
        visitor convention; multi-name forms wrap in :class:`py.MultiImport`
        with ``from_=None``.
        """
        keyword = node.children[0]  # 'import'
        prefix = self._parse_space(keyword.prefix)
        rest = node.children[1:]
        if len(rest) != 1:
            return self._convert_generic(node)
        target = rest[0]

        if getattr(target, 'type', None) == 'dotted_as_names':
            imports = self._imports_from_as_names(target.children)
            if imports is None:
                return self._convert_generic(node)
            # The first import's qualid carries the space between 'import'
            # and the first name; the JContainer's leading space is EMPTY.
            return py.MultiImport(
                random_id(),
                prefix,
                Markers.EMPTY,
                None,  # from_
                False,  # parenthesized
                JContainer(Space.EMPTY, imports, Markers.EMPTY),
            )

        imp = self._build_one_import(target)
        if imp is None:
            return self._convert_generic(node)
        return j.Import(
            random_id(),
            prefix,
            Markers.EMPTY,
            imp.padding.static,
            imp.qualid,
            imp.padding.alias,
        )

    def _convert_import_from(self, node) -> Optional[j.J]:
        """Convert ``from M import X``, ``from M import X, Y``,
        ``from M import (X, Y)``, ``from M import *``, and relative
        ``from . import X`` / ``from .pkg import X`` forms.

        Always produces :class:`py.MultiImport`; the ``from_`` slot carries
        the module reference.
        """
        children = node.children
        # children[0] is 'from'. Find the 'import' keyword's index.
        prefix = self._parse_space(children[0].prefix)
        import_idx = next(
            (i for i, c in enumerate(children)
             if getattr(c, 'type', None) == 'keyword' and getattr(c, 'value', None) == 'import'),
            -1,
        )
        if import_idx < 1:
            return self._convert_generic(node)

        module_children = children[1:import_idx]
        import_kw = children[import_idx]
        after_import = children[import_idx + 1:]

        from_tree = self._build_from_module(module_children)
        if from_tree is None:
            return self._convert_generic(node)

        # JRightPadded.after holds the space between the module ref and 'import'.
        from_padded = JRightPadded(from_tree, self._parse_space(import_kw.prefix), Markers.EMPTY)

        # Parse the right-hand-side names. Two parenthesized forms exist:
        # `(a, b)` (list_in_parens) and bare names.
        parenthesized = False
        names_leading = Space.EMPTY
        close_paren = None
        rhs = after_import
        if rhs and getattr(rhs[0], 'value', None) == '(':
            parenthesized = True
            open_paren = rhs[0]
            close_paren = rhs[-1] if getattr(rhs[-1], 'value', None) == ')' else None
            names_leading = self._parse_space(open_paren.prefix)
            rhs = rhs[1:-1] if close_paren else rhs[1:]
        # For the non-parenthesized form, the leading whitespace is
        # preserved on the inner Identifier inside each import's qualid
        # (set by ``_build_qualid``). Leaving ``names_leading`` empty
        # avoids double-emitting that space.

        if len(rhs) == 1 and getattr(rhs[0], 'value', None) == '*':
            star = rhs[0]
            star_qualid = j.FieldAccess(
                random_id(),
                self._parse_space(star.prefix),
                Markers.EMPTY,
                j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                JLeftPadded(
                    Space.EMPTY,
                    j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], '*', None, None),
                    Markers.EMPTY,
                ),
                None,
            )
            imports = [JRightPadded(
                j.Import(
                    random_id(), Space.EMPTY, Markers.EMPTY,
                    JLeftPadded(Space.EMPTY, False, Markers.EMPTY),
                    star_qualid, None,
                ),
                Space.EMPTY,
                Markers.EMPTY,
            )]
        elif len(rhs) == 1 and getattr(rhs[0], 'type', None) == 'import_as_names':
            imports = self._imports_from_as_names(rhs[0].children)
            if imports is None:
                return self._convert_generic(node)
        elif len(rhs) >= 1:
            # Single import name (with or without `as`), flat in the children.
            imports = self._imports_from_as_names(rhs)
            if imports is None:
                return self._convert_generic(node)
        else:
            return self._convert_generic(node)

        # When parenthesized, the last import's `after` covers space before `)`.
        if parenthesized and close_paren is not None and imports:
            last = imports[-1]
            imports[-1] = JRightPadded(last.element, self._parse_space(close_paren.prefix), last.markers)

        return py.MultiImport(
            random_id(),
            prefix,
            Markers.EMPTY,
            from_padded,
            parenthesized,
            JContainer(names_leading, imports, Markers.EMPTY),
        )

    def _imports_from_as_names(self, items) -> Optional[list]:
        """Walk a flat ``[item, ',', item, ',', ...]`` list of import items
        (each either a NAME / dotted_name or a dotted_as_name / import_as_name)
        into ``JRightPadded[j.Import]`` entries."""
        out = []
        i = 0
        while i < len(items):
            child = items[i]
            if getattr(child, 'value', None) == ',':
                i += 1
                continue
            imp = self._build_one_import(child)
            if imp is None:
                return None
            if i + 1 < len(items) and getattr(items[i + 1], 'value', None) == ',':
                comma = items[i + 1]
                out.append(JRightPadded(imp, self._parse_space(comma.prefix), Markers.EMPTY))
                i += 2
            else:
                out.append(JRightPadded(imp, Space.EMPTY, Markers.EMPTY))
                i += 1
        return out

    def _build_one_import(self, target) -> Optional[j.Import]:
        """Build a ``j.Import`` for a single import item.

        Accepts:
        * a NAME leaf — ``import os``
        * a ``dotted_name`` node — ``import os.path``
        * a ``dotted_as_name`` node — ``import os as o``
        * an ``import_as_name`` node — ``X as Y`` inside a ``from`` clause
        """
        alias = None
        if getattr(target, 'type', None) in ('dotted_as_name', 'import_as_name'):
            inner_name = target.children[0]
            as_kw = target.children[1]
            alias_leaf = target.children[2]
            alias_ident = j.Identifier(
                random_id(),
                self._parse_space(alias_leaf.prefix),
                Markers.EMPTY, [], alias_leaf.value, None, None,
            )
            alias = JLeftPadded(self._parse_space(as_kw.prefix), alias_ident, Markers.EMPTY)
            target = inner_name

        qualid = self._build_qualid(target)
        if qualid is None:
            return None
        return j.Import(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            JLeftPadded(Space.EMPTY, False, Markers.EMPTY),
            qualid,
            alias,
        )

    def _build_qualid(self, node_or_leaf) -> Optional[j.FieldAccess]:
        """Build a :class:`j.FieldAccess` for an :class:`j.Import.qualid`.

        For a single bare name, returns ``FieldAccess(target=Empty, name)``
        which the import printer special-cases (it emits just the name,
        not ``.name``). For dotted names, returns a left-associative chain
        whose leftmost target is a bare :class:`j.Identifier` (not an
        Empty-target FieldAccess) so a regular FieldAccess print walks
        produce ``a.b.c`` without a stray leading dot.
        """
        if isinstance(node_or_leaf, parso_tree.PythonLeaf):
            # The Identifier carries the parso prefix so that both the
            # standalone ``j.Import`` printer branch (which would otherwise
            # consume the FieldAccess's outer prefix) and the
            # ``py.MultiImport`` printer branch (which skips the FieldAccess
            # prefix and only renders the name) reproduce the whitespace
            # before the name.
            ident = j.Identifier(
                random_id(),
                self._parse_space(node_or_leaf.prefix),
                Markers.EMPTY, [],
                node_or_leaf.value, None, None,
            )
            return j.FieldAccess(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                JLeftPadded(Space.EMPTY, ident, Markers.EMPTY),
                None,
            )

        if getattr(node_or_leaf, 'type', None) != 'dotted_name':
            return None

        children = node_or_leaf.children
        first = children[0]
        if not isinstance(first, parso_tree.PythonLeaf):
            return None
        # Leftmost element is a bare Identifier carrying the prefix of the
        # whole dotted chain.
        leftmost: j.J = j.Identifier(
            random_id(),
            self._parse_space(first.prefix),
            Markers.EMPTY, [], first.value, None, None,
        )
        i = 1
        while i + 1 <= len(children) - 1:
            dot = children[i]
            name_leaf = children[i + 1]
            name_ident = j.Identifier(
                random_id(),
                self._parse_space(name_leaf.prefix),
                Markers.EMPTY, [], name_leaf.value, None, None,
            )
            leftmost = j.FieldAccess(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                leftmost,
                JLeftPadded(self._parse_space(dot.prefix), name_ident, Markers.EMPTY),
                None,
            )
            i += 2
        return leftmost

    def _build_from_module(self, items) -> Optional[j.J]:
        """Construct the module reference between ``from`` and ``import``.

        Forms handled (matching the Py3 visitor's convention of folding
        leading dots into the textual name):
        * ``[name]``                  → :class:`j.FieldAccess` wrapping the name
        * ``[dotted_name]``           → :class:`j.FieldAccess` chain
        * ``[. . ... ]`` (pure dots)  → :class:`j.Identifier` whose value is the dots
        * ``[. . pkg]`` (mixed)       → :class:`j.Identifier` whose value is ``..pkg``
        """
        if not items:
            return None

        dot_count = 0
        idx = 0
        while idx < len(items) and getattr(items[idx], 'value', None) == '.':
            dot_count += 1
            idx += 1

        if dot_count == 0:
            target = items[0]
            # Single NAME → bare :class:`j.Identifier`; dotted → :class:`j.FieldAccess`.
            # The printer renders the former cleanly while the latter chains dots.
            if isinstance(target, parso_tree.PythonLeaf):
                return j.Identifier(
                    random_id(),
                    self._parse_space(target.prefix),
                    Markers.EMPTY, [], target.value, None, None,
                )
            return self._build_qualid(target)

        first_dot = items[0]
        prefix = self._parse_space(first_dot.prefix)
        rest = items[idx:]
        if not rest:
            text = '.' * dot_count
        else:
            # Mixed dot+name forms (e.g. ``from .pkg.sub import X``). We
            # collect the textual form to preserve round-trip without
            # inventing a new LST shape for "relative dotted name".
            parts = []
            for c in rest:
                if isinstance(c, parso_tree.PythonLeaf):
                    parts.append(c.value)
                elif getattr(c, 'type', None) == 'dotted_name':
                    parts.append(c.get_code(include_prefix=False))
                else:
                    parts.append(c.get_code(include_prefix=False))
            text = '.' * dot_count + ''.join(parts)
        return j.Identifier(
            random_id(), prefix, Markers.EMPTY, [], text, None, None,
        )

    def _convert_suite(self, node) -> j.Block:
        """Convert a suite (block of statements)."""
        statements = []
        for child in node.children:
            stmt = self._convert_node(child)
            if stmt:
                statements.append(self._pad_statement(stmt))
        return j.Block(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            JRightPadded(False, Space.EMPTY, Markers.EMPTY),  # static
            statements,
            Space.EMPTY
        )

    def _convert_atom(self, node) -> Optional[j.J]:
        """Convert an atom (basic expression).

        Handles:
        * Single-child passthrough (NAME/NUMBER/STRING).
        * Backtick repr ``` `expr` ``` (Python 2 only) — already covered.
        * Parenthesized single expression ``(expr)`` → :class:`j.Parentheses`.

        Not yet handled (TODO): tuples ``(a, b)``, list ``[...]``, dict ``{k:v}``,
        set ``{...}``, and generator expressions. These fall through to the
        generic ``<atom>`` placeholder.
        """
        if len(node.children) == 1:
            return self._convert_node(node.children[0])

        # Handle backtick repr: `expr` (Python 2 only)
        if (len(node.children) >= 3 and
                hasattr(node.children[0], 'value') and node.children[0].value == '`' and
                hasattr(node.children[-1], 'value') and node.children[-1].value == '`'):
            prefix = self._parse_space(node.children[0].prefix)
            # Reconstruct the full backtick expression as a literal
            inner = ''.join(c.get_code() for c in node.children[1:-1])
            value_source = '`' + inner + '`'
            markers = Markers.build(random_id(), [Quoted(random_id(), Quoted.Style.BACKTICK)])
            return j.Literal(
                random_id(),
                prefix,
                markers,
                value_source,
                value_source,
                None,  # unicode_escapes
                JavaType.Primitive.String
            )

        # Paren / bracket / brace forms.
        first_v = getattr(node.children[0], 'value', None)
        last_v = getattr(node.children[-1], 'value', None)
        if first_v == '[' and last_v == ']':
            return self._convert_list_atom(node)
        if first_v == '{' and last_v == '}':
            return self._convert_braced_atom(node)
        if first_v == '(' and last_v == ')':
            return self._convert_paren_atom(node)

        return self._convert_generic(node)

    def _convert_paren_atom(self, node) -> Optional[j.J]:
        """Convert ``(...)`` atom: parenthesized expression, empty tuple, or tuple literal."""
        open_paren = node.children[0]
        close_paren = node.children[-1]
        leading = self._parse_space(open_paren.prefix)
        before_close = self._parse_space(close_paren.prefix)

        if len(node.children) == 2:
            # Empty tuple `()`.
            empty = j.Empty(random_id(), before_close, Markers.EMPTY)
            return py.CollectionLiteral(
                random_id(), leading, Markers.EMPTY,
                py.CollectionLiteral.Kind.TUPLE,
                JContainer(Space.EMPTY, [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)], Markers.EMPTY),
                None,
            )

        inner = node.children[1]
        # Single non-tuple expression → Parentheses.
        if getattr(inner, 'type', None) not in ('testlist_comp', 'testlist'):
            inner_expr = self._convert_node(inner)
            if inner_expr is None:
                return None
            return j.Parentheses(
                random_id(), leading, Markers.EMPTY,
                JRightPadded(inner_expr, before_close, Markers.EMPTY),
            )

        # `(expr for x in xs)` — generator expression.
        if getattr(inner, 'type', None) == 'testlist_comp' and self._is_comprehension(inner.children):
            return self._build_listcomp(inner, leading, before_close,
                                        py.ComprehensionExpression.Kind.GENERATOR)
        # Tuple. testlist_comp here is the comma-separated, non-comprehension form.
        items = self._pad_comma_list(inner.children, before_close)
        if items is None:
            return None
        # `(x,)` (1-element tuple) is still a tuple; the trailing comma is
        # preserved in the JRightPadded.after of the lone element.
        return py.CollectionLiteral(
            random_id(), leading, Markers.EMPTY,
            py.CollectionLiteral.Kind.TUPLE,
            JContainer(Space.EMPTY, items, Markers.EMPTY),
            None,
        )

    def _convert_list_atom(self, node) -> Optional[j.J]:
        """Convert ``[...]`` atom — list literal (comprehensions deferred)."""
        open_b = node.children[0]
        close_b = node.children[-1]
        leading = self._parse_space(open_b.prefix)
        before_close = self._parse_space(close_b.prefix)

        if len(node.children) == 2:
            empty = j.Empty(random_id(), before_close, Markers.EMPTY)
            return py.CollectionLiteral(
                random_id(), leading, Markers.EMPTY,
                py.CollectionLiteral.Kind.LIST,
                JContainer(Space.EMPTY, [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)], Markers.EMPTY),
                None,
            )
        inner = node.children[1]
        if getattr(inner, 'type', None) == 'testlist_comp' and self._is_comprehension(inner.children):
            return self._build_listcomp(inner, leading, before_close,
                                        py.ComprehensionExpression.Kind.LIST)
        item_nodes = inner.children if getattr(inner, 'type', None) == 'testlist_comp' else [inner]
        items = self._pad_comma_list(item_nodes, before_close)
        if items is None:
            return None
        return py.CollectionLiteral(
            random_id(), leading, Markers.EMPTY,
            py.CollectionLiteral.Kind.LIST,
            JContainer(Space.EMPTY, items, Markers.EMPTY),
            None,
        )

    def _build_listcomp(self, testlist_comp, leading, suffix, kind) -> Optional[j.J]:
        """Build a list/set/generator comprehension from a ``testlist_comp``."""
        children = testlist_comp.children
        if len(children) < 2:
            return None
        result_expr = self._convert_node(children[0])
        if result_expr is None:
            return None
        return self._build_comprehension(kind, result_expr, children[1],
                                         leading, suffix)

    def _convert_braced_atom(self, node) -> Optional[j.J]:
        """Convert ``{...}`` atom — dict, set, or empty dict.

        Empty ``{}`` is a dict literal (Python convention).
        ``{1: 2}`` → dict (look for ``:`` separator).
        ``{1, 2}`` → set.
        """
        open_b = node.children[0]
        close_b = node.children[-1]
        leading = self._parse_space(open_b.prefix)
        before_close = self._parse_space(close_b.prefix)

        if len(node.children) == 2:
            empty = j.Empty(random_id(), before_close, Markers.EMPTY)
            return py.DictLiteral(
                random_id(), leading, Markers.EMPTY,
                JContainer(Space.EMPTY, [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)], Markers.EMPTY),
                None,
            )

        inner = node.children[1]
        if getattr(inner, 'type', None) != 'dictorsetmaker':
            # Single element set like `{x}`.
            elem = self._convert_node(inner)
            if elem is None:
                return None
            return py.CollectionLiteral(
                random_id(), leading, Markers.EMPTY,
                py.CollectionLiteral.Kind.SET,
                JContainer(Space.EMPTY, [JRightPadded(elem, before_close, Markers.EMPTY)], Markers.EMPTY),
                None,
            )

        kids = inner.children
        if self._is_comprehension(kids):
            # Either ``{x for x in xs}`` (set comp) or ``{k: v for k, v in d}`` (dict comp).
            has_colon = any(getattr(c, 'value', None) == ':' for c in kids)
            if has_colon:
                # Find the colon, the value is between colon and the comp_for.
                colon_idx = next(i for i, c in enumerate(kids)
                                 if getattr(c, 'value', None) == ':')
                key = self._convert_node(kids[0])
                value = self._convert_node(kids[colon_idx + 1])
                comp_for_idx = next(i for i, c in enumerate(kids)
                                    if getattr(c, 'type', None) in ('comp_for', 'sync_comp_for'))
                if key is None or value is None:
                    return None
                key_padded = JRightPadded(key, self._parse_space(kids[colon_idx].prefix), Markers.EMPTY)
                result = py.KeyValue(random_id(), Space.EMPTY, Markers.EMPTY,
                                     key_padded, value, None)
                return self._build_comprehension(
                    py.ComprehensionExpression.Kind.DICT,
                    result, kids[comp_for_idx], leading, before_close,
                )
            # set comprehension
            result_expr = self._convert_node(kids[0])
            if result_expr is None:
                return None
            return self._build_comprehension(
                py.ComprehensionExpression.Kind.SET,
                result_expr, kids[1], leading, before_close,
            )
        # Decide dict vs. set by presence of a top-level ':'.
        is_dict = any(getattr(c, 'value', None) == ':' for c in kids)
        if is_dict:
            entries = self._build_dict_entries(kids, before_close)
            if entries is None:
                return None
            return py.DictLiteral(
                random_id(), leading, Markers.EMPTY,
                JContainer(Space.EMPTY, entries, Markers.EMPTY),
                None,
            )
        items = self._pad_comma_list(kids, before_close)
        if items is None:
            return None
        return py.CollectionLiteral(
            random_id(), leading, Markers.EMPTY,
            py.CollectionLiteral.Kind.SET,
            JContainer(Space.EMPTY, items, Markers.EMPTY),
            None,
        )

    @staticmethod
    def _is_comprehension(children) -> bool:
        """True if a ``testlist_comp`` / ``dictorsetmaker`` child list
        contains a ``(sync_)comp_for`` clause — i.e. it's a comprehension or
        generator rather than a plain literal."""
        return any(getattr(c, 'type', None) in ('comp_for', 'sync_comp_for') for c in children)

    def _build_comprehension(self, kind, result, clause_children, leading, suffix) -> Optional[j.J]:
        """Build a :class:`py.ComprehensionExpression`.

        ``result`` is the LST node for the value expression that precedes
        the ``for`` clauses (or a KeyValue for dict comps).
        ``clause_children`` is the parso ``sync_comp_for`` node's children.
        """
        clauses = self._parse_comp_clauses(clause_children)
        if clauses is None:
            return None
        return py.ComprehensionExpression(
            random_id(), leading, Markers.EMPTY,
            kind, result, clauses, suffix, None,
        )

    def _parse_comp_clauses(self, node) -> Optional[list]:
        """Walk a (chain of) ``(sync_)comp_for`` / ``comp_if`` nodes into
        :class:`py.ComprehensionExpression.Clause` entries.

        Grammar:
            sync_comp_for: 'for' exprlist 'in' or_test [comp_iter]
            comp_iter:     comp_for | comp_if
            comp_if:       'if' test_nocond [comp_iter]
        Each ``for`` boundary begins a new Clause; ``if`` clauses are
        attached as conditions on the most recent Clause.
        """
        clauses: list = []
        conditions: list = []
        current = node

        def stash_clause(for_kw, target_node, in_kw, iterable_node):
            if getattr(target_node, 'type', None) == 'exprlist':
                tuple_items = self._pad_comma_list(target_node.children, Space.EMPTY)
                if tuple_items is None:
                    return False
                target = py.CollectionLiteral(
                    random_id(), Space.EMPTY, Markers.EMPTY,
                    py.CollectionLiteral.Kind.TUPLE,
                    JContainer(Space.EMPTY, tuple_items, Markers.EMPTY),
                    None,
                )
            else:
                target = self._convert_node(target_node)
            iterable = self._convert_node(iterable_node)
            if target is None or iterable is None:
                return False
            clause = py.ComprehensionExpression.Clause(
                random_id(),
                self._parse_space(for_kw.prefix),
                Markers.EMPTY,
                None,  # async
                target,
                JLeftPadded(self._parse_space(in_kw.prefix), iterable, Markers.EMPTY),
                conditions or None,
            )
            clauses.append(clause)
            return True

        while current is not None:
            ctype = getattr(current, 'type', None)
            if ctype in ('comp_for', 'sync_comp_for'):
                children = current.children
                if len(children) < 4:
                    return None
                for_kw = children[0]
                target_node = children[1]
                in_kw = children[2]
                iter_node = children[3]
                conditions = []
                if not stash_clause(for_kw, target_node, in_kw, iter_node):
                    return None
                current = children[4] if len(children) > 4 else None
            elif ctype == 'comp_if':
                children = current.children
                if len(children) < 2:
                    return None
                if_kw = children[0]
                cond_node = children[1]
                cond_expr = self._convert_node(cond_node)
                if cond_expr is None:
                    return None
                condition = py.ComprehensionExpression.Condition(
                    random_id(),
                    self._parse_space(if_kw.prefix),
                    Markers.EMPTY,
                    cond_expr,
                )
                # Attach to most-recent clause
                last = clauses[-1]
                new_conds = list(last.conditions or []) + [condition]
                clauses[-1] = py.ComprehensionExpression.Clause(
                    random_id(), last.prefix, last.markers,
                    None, last.iterator_variable,
                    JLeftPadded(last.padding.iterated_list.before,
                                last.padding.iterated_list.element,
                                last.padding.iterated_list.markers),
                    new_conds,
                )
                current = children[2] if len(children) > 2 else None
            else:
                # Unknown node — bail out so the caller falls back.
                return None

        return clauses

    def _pad_comma_list(self, items, before_close) -> Optional[list]:
        """Turn a ``[item, ',', item, ',', ...]`` flat parso list into
        :class:`JRightPadded` entries.

        A trailing comma — present in ``(1, 2,)``, ``[1, 2,]``, etc. — is
        recorded via the :class:`TrailingComma` marker on the last element
        with its ``suffix`` carrying the whitespace between the comma and
        the closing delimiter, so the printer round-trips both the comma
        and the whitespace exactly.
        """
        result = []
        i = 0
        while i < len(items):
            child = items[i]
            if getattr(child, 'value', None) == ',':
                i += 1
                continue
            expr = self._convert_node(child)
            if expr is None:
                return None
            if i + 1 < len(items) and getattr(items[i + 1], 'value', None) == ',':
                comma = items[i + 1]
                comma_prefix = self._parse_space(comma.prefix)
                if i + 2 >= len(items):
                    # Trailing comma at the very end of the list.
                    result.append(JRightPadded(
                        expr, comma_prefix,
                        Markers.build(random_id(), [TrailingComma(random_id(), before_close)]),
                    ))
                else:
                    result.append(JRightPadded(expr, comma_prefix, Markers.EMPTY))
                i += 2
            else:
                result.append(JRightPadded(expr, before_close, Markers.EMPTY))
                i += 1
        return result

    def _build_dict_entries(self, kids, before_close) -> Optional[list]:
        """Walk a ``dictorsetmaker`` list for the dict case:
        ``[key, ':', value, ',', key, ':', value, ...]`` (optional trailing comma).

        Trailing commas are recorded via the :class:`TrailingComma` marker
        on the last entry, with the suffix carrying the whitespace before
        the closing ``}`` — mirroring the convention used by
        :meth:`_pad_comma_list` so the printer round-trips both shapes
        consistently.
        """
        entries = []
        i = 0
        while i < len(kids):
            k = kids[i]
            if getattr(k, 'value', None) == ',':
                i += 1
                continue
            key = self._convert_node(k)
            if key is None or i + 2 >= len(kids):
                return None
            colon = kids[i + 1]
            if getattr(colon, 'value', None) != ':':
                return None
            value = self._convert_node(kids[i + 2])
            if value is None:
                return None
            key_padded = JRightPadded(key, self._parse_space(colon.prefix), Markers.EMPTY)
            entry = py.KeyValue(random_id(), Space.EMPTY, Markers.EMPTY,
                                key_padded, value, None)
            i += 3
            if i < len(kids) and getattr(kids[i], 'value', None) == ',':
                comma = kids[i]
                comma_prefix = self._parse_space(comma.prefix)
                i += 1
                if i >= len(kids):
                    entries.append(JRightPadded(
                        entry, comma_prefix,
                        Markers.build(random_id(), [TrailingComma(random_id(), before_close)]),
                    ))
                else:
                    entries.append(JRightPadded(entry, comma_prefix, Markers.EMPTY))
            else:
                entries.append(JRightPadded(entry, before_close, Markers.EMPTY))
        return entries

    def _convert_power(self, node) -> Optional[j.J]:
        """Convert a power expression.

        Grammar:
            power: atom trailer* ['**' factor]
            trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME

        Trailers are applied left-to-right to the atom. A trailing ``**``
        wraps the result in a right-associative ``py.Binary(Power)``.
        """
        if len(node.children) == 1:
            return self._convert_node(node.children[0])

        children = node.children

        # Detect trailing `'**' factor` — it's always the last two children.
        pow_idx = -1
        if len(children) >= 3 and getattr(children[-2], 'value', None) == '**':
            pow_idx = len(children) - 2
        trailer_end = pow_idx if pow_idx >= 0 else len(children)

        # Start with the atom (which may carry leading whitespace as its prefix).
        current = self._convert_node(children[0])
        if current is None:
            return self._convert_generic(node)

        for i in range(1, trailer_end):
            trailer = children[i]
            applied = self._apply_trailer(current, trailer)
            if applied is None:
                return self._convert_generic(node)
            current = applied

        if pow_idx >= 0:
            op_leaf = children[pow_idx]
            rhs = self._convert_node(children[pow_idx + 1])
            if rhs is None:
                return self._convert_generic(node)
            current = py.Binary(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                current,
                JLeftPadded(self._parse_space(op_leaf.prefix), py.Binary.Type.Power, Markers.EMPTY),
                None,  # negation slot (unused)
                rhs,
                None,
            )

        return current

    def _apply_trailer(self, base, trailer) -> Optional[j.J]:
        """Apply one parso ``trailer`` node to ``base``.

        Trailer shapes:
            ``'.' NAME``                → :class:`j.FieldAccess`
            ``'(' [arglist] ')'``       → :class:`j.MethodInvocation`
            ``'[' subscriptlist ']'``   → :class:`j.ArrayAccess`
        """
        op_leaf = trailer.children[0]
        op_value = getattr(op_leaf, 'value', None)

        if op_value == '.':
            # '.' NAME
            name_leaf = trailer.children[1]
            name_ident = self._convert_node(name_leaf)
            if not isinstance(name_ident, j.Identifier):
                return None
            return j.FieldAccess(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                base,
                JLeftPadded(self._parse_space(op_leaf.prefix), name_ident, Markers.EMPTY),
                None,  # type
            )

        if op_value == '(':
            close_paren = trailer.children[-1]
            inner = trailer.children[1:-1]
            args = self._convert_call_args(inner, close_paren, self._parse_space(op_leaf.prefix))
            if args is None:
                return None
            # Re-shape the base into MethodInvocation form. When the base is
            # a FieldAccess we pull it apart so the method name and select are
            # explicit on the invocation node.
            if isinstance(base, j.FieldAccess):
                select_after = base.padding.name.before
                return j.MethodInvocation(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    JRightPadded(base.target, select_after, Markers.EMPTY),
                    None,
                    base.name,
                    args,
                    None,
                )
            if isinstance(base, j.Identifier):
                return j.MethodInvocation(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    None,
                    None,
                    base,
                    args,
                    None,
                )
            # Call on an arbitrary expression result, e.g. ``f()()``.
            return j.MethodInvocation(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded(base, Space.EMPTY, Markers.EMPTY),
                None,
                j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "", None, None),
                args,
                None,
            )

        if op_value == '[':
            close_bracket = trailer.children[-1]
            inner = trailer.children[1] if len(trailer.children) == 3 else None
            if inner is None:
                return None
            if getattr(inner, 'type', None) == 'subscript':
                index = self._convert_slice(inner)
            elif getattr(inner, 'type', None) == 'subscriptlist':
                # Multi-element subscript ``a[i, j]`` — render as a tuple.
                items = self._pad_comma_list(inner.children, Space.EMPTY)
                if items is None:
                    return None
                index = py.CollectionLiteral(
                    random_id(), Space.EMPTY, Markers.EMPTY,
                    py.CollectionLiteral.Kind.TUPLE,
                    JContainer(Space.EMPTY, items, Markers.EMPTY),
                    None,
                )
            else:
                index = self._convert_node(inner)
            if index is None:
                return None
            return j.ArrayAccess(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                base,
                j.ArrayDimension(
                    random_id(),
                    self._parse_space(op_leaf.prefix),
                    Markers.EMPTY,
                    JRightPadded(index, self._parse_space(close_bracket.prefix), Markers.EMPTY),
                ),
                None,
            )

        return None

    def _convert_slice(self, subscript_node) -> Optional[j.J]:
        """Convert a parso ``subscript`` (start[:stop[:step]]) to :class:`py.Slice`.

        Each component is optional; absent components produce ``None`` slots.
        ``a[::2]`` parses as ``[':', sliceop[':', 2]]`` — no start and no stop.
        """
        children = subscript_node.children
        start = None
        stop = None
        step = None
        idx = 0
        # Parse optional start (any non-':' child before the first ':').
        if idx < len(children) and getattr(children[idx], 'value', None) != ':':
            expr = self._convert_node(children[idx])
            if expr is None:
                return None
            start = JRightPadded(expr, Space.EMPTY, Markers.EMPTY)
            idx += 1
        # First ':'.
        if idx >= len(children) or getattr(children[idx], 'value', None) != ':':
            return None
        idx += 1
        # Optional stop.
        if idx < len(children) and getattr(children[idx], 'value', None) != ':' \
                and getattr(children[idx], 'type', None) != 'sliceop':
            expr = self._convert_node(children[idx])
            if expr is None:
                return None
            stop = JRightPadded(expr, Space.EMPTY, Markers.EMPTY)
            idx += 1
        # Optional sliceop = [':' [expr]].
        if idx < len(children) and getattr(children[idx], 'type', None) == 'sliceop':
            sop = children[idx]
            if len(sop.children) >= 2:
                expr = self._convert_node(sop.children[1])
                if expr is None:
                    return None
                step = JRightPadded(expr, Space.EMPTY, Markers.EMPTY)
        return py.Slice(random_id(), Space.EMPTY, Markers.EMPTY, start, stop, step)

    def _convert_call_args(self, inner, close_paren, leading_space) -> Optional[JContainer]:
        """Build the JContainer holding a function call's arguments.

        ``inner`` is the list of parso children between ``(`` and ``)``;
        ``close_paren`` is the ``)`` leaf (used to capture the space before it);
        ``leading_space`` is the space before the ``(``.

        Argument forms currently handled: positional expressions. Not yet
        handled: keyword arguments (``f(a=1)``), star/double-star unpacking
        (``f(*xs, **kw)``), generator-expression call argument (``f(x for x in xs)``).
        """
        before_close = self._parse_space(close_paren.prefix)

        # Unwrap arglist if present.
        if len(inner) == 1 and getattr(inner[0], 'type', None) == 'arglist':
            items = list(inner[0].children)
        else:
            items = list(inner)

        if not items:
            empty = j.Empty(random_id(), before_close, Markers.EMPTY)
            return JContainer(
                leading_space,
                [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)],
                Markers.EMPTY,
            )

        args = []
        i = 0
        while i < len(items):
            child = items[i]
            cval = getattr(child, 'value', None)
            if cval == ',':
                # Stray comma — skip (e.g. trailing comma after last arg).
                i += 1
                continue
            if cval == '*' and i + 1 < len(items):
                # ``*xs`` unpack
                star_leaf = child
                inner = self._convert_node(items[i + 1])
                if inner is None:
                    return None
                arg = py.Star(
                    random_id(),
                    self._parse_space(star_leaf.prefix),
                    Markers.EMPTY,
                    py.Star.Kind.LIST,
                    inner,
                    None,
                )
                i += 2
            elif cval == '**' and i + 1 < len(items):
                # ``**kw`` unpack
                star_leaf = child
                inner = self._convert_node(items[i + 1])
                if inner is None:
                    return None
                arg = py.Star(
                    random_id(),
                    self._parse_space(star_leaf.prefix),
                    Markers.EMPTY,
                    py.Star.Kind.DICT,
                    inner,
                    None,
                )
                i += 2
            elif getattr(child, 'type', None) == 'argument':
                arg = self._convert_argument(child)
                if arg is None:
                    return None
                i += 1
            else:
                arg = self._convert_node(child)
                if arg is None:
                    return None
                i += 1
            if i < len(items) and getattr(items[i], 'value', None) == ',':
                comma = items[i]
                comma_prefix = self._parse_space(comma.prefix)
                i += 1
                # A comma that lands at the very end of the items list is a
                # trailing comma (``foo(a, b,)``). We record it via the
                # :class:`TrailingComma` marker whose suffix carries the
                # whitespace between the comma and ``)`` so round-trip is
                # byte-exact.
                if i >= len(items):
                    args.append(JRightPadded(
                        arg, comma_prefix,
                        Markers.build(random_id(), [TrailingComma(random_id(), before_close)]),
                    ))
                else:
                    args.append(JRightPadded(arg, comma_prefix, Markers.EMPTY))
            else:
                args.append(JRightPadded(arg, before_close, Markers.EMPTY))

        return JContainer(leading_space, args, Markers.EMPTY)

    def _convert_argument(self, arg_node) -> Optional[j.J]:
        """Convert a parso ``argument`` node.

        Shapes:
        * ``NAME '=' test``           → :class:`py.NamedArgument` (keyword arg)
        * ``test (sync_)comp_for``    → generator expression as call arg
        """
        children = arg_node.children
        if len(children) >= 3 and getattr(children[1], 'value', None) == '=':
            name_leaf = children[0]
            if not hasattr(name_leaf, 'value'):
                return None
            name_ident = j.Identifier(
                random_id(),
                self._parse_space(name_leaf.prefix),
                Markers.EMPTY, [], name_leaf.value, None, None,
            )
            value = self._convert_node(children[2])
            if value is None:
                return None
            return py.NamedArgument(
                random_id(), Space.EMPTY, Markers.EMPTY,
                name_ident,
                JLeftPadded(self._parse_space(children[1].prefix), value, Markers.EMPTY),
                None,
            )
        # Generator-expression argument: ``test (sync_)comp_for ...``.
        if (len(children) >= 2 and
                getattr(children[1], 'type', None) in ('comp_for', 'sync_comp_for')):
            result_expr = self._convert_node(children[0])
            if result_expr is None:
                return None
            return self._build_comprehension(
                py.ComprehensionExpression.Kind.GENERATOR,
                result_expr, children[1], Space.EMPTY, Space.EMPTY,
            )
        return None

    def _convert_testlist(self, node) -> Optional[j.J]:
        """Convert a ``testlist`` (comma-separated expressions).

        A multi-child testlist is a tuple (e.g. ``return a, b`` returns a
        tuple, ``a, b = 1, 2`` unpacks a tuple). The result is a
        :class:`py.CollectionLiteral` of kind ``TUPLE`` whose elements
        container carries the ``OmitParentheses`` marker — testlists in
        source never wear parentheses; tuple atoms ``(a, b)`` go through
        :meth:`_convert_paren_atom` and *do* keep them.
        """
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        items = self._pad_comma_list(node.children, Space.EMPTY)
        if items is None:
            return self._convert_generic(node)
        return py.CollectionLiteral(
            random_id(), Space.EMPTY, Markers.EMPTY,
            py.CollectionLiteral.Kind.TUPLE,
            JContainer(Space.EMPTY, items,
                       Markers.build(random_id(), [OmitParentheses(random_id())])),
            None,
        )

    def _convert_test(self, node) -> Optional[j.J]:
        """Convert a test expression.

        Handles the ternary form ``a if b else c`` (parso shape
        ``[a, 'if', b, 'else', c]``) into :class:`j.Ternary`. Note that
        :class:`j.Ternary` stores its condition first, then the
        true / false parts — but Python's source order is
        true-part / condition / false-part, so the printer reverses again.
        """
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        if (len(node.children) == 5
                and getattr(node.children[1], 'value', None) == 'if'
                and getattr(node.children[3], 'value', None) == 'else'):
            true_node, if_kw, cond_node, else_kw, false_node = node.children
            true_part = self._convert_node(true_node)
            condition = self._convert_node(cond_node)
            false_part = self._convert_node(false_node)
            if true_part is None or condition is None or false_part is None:
                return self._convert_generic(node)
            return j.Ternary(
                random_id(), Space.EMPTY, Markers.EMPTY,
                condition,
                JLeftPadded(self._parse_space(if_kw.prefix), true_part, Markers.EMPTY),
                JLeftPadded(self._parse_space(else_kw.prefix), false_part, Markers.EMPTY),
                None,
            )
        return self._convert_generic(node)

    def _convert_strings(self, node) -> Optional[j.J]:
        """Convert implicit string concatenation (``"a" "b"``).

        parso wraps adjacent string literals in a ``strings`` node. We
        emit a single :class:`j.Literal` whose ``value_source`` preserves
        the inter-piece whitespace verbatim so the printer reproduces the
        original layout.
        """
        pieces = [c for c in node.children if isinstance(c, parso_tree.PythonLeaf)
                  and c.type == 'string']
        if not pieces:
            return None
        outer_prefix = self._parse_space(pieces[0].prefix)
        value_source = pieces[0].value + ''.join(p.prefix + p.value for p in pieces[1:])
        markers = Markers.build(random_id(), [Quoted(random_id(), self._detect_quote_style(pieces[0].value))])
        return j.Literal(
            random_id(),
            outer_prefix,
            markers,
            value_source,
            value_source,
            None,
            JavaType.Primitive.String,
        )

    def _convert_or_test(self, node) -> Optional[j.J]:
        """Convert an or_test expression (``a or b or c``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        return self._fold_binary(node.children) or self._convert_generic(node)

    def _convert_and_test(self, node) -> Optional[j.J]:
        """Convert an and_test expression (``a and b and c``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        return self._fold_binary(node.children) or self._convert_generic(node)

    def _convert_not_test(self, node) -> Optional[j.J]:
        """Convert a not_test expression (``not x``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # not_test: 'not' not_test
        return self._convert_unary(node.children[0], node.children[1])

    def _convert_comparison(self, node) -> Optional[j.J]:
        """Convert a comparison expression (``a < b <= c``).

        Two-token Py2 operators (``not in``, ``is not``) are not yet handled;
        callers fall back to the generic placeholder for those.
        """
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        return self._fold_binary(node.children) or self._convert_generic(node)

    def _convert_arith_expr(self, node) -> Optional[j.J]:
        """Convert an arithmetic expression (``a + b - c``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        return self._fold_binary(node.children) or self._convert_generic(node)

    def _convert_term(self, node) -> Optional[j.J]:
        """Convert a term expression (``a * b / c % d``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        return self._fold_binary(node.children) or self._convert_generic(node)

    def _convert_factor(self, node) -> Optional[j.J]:
        """Convert a factor expression (``-x``, ``+x``, ``~x``)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # factor: ('+'|'-'|'~') factor
        return self._convert_unary(node.children[0], node.children[1])

    def _convert_lambdef(self, node) -> Optional[j.J]:
        """Convert ``lambda [params]: expr``.

        Grammar:
            lambdef: 'lambda' [varargslist] ':' test
        Parameter shapes mirror funcdef's ``param`` form (positional,
        default, ``*args``, ``**kw``).
        """
        children = list(node.children)
        if len(children) < 3:
            return self._convert_generic(node)
        lambda_kw = children[0]
        prefix = self._parse_space(lambda_kw.prefix)

        colon_idx = next((i for i, c in enumerate(children)
                          if getattr(c, 'value', None) == ':'), -1)
        if colon_idx < 1:
            return self._convert_generic(node)
        colon = children[colon_idx]
        body_node = children[colon_idx + 1] if colon_idx + 1 < len(children) else None
        if body_node is None:
            return self._convert_generic(node)

        param_nodes = children[1:colon_idx]
        params_padded = []
        if not param_nodes:
            empty = j.Empty(random_id(), Space.EMPTY, Markers.EMPTY)
            params_padded = [JRightPadded(empty, Space.EMPTY, Markers.EMPTY)]
        else:
            for i, p in enumerate(param_nodes):
                if getattr(p, 'value', None) == ',':
                    continue
                if getattr(p, 'type', None) == 'param':
                    converted = self._convert_param(p)
                    after = self._extract_param_trailing_space(p)
                else:
                    # Lone NAME leaf (parso sometimes emits this form in
                    # old_lambdef contexts).
                    if not hasattr(p, 'value'):
                        return self._convert_generic(node)
                    ident = j.Identifier(random_id(),
                                         self._parse_space(p.prefix),
                                         Markers.EMPTY, [], p.value, None, None)
                    named = j.VariableDeclarations.NamedVariable(
                        random_id(), Space.EMPTY, Markers.EMPTY, ident, [], None, None,
                    )
                    converted = j.VariableDeclarations(
                        random_id(), Space.EMPTY, Markers.EMPTY,
                        [], [], None, None, [],
                        [JRightPadded(named, Space.EMPTY, Markers.EMPTY)],
                    )
                    after = Space.EMPTY
                if converted is None:
                    return self._convert_generic(node)
                params_padded.append(JRightPadded(converted, after, Markers.EMPTY))

        body = self._convert_node(body_node)
        if body is None:
            return self._convert_generic(node)

        return j.Lambda(
            random_id(), prefix, Markers.EMPTY,
            j.Lambda.Parameters(random_id(), Space.EMPTY, Markers.EMPTY,
                                False, params_padded),
            self._parse_space(colon.prefix),  # arrow Space — for lambda it's the space before `:`
            body,
            None,  # type
        )

    def _convert_generic(self, node) -> Optional[j.J]:
        """Generic conversion for unhandled node types.

        Creates a placeholder identifier to avoid crashing.
        """
        prefix = Space.EMPTY
        if hasattr(node, 'children') and node.children:
            first_child = node.children[0]
            if hasattr(first_child, 'prefix'):
                prefix = self._parse_space(first_child.prefix)

        # Return a placeholder identifier
        node_type = node.type if hasattr(node, 'type') else type(node).__name__
        return j.Identifier(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],
            f"<{node_type}>",
            None,
            None
        )

    # --- Helper methods ---

    def _parse_space(self, text: str) -> Space:
        """Convert a parso prefix string into a :class:`Space`.

        parso embeds whitespace and ``# ...`` comments together in a
        single ``prefix`` string. We tokenize them so recipes can query
        :attr:`Space.comments` instead of grovelling through the raw
        whitespace; the printer's render order (whitespace, then each
        comment with its trailing ``suffix``) keeps round-trip output
        byte-identical.
        """
        if not text:
            return Space.EMPTY
        if '#' not in text:
            return Space([], text)

        comments: List[TextComment] = []
        prefix = ''
        i = 0
        n = len(text)
        while i < n:
            hash_idx = text.find('#', i)
            if hash_idx < 0:
                tail = text[i:]
                if comments and tail:
                    comments[-1] = comments[-1].replace(suffix=tail)
                break
            inter = text[i:hash_idx]
            if not comments:
                prefix = inter
            elif inter:
                comments[-1] = comments[-1].replace(suffix=inter)
            nl = text.find('\n', hash_idx + 1)
            end = n if nl < 0 else nl
            comments.append(TextComment(False, text[hash_idx + 1:end], '', Markers.EMPTY))
            i = end
        return Space(comments, prefix)

    def _pad_statement(self, stmt: j.J) -> JRightPadded:
        """Wrap a statement in JRightPadded."""
        return JRightPadded(stmt, Space.EMPTY, Markers.EMPTY)

    def _trailing_whitespace(self) -> Space:
        """Get the trailing whitespace (EOF space)."""
        # In parso, the endmarker has the trailing whitespace
        if hasattr(self._tree, 'children') and self._tree.children:
            last = self._tree.children[-1]
            if hasattr(last, 'type') and last.type == 'endmarker':
                return self._parse_space(last.prefix)
        return Space.EMPTY

    def _number_type(self, value: str) -> Optional[JavaType]:
        """Determine the type of a numeric literal."""
        value_lower = value.lower()
        if 'j' in value_lower:
            return None  # Complex number
        elif '.' in value or 'e' in value_lower:
            return JavaType.Primitive.Double
        elif value_lower.endswith('l'):
            return JavaType.Primitive.Long
        else:
            return JavaType.Primitive.Int

    # --- Expression folding helpers ---

    # parso augmented-assignment operator text -> j.AssignmentOperation.Type
    @staticmethod
    def _aug_assign_map():
        return {
            '+=':  j.AssignmentOperation.Type.Addition,
            '-=':  j.AssignmentOperation.Type.Subtraction,
            '*=':  j.AssignmentOperation.Type.Multiplication,
            '/=':  j.AssignmentOperation.Type.Division,
            '%=':  j.AssignmentOperation.Type.Modulo,
            '&=':  j.AssignmentOperation.Type.BitAnd,
            '|=':  j.AssignmentOperation.Type.BitOr,
            '^=':  j.AssignmentOperation.Type.BitXor,
            '<<=': j.AssignmentOperation.Type.LeftShift,
            '>>=': j.AssignmentOperation.Type.RightShift,
            '**=': j.AssignmentOperation.Type.Exponentiation,
            '//=': j.AssignmentOperation.Type.FloorDivision,
            '@=':  j.AssignmentOperation.Type.MatrixMultiplication,
        }

    # parso operator text -> (Binary class, Type enum value).
    # `j.Binary` is used when the operator maps directly to Java semantics;
    # `py.Binary` is used for Python-specific operators (FloorDivision, Power,
    # In/Is, MatrixMultiplication) that the printer renders differently.
    @staticmethod
    def _bin_op_map():
        return {
            '+':   (j.Binary, j.Binary.Type.Addition),
            '-':   (j.Binary, j.Binary.Type.Subtraction),
            '*':   (j.Binary, j.Binary.Type.Multiplication),
            '/':   (j.Binary, j.Binary.Type.Division),
            '%':   (j.Binary, j.Binary.Type.Modulo),
            '|':   (j.Binary, j.Binary.Type.BitOr),
            '&':   (j.Binary, j.Binary.Type.BitAnd),
            '^':   (j.Binary, j.Binary.Type.BitXor),
            '<<':  (j.Binary, j.Binary.Type.LeftShift),
            '>>':  (j.Binary, j.Binary.Type.RightShift),
            '<':   (j.Binary, j.Binary.Type.LessThan),
            '<=':  (j.Binary, j.Binary.Type.LessThanOrEqual),
            '>':   (j.Binary, j.Binary.Type.GreaterThan),
            '>=':  (j.Binary, j.Binary.Type.GreaterThanOrEqual),
            '==':  (j.Binary, j.Binary.Type.Equal),
            '!=':  (j.Binary, j.Binary.Type.NotEqual),
            # Py2 spelling of '!='. parso < 0.8 never emits '<>' as a
            # token (it pre-rewrites it to '!='), so this entry is
            # unreachable through the normal fold path; it is kept so
            # that a future parso upgrade — or source pre-processing
            # that injects a '<>' operator leaf directly — finds the
            # marker/printer wiring already in place.
            '<>':  (j.Binary, j.Binary.Type.NotEqual),
            'and': (j.Binary, j.Binary.Type.And),
            'or':  (j.Binary, j.Binary.Type.Or),
            '//':  (py.Binary, py.Binary.Type.FloorDivision),
            '**':  (py.Binary, py.Binary.Type.Power),
            '@':   (py.Binary, py.Binary.Type.MatrixMultiplication),
            'in':  (py.Binary, py.Binary.Type.In),
            'is':  (py.Binary, py.Binary.Type.Is),
        }

    # parso unary-operator text -> j.Unary.Type
    @staticmethod
    def _unary_op_map():
        return {
            '+':   j.Unary.Type.Positive,
            '-':   j.Unary.Type.Negative,
            '~':   j.Unary.Type.Complement,
            'not': j.Unary.Type.Not,
        }

    def _fold_binary(self, children) -> Optional[j.J]:
        """Fold parso's flat ``[lhs, op, rhs, op, rhs, ...]`` children into a
        left-associative ``j.Binary`` / ``py.Binary`` chain.

        Operators may be:
        * a single token (``+``, ``and``, ``==``, etc.)
        * a parso ``comp_op`` node carrying a two-token form
          (``not in``, ``is not``) — handled via the ``py.Binary`` negation
          slot.

        Returns ``None`` if any operator is unrecognized; the caller falls
        back to a generic placeholder rather than dropping operator info.
        """
        bin_map = self._bin_op_map()
        left = self._convert_node(children[0])
        if left is None:
            return None
        i = 1
        while i + 1 < len(children):
            op_leaf = children[i]
            rhs_node = children[i + 1]
            # Two-token operator wrapped in a `comp_op` node.
            if getattr(op_leaf, 'type', None) == 'comp_op':
                op_kids = op_leaf.children
                if len(op_kids) == 2:
                    first = op_kids[0].value
                    second = op_kids[1].value
                    if first == 'not' and second == 'in':
                        op_type = py.Binary.Type.NotIn
                    elif first == 'is' and second == 'not':
                        op_type = py.Binary.Type.IsNot
                    else:
                        return None
                    op_padded = JLeftPadded(
                        self._parse_space(op_kids[0].prefix),
                        op_type,
                        Markers.EMPTY,
                    )
                    negation = self._parse_space(op_kids[1].prefix)
                    rhs = self._convert_node(rhs_node)
                    if rhs is None:
                        return None
                    left = py.Binary(
                        random_id(), Space.EMPTY, Markers.EMPTY,
                        left, op_padded, negation, rhs, None,
                    )
                    i += 2
                    continue
                return None

            op_text = getattr(op_leaf, 'value', None)
            mapping = bin_map.get(op_text)
            if mapping is None:
                return None
            cls, op_type = mapping
            op_padded = JLeftPadded(
                self._parse_space(op_leaf.prefix),
                op_type,
                Markers.EMPTY,
            )
            rhs = self._convert_node(rhs_node)
            if rhs is None:
                return None
            node_markers = Markers.EMPTY
            if op_text == '<>':
                node_markers = Markers.build(random_id(), [LegacyNotEqual(random_id())])
            if cls is py.Binary:
                left = py.Binary(
                    random_id(),
                    Space.EMPTY,
                    node_markers,
                    left,
                    op_padded,
                    None,  # negation slot for `not in` / `is not` (unused here)
                    rhs,
                    None,  # type — left blank; type attribution happens later
                )
            else:
                left = j.Binary(
                    random_id(),
                    Space.EMPTY,
                    node_markers,
                    left,
                    op_padded,
                    rhs,
                    None,
                )
            i += 2
        return left

    def _convert_unary(self, op_leaf, operand_node) -> Optional[j.J]:
        """Build a ``j.Unary`` from a ``[op_leaf, operand]`` parso pair."""
        op_text = getattr(op_leaf, 'value', None)
        op_type = self._unary_op_map().get(op_text)
        if op_type is None:
            return None
        operand = self._convert_node(operand_node)
        if operand is None:
            return None
        return j.Unary(
            random_id(),
            self._parse_space(op_leaf.prefix),
            Markers.EMPTY,
            JLeftPadded(Space.EMPTY, op_type, Markers.EMPTY),
            operand,
            None,
        )

