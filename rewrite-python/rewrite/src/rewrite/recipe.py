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

"""Recipe base classes and option metadata."""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, fields, is_dataclass
from typing import (
    Any,
    Callable,
    Dict,
    List,
    Optional,
    TYPE_CHECKING,
    TypeVar,
    Generic,
    cast,
)

if TYPE_CHECKING:
    from rewrite.data_table import DataTable
    from rewrite.visitor import TreeVisitor, Cursor
    from rewrite.execution import ExecutionContext, LargeSourceSet, Result
    from rewrite.tree import SourceFile


def option(
    display_name: str,
    description: str,
    example: Optional[str] = None,
    required: bool = True,
    valid: Optional[List[str]] = None,
) -> dict[str, Any]:
    """
    Create option metadata for a recipe field.

    Use this with dataclasses.field(metadata=option(...)) to define
    recipe options that will be exposed in the marketplace.

    Args:
        display_name: Human-readable name for the option
        description: Description of what the option does (supports markdown)
        example: Example value for the option
        required: Whether the option is required (default True)
        valid: List of valid values (for enum-like options)

    Returns:
        Metadata dictionary to pass to dataclasses.field()

    Example:
        @dataclass
        class MyRecipe(Recipe):
            pattern: str = field(metadata=option(
                display_name="Pattern",
                description="The pattern to search for",
                example="*.py"
            ))
    """
    return {
        "option": OptionDescriptor(
            display_name=display_name,
            description=description,
            example=example,
            required=required,
            valid=valid,
        )
    }


@dataclass(frozen=True)
class OptionDescriptor:
    """Descriptor for a recipe option."""

    display_name: str
    description: str
    example: Optional[str] = None
    required: bool = True
    valid: Optional[List[str]] = None


@dataclass(frozen=True)
class RecipeDescriptor:
    """
    Descriptor for a recipe, used for marketplace display.

    Contains all the metadata needed to display a recipe in the
    marketplace and configure it for execution.
    """

    name: str
    display_name: str
    description: str
    tags: List[str]
    estimated_effort_per_occurrence: int
    options: List[tuple[str, Any, OptionDescriptor]]
    data_tables: List[dict]
    recipe_list: List[RecipeDescriptor]

    @classmethod
    def from_recipe(cls, recipe: Recipe) -> RecipeDescriptor:
        """Create a descriptor from a recipe instance."""
        options: List[tuple[str, Any, OptionDescriptor]] = []

        # Extract options from dataclass fields
        if is_dataclass(recipe) and not isinstance(recipe, type):
            for f in fields(recipe):
                if "option" in f.metadata:
                    descriptor = f.metadata["option"]
                    value = getattr(recipe, f.name)
                    options.append((f.name, value, descriptor))

        recipe_list = [cls.from_recipe(r) for r in recipe.recipe_list()]

        # A recipe reports the union of the data tables it owns and every table its
        # children produce, so consumers that read only the top-level descriptor --
        # the marketplace listing and everything derived from it -- can resolve a
        # composite's tables. Mirrors Java's Recipe#aggregateDataTableDescriptors.
        # Sub-descriptors are built the same way, so the union is recursive; the
        # recipe's own tables come first and the first descriptor for a given name
        # wins.
        data_tables: Dict[str, dict] = {}
        for data_table in recipe.data_tables:
            data_tables.setdefault(data_table.name, data_table.descriptor())
        for sub_recipe in recipe_list:
            for descriptor in sub_recipe.data_tables:
                data_tables.setdefault(descriptor["name"], descriptor)

        return cls(
            name=recipe.name,
            display_name=recipe.display_name,
            description=recipe.description,
            tags=recipe.tags,
            estimated_effort_per_occurrence=recipe.estimated_effort_per_occurrence,
            options=options,
            data_tables=list(data_tables.values()),
            recipe_list=recipe_list,
        )


