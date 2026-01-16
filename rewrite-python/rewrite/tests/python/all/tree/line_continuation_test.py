from rewrite.test import RecipeSpec, python


def test_docstring_with_line_continuation():
    # language=python
    RecipeSpec().rewrite_run(python(
        '''\
"""Hello, world!"""\\

x = 1
'''
    ))
