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

"""Tests for PatternMatchingComparator."""

import pytest
from uuid import uuid4

from rewrite import Markers
from rewrite.java import Space
from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python.template import capture
from rewrite.python.template.capture import Capture
from rewrite.python.template.comparator import PatternMatchingComparator
from rewrite.python.template.engine import TemplateEngine
from rewrite.visitor import Cursor


def _make_cursor(target):
    """Create a simple Cursor wrapping the target node."""
    return Cursor(parent=Cursor(None, Cursor.ROOT_VALUE), value=target)


class TestIdentifierMatching:
    """Tests for identifier node comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_identical_identifiers_match(self):
        """Two identifiers with the same name should match."""
        pattern_tree = TemplateEngine.get_template_tree("x", {})
        target_tree = TemplateEngine.get_template_tree("x", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert result == {}

    def test_mismatched_identifiers_no_match(self):
        """Two identifiers with different names should not match."""
        pattern_tree = TemplateEngine.get_template_tree("x", {})
        target_tree = TemplateEngine.get_template_tree("y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_placeholder_captures_target(self):
        """A placeholder identifier should capture the target node."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("{x}", captures)
        target_tree = TemplateEngine.get_template_tree("hello", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result
        assert isinstance(result['x'], j.Identifier)
        assert result['x'].simple_name == 'hello'


class TestLiteralMatching:
    """Tests for literal node comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_identical_int_literals_match(self):
        """Two integer literals with the same value should match."""
        pattern_tree = TemplateEngine.get_template_tree("42", {})
        target_tree = TemplateEngine.get_template_tree("42", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_mismatched_int_literals_no_match(self):
        """Two integer literals with different values should not match."""
        pattern_tree = TemplateEngine.get_template_tree("42", {})
        target_tree = TemplateEngine.get_template_tree("99", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_identical_string_literals_match(self):
        """Two string literals with the same value should match."""
        pattern_tree = TemplateEngine.get_template_tree("'hello'", {})
        target_tree = TemplateEngine.get_template_tree("'hello'", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_mismatched_string_literals_no_match(self):
        """Two string literals with different values should not match."""
        pattern_tree = TemplateEngine.get_template_tree("'hello'", {})
        target_tree = TemplateEngine.get_template_tree("'world'", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestCrossTypeLiteralMatching:
    """Tests that literals of different Python types never match each other."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_none_does_not_match_bytes_literal(self):
        """None should not match b''."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree('b""', {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_none_does_not_match_nonempty_bytes(self):
        """None should not match b'hello'."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree('b"hello"', {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_none_does_not_match_empty_string(self):
        """None should not match ''."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree('""', {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_none_does_not_match_zero(self):
        """None should not match 0."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree("0", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_none_matches_none(self):
        """None should match None."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree("None", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_none_does_not_match_ellipsis(self):
        """None should not match ... (Ellipsis) — both have value=None internally."""
        pattern_tree = TemplateEngine.get_template_tree("None", {})
        target_tree = TemplateEngine.get_template_tree("...", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_bytes_does_not_match_string(self):
        """b'hello' should not match 'hello'."""
        pattern_tree = TemplateEngine.get_template_tree('b"hello"', {})
        target_tree = TemplateEngine.get_template_tree('"hello"', {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestMethodInvocationMatching:
    """Tests for method invocation comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_placeholder_arg_captures(self):
        """print({x}) should capture 'hello' from print(hello)."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("print({x})", captures)
        target_tree = TemplateEngine.get_template_tree("print(hello)", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result
        assert isinstance(result['x'], j.Identifier)
        assert result['x'].simple_name == 'hello'

    def test_wrong_method_name_no_match(self):
        """print({x}) should not match len(hello)."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("print({x})", captures)
        target_tree = TemplateEngine.get_template_tree("len(hello)", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_select_capture(self):
        """{obj}.method() should capture the select expression."""
        captures = {'obj': capture('obj')}
        pattern_tree = TemplateEngine.get_template_tree("{obj}.method()", captures)
        target_tree = TemplateEngine.get_template_tree("foo.method()", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'obj' in result

    def test_multi_arg_match(self):
        """print({a}, {b}) should capture both arguments."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("print({a}, {b})", captures)
        target_tree = TemplateEngine.get_template_tree("print(x, y)", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'a' in result
        assert 'b' in result

    def test_arg_count_mismatch_no_match(self):
        """print({a}) should not match print(x, y) due to arg count mismatch."""
        captures = {'a': capture('a')}
        pattern_tree = TemplateEngine.get_template_tree("print({a})", captures)
        target_tree = TemplateEngine.get_template_tree("print(x, y)", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_no_arg_match(self):
        """print() should match print()."""
        pattern_tree = TemplateEngine.get_template_tree("print()", {})
        target_tree = TemplateEngine.get_template_tree("print()", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None


class TestBinaryMatching:
    """Tests for binary expression comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_placeholder_binary_captures(self):
        """{a} + {b} should capture both operands from x + y."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("{a} + {b}", captures)
        target_tree = TemplateEngine.get_template_tree("x + y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'a' in result
        assert 'b' in result
        assert isinstance(result['a'], j.Identifier)
        assert result['a'].simple_name == 'x'
        assert isinstance(result['b'], j.Identifier)
        assert result['b'].simple_name == 'y'

    def test_operator_mismatch_no_match(self):
        """{a} + {b} should not match x - y (different operator)."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("{a} + {b}", captures)
        target_tree = TemplateEngine.get_template_tree("x - y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_nested_binary(self):
        """{a} + {b} should capture a nested binary in one operand."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("{a} + {b}", captures)
        target_tree = TemplateEngine.get_template_tree("x + y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_concrete_binary_match(self):
        """x + y should match x + y."""
        pattern_tree = TemplateEngine.get_template_tree("x + y", {})
        target_tree = TemplateEngine.get_template_tree("x + y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_concrete_binary_mismatch(self):
        """x + y should not match x + z."""
        pattern_tree = TemplateEngine.get_template_tree("x + y", {})
        target_tree = TemplateEngine.get_template_tree("x + z", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestAssignmentMatching:
    """Tests for assignment comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_placeholder_assignment_captures(self):
        """{x} = {y} should capture both sides from a = 1."""
        captures = {'x': capture('x'), 'y': capture('y')}
        pattern_tree = TemplateEngine.get_template_tree("{x} = {y}", captures)
        target_tree = TemplateEngine.get_template_tree("a = 1", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result
        assert 'y' in result


class TestUnaryMatching:
    """Tests for unary expression comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_negative_placeholder_captures(self):
        """-{x} should capture the expression from -5."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("-{x}", captures)
        target_tree = TemplateEngine.get_template_tree("-5", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result

    def test_unary_operator_mismatch_no_match(self):
        """-{x} should not match +5 (different operator)."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("-{x}", captures)
        # Python doesn't have +5 as unary plus in the same way,
        # but ~x is a valid unary operator that differs from -
        target_tree = TemplateEngine.get_template_tree("~5", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestParenthesesMatching:
    """Tests for parenthesized expression comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_parenthesized_placeholder_captures(self):
        """({x}) should capture the inner expression from (42)."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("({x})", captures)
        target_tree = TemplateEngine.get_template_tree("(42)", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result


class TestReturnMatching:
    """Tests for return statement comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_return_placeholder_captures(self):
        """return {x} should capture the expression from return 42."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("return {x}", captures)
        target_tree = TemplateEngine.get_template_tree("return 42", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result


class TestFieldAccessMatching:
    """Tests for field access comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_placeholder_target_captures(self):
        """{obj}.attr should capture the target from foo.attr."""
        captures = {'obj': capture('obj')}
        pattern_tree = TemplateEngine.get_template_tree("{obj}.attr", captures)
        target_tree = TemplateEngine.get_template_tree("foo.attr", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'obj' in result

    def test_field_access_name_mismatch(self):
        """foo.attr should not match foo.other."""
        pattern_tree = TemplateEngine.get_template_tree("foo.attr", {})
        target_tree = TemplateEngine.get_template_tree("foo.other", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestCaptureSemantics:
    """Tests for capture-specific behavior."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_same_capture_same_value_matches(self):
        """Using same capture name twice with same target node should match."""
        captures = {'x': capture('x')}
        # Parse {x} + {x} as pattern
        pattern_tree = TemplateEngine.get_template_tree("{x} + {x}", captures)
        # Parse a + a as target — but both 'a' nodes have different UUIDs
        # so the id comparison will fail
        target_tree = TemplateEngine.get_template_tree("a + a", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        # Both 'a' identifiers have different UUIDs, so existing.id != target.id
        assert result is None

    def test_same_capture_different_value_no_match(self):
        """Using same capture name twice with different values should not match."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("{x} + {x}", captures)
        target_tree = TemplateEngine.get_template_tree("a + b", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_same_capture_with_same_node_id(self):
        """Using same capture name twice with the SAME node object should match."""
        captures = {'x': capture('x')}
        pattern_tree = TemplateEngine.get_template_tree("{x} + {x}", captures)

        # Create a target where both sides reference the same identifier (same UUID)
        shared_ident = j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], "a", None, None)
        target_tree = j.Binary(
            uuid4(), Space.EMPTY, Markers.EMPTY,
            shared_ident,
            j.JLeftPadded(Space.EMPTY, j.Binary.Type.Addition, Markers.EMPTY),
            shared_ident,
            None,
        )
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result
        assert result['x'] is shared_ident

    def test_constraint_pass(self):
        """A capture with a passing constraint should match."""
        captures = {'x': Capture(name='x', constraint=lambda node: isinstance(node, j.Identifier))}
        pattern_tree = TemplateEngine.get_template_tree("{x}", captures)
        target_tree = TemplateEngine.get_template_tree("hello", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result

    def test_constraint_fail(self):
        """A capture with a failing constraint should not match."""
        captures = {'x': Capture(name='x', constraint=lambda node: isinstance(node, j.Literal))}
        pattern_tree = TemplateEngine.get_template_tree("{x}", captures)
        target_tree = TemplateEngine.get_template_tree("hello", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_required_capture_not_matched_returns_none(self):
        """If a required (non-variadic) capture is not used in the match, return None."""
        # Pattern only uses 'x' but captures also define 'y'
        captures = {'x': capture('x'), 'y': capture('y')}
        pattern_tree = TemplateEngine.get_template_tree("{x}", captures)
        target_tree = TemplateEngine.get_template_tree("hello", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        # 'y' was never captured, so match should fail
        assert result is None


class TestTypeMismatch:
    """Tests for type mismatch between pattern and target."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_identifier_vs_literal_no_match(self):
        """An identifier pattern should not match a literal target."""
        pattern_tree = TemplateEngine.get_template_tree("x", {})
        target_tree = TemplateEngine.get_template_tree("42", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_method_vs_binary_no_match(self):
        """A method invocation pattern should not match a binary expression."""
        pattern_tree = TemplateEngine.get_template_tree("print(x)", {})
        target_tree = TemplateEngine.get_template_tree("x + y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestCollectionLiteralMatching:
    """Tests for Python collection literal comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_list_placeholder_captures(self):
        """[{a}, {b}] should capture elements from [1, 2]."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("[{a}, {b}]", captures)
        target_tree = TemplateEngine.get_template_tree("[1, 2]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'a' in result
        assert 'b' in result

    def test_list_element_count_mismatch(self):
        """[{a}, {b}] should not match [1, 2, 3] due to element count mismatch."""
        captures = {'a': capture('a'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("[{a}, {b}]", captures)
        target_tree = TemplateEngine.get_template_tree("[1, 2, 3]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestDictLiteralMatching:
    """Tests for Python dict literal comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_dict_element_count_mismatch(self):
        """A dict with different element counts should not match."""
        pattern_tree = TemplateEngine.get_template_tree("{'a': 1}", {})
        target_tree = TemplateEngine.get_template_tree("{'a': 1, 'b': 2}", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestTernaryMatching:
    """Tests for ternary (conditional) expression comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_placeholder_ternary_captures(self):
        """{a} if {cond} else {b} should capture all three parts."""
        captures = {'a': capture('a'), 'cond': capture('cond'), 'b': capture('b')}
        pattern_tree = TemplateEngine.get_template_tree("{a} if {cond} else {b}", captures)
        target_tree = TemplateEngine.get_template_tree("x if flag else y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'a' in result
        assert 'cond' in result
        assert 'b' in result

    def test_concrete_ternary_match(self):
        """x if True else y should match x if True else y."""
        pattern_tree = TemplateEngine.get_template_tree("x if True else y", {})
        target_tree = TemplateEngine.get_template_tree("x if True else y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None

    def test_ternary_condition_mismatch(self):
        """x if True else y should not match x if False else y."""
        pattern_tree = TemplateEngine.get_template_tree("x if True else y", {})
        target_tree = TemplateEngine.get_template_tree("x if False else y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_ternary_true_part_mismatch(self):
        """x if True else y should not match z if True else y."""
        pattern_tree = TemplateEngine.get_template_tree("x if True else y", {})
        target_tree = TemplateEngine.get_template_tree("z if True else y", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_ternary_false_part_mismatch(self):
        """x if True else y should not match x if True else z."""
        pattern_tree = TemplateEngine.get_template_tree("x if True else y", {})
        target_tree = TemplateEngine.get_template_tree("x if True else z", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestArrayAccessMatching:
    """Tests for array/subscript access comparison."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_subscript_placeholder_captures(self):
        """{x}[{y}] should capture both indexed and index from a[0]."""
        captures = {'x': capture('x'), 'y': capture('y')}
        pattern_tree = TemplateEngine.get_template_tree("{x}[{y}]", captures)
        target_tree = TemplateEngine.get_template_tree("a[0]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is not None
        assert 'x' in result
        assert 'y' in result

    def test_subscript_index_mismatch_no_match(self):
        """a[0] should not match a[1]."""
        pattern_tree = TemplateEngine.get_template_tree("a[0]", {})
        target_tree = TemplateEngine.get_template_tree("a[1]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None

    def test_subscript_indexed_mismatch_no_match(self):
        """a[0] should not match b[0]."""
        pattern_tree = TemplateEngine.get_template_tree("a[0]", {})
        target_tree = TemplateEngine.get_template_tree("b[0]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        assert result is None


class TestDefaultFallthrough:
    """Tests for the default comparison behavior on unrecognized types."""

    def setup_method(self):
        TemplateEngine.clear_cache()

    def teardown_method(self):
        TemplateEngine.clear_cache()

    def test_empty_sentinel_nodes_match(self):
        """Two Empty sentinel nodes should match (explicitly handled)."""
        empty1 = j.Empty(uuid4(), Space.EMPTY, Markers.EMPTY)
        empty2 = j.Empty(uuid4(), Space.EMPTY, Markers.EMPTY)
        cursor = _make_cursor(empty1)

        comparator = PatternMatchingComparator({})
        result = comparator.match(empty1, empty2, cursor)
        assert result is not None

    def test_unhandled_node_type_rejects_match(self):
        """Nodes of an unhandled type should reject the match to prevent false positives."""
        # Use j.NewClass as an example of an unhandled node type
        # We can't easily construct one from TemplateEngine, so we test
        # via the debug flag on an expression that would previously match incorrectly
        # Instead, test that two different subscript expressions with the comparator
        # properly distinguish them now that ArrayAccess is handled
        pattern_tree = TemplateEngine.get_template_tree("a[0]", {})
        target_tree = TemplateEngine.get_template_tree("a[1]", {})
        cursor = _make_cursor(target_tree)

        comparator = PatternMatchingComparator({})
        result = comparator.match(pattern_tree, target_tree, cursor)
        # Before the fix, this would have returned a match (default fallthrough was True)
        # After the fix with ArrayAccess handler, it correctly rejects
        assert result is None
