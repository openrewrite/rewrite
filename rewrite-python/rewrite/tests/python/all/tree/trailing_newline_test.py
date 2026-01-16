from rewrite.test import RecipeSpec, python


def test_trailing_newline():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass
        """
    ))


def test_trailing_blank_line():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass

        """
    ))


def test_multiple_trailing_blank_lines():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass


        """
    ))


def test_trailing_blank_line_after_method():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Class:
            def __init__(self):
                print('hello')

        """
    ))


def test_trailing_blank_line_with_fstring_debug():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Class:
            def __init__(self):
                print(f"{self.attr=}")

        """
    ))
