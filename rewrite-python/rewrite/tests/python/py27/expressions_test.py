# Copyright 2026 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Structural tests for the Py2ParserVisitor expression layer.

These bypass the RPC pipeline and exercise the visitor directly, asserting
that the LST contains rich ``j.Binary`` / ``j.Unary`` nodes rather than the
fallback ``<arith_expr>`` / ``<term>`` placeholder identifiers that signal
an unimplemented production.
"""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor


def _first_stmt(source: str):
    """Parse a single-statement source and return the first statement (unwrapped)."""
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()
    return cu.statements[0]


def _first_expr(source: str):
    """Parse a single-statement source and return the first expression/assignment."""
    stmt = _first_stmt(source)
    if isinstance(stmt, py.ExpressionStatement):
        return stmt.expression
    if isinstance(stmt, j.Assignment):
        return stmt.assignment
    return stmt


def _collect_placeholders(node):
    """Walk the LST and yield identifier names that look like ``<grammar_node>`` —
    the fallback shape emitted by ``_convert_generic`` when a production has
    no real mapping yet."""
    if isinstance(node, j.Identifier) and node.simple_name.startswith("<") and node.simple_name.endswith(">"):
        yield node.simple_name
    for attr in ("left", "right", "operand", "expression"):
        child = getattr(node, attr, None)
        if child is not None:
            yield from _collect_placeholders(child)
    op = getattr(node, "operator", None)
    if op is not None and hasattr(op, "element"):
        pass  # operator type, not a tree node
    # JLeftPadded carrying expressions
    for attr in ("assignment",):
        wrapped = getattr(node, attr, None)
        if wrapped is not None and hasattr(wrapped, "element"):
            yield from _collect_placeholders(wrapped.element)


class TestArithmetic:
    def test_simple_addition(self):
        expr = _first_expr("x = 1 + 2\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.Addition
        assert isinstance(expr.left, j.Literal)
        assert expr.left.value_source == "1"
        assert isinstance(expr.right, j.Literal)
        assert expr.right.value_source == "2"

    def test_left_associative_fold(self):
        # 1 + 2 + 3 should fold as ((1 + 2) + 3)
        expr = _first_expr("x = 1 + 2 + 3\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.Addition
        assert isinstance(expr.right, j.Literal)
        assert expr.right.value_source == "3"
        assert isinstance(expr.left, j.Binary)
        assert expr.left.operator == j.Binary.Type.Addition

    def test_mul_div_mod(self):
        for src, op_type in [
            ("x = a * b\n", j.Binary.Type.Multiplication),
            ("x = a / b\n", j.Binary.Type.Division),
            ("x = a % b\n", j.Binary.Type.Modulo),
        ]:
            expr = _first_expr(src)
            assert isinstance(expr, j.Binary), src
            assert expr.operator == op_type, src

    def test_floor_division_uses_py_binary(self):
        expr = _first_expr("x = a // b\n")
        assert isinstance(expr, py.Binary)
        assert expr.operator == py.Binary.Type.FloorDivision

    def test_power_uses_py_binary(self):
        expr = _first_expr("x = 2 ** 3\n")
        assert isinstance(expr, py.Binary)
        assert expr.operator == py.Binary.Type.Power


class TestComparison:
    @pytest.mark.parametrize("src,op_type", [
        ("x = a == b\n", j.Binary.Type.Equal),
        ("x = a != b\n", j.Binary.Type.NotEqual),
        ("x = a < b\n", j.Binary.Type.LessThan),
        ("x = a <= b\n", j.Binary.Type.LessThanOrEqual),
        ("x = a > b\n", j.Binary.Type.GreaterThan),
        ("x = a >= b\n", j.Binary.Type.GreaterThanOrEqual),
    ])
    def test_simple_comparison(self, src, op_type):
        expr = _first_expr(src)
        assert isinstance(expr, j.Binary)
        assert expr.operator == op_type


class TestBoolean:
    def test_and(self):
        expr = _first_expr("x = a and b\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.And

    def test_or(self):
        expr = _first_expr("x = a or b\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.Or

    def test_and_or_chain(self):
        # `a and b or c` parses as `(a and b) or c` (Python precedence:
        # `and` binds tighter than `or`).
        expr = _first_expr("x = a and b or c\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.Or
        assert isinstance(expr.left, j.Binary)
        assert expr.left.operator == j.Binary.Type.And


class TestUnary:
    @pytest.mark.parametrize("src,op_type", [
        ("x = -a\n", j.Unary.Type.Negative),
        ("x = +a\n", j.Unary.Type.Positive),
        ("x = ~a\n", j.Unary.Type.Complement),
        ("x = not a\n", j.Unary.Type.Not),
    ])
    def test_unary(self, src, op_type):
        expr = _first_expr(src)
        assert isinstance(expr, j.Unary), src
        assert expr.operator == op_type

    def test_double_negation(self):
        expr = _first_expr("x = --a\n")
        assert isinstance(expr, j.Unary)
        assert expr.operator == j.Unary.Type.Negative
        assert isinstance(expr.expression, j.Unary)
        assert expr.expression.operator == j.Unary.Type.Negative


class TestNoPlaceholdersLeak:
    """Regression tests: any LST produced by these expressions must not contain
    a `<grammar_node>` placeholder identifier."""

    @pytest.mark.parametrize("src", [
        "x = 1 + 2 * 3\n",
        "x = (a - b) * c\n",  # Note: parens still placeholder, will fail until atom is wired
        "x = a == b and c != d\n",
        "x = not (a or b)\n",
        "x = -a + b\n",
    ])
    def test_no_placeholders(self, src):
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        placeholders = list(_collect_placeholders(stmt))
        # Filter to non-atom placeholders for now — parenthesized atoms
        # are not yet wired.
        non_atom = [p for p in placeholders if p != "<atom>"]
        assert non_atom == [], f"unexpected placeholders in {src!r}: {placeholders}"


class TestCalls:
    def test_simple_call_no_args(self):
        expr = _first_expr("x = f()\n")
        assert isinstance(expr, j.MethodInvocation)
        assert isinstance(expr.name, j.Identifier)
        assert expr.name.simple_name == "f"
        assert expr.select is None

    def test_simple_call_one_arg(self):
        expr = _first_expr("x = f(a)\n")
        assert isinstance(expr, j.MethodInvocation)
        assert expr.name.simple_name == "f"
        assert len(expr.arguments) == 1
        assert isinstance(expr.arguments[0], j.Identifier)
        assert expr.arguments[0].simple_name == "a"

    def test_simple_call_multiple_args(self):
        expr = _first_expr("x = f(a, b, c)\n")
        assert isinstance(expr, j.MethodInvocation)
        names = [a.simple_name for a in expr.arguments if isinstance(a, j.Identifier)]
        assert names == ["a", "b", "c"]

    def test_method_call(self):
        expr = _first_expr("x = obj.method(arg)\n")
        assert isinstance(expr, j.MethodInvocation)
        assert expr.name.simple_name == "method"
        assert expr.select is not None
        assert isinstance(expr.select, j.Identifier)
        assert expr.select.simple_name == "obj"
        assert len(expr.arguments) == 1

    def test_chained_method_call(self):
        # obj.foo().bar() — call result is the receiver of another call.
        expr = _first_expr("x = obj.foo().bar()\n")
        assert isinstance(expr, j.MethodInvocation)
        assert expr.name.simple_name == "bar"
        inner = expr.select
        assert isinstance(inner, j.MethodInvocation)
        assert inner.name.simple_name == "foo"

    def test_call_arg_is_expression(self):
        expr = _first_expr("x = f(a + 1)\n")
        assert isinstance(expr, j.MethodInvocation)
        assert isinstance(expr.arguments[0], j.Binary)
        assert expr.arguments[0].operator == j.Binary.Type.Addition


class TestAttributeAndSubscript:
    def test_attribute_chain(self):
        expr = _first_expr("x = a.b.c\n")
        assert isinstance(expr, j.FieldAccess)
        assert expr.name.simple_name == "c"
        assert isinstance(expr.target, j.FieldAccess)
        assert expr.target.name.simple_name == "b"
        assert isinstance(expr.target.target, j.Identifier)
        assert expr.target.target.simple_name == "a"

    def test_subscript(self):
        expr = _first_expr("x = a[0]\n")
        assert isinstance(expr, j.ArrayAccess)
        assert isinstance(expr.indexed, j.Identifier)
        assert expr.indexed.simple_name == "a"

    def test_attribute_then_subscript(self):
        expr = _first_expr("x = obj.data[i]\n")
        assert isinstance(expr, j.ArrayAccess)
        assert isinstance(expr.indexed, j.FieldAccess)
        assert expr.indexed.name.simple_name == "data"


class TestParens:
    def test_paren_preserves_inner(self):
        expr = _first_expr("x = (a + b)\n")
        assert isinstance(expr, j.Parentheses)
        inner = expr.tree
        assert isinstance(inner, j.Binary)
        assert inner.operator == j.Binary.Type.Addition

    def test_paren_changes_precedence(self):
        # `(a + b) * c` — the parens make the addition the LHS of multiplication.
        expr = _first_expr("x = (a + b) * c\n")
        assert isinstance(expr, j.Binary)
        assert expr.operator == j.Binary.Type.Multiplication
        assert isinstance(expr.left, j.Parentheses)
        inner = expr.left.tree
        assert isinstance(inner, j.Binary)
        assert inner.operator == j.Binary.Type.Addition


class TestAugmentedAssignment:
    @pytest.mark.parametrize("src,op_type", [
        ("x += 1\n",   j.AssignmentOperation.Type.Addition),
        ("x -= 1\n",   j.AssignmentOperation.Type.Subtraction),
        ("x *= 2\n",   j.AssignmentOperation.Type.Multiplication),
        ("x /= 2\n",   j.AssignmentOperation.Type.Division),
        ("x //= 2\n",  j.AssignmentOperation.Type.FloorDivision),
        ("x **= 2\n",  j.AssignmentOperation.Type.Exponentiation),
        ("x %= 2\n",   j.AssignmentOperation.Type.Modulo),
        ("x &= 1\n",   j.AssignmentOperation.Type.BitAnd),
        ("x |= 1\n",   j.AssignmentOperation.Type.BitOr),
        ("x ^= 1\n",   j.AssignmentOperation.Type.BitXor),
        ("x <<= 1\n",  j.AssignmentOperation.Type.LeftShift),
        ("x >>= 1\n",  j.AssignmentOperation.Type.RightShift),
    ])
    def test_augmented(self, src, op_type):
        stmt = _first_stmt(src)
        assert isinstance(stmt, j.AssignmentOperation), src
        assert stmt.operator == op_type


class TestExpressionRegressions:
    """No placeholder identifiers should leak through these constructs."""

    @pytest.mark.parametrize("src", [
        "x = f(a, b)\n",
        "x = obj.method()\n",
        "x = a.b.c\n",
        "x = arr[i]\n",
        "x = 2 ** 3 + 1\n",
        "x = (a + b) * c\n",
        "x += 1\n",
        "x = f(a + b, c * d)\n",
    ])
    def test_no_placeholders(self, src):
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        placeholders = list(_collect_placeholders(stmt))
        assert placeholders == [], f"unexpected placeholders in {src!r}: {placeholders}"
