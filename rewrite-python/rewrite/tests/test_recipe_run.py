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

"""Tests for the native recipe scheduler in ``Recipe.run``.

Ports the scan/generate/edit lifecycle test suite from
``rewrite-javascript/rewrite/test/run.test.ts``.
"""

import ast
from pathlib import Path
from typing import Any, Dict, List

from rewrite import (
    ExecutionContext,
    InMemoryExecutionContext,
    InMemoryLargeSourceSet,
    Recipe,
    ScanningRecipe,
    TreeVisitor,
)
from rewrite.java import tree as j
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


class Composite(Recipe):
    """A plain composite recipe that only contributes a recipe list."""

    def __init__(self, children: List[Recipe]):
        self._children = children

    @property
    def name(self) -> str:
        return "org.openrewrite.test.composite"

    @property
    def display_name(self) -> str:
        return "Composite"

    @property
    def description(self) -> str:
        return "A plain composite recipe that only contributes a recipe list."

    def recipe_list(self) -> List[Recipe]:
        return self._children


class ScanningEditor(ScanningRecipe[Dict[str, int]]):
    """Counts source files during scanning; appends the count to each string literal during editing."""

    @property
    def name(self) -> str:
        return "org.openrewrite.test.scanning-editor"

    @property
    def display_name(self) -> str:
        return "Scanning editor"

    @property
    def description(self) -> str:
        return "Appends the count of source files to each string literal."

    def initial_value(self, ctx: ExecutionContext) -> Dict[str, int]:
        return {"count": 0}

    def scanner(self, acc: Dict[str, int]) -> TreeVisitor[Any, ExecutionContext]:
        class _Scanner(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu, p):
                acc["count"] += 1
                return cu

        return _Scanner()

    def editor_with_data(self, acc: Dict[str, int]) -> TreeVisitor[Any, ExecutionContext]:
        return _AppendToLiteral(f" (count: {acc['count']})")


class _AppendToLiteral(PythonVisitor[ExecutionContext]):
    def __init__(self, suffix: str):
        self._suffix = suffix

    def visit_literal(self, literal: j.Literal, p: ExecutionContext) -> j.J:
        if isinstance(literal.value, str) and literal.value_source:
            new_value = literal.value + self._suffix
            quote = literal.value_source[0]
            return literal.replace(value=new_value, value_source=f"{quote}{new_value}{quote}")
        return literal


class InliningComposite(Recipe):
    """A plain composite that instantiates its sub-recipes inside recipe_list()."""

    @property
    def name(self) -> str:
        return "org.openrewrite.test.inlining-composite"

    @property
    def display_name(self) -> str:
        return "Inlining composite"

    @property
    def description(self) -> str:
        return "A plain composite that instantiates its sub-recipes inside recipe_list()."

    def recipe_list(self) -> List[Recipe]:
        return [ScanningEditor()]


class Exclaim(Recipe):
    """Appends an exclamation mark to each string literal."""

    @property
    def name(self) -> str:
        return "org.openrewrite.test.exclaim"

    @property
    def display_name(self) -> str:
        return "Exclaim"

    @property
    def description(self) -> str:
        return "Appends an exclamation mark to each string literal."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        return _AppendToLiteral("!")


class DeleteFile(Recipe):
    """Deletes every source file it visits."""

    @property
    def name(self) -> str:
        return "org.openrewrite.test.delete-file"

    @property
    def display_name(self) -> str:
        return "Delete file"

    @property
    def description(self) -> str:
        return "Deletes every source file it visits."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class _Deleter(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu, p):
                return None

        return _Deleter()


def _parse(*sources: str) -> List[CompilationUnit]:
    parsed = []
    for i, source in enumerate(sources):
        cu = ParserVisitor(source, None, None).visit(ast.parse(source))
        parsed.append(cu.replace(source_path=Path(f"file{i}.py")))
    return parsed


