# Copyright 2026 the original author or authors.
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

"""Which names a file's imports bind, and whether an identifier references one.

Structural throughout: a parse without a ``ty_client`` attributes no types, and a recipe
gated on attribution then finds nothing while reporting success. Types widen matching;
they never decide it.
"""

from dataclasses import dataclass
from typing import Any, Dict, Iterable, Iterator, List, Optional, Sequence, Tuple, Union

from rewrite import Cursor
from rewrite.java.support_types import J, Statement
from rewrite.java.tree import (Assignment, FieldAccess, ForEachLoop, Identifier, If, Import,
                               MethodInvocation, Parentheses)
from rewrite.python.import_utils import get_alias_name, unconditional_body
from rewrite.python.scope_utils import scope_of
from rewrite.python.tree import (ChainedAssignment, CollectionLiteral, ComprehensionExpression,
                                 CompilationUnit, Del, ExpressionStatement, MultiImport, Star,
                                 TypeHintedExpression, VariableScope)

# The wrappers a target puts between a statement and the names it binds, as `scope_utils`
# unwraps them to collect those names.
_TARGET_WRAPPERS = (Parentheses, Star, CollectionLiteral, ExpressionStatement, TypeHintedExpression)
from rewrite.visitor import TreeVisitor

_IMPORT_BINDINGS = 'org.openrewrite.python.importBindings'


@dataclass(frozen=True)
class Binding:
    """One name an import binds."""

    name: str
    """The local name: the alias where there is one, else the member of a ``from`` import and
    the root package of ``import a.b.c``, which binds only ``a``."""

    module: str
    """The module as written, keeping a relative import's leading dots: ``.locale`` is a
    sibling module and never the standard library's ``locale``."""

    member: Optional[str]
    """The member of ``from <module> import <member>``, None for ``import <module>``, ``'*'``
    for a wildcard."""

    imp: Import
    """The node binding this one name; a ``MultiImport`` holds several."""

    guarded: bool
    """Whether an enclosing ``if`` decides the binding, as ``if TYPE_CHECKING:`` does. A
    reference emitted at runtime may then find no name."""


class ImportBindings:
    """The names a file's imports bind, reached through :func:`import_bindings`."""

    __slots__ = ('_bindings', '_by_name')

    def __init__(self, bindings: Sequence[Binding]) -> None:
        self._bindings = tuple(bindings)
        # A second import of a name replaces the first, as running the statements would.
        self._by_name: Dict[str, Binding] = {b.name: b for b in self._bindings if b.member != '*'}

    def __iter__(self) -> Iterator[Binding]:
        return iter(self._bindings)

    def __len__(self) -> int:
        return len(self._bindings)

    def for_name(self, name: str) -> Optional[Binding]:
        """The binding ``name`` holds, or None where no import binds it — which is weaker than
        ``name`` being free, since a wildcard import and a ``try``/``except ImportError``
        fallback both bind names no ``Binding`` records."""
        return self._by_name.get(name)

    def for_module(self, module: str, member: Optional[str] = None) -> Tuple[Binding, ...]:
        """Every binding of ``module`` — of its ``member`` when one is given. A module can be
        bound more than once, which a recipe removing the import has to reckon with: taking
        the statement out takes every name it bound."""
        return tuple(b for b in self._bindings
                     if b.module == module and (member is None or b.member == member))

    def reference(self, cursor: Cursor, ident: Identifier) -> Optional[Binding]:
        """The binding ``ident`` reads, or None where it reads something else: a name in
        member position, a name nothing imports, or one an enclosing scope rebinds. A quoted
        forward reference holds an expression rather than a name, so its names come from
        :func:`referenced_names` instead. Check :attr:`Binding.guarded` before emitting a
        reference that has to resolve at runtime."""
        binding = self._by_name.get(ident.simple_name)
        if binding is None or not is_reference(cursor, ident):
            return None
        declaring = scope_of(cursor).declaring_scope(ident.simple_name)
        return None if declaring is not None and not isinstance(declaring, CompilationUnit) else binding


def import_bindings(source: Union[TreeVisitor[Any, Any], CompilationUnit,
                                  Iterable[Statement]]) -> ImportBindings:
    """The bindings the imports of ``source`` introduce: a visitor answers for the file it is
    visiting and scans it once, a compilation unit or statement list scans on every call.

    A scan descends into the body of an ``if`` that has no ``else``, so a ``try``/``except
    ImportError`` fallback never has its shim taken for the module — see :attr:`Binding.guarded`.
    """
    if isinstance(source, CompilationUnit):
        return ImportBindings(_scan(source.statements, guarded=False))
    if isinstance(source, TreeVisitor):
        return _cached(source.cursor)
    return ImportBindings(_scan(source, guarded=False))


