"""Tests for PEP 646 - Variadic Generics.

PEP 646 adds support for variadic generics including starred expressions
in subscripts and type hints.

See: https://peps.python.org/pep-0646/
"""
from rewrite.test import RecipeSpec, python


# --- Basic starred subscript tests ---

def test_starred_subscript_simple():
    # language=python
    RecipeSpec().rewrite_run(python("A[*b]"))


def test_starred_subscript_with_spaces():
    # language=python
    RecipeSpec().rewrite_run(python("A[  *  b  ]"))


def test_starred_subscript_with_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("A[ *  b ,  ]"))


def test_starred_subscript_trailing_comma_space_before_comma():
    # language=python - Regression test for space before trailing comma
    RecipeSpec().rewrite_run(python("A[*b ,]"))


def test_starred_subscript_assignment():
    # language=python
    RecipeSpec().rewrite_run(python("A[*b] = 1"))


def test_starred_subscript_del():
    # language=python
    RecipeSpec().rewrite_run(python("del A[*b]"))


# --- Multiple starred expressions ---

def test_multiple_starred_expressions():
    # language=python
    RecipeSpec().rewrite_run(python("A[* b ,  * b]"))


def test_starred_with_regular_elements():
    # language=python
    RecipeSpec().rewrite_run(python("A[ b, *b]"))


def test_starred_before_regular():
    # language=python
    RecipeSpec().rewrite_run(python("A[* b, b]"))


def test_starred_multiple_regular():
    # language=python
    RecipeSpec().rewrite_run(python("A[ *  b,b, b]"))


def test_mixed_starred_and_regular():
    # language=python
    RecipeSpec().rewrite_run(python("A[b, *b, b]"))


# --- Nested starred expressions ---

def test_nested_starred():
    # language=python
    RecipeSpec().rewrite_run(python("A[*A[b, *b, b], b]"))


def test_ellipsis_subscript():
    # language=python
    RecipeSpec().rewrite_run(python("A[b, ...]"))


def test_nested_starred_with_ellipsis():
    # language=python
    RecipeSpec().rewrite_run(python("A[*A[b, ...]]"))


# --- Starred tuple/list unpacking in subscript ---

def test_starred_parenthesized_tuple():
    # language=python
    RecipeSpec().rewrite_run(python("A[ * ( 1,2,3)]"))


def test_starred_list_in_subscript():
    # language=python
    RecipeSpec().rewrite_run(python("A[ * [ 1,2,3]]"))


# --- Starred with slices ---

def test_slice_with_starred():
    # language=python
    RecipeSpec().rewrite_run(python("A[1:2, *t]"))


def test_slices_with_starred_middle():
    # language=python
    RecipeSpec().rewrite_run(python("A[1:, *t, 1:2]"))


def test_empty_slices_with_starred():
    # language=python
    RecipeSpec().rewrite_run(python("A[:, *t, :]"))


def test_multiple_starred_with_slices():
    # language=python
    RecipeSpec().rewrite_run(python("A[*t, :, *t]"))


# --- Starred with function calls ---

def test_starred_function_call():
    # language=python
    RecipeSpec().rewrite_run(python("A[* returns_list()]"))


def test_multiple_starred_function_calls():
    # language=python
    RecipeSpec().rewrite_run(python("A[*returns_list(), * returns_list(), b]"))


# --- PEP 646 function annotations ---

def test_starred_varargs_annotation_simple():
    # language=python
    RecipeSpec().rewrite_run(python("def f1(*args: *b): pass"))


def test_starred_varargs_annotation_with_following_arg():
    # language=python
    RecipeSpec().rewrite_run(python("def f2(*args: *b, arg1): pass"))


def test_starred_varargs_annotation_with_typed_arg():
    # language=python
    RecipeSpec().rewrite_run(python("def f3(*args: *b, arg1: int): pass"))


def test_starred_varargs_annotation_with_default():
    # language=python
    RecipeSpec().rewrite_run(python("def f4(*args: *b, arg1: int = 1): pass"))


def test_starred_varargs_with_tuple_annotation():
    # language=python
    RecipeSpec().rewrite_run(python("def f(*args: *tuple[int, ...]): pass"))


def test_starred_varargs_with_typevar_tuple():
    # language=python
    RecipeSpec().rewrite_run(python("def f(*args: *tuple[int, *Ts]): pass"))


def test_return_type_with_starred_tuple():
    # language=python
    RecipeSpec().rewrite_run(python("def f() -> tuple[int, *tuple[int, ...]]: pass"))
