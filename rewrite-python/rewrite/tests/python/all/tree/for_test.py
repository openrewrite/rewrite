from rewrite.java.tree import ForEachLoop, Identifier
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def test_for():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        for x in [1]:
            pass
        """
    ))


def test_for_with_destruct():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        for x, y in [(1,2),(3,4)]:
            pass
        """
    ))


def test_for_with_destruct_and_parens_1():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        for (x, y) in [(1,2),(3,4)]:
            pass
        """
    ))


def test_for_with_target_expression():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        for d['a'] in [1, 2, 3]:
            pass
        """
    ))


def test_for_with_else():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        for x in [1]:
            pass
        else:
            pass
        """
    ))


def test_async():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        async for x in [1]:
            pass
        """
    ))


def test_for_loop_variable_type_attribution():
    """Verify that the loop control variable in 'for x in [1, 2, 3]' has a type."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_for_each_loop(self, for_each, p):
                if not isinstance(for_each, ForEachLoop):
                    return for_each
                variable = for_each.control.variable
                if isinstance(variable, Identifier):
                    if variable.type is None:
                        errors.append("ForEachLoop control variable Identifier.type is None")
                return for_each

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        for x in [1, 2, 3]:
            pass
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
