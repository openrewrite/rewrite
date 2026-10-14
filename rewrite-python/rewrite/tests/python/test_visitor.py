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
import ast
import dataclasses

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor
from rewrite.test import dedent
from rewrite.tree import Tree


def test_visit_reaches_every_node():
    source = dedent("""
        a = [v for v in vs if v if v > 1]
        match a:
            case [1, *rest] | {"k": v} if rest:
                pass
    """)
    cu = ParserVisitor(source, file_path=None, ty_client=None).visit(ast.parse(source))

    # Walks the fields, so a node the visitor skips is still counted
    walked = {}

    def walk(node):
        if isinstance(node, Tree):
            walked[node.id] = type(node).__name__
        if dataclasses.is_dataclass(node) and not isinstance(node, type):
            for field in dataclasses.fields(node):
                walk(getattr(node, field.name))
        elif isinstance(node, (list, tuple)):
            for element in node:
                walk(element)
    walk(cu)

    class Collect(PythonVisitor[None]):
        def __init__(self):
            super().__init__()
            self.visited = set()

        def post_visit(self, tree, p):
            self.visited.add(tree.id)
            return tree
    collector = Collect()
    collector.visit(cu, None)

    assert sorted(name for i, name in walked.items() if i not in collector.visited) == []