def is_reference(cursor: Cursor, ident: Identifier) -> bool:
    """Whether ``ident`` reads a name, rather than filling a slot that names something.

    Position is all this reads, so a name an enclosing scope rebinds still answers True;
    :meth:`ImportBindings.reference` composes the two. ``cursor`` stands on ``ident`` or on
    its parent, which lets a visitor ask about a node's own child from where it already is.
    """
    node: J = ident
    parent: Optional[J] = None
    dotted = False
    for enclosing in _enclosing_nodes(cursor, ident):
        # A dotted name reads through its root, and a tuple or starred target spreads over the
        # names under it, so in both the enclosing node decides for the one below.
        if isinstance(enclosing, FieldAccess) and enclosing.target is node:
            node, dotted = enclosing, True
        elif isinstance(enclosing, _TARGET_WRAPPERS):
            node = enclosing
        else:
            parent = enclosing
            break

    if isinstance(parent, (Import, MultiImport, VariableScope)):
        # An import names the module or member it binds; `global x` names a binding elsewhere.
        return False
    if isinstance(parent, MethodInvocation):
        # A call selecting nothing calls a name it read; with a receiver the name is a member.
        return parent.name is not node or parent.select is None
    if not dotted and _is_target(parent, node):
        return False
    # Every other node that names rather than reads keeps the name on `name`: an attribute, a
    # keyword argument, a `def`, a `class`, a parameter, a type alias.
    return getattr(parent, 'name', None) is not node


def _is_target(parent: Optional[J], node: J) -> bool:
    """Whether ``node`` is a target ``parent`` binds. Only an undotted name asks: ``json.x = 1``
    reads ``json`` to assign through it."""
    if isinstance(parent, Assignment):
        return parent.variable is node
    if isinstance(parent, ChainedAssignment):
        return any(variable is node for variable in parent.variables)
    if isinstance(parent, ForEachLoop.Control):
        return parent.variable is node
    if isinstance(parent, ComprehensionExpression.Clause):
        return parent.iterator_variable is node
    return isinstance(parent, Del)


def _enclosing_nodes(cursor: Cursor, ident: Identifier) -> Iterator[J]:
    """The nodes enclosing ``ident``, innermost first, padding wrappers left out."""
    c: Optional[Cursor] = cursor
    while c is not None:
        value = c.value
        if isinstance(value, J) and value is not ident:
            yield value
        c = c.parent


def _cached(cursor: Cursor) -> ImportBindings:
    """The scan of the file being visited, kept on its cursor as ``scope_utils`` keeps its own."""
    for c in cursor.get_path_as_cursors():
        if isinstance(c.value, CompilationUnit):
            bindings = c.get_message(_IMPORT_BINDINGS, None)
            if bindings is None:
                bindings = ImportBindings(_scan(c.value.statements, guarded=False))
                c.put_message(_IMPORT_BINDINGS, bindings)
            return bindings
    return ImportBindings(())


def _scan(statements: Iterable[Statement], guarded: bool) -> List[Binding]:
    """The bindings of ``statements`` in source order, which is the order that decides which
    import of a name is the one in force."""
    bindings: List[Binding] = []
    for stmt in statements:
        if isinstance(stmt, MultiImport):
            module = _dotted_path(stmt.from_) if stmt.from_ is not None else None
            bindings.extend(_binding(imp, module, guarded) for imp in stmt.names)
        elif isinstance(stmt, Import):
            bindings.append(_binding(stmt, None, guarded))
        elif isinstance(stmt, If):
            body = unconditional_body(stmt)
            if body is not None:
                bindings.extend(_scan(body.statements, guarded=True))
    return bindings


def _binding(imp: Import, from_module: Optional[str], guarded: bool) -> Binding:
    qualid = _dotted_path(imp.qualid)
    alias = get_alias_name(imp)
    if from_module is not None:
        return Binding(alias or qualid, from_module, qualid, imp, guarded)
    return Binding(alias or qualid.split('.')[0], qualid, None, imp, guarded)


def _dotted_path(name: Optional[J]) -> str:
    """A name tree as written, keeping the empty parts a relative import's leading dots parse to."""
    parts: List[str] = []

    def collect(node: Optional[J]) -> None:
        if isinstance(node, FieldAccess):
            collect(node.target)
            parts.append(node.name.simple_name)
        elif isinstance(node, Identifier):
            parts.append(node.simple_name)

    collect(name)
    return '.'.join(parts)


__all__ = ['Binding', 'ImportBindings', 'import_bindings', 'is_reference']
