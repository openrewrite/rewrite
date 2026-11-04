# Copyright 2026 the original author or authors.
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

"""Writes to `Py.ExpressionStatement` and `Py.StatementExpression` land on the node
each of them wraps, which is where their prefix and markers live."""

import ast

from rewrite import InMemoryExecutionContext, Markers, random_id
from rewrite.java import Block, MethodDeclaration, Space
from rewrite.markers import SearchResult
from rewrite.python import ExpressionStatement, PythonVisitor, StatementExpression
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source))


class _Reindent(PythonVisitor):
    """Re-indents through a visitor, the shape a formatting recipe reaches these nodes in."""

    def __init__(self, target, whitespace):
        self._target = target
        self._whitespace = whitespace

    def pre_visit(self, tree, p):
        return tree.replace(_prefix=Space([], self._whitespace)) if isinstance(tree, self._target) else tree


def test_expression_statement_replace_reaches_the_wrapped_expression():
    cu = _parse('"""docstring"""\n')
    stmt = cu.statements[0]
    assert isinstance(stmt, ExpressionStatement)

    marker = SearchResult(random_id(), None)
    written = stmt.replace(_prefix=Space([], "\n    "), _markers=Markers(random_id(), [marker]))

    assert written.prefix.whitespace == "\n    "
    assert written.expression.prefix.whitespace == "\n    "
    assert written.markers.find_first(SearchResult) is marker
    assert written.expression.markers.find_first(SearchResult) is marker
    assert written.type is not None and written.type is written.expression.type
    assert stmt.replace(_type=None).expression.type is None
    assert stmt.prefix.whitespace == "" and stmt.markers.find_first(SearchResult) is None

    reindented = _Reindent(ExpressionStatement, "\n    ").visit(cu, InMemoryExecutionContext())
    assert PythonPrinter().print(reindented) == '\n    """docstring"""\n'


def test_statement_expression_replace_reaches_the_wrapped_statement():
    cu = _parse("def f():\n    yield 1\n")
    method = cu.statements[0]
    assert isinstance(method, MethodDeclaration) and isinstance(method.body, Block)
    stmt = method.body.statements[0]
    assert isinstance(stmt, StatementExpression)

    marker = SearchResult(random_id(), None)
    written = stmt.replace(_prefix=Space([], "\n        "), _markers=Markers(random_id(), [marker]))

    assert written.prefix.whitespace == "\n        "
    assert written.statement.prefix.whitespace == "\n        "
    assert written.markers.find_first(SearchResult) is marker
    assert written.statement.markers.find_first(SearchResult) is marker
    assert stmt.prefix.whitespace == "\n    " and stmt.markers.find_first(SearchResult) is None
    assert stmt.replace(_type=None) is stmt

    reindented = _Reindent(StatementExpression, "\n        ").visit(cu, InMemoryExecutionContext())
    assert PythonPrinter().print(reindented) == "def f():\n        yield 1\n"
