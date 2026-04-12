from rewrite.java.tree import Assignment, Lambda
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def test_no_parameters():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda: None"))


def test_single_parameter():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x: x"))


def test_multiple_parameter():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x, y: x + y"))


def test_parameters_with_defaults():
    # language=python
    RecipeSpec().rewrite_run(python("l = lambda x, y=0: x + y"))


def test_parameters_with_defaults_2():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda self, v, n=n: 1'))


def test_positional_only():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, /, b: ...'))


def test_positional_only_last():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a=1, /: ...'))


def test_positional_only_last_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, /,: ...'))


def test_keyword_only():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda kw=1, *, a: ...'))


def test_complex():
    # language=python
    RecipeSpec().rewrite_run(python('l = lambda a, b=20, /, c=30: 1'))


def test_multiple_complex():
    # language=python
    RecipeSpec().rewrite_run(python('''\
        lambda a, b=20, /, c=30: 1
        lambda a, b, /, c, *, d, e: 0
    '''))


def test_lambda_type_attribution():
    """Verify that lambda x: x + 1 has a non-None type."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                # Check the RHS is a Lambda with a type
                if isinstance(assignment.assignment, Lambda):
                    if assignment.assignment.type is None:
                        errors.append("Lambda.type is None")
                return assignment

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "l = lambda x: x + 1",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
