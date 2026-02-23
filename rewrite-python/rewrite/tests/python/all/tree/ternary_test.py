import shutil

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Ternary
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python("assert True if True else False"))


@requires_ty_cli
def test_ternary_type_attribution():
    """Verify that '1 if True else 2' has type Int."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_ternary(self, ternary, p):
                if not isinstance(ternary, Ternary):
                    return ternary
                if ternary.type is None:
                    errors.append("Ternary.type is None")
                elif ternary.type != JavaType.Primitive.Int:
                    errors.append(f"Ternary.type is {ternary.type}, expected Primitive.Int")
                return ternary

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = 1 if True else 2",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
