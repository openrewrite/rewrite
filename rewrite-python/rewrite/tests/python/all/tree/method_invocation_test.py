import pytest

from rewrite.test import RecipeSpec, python


def test_no_select():
    # language=python
    RecipeSpec().rewrite_run(python("assert len('a')"))


def test_select():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'a'.islower( )"))


def test_invoke_function_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert a(0)(1)"))


def test_qualified_receiver_with_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        class Foo:
            def getter(self, row):
                pass
            def foo(self):
                return (self.getter )(1)
        """
    ))


def test_invoke_lambda_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert (lambda x: x)(1)"))


def test_invoke_array_access_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert a[0](1)"))


def test_sequence_unpack():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        td = datetime.timedelta(*(5, 12, 30, 0))
        """
    ))


def test_mixed_order():
    # language=python
    RecipeSpec().rewrite_run(python("set_stroke(color=color, width=width, *args, **kwargs)"))


def test_named_arguments():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        td = datetime.datetime(year=2024, month= 1, day =1)
        """
    ))


def test_dict_unpack():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        dt = datetime.datetime(**{'year': 2023, 'month': 10, 'day': 1, 'hour': 12, 'minute': 30})
        """
    ))


def test_keyword_argument():
    # language=python
    RecipeSpec().rewrite_run(python("l = sorted([1, 2, 3], key=lambda x: x, reverse=True)"))


def test_no_name():
    # language=python
    RecipeSpec().rewrite_run(python("v = (a)()"))
