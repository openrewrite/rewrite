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

from typing import Dict, List, Optional, TYPE_CHECKING
from uuid import uuid4

from rewrite.java import J, Expression
from rewrite.java import tree as j
from rewrite.java.support_types import JContainer, JRightPadded
from rewrite.python import tree as py
from rewrite.python.visitor import PythonVisitor
from .placeholder import from_placeholder

if TYPE_CHECKING:
    from rewrite.visitor import Cursor


# Operator precedence for Python binary operators (higher number = higher precedence).
# Only operators relevant for precedence-sensitive substitution are listed.
_BINARY_PRECEDENCE: Dict[object, int] = {
    j.Binary.Type.Or: 1,
    j.Binary.Type.And: 2,
    # Comparisons (all same precedence)
    j.Binary.Type.Equal: 4,
    j.Binary.Type.NotEqual: 4,
    j.Binary.Type.LessThan: 4,
    j.Binary.Type.GreaterThan: 4,
    j.Binary.Type.LessThanOrEqual: 4,
    j.Binary.Type.GreaterThanOrEqual: 4,
    # Python-specific comparisons
    py.Binary.Type.In: 4,
    py.Binary.Type.NotIn: 4,
    py.Binary.Type.Is: 4,
    py.Binary.Type.IsNot: 4,
    # Bitwise
    j.Binary.Type.BitOr: 5,
    j.Binary.Type.BitXor: 6,
    j.Binary.Type.BitAnd: 7,
    # Shifts
    j.Binary.Type.LeftShift: 8,
    j.Binary.Type.RightShift: 8,
    # Arithmetic
    j.Binary.Type.Addition: 9,
    j.Binary.Type.Subtraction: 9,
    j.Binary.Type.Multiplication: 10,
    j.Binary.Type.Division: 10,
    j.Binary.Type.Modulo: 10,
    py.Binary.Type.FloorDivision: 10,
    py.Binary.Type.MatrixMultiplication: 10,
    py.Binary.Type.Power: 11,
}


def _get_precedence(expr: J) -> Optional[int]:
    """Return the precedence of an expression, or None if not a binary op."""
    if isinstance(expr, j.Binary):
        return _BINARY_PRECEDENCE.get(expr.operator)
    if isinstance(expr, py.Binary):
        return _BINARY_PRECEDENCE.get(expr.operator)
    return None


def _wrap_in_parens(expr: Expression) -> j.Parentheses:
    """Wrap an expression in parentheses, preserving its prefix on the outer node."""
    return j.Parentheses(
        _id=uuid4(),
        _prefix=expr.prefix,
        _markers=j.Markers.EMPTY,
        _tree=JRightPadded(
            expr.replace(_prefix=j.Space([], '')),
            j.Space([], ''),
            j.Markers.EMPTY,
        ),
    )


def _needs_parens_in_binary(child: J, parent_op_prec: int) -> bool:
    """Check if a child expression needs parentheses inside a binary with the given precedence."""
    child_prec = _get_precedence(child)
    if child_prec is None:
        return False
    return child_prec < parent_op_prec


def _needs_parens_under_not(child: J) -> bool:
    """Check if a child expression needs parentheses when placed under `not`."""
    # `not` binds tighter than `and` and `or`, so both need parens
    child_prec = _get_precedence(child)
    if child_prec is None:
        return False
    # `not` has precedence 3 (between `and` at 2 and comparisons at 4)
    return child_prec < 3


def maybe_parenthesize(result: J, cursor: 'Cursor') -> J:
    """Wrap *result* in parentheses when its precedence is lower than the
    surrounding context (the cursor's parent tree node).

    This mirrors ``ParenthesizeVisitor.maybeParenthesize`` in the Java
    ``JavaTemplate`` implementation.
    """
    if not isinstance(result, (j.Binary, py.Binary, j.Unary)):
        return result

    parent_cursor = cursor.parent_tree_cursor()
    parent = parent_cursor.value
    if not isinstance(parent, J):
        return result

    # Parent is a binary operator — check precedence
    if isinstance(parent, (j.Binary, py.Binary)):
        parent_prec = _get_precedence(parent)
        result_prec = _get_precedence(result)
        if parent_prec is not None and result_prec is not None and result_prec < parent_prec:
            return _wrap_in_parens(result)

    # Parent is `not` — or/and need parens underneath
    if isinstance(parent, j.Unary) and parent.operator == j.Unary.Type.Not:
        if _needs_parens_under_not(result):
            return _wrap_in_parens(result)

    return result


