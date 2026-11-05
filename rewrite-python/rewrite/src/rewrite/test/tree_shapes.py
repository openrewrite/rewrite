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

"""Find the nodes in a recipe's output that the parser builds differently from the text they print.

A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that parsed code never holds.
"""

from __future__ import annotations

import re
from typing import Any, List, Optional

from rewrite import Cursor, PrintOutputCapture, SourceFile, Tree
from rewrite.java import tree as j
from rewrite.java.support_types import J, JavaType, Space
from rewrite.python import tree as py
from rewrite.python.visitor import PythonVisitor

_MAX_PER_FILE = 5
_SNIPPET_LENGTH = 60
_WHITESPACE = re.compile(r"\s+")


def problems(after: SourceFile, reparsed: SourceFile) -> List[str]:
    """Each node of ``after`` that ``reparsed``, a fresh parse of the text ``after`` prints, holds differently."""
    found: List[str] = []
    _compare(_skeleton(after), _skeleton(reparsed), found)
    return found


class _Node:
    """A node of a tree, with the nodes it holds and the cursor that prints it."""

    def __init__(self, tree: Tree, cursor: Cursor):
        self.tree = tree
        self.cursor = cursor
        self.children: List[_Node] = []

    def __str__(self) -> str:
        try:
            tree = self.tree.replace(prefix=Space.EMPTY) if isinstance(self.tree, J) else self.tree
            printed = tree.print(self.cursor, PrintOutputCapture(0, PrintOutputCapture.MarkerPrinter.SANITIZED))
        except Exception:
            printed = "?"
        printed = _WHITESPACE.sub(" ", printed).strip()
        return f"`{printed[:_SNIPPET_LENGTH]}...`" if len(printed) > _SNIPPET_LENGTH else f"`{printed}`"


class _Skeleton(PythonVisitor[Any]):
    """Collects the nodes of a tree, leaving out the empty elements the parser adds or leaves out without the text
    changing."""

    def __init__(self) -> None:
        self.root: Optional[_Node] = None
        self._path: List[_Node] = []

    def pre_visit(self, tree, p):
        if isinstance(tree, j.Empty):
            self.stop_after_pre_visit()
            return tree
        node = _Node(tree, self.cursor.parent_or_throw)
        if self._path:
            self._path[-1].children.append(node)
        else:
            self.root = node
        self._path.append(node)
        return tree

    def post_visit(self, tree, p):
        self._path.pop()
        return tree


def _skeleton(source_file: SourceFile) -> _Node:
    skeleton = _Skeleton()
    skeleton.visit(source_file, None)
    return skeleton.root if skeleton.root is not None else _Node(source_file, Cursor(None, Cursor.ROOT_VALUE))


def _compare(node: _Node, parsed: _Node, found: List[str]) -> None:
    if len(found) >= _MAX_PER_FILE:
        return
    if type(node.tree) is not type(parsed.tree):
        found.append(f"{node} is a {_kind(node.tree)} where the parser makes a {_kind(parsed.tree)}")
        return
    value, parsed_value = _value(node.tree), _value(parsed.tree)
    if value != parsed_value:
        found.append(f"{node} has {value} where the parser makes {parsed_value}")
    if len(node.children) != len(parsed.children):
        found.append(f"{node} holds {_kinds(node.children)} where the parser makes {_kinds(parsed.children)}")
        return
    for child, parsed_child in zip(node.children, parsed.children):
        _compare(child, parsed_child, found)


def _value(tree: Tree) -> Optional[str]:
    """What tells ``tree`` apart from another node of its class with the same children, or None when nothing does."""
    if isinstance(tree, j.Identifier):
        return f"the name `{tree.simple_name}`"
    if isinstance(tree, j.Literal):
        value = tree.value
        primitive = tree.type.name if isinstance(tree.type, JavaType.Primitive) else None
        described = "the value None" if value is None else f"the {type(value).__name__} value {_quoted(value)}"
        return f"{described} of type {primitive}"
    if isinstance(tree, j.Modifier):
        return f"the modifier {tree.keyword or tree.type.name}"
    if isinstance(tree, (j.Binary, j.Unary, j.AssignmentOperation, py.Binary)):
        return f"the operator {tree.operator.name}"
    return None


def _quoted(value: Any) -> str:
    printed = repr(value)
    return printed[:40] + "..." if len(printed) > 40 else printed


def _kind(tree: Tree) -> str:
    module = type(tree).__module__
    prefix = "J." if module == j.__name__ else "Py." if module == py.__name__ else ""
    return prefix + type(tree).__name__


def _kinds(nodes: List[_Node]) -> str:
    return ", ".join(_kind(node.tree) for node in nodes) if nodes else "nothing"
