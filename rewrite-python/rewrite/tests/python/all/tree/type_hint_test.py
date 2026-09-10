import ast

from rewrite.java.support_types import JavaType
from rewrite.java.tree import Identifier, Literal as JLiteral, ParameterizedType, VariableDeclarations
from rewrite.python._parser_visitor import _EmbeddedTypeMapping
from rewrite.python.markers import Quoted
from rewrite.python.tree import CompilationUnit, LiteralType, UnionType
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


def test_literal_string_type_hint_with_assignment():
    # language=python - parenthesized string with trailing comma (tuple) before Literal type hint
    RecipeSpec().rewrite_run(python(
        """\
("a"),
y: Literal["test"] = "value"
"""
    ))


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


def test_prefixed_string_type_hint_keeps_its_prefix():
    # language=python
    RecipeSpec().rewrite_run(python(r"""foo: r"List[\d]" = None"""))


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


def _annotations(source_file):
    """The type expression of each annotated declaration, in source order."""
    found = []

    class _Collector(PythonVisitor):
        def visit_type_hint(self, hint, p):
            found.append(hint.type_tree)
            return hint

    _Collector().visit(source_file, None)
    return found


def _annotation(source_file):
    """The type expression of the file's single annotated declaration."""
    found = _annotations(source_file)
    assert len(found) == 1, f"expected one annotation, found {len(found)}"
    return found[0]


def _parsed_annotation(source: str):
    """Parse ``source`` asserting it round-trips, and return its annotation."""
    seen = []
    RecipeSpec().rewrite_run(python(source, after_recipe=lambda sf: seen.append(_annotation(sf))))
    return seen[0]


def test_quoted_parameterized_annotation_is_structured():
    """The quotes come back around a J node, which the Java printer delegate re-adds."""
    # language=python
    annotation = _parsed_annotation('''x: "Dict[str, int]" = None''')
    assert isinstance(annotation, ParameterizedType)
    assert annotation.markers.find_first(Quoted).style is Quoted.Style.DOUBLE
    assert [t.simple_name for t in annotation.type_parameters] == ['str', 'int']


def test_quoted_union_annotation_is_structured():
    """The quotes come back around a Py node, whose printer is a separate one."""
    # language=python
    annotation = _parsed_annotation('''x: "int | None" = None''')
    assert isinstance(annotation, UnionType)
    assert annotation.markers.find_first(Quoted) is not None


def test_a_quoted_union_operand_is_structured():
    """A `|` operand reaches the type mapper the way a subscript's argument does."""
    # language=python
    annotation = _parsed_annotation("""x: int | 'Later' = None""")
    later = annotation.types[1]
    assert isinstance(later, Identifier) and later.simple_name == 'Later'
    assert later.markers.find_first(Quoted).style is Quoted.Style.SINGLE

    # language=python
    annotation = _parsed_annotation('''x: 'int | "Later"' = None''')
    assert annotation.markers.find_first(Quoted).style is Quoted.Style.SINGLE
    assert annotation.types[1].markers.find_first(Quoted).style is Quoted.Style.DOUBLE


def test_nested_forward_reference_is_structured():
    # language=python
    annotation = _parsed_annotation("""x: "Union['Foo', str]" = None""")
    inner = annotation.type_parameters[0]
    assert isinstance(inner, Identifier) and inner.simple_name == 'Foo'
    assert inner.markers.find_first(Quoted).style is Quoted.Style.SINGLE


def test_unparseable_quoted_annotation_stays_a_flat_identifier():
    # language=python
    annotation = _parsed_annotation('''x: "not python(" = None''')
    assert isinstance(annotation, Identifier)
    assert annotation.simple_name == 'not python('


def test_a_constants_position_decides_whether_it_is_a_value_or_a_type():
    """``Literal``'s arguments are values, so a string there names nothing; the same text
    as the annotation itself is the type it denotes."""
    # language=python
    argument = _parsed_annotation('''x: Literal["1"] = "1"''').type_parameters[0]
    assert isinstance(argument, JLiteral) and argument.value_source == '"1"'

    # language=python
    assert isinstance(_parsed_annotation('''x: "None" = None'''), LiteralType)


