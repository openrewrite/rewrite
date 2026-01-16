from rewrite.test import RecipeSpec, python


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
