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

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Empty, Identifier, MethodInvocation
from rewrite.python.type_mapping import PRIMITIVE_TO_PYTHON
from rewrite.python.type_utils import is_of_type_with_name


@dataclass
class MethodMatcher:
    """
    Matches method invocations against an AspectJ-style pattern signature.

    Matching turns on the call's declaring type, which an import-resolved call
    carries with no type check running; a pattern naming a concrete receiver fails
    where that type is ``JavaType.Unknown``, though ``*..*`` still matches and
    ``matches(method, match_unknown_types=True)`` falls back to the call as written.
    ``REWRITE_PYTHON_DUMP_TYPES=1`` in a test run prints what each call got.
    """

    _type_matcher: "TypeMatcher"
    _method_matcher: "MethodNameMatcher"
    _argument_matchers: List["ArgumentMatcher"]
    _varargs_position: int  # -1 if no varargs
    _original_pattern: str
    _match_overrides: bool = False

    @classmethod
    def create(cls, pattern: str, match_overrides: bool = False) -> "MethodMatcher":
        """
        Create a MethodMatcher from a pattern string.

        Pattern format: `type#method(args)` or `type method(args)`

        Args:
            pattern: The method signature pattern
            match_overrides: Also match calls whose declaring type is a subtype
                of the pattern's type

        Returns:
            A configured MethodMatcher

        Examples:
            >>> m = MethodMatcher.create("datetime.datetime utcnow()")
            >>> m = MethodMatcher.create("datetime.datetime#now(..)")
            >>> m = MethodMatcher.create("datetime..* *(..)")
            >>> m = MethodMatcher.create("threading.Thread getName()", match_overrides=True)
        """
        parser = _Parser(pattern)
        parser.parse()

        return cls(
            _type_matcher=parser.type_matcher,
            _method_matcher=parser.method_matcher,
            _argument_matchers=parser.argument_matchers,
            _varargs_position=parser.varargs_position,
            _original_pattern=pattern,
            _match_overrides=match_overrides,
        )

    def matches(self, method: MethodInvocation, match_unknown_types: bool = False) -> bool:
        """
        Check if a method invocation matches this pattern.

        Args:
            method: The method invocation to check
            match_unknown_types: when the attributed types don't match,
                also match structurally, on the call's written receiver,
                name and arguments. Reaches calls whose declaring type is
                ``JavaType.Unknown``, at the risk of false positives on
                unrelated calls, so it is off by default.

        Returns:
            True if the method matches the pattern
        """
        if self._matches_typed(method):
            return True
        return match_unknown_types and self._matches_allowing_unknown_types(method)

    def _matches_typed(self, method: MethodInvocation) -> bool:
        """Check the pattern against the call's type attribution."""
        # Check method name first (fast path)
        if not self._matches_method_name(method):
            return False

        # A parsed call's declaring type is JavaType.Unknown, never None, so this
        # guard leaves Unknown to the target-type check below, where a wildcard
        # receiver still matches it. Rejecting it here would diverge from Java.
        if method.method_type is None or method.method_type.declaring_type is None:
            return False

        # Check declaring type
        if not self._matches_target_type(method.method_type.declaring_type):
            return False

        # Check arguments
        return self._matches_arguments(method)

    def _matches_target_type(self, type_obj) -> bool:
        """Check if the declaring type matches, optionally through supertypes."""
        if self._type_matcher.matches(type_obj):
            return True
        return self._match_overrides and is_of_type_with_name(
            type_obj, True, self._type_matcher.matches_name
        )

    def _matches_allowing_unknown_types(self, method: MethodInvocation) -> bool:
        """Check the pattern against the call as written, ignoring absent types."""
        if not self._method_matcher.matches(method.name.simple_name):
            return False

        # Only a bare identifier receiver is compared; a qualified or computed
        # one is left unchecked, as in Java.
        select = method.select
        if isinstance(select, Identifier) and \
                not self._type_matcher.matches_simple_name(select.simple_name):
            return False

        return self._matches_arguments(method, allow_unknown=True)

    def _matches_method_name(self, method: MethodInvocation) -> bool:
        """Check if the method name matches.

        The method type names what is called, which for a construction is
        ``<constructor>`` rather than the class name at the call site. Java's
        MethodMatcher reads the same name, so one pattern serves both.
        """
        return method.method_type is not None and \
            self._method_matcher.matches(method.method_type.name)

    def _matches_arguments(self, method: MethodInvocation, allow_unknown: bool = False) -> bool:
        """Check if method arguments match the expected pattern."""
        args = method.arguments
        if len(args) == 1 and isinstance(args[0], Empty):
            # A call with no arguments holds one Empty placeholder, which
            # no pattern names.
            args = []
        arg_count = len(args)

        def accepts(matcher: "ArgumentMatcher", arg_type) -> bool:
            return matcher.matches_unknown(arg_type) if allow_unknown else matcher.matches(arg_type)

        if self._varargs_position == -1:
            # No varargs - exact count required
            if arg_count != len(self._argument_matchers):
                return False
            for i, matcher in enumerate(self._argument_matchers):
                arg_type = args[i].type if hasattr(args[i], 'type') else None
                if not accepts(matcher, arg_type):
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
                if not accepts(self._argument_matchers[i], arg_type):
                    return False

            # Match after varargs
            for i in range(after_count):
                arg_idx = arg_count - after_count + i
                matcher_idx = self._varargs_position + 1 + i
                arg_type = args[arg_idx].type if hasattr(args[arg_idx], 'type') else None
                if not accepts(self._argument_matchers[matcher_idx], arg_type):
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

    def matches_simple_name(self, simple_name: str) -> bool:
        """Whether the pattern's trailing component matches an unqualified name."""
        raise NotImplementedError


