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

"""RemoveImport visitor for Python import handling."""

from dataclasses import dataclass
from typing import List, Optional, Set
from uuid import uuid4

from rewrite.java import J
from rewrite.java.support_types import JContainer, JRightPadded
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import, Space
from rewrite.markers import Markers
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor


@dataclass
class RemoveImportOptions:
    """Options for removing an import.

    Attributes:
        module: The module to remove imports from (e.g., 'os.path', 'typing')
        name: The specific name to remove. If None, removes the entire module import.
        only_if_unused: If True, only remove the import if the name is not used (default: True)
    """
    module: str
    name: Optional[str] = None
    only_if_unused: bool = True


def maybe_remove_import(visitor: PythonVisitor, options: RemoveImportOptions) -> None:
    """Register a RemoveImport visitor to remove an import statement.

    This function schedules the import removal after the current visit completes.

    Args:
        visitor: The visitor to register the import removal with
        options: Configuration for the import to remove

    Examples:
        # Remove: import os (entire module)
        maybe_remove_import(visitor, RemoveImportOptions(module='os'))

        # Remove: from os import path
        maybe_remove_import(visitor, RemoveImportOptions(module='os', name='path'))

        # Remove: from os.path import join
        maybe_remove_import(visitor, RemoveImportOptions(module='os.path', name='join'))
    """
    if not hasattr(visitor, 'after_visit') or visitor.after_visit is None:
        visitor.after_visit = []

    for v in visitor.after_visit:
        if isinstance(v, RemoveImport):
            if v.module == options.module and v.name == options.name:
                return  # Already registered

    visitor.after_visit.append(RemoveImport(options))


class RemoveImport(PythonVisitor):
    """Visitor that removes an import statement from a Python file.

    This visitor handles:
    - Removing entire import statements
    - Removing specific names from 'from X import a, b, c' statements
    - Checking if identifiers are still used before removing
    """

    def __init__(self, options: RemoveImportOptions):
        super().__init__()
        self.module = options.module
        self.name = options.name
        self.only_if_unused = options.only_if_unused

    def visit_compilation_unit(self, cu: CompilationUnit, p) -> J:
        # If only_if_unused is True, check if the identifier is used
        if self.only_if_unused:
            if self._is_referenced(cu):
                return cu

        # Remove the import
        return self._remove_import(cu)

    def _is_referenced(self, cu: CompilationUnit) -> bool:
        """Check if the identifier we're removing is still used elsewhere."""
        target_name = self.name or self.module.split('.')[-1]

        # Collect all used identifiers (excluding import statements)
        used = self._collect_used_identifiers(cu)
        return target_name in used

    def _collect_used_identifiers(self, cu: CompilationUnit) -> Set[str]:
        """Collect all identifiers used in the code (excluding imports)."""
        used: Set[str] = set()

        class UsageCollector(PythonVisitor):
            def __init__(self):
                super().__init__()
                self.in_import = False

            def visit_multi_import(self, multi: MultiImport, p) -> J:
                # Don't collect identifiers from import statements
                self.in_import = True
                result = super().visit_multi_import(multi, p)
                self.in_import = False
                return result

            def visit_identifier(self, ident: Identifier, p) -> J:
                if not self.in_import:
                    used.add(ident.simple_name)
                return ident

        collector = UsageCollector()
        collector.visit(cu, None)
        return used

    def _remove_import(self, cu: CompilationUnit) -> CompilationUnit:
        """Remove the import from the compilation unit."""
        new_statements = []
        changed = False

        for stmt in cu.statements:
            if isinstance(stmt, MultiImport):
                result = self._process_multi_import(stmt)
                if result is None:
                    # Remove the entire statement
                    changed = True
                elif result is not stmt:
                    # Statement was modified
                    new_statements.append(result)
                    changed = True
                else:
                    new_statements.append(stmt)
            else:
                new_statements.append(stmt)

        if changed:
            return cu.replace(statements=new_statements)
        return cu

    def _process_multi_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Process a MultiImport and return None to remove, modified, or original."""
        if self.name is None:
            # Removing entire module import
            return self._remove_module_import(multi)
        else:
            # Removing specific name from "from X import ..."
            return self._remove_name_from_import(multi)

    def _remove_module_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Remove an entire module import (import X or from X import *)."""
        if multi.from_ is not None:
            # This is a "from X import Y" statement
            from_name = self._get_name_string(multi.from_)
            if from_name == self.module:
                # Check if removing all names or just star import
                if len(multi.names) == 1:
                    name = self._get_qualid_name(multi.names[0].qualid)
                    if name == '*':
                        return None  # Remove "from X import *"
                # For "from X import a, b, c", only remove if we want to remove all
                return None
        else:
            # This is a "import X" statement
            new_names = []
            for imp in multi.names:
                name = self._get_qualid_name(imp.qualid)
                alias = self._get_alias_name(imp)
                # Keep imports that don't match
                if name != self.module:
                    new_names.append(imp)

            if len(new_names) == 0:
                return None  # Remove entire statement
            if len(new_names) < len(multi.names):
                # Some names removed
                return MultiImport(
                    multi.id,
                    multi.prefix,
                    multi.markers,
                    multi.padding.from_,
                    multi.parenthesized,
                    JContainer(
                        multi.padding.names.before,
                        [self._pad_right(n) for n in new_names],
                        multi.padding.names.markers
                    )
                )

        return multi

    def _remove_name_from_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Remove a specific name from a 'from X import a, b, c' statement."""
        if multi.from_ is None:
            return multi  # Not a "from" import

        from_name = self._get_name_string(multi.from_)
        if from_name != self.module:
            return multi  # Different module

        # Filter out the name we want to remove
        new_names = []
        for imp in multi.names:
            name = self._get_qualid_name(imp.qualid)
            if name != self.name:
                new_names.append(imp)

        if len(new_names) == 0:
            return None  # Remove entire statement
        if len(new_names) < len(multi.names):
            # Some names removed
            return MultiImport(
                multi.id,
                multi.prefix,
                multi.markers,
                multi.padding.from_,
                multi.parenthesized,
                JContainer(
                    multi.padding.names.before,
                    [self._pad_right(n) for n in new_names],
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
        alias = imp.alias.element
        if isinstance(alias, Identifier):
            return alias.simple_name
        return None

    def _pad_right(self, elem) -> JRightPadded:
        """Wrap an element in a JRightPadded."""
        return JRightPadded(elem, Space.EMPTY, Markers.EMPTY)
