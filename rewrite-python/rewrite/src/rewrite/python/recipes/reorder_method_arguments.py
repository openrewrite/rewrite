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
ReorderMethodArguments recipe for Python that delegates to Java's ReorderMethodArguments.

This recipe reorders arguments in method invocations matching a pattern.
"""
from dataclasses import field
from importlib.metadata import metadata
from typing import Any, List, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.rpc.java_recipe import prepare_java_recipe, JavaRecipeVisitor, PreparedJavaRecipe


class ReorderMethodArguments(Recipe):
    """
    Reorder arguments in method invocations matching a pattern.

    This recipe delegates to Java's org.openrewrite.java.ReorderMethodArguments recipe.

    Example:
        ReorderMethodArguments(
            method_pattern="datetime.datetime replace(..)",
            new_parameter_names=["year", "month", "day"],
            old_parameter_names=["day", "month", "year"]
        )
    """

    method_pattern: str = field(metadata=option(
        display_name="Method pattern",
        description="A method pattern that matches method invocations to reorder.",
        example="datetime.datetime replace(..)"
    ))

    new_parameter_names: List[str] = field(metadata=option(
        display_name="New parameter names",
        description="The parameter names in the desired order.",
        example='["year", "month", "day"]'
    ))

    old_parameter_names: Optional[List[str]] = field(metadata=option(
        display_name="Old parameter names",
        description="The current parameter names (optional if using positional reordering).",
        required=False
    ))

    def __init__(
            self,
            method_pattern: str,
            new_parameter_names: List[str],
            old_parameter_names: Optional[List[str]] = None
    ):
        self.method_pattern = method_pattern
        self.new_parameter_names = new_parameter_names
        self.old_parameter_names = old_parameter_names
        self._prepared_recipe: Optional[PreparedJavaRecipe] = None

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ReorderMethodArguments"

    @property
    def display_name(self) -> str:
        return "Reorder method arguments"

    @property
    def description(self) -> str:
        return "Reorder arguments in method invocations matching a pattern."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        if self._prepared_recipe is None:
            options = {
                "methodPattern": self.method_pattern,
                "newParameterNames": self.new_parameter_names
            }
            if self.old_parameter_names:
                options["oldParameterNames"] = self.old_parameter_names

            self._prepared_recipe = prepare_java_recipe(
                "org.openrewrite.java.ReorderMethodArguments",
                options
            )

        return JavaRecipeVisitor(self._prepared_recipe)
