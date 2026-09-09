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

"""Shared utility functions for Python import handling."""

import ast
from typing import Iterator, Optional, Sequence, Set, Tuple

from rewrite.java.support_types import JavaType, JRightPadded, Space, Statement
from rewrite.java.tree import (Assignment, AssignmentOperation, Block, Empty, FieldAccess,
                               Identifier, If, Import, Literal, MethodInvocation)
from rewrite.markers import Markers
from rewrite.python.markers import Quoted
from rewrite.python.tree import (ChainedAssignment, CollectionLiteral, ExpressionStatement,
                                 StatementExpression, TypeHintedExpression)

# List methods that reorder or read `__all__` without changing which names it holds.
_MEMBERSHIP_PRESERVING_CALLS = frozenset({'sort', 'index', 'count', 'copy'})


def unconditional_body(if_: If) -> Optional[Block]:
    """The body of an `if` that only adds bindings to the enclosing scope.

    None once there is an `else`: the branches are then alternative bindings of
    the same name, and honouring one would rewrite the other's binding too.
    """
    then_part = if_.then_part
    return then_part if if_.else_part is None and isinstance(then_part, Block) else None


def module_scope_blocks(statements: Sequence[Statement]) -> Iterator[Block]:
    """The `if` bodies whose bindings land in the module scope.

    `if TYPE_CHECKING:` is where files that defer their annotations keep their
    typing imports.
    """
    for stmt in statements:
        body = unconditional_body(stmt) if isinstance(stmt, If) else None
        if body is not None:
            yield body
            yield from module_scope_blocks(body.statements)


def _unwrap(expr):
    """The expression under the statement and annotation wrappers, so that
    `__all__: list = [...]` reaches the same identifier as `__all__ = [...]`."""
    while isinstance(expr, (ExpressionStatement, StatementExpression, TypeHintedExpression)):
        expr = expr.expression
    return expr


def _exported_entries(value) -> Optional[Set[str]]:
    """The strings a list or tuple literal holds, or None for any other value or
    any entry that is not a string literal. An empty literal holds one `Empty`."""
    if not isinstance(value, CollectionLiteral) or value.kind not in (
            CollectionLiteral.Kind.LIST, CollectionLiteral.Kind.TUPLE):
        return None
    names: Set[str] = set()
    for element in value.elements:
        if isinstance(element, Empty):
            continue
        if not isinstance(element, Literal) or not isinstance(element.value, str):
            return None
        names.add(element.value)
    return names


def _binds_all(expr) -> bool:
    """True when ``expr`` names ``__all__``."""
    target = _unwrap(expr)
    return isinstance(target, Identifier) and target.simple_name == '__all__'


def _every_mention_read(cu, read: Set[int]) -> bool:
    """True when every ``__all__`` in the file is one this already read. A mention
    anywhere else — a tuple target, an `if`/`else` or `try` body, `__all__.append(...)` —
    contributes members by a route with no literal to read."""
    from rewrite.python.visitor import PythonVisitor  # its module imports this one

    unread = False

    class Scan(PythonVisitor):
        def visit_identifier(self, ident: Identifier, p):
            nonlocal unread
            if ident.simple_name == '__all__' and id(ident) not in read:
                unread = True
            return ident

    Scan().visit(cu, None)
    return not unread


def module_exported_names(cu) -> Optional[Set[str]]:
    """The names a module re-exports through a module-scope ``__all__``, empty when it
    declares none, None once one is written in a shape whose entries cannot be read.

    `type_mapping._module_all_names` answers the same question over `ast`, for
    attribution, but classifies a public surface and may skip an entry it cannot read.
    An entry missed here would drop an import, so anything unreadable is None instead.
    """
    statements = list(cu.statements)
    for block in module_scope_blocks(cu.statements):
        statements.extend(block.statements)

    names: Set[str] = set()
    read: Set[int] = set()
    for stmt in statements:
        stmt = _unwrap(stmt)
        if isinstance(stmt, (Assignment, AssignmentOperation)):
            targets = [stmt.variable]
        elif isinstance(stmt, ChainedAssignment):
            targets = list(stmt.variables)
        elif isinstance(stmt, MethodInvocation) and _binds_all(stmt.select) and \
                stmt.name.simple_name in _MEMBERSHIP_PRESERVING_CALLS:
            read.add(id(_unwrap(stmt.select)))
            continue
        else:
            continue

        bound = [target for target in targets if _binds_all(target)]
        if not bound:
            continue
        entries = _exported_entries(stmt.assignment)
        if entries is None:
            return None
        names.update(entries)
        read.update(id(_unwrap(target)) for target in bound)

    return names if _every_mention_read(cu, read) else None


def get_qualid_name(qualid) -> str:
    """Get the string representation of a qualified name."""
    if isinstance(qualid, Identifier):
        return qualid.simple_name
    elif isinstance(qualid, FieldAccess):
        target = get_name_string(qualid.target)
        name = qualid.name.simple_name
        if target:
            return f"{target}.{name}"
        return name
    return ""


def get_name_string(name) -> str:
    """Get string from a NameTree."""
    if isinstance(name, Identifier):
        return name.simple_name
    elif isinstance(name, FieldAccess):
        target = get_name_string(name.target)
        if target:
            return f"{target}.{name.name.simple_name}"
        return name.name.simple_name
    elif isinstance(name, Empty):
        return ""
    return str(name) if name else ""


def get_alias_name(imp: Import) -> Optional[str]:
    """Get the alias name from an Import, or None if no alias."""
    if imp.alias is None:
        return None
    alias = imp.alias
    if isinstance(alias, Identifier):
        return alias.simple_name
    return None


def module_binding_name(module: str) -> str:
    """The name ``import <module>`` binds: its root package, since ``import os.path``
    binds ``os`` and that is what a reference through the module reads."""
    return module.split('.')[0]


def get_canonical_fqn(imp: Import) -> Optional[str]:
    """The fully qualified name of the symbol ``imp`` binds, at the module defining it,
    read off the qualid's own type, or None when unattributed."""
    t = getattr(imp.qualid, 'type', None)
    if isinstance(t, JavaType.Method):
        declaring = t.declaring_type
        if isinstance(declaring, JavaType.FullyQualified) and \
                not isinstance(declaring, JavaType.Unknown) and t.name:
            declaring_fqn = getattr(declaring, 'fully_qualified_name', None)
            if declaring_fqn:
                return f"{declaring_fqn}.{t.name}"
        return None
    if isinstance(t, JavaType.Parameterized):
        t = t.type
    if isinstance(t, JavaType.FullyQualified) and not isinstance(t, JavaType.Unknown):
        return getattr(t, 'fully_qualified_name', None) or None
    return None


def referenced_names(ident: Identifier) -> Tuple[str, ...]:
    """The names ``ident`` looks up. A quoted identifier is a forward reference, naming
    one symbol where it spells one; text the parser kept whole names whatever parsing it
    finds, and text that is not an expression names nothing."""
    if ident.markers.find_first(Quoted) is None or ident.simple_name.isidentifier():
        return (ident.simple_name,)
    try:
        reference = ast.parse(ident.simple_name.strip(), mode='eval')
    except (SyntaxError, ValueError):
        return ()
    return tuple(node.id for node in ast.walk(reference) if isinstance(node, ast.Name))


def pad_right(elem) -> JRightPadded:
    """Wrap an element in a JRightPadded."""
    return JRightPadded(elem, Space.EMPTY, Markers.EMPTY)
