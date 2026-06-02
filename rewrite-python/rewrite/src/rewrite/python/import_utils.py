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

from typing import Optional

from rewrite.java.support_types import JRightPadded, Space
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import
from rewrite.markers import Markers


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


def pad_right(elem) -> JRightPadded:
    """Wrap an element in a JRightPadded."""
    return JRightPadded(elem, Space.EMPTY, Markers.EMPTY)
