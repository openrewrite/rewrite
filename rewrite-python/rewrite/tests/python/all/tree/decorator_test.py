from rewrite.test import RecipeSpec, python


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


def test_parenthesized_decorator():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        @(pytest.fixture())
        def outer_paren_fixture():
            return 42
        """
    ))
