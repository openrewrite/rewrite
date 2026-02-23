import shutil

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import MethodDeclaration
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


def test_async_def():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        async def main():
            pass
        """
    ))


@requires_ty_cli
def test_async_def_type_attribution():
    """Verify that async def main() -> int has a method_type with a return_type.

    Note: ty-types correctly reports the return type as CoroutineType (the actual
    runtime return type of an async function), not int (the annotated return type).
    """
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_declaration(self, method, p):
                if not isinstance(method, MethodDeclaration):
                    return method
                if method.name.simple_name != 'main':
                    return method
                if method.method_type is None:
                    errors.append("MethodDeclaration.method_type is None for async def main()")
                else:
                    if method.method_type._return_type is None:
                        errors.append("method_type.return_type is None")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        async def main() -> int:
            return 0
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
