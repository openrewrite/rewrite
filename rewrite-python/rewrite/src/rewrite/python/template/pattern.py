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

"""Pattern class for matching against Python AST."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Union, TYPE_CHECKING

from rewrite.java import J

from .capture import Capture
from .engine import TemplateEngine, TemplateOptions

if TYPE_CHECKING:
    from rewrite.visitor import Cursor


class MatchResult:
    """
    Result of a successful pattern match.

    Provides access to captured values by name or Capture object.

    Examples:
        match = pattern.match(node, cursor)
        if match:
            x_value = match.get('x')
            # or
            x_value = match.get(x_capture)
            # or
            x_value = match['x']
    """

    def __init__(self, captures: Dict[str, J]):
        """
        Initialize match result.

        Args:
            captures: Dict mapping capture names to their matched AST values.
        """
        self._captures = captures

    def get(self, capture: Union[str, Capture]) -> Optional[J]:
        """
        Get a captured value.

        Args:
            capture: The capture name or Capture object.

        Returns:
            The captured AST node, or None if not found.
        """
        name = capture.name if isinstance(capture, Capture) else capture
        return self._captures.get(name)

    def __getitem__(self, key: Union[str, Capture]) -> J:
        """
        Get a captured value (dict-style access).

        Args:
            key: The capture name or Capture object.

        Returns:
            The captured AST node.

        Raises:
            KeyError: If the capture was not matched.
        """
        name = key.name if isinstance(key, Capture) else key
        if name not in self._captures:
            raise KeyError(f"Capture '{name}' not found in match result")
        return self._captures[name]

    def has(self, capture: Union[str, Capture]) -> bool:
        """
        Check if a capture was matched.

        Args:
            capture: The capture name or Capture object.

        Returns:
            True if the capture has a value.
        """
        name = capture.name if isinstance(capture, Capture) else capture
        return name in self._captures

    def __contains__(self, item: Union[str, Capture]) -> bool:
        """Support 'in' operator."""
        return self.has(item)

    def names(self) -> List[str]:
        """Get all capture names in this result."""
        return list(self._captures.keys())

    def as_dict(self) -> Dict[str, J]:
        """Get all captures as a dictionary."""
        return dict(self._captures)

    def __bool__(self) -> bool:
        """Match results are truthy (use for if match: pattern)."""
        return True

    def __repr__(self) -> str:
        return f"MatchResult({list(self._captures.keys())})"


class Pattern:
    """
    Pattern for matching against Python AST nodes.

    Patterns define a structural template with captures that can match
    against AST nodes and extract captured values.

    Examples:
        # Pattern with named capture
        x = capture('x')
        pat = pattern("print({x})", x=x)

        # Match against a node
        match = pat.match(node, cursor)
        if match:
            captured_x = match.get(x)  # or match.get('x')

        # Pattern with multiple captures
        a, b = capture('a'), capture('b')
        pat = pattern("{a} + {b}", a=a, b=b)
    """

    def __init__(
        self,
        code: str,
        captures: Optional[Dict[str, Capture]] = None,
        imports: Optional[List[str]] = None,
    ):
        """
        Initialize a pattern.

        Args:
            code: Python code with {name} placeholders.
            captures: Dict mapping capture names to Capture objects.
            imports: Import statements for type resolution.
        """
        self._code = code
        self._captures = captures or {}
        self._options = TemplateOptions(
            imports=tuple(imports) if imports else (),
        )
        self._cached_tree: Optional[J] = None

    @property
    def code(self) -> str:
        """The pattern code string."""
        return self._code

    @property
    def captures(self) -> Dict[str, Capture]:
        """The captures defined for this pattern."""
        return self._captures

    def get_tree(self) -> J:
        """
        Get the parsed pattern tree (cached).

        Returns:
            The parsed AST node representing the pattern.
        """
        if self._cached_tree is None:
            self._cached_tree = TemplateEngine.get_template_tree(
                self._code,
                self._captures,
                self._options,
            )
        return self._cached_tree

    def match(
        self,
        tree: J,
        cursor: 'Cursor',
        *,
        debug: bool = False
    ) -> Optional[MatchResult]:
        """
        Match this pattern against an AST node.

        Args:
            tree: The AST node to match against.
            cursor: Cursor at the node's position.
            debug: Enable debug logging.

        Returns:
            MatchResult if matched, None otherwise.

        Examples:
            match = pattern.match(node, cursor)
            if match:
                value = match.get('x')
        """
        from .comparator import PatternMatchingComparator

        pattern_tree = self.get_tree()
        comparator = PatternMatchingComparator(self._captures)

        captured = comparator.match(pattern_tree, tree, cursor, debug=debug)

        if captured is not None:
            return MatchResult(captured)
        return None

    def matches(
        self,
        tree: J,
        cursor: 'Cursor'
    ) -> bool:
        """
        Check if this pattern matches an AST node (without capturing).

        Args:
            tree: The AST node to match against.
            cursor: Cursor at the node's position.

        Returns:
            True if the pattern matches.
        """
        return self.match(tree, cursor) is not None


def pattern(
    code,
    *,
    imports: Optional[List[str]] = None,
    **captures: Capture
) -> Pattern:
    """
    Create a pattern from Python code.

    This is the primary factory function for creating patterns.

    Args:
        code: Python code with {name} placeholders, or a t-string
              (Python 3.14+) with Capture/RawCode interpolations.
        imports: Optional import statements for type resolution.
        **captures: Named capture specifications (not allowed with t-strings).

    Returns:
        A Pattern instance.

    Examples:
        # Pattern with capture
        x = capture('x')
        pat = pattern("print({x})", x=x)

        # With t-string (Python 3.14+)
        x = capture('x')
        pat = pattern(t"print({x})")

        # Pattern with multiple captures
        a, b = capture('a'), capture('b')
        pat = pattern("{a} + {b}", a=a, b=b)

        # Variadic pattern (matches multiple arguments)
        args = capture('args', variadic=True)
        pat = pattern("func({args})", args=args)
    """
    from rewrite.python.template._tstring_support import is_tstring, convert_tstring

    if is_tstring(code):
        if captures:
            raise TypeError(
                "Cannot pass keyword captures when using a t-string; "
                "interpolate Capture objects directly in the t-string instead"
            )
        code, captures = convert_tstring(code)

    return Pattern(
        code=code,
        captures=captures,
        imports=imports,
    )
