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

from typing import Any

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.python.tree import Pass
from rewrite.python.visitor import PythonVisitor
from rewrite.java import J

# Define category path locally to avoid circular imports
_Cleanup = [*Python, CategoryDescriptor(display_name="Cleanup")]


@categorize(_Cleanup)
class RemovePass(Recipe):
    """
    Remove `pass` statements from Python code.

    This recipe removes all `pass` statements. Note that this may make
    code syntactically invalid if the `pass` is the only statement in a
    block (e.g., empty function body).
    """

    @property
    def name(self) -> str:
        return "org.openrewrite.python.RemovePass"

    @property
    def display_name(self) -> str:
        return "Remove pass statements"

    @property
    def description(self) -> str:
        return "Remove `pass` statements from Python code."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_pass(self, pass_: Pass, ctx: ExecutionContext) -> J:
                return None

        return Visitor()
