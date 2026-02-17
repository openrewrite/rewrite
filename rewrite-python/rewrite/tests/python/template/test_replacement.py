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
from rewrite.java.support_types import Space
from rewrite.markers import Markers
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
