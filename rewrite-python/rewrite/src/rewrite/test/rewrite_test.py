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

"""Core test infrastructure for OpenRewrite Python recipes."""

from __future__ import annotations

import ast
from contextlib import contextmanager
from dataclasses import dataclass, field, replace
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Tuple, TYPE_CHECKING, Union
from uuid import uuid4

from rewrite import (
    Cursor,
    DelegatingExecutionContext,
    ExecutionContext,
    InMemoryExecutionContext,
    InMemoryLargeSourceSet,
    PrintOutputCapture,
    Recipe,
    TreeVisitor,
)
from rewrite.data_table import DATA_TABLE_STORE, DataTable, InMemoryDataTableStore, Row
from rewrite.execution import Result
from rewrite.parser import ParseError
from rewrite.markers import ParseExceptionResult

from .added_markers import assert_no_added_markers
from .spec import SourceSpec, AfterRecipeText, dedent
from .well_formed import assert_well_formed

if TYPE_CHECKING:
    from rewrite.tree import SourceFile
    from rewrite.python.tree import CompilationUnit


class NoopRecipe(Recipe):
    """A recipe that makes no changes, used as default."""

    @property
    def name(self) -> str:
        return "org.openrewrite.noop"

    @property
    def display_name(self) -> str:
        return "Do nothing"

    @property
    def description(self) -> str:
        return "Default no-op test recipe."


class AdHocRecipe(Recipe):
    """
    A recipe that wraps a visitor for ad-hoc testing.

    This allows testing visitors directly without creating a full Recipe subclass.
    """

    def __init__(self, visitor: TreeVisitor[Any, ExecutionContext]):
        self._visitor = visitor

    @property
    def name(self) -> str:
        return "org.openrewrite.adhoc"

    @property
    def display_name(self) -> str:
        return "Ad-hoc recipe"

    @property
    def description(self) -> str:
        return "Ad-hoc recipe for testing."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        return self._visitor


def from_visitor(visitor: TreeVisitor[Any, ExecutionContext]) -> Recipe:
    """
    Create a Recipe from a TreeVisitor for testing.

    This is a convenience function for testing visitors directly:

        spec = RecipeSpec(recipe=from_visitor(MyVisitor()))
        spec.rewrite_run(python("x = 1", "x = 2"))

    Args:
        visitor: The visitor to wrap

    Returns:
        A Recipe that applies the visitor
    """
    return AdHocRecipe(visitor)


@dataclass
class _Workspace:
    """Where a run's sources sit on disk while they parse."""

    paths: Dict[int, Path]
    texts: Dict[int, str]
    written: Dict[int, str]
    ty_client: Optional[Any]
    # Whether ty attributed this run, and if not, why.
    ty_status: str

    def source_path(self, spec: SourceSpec) -> Path:
        return self.paths[id(spec)]

    def text(self, spec: SourceSpec) -> str:
        return self.texts[id(spec)]

    def file_path(self, spec: SourceSpec) -> Optional[str]:
        # Only a ty session reads this path; an untyped parse has no use for it.
        return self.written.get(id(spec)) if self.ty_client is not None else None


