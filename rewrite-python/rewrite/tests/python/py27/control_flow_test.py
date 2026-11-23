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

"""Structural tests for Py2 control-flow constructs: if/elif/else, while, for.

Each test asserts the rich LST shape (no ``<grammar_node>`` placeholders),
including correct nesting for elif chains and the
:class:`py.TrailingElseWrapper` for loop ``else:`` clauses.
"""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor


def _stmt(source: str):
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()
    return cu.statements[0]


class TestIf:
    def test_simple_if(self):
        stmt = _stmt("if x:\n    pass\n")
        assert isinstance(stmt, j.If)
        assert isinstance(stmt.if_condition, j.ControlParentheses)
        # The condition wraps an Identifier 'x'.
        inner = stmt.if_condition.tree
        assert isinstance(inner, j.Identifier)
        assert inner.simple_name == "x"
        assert isinstance(stmt.then_part, j.Block)
        assert stmt.else_part is None

    def test_if_with_complex_condition(self):
        stmt = _stmt("if a and b:\n    pass\n")
        assert isinstance(stmt, j.If)
        inner = stmt.if_condition.tree
        assert isinstance(inner, j.Binary)
        assert inner.operator == j.Binary.Type.And

    def test_if_else(self):
        stmt = _stmt("if x:\n    a\nelse:\n    b\n")
        assert isinstance(stmt, j.If)
        assert stmt.else_part is not None
        assert isinstance(stmt.else_part, j.If.Else)
        assert isinstance(stmt.else_part.body, j.Block)

    def test_if_elif_else(self):
        stmt = _stmt("if x:\n    a\nelif y:\n    b\nelse:\n    c\n")
        assert isinstance(stmt, j.If)
        # outer if -> else -> body is a nested If
        outer_else = stmt.else_part
        assert outer_else is not None
        nested = outer_else.body
        assert isinstance(nested, j.If)
        # nested if's condition is 'y'
        assert nested.if_condition.tree.simple_name == "y"
        # nested if's else holds the final block
        assert nested.else_part is not None
        assert isinstance(nested.else_part.body, j.Block)

    def test_if_elif_elif_else(self):
        # Three-way elif chain. Each elif should nest one level deeper.
        stmt = _stmt(
            "if a:\n    pass\n"
            "elif b:\n    pass\n"
            "elif c:\n    pass\n"
            "else:\n    pass\n"
        )
        assert isinstance(stmt, j.If)
        level1 = stmt.else_part.body
        assert isinstance(level1, j.If)
        assert level1.if_condition.tree.simple_name == "b"
        level2 = level1.else_part.body
        assert isinstance(level2, j.If)
        assert level2.if_condition.tree.simple_name == "c"
        assert isinstance(level2.else_part.body, j.Block)

    def test_block_contains_converted_statements(self):
        # The body holds rich statements, not placeholders.
        stmt = _stmt("if x:\n    y = a + b\n")
        body = stmt.then_part
        inner = body.statements[0]
        assert isinstance(inner, j.Assignment)
        assert isinstance(inner.assignment, j.Binary)
        assert inner.assignment.operator == j.Binary.Type.Addition


class TestWhile:
    def test_simple_while(self):
        stmt = _stmt("while x:\n    pass\n")
        assert isinstance(stmt, j.WhileLoop)
        assert isinstance(stmt.condition, j.ControlParentheses)
        assert isinstance(stmt.body, j.Block)

    def test_while_with_else(self):
        stmt = _stmt("while x:\n    pass\nelse:\n    cleanup\n")
        assert isinstance(stmt, py.TrailingElseWrapper)
        assert isinstance(stmt.statement, j.WhileLoop)
        assert isinstance(stmt.else_block, j.Block)


class TestFor:
    def test_simple_for(self):
        stmt = _stmt("for i in xs:\n    pass\n")
        assert isinstance(stmt, j.ForEachLoop)
        # Control holds the loop variable + iterable.
        assert isinstance(stmt.control, j.ForEachLoop.Control)
        assert isinstance(stmt.body, j.Block)

    def test_for_with_iterable_expression(self):
        # Iterable is a method call, not just a name.
        stmt = _stmt("for item in items.values():\n    pass\n")
        assert isinstance(stmt, j.ForEachLoop)
        assert isinstance(stmt.control.iterable, j.MethodInvocation)
        assert stmt.control.iterable.name.simple_name == "values"

    def test_for_with_else(self):
        stmt = _stmt("for i in xs:\n    pass\nelse:\n    z\n")
        assert isinstance(stmt, py.TrailingElseWrapper)
        assert isinstance(stmt.statement, j.ForEachLoop)

    def test_tuple_target(self):
        # Tuple destructuring produces a TUPLE-kind CollectionLiteral as
        # the target.
        stmt = _stmt("for i, j in items:\n    pass\n")
        assert isinstance(stmt, j.ForEachLoop)
        target_holder = stmt.control.variable
        from rewrite.python import tree as py
        target = target_holder.expression if hasattr(target_holder, 'expression') else target_holder
        assert isinstance(target, py.CollectionLiteral)
        assert target.kind == py.CollectionLiteral.Kind.TUPLE


class TestNoPlaceholders:
    """Whole-tree regression: no ``<grammar_node>`` placeholders survive."""

    @pytest.mark.parametrize("src", [
        "if x:\n    pass\n",
        "if a == b:\n    y = 1\n",
        "if x:\n    a\nelse:\n    b\n",
        "if x:\n    a\nelif y:\n    b\nelse:\n    c\n",
        "while x < 10:\n    x += 1\n",
        "while True:\n    pass\nelse:\n    cleanup()\n",
        "for i in range(10):\n    print(i)\n",
        "for item in items.values():\n    process(item)\n",
    ])
    def test_no_placeholders(self, src):
        from tests.python.py27.expressions_test import _collect_placeholders
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        placeholders = list(_collect_placeholders(stmt))
        assert placeholders == [], f"unexpected placeholders in {src!r}: {placeholders}"
