import shutil

import pytest

from rewrite import ExecutionContext
from rewrite.java import J
from rewrite.python import CompilationUnit, PythonVisitor, SpacesVisitor
from rewrite.python.style import IntelliJ
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


def _spaces():
    return RecipeSpec().with_recipe(from_visitor(SpacesVisitor(IntelliJ.spaces())))


def test_not_operand_gets_a_space():
    rewrite_run(
        # language=python
        python("assert not(x)", "assert not (x)"),
        spec=_spaces()
    )


def test_not_operand_extra_space_collapsed():
    rewrite_run(
        # language=python
        python("assert not  x", "assert not x"),
        spec=_spaces()
    )


def test_negation_is_tightened():
    rewrite_run(
        # language=python
        python("y = - z", "y = -z"),
        spec=_spaces()
    )


def test_complement_is_tightened():
    rewrite_run(
        # language=python
        python("y = ~ z", "y = ~z"),
        spec=_spaces()
    )


def test_trailing_comma_takes_the_space_inside_its_brackets():
    rewrite_run(
        # language=python
        python("a = (0, )", "a = (0,)"),
        python("b: Dict[str, int, ] = {}", "b: Dict[str, int,] = {}"),
        python("c = {0: 1, }", "c = {0: 1,}"),
        spec=_spaces()
    )

    spaces = IntelliJ.spaces()
    rewrite_run(
        # language=python
        python("d = [0,]", "d = [ 0, ]"),
        spec=RecipeSpec().with_recipe(from_visitor(SpacesVisitor(
            spaces.with_within(spaces.within.with_brackets(True)))))
    )


@pytest.mark.skipif(shutil.which("ty-types") is None, reason="ty-types not installed")
def test_a_default_value_gets_spaces_only_after_an_annotation():
    rewrite_run(
        # language=python
        python(
            "def f(a, b = 1, c: int=2, *, d = None):\n    return a\n",
            "def f(a, b=1, c: int = 2, *, d=None):\n    return a\n",
        ),
        # With types, every parameter has one, annotated or not
        spec=RecipeSpec(type_attribution=True).with_recipe(from_visitor(SpacesVisitor(IntelliJ.spaces())))
    )


def test_formatting_stops_after_the_given_tree():
    class SpaceTheFirstStatement(PythonVisitor[ExecutionContext]):
        def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
            return SpacesVisitor(IntelliJ.spaces(), cu.statements[0]).visit(cu, ctx)

    rewrite_run(
        # language=python
        python("a=1\nb=2\n", "a = 1\nb=2\n"),
        spec=RecipeSpec().with_recipe(from_visitor(SpaceTheFirstStatement()))
    )
