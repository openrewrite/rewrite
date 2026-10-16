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

"""Binding a module or member in a file, and moving a binding to another module.

Both answer with the name a reference spells, so a caller can write its references without
knowing how the file imports.
"""

import dataclasses
from typing import Any, Dict, FrozenSet, List, NamedTuple, Optional, Set, Tuple

from rewrite import random_id
from rewrite.java.support_types import Expression, JavaType, JContainer, JLeftPadded, JRightPadded, Statement
from rewrite.java.tree import FieldAccess, Identifier, If, Import, J, MethodInvocation, Space
from rewrite.markers import Markers
from rewrite.python.add_import import (
    AddImport,
    AddImportOptions,
    create_import_element,
    create_import_statement,
    insert_member,
    maybe_add_import,
)
from rewrite.python.binding_utils import Binding, dotted_path, import_bindings, is_reference, resolves_in_scope
from rewrite.python.import_utils import (
    get_alias_name,
    get_canonical_fqn,
    get_name_string,
    get_qualid_name,
    module_scope_blocks,
    unconditional_body,
)
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import, prefix_to_inherit
from rewrite.python.scope_utils import LocalBindings
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor
from rewrite.visitor import Cursor, TreeVisitor

_NAMES_SPELLED = 'org.openrewrite.python.namesSpelled'
_LOCALS = LocalBindings()

# An import to bind, as (module, name, alias). A None name means `import module`.
_Import = Tuple[str, Optional[str], Optional[str]]


def maybe_bind(visitor: TreeVisitor[Any, Any], module: str, member: Optional[str] = None, *,
               alias: Optional[str] = None) -> Optional[str]:
    """The name a reference to ``module``, or to its ``member``, spells in the file ``visitor``
    is visiting. An unguarded module-scope import of it answers where there is one, else an
    import is queued for once the caller has written its reference. None where the name the
    import would bind already stands for something else, or ``alias`` is no identifier.
    """
    cu = _compilation_unit(visitor)
    bound = None if cu is None else _binding_for(visitor, cu, module, member, alias)
    if bound is None:
        return None
    name, new = bound
    if new:
        maybe_add_import(visitor, AddImportOptions(module=module, name=member, alias=alias))
    return name


def _binding_for(visitor: TreeVisitor[Any, Any], cu: CompilationUnit, module: str,
                 member: Optional[str], alias: Optional[str]) -> Optional[Tuple[str, bool]]:
    """``maybe_bind``'s answer, and whether it needs an import, without queuing one."""
    if member == '*' or (alias is not None and not alias.isidentifier()):
        return None
    for b in import_bindings(visitor):
        if (b.module == module and b.member == member and not b.guarded
                and (alias is None or b.name == alias)):
            return _spelling(b), False
    name = alias or member or module
    if _taken(visitor, cu, name.split('.')[0], _Stands(module, member, alias is not None)):
        return None
    return name, True


def maybe_rebind(visitor: TreeVisitor[Any, Any], from_module: str, to_module: str, *,
                 from_member: Optional[str] = None, to_member: Optional[str] = None,
                 alias: Optional[str] = None, declared_in: Optional[str] = None) -> Optional[str]:
    """Moves the file's binding of ``from_module``, or of its ``from_member``, to ``to_module``,
    and answers with the name a reference to it now spells. ``to_member`` defaults to
    ``from_member``. A member read through a binding of its module, as ``m.member``, moves
    with it. None, changing nothing, where the move cannot be expressed safely.

    Every type in the file naming what moved is renamed, not re-resolved, so a moved member
    keeps its old declaration's signature. ``declared_in`` names the module defining the member
    where ``to_module`` re-exports it.
    """
    cu = _compilation_unit(visitor)
    if cu is None or '*' in (from_member, to_member) or (from_member is None and to_member):
        return None
    if visitor._after_visit is None:
        visitor._after_visit = []
    queued = next((v for v in visitor._after_visit
                   if isinstance(v, _RebindImport) and v.key == (from_module, from_member)), None)
    if queued is not None:
        return queued.answer
    to_member = to_member or from_member
    bindings = import_bindings(visitor)
    moved = next((b for b in bindings.for_module(from_module, from_member)
                  if b.member == from_member), None)
    # `import a.b` binds `a`, so `a.member` reads the package, not the module.
    through = None if from_member is None else next(
        (b for b in bindings.for_module(from_module) if b.member is None and '.' not in _spelling(b)), None)

    name = None
    if moved is not None:
        name = _rebinding_name(visitor, cu, moved, to_module, to_member, alias)
        if name is None or (name != moved.name and _match_outside_module_scope(cu, moved)):
            return None
    identities = None if through is None else _member_reads(cu, through.name, from_member)
    spelled_module, new_module_import = None, False
    if through is not None and identities is not None:
        if through.guarded:
            # `_RebindImport` binds the new module in the block that binds the old one.
            spelled_module = to_module
        else:
            bound = _binding_for(visitor, cu, to_module, None, None)
            if bound is None:
                return None
            spelled_module, new_module_import = bound
    if name is None and spelled_module is None:
        return None

    answer = name if name is not None else f'{spelled_module}.{to_member}'
    visitor._after_visit.append(_RebindImport(from_module, from_member, to_module, to_member, moved, name,
                                              through, spelled_module, new_module_import, identities or set(),
                                              declared_in, answer))
    return answer


