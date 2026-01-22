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
    List,
    Optional,
    TYPE_CHECKING,
    TypeVar,
    Generic,
)

if TYPE_CHECKING:
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

        return cls(
            name=recipe.name,
            display_name=recipe.display_name,
            description=recipe.description,
            tags=recipe.tags,
            estimated_effort_per_occurrence=recipe.estimated_effort_per_occurrence,
            options=options,
            recipe_list=[cls.from_recipe(r) for r in recipe.recipe_list()],
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

        Args:
            before: The source files to transform
            ctx: The execution context

        Returns:
            List of results showing before/after for each changed file
        """
        from rewrite.visitor import Cursor

        lss = self._run_internal(before, ctx, Cursor(None, Cursor.ROOT_VALUE))
        return lss.get_changeset()

    def _run_internal(
        self, before: LargeSourceSet, ctx: ExecutionContext, root: Cursor
    ) -> LargeSourceSet:
        """
        Internal implementation of recipe execution.

        Applies this recipe's editor to each source file, then recursively
        applies any child recipes from recipe_list().
        """
        after = before.edit(lambda source: self.editor().visit(source, ctx, root))
        for recipe in self.recipe_list():
            after = recipe._run_internal(after, ctx, root)
        return after


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

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        """
        Internal implementation - delegates to scanner and editor_with_data.

        Do not override this method. Override scanner() and editor_with_data()
        instead.
        """
        # This will be set up by the recipe scheduler
        from rewrite.visitor import TreeVisitor

        return TreeVisitor.noop()
