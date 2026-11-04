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
import dataclasses
from typing import Any, List, Optional, Set

from rewrite.java.support_types import J, JavaType
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor
from rewrite.test import dedent

_SOURCE = dedent('''
    import os
    from typing import Any
    type Pair = tuple[int, int]
    class C(Exception):
        x: int = 1
    def f(a: int | None, /, *args: Any, b: "C" = None, **kwargs) -> list[int]:
        s = t = [i for i in args if not i]
        d = {"k": a, **kwargs}
        g = lambda v: v if v else -v
        h = f"{a!r:>3}"
        os.path.join("a", "b")
        f(1, b=None)
        x = s[0] + 1.5
        x += 1
        try:
            pass
        except (ValueError, TypeError) as e:
            raise RuntimeError() from e
        match a:
            case C(x=1) | [1, *rest]:
                pass
        return {1, 2}
    async def co():
        await co()
        yield from ()
    ''')


def _type_slots(tree: Any) -> List[str]:
    return [f.name for f in dataclasses.fields(tree) if 'JavaType' in str(f.type)]


def test_every_type_slot_is_handed_to_visit_type():
    seen = JavaType.Unknown()
    cu = ParserVisitor(_SOURCE, 'm.py', None).visit_Module(ast.parse(_SOURCE))

    class Retype(PythonVisitor[None]):
        def visit_type(self, java_type: Optional[JavaType], p: None) -> Optional[JavaType]:
            return seen

    unvisited: Set[str] = set()
    kinds: Set[str] = set()

    class Check(PythonVisitor[None]):
        def post_visit(self, tree: Any, p: None) -> Any:
            if isinstance(tree, J):
                for slot in _type_slots(tree):
                    kinds.add(type(tree).__name__)
                    if getattr(tree, slot) is not seen:
                        unvisited.add(f'{type(tree).__name__}.{slot}')
            return tree

    Check().visit(Retype().visit(cu, None), None)
    assert unvisited == set()
    # The source has to reach the node kinds that carry types for the check to mean anything.
    assert len(kinds) >= 25, sorted(kinds)
