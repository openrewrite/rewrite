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

import builtins
import re
import sys
from collections.abc import Callable
from dataclasses import dataclass
from enum import Enum, IntEnum
from pathlib import Path
from typing import NamedTuple, Optional

from rewrite import random_id
from rewrite.java import J
from rewrite.java.support_types import JContainer, JLeftPadded, JRightPadded
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import, Literal, Space
from rewrite.markers import Markers
from rewrite.python.binding_utils import is_reference
from rewrite.python.format.line_breaks import line_break
from rewrite.python.import_utils import (get_qualid_name, get_name_string, get_alias_name,
                                         get_canonical_fqn, module_binding_name, pad_right,
                                         referenced_names)
from rewrite.python.tree import CompilationUnit, ExpressionStatement, MultiImport, Shebang
from rewrite.python.visitor import PythonVisitor


def _is_module_docstring(padded_stmts: list) -> bool:
    """True when the file opens with a module docstring -- a bare string expression."""
    if not padded_stmts:
        return False
    stmt = padded_stmts[0].element
    if not isinstance(stmt, ExpressionStatement):
        return False
    return isinstance(stmt.expression, Literal) and isinstance(stmt.expression.value, str)


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
    if visitor._after_visit is None:
        visitor._after_visit = []

    for v in visitor._after_visit:
        if isinstance(v, AddImport):
            if (v.module == options.module and
                v.name == options.name and
                v.alias == options.alias):
                return  # Already registered

    visitor._after_visit.append(AddImport(options))


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
        # builtins are always available; only import them under an explicit alias
        if self.module == 'builtins' and self.alias is None:
            return cu

        # A bare builtin name (e.g. `list` when ChangeType retargets a type to a builtin) is
        # not a module, so there is no import that could bind it.
        if self.name is None and '.' not in self.module and hasattr(builtins, self.module):
            return cu

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
            if isinstance(stmt, Import) and not isinstance(stmt, MultiImport):
                if self.name is None and self._import_name_matches(stmt, self.module, self.alias):
                    return True
            elif isinstance(stmt, MultiImport):
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
            from_name = get_name_string(multi.from_)
            for imp in multi.names:
                if from_name == self.module and self._import_name_matches(imp, self.name, self.alias):
                    return True
                # A member canonically matching the requested module.name
                # satisfies the request too — provided it binds the same name,
                # so references to the requested name resolve through it.
                if get_canonical_fqn(imp) == f"{self.module}.{self.name}" and \
                        (get_alias_name(imp) or get_qualid_name(imp.qualid)) == (self.alias or self.name):
                    return True
        return False

    def _import_name_matches(self, imp: Import, name: str, alias: Optional[str]) -> bool:
        """Check if an Import matches the given name and alias."""
        import_name = get_qualid_name(imp.qualid)
        import_alias = get_alias_name(imp)

        if import_name != name:
            return False
        if alias is not None and import_alias != alias:
            return False
        if alias is None and import_alias is not None:
            return False
        return True

    def _is_referenced(self, cu: CompilationUnit) -> bool:
        """Check if the identifier we're importing is actually used."""
        target_name = self.alias or self.name or module_binding_name(self.module)

        class ReferenceChecker(PythonVisitor):
            def __init__(self):
                super().__init__()
                self.found = False

            def visit_identifier(self, ident: Identifier, p) -> J:
                if is_reference(self.cursor, ident) and target_name in referenced_names(ident):
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
            from_name = get_name_string(stmt.from_)
            if from_name != self.module:
                continue

            # Found an existing import from the same module - add our name in
            # case-insensitive alphabetical position (mirrors rewrite-javascript's
            # AddImport). Existing members are not reordered; only the new member
            # is positioned.
            new_import = create_import_element(self.name, self.alias)
            existing_padded = insert_member(
                list(stmt.padding.names.padding.elements), new_import)

            # Recreate the MultiImport with the new names
            new_multi = MultiImport(
                stmt.id,
                stmt.prefix,
                stmt.markers,
                stmt.padding.from_,
                stmt.parenthesized,
                JContainer(
                    stmt.padding.names.before,
                    existing_padded,
                    stmt.padding.names.markers
                )
            )

            # Replace the statement at the padding level
            padded_stmts = list(cu.padding.statements)
            old_padded = padded_stmts[i]
            padded_stmts[i] = JRightPadded(new_multi, old_padded.after, old_padded.markers)
            return cu.padding.replace(_statements=padded_stmts)

        return cu

    def _add_import(self, cu: CompilationUnit) -> CompilationUnit:
        """Add a new import statement to the compilation unit."""
        new_import = create_import_statement(self.module, self.name, self.alias)
        newline = line_break(cu)

        # The shebang and the module docstring must stay first.
        padded_stmts = list(cu.padding.statements)
        shebang = padded_stmts[0] if padded_stmts and isinstance(padded_stmts[0].element, Shebang) else None
        header = 1 if shebang else 0
        if _is_module_docstring(padded_stmts[header:]):
            header += 1
        first_import = None
        insert_idx = header
        for i in range(header, len(padded_stmts)):
            if isinstance(padded_stmts[i].element, (Import, MultiImport)):
                if first_import is None:
                    first_import = i
                insert_idx = i + 1
            elif insert_idx > header:
                break  # Stop after we've passed the import section

        # Insert the new import at the padding level
        if not padded_stmts or (len(padded_stmts) == 1 and isinstance(padded_stmts[0].element, Empty)):
            # An empty module holds one Empty statement, which the new import replaces.
            # Any trailing newline lives in cu.eof, which must open with a line break for a comment in
            # it to stay off the import's line.
            new_padded = JRightPadded(new_import.replace(prefix=Space.EMPTY), Space.EMPTY, Markers.EMPTY)
            padded_stmts = [new_padded]
            eof = cu.eof
            if eof.comments and '\n' not in eof.whitespace:
                cu = cu.replace(eof=eof.replace(whitespace=newline + eof.whitespace))
        elif insert_idx == 0:
            first = padded_stmts[0]
            first_prefix = first.element.prefix
            if first_prefix.whitespace and first_prefix.whitespace.startswith(('\n', '\r\n')):
                # First statement has a leading newline (e.g., a prior import was
                # removed). The new import becomes the true first statement with
                # empty prefix; the existing statement keeps its line break.
                new_padded = JRightPadded(new_import.replace(prefix=Space.EMPTY), Space.EMPTY, Markers.EMPTY)
            else:
                # Normal case: new import takes the first statement's prefix,
                # and the first statement gets a line break as separator.
                new_padded = JRightPadded(new_import.replace(prefix=first_prefix), Space.EMPTY, Markers.EMPTY)
                padded_stmts[0] = JRightPadded(
                    first.element.replace(prefix=Space([], newline)),
                    first.after, first.markers
                )
            padded_stmts.insert(insert_idx, new_padded)
        else:
            placement = None
            if first_import is not None:
                placement = self._place_in_sections(cu, padded_stmts[first_import:insert_idx], first_import)
            # A future import must precede every statement but the header, where isort adds a file's first
            # import too. An existing `from __future__ import` takes the new name instead.
            if (first_import is None or self.module == '__future__') and header < len(padded_stmts) \
                    and (placement is None or placement.index != header):
                placement = _Placement(header, after=False, new_group=False)
            if placement is None:
                placement = _Placement(insert_idx - 1, after=True, new_group=False)
            # A shebang holds the line break that ends it. The statement after it borrows the break meanwhile,
            # so that its prefix reads as any other statement's.
            lent = shebang.after.whitespace if shebang is not None else ''
            if lent:
                padded_stmts[0] = shebang.replace(_after=Space.EMPTY)
                cu = _respace(cu, padded_stmts, 1, lambda whitespace: lent + whitespace)
            cu, padded_stmts = _insert(cu, padded_stmts, new_import, newline, placement, first_import)
            if lent:
                padded_stmts[0] = shebang
                cu = _respace(cu, padded_stmts, 1, lambda whitespace: whitespace.removeprefix(lent))

        return cu.padding.replace(_statements=padded_stmts)

    def _place_in_sections(self, cu: CompilationUnit, block: list, offset: int) -> Optional['_Placement']:
        """Where the new import goes when `block` is laid out in isort's sections: future, standard
        library, third party and local, each a run of imports separated from the next by a blank line,
        in that order. None where the imports are laid out otherwise, or are one run that shows no layout."""
        # First-party packages sit in the project root or its `src` directory, isort's default `src_paths`.
        directories = Path(str(cu.source_path)).parts[:-1]
        local_roots = {directories[0]} if directories else set()
        local_roots.update(directories[i + 1] for i, directory in enumerate(directories[:-1]) if directory == 'src')
        groups: list[list[int]] = []
        for i, padded in enumerate(block):
            if not groups or _BLANK_LINE.search(_text_of(padded.element.prefix)):
                groups.append([])
            groups[-1].append(offset + i)
        if len(groups) < 2:
            return None
        sections = []
        for group in groups:
            in_group = {_section(_module_of(block[i - offset].element), local_roots) for i in group}
            if len(in_group) != 1:
                return None
            sections.append(in_group.pop())
        if sections != sorted(sections):
            return None

        section = _section(self.module, local_roots)
        key = (self.name is not None, self.module.lower())
        own = [group for group, group_section in zip(groups, sections) if group_section == section]
        if own:
            # A section can hold its `import m` and `from m import n` statements in separate runs, and either
            # kind in several.
            runs = [group for group in own
                    if all(_isort_key(block[i - offset].element)[0] == key[0] for i in group)]
            group = next((run for run in reversed(runs) if _isort_key(block[run[0] - offset].element) <= key),
                         runs[0] if runs else own[0])
            for i in group:
                if key < _isort_key(block[i - offset].element):
                    return _Placement(i, after=False, new_group=False)
            return _Placement(group[-1], after=True, new_group=False)
        earlier = [group for group, group_section in zip(groups, sections) if group_section < section]
        if earlier:
            return _Placement(earlier[-1][-1], after=True, new_group=True)
        return _Placement(groups[0][0], after=False, new_group=True)