class PlaceholderReplacementVisitor(PythonVisitor[None]):
    """
    Visitor that replaces placeholder identifiers with actual values.

    This visitor traverses a template AST and replaces any identifiers
    that match the placeholder pattern (__placeholder_name__) with
    the corresponding captured values.

    When a substituted value has lower operator precedence than the
    surrounding context, it is automatically wrapped in parentheses
    to preserve semantics.
    """

    def __init__(self, values: Dict[str, J]):
        """
        Initialize the replacement visitor.

        Args:
            values: Dict mapping capture names to their AST values.
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

            return replacement

        # Not a placeholder or no value provided, continue normally
        return super().visit_identifier(ident, p)

    def visit_binary(self, binary: j.Binary, p: None) -> J:
        """Visit a Java Binary and auto-parenthesize substituted operands if needed."""
        binary = super().visit_binary(binary, p)
        parent_prec = _BINARY_PRECEDENCE.get(binary.operator)
        if parent_prec is None:
            return binary

        left = binary.left
        right = binary.right

        if _needs_parens_in_binary(left, parent_prec):
            left = _wrap_in_parens(left)
        if _needs_parens_in_binary(right, parent_prec):
            right = _wrap_in_parens(right)

        if left is not binary.left or right is not binary.right:
            binary = binary.replace(_left=left, _right=right)
        return binary

    def visit_python_binary(self, binary: py.Binary, p: None) -> J:
        """Visit a Python Binary and auto-parenthesize substituted operands if needed."""
        binary = super().visit_python_binary(binary, p)
        parent_prec = _BINARY_PRECEDENCE.get(binary.operator)
        if parent_prec is None:
            return binary

        left = binary.left
        right = binary.right

        if _needs_parens_in_binary(left, parent_prec):
            left = _wrap_in_parens(left)
        if _needs_parens_in_binary(right, parent_prec):
            right = _wrap_in_parens(right)

        if left is not binary.left or right is not binary.right:
            binary = binary.replace(_left=left, _right=right)
        return binary

    def visit_unary(self, unary: j.Unary, p: None) -> J:
        """Visit a Unary and auto-parenthesize substituted operand under `not` if needed."""
        unary = super().visit_unary(unary, p)
        if unary.operator == j.Unary.Type.Not:
            expr = unary.expression
            if _needs_parens_under_not(expr):
                unary = unary.replace(_expression=_wrap_in_parens(expr))
        return unary

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
                new_elem = self.visit(rp.element, p)
                if new_elem is not None:
                    new_padded.append(rp.replace(element=new_elem))
            method = method.padding.replace(
                _arguments=JContainer(
                    padded_args.before, new_padded, padded_args.markers
                )
            )

        return method


class VariadicExpansionVisitor(PythonVisitor[None]):
    """
    Visitor that expands variadic captures in containers.

    When a variadic capture matches multiple elements (like function arguments),
    this visitor handles expanding them into the appropriate container structure.
    """

    def __init__(
        self,
        values: Dict[str, J],
        variadic_values: Dict[str, List[J]]
    ):
        """
        Initialize the expansion visitor.

        Args:
            values: Dict mapping capture names to single AST values.
            variadic_values: Dict mapping capture names to lists of AST values.
        """
        super().__init__()
        self._values = values
        self._variadic_values = variadic_values

    # TODO: Implement variadic expansion for:
    # - Function arguments
    # - List/tuple/set elements
    # - Dict key-value pairs
    # - Block statements
