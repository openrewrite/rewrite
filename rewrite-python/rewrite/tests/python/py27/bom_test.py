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

"""UTF-8 BOM handling for the Py2 parser visitor.

The visitor strips the BOM before handing the source to parso and records
the fact on ``CompilationUnit.charset_bom_marked`` so the parsed file
remains distinguishable from one without a BOM.
"""

from rewrite.python._py2_parser_visitor import Py2ParserVisitor


_BOM = "﻿"


def test_bom_is_recorded_on_cu():
    cu = Py2ParserVisitor(_BOM + "x = 1\n", "<test>", "2.7").parse()
    assert cu.charset_bom_marked is True


def test_no_bom_is_recorded_on_cu():
    cu = Py2ParserVisitor("x = 1\n", "<test>", "2.7").parse()
    assert cu.charset_bom_marked is False


def test_parse_succeeds_with_bom():
    # Body still parses to a real Assignment, not a placeholder.
    cu = Py2ParserVisitor(_BOM + "x = 1\n", "<test>", "2.7").parse()
    stmt = cu.statements[0]
    from rewrite.java import tree as j
    assert isinstance(stmt, j.Assignment)


def test_bom_with_py2_specific_syntax():
    cu = Py2ParserVisitor(_BOM + "print \"hi\"\n", "<test>", "2.7").parse()
    from rewrite.java import tree as j
    assert isinstance(cu.statements[0], j.MethodInvocation)
