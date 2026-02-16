from rewrite.test import RecipeSpec, python


# PEP 701 — f-string syntax features requiring Python 3.12+
# (nested quote reuse, backslashes in expressions, comments in expressions)

def test_concat_fstring_nested_quotes():
    # language=python
    RecipeSpec().rewrite_run(python("""
        _ = f"b {f"c" f"d {f"e" f"f"} g"} h"
        """
           ))


def test_fstring_backslash_in_expr():
    # language=python
    RecipeSpec().rewrite_run(python('_ = f"n{\' \':{1}}Groups"'))


def test_comment_in_expr():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        f"abc{a # This is a comment }
        + 3}"
        """
    ))


def test_nested_fstring_with_format_value():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{f"{'foo'}":>{2*3}}'"""))