@dataclass
class RecipeSpec:
    """
    Configuration for running recipe tests.

    Provides the context and configuration for testing recipes against
    source files using the before/after pattern.

    Example:
        spec = RecipeSpec(recipe=MyRecipe())
        spec.rewrite_run(
            python(
                '''
                import os
                ''',
                '''
                import sys
                '''
            )
        )
    """

    # The recipe to test (defaults to no-op)
    recipe: Recipe = field(default_factory=NoopRecipe)

    # Execution context for parsing and recipe execution
    execution_context: ExecutionContext = field(default_factory=InMemoryExecutionContext)

    # Whether to validate parse/print idempotence
    check_parse_print_idempotence: bool = True

    # Whether to allow empty diffs (recipe modifies AST but printed output unchanged)
    allow_empty_diff: bool = False

    # Whether to fail a recipe that leaves markers on the LST that neither its inputs
    # nor a fresh parse of its output carry
    check_reporting_markers_only: bool = True

    # Whether to enable type attribution via ty-types.
    # Set to False for pure parse/print tests to avoid the ty-types subprocess overhead.
    type_attribution: bool = True

    # The cycles to run, by default one more than `expected_cycles_that_make_changes` and at least two.
    # The cycles after the expected ones, and a run on a fresh parse of the output, fail the test if
    # they change anything, so setting this to 1 turns the stability checks off.
    cycles: Optional[int] = None

    # How many cycles a recipe needs to settle, by default 1. A recipe that only reaches its result
    # over several cycles declares how many here, and the test then compares the output of the last
    # of them.
    expected_cycles_that_make_changes: Optional[int] = None

    # Assertions on the rows the first cycle writes to a data table, each with the name of its table
    data_table_assertions: List[Tuple[str, Callable[[List[Any]], None]]] = field(default_factory=list)

    def with_recipe(self, recipe: Recipe) -> "RecipeSpec":
        return replace(self, recipe=recipe)

    def with_allow_empty_diff(self, value: bool) -> "RecipeSpec":
        return replace(self, allow_empty_diff=value)

    def with_recipes(self, *recipes: Recipe) -> "RecipeSpec":
        if len(recipes) == 1:
            return self.with_recipe(recipes[0])
        return self.with_recipe(_CompositeRecipe(list(recipes)))

    def data_table(
        self, table: Union[str, DataTable[Row]], assertion: Callable[[List[Row]], None],
    ) -> "RecipeSpec":
        """
        Assert on the rows the first cycle of each ``rewrite_run`` writes to a data table,
        failing the run when the recipe wrote none.

        Example:
            def expect_usages(rows: List[DeprecatedUsage]) -> None:
                assert rows == [DeprecatedUsage("a.py", "foo")]

            spec = RecipeSpec(recipe=FindDeprecatedUsages()).data_table(
                FindDeprecatedUsages.table, expect_usages)

        Args:
            table: The data table, or its fully qualified name
            assertion: Called with the rows the recipe wrote to the table
        """
        name = table if isinstance(table, str) else table.name
        return replace(self, data_table_assertions=[*self.data_table_assertions, (name, assertion)])

    def rewrite_run(self, *source_specs: SourceSpec) -> None:
        """
        Execute the recipe test with the given source specifications.

        This method:
        1. Groups specs by kind (language)
        2. Parses them using appropriate parsers
        3. Validates parse/print idempotence if enabled
        4. Runs the recipe for ``expected_cycles_that_make_changes`` cycles, each on the
           output of the one before
        5. Checks that the recipe added no markers for its own use, if enabled
        6. Compares results with expected "after" states
        7. Checks that one more cycle, and a run on a fresh parse of the output, change nothing

        Setting ``REWRITE_PYTHON_DUMP_TYPES`` prints each parsed file's type
        attribution, and a recipe that produces no change names the nodes that
        carry none. See the README section "Inspecting type attribution".

        Args:
            *source_specs: Variable number of source specifications

        Raises:
            AssertionError: If any validation fails
        """
        specs = list(source_specs)
        specs_by_kind = self._group_by_kind(specs)

        # Parse and validate all source files
        all_parsed: List[Tuple[SourceSpec, SourceFile]] = []
        texts = {id(spec): dedent(spec.before) for spec in specs if spec.before is not None}
        with self._workspace(specs, texts) as workspace:
            ty_status = workspace.ty_status
            for kind, kind_specs in specs_by_kind.items():
                parsed = self._parse(kind_specs, workspace)
                self._expect_no_parse_failures(parsed)
                for _, sf in parsed:
                    assert_well_formed(sf)
                if self.check_parse_print_idempotence:
                    self._expect_parse_print_idempotence(parsed)
                all_parsed.extend(parsed)

        # Apply before_recipe hooks (after idempotence check, before recipe execution)
        all_parsed = [
            (spec, spec.before_recipe(sf) or sf) if spec.before_recipe else (spec, sf)
            for spec, sf in all_parsed
        ]

        source_files = [sf for _, sf in all_parsed]
        expected_cycles = self.expected_cycles_that_make_changes
        if expected_cycles is None:
            expected_cycles = 1
        cycles = self.cycles if self.cycles is not None else max(2, expected_cycles + 1)
        if cycles < max(1, expected_cycles):
            raise ValueError(
                f"cycles={cycles} has to be at least 1 and at least "
                f"expected_cycles_that_make_changes={expected_cycles}"
            )
        data_tables = InMemoryDataTableStore()
        self.execution_context.put_message(DATA_TABLE_STORE, data_tables)
        results, unsettled = self._run_cycles(source_files, cycles, expected_cycles)
        if self.check_reporting_markers_only:
            assert_no_added_markers(source_files, results, self._reparse)

        # Build result map: source_file.id -> after_source_file
        result_map: Dict[Any, Optional[SourceFile]] = {}
        generated: List[SourceFile] = []
        for result in results:
            if result._before is None:
                # A file the recipe created, which no spec's `before` can be keyed by.
                if result._after is not None:
                    generated.append(result._after)
            else:
                # Check if there was a change
                if result._after is not None and result._after is not result._before:
                    result_map[result._before.id] = result._after
                else:
                    result_map[result._before.id] = None

        # Validate results match expected after states
        self._expect_results_to_match_after(source_specs, all_parsed, result_map, ty_status)
        self._expect_generated(source_specs, generated)
        if unsettled is not None:
            cycle, changes = unsettled
            self._expect_no_further_changes(
                changes,
                f"Expected recipe to complete in {_plural_cycles(expected_cycles)}, but cycle {cycle} changed",
            )
        if cycles > expected_cycles:
            self._expect_stable_on_fresh_parse(specs, all_parsed, results)
        self._expect_data_tables(data_tables)

    def _expect_data_tables(self, store: InMemoryDataTableStore) -> None:
        written = store.get_data_tables()
        for name, assertion in self.data_table_assertions:
            data_table = next((dt for dt in written if dt.name == name), None)
            if data_table is None:
                found = list(dict.fromkeys(dt.name for dt in written))
                raise AssertionError(f"No data table named {name}"
                                     + (f"\nFound data tables: {', '.join(found)}" if found else ""))
            assertion(list(store.get_rows(name, data_table.group)))

    def _run_cycles(
        self, sources: List[SourceFile], cycles: int, expected_cycles: int,
    ) -> Tuple[List[Result], Optional[Tuple[int, List[Result]]]]:
        """Run the recipe cycle after cycle on what the cycle before produced.

        Returns the changes of the expected cycles, from the parsed sources to the output of the
        last of them, and the cycle that changed the sources after that, with its changes.
        """
        # Each entry follows one source file from where it started to where it is now, either
        # end being None for a file a cycle generated or deleted.
        lineages: List[List[Optional[SourceFile]]] = [[sf, sf] for sf in sources]

        def changeset() -> List[Result]:
            return [Result(origin, latest) for origin, latest in lineages]

        settled: Optional[List[Result]] = changeset() if expected_cycles == 0 else None
        cycles_that_made_changes = 0
        for cycle in range(1, cycles + 1):
            live = [latest for _, latest in lineages if latest is not None]
            ctx = self.execution_context if cycle == 1 else _LaterCycleContext(self.execution_context)
            changes = [r for r in self.recipe.run(InMemoryLargeSourceSet(live), ctx) if r._before is not r._after]
            if not changes:
                break
            if settled is not None:
                return settled, (cycle, changes)
            cycles_that_made_changes += 1
            for change in changes:
                lineage = next((lineage for lineage in lineages
                                if change._before is not None and lineage[1] is change._before), None)
                if lineage is not None:
                    lineage[1] = change._after
                else:
                    lineages.append([None, change._after])
            if cycle >= expected_cycles:
                settled = changeset()
        if expected_cycles > 1 and cycles_that_made_changes < expected_cycles:
            raise AssertionError(
                f"Expected recipe to complete in {_plural_cycles(expected_cycles)}, but it settled after "
                f"{_plural_cycles(cycles_that_made_changes)}. Lower expected_cycles_that_make_changes."
            )
        return (settled if settled is not None else changeset()), None

    def _expect_no_further_changes(self, changes: List[Result], headline: str, sanitized: bool = False) -> None:
        found: List[str] = []
        for change in changes:
            before, after = change._before, change._after
            if before is None:
                if after is not None:
                    found.append(f"  {after.source_path}, which it generated")
            elif after is None:
                found.append(f"  {before.source_path}, which it deleted")
            else:
                expected, actual = _print(before, sanitized), _print(after, sanitized)
                if expected != actual:
                    found.append(f"  {after.source_path}\nExpected:\n{expected}\n\nActual:\n{actual}")
                elif not sanitized and not self.allow_empty_diff:
                    found.append(f"  {after.source_path}, whose LST it changed without changing the printed source")
        if found:
            raise AssertionError("\n".join([
                f"{headline} the sources again:",
                *found,
                "A recipe should recognize code it has already changed and leave it alone, so that running it "
                "again changes nothing. A recipe that is meant to need more cycles declares how many with "
                "expected_cycles_that_make_changes.",
            ]))

    def _expect_stable_on_fresh_parse(
        self,
        specs: List[SourceSpec],
        parsed: List[Tuple[SourceSpec, SourceFile]],
        results: List[Result],
    ) -> None:
        """Run the recipe once more on a fresh parse of its output.

        Code that the next run reads is parsed from the printed output, not the LST this run
        produced, and a recipe that recognizes its own output only by the shape of that LST
        changes it again.
        """
        from rewrite.python.tree import CompilationUnit
        if not any(r._before is not r._after for r in results):
            return
        finals = [r._after for r in results if r._after is not None]

        spec_of: Dict[int, SourceSpec] = {}
        for result in results:
            if result._after is None:
                continue
            spec = next((s for s, sf in parsed if sf is result._before), None) if result._before is not None else None
            spec_of[id(result._after)] = spec if spec is not None else SourceSpec("python", None)

        reparsable = [sf for sf in finals if isinstance(sf, CompilationUnit) and _in_workspace(sf.source_path)]
        reparse_specs = {id(sf): replace(spec_of[id(sf)], path=Path(str(sf.source_path))) for sf in reparsable}
        texts = {id(reparse_specs[id(sf)]): _print(sf, True) for sf in reparsable}
        # The rest of the run's specs, like a pyproject(), shape the workspace as they did the first parse.
        others = [s for s in specs if s.kind != "python" and s.before is not None]
        texts.update({id(s): dedent(s.before) for s in others})

        reparsed_by_final: Dict[int, SourceFile] = {}
        with self._workspace([*reparse_specs.values(), *others], texts) as workspace:
            for sf in reparsable:
                spec = reparse_specs[id(sf)]
                try:
                    reparsed = self._parse_python(
                        workspace.text(spec), workspace.source_path(spec), workspace.file_path(spec),
                        workspace.ty_client,
                    )
                except Exception:
                    continue
                if _print(reparsed, True) == workspace.text(spec):
                    # What the source file carries, like its styles, comes from outside its text.
                    reparsed_by_final[id(sf)] = reparsed.replace(markers=sf.markers)
        if not reparsed_by_final:
            return

        sources = [reparsed_by_final.get(id(sf), sf) for sf in finals]
        ctx = _LaterCycleContext(self.execution_context)
        changes = [r for r in self.recipe.run(InMemoryLargeSourceSet(sources), ctx) if r._before is not r._after]
        self._expect_no_further_changes(
            changes, "Running the recipe on a fresh parse of its output changed", sanitized=True)

    def _group_by_kind(self, specs: List[SourceSpec]) -> Dict[str, List[SourceSpec]]:
        """Group source specs by their kind."""
        groups: Dict[str, List[SourceSpec]] = {}
        for spec in specs:
            if spec.kind not in groups:
                groups[spec.kind] = []
            groups[spec.kind].append(spec)
        return groups

    def _parse(
        self, specs: List[SourceSpec], workspace: "_Workspace"
    ) -> List[Tuple[SourceSpec, SourceFile]]:
        """Parse all specs using the appropriate parser."""
        result: List[Tuple[SourceSpec, SourceFile]] = []

        for spec in specs:
            if spec.before is None:
                # Generated file - skip parsing
                continue

            if spec.kind != "python":
                # Non-Python specs shape the workspace rather than being parsed:
                # a pyproject() is there to declare the project.
                continue

            parsed = self._parse_python(
                workspace.text(spec),
                workspace.source_path(spec),
                workspace.file_path(spec),
                workspace.ty_client,
            )

            # Reports the attribution this run produced, which is the one a
            # pattern written for this test has to match.
            from rewrite.python.type_report import dump_types_if_requested
            dump_types_if_requested(parsed, ty_status=workspace.ty_status)

            result.append((spec, parsed))

        return result

    @contextmanager
    def _workspace(self, specs: List[SourceSpec], texts: Dict[int, str]):
        """Hold every source in the run in one directory while it parses.

        ty reads the sources and the project metadata from there, so a declared
        Python version picks the typeshed and the sources resolve against each
        other. ``uv()`` supplies its dependency workspace through the specs;
        otherwise the run owns one. ``texts`` holds each source's contents by
        the ``id`` of its spec.
        """
        import shutil
        import tempfile
        from rewrite.python._version_detect import detect_from_project, ty_python_version

        paths = {id(spec): spec.path or Path(f"_{uuid4().hex}.{spec.ext}") for spec in specs}

        if not self.type_attribution:
            yield _Workspace(paths, texts, {}, None, "off, type_attribution=False")
            return

        root = next((spec.project_root for spec in specs if spec.project_root), None)
        owns_root = root is None
        if owns_root:
            root = tempfile.mkdtemp()

        written: Dict[int, str] = {}
        created: List[Path] = []
        ty_client = None
        ty_status = "on"
        try:
            for spec in specs:
                if id(spec) not in texts:
                    continue
                logical = paths[id(spec)]
                if not _in_workspace(logical):
                    # The run removes what it writes, so a path leading out of the
                    # workspace would write and delete on the caller's tree.
                    raise ValueError(f"a source path stays inside the workspace: {logical}")
                destination = Path(root) / logical
                created.extend(p for p in reversed(destination.parents) if not p.exists())
                if not destination.exists():
                    created.append(destination)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(texts[id(spec)])
                written[id(spec)] = str(destination)

            try:
                from rewrite.python.ty_client import TyTypesClient
                # handle_parse resolves this same version for a real parse.
                ty_client = TyTypesClient(python_version=ty_python_version(detect_from_project(root)))
                if not ty_client.initialize(root):
                    ty_status = f"off, ty could not initialize against {root}"
            except (ImportError, RuntimeError) as exc:
                ty_client = None
                ty_status = f"off, ty-types unavailable: {exc}"

            yield _Workspace(paths, texts, written, ty_client, ty_status)
        finally:
            if ty_client is not None:
                ty_client.shutdown()
            if owns_root:
                shutil.rmtree(root, ignore_errors=True)
            else:
                # A uv() workspace is cached across runs and holds the project's own
                # files — among them the pyproject.toml it was built from — so give
                # back what this run added and nothing else.
                for path in reversed(created):
                    try:
                        if path.is_dir():
                            path.rmdir()
                        else:
                            path.unlink()
                    except OSError:
                        pass

    def _parse_python(
        self,
        source: str,
        source_path: Path,
        file_path: Optional[str],
        ty_client: Optional[Any],
    ) -> CompilationUnit:
        """Parse Python source code into a CompilationUnit.

        ``source_path`` is the logical path the tree carries; ``file_path`` is where
        the source sits in the workspace, and is None for an untyped parse.
        """
        from rewrite.python._parser_visitor import ParserVisitor

        visitor = ParserVisitor(source, file_path, ty_client)
        # Strip BOM before passing to ast.parse (ParserVisitor does this internally)
        source_for_ast = source[1:] if source.startswith('\ufeff') else source
        tree = ast.parse(source_for_ast)
        cu = visitor.visit_Module(tree)
        return cu.replace(source_path=source_path)

    def _reparse(self, source_file: SourceFile) -> Optional[SourceFile]:
        """Parse what ``source_file`` prints, or return None for a source this harness cannot parse."""
        from rewrite.python.tree import CompilationUnit
        if not isinstance(source_file, CompilationUnit):
            return None
        return self._parse_python(_print(source_file, True), source_file.source_path, None, None)

    def _expect_no_parse_failures(
        self, parsed: List[Tuple[SourceSpec, SourceFile]]
    ) -> None:
        """Validate no parse errors occurred."""
        for spec, source_file in parsed:
            if isinstance(source_file, ParseError):
                marker = source_file.markers.find_first(ParseExceptionResult)
                msg = marker.message if marker else "Unknown parse error"
                raise AssertionError(f"Parse error in source: {msg}")

    def _expect_parse_print_idempotence(
        self, parsed: List[Tuple[SourceSpec, SourceFile]]
    ) -> None:
        """Validate that printing the AST produces the original source."""
        for spec, source_file in parsed:
            expected = dedent(spec.before)
            actual = source_file.print_all()
            assert actual == expected, (
                f"Parse/print not idempotent for {source_file.source_path}\n"
                f"Expected:\n{repr(expected)}\n\nActual:\n{repr(actual)}"
            )

    def _expect_results_to_match_after(
        self,
        specs: Tuple[SourceSpec, ...],
        parsed: List[Tuple[SourceSpec, SourceFile]],
        result_map: Dict[Any, Optional[SourceFile]],
        ty_status: str,
    ) -> None:
        """Validate recipe results match expected after states."""
        # Build a map from spec to parsed source file
        parsed_map = {id(spec): sf for spec, sf in parsed}

        for spec in specs:
            if spec.before is None:
                # A generated file, which `_expect_generated` checks: there is no parsed
                # source file here to key a result by.
                continue

            source_file = parsed_map.get(id(spec))
            if source_file is None:
                continue

            # Get the result (may be None if no change, or the changed file)
            after_sf = result_map.get(source_file.id)
            if after_sf is not None:
                assert_well_formed(after_sf)

            if spec.after is None:
                # No change expected
                if after_sf is not None:
                    actual = after_sf.print_all()
                    expected = dedent(spec.before)
                    if actual == expected:
                        if not self.allow_empty_diff:
                            raise AssertionError(
                                "An empty diff was generated. The recipe incorrectly "
                                "changed the AST without changing the printed output."
                            )
                    else:
                        raise AssertionError(
                            f"Expected no change but recipe modified the file.\n"
                            f"Before:\n{repr(expected)}\n\nAfter:\n{repr(actual)}"
                        )
            else:
                # Change expected
                if after_sf is None:
                    from rewrite.python.type_report import attribution_hint
                    before_sf = parsed_map.get(id(spec))
                    hint = "" if before_sf is None else attribution_hint(before_sf, ty_status=ty_status)
                    raise AssertionError(
                        f"Expected recipe to produce a change for:\n{dedent(spec.before)}{hint}"
                    )

                actual = after_sf.print_all()
                expected = self._resolve_after(spec.after, actual)

                assert actual == expected, (
                    f"Recipe output does not match expected.\n"
                    f"Expected:\n{repr(expected)}\n\nActual:\n{repr(actual)}"
                )

            # Call after_recipe hook if provided
            if spec.after_recipe:
                final_sf = after_sf if after_sf is not None else source_file
                spec.after_recipe(final_sf)

    def _match_generated(
        self, spec: SourceSpec, unclaimed: List[SourceFile],
    ) -> Optional[SourceFile]:
        """The generated file `spec` describes, or None when the recipe produced no match.

        Matched on the source path when the spec gives one, and otherwise on contents, which
        is what the Java test framework does and is the only thing left to match on.
        """
        if spec.path is not None:
            for source_file in unclaimed:
                if Path(str(source_file.source_path)) == Path(spec.path):
                    return source_file
            return None
        for source_file in unclaimed:
            actual = source_file.print_all()
            if actual == self._resolve_after(spec.after, actual):
                return source_file
        return None

    def _expect_generated(
        self,
        specs: Tuple[SourceSpec, ...],
        generated: List[SourceFile],
    ) -> None:
        """Check the files the recipe created against the specs that have no `before`."""
        unclaimed = list(generated)
        for spec in specs:
            if spec.before is not None:
                continue
            if spec.after is None:
                raise AssertionError(
                    "A source spec with neither a `before` nor an `after` describes nothing. "
                    "Pass the expected contents as `after`, and a `path` when the recipe "
                    "chooses where the file lands."
                )

            match = self._match_generated(spec, unclaimed)
            if match is None:
                raise AssertionError(
                    "Expected the recipe to generate a source file%s, and it generated:\n%s"
                    % ("" if spec.path is None else f" at {spec.path}",
                       _source_paths(generated))
                )
            unclaimed.remove(match)
            assert_well_formed(match)

            actual = match.print_all()
            expected = self._resolve_after(spec.after, actual)
            assert actual == expected, (
                f"Generated {match.source_path} does not match expected.\n"
                f"Expected:\n{repr(expected)}\n\nActual:\n{repr(actual)}"
            )
            if spec.after_recipe:
                spec.after_recipe(match)

        if unclaimed:
            raise AssertionError(
                "The recipe generated source files the test did not expect:\n%s"
                % _source_paths(unclaimed)
            )

    def _resolve_after(self, after: AfterRecipeText, actual: str) -> str:
        """Resolve the expected after value."""
        if callable(after):
            result = after(actual)  # ty: ignore[call-top-callable]
            if result is None:
                return actual  # Callable returned None = actual is acceptable
            return result
        return dedent(after) if after else actual


