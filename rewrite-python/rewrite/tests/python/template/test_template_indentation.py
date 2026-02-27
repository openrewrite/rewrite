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

"""Tests for template apply preserving indentation in nested contexts.

When a template replaces a compound statement (e.g., try -> with) inside
a function body, the replacement must inherit the original statement's
indentation. The template engine's apply_coordinates() calls auto_format
which must receive the correct cursor context to compute indentation.
"""

from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.java import tree as j
from rewrite.python import tree as py_tree
from rewrite.python.template import template, capture
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


# Template: replace a try/except:pass with a with-statement
_exc = capture('exc')
_body = capture('body')
_with_template = template(
    'with suppress({exc}):\n    {body}',
    exc=_exc,
    body=_body,
)


class _ReplaceTryWithWith(Recipe):
    """Replaces try/except <Exc>: pass with: with suppress(<Exc>): <body>."""

    @property
    def name(self) -> str:
        return "test.ReplaceTryWithWith"

    @property
    def display_name(self) -> str:
        return "Test replace try with with-statement"

    @property
    def description(self) -> str:
        return "Replaces single-except try/pass with a with-statement."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_try(
                self, try_stmt: j.Try, p: ExecutionContext
            ) -> Optional[j.Try]:
                try_stmt = super().visit_try(try_stmt, p)

                if len(try_stmt.catches) != 1:
                    return try_stmt
                catch = try_stmt.catches[0]
                if len(catch.body.statements) != 1:
                    return try_stmt
                if not isinstance(catch.body.statements[0], py_tree.Pass):
                    return try_stmt

                body_stmts = try_stmt.body.statements
                if len(body_stmts) != 1:
                    return try_stmt

                vd = catch.parameter.tree
                te = vd.type_expression
                if te is None:
                    return try_stmt
                if isinstance(te, py_tree.ExceptionType):
                    exc_expr = te.expression
                else:
                    exc_expr = te

                values = {
                    'exc': exc_expr,
                    'body': body_stmts[0],
                }
                return _with_template.apply(self.cursor, values=values)

        return Visitor()


def test_template_replace_try_at_top_level():
    """Template replacement at top level preserves zero indentation."""
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "try:\n"
            "    do_stuff()\n"
            "except KeyError:\n"
            "    pass",
            #
            "with suppress(KeyError):\n"
            "    do_stuff()",
        )
    )


def test_template_replace_try_inside_function():
    """Template replacement inside a function must preserve indentation.

    The with-statement should be at column 4 (inside the function body),
    and its body at column 8.
    """
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "def f():\n"
            "    try:\n"
            "        do_stuff()\n"
            "    except KeyError:\n"
            "        pass",
            #
            "def f():\n"
            "    with suppress(KeyError):\n"
            "        do_stuff()",
        )
    )


def test_template_replace_try_inside_class_method():
    """Template replacement inside a class method must preserve indentation.

    The with-statement should be at column 8 (inside the method body).
    """
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "class Foo:\n"
            "    def bar(self):\n"
            "        try:\n"
            "            do_stuff()\n"
            "        except KeyError:\n"
            "            pass",
            #
            "class Foo:\n"
            "    def bar(self):\n"
            "        with suppress(KeyError):\n"
            "            do_stuff()",
        )
    )
