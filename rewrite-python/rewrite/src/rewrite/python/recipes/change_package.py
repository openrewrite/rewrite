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
ChangePackage recipe for Python that delegates to Java's ChangePackage.

This recipe changes package/module references from one name to another.
"""
from dataclasses import field

from rewrite import Recipe, option
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.rpc.rpc_recipe import RpcRecipe


@categorize(Python)
class ChangePackage(Recipe):
    """
    Change package/module references from one name to another.

    This recipe delegates to Java's org.openrewrite.java.ChangePackage recipe,
    which handles renaming package references in imports and type usages.

    Example:
        ChangePackage(
            old_package_name="collections",
            new_package_name="collections.abc"
        )
    """

    old_package_name: str = field(metadata=option(
        display_name="Old package name",
        description="The package/module name to change from.",
        example="collections"
    ))

    new_package_name: str = field(metadata=option(
        display_name="New package name",
        description="The package/module name to change to.",
        example="collections.abc"
    ))

    recursive: bool = field(metadata=option(
        display_name="Recursive",
        description="When enabled, also rename subpackages.",
        required=False
    ))

    def __init__(
        self,
        old_package_name: str = "",
        new_package_name: str = "",
        recursive: bool = True
    ):
        self.old_package_name = old_package_name
        self.new_package_name = new_package_name
        self.recursive = recursive

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangePackage"

    @property
    def display_name(self) -> str:
        return "Change package"

    @property
    def description(self) -> str:
        return "Change package/module references from one name to another."

    def recipe_list(self) -> list[Recipe]:
        return [RpcRecipe(
            "org.openrewrite.java.ChangePackage",
            oldPackageName=self.old_package_name,
            newPackageName=self.new_package_name,
            recursive=self.recursive,
        )]