class _LaterCycleContext(DelegatingExecutionContext):
    """The context of a cycle after the first, which writes no data tables, as in Java."""

    def __init__(self, delegate: ExecutionContext):
        super().__init__(delegate)
        self._data_tables = None

    def get_message(self, key, default_value=None):
        if key == DATA_TABLE_STORE:
            return default_value if self._data_tables is None else self._data_tables
        return super().get_message(key, default_value)

    def put_message(self, key, value):
        if key == DATA_TABLE_STORE:
            self._data_tables = value
        else:
            super().put_message(key, value)


def _print(source_file: SourceFile, sanitized: bool = False) -> str:
    if not sanitized:
        return source_file.print_all()
    # Printed without search markers, which print as comments that are not Python.
    return source_file.print(Cursor(None, Cursor.ROOT_VALUE),
                             PrintOutputCapture(0, PrintOutputCapture.MarkerPrinter.SANITIZED))


def _in_workspace(path: Any) -> bool:
    path = Path(str(path))
    return not path.is_absolute() and ".." not in path.parts


def _plural_cycles(n: int) -> str:
    return f"{n} cycle" if n == 1 else f"{n} cycles"


def _source_paths(source_files: List[SourceFile]) -> str:
    if not source_files:
        return "    (nothing)"
    return "\n".join(f"    {source_file.source_path}" for source_file in source_files)


class _CompositeRecipe(Recipe):

    def __init__(self, recipes: List[Recipe]):
        self._recipes = recipes

    @property
    def name(self) -> str:
        return "org.openrewrite.composite"

    @property
    def display_name(self) -> str:
        return "Composite recipe"

    @property
    def description(self) -> str:
        return "Composite recipe for testing."

    def recipe_list(self) -> List[Recipe]:
        return self._recipes


def rewrite_run(*source_specs: SourceSpec, spec: Optional[RecipeSpec] = None) -> None:
    if spec is None:
        spec = RecipeSpec()
    spec.rewrite_run(*source_specs)
