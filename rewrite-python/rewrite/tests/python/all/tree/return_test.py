from rewrite.test import RecipeSpec, python


def test_empty():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        def foo():
            return
        """
    ))


def test_trailing_semicolon():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        def foo():
            return;
        """
    ))


def test_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        def foo():
            return 1, 2, 3
        """
    ))


def test_value():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        def foo():
            return 1
        """
    ))
