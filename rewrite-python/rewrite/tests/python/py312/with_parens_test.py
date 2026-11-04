import pytest

from rewrite.test import RecipeSpec, python


# noinspection PyCompatibility
def test_with_parentheses():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        with (open('/dev/null') as x):
            pass
        """
    ))
