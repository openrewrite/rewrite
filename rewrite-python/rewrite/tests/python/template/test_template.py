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


class TestTemplateApplyRecipe:
    """End-to-end tests for pattern match + template apply in recipes."""

    def test_replace_method_call_with_assignment(self):
        """Test that pattern({obj}.setX({val})) + template({obj}.x = {val}) produces '=' not ':='."""
        from rewrite import ExecutionContext, Recipe, TreeVisitor
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import pattern, capture
        from rewrite.java.tree import MethodInvocation
        from rewrite.test import RecipeSpec, python

        obj = capture('obj')
        val = capture('val')
        pat = pattern("{obj}.setDaemon({val})", obj=obj, val=val)
        tmpl = template("{obj}.daemon = {val}", obj=obj, val=val)

        class TestRecipe(Recipe):
            @property
            def name(self) -> str:
                return "test.ReplaceSetterWithAssignment"

            @property
            def display_name(self) -> str:
                return "Test"

            @property
            def description(self) -> str:
                return "Test"

            def editor(self):
                class Visitor(PythonVisitor[ExecutionContext]):
                    def visit_method_invocation(self, method, p):
                        method = super().visit_method_invocation(method, p)
                        match = pat.match(method, self.cursor)
                        if match:
                            return tmpl.apply(self.cursor, values=match)
                        return method
                return Visitor()

        spec = RecipeSpec(recipe=TestRecipe())
        spec.rewrite_run(
            python(
                "thread.setDaemon(True)",
                "thread.daemon = True",
            )
        )

    def test_replace_method_call_with_method_call(self):
        """Test that pattern(datetime.utcnow()) + template(datetime.now(datetime.UTC)) works."""
        from rewrite import ExecutionContext, Recipe, TreeVisitor
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import pattern, capture
        from rewrite.java.tree import MethodInvocation
        from rewrite.test import RecipeSpec, python

        pat = pattern("datetime.utcnow()")
        tmpl = template("datetime.now(datetime.UTC)")

        class TestRecipe(Recipe):
            @property
            def name(self) -> str:
                return "test.ReplaceUtcNow"

            @property
            def display_name(self) -> str:
                return "Test"

            @property
            def description(self) -> str:
                return "Test"

            def editor(self):
                class Visitor(PythonVisitor[ExecutionContext]):
                    def visit_method_invocation(self, method, p):
                        method = super().visit_method_invocation(method, p)
                        match = pat.match(method, self.cursor)
                        if match:
                            return tmpl.apply(self.cursor, values=match)
                        return method
                return Visitor()

        spec = RecipeSpec(recipe=TestRecipe())
        spec.rewrite_run(
            python(
                "now = datetime.utcnow()",
                "now = datetime.now(datetime.UTC)",
            )
        )

    def test_replace_method_call_with_field_access(self):
        """Test that pattern({obj}.getName()) + template({obj}.name) works."""
        from rewrite import ExecutionContext, Recipe, TreeVisitor
        from rewrite.python.visitor import PythonVisitor
        from rewrite.python.template import pattern, capture
        from rewrite.java.tree import MethodInvocation
        from rewrite.test import RecipeSpec, python

        obj = capture('obj')
        pat = pattern("{obj}.getName()", obj=obj)
        tmpl = template("{obj}.name", obj=obj)

        class TestRecipe(Recipe):
            @property
            def name(self) -> str:
                return "test.ReplaceGetterWithProperty"

            @property
            def display_name(self) -> str:
                return "Test"

            @property
            def description(self) -> str:
                return "Test"

            def editor(self):
                class Visitor(PythonVisitor[ExecutionContext]):
                    def visit_method_invocation(self, method, p):
                        method = super().visit_method_invocation(method, p)
                        match = pat.match(method, self.cursor)
                        if match:
                            return tmpl.apply(self.cursor, values=match)
                        return method
                return Visitor()

        spec = RecipeSpec(recipe=TestRecipe())
        spec.rewrite_run(
            python(
                "name = thread.getName()",
                "name = thread.name",
            )
        )


class TestTemplateApply:
    """Tests for Template.apply()."""

    def test_apply_no_captures_returns_tree(self):
        """Test that apply with no captures returns a tree."""
        tmpl = template("x + 1")
        result = tmpl.apply(cursor=None)
        assert result is not None

    def test_apply_with_dict_values(self):
        """Test applying a template with dict values."""
        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)

        ident = j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], "hello", None, None)
        result = tmpl.apply(cursor=None, values={'expr': ident})

        assert isinstance(result, j.MethodInvocation)
        assert len(result.arguments) == 1
        assert isinstance(result.arguments[0], j.Identifier)
        assert result.arguments[0].simple_name == "hello"

    def test_apply_with_match_result(self):
        """Test applying a template with a MatchResult."""
        from rewrite.python.template import MatchResult

        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)

        ident = j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], "world", None, None)
        match_result = MatchResult({'expr': ident})
        result = tmpl.apply(cursor=None, values=match_result)

        assert isinstance(result, j.MethodInvocation)
        assert len(result.arguments) == 1
        assert isinstance(result.arguments[0], j.Identifier)
        assert result.arguments[0].simple_name == "world"
