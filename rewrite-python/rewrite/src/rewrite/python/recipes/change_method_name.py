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
ChangeMethodName recipe for Python that delegates to Java's ChangeMethodName.

This recipe renames method invocations matching a pattern.
"""
from dataclasses import field
from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.rpc.java_recipe import prepare_java_recipe, JavaRecipeVisitor, PreparedJavaRecipe


@categorize(Python)
class ChangeMethodName(Recipe):
    """
    Rename method invocations matching a pattern.

    This recipe delegates to Java's org.openrewrite.java.ChangeMethodName recipe,
    which handles renaming method calls while preserving formatting and updating
    type attribution.

    Example:
        ChangeMethodName(
            method_pattern="datetime.datetime utcnow()",
            new_method_name="now"
        )
    """

    method_pattern: str = field(metadata=option(
        display_name="Method pattern",
        description="A method pattern that matches method invocations to rename.",
        example="datetime.datetime utcnow()"
    ))

    new_method_name: str = field(metadata=option(
        display_name="New method name",
        description="The new name for the method.",
        example="now"
    ))

    match_overrides: bool = field(metadata=option(
        display_name="Match overrides",
        description="When enabled, also match method overrides.",
        required=False
    ))

    ignore_definition: bool = field(metadata=option(
        display_name="Ignore definition",
        description="When enabled, the method definition itself will not be renamed.",
        required=False
    ))

    def __init__(
        self,
        method_pattern: str = "",
        new_method_name: str = "",
        match_overrides: bool = False,
        ignore_definition: bool = False
    ):
        self.method_pattern = method_pattern
        self.new_method_name = new_method_name
        self.match_overrides = match_overrides
        self.ignore_definition = ignore_definition
        self._prepared_recipe: Optional[PreparedJavaRecipe] = None

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeMethodName"

    @property
    def display_name(self) -> str:
        return "Change method name"

    @property
    def description(self) -> str:
        return "Rename method invocations matching a pattern."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        if self._prepared_recipe is None:
            self._prepared_recipe = prepare_java_recipe(
                "org.openrewrite.java.ChangeMethodName",
                {
                    "methodPattern": self.method_pattern,
                    "newMethodName": self.new_method_name,
                    "matchOverrides": self.match_overrides,
                    "ignoreDefinition": self.ignore_definition
                }
            )

        return JavaRecipeVisitor(self._prepared_recipe)
