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


"""Compare a recipe's result with a parse of its printed text, node kind by node kind."""

import ast
from collections.abc import Callable

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor
from rewrite.test import dedent


def _kinds(tree) -> list[str]:
    kinds: list[str] = []

    class _Collector(PythonVisitor):
        def visit(self, t, p, parent=None):
            if t is not None:
                kinds.append(type(t).__name__)
            return super().visit(t, p, parent)

    _Collector().visit(tree, None)
    return kinds


def parse_shape(expected: str) -> Callable:
    """An ``after_recipe`` hook asserting the result has the node kinds a parse of ``expected`` has."""
    source = dedent(expected)

    def check(result) -> None:
        actual = _kinds(result)
        expected_kinds = _kinds(ParserVisitor(source, "expected.py", None).visit(ast.parse(source)))
        assert actual == expected_kinds, f"result {actual}\nreparse {expected_kinds}"

    return check
