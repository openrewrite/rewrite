import pytest

from rewrite.test import RecipeSpec, python


def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case 1:
                    pass
                case 2:
                    pass
        """
    ))


def test_as():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case 1 as y:
                    return y
        """
    ))


def test_sequence_as():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case [int(), str()] as y:
                    return y
        """
    ))


def test_wildcard():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case 1:
                    pass
                case _:
                    pass
        """
    ))


def test_wildcard_as():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case 1:
                    pass
                case _ as x:
                    return x
        """
    ))


def test_sequence():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case [1, 2]:
                    pass
        """
    ))


def test_nested():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case [int(), str()]:
                    pass
        """
    ))


def test_star():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case [1, *rest]:
                    return rest
        """
    ))


def test_guard():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case [1, *rest] if 42 > rest:
                    return rest
        """
    ))


def test_or():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case 1 | 2:
                    pass
        """
    ))


def test_value():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x, value):
            match x:
                case value.pattern:
                    pass
        """
    ))


def test_group():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x, value):
            match x:
                case (value.pattern):
                    pass
        """
    ))


def test_mapping():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case {"x": x, "y": y}:
                    return x+y
        """
    ))


def test_mapping_with_rest():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x):
            match x:
                case {"x": x, "y": y, **z}:
                    return x+y+sum(z.values())
        """
    ))


def test_sequence_target():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x, y):
            match x, y:
                case a, b:
                    return a+b
        """
    ))


@pytest.mark.parametrize("args", ['', 'a', 'b, c', 'a, b=c', 'a, b=c, d=(e,f)'])
def test_class(args):
    RecipeSpec().rewrite_run(python(
        """\
        from abc import ABC
    
        def test(x, y):
            match x:
                case ABC({0}):
                    pass
        """.format(args)
    ))
