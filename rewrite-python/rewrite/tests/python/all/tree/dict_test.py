from rewrite.test import RecipeSpec, python


def test_empty():
    # language=python
    RecipeSpec().rewrite_run(python("d = { }"))


def test_single():
    # language=python
    RecipeSpec().rewrite_run(python("d = {'x' :   1 }"))


def test_multiple():
    # language=python
    RecipeSpec().rewrite_run(python("d = {'x':1 , 'y':2 }"))


def test_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("d = {'x':1 , }"))
