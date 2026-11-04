from rewrite.test import RecipeSpec, python


def test_del():
    # language=python
    RecipeSpec().rewrite_run(python("del a"))
