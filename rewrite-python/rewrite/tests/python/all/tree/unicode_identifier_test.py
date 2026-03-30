from rewrite.test import RecipeSpec, python


def test_unicode_identifier_simple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        π = 3.14
        """
    ))


def test_unicode_import_alias():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from numpy import pi as π
        """
    ))


def test_unicode_multiple_imports():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from astropy.constants import hbar as ℏ
        from numpy import pi as π
        """
    ))


def test_unicode_expression():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        π = 3.14
        ℏ = 1.05e-34
        h = 2 * π * ℏ
        """
    ))
