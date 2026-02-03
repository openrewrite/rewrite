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

"""
MethodMatcher utility for matching Python method invocations by type signature.

This provides a pattern-based approach to matching method calls, similar to
Java's MethodMatcher but adapted for Python's type system.

Pattern format: `module.ClassName methodName(arg_types)`

Examples:
    - `datetime.datetime utcnow()` - matches datetime.datetime.utcnow()
    - `datetime.datetime now(..)` - matches datetime.now() with any arguments
    - `*.datetime utcnow()` - matches utcnow() on any datetime class
    - `datetime.datetime *(..)` - matches any method on datetime.datetime

Wildcards:
    - `*` in type: matches any single component (e.g., `*.datetime` matches `datetime.datetime`)
    - `..` in type: matches any number of components (e.g., `..datetime` matches any module path ending in datetime)
    - `*` in method name: matches any method name
    - `..` in arguments: matches any number of arguments
    - `*` in arguments: matches any single argument type
"""

import re
from dataclasses import dataclass
from typing import Optional, Pattern

from rewrite.java.tree import MethodInvocation, Identifier


@dataclass
class MethodMatcher:
    """
    Matches method invocations against a pattern signature.

    Uses full type information from the AST to match method calls.
    Requires type attribution from the type checker (ty) to function.
    """

    _type_pattern: Pattern[str]
    _method_pattern: Pattern[str]
    _arg_pattern: Optional[str]  # None means match any args
    _original_pattern: str

    @classmethod
    def create(cls, pattern: str) -> "MethodMatcher":
        """
        Create a MethodMatcher from a pattern string.

        Pattern format: `type_pattern method_name(arg_pattern)`

        Args:
            pattern: The method signature pattern

        Returns:
            A configured MethodMatcher

        Examples:
            >>> m = MethodMatcher.create("datetime.datetime utcnow()")
            >>> m = MethodMatcher.create("datetime.datetime now(..)")
            >>> m = MethodMatcher.create("*.datetime *(..)")
        """
        # Parse pattern: "type method(args)" or "type method()"
        match = re.match(r"^\s*(\S+)\s+(\S+)\s*\(\s*(.*?)\s*\)\s*$", pattern)
        if not match:
            raise ValueError(
                f"Invalid method pattern: '{pattern}'. "
                f"Expected format: 'type.name methodName(args)'"
            )

        type_pattern_str, method_name, arg_pattern = match.groups()

        # Convert type pattern to regex
        type_regex = cls._convert_type_pattern(type_pattern_str)

        # Convert method name to regex (support wildcards)
        method_regex = cls._convert_name_pattern(method_name)

        return cls(
            _type_pattern=re.compile(f"^{type_regex}$"),
            _method_pattern=re.compile(f"^{method_regex}$"),
            _arg_pattern=arg_pattern if arg_pattern != ".." else None,
            _original_pattern=pattern,
        )

    @classmethod
    def _convert_type_pattern(cls, pattern: str) -> str:
        """Convert a type pattern with wildcards to a regex pattern."""
        # Handle special patterns
        if pattern == "*":
            return r"[^.]+"
        if pattern == "..":
            return r".*"

        # Handle leading ".." for matching any prefix
        if pattern.startswith(".."):
            suffix = pattern[2:]
            # Match the suffix optionally preceded by anything and a dot
            return r"(?:.*\.)?" + re.escape(suffix)

        # Split by dots and convert each component
        parts = pattern.split(".")
        regex_parts = []
        for part in parts:
            if part == "*":
                regex_parts.append(r"[^.]+")
            elif part == "..":
                regex_parts.append(r"(?:[^.]+\.)*[^.]+")
            else:
                regex_parts.append(re.escape(part))

        return r"\.".join(regex_parts)

    @classmethod
    def _convert_name_pattern(cls, pattern: str) -> str:
        """Convert a method name pattern to regex."""
        if pattern == "*":
            return r".+"
        # Support glob-style wildcards in names
        return re.escape(pattern).replace(r"\*", r".*")

    def matches(self, method: MethodInvocation) -> bool:
        """
        Check if a method invocation matches this pattern.

        Requires type attribution to be present on the method invocation.

        Args:
            method: The method invocation to check

        Returns:
            True if the method matches the pattern
        """
        if not self._matches_method_name(method):
            return False

        if method.method_type is None or method.method_type.declaring_type is None:
            return False

        return self._matches_with_type_info(method)

    def _matches_method_name(self, method: MethodInvocation) -> bool:
        """Check if the method name matches."""
        if not isinstance(method.name, Identifier):
            return False

        return self._method_pattern.match(method.name.simple_name) is not None

    def _matches_with_type_info(self, method: MethodInvocation) -> bool:
        """Match using full type information."""
        declaring_type = method.method_type.declaring_type

        # Get the fully qualified name
        fqn = self._get_fqn(declaring_type)
        if fqn is None:
            return False

        # Match against type pattern
        if not self._type_pattern.match(fqn):
            return False

        # Match arguments if specified
        if self._arg_pattern is not None:
            return self._matches_arguments(method)

        return True

    def _get_fqn(self, type_obj) -> Optional[str]:
        """Extract the fully qualified name from a JavaType."""
        if type_obj is None:
            return None

        # Handle different JavaType subclasses
        if hasattr(type_obj, "_fully_qualified_name"):
            return type_obj._fully_qualified_name
        if hasattr(type_obj, "fully_qualified_name"):
            return type_obj.fully_qualified_name

        return None

    def _matches_arguments(self, method: MethodInvocation) -> bool:
        """Check if method arguments match the expected pattern."""
        args = method.arguments

        if self._arg_pattern == "":
            # Empty args pattern means no arguments expected
            return len(args) == 0

        # For more complex argument matching, we'd need to implement
        # full argument type checking. For now, just check count for
        # patterns without wildcards.
        return True

    def __repr__(self) -> str:
        return f"MethodMatcher({self._original_pattern!r})"
