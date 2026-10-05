from rewrite.test import RecipeSpec, python


def test_any_expression():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        @False or a
        def f(): pass
        @d := a
        def g(): pass
        @lambda f: a(f)
        def h(): pass
        @[..., a, ...][1]
        def i(): pass
        @a(a)(a)
        def j(): pass
        @a if b else c
        class C: pass
        @not a
        class D: pass
        """
    ))


def test_function_unqualified():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from functools import lru_cache
        
        @lru_cache
        def f(n):
            return n
        """
    ))


def test_function_no_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import functools
        
        @functools.lru_cache
        def f(n):
            return n
        """
    ))


def test_function_empty_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import functools
        
        @functools.lru_cache(1)
        def f(n):
            return n
        """
    ))


def test_function_with_arg():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import functools
        
        @functools.lru_cache(1, )
        def f(n):
            return n
        """
    ))


def test_function_with_named_arg():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import functools
        
        @functools.lru_cache(maxsize=1)
        def f(n):
            return n
        """
    ))


def test_class_no_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from dataclasses import dataclass
        
        @dataclass
        class T:
            pass
        """
    ))


def test_class_empty_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import dataclasses

        @dataclasses.dataclass()
        class T:
            pass
        """
    ))


def test_subscript_decorator():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            @staticmethod
            def property(f):
                return f

            id = 1

            @[property][0]
            def f(self, x=[id]):
                return x
        """
    ))


def test_call_of_call_decorator():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def factory():
            return lambda n: lambda f: f

        @factory()(1)
        def f():
            pass
        """
    ))


def test_parenthesized_decorator():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        @(pytest.fixture())
        def outer_paren_fixture():
            return 42
        """
    ))


def test_decorator_with_space_after_at():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(fun):
            return fun

        @ foo
        def bar():
            pass
        """
    ))


def test_decorator_with_line_continuation():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(fun):
            return fun

        @ \\
        foo
        def bar():
            pass
        """
    ))
