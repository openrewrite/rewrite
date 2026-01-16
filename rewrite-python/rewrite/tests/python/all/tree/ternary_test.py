from rewrite.test import RecipeSpec, python


def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python("assert True if True else False"))
