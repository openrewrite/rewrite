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

"""Direct parse-print round-trip tests for the Py2 parser visitor.

Each test parses a Python 2 source string with :class:`Py2ParserVisitor`,
runs the result through :class:`PythonPrinter`, and asserts byte-for-byte
equality with the original input — the same invariant that
``print_equals_input`` enforces inside the RPC pipeline. Tests here run
without RPC, so a printer regression fails fast and locally.
"""

import pytest

from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _round_trip(src: str) -> str:
    cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
    return PythonPrinter().print(cu)


# ---------------------------------------------------------------------------
# Each test asserts byte-for-byte round-trip on a representative idiom.
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("src", [
    # bare statements and expressions
    "x = 1\n",
    "x = 1 + 2 * 3\n",
    "x = a == b and c != d\n",
    "x += 1\n",
    "a = b = 1\n",

    # Py2-specific forms
    "print \"hello\", x\n",
    "print >> sys.stderr, \"err\"\n",
    "raise E, v, tb\n",

    # comments
    "# top-line comment\nx = 1\n",

    # control flow
    "if x:\n    pass\nelse:\n    pass\n",
    "if x:\n    a\nelif y:\n    b\nelse:\n    c\n",
    "for i in xs:\n    pass\n",
    "while x:\n    pass\n",

    # try / with
    "try:\n    a\nexcept:\n    b\n",
    "try:\n    a\nexcept E, e:\n    b\n",
    "try:\n    a\nexcept E as e:\n    b\n",
    "try:\n    a\nfinally:\n    c\n",
    "with foo as f:\n    pass\n",
    "with foo as f, bar as b:\n    pass\n",

    # imports
    "import os\n",
    "import os.path\n",
    "from os import path\n",
    "from os.path import join\n",
    "from os import path, environ\n",
    "from . import foo\n",

    # functions and classes
    "def f():\n    pass\n",
    "def f(a, b=1):\n    return a + b\n",
    "def f(*args, **kw):\n    return args\n",
    "@d\ndef f():\n    pass\n",
    "class Foo:\n    pass\n",
    "class Foo(Base):\n    def m(self):\n        return self.x\n",

    # collections and comprehensions
    "x = [1, 2, 3]\n",
    "x = {1: 2, 3: 4}\n",
    "x = (1, 2, 3)\n",
    "x = [i for i in xs if i > 0]\n",

    # calls
    "x = obj.method(a, b)\n",
    "x = f(a=1, b=2)\n",
    "x = f(*args, **kw)\n",
])
def test_round_trip(src):
    assert _round_trip(src) == src


def test_comprehensive_round_trip():
    """Full Py2 program round-trips byte-for-byte."""
    src = (
        "import os\n"
        "from os.path import join, dirname\n"
        "\n"
        "\n"
        "def greet(name, greeting=\"hello\"):\n"
        "    if name:\n"
        "        print greeting, name\n"
        "    else:\n"
        "        print >> sys.stderr, \"missing name\"\n"
        "    return greeting + name\n"
        "\n"
        "\n"
        "class Worker(Base):\n"
        "    def run(self, items):\n"
        "        results = []\n"
        "        for item in items:\n"
        "            try:\n"
        "                results.append(self.process(item))\n"
        "            except ValueError, e:\n"
        "                self.log(e)\n"
        "        return results\n"
    )
    assert _round_trip(src) == src
