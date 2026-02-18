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
from rewrite.marketplace import Python
from rewrite.recipe import option
from rewrite.java import J
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor
from rewrite.python.add_import import AddImportOptions, maybe_add_import


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
        old_module = self.old_module
        old_name = self.old_name
        new_module = self.new_module
        new_name = self.new_name if self.new_name else old_name
        new_alias = self.new_alias

        class ChangeImportVisitor(PythonVisitor[ExecutionContext]):
            def __init__(self):
                super().__init__()
                self.has_old_import = False
                self.old_alias: Optional[str] = None

            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                # First pass: check if the old import exists
                self.has_old_import = False
                self.old_alias = None

                for stmt in cu.statements:
                    if isinstance(stmt, Import) and not isinstance(stmt, MultiImport):
                        alias = self._check_for_old_single_import(stmt)
                        if alias is not None:
                            self.has_old_import = True
                            self.old_alias = alias if alias != "" else None
                            break
                    elif isinstance(stmt, MultiImport):
                        alias = self._check_for_old_import(stmt)
                        if alias is not None:
                            self.has_old_import = True
                            self.old_alias = alias if alias != "" else None
                            break

                if not self.has_old_import:
                    return cu

                # Visit to transform imports
                result = super().visit_compilation_unit(cu, p)
                if not isinstance(result, CompilationUnit):
                    return result

                # Schedule adding the new import
                alias_to_use = new_alias or self.old_alias
                if new_name:
                    maybe_add_import(self, AddImportOptions(
                        module=new_module,
                        name=new_name,
                        alias=alias_to_use,
                        only_if_referenced=False
                    ))
                else:
                    maybe_add_import(self, AddImportOptions(
                        module=new_module,
                        alias=alias_to_use,
                        only_if_referenced=False
                    ))

                return result

            def visit_import(self, import_: Import, p: ExecutionContext) -> Optional[J]:  # ty: ignore[invalid-method-override]
                if not self.has_old_import or old_name:
                    return import_
                if self.cursor.first_enclosing(MultiImport):
                    return import_
                alias = self._check_for_old_single_import(import_)
                if alias is None:
                    return import_
                return None

            def visit_multi_import(self, multi: MultiImport, p: ExecutionContext) -> Optional[J]:  # ty: ignore[invalid-method-override]
                if not self.has_old_import:
                    return multi

                alias = self._check_for_old_import(multi)
                if alias is None:
                    return multi

                # Remove this import (or the specific name from it)
                if old_name:
                    # from X import name - remove specific name
                    return self._remove_name_from_import(multi, old_name)
                else:
                    # import X - remove entire import
                    return self._remove_module_from_import(multi, old_module)

            def _check_for_old_single_import(self, imp: Import) -> Optional[str]:
                """Check if a standalone J.Import matches the old import."""
                if old_name:
                    return None
                name = self._get_qualid_name(imp.qualid)
                if name == old_module:
                    return self._get_alias_name(imp) or ""
                return None

            def _check_for_old_import(self, multi: MultiImport) -> Optional[str]:
                """Check if this MultiImport matches the old import.

                Returns:
                    None if no match
                    "" if match with no alias
                    alias string if match with alias
                """
                if old_name:
                    # Looking for: from old_module import old_name [as alias]
                    if multi.from_ is None:
                        return None
                    from_name = self._get_name_string(multi.from_)
                    if from_name != old_module:
                        return None
                    for imp in multi.names:
                        name = self._get_qualid_name(imp.qualid)
                        if name == old_name:
                            return self._get_alias_name(imp) or ""
                else:
                    # Looking for: import old_module [as alias]
                    if multi.from_ is not None:
                        return None
                    for imp in multi.names:
                        name = self._get_qualid_name(imp.qualid)
                        if name == old_module:
                            return self._get_alias_name(imp) or ""
                return None

            def _remove_name_from_import(self, multi: MultiImport, name_to_remove: str) -> Optional[J]:
                """Remove a specific name from a 'from X import a, b, c' statement."""
                from rewrite.java.support_types import JContainer
                from rewrite.java.tree import Space

                existing_padded = multi.padding.names.padding.elements
                new_padded = [
                    p for p in existing_padded
                    if self._get_qualid_name(p.element.qualid) != name_to_remove
                ]

                if len(new_padded) == 0:
                    return None
                if len(new_padded) < len(existing_padded):
                    # Fix up first element prefix
                    first = new_padded[0]
                    if first.element.prefix != Space.EMPTY:
                        new_padded[0] = first.replace(_element=first.element.replace(prefix=Space.EMPTY))
                    return multi.padding.replace(
                        _names=JContainer(
                            multi.padding.names.before,
                            new_padded,
                            multi.padding.names.markers
                        )
                    )
                return multi

            def _remove_module_from_import(self, multi: MultiImport, module_to_remove: str) -> Optional[J]:
                """Remove a module from an import statement."""
                from rewrite.java.support_types import JContainer
                from rewrite.java.tree import Space

                existing_padded = multi.padding.names.padding.elements
                new_padded = [
                    p for p in existing_padded
                    if self._get_qualid_name(p.element.qualid) != module_to_remove
                ]

                if len(new_padded) == 0:
                    return None
                if len(new_padded) < len(existing_padded):
                    first = new_padded[0]
                    if first.element.prefix != Space.EMPTY:
                        new_padded[0] = first.replace(_element=first.element.replace(prefix=Space.EMPTY))
                    return multi.padding.replace(
                        _names=JContainer(
                            multi.padding.names.before,
                            new_padded,
                            multi.padding.names.markers
                        )
                    )
                return multi

            def _get_qualid_name(self, qualid) -> str:
                """Get the string representation of a qualified name."""
                if isinstance(qualid, Identifier):
                    return qualid.simple_name
                elif isinstance(qualid, FieldAccess):
                    target = self._get_name_string(qualid.target)
                    name = qualid.name.simple_name
                    if target:
                        return f"{target}.{name}"
                    return name
                return ""

            def _get_name_string(self, name) -> str:
                """Get string from a NameTree."""
                if isinstance(name, Identifier):
                    return name.simple_name
                elif isinstance(name, FieldAccess):
                    target = self._get_name_string(name.target)
                    if target:
                        return f"{target}.{name.name.simple_name}"
                    return name.name.simple_name
                elif isinstance(name, Empty):
                    return ""
                return str(name) if name else ""

            def _get_alias_name(self, imp: Import) -> Optional[str]:
                """Get the alias name from an Import, or None if no alias."""
                if imp.alias is None:
                    return None
                alias = imp.alias
                if isinstance(alias, Identifier):
                    return alias.simple_name
                return None

        return ChangeImportVisitor()