_BLANK_LINE = re.compile(r'\n[ \t\r\f]*\n')


class _Section(IntEnum):
    FUTURE = 0
    STANDARD_LIBRARY = 1
    THIRD_PARTY = 2
    LOCAL = 3


def _section(module: str, local_roots: set[str]) -> _Section:
    if module.startswith('.'):
        return _Section.LOCAL
    root = module.split('.')[0]
    if root == '__future__':
        return _Section.FUTURE
    if root in sys.stdlib_module_names:
        return _Section.STANDARD_LIBRARY
    if root in local_roots:
        return _Section.LOCAL
    return _Section.THIRD_PARTY


def _module_of(statement: Import | MultiImport) -> str:
    """The module an import statement reads, with a leading dot where it is relative."""
    if isinstance(statement, MultiImport):
        if statement.from_ is None:
            return get_qualid_name(statement.names[0].qualid)
        leftmost = statement.from_
        while isinstance(leftmost, FieldAccess):
            leftmost = leftmost.target
        relative = isinstance(leftmost, Identifier) and leftmost.simple_name == ''
        return ('.' if relative else '') + get_name_string(statement.from_)
    return get_qualid_name(statement.qualid)


def _isort_key(statement: Import | MultiImport) -> tuple[bool, str]:
    """isort's order within a section: `import m` before `from m import n`, then by module."""
    is_from = isinstance(statement, MultiImport) and statement.from_ is not None
    return is_from, _module_of(statement).lower()


