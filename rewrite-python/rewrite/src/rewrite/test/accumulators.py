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

"""Find the Heavyweight objects, such as trees, that ScanningRecipe accumulators keep.

An accumulator lives from the first source file scanned until the cycle ends. The walk reads attributes and the
contents of built-in collections, and runs none of the recipe's own code.
"""

from __future__ import annotations

import re
import sys
import types
from collections import deque
from dataclasses import dataclass
from enum import Enum
from pathlib import PurePath
from typing import Any, Dict, Iterator, List, Optional, Tuple
from uuid import UUID

from rewrite import Cursor, ExecutionContext, Heavyweight, Recipe, SourceFile, Tree, TreeVisitor
from rewrite.java.support_types import JavaType
from rewrite.markers import Marker

# Beyond this many objects in one accumulator the walk stops descending.
_MAX_OBJECTS = 100_000
_MAX_PATHS = 5
_KEY_OR_INDEX = re.compile(r"\[[^\]]*]")

# A recipe lives for the whole run anyway, and type attribution refers to no tree.
_LEAVES = (str, bytes, bytearray, int, float, complex, type, Enum, PurePath, UUID, Recipe, JavaType, types.ModuleType)

_COLLECTIONS = (list, tuple, set, frozenset, deque)


class AccumulatorContents:
    """What the accumulators of the scanning recipes in a run keep, by recipe and by the shape of the path to it."""

    def __init__(self) -> None:
        self._findings: Dict[str, Dict[str, Tuple[str, str, str]]] = {}

    def inspect(self, recipe: Recipe, acc: Any, when: str) -> None:
        try:
            found = _heavyweight(acc)
        except Exception:
            return  # an accumulator the walk can't read is left alone
        for path, what in found:
            shape = _KEY_OR_INDEX.sub("[]", path) + " -> " + what
            self._findings.setdefault(recipe.name, {}).setdefault(shape, (path, what, when))

    def assert_none_found(self) -> None:
        if not self._findings:
            return
        lines = ["A ScanningRecipe's accumulator should keep the values the recipe needs, such as source paths, names "
                 "and ids, and no Heavyweight objects, such as trees (a CompilationUnit or any node in it), cursors, "
                 "visitors, execution contexts, PythonResolutionResult markers and their resolved dependencies:"]
        for recipe, findings in self._findings.items():
            lines.append(f"  {recipe} keeps")
            for shown, (path, what, when) in enumerate(findings.values()):
                if shown == _MAX_PATHS:
                    lines.append(f"    and {len(findings) - _MAX_PATHS} more")
                    break
                lines.append(f"    {path} -> {what}, {when}")
        lines.append("An accumulator stays in memory from the first source file scanned until the cycle ends, so what "
                     "it keeps for each source file adds up over a large repository, and a PythonResolutionResult or "
                     "a resolved dependency reaches the dependencies of a whole project. A recipe that must keep a "
                     "Heavyweight object opts out with RecipeSpec(check_lightweight_accumulators=False).")
        raise AssertionError("\n".join(lines))


@dataclass
class _Node:
    """An object the walk reached, with the way there, spelled out only for a finding."""

    value: Any
    parent: Optional[_Node]
    # An attribute or the accumulator's type name as text, an index, or a _Key
    step: Any

    def path(self) -> str:
        steps: List[str] = []
        node: Optional[_Node] = self
        while node is not None:
            step = node.step
            steps.append(f"[{step}]" if isinstance(step, int) else f"[{_key(step.key)}]"
                         if isinstance(step, _Key) else step)
            node = node.parent
        return "".join(reversed(steps))


@dataclass
class _Key:
    key: Any


def _heavyweight(acc: Any) -> List[Tuple[str, str]]:
    """The path to each heavyweight object `acc` keeps and what it is, breadth first so that each path is short."""
    found: List[Tuple[str, str]] = []
    visited: set = set()
    queue: deque = deque()
    budget = _MAX_OBJECTS

    def visit(value: Any, parent: Optional[_Node], step: Any) -> None:
        nonlocal budget
        if isinstance(value, Heavyweight):
            found.append((_Node(value, parent, step).path(), _describe(value)))
        elif _walks(value) and budget > 0 and id(value) not in visited:
            budget -= 1
            visited.add(id(value))
            queue.append(_Node(value, parent, step))

    visit(acc, None, _type_name(type(acc)))
    while queue:
        node = queue.popleft()
        for child, step in _children(node.value):
            visit(child, node, step)
    return found


def _walks(value: Any) -> bool:
    if value is None or isinstance(value, _LEAVES):
        return False
    return isinstance(value, (dict, *_COLLECTIONS)) or not _in_stdlib(type(value))


def _children(value: Any) -> Iterator[Tuple[Any, Any]]:
    # A collection's contents are read with the built-in methods, which a subclass of the recipe's own can't override.
    if isinstance(value, dict):
        for key, item in dict.items(value):
            yield key, _Key(key)
            yield item, _Key(key)
    elif isinstance(value, _COLLECTIONS):
        base = next(b for b in type(value).__mro__ if b in _COLLECTIONS)
        for i, item in enumerate(base.__iter__(value)):
            yield item, i
    if not _in_stdlib(type(value)):
        yield from ((item, "." + name) for name, item in _attributes(value))


def _attributes(value: Any) -> Iterator[Tuple[str, Any]]:
    try:
        attributes = object.__getattribute__(value, "__dict__")
    except AttributeError:
        attributes = None
    if isinstance(attributes, dict):
        yield from dict.items(attributes)
    for cls in type(value).__mro__:
        slots = cls.__dict__.get("__slots__", ())
        for name in (slots,) if isinstance(slots, str) else slots:
            mangled = f"_{cls.__name__.lstrip('_')}{name}" if name.startswith("__") and not name.endswith("__") else name
            descriptor = cls.__dict__.get(mangled)
            if isinstance(descriptor, types.MemberDescriptorType):
                try:
                    yield name, descriptor.__get__(value, cls)
                except AttributeError:
                    pass  # a slot that was never set


def _in_stdlib(cls: type) -> bool:
    return (cls.__module__ or "").split(".")[0] in sys.stdlib_module_names


def _describe(value: Any) -> str:
    if isinstance(value, SourceFile):
        return f"{_type_name(type(value))} {value.source_path}"
    for kind in (Cursor, TreeVisitor, ExecutionContext):
        if isinstance(value, kind):
            return kind.__name__
    return _type_name(type(value))


def _key(key: Any) -> str:
    if isinstance(key, Recipe):
        text = key.name
    elif isinstance(key, SourceFile):
        text = str(key.source_path)
    elif isinstance(key, (Tree, Marker)):
        text = _type_name(type(key))
    elif isinstance(key, str):
        text = repr(key)
    elif isinstance(key, tuple) and type(key) is tuple:
        text = "(" + ", ".join(_key(k) for k in key) + ("," if len(key) == 1 else "") + ")"
    elif key is None or isinstance(key, (int, float, complex, bytes, PurePath, UUID, Enum)):
        text = str(key)
    else:
        text = _type_name(type(key))
    return text if len(text) <= 60 else text[:60] + "..."


def _type_name(cls: type) -> str:
    return cls.__qualname__.rsplit("<locals>.", 1)[-1]