class TestScanningPhaseTraversal:
    def test_scanning_recipe_one_level_under_a_plain_composite(self):
        spec = RecipeSpec(recipe=Composite([ScanningEditor()]), type_attribution=False)
        spec.rewrite_run(
            python('s = "hello"', 's = "hello (count: 1)"')
        )

    def test_scanning_recipe_several_levels_deep(self):
        spec = RecipeSpec(
            recipe=Composite([Composite([Composite([ScanningEditor()])])]),
            type_attribution=False,
        )
        spec.rewrite_run(
            python('s = "hello"', 's = "hello (count: 1)"')
        )

    def test_scanning_recipe_positioned_after_a_plain_sibling(self):
        # The root is itself a scanning recipe, so traversal starts out fine. The plain
        # sibling in front of the scanning recipe must not abort the recipe list.
        class ScanningRoot(ScanningRecipe[Dict[str, int]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.test.scanning-root"

            @property
            def display_name(self) -> str:
                return "Scanning root"

            @property
            def description(self) -> str:
                return "A scanning recipe that contributes a recipe list."

            def initial_value(self, ctx: ExecutionContext) -> Dict[str, int]:
                return {}

            def recipe_list(self) -> List[Recipe]:
                return [Composite([]), ScanningEditor()]

        spec = RecipeSpec(recipe=ScanningRoot(), type_attribution=False)
        spec.rewrite_run(
            python('s = "hello"', 's = "hello (count: 1)"')
        )

    def test_scanning_recipe_instantiated_inside_recipe_list(self):
        # The recipe list is resolved once per run, so the scan phase and the edit phase
        # see the same sub-recipe instance and therefore the same accumulator.
        spec = RecipeSpec(recipe=InliningComposite(), type_attribution=False)
        spec.rewrite_run(
            python('s = "alpha"', 's = "alpha (count: 2)"'),
            python('s = "beta"', 's = "beta (count: 2)"'),
        )

    def test_accumulator_sees_every_file_before_any_file_is_edited(self):
        spec = RecipeSpec(recipe=Composite([ScanningEditor()]), type_attribution=False)
        spec.rewrite_run(
            python('s = "alpha"', 's = "alpha (count: 3)"'),
            python('s = "beta"', 's = "beta (count: 3)"'),
            python('s = "gamma"', 's = "gamma (count: 3)"'),
        )

    def test_scanner_returning_none_does_not_halt_the_run(self):
        # Scanning must not modify the tree, so the visit result is discarded entirely,
        # matching the Java RecipeRunCycle, which keeps the original source file.
        class DeletingScanner(ScanningRecipe[Dict[str, int]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.test.deleting-scanner"

            @property
            def display_name(self) -> str:
                return "Deleting scanner"

            @property
            def description(self) -> str:
                return "A scanner whose visitor returns None."

            def initial_value(self, ctx: ExecutionContext) -> Dict[str, int]:
                return {}

            def scanner(self, acc: Dict[str, int]) -> TreeVisitor[Any, ExecutionContext]:
                class _Deleter(PythonVisitor[ExecutionContext]):
                    def visit_compilation_unit(self, cu, p):
                        return None

                return _Deleter()

        spec = RecipeSpec(
            recipe=Composite([DeletingScanner(), ScanningEditor()]),
            type_attribution=False,
        )
        spec.rewrite_run(
            python('s = "hello"', 's = "hello (count: 1)"')
        )


class TestGeneratePhase:
    def test_generated_file_is_edited_by_later_recipes(self):
        class Generating(ScanningRecipe[Dict[str, int]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.test.generating"

            @property
            def display_name(self) -> str:
                return "Generating"

            @property
            def description(self) -> str:
                return "Generates a new source file."

            def initial_value(self, ctx: ExecutionContext) -> Dict[str, int]:
                return {}

            def generate(self, acc, ctx):
                return _parse('s = "generated"')

        before = _parse("x = 1")
        results = Composite([Generating(), Exclaim()]).run(
            InMemoryLargeSourceSet(before), InMemoryExecutionContext()
        )

        generated = [r for r in results if r._before is None]
        assert len(generated) == 1
        assert generated[0]._after.print_all() == 's = "generated!"'


class TestEditPhaseShortCircuit:
    def test_deleted_file_stops_being_visited(self):
        before = _parse('s = "hello"')
        results = Composite([DeleteFile(), Exclaim()]).run(
            InMemoryLargeSourceSet(before), InMemoryExecutionContext()
        )

        assert len(results) == 1
        assert results[0]._before is before[0]
        assert results[0]._after is None

    def test_deleted_file_stops_being_visited_when_the_run_also_scans(self):
        before = _parse('s = "hello"')
        results = Composite([ScanningEditor(), DeleteFile(), Exclaim()]).run(
            InMemoryLargeSourceSet(before), InMemoryExecutionContext()
        )

        assert len(results) == 1
        assert results[0]._before is before[0]
        assert results[0]._after is None


class TestMultiFileEdit:
    def test_edit_of_earlier_file_survives_unchanged_later_file(self):
        # The second file contains no string literal, so only the first is edited.
        before = _parse('s = "hello"', "x = 1")
        results = Exclaim().run(
            InMemoryLargeSourceSet(before), InMemoryExecutionContext()
        )

        changed = [r for r in results if r._after is not None and r._after is not r._before]
        assert len(changed) == 1
        assert changed[0]._after.print_all() == 's = "hello!"'
