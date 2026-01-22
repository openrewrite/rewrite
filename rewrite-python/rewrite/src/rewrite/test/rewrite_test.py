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
from dataclasses import dataclass, field
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
        # Group specs by kind
        specs_by_kind = self._group_by_kind(list(source_specs))

        # Parse and validate all source files
        all_parsed: List[Tuple[SourceSpec, SourceFile]] = []
        for kind, specs in specs_by_kind.items():
            parsed = self._parse(specs)
            self._expect_no_parse_failures(parsed)
            if self.check_parse_print_idempotence:
                self._expect_parse_print_idempotence(parsed)
            all_parsed.extend(parsed)

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

    def _parse(self, specs: List[SourceSpec]) -> List[Tuple[SourceSpec, SourceFile]]:
        """Parse all specs using the appropriate parser."""
        result: List[Tuple[SourceSpec, SourceFile]] = []

        for spec in specs:
            if spec.before is None:
                # Generated file - skip parsing
                continue

            # Determine source path
            source_path = spec.path or Path(f"{uuid4().hex}.{spec.ext}")

            # Parse the source
            source = dedent(spec.before)
            parsed = self._parse_python(source, source_path)

            # Call before_recipe hook if provided
            if spec.before_recipe:
                modified = spec.before_recipe(parsed)
                if modified is not None:
                    parsed = modified

            result.append((spec, parsed))

        return result

    def _parse_python(self, source: str, source_path: Path) -> CompilationUnit:
        """Parse Python source code into a CompilationUnit."""
        from rewrite.python._parser_visitor import ParserVisitor

        visitor = ParserVisitor(source)
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

            if spec.after is None:
                # No change expected
                if after_sf is not None:
                    actual = after_sf.print_all()
                    expected = dedent(spec.before)
                    assert actual == expected, (
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
            result = after(actual)
            if result is None:
                return actual  # Callable returned None = actual is acceptable
            return result
        return dedent(after) if after else actual
