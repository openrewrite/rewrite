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

"""Tests for the indentation a format takes from the file it formats."""

import ast

from rewrite.python import IntelliJ
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.format.indentation import tabs_and_indents_style
from rewrite.style import NamedStyles


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source))


def test_a_tab_indented_file_indents_with_tabs():
    style = tabs_and_indents_style(_parse("class A:\n\tdef f(self):\n\t\tif x:\n\t\t\tpass\n\t\treturn 1\n"))

    assert style.use_tab_character
    assert style.tab_size == style.indent_size


def test_a_two_space_file_indents_by_two_spaces():
    style = tabs_and_indents_style(_parse("def f():\n  if x:\n    a()\n  else:\n    b()\n  return 1\n"))

    assert not style.use_tab_character
    assert style.indent_size == 2


def test_the_most_common_level_wins():
    style = tabs_and_indents_style(_parse(
        "def f():\n    a()\n    b()\n    if x:\n      c()\n"
        "def g():\n    d()\n"
    ))

    assert style.indent_size == 4


def test_a_file_without_indented_statements_indents_as_intellij_does():
    assert tabs_and_indents_style(_parse("x = 1\nif x: pass\n")) == IntelliJ.tabs_and_indents()


def test_a_configured_style_wins_over_the_files_indentation():
    configured = IntelliJ.tabs_and_indents().with_indent_size(3)
    cu = _parse("def f():\n\treturn 1\n")
    cu = cu.replace(markers=cu.markers.add(NamedStyles.build(configured)))

    assert tabs_and_indents_style(cu) == configured
