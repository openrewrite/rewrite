from typing import Callable

from rewrite.java import Space, J
from rewrite.python import NormalizeFormatVisitor, PythonVisitor, CompilationUnit
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


class RemoveDecorators(PythonVisitor):
    def visit_method_declaration(self, method, p):
        return method.replace(leading_annotations=[])


def test_remove_decorator():
    rewrite_run(
        # language=python
        python(
            """
            from functools import lru_cache

            @lru_cache
            def f(n):
                return n
            """,
            """
            from functools import lru_cache


            def f(n):
                return n
            """,
            after_recipe=assert_prefix(lambda cu: cu.statements[1], Space([], '\n\n\n'))
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(RemoveDecorators()),
            from_visitor(NormalizeFormatVisitor())
        )
    )

def assert_prefix(extract: Callable[[CompilationUnit], J], expected: Space) -> Callable[[CompilationUnit], None]:
    def fun(cu: CompilationUnit):
        assert extract(cu).prefix == expected
    return fun
