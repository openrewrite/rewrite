"""Tests for t-string support in template() and pattern() (Python 3.14+)."""

import pytest

from rewrite.python.template import template, pattern, capture, raw


# -- template() with t-strings --

class TestTemplateTstring:

    def test_no_captures(self):
        tmpl = template(t"x + 1")
        assert tmpl.code == "x + 1"
        assert tmpl.captures == {}

    def test_single_capture(self):
        expr = capture('expr')
        tmpl = template(t"print({expr})")
        assert tmpl.code == "print({expr})"
        assert tmpl.captures == {'expr': expr}

    def test_multiple_captures(self):
        a = capture('a')
        b = capture('b')
        tmpl = template(t"{a} + {b}")
        assert tmpl.code == "{a} + {b}"
        assert tmpl.captures == {'a': a, 'b': b}

    def test_raw_code_spliced(self):
        method = raw("warn")
        tmpl = template(t"logger.{method}(msg)")
        assert tmpl.code == "logger.warn(msg)"
        assert tmpl.captures == {}

    def test_mixed_capture_and_raw(self):
        method = raw("info")
        expr = capture('expr')
        tmpl = template(t"logger.{method}({expr})")
        assert tmpl.code == "logger.info({expr})"
        assert tmpl.captures == {'expr': expr}

    def test_non_capture_interpolation_raises(self):
        with pytest.raises(TypeError, match="must be Capture or RawCode"):
            template(t"print({42})")

    def test_kwargs_with_tstring_raises(self):
        expr = capture('expr')
        with pytest.raises(TypeError, match="Cannot pass keyword captures"):
            template(t"print({expr})", expr=expr)

    def test_equivalent_to_string_form(self):
        expr = capture('expr')
        t1 = template(t"print({expr})")
        t2 = template("print({expr})", expr=expr)
        assert t1.code == t2.code
        assert t1.captures == t2.captures

    def test_with_imports(self):
        expr = capture('expr')
        tmpl = template(t"datetime.now() + {expr}", imports=["from datetime import datetime"])
        assert tmpl.code == "datetime.now() + {expr}"
        assert tmpl.captures == {'expr': expr}


# -- pattern() with t-strings --

class TestPatternTstring:

    def test_no_captures(self):
        pat = pattern(t"x + 1")
        assert pat.code == "x + 1"
        assert pat.captures == {}

    def test_single_capture(self):
        expr = capture('expr')
        pat = pattern(t"print({expr})")
        assert pat.code == "print({expr})"
        assert pat.captures == {'expr': expr}

    def test_multiple_captures(self):
        a = capture('a')
        b = capture('b')
        pat = pattern(t"{a} + {b}")
        assert pat.code == "{a} + {b}"
        assert pat.captures == {'a': a, 'b': b}

    def test_raw_code_spliced(self):
        method = raw("warn")
        pat = pattern(t"logger.{method}(msg)")
        assert pat.code == "logger.warn(msg)"
        assert pat.captures == {}

    def test_mixed_capture_and_raw(self):
        method = raw("info")
        expr = capture('expr')
        pat = pattern(t"logger.{method}({expr})")
        assert pat.code == "logger.info({expr})"
        assert pat.captures == {'expr': expr}

    def test_non_capture_interpolation_raises(self):
        with pytest.raises(TypeError, match="must be Capture or RawCode"):
            pattern(t"print({42})")

    def test_kwargs_with_tstring_raises(self):
        expr = capture('expr')
        with pytest.raises(TypeError, match="Cannot pass keyword captures"):
            pattern(t"print({expr})", expr=expr)

    def test_equivalent_to_string_form(self):
        expr = capture('expr')
        p1 = pattern(t"print({expr})")
        p2 = pattern("print({expr})", expr=expr)
        assert p1.code == p2.code
        assert p1.captures == p2.captures
