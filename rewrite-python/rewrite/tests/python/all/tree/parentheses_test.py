from rewrite.test import RecipeSpec, python


def test_single():
    # language=python
    RecipeSpec().rewrite_run(python("assert (True)"))


def test_left():
    # language=python
    RecipeSpec().rewrite_run(python("assert (True) or False"))


def test_double_1():
    # language=python
    RecipeSpec().rewrite_run(python("assert ((True))"))


def test_double_2():
    # language=python
    RecipeSpec().rewrite_run(python("((5)) * 8"))


def test_triple():
    # language=python
    RecipeSpec().rewrite_run(python("1 ^ (((5)) // 8)"))


def test_nested_1():
    # language=python
    RecipeSpec().rewrite_run(python("assert ((True) or False)"))


def test_nested_2():
    # language=python
    RecipeSpec().rewrite_run(python("assert (True or (False))"))


def test_nested_spaces():
    # language=python
    RecipeSpec().rewrite_run(python("assert (  True or ( False ) )"))


def test_nested_in_field_access_select():
    # language=python
    RecipeSpec().rewrite_run(python("area_a = ((0 + 0) + 1).real"))


def test_multiline():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        b = (
            True
        ) or False
        """
    ))
