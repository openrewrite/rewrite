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

"""Tests for placeholder utilities."""

import pytest

from rewrite.python.template import capture
from rewrite.python.template.placeholder import (
    to_placeholder,
    from_placeholder,
    is_placeholder,
    find_placeholders,
    substitute_placeholders,
    validate_captures,
    PlaceholderInfo,
)


class TestPlaceholderConversion:
    """Tests for placeholder identifier conversion."""

    def test_to_placeholder(self):
        """Test converting capture name to placeholder."""
        assert to_placeholder('x') == '__placeholder_x__'
        assert to_placeholder('expr') == '__placeholder_expr__'
        assert to_placeholder('my_var') == '__placeholder_my_var__'

    def test_from_placeholder_valid(self):
        """Test extracting name from valid placeholder."""
        assert from_placeholder('__placeholder_x__') == 'x'
        assert from_placeholder('__placeholder_expr__') == 'expr'
        assert from_placeholder('__placeholder_my_var__') == 'my_var'

    def test_from_placeholder_invalid(self):
        """Test that non-placeholders return None."""
        assert from_placeholder('regular_var') is None
        assert from_placeholder('__private') is None
        assert from_placeholder('__dunder__') is None

    def test_is_placeholder(self):
        """Test placeholder detection."""
        assert is_placeholder('__placeholder_x__') is True
        assert is_placeholder('__placeholder_expr__') is True
        assert is_placeholder('regular_var') is False
        assert is_placeholder('x') is False


class TestFindPlaceholders:
    """Tests for finding placeholders in code."""

    def test_single_placeholder(self):
        """Test finding a single placeholder."""
        placeholders = find_placeholders("print({x})")
        assert len(placeholders) == 1
        assert placeholders[0].name == 'x'
        assert placeholders[0].type_hint is None

    def test_multiple_placeholders(self):
        """Test finding multiple placeholders."""
        placeholders = find_placeholders("{a} + {b}")
        assert len(placeholders) == 2
        assert placeholders[0].name == 'a'
        assert placeholders[1].name == 'b'

    def test_typed_placeholder(self):
        """Test finding a typed placeholder."""
        placeholders = find_placeholders("{x:int}")
        assert len(placeholders) == 1
        assert placeholders[0].name == 'x'
        assert placeholders[0].type_hint == 'int'

    def test_mixed_placeholders(self):
        """Test finding mixed typed and untyped placeholders."""
        placeholders = find_placeholders("{a} + {b:float}")
        assert len(placeholders) == 2
        assert placeholders[0].type_hint is None
        assert placeholders[1].type_hint == 'float'

    def test_no_placeholders(self):
        """Test code with no placeholders."""
        placeholders = find_placeholders("x + 1")
        assert len(placeholders) == 0


class TestSubstitutePlaceholders:
    """Tests for placeholder substitution."""

    def test_simple_substitution(self):
        """Test simple placeholder substitution."""
        captures = {'x': capture('x')}
        result, mapping = substitute_placeholders("print({x})", captures)

        assert result == "print(__placeholder_x__)"
        assert mapping['__placeholder_x__'] == 'x'

    def test_multiple_substitution(self):
        """Test multiple placeholder substitution."""
        captures = {'a': capture('a'), 'b': capture('b')}
        result, mapping = substitute_placeholders("{a} + {b}", captures)

        assert '__placeholder_a__' in result
        assert '__placeholder_b__' in result
        assert '+' in result

    def test_undefined_capture_raises(self):
        """Test that undefined captures raise ValueError."""
        captures = {'x': capture('x')}
        with pytest.raises(ValueError, match="no corresponding capture"):
            substitute_placeholders("print({y})", captures)


class TestValidateCaptures:
    """Tests for capture validation."""

    def test_valid_captures(self):
        """Test validation passes with valid captures."""
        captures = {'x': capture('x'), 'y': capture('y')}
        # Should not raise
        validate_captures("{x} + {y}", captures)

    def test_undefined_capture(self):
        """Test validation fails with undefined capture."""
        captures = {'x': capture('x')}
        with pytest.raises(ValueError, match="undefined"):
            validate_captures("{x} + {y}", captures)
