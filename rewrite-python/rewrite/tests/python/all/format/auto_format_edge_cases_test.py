import sys

import pytest

from rewrite.python import AutoFormat
from rewrite.test import rewrite_run, python, RecipeSpec


def test_deeply_nested_indentation():
    rewrite_run(
        # language=python
        python(
            """\
            def deep():
                for i in range(10):
                    if i > 0:
                        for j in range(5):
                            if j > 0:
                                result = i + j
                                if result > 10:
                                    print(result)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_mixed_string_types():
    rewrite_run(
        # language=python
        python(
            """\
            def strings():
                a = 'single quoted'
                b = "double quoted"
                c = '''triple
            single'''
                d = \"\"\"triple
            double\"\"\"
                e = r"raw string"
                f = b"byte string"
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_multiline_dict_with_trailing_commas():
    rewrite_run(
        # language=python
        python(
            """\
            config = {
                "host": "localhost",
                "port": 8080,
                "debug": True,
                "options": {
                    "timeout": 30,
                    "retries": 3,
                },
            }
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_multiline_function_call_with_keyword_args():
    rewrite_run(
        # language=python
        python(
            """\
            def main():
                result = some_function(
                    first_arg=1,
                    second_arg="hello",
                    third_arg=True,
                    fourth_arg=[1, 2, 3],
                )
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_decorator_with_complex_expression():
    rewrite_run(
        # language=python
        python(
            """\
            import module


            @module.decorator(arg1, arg2)
            def decorated():
                pass


            @module.sub.decorator(
                param1="value",
                param2=True
            )
            def also_decorated():
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_class_with_multiple_bases_multiline():
    rewrite_run(
        # language=python
        python(
            """\
            class MyClass(
                    Base1,
                    Base2,
                    Mixin1,
                    Mixin2):

                def method(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_assignment_with_multiline_rhs():
    rewrite_run(
        # language=python
        python(
            """\
            def f():
                x = (
                    very_long_variable_name
                    + another_long_variable_name
                    + yet_another_one
                )
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_return_with_tuple():
    rewrite_run(
        # language=python
        python(
            """\
            def multi_return():
                a, b, c = 1, 2, 3
                return a, b, c
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_yield_and_yield_from():
    rewrite_run(
        # language=python
        python(
            """\
            def generator(items):
                for item in items:
                    yield item


            def delegating(items):
                yield from items
                yield from range(10)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_raise_with_from():
    rewrite_run(
        # language=python
        python(
            """\
            def risky():
                try:
                    x = 1 / 0
                except ZeroDivisionError as original:
                    raise ValueError("bad math") from original
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_empty_function_body():
    rewrite_run(
        # language=python
        python(
            """\
            def empty_inline():
                pass


            def empty_multiline():
                pass


            class Empty:
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_semicolon_separated_statements():
    rewrite_run(
        # language=python
        python(
            """\
            def compact():
                a = 1; b = 2; c = 3
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_backslash_line_continuation():
    rewrite_run(
        # language=python
        python(
            """\
            def continued():
                x = 1 + 2 + 3 + \\
                    4 + 5 + 6
                result = x * \\
                    2
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_parenthesized_line_continuation():
    rewrite_run(
        # language=python
        python(
            """\
            def continued():
                x = (1 + 2 + 3 +
                     4 + 5 + 6)
                y = (
                    "hello"
                    + " "
                    + "world"
                )
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_nested_comprehension():
    rewrite_run(
        # language=python
        python(
            """\
            def nested():
                flat = [x for row in matrix for x in row]
                filtered = [x for row in matrix for x in row if x > 0]
                coords = [(x, y) for x in range(5) for y in range(5)]
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_dict_unpacking():
    rewrite_run(
        # language=python
        python(
            """\
            def merge_dicts():
                d1 = {"a": 1}
                d2 = {"b": 2}
                merged = {**d1, **d2, "key": "value"}
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_subscript_with_complex_slices():
    rewrite_run(
        # language=python
        python(
            """\
            def slices(a):
                x = a[1:2]
                y = a[::2]
                z = a[..., :5]
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_delete_statement():
    rewrite_run(
        # language=python
        python(
            """\
            def cleanup():
                a = 1
                b = [1, 2, 3]

                class Obj:
                    attr = 42

                o = Obj()
                del a, b[0], o.attr
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_exec_statement():
    rewrite_run(
        # language=python
        python(
            """\
            def dynamic():
                code = "x = 1 + 2"
                exec(code, globals(), locals())
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.skipif(sys.version_info < (3, 12), reason='type aliases require Python 3.12+')
def test_type_alias():
    rewrite_run(
        # language=python
        python(
            """\
            type Point = tuple[int, int]
            type Matrix = list[list[float]]
            type Callback = Callable[[int, str], bool]
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )
