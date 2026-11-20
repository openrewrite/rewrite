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

"""Py2 sources that previously failed the print-idempotency check."""

import ast

import pytest

from rewrite.java import tree as j
from rewrite.java.markers import OmitParentheses
from rewrite.python import tree as py
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.python.visitor import PythonVisitor
from tests.python.py27.expressions_test import _collect_placeholders


def _parse(src: str) -> py.CompilationUnit:
    return Py2ParserVisitor(src, "<test>", "2.7").parse()


def _slices(cu: py.CompilationUnit) -> list:
    found = []

    def shape(padded):
        return padded and (type(padded.element).__name__, padded.element.prefix.whitespace, padded.after.whitespace)

    class Slices(PythonVisitor):
        def visit_slice(self, slice_, p):
            found.append((slice_.prefix.whitespace,
                          shape(slice_.padding.start), shape(slice_.padding.stop), shape(slice_.padding.step)))
            return super().visit_slice(slice_, p)

    Slices().visit(cu, None)
    return found


def _assigned_value(cu: py.CompilationUnit):
    stmt = cu.statements[0]
    assert isinstance(stmt, j.Assignment)
    return stmt.assignment


@pytest.mark.parametrize("src", [
    "def f():\n    if x:\n        a\n    elif y:\n        b\n",
    "if x:\n    a\n# c\nelif y:\n    b\n",
    "if x:\n    a\n\nelif y:\n    b\n",
    "if x:\n    a\nelif y:\n    if z:\n        b\n    elif w:\n        c\n",

    "for a, b in xs:\n    pass\n",
    "for a,b in xs: pass\n",
    "for a, in xs:\n    pass\n",
    "for (a, b) in xs:\n    pass\n",
    "x = [1 for a, b in xs]\n",
    "x = a[i, j]\n",

    "x = sum(i for i in xs)\n",
    "f( i for i in xs )\n",
    "x = sum((i for i in xs))\n",

    "x = lambda (x, w): x\n",
    "x = lambda (x, w), y: x\n",
    "x = lambda y, (x, (w, z)) = t: x\n",
    "x = lambda ( x , w , ): x\n",
    "def f((a, b)):\n    pass\n",
    "def f(a, (b, (c, d))=t, *e, **kw):\n    pass\n",
    "def f(((a))):\n    pass\n",
    "def f(a, ):\n    pass\n",
    "def f((a, b) , ):\n    pass\n",
    "x = lambda a , : a\n",

    "if y:  # c\n    b\n",
    "if x:\n    a\nelif y:  # c\n    b\nelse:  # d\n    c\n",
    "def f():  # c\n    # d\n    b\n",
    "x = 1;  # c\n",
    "x = 1;\n\n# e\n",
    "if x: a; b;  # c\n",
    "if x: a = 1",

    "x = a[...]\n",
    "x = a[. . .]\n",
    "x = a[1:2, ..., :, 3]\n",
    "x = [y for y in 1, 2]\n",
])
def test_round_trip(src):
    cu = _parse(src)
    assert list(_collect_placeholders(cu)) == []
    assert PythonPrinter().print(cu) == src


def test_elif_prefix_is_on_else():
    cu = _parse("def f():\n    if x:\n        a\n    elif y:\n        b\n")
    if_ = cu.statements[0].body.statements[0]
    assert isinstance(if_, j.If)
    assert if_.else_part.prefix.whitespace == "    "
    assert isinstance(if_.else_part.body, j.If)
    assert if_.else_part.body.prefix.whitespace == ""


def test_comment_after_colon_is_on_first_statement():
    block = _parse("if x:  # c\n    a\n").statements[0].then_part
    first = block.statements[0]
    assert first.prefix.whitespace == "  "
    assert [c.text for c in first.prefix.comments] == [" c"]
    assert first.prefix.comments[0].suffix == "\n    "


def test_lambda_tuple_parameter():
    lam = _assigned_value(_parse("x = lambda (a, b), c: a\n"))
    assert isinstance(lam, j.Lambda)
    first, second = lam.parameters.parameters
    assert isinstance(first, py.ExpressionStatement)
    assert isinstance(first.expression, py.CollectionLiteral)
    assert first.expression.kind == py.CollectionLiteral.Kind.TUPLE
    assert [e.simple_name for e in first.expression.elements] == ["a", "b"]
    assert isinstance(second, j.VariableDeclarations)


def test_def_tuple_parameter_with_default():
    m = _parse("def f((a, b)=t):\n    pass\n").statements[0]
    assert isinstance(m, j.MethodDeclaration)
    [param] = m.parameters
    assert isinstance(param, py.ExpressionStatement)
    param = param.expression
    assert isinstance(param, j.Assignment)
    assert isinstance(param.variable, py.CollectionLiteral)
    assert isinstance(param.assignment, j.Identifier)
    assert param.assignment.simple_name == "t"


def test_generator_argument_omits_parentheses():
    mi = _parse("sum(i for i in xs)\n").statements[0]
    assert isinstance(mi, j.MethodInvocation)
    [arg] = mi.arguments
    assert isinstance(arg, py.ComprehensionExpression)
    assert arg.kind == py.ComprehensionExpression.Kind.GENERATOR
    assert arg.markers.find_first(OmitParentheses) is not None


@pytest.mark.parametrize("src", [
    "@d  # c\n@e  # d\ndef f():\n    pass\n",
    "@d  # c\nclass A:\n    pass\n",
], ids=["def", "class"])
def test_decorator_line_comments_round_trip_as_comments(src):
    cu = _parse(src)
    assert PythonPrinter().print(cu) == src

    texts = []

    class Spaces(PythonVisitor):
        def visit_space(self, space, p):
            texts.append(space.whitespace)
            texts.extend(comment.suffix for comment in space.comments)
            return space

    Spaces().visit(cu, None)
    assert not [text for text in texts if '#' in text]


@pytest.mark.parametrize("src", [
    "x = a[x, :]\n",
    "x = a[1:]\n",
    "x = a[::]\n",
    "x = a[x, ::2]\n",
    "x = a[ 1 : 2 :3]\n",
])
def test_slices_take_the_shape_the_python_3_parser_gives_them(src):
    cu = _parse(src)
    assert PythonPrinter().print(cu) == src
    assert _slices(cu) == _slices(ParserVisitor(src, "<test>", None).visit_Module(ast.parse(src)))
