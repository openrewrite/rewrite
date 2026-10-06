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

"""Recipe to change Python imports from one module/name to another."""

from dataclasses import dataclass, field
from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.java import J
from rewrite.marketplace import Python
from rewrite.python.binding import maybe_rebind
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.recipe import option

_Imports = [*Python, CategoryDescriptor(display_name="Imports")]

@categorize(_Imports)
@dataclass
class ChangeImport(Recipe):
    """
    Change a Python import from one module/name to another.

    This recipe is useful for:
    - Library migrations (e.g., moving from `urllib2` to `urllib.request`)
    - Module restructuring
    - Renaming imported members

    Examples:
        # Change: from collections import Mapping -> from collections.abc import Mapping
        ChangeImport(
            old_module="collections",
            old_name="Mapping",
            new_module="collections.abc",
            new_name="Mapping"
        )

        # Change: import urllib2 -> import urllib.request as urllib2
        ChangeImport(
            old_module="urllib2",
            new_module="urllib.request",
            new_alias="urllib2"
        )

        # Change: from os.path import join -> from pathlib import Path
        ChangeImport(
            old_module="os.path",
            old_name="join",
            new_module="pathlib",
            new_name="Path"
        )
    """

    old_module: str = field(default="", metadata=option(
        display_name="Old module",
        description="The module to change imports from",
        example="collections"
    ))

    old_name: Optional[str] = field(default=None, metadata=option(
        display_name="Old name",
        description="The name to change (for 'from X import name' style). Leave empty for direct imports.",
        example="Mapping",
        required=False
    ))

    new_module: str = field(default="", metadata=option(
        display_name="New module",
        description="The module to change imports to",
        example="collections.abc"
    ))

    new_name: Optional[str] = field(default=None, metadata=option(
        display_name="New name",
        description="The new name. If not specified, uses the old name.",
        example="Mapping",
        required=False
    ))

    new_alias: Optional[str] = field(default=None, metadata=option(
        display_name="New alias",
        description="Optional alias for the new import",
        required=False
    ))

    new_declaring_module: Optional[str] = field(default=None, metadata=option(
        display_name="New declaring module",
        description="The module that defines the new member, where the new module re-exports it "
                    "from another. Type attribution names the member after it.",
        example="httpx._client",
        required=False
    ))

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeImport"

    @property
    def display_name(self) -> str:
        return "Change import"

    @property
    def description(self) -> str:
        return "Change a Python import from one module/name to another, updating all type attributions."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        recipe = self

        class ChangeImportVisitor(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                maybe_rebind(self, recipe.old_module, recipe.new_module,
                             from_member=recipe.old_name, to_member=recipe.new_name or recipe.old_name,
                             alias=recipe.new_alias, declared_in=recipe.new_declaring_module)
                return cu

        if not self.old_module:
            return ChangeImportVisitor()
        # Gate on the as-written import: a file can only contain `import old_module`
        # or `from old_module import ...` if it imports old_module, so this is a
        # correct superset. uses_import (not uses_type) because the type checker
        # canonicalizes aliases and drops removed symbols, both of which would make
        # a type-based gate skip files this recipe must change.
        from rewrite import Preconditions
        from rewrite.python.preconditions import uses_import
        return Preconditions.check(uses_import(self.old_module), ChangeImportVisitor())
