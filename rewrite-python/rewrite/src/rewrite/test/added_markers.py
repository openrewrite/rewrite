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

"""Check that a recipe leaves no markers on the LST for its own use.

A marker in a result is explained when a source the recipe was given, or a fresh parse of the printed result,
holds a marker of its type in the same kind of place: on the source file itself, or on an element.
"""

from __future__ import annotations

import dataclasses
from typing import Any, Callable, Dict, List, Optional, Set, Tuple

from rewrite import Tree
from rewrite.execution import Result
from rewrite.java.support_types import J, JavaType, JLeftPadded, JRightPadded, Space
from rewrite.markers import Markers, Markup, RecipesThatMadeChanges, SearchResult, UnknownJavaMarker
from rewrite.style import NamedStyles
from rewrite.tree import PrintOutputCapture, SourceFile
from rewrite.visitor import Cursor

# (whether the marker is on the source file itself, the marker's type)
Placement = Tuple[bool, str]

# How a recipe reports what it found, and the run's own record of which recipes changed a file.
_ALLOWED = (SearchResult, Markup, RecipesThatMadeChanges)

# Markers that rewrite's own Java recipes still leave for themselves, as the Java harness allows.
_REWRITE_RECIPE_MARKERS = frozenset({
    "org.openrewrite.marker.AlreadyReplaced",
    "org.openrewrite.maven.marker.AlreadyIncremented",
    "org.openrewrite.yaml.MultilineScalarChanged",
    "org.openrewrite.java.search.UsesMethod$MethodMatch",
})
_FIELDS: Dict[type, Tuple[str, ...]] = {}

_GUIDANCE = (
    "Markers travel with the LST to every recipe that runs after this one and to every tool that reads the "
    "result, so they are not a place for a recipe to keep information for itself. Instead:\n"
    "  - share information between the visit methods of one visitor with cursor messages, "
    "e.g. self.cursor.put_message(...) and self.cursor.get_nearest_message(...)\n"
    "  - share information between source files, or from scanning to editing, through the accumulator of a "
    "ScanningRecipe\n"
    "  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging that code\n"
    "  - report a finding with SearchResult, and a problem with Markup"
)


def assert_no_added_markers(
        before: List[SourceFile],
        results: List[Result],
        reparse: Callable[[SourceFile], Optional[SourceFile]],
) -> None:
    """Raise `AssertionError` naming each marker a result holds where no source and no fresh parse holds one."""
    explained: Optional[Set[Placement]] = None
    lines: List[str] = []
    for result in results:
        after = result._after
        if after is None or after is result._before:
            continue
        if explained is None:
            explained = set()
            for source_file in before:
                explained.update(placements(source_file))
        unexplained = {p: holder for p, holder in placements(after).items() if p not in explained}
        if unexplained:
            for p in _fresh_placements(after, reparse):
                unexplained.pop(p, None)
        if unexplained:
            lines.append(f"  {after.source_path}{_how_changed(result._before, after)}")
            lines.extend(f"    {marker_type} {_place(after, on_source_file, holder)}"
                         for (on_source_file, marker_type), holder in unexplained.items())
    if lines:
        raise AssertionError(
            "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of "
            "its output contain:\n" + "\n".join(lines) + "\n" + _GUIDANCE
        )


def placements(source_file: SourceFile) -> Dict[Placement, Any]:
    """Each placement in `source_file`, with the first node, padding or comment that holds it."""
    found: Dict[Placement, Any] = {}
    seen: Set[int] = set()
    pending: List[Any] = [source_file]
    while pending:
        value = pending.pop()
        if id(value) in seen:
            continue
        if isinstance(value, list):
            seen.add(id(value))
            pending.extend(reversed(value))
        # A type graph is cyclic and large, and holds no markers.
        elif dataclasses.is_dataclass(value) and not isinstance(value, (type, JavaType)):
            seen.add(id(value))
            children = []
            for name in _field_names(type(value)):
                child = getattr(value, name)
                if isinstance(child, Markers):
                    for marker in child.markers:
                        # A recipe that formats with a style it was given passes the style on to later formatting
                        if not isinstance(marker, _ALLOWED) and not (value is source_file and isinstance(marker, NamedStyles)):
                            marker_type = _marker_type(marker)
                            if marker_type not in _REWRITE_RECIPE_MARKERS:
                                found.setdefault((value is source_file, marker_type), value)
                else:
                    children.append(child)
            pending.extend(reversed(children))
    return found


def _field_names(cls: type) -> Tuple[str, ...]:
    names = _FIELDS.get(cls)
    if names is None:
        names = _FIELDS[cls] = tuple(f.name for f in dataclasses.fields(cls))
    return names


def _marker_type(marker: Any) -> str:
    # A host marker Python has no class for arrives as a mapping, which names the host type.
    data = marker.data if isinstance(marker, UnknownJavaMarker) else marker
    if isinstance(data, dict):
        host_type = data.get('kind') or data.get('@c')
        if isinstance(host_type, str):
            return host_type
    cls = type(marker)
    return f"{cls.__module__}.{cls.__qualname__}"


def _fresh_placements(source_file: SourceFile, reparse: Callable[[SourceFile], Optional[SourceFile]]) -> Set[Placement]:
    """The placements of a fresh parse of `source_file`, which explain the syntax markers of code a recipe wrote."""
    try:
        fresh = reparse(source_file)
    except Exception:
        return set()
    return set() if fresh is None else set(placements(fresh))


def _how_changed(before: Optional[SourceFile], after: SourceFile) -> str:
    if before is None:
        return " (generated)"
    return " (the printed source is unchanged)" if before.print_all() == after.print_all() else ""


def _place(source_file: SourceFile, on_source_file: bool, holder: Any) -> str:
    if on_source_file:
        return "on the source file"
    snippet = _snippet(source_file, holder)
    return f"on {type(holder).__name__}" + (f" `{snippet}`" if snippet else "")


def _snippet(source_file: SourceFile, holder: Any) -> Optional[str]:
    """The first line of what `holder` prints, or None when it prints only as part of the tree around it."""
    tree = holder.element if isinstance(holder, (JRightPadded, JLeftPadded)) else holder
    if not isinstance(tree, Tree):
        return None
    try:
        if isinstance(tree, J):
            tree = tree.replace(prefix=Space.EMPTY)
        printed = tree.print(Cursor(Cursor(None, Cursor.ROOT_VALUE), source_file),
                             PrintOutputCapture(0, PrintOutputCapture.MarkerPrinter.SANITIZED))
    except Exception:
        return None
    line = next(iter(printed.strip().splitlines()), "").strip()
    return line if len(line) <= 60 else line[:57] + "..."
