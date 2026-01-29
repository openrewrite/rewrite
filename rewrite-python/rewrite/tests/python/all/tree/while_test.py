from rewrite.test import RecipeSpec, python


def test_while():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(i):
            while i < 6:
                i += 1
        """
    ))


def test_while_else():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(i):
            while i < 6:
                i += 1
            else:
                i = 10
        """
    ))
