from rewrite.java.support_types import JavaType
from rewrite.java.tree import MethodDeclaration, VariableDeclarations
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def test_whitespace_before_colon():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo() :
            pass
        """
    ))


def test_varargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(*args) :
            pass
        """
    ))


def test_function_containing_async_identifier():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def async_foo():
            pass
        """
    ))


def test_kwargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(x, **kwargs) :
            pass
        """
    ))


# https://peps.python.org/pep-3102/
def test_keyword_only_args():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def func(x, *, kwarg1, kwarg2):
            pass
        """
    ))


def test_keyword_only_args_with_space_after_star():
    # language=python - whitespace after bare * separator
    RecipeSpec().rewrite_run(python(
        """\
        def func(x,* ,kwarg1):
            pass
        """
    ))


def test_keyword_only_args_with_newline_after_star():
    # language=python - newline after bare * separator
    RecipeSpec().rewrite_run(python(
        """\
        def foo(a, *
            , bar):
            pass
        """
    ))


def test_kwonlyargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(foo, *, bar) :
            pass
        """
    ))


def test_all():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def retry_until_timeout(cb_fn, *args, timeout=4, **kwargs):
            pass
        """
    ))


def test_trailing_comma_on_kwonly_arg():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def play(
            *proto_animations,
            lag_ratio,
        ):
            pass
        """
    ))


def test_varargs_and_kwonlyargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(foo, *bar, baz, qux=True) :
            pass
        """
    ))


def test_kwargs():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(**kwargs) :
            pass
        """
    ))


def test_param_type_with_parens():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import Tuple
        def foo(i: (Tuple[int])):
            pass
        """
    ))


def test_one_line():
    # language=python
    RecipeSpec().rewrite_run(python("def f(x): x = x + 1; return x"))


def test_line_break_after_last_param():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def f(
            x = 0,
        ):
            return x
        """
    ))


def test_generic_function_type_params():
    """Verify method_type.declared_formal_type_names for def identity[T](x: T) -> T."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_declaration(self, method, p):
                if not isinstance(method, MethodDeclaration):
                    return method
                if method.name.simple_name != 'identity':
                    return method
                if method.method_type is None:
                    errors.append("MethodDeclaration.method_type is None")
                else:
                    mt = method.method_type
                    if mt._declared_formal_type_names is None:
                        errors.append("method_type._declared_formal_type_names is None")
                    elif mt._declared_formal_type_names != ['T']:
                        errors.append(f"method_type._declared_formal_type_names is {mt._declared_formal_type_names}, expected ['T']")
                    if mt._parameter_names is not None and 'x' not in mt._parameter_names:
                        errors.append(f"parameter_names {mt._parameter_names} does not contain 'x'")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        def identity[T](x: T) -> T:
            return x
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_method_declaration_type_attribution():
    """Verify method_type on a function with typed parameters and return type."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_method_declaration(self, method, p):
                if not isinstance(method, MethodDeclaration):
                    return method
                if method.name.simple_name != 'foo':
                    return method
                if method.method_type is None:
                    errors.append("MethodDeclaration.method_type is None")
                else:
                    mt = method.method_type
                    if mt._return_type is None:
                        errors.append("method_type.return_type is None")
                    elif mt._return_type != JavaType.Primitive.Boolean:
                        errors.append(f"method_type.return_type is {mt._return_type}, expected Primitive.Boolean")
                    if mt._parameter_types is None:
                        errors.append("method_type.parameter_types is None")
                    elif len(mt._parameter_types) < 2:
                        errors.append(f"method_type.parameter_types has {len(mt._parameter_types)} elements, expected at least 2")
                    else:
                        if mt._parameter_types[0] != JavaType.Primitive.Int:
                            errors.append(f"parameter_types[0] is {mt._parameter_types[0]}, expected Primitive.Int")
                        if mt._parameter_types[1] != JavaType.Primitive.String:
                            errors.append(f"parameter_types[1] is {mt._parameter_types[1]}, expected Primitive.String")
                    if mt._parameter_names is not None and len(mt._parameter_names) >= 2:
                        if mt._parameter_names[0] != 'a':
                            errors.append(f"parameter_names[0] is '{mt._parameter_names[0]}', expected 'a'")
                        if mt._parameter_names[1] != 'b':
                            errors.append(f"parameter_names[1] is '{mt._parameter_names[1]}', expected 'b'")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        def foo(a: int, b: str) -> bool:
            return True
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_param_identifier_field_type():
    """Verify J.Identifier.field_type is JavaType.Variable for typed parameters."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_variable_declarations(self, vd, p):
                if not isinstance(vd, VariableDeclarations):
                    return vd
                for named_var in vd.variables:
                    ident = named_var.name
                    if ident.simple_name not in ('x', 'y'):
                        continue
                    if ident.field_type is None:
                        errors.append(f"Identifier '{ident.simple_name}' has field_type=None")
                    elif not isinstance(ident.field_type, JavaType.Variable):
                        errors.append(f"Identifier '{ident.simple_name}' field_type is {type(ident.field_type)}, expected JavaType.Variable")
                    else:
                        if ident.field_type._name != ident.simple_name:
                            errors.append(f"field_type._name is '{ident.field_type._name}', expected '{ident.simple_name}'")
                        if ident.field_type._type is None:
                            errors.append(f"field_type._type is None for '{ident.simple_name}'")
                return vd

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        def greet(x: int, y: str) -> bool:
            return True
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
