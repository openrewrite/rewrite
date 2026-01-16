from rewrite.test import RecipeSpec, python


def test_empty():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo:
            pass
        """
    ))


def test_field():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo:
            _f = 0
        """
    ))


def test_typed_field():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo:
            def __init__(self):
                self.target: int
        """
    ))


def test_enum():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from enum import Enum
        class Foo(Enum):
            '''Foo'''
            Foo = 'Foo'
            '''Bar'''
            Bar = 'Bar'
        """
    ))


def test_single_base():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import abc
        class Foo (abc.ABC) :
            pass
        """
    ))


def test_two_bases():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import abc
        class Foo(abc.ABC, abc.ABC,):
            pass
        """
    ))


def test_empty_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Foo (  ):
            pass
        """
    ))


def test_bases_via_call():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def fun():
            return []
    
        class Derived(fun()):
            pass
        """
    ))


def test_metaclass():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import Type
        class Derived(metaclass=Type):
            pass
        """
    ))
