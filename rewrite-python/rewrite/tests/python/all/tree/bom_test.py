from rewrite.test import RecipeSpec, python


def test_file_with_bom():
    # language=python
    RecipeSpec().rewrite_run(python(
        "\ufeffx = 1\n"
    ))


def test_file_with_bom_and_comment():
    # language=python
    RecipeSpec().rewrite_run(python(
        "\ufeff# comment\nx = 1\n"
    ))
