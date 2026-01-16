import pytest

from rewrite.test import RecipeSpec, python


def test_bool_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert True or False"))
    # language=python
    RecipeSpec().rewrite_run(python("assert True and False"))


def test_arithmetic_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 + 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 - 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 * 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 / 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 % 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 // 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 ** 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert [[1]] @ [[2]]"))


def test_eq_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 == 1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 != 2"))


def test_right_with_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        assert index > 0 and (token.lineno, token.column) >= pos
        """
    ))


def test_in():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 in [1]"))


def test_not_in():
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 not in [1]"))


def test_is():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is 1"))


def test_isnot():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is not 2"))


def test_comparison_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 < 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 <= 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 > 1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 >= 1"))


def test_chained_comparison():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 < 2 <= 3  >=0"))


def test_chained_comparison_2():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is not 2 is not 3"))


def test_multiline_tuple_comparison():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        (
        True
        , False) is not None
        """,
    ),)
