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

"""Recipe to remove redundant pass statements from Python code."""

from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.python.tree import CompilationUnit, Pass
from rewrite.python.visitor import PythonVisitor
from rewrite.java import J
from rewrite.java.tree import Block

# Define category path locally to avoid circular imports
_Cleanup = [*Python, CategoryDescriptor(display_name="Cleanup")]


@categorize(_Cleanup)
class RemovePass(Recipe):
    """
    Remove redundant `pass` statements from Python code.

    This recipe removes `pass` statements only when they are redundant -
    i.e., when there are other statements in the same block. It will NOT
    remove `pass` when it's the only statement in a block, as that would
    make the code syntactically invalid.
    """

    @property
    def name(self) -> str:
        return "org.openrewrite.python.RemovePass"

    @property
    def display_name(self) -> str:
        return "Remove redundant pass statements"

    @property
    def description(self) -> str:
        return "Remove redundant `pass` statements from Python code when there are other statements in the block."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_pass(self, pass_: Pass, ctx: ExecutionContext) -> Optional[J]:
                # Find the enclosing block or compilation unit
                block = self.cursor.first_enclosing(Block)
                if block is not None:
                    # Count non-pass statements in the block
                    other_statements = sum(
                        1 for stmt in block.statements
                        if not isinstance(stmt, Pass)
                    )
                    # Only remove pass if there are other statements
                    if other_statements > 0:
                        return None
                    return pass_

                # Check for module-level statements in CompilationUnit
                cu = self.cursor.first_enclosing(CompilationUnit)
                if cu is not None:
                    # Count non-pass statements at module level
                    other_statements = sum(
                        1 for stmt in cu.statements
                        if not isinstance(stmt, Pass)
                    )
                    # Only remove pass if there are other statements
                    if other_statements > 0:
                        return None

                return pass_

        return Visitor()
