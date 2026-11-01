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

"""Run a recipe on sources its test doesn't give it, which repositories hold besides those the test does."""

from __future__ import annotations

import sysconfig
import time
import traceback
from pathlib import Path
from typing import Callable, Dict, List, Optional, Set, Tuple

from rewrite import ExecutionContext, InMemoryLargeSourceSet, Recipe, RecipeRunException, SourceFile, random_id
from rewrite.markers import Markers, ParseExceptionResult
from rewrite.parser import ParseError

# Probes of a recipe stop in this process once they have taken this long.
_TIME_CAP_SECONDS = 20.0
_PROBE_SECONDS: Dict[str, float] = {}

# The recipe, with its options, and the kind of source each probe ran with, so that each runs once in this process.
_PROBED: Set[Tuple[str, str, str]] = set()

_STDLIB = Path(sysconfig.get_paths()["stdlib"]).resolve()


def assert_none_throw_on_empty(
        recipe: Recipe,
        sources: List[SourceFile],
        changed: Set[object],
        parse_empty: Callable[[SourceFile], Optional[SourceFile]],
        context: Callable[[], ExecutionContext],
        cycles: int,
) -> None:
    """Run the recipe on an empty file of each kind it changed, and fail if it throws there.

    ``parse_empty`` parses an empty file at the path of a source, and ``changed`` holds the ids of the sources the
    recipe changed.
    """
    kinds: Set[type] = set()
    for before in sources:
        if before.id not in changed or type(before) in kinds:
            continue
        kinds.add(type(before))
        if not _first_probe(recipe, before, "empty"):
            continue
        start = time.perf_counter()
        try:
            empty = _with_markers_of(parse_empty(before), before)
            thrown = None if empty is None else run(recipe, [empty], context(), cycles)
        finally:
            _charge(recipe, time.perf_counter() - start)
        if empty is not None and thrown is not None:
            raise AssertionError(_empty_message(recipe, empty, thrown)) from thrown


def assert_none_throw_on_unparsed(
        recipe: Recipe,
        sources: List[SourceFile],
        changed: Set[object],
        context: Callable[[], ExecutionContext],
        cycles: int,
) -> None:
    """Run the recipe with a source it changed in place of the file as a parse error, and fail if it throws there.

    Java also puts a quark and plain text in the source's place, which Python has no counterpart of.
    """
    kinds: Set[type] = set()
    for before in sources:
        if before.id not in changed or isinstance(before, ParseError) or type(before) in kinds:
            continue
        kinds.add(type(before))
        if not _first_probe(recipe, before, "parse error"):
            continue
        start = time.perf_counter()
        try:
            unparsed = _parse_error(before)
            thrown = None if unparsed is None else run(
                recipe, [unparsed if source is before else source for source in sources], context(), cycles)
        finally:
            _charge(recipe, time.perf_counter() - start)
        if unparsed is not None and thrown is not None:
            raise AssertionError(_unparsed_message(recipe, unparsed, thrown)) from thrown


def run(recipe: Recipe, sources: List[SourceFile], ctx: ExecutionContext, cycles: int) -> Optional[Exception]:
    """What the recipe raises when it runs for up to ``cycles`` cycles on ``sources``, or None."""
    try:
        for _ in range(cycles):
            results = recipe.run(InMemoryLargeSourceSet(sources), ctx)
            if all(result._before is result._after for result in results):
                break
            sources = [result._after for result in results if result._after is not None]
    except Exception as thrown:
        return thrown
    return None


def describe(thrown: BaseException, indent: str, recipe: Recipe) -> str:
    """The exception, with the first line of its message, the innermost frame outside the standard library and,
    when that frame isn't the recipe's, the innermost frame that is."""
    cause = thrown.cause if isinstance(thrown, RecipeRunException) and thrown.cause is not None else thrown
    cls = type(cause)
    description = indent + (cls.__qualname__ if cls.__module__ == "builtins" else f"{cls.__module__}.{cls.__qualname__}")
    message = str(cause).strip()
    if message:
        first_line = message.splitlines()[0]
        description += ": " + (first_line[:200] + "..." if len(first_line) > 200 else first_line)
    modules = _modules(recipe, set())
    outside_stdlib = False
    for frame, line in reversed(list(traceback.walk_tb(cause.__traceback__))):
        of_recipe = frame.f_globals.get("__name__") in modules
        if not outside_stdlib and not _in_stdlib(frame.f_code.co_filename):
            outside_stdlib = True
            description += f"\n{indent}    at {_frame(frame.f_code, line)}"
            if of_recipe:
                break
        elif outside_stdlib and of_recipe:
            description += f"\n{indent}    at {_frame(frame.f_code, line)}"
            break
    return description


def _first_probe(recipe: Recipe, source: SourceFile, kind: str) -> bool:
    """Whether this kind of probe of the recipe on this kind of source has yet to run in this process."""
    if _PROBE_SECONDS.get(_time_cap_key(recipe), 0.0) > _TIME_CAP_SECONDS:
        return False
    probe = (repr(recipe.descriptor()), _type_name(type(source)), kind)
    if probe in _PROBED:
        return False
    _PROBED.add(probe)
    return True


