import pytest

from rewrite.test import RecipeSpec, python


def test_bool_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert not True"))


def test_arithmetic_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert +1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert -1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert ~1"))
