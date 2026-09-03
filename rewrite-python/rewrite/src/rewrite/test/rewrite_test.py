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
from typing import Any, Dict, List, Optional, Tuple, TYPE_CHECKING
from uuid import uuid4

from rewrite import (
    ExecutionContext,
    InMemoryExecutionContext,
    InMemoryLargeSourceSet,
    Recipe,
    TreeVisitor,
)
from rewrite.execution import Result
from rewrite.parser import ParseError
from rewrite.markers import ParseExceptionResult

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
    written: Dict[int, str]
    ty_client: Optional[Any]

    def source_path(self, spec: SourceSpec) -> Path:
        return self.paths[id(spec)]

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

    # Whether to enable type attribution via ty-types.
    # Set to False for pure parse/print tests to avoid the ty-types subprocess overhead.
    type_attribution: bool = True

    def with_recipe(self, recipe: Recipe) -> "RecipeSpec":
        return replace(self, recipe=recipe)

    def with_allow_empty_diff(self, value: bool) -> "RecipeSpec":
        return replace(self, allow_empty_diff=value)

    def with_recipes(self, *recipes: Recipe) -> "RecipeSpec":
        if len(recipes) == 1:
            return self.with_recipe(recipes[0])
        return self.with_recipe(_CompositeRecipe(list(recipes)))

    def rewrite_run(self, *source_specs: SourceSpec) -> None:
        """
        Execute the recipe test with the given source specifications.

        This method:
        1. Groups specs by kind (language)
        2. Parses them using appropriate parsers
        3. Validates parse/print idempotence if enabled
        4. Runs the recipe
        5. Compares results with expected "after" states

        Args:
            *source_specs: Variable number of source specifications

        Raises:
            AssertionError: If any validation fails
        """
        specs = list(source_specs)
        specs_by_kind = self._group_by_kind(specs)

        # Parse and validate all source files
        all_parsed: List[Tuple[SourceSpec, SourceFile]] = []
        with self._workspace(specs) as workspace:
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

        # Run the recipe
        source_files = [sf for _, sf in all_parsed]
        lss = InMemoryLargeSourceSet(source_files)
        results = self.recipe.run(lss, self.execution_context)

        # Build result map: source_file.id -> after_source_file
        result_map: Dict[Any, Optional[SourceFile]] = {}
        for result in results:
            if result._before is not None:
                # Check if there was a change
                if result._after is not None and result._after is not result._before:
                    result_map[result._before.id] = result._after
                else:
                    result_map[result._before.id] = None

        # Validate results match expected after states
        self._expect_results_to_match_after(source_specs, all_parsed, result_map)

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
                dedent(spec.before),
                workspace.source_path(spec),
                workspace.file_path(spec),
                workspace.ty_client,
            )

            result.append((spec, parsed))

        return result

    @contextmanager
    def _workspace(self, specs: List[SourceSpec]):
        """Hold every source in the run in one directory while it parses.

        ty reads the sources and the project metadata from there, so a declared
        Python version picks the typeshed and the sources resolve against each
        other. ``uv()`` supplies its dependency workspace through the specs;
        otherwise the run owns one.
        """
        import shutil
        import tempfile
        from rewrite.python._version_detect import detect_from_project, ty_python_version

        paths = {id(spec): spec.path or Path(f"_{uuid4().hex}.{spec.ext}") for spec in specs}

        if not self.type_attribution:
            yield _Workspace(paths, {}, None)
            return

        root = next((spec.project_root for spec in specs if spec.project_root), None)
        owns_root = root is None
        if owns_root:
            root = tempfile.mkdtemp()

        written: Dict[int, str] = {}
        created: List[Path] = []
        ty_client = None
        try:
            for spec in specs:
                if spec.before is None:
                    continue
                logical = paths[id(spec)]
                if logical.is_absolute() or ".." in logical.parts:
                    # The run removes what it writes, so a path leading out of the
                    # workspace would write and delete on the caller's tree.
                    raise ValueError(f"a source path stays inside the workspace: {logical}")
                destination = Path(root) / logical
                created.extend(p for p in reversed(destination.parents) if not p.exists())
                if not destination.exists():
                    created.append(destination)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(dedent(spec.before))
                written[id(spec)] = str(destination)

            try:
                from rewrite.python.ty_client import TyTypesClient
                # handle_parse resolves this same version for a real parse.
                ty_client = TyTypesClient(python_version=ty_python_version(detect_from_project(root)))
                ty_client.initialize(root)
            except (ImportError, RuntimeError):
                ty_client = None

            yield _Workspace(paths, written, ty_client)
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
    ) -> None:
        """Validate recipe results match expected after states."""
        # Build a map from spec to parsed source file
        parsed_map = {id(spec): sf for spec, sf in parsed}

        for spec in specs:
            if spec.before is None:
                # Generated file case - not implemented yet
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
                    raise AssertionError(
                        f"Expected recipe to produce a change for:\n{dedent(spec.before)}"
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

    def _resolve_after(self, after: AfterRecipeText, actual: str) -> str:
        """Resolve the expected after value."""
        if callable(after):
            result = after(actual)  # ty: ignore[call-top-callable]
            if result is None:
                return actual  # Callable returned None = actual is acceptable
            return result
        return dedent(after) if after else actual


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
