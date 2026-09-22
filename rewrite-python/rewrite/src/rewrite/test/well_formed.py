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
identically to a correct one, so this is the only test-time signal for a tree the Java side
rejects: at receive time for single-node slots, on first typed access for padded lists.
"""

from __future__ import annotations

import dataclasses
import types
from collections import deque
from typing import Any, Dict, List, Optional, Tuple, Type, Union, get_args, get_origin, get_type_hints

from rewrite import Tree
from rewrite.java.support_types import JContainer, JLeftPadded, JRightPadded

# A slot spec is ("tree", cls): an instance of the Tree subclass cls; ("list", elem): a list whose
# elements satisfy elem; ("padded", wrapper, elem): a JRightPadded/JLeftPadded whose element
# satisfies elem; ("container", elem): a JContainer whose elements satisfy elem; or ("any",):
# unconstrained, though trees found inside are still walked.
_Spec = Tuple[Any, ...]
_ANY: _Spec = ("any",)
_SLOTS_CACHE: Dict[type, List[Tuple[str, str, _Spec]]] = {}


def assert_well_formed(tree: Tree) -> None:
    """Raise `AssertionError` naming every slot whose value is not an instance of its declared type."""
    offenders: List[str] = []
    # Each pending item is (value, spec, path, owner); `path` is a linked list of segments
    # rendered only for an offender, `owner` the "Type.field" that declares the slot.
    pending = deque([(tree, ("tree", type(tree)), (None, type(tree).__name__), None)])
    while pending:
        value, spec, path, owner = pending.popleft()
        if value is None:
            continue
        kind = spec[0]
        if kind == "tree":
            if not isinstance(value, spec[1]):
                offenders.append(_offence(path, value, spec[1].__name__, owner))
            else:
                _push_fields(pending, value, path)
        elif kind == "list":
            if not isinstance(value, list):
                offenders.append(_offence(path, value, "list", owner))
            else:
                pending.extend((v, spec[1], (path, f"[{i}]"), owner) for i, v in enumerate(value))
        elif kind == "padded":
            if not isinstance(value, spec[1]):
                offenders.append(_offence(path, value, spec[1].__name__, owner))
            else:
                pending.append((value.element, spec[2], path, owner))
        elif kind == "container":
            if not isinstance(value, JContainer):
                offenders.append(_offence(path, value, "JContainer", owner))
            else:
                pending.extend((p.element, spec[1], (path, f"[{i}]"), owner)
                               for i, p in enumerate(value.padding.elements))
        elif isinstance(value, Tree):
            _push_fields(pending, value, path)
        elif isinstance(value, list):
            pending.extend((v, _ANY, (path, f"[{i}]"), owner) for i, v in enumerate(value))
        elif isinstance(value, (JRightPadded, JLeftPadded)):
            pending.append((value.element, _ANY, path, owner))
        elif isinstance(value, JContainer):
            pending.extend((p.element, _ANY, (path, f"[{i}]"), owner) for i, p in enumerate(value.padding.elements))
    assert not offenders, "Malformed tree:\n  " + "\n  ".join(offenders)


def _push_fields(pending: deque, node: Tree, path: Tuple) -> None:
    cls = type(node)
    for attr, name, spec in _slot_specs(cls):
        pending.append((getattr(node, attr), spec, (path, "." + name), f"{cls.__name__}.{name}"))


def _offence(path: Tuple, value: Any, expected: str, owner: Optional[str]) -> str:
    segments: List[str] = []
    while path is not None:
        path, segment = path
        segments.append(segment)
    where = "".join(reversed(segments))
    declared = f" ({owner})" if owner else ""
    return f"{where} holds {type(value).__name__}, expected {expected}{declared}"


def _slot_specs(cls: Type[Tree]) -> List[Tuple[str, str, _Spec]]:
    """(attribute, display name, spec) for each field of `cls` whose annotation can hold a tree."""
    specs = _SLOTS_CACHE.get(cls)
    if specs is None:
        hints = get_type_hints(cls)
        specs = []
        for f in dataclasses.fields(cls):
            spec = _compile(hints.get(f.name))
            if spec is not None:
                specs.append((f.name, f.name.lstrip("_"), spec))
        _SLOTS_CACHE[cls] = specs
    return specs


def _compile(hint: Any) -> Optional[_Spec]:
    """Compile a field annotation into a slot spec, or None for a field that never holds a tree."""
    origin = get_origin(hint)
    if origin in (Union, types.UnionType):
        args = [a for a in get_args(hint) if a is not type(None)]
        return _compile(args[0]) if len(args) == 1 else _ANY
    if origin is list:
        elem = _compile(get_args(hint)[0])
        return None if elem is None else ("list", elem)
    if origin in (JRightPadded, JLeftPadded):
        return ("padded", origin, _compile(get_args(hint)[0]) or _ANY)
    if origin is JContainer:
        return ("container", _compile(get_args(hint)[0]) or _ANY)
    if isinstance(hint, type):
        return ("tree", hint) if issubclass(hint, Tree) else None
    if origin is not None and isinstance(origin, type):
        return None  # a parameterised non-LST type such as a weakref
    return _ANY