def _charge(recipe: Recipe, seconds: float) -> None:
    key = _time_cap_key(recipe)
    _PROBE_SECONDS[key] = _PROBE_SECONDS.get(key, 0.0) + seconds


def _time_cap_key(recipe: Recipe) -> str:
    # A composite's steps tell it apart, since a test's composites share one name.
    try:
        steps = recipe.recipe_list()
    except Exception:
        steps = []
    return recipe.name + ("[" + ",".join(_time_cap_key(step) for step in steps) + "]" if steps else "")


def _with_markers_of(empty: Optional[SourceFile], before: SourceFile) -> Optional[SourceFile]:
    """``empty`` with the markers of ``before`` that describe its project, or None when it isn't an empty source of
    the same kind."""
    if empty is None or type(empty) is not type(before) or empty.print_all() != "":
        return None
    present = {type(marker) for marker in empty.markers.markers}
    markers = empty.markers
    for marker in before.markers.markers:
        # A resolution result describes what the file itself declares, which an empty file doesn't.
        if type(marker) not in present and not type(marker).__name__.endswith("ResolutionResult"):
            markers = markers.add(marker)
    return empty.replace(markers=markers)


def _empty_message(recipe: Recipe, empty: SourceFile, thrown: Exception) -> str:
    return (
        "Check for the structure the recipe edits before using it, so that it handles an empty file or leaves it "
        "unchanged instead of throwing.\n"
        f"{recipe.name} threw when run on an empty {_type_name(type(empty))} at {empty.source_path}"
        f"{_markers_of_the_test(empty.markers)}:\n"
        f"{describe(thrown, '    ', recipe)}\n"
        "The test gives the recipe only files that contain what it edits, but repositories also hold empty files, "
        "such as an empty __init__.py, which parses to a CompilationUnit whose only statement is an Empty.\n"
        "  - return the tree unchanged when what the visitor assumes is absent: check isinstance before using a node "
        "as a subtype, the length of a list before indexing it and None before reading an attribute\n"
        "  - if the recipe adds what is missing, build that content correctly for an empty file too\n"
        "  - don't catch the exception to make this pass\n"
        'Then add a test with an empty source, python(""), that states the intended result.'
    )


def _parse_error(before: SourceFile) -> Optional[ParseError]:
    """The file ``before`` was parsed from, as the parse error a repository holds when the parser fails on it."""
    try:
        # These come from parsing the file itself.
        kept = [marker for marker in before.markers.markers if not isinstance(marker, ParseExceptionResult)
                and not type(marker).__name__.endswith("ResolutionResult")]
        exception = ParseExceptionResult(random_id(), "PythonParser", "SyntaxError", "The file could not be parsed")
        return ParseError(random_id(), before.markers.replace(markers=[*kept, exception]), before.source_path,
                          before.file_attributes, before.charset_name, getattr(before, "charset_bom_marked", False),
                          getattr(before, "checksum", None), before.print_all(), None)
    except Exception:
        return None


def _unparsed_message(recipe: Recipe, unparsed: ParseError, thrown: Exception) -> str:
    return (
        "Check the kind of source file before using it, so that the recipe leaves a file it can't edit unchanged "
        "instead of throwing.\n"
        f"{recipe.name} threw when {unparsed.source_path} was a ParseError (a file its parser failed on)"
        f"{_markers_of_the_test(unparsed.markers)}:\n"
        f"{describe(thrown, '    ', recipe)}\n"
        "Besides the files the test gives it, a repository holds files the parser failed on, at the same paths and "
        "with the same markers as files the recipe edits.\n"
        "  - visit with a PythonVisitor, whose is_acceptable skips other kinds of source file\n"
        "  - in a TreeVisitor, check isinstance(tree, CompilationUnit) before using it as one, not the file's path "
        "or markers\n"
        "  - when the scanner found no file of the kind the recipe edits, generate and change nothing, instead of "
        "reading an empty accumulator\n"
        "  - don't catch the exception to make this pass"
    )


def _markers_of_the_test(markers: Markers) -> str:
    names = list(dict.fromkeys(_type_name(type(marker)) for marker in markers.markers
                               if not isinstance(marker, ParseExceptionResult)))
    return f" with the markers of the test's source ({', '.join(names)})" if names else ""


def _modules(recipe: Recipe, modules: Set[str]) -> Set[str]:
    if type(recipe).__module__ not in modules:
        modules.add(type(recipe).__module__)
        try:
            for child in recipe.recipe_list():
                _modules(child, modules)
        except Exception:
            pass  # a recipe that can't list its steps is still named
    return modules


def _in_stdlib(filename: str) -> bool:
    try:
        return Path(filename).resolve().is_relative_to(_STDLIB) and "site-packages" not in filename
    except (OSError, ValueError):
        return False


def _frame(code, line: Optional[int]) -> str:
    return f"{code.co_qualname.rsplit('<locals>.', 1)[-1]} ({Path(code.co_filename).name}:{line})"


def _type_name(cls: type) -> str:
    return cls.__qualname__.rsplit("<locals>.", 1)[-1]
