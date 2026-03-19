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

import ast
from pathlib import Path
from typing import List, Optional, Union


from rewrite.java import J
from rewrite.python.support_types import Py
from rewrite.python.printer import PythonPrinter, PythonJavaPrinter, PrintOutputCapture
from rewrite.python._parser_visitor import ParserVisitor
from rewrite import Tree


def _prettify_type(tree: Tree) -> str:
    cls = type(tree)
    module = cls.__module__
    if 'python' in module:
        return f"Py.{cls.__name__}"
    elif 'java' in module:
        return f"J.{cls.__name__}"
    return cls.__name__


class OutputNode:
    def __init__(self, element: Tree):
        self.element = element
        self.children: List[Union['OutputNode', str]] = []

    def add_child(self, child: Union['OutputNode', str]) -> None:
        self.children.append(child)

    def __str__(self) -> str:
        children_str = ', '.join(
            f"Text({child})" if isinstance(child, str) else str(child)
            for child in self.children
        )
        return f"{_prettify_type(self.element)}{{{children_str}}}"


class TreeStructurePrintOutputCapture(PrintOutputCapture):
    def __init__(self):
        super().__init__()
        self.root_nodes: List[OutputNode] = []
        self._node_stack: List[OutputNode] = []

    def start_node(self, element: Tree) -> None:
        node = OutputNode(element)
        if self._node_stack:
            self._node_stack[-1].add_child(node)
        else:
            self.root_nodes.append(node)
        self._node_stack.append(node)

    def end_node(self) -> None:
        self._node_stack.pop()

    def append(self, text: Optional[str]) -> 'TreeStructurePrintOutputCapture':
        if text and len(text) > 0:
            if self._node_stack:
                self._node_stack[-1].add_child(text)
            super().append(text)
        return self


class TreeCapturingPythonJavaPrinter(PythonJavaPrinter):
    def _before_syntax(self, tree: J, p: PrintOutputCapture) -> None:
        if isinstance(p, TreeStructurePrintOutputCapture):
            p.start_node(tree)
        super()._before_syntax(tree, p)

    def _after_syntax(self, tree: J, p: PrintOutputCapture) -> None:
        super()._after_syntax(tree, p)
        if isinstance(p, TreeStructurePrintOutputCapture):
            p.end_node()


class TreeCapturingPythonPrinter(PythonPrinter):
    def __init__(self):
        super().__init__()
        self._delegate = TreeCapturingPythonJavaPrinter(self)

    def _before_syntax(self, tree: Union[Py, J], p: PrintOutputCapture) -> None:
        if isinstance(p, TreeStructurePrintOutputCapture):
            p.start_node(tree)
        super()._before_syntax(tree, p)

    def _after_syntax(self, tree: Union[Py, J], p: PrintOutputCapture) -> None:
        super()._after_syntax(tree, p)
        if isinstance(p, TreeStructurePrintOutputCapture):
            p.end_node()


def _find_whitespace_violations(root_nodes: List[OutputNode]) -> List[str]:
    violations: List[str] = []

    def check_node(node: OutputNode) -> None:
        if node.children:
            first_child = node.children[0]
            if isinstance(first_child, OutputNode):
                if first_child.children:
                    grandchild = first_child.children[0]
                    if isinstance(grandchild, str) and grandchild.strip() == '' and len(grandchild) > 0:
                        parent_kind = _prettify_type(node.element)
                        child_kind = _prettify_type(first_child.element)
                        violations.append(
                            f"{parent_kind} has child {child_kind} starting with whitespace "
                            f"|{grandchild}|. The whitespace should rather be attached to {parent_kind}."
                        )
        for child in node.children:
            if isinstance(child, OutputNode):
                check_node(child)

    for node in root_nodes:
        check_node(node)

    return violations


def _parse_python(source: str) -> Tree:
    source_path = Path("test.py")
    visitor = ParserVisitor(source, None, None)
    tree = ast.parse(source)
    cu = visitor.visit_Module(tree)
    return cu.replace(source_path=source_path)


def test_simple_assignment():
    # given
    source = "x = 1"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_function_definition():
    # given
    source = "def foo(x, y):\n    return x + y"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_dict_literal():
    # given
    source = "d = {'x': 1, 'y': 2}"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_class_definition():
    # given
    source = "class Foo:\n    def bar(self):\n        pass"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_import_statement():
    # given
    source = "from os.path import join"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_list_comprehension():
    # given
    source = "result = [x * 2 for x in range(10) if x > 3]"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_lambda_expression():
    # given
    source = "f = lambda x, y: x + y"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_multiline_function():
    # given
    source = "def greet(name):\n    greeting = 'Hello, ' + name\n    return greeting"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_try_except():
    # given
    source = "try:\n    x = 1\nexcept ValueError:\n    x = 0"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_for_loop():
    # given
    source = "for i in range(10):\n    print(i)"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []


def test_decorator():
    # given
    source = "@staticmethod\ndef foo():\n    pass"
    cu = _parse_python(source)
    capture = TreeStructurePrintOutputCapture()
    printer = TreeCapturingPythonPrinter()

    # when
    printer.print(cu, capture)

    # then
    assert capture.out == source
    violations = _find_whitespace_violations(capture.root_nodes)
    assert violations == []
