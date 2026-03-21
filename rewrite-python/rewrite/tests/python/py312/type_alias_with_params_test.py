from rewrite.test import RecipeSpec, python


def test_type_alias_simple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo = int
        """
    ))


def test_type_alias_with_type_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T] = list[T]
        """
    ))


def test_type_alias_with_multiple_type_params():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T, U] = dict[T, U]
        """
    ))


def test_type_alias_with_bound():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T: int] = list[T]
        """
    ))
