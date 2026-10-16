from uuid import uuid4

import pytest

from rewrite import ExecutionContext
from rewrite.java.support_types import JavaType, JLeftPadded, Space
from rewrite.java.tree import Assert, Unary
from rewrite.markers import Markers
from rewrite.python.tree import Await, CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


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


class NegateAssertCondition(PythonVisitor[ExecutionContext]):
    def visit_assert(self, assert_: Assert, p: ExecutionContext) -> Assert:
        condition = assert_.condition
        return assert_.replace(condition=Unary(
            uuid4(),
            condition.prefix,
            Markers.EMPTY,
            JLeftPadded(Space.EMPTY, Unary.Type.Not, Markers.EMPTY),
            condition.replace(prefix=Space.EMPTY),
            JavaType.Primitive.Boolean,
        ))


@pytest.mark.parametrize("before, after", [
    ("assert x", "assert not x"),
    ("assert os.path.islink(p)", "assert not os.path.islink(p)"),
    ("assert obj.flag", "assert not obj.flag"),
    ("assert 0", "assert not 0"),
    ("assert (x)", "assert not(x)"),
    ("assert [x]", "assert not[x]"),
    ("assert 's'", "assert not's'"),
])
def test_synthesized_not_is_separated_from_operand(before, after):
    # given
    spec = RecipeSpec(recipe=from_visitor(NegateAssertCondition()))

    # when / then
    spec.rewrite_run(python(before, after))


def test_parsed_not_glued_to_bracketed_operand_round_trips():
    # language=python
    RecipeSpec().rewrite_run(python("x = not(a), not[a], not{a}, not'a'"))


class AwaitAssertCondition(PythonVisitor[ExecutionContext]):
    def visit_assert(self, assert_: Assert, p: ExecutionContext) -> Assert:
        condition = assert_.condition
        if isinstance(condition, Await):
            return assert_
        return assert_.replace(condition=Await(
            uuid4(),
            condition.prefix,
            Markers.EMPTY,
            condition.replace(prefix=Space.EMPTY),
            None,
        ))


@pytest.mark.parametrize("before, after", [
    ("assert x", "assert await x"),
    ("assert session.ready()", "assert await session.ready()"),
    ("assert (x)", "assert await(x)"),
])
def test_synthesized_await_is_separated_from_operand(before, after):
    # given
    spec = RecipeSpec(recipe=from_visitor(AwaitAssertCondition()))

    # when / then
    spec.rewrite_run(python(f"async def f():\n    {before}", f"async def f():\n    {after}"))
