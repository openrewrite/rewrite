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
AddLiteralMethodArgument recipe for Python that delegates to Java's AddLiteralMethodArgument.

This recipe adds a literal argument to method invocations matching a pattern.
"""
from dataclasses import field
from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.rpc.java_recipe import prepare_java_recipe, JavaRecipeVisitor, PreparedJavaRecipe


@categorize(Python)
class AddLiteralMethodArgument(Recipe):
    """
    Add a literal argument to method invocations matching a pattern.

    This recipe delegates to Java's org.openrewrite.java.AddLiteralMethodArgument recipe.

    Example:
        AddLiteralMethodArgument(
            method_pattern="datetime.datetime now()",
            argument_index=0,
            literal="datetime.UTC"
        )
    """

    method_pattern: str = field(metadata=option(
        display_name="Method pattern",
        description="A method pattern that matches method invocations to modify.",
        example="datetime.datetime now()"
    ))

    argument_index: int = field(metadata=option(
        display_name="Argument index",
        description="The zero-based position where to insert the argument.",
        example="0"
    ))

    literal: str = field(metadata=option(
        display_name="Literal",
        description="The literal value to add as an argument.",
        example="datetime.UTC"
    ))

    def __init__(
        self,
        method_pattern: str = "",
        argument_index: int = 0,
        literal: str = ""
    ):
        self.method_pattern = method_pattern
        self.argument_index = argument_index
        self.literal = literal
        self._prepared_recipe: Optional[PreparedJavaRecipe] = None

    @property
    def name(self) -> str:
        return "org.openrewrite.python.AddLiteralMethodArgument"

    @property
    def display_name(self) -> str:
        return "Add literal method argument"

    @property
    def description(self) -> str:
        return "Add a literal argument to method invocations matching a pattern."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        if self._prepared_recipe is None:
            self._prepared_recipe = prepare_java_recipe(
                "org.openrewrite.java.AddLiteralMethodArgument",
                {
                    "methodPattern": self.method_pattern,
                    "argumentIndex": self.argument_index,
                    "literal": self.literal
                }
            )

        return JavaRecipeVisitor(self._prepared_recipe)
