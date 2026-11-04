from rewrite.test import RecipeSpec, python


def test_formatting():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        assert \\
        True \\
        ,\\
        'foo'
        """,
    ),)


def test_with_message():
    # language=python
    RecipeSpec().rewrite_run(python("assert True, 'foo'"))


def test_assert():
    # language=python
    RecipeSpec().rewrite_run(python("assert True"))