class Recipe(ABC):
    """
    Base class for all recipes.

    A recipe defines a transformation that can be applied to source code.
    Recipes can be simple (a single visitor) or composite (containing
    other recipes).

    To create a recipe:
    1. Subclass Recipe (or use @dataclass for recipes with options)
    2. Implement the abstract properties (name, display_name, description)
    3. Override editor() to return your transformation visitor

    Example:
        @dataclass
        class ChangeImport(Recipe):
            old_module: str = field(metadata=option(
                display_name="Old module",
                description="The module to change imports from",
                example="flask"
            ))
            new_module: str = field(metadata=option(
                display_name="New module",
                description="The module to change imports to",
                example="flask_restful"
            ))

            @property
            def name(self) -> str:
                return "org.openrewrite.python.ChangeImport"

            @property
            def display_name(self) -> str:
                return "Change import"

            @property
            def description(self) -> str:
                return "Changes an import from one module to another."

            def editor(self) -> TreeVisitor:
                # Return visitor that performs transformation
                ...
    """

    @property
    @abstractmethod
    def name(self) -> str:
        """
        Fully qualified recipe name.

        Should be a dot-separated identifier like
        'org.openrewrite.python.cleanup.RemoveUnusedImports'.
        """
        ...

    @property
    @abstractmethod
    def display_name(self) -> str:
        """
        Human-readable display name for the recipe.

        Should be initial-capped with no period at the end.
        Example: "Remove unused imports"
        """
        ...

    @property
    @abstractmethod
    def description(self) -> str:
        """
        Full description of what the recipe does.

        Supports markdown formatting. Should be one or more complete
        sentences ending with a period.
        """
        ...

    @property
    def tags(self) -> List[str]:
        """Tags for categorizing this recipe."""
        return []

    @property
    def estimated_effort_per_occurrence(self) -> int:
        """Estimated minutes to perform this change manually."""
        return 5

    @property
    def data_tables(self) -> List[DataTable]:
        """
        Return data tables this recipe produces.

        Override this method to declare what data tables your recipe
        will populate during execution. Data tables allow recipes to
        output structured data that can be displayed in the UI or
        exported to CSV files.

        Returns:
            List of DataTable instances this recipe produces
        """
        return []

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        """
        Return the visitor that performs the transformation.

        Override this method to provide your recipe's transformation logic.
        """
        from rewrite.visitor import TreeVisitor

        return TreeVisitor.noop()

    def recipe_list(self) -> List[Recipe]:
        """
        Return child recipes for composite recipes.

        Override this for recipes that combine multiple transformations.
        """
        return []

    def descriptor(self) -> RecipeDescriptor:
        """Get the recipe descriptor for marketplace display."""
        return RecipeDescriptor.from_recipe(self)

    def run(self, before: LargeSourceSet, ctx: ExecutionContext) -> List[Result]:
        """
        Run this recipe on a set of source files.

        Executes the scan/generate/edit lifecycle: every source file is
        scanned by every ScanningRecipe in the recipe tree, then new files
        are generated, then all files (including generated ones) are edited.

        Args:
            before: The source files to transform
            ctx: The execution context

        Returns:
            List of results showing before/after for each changed file
        """
        from rewrite.visitor import Cursor

        cursor = Cursor(None, Cursor.ROOT_VALUE)
        recipe_lists: _RecipeLists = {}

        after = before
        if _has_scanning_recipe(self, recipe_lists):
            # Phase 1: scan each file. Scanning must not modify the tree, so the
            # visit result is discarded and the original file is passed on to the
            # next recipe in the list. Returning None here would abort traversal
            # of the remainder of the recipe list, leaving nested scanners unrun.
            def scan_one(recipe: Recipe, source: SourceFile) -> SourceFile:
                if isinstance(recipe, ScanningRecipe):
                    recipe.scanner(recipe.accumulator(cursor, ctx)).visit(source, ctx, cursor)
                return source

            # edit() is used only to iterate the source files; scan_one returns
            # each file unchanged, so the returned source set is the same
            after.edit(lambda source: _recurse_recipe_list(self, source, recipe_lists, scan_one))

            # Phase 2: collect generated files
            def generate_one(recipe: Recipe, generated: List[SourceFile]) -> List[SourceFile]:
                if isinstance(recipe, ScanningRecipe):
                    generated.extend(recipe.generate(recipe.accumulator(cursor, ctx), ctx))
                return generated

            after = after.generate(_recurse_recipe_list(self, [], recipe_lists, generate_one))

        # Phase 3: edit all files, including generated ones. An editor returning
        # None deletes the file and stops later recipes from visiting it.
        def edit_one(recipe: Recipe, source: SourceFile) -> Optional[SourceFile]:
            return recipe.editor().visit(source, ctx, cursor)

        after = after.edit(lambda source: _recurse_recipe_list(self, source, recipe_lists, edit_one))
        return after.get_changeset()


T = TypeVar("T")


