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

"""Source specification types and helpers for the Python test harness."""

from __future__ import annotations

import textwrap
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, List, Optional, Union, TYPE_CHECKING

if TYPE_CHECKING:
    from rewrite.tree import SourceFile

# Type alias for the "after" field:
# - None: No change expected (recipe should not modify the file)
# - str: Expected source after transformation
# - Callable[[str], str | None]: Function that receives actual output and returns expected
#   (returning None means the actual output is acceptable as-is)
AfterRecipeText = Union[str, Callable[[str], Optional[str]], None]


@dataclass
class SourceSpec:
    """
    Specification for a source file in a rewrite test.

    Defines the before state, expected after state, and optional hooks
    for a single source file being tested.

    Attributes:
        kind: Language identifier (e.g., "python")
        before: Source code before the recipe runs. None for generated files.
        after: Expected source after transformation:
            - None: No change expected
            - str: Expected output
            - Callable: Function receiving actual output, returning expected
        path: Optional file path (auto-generated if not provided)
        before_recipe: Hook called after parsing but before recipe runs
        after_recipe: Hook called after recipe runs
        ext: File extension for auto-generated paths
        project_root: Optional workspace directory for type attribution
            (set by ``uv()`` — not normally used directly).
    """

    kind: str
    before: Optional[str]
    after: AfterRecipeText = None
    path: Optional[Path] = None
    before_recipe: Optional[Callable[["SourceFile"], Optional["SourceFile"]]] = None
    after_recipe: Optional[Callable[["SourceFile"], None]] = None
    ext: str = "py"
    project_root: Optional[str] = None


def dedent(s: Optional[str]) -> str:
    """
    Remove common leading whitespace from a string.

    Behavior:
    - Removes ONE leading newline if present (for template string ergonomics)
    - Preserves trailing newlines
    - Removes common indentation from all lines

    This allows tests to use cleanly indented triple-quoted strings:

        python(
            '''
            def foo():
                pass
            '''
        )

    Instead of awkward formatting:

        python(
            '''def foo():
        pass
'''
        )

    Args:
        s: The string to dedent

    Returns:
        The dedented string, or empty string if input is None/empty
    """
    if not s:
        return ""

    # Remove single leading newline for ergonomics
    if s.startswith("\n"):
        s = s[1:]

    if not s:
        return ""

    # Preserve \r\n sequences through dedent: replace \r\n with a marker
    # that keeps the \n (so line boundaries survive for textwrap.dedent)
    _CR = '\x00CR\x00'
    s = s.replace('\r\n', _CR + '\n')
    s = textwrap.dedent(s)
    return s.replace(_CR + '\n', '\r\n')


def uv(
    *source_specs: SourceSpec,
    root: Optional[str] = None,
) -> List[SourceSpec]:
    """Set up a Python workspace with dependencies for type attribution.

    This is the Python equivalent of the Java ``Assertions.uv()`` and the
    JavaScript ``npm()`` test helpers.  It finds a ``pyproject()`` spec
    among the given source specs, creates (or reuses) a cached workspace
    with the declared PyPI dependencies installed, and tags every spec
    with the workspace path so ``TyTypesClient`` can resolve types
    during parsing.

    Args:
        *source_specs: Source specifications, typically including a
            ``pyproject(...)`` spec and one or more ``python(...)`` specs.
        root: Optional workspace directory.  When omitted the workspace
            is created/cached automatically from the ``pyproject.toml``
            content.

    Returns:
        The source specs with ``project_root`` set on each.

    Example::

        spec = RecipeSpec(recipe=MyRecipe())
        spec.rewrite_run(
            *uv(
                pyproject('''
                [project]
                name = "test"
                version = "0.0.0"
                requires-python = ">=3.10"
                dependencies = ["requests==2.31.0"]
                '''),
                python("import requests", ...),
            )
        )
    """
    from rewrite.python.template.dependency_workspace import DependencyWorkspace

    workspace = root
    if workspace is None:
        # Find pyproject.toml content from source specs
        for spec in source_specs:
            if spec.kind == "toml" and spec.path and spec.path.name == "pyproject.toml":
                if spec.before is not None:
                    workspace = DependencyWorkspace.get_or_create_from_pyproject(
                        dedent(spec.before)
                    )
                    break

    if workspace is None:
        raise ValueError(
            "uv() requires either a pyproject() spec or an explicit root= argument"
        )

    # Tag every spec with the workspace
    return [
        SourceSpec(
            kind=s.kind,
            before=s.before,
            after=s.after,
            path=s.path,
            before_recipe=s.before_recipe,
            after_recipe=s.after_recipe,
            ext=s.ext,
            project_root=workspace,
        )
        for s in source_specs
    ]


def pyproject(
    before: Optional[str],
    after: AfterRecipeText = None,
) -> SourceSpec:
    """Create a SourceSpec for a ``pyproject.toml`` file.

    Used inside ``uv()`` to declare the project's dependencies::

        uv(
            pyproject('''
            [project]
            name = "test"
            version = "0.0.0"
            dependencies = ["requests==2.31.0"]
            '''),
            python("import requests"),
        )

    Args:
        before: ``pyproject.toml`` content before transformation.
        after: Expected content after transformation (None = no change).

    Returns:
        A SourceSpec configured for TOML with path ``pyproject.toml``.
    """
    return SourceSpec(
        kind="toml",
        before=before,
        after=after,
        path=Path("pyproject.toml"),
        ext="toml",
    )


def python(
    before: Optional[str],
    after: AfterRecipeText = None,
    *,
    path: Optional[Path] = None,
    before_recipe: Optional[Callable[["SourceFile"], Optional["SourceFile"]]] = None,
    after_recipe: Optional[Callable[["SourceFile"], None]] = None,
) -> SourceSpec:
    """
    Create a SourceSpec for Python source code.

    This is the primary helper function for creating test specifications
    for Python source files.

    Args:
        before: Source code before transformation (None for generated files)
        after: Expected source after transformation:
            - None: No change expected
            - str: Expected output
            - Callable: Function receiving actual, returning expected
        path: Optional file path (auto-generated if not provided)
        before_recipe: Hook called after parsing, before recipe runs.
            Can modify the parsed AST by returning a new SourceFile.
        after_recipe: Hook called after recipe runs to validate results.

    Returns:
        A SourceSpec configured for Python

    Example:
        # Test a recipe that removes unused imports
        python(
            '''
            import os
            import sys
            print("hello")
            ''',
            '''
            import sys
            print("hello")
            '''
        )

        # Test that no changes are made
        python(
            '''
            import sys
            '''
            # No after = no change expected
        )

        # Use a callable for dynamic assertions
        python(
            '''
            x = 1
            ''',
            lambda actual: actual if "x" in actual else None
        )
    """
    return SourceSpec(
        kind="python",
        before=before,
        after=after,
        path=path,
        before_recipe=before_recipe,
        after_recipe=after_recipe,
        ext="py",
    )
