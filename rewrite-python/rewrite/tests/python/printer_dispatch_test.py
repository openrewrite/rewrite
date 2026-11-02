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

"""Each node is dispatched to the visit method of the printer instance it is handed, so a
subclass's override of one is used."""

import ast

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonJavaPrinter, PythonPrinter


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source))


class _MarkingJavaPrinter(PythonJavaPrinter):
    def visit_identifier(self, ident, p):
        p.append("<")
        result = super().visit_identifier(ident, p)
        p.append(">")
        return result


class _MarkingPrinter(PythonPrinter):
    def __init__(self):
        super().__init__()
        self._delegate = _MarkingJavaPrinter(self)

    def visit_pass(self, pass_, p):
        p.append("!")
        return super().visit_pass(pass_, p)


def test_subclass_overrides_of_visit_methods_are_dispatched_to():
    cu = _parse("x = 1\npass\n")
    assert PythonPrinter().print(cu) == "x = 1\npass\n"
    assert _MarkingPrinter().print(cu) == "<x> = 1!\npass\n"