def _compilation_unit(visitor: TreeVisitor[Any, Any]) -> Optional[CompilationUnit]:
    cursor = getattr(visitor, '_cursor', None)
    return None if cursor is None else cursor.first_enclosing(CompilationUnit)


def _spelling(binding: Binding) -> str:
    """How a reference reads ``binding``: ``import a.b`` is read as ``a.b``, not as the ``a`` it binds."""
    if binding.member is None and get_alias_name(binding.imp) is None:
        return binding.module
    return binding.name


def _names_spelled(visitor: TreeVisitor[Any, Any], cu: CompilationUnit) -> FrozenSet[str]:
    for c in visitor.cursor.get_path_as_cursors():
        if c.value is cu:
            cached = c.get_message(_NAMES_SPELLED, None)
            if cached is None:
                cached = _spelled_in(cu)
                c.put_message(_NAMES_SPELLED, cached)
            return cached
    return _spelled_in(cu)


def _spelled_in(cu: CompilationUnit) -> FrozenSet[str]:
    names: Set[str] = set()

    class Spelled(PythonVisitor[None]):
        # A module path binds nothing, so an import spells only the name it binds.
        def visit_multi_import(self, multi: MultiImport, p: None) -> Any:
            for imp in multi.names:
                self.visit_import(imp, p)
            return multi

        def visit_import(self, import_: Import, p: None) -> Any:
            names.add(get_alias_name(import_) or get_qualid_name(import_.qualid).split('.')[0])
            return import_

        def visit_identifier(self, ident: Identifier, p: None) -> Any:
            names.add(ident.simple_name)
            return ident

    Spelled().visit(cu, None)
    return frozenset(names)


class _Stands(NamedTuple):
    """What a new binding stands for: ``member`` of ``module``, or the module itself."""

    module: str
    member: Optional[str]
    aliased: bool

    def shares(self, module: str, member: Optional[str], aliased: bool) -> bool:
        """Whether a binding of ``module``/``member`` under the same name binds the same thing.
        ``import a.b`` and ``import a.c`` both bind the package ``a``."""
        if self.member is None and member is None and not self.aliased and not aliased:
            return module.split('.')[0] == self.module.split('.')[0]
        return (module, member) == (self.module, self.member)


def _taken(visitor: TreeVisitor[Any, Any], cu: CompilationUnit, name: str,
           stands: Optional[_Stands] = None) -> bool:
    """Whether a new binding of ``name`` would capture references to something else, or be
    captured by them. An import already binding what ``stands`` names under it is no collision,
    whether the file holds it or an earlier call queued it."""
    def other(module: str, member: Optional[str], aliased: bool) -> bool:
        return stands is None or not stands.shares(module, member, aliased)

    for v in visitor._after_visit or []:
        if isinstance(v, _RebindImport):
            if v.bound_name == name and other(v.to_module, v.to_member, True):
                return True
            if v.new_module_import and v.to_module.split('.')[0] == name and other(v.to_module, None, False):
                return True
        if isinstance(v, AddImport) and (v.alias or v.name or v.module.split('.')[0]) == name and other(
                v.module, v.name, v.alias is not None):
            return True
    if name not in _names_spelled(visitor, cu):
        return False
    holders = [b for b in import_bindings(visitor) if b.name == name]
    return (not holders or any(other(b.module, b.member, get_alias_name(b.imp) is not None) for b in holders)
            or _declared_besides_module_imports(cu, name))


