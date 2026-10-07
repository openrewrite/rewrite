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

"""A subtree prints as it reads in its file when the printer is given its ancestors.

Some syntax is decided by what encloses a node: ``=`` over ``:=``, the ``->`` of a return
type, ``elif``, the ``import`` keyword. A caller's cursor holds padding between a tree and
its parent, which the printer has to look through.
"""

import ast
import copy
from uuid import uuid4

import pytest

from rewrite import Cursor
from rewrite.java import Space
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter

_SOURCE = (
    "x = 1\n"
    "if a:\n"
    "    pass\n"
    "elif b:\n"
    "    pass\n"
    "def f(a: int) -> int:\n"
    "    return a\n"
    "from m import n\n"
    "g = lambda p, q=1: p\n"
    "def h[T](): pass\n"
)


def _subtrees():
    """Each subtree with its ancestors, innermost first, as a visitor's cursor holds them."""
    cu = ParserVisitor(_SOURCE, None, None).visit_Module(ast.parse(_SOURCE))
    assignment, if_, method, multi_import, lambda_assignment, generic = cu.padding.statements
    parameter = method.element.padding.parameters.padding.elements[0]
    variable = parameter.element.padding.variables[0]
    names = multi_import.element.padding.names
    return {
        "assignment": (assignment.element, [assignment, cu]),
        "else": (if_.element.else_part, [if_.element, if_, cu]),
        "return type": (method.element.return_type_expression, [method.element, method, cu]),
        "parameter": (variable.element, [variable, parameter.element, parameter, method.element, method, cu]),
        "import": (names.padding.elements[0].element, [names.padding.elements[0], names, multi_import.element, multi_import, cu]),
        "lambda parameters": (lambda_assignment.element.assignment.parameters,
                              [lambda_assignment.element.assignment, lambda_assignment.element, lambda_assignment, cu]),
        "type parameters": (generic.element.padding.type_parameters, [generic.element, generic, cu]),
    }


def _cursor(ancestors) -> Cursor:
    cursor = Cursor(None, Cursor.ROOT_VALUE)
    for ancestor in reversed(ancestors):
        cursor = Cursor(cursor, ancestor)
    return cursor


@pytest.mark.parametrize("subtree, expected", [
    ("assignment", "x = 1"),
    ("else", "\nelif b:\n    pass"),
    ("return type", " -> int"),
    ("parameter", "a: int"),
    ("import", "n"),
    ("lambda parameters", " p, q=1"),
    ("type parameters", "[T]"),
])
def test_subtree_under_its_cursor(subtree, expected):
    tree, ancestors = _subtrees()[subtree]

    assert PythonPrinter().print(tree, cursor=_cursor(ancestors)) == expected


@pytest.mark.parametrize("subtree, expected", [
    ("assignment", "x := 1"),
    ("else", "\nelse:if b:\n    pass"),
    ("return type", " : int"),
    ("parameter", "a"),
    ("import", "importn"),
    ("lambda parameters", " p, q=1"),
    ("type parameters", "[T]"),
])
def test_subtree_without_ancestors(subtree, expected):
    # with nothing above it, a node cannot tell which of its forms its file had
    tree, _ = _subtrees()[subtree]

    assert PythonPrinter().print(tree) == expected


def _finding_themselves_in_their_parent():
    """Subtrees whose printing depends on which child of their parent they are."""
    cu = ParserVisitor(_SOURCE, None, None).visit_Module(ast.parse(_SOURCE))
    _, if_, method = cu.padding.statements[:3]

    parameter = method.element.padding.parameters.padding.elements[0].element
    variable = parameter.padding.variables[0]
    spaced = parameter.padding.replace(_variables=[variable.replace(after=Space([], ' '))])

    assignment = cu.padding.statements[0].element.replace(_prefix=Space([], ' '))
    inline = if_.element.padding.replace(_then_part=if_.element.padding.then_part.replace(element=assignment))
    return {
        "parameter": (variable.element, [spaced]),
        "assignment": (assignment, [inline]),
    }


@pytest.mark.parametrize("subtree, expected", [
    ("parameter", "a : int"),
    ("assignment", " x = 1"),
])
def test_ancestors_fetched_apart_from_the_subtree(subtree, expected):
    # over RPC each ancestor arrives as an object of its own, so the subtree is not the instance its parent holds
    tree, ancestors = _finding_themselves_in_their_parent()[subtree]
    apart = [copy.deepcopy(ancestor) for ancestor in ancestors]
    assert all(ancestor is not original for ancestor, original in zip(apart, ancestors))

    assert PythonPrinter().print(tree, cursor=_cursor(apart)) == expected


def test_handle_print_rebuilds_the_cursor(monkeypatch):
    from rewrite.rpc import server

    tree, ancestors = _subtrees()["assignment"]
    # the host's chain ends in its root sentinel, which is no tree
    objects = {str(uuid4()): o for o in [tree] + ancestors + ["root"]}
    ids = list(objects)
    monkeypatch.setattr(server, "get_object_from_java", lambda obj_id, *a, **k: objects[obj_id])

    request = {"treeId": ids[0], "sourceFileType": "org.openrewrite.python.tree.Py$CompilationUnit"}
    assert server.handle_print({**request, "cursor": ids[1:]}) == "x = 1"


@pytest.mark.parametrize("cursor", [{}, {"cursor": None}, {"cursor": []}], ids=["absent", "null", "empty"])
def test_handle_print_without_a_cursor(monkeypatch, cursor):
    from rewrite.rpc import server

    tree, _ = _subtrees()["assignment"]
    monkeypatch.setattr(server, "get_object_from_java", lambda obj_id, *a, **k: tree)

    request = {"treeId": str(uuid4()), "sourceFileType": "org.openrewrite.python.tree.Py$CompilationUnit"}
    assert server.handle_print({**request, **cursor}) == "x := 1"
