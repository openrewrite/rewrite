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

"""Tests for placeholder replacement visitor."""

from uuid import uuid4

from rewrite.java import tree as j
from rewrite.java.support_types import JLeftPadded, Space
from rewrite.markers import Markers
from rewrite.python import tree as py
from rewrite.python.template import capture
from rewrite.python.template.engine import TemplateEngine
from rewrite.python.template.replacement import PlaceholderReplacementVisitor


def _ident(name):
    return j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], name, None, None)


class TestPlaceholderReplacement:
    """Tests for PlaceholderReplacementVisitor with simple expressions."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_replace_single_placeholder(self):
        """Replace {x} with foo in a simple expression."""
        tree = TemplateEngine.get_template_tree("{x}", {'x': capture('x')})
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Identifier)
        assert result.simple_name == 'foo'

    def test_replace_preserves_prefix(self):
        """Whitespace from the placeholder's position should transfer to the replacement."""
        tree = TemplateEngine.get_template_tree("{x}", {'x': capture('x')})
        # The placeholder identifier has some prefix from parsing; the replacement
        # should adopt whatever prefix the placeholder had.
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Identifier)
        # The replacement should have the prefix from the placeholder, not its own
        assert result.prefix == tree.prefix

    def test_non_placeholder_identifiers_unchanged(self):
        """Non-placeholder identifiers should pass through unchanged."""
        tree = TemplateEngine.get_template_tree("regular_var", {})
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Identifier)
        assert result.simple_name == 'regular_var'

    def test_no_matching_value_leaves_placeholder(self):
        """A placeholder with no corresponding value stays as-is."""
        tree = TemplateEngine.get_template_tree("{x}", {'x': capture('x')})
        # Provide no values for 'x'
        visitor = PlaceholderReplacementVisitor({})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Identifier)
        assert result.simple_name == '__placeholder_x__'

    def test_replace_in_nested_expression(self):
        """Replace placeholder in a binary expression."""
        tree = TemplateEngine.get_template_tree("{x} + 1", {'x': capture('x')})
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.left, j.Identifier)
        assert result.left.simple_name == 'foo'


class TestMethodInvocationReplacement:
    """Tests for PlaceholderReplacementVisitor with method invocations."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_replace_placeholder_in_method_arguments(self):
        """Replace {x} in print({x}) -> print(foo)."""
        tree = TemplateEngine.get_template_tree("print({x})", {'x': capture('x')})
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.MethodInvocation)
        args = result.arguments
        assert len(args) == 1
        assert isinstance(args[0], j.Identifier)
        assert args[0].simple_name == 'foo'

    def test_replace_placeholder_in_method_select(self):
        """Replace {obj} in {obj}.method() -> myobj.method()."""
        tree = TemplateEngine.get_template_tree("{obj}.method()", {'obj': capture('obj')})
        visitor = PlaceholderReplacementVisitor({'obj': _ident('myobj')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.MethodInvocation)
        assert result.select is not None
        assert isinstance(result.select, j.Identifier)
        assert result.select.simple_name == 'myobj'

    def test_replace_multiple_arguments(self):
        """Replace multiple placeholders: func({a}, {b}) -> func(x, y)."""
        tree = TemplateEngine.get_template_tree(
            "func({a}, {b})", {'a': capture('a'), 'b': capture('b')}
        )
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('x'),
            'b': _ident('y'),
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.MethodInvocation)
        args = result.arguments
        assert len(args) == 2
        assert isinstance(args[0], j.Identifier)
        assert args[0].simple_name == 'x'
        assert isinstance(args[1], j.Identifier)
        assert args[1].simple_name == 'y'

    def test_method_with_no_placeholders_unchanged(self):
        """A method invocation with no placeholders should be unchanged."""
        tree = TemplateEngine.get_template_tree("print('hello')", {})
        visitor = PlaceholderReplacementVisitor({})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.MethodInvocation)
        assert result.name.simple_name == 'print'


def _make_binary(left, op, right):
    """Helper to construct a j.Binary node."""
    return j.Binary(
        uuid4(), Space.EMPTY, Markers.EMPTY,
        left,
        JLeftPadded(Space([], ' '), op, Markers.EMPTY),
        right,
        None,
    )


def _make_py_binary(left, op, right):
    """Helper to construct a py.Binary node."""
    return py.Binary(
        uuid4(), Space.EMPTY, Markers.EMPTY,
        left,
        JLeftPadded(Space([], ' '), op, Markers.EMPTY),
        None,   # negation
        right,
        None,   # type
    )


class TestAutoParenthesization:
    """Tests for automatic parenthesization of substituted operands."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_or_operand_in_and_gets_parens(self):
        """{a} and {b} with b=(x or y) should produce a and (x or y)."""
        tree = TemplateEngine.get_template_tree(
            "{a} and {b}", {'a': capture('a'), 'b': capture('b')}
        )
        or_expr = _make_binary(_ident('x'), j.Binary.Type.Or, _ident('y'))
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('a'),
            'b': or_expr,
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert result.operator == j.Binary.Type.And
        # Right operand should be wrapped in Parentheses
        assert isinstance(result.right, j.Parentheses)
        inner = result.right.tree
        assert isinstance(inner, j.Binary)
        assert inner.operator == j.Binary.Type.Or

    def test_and_operand_in_and_no_parens(self):
        """{a} and {b} with b=(x and y) should NOT add parens (same precedence)."""
        tree = TemplateEngine.get_template_tree(
            "{a} and {b}", {'a': capture('a'), 'b': capture('b')}
        )
        and_expr = _make_binary(_ident('x'), j.Binary.Type.And, _ident('y'))
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('a'),
            'b': and_expr,
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        # Right operand should NOT be wrapped (same precedence)
        assert isinstance(result.right, j.Binary)

    def test_or_operand_in_left_of_and_gets_parens(self):
        """{a} and {b} with a=(p or q) should produce (p or q) and b."""
        tree = TemplateEngine.get_template_tree(
            "{a} and {b}", {'a': capture('a'), 'b': capture('b')}
        )
        or_expr = _make_binary(_ident('p'), j.Binary.Type.Or, _ident('q'))
        visitor = PlaceholderReplacementVisitor({
            'a': or_expr,
            'b': _ident('b'),
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.left, j.Parentheses)

    def test_addition_in_multiplication_gets_parens(self):
        """{a} * {b} with b=(x + y) should produce a * (x + y)."""
        tree = TemplateEngine.get_template_tree(
            "{a} * {b}", {'a': capture('a'), 'b': capture('b')}
        )
        add_expr = _make_binary(_ident('x'), j.Binary.Type.Addition, _ident('y'))
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('a'),
            'b': add_expr,
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.right, j.Parentheses)

    def test_multiplication_in_addition_no_parens(self):
        """{a} + {b} with b=(x * y) should NOT add parens (higher prec)."""
        tree = TemplateEngine.get_template_tree(
            "{a} + {b}", {'a': capture('a'), 'b': capture('b')}
        )
        mul_expr = _make_binary(_ident('x'), j.Binary.Type.Multiplication, _ident('y'))
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('a'),
            'b': mul_expr,
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.right, j.Binary)  # No parens

    def test_identifier_operand_no_parens(self):
        """{a} and {b} with simple identifiers should NOT add parens."""
        tree = TemplateEngine.get_template_tree(
            "{a} and {b}", {'a': capture('a'), 'b': capture('b')}
        )
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('x'),
            'b': _ident('y'),
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.left, j.Identifier)
        assert isinstance(result.right, j.Identifier)

    def test_python_in_operand_in_or_no_parens(self):
        """{a} or {b} with b=(x in y) should NOT add parens (higher prec)."""
        tree = TemplateEngine.get_template_tree(
            "{a} or {b}", {'a': capture('a'), 'b': capture('b')}
        )
        in_expr = _make_py_binary(_ident('x'), py.Binary.Type.In, _ident('y'))
        visitor = PlaceholderReplacementVisitor({
            'a': _ident('a'),
            'b': in_expr,
        })
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Binary)
        assert isinstance(result.right, py.Binary)  # No parens


