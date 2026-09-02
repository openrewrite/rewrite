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

"""Placeholder replacement visitor for template substitution."""

from __future__ import annotations

from typing import Dict, List, Union

from rewrite.java import J
from rewrite.java import tree as j
from rewrite.java.support_types import JContainer, JRightPadded
from rewrite.python import tree as py
from rewrite.python.visitor import PythonVisitor
from .placeholder import from_placeholder
from .precedence import enclosing_tree, maybe_parenthesize


class PlaceholderReplacementVisitor(PythonVisitor[None]):
    """
    Visitor that replaces placeholder identifiers with actual values.

    This visitor traverses a template AST and replaces any identifiers
    that match the placeholder pattern (__plh_name__) with
    the corresponding captured values.

    When a substituted value binds more loosely than the slot it lands
    in demands, it is wrapped in parentheses to preserve semantics.
    """

    def __init__(self, values: Dict[str, Union[J, List[J]]]):
        """
        Initialize the replacement visitor.

        Args:
            values: Dict mapping capture names to their AST values.
                Variadic captures map to List[J].
        """
        super().__init__()
        self._values = values

    def visit_identifier(self, ident: j.Identifier, p: None) -> J:
        """
        Visit an identifier and replace if it's a placeholder.

        Args:
            ident: The identifier node.
            p: Visitor parameter (unused).

        Returns:
            The replacement value if this is a placeholder, otherwise the identifier.
        """
        name = ident.simple_name
        capture_name = from_placeholder(name)

        if capture_name is not None and capture_name in self._values:
            replacement = self._values[capture_name]

            # Preserve the placeholder's prefix (whitespace before)
            if hasattr(replacement, 'prefix'):
                replacement = replacement.replace(prefix=ident.prefix)

            # The cursor is still on the placeholder, so its parent owns the slot the value lands in
            parent = enclosing_tree(self.cursor.parent) if self.cursor is not None else None
            return maybe_parenthesize(parent, ident.id, replacement)

        # Not a placeholder or no value provided, continue normally
        return super().visit_identifier(ident, p)

    def visit_block(self, block: j.Block, p: None) -> J:
        """Override visit_block to unwrap ExpressionStatement around placeholders.

        When a template has a placeholder in statement position (e.g., the body
        of a ``with`` block), the parser wraps it as ``ExpressionStatement(Identifier(...))``
        since it looks like a bare expression.  If the replacement value is a
        non-Expression statement (``return``, ``if``, ``for``, etc.), we must
        substitute it directly into the statements list, bypassing the
        ``ExpressionStatement`` wrapper.  This mirrors the JS template engine's
        ``visitBlock`` logic.
        """
        # Substitute statement-position placeholders BEFORE the default
        # visitor runs, so visit_expression_statement never sees them.
        new_stmts: List[JRightPadded] = []
        changed = False
        for rp in block.padding.statements:
            stmt = rp.element
            if isinstance(stmt, py.ExpressionStatement):
                expr = stmt.expression
                if isinstance(expr, j.Identifier):
                    capture_name = from_placeholder(expr.simple_name)
                    if capture_name is not None and capture_name in self._values:
                        replacement = self._values[capture_name]
                        # Preserve the placeholder's whitespace prefix
                        if hasattr(replacement, '_prefix'):
                            replacement = replacement.replace(_prefix=expr.prefix)
                        new_stmts.append(rp.replace(element=replacement))
                        changed = True
                        continue
            new_stmts.append(rp)

        if changed:
            block = block.padding.replace(statements=new_stmts)

        return super().visit_block(block, p)

    def visit_method_invocation(self, method: j.MethodInvocation, p: None) -> J:
        """
        Visit a method invocation.

        This handles cases where the method name itself might be a placeholder,
        or where arguments contain placeholders.
        """
        # First, check if the method select (receiver) needs replacement
        method = method.replace(
            prefix=self.visit_space(method.prefix, p)
        )
        method = method.replace(markers=self.visit_markers(method.markers, p))

        # Visit select (receiver expression) — _select is JRightPadded
        if method.select is not None:
            new_select = self.visit_and_cast(method.select, type(method.select), p)
            if new_select is not method.select:
                padded_select = method.padding.select
                assert padded_select is not None
                method = method.padding.replace(
                    _select=padded_select.replace(element=new_select)
                )

        # Visit name
        new_name = self.visit_and_cast(method.name, j.Identifier, p)
        # Handle case where name was replaced with a non-identifier
        if isinstance(new_name, j.Identifier):
            method = method.replace(name=new_name)

        # Visit type parameters
        if method.type_parameters is not None:
            # Type parameters don't usually contain placeholders but handle anyway
            pass

        # Visit arguments — _arguments is JContainer[JRightPadded[Expression]]
        padded_args = method.padding.arguments
        if padded_args is not None:
            new_padded = []
            for rp in padded_args.padding.elements:
                elem = rp.element
                # Check if this argument is a variadic placeholder
                if isinstance(elem, j.Identifier):
                    cap_name = from_placeholder(elem.simple_name)
                    if cap_name is not None and cap_name in self._values:
                        value = self._values[cap_name]
                        if isinstance(value, list):
                            # Splice the list into the argument positions
                            for i, item in enumerate(value):
                                prefix = elem.prefix if i == 0 else j.Space([], ' ')
                                spliced = item.replace(prefix=prefix) if hasattr(item, 'prefix') else item
                                new_padded.append(JRightPadded(
                                    spliced, j.Space([], ''), j.Markers.EMPTY,
                                ))
                            continue
                # Scalar replacement (or non-placeholder) — visit normally
                new_elem = self.visit(elem, p)
                if new_elem is not None:
                    new_padded.append(rp.replace(element=new_elem))
            method = method.padding.replace(
                _arguments=JContainer(
                    padded_args.before, new_padded, padded_args.markers
                )
            )

        return method