def _rebinding_name(visitor: TreeVisitor[Any, Any], cu: CompilationUnit, moved: Binding,
                    to_module: str, to_member: Optional[str], alias: Optional[str]) -> Optional[str]:
    """The name the moved binding takes, or None where no name keeps its references bound."""
    if alias is not None:
        return alias if alias.isidentifier() and (
            alias == moved.name or not _taken(visitor, cu, alias)) else None
    if get_alias_name(moved.imp) is not None:
        return moved.name
    if to_member is not None:
        if to_member == moved.name or (not _taken(visitor, cu, to_member, _Stands(to_module, to_member, False))
                                       and not _moves_away(visitor, to_member)):
            return to_member
        return moved.name
    # `import m` binds the module's own name, which references spell.
    if '.' in moved.module:
        # References read `a.b.x`, which no name of the new module answers.
        return to_module if not _reads_name(cu, moved.name) else None
    if not _reads_name(cu, moved.name):
        return to_module
    if '.' not in to_module and not _taken(visitor, cu, to_module):
        return to_module
    return moved.name


def _moves_away(visitor: TreeVisitor[Any, Any], name: str) -> bool:
    """Whether a rebind queued earlier in the visit moves the binding holding ``name``."""
    return any(isinstance(v, _RebindImport) and v.local_name == name for v in visitor._after_visit or [])


def _declared_besides_module_imports(cu: CompilationUnit, name: str) -> bool:
    """Whether a statement other than a module-scope import binds ``name``. A parameter or a
    nested import of it would capture a reference renamed onto it."""
    module_scope = _module_scope_ids(cu)
    found: List[bool] = []

    class Declares(PythonVisitor[None]):
        def visit_multi_import(self, multi: MultiImport, p: None) -> Any:
            if multi.id not in module_scope and any(
                    (get_alias_name(imp) or get_qualid_name(imp.qualid).split('.')[0]) == name
                    for imp in multi.names):
                found.append(True)
            return multi

        def visit_identifier(self, ident: Identifier, p: None) -> Any:
            if (ident.simple_name == name and resolves_in_scope(self.cursor, ident)
                    and not is_reference(self.cursor, ident)):
                found.append(True)
            return ident

    Declares().visit(cu, None)
    return bool(found)


def _reads_name(cu: CompilationUnit, name: str) -> bool:
    """Whether anything outside the imports reads ``name``."""
    found: List[bool] = []

    class Reads(PythonVisitor[None]):
        def visit_multi_import(self, multi: MultiImport, p: None) -> Any:
            return multi

        def visit_import(self, import_: Import, p: None) -> Any:
            return import_

        def visit_identifier(self, ident: Identifier, p: None) -> Any:
            if ident.simple_name == name and is_reference(self.cursor, ident):
                found.append(True)
            return ident

    Reads().visit(cu, None)
    return bool(found)


def _module_scope_ids(cu: CompilationUnit) -> Set[Any]:
    """The ids of the statements a module-scope import can be among."""
    ids = {stmt.id for stmt in cu.statements}
    for block in module_scope_blocks(cu.statements):
        ids.update(stmt.id for stmt in block.statements)
    return ids


def _reads_through(cursor: Cursor, target: Any, name: Any, module_name: str, member: str) -> bool:
    """Whether ``target.name`` at ``cursor`` reads ``member`` through the module-scope binding
    ``module_name``, which no nearer scope redeclares."""
    return (isinstance(target, Identifier) and target.simple_name == module_name
            and isinstance(name, Identifier) and name.simple_name == member
            and not _LOCALS.is_bound(cursor, module_name))