class ScanningRecipe(Recipe, Generic[T], ABC):
    """
    A recipe that scans all source files before making changes.

    Scanning recipes have two phases:
    1. Scan phase: Accumulate data across all source files
    2. Edit phase: Apply transformations using the accumulated data

    This is useful for recipes that need global information, like
    finding all usages of a method before deciding which to change.

    Example:
        @dataclass
        class RemoveUnusedMethods(ScanningRecipe[set[str]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.python.RemoveUnusedMethods"

            def initial_value(self, ctx: ExecutionContext) -> set[str]:
                return set()

            def scanner(self, acc: set[str]) -> TreeVisitor:
                # Return visitor that collects all method calls
                ...

            def editor_with_data(self, acc: set[str]) -> TreeVisitor:
                # Return visitor that removes methods not in acc
                ...
    """

    @abstractmethod
    def initial_value(self, ctx: ExecutionContext) -> T:
        """Create the initial accumulator value."""
        ...

    def scanner(self, acc: T) -> TreeVisitor[Any, ExecutionContext]:
        """
        Return the visitor for the scan phase.

        This visitor should collect data into the accumulator without
        making any changes to the source files.
        """
        from rewrite.visitor import TreeVisitor

        return TreeVisitor.noop()

    def editor_with_data(self, acc: T) -> TreeVisitor[Any, ExecutionContext]:
        """
        Return the visitor for the edit phase.

        This visitor can use the accumulated data to make informed
        transformations.
        """
        from rewrite.visitor import TreeVisitor

        return TreeVisitor.noop()

    def generate(self, acc: T, ctx: ExecutionContext) -> List[SourceFile]:
        """
        Generate new source files based on accumulated data.

        Override to create new files as part of the recipe.
        """
        return []

    def accumulator(self, cursor: Cursor, ctx: ExecutionContext) -> T:
        """
        Get this recipe instance's accumulator, creating it on first access.

        The accumulator is stored in the root cursor's messages under a
        per-instance key, so all phases of a run share it while separate runs
        (each with a fresh root cursor) start from initial_value().
        """
        root = cursor.root
        key = f"org.openrewrite.recipe.acc.{id(self)}"
        if root.messages is not None and key in root.messages:
            return cast(T, root.messages[key])
        acc = self.initial_value(ctx)
        root.put_message(key, acc)
        return acc

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        """
        Internal implementation - delegates to editor_with_data() with the
        current run's accumulator.

        Do not override this method. Override scanner() and editor_with_data()
        instead.
        """
        from rewrite.visitor import TreeVisitor

        recipe = self

        class _AccumulatorEditor(TreeVisitor):
            _delegate: Optional[TreeVisitor[Any, ExecutionContext]] = None

            def visit(self, tree, p, parent=None):
                if self._delegate is None:
                    cursor = parent if parent is not None else self._cursor
                    self._delegate = recipe.editor_with_data(recipe.accumulator(cursor, p))
                return self._delegate.visit(tree, p, parent)

        return _AccumulatorEditor()


# Recipes commonly instantiate their sub-recipes inside recipe_list(), so calling it more
# than once yields different instances. A scanning sub-recipe would then hold a different
# accumulator in every phase and for every source file. Resolving each recipe list once per
# run keeps sub-recipe identity, and therefore accumulator identity, stable. Keyed by id()
# rather than by recipe (dataclass-based recipes are unhashable); every keyed recipe stays
# referenced through the cached lists (the root through the caller), so ids cannot be
# recycled during the run.
_RecipeLists = dict[int, list[Recipe]]


def _sub_recipes(recipe: Recipe, recipe_lists: _RecipeLists) -> List[Recipe]:
    resolved = recipe_lists.get(id(recipe))
    if resolved is None:
        resolved = recipe.recipe_list()
        recipe_lists[id(recipe)] = resolved
    return resolved


def _has_scanning_recipe(recipe: Recipe, recipe_lists: _RecipeLists) -> bool:
    if isinstance(recipe, ScanningRecipe):
        return True
    return any(_has_scanning_recipe(r, recipe_lists) for r in _sub_recipes(recipe, recipe_lists))


def _recurse_recipe_list(
    recipe: Recipe,
    initial: T,
    recipe_lists: _RecipeLists,
    fn: Callable[[Recipe, T], Optional[T]],
) -> Optional[T]:
    """Apply fn to every recipe in the tree in pre-order, threading the value through.

    A None result short-circuits the remaining traversal (a deleted file
    stops being visited).
    """
    t = fn(recipe, initial)
    for sub_recipe in _sub_recipes(recipe, recipe_lists):
        if t is None:
            return None
        t = _recurse_recipe_list(sub_recipe, t, recipe_lists, fn)
    return t
