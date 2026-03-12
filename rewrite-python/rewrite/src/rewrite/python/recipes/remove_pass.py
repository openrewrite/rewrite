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

from typing import Any, List, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.python.tree import CompilationUnit, ExpressionStatement, Pass
from rewrite.python.visitor import PythonVisitor
from rewrite.java import J
from rewrite.java.tree import Block, ClassDeclaration, Literal, MethodDeclaration


# Define category path locally to avoid circular imports
_Cleanup = [*Python, CategoryDescriptor(display_name="Cleanup")]


@categorize(_Cleanup)
class RemovePass(Recipe):
    """
    Remove redundant `pass` statements from Python code.

    This recipe removes `pass` statements only when they are redundant -
    i.e., when there are other executable statements in the same block.
    It will NOT remove `pass` when:
    - It's the only statement in a block
    - The only other statement is a docstring

    This ensures the code remains syntactically valid and intentionally
    empty functions/classes with docstrings retain their `pass`.
    """

    @property
    def name(self) -> str:
        return "org.openrewrite.python.RemovePass"

    @property
    def display_name(self) -> str:
        return "Remove redundant pass statements"

    @property
    def description(self) -> str:
        return "Remove redundant `pass` statements from Python code when there are other executable statements in the block."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_pass(self, pass_: Pass, p: ExecutionContext) -> Optional[J]:
                # Find the enclosing block or compilation unit
                block = self.cursor.first_enclosing(Block)
                if block is not None:
                    # Docstrings are only valid in class or method/function bodies
                    class_decl = self.cursor.first_enclosing(ClassDeclaration)
                    method_decl = self.cursor.first_enclosing(MethodDeclaration)
                    docstrings_valid = class_decl is not None or method_decl is not None

                    # Count executable statements (excluding pass and docstrings where valid)
                    executable_count = _count_executable_statements(block.statements, docstrings_valid)
                    # Only remove pass if there are executable statements
                    if executable_count > 0:
                        return None
                    return pass_

                # Check for module-level statements in CompilationUnit
                cu = self.cursor.first_enclosing(CompilationUnit)
                if cu is not None:
                    # Module-level docstrings are valid
                    executable_count = _count_executable_statements(cu.statements, docstrings_valid=True)
                    # Only remove pass if there are executable statements
                    if executable_count > 0:
                        return None

                return pass_

        return Visitor()


def _is_docstring(stmt: Any, index: int) -> bool:
    """
    Check if a statement could be a docstring.

    A docstring is a string literal expression statement that appears
    as the first statement. Note: The caller must verify this is in a
    valid docstring context (function, class, or module level).
    """
    if index != 0:
        return False
    if not isinstance(stmt, ExpressionStatement):
        return False
    expr = stmt.expression
    if not isinstance(expr, Literal):
        return False
    # Check if the literal value is a string
    return isinstance(expr.value, str)


def _count_executable_statements(statements: List[Any], docstrings_valid: bool) -> int:
    """
    Count statements that are neither pass nor docstrings.

    This excludes:
    - pass statements
    - docstrings (string literal as first statement, only if docstrings_valid is True)
    """
    count = 0
    for i, stmt in enumerate(statements):
        if isinstance(stmt, Pass):
            continue
        if docstrings_valid and _is_docstring(stmt, i):
            continue
        count += 1
    return count
