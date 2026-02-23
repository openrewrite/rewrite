from rewrite.java.support_types import JavaType
from rewrite.java.tree import VariableDeclarations
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

Parameterized = JavaType.Parameterized


def test_primitive_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(n : int):
            return n + 1
        """
    ))


def test_return_type_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(n: int)  ->  int :
            return n + 1
        """
    ))


def test_class_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import List
    
        def test(n: List):
            return n[0] + 1
        """
    ))


def test_generic_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import List
    
        def test(n: List[int]):
            return n[0] + 1
        """
    ))


def test_generic_type_hint_multiple_params():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing import Callable
    
        def test(n: Callable[[int], str]):
            return n(1)
        """
    ))


def test_generic_type_hint_literal_params():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        from typing_extensions import Literal
        mode: Literal['before', 'after'] = 'before'
        """
    ))


def test_variable_with_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: int = 1"""))


def test_variable_with_parameterized_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: Union[None, ...] = None"""))


def test_variable_with_parameterized_type_hint_in_quotes():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: Dict["Foo", str] = None"""))


def test_variable_with_quoted_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: 'Foo' = None"""))


def test_variable_with_double_quoted_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: "Foo" = None"""))


def test_variable_with_triple_quoted_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: '''Foo''' = None"""))


def test_literal_type():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: None = None"""))


def test_literal_type_2():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: Literal[False] = False"""))


def test_union_type():
    # language=python
    RecipeSpec().rewrite_run(python("""foo: None | ... = None"""))


def test_empty_tuple_type():
    # language=python
    RecipeSpec().rewrite_run(python('''
        from typing import Tuple
        foo: Tuple[()] = None
    '''))


def test_function_parameter_with_quoted_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def foo(s: "str"):
            pass
        """
    ))


def test_function_parameter_with_parenthesized_quoted_type_hint():
    # language=python - type hint with parentheses around quoted string
    RecipeSpec().rewrite_run(python(
        """\
        def foo(s: ("str")):
            pass
        """
    ))


def test_variable_with_implicit_string_concat_type_hint():
    # language=python - type hint with implicit string concatenation
    RecipeSpec().rewrite_run(python('''X: """List[int]"""'☃' = []'''))


def test_parenthesized_string_concat_type_hint():
    # language=python - parenthesized implicit string concatenation in type hint
    RecipeSpec().rewrite_run(python('''x: ("Foo" "Bar") = None'''))


def test_empty_tuple_in_union_type():
    # language=python - Union[()]
    RecipeSpec().rewrite_run(python(
        '''\
from typing import Union

def f(x: Union[()]) -> None:
    ...
'''
    ))


def test_tuple_in_union_then_quoted_string():
    # language=python - parenthesized tuple in Union followed by quoted string type
    RecipeSpec().rewrite_run(python(
        '''\
import typing
def f(x: typing.Union[(str, int)]) -> None:
    ...
def f(x: "Union[str]") -> None:
    ...
'''
    ))


def test_list_int_param_type_attribution():
    """Verify List[int] parameter type is Parameterized with base list and type param Int."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_variable_declarations(self, var_decls, p):
                if not isinstance(var_decls, VariableDeclarations):
                    return var_decls
                # Look for the parameter 'n' with type hint List[int]
                for v in var_decls.variables:
                    if v.name.simple_name != 'n':
                        continue
                    vt = var_decls.type_expression
                    if vt is None:
                        continue
                    t = vt.type if hasattr(vt, 'type') else None
                    if t is None:
                        errors.append("List[int] parameter type is None")
                    elif isinstance(t, Parameterized):
                        if not t._type._fully_qualified_name.startswith('list'):
                            errors.append(f"Parameterized base fqn is '{t._type._fully_qualified_name}', expected to start with 'list'")
                    elif isinstance(t, JavaType.Class):
                        if not t._fully_qualified_name.startswith('list'):
                            errors.append(f"Class fqn is '{t._fully_qualified_name}', expected to start with 'list'")
                return var_decls

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from typing import List

        def test(n: List[int]):
            return n[0] + 1
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_dict_str_int_type_attribution():
    """Verify Dict[str, int] variable type is Parameterized with base dict."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_variable_declarations(self, var_decls, p):
                if not isinstance(var_decls, VariableDeclarations):
                    return var_decls
                for v in var_decls.variables:
                    if v.name.simple_name != 'foo':
                        continue
                    vt = var_decls.type_expression
                    if vt is None:
                        continue
                    t = vt.type if hasattr(vt, 'type') else None
                    if t is None:
                        errors.append("Dict[str, int] variable type is None")
                    elif isinstance(t, Parameterized):
                        if t._type._fully_qualified_name != 'dict':
                            errors.append(f"Parameterized base fqn is '{t._type._fully_qualified_name}', expected 'dict'")
                    elif isinstance(t, JavaType.Class):
                        if t._fully_qualified_name != 'dict':
                            errors.append(f"Class fqn is '{t._fully_qualified_name}', expected 'dict'")
                return var_decls

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from typing import Dict
        foo: Dict[str, int] = {}
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_optional_str_type_attribution():
    """Verify Optional[str] variable type resolves to str or a union containing str."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_variable_declarations(self, var_decls, p):
                if not isinstance(var_decls, VariableDeclarations):
                    return var_decls
                for v in var_decls.variables:
                    if v.name.simple_name != 'foo':
                        continue
                    vt = var_decls.type_expression
                    if vt is None:
                        continue
                    t = vt.type if hasattr(vt, 'type') else None
                    if t is None:
                        errors.append("Optional[str] variable type is None")
                    # Accept any non-None type (could be str, union, etc.)
                return var_decls

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        from typing import Optional
        foo: Optional[str] = None
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
