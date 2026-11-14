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
from rewrite.java import J, tree as j
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


def test_a_replaced_statement_keeps_the_blank_lines_before_it():
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "import x\n"
            "\n"
            "\n"
            "try:\n"
            "    do_stuff()\n"
            "except KeyError:\n"
            "    pass\n"
            "\n"
            "\n"
            "def f():\n"
            "    a()\n"
            "\n"
            "\n"
            "    try:\n"
            "        do_stuff()\n"
            "    except KeyError:\n"
            "        pass\n",
            #
            "import x\n"
            "\n"
            "\n"
            "with suppress(KeyError):\n"
            "    do_stuff()\n"
            "\n"
            "\n"
            "def f():\n"
            "    a()\n"
            "\n"
            "\n"
            "    with suppress(KeyError):\n"
            "        do_stuff()\n",
        )
    )


def test_a_statement_replaced_after_an_import_gains_no_blank_line_after_imports():
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "import x\n"
            "try:\n"
            "    do_stuff()\n"
            "except KeyError:\n"
            "    pass\n",
            #
            "import x\n"
            "with suppress(KeyError):\n"
            "    do_stuff()\n",
        )
    )


_function = template('def x():\n    return 1')


class _AssignmentToFunction(Recipe):
    @property
    def name(self) -> str:
        return "test.AssignmentToFunction"

    @property
    def display_name(self) -> str:
        return "Test replace an assignment to x with a function"

    @property
    def description(self) -> str:
        return "Replaces each statement assigning to `x` with a function returning 1."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_assignment(self, assignment: j.Assignment, p: ExecutionContext) -> Optional[J]:
                if not isinstance(self.cursor.parent_tree_cursor().value, (py_tree.CompilationUnit, j.Block)) or \
                        not isinstance(assignment.variable, j.Identifier) or assignment.variable.simple_name != 'x':
                    return assignment
                return _function.apply(self.cursor)

        return Visitor()


def test_a_replaced_first_statement_keeps_the_first_line():
    spec = RecipeSpec(recipe=_AssignmentToFunction())
    spec.rewrite_run(
        python(
            "x = 1\n",
            #
            "def x():\n"
            "    return 1\n",
        ),
        python(
            "# x is 1\n"
            "x = 1\n",
            #
            "# x is 1\n"
            "def x():\n"
            "    return 1\n",
        ),
    )


def test_a_replaced_first_statement_of_a_class_gets_no_blank_line_before_it():
    spec = RecipeSpec(recipe=_AssignmentToFunction())
    spec.rewrite_run(
        python(
            "class A:\n"
            "    x = 1\n"
            "    y = 2\n",
            #
            "class A:\n"
            "    def x():\n"
            "        return 1\n"
            "    y = 2\n",
        )
    )


def test_a_statement_replaced_by_a_function_gets_the_blank_lines_around_one():
    spec = RecipeSpec(recipe=_AssignmentToFunction())
    spec.rewrite_run(
        python(
            "import os\n"
            "x = 1\n",
            #
            "import os\n"
            "\n"
            "\n"
            "def x():\n"
            "    return 1\n",
        )
    )


def test_a_statement_replaced_by_a_method_gets_the_blank_line_around_one():
    spec = RecipeSpec(recipe=_AssignmentToFunction())
    spec.rewrite_run(
        python(
            "class A:\n"
            "    y = 1\n"
            "    x = 2\n",
            #
            "class A:\n"
            "    y = 1\n"
            "\n"
            "    def x():\n"
            "        return 1\n",
        )
    )


