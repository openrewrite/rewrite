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

This provides AspectJ-style method pattern matching, aligned with Java's MethodMatcher.

Pattern format: `<declaring type>#<method name>(<argument list>)` or
                `<declaring type> <method name>(<argument list>)`

Examples:
    - `datetime.datetime utcnow()` - matches datetime.datetime.utcnow() with no args
    - `datetime.datetime now(..)` - matches datetime.now() with any arguments
    - `datetime..* *(..)` - matches any method on datetime or its submodules
    - `* *(..)` - matches any method on any type

Wildcards:
    - `*` in type: matches any single component (no dots)
    - `..` in type: matches any sequence of components (e.g., `datetime..*` matches submodules)
    - `*` in method name: matches any method name
    - `..` in arguments: matches zero or more arguments of any type
    - `*` in arguments: matches exactly one argument of any type
"""

from dataclasses import dataclass
from typing import Optional, List

from rewrite.java.tree import MethodInvocation, Identifier


@dataclass
class MethodMatcher:
    """
    Matches method invocations against an AspectJ-style pattern signature.

    Uses full type information from the AST to match method calls.
    Requires type attribution from the type checker (ty) to function.
    """

    _type_matcher: "TypeMatcher"
    _method_matcher: "MethodNameMatcher"
    _argument_matchers: List["ArgumentMatcher"]
    _varargs_position: int  # -1 if no varargs
    _original_pattern: str

    @classmethod
    def create(cls, pattern: str) -> "MethodMatcher":
        """
        Create a MethodMatcher from a pattern string.

        Pattern format: `type#method(args)` or `type method(args)`

        Args:
            pattern: The method signature pattern

        Returns:
            A configured MethodMatcher

        Examples:
            >>> m = MethodMatcher.create("datetime.datetime utcnow()")
            >>> m = MethodMatcher.create("datetime.datetime#now(..)")
            >>> m = MethodMatcher.create("datetime..* *(..)")
        """
        parser = _Parser(pattern)
        parser.parse()

        return cls(
            _type_matcher=parser.type_matcher,
            _method_matcher=parser.method_matcher,
            _argument_matchers=parser.argument_matchers,
            _varargs_position=parser.varargs_position,
            _original_pattern=pattern,
        )

    def matches(self, method: MethodInvocation) -> bool:
        """
        Check if a method invocation matches this pattern.

        Requires type attribution to be present on the method invocation.

        Args:
            method: The method invocation to check

        Returns:
            True if the method matches the pattern
        """
        # Check method name first (fast path)
        if not self._matches_method_name(method):
            return False

        # Require type info
        if method.method_type is None or method.method_type.declaring_type is None:
            return False

        # Check declaring type
        if not self._type_matcher.matches(method.method_type.declaring_type):
            return False

        # Check arguments
        return self._matches_arguments(method)

    def _matches_method_name(self, method: MethodInvocation) -> bool:
        """Check if the method name matches."""
        if not isinstance(method.name, Identifier):
            return False

        return self._method_matcher.matches(method.name.simple_name)

    def _matches_arguments(self, method: MethodInvocation) -> bool:
        """Check if method arguments match the expected pattern."""
        args = method.arguments
        arg_count = len(args)

        if self._varargs_position == -1:
            # No varargs - exact count required
            if arg_count != len(self._argument_matchers):
                return False
            for i, matcher in enumerate(self._argument_matchers):
                arg_type = args[i].type if hasattr(args[i], 'type') else None
                if not matcher.matches(arg_type):
                    return False
            return True
        else:
            # Has wildcard varargs (..) - can match any number
            before_count = self._varargs_position
            after_count = len(self._argument_matchers) - self._varargs_position - 1

            if arg_count < before_count + after_count:
                return False

            # Match before varargs
            for i in range(before_count):
                arg_type = args[i].type if hasattr(args[i], 'type') else None
                if not self._argument_matchers[i].matches(arg_type):
                    return False

            # Match after varargs
            for i in range(after_count):
                arg_idx = arg_count - after_count + i
                matcher_idx = self._varargs_position + 1 + i
                arg_type = args[arg_idx].type if hasattr(args[arg_idx], 'type') else None
                if not self._argument_matchers[matcher_idx].matches(arg_type):
                    return False

            return True

    def __repr__(self) -> str:
        return f"MethodMatcher({self._original_pattern!r})"


class TypeMatcher:
    """Matches type patterns against fully qualified type names."""

    def matches(self, type_obj) -> bool:
        raise NotImplementedError

    def matches_name(self, fqn: str) -> bool:
        raise NotImplementedError


class WildcardTypeMatcher(TypeMatcher):
    """Matches any type."""

    def matches(self, type_obj) -> bool:
        return True

    def matches_name(self, fqn: str) -> bool:
        return True


@dataclass
class PatternTypeMatcher(TypeMatcher):
    """Matches types against a pattern with wildcards."""

    _segments: List[str]  # Pattern segments split by '.'
    _has_double_wildcard: bool  # Whether pattern contains '..'

    @classmethod
    def create(cls, pattern: str) -> "PatternTypeMatcher":
        # Check if pattern contains ..
        has_double = ".." in pattern

        if not has_double:
            # Simple case - no double wildcards
            segments = pattern.split(".")
            return cls(_segments=segments, _has_double_wildcard=False)

        # Handle patterns with .. by splitting on ".." first
        # Examples: "datetime..*" -> ["datetime", "*"]
        #           "..datetime" -> ["", "datetime"]
        #           "foo..bar" -> ["foo", "bar"]
        #           "*..*" -> ["*", "*"]
        parts = pattern.split("..")
        segments = []

        for i, part in enumerate(parts):
            if i > 0:
                # Insert .. marker between parts
                segments.append("..")

            # Split this part by single dots
            if part:
                if part.startswith("."):
                    part = part[1:]
                if part.endswith("."):
                    part = part[:-1]
                if part:
                    segments.extend(part.split("."))

        return cls(_segments=segments, _has_double_wildcard=True)

    def matches(self, type_obj) -> bool:
        fqn = _get_fqn(type_obj)
        if fqn is None:
            return False
        return self.matches_name(fqn)

    def matches_name(self, fqn: str) -> bool:
        fqn_parts = fqn.split(".")
        return self._match_segments(self._segments, fqn_parts)

    def _match_segments(self, pattern: List[str], parts: List[str]) -> bool:
        """Match pattern segments against FQN parts."""
        p_idx = 0
        t_idx = 0

        while p_idx < len(pattern):
            if t_idx >= len(parts):
                # Remaining pattern must be all wildcards
                while p_idx < len(pattern) and pattern[p_idx] in ("*", ".."):
                    p_idx += 1
                return p_idx >= len(pattern)

            seg = pattern[p_idx]

            if seg == "..":
                # Double wildcard - matches zero or more segments
                p_idx += 1
                if p_idx >= len(pattern):
                    # .. at end matches everything
                    return True

                # Try matching remaining pattern at each position
                for try_idx in range(t_idx, len(parts) + 1):
                    if self._match_segments(pattern[p_idx:], parts[try_idx:]):
                        return True
                return False

            elif seg == "*":
                # Single wildcard - matches exactly one segment
                p_idx += 1
                t_idx += 1

            else:
                # Literal match (may contain * for partial matching)
                if "*" in seg:
                    if not self._matches_glob(seg, parts[t_idx]):
                        return False
                elif seg != parts[t_idx]:
                    return False
                p_idx += 1
                t_idx += 1

        return t_idx >= len(parts)

    def _matches_glob(self, pattern: str, text: str) -> bool:
        """Match a glob pattern (with *) against text."""
        if pattern == "*":
            return True

        parts = pattern.split("*")
        if len(parts) == 1:
            return pattern == text

        # Check prefix
        if parts[0] and not text.startswith(parts[0]):
            return False

        # Check suffix
        if parts[-1] and not text.endswith(parts[-1]):
            return False

        # Check middle parts exist in order
        pos = len(parts[0])
        for i in range(1, len(parts) - 1):
            if parts[i]:
                idx = text.find(parts[i], pos)
                if idx == -1:
                    return False
                pos = idx + len(parts[i])

        return True


class MethodNameMatcher:
    """Matches method names."""

    def matches(self, name: str) -> bool:
        raise NotImplementedError


class ExactMethodNameMatcher(MethodNameMatcher):
    """Matches an exact method name."""

    def __init__(self, name: str):
        self._name = name

    def matches(self, name: str) -> bool:
        return self._name == name


class WildcardMethodNameMatcher(MethodNameMatcher):
    """Matches any method name."""

    def matches(self, name: str) -> bool:
        return True


@dataclass
class PatternMethodNameMatcher(MethodNameMatcher):
    """Matches method names with glob patterns."""

    _pattern: str

    def matches(self, name: str) -> bool:
        return self._matches_glob(self._pattern, name)

    def _matches_glob(self, pattern: str, text: str) -> bool:
        """Match a glob pattern (with *) against text."""
        if pattern == "*":
            return True

        parts = pattern.split("*")
        if len(parts) == 1:
            return pattern == text

        # Check prefix
        if parts[0] and not text.startswith(parts[0]):
            return False

        # Check suffix
        if parts[-1] and not text.endswith(parts[-1]):
            return False

        # For patterns like "utc*", just check prefix
        if len(parts) == 2:
            if parts[0] and parts[1]:
                return text.startswith(parts[0]) and text.endswith(parts[1])
            return True

        # Check middle parts exist in order
        pos = len(parts[0])
        for i in range(1, len(parts) - 1):
            if parts[i]:
                idx = text.find(parts[i], pos)
                if idx == -1:
                    return False
                pos = idx + len(parts[i])

        return True


class ArgumentMatcher:
    """Matches argument types."""

    def matches(self, arg_type) -> bool:
        raise NotImplementedError


class WildcardArgumentMatcher(ArgumentMatcher):
    """Matches any single argument."""

    def matches(self, arg_type) -> bool:
        return True


class WildcardVarargsArgumentMatcher(ArgumentMatcher):
    """Matches zero or more arguments of any type (..)."""

    def matches(self, arg_type) -> bool:
        return True


@dataclass
class TypedArgumentMatcher(ArgumentMatcher):
    """Matches arguments of a specific type pattern."""

    _type_pattern: str

    def matches(self, arg_type) -> bool:
        if arg_type is None:
            return False

        fqn = _get_fqn(arg_type)
        if fqn is None:
            return False

        # Simple matching for now - could be enhanced
        if self._type_pattern == "*":
            return True

        return fqn == self._type_pattern or fqn.endswith("." + self._type_pattern)


def _get_fqn(type_obj) -> Optional[str]:
    """Extract the fully qualified name from a type object."""
    if type_obj is None:
        return None

    if hasattr(type_obj, "_fully_qualified_name"):
        return type_obj._fully_qualified_name
    if hasattr(type_obj, "fully_qualified_name"):
        return type_obj.fully_qualified_name

    return None


class _Parser:
    """Parses method patterns into matchers."""

    def __init__(self, pattern: str):
        self.pattern = pattern
        self.type_matcher: TypeMatcher = WildcardTypeMatcher()
        self.method_matcher: MethodNameMatcher = WildcardMethodNameMatcher()
        self.argument_matchers: List[ArgumentMatcher] = []
        self.varargs_position: int = -1

    def parse(self):
        pattern = self.pattern.strip()

        # Find argument list
        open_paren = pattern.find("(")
        if open_paren == -1:
            raise ValueError(
                f"Invalid method pattern: '{self.pattern}'. "
                f"Expected format: 'type.name methodName(args)' - missing '('"
            )

        close_paren = pattern.rfind(")")
        if close_paren == -1 or close_paren <= open_paren:
            raise ValueError(
                f"Invalid method pattern: '{self.pattern}'. "
                f"Expected format: 'type.name methodName(args)' - missing or misplaced ')'"
            )

        # Find separator between type and method (# or last space before '(')
        before_paren = pattern[:open_paren]
        separator = before_paren.rfind("#")
        if separator == -1:
            separator = before_paren.rfind(" ")
            if separator == -1:
                raise ValueError(
                    f"Invalid method pattern: '{self.pattern}'. "
                    f"Expected format: 'type.name methodName(args)' - missing separator"
                )

        # Parse type pattern
        type_pattern = before_paren[:separator].strip()
        if not type_pattern:
            raise ValueError(
                f"Invalid method pattern: '{self.pattern}'. "
                f"Empty type pattern"
            )
        self.type_matcher = self._parse_type_matcher(type_pattern)

        # Parse method name
        method_name = before_paren[separator + 1:].strip()
        if not method_name:
            raise ValueError(
                f"Invalid method pattern: '{self.pattern}'. "
                f"Empty method name"
            )
        self.method_matcher = self._parse_method_matcher(method_name)

        # Parse arguments
        args_str = pattern[open_paren + 1:close_paren].strip()
        self._parse_arguments(args_str)

    def _parse_type_matcher(self, pattern: str) -> TypeMatcher:
        # Universal wildcards that match everything
        if pattern in ("*", "..*", "*..", "*..*", "*.."):
            return WildcardTypeMatcher()
        return PatternTypeMatcher.create(pattern)

    def _parse_method_matcher(self, name: str) -> MethodNameMatcher:
        if name == "*":
            return WildcardMethodNameMatcher()
        if "*" in name:
            return PatternMethodNameMatcher(name)
        return ExactMethodNameMatcher(name)

    def _parse_arguments(self, args_str: str):
        if not args_str:
            return

        if args_str == "..":
            self.argument_matchers.append(WildcardVarargsArgumentMatcher())
            self.varargs_position = 0
            return

        # Split by comma
        args = [a.strip() for a in args_str.split(",")]

        for arg in args:
            if not arg:
                continue

            if arg == "..":
                if self.varargs_position != -1:
                    raise ValueError(
                        f"Invalid method pattern: '{self.pattern}'. "
                        f"Only one '..' wildcard allowed in arguments"
                    )
                self.varargs_position = len(self.argument_matchers)
                self.argument_matchers.append(WildcardVarargsArgumentMatcher())
            elif arg == "*":
                self.argument_matchers.append(WildcardArgumentMatcher())
            else:
                self.argument_matchers.append(TypedArgumentMatcher(arg))
