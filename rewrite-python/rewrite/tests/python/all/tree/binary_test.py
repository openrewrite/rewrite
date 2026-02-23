import shutil

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Binary
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


def test_bool_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert True or False"))
    # language=python
    RecipeSpec().rewrite_run(python("assert True and False"))


def test_arithmetic_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 + 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 - 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 * 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 / 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 % 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 // 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 ** 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert [[1]] @ [[2]]"))


def test_eq_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 == 1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 != 2"))


def test_right_with_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        assert index > 0 and (token.lineno, token.column) >= pos
        """
    ))


def test_in():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 in [1]"))


def test_not_in():
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 not in [1]"))


def test_is():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is 1"))


def test_isnot():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is not 2"))


def test_comparison_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 < 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 <= 2"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 > 1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert 2 >= 1"))


def test_chained_comparison():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 < 2 <= 3  >=0"))


def test_chained_comparison_2():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 is not 2 is not 3"))


def test_multiline_tuple_comparison():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        (
        True
        , False) is not None
        """,
    ),)


@requires_ty_cli
def test_arithmetic_type_attribution():
    """Verify that 1 + 2 has type Int."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_binary(self, binary, p):
                if not isinstance(binary, Binary):
                    return binary
                if binary.operator != Binary.Type.Addition:
                    return binary
                if binary.type is None:
                    errors.append("Binary(Addition).type is None")
                elif binary.type != JavaType.Primitive.Int:
                    errors.append(f"Binary(Addition).type is {binary.type}, expected Primitive.Int")
                return binary

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = 1 + 2",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_ty_cli
def test_comparison_type_attribution():
    """Verify that 1 < 2 has type Boolean."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_binary(self, binary, p):
                if not isinstance(binary, Binary):
                    return binary
                if binary.operator != Binary.Type.LessThan:
                    return binary
                if binary.type is None:
                    errors.append("Binary(LessThan).type is None")
                elif binary.type != JavaType.Primitive.Boolean:
                    errors.append(f"Binary(LessThan).type is {binary.type}, expected Primitive.Boolean")
                return binary

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = 1 < 2",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_ty_cli
def test_boolean_op_type_attribution():
    """Verify that True and False has type Boolean."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_binary(self, binary, p):
                if not isinstance(binary, Binary):
                    return binary
                if binary.operator != Binary.Type.And:
                    return binary
                if binary.type is None:
                    errors.append("Binary(And).type is None")
                elif binary.type != JavaType.Primitive.Boolean:
                    errors.append(f"Binary(And).type is {binary.type}, expected Primitive.Boolean")
                return binary

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = True and False",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
