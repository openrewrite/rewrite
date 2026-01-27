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

"""AddImport visitor for Python import handling."""

from dataclasses import dataclass
from enum import Enum
from typing import List, Optional
from uuid import uuid4

from rewrite.java import J
from rewrite.java.support_types import JContainer, JLeftPadded, JRightPadded
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import, Space
from rewrite.markers import Markers
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor


class ImportStyle(Enum):
    """Python import styles."""
    DIRECT = "direct"  # import module
    FROM = "from"  # from module import name


@dataclass
class AddImportOptions:
    """Options for adding an import.

    Attributes:
        module: The module to import from (e.g., 'os.path', 'typing')
        name: The specific name to import. If None, imports the module directly.
              Special values:
              - '*': Adds a star import (from module import *)
        alias: Optional alias for the imported name (e.g., 'np' for numpy)
        only_if_referenced: If True, only add the import if the name is used (default: True)
    """
    module: str
    name: Optional[str] = None
    alias: Optional[str] = None
    only_if_referenced: bool = True


def maybe_add_import(visitor: PythonVisitor, options: AddImportOptions) -> None:
    """Register an AddImport visitor to add an import statement.

    This function schedules the import to be added after the current visit completes.

    Args:
        visitor: The visitor to register the import addition with
        options: Configuration for the import to add

    Examples:
        # Add: import os
        maybe_add_import(visitor, AddImportOptions(module='os'))

        # Add: from os import path
        maybe_add_import(visitor, AddImportOptions(module='os', name='path'))

        # Add: from os.path import join
        maybe_add_import(visitor, AddImportOptions(module='os.path', name='join'))

        # Add: from os.path import join as pjoin
        maybe_add_import(visitor, AddImportOptions(module='os.path', name='join', alias='pjoin'))

        # Add: import numpy as np
        maybe_add_import(visitor, AddImportOptions(module='numpy', alias='np'))

        # Add: from typing import *
        maybe_add_import(visitor, AddImportOptions(module='typing', name='*'))
    """
    # Check for duplicate registrations
    if not hasattr(visitor, 'after_visit') or visitor.after_visit is None:
        visitor.after_visit = []

    for v in visitor.after_visit:
        if isinstance(v, AddImport):
            if (v.module == options.module and
                v.name == options.name and
                v.alias == options.alias):
                return  # Already registered

    visitor.after_visit.append(AddImport(options))


