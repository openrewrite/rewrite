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
from typing import Dict, FrozenSet, List, Optional, Sequence, Set, Tuple

from rewrite.java import J
from rewrite.java.tree import Block, Identifier
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.binding_utils import Binding, ImportBindings, import_bindings, is_reference
from rewrite.python.import_utils import module_binding_name
from rewrite.python.scope_utils import scope_of
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.visitor import Cursor, TreeVisitor


@dataclass(frozen=True)
class ContextBinding:
    """One name a template's context binds."""

    name: str
    """The local name the template's code uses: the alias where the context gives one, else the
    member of a ``from`` import and :func:`module_binding_name` of a plain one."""

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
                bindings.append(ContextBinding(name.asname or module_binding_name(name.name),
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
    return is_reference(cursor, ident) and _shadowing_scope(cursor, ident.simple_name) is None


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


def bind_context(visitor: TreeVisitor, cursor: Cursor,
                 bindings: Sequence[ContextBinding]) -> Dict[str, str]:
    """Binds each of ``bindings`` in the file ``visitor`` is visiting, as read from ``cursor``,
    and returns the names to rename the template's references to where the file already binds a
    module under another name. A name held by something else raises, no import being able to make
    it read the module."""
    existing = import_bindings(visitor)
    declared = {binding.name for binding in bindings}
    renames: Dict[str, str] = {}
    additions: List[ContextBinding] = []
    for binding in bindings:
        if _imported_locally(cursor, binding):
            continue
        bound = next((b.name for b in existing if b.module == binding.module
                      and b.member == binding.member and not b.guarded), None)
        # The spliced code reads the file's name for the module wherever it has one, so that is
        # the name whose scope decides whether the splice can reach the module at all.
        name = bound or binding.name
        if _shadowing_scope(cursor, name) is not None:
            raise ValueError(
                f"A scope at the splice site binds '{name}', so the spliced code would read that "
                f"rather than {_describe(binding)}. No import reaches a shadowed name; apply the "
                f"template where '{name}' is free.")
        if bound is None:
            if _taken(existing, cursor, binding):
                raise ValueError(
                    f"The file binds '{binding.name}' to something other than {_describe(binding)}, "
                    f"so importing it under that name would rebind what the file already reads. "
                    f"Import it under an alias the file leaves free — "
                    f"context=[\"{_as_alias(binding)}\"].")
            additions.append(binding)
        elif bound != binding.name:
            if bound in declared:
                raise ValueError(
                    f"The file binds {_describe(binding)} as '{bound}', which this template's "
                    f"context binds to something else, so renaming to it would read one for the "
                    f"other. Import it under an alias the template does not already use.")
            renames[binding.name] = bound
    # Every binding holds before any import is registered, so a refusal leaves the file alone.
    for binding in additions:
        maybe_add_import(visitor, AddImportOptions(
            module=binding.module, name=binding.member, alias=binding.alias))
    return renames


def _imported_locally(cursor: Cursor, binding: ContextBinding) -> bool:
    """Whether a scope between the splice and the module already imports what ``binding`` names,
    as a function importing lazily does. A conditional import binds nothing at runtime, and an
    import the splice is reached before makes the name local to the whole call rather than bound
    where the splice reads it."""
    scope = _shadowing_scope(cursor, binding.name)
    if scope is None:
        return False
    held = _scope_imports(scope, cursor).for_name(binding.name)
    return held is not None and not held.guarded and _binds(held, binding)


def _describe(binding: ContextBinding) -> str:
    return (f"'{binding.member}' from '{binding.module}'" if binding.member is not None
            else f"the module '{binding.module}'")


def _as_alias(binding: ContextBinding) -> str:
    return (f"from {binding.module} import {binding.member} as ..." if binding.member is not None
            else f"import {binding.module} as ...")


def _shadowing_scope(cursor: Cursor, name: str) -> Optional[J]:
    """The innermost scope binding ``name`` between the splice and the module, None where the
    module scope is what decides the name."""
    scope = scope_of(cursor).declaring_scope(name)
    return None if scope is None or isinstance(scope, CompilationUnit) else scope


def _scope_imports(scope: J, cursor: Cursor) -> ImportBindings:
    """What the imports of ``scope`` ahead of the splice bind. An import nested deeper — under a
    ``with``, in a loop — is not reported, so the splice is refused rather than accepted."""
    body = getattr(scope, 'body', None)
    if not isinstance(body, Block):
        return import_bindings(())
    holding = {id(node) for node in cursor.get_path()}
    preceding = []
    for statement in body.statements:
        if id(statement) in holding:
            break
        preceding.append(statement)
    return import_bindings(preceding)


def _taken(existing: ImportBindings, cursor: Cursor, binding: ContextBinding) -> bool:
    """Whether the file's module scope already reads ``binding.name`` as something else. An
    import of the same module under that name is not something else, however it is guarded."""
    if scope_of(cursor).declaring_scope(binding.name) is None:
        return False
    held = existing.for_name(binding.name)
    return not _binds(held, binding) and not _same_package(held, binding)


def _binds(held: Optional[Binding], binding: ContextBinding) -> bool:
    return (held is not None and held.module == binding.module
            and held.member == binding.member)


def _same_package(held: Optional[Binding], binding: ContextBinding) -> bool:
    """Whether an import already holding the name binds the same package: ``import a.b`` and
    ``import a.c`` both bind ``a`` to it."""
    return (held is not None and held.member is None and binding.member is None
            and binding.alias is None and held.name == binding.name
            and module_binding_name(held.module) == module_binding_name(binding.module))


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
