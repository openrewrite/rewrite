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

"""Tests for Pattern class."""

import pytest

from rewrite.java import tree as j
from rewrite.python.template import pattern, capture, Pattern, MatchResult


class TestPattern:
    """Tests for Pattern class and pattern() factory."""

    def test_simple_pattern(self):
        """Test creating a simple pattern."""
        pat = pattern("x + 1")

        assert pat.code == "x + 1"
        assert len(pat.captures) == 0

    def test_pattern_with_capture(self):
        """Test creating a pattern with a capture."""
        expr = capture('expr')
        pat = pattern("print({expr})", expr=expr)

        assert pat.code == "print({expr})"
        assert 'expr' in pat.captures
        assert pat.captures['expr'] is expr

    def test_pattern_get_tree(self):
        """Test getting the parsed pattern tree."""
        pat = pattern("x + 1")
        tree = pat.get_tree()

        assert isinstance(tree, j.Binary)

    def test_multiple_captures(self):
        """Test pattern with multiple captures."""
        a, b = capture('a'), capture('b')
        pat = pattern("{a} + {b}", a=a, b=b)

        assert len(pat.captures) == 2
        assert 'a' in pat.captures
        assert 'b' in pat.captures


class TestMatchResult:
    """Tests for MatchResult class."""

    def test_get_by_name(self):
        """Test getting capture by name."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        assert result.get('x') is ident

    def test_get_by_capture(self):
        """Test getting capture by Capture object."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        cap = capture('x')
        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        assert result.get(cap) is ident

    def test_getitem(self):
        """Test dict-style access."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        assert result['x'] is ident

    def test_getitem_missing_raises(self):
        """Test that missing key raises KeyError."""
        result = MatchResult({})

        with pytest.raises(KeyError):
            _ = result['x']

    def test_has(self):
        """Test has() method."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        assert result.has('x') is True
        assert result.has('y') is False

    def test_contains(self):
        """Test 'in' operator."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        assert 'x' in result
        assert 'y' not in result

    def test_names(self):
        """Test names() method."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'a': ident, 'b': ident})

        names = result.names()
        assert 'a' in names
        assert 'b' in names

    def test_as_dict(self):
        """Test as_dict() method."""
        from rewrite import random_id, Markers
        from rewrite.java import Space

        ident = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
        result = MatchResult({'x': ident})

        d = result.as_dict()
        assert isinstance(d, dict)
        assert d['x'] is ident

    def test_truthy(self):
        """Test that MatchResult is truthy."""
        result = MatchResult({})
        assert bool(result) is True
