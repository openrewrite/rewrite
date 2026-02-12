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

"""Tests for Template class."""

from uuid import uuid4

import pytest

from rewrite.java import tree as j
from rewrite.java.support_types import Space
from rewrite.markers import Markers
from rewrite.python.template import template, capture, Template, TemplateBuilder
from rewrite.python.template.engine import TemplateEngine


class TestTemplate:
    """Tests for Template class and template() factory."""

    def test_simple_template(self):
        """Test creating a simple template."""
        tmpl = template("x + 1")

        assert tmpl.code == "x + 1"
        assert len(tmpl.captures) == 0

    def test_template_with_capture(self):
        """Test creating a template with a capture."""
        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)

        assert tmpl.code == "print({expr})"
        assert 'expr' in tmpl.captures
        assert tmpl.captures['expr'] is expr

    def test_template_get_tree(self):
        """Test getting the parsed template tree."""
        tmpl = template("x + 1")
        tree = tmpl.get_tree()

        assert isinstance(tree, j.Binary)

    def test_template_tree_cached(self):
        """Test that template tree is cached."""
        tmpl = template("x + 1")

        tree1 = tmpl.get_tree()
        tree2 = tmpl.get_tree()

        assert tree1 is tree2

    def test_multiple_captures(self):
        """Test template with multiple captures."""
        a, b = capture('a'), capture('b')
        tmpl = template("{a} + {b}", a=a, b=b)

        assert len(tmpl.captures) == 2
        assert 'a' in tmpl.captures
        assert 'b' in tmpl.captures


class TestApplySubstitutions:
    """Tests for TemplateEngine.apply_substitutions with placeholder replacement."""

    @staticmethod
    def _ident(name: str) -> j.Identifier:
        return j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], name, None, None)

    def test_replace_placeholder_in_method_args(self):
        """Placeholder in method arguments is replaced correctly."""
        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)
        tree = tmpl.get_tree()

        assert isinstance(tree, j.MethodInvocation)

        result = TemplateEngine.apply_substitutions(
            tree, {'expr': self._ident("hello")}, cursor=None,
        )

        assert isinstance(result, j.MethodInvocation)
        assert result.name.simple_name == "print"
        assert len(result.arguments) == 1
        assert isinstance(result.arguments[0], j.Identifier)
        assert result.arguments[0].simple_name == "hello"

    def test_replace_placeholder_in_method_select(self):
        """Placeholder in method select (receiver) is replaced correctly."""
        obj = capture('obj')
        tmpl = template("{obj}.method()", obj=obj)
        tree = tmpl.get_tree()

        assert isinstance(tree, j.MethodInvocation)

        result = TemplateEngine.apply_substitutions(
            tree, {'obj': self._ident("myobj")}, cursor=None,
        )

        assert isinstance(result, j.MethodInvocation)
        assert isinstance(result.select, j.Identifier)
        assert result.select.simple_name == "myobj"
        assert result.name.simple_name == "method"

    def test_replace_multiple_placeholders(self):
        """Multiple placeholders in a binary expression are replaced."""
        a, b = capture('a'), capture('b')
        tmpl = template("{a} + {b}", a=a, b=b)
        tree = tmpl.get_tree()

        assert isinstance(tree, j.Binary)

        result = TemplateEngine.apply_substitutions(
            tree, {'a': self._ident("x"), 'b': self._ident("y")}, cursor=None,
        )

        assert isinstance(result, j.Binary)
        assert isinstance(result.left, j.Identifier)
        assert result.left.simple_name == "x"
        assert isinstance(result.right, j.Identifier)
        assert result.right.simple_name == "y"


class TestTemplateBuilder:
    """Tests for TemplateBuilder class."""

    def test_simple_builder(self):
        """Test simple template building."""
        tmpl = (Template.builder()
            .code("x + 1")
            .build())

        assert tmpl.code == "x + 1"

    def test_builder_with_param(self):
        """Test building template with parameter."""
        expr = capture('expr')
        tmpl = (Template.builder()
            .code("print({expr})")
            .param(expr)
            .build())

        assert 'expr' in tmpl.captures

    def test_builder_concatenation(self):
        """Test building template from multiple code segments."""
        tmpl = (Template.builder()
            .code("print(")
            .code("'hello'")
            .code(")")
            .build())

        assert tmpl.code == "print('hello')"

    def test_builder_with_raw(self):
        """Test building template with raw code."""
        method = "warn"
        tmpl = (Template.builder()
            .code("logger.")
            .raw(method)
            .code("('msg')")
            .build())

        assert "warn" in tmpl.code

    def test_builder_with_imports(self):
        """Test building template with imports."""
        tmpl = (Template.builder()
            .code("datetime.now()")
            .imports("from datetime import datetime")
            .build())

        # Imports should be stored but not affect code
        assert tmpl.code == "datetime.now()"

    def test_builder_chaining(self):
        """Test that builder methods return self for chaining."""
        builder = Template.builder()

        assert builder.code("x") is builder
        assert builder.raw("y") is builder
        assert builder.imports("import z") is builder