def _member_reads(cu: CompilationUnit, module_name: str, member: str) -> Optional[Set[str]]:
    """The canonical names the file's reads of ``module_name.member`` carry, or None where
    nothing reads it."""
    found: Optional[Set[str]] = None

    class Reads(PythonVisitor[None]):
        def read(self, target: Any, name: Any, identity: Optional[str]) -> None:
            nonlocal found
            if _reads_through(self.cursor, target, name, module_name, member):
                found = found if found is not None else set()
                if identity is not None and identity.rpartition('.')[2] == member:
                    found.add(identity)

        def visit_field_access(self, field_access: FieldAccess, p: None) -> Any:
            field_access = super().visit_field_access(field_access, p)  # ty: ignore[invalid-assignment]  # visitor covariance
            if isinstance(field_access, FieldAccess):
                t = field_access.type
                self.read(field_access.target, field_access.name,
                          t.fully_qualified_name if isinstance(t, JavaType.Class) else None)
            return field_access

        def visit_method_invocation(self, method: MethodInvocation, p: None) -> Any:
            method = super().visit_method_invocation(method, p)  # ty: ignore[invalid-assignment]  # visitor covariance
            if isinstance(method, MethodInvocation):
                t = method.method_type
                declaring = getattr(t.declaring_type, 'fully_qualified_name', None) if t is not None else None
                identity = None if t is None or not declaring else (
                    declaring if t.is_constructor else f'{declaring}.{t.name}')
                self.read(method.select, method.name, identity)
            return method

    Reads().visit(cu, None)
    return found


def _match_outside_module_scope(cu: CompilationUnit, moved: Binding) -> bool:
    """Whether an import of what moved sits where a rebind leaves it, deeper than module scope.
    It goes on binding the old name, so renaming the references it serves leaves them unbound."""
    in_scope = _module_scope_ids(cu)
    found: List[bool] = []

    class Finder(PythonVisitor[None]):
        def visit_multi_import(self, multi: MultiImport, p: None) -> Any:
            if multi.id not in in_scope and _binds(multi, moved.module, moved.member, moved.name):
                found.append(True)
            return multi

    Finder().visit(cu, None)
    return bool(found)


def _moves(element: Import, from_: Optional[J], module: str, member: Optional[str], local: str) -> bool:
    """Whether ``element`` of a statement importing ``from_`` binds ``member`` of ``module``, or
    ``module`` itself, as ``local``. A second import binding it under another name stays."""
    qualid = get_qualid_name(element.qualid)
    if member is None:
        return from_ is None and qualid == module and (get_alias_name(element) or qualid.split('.')[0]) == local
    return (from_ is not None and dotted_path(from_) == module and qualid == member
            and (get_alias_name(element) or qualid) == local)


def _binds(stmt: Statement, module: str, member: Optional[str], local: str) -> bool:
    if isinstance(stmt, MultiImport):
        return any(_moves(imp, stmt.from_, module, member, local) for imp in stmt.names)
    return isinstance(stmt, Import) and _moves(stmt, None, module, member, local)


def _binds_module(stmt: Statement, module: str) -> bool:
    if isinstance(stmt, MultiImport):
        return stmt.from_ is None and any(get_qualid_name(imp.qualid) == module for imp in stmt.names)
    return isinstance(stmt, Import) and get_qualid_name(stmt.qualid) == module


def _without(multi: MultiImport, module: str, member: Optional[str], local: str) -> Optional[MultiImport]:
    """``multi`` without what moves, or None where nothing would be left."""
    elements = multi.padding.names.padding.elements
    kept = [e for e in elements if not _moves(e.element, multi.from_, module, member, local)]
    if not kept:
        return None
    if len(kept) == len(elements):
        return multi
    if kept[0].element.prefix != Space.EMPTY:
        kept[0] = kept[0].replace(_element=kept[0].element.replace(prefix=Space.EMPTY))
    return multi.padding.replace(
        _names=JContainer(multi.padding.names.before, kept, multi.padding.names.markers))


