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


def test_class_type_param_with_constraint_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T: (int, str)]:
            pass
        """
    ))


def test_bound_is_modelled_as_a_type():
    bounds = []

    def collect(source_file):
        bounds.append(type(source_file.statements[0].type_parameters[0].bounds[0]).__name__)

    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T: list[int]]:
            pass
        """,
        after_recipe=collect,
    ))
    assert bounds[-1] == "ParameterizedType"

    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T: int | str]:
            pass
        """,
        after_recipe=collect,
    ))
    assert bounds[-1] == "UnionType"


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
