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

"""Tests for capture module."""

import pytest

from rewrite.python.template import capture, raw, Capture, RawCode


class TestCapture:
    """Tests for Capture class and capture() factory function."""

    def test_simple_capture(self):
        """Test creating a simple capture."""
        cap = capture('x')
        assert cap.name == 'x'
        assert cap.variadic is False
        assert cap.constraint is None
        assert cap.type_hint is None

    def test_variadic_capture(self):
        """Test creating a variadic capture."""
        cap = capture('args', variadic=True)
        assert cap.name == 'args'
        assert cap.variadic is True

    def test_capture_with_constraint(self):
        """Test creating a capture with a constraint."""
        def is_positive(n):
            return n > 0

        cap = capture('n', constraint=is_positive)
        assert cap.name == 'n'
        assert cap.constraint is is_positive

    def test_capture_with_type_hint(self):
        """Test creating a capture with a type hint."""
        cap = capture('expr', type_hint='int')
        assert cap.name == 'expr'
        assert cap.type_hint == 'int'

    def test_capture_with_variadic_bounds(self):
        """Test creating a variadic capture with bounds."""
        cap = capture('args', variadic=True, min_count=1, max_count=5)
        assert cap.variadic is True
        assert cap.min_count == 1
        assert cap.max_count == 5

    def test_capture_equality(self):
        """Test capture equality."""
        cap1 = capture('x')
        cap2 = capture('x')
        cap3 = capture('y')

        assert cap1 == cap2
        assert cap1 != cap3

    def test_capture_hashable(self):
        """Test that captures can be used as dict keys."""
        cap1 = capture('x')
        cap2 = capture('y')

        d = {cap1: 'value1', cap2: 'value2'}
        assert d[cap1] == 'value1'
        assert d[cap2] == 'value2'


class TestRawCode:
    """Tests for RawCode class and raw() factory function."""

    def test_simple_raw(self):
        """Test creating raw code."""
        r = raw('method_name')
        assert r.code == 'method_name'

    def test_raw_with_operator(self):
        """Test raw code with an operator."""
        r = raw('>=')
        assert r.code == '>='

    def test_raw_equality(self):
        """Test raw code equality."""
        r1 = raw('x')
        r2 = raw('x')
        r3 = raw('y')

        assert r1 == r2
        assert r1 != r3
