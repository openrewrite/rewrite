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

"""Placeholder utilities for template parsing."""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from .capture import Capture

# Placeholder identifier format: __placeholder_name__
PLACEHOLDER_PREFIX = "__placeholder_"
PLACEHOLDER_SUFFIX = "__"

# Regex to find {name} placeholders in template code
# Matches: {name} or {name:type}
PLACEHOLDER_PATTERN = re.compile(r'\{([a-zA-Z_][a-zA-Z0-9_]*)(?::([^}]+))?\}')


def to_placeholder(name: str) -> str:
    """
    Convert a capture name to a placeholder identifier.

    Args:
        name: The capture name.

    Returns:
        A placeholder identifier string.

    Examples:
        >>> to_placeholder('expr')
        '__placeholder_expr__'
        >>> to_placeholder('x')
        '__placeholder_x__'
    """
    return f"{PLACEHOLDER_PREFIX}{name}{PLACEHOLDER_SUFFIX}"


def from_placeholder(identifier: str) -> Optional[str]:
    """
    Extract the capture name from a placeholder identifier.

    Args:
        identifier: A potential placeholder identifier.

    Returns:
        The capture name if this is a placeholder, None otherwise.

    Examples:
        >>> from_placeholder('__placeholder_expr__')
        'expr'
        >>> from_placeholder('regular_var')
        None
    """
    if identifier.startswith(PLACEHOLDER_PREFIX) and identifier.endswith(PLACEHOLDER_SUFFIX):
        return identifier[len(PLACEHOLDER_PREFIX):-len(PLACEHOLDER_SUFFIX)]
    return None


def is_placeholder(identifier: str) -> bool:
    """
    Check if an identifier is a placeholder.

    Args:
        identifier: The identifier to check.

    Returns:
        True if this is a placeholder identifier.
    """
    return from_placeholder(identifier) is not None


@dataclass(frozen=True)
class PlaceholderInfo:
    """Information about a placeholder found in template code."""
    name: str
    type_hint: Optional[str]
    start: int
    end: int


def find_placeholders(code: str) -> List[PlaceholderInfo]:
    """
    Find all {name} placeholders in template code.

    Args:
        code: The template code string.

    Returns:
        List of PlaceholderInfo objects for each placeholder found.

    Examples:
        >>> find_placeholders("print({x})")
        [PlaceholderInfo(name='x', type_hint=None, start=6, end=9)]
        >>> find_placeholders("{a} + {b:int}")
        [PlaceholderInfo(name='a', type_hint=None, start=0, end=3),
         PlaceholderInfo(name='b', type_hint='int', start=6, end=13)]
    """
    results = []
    for match in PLACEHOLDER_PATTERN.finditer(code):
        name = match.group(1)
        type_hint = match.group(2)  # May be None
        results.append(PlaceholderInfo(
            name=name,
            type_hint=type_hint,
            start=match.start(),
            end=match.end(),
        ))
    return results


def substitute_placeholders(code: str, captures: Dict[str, Capture]) -> Tuple[str, Dict[str, str]]:
    """
    Replace {name} placeholders with parseable identifiers.

    Converts template code with f-string style placeholders into code
    that can be parsed by Python's ast module.

    Args:
        code: Template code with {name} placeholders.
        captures: Dict mapping capture names to Capture objects.

    Returns:
        Tuple of (substituted_code, mapping) where mapping maps
        placeholder identifiers back to capture names.

    Raises:
        ValueError: If a placeholder references an undefined capture.

    Examples:
        >>> code = "print({x})"
        >>> captures = {'x': capture('x')}
        >>> substitute_placeholders(code, captures)
        ('print(__placeholder_x__)', {'__placeholder_x__': 'x'})
    """
    placeholders = find_placeholders(code)
    mapping: Dict[str, str] = {}

    # Check that all placeholders have corresponding captures
    for ph in placeholders:
        if ph.name not in captures:
            raise ValueError(
                f"Placeholder '{{{ph.name}}}' has no corresponding capture. "
                f"Available captures: {list(captures.keys())}"
            )

    # Build result string with substitutions
    result_parts: List[str] = []
    last_end = 0

    for ph in placeholders:
        # Add text before this placeholder
        result_parts.append(code[last_end:ph.start])

        # Add the placeholder identifier
        placeholder_id = to_placeholder(ph.name)
        result_parts.append(placeholder_id)
        mapping[placeholder_id] = ph.name

        last_end = ph.end

    # Add remaining text after last placeholder
    result_parts.append(code[last_end:])

    return ''.join(result_parts), mapping


def validate_captures(code: str, captures: Dict[str, Capture]) -> None:
    """
    Validate that all captures are used and all placeholders have captures.

    Args:
        code: Template code with {name} placeholders.
        captures: Dict mapping capture names to Capture objects.

    Raises:
        ValueError: If validation fails.
    """
    placeholders = find_placeholders(code)
    placeholder_names = {ph.name for ph in placeholders}
    capture_names = set(captures.keys())

    # Check for undefined captures
    undefined = placeholder_names - capture_names
    if undefined:
        raise ValueError(
            f"Placeholders reference undefined captures: {undefined}. "
            f"Available captures: {capture_names}"
        )

    # Check for unused captures (warning, not error)
    unused = capture_names - placeholder_names
    if unused:
        # This is just informational, not an error
        pass
