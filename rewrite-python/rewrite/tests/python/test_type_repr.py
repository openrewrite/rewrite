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
"""How much a type-attributed node's `repr` prints."""
from rewrite.java import JavaType
from rewrite.java.tree import Identifier
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec
from rewrite.test.spec import python

_SOURCE = '''\
from typing import List

x: List[int] = []
'''


def _annotated_identifier() -> Identifier:
    captured = []

    def capture(cu):
        class Capture(PythonVisitor):
            def visit_identifier(self, ident, p):
                if ident.simple_name == 'x' and not captured:
                    captured.append(ident)
                return ident

        Capture().visit(cu, None)
        return cu

    RecipeSpec(type_attribution=True).rewrite_run(python(_SOURCE, before_recipe=capture))
    return captured[0]


def test_java_type_repr_renders_an_identity_not_a_graph():
    # Ordered first: a regression fails here in microseconds, where the attributed
    # node below would exhaust memory before failing.
    cls = JavaType.Class(_fully_qualified_name='a.B')
    assert repr(cls) == "JavaType.Class('a.B')"
    assert repr(JavaType.Parameterized(_type=cls)) == "JavaType.Parameterized('a.B')"
    assert repr(JavaType.Annotation(_type=cls)) == "JavaType.Annotation('a.B')"
    assert repr(JavaType.Unknown()) == 'JavaType.Unknown()'

    assert len(repr(_annotated_identifier())) < 2000