class _MovedTypes:
    """Renames the types that name a moved member or module onto where it moved.

    It follows a type's signature as Java's ``ChangeType`` does, not a class's members. A type
    reached while it is still being visited answers with itself, which ends a cycle. An answer
    settled inside a cycle can miss a rename.
    """

    def __init__(self, names: Dict[str, str], module: Optional[Tuple[str, str]] = None) -> None:
        """``names`` maps a moved class or member to its new name. ``module`` is a whole module
        that moved and its new name, which the classes it declares follow."""
        self._names = names
        self._module = module
        self._answered: Dict[int, Tuple[JavaType, JavaType]] = {}
        self._on_path: Set[int] = set()
        self._owners: Dict[int, JavaType] = {}
        self._modules: Dict[str, JavaType] = {}

    def __call__(self, type_: Optional[JavaType]) -> Optional[JavaType]:
        if type_ is None or id(type_) in self._on_path or isinstance(
                type_, (JavaType.Unknown, JavaType.Primitive)):
            return type_
        answered = self._answered.get(id(type_))
        if answered is not None:
            return answered[1]
        self._on_path.add(id(type_))
        try:
            answer = self._visit(type_)
        finally:
            self._on_path.discard(id(type_))
        # The key holds the type too, so its id cannot be reused while the answer is cached.
        self._answered[id(type_)] = (type_, answer)
        return answer

    def module(self, module_type: Optional[JavaType], name: str) -> Optional[JavaType]:
        """The type of module ``name``, modelled on ``module_type``, the old module's own."""
        if not isinstance(module_type, JavaType.Class):
            return module_type
        moved = self._modules.get(name)
        if moved is None:
            moved = self._modules[name] = dataclasses.replace(module_type, _fully_qualified_name=name)
        return moved

    def _list(self, types: Optional[List[JavaType]]) -> Optional[List[Any]]:
        if not types:
            return types
        visited = [self(t) for t in types]
        return types if all(a is b for a, b in zip(visited, types)) else visited

    def _visit(self, t: JavaType) -> JavaType:
        if isinstance(t, JavaType.Class):
            fqn = t.fully_qualified_name
            return _replace(t, _type_parameters=self._list(t._type_parameters),
                            _supertype=self(t._supertype), _owning_class=self(t._owning_class),
                            _interfaces=self._list(t._interfaces),
                            _fully_qualified_name=self._renamed(fqn))
        if isinstance(t, JavaType.Parameterized):
            return _replace(t, _type=self(t._type), _type_parameters=self._list(t._type_parameters))
        if isinstance(t, JavaType.Method):
            visited = _replace(t, _declaring_type=self(t._declaring_type), _return_type=self(t._return_type),
                               _parameter_types=self._list(t._parameter_types))
            return self._member(visited, t._declaring_type, t._name, '_declaring_type')
        if isinstance(t, JavaType.Variable):
            visited = _replace(t, _owner=self(t._owner), _type=self(t._type))
            return self._member(visited, t._owner, t._name, '_owner')
        if isinstance(t, (JavaType.GenericTypeVariable, JavaType.Union, JavaType.Intersection)):
            return _replace(t, _bounds=self._list(t._bounds))
        if isinstance(t, JavaType.Array):
            return _replace(t, _elem_type=self(t._elem_type))
        if isinstance(t, JavaType.Annotation):
            return _replace(t, _type=self(t._type))
        return t

    def _renamed(self, fqn: str) -> str:
        renamed = self._names.get(fqn)
        if renamed is not None or self._module is None:
            return renamed or fqn
        old, new = self._module
        owner, _, name = fqn.rpartition('.')
        # A class nested in another, or declared in a submodule, did not move with the module.
        return f'{new}.{name}' if owner == old else fqn

    def _member(self, visited: JavaType, owner: Optional[JavaType], name: str, slot: str) -> JavaType:
        """A function or module constant renamed where its module declares the moved member.
        A constructor is declared on its class, which the class rename covers."""
        owner_name = getattr(owner, 'fully_qualified_name', None)
        if owner is None or not owner_name or isinstance(owner, JavaType.Unknown):
            return visited
        moved = self._names.get(f'{owner_name}.{name}')
        if moved is None:
            return visited
        new_owner, _, new_name = moved.rpartition('.')
        module = self._owners.get(id(owner))
        if module is None:
            module = self._owners[id(owner)] = dataclasses.replace(owner, _fully_qualified_name=new_owner) \
                if isinstance(owner, JavaType.Class) else owner
        return dataclasses.replace(visited, **{slot: module, '_name': new_name})


def _replace(t: Any, **changes: Any) -> Any:
    """``t`` with ``changes``, or ``t`` itself where none of them changes anything. Identity
    decides it, since equality would walk a cyclic type graph."""
    changed = {k: v for k, v in changes.items() if getattr(t, k) is not v}
    return dataclasses.replace(t, **changed) if changed else t


