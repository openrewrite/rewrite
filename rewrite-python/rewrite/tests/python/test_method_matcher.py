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


class TestMethodMatcherPatternParsing:
    """Test pattern parsing and validation."""

    def test_simple_pattern(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert m._original_pattern == "datetime.datetime utcnow()"

    def test_pattern_with_varargs(self):
        m = MethodMatcher.create("datetime.datetime fromtimestamp(..)")
        assert m._arg_pattern is None  # ".." means match any

    def test_pattern_with_empty_args(self):
        m = MethodMatcher.create("datetime.datetime now()")
        assert m._arg_pattern == ""

    def test_invalid_pattern_no_parens(self):
        with pytest.raises(ValueError, match="Invalid method pattern"):
            MethodMatcher.create("datetime.datetime utcnow")

    def test_invalid_pattern_empty(self):
        with pytest.raises(ValueError, match="Invalid method pattern"):
            MethodMatcher.create("")


class TestMethodMatcherTypePatterns:
    """Test type pattern regex generation."""

    def test_exact_type_match(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert m._type_pattern.match("datetime.datetime")
        assert not m._type_pattern.match("other.datetime")
        assert not m._type_pattern.match("datetime.datetime.extra")

    def test_single_wildcard_match(self):
        m = MethodMatcher.create("*.datetime utcnow()")
        assert m._type_pattern.match("foo.datetime")
        assert m._type_pattern.match("datetime.datetime")
        assert not m._type_pattern.match("foo.bar.datetime")

    def test_double_wildcard_match(self):
        m = MethodMatcher.create("..datetime utcnow()")
        assert m._type_pattern.match("datetime")
        assert m._type_pattern.match("foo.datetime")
        assert m._type_pattern.match("foo.bar.baz.datetime")


class TestMethodMatcherMethodName:
    """Test method name pattern matching."""

    def test_exact_method_match(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert m._method_pattern.match("utcnow")
        assert not m._method_pattern.match("utcfromtimestamp")

    def test_wildcard_method_match(self):
        m = MethodMatcher.create("datetime.datetime *()")
        assert m._method_pattern.match("utcnow")
        assert m._method_pattern.match("now")
        assert m._method_pattern.match("fromtimestamp")

    def test_prefix_wildcard_match(self):
        m = MethodMatcher.create("datetime.datetime utc*()")
        assert m._method_pattern.match("utcnow")
        assert m._method_pattern.match("utcfromtimestamp")
        assert not m._method_pattern.match("now")


class TestMethodMatcherRepr:
    """Test string representation."""

    def test_repr(self):
        m = MethodMatcher.create("datetime.datetime utcnow()")
        assert repr(m) == "MethodMatcher('datetime.datetime utcnow()')"
