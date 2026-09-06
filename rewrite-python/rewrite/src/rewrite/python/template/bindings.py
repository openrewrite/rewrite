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

"""The modules a template's context binds, and how they reach the file it is spliced into."""

from __future__ import annotations

import ast
from dataclasses import dataclass
from typing import Dict, FrozenSet, Optional, Sequence, Set, Tuple

from rewrite.java import J
from rewrite.java.tree import Identifier
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.binding_utils import Binding, ImportBindings, import_bindings, is_reference
from rewrite.python.scope_utils import LocalBindings, scope_of
from rewrite.python.visitor import PythonVisitor
from rewrite.visitor import Cursor, TreeVisitor


@dataclass(frozen=True)
class ContextBinding:
    """One name a template's context binds."""

    name: str
    """The local name the template's code uses: the alias where the context gives one, else the
    member of a ``from`` import and the root package of ``import a.b.c``."""

    module: str
    """The module as written, keeping a relative import's leading dots."""

    member: Optional[str]
    """The member of ``from <module> import <member>``, None for ``import <module>``."""

    alias: Optional[str]
    """The name the context imports under, when it renames it."""


def context_bindings(context: Sequence[str]) -> Tuple[ContextBinding, ...]:
    """The modules ``context`` binds. Context that binds nothing importable — a type alias, a
    stub definition — yields nothing, and neither does context that does not parse, which the
    template's own parse reports."""
    try:
        parsed = ast.parse('\n'.join(context))
    except SyntaxError:
        return ()

    bindings = []
    for node in parsed.body:
        if isinstance(node, ast.Import):
            for name in node.names:
                bindings.append(ContextBinding(name.asname or name.name.split('.')[0],
                                               name.name, None, name.asname))
        elif isinstance(node, ast.ImportFrom):
            module = '.' * node.level + (node.module or '')
            for name in node.names:
                # A wildcard's names are a property of the module, so there is nothing here to
                # recognise in the target file or to add to it.
                if name.name != '*':
                    bindings.append(ContextBinding(name.asname or name.name,
                                                   module, name.name, name.asname))
    return tuple(bindings)


def reads_context_name(cursor: Cursor, ident: Identifier) -> bool:
    """Whether ``ident`` reads a name the context binds. A name the template binds for itself is
    its own, whatever the context calls the same spelling."""
    return is_reference(cursor, ident) and not LocalBindings().is_bound(cursor, ident.simple_name)


def names_read(tree: J) -> FrozenSet[str]:
    """The context names ``tree`` reads, which are the ones the target file has to bind."""
    names: Set[str] = set()

    class Scan(PythonVisitor[None]):
        def visit_identifier(self, ident: Identifier, p: None) -> J:
            if reads_context_name(self.cursor, ident):
                names.add(ident.simple_name)
            return ident

    Scan().visit(tree, None)
    return frozenset(names)


def bind_context(visitor: TreeVisitor, bindings: Sequence[ContextBinding]) -> Dict[str, str]:
    """Binds each of ``bindings`` in the file ``visitor`` is visiting, and returns the names to
    rename the template's references to where the file already binds a module under another name.
    A conditional import binds nothing a spliced reference reaches, so it counts as unbound, and
    a name held by something else raises, no import being able to make it read the module."""
    existing = import_bindings(visitor)
    cursor = visitor.cursor
    renames: Dict[str, str] = {}
    for binding in bindings:
        bound = next((b.name for b in existing if b.module == binding.module
                      and b.member == binding.member and not b.guarded), None)
        name = bound or binding.name
        if LocalBindings().is_bound(cursor, name):
            raise ValueError(
                f"The scope at the splice site binds '{name}' to something other than "
                f"'{binding.module}', so the spliced code would read that instead. No import "
                f"reaches a shadowed name; apply the template where '{name}' is free.")
        if bound is None:
            if _taken(existing, cursor, binding):
                raise ValueError(
                    f"The file binds '{name}' to something other than '{binding.module}', so "
                    f"importing the module under that name would rebind it. Import it under an "
                    f"alias the file leaves free — context=[\"import {binding.module} as ...\"].")
            maybe_add_import(visitor, AddImportOptions(
                module=binding.module, name=binding.member, alias=binding.alias))
        elif bound != binding.name:
            renames[binding.name] = bound
    return renames


def _taken(existing: ImportBindings, cursor: Cursor, binding: ContextBinding) -> bool:
    """Whether the file's module scope already reads ``binding.name`` as something else."""
    if scope_of(cursor).declaring_scope(binding.name) is None:
        return False
    return not _same_package(existing.for_name(binding.name), binding)


def _same_package(held: Optional[Binding], binding: ContextBinding) -> bool:
    """Whether an import already holding the name binds the same package: ``import a.b`` and
    ``import a.c`` both bind ``a`` to it."""
    return (held is not None and held.member is None and binding.member is None
            and binding.alias is None and held.name == binding.name
            and held.module.split('.')[0] == binding.module.split('.')[0])


class RenameBindings(PythonVisitor[None]):
    """Renames a template's references to a context binding to the name the target file uses."""

    def __init__(self, renames: Dict[str, str]) -> None:
        super().__init__()
        self._renames = renames

    def visit_identifier(self, ident: Identifier, p: None) -> J:
        renamed = self._renames.get(ident.simple_name)
        if renamed is not None and reads_context_name(self.cursor, ident):
            return ident.replace(simple_name=renamed)
        return super().visit_identifier(ident, p)
