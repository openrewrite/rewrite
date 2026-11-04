from rewrite.test import RecipeSpec, python


# NOTE: This was added in Python 3.10
# noinspection PyUnresolvedReferences
def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python("foo: int | str = 42"))
