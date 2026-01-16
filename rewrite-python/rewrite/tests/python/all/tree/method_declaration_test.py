from rewrite.test import RecipeSpec, python


def test_whitespace_before_colon():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo() :
            pass
        """
    ))


def test_varargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(*args) :
            pass
        """
    ))


def test_function_containing_async_identifier():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def async_foo():
            pass
        """
    ))


def test_kwargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x, **kwargs) :
            pass
        """
    ))


# https://peps.python.org/pep-3102/
def test_keyword_only_args():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def func(x, *, kwarg1, kwarg2):
            pass
        """
    ))


def test_kwonlyargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(foo, *, bar) :
            pass
        """
    ))


def test_all():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def retry_until_timeout(cb_fn, *args, timeout=4, **kwargs):
            pass
        """
    ))


def test_trailing_comma_on_kwonly_arg():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def play(
            *proto_animations,
            lag_ratio,
        ):
            pass
        """
    ))


def test_varargs_and_kwonlyargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(foo, *bar, baz, qux=True) :
            pass
        """
    ))


def test_kwargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(**kwargs) :
            pass
        """
    ))


def test_param_type_with_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import Tuple
        def foo(i: (Tuple[int])):
            pass
        """
    ))


def test_one_line():
    # language=python
    RecipeSpec().rewrite_run(python("def f(x): x = x + 1; return x"))


def test_line_break_after_last_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def f(
            x = 0,
        ):
            return x
        """
    ))
