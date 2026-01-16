import pytest

from rewrite.test import RecipeSpec, python


def test_basic_list_comprehension():
    # language=python
    RecipeSpec().rewrite_run(python("a = [ e+1 for e in [1, 2, ]]"))


def test_async_list_comprehension():
    # language=python
    RecipeSpec().rewrite_run(python("a = [ e+1 async for e in [1, 2, ]]"))


def test_list_comprehension_with_if():
    # language=python
    RecipeSpec().rewrite_run(python("a = [ e+1 for e in [1, 2, ] if e > 1]"))


def test_list_comprehension_with_multiple_ifs():
    # language=python
    RecipeSpec().rewrite_run(python("a = [ e+1 for e in [1, 2, ] if e > 1  if e < 10]"))


def test_basic_set_comprehension():
    # language=python
    RecipeSpec().rewrite_run(python("a = { e for e in range(10)}"))


def test_set_comprehension_with_if():
    # language=python
    RecipeSpec().rewrite_run(python("a = { e for e in range(10) if e > 1}"))


def test_set_comprehension_with_multiple_ifs():
    # language=python
    RecipeSpec().rewrite_run(python("a = { e for e in range(10) if e > 1 if e < 10}"))


def test_basic_dict_comprehension():
    # language=python
    RecipeSpec().rewrite_run(python("a = {n: n * 2 for n in range(10)}"))


def test_dict_comprehension_with_if():
    # language=python
    RecipeSpec().rewrite_run(python("a = {e:e for e in range(10) if e > 1}"))


def test_dict_comprehension_with_multiple_ifs():
    # language=python
    RecipeSpec().rewrite_run(python("a = {e:None for e in range(10) if e > 1 if e < 10}"))


def test_basic_generator():
    # language=python
    RecipeSpec().rewrite_run(python("a = (n * 2 for n in range(10))"))


def test_generator_without_parens_in_call_1():
    # language=python
    RecipeSpec().rewrite_run(python("a = list(n * 2 for n in range(10))"))


def test_generator_without_parens_in_call_2():
    # language=python
    RecipeSpec().rewrite_run(python("a = list((n * 2) for n in range(10))"))


def test_generator_without_parens_in_call_3():
    # language=python
    RecipeSpec().rewrite_run(python("a = list((n,) for n in range(10))"))


def test_generator_without_parens_in_call_4():
    # language=python
    RecipeSpec().rewrite_run(python("a = list(((n,m) for m in range(10)) for n in range(10))"))


def test_generator_without_parens_in_call_5():
    # language=python
    RecipeSpec().rewrite_run(python("a = any(is_documentation_link(word) for line in body.splitlines() for word in line.split())"))


def test_generator_with_if():
    # language=python
    RecipeSpec().rewrite_run(python("a = {e:e for e in range(10) if e > 1}"))


def test_generator_with_multiple_ifs():
    # language=python
    RecipeSpec().rewrite_run(python("a = {e:None for e in range(10) if e > 1 if e < 10}"))


def test_generator_without_parens_inside_call():
    # language=python
    RecipeSpec().rewrite_run(python("a = sum((2 - 1) * k for k in [1])"))
