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
from typing import Optional, List
from uuid import UUID

import parso
from parso.python import tree as parso_tree

from rewrite import random_id, Markers
from rewrite.java import Space, JRightPadded, JLeftPadded, JContainer, JavaType
from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python.markers import PrintSyntax, ExecSyntax, Quoted


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
        statements = self._convert_module(self._tree)

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
            [],    # imports (TODO: extract from statements)
            statements,
            self._trailing_whitespace()
        )

        return cu

    def _convert_module(self, module: parso_tree.Module) -> List[JRightPadded]:
        """Convert a parso Module to a list of LST statements."""
        statements = []

        for child in module.children:
            stmt = self._convert_node(child)
            if stmt is not None:
                statements.append(self._pad_statement(stmt))

        # If empty, add an Empty element
        if not statements:
            statements.append(JRightPadded(
                j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                Space.EMPTY,
                Markers.EMPTY
            ))

        return statements

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
            'simple_stmt': self._convert_simple_stmt,
            'expr_stmt': self._convert_expr_stmt,
            'print_stmt': self._convert_print_stmt,
            'exec_stmt': self._convert_exec_stmt,
            'pass_stmt': self._convert_pass_stmt,
            'break_stmt': self._convert_break_stmt,
            'continue_stmt': self._convert_continue_stmt,
            'return_stmt': self._convert_return_stmt,
            'funcdef': self._convert_funcdef,
            'classdef': self._convert_classdef,
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
        leaf_type = leaf.type.upper()

        if leaf_type == 'NAME' or leaf_type == 'KEYWORD':
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

    def _convert_simple_stmt(self, node) -> Optional[j.J]:
        """Convert a simple_stmt node."""
        # simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
        for child in node.children:
            if hasattr(child, 'type') and child.type != 'NEWLINE' and child.type != 'SEMI':
                result = self._convert_node(child)
                if result:
                    return result
        return None

    def _convert_expr_stmt(self, node) -> Optional[j.J]:
        """Convert an expression statement."""
        if len(node.children) == 1:
            # Simple expression
            expr = self._convert_node(node.children[0])
            if expr:
                return py.ExpressionStatement(random_id(), expr)
        elif len(node.children) >= 3 and hasattr(node.children[1], 'value') and node.children[1].value == '=':
            # Assignment: lhs = rhs
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
        # TODO: Handle augmented assignments
        return self._convert_generic(node)

    def _convert_pass_stmt(self, node) -> j.Empty:
        """Convert pass statement."""
        prefix = self._parse_space(node.children[0].prefix)
        return j.Empty(random_id(), prefix, Markers.EMPTY)

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

    def _convert_funcdef(self, node) -> j.MethodDeclaration:
        """Convert function definition."""
        # TODO: Implement full function parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_method(prefix, "funcdef")

    def _convert_classdef(self, node) -> j.ClassDeclaration:
        """Convert class definition."""
        # TODO: Implement full class parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_class(prefix, "classdef")

    def _convert_if_stmt(self, node) -> j.If:
        """Convert if statement."""
        # TODO: Implement full if parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_if(prefix)

    def _convert_while_stmt(self, node) -> j.WhileLoop:
        """Convert while statement."""
        # TODO: Implement full while parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_while(prefix)

    def _convert_for_stmt(self, node) -> j.ForEachLoop:
        """Convert for statement."""
        # TODO: Implement full for parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_for(prefix)

    def _convert_try_stmt(self, node) -> j.Try:
        """Convert try statement."""
        # TODO: Implement full try parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_try(prefix)

    def _convert_with_stmt(self, node) -> j.Try:
        """Convert with statement (maps to Try with resources)."""
        # TODO: Implement full with parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_try(prefix)

    def _convert_import_name(self, node) -> j.Import:
        """Convert import statement."""
        # TODO: Implement full import parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_import(prefix)

    def _convert_import_from(self, node) -> j.Import:
        """Convert from-import statement."""
        # TODO: Implement full from-import parsing
        prefix = self._parse_space(node.children[0].prefix)
        return self._placeholder_import(prefix)

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
        """Convert an atom (basic expression)."""
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

        # TODO: Handle parenthesized expressions, list/dict literals, etc.
        return self._convert_generic(node)

    def _convert_power(self, node) -> Optional[j.J]:
        """Convert a power expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle ** operator, attribute access, function calls
        return self._convert_generic(node)

    def _convert_testlist(self, node) -> Optional[j.J]:
        """Convert a testlist (comma-separated expressions)."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle tuples
        return self._convert_generic(node)

    def _convert_test(self, node) -> Optional[j.J]:
        """Convert a test expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle ternary expressions
        return self._convert_generic(node)

    def _convert_or_test(self, node) -> Optional[j.J]:
        """Convert an or_test expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle 'or' operator
        return self._convert_generic(node)

    def _convert_and_test(self, node) -> Optional[j.J]:
        """Convert an and_test expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle 'and' operator
        return self._convert_generic(node)

    def _convert_not_test(self, node) -> Optional[j.J]:
        """Convert a not_test expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle 'not' operator
        return self._convert_generic(node)

    def _convert_comparison(self, node) -> Optional[j.J]:
        """Convert a comparison expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle comparison operators
        return self._convert_generic(node)

    def _convert_arith_expr(self, node) -> Optional[j.J]:
        """Convert an arithmetic expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle + and - operators
        return self._convert_generic(node)

    def _convert_term(self, node) -> Optional[j.J]:
        """Convert a term expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle *, /, //, % operators
        return self._convert_generic(node)

    def _convert_factor(self, node) -> Optional[j.J]:
        """Convert a factor expression."""
        if len(node.children) == 1:
            return self._convert_node(node.children[0])
        # TODO: Handle unary +, -, ~ operators
        return self._convert_generic(node)

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
        """Convert whitespace/comment string to Space object.

        Parso includes whitespace and comments in the 'prefix' attribute of nodes.
        """
        if not text:
            return Space.EMPTY

        # TODO: Parse comments from the whitespace
        # For now, just return the whitespace
        return Space([], text)

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

    # --- Placeholder methods for complex constructs ---

    def _placeholder_method(self, prefix: Space, name: str) -> j.MethodDeclaration:
        """Create a placeholder method declaration."""
        return j.MethodDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],  # leading_annotations
            [],  # modifiers
            None,  # type_parameters
            None,  # return_type_expression
            [],  # name_annotations
            j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], name, None, None),
            JContainer(Space.EMPTY, [], Markers.EMPTY),  # parameters
            None,  # throws
            j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                    JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
            None,  # default_value
            None   # method_type
        )

    def _placeholder_class(self, prefix: Space, name: str) -> j.ClassDeclaration:
        """Create a placeholder class declaration."""
        return j.ClassDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],  # leading_annotations
            [],  # modifiers
            j.ClassDeclaration.Kind([], random_id(), Space.EMPTY, Markers.EMPTY,
                                     j.ClassDeclaration.Kind.Type.Class),
            j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], name, None, None),
            None,  # type_parameters
            None,  # primary_constructor
            None,  # extends
            None,  # implements
            None,  # permits
            j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                    JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
            None   # class_type
        )

    def _placeholder_if(self, prefix: Space) -> j.If:
        """Create a placeholder if statement."""
        return j.If(
            random_id(),
            prefix,
            Markers.EMPTY,
            j.ControlParentheses(
                random_id(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(
                    j.Literal(random_id(), Space.EMPTY, Markers.EMPTY, True, "True", None, None),
                    Space.EMPTY, Markers.EMPTY
                )
            ),
            JRightPadded(
                j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                        JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
                Space.EMPTY, Markers.EMPTY
            ),
            None  # else
        )

    def _placeholder_while(self, prefix: Space) -> j.WhileLoop:
        """Create a placeholder while loop."""
        return j.WhileLoop(
            random_id(),
            prefix,
            Markers.EMPTY,
            j.ControlParentheses(
                random_id(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(
                    j.Literal(random_id(), Space.EMPTY, Markers.EMPTY, True, "True", None, None),
                    Space.EMPTY, Markers.EMPTY
                )
            ),
            JRightPadded(
                j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                        JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
                Space.EMPTY, Markers.EMPTY
            )
        )

    def _placeholder_for(self, prefix: Space) -> j.ForEachLoop:
        """Create a placeholder for loop."""
        return j.ForEachLoop(
            random_id(),
            prefix,
            Markers.EMPTY,
            j.ForEachLoop.Control(
                random_id(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(
                    j.VariableDeclarations(
                        random_id(), Space.EMPTY, Markers.EMPTY, [], [], None, None, [],
                        [JRightPadded(
                            j.VariableDeclarations.NamedVariable(
                                random_id(), Space.EMPTY, Markers.EMPTY,
                                j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "_", None, None),
                                [], None, None
                            ),
                            Space.EMPTY, Markers.EMPTY
                        )]
                    ),
                    Space.EMPTY, Markers.EMPTY
                ),
                JRightPadded(
                    j.Literal(random_id(), Space.EMPTY, Markers.EMPTY, [], "[]", None, None),
                    Space.EMPTY, Markers.EMPTY
                )
            ),
            JRightPadded(
                j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                        JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
                Space.EMPTY, Markers.EMPTY
            )
        )

    def _placeholder_try(self, prefix: Space) -> j.Try:
        """Create a placeholder try statement."""
        return j.Try(
            random_id(),
            prefix,
            Markers.EMPTY,
            None,  # resources
            j.Block(random_id(), Space.EMPTY, Markers.EMPTY,
                    JRightPadded(False, Space.EMPTY, Markers.EMPTY), [], Space.EMPTY),
            [],    # catches
            None   # finally
        )

    def _placeholder_import(self, prefix: Space) -> j.Import:
        """Create a placeholder import."""
        return j.Import(
            random_id(),
            prefix,
            Markers.EMPTY,
            JLeftPadded(Space.EMPTY, False, Markers.EMPTY),  # static
            j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "placeholder", None, None),
            None  # alias
        )
