from typing import cast

from rewrite.java import MethodDeclaration, Return
from rewrite.java.support_types import JavaType
from rewrite.java.tree import Assignment
from rewrite.python import CollectionLiteral, CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

Parameterized = JavaType.Parameterized


def test_empty_tuple():
    # language=python
    RecipeSpec().rewrite_run(python("t = ( )"))


def test_implicit_tuple():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def f():
            return 1, 2 # comment
    
        def g():
            pass
        """,
        after_recipe=_assert_no_padding
    ))

def _assert_no_padding(cu: CompilationUnit) -> None:
    ret = cast(Return, cast(MethodDeclaration, cu.statements[0]).body.statements[0])  # type: ignore
    lit = cast(CollectionLiteral, ret.expression)
    right_padded = lit.padding.elements.padding.elements[-1]
    assert right_padded.after.whitespace == ''

def test_single_element_tuple():
    # language=python
    RecipeSpec().rewrite_run(python("t = (1 )"))


def test_single_element_tuple_with_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("t = (1 , )"))


def test_single_element_tuple_with_trailing_comma_outside_parens():
    # language=python - parenthesized expression with trailing comma outside
    RecipeSpec().rewrite_run(python('("a"),\n'))


def test_single_element_tuple_with_trailing_comma_outside_parens_multiline():
    # language=python - multi-line parenthesized string with trailing comma outside
    RecipeSpec().rewrite_run(python(
        """\
(
    "Part 1"
    " Part 2"
),
"""
    ))


def test_tuple_with_first_element_in_parens():
    # language=python
    RecipeSpec().rewrite_run(python("x = (1) // 2, 0"))


def test_tuple_with_all_elements_in_parens():
    # language=python - each element wrapped in parens, tuple has no outer parens
    RecipeSpec().rewrite_run(python("x = (a), (b)"))


def test_tuple_with_parenthesized_subscript_elements():
    # language=python - parenthesized subscript expressions as tuple elements
    RecipeSpec().rewrite_run(python("w, h = (bbox[2] - bbox[0]), (bbox[3] - bbox[1])"))


def test_tuple_with_parenthesized_arithmetic():
    # language=python - expression with division as tuple elements
    RecipeSpec().rewrite_run(python("center = (bbox[2] + bbox[0]) / 2, (bbox[3] + bbox[1]) / 2"))


# note: `{}` is always a dict
def test_empty_set():
    # language=python
    RecipeSpec().rewrite_run(python("t = set()"))


def test_single_element_set():
    # language=python
    RecipeSpec().rewrite_run(python("t = {1 }"))


def test_single_element_set_with_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("t = {1 , }"))


def test_deeply_nested():
    # language=python
    RecipeSpec().rewrite_run(python('d2 = (((((((((((((((((((((((((((("¯\\_(ツ)_/¯",),),),),),),),),),),),),),),),),),),),),),),),),),),),)'))


def test_tuple_generator():
    # language=python
    RecipeSpec().rewrite_run(python("_local = tuple((i, \"\") if isinstance(i, int) else (NegativeInfinity, i) for i in local)"))


def test_list_of_tuples_with_double_parens():
    # language=python
    RecipeSpec().rewrite_run(python("""
[({"a"}, {
    "a": 0
}), ((set(), {}))]
"""))


def test_list_literal_type_attribution():
    """Verify that [1, 2, 3] has type list."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                if assignment.type is None:
                    errors.append("Assignment.type is None for list literal")
                elif isinstance(assignment.type, Parameterized):
                    if not assignment.type._type._fully_qualified_name.startswith('list'):
                        errors.append(f"Parameterized base fqn is '{assignment.type._type._fully_qualified_name}', expected to start with 'list'")
                elif isinstance(assignment.type, JavaType.Class):
                    if not assignment.type._fully_qualified_name.startswith('list'):
                        errors.append(f"Class fqn is '{assignment.type._fully_qualified_name}', expected to start with 'list'")
                return assignment

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x = [1, 2, 3]",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_dict_literal_type_attribution():
    """Verify that {"a": 1} has type dict."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                if assignment.type is None:
                    errors.append("Assignment.type is None for dict literal")
                elif isinstance(assignment.type, Parameterized):
                    if not assignment.type._type._fully_qualified_name.startswith('dict'):
                        errors.append(f"Parameterized base fqn is '{assignment.type._type._fully_qualified_name}', expected to start with 'dict'")
                elif isinstance(assignment.type, JavaType.Class):
                    if not assignment.type._fully_qualified_name.startswith('dict'):
                        errors.append(f"Class fqn is '{assignment.type._fully_qualified_name}', expected to start with 'dict'")
                return assignment

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        'x = {"a": 1}',
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
