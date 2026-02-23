import json
import shutil
import subprocess
import tempfile

import pytest

from rewrite.java.support_types import JavaType
from rewrite.java.tree import MethodInvocation
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

requires_ty_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed"
)


def _ty_types_has_module_name() -> bool:
    """Check if the installed ty-types CLI provides moduleName on function descriptors."""
    if shutil.which('ty-types') is None:
        return False
    try:
        with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
            f.write('from os.path import join\njoin("a", "b")\n')
            fname = f.name
        result = subprocess.run(['ty-types', fname], capture_output=True, text=True, timeout=30)
        data = json.loads(result.stdout)
        return any(
            d.get('kind') == 'function' and 'moduleName' in d
            for d in data.get('types', {}).values()
        )
    except Exception:
        return False


requires_module_name = pytest.mark.skipif(
    not _ty_types_has_module_name(),
    reason="ty-types CLI does not provide moduleName on function descriptors"
)


def test_no_select():
    # language=python
    RecipeSpec().rewrite_run(python("assert len('a')"))


def test_select():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'a'.islower( )"))


def test_invoke_function_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert a(0)(1)"))


def test_qualified_receiver_with_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        class Foo:
            def getter(self, row):
                pass
            def foo(self):
                return (self.getter )(1)
        """
    ))


def test_invoke_lambda_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert (lambda x: x)(1)"))


def test_invoke_array_access_receiver():
    # language=python
    RecipeSpec().rewrite_run(python("assert a[0](1)"))


def test_sequence_unpack():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        td = datetime.timedelta(*(5, 12, 30, 0))
        """
    ))


def test_mixed_order():
    # language=python
    RecipeSpec().rewrite_run(python("set_stroke(color=color, width=width, *args, **kwargs)"))


def test_named_arguments():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        td = datetime.datetime(year=2024, month= 1, day =1)
        """
    ))


def test_dict_unpack():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import datetime
        dt = datetime.datetime(**{'year': 2023, 'month': 10, 'day': 1, 'hour': 12, 'minute': 30})
        """
    ))


def test_keyword_argument():
    # language=python
    RecipeSpec().rewrite_run(python("l = sorted([1, 2, 3], key=lambda x: x, reverse=True)"))


def test_no_name():
    # language=python
    RecipeSpec().rewrite_run(python("v = (a)()"))


@requires_ty_cli
def test_builtin_function_type_attribution():
    """Verify type attribution on a builtin function call like len()."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'len':
                    return method
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None for len()")
                else:
                    if method.method_type.name != 'len':
                        errors.append(f"method_type.name is '{method.method_type.name}', expected 'len'")
                    if method.method_type._return_type is None:
                        errors.append("method_type.return_type is None")
                    elif method.method_type._return_type != JavaType.Primitive.Int:
                        errors.append(f"method_type.return_type is {method.method_type._return_type}, expected Primitive.Int")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        'x = len("hello")',
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_ty_cli
def test_string_method_type_attribution():
    """Verify type attribution on a string method call like str.upper()."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'upper':
                    return method
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None for upper()")
                else:
                    if method.method_type.declaring_type is not None:
                        dt = method.method_type.declaring_type
                        if dt._fully_qualified_name != 'str':
                            errors.append(f"declaring_type fqn is '{dt._fully_qualified_name}', expected 'str'")
                    if method.method_type._return_type is None:
                        errors.append("method_type.return_type is None")
                    elif method.method_type._return_type != JavaType.Primitive.String:
                        errors.append(f"method_type.return_type is {method.method_type._return_type}, expected Primitive.String")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        'x = "hello".upper()',
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_ty_cli
def test_stdlib_function_type_attribution():
    """Verify type attribution on a stdlib function call like os.getcwd()."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'getcwd':
                    return method
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None for os.getcwd()")
                else:
                    if method.method_type.declaring_type is not None:
                        dt = method.method_type.declaring_type
                        if dt._fully_qualified_name != 'os':
                            errors.append(f"declaring_type fqn is '{dt._fully_qualified_name}', expected 'os'")
                    if method.method_type._return_type is None:
                        errors.append("method_type.return_type is None")
                    elif method.method_type._return_type != JavaType.Primitive.String:
                        errors.append(f"method_type.return_type is {method.method_type._return_type}, expected Primitive.String")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        import os
        x = os.getcwd()
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_ty_cli
def test_generic_call_site_return_type():
    """Verify method_invocation_type returns call-site-specific return type for generic functions."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'identity':
                    return method
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None for identity()")
                else:
                    mt = method.method_type
                    if mt._return_type is None:
                        errors.append("method_type.return_type is None")
                    elif mt._return_type != JavaType.Primitive.Int:
                        errors.append(f"method_type.return_type is {mt._return_type}, expected Primitive.Int")
                    if mt._declared_formal_type_names is None:
                        errors.append("method_type._declared_formal_type_names is None")
                    elif 'T' not in mt._declared_formal_type_names:
                        errors.append(f"method_type._declared_formal_type_names is {mt._declared_formal_type_names}, expected to contain 'T'")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        def identity[T](x: T) -> T:
            return x
        result = identity(42)
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


@requires_module_name
def test_bare_function_declaring_type_has_module():
    """Verify that a bare function call imported from a module gets a declaring type with the module name."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'join':
                    return method
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None for join()")
                else:
                    dt = method.method_type.declaring_type
                    if dt is None:
                        errors.append("method_type.declaring_type is None for join()")
                    elif not hasattr(dt, '_fully_qualified_name') or 'posixpath' not in dt._fully_qualified_name:
                        errors.append(
                            f"declaring_type fqn is '{getattr(dt, '_fully_qualified_name', '?')}', "
                            f"expected to contain 'posixpath' (os.path module)"
                        )
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from os.path import join
        x = join("a", "b")
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