class WildcardTypeMatcher(TypeMatcher):
    """Matches any type."""

    def matches(self, type_obj) -> bool:
        return True

    def matches_name(self, fqn: str) -> bool:
        return True

    def matches_simple_name(self, simple_name: str) -> bool:
        return True


@dataclass
class PatternTypeMatcher(TypeMatcher):
    """Matches types against a pattern with wildcards."""

    _pattern: str
    _segments: List[str]  # Pattern segments split by '.'
    _has_double_wildcard: bool  # Whether pattern contains '..'

    @classmethod
    def create(cls, pattern: str) -> "PatternTypeMatcher":
        # Check if pattern contains ..
        has_double = ".." in pattern

        if not has_double:
            # Simple case - no double wildcards
            segments = pattern.split(".")
            return cls(_pattern=pattern, _segments=segments, _has_double_wildcard=False)

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

        return cls(_pattern=pattern, _segments=segments, _has_double_wildcard=True)

    def matches(self, type_obj) -> bool:
        fqn = _get_fqn(type_obj)
        if fqn is None:
            return False
        return self.matches_name(fqn)

    def matches_name(self, fqn: str) -> bool:
        fqn_parts = fqn.split(".")
        return self._match_segments(self._segments, fqn_parts)

    def matches_simple_name(self, simple_name: str) -> bool:
        last_dot = self._pattern.rfind(".")
        if last_dot < 0:
            # Java accepts a dotless pattern as a receiver name only when literal.
            return "*" not in self._pattern and self._pattern == simple_name

        tail = self._pattern[last_dot + 1:]
        if not tail:
            return False
        if "*" in tail:
            return self._matches_glob(tail, simple_name)
        return tail == simple_name

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

    def matches_unknown(self, arg_type) -> bool:
        """Whether the argument matches once absent type information is excused."""
        raise NotImplementedError


class WildcardArgumentMatcher(ArgumentMatcher):
    """Matches any single argument."""

    def matches(self, arg_type) -> bool:
        return True

    def matches_unknown(self, arg_type) -> bool:
        return True


class WildcardVarargsArgumentMatcher(ArgumentMatcher):
    """Matches zero or more arguments of any type (..)."""

    def matches(self, arg_type) -> bool:
        return True

    def matches_unknown(self, arg_type) -> bool:
        return True


@dataclass
class TypedArgumentMatcher(ArgumentMatcher):
    """Matches arguments of a specific type pattern."""

    _type_pattern: str
    _type_matcher: "TypeMatcher"

    @classmethod
    def create(cls, type_pattern: str) -> "TypedArgumentMatcher":
        return cls(_type_pattern=type_pattern,
                   _type_matcher=_type_matcher_for(type_pattern))

    def matches(self, arg_type) -> bool:
        fqn = _get_fqn(arg_type)
        if fqn is None:
            return False

        # An unqualified pattern also names the trailing component of a qualified
        # type, so `datetime` reaches `datetime.datetime`.
        return self._type_matcher.matches_name(fqn) or \
            fqn.endswith("." + self._type_pattern)

    def matches_unknown(self, arg_type) -> bool:
        if arg_type is None or isinstance(arg_type, JavaType.Unknown):
            return True
        return self.matches(arg_type)


def _get_fqn(type_obj) -> Optional[str]:
    """The name a pattern spells this type by."""
    if type_obj is None:
        return None

    if isinstance(type_obj, JavaType.Primitive):
        return PRIMITIVE_TO_PYTHON.get(type_obj)

    if hasattr(type_obj, "fully_qualified_name"):
        return type_obj.fully_qualified_name

    return None


def _type_matcher_for(pattern: str) -> TypeMatcher:
    # Universal wildcards that match everything
    if pattern in ("*", "..*", "*..", "*..*", "*.."):
        return WildcardTypeMatcher()
    return PatternTypeMatcher.create(pattern)


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
        self.type_matcher = _type_matcher_for(type_pattern)

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
                self.argument_matchers.append(TypedArgumentMatcher.create(arg))
