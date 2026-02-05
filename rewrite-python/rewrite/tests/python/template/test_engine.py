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
