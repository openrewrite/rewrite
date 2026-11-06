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

"""Find a recipe that auto-formats a whole source file or class that existed before the run.

Test inputs are usually formatted already, so the reformatting this causes on real code rarely shows in an expected
output.
"""

from __future__ import annotations

import re
import sys
from contextlib import contextmanager
from dataclasses import dataclass
from pathlib import Path
from types import FrameType
from typing import Dict, Iterator, List, Optional, Set, Tuple

from rewrite import Cursor, Recipe, SourceFile, Tree
from rewrite.execution import Result
from rewrite.java import tree as j
from rewrite.python.format.auto_format import formatting_listener
from rewrite.python.visitor import PythonVisitor

_WHITESPACE = re.compile(r"\s+")

# The formatter and the visitor machinery between it and the code that asked for the format.
_PLUMBING = ("rewrite.python.format", "rewrite.visitor", "rewrite.java.visitor", "rewrite.python.visitor",
             "rewrite.preconditions")


@dataclass
class _Request:
    tree: Tree
    source_file_id: object
    call_site: str
    target: str


class FormattingScope:
    """Records each request to auto-format a whole source file or class while the recipe runs."""

    def __init__(self, recipe: Recipe, enabled: bool):
        self._recipe = recipe
        self._enabled = enabled
        self._requests: List[_Request] = []
        # By id, holding each tree so that no other takes its id while the run lasts
        self._requested: Dict[int, Tree] = {}

    @contextmanager
    def record(self) -> Iterator[None]:
        if not self._enabled:
            yield
            return
        with formatting_listener(self._on_format):
            yield

    def _on_format(self, tree: Tree, stop_after: Optional[Tree], cursor: Cursor) -> None:
        if stop_after is not None or not isinstance(tree, (SourceFile, j.ClassDeclaration)) or \
                id(tree) in self._requested:
            return
        self._requested[id(tree)] = tree
        source_file = tree if isinstance(tree, SourceFile) else cursor.first_enclosing(SourceFile)
        if source_file is None:
            return
        caller, entry = _caller(sys._getframe(1))
        # Formatters format whole files, and so does a recipe whose own visitor is a formatter
        if caller is None or _in_format_package(_module(caller)) or _module(caller) == "rewrite.recipe":
            return
        self._requests.append(_Request(tree, source_file.id, _call_site(caller, entry), _describe(tree)))

    def assert_formatted_only_what_changed(self, results: List[Result]) -> None:
        if not self._requests or _is_formatter(self._recipe):
            return
        by_before = {r._before.id: r for r in results if r._before is not None and r._after is not None}
        trees_by_file: Dict[object, Dict[object, Tree]] = {}
        found: Dict[str, Dict[str, None]] = {}
        for request in self._requests:
            result = by_before.get(request.source_file_id)
            if result is None or _only_whitespace_changed(result):
                continue
            before = result._before
            assert before is not None
            if before.id not in trees_by_file:
                trees_by_file[before.id] = _trees_by_id(before)
            original = trees_by_file[before.id].get(request.tree.id)
            if original is not None and not _holds_none_of_its_children(original, request.tree):
                found.setdefault(request.call_site, {})[f"{request.target} of {before.source_path}"] = None
        if found:
            raise AssertionError(_message(self._recipe, found))


def _caller(frame: Optional[FrameType]) -> Tuple[Optional[FrameType], Optional[FrameType]]:
    """The first frame outside the formatter and the visitor machinery, and the frame it called."""
    entry = None
    while frame is not None and _is_plumbing(frame):
        entry, frame = frame, frame.f_back
    return frame, entry


def _is_plumbing(frame: FrameType) -> bool:
    module = _module(frame)
    # A ScanningRecipe's editor passes the visit on to the visitor of its accumulator
    return module == __name__ or any(module == p or module.startswith(p + ".") for p in _PLUMBING) or \
        module == "rewrite.recipe" and frame.f_code.co_name == "visit"


