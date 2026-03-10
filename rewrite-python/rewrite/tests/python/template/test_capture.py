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
from rewrite.python.template._fstring_support import clear_registry, collect_captures


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


class TestRawCodeFormat:
    """Tests for RawCode.__format__."""

    def test_fstring_splice(self):
        """RawCode splices directly into an f-string."""
        r = raw('warn')
        result = f"logger.{r}(msg)"
        assert result == "logger.warn(msg)"

    def test_format_spec_rejected(self):
        """Format specs are not allowed."""
        r = raw('x')
        with pytest.raises(ValueError, match="format specs"):
            format(r, '.2f')


class TestCaptureFormat:
    """Tests for Capture.__format__ and auto-registration."""

    def setup_method(self):
        """Clear the registry before each test."""
        clear_registry()

    def test_fstring_produces_template_code(self):
        """f-string with Capture produces internal placeholder identifier."""
        expr = capture('expr')
        result = f"print({expr})"
        assert result == "print(__plh_expr__)"

    def test_format_spec_rejected(self):
        """Format specs are not allowed on Capture."""
        cap = capture('x')
        with pytest.raises(ValueError, match="format specs"):
            format(cap, '>10')

    def test_auto_registration(self):
        """Formatting a Capture registers it in the contextvars registry."""
        cap = capture('expr')
        code = f"print({cap})"  # triggers __format__
        collected = collect_captures(code)
        assert 'expr' in collected
        assert collected['expr'] is cap

    def test_registration_only_matching(self):
        """collect_captures only returns captures whose placeholder appears in the code."""
        a = capture('a')
        b = capture('b')
        f"{a} + {b}"
        # Only collect 'a' — 'b' is not in the code string
        collected = collect_captures("__plh_a__ + x")
        assert 'a' in collected
        assert 'b' not in collected


class TestCaptureOptionalName:
    """Tests for optional name in capture()."""

    def test_auto_generated_name(self):
        """capture() without name generates an auto name."""
        cap = capture()
        assert cap.name.startswith('_capture_')

    def test_auto_names_are_unique(self):
        """Each call to capture() generates a different name."""
        c1 = capture()
        c2 = capture()
        assert c1.name != c2.name


class TestFstringIntegration:
    """Integration tests for f-string support with template() and pattern()."""

    def setup_method(self):
        """Clear the registry before each test."""
        clear_registry()

    def test_template_with_fstring(self):
        """template(f"...{cap}...") picks up auto-registered capture."""
        from rewrite.python.template import template

        expr = capture('expr')
        tmpl = template(f"print({expr})")
        assert tmpl.code == "print(__plh_expr__)"
        assert 'expr' in tmpl.captures
        assert tmpl.captures['expr'] is expr

    def test_pattern_with_fstring(self):
        """pattern(f"...{cap}...") picks up auto-registered capture."""
        from rewrite.python.template import pattern

        expr = capture('expr')
        pat = pattern(f"print({expr})")
        assert pat.code == "print(__plh_expr__)"
        assert 'expr' in pat.captures
        assert pat.captures['expr'] is expr

    def test_unnamed_capture_fstring(self):
        """Unnamed captures work with f-strings."""
        from rewrite.python.template import template

        expr = capture()
        tmpl = template(f"print({expr})")
        assert expr.name in tmpl.captures
        assert tmpl.captures[expr.name] is expr

    def test_mixed_rawcode_and_capture(self):
        """RawCode and Capture can be mixed in a single f-string."""
        from rewrite.python.template import template

        method = raw('warn')
        expr = capture('expr')
        tmpl = template(f"logger.{method}({expr})")
        assert tmpl.code == "logger.warn(__plh_expr__)"
        assert 'expr' in tmpl.captures
        assert tmpl.captures['expr'] is expr

    def test_explicit_kwargs_override(self):
        """Explicit kwargs take priority over auto-registered captures."""
        from rewrite.python.template import template

        expr = capture('expr')
        expr2 = capture('expr', variadic=True)
        # f-string registers expr, but explicit kwarg overrides
        tmpl = template(f"print({expr})", expr=expr2)
        assert tmpl.captures['expr'] is expr2
        assert tmpl.captures['expr'].variadic is True

    def test_stale_entries_cleared_by_template(self):
        """Stale registry entries are cleared when template() is called."""
        from rewrite.python.template import template

        stale = capture('stale')
        f"unused {stale}"  # registers but never consumed

        # Next template() call should clear the registry
        tmpl = template("print(x)")
        assert 'stale' not in tmpl.captures

        # Registry should be empty now
        assert collect_captures("__plh_stale__") == {}
