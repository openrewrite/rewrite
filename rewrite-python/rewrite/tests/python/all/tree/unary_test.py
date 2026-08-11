from rewrite.java.support_types import JavaType, Space
from rewrite.java.tree import Unary
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


def test_bool_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert not True"))


def test_not_keeps_separator_when_operand_prefix_collapses():
    class RemoveWhitespace(PythonVisitor):
        def visit_unary(self, unary, p):
            u = super().visit_unary(unary, p)
            if isinstance(u, Unary) and u.operator == Unary.Type.Not:
                return u.replace(_expression=u.expression.replace(_prefix=Space.EMPTY))
            return u

    RecipeSpec(recipe=from_visitor(RemoveWhitespace())).rewrite_run(
        # language=python
        python("assert not  x", "assert not x")
    )


def test_arithmetic_ops():
    # language=python
    RecipeSpec().rewrite_run(python("assert +1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert -1"))
    # language=python
    RecipeSpec().rewrite_run(python("assert ~1"))


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
