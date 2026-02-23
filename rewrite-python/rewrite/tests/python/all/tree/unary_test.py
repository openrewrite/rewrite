import shutil

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Unary
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


def test_bool_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert not True"))


def test_arithmetic_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert +1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert -1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert ~1"))


@requires_ty_cli
def test_not_type_attribution():
    """Verify that 'not True' has type Boolean."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_unary(self, unary, p):
                if not isinstance(unary, Unary):
                    return unary
                if unary.operator != Unary.Type.Not:
                    return unary
                if unary.type is None:
                    errors.append("Unary(Not).type is None")
                elif unary.type != JavaType.Primitive.Boolean:
                    errors.append(f"Unary(Not).type is {unary.type}, expected Primitive.Boolean")
                return unary

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = not True",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
