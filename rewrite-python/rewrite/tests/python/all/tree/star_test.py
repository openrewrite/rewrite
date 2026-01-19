from rewrite.test import RecipeSpec, python


def test_list():
    # language=python
    RecipeSpec().rewrite_run(python("l = [*[1], 2]"))

def test_dict():
    # language=python
    RecipeSpec().rewrite_run(python("d = {**{'x':1}, 'y':2}"))
