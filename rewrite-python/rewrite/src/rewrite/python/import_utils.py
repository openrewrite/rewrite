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
from typing import Optional, Tuple

from rewrite.java.support_types import JavaType, JRightPadded, Space
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import
from rewrite.markers import Markers
from rewrite.python.markers import CanonicalName, Quoted


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


def get_canonical_fqn(imp: Import) -> Optional[str]:
    """The fully qualified name of the symbol ``imp`` binds, at the module defining
    it, or None when unattributed: the :class:`CanonicalName` marker when that module
    re-exports it under the written path, else the qualid's own type."""
    canonical = imp.markers.find_first(CanonicalName)
    if canonical is not None:
        return canonical.fqn
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
    """The names ``ident`` looks up. A quoted identifier is a forward reference
    holding a whole expression, so its names come from parsing it; a string that
    is not an expression names nothing."""
    if ident.markers.find_first(Quoted) is None:
        return (ident.simple_name,)
    try:
        reference = ast.parse(ident.simple_name.strip(), mode='eval')
    except (SyntaxError, ValueError):
        return ()
    return tuple(node.id for node in ast.walk(reference) if isinstance(node, ast.Name))


def pad_right(elem) -> JRightPadded:
    """Wrap an element in a JRightPadded."""
    return JRightPadded(elem, Space.EMPTY, Markers.EMPTY)
