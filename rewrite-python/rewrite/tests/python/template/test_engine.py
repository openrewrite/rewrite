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

"""Tests for template engine."""

import pytest

from rewrite.java import tree as j
from rewrite.python.template import capture, TemplateEngine
from rewrite.python.template.engine import TemplateOptions


class TestTemplateEngine:
    """Tests for TemplateEngine class."""

    def test_parse_simple_expression(self):
        """Test parsing a simple expression template."""
        captures = {}
        tree = TemplateEngine.get_template_tree("x + 1", captures)

        # Should be a Binary expression
        assert isinstance(tree, j.Binary)

    def test_parse_method_call(self):
        """Test parsing a method call template."""
        captures = {}
        tree = TemplateEngine.get_template_tree("print('hello')", captures)

        # Should be a MethodInvocation
        assert isinstance(tree, j.MethodInvocation)

    def test_parse_with_placeholder(self):
        """Test parsing a template with placeholder."""
        captures = {'x': capture('x')}
        tree = TemplateEngine.get_template_tree("print({x})", captures)

        # Should be a MethodInvocation
        assert isinstance(tree, j.MethodInvocation)

        # Argument should be an Identifier with placeholder name
        # Note: tree.arguments returns unwrapped List[Expression]
        args = tree.arguments
        assert len(args) == 1
        arg = args[0]
        assert isinstance(arg, j.Identifier)
        assert arg.simple_name == '__placeholder_x__'

    def test_parse_statement(self):
        """Test parsing a statement template."""
        captures = {}
        tree = TemplateEngine.get_template_tree("return 42", captures)

        # Should be a Return statement
        assert isinstance(tree, j.Return)

    def test_caching(self):
        """Test that templates are cached."""
        captures = {'x': capture('x')}

        # First call
        tree1 = TemplateEngine.get_template_tree("print({x})", captures)

        # Second call should return same object (from cache)
        tree2 = TemplateEngine.get_template_tree("print({x})", captures)

        assert tree1 is tree2

    def test_clear_cache(self):
        """Test clearing the template cache."""
        captures = {}
        TemplateEngine.get_template_tree("x + 1", captures)

        TemplateEngine.clear_cache()

        # After clearing, cache should be empty
        # (we can't directly check the cache, but this shouldn't raise)
        TemplateEngine.get_template_tree("x + 1", captures)

    def test_is_expression(self):
        """Test expression detection."""
        assert TemplateEngine._is_expression("x + 1") is True
        assert TemplateEngine._is_expression("print()") is True
        assert TemplateEngine._is_expression("return 1") is False
        assert TemplateEngine._is_expression("if x: pass") is False

    def test_generate_wrapper_expression(self):
        """Test wrapper generation for expression."""
        from rewrite.python.template.engine import TemplateOptions
        wrapper = TemplateEngine._generate_wrapper("x + 1", TemplateOptions())

        assert "def __WRAPPER__():" in wrapper
        assert "return x + 1" in wrapper

    def test_generate_wrapper_statement(self):
        """Test wrapper generation for statement."""
        from rewrite.python.template.engine import TemplateOptions
        wrapper = TemplateEngine._generate_wrapper("return 42", TemplateOptions())

        assert "def __WRAPPER__():" in wrapper
        # Statement should be indented, not have return
        assert "return 42" in wrapper


class TestTemplateEngineErrors:
    """Tests for error handling in TemplateEngine."""

    def test_syntax_error_raises(self):
        """Test that syntax errors are raised."""
        captures = {}
        with pytest.raises(SyntaxError):
            TemplateEngine.get_template_tree("def print(", captures)

    def test_undefined_placeholder_raises(self):
        """Test that undefined placeholders raise errors."""
        captures = {'x': capture('x')}
        with pytest.raises(ValueError, match="no corresponding capture"):
            TemplateEngine.get_template_tree("print({y})", captures)


class TestTemplateEngineExtraTypes:
    """Tests for parsing various expression and statement types."""

    def test_parse_assignment(self):
        """Test parsing an assignment template."""
        tree = TemplateEngine.get_template_tree("x = 1", {})
        assert isinstance(tree, j.Assignment)

    def test_parse_identifier(self):
        """Test parsing a bare identifier."""
        tree = TemplateEngine.get_template_tree("foo", {})
        assert isinstance(tree, j.Identifier)

    def test_parse_literal_string(self):
        """Test parsing a string literal."""
        tree = TemplateEngine.get_template_tree("'hello'", {})
        assert isinstance(tree, j.Literal)

    def test_parse_literal_int(self):
        """Test parsing an integer literal."""
        tree = TemplateEngine.get_template_tree("42", {})
        assert isinstance(tree, j.Literal)

    def test_parse_field_access(self):
        """Test parsing a field access."""
        tree = TemplateEngine.get_template_tree("a.b", {})
        assert isinstance(tree, j.FieldAccess)

    def test_parse_unary(self):
        """Test parsing a unary expression."""
        tree = TemplateEngine.get_template_tree("-x", {})
        assert isinstance(tree, j.Unary)

    def test_parse_parenthesized(self):
        """Test parsing a parenthesized expression."""
        tree = TemplateEngine.get_template_tree("(x + 1)", {})
        assert isinstance(tree, j.Parentheses)

    def test_parse_return_with_placeholder(self):
        """Test parsing a return statement with a placeholder."""
        captures = {'x': capture('x')}
        tree = TemplateEngine.get_template_tree("return {x}", captures)
        assert isinstance(tree, j.Return)
        assert isinstance(tree.expression, j.Identifier)
        assert tree.expression.simple_name == '__placeholder_x__'

    def test_parse_multiple_placeholders(self):
        """Test parsing an expression with multiple placeholders."""
        captures = {'a': capture('a'), 'b': capture('b')}
        tree = TemplateEngine.get_template_tree("{a} + {b}", captures)
        assert isinstance(tree, j.Binary)
        assert isinstance(tree.left, j.Identifier)
        assert tree.left.simple_name == '__placeholder_a__'
        assert isinstance(tree.right, j.Identifier)
        assert tree.right.simple_name == '__placeholder_b__'


