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


"""Structural check that every slot of an LST holds the node kind its field declares.

The printer renders a mis-slotted node (an `Expression` where a `Statement` is declared)
identically to a correct one, so this is the only test-time signal for a tree that the Java
receiver would reject on deserialization.
"""

from __future__ import annotations

import dataclasses
from collections import deque
from typing import Any, List, Optional, Tuple, Type, Union, get_args, get_origin, get_type_hints

from rewrite import Tree
from rewrite.java.support_types import JContainer, JLeftPadded, JRightPadded

_HINTS_CACHE: dict = {}


def assert_well_formed(tree: Tree) -> None:
    """Raise `AssertionError` naming every slot whose value is not an instance of its declared type."""
    offenders: List[str] = []
    pending: deque[Tuple[Any, Any, str]] = deque([(tree, type(tree), type(tree).__name__)])
    while pending:
        value, hint, path = pending.popleft()
        hint = _strip_optional(hint)
        if isinstance(value, list):
            elem_hint = get_args(hint)[0] if get_origin(hint) is list else None
            pending.extend((v, elem_hint, f"{path}[{i}]") for i, v in enumerate(value))
        elif isinstance(value, (JRightPadded, JLeftPadded)):
            pending.append((value.element, _type_arg(hint), path))
        elif isinstance(value, JContainer):
            pending.extend((p.element, _type_arg(hint), f"{path}[{i}]") for i, p in enumerate(value.padding.elements))
        elif isinstance(value, Tree):
            if isinstance(hint, type) and issubclass(hint, Tree) and not isinstance(value, hint):
                offenders.append(f"{path} holds {type(value).__name__}, expected {hint.__name__}")
            for name, field_hint in _slot_hints(type(value)):
                pending.append((getattr(value, name), field_hint, f"{type(value).__name__}.{name.lstrip('_')}"))
    assert not offenders, "Malformed tree:\n  " + "\n  ".join(offenders)


def _slot_hints(cls: Type[Tree]) -> List[Tuple[str, Any]]:
    hints = _HINTS_CACHE.get(cls)
    if hints is None:
        resolved = get_type_hints(cls)
        hints = [(f.name, resolved.get(f.name)) for f in dataclasses.fields(cls)]
        _HINTS_CACHE[cls] = hints
    return hints


def _strip_optional(hint: Any) -> Any:
    if get_origin(hint) is Union:
        args = [a for a in get_args(hint) if a is not type(None)]
        return args[0] if len(args) == 1 else None
    return hint


def _type_arg(hint: Any) -> Optional[Any]:
    args = get_args(hint)
    return args[0] if args else None
