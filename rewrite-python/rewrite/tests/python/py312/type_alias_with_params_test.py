from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, from_visitor, python


def test_type_alias_simple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo = int
        """
    ))


def test_type_alias_with_type_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T] = list[T]
        """
    ))


def test_type_alias_with_multiple_type_params():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T, U] = dict[T, U]
        """
    ))


def test_type_alias_with_bound():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        type Foo[T: int] = list[T]
        """
    ))


def test_type_parameter_bound_is_rewritten():
    class RenameVisitor(PythonVisitor):
        def visit_identifier(self, ident, p):
            return ident.replace(simple_name='New') if ident.simple_name == 'Old' else ident

    # language=python
    RecipeSpec(recipe=from_visitor(RenameVisitor())).rewrite_run(python(
        """\
        type Foo[T: Old] = list[T]
        """,
        """\
        type Foo[T: New] = list[T]
        """
    ))
