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

from dataclasses import dataclass, field, replace as dc_replace
from typing import Any, Optional

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.recipe import option
from rewrite.java import J
from rewrite.java.support_types import JavaType
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import, MethodDeclaration, MethodInvocation
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import


_Imports = [*Python, CategoryDescriptor(display_name="Imports")]


def _create_module_type(fqn: str) -> JavaType.Class:
    """Create a JavaType.Class for a module from its fully qualified name.

    JavaType.Class is not a dataclass, so fields are set directly after
    construction.  This matches the pattern used elsewhere in the codebase.
    """
    class_type = JavaType.Class()
    class_type._flags_bit_map = 0
    class_type._fully_qualified_name = fqn
    class_type._kind = JavaType.FullyQualified.Kind.Class
    return class_type


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
            has_old_import: bool = False
            old_alias: Optional[str] = None
            has_direct_module_import: bool = False
            module_alias: Optional[str] = None
            rewrote_qualified_refs: bool = False
            new_module_type: Optional[JavaType.Class] = None

            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                self.has_old_import = False
                self.old_alias = None
                self.has_direct_module_import = False
                self.module_alias = None
                self.rewrote_qualified_refs = False
                self.new_module_type = None

                # Single pass: detect old imports and direct module imports
                for stmt in cu.statements:
                    if isinstance(stmt, Import) and not isinstance(stmt, MultiImport):
                        if not self.has_old_import:
                            alias = self._check_for_old_single_import(stmt)
                            if alias is not None:
                                self.has_old_import = True
                                self.old_alias = alias if alias != "" else None
                        if old_name and not self.has_direct_module_import:
                            name = self._get_qualid_name(stmt.qualid)
                            if name == old_module:
                                self.has_direct_module_import = True
                                self.module_alias = self._get_alias_name(stmt)
                    elif isinstance(stmt, MultiImport):
                        if not self.has_old_import:
                            alias = self._check_for_old_import(stmt)
                            if alias is not None:
                                self.has_old_import = True
                                self.old_alias = alias if alias != "" else None
                        if old_name and not self.has_direct_module_import and stmt.from_ is None:
                            for imp in stmt.names:
                                name = self._get_qualid_name(imp.qualid)
                                if name == old_module:
                                    self.has_direct_module_import = True
                                    self.module_alias = self._get_alias_name(imp)
                                    break

                if not self.has_old_import and not self.has_direct_module_import:
                    return cu

                # Visit to transform imports
                result = super().visit_compilation_unit(cu, p)
                if not isinstance(result, CompilationUnit):
                    return result

                # Schedule adding the new import (only for direct import changes)
                if self.has_old_import:
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

                # If we rewrote qualified references, manage the direct import
                if self.rewrote_qualified_refs:
                    maybe_add_import(self, AddImportOptions(
                        module=new_module,
                        alias=new_alias,
                        only_if_referenced=False
                    ))
                    maybe_remove_import(self, RemoveImportOptions(
                        module=old_module,
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

            def visit_identifier(self, ident: Identifier, p: ExecutionContext) -> J:
                ident = super().visit_identifier(ident, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not isinstance(ident, Identifier):
                    return ident
                if not old_name or not new_name or not self.has_old_import:
                    return ident
                old_ref_name = self.old_alias or old_name
                new_ref_name = new_alias or self.old_alias or new_name
                if old_ref_name == new_ref_name:
                    return ident
                if ident.simple_name != old_ref_name:
                    return ident
                # Skip identifiers inside import statements
                if self.cursor.first_enclosing(Import):
                    return ident
                # Skip local variables that shadow the imported name.
                # Only check field_type inside function scopes — at module level,
                # bare references to the imported name always need renaming.
                # When ty is unavailable, field_type is None for all identifiers
                # and shadowed locals may be incorrectly renamed.
                if self.cursor.first_enclosing(MethodDeclaration) is not None:
                    if ident.field_type is not None:
                        return ident
                return ident.replace(_simple_name=new_ref_name)

            def visit_method_invocation(self, method: MethodInvocation, p: ExecutionContext) -> J:
                method = super().visit_method_invocation(method, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not isinstance(method, MethodInvocation):
                    return method
                if not old_name or not self.has_direct_module_import:
                    return method
                # Only matches simple module.func() calls where the select is an
                # Identifier. Nested attribute chains like pkg.module.func()
                # (where select is a FieldAccess) are not currently handled.
                if not isinstance(method.select, Identifier):
                    return method
                if not isinstance(method.name, Identifier):
                    return method

                select_name = method.select.simple_name
                # For dotted modules without aliases (e.g. `import os.path`),
                # `old_module` is a dotted string like "os.path" which will
                # never match a simple Identifier name — but those cases are
                # already excluded by the `isinstance(method.select, Identifier)`
                # guard above (the select would be a FieldAccess instead).
                expected_name = self.module_alias or old_module
                if select_name != expected_name:
                    return method
                if method.name.simple_name != old_name:
                    return method

                self.rewrote_qualified_refs = True
                new_select_name = new_alias or new_module
                new_select = method.select.replace(_simple_name=new_select_name)
                # Update type attribution on the select identifier
                if method.select.type is not None:
                    new_select = new_select.replace(_type=self._get_new_module_type())
                padded_select = method.padding.select
                if padded_select is None:
                    return method
                new_padded_select = padded_select.replace(_element=new_select)
                result = method.padding.replace(_select=new_padded_select)
                if new_name and new_name != old_name:
                    result = result.replace(_name=result.name.replace(_simple_name=new_name))
                # Update method_type declaring type and name
                if result.method_type is not None:
                    result = result.replace(_method_type=dc_replace(
                        result.method_type,
                        _declaring_type=self._get_new_module_type(),
                        _name=new_name or old_name,
                    ))
                return result

            def visit_field_access(self, field_access: FieldAccess, p: ExecutionContext) -> J:
                field_access = super().visit_field_access(field_access, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not old_name or not self.has_direct_module_import:
                    return field_access
                if not isinstance(field_access, FieldAccess):
                    return field_access
                if not isinstance(field_access.target, Identifier):
                    return field_access

                existing_name = field_access.target.simple_name
                expected_name = self.module_alias or old_module
                if existing_name != expected_name:
                    return field_access
                if field_access.name.simple_name != old_name:
                    return field_access

                self.rewrote_qualified_refs = True
                new_target_name = new_alias or new_module
                new_target = field_access.target.replace(_simple_name=new_target_name)
                # Update type attribution on the target identifier
                if field_access.target.type is not None:
                    new_target = new_target.replace(_type=self._get_new_module_type())
                result = field_access.replace(_target=new_target)
                if new_name and new_name != old_name:
                    new_name_ident = result.name.replace(_simple_name=new_name)
                    result = result.padding.replace(_name=result.padding.name.replace(_element=new_name_ident))
                return result

            def _get_new_module_type(self) -> JavaType.Class:
                if self.new_module_type is None:
                    self.new_module_type = _create_module_type(new_module)
                return self.new_module_type

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