class AddImport(PythonVisitor):
    """Visitor that adds an import statement to a Python file.

    This visitor handles:
    - Direct imports: import module [as alias]
    - From imports: from module import name [as alias]
    - Star imports: from module import *
    - Merging into existing imports from the same module
    """

    def __init__(self, options: AddImportOptions):
        super().__init__()
        self.module = options.module
        self.name = options.name
        self.alias = options.alias
        self.only_if_referenced = options.only_if_referenced

    def visit_compilation_unit(self, cu: CompilationUnit, p) -> J:
        # Check if import already exists
        if self._import_exists(cu):
            return cu

        # If only_if_referenced is True, check if the identifier is used
        if self.only_if_referenced:
            if not self._is_referenced(cu):
                return cu

        # Try to merge into an existing import from the same module
        merged = self._try_merge_into_existing(cu)
        if merged is not cu:
            return merged

        # Add new import statement
        return self._add_import(cu)

    def _import_exists(self, cu: CompilationUnit) -> bool:
        """Check if the import already exists."""
        for stmt in cu.statements:
            if isinstance(stmt, MultiImport):
                if self._multi_import_matches(stmt):
                    return True
        return False

    def _multi_import_matches(self, multi: MultiImport) -> bool:
        """Check if a MultiImport matches the import we want to add."""
        if self.name is None:
            # We want: import module [as alias]
            if multi.from_ is not None:
                return False  # This is a "from" import
            for imp in multi.names:
                if self._import_name_matches(imp, self.module, self.alias):
                    return True
        else:
            # We want: from module import name [as alias]
            if multi.from_ is None:
                return False  # This is not a "from" import
            from_name = self._get_name_string(multi.from_)
            if from_name != self.module:
                return False
            for imp in multi.names:
                if self._import_name_matches(imp, self.name, self.alias):
                    return True
        return False

    def _import_name_matches(self, imp: Import, name: str, alias: Optional[str]) -> bool:
        """Check if an Import matches the given name and alias."""
        import_name = self._get_qualid_name(imp.qualid)
        import_alias = self._get_alias_name(imp)

        if import_name != name:
            return False
        if alias is not None and import_alias != alias:
            return False
        if alias is None and import_alias is not None:
            return False
        return True

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

    def _is_referenced(self, cu: CompilationUnit) -> bool:
        """Check if the identifier we're importing is actually used."""
        target_name = self.alias or self.name or self.module.split('.')[-1]

        class ReferenceChecker(PythonVisitor):
            def __init__(self):
                super().__init__()
                self.found = False

            def visit_identifier(self, ident: Identifier, p) -> J:
                if ident.simple_name == target_name:
                    self.found = True
                return ident

        checker = ReferenceChecker()
        checker.visit(cu, None)
        return checker.found

    def _try_merge_into_existing(self, cu: CompilationUnit) -> CompilationUnit:
        """Try to merge the new import into an existing import from the same module."""
        if self.name is None:
            # Direct import - cannot merge
            return cu

        # Look for existing "from module import ..." statements
        for i, stmt in enumerate(cu.statements):
            if not isinstance(stmt, MultiImport):
                continue
            if stmt.from_ is None:
                continue
            from_name = self._get_name_string(stmt.from_)
            if from_name != self.module:
                continue

            # Found an existing import from the same module - add our name
            new_import = self._create_import_element(self.name, self.alias)
            new_names = list(stmt.names) + [new_import]

            # Recreate the MultiImport with the new names
            new_multi = MultiImport(
                stmt.id,
                stmt.prefix,
                stmt.markers,
                stmt.padding.from_,
                stmt.parenthesized,
                JContainer(
                    stmt.padding.names.before,
                    [self._pad_right(n) for n in new_names],
                    stmt.padding.names.markers
                )
            )

            # Replace the statement
            new_statements = list(cu.statements)
            new_statements[i] = new_multi
            return cu.replace(statements=new_statements)

        return cu

    def _add_import(self, cu: CompilationUnit) -> CompilationUnit:
        """Add a new import statement to the compilation unit."""
        new_import = self._create_multi_import()

        # Find insertion point (after existing imports)
        insert_idx = 0
        for i, stmt in enumerate(cu.statements):
            if isinstance(stmt, MultiImport):
                insert_idx = i + 1
            elif insert_idx > 0:
                break  # Stop after we've passed the import section

        # Insert the new import
        new_statements = list(cu.statements)
        new_statements.insert(insert_idx, new_import)

        # Adjust spacing
        if insert_idx > 0 and insert_idx < len(new_statements):
            # Add newline before next statement if needed
            next_stmt = new_statements[insert_idx + 1] if insert_idx + 1 < len(new_statements) else None
            if next_stmt and not next_stmt.prefix.whitespace.startswith('\n'):
                new_statements[insert_idx + 1] = next_stmt.replace(
                    prefix=Space.format('\n' + next_stmt.prefix.whitespace)
                )

        return cu.replace(statements=new_statements)

    def _create_multi_import(self) -> MultiImport:
        """Create a new MultiImport statement."""
        if self.name is None:
            # Direct import: import module [as alias]
            import_elem = self._create_import_element(self.module, self.alias)
            return MultiImport(
                uuid4(),
                Space.format('\n'),
                Markers.EMPTY,
                None,  # No 'from'
                False,  # Not parenthesized
                JContainer(
                    Space.format(' '),
                    [self._pad_right(import_elem)],
                    Markers.EMPTY
                )
            )
        else:
            # From import: from module import name [as alias]
            from_name = self._create_qualified_name(self.module)
            import_elem = self._create_import_element(self.name, self.alias)
            return MultiImport(
                uuid4(),
                Space.format('\n'),
                Markers.EMPTY,
                JRightPadded(from_name, Space.format(' '), Markers.EMPTY),
                False,  # Not parenthesized
                JContainer(
                    Space.format(' '),
                    [self._pad_right(import_elem)],
                    Markers.EMPTY
                )
            )

    def _create_import_element(self, name: str, alias: Optional[str]) -> Import:
        """Create an Import element."""
        qualid = self._create_qualified_name(name)
        alias_left_padded = None
        if alias:
            alias_ident = Identifier(
                uuid4(),
                Space.format(' '),
                Markers.EMPTY,
                [],
                alias,
                None,
                None
            )
            alias_left_padded = JLeftPadded(
                Space.format(' '),
                alias_ident,
                Markers.EMPTY
            )

        return Import(
            uuid4(),
            Space.EMPTY,
            Markers.EMPTY,
            JLeftPadded(Space.EMPTY, False, Markers.EMPTY),
            qualid,
            alias_left_padded
        )

    def _create_qualified_name(self, name: str) -> FieldAccess:
        """Create a FieldAccess for a qualified name."""
        parts = name.split('.')
        if len(parts) == 1:
            return FieldAccess(
                uuid4(),
                Space.EMPTY,
                Markers.EMPTY,
                Empty(uuid4(), Space.EMPTY, Markers.EMPTY),
                JLeftPadded(
                    Space.EMPTY,
                    Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], parts[0], None, None),
                    Markers.EMPTY
                ),
                None
            )

        # Build nested FieldAccess for qualified names
        result = Empty(uuid4(), Space.EMPTY, Markers.EMPTY)
        for part in parts:
            result = FieldAccess(
                uuid4(),
                Space.EMPTY,
                Markers.EMPTY,
                result,
                JLeftPadded(
                    Space.EMPTY if isinstance(result, Empty) else Space.EMPTY,
                    Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], part, None, None),
                    Markers.EMPTY
                ),
                None
            )
        return result

    def _pad_right(self, elem) -> JRightPadded:
        """Wrap an element in a JRightPadded."""
        return JRightPadded(elem, Space.EMPTY, Markers.EMPTY)
