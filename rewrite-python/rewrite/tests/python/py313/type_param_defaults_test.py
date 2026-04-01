from rewrite.test import RecipeSpec, python


# PEP 696 — Type parameter defaults (Python 3.13+)

def test_class_type_param_with_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T = int]:
            pass
        """
    ))


def test_class_type_param_with_bound_and_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[T: str = "default"]:
            pass
        """
    ))


def test_class_param_spec_with_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[**P = [int, str]]:
            pass
        """
    ))


def test_class_type_var_tuple_with_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo[*Ts = *tuple[int, ...]]:
            pass
        """
    ))


def test_function_type_param_with_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo[T = int]():
            pass
        """
    ))


def test_type_alias_with_default():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Alias[T = int] = list[T]
        """
    ))