def _text_of(space: Space) -> str:
    return space.whitespace + ''.join(comment.suffix for comment in space.comments)


class _Placement(NamedTuple):
    index: int
    after: bool
    new_group: bool


def _insert(cu: CompilationUnit, padded_stmts: list, new_import: J, newline: str,
            placement: _Placement, first_import: Optional[int]) -> tuple[CompilationUnit, list]:
    separator = newline * 2 if placement.new_group else newline
    if not placement.after:
        # The new import takes over the separation ahead of the statement it precedes. The comments
        # above the file's first import head the file and stay above both.
        anchor = padded_stmts[placement.index]
        heads_imports = first_import is None or placement.index <= first_import
        prefix, attached = (anchor.element.prefix, []) if heads_imports \
            else _split_attached_comments(anchor.element.prefix)
        # Code keeps its distance from the imports above it.
        whitespace = anchor.element.prefix.whitespace
        if isinstance(anchor.element, (Import, MultiImport)) or not ('\n' in whitespace or '\r' in whitespace):
            whitespace = separator
        padded_stmts[placement.index] = anchor.replace(
            _element=anchor.element.replace(prefix=Space(attached, whitespace)))
        padded_stmts.insert(placement.index, JRightPadded(new_import.replace(prefix=prefix),
                                                          Space.EMPTY, Markers.EMPTY))
        return cu, padded_stmts

    at = placement.index + 1
    following = padded_stmts[at].element.prefix if at < len(padded_stmts) else cu.eof
    prefix, following = _split_trailing_comment(following, separator, newline)
    if at < len(padded_stmts):
        anchor = padded_stmts[at]
        padded_stmts[at] = anchor.replace(_element=anchor.element.replace(prefix=following))
    else:
        cu = cu.replace(eof=following)
    padded_stmts.insert(at, JRightPadded(new_import.replace(prefix=prefix), Space.EMPTY, Markers.EMPTY))
    return cu, padded_stmts