class TestEngineEdgeCases:
    """Tests for edge cases in TemplateEngine."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_cache_key_varies_by_captures(self):
        """Test that different captures produce different cache keys."""
        key1 = TemplateEngine._make_cache_key("x", {'a': capture('a')}, TemplateOptions())
        key2 = TemplateEngine._make_cache_key("x", {'b': capture('b')}, TemplateOptions())
        assert key1 != key2

    def test_cache_key_varies_by_imports(self):
        """Test that different imports produce different cache keys."""
        key1 = TemplateEngine._make_cache_key("x", {}, TemplateOptions(imports=("import os",)))
        key2 = TemplateEngine._make_cache_key("x", {}, TemplateOptions(imports=("import sys",)))
        assert key1 != key2

    def test_wrapper_with_imports(self):
        """Test that wrapper generation includes imports."""
        wrapper = TemplateEngine._generate_wrapper("x", TemplateOptions(imports=("import os",)))
        assert "import os" in wrapper

    def test_indented_template_dedented(self):
        """Test that indented template code is dedented before parsing."""
        tree1 = TemplateEngine.get_template_tree("x + 1", {})
        TemplateEngine.clear_cache()
        tree2 = TemplateEngine.get_template_tree("    x + 1", {})
        assert type(tree1) == type(tree2)
        assert isinstance(tree1, j.Binary)
        assert isinstance(tree2, j.Binary)

    def test_empty_captures_dict(self):
        """Test that an empty captures dict works fine."""
        tree = TemplateEngine.get_template_tree("x + 1", {})
        assert isinstance(tree, j.Binary)


class TestAutoFormatIntegration:
    """Tests for auto-format integration via rewrite_run."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def test_template_apply_formats_operator_spacing(self):
        """Template replacement normalizes operator spacing through the full pipeline."""
        from rewrite import ExecutionContext
        from rewrite.java import J
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import template, pattern, capture
        from rewrite.test import RecipeSpec, python, from_visitor

        expr = capture('expr')
        pat = pattern("{expr} + 1", expr=expr)
        # Template intentionally has no spaces around +
        tmpl = template("{expr}+2", expr=expr)

        class ReplaceVisitor(PythonVisitor[ExecutionContext]):
            def visit_binary(self, binary: j.Binary, p: ExecutionContext) -> J:
                b = super().visit_binary(binary, p)
                match = pat.match(b, self._cursor)
                if match:
                    return tmpl.apply(self._cursor, values=match)
                return b

        spec = RecipeSpec(recipe=from_visitor(ReplaceVisitor()))
        spec.rewrite_run(
            python(
                "x = a + 1\n",
                "x = a + 2\n",
            )
        )

    def test_template_apply_formats_method_call(self):
        """Template replacement of a method call gets properly formatted."""
        from rewrite import ExecutionContext
        from rewrite.java import J
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import template, pattern, capture
        from rewrite.test import RecipeSpec, python, from_visitor

        msg = capture('msg')
        pat = pattern("print({msg})", msg=msg)
        tmpl = template("print('prefix',{msg})", msg=msg)

        class AddPrefixVisitor(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, method: j.MethodInvocation, p: ExecutionContext) -> J:
                m = super().visit_method_invocation(method, p)
                match = pat.match(m, self._cursor)
                if match:
                    return tmpl.apply(self._cursor, values=match)
                return m

        spec = RecipeSpec(recipe=from_visitor(AddPrefixVisitor()))
        spec.rewrite_run(
            python(
                "print('hello')\n",
                "print('prefix', 'hello')\n",
            )
        )

    def test_template_formats_in_nested_context(self):
        """Formatting respects indentation depth when template is applied in nested scope."""
        from rewrite import ExecutionContext
        from rewrite.java import J
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import template, pattern, capture
        from rewrite.test import RecipeSpec, python, from_visitor

        expr = capture('expr')
        pat = pattern("{expr} + 1", expr=expr)
        tmpl = template("{expr}+2", expr=expr)

        class ReplaceVisitor(PythonVisitor[ExecutionContext]):
            def visit_binary(self, binary: j.Binary, p: ExecutionContext) -> J:
                b = super().visit_binary(binary, p)
                match = pat.match(b, self._cursor)
                if match:
                    return tmpl.apply(self._cursor, values=match)
                return b

        spec = RecipeSpec(recipe=from_visitor(ReplaceVisitor()))
        spec.rewrite_run(
            python(
                """
                class Foo:
                    def bar(self):
                        if True:
                            x = a + 1
                            y = b + 1
                        return x
                """,
                """
                class Foo:
                    def bar(self):
                        if True:
                            x = a + 2
                            y = b + 2
                        return x
                """,
            )
        )

    def test_apply_coordinates_no_cu_context_does_not_crash(self):
        """_apply_coordinates with cursor lacking CU context doesn't crash."""
        from rewrite.visitor import Cursor
        from rewrite.python.template.coordinates import PythonCoordinates

        original = TemplateEngine.get_template_tree("x + 1", {})
        TemplateEngine.clear_cache()
        result = TemplateEngine.get_template_tree("y + 2", {})

        coordinates = PythonCoordinates.replace(original)
        cursor = Cursor(parent=Cursor(None, Cursor.ROOT_VALUE), value=original)

        out = TemplateEngine._apply_coordinates(result, cursor, coordinates)
        assert out is not None
