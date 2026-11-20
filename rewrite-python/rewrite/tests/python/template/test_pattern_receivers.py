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

"""When a pattern's call receiver has to match the target's, on type-attributed sources."""

from typing import List

from rewrite import ExecutionContext, Recipe
from rewrite.python.template import Pattern, capture, pattern
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

x, y = capture('x'), capture('y')


def _matched(pat: Pattern, source: str) -> List[str]:
    """The names of the calls in ``source`` that ``pat`` matches."""
    matched: List[str] = []

    class Collect(Recipe):
        name = "test.Collect"
        display_name = "Collect"
        description = "Collect."

        def editor(self):
            class Visitor(PythonVisitor[ExecutionContext]):
                def visit_method_invocation(self, method, p):
                    method = super().visit_method_invocation(method, p)
                    if pat.match(method, self.cursor):
                        matched.append(method.name.simple_name)
                    return method

            return Visitor()

    RecipeSpec(recipe=Collect()).rewrite_run(python(source))
    return matched


def test_instance_call_compares_receiver():
    pat = pattern(f"items.append({x})", context=["items: list[int] = []"])
    assert _matched(pat, "others: list[int] = []\nothers.append(1)\n") == []
    assert _matched(pat, "items: list[int] = []\nitems.append(1)\n") == ['append']


def test_instance_call_on_module_attribute_compares_receiver():
    pat = pattern(f"sys.stdout.write({x})", context=["import sys"])
    assert _matched(pat, "import sys\nsys.stderr.write('a')\n") == []


def test_module_function_matches_any_spelling():
    assert _matched(pattern(f"os.path.join({x}, {y})", context=["import os.path"]),
                    "import os.path as p\np.join('a', 'b')\n") == ['join']
    assert _matched(pattern(f"join({x}, {y})", context=["from os.path import join"]),
                    "import os.path\nos.path.join('a', 'b')\n") == ['join']


def test_classmethod_matches_any_spelling():
    pat = pattern("datetime.datetime.now()", context=["import datetime"])
    assert _matched(pat, "from datetime import datetime\ndatetime.now()\n") == ['now']


def test_unresolved_module_function_matches_any_spelling():
    pat = pattern(f"notinstalled.get({x})", context=["import notinstalled"])
    assert _matched(pat, "import notinstalled as ni\nni.get(1)\n") == ['get']


def test_constructor_matches_any_spelling():
    pat = pattern(f"collections.OrderedDict({x})", context=["import collections"])
    assert _matched(pat, "from collections import OrderedDict\nOrderedDict(1)\n") == ['OrderedDict']