def _module(frame: FrameType) -> str:
    return str(frame.f_globals.get("__name__", ""))


def _call_site(caller: FrameType, entry: Optional[FrameType]) -> str:
    called = entry.f_code.co_name if entry is not None else ""
    code = caller.f_code
    site = f"{code.co_qualname.rsplit('<locals>.', 1)[-1]} ({Path(code.co_filename).name}:{caller.f_lineno})"
    return site + (f" called {called} on" if called in ("auto_format", "maybe_auto_format")
                   else " ran an AutoFormatVisitor on")


def _describe(tree: Tree) -> str:
    return f"class `{tree.name.simple_name}`" if isinstance(tree, j.ClassDeclaration) else "all"


def _is_formatter(recipe: Recipe) -> bool:
    # A formatting recipe may format whole files, and so may the recipes run alongside it
    if _in_format_package(recipe.name.rpartition(".")[0]) or _in_format_package(type(recipe).__module__):
        return True
    try:
        return any(_is_formatter(child) for child in recipe.recipe_list())
    except Exception:
        return False


def _in_format_package(package: str) -> bool:
    return ".format." in f".{package}."


def _only_whitespace_changed(result: Result) -> bool:
    before, after = result._before, result._after
    return before is not None and after is not None and \
        _WHITESPACE.sub("", before.print_all()) == _WHITESPACE.sub("", after.print_all())


class _Trees(PythonVisitor[Dict[object, Tree]]):
    def pre_visit(self, tree, p):
        p[tree.id] = tree
        return tree


def _trees_by_id(tree: Tree) -> Dict[object, Tree]:
    trees: Dict[object, Tree] = {}
    _Trees().visit(tree, trees)
    return trees


def _holds_none_of_its_children(original: Tree, formatted: Tree) -> bool:
    """Whether ``formatted`` holds none of the children ``original`` had, as a class with its body set aside or a copy
    holding only a new element does, which reformats no existing code. A tree with no children to begin with would
    format them on real code."""
    children = _children(original)
    return bool(children) and children.isdisjoint(_children(formatted))


def _children(tree: Tree) -> Set[object]:
    """The ids of the statements in a class's body, or of the trees any other tree holds directly."""
    parent = tree.body if isinstance(tree, j.ClassDeclaration) else tree
    found: Set[object] = set()

    class Children(PythonVisitor[None]):
        def pre_visit(self, tree, p):
            if tree is not parent:
                found.add(tree.id)
                self.stop_after_pre_visit()
            return tree

    Children().visit(parent, None)
    return found


def _message(recipe: Recipe, found: Dict[str, Dict[str, None]]) -> str:
    lines = ["Format only what the recipe changed, not the whole class or file around it.",
             f"{recipe.name} auto-formatted a whole file or class that existed before the run, with no stop_after:"]
    for call_site, targets in found.items():
        lines.append(f"  {call_site}")
        lines.extend(f"    {target}" for target in targets)
    lines.append(
        "That rewrites the whitespace of code the recipe did not change. Test inputs are usually formatted already, "
        "so expected outputs rarely show it, but on real code the diff will carry unrelated reformatting. Instead:\n"
        "  - format the element you created or replaced: maybe_auto_format(original, changed, ctx, "
        "cursor=self.cursor.parent_tree_cursor()) on that element, or auto_format(new_element, ctx, cursor=...) "
        "with a cursor on its parent\n"
        "  - when a new element only formats right inside its container, format a copy of the container that holds "
        "just the new element, and add the formatted element to the original\n"
        "  - after changing a class's decorators, stop after its name: maybe_auto_format(c, updated, ctx, "
        "stop_after=updated.name, cursor=self.cursor.parent_tree_cursor())\n"
        "  - when you remove an element, give its prefix to the element that takes its place\n"
        "  - a template already formats what it inserts, so drop the extra call\n"
        "Recipes in a *.format package are exempt. Any other recipe whose purpose is formatting opts out with "
        "RecipeSpec(check_minimal_formatting=False).")
    return "\n".join(lines)
