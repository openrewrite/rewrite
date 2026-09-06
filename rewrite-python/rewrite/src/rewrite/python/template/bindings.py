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
from typing import Dict, Optional, Sequence, Tuple

from rewrite.java import J
from rewrite.java.tree import Identifier
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.python.binding_utils import import_bindings, is_reference
from rewrite.python.visitor import PythonVisitor


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


def bind_context(visitor: PythonVisitor, bindings: Sequence[ContextBinding]) -> Dict[str, str]:
    """Binds each of ``bindings`` in the file ``visitor`` is visiting, and returns the names to
    rename the template's references to where the file already binds a module under another name.
    A conditional import binds nothing a spliced reference reaches, so it counts as unbound."""
    existing = import_bindings(visitor)
    renames: Dict[str, str] = {}
    for binding in bindings:
        bound = next((b.name for b in existing if b.module == binding.module
                      and b.member == binding.member and not b.guarded), None)
        if bound is None:
            maybe_add_import(visitor, AddImportOptions(
                module=binding.module, name=binding.member, alias=binding.alias))
        elif bound != binding.name:
            renames[binding.name] = bound
    return renames


class RenameBindings(PythonVisitor[None]):
    """Renames a template's references to a context binding to the name the target file uses."""

    def __init__(self, renames: Dict[str, str]) -> None:
        super().__init__()
        self._renames = renames

    def visit_identifier(self, ident: Identifier, p: None) -> J:
        renamed = self._renames.get(ident.simple_name)
        if renamed is not None and is_reference(self.cursor, ident):
            return ident.replace(simple_name=renamed)
        return super().visit_identifier(ident, p)
