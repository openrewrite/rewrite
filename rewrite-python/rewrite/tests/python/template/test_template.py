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

from typing import Any, Optional
from uuid import uuid4

import pytest

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.java import tree as j
from rewrite.java.tree import Unary
from rewrite.java.support_types import Space, JRightPadded
from rewrite.markers import Markers
from rewrite.python.template import template, capture, pattern, Template, TemplateBuilder
from rewrite.python.template.engine import TemplateEngine
from rewrite.python.template.replacement import maybe_parenthesize
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python
from rewrite.visitor import Cursor


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
            tree, {'expr': self._ident("hello")},
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
            tree, {'obj': self._ident("myobj")},
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
            tree, {'a': self._ident("x"), 'b': self._ident("y")},
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


# ---------------------------------------------------------------------------
# maybe_parenthesize unit tests
# ---------------------------------------------------------------------------


class TestMaybeParenthesize:
    """Unit tests for maybe_parenthesize() — verifies that the function
    wraps a result node in parentheses when required by the parent context.
    """

    @staticmethod
    def _ident(name: str) -> j.Identifier:
        return j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], name, None, None)

    @staticmethod
    def _binary(left, op, right) -> j.Binary:
        return j.Binary(
            uuid4(), Space.EMPTY, Markers.EMPTY, left,
            JRightPadded(op, Space([], ' '), Markers.EMPTY),
            right, None,
        )

    @staticmethod
    def _cursor_chain(*nodes) -> Cursor:
        """Build a cursor chain from root → … → leaf."""
        cur = Cursor(None, Cursor.ROOT_VALUE)
        for node in nodes:
            cur = Cursor(cur, node)
        return cur

    def test_or_result_in_and_parent(self):
        """An `or` expression placed inside an `and` must be parenthesized."""
        or_expr = self._binary(self._ident('a'), j.Binary.Type.Or, self._ident('b'))
        and_parent = self._binary(or_expr, j.Binary.Type.And, self._ident('z'))
        cursor = self._cursor_chain(and_parent, or_expr)

        result = maybe_parenthesize(or_expr, cursor)
        assert isinstance(result, j.Parentheses)

    def test_and_result_in_or_parent(self):
        """`and` has higher precedence than `or`, so no parens needed."""
        and_expr = self._binary(self._ident('a'), j.Binary.Type.And, self._ident('b'))
        or_parent = self._binary(and_expr, j.Binary.Type.Or, self._ident('z'))
        cursor = self._cursor_chain(or_parent, and_expr)

        result = maybe_parenthesize(and_expr, cursor)
        assert not isinstance(result, j.Parentheses)

    def test_or_result_under_not(self):
        """`or` under `not` needs parens (not has precedence 3, or is 1)."""
        or_expr = self._binary(self._ident('a'), j.Binary.Type.Or, self._ident('b'))
        not_parent = j.Unary(
            uuid4(), Space.EMPTY, Markers.EMPTY,
            JRightPadded(j.Unary.Type.Not, Space.EMPTY, Markers.EMPTY),
            or_expr, None,
        )
        cursor = self._cursor_chain(not_parent, or_expr)

        result = maybe_parenthesize(or_expr, cursor)
        assert isinstance(result, j.Parentheses)

    def test_identifier_result_unchanged(self):
        """Non-binary/unary results are returned unchanged."""
        ident = self._ident('x')
        and_parent = self._binary(ident, j.Binary.Type.And, self._ident('z'))
        cursor = self._cursor_chain(and_parent, ident)

        result = maybe_parenthesize(ident, cursor)
        assert result is ident

    def test_same_precedence_no_parens(self):
        """`and` inside `and` — same precedence, no parens needed."""
        inner = self._binary(self._ident('a'), j.Binary.Type.And, self._ident('b'))
        outer = self._binary(inner, j.Binary.Type.And, self._ident('c'))
        cursor = self._cursor_chain(outer, inner)

        result = maybe_parenthesize(inner, cursor)
        assert not isinstance(result, j.Parentheses)

    def test_template_apply_parenthesizes_in_binary_context(self):
        """template.apply() should parenthesize its result when the cursor
        parent is a higher-precedence binary operator."""
        from rewrite.python.tree import ExpressionStatement

        _x = capture('x')
        _y = capture('y')
        _not_x_or_not_y = template("not {x} or not {y}", x=_x, y=_y)

        # Build cursor: root → Binary(and) → Unary(not)
        # The Unary is the node being replaced by the template result.
        x_ident = self._ident('x')
        y_ident = self._ident('y')
        and_inner = self._binary(x_ident, j.Binary.Type.And, y_ident)
        not_expr = j.Unary(
            uuid4(), Space.EMPTY, Markers.EMPTY,
            JRightPadded(j.Unary.Type.Not, Space.EMPTY, Markers.EMPTY),
            j.Parentheses(
                uuid4(), Space.EMPTY, Markers.EMPTY,
                JRightPadded(and_inner, Space.EMPTY, Markers.EMPTY),
            ),
            None,
        )
        z_ident = self._ident('z')
        outer_and = self._binary(not_expr, j.Binary.Type.And, z_ident)
        cursor = self._cursor_chain(outer_and, not_expr)

        result = _not_x_or_not_y.apply(
            cursor, values={'x': x_ident, 'y': y_ident}
        )

        # apply() wraps in ExpressionStatement (target Unary is a Statement);
        # the inner expression should be Parentheses since `or` is placed
        # inside an `and` context.
        inner = result.expression if isinstance(result, ExpressionStatement) else result
        assert isinstance(inner, j.Parentheses), (
            f"Expected Parentheses but got {type(inner).__name__}"
        )


