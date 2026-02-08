import pytest

from rewrite.test import RecipeSpec, python


def test_async_def():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        async def main():
            pass
        """
    ))