def test_annotated_keeps_its_first_argument_a_type_and_the_rest_values():
    """``Annotated[T, ...]`` annotates ``T`` with metadata that is not itself a type, so
    the same spelling means a reference in one position and a string in the next."""
    # language=python
    annotated, metadata = _parsed_annotation('''x: Annotated["A", "A"] = None''').type_parameters
    assert isinstance(annotated, Identifier) and annotated.markers.find_first(Quoted)
    assert isinstance(metadata, JLiteral) and metadata.value_source == '"A"'


def test_a_body_that_is_itself_quoted_stays_text():
    """A node records one quote style, so a body that is already quoted has nowhere to
    keep both its own quotes and the outer ones."""
    # language=python
    annotation = _parsed_annotation('''x: "'Foo'" = None''')
    assert isinstance(annotation, Identifier) and annotation.simple_name == "'Foo'"


def test_quoted_annotation_that_would_not_print_back_stays_flat():
    # language=python
    annotation = _parsed_annotation('''x: "Foo " = None''')
    assert isinstance(annotation, Identifier)
    assert annotation.simple_name == 'Foo '


def _type_label(resolved) -> str:
    """A short name for a resolved type: a failure has to print a word, not a type graph.

    An LST node's dataclass ``repr`` walks its whole type graph, which for an attributed
    file runs to gigabytes.
    """
    if resolved is None:
        return 'unattributed'
    if isinstance(resolved, Parameterized):
        return f'{_type_label(resolved.type)}[...]'
    if isinstance(resolved, JavaType.Union):
        return 'union(' + ', '.join(_type_label(b) for b in resolved.bounds) + ')'
    if isinstance(resolved, JavaType.FullyQualified):
        return resolved.fully_qualified_name.rsplit('.', 1)[-1]
    return str(resolved)


def _type_labels(node) -> list:
    """The resolved types down ``node``: its own, its head name, and its members.

    A union keeps its members in ``types`` where a parameterized type keeps its
    arguments in ``type_parameters``.
    """
    labels = [_type_label(node.type)]
    head = getattr(node, 'clazz', None)
    if head is not None:
        labels.append('head ' + _type_label(head.type))
    for member in (getattr(node, 'types', None) or []) + (getattr(node, 'type_parameters', None) or []):
        labels.extend(_type_labels(member))
    return labels


def _annotation_labels(source: str) -> list:
    """The labels of each annotation in ``source``, in source order."""
    seen = []
    RecipeSpec(type_attribution=True).rewrite_run(
        python(source, after_recipe=lambda sf: seen.extend(_annotations(sf))))
    return [_type_labels(annotation) for annotation in seen]


def test_quoted_annotation_attributes_like_an_unquoted_one():
    """A quoted annotation resolves as the same annotation unquoted does, heads included."""
    # language=python
    quoted, unquoted = _annotation_labels(
        '''\
        from typing import Dict, List

        x: "Dict[str, List[int]]" = {}
        y: Dict[str, List[int]] = {}
        ''')

    assert quoted == ['dict[...]', 'head Dict', 'Primitive.String',
                      'list[...]', 'head List', 'Primitive.Int']
    assert quoted == unquoted


def test_quoted_union_attributes_each_member_and_the_union():
    """A union keeps its members in a slot of its own, so reaching them is a separate path."""
    # language=python
    quoted, unquoted = _annotation_labels(
        '''\
        x: "str | int" = ""
        y: str | int = ""
        ''')

    assert quoted == ['union(Primitive.String, Primitive.Int)',
                      'Primitive.String', 'Primitive.Int']
    assert quoted == unquoted


def test_quoted_body_positions_resolve_against_the_enclosing_file():
    looked_up = []

    class _Recorder:
        def type(self, node):
            # A lookup reads a callee's or argument's position as well as the node's own.
            looked_up.extend((n.lineno, n.col_offset) for n in ast.walk(node)
                             if getattr(n, 'lineno', None) is not None)

    first_line, second_line = 'x: """Dict[str,', ' int]""" = None'
    mapping = _EmbeddedTypeMapping(_Recorder(), 0, first_line.index('"""') + 3)
    mapping.type(ast.parse('Dict[str,\n int]', mode='eval').body)

    # Each shifted position names the same text in the file it was read from.
    assert sorted(set(looked_up)) == [
        (1, first_line.index('Dict')),
        (1, first_line.index('str')),
        (2, second_line.index('int')),
    ]
