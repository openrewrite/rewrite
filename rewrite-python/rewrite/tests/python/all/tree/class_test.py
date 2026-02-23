import shutil

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Assignment
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


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


def test_starred_base():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C3(Generic[T], metaclass=type, *[str]):
            ...
        """
    ))


@requires_ty_cli
def test_class_instance_type_attribution():
    """Verify that x = Foo() assigns a type with fqn 'Foo'."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                if assignment.type is None:
                    errors.append("Assignment.type is None for Foo()")
                elif isinstance(assignment.type, JavaType.Class):
                    if assignment.type._fully_qualified_name != 'Foo':
                        errors.append(f"Assignment.type fqn is '{assignment.type._fully_qualified_name}', expected 'Foo'")
                else:
                    # Accept any non-None type
                    pass
                return assignment

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        class Foo:
            pass
        x = Foo()
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
