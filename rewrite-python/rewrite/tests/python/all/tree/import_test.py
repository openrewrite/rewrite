from rewrite.java import Import
from rewrite.java.support_types import JavaType
from rewrite.java.tree import MethodInvocation
from rewrite.python import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def _assert_single_j_import(cu: CompilationUnit) -> None:
    assert len(cu.statements) == 1
    stmt = cu.statements[0]
    assert isinstance(stmt, Import), \
        f"Single import should be a J.Import, got {type(stmt).__name__}"


# noinspection PyUnresolvedReferences
def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python("import io", after_recipe=_assert_single_j_import))


# noinspection PyUnresolvedReferences
def test_simple_with_alias():
    # language=python
    RecipeSpec().rewrite_run(python("import io as io"))


# noinspection PyUnresolvedReferences
def test_unicode_char_normalization():
    # language=python
    RecipeSpec().rewrite_run(python("from .main import MaµToMan"))


# noinspection PyUnresolvedReferences
def test_qualified():
    # language=python
    RecipeSpec().rewrite_run(python("import xml.dom", after_recipe=_assert_single_j_import))


# noinspection PyUnresolvedReferences
def test_multiple():
    # language=python
    RecipeSpec().rewrite_run(python("import xml.dom ,  io "))


# noinspection PyUnresolvedReferences
def test_from():
    # language=python
    RecipeSpec().rewrite_run(python("from io import StringIO as sio"))


# noinspection PyUnresolvedReferences
def test_from_parenthesized():
    # language=python
    RecipeSpec().rewrite_run(python("from io import (StringIO as sio)"))


# noinspection PyUnresolvedReferences
def test_from_parenthesized_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("from io import (StringIO as sio , )"))


# noinspection PyUnresolvedReferences
def test_relative_import_0():
    # language=python
    RecipeSpec().rewrite_run(python("from . import bar"))


# noinspection PyUnresolvedReferences
def test_relative_import_1():
    # language=python
    RecipeSpec().rewrite_run(python("from .foo import bar"))


# noinspection PyUnresolvedReferences
def test_relative_import_2():
    # language=python
    RecipeSpec().rewrite_run(python("from ..foo import bar"))


# noinspection PyUnresolvedReferences
def test_crlf():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import foo
        import bar
        """.replace('\n', '\r\n')
    ))


def _check_getcwd_declaring_type(source_file, errors):
    """Shared checker: verify getcwd() method_type.declaring_type.fqn == 'os'."""
    assert isinstance(source_file, CompilationUnit)

    class TypeChecker(PythonVisitor):
        def visit_method_invocation(self, method, p):
            if not isinstance(method, MethodInvocation):
                return method
            if method.name.simple_name != 'getcwd':
                return method
            if method.method_type is None:
                errors.append("MethodInvocation.method_type is None for getcwd()")
            else:
                if method.method_type.declaring_type is None:
                    errors.append("method_type.declaring_type is None")
                elif method.method_type.declaring_type._fully_qualified_name != 'os':
                    errors.append(
                        f"declaring_type fqn is '{method.method_type.declaring_type._fully_qualified_name}', expected 'os'"
                    )
            return method

    TypeChecker().visit(source_file, None)


def test_qualified_import_type_attribution():
    """import os; os.getcwd() → declaring_type.fqn == 'os'."""
    errors = []
    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        import os
        x = os.getcwd()
        """,
        after_recipe=lambda sf: _check_getcwd_declaring_type(sf, errors),
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_from_import_type_attribution():
    """from os import getcwd; getcwd() → declaring_type.fqn == 'os'."""
    errors = []
    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from os import getcwd
        x = getcwd()
        """,
        after_recipe=lambda sf: _check_getcwd_declaring_type(sf, errors),
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_aliased_import_type_attribution():
    """import os as o; o.getcwd() → declaring_type.fqn == 'os'."""
    errors = []
    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        import os as o
        x = o.getcwd()
        """,
        after_recipe=lambda sf: _check_getcwd_declaring_type(sf, errors),
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_aliased_from_import_type_attribution():
    """from os import getcwd as gwd; gwd() → declaring_type.fqn == 'os'."""
    errors = []
    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from os import getcwd as gwd
        x = gwd()
        """,
        after_recipe=lambda sf: _check_getcwd_declaring_type(sf, errors),
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
