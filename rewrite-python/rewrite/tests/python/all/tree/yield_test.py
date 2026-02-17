from rewrite.test import RecipeSpec, python


def test_yield():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test():
            yield 1
        """
    ))


def test_empty_yield():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test():
            yield
        """
    ))


def test_yield_from():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test():
            yield from range(5)
        """
    ))