def _respace(cu: CompilationUnit, padded_stmts: list, at: int, change: Callable[[str], str]) -> CompilationUnit:
    """Apply `change` to the whitespace that opens the statement at `at`, or the end of the file past the last.
    A statement is replaced in `padded_stmts` itself."""
    if at == len(padded_stmts):
        return cu.replace(eof=cu.eof.replace(whitespace=change(cu.eof.whitespace)))
    padded = padded_stmts[at]
    prefix = padded.element.prefix
    padded_stmts[at] = padded.replace(
        _element=padded.element.replace(prefix=prefix.replace(whitespace=change(prefix.whitespace))))
    return cu


def _split_attached_comments(space: Space) -> tuple[Space, list]:
    """`space` up to the comment lines directly above the statement it precedes, and those comments.
    As in isort, such a comment moves with the statement."""
    comments = space.comments
    attached = len(comments)
    while attached > 0:
        before = comments[attached - 2].suffix if attached > 1 else space.whitespace
        on_own_line = '\n' in before or '\r' in before
        if _BLANK_LINE.search(comments[attached - 1].suffix) or not on_own_line:
            break
        attached -= 1
    return Space(comments[:attached], space.whitespace), comments[attached:]


def _split_trailing_comment(space: Space, separator: str, newline: str) -> tuple[Space, Space]:
    """The prefix of a statement inserted where `space` was, and what remains of `space` after it.

    A comment `space` opens without a line break ends the line of the statement before, and stays on it.
    """
    if not space.comments or '\n' in space.whitespace or '\r' in space.whitespace:
        return Space([], separator), space
    comment = space.comments[0]
    line_break = re.match(r'\r\n|\n|\r', comment.suffix)
    if line_break is None:
        return Space([], separator), space
    end = line_break.end()
    return (Space([comment.replace(suffix=comment.suffix[:end] + separator[len(newline):])], space.whitespace),
            Space(space.comments[1:], newline + comment.suffix[end:]))


def insert_member(elements: list, new_import: Import) -> list:
    """Insert ``new_import`` into a list of ``JRightPadded[Import]`` at its
    case-insensitive alphabetical position.

    The space after the ``import`` keyword lives in the surrounding
    ``JContainer.before``, so the element at index 0 carries an empty prefix
    while every later element carries a single-space prefix (the space after
    the separating comma). Trailing whitespace (e.g. before a ``)`` in a
    parenthesized import) lives in the last element's ``.after`` and must
    travel with whichever element ends up last.
    """
    insert_idx = _sorted_insert_index(elements, new_import)
    end = len(elements)

    if insert_idx == 0:
        prefix = Space.EMPTY
        if elements:
            # The displaced first element now follows a comma.
            first = elements[0]
            elements[0] = first.replace(
                _element=first.element.replace(prefix=Space.SINGLE_SPACE))
    else:
        prefix = Space.SINGLE_SPACE
    new_import = new_import.replace(prefix=prefix)

    if insert_idx == end and elements:
        # Appending at the end: the new element becomes the last, so any
        # trailing whitespace moves from the old last element onto it.
        last = elements[-1]
        elements[-1] = last.replace(_after=Space.EMPTY)
        after = last.after
    else:
        after = Space.EMPTY

    elements.insert(insert_idx, JRightPadded(new_import, after, Markers.EMPTY))
    return elements

