from rewrite.python import AutoFormat
from rewrite.test import rewrite_run, python, RecipeSpec


def test_fix_missing_space_around_assignment():
    rewrite_run(
        # language=python
        python(
            """\
            x=1
            y="hello"
            z=True
            """,
            """\
            x = 1
            y = "hello"
            z = True
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_missing_space_around_binary_ops():
    rewrite_run(
        # language=python
        python(
            """\
            def compute(a, b, c):
                x = a+b*c
                y = a-b/c
                z = a//b%c
                w = a|b&c
                v = a^b<<c
            """,
            """\
            def compute(a, b, c):
                x = a + b * c
                y = a - b / c
                z = a // b % c
                w = a | b & c
                v = a ^ b << c
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_extra_space_in_function_parens():
    rewrite_run(
        # language=python
        python(
            """\
            def f( x ):
                return x
            """,
            """\
            def f(x):
                return x
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_missing_space_after_comma():
    rewrite_run(
        # language=python
        python(
            """\
            def f(a,b,c):
                return a,b,c
            """,
            """\
            def f(a, b, c):
                return a, b, c
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_extra_space_before_comma():
    rewrite_run(
        # language=python
        python(
            """\
            def f(a , b , c):
                return a , b , c
            """,
            """\
            def f(a, b, c):
                return a, b, c
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_missing_space_after_colon():
    rewrite_run(
        # language=python
        python(
            """\
            d = {"a":1, "b":2, "c":3}
            """,
            """\
            d = {"a": 1, "b": 2, "c": 3}
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_wrong_indentation():
    rewrite_run(
        # language=python
        python(
            """\
            def foo():
              x = 1
              if x > 0:
                return x
              return 0
            """,
            """\
            def foo():
                x = 1
                if x > 0:
                    return x
                return 0
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_missing_blank_lines_between_top_level():
    rewrite_run(
        # language=python
        python(
            """\
            class Foo:
                pass
            class Bar:
                pass
            def baz():
                pass
            """,
            """\
            class Foo:
                pass


            class Bar:
                pass


            def baz():
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_missing_blank_lines_between_methods():
    rewrite_run(
        # language=python
        python(
            """\
            class MyClass:
                def first(self):
                    pass
                def second(self):
                    pass
                def third(self):
                    pass
            """,
            """\
            class MyClass:
                def first(self):
                    pass

                def second(self):
                    pass

                def third(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_extra_blank_lines_in_code():
    rewrite_run(
        # language=python
        python(
            """\
            def foo():
                x = 1



                y = 2



                return x + y
            """,
            """\
            def foo():
                x = 1

                y = 2

                return x + y
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_trailing_whitespace():
    rewrite_run(
        # language=python
        python(
            "def foo():   \n    x = 1  \n    return x   \n",
            "def foo():\n    x = 1\n    return x\n"
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_around_type_hint_colon():
    rewrite_run(
        # language=python
        python(
            """\
            def f(x:int, y:str) -> None:
                z:float = 3.14
            """,
            """\
            def f(x: int, y: str) -> None:
                z: float = 3.14
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_around_return_type():
    rewrite_run(
        # language=python
        python(
            """\
            def f(x: int)->int:
                return x
            """,
            """\
            def f(x: int) -> int:
                return x
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_extra_space_in_call_parens():
    rewrite_run(
        # language=python
        python(
            """\
            def main():
                f(  x  )
                g(  a, b  )
            """,
            """\
            def main():
                f(x)
                g(a, b)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_before_function_parens():
    rewrite_run(
        # language=python
        python(
            """\
            def f ():
                pass


            class Foo :
                def method (self):
                    pass
            """,
            """\
            def f():
                pass


            class Foo:
                def method(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_multiline_method_alignment():
    rewrite_run(
        # language=python
        python(
            """\
            def function_with_long_name(
            first_param: int,
                    second_param: str,
              third_param: float) -> None:
                pass
            """,
            """\
            def function_with_long_name(
                    first_param: int,
                    second_param: str,
                    third_param: float) -> None:
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_around_augmented_assign():
    rewrite_run(
        # language=python
        python(
            """\
            def f():
                x = 0
                x+=1
                x-=2
                x*=3
                x//=4
            """,
            """\
            def f():
                x = 0
                x += 1
                x -= 2
                x *= 3
                x //= 4
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_around_comparison():
    rewrite_run(
        # language=python
        python(
            """\
            def check(x, y):
                a = x>5 and y<10
                b = x>=5 or y<=10
                c = x==y and x!=0
            """,
            """\
            def check(x, y):
                a = x > 5 and y < 10
                b = x >= 5 or y <= 10
                c = x == y and x != 0
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_around_power_op():
    rewrite_run(
        # language=python
        python(
            """\
            def powers(x):
                a = x**2
                b = x**3
                c = 2**x
            """,
            """\
            def powers(x):
                a = x ** 2
                b = x ** 3
                c = 2 ** x
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_fix_space_in_multi_import():
    rewrite_run(
        # language=python
        python(
            """\
            from collections import defaultdict,OrderedDict
            """,
            """\
            from collections import defaultdict, OrderedDict
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )
