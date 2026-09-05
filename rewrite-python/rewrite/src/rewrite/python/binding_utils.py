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
from rewrite.java.tree import (Assignment, Case, FieldAccess, ForEachLoop, Identifier, If,
                               Import, MethodInvocation, Parentheses)
from rewrite.python.import_utils import get_alias_name, unconditional_body
from rewrite.python.scope_utils import captures, scope_of
from rewrite.python.tree import (ChainedAssignment, CollectionLiteral, ComprehensionExpression,
                                 CompilationUnit, ExpressionStatement, MatchCase, MultiImport,
                                 NamedArgument, Star, TypeHintedExpression, VariableScope)
from rewrite.visitor import TreeVisitor

_IMPORT_BINDINGS = 'org.openrewrite.python.importBindings'

# The wrappers a target puts between a statement and the names it binds, as `scope_utils`
# unwraps them to collect those names.
_TARGET_WRAPPERS = (Parentheses, Star, CollectionLiteral, ExpressionStatement, TypeHintedExpression)


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
        self._by_name: Dict[str, Binding] = {}
        for binding in self._bindings:
            held = self._by_name.get(binding.name)
            # A later import of a name replaces an earlier one, as running the statements would,
            # except that a guarded import binds nothing at runtime for an unguarded one to lose to.
            if binding.member != '*' and (held is None or held.guarded or not binding.guarded):
                self._by_name[binding.name] = binding

    def __iter__(self) -> Iterator[Binding]:
        return iter(self._bindings)

    def __len__(self) -> int:
        return len(self._bindings)

    def for_name(self, name: str) -> Optional[Binding]:
        """The binding ``name`` holds, or None where no import binds it — which is weaker than
        ``name`` being free, since a wildcard import and an import under any ``try`` both bind
        names no ``Binding`` records."""
        return self._by_name.get(name)

    def for_module(self, module: str, member: Optional[str] = None) -> Tuple[Binding, ...]:
        """Every binding of ``module`` — of its ``member`` when one is given. A module can be
        bound more than once, which a recipe removing the import has to reckon with: taking
        the statement out takes every name it bound."""
        return tuple(b for b in self._bindings
                     if b.module == module and (member is None or b.member == member))

    def reference(self, cursor: Cursor, ident: Identifier) -> Optional[Binding]:
        """The binding ``ident`` reads, or None where it reads something else: a name in
        member position, a name nothing imports, or one an enclosing scope rebinds. None is not
        "unused": a quoted forward reference holds an expression, so a use census composes
        :func:`is_reference` with :func:`referenced_names` rather than calling this. Check
        :attr:`Binding.guarded` before emitting a runtime reference."""
        binding = self._by_name.get(ident.simple_name)
        if binding is None or not is_reference(cursor, ident):
            return None
        declaring = scope_of(cursor).declaring_scope(ident.simple_name)
        return None if declaring is not None and not isinstance(declaring, CompilationUnit) else binding


def import_bindings(source: Union[TreeVisitor[Any, Any], Cursor, CompilationUnit,
                                  Iterable[Statement]]) -> ImportBindings:
    """The bindings the imports of ``source`` introduce: a visitor or a cursor answers for the
    file being visited and scans it once, a compilation unit or statement list scans on every
    call.

    A scan descends into an ``if`` that has no ``else`` and into no ``try`` whatever it catches,
    so a fallback import's shim is never taken for the module — see :attr:`Binding.guarded`.
    """
    if isinstance(source, CompilationUnit):
        return ImportBindings(_scan(source.statements, guarded=False))
    if isinstance(source, TreeVisitor):
        return _cached(source.cursor)
    if isinstance(source, Cursor):
        return _cached(source)
    return ImportBindings(_scan(source, guarded=False))


def resolves_in_scope(cursor: Cursor, ident: Identifier) -> bool:
    """Whether ``ident`` names something the enclosing scopes bind, rather than a member of
    another namespace: an attribute, a keyword argument, or a name an import spells out.
    Declarations and writes of a binding answer True, which is what a rename follows, and
    position is all it reads, so pair it with a scope lookup. ``cursor`` stands on ``ident``
    or on its parent, letting a visitor ask about a node's own child from where it is.
    """
    node, parent, _ = _position(cursor, ident)
    return _resolves(node, parent)


def is_reference(cursor: Cursor, ident: Identifier) -> bool:
    """Whether ``ident`` reads a name: :func:`resolves_in_scope` less the declarations, the
    targets a statement binds and the names a ``global`` or ``nonlocal`` declares.
    """
    node, parent, dotted = _position(cursor, ident)
    if not _resolves(node, parent) or _declares(node, parent):
        return False
    if isinstance(parent, VariableScope):
        return False
    return dotted or not _is_target(parent, node)


def _position(cursor: Cursor, ident: Identifier) -> Tuple[J, Optional[J], bool]:
    """The node standing for ``ident`` where it sits, the node above that, and whether a
    dotted name carried it there."""
    node: J = ident
    dotted = False
    for enclosing in _enclosing_nodes(cursor, ident):
        # A dotted name reads through its root, and a tuple or starred target spreads over the
        # names under it, so in both the enclosing node decides for the one below.
        if isinstance(enclosing, FieldAccess) and enclosing.target is node:
            node, dotted = enclosing, True
        elif isinstance(enclosing, _TARGET_WRAPPERS):
            node = enclosing
        else:
            return node, enclosing, dotted
    return node, None, dotted


def _resolves(node: J, parent: Optional[J]) -> bool:
    """Whether the scopes decide what ``node`` names, rather than the node above it."""
    if isinstance(parent, (Import, MultiImport)):
        # An import names the module or member it binds, and holds it on `qualid`, not `name`.
        return False
    if isinstance(parent, MethodInvocation):
        # A call selecting nothing calls a name it resolved; with a receiver it is a member.
        return parent.name is not node or parent.select is None
    if isinstance(parent, (FieldAccess, NamedArgument)):
        # An attribute belongs to its target and a keyword argument to the callee's signature.
        return parent.name is not node
    if isinstance(parent, MatchCase.Pattern) and parent.kind is MatchCase.Pattern.Kind.KEYWORD:
        # A class pattern's `attr=p` names an attribute of the class it matches.
        return parent.children[0] is not node
    return True


def _declares(node: J, parent: Optional[J]) -> bool:
    """Whether ``parent`` introduces ``node`` as a name of its own: a ``def``, a ``class``, a
    parameter, a type alias. A receiverless call holds on ``name`` a name it resolved."""
    return not isinstance(parent, MethodInvocation) and getattr(parent, 'name', None) is node


def _is_target(parent: Optional[J], node: J) -> bool:
    """Whether ``node`` is a target ``parent`` binds rather than an expression it evaluates."""
    if isinstance(parent, Assignment):
        return parent.variable is node
    if isinstance(parent, ChainedAssignment):
        return any(variable is node for variable in parent.variables)
    if isinstance(parent, ForEachLoop.Control):
        return parent.variable is node
    if isinstance(parent, ComprehensionExpression):
        # A clause holds its target directly, so the comprehension is the node above the name.
        return any(clause.iterator_variable is node for clause in parent.clauses)
    if isinstance(parent, Case):
        return any(capture is node for label in parent.case_labels for capture in captures(label))
    # A pattern nested in another holds its own cursor, so the case above it is out of reach.
    return (isinstance(parent, (MatchCase, MatchCase.Pattern))
            and any(capture is node for capture in captures(parent)))


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


__all__ = ['Binding', 'ImportBindings', 'import_bindings', 'is_reference',
           'resolves_in_scope']
