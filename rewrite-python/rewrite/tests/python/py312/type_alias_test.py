from rewrite.java.tree import ParameterizedType
from rewrite.python import AutoFormat
from rewrite.python.markers import Quoted
from rewrite.test import RecipeSpec, python


# noinspection PyCompatibility
def test_type_alias():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import Tuple
    
        type Coordinates = Tuple[float, float]
        """
    ))


def test_alias_value_is_modelled_as_a_type():
    values = []

    def collect(source_file):
        values.append(source_file.statements[0].value)

    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Coordinates = list[int]
        """,
        after_recipe=collect,
    ))
    assert isinstance(values[-1], ParameterizedType)

    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Coordinates = "list[int]"
        """,
        after_recipe=collect,
    ))
    assert isinstance(values[-1], ParameterizedType)
    assert values[-1].markers.find_first(Quoted).style is Quoted.Style.DOUBLE


def test_alias_value_without_a_type_tree_form_keeps_its_space():
    # language=python
    RecipeSpec().with_recipe(AutoFormat()).rewrite_run(python(
        """\
        type Constraints = (int, str)
        """
    ))
