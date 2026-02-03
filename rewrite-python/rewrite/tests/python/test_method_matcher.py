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

"""Tests for the MethodMatcher utility."""

import pytest

from rewrite.python import MethodMatcher
from rewrite.python.method_matcher import (
    PatternTypeMatcher,
    WildcardTypeMatcher,
    ExactMethodNameMatcher,
    PatternMethodNameMatcher,
    WildcardMethodNameMatcher,
)


class TestMethodMatcherPatternParsing:
    """Test pattern parsing and validation."""

    def test_simple_pattern(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert m._original_pattern == "datetime.datetime utcnow()"
        assert isinstance(m._type_matcher, PatternTypeMatcher)
        assert isinstance(m._method_matcher, ExactMethodNameMatcher)

    def test_pattern_with_hash_separator(self):
        m = MethodMatcher.create("datetime.datetime#utcnow()")
        assert m._original_pattern == "datetime.datetime#utcnow()"
        assert isinstance(m._method_matcher, ExactMethodNameMatcher)

    def test_pattern_with_varargs(self):
        m = MethodMatcher.create("datetime.datetime fromtimestamp(..)")
        assert m._varargs_position == 0

    def test_pattern_with_empty_args(self):
        m = MethodMatcher.create("datetime.datetime now()")
        assert len(m._argument_matchers) == 0
        assert m._varargs_position == -1

    def test_wildcard_type_pattern(self):
        m = MethodMatcher.create("* utcnow()")
        assert isinstance(m._type_matcher, WildcardTypeMatcher)

    def test_wildcard_method_pattern(self):
        m = MethodMatcher.create("datetime.datetime *()")
        assert isinstance(m._method_matcher, WildcardMethodNameMatcher)

    def test_invalid_pattern_no_parens(self):
        with pytest.raises(ValueError, match="Invalid method pattern"):
            MethodMatcher.create("datetime.datetime utcnow")

    def test_invalid_pattern_empty(self):
        with pytest.raises(ValueError, match="Invalid method pattern"):
            MethodMatcher.create("")

    def test_invalid_pattern_no_separator(self):
        with pytest.raises(ValueError, match="Invalid method pattern"):
            MethodMatcher.create("datetime.datetime.utcnow()")


class TestTypePatternMatching:
    """Test type pattern matching."""

    def test_exact_type_match(self):
        m = PatternTypeMatcher.create("datetime.datetime")
        assert m.matches_name("datetime.datetime")
        assert not m.matches_name("other.datetime")
        assert not m.matches_name("datetime.datetime.extra")

    def test_single_wildcard_match(self):
        m = PatternTypeMatcher.create("*.datetime")
        assert m.matches_name("foo.datetime")
        assert m.matches_name("datetime.datetime")
        assert not m.matches_name("foo.bar.datetime")

    def test_double_wildcard_suffix(self):
        """Test datetime..* matches datetime and submodules."""
        m = PatternTypeMatcher.create("datetime..*")
        assert m.matches_name("datetime.datetime")
        assert m.matches_name("datetime.timezone")
        assert m.matches_name("datetime.date")
        assert m.matches_name("datetime.foo.bar")
        assert not m.matches_name("other.datetime")

    def test_double_wildcard_prefix(self):
        """Test ..datetime matches any path ending in datetime."""
        m = PatternTypeMatcher.create("..datetime")
        assert m.matches_name("datetime")
        assert m.matches_name("foo.datetime")
        assert m.matches_name("foo.bar.datetime")
        assert not m.matches_name("datetime.foo")

    def test_double_wildcard_middle(self):
        """Test foo..bar matches foo.*.bar patterns."""
        m = PatternTypeMatcher.create("foo..bar")
        assert m.matches_name("foo.bar")
        assert m.matches_name("foo.x.bar")
        assert m.matches_name("foo.x.y.z.bar")
        assert not m.matches_name("other.bar")

    def test_universal_wildcard(self):
        """Test *..*  matches everything."""
        m = MethodMatcher.create("*..* *(..)")
        assert isinstance(m._type_matcher, WildcardTypeMatcher)


class TestMethodNameMatching:
    """Test method name pattern matching."""

    def test_exact_method_match(self):
        m = ExactMethodNameMatcher("utcnow")
        assert m.matches("utcnow")
        assert not m.matches("utcfromtimestamp")

    def test_wildcard_method_match(self):
        m = WildcardMethodNameMatcher()
        assert m.matches("utcnow")
        assert m.matches("now")
        assert m.matches("fromtimestamp")

    def test_prefix_wildcard_match(self):
        m = PatternMethodNameMatcher("utc*")
        assert m.matches("utcnow")
        assert m.matches("utcfromtimestamp")
        assert not m.matches("now")

    def test_suffix_wildcard_match(self):
        m = PatternMethodNameMatcher("*now")
        assert m.matches("utcnow")
        assert m.matches("now")
        assert not m.matches("utcfromtimestamp")


class TestArgumentMatching:
    """Test argument pattern matching."""

    def test_no_args_pattern(self):
        m = MethodMatcher.create("foo.Bar baz()")
        assert len(m._argument_matchers) == 0
        assert m._varargs_position == -1

    def test_varargs_pattern(self):
        m = MethodMatcher.create("foo.Bar baz(..)")
        assert m._varargs_position == 0

    def test_single_wildcard_arg(self):
        m = MethodMatcher.create("foo.Bar baz(*)")
        assert len(m._argument_matchers) == 1
        assert m._varargs_position == -1

    def test_typed_arg(self):
        m = MethodMatcher.create("foo.Bar baz(str)")
        assert len(m._argument_matchers) == 1
        assert m._varargs_position == -1

    def test_mixed_args(self):
        m = MethodMatcher.create("foo.Bar baz(str, .., int)")
        assert len(m._argument_matchers) == 3
        assert m._varargs_position == 1

    def test_multiple_varargs_rejected(self):
        with pytest.raises(ValueError, match="Only one"):
            MethodMatcher.create("foo.Bar baz(.., ..)")


class TestMethodMatcherRepr:
    """Test string representation."""

    def test_repr(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert repr(m) == "MethodMatcher('datetime.datetime utcnow()')"
