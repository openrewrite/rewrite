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
from typing import List, Optional, Sequence, Set

from rewrite.java import J
from rewrite.java.support_types import JContainer, JRightPadded, Statement
from rewrite.java.tree import Identifier, If, Import, Space
from rewrite.python.import_utils import (get_qualid_name, get_name_string, get_alias_name,
                                         get_canonical_fqn, referenced_names,
                                         unconditional_body)
from rewrite.python.scope_utils import LocalBindings
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


def prefix_to_inherit(stmt, index: int) -> Optional[Space]:
    """The removed statement's prefix only when worth rescuing (comments, or the file's leading
    prefix), since otherwise it is blank-line separation the next statement already carries."""
    return stmt.prefix if (index == 0 or stmt.prefix.comments) else None


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
    if visitor._after_visit is None:
        visitor._after_visit = []

    for v in visitor._after_visit:
        if isinstance(v, RemoveImport):
            if v.module == options.module and v.name == options.name:
                return  # Already registered

    visitor._after_visit.append(RemoveImport(options))


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
        self._used: Optional[Set[str]] = None
        self._cu: Optional[CompilationUnit] = None

    def visit_compilation_unit(self, cu: CompilationUnit, p) -> J:
        self._used = self._collect_used_identifiers(cu) if self.only_if_unused else None
        self._cu = cu
        return self._remove_import(cu)

    def _is_removable(self, imp: Import, fallback_bound: str) -> bool:
        """True when ``only_if_unused`` permits removing ``imp``, judged by the
        name the import binds: ``from typing import List as L`` binds ``L``,
        not ``List``."""
        if self._used is None:
            return True
        bound = get_alias_name(imp) or fallback_bound
        if bound not in self._used:
            return True
        # Removable anyway if another import already binds the name (what ChangeType leaves behind),
        # since that binding shadows this one and the references keep resolving through it.
        return self._bound_by_another_import(self._cu, bound, imp)

    def _bound_by_another_import(self, cu: CompilationUnit, target_name: str,
                                 removing: Import) -> bool:
        """True when some import other than ``removing`` binds ``target_name``."""
        for stmt in cu.statements:
            if isinstance(stmt, MultiImport):
                from_name = get_name_string(stmt.from_) if stmt.from_ is not None else None
                for imp in stmt.names:
                    if self._binds_other(imp, from_name, target_name, removing):
                        return True
            elif isinstance(stmt, Import):
                if self._binds_other(stmt, None, target_name, removing):
                    return True
        return False

    def _binds_other(self, imp: Import, from_name: Optional[str], target_name: str,
                     removing: Import) -> bool:
        """True when ``imp`` binds ``target_name`` and is not the import being removed."""
        if imp is removing:
            return False
        alias = get_alias_name(imp)
        qualid = get_qualid_name(imp.qualid)
        bound = alias or (qualid if from_name is not None else qualid.split('.')[0])
        if bound != target_name:
            return False
        # Same module and same name means this *is* the import we were asked to remove.
        if self.name is None:
            return from_name is not None or qualid != self.module
        return from_name != self.module or qualid != self.name

    def _collect_used_identifiers(self, cu: CompilationUnit) -> Set[str]:
        """Collect all identifiers used in the code (excluding imports)."""
        used: Set[str] = set()
        bindings = LocalBindings()

        class UsageCollector(PythonVisitor):
            def __init__(self):
                super().__init__()
                self.in_import = False

            def visit_import(self, import_: Import, p) -> J:
                # Don't collect identifiers from standalone import statements
                self.in_import = True
                try:
                    return super().visit_import(import_, p)
                finally:
                    self.in_import = False

            def visit_multi_import(self, multi: MultiImport, p) -> J:
                # Don't collect identifiers from import statements
                self.in_import = True
                try:
                    return super().visit_multi_import(multi, p)
                finally:
                    self.in_import = False

            def visit_identifier(self, ident: Identifier, p) -> J:
                if not self.in_import:
                    used.update(name for name in referenced_names(ident)
                                if not bindings.is_bound(self.cursor, name))
                return ident

        collector = UsageCollector()
        collector.visit(cu, None)
        return used

    def _remove_import(self, cu: CompilationUnit) -> CompilationUnit:
        """Remove the import from the compilation unit."""
        kept = self._prune_statements(cu.padding.statements)
        return cu if kept is None else cu.padding.replace(_statements=kept)

    def _prune_statements(self, padded_statements: Sequence[JRightPadded]) -> Optional[List[JRightPadded]]:
        """The statements with the import gone, or None to leave them as they are —
        because nothing matched, or because a comment would go with the removal. One
        prefix cannot carry two comments, so a collision abandons the removal."""
        kept: List[JRightPadded] = []
        inherited: Optional[Space] = None
        changed = False
        for index, padded in enumerate(padded_statements):
            stmt = padded.element
            if isinstance(stmt, MultiImport):
                result: Optional[Statement] = self._process_multi_import(stmt)
            elif isinstance(stmt, Import):
                result = self._process_single_import(stmt)
            elif isinstance(stmt, If):
                result = self._prune_if_body(stmt)
            else:
                result = stmt

            if result is None:
                changed = True
                if inherited is None:
                    inherited = prefix_to_inherit(stmt, index)
                elif stmt.prefix.comments:
                    return None
                continue
            if result is not stmt:
                padded = JRightPadded(result, padded.after, padded.markers)
                changed = True
            if inherited is not None:
                if not padded.element.prefix.comments:
                    padded = JRightPadded(
                        padded.element.replace(prefix=inherited), padded.after, padded.markers
                    )
                elif inherited.comments:
                    return None
                inherited = None
            kept.append(padded)
        # A leftover prefix means nothing followed the removal to carry it, so a
        # comment on the removed statement would be dropped with it.
        if inherited is not None and inherited.comments:
            return None
        return kept if changed else None

    def _prune_if_body(self, if_: If) -> Optional[If]:
        """`if_` with the import gone from its body, or None once that empties it."""
        body = unconditional_body(if_)
        if body is None:
            return if_
        kept = self._prune_statements(body.padding.statements)
        if kept is None:
            return if_
        if not kept:
            return None
        padded = if_.padding.then_part
        return if_.padding.replace(
            _then_part=JRightPadded(
                body.padding.replace(_statements=kept), padded.after, padded.markers
            )
        )

    def _process_single_import(self, imp: Import) -> Optional[Import]:
        """Process a standalone J.Import. Return None to remove, or the original."""
        if self.name is not None:
            return imp
        name = get_qualid_name(imp.qualid)
        if name == self.module and self._is_removable(imp, self.module.split('.')[-1]):
            return None
        return imp

    def _process_multi_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Process a MultiImport and return None to remove, modified, or original."""
        if self.name is None:
            # Removing entire module import
            return self._remove_module_import(multi)
        else:
            # Removing specific name from "from X import ..."
            return self._remove_name_from_import(multi)

    def _remove_module_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Remove the import that binds the module itself."""
        if multi.from_ is not None:
            # This is a "from X import Y" statement
            new_padded = [
                padded_imp for padded_imp in multi.padding.names.padding.elements
                if not self._canonical_module_matches(padded_imp.element)
                or not self._is_removable(padded_imp.element,
                                          get_qualid_name(padded_imp.element.qualid))
            ]
        else:
            # This is a "import X" statement
            new_padded = []
            for padded_imp in multi.padding.names.padding.elements:
                name = get_qualid_name(padded_imp.element.qualid)
                if name != self.module or not self._is_removable(
                        padded_imp.element, name.split('.')[-1]):
                    new_padded.append(padded_imp)

        return self._prune_names(multi, new_padded)

    def _canonical_module_matches(self, imp: Import) -> bool:
        """True when ``imp`` binds the requested module itself (``from os import
        path`` for ``os.path``). Membership is deliberately not enough, written
        (``from typing import Any``) or canonical (``typing`` is the home of every
        symbol re-exported through it): a member binds the member, not the module."""
        return get_canonical_fqn(imp) == self.module

    def _remove_name_from_import(self, multi: MultiImport) -> Optional[MultiImport]:
        """Remove a specific name from a 'from X import a, b, c' statement.

        A member matches when written module and name match, or when its
        canonical FQN equals the requested ``module.name``.
        """
        if multi.from_ is None:
            return multi  # Not a "from" import

        syntactic = get_name_string(multi.from_) == self.module
        target_fqn = f"{self.module}.{self.name}"

        new_padded = []
        for padded_imp in multi.padding.names.padding.elements:
            name = get_qualid_name(padded_imp.element.qualid)
            matches = (syntactic and name == self.name) or \
                get_canonical_fqn(padded_imp.element) == target_fqn
            if not matches or not self._is_removable(padded_imp.element, name):
                new_padded.append(padded_imp)

        return self._prune_names(multi, new_padded)

    def _prune_names(self, multi: MultiImport, new_padded: list) -> Optional[MultiImport]:
        """Rebuild ``multi`` with only ``new_padded`` names, preserving padding.

        Returns None when nothing is left, or the original when nothing was removed.
        """
        existing_padded = multi.padding.names.padding.elements
        if len(new_padded) == 0:
            return None  # Remove entire statement
        if len(new_padded) == len(existing_padded):
            return multi

        # Whatever separated the statement from its first name — nothing, or the line break
        # and indent of a parenthesized import — belongs to whichever name is first now.
        first = new_padded[0]
        leading = existing_padded[0].element.prefix
        if first.element.prefix != leading:
            new_padded[0] = first.replace(_element=first.element.replace(prefix=leading))

        return MultiImport(
            multi.id,
            multi.prefix,
            multi.markers,
            multi.padding.from_,
            multi.parenthesized,
            JContainer(
                multi.padding.names.before,
                new_padded,
                multi.padding.names.markers
            )
        )

