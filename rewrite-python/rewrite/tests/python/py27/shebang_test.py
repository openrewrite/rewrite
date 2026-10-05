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

"""The parso-based Py2 parser models a leading ``#!`` line as a Shebang.

Mirrors the Python 3 parser: the shebang becomes ``statements[0]`` and the
whole source round-trips byte-for-byte through the printer.
"""

import pytest

from rewrite.python import Shebang
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.printer import PythonPrinter


def _parse(src):
    return Py2ParserVisitor(src, "<test>", "2.7").parse()


@pytest.mark.parametrize("src", [
    "#!/usr/bin/env python2\nprint 'hi'\n",
    "#!/usr/bin/env python2\n\nx = 1\n",
    "#!/usr/bin/env python2\n# a comment\nx = 1\n",
    "#!/usr/bin/env python2\n",
    "#!/usr/bin/env python2\r\nx = 1\r\n",
])
def test_shebang_is_first_statement_and_round_trips(src):
    cu = _parse(src)
    first = cu.statements[0]
    assert isinstance(first, Shebang)
    assert first.text == "#!/usr/bin/env python2"
    assert PythonPrinter().print(cu) == src


def test_leading_comment_is_not_a_shebang():
    cu = _parse("# not a shebang\nx = 1\n")
    assert not isinstance(cu.statements[0], Shebang)