class _RebindImport(PythonVisitor[Any]):
    """Moves one binding, the references that read it and the types that name what moved."""

    def __init__(self, from_module: str, from_member: Optional[str], to_module: str,
                 to_member: Optional[str], moved: Optional[Binding], bound_name: Optional[str],
                 through: Optional[Binding], spelled_module: Optional[str], new_module_import: bool,
                 identities: Set[str], declared_in: Optional[str], answer: str) -> None:
        super().__init__()
        self.key = (from_module, from_member)
        self.from_module = from_module
        self.from_member = from_member
        self.to_module = to_module
        self.to_member = to_member
        self.local_name = moved.name if moved is not None else None
        self.bound_name = bound_name
        self.through_name = through.name if through is not None else None
        self.through_at_module_level = through is not None and not through.guarded
        self.spelled_module = spelled_module
        self.new_module_import = new_module_import
        self.identities = identities
        self.declared_in = declared_in
        self.answer = answer
        self.moved_types: Optional[_MovedTypes] = None
        self.local_bindings = LocalBindings()
        self.rewrote_qualified = False
        self.old_at_module_level = False

    @property
    def _renaming(self) -> bool:
        return self.bound_name is not None and self.bound_name != self.local_name

    def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> Any:
        self.moved_types = self._moved_types(cu)
        self.old_at_module_level = any(
            self.local_name is not None and self._binds(stmt) for stmt in cu.statements)
        result = super().visit_compilation_unit(cu, p)
        if not isinstance(result, CompilationUnit):
            return result
        result = self._transfer_removed_prefixes(cu, result)
        result = self._rewrite_block_imports(result)
        if self.local_name is not None and self.old_at_module_level:
            maybe_add_import(self, AddImportOptions(
                module=self.to_module, name=self.to_member, alias=self._import_alias(),
                only_if_referenced=False))
        if self.new_module_import:
            # RemoveImport keeps a read `import a` unless another import binds `a`, so `import a.b` goes first.
            maybe_add_import(self, AddImportOptions(module=self.to_module, only_if_referenced=False))
        if self.rewrote_qualified:
            maybe_remove_import(self, RemoveImportOptions(module=self.from_module))
        return result

    def _binds(self, stmt: Statement) -> bool:
        return self.local_name is not None and _binds(stmt, self.from_module, self.from_member, self.local_name)

    def _is_moved(self, binding: Binding) -> bool:
        return (binding.module, binding.member, binding.name) == (self.from_module, self.from_member,
                                                                  self.local_name)

    def _import_alias(self) -> Optional[str]:
        """The alias the new import needs, None where it binds ``bound_name`` without one."""
        unaliased = self.to_member if self.from_member is not None else self.to_module
        return None if self.bound_name == unaliased else self.bound_name

    def _moved_types(self, cu: CompilationUnit) -> _MovedTypes:
        """The renames this move makes, less the ones another binding in the file shares."""
        owner = self.declared_in or self.to_module
        others = [b for b in import_bindings(cu) if not self._is_moved(b)]
        if self.from_member is None:
            if any(b.module == self.from_module for b in others):
                return _MovedTypes({})
            return _MovedTypes({self.from_module: owner}, (self.from_module, owner))
        old = {f'{self.from_module}.{self.from_member}'}
        old.update(self.identities)
        for b in import_bindings(cu):
            # A constant's import carries the type of its value, which names something else.
            canonical = get_canonical_fqn(b.imp)
            if self._is_moved(b) and canonical is not None and canonical.rpartition('.')[2] == self.from_member:
                old.add(canonical)
        if any(get_canonical_fqn(b.imp) in old for b in others if b.member is not None):
            return _MovedTypes({})
        return _MovedTypes({fqn: f'{owner}.{self.to_member}' for fqn in old})

    def visit_type(self, java_type: Optional[JavaType], p: Any) -> Optional[JavaType]:
        return self.moved_types(java_type) if self.moved_types is not None else java_type

    def _at_module_level(self) -> bool:
        return isinstance(self.cursor.parent_tree_cursor().value, CompilationUnit)

    def visit_import(self, import_: Import, p: Any) -> Any:
        if self.from_member is not None or self.local_name is None or not self._at_module_level():
            return import_
        return None if self._binds(import_) else import_

    def visit_multi_import(self, multi: MultiImport, p: Any) -> Any:
        multi = super().visit_multi_import(multi, p)  # ty: ignore[invalid-assignment]  # visitor covariance
        if self.local_name is None or not isinstance(multi, MultiImport) or not self._at_module_level():
            return multi
        if not self._binds(multi):
            return multi
        return _without(multi, self.from_module, self.from_member, self.local_name)

    def visit_identifier(self, ident: Identifier, p: Any) -> Any:
        # The position predicates match the cursor's nodes by identity, so they are asked of
        # the identifier the cursor holds.
        at_cursor = ident
        ident = super().visit_identifier(ident, p)  # ty: ignore[invalid-assignment]  # visitor covariance
        if not isinstance(ident, Identifier) or not self._renaming or ident.simple_name != self.local_name:
            return ident
        if not resolves_in_scope(self.cursor, at_cursor):
            return ident
        binding = not is_reference(self.cursor, at_cursor)
        if self.local_bindings.is_bound(self.cursor, self.local_name, binding=binding):
            return ident
        return ident.replace(_simple_name=self.bound_name)

    def _reads_through(self, target: Any, name: Any) -> bool:
        return (self.spelled_module is not None and self.through_name is not None
                and self.from_member is not None
                and _reads_through(self.cursor, target, name, self.through_name, self.from_member))

    def _module_reference(self, target: Identifier) -> Expression:
        """The name tree reading the new module where ``target`` read the old one, each part
        typed as the module it names wherever ``target`` was typed. An alias names the module
        it binds."""
        moved_types = self.moved_types
        assert self.spelled_module is not None and moved_types is not None
        parts = self.spelled_module.split('.')
        aliased = self.spelled_module != self.to_module

        def typed(name: str) -> Optional[JavaType]:
            return None if target.type is None else moved_types.module(
                target.type, self.to_module if aliased else name)

        result: Expression = target.replace(_simple_name=parts[0], _type=typed(parts[0]))
        for i in range(1, len(parts)):
            name = '.'.join(parts[:i + 1])
            result = FieldAccess(random_id(), Space.EMPTY, Markers.EMPTY, result.replace(prefix=Space.EMPTY),
                                 JLeftPadded(Space.EMPTY, Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [],
                                                                     parts[i], typed(name), None), Markers.EMPTY),
                                 typed(name)).replace(prefix=target.prefix)
        return result

    def visit_method_invocation(self, method: MethodInvocation, p: Any) -> Any:
        method = super().visit_method_invocation(method, p)  # ty: ignore[invalid-assignment]  # visitor covariance
        if not isinstance(method, MethodInvocation) or not self._reads_through(method.select, method.name):
            return method
        self.rewrote_qualified = True
        padded_select = method.padding.select
        if padded_select is None:
            return method
        result = method.padding.replace(_select=padded_select.replace(
            _element=self._module_reference(method.select)))
        return result.replace(_name=result.name.replace(_simple_name=self.to_member))

    def visit_field_access(self, field_access: FieldAccess, p: Any) -> Any:
        field_access = super().visit_field_access(field_access, p)  # ty: ignore[invalid-assignment]  # visitor covariance
        if (not isinstance(field_access, FieldAccess)
                or not self._reads_through(field_access.target, field_access.name)):
            return field_access
        self.rewrote_qualified = True
        result = field_access.replace(_target=self._module_reference(field_access.target))
        name = result.name.replace(_simple_name=self.to_member)
        return result.padding.replace(_name=result.padding.name.replace(_element=name))

    def _rewrite_block_imports(self, cu: CompilationUnit) -> CompilationUnit:
        """Rewrites a match inside an `if TYPE_CHECKING:`-style block where it stands.

        The replacement import is bound in the same block. Hoisting it to module level would
        run at import time an import the file deliberately deferred.
        """
        kept = self._rewrite_statements(cu.padding.statements)
        return cu if kept is None else cu.padding.replace(_statements=kept)

    def _rewrite_statements(self, padded_statements: List[JRightPadded]) -> Optional[List[JRightPadded]]:
        kept: List[JRightPadded] = []
        changed = False
        for padded in padded_statements:
            stmt = padded.element
            if isinstance(stmt, If):
                rewritten = self._rewrite_if(stmt)
                if rewritten is not stmt:
                    padded = padded.replace(_element=rewritten)
                    changed = True
            kept.append(padded)
        return kept if changed else None

    def _rewrite_if(self, if_: If) -> If:
        body = unconditional_body(if_)
        if body is None:
            return if_
        kept = self._rewrite_block(body.padding.statements)
        if kept is None:
            return if_
        padded = if_.padding.then_part
        return if_.padding.replace(_then_part=JRightPadded(
            body.padding.replace(_statements=kept), padded.after, padded.markers))

    def _rewrite_block(self, padded_statements: List[JRightPadded]) -> Optional[List[JRightPadded]]:
        """None where nothing in the block, or in an `if` nested in it, matched."""
        kept: List[JRightPadded] = []
        changed = False
        to_add: List[Tuple[_Import, int, Space]] = []
        for padded in padded_statements:
            stmt = padded.element
            if isinstance(stmt, If):
                rewritten = self._rewrite_if(stmt)
                if rewritten is not stmt:
                    padded = padded.replace(_element=rewritten)
                    changed = True
                kept.append(padded)
                continue
            reduced, binding = self._match_in_block(stmt)
            if reduced is not stmt:
                changed = True
            if reduced is not None:
                kept.append(padded if reduced is stmt else padded.replace(_element=reduced))
            if binding is not None:
                # A comment on the statement describes what it imports, so it travels only
                # when the whole statement is replaced.
                prefix = stmt.prefix if reduced is None else Space([], stmt.prefix.whitespace)
                to_add.append((binding, len(kept), prefix))
        # Back to front, so an insertion never shifts a pending position.
        for binding, at, prefix in reversed(to_add):
            if self._place_import(kept, binding, at, prefix):
                changed = True
        return kept if changed else None

    def _match_in_block(self, stmt: Statement) -> Tuple[Optional[Statement], Optional[_Import]]:
        if self.local_name is not None and self._binds(stmt):
            if isinstance(stmt, MultiImport):
                reduced = _without(stmt, self.from_module, self.from_member, self.local_name)
            else:
                # The module is the whole statement, so the statement goes.
                reduced = None
            return reduced, (self.to_module, self.to_member if self.from_member else None,
                             self._import_alias())
        # A block importing the old module binds the new one too where this rebind rewrote
        # references to it. RemoveImport drops the old import once nothing reads it.
        if (self.rewrote_qualified and not self.through_at_module_level
                and _binds_module(stmt, self.from_module)):
            return stmt, (self.to_module, None, None)
        return stmt, None

    def _place_import(self, kept: List[JRightPadded], binding: _Import, at: int, prefix: Space) -> bool:
        """False where the block already binds `binding`."""
        module, name, alias = binding
        if name is None:
            if any(_binds_module(p.element, module) for p in kept):
                return False
        else:
            bound = alias or name
            for index, padded in enumerate(kept):
                stmt = padded.element
                if not isinstance(stmt, MultiImport) or stmt.from_ is None:
                    continue
                if get_name_string(stmt.from_) != module:
                    continue
                elements = list(stmt.padding.names.padding.elements)
                if any((get_alias_name(e.element) or get_qualid_name(e.element.qualid)) == bound
                       for e in elements):
                    return False
                if prefix.comments:
                    # A merge has nowhere to carry that comment.
                    break
                kept[index] = padded.replace(_element=stmt.padding.replace(
                    _names=JContainer(stmt.padding.names.before,
                                      insert_member(elements, create_import_element(name, alias)),
                                      stmt.padding.names.markers)))
                return True
        statement = create_import_statement(module, name, alias).replace(prefix=prefix)
        kept.insert(at, JRightPadded(statement, Space.EMPTY, Markers.EMPTY))
        return True

    def _transfer_removed_prefixes(self, before: CompilationUnit, after: CompilationUnit) -> CompilationUnit:
        """Dropping a statement discards its prefix. When it is worth rescuing (see
        prefix_to_inherit) it goes to the next surviving statement, as RemoveImport does."""
        removed_ids = ({p.element.id for p in before.padding.statements}
                       - {p.element.id for p in after.padding.statements})
        if not removed_ids:
            return after
        inherited = None
        prefix_by_id = {}
        for index, padded in enumerate(before.padding.statements):
            stmt = padded.element
            if stmt.id in removed_ids:
                prefix = prefix_to_inherit(stmt, index)
                if prefix is not None:
                    inherited = prefix
            elif inherited is not None:
                # A whitespace-only prefix goes only to a following import. A following plain
                # statement keeps its own separation, which AddImport relies on when it inserts
                # the replacement import before it.
                if inherited.comments or isinstance(stmt, (Import, MultiImport)):
                    prefix_by_id[stmt.id] = inherited
                inherited = None
        if not prefix_by_id:
            return after
        return after.padding.replace(_statements=[
            p.replace(_element=p.element.replace(prefix=prefix_by_id[p.element.id]))
            if p.element.id in prefix_by_id else p
            for p in after.padding.statements])


__all__ = ['maybe_bind', 'maybe_rebind']
