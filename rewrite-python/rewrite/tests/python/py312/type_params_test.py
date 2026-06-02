from rewrite.test import RecipeSpec, python


def test_class_simple_type_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T]:
            pass
        """
    ))


def test_class_type_param_with_bound():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T: int]:
            pass
        """
    ))


def test_class_type_var_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[*Ts]:
            pass
        """
    ))


def test_class_param_spec():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[**P]:
            pass
        """
    ))


def test_class_multiple_type_params():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T, U, V]:
            pass
        """
    ))


def test_class_type_params_with_base():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T](Bar):
            pass
        """
    ))


def test_function_type_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo[T](x: T) -> T:
            return x
        """
    ))


def test_function_type_param_with_bound():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo[T: int](x: T) -> T:
            return x
        """
    ))
