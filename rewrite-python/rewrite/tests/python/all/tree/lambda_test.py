from rewrite.test import RecipeSpec, python


def test_no_parameters():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda: None"))


def test_single_parameter():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x: x"))


def test_multiple_parameter():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x, y: x + y"))


def test_parameters_with_defaults():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x, y=0: x + y"))


def test_parameters_with_defaults_2():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda self, v, n=n: 1'))


def test_positional_only():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, /, b: ...'))


def test_positional_only_last():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a=1, /: ...'))


def test_positional_only_last_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, /,: ...'))


def test_keyword_only():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda kw=1, *, a: ...'))


def test_complex():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, b=20, /, c=30: 1'))


def test_multiple_complex():
    # language=python
    RecipeSpec().rewrite_run(python('''\
        lambda a, b=20, /, c=30: 1
        lambda a, b, /, c, *, d, e: 0
    '''))