# ---------------------------------------------------------------------------
# Template apply precedence in surrounding context (integration tests)
# ---------------------------------------------------------------------------
#
# When template.apply() returns a node with lower precedence than the
# site where it replaces, the framework should wrap it in parentheses.
#
# Example: "not {x} or not {y}" produces an `or` expression. If the
# replacement site's parent is `and`, the result needs outer parentheses
# because `or` has lower precedence than `and`.
# ---------------------------------------------------------------------------


class _DeMorganRecipe(Recipe):
    """Minimal De Morgan recipe that demonstrates the parenthesization bug."""

    @property
    def name(self) -> str:
        return "test.DeMorgan"

    @property
    def display_name(self) -> str:
        return "Test De Morgan"

    @property
    def description(self) -> str:
        return "Test De Morgan parenthesization"

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        _x = capture('x')
        _y = capture('y')
        _not_and = pattern("not ({x} and {y})", x=_x, y=_y)
        _not_or = pattern("not ({x} or {y})", x=_x, y=_y)
        _not_x_or_not_y = template("not {x} or not {y}", x=_x, y=_y)
        _not_x_and_not_y = template("not {x} and not {y}", x=_x, y=_y)

        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_unary(
                self, unary: Unary, p: ExecutionContext
            ) -> Optional[Unary]:
                unary = super().visit_unary(unary, p)

                match = _not_and.match(unary, self.cursor)
                if match:
                    return _not_x_or_not_y.apply(self.cursor, values=match)

                match = _not_or.match(unary, self.cursor)
                if match:
                    return _not_x_and_not_y.apply(self.cursor, values=match)

                return unary

        return Visitor()


class TestTemplateApplyPrecedenceInContext:
    """Template.apply() result must be parenthesized when inserted into
    a higher-precedence context.

    All tests use a minimal De Morgan recipe:
        not (x and y) → not x or not y
        not (x or y)  → not x and not y
    """

    def test_demorgan_standalone(self):
        """Baseline: De Morgan works at top level (no surrounding context)."""
        spec = RecipeSpec(recipe=_DeMorganRecipe())
        spec.rewrite_run(
            python(
                "result = not (a and b)",
                "result = not a or not b",
            )
        )

    def test_demorgan_result_in_and_context(self):
        """not (x and y) inside `... and z` must wrap result in parens.

        not (x and y) and z
          → (not x or not y) and z

        Without parens: not x or not y and z
        Python parses:  (not x) or ((not y) and z)  ← WRONG
        """
        spec = RecipeSpec(recipe=_DeMorganRecipe())
        spec.rewrite_run(
            python(
                "result = not (x and y) and z",
                "result = (not x or not y) and z",
            )
        )

    def test_demorgan_result_in_or_context(self):
        """not (x or y) inside `... or z` — no parens needed.

        not (x or y) or z
          → not x and not y or z

        `and` has higher precedence than `or`, so
        Python parses:  ((not x) and (not y)) or z  ← correct without parens.
        """
        spec = RecipeSpec(recipe=_DeMorganRecipe())
        spec.rewrite_run(
            python(
                "result = not (x or y) or z",
                "result = not x and not y or z",
            )
        )

    def test_demorgan_result_on_right_of_and(self):
        """z and not (x and y) must wrap result in parens.

        z and not (x and y)
          → z and (not x or not y)

        Without parens: z and not x or not y
        Python parses:  (z and (not x)) or (not y)  ← WRONG
        """
        spec = RecipeSpec(recipe=_DeMorganRecipe())
        spec.rewrite_run(
            python(
                "result = z and not (x and y)",
                "result = z and (not x or not y)",
            )
        )

    def test_demorgan_result_in_if_condition(self):
        """De Morgan in an if condition should still be correct."""
        spec = RecipeSpec(recipe=_DeMorganRecipe())
        spec.rewrite_run(
            python(
                "if not (x and y) and z:\n    pass",
                "if (not x or not y) and z:\n    pass",
            )
        )
