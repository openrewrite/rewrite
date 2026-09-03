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

"""Which names a Python scope binds, and which scope binds a name at a given cursor."""

from typing import Any, Callable, Dict, FrozenSet, List, Optional, Set

from rewrite import Cursor
from rewrite.java import J
from rewrite.java.tree import (Assignment, AssignmentOperation, ClassDeclaration, ForEachLoop,
                               Identifier, Import, Lambda, MethodDeclaration, Parentheses,
                               VariableDeclarations)
from rewrite.python.import_utils import get_alias_name, get_qualid_name
from rewrite.python.tree import (ChainedAssignment, CollectionLiteral, CompilationUnit,
                                 ComprehensionExpression, ExpressionStatement, MultiImport, Star,
                                 TypeHintedExpression, VariableScope)
from rewrite.python.visitor import PythonVisitor

_NO_NAMES: FrozenSet[str] = frozenset()

# The nodes _BindingScan answers for; everything else binds nothing and is not a scope.
_SCOPES = (MethodDeclaration, Lambda, ClassDeclaration, ComprehensionExpression, CompilationUnit)

_SCOPE_NAMES = 'org.openrewrite.python.scopeNames'


class Scope:
    """One scope: the names it binds itself, the scopes around it, and what they answer
    together. Reached through :func:`scope_of`."""

    __slots__ = ('_chain', '_index', '_cache')

    def __init__(self, chain: List[J], index: int, cache: Dict[J, FrozenSet[str]]) -> None:
        self._chain = chain
        self._index = index
        self._cache = cache

    def names(self) -> FrozenSet[str]:
        """The names this scope binds itself, which is not what it reaches: for that, ask
        :meth:`declares`."""
        return (_names(self._chain[self._index], self._cache)
                if self._index < len(self._chain) else _NO_NAMES)

    def walk(self, visit: Callable[['Scope'], bool]) -> None:
        """Visit this scope and then each one enclosing it, innermost first, stopping where
        ``visit`` returns False."""
        for index in range(self._index, len(self._chain)):
            if not visit(Scope(self._chain, index, self._cache)):
                return

    def declares(self, name: str) -> bool:
        """Whether this scope or one enclosing it binds ``name``."""
        return self.declaring_scope(name) is not None

    def declaring_scope(self, name: str) -> Optional[J]:
        """The node owning the innermost scope that binds ``name`` — the compilation unit for
        a module-level binding, otherwise the ``def``, ``class``, ``lambda`` or comprehension
        holding it — or None where nothing in scope does. A caller holding a declaration asks
        whether this is the node it came from: anything nearer shadows it."""
        for scope in self._chain[self._index:]:
            if name in _names(scope, self._cache):
                return scope
        return None


def scope_of(cursor: Cursor) -> Scope:
    """The innermost scope holding ``cursor``, which a visitor's own cursor rarely is: it
    stands on whatever node it is visiting.

    Syntactic, because ``field_type`` cannot decide it: it is unset for every identifier
    when type attribution is unavailable.
    """
    return Scope(_enclosing_scopes(cursor), 0, _names_cache(cursor))


class LocalBindings:
    """Whether an identifier is a local of some enclosing scope rather than a reference to a
    module-level import — :func:`scope_of` narrowed to that one question."""

    def is_bound(self, cursor: Cursor, name: str) -> bool:
        scope = scope_of(cursor).declaring_scope(name)
        return scope is not None and not isinstance(scope, CompilationUnit)


def _names_cache(cursor: Cursor) -> Dict[J, FrozenSet[str]]:
    """Where a scan of one scope is kept, so a traversal scans each scope once. Hung on the
    compilation unit's cursor, which lives exactly as long as the visit of that file; the
    root cursor ``TreeVisitor`` starts from is a class attribute shared process-wide."""
    for c in cursor.get_path_as_cursors():
        if isinstance(c.value, CompilationUnit):
            cache = c.get_message(_SCOPE_NAMES, None)
            if cache is None:
                cache = {}
                c.put_message(_SCOPE_NAMES, cache)
            return cache
    return {}


def _names(scope: J, cache: Dict[J, FrozenSet[str]]) -> FrozenSet[str]:
    names = cache.get(scope)
    if names is None:
        names = frozenset(_BindingScan(scope).run())
        cache[scope] = names
    return names


def _enclosing_scopes(cursor: Optional[Cursor]) -> List[J]:
    """The scopes a reference resolves through, innermost first.

    A scope covers only the region Python evaluates inside it: a ``def``'s decorators,
    annotations and parameter defaults belong to the scope enclosing it. A class body is
    reachable only from directly within it, never from a function nested in it, nor from
    a class nested in it.
    """
    path = _tree_path(cursor)
    scopes: List[J] = []
    for i, node in enumerate(path):
        if not isinstance(node, _SCOPES):
            continue
        if not _governs(node, i, path):
            continue
        if isinstance(node, ClassDeclaration) and scopes:
            continue
        scopes.append(node)
    return scopes


def _tree_path(cursor: Optional[Cursor]) -> List[Any]:
    """The tree nodes from the cursor outwards, skipping padding wrappers."""
    path = []
    while cursor is not None:
        value = cursor.value
        if isinstance(value, (J, ComprehensionExpression.Clause)):
            path.append(value)
        cursor = cursor.parent
    return path


