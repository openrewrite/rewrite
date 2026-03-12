from rewrite.test import RecipeSpec, python


def test_await():
    # language=python
    RecipeSpec().rewrite_run(python("a = await 1"))
