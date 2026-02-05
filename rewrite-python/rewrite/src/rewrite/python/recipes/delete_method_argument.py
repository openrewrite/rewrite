# Copyright 2025 the original author or authors.
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

"""
DeleteMethodArgument recipe for Python that delegates to Java's DeleteMethodArgument.

This recipe removes an argument from method invocations matching a pattern.
"""

from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.rpc.java_recipe import prepare_java_recipe, JavaRecipeVisitor, PreparedJavaRecipe


class DeleteMethodArgument(Recipe):
    """
    Remove an argument from method invocations matching a pattern.

    This recipe delegates to Java's org.openrewrite.java.DeleteMethodArgument recipe.

    Example:
        DeleteMethodArgument(
            method_pattern="datetime.datetime fromtimestamp(..)",
            argument_index=1
        )
    """

    method_pattern: str = option(
        display_name="Method pattern",
        description="A method pattern that matches method invocations to modify.",
        example="datetime.datetime fromtimestamp(..)"
    )

    argument_index: int = option(
        display_name="Argument index",
        description="The zero-based index of the argument to remove.",
        example="1"
    )

    def __init__(
        self,
        method_pattern: str,
        argument_index: int
    ):
        self.method_pattern = method_pattern
        self.argument_index = argument_index
        self._prepared_recipe: Optional[PreparedJavaRecipe] = None

    @property
    def name(self) -> str:
        return "org.openrewrite.python.DeleteMethodArgument"

    @property
    def display_name(self) -> str:
        return "Delete method argument"

    @property
    def description(self) -> str:
        return "Remove an argument from method invocations matching a pattern."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        if self._prepared_recipe is None:
            self._prepared_recipe = prepare_java_recipe(
                "org.openrewrite.java.DeleteMethodArgument",
                {
                    "methodPattern": self.method_pattern,
                    "argumentIndex": self.argument_index
                }
            )

        return JavaRecipeVisitor(self._prepared_recipe)
