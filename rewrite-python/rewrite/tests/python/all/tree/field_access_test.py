from rewrite.test import RecipeSpec, python


# noinspection PyUnresolvedReferences
def test_attribute():
    # language=python
    RecipeSpec().rewrite_run(python("a = foo.bar"))


# noinspection PyUnresolvedReferences
def test_nested_attribute():
    # language=python
    RecipeSpec().rewrite_run(python("a = foo.bar.baz"))
