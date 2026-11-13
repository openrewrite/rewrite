from rewrite.test import RecipeSpec, python


# noinspection PyCompatibility
def test_with_parentheses():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        try:
            foo()
        except* Exception:
            pass
        """
    ))


# noinspection PyCompatibility
def test_space_before_the_star():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        try:
            foo()
        except *TypeError as e:
            pass
        except  *  (ValueError, KeyError):
            pass
        """
    ))
