from rewrite.python import SpacesVisitor
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
