from typing import cast

import pytest

from rewrite.java import MethodDeclaration, Return
from rewrite.python import CollectionLiteral, CompilationUnit
from rewrite.test import RecipeSpec, python


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


def test_tuple_with_first_element_in_parens():
    # language=python
    RecipeSpec().rewrite_run(python("x = (1) // 2, 0"))


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
