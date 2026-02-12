"""Tests for t-string parsing (Python 3.14+).

These tests verify that the parser correctly handles t-string syntax
by round-tripping through parse -> print. No actual t-string syntax
appears in the test file; source code is passed as plain strings.
"""
import sys

import pytest

from rewrite.test import RecipeSpec, python

pytestmark = pytest.mark.skipif(
    sys.version_info < (3, 14),
    reason="t-strings require Python 3.14+",
)


def test_simple_tstring():
    RecipeSpec().rewrite_run(python('a = t"hello"'))


def test_tstring_with_interpolation():
    RecipeSpec().rewrite_run(python('a = t"hello {name}"'))


def test_tstring_with_conversion_r():
    RecipeSpec().rewrite_run(python('a = t"{name!r}"'))


def test_tstring_with_conversion_s():
    RecipeSpec().rewrite_run(python('a = t"{name!s}"'))


def test_tstring_with_conversion_a():
    RecipeSpec().rewrite_run(python('a = t"{name!a}"'))


def test_tstring_with_format_spec():
    RecipeSpec().rewrite_run(python('a = t"{value:.2f}"'))


def test_tstring_with_debug():
    RecipeSpec().rewrite_run(python('a = t"{expr=}"'))


def test_tstring_triple_quoted():
    RecipeSpec().rewrite_run(python(
        'a = t"""multi\nline"""'
    ))


def test_tstring_single_quotes():
    RecipeSpec().rewrite_run(python("a = t'hello'"))


@pytest.mark.parametrize('style', ["'", '"', '"""', "'''"])
def test_tstring_delimiters(style: str):
    RecipeSpec().rewrite_run(python(f"a = t{style}foo{style}"))


def test_tstring_concatenation():
    RecipeSpec().rewrite_run(python('a = t"hello" t" world"'))


def test_tstring_with_expression():
    RecipeSpec().rewrite_run(python('a = t"{1 + 2}"'))


def test_tstring_with_nested_braces():
    RecipeSpec().rewrite_run(python('a = t"{{escaped}}"'))


def test_tstring_empty():
    RecipeSpec().rewrite_run(python('a = t""'))


def test_tstring_with_debug_and_conversion():
    RecipeSpec().rewrite_run(python('a = t"{expr=!r}"'))


def test_tstring_with_debug_and_format():
    RecipeSpec().rewrite_run(python('a = t"{expr=:.2f}"'))


def test_tstring_multiline_expression():
    RecipeSpec().rewrite_run(python("""
a = t"result: {
    1 + 2
}"
"""))
