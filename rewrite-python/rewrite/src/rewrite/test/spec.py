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
from typing import Callable, Optional, Union, TYPE_CHECKING

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
    """

    kind: str
    before: Optional[str]
    after: AfterRecipeText = None
    path: Optional[Path] = None
    before_recipe: Optional[Callable[["SourceFile"], Optional["SourceFile"]]] = None
    after_recipe: Optional[Callable[["SourceFile"], None]] = None
    ext: str = "py"


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

    return textwrap.dedent(s)


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