def _sorted_insert_index(elements: list, new_import: Import) -> int:
    """Return the index at which the new member keeps the list in
    case-insensitive alphabetical order: the first existing member whose
    bound name sorts after the new member's, or the end if none does.

    Members are sorted by their bound name (alias if present, else the
    imported name), matching rewrite-javascript's comparator.
    """
    new_key = _sort_key(new_import)
    for i, padded in enumerate(elements):
        if new_key < _sort_key(padded.element):
            return i
    return len(elements)

def _sort_key(imp: Import) -> str:
    """Case-insensitive sort key for an imported member: its alias if it has
    one, otherwise the imported name."""
    return (get_alias_name(imp) or get_qualid_name(imp.qualid)).lower()


def create_import_statement(module: str, name: Optional[str] = None,
                            alias: Optional[str] = None) -> Import | MultiImport:
    """Build ``import module [as alias]`` or ``from module import name [as alias]``."""
    if name is None:
        # A parse gives a lone ``import module`` as a bare Import, its qualid prefixed by the
        # space after the keyword.
        import_elem = create_import_element(module, alias)
        return import_elem.replace(prefix=Space([], '\n'),
                                   qualid=import_elem.qualid.replace(prefix=Space.SINGLE_SPACE))
    else:
        # From import: from module import name [as alias]
        from_name = _create_module_name(module)
        # Add space prefix (the space between 'from' and module name)
        from_name = from_name.replace(prefix=Space.SINGLE_SPACE)
        import_elem = create_import_element(name, alias)
        return MultiImport(
            random_id(),
            Space([], '\n'),
            Markers.EMPTY,
            JRightPadded(from_name, Space.SINGLE_SPACE, Markers.EMPTY),
            False,  # Not parenthesized
            JContainer(
                Space.SINGLE_SPACE,
                [pad_right(import_elem)],
                Markers.EMPTY
            )
        )

def create_import_element(name: str, alias: Optional[str] = None) -> Import:
    """Build one member of an import statement: ``name [as alias]``."""
    qualid = _create_qualified_name(name)
    alias_left_padded = None
    if alias:
        alias_ident = Identifier(
            random_id(),
            Space.SINGLE_SPACE,
            Markers.EMPTY,
            [],
            alias,
            None,
            None
        )
        alias_left_padded = JLeftPadded(
            Space.SINGLE_SPACE,
            alias_ident,
            Markers.EMPTY
        )

    return Import(
        random_id(),
        Space.EMPTY,
        Markers.EMPTY,
        JLeftPadded(Space.EMPTY, False, Markers.EMPTY),
        qualid,
        alias_left_padded
    )

def _create_qualified_name(name: str) -> FieldAccess:
    """Create a FieldAccess for a qualified name."""
    parts = name.split('.')
    if len(parts) == 1:
        return FieldAccess(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            Empty(random_id(), Space.EMPTY, Markers.EMPTY),
            JLeftPadded(
                Space.EMPTY,
                Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], parts[0], None, None),
                Markers.EMPTY
            ),
            None
        )

    # Build nested FieldAccess for qualified names like "os.path"
    # Start with the first part as an Identifier target
    result: J = Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], parts[0], None, None)
    # Wrap remaining parts as FieldAccess nodes
    for part in parts[1:]:
        result = FieldAccess(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            result,
            JLeftPadded(
                Space.EMPTY,
                Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], part, None, None),
                Markers.EMPTY
            ),
            None
        )
    # For multi-part names, result is already a FieldAccess.
    # Wrap single-part Identifier in FieldAccess(Empty, name) for consistency
    # (but single-part is handled above, so this shouldn't happen)
    assert isinstance(result, FieldAccess)
    return result

def _create_module_name(name: str) -> J:
    """Create a name tree for use as a 'from' module name.

    Single-part names become Identifier; multi-part become FieldAccess.
    Unlike _create_qualified_name, this does NOT wrap single parts in
    FieldAccess(Empty, name) since the 'from' printer has no special
    handling for Empty targets.
    """
    parts = name.split('.')
    result: J = Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], parts[0], None, None)
    for part in parts[1:]:
        result = FieldAccess(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            result,
            JLeftPadded(
                Space.EMPTY,
                Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], part, None, None),
                Markers.EMPTY
            ),
            None
        )
    return result
