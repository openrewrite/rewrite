from rewrite.test import RecipeSpec, python


def test_generator_in_call():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        even_nums = list(2 * x for x in range(3))
        """
    ))


def test_generator_in_parens_in_call():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        list((2 * x for x in range(3)))
        """
    ))


def test_generator_in_double_parens_in_call():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        list(((2 * x for x in range(3))))
        """
    ))


def test_generator_in_triple_parens_in_call():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        list((((2 * x for x in range(3)))))
        """
    ))


def test_generator_with_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        list((0 for _ in []),)
        """
    ))