class TestNotAutoParenthesization:
    """Tests for auto-parenthesization under `not` operator."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_and_operand_under_not_gets_parens(self):
        """not {x} with x=(a and b) should produce not (a and b)."""
        tree = TemplateEngine.get_template_tree(
            "not {x}", {'x': capture('x')}
        )
        and_expr = _make_binary(_ident('a'), j.Binary.Type.And, _ident('b'))
        visitor = PlaceholderReplacementVisitor({'x': and_expr})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Unary)
        assert isinstance(result.expression, j.Parentheses)
        inner = result.expression.tree
        assert isinstance(inner, j.Binary)
        assert inner.operator == j.Binary.Type.And

    def test_or_operand_under_not_gets_parens(self):
        """not {x} with x=(a or b) should produce not (a or b)."""
        tree = TemplateEngine.get_template_tree(
            "not {x}", {'x': capture('x')}
        )
        or_expr = _make_binary(_ident('a'), j.Binary.Type.Or, _ident('b'))
        visitor = PlaceholderReplacementVisitor({'x': or_expr})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Unary)
        assert isinstance(result.expression, j.Parentheses)

    def test_identifier_under_not_no_parens(self):
        """not {x} with x=foo should NOT add parens."""
        tree = TemplateEngine.get_template_tree(
            "not {x}", {'x': capture('x')}
        )
        visitor = PlaceholderReplacementVisitor({'x': _ident('foo')})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Unary)
        assert isinstance(result.expression, j.Identifier)

    def test_comparison_under_not_no_parens(self):
        """not {x} with x=(a == b) should NOT add parens (comparisons have higher prec)."""
        tree = TemplateEngine.get_template_tree(
            "not {x}", {'x': capture('x')}
        )
        eq_expr = _make_binary(_ident('a'), j.Binary.Type.Equal, _ident('b'))
        visitor = PlaceholderReplacementVisitor({'x': eq_expr})
        result = visitor.visit(tree, None)

        assert isinstance(result, j.Unary)
        # Comparisons have precedence 4 which is >= 3 (not threshold), so no parens
        assert isinstance(result.expression, j.Binary)


class TestBlockStatementPlaceholder:
    """Tests for placeholder replacement in statement position inside blocks.

    When a template has {body} inside a block (e.g. ``if True:\\n    {body}``),
    the parser wraps it as ``ExpressionStatement(Identifier(placeholder))``.
    If the replacement is a non-Expression statement (Return, If, etc.),
    the visitor must unwrap the ExpressionStatement so the block directly
    contains the replacement statement.
    """

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_return_in_block_placeholder(self):
        """Substituting a Return for a block placeholder should unwrap ExpressionStatement."""
        tree = TemplateEngine.get_template_tree(
            "if True:\n    {body}", {'body': capture('body')}
        )
        return_stmt = j.Return(
            uuid4(), Space([], '    '), Markers.EMPTY, _ident('result')
        )
        visitor = PlaceholderReplacementVisitor({'body': return_stmt})
        result = visitor.visit(tree, None)

        # The result should be an If statement
        assert isinstance(result, j.If)
        # The then-part block should contain a Return, NOT an ExpressionStatement
        then_block = result.then_part
        assert isinstance(then_block, j.Block)
        stmts = then_block.statements
        assert len(stmts) == 1
        assert isinstance(stmts[0], j.Return), (
            f"Expected Return in block, got {type(stmts[0]).__name__}"
        )
