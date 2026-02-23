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
ChangeType recipe for Python that delegates to Java's ChangeType.

This recipe changes type references from one fully qualified name to another,
handling all the complexities of type attribution and imports.
"""
from dataclasses import field
from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.rpc.java_recipe import prepare_java_recipe, JavaRecipeVisitor, PreparedJavaRecipe


class ChangeType(Recipe):
    """
    Change a type reference from one fully qualified name to another.

    This recipe delegates to Java's org.openrewrite.java.ChangeType recipe,
    which handles all the complexities of:
    - Finding type references in code
    - Updating imports
    - Updating type attribution

    Example:
        ChangeType(
            old_fully_qualified_type_name="ast.Num",
            new_fully_qualified_type_name="ast.Constant"
        )
    """

    old_fully_qualified_type_name: str = field(metadata=option(
        display_name="Old fully-qualified type name",
        description="Fully-qualified class name of the original type.",
        example="ast.Num"
    ))

    new_fully_qualified_type_name: str = field(metadata=option(
        display_name="New fully-qualified type name",
        description="Fully-qualified class name of the replacement type.",
        example="ast.Constant"
    ))

    ignore_definition: bool = field(metadata=option(
        display_name="Ignore type definition",
        description="When set to True, the definition of the old type will be left untouched. "
                    "This is useful when you're renaming the type itself.",
        required=False
    ))

    def __init__(
        self,
        old_fully_qualified_type_name: str,
        new_fully_qualified_type_name: str,
        ignore_definition: bool = False
    ):
        self.old_fully_qualified_type_name = old_fully_qualified_type_name
        self.new_fully_qualified_type_name = new_fully_qualified_type_name
        self.ignore_definition = ignore_definition
        self._prepared_recipe: Optional[PreparedJavaRecipe] = None

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeType"

    @property
    def display_name(self) -> str:
        return "Change type"

    @property
    def description(self) -> str:
        return "Change a type reference from one fully qualified name to another."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        # Prepare the Java recipe if not already done
        if self._prepared_recipe is None:
            self._prepared_recipe = prepare_java_recipe(
                "org.openrewrite.java.ChangeType",
                {
                    "oldFullyQualifiedTypeName": self.old_fully_qualified_type_name,
                    "newFullyQualifiedTypeName": self.new_fully_qualified_type_name,
                    "ignoreDefinition": self.ignore_definition
                }
            )

        return JavaRecipeVisitor(self._prepared_recipe)
