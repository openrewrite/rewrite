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
