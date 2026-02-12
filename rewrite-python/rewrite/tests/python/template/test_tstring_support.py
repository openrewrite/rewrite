"""Tests for _tstring_support module using mock t-string objects.

These tests run on any Python version by simulating the t-string
Template structure that Python 3.14+ provides natively.
"""

import pytest

from rewrite.python.template._tstring_support import is_tstring, convert_tstring
from rewrite.python.template import capture, raw


class MockInterpolation:
    """Simulates a string.templatelib.Interpolation."""

    def __init__(self, value, expression=""):
        self.value = value
        self.expression = expression


class MockTemplate:
    """Simulates a string.templatelib.Template with .args iterable."""

    def __init__(self, *args):
        self.args = args


class TestIsTstring:

    def test_plain_string_is_not_tstring(self):
        assert is_tstring("hello") is False

    def test_none_is_not_tstring(self):
        assert is_tstring(None) is False

    def test_int_is_not_tstring(self):
        assert is_tstring(42) is False

    def test_mock_template_is_not_tstring(self):
        # MockTemplate is not the real string.templatelib.Template,
        # so is_tstring should return False (unless on Python 3.14+
        # where it would match the real type)
        assert is_tstring(MockTemplate("hello")) is False


class TestConvertTstring:

    def test_static_only(self):
        tpl = MockTemplate("x + 1")
        code, captures = convert_tstring(tpl)
        assert code == "x + 1"
        assert captures == {}

    def test_single_capture(self):
        expr = capture('expr')
        tpl = MockTemplate("print(", MockInterpolation(expr, "expr"), ")")
        code, captures = convert_tstring(tpl)
        assert code == "print({expr})"
        assert captures == {'expr': expr}

    def test_multiple_captures(self):
        a = capture('a')
        b = capture('b')
        tpl = MockTemplate("", MockInterpolation(a, "a"), " + ", MockInterpolation(b, "b"), "")
        code, captures = convert_tstring(tpl)
        assert code == "{a} + {b}"
        assert captures == {'a': a, 'b': b}

    def test_raw_code_spliced(self):
        method = raw("warn")
        tpl = MockTemplate("logger.", MockInterpolation(method, "method"), "(msg)")
        code, captures = convert_tstring(tpl)
        assert code == "logger.warn(msg)"
        assert captures == {}

    def test_mixed_capture_and_raw(self):
        method = raw("info")
        expr = capture('expr')
        tpl = MockTemplate(
            "logger.", MockInterpolation(method, "method"),
            "(", MockInterpolation(expr, "expr"), ")"
        )
        code, captures = convert_tstring(tpl)
        assert code == "logger.info({expr})"
        assert captures == {'expr': expr}

    def test_non_capture_interpolation_raises(self):
        tpl = MockTemplate("print(", MockInterpolation(42, "42"), ")")
        with pytest.raises(TypeError, match="must be Capture or RawCode"):
            convert_tstring(tpl)

    def test_capture_name_from_capture_object(self):
        # Variable name doesn't matter; Capture.name is used
        my_var = capture('x')
        tpl = MockTemplate("f(", MockInterpolation(my_var, "my_var"), ")")
        code, captures = convert_tstring(tpl)
        assert code == "f({x})"
        assert captures == {'x': my_var}
