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
