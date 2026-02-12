import pytest

from rewrite.python import AutoFormat
from rewrite.test import rewrite_run, python, RecipeSpec


def test_idempotent_simple_function():
    rewrite_run(
        # language=python
        python(
            """\
            def greet(name):
                message = "Hello, " + name
                return message


            def add(a, b):
                return a + b
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_class_with_methods():
    rewrite_run(
        # language=python
        python(
            """\
            class Calculator:

                def __init__(self, value=0):
                    self.value = value

                def add(self, x):
                    self.value += x
                    return self

                def subtract(self, x):
                    self.value -= x
                    return self

                def reset(self):
                    self.value = 0
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_imports():
    rewrite_run(
        # language=python
        python(
            """\
            import os
            import sys
            from collections import defaultdict, OrderedDict
            from typing import List, Optional


            def main():
                path = os.getcwd()
                args = sys.argv
                data = defaultdict(list)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_operators():
    rewrite_run(
        # language=python
        python(
            """\
            def operators():
                a = 1 + 2
                b = 3 - 4
                c = 5 * 6
                d = 7 / 8
                e = 9 // 2
                f = 10 % 3
                g = 2 ** 8
                h = a << 2
                i = b >> 1
                j = c & d
                k = e | f
                m = g ^ h
                n = a == b
                o = c != d
                p = e < f
                q = g > h
                r = i <= j
                s = k >= m
                t = n and o
                u = p or q
                v = not r
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_collections():
    rewrite_run(
        # language=python
        python(
            """\
            def collections():
                numbers = [1, 2, 3, 4, 5]
                matrix = [
                    [1, 2, 3],
                    [4, 5, 6],
                    [7, 8, 9],
                ]
                person = {"name": "Alice", "age": 30}
                config = {
                    "host": "localhost",
                    "port": 8080,
                    "debug": True,
                }
                unique = {1, 2, 3}
                point = (10, 20)
                triple = (1, 2, 3)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_comprehensions():
    rewrite_run(
        # language=python
        python(
            """\
            def comprehensions():
                squares = [x * x for x in range(10)]
                evens = [x for x in range(20) if x % 2 == 0]
                pairs = {k: v for k, v in zip("abc", [1, 2, 3])}
                unique = {x % 5 for x in range(20)}
                total = sum(x * x for x in range(10))
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_type_hints():
    rewrite_run(
        # language=python
        python(
            """\
            from typing import List, Optional, Dict, Tuple


            def process(items: List[int], flag: bool = True) -> Optional[str]:
                count: int = len(items)
                result: Optional[str] = None
                if flag and count > 0:
                    result = str(count)
                return result


            def lookup(data: Dict[str, int], key: str) -> Tuple[bool, int]:
                if key in data:
                    return True, data[key]
                return False, 0
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_decorators():
    rewrite_run(
        # language=python
        python(
            """\
            import functools


            def log(func):
                @functools.wraps(func)
                def wrapper(*args, **kwargs):
                    return func(*args, **kwargs)

                return wrapper


            def repeat(n):
                def decorator(func):
                    @functools.wraps(func)
                    def wrapper(*args, **kwargs):
                        for _ in range(n):
                            func(*args, **kwargs)

                    return wrapper

                return decorator


            class MyClass:

                @staticmethod
                def static_method():
                    pass

                @classmethod
                def class_method(cls):
                    pass

                @log
                @repeat(3)
                def decorated(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_try_except():
    rewrite_run(
        # language=python
        python(
            """\
            def safe_divide(a, b):
                try:
                    result = a / b
                except ZeroDivisionError:
                    return None
                except (TypeError, ValueError) as e:
                    raise RuntimeError("bad input") from e
                else:
                    return result
                finally:
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_with_statement():
    rewrite_run(
        # language=python
        python(
            """\
            def file_operations():
                with open("input.txt") as f:
                    data = f.read()

                with open("in.txt") as src, open("out.txt", "w") as dst:
                    dst.write(src.read())
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_lambda():
    rewrite_run(
        # language=python
        python(
            """\
            def lambda_examples():
                double = lambda x: x * 2
                items = [3, 1, 4, 1, 5]
                items.sort(key=lambda x: -x)
                mapped = list(map(lambda x: x + 1, items))
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_conditional_expression():
    rewrite_run(
        # language=python
        python(
            """\
            def conditionals(x, y):
                result = "positive" if x > 0 else "non-positive"
                value = x if x is not None else y
                label = "even" if x % 2 == 0 else "odd"
                return result, value, label
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_nested_functions():
    rewrite_run(
        # language=python
        python(
            """\
            def outer(x):
                def middle(y):
                    def inner(z):
                        return x + y + z

                    return inner

                return middle


            def make_adder(n):
                def adder(x):
                    return x + n

                return adder
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_nested_classes():
    rewrite_run(
        # language=python
        python(
            """\
            class Outer:

                class Inner:

                    class DeepInner:
                        value = 42

                    def method(self):
                        return self.DeepInner.value

                def create_inner(self):
                    return self.Inner()
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_star_expressions():
    rewrite_run(
        # language=python
        python(
            """\
            def star_examples(*args, **kwargs):
                first, *rest = [1, 2, 3, 4]
                a, *middle, b = range(10)
                merged = {**kwargs, "extra": True}
                combined = [*args, *rest]
                return first, middle, merged, combined
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_slicing():
    rewrite_run(
        # language=python
        python(
            """\
            def slicing():
                data = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
                a = data[1:5]
                b = data[:3]
                c = data[7:]
                d = data[::2]
                e = data[1:8:3]
                f = data[::-1]
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_assert():
    rewrite_run(
        # language=python
        python(
            """\
            def validate(x, items):
                assert x > 0
                assert isinstance(x, int), "x must be an integer"
                assert len(items) > 0, f"expected non-empty, got {len(items)}"
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_global_nonlocal():
    rewrite_run(
        # language=python
        python(
            """\
            counter = 0


            def increment():
                global counter
                counter += 1


            def make_counter():
                count = 0

                def inc():
                    nonlocal count
                    count += 1
                    return count

                return inc
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_fstrings():
    rewrite_run(
        # language=python
        python(
            """\
            def fstring_examples():
                name = "world"
                greeting = f"Hello, {name}!"
                width = 10
                padded = f"{name:>{width}}"
                result = f"sum={1 + 2}"
                nested = f"{'yes' if True else 'no'}"
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_multiline_strings():
    rewrite_run(
        # language=python
        python(
            """\
            def documented():
                \"\"\"This is a docstring.

                It has multiple lines.
                \"\"\"
                text = \"\"\"
            First line
            Second line
            Third line
            \"\"\"
                return text
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_async_await():
    rewrite_run(
        # language=python
        python(
            """\
            import asyncio


            async def fetch(url):
                await asyncio.sleep(1)
                return url


            async def process(urls):
                async for chunk in aiter(urls):
                    pass

                async with asyncio.Lock() as lock:
                    results = [await fetch(u) for u in urls]
                    return results
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_match_case():
    rewrite_run(
        # language=python
        python(
            """\
            def handle(command):
                match command:
                    case "quit":
                        return False
                    case "hello" | "hi":
                        print("Hello!")
                    case str(s) if s.startswith("go"):
                        print(f"Going to {s[2:]}")
                    case [x, y]:
                        print(f"Pair: {x}, {y}")
                    case {"action": action, "value": value}:
                        print(f"{action}: {value}")
                    case _:
                        print("Unknown")
                return True
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_walrus_operator():
    rewrite_run(
        # language=python
        python(
            """\
            def walrus_examples(data):
                if (n := len(data)) > 10:
                    print(f"Long: {n}")

                filtered = [y for x in data if (y := x * 2) > 5]

                while chunk := data[:10]:
                    data = data[10:]
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_chained_method_calls():
    rewrite_run(
        # language=python
        python(
            """\
            def chained():
                result = "  Hello, World!  ".strip().lower().replace("world", "python")
                items = [3, 1, 4, 1, 5]
                items.sort()
                items.append(9)
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_idempotent_multiline_method_chain():
    rewrite_run(
        # language=python
        python(
            """\
            def multiline_chain(data):
                result = (data
                    .filter(lambda x: x > 0)
                    .map(lambda x: x * 2)
                    .sorted()
                    .collect())
                return result
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_comments_everywhere():
    rewrite_run(
        # language=python
        python(
            """\
            # Module-level comment
            import os


            # Function comment
            def compute(x, y):
                # Body comment
                result = x + y  # inline comment
                # Another comment
                return result


            class Foo:
                # Class body comment
                value = 42  # field comment

                def method(self):
                    # Method body
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


@pytest.mark.xfail(reason='Formatter incorrectly modifies well-formatted code')
def test_idempotent_string_concatenation():
    rewrite_run(
        # language=python
        python(
            """\
            def strings():
                implicit = ("hello "
                            "world "
                            "foo")
                explicit = "hello" + " " + "world"
                mixed = ("start "
                         + "middle "
                         + "end")
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_empty_containers():
    rewrite_run(
        # language=python
        python(
            """\
            def empty_containers():
                empty_list = []
                empty_dict = {}
                empty_tuple = ()
                empty_set = set()
                single = [1]
                single_dict = {"a": 1}
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_complex_defaults():
    rewrite_run(
        # language=python
        python(
            """\
            def complex_defaults(
                    items=None,
                    callback=lambda x: x,
                    mapping=None,
                    threshold=0.5,
                    flags=(True, False)):
                if items is None:
                    items = []
                if mapping is None:
                    mapping = {}
                return items, callback, mapping, threshold, flags
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )


def test_idempotent_class_inheritance():
    rewrite_run(
        # language=python
        python(
            """\
            class Base:
                pass


            class Mixin:
                pass


            class Meta(type):
                pass


            class Child(Base):
                pass


            class Multi(Base, Mixin):
                pass


            class WithMeta(Base, Mixin, metaclass=Meta):
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipe(AutoFormat())
    )
