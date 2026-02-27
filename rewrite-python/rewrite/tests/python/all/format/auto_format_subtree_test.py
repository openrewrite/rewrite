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

"""Tests for auto_format when applied to sub-trees with a cursor.

When a recipe calls auto_format(node, p, cursor=self.cursor) on a node
that is not the root CompilationUnit, the formatter must preserve the
node's own indentation level and only adjust its children.  Previously,
TabsAndIndentsVisitor.visit() called pre_visit() on the root element
during setup, which set indent_type=INDENT on the cursor and caused an
extra indentation level to be added to the node's own prefix.
"""

from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.java.tree import (
    Block,
    If as JIf,
)
from rewrite.python import tree as py_tree
from rewrite.python.format import auto_format
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


class _AutoFormatIfRecipe(Recipe):
    """Calls auto_format on the If node to test sub-tree formatting."""

    @property
    def name(self) -> str:
        return "test.AutoFormatIf"

    @property
    def display_name(self) -> str:
        return "Test auto_format on If sub-tree"

    @property
    def description(self) -> str:
        return "Calls auto_format on an If node to verify indentation is preserved."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_if(
                self, if_stmt: JIf, p: ExecutionContext
            ) -> Optional[JIf]:
                if_stmt = super().visit_if(if_stmt, p)
                # Only process if-statements with a pass body
                if isinstance(if_stmt.then_part, Block):
                    stmts = if_stmt.then_part.statements
                    if len(stmts) == 1 and isinstance(stmts[0], py_tree.Pass):
                        return auto_format(if_stmt, p, cursor=self.cursor)
                return if_stmt

        return Visitor()


def test_auto_format_if_in_method_preserves_indent():
    """auto_format on an If inside a method must not add extra indentation.

    The If at 4-space indent should stay at 4-space indent, not shift to 8.
    """
    spec = RecipeSpec(recipe=_AutoFormatIfRecipe())
    spec.rewrite_run(
        python(
            "def resolve(self):\n"
            "    if cond:\n"
            "        pass\n",
        )
    )


def test_auto_format_if_in_nested_method_preserves_indent():
    """auto_format on a deeply nested If should preserve its indentation."""
    spec = RecipeSpec(recipe=_AutoFormatIfRecipe())
    spec.rewrite_run(
        python(
            "class Foo:\n"
            "    def resolve(self):\n"
            "        if cond:\n"
            "            pass\n",
        )
    )


def test_auto_format_if_at_top_level_preserves_indent():
    """auto_format on a top-level If should preserve zero indentation."""
    spec = RecipeSpec(recipe=_AutoFormatIfRecipe())
    spec.rewrite_run(
        python(
            "if cond:\n"
            "    pass\n",
        )
    )
