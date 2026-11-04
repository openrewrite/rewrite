from rewrite.test import RecipeSpec, python


def test_break():
    RecipeSpec().rewrite_run(python("break"))