def test_a_replacement_keeps_more_blank_lines_than_the_style_asks_for():
    spec = RecipeSpec(recipe=_AssignmentToFunction())
    spec.rewrite_run(
        python(
            "import os\n"
            "\n"
            "\n"
            "\n"
            "x = 1\n"
            "\n"
            "\n"
            "def f():\n"
            "    a()\n"
            "\n"
            "\n"
            "\n"
            "    x = 2\n",
            #
            "import os\n"
            "\n"
            "\n"
            "\n"
            "def x():\n"
            "    return 1\n"
            "\n"
            "\n"
            "def f():\n"
            "    a()\n"
            "\n"
            "\n"
            "\n"
            "    def x():\n"
            "        return 1\n",
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


_stmt = capture('stmt')
_wrap_template = template('with lock:\n    {stmt}', stmt=_stmt)


class _WrapIfInWith(Recipe):
    @property
    def name(self) -> str:
        return "test.WrapIfInWith"

    @property
    def display_name(self) -> str:
        return "Test wrap if in with-statement"

    @property
    def description(self) -> str:
        return "Wraps each if-statement in a with-statement."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_if(self, if_stmt: j.If, p: ExecutionContext) -> Optional[J]:
                if self.cursor.first_enclosing(j.Try) is not None:
                    return if_stmt
                return _wrap_template.apply(self.cursor, values={'stmt': if_stmt})

        return Visitor()


def test_template_reindents_a_substituted_block_moved_deeper():
    spec = RecipeSpec(recipe=_WrapIfInWith())
    spec.rewrite_run(
        python(
            "def f():\n"
            "    if a :\n"
            "        b( 1 )\n",
            #
            "def f():\n"
            "    with lock:\n"
            "        if a :\n"
            "            b( 1 )\n",
        )
    )


_three = template('3')


class _ReplaceTwoWithThree(Recipe):
    @property
    def name(self) -> str:
        return "test.ReplaceTwoWithThree"

    @property
    def display_name(self) -> str:
        return "Test replace 2 with 3"

    @property
    def description(self) -> str:
        return "Replaces each literal 2 with a template for 3."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_literal(self, literal: j.Literal, p: ExecutionContext) -> Optional[J]:
                return _three.apply(self.cursor) if literal.value == 2 else literal

        return Visitor()


def test_a_replaced_operand_keeps_the_indentation_of_its_continuation_line():
    spec = RecipeSpec(recipe=_ReplaceTwoWithThree())
    spec.rewrite_run(
        python(
            "a = (1 +\n"
            "     2)\n"
            "b = f(1,\n"
            "      2)\n"
            "c = [1,\n"
            "     2]\n",
            #
            "a = (1 +\n"
            "     3)\n"
            "b = f(1,\n"
            "      3)\n"
            "c = [1,\n"
            "     3]\n",
        )
    )


def test_a_replaced_statement_keeps_the_indentation_of_its_block():
    """The lines inside the result indent from there as the file indents."""
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "def f():\n"
            "  try:\n"
            "    do_stuff()\n"
            "  except KeyError:\n"
            "    pass\n"
            "  done()\n",
            #
            "def f():\n"
            "  with suppress(KeyError):\n"
            "    do_stuff()\n"
            "  done()\n",
        )
    )


def test_a_replacement_in_a_tab_indented_file_indents_with_tabs():
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "def f():\n"
            "\ttry:\n"
            "\t\tdo_stuff()\n"
            "\texcept KeyError:\n"
            "\t\tpass\n"
            "\tdone()\n",
            #
            "def f():\n"
            "\twith suppress(KeyError):\n"
            "\t\tdo_stuff()\n"
            "\tdone()\n",
        )
    )


def test_template_replacement_keeps_crlf_line_breaks():
    spec = RecipeSpec(recipe=_ReplaceTryWithWith())
    spec.rewrite_run(
        python(
            "def f():\r\n"
            "    try:\r\n"
            "        do_stuff()\r\n"
            "    except KeyError:\r\n"
            "        pass\r\n",
            #
            "def f():\r\n"
            "    with suppress(KeyError):\r\n"
            "        do_stuff()\r\n",
        )
    )


def test_template_with_a_substituted_block_keeps_crlf_line_breaks():
    spec = RecipeSpec(recipe=_WrapIfInWith())
    spec.rewrite_run(
        python(
            "def f():\r\n"
            "    if a :\r\n"
            "        b( 1 )\r\n",
            #
            "def f():\r\n"
            "    with lock:\r\n"
            "        if a :\r\n"
            "            b( 1 )\r\n",
        )
    )
