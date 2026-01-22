from rewrite.test import RecipeSpec, python


def test_continue():
    RecipeSpec().rewrite_run(python("continue"))
