from rewrite.test import RecipeSpec, python

#language=python
def test_list():
    RecipeSpec().rewrite_run(python("a = [1, 2, 3]"))