def _governs(scope: J, index: int, path: List[Any]) -> bool:
    """Whether ``scope``'s bindings reach ``path[:index]``, the nodes it holds on the path."""
    if index == 0:
        # The cursor stands on the scope node itself, which whatever encloses it evaluates —
        # and a compilation unit is enclosed by nothing.
        return isinstance(scope, CompilationUnit)
    child = path[index - 1]
    if isinstance(scope, MethodDeclaration):
        return child is scope.body or (isinstance(child, VariableDeclarations) and _is_parameter_name(path))
    if isinstance(scope, Lambda):
        return child is scope.body or (child is scope.parameters and _is_parameter_name(path))
    if isinstance(scope, ClassDeclaration):
        return child is scope.body
    if isinstance(scope, ComprehensionExpression):
        # A comprehension's leading iterable is evaluated before its targets exist.
        leading = scope.clauses[0].iterated_list if scope.clauses else None
        return leading is None or all(node is not leading for node in path[:index])
    return True


def _is_parameter_name(path: List[Any]) -> bool:
    return (len(path) > 1 and isinstance(path[1], VariableDeclarations.NamedVariable)
            and path[1].name is path[0])


def _bound_import_name(imp: Import, from_: Optional[Any]) -> str:
    """The name an import binds: its alias, else the member for ``from X import
    Y``, else the root package of a dotted ``import a.b.c``."""
    alias = get_alias_name(imp)
    if alias:
        return alias
    qualid = get_qualid_name(imp.qualid)
    return qualid if from_ is not None else qualid.split('.')[0]


class _BindingScan(PythonVisitor[Any]):
    """The names ``scope`` binds directly: assignment targets, parameters, loop and
    comprehension targets, ``as`` clauses, imports, and nested ``def``/``class`` names.
    """

    def __init__(self, scope: J) -> None:
        super().__init__()
        self._scope = scope
        self._names: Set[str] = set()
        self._declared_elsewhere: Set[str] = set()

    def run(self) -> Set[str]:
        self.visit(self._scope, None)
        return self._names - self._declared_elsewhere

    def _bind(self, target: Optional[J]) -> None:
        """Bind the names a target expression introduces. Attribute and subscript
        targets (``self.x``, ``d[k]``) assign through an object without binding."""
        if isinstance(target, Identifier):
            self._names.add(target.simple_name)
        elif isinstance(target, ExpressionStatement):
            self._bind(target.expression)
        elif isinstance(target, TypeHintedExpression):
            self._bind(target.expression)
        elif isinstance(target, Parentheses):
            self._bind(target.tree)
        elif isinstance(target, Star):
            self._bind(target.expression)
        elif isinstance(target, CollectionLiteral):
            for element in target.elements:
                self._bind(element)

    def _is_nested(self, scope: J, name: Optional[Identifier] = None) -> bool:
        """True once past the scope being scanned, binding the nested scope's own
        name (a ``def``/``class``) in the scope that encloses it."""
        if scope is self._scope:
            return False
        self._bind(name)
        return True

    def visit_method_declaration(self, method: MethodDeclaration, p: Any) -> J:
        if self._is_nested(method, method.name):
            return method
        return super().visit_method_declaration(method, p)

    def visit_class_declaration(self, class_decl: ClassDeclaration, p: Any) -> J:
        if self._is_nested(class_decl, class_decl.name):
            return class_decl
        return super().visit_class_declaration(class_decl, p)

    def visit_lambda(self, lambda_: Lambda, p: Any) -> J:
        if self._is_nested(lambda_):
            return lambda_
        return super().visit_lambda(lambda_, p)

    def visit_comprehension_expression(self, comp: ComprehensionExpression, p: Any) -> J:
        if self._is_nested(comp):
            return comp
        return super().visit_comprehension_expression(comp, p)

    def visit_variable_scope(self, scope: VariableScope, p: Any) -> J:
        for name in scope.names:
            self._declared_elsewhere.add(name.simple_name)
        return scope

    def visit_import(self, import_: Import, p: Any) -> J:
        if not isinstance(import_, MultiImport):
            self._names.add(_bound_import_name(import_, None))
        return import_

    def visit_multi_import(self, multi: MultiImport, p: Any) -> J:
        for imp in multi.names:
            self._names.add(_bound_import_name(imp, multi.from_))
        return multi

    def visit_variable_declarations(self, var_decls: VariableDeclarations, p: Any) -> J:
        for variable in var_decls.variables:
            self._bind(variable.name)
        return super().visit_variable_declarations(var_decls, p)

    def visit_assignment(self, assignment: Assignment, p: Any) -> J:
        self._bind(assignment.variable)
        return super().visit_assignment(assignment, p)

    def visit_assignment_operation(self, assign_op: AssignmentOperation, p: Any) -> J:
        self._bind(assign_op.variable)
        return super().visit_assignment_operation(assign_op, p)

    def visit_chained_assignment(self, chained: ChainedAssignment, p: Any) -> J:
        for variable in chained.variables:
            self._bind(variable)
        return super().visit_chained_assignment(chained, p)

    def visit_for_each_control(self, control: ForEachLoop.Control, p: Any) -> J:
        self._bind(control.variable)
        return super().visit_for_each_control(control, p)

    def visit_comprehension_clause(self, clause: ComprehensionExpression.Clause,
                                   p: Any) -> ComprehensionExpression.Clause:
        self._bind(clause.iterator_variable)
        return super().visit_comprehension_clause(clause, p)
