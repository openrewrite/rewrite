from typing import Type, Optional, TypeVar

from rewrite import Tree
from rewrite.java import Literal, JavaType
from rewrite.python import PythonVisitor
from rewrite.test import RecipeSpec, python

T = TypeVar('T', bound=Tree)


def test_none():
    # language=python
    RecipeSpec().rewrite_run(python("assert None", after_recipe=check_first_literal_type(JavaType.Primitive.None_)))


def test_boolean():
    # language=python
    RecipeSpec().rewrite_run(python("assert True", after_recipe=check_first_literal_type(JavaType.Primitive.Boolean)))
    RecipeSpec().rewrite_run(python("assert False", after_recipe=check_first_literal_type(JavaType.Primitive.Boolean)))


def test_dec_int():
    # language=python
    RecipeSpec().rewrite_run(python("assert 0", after_recipe=check_first_literal_type(JavaType.Primitive.Int)))


def test_hex_int():
    # language=python
    RecipeSpec().rewrite_run(python("assert 0x1f", after_recipe=check_first_literal_type(JavaType.Primitive.Int)))


def test_fraction():
    # language=python
    RecipeSpec().rewrite_run(python("assert 0.000", after_recipe=check_first_literal_type(JavaType.Primitive.Double)))


def test_fraction_leading_dot():
    # language=python
    RecipeSpec().rewrite_run(python("assert .0", after_recipe=check_first_literal_type(JavaType.Primitive.Double)))


def test_large_int():
    # language=python
    RecipeSpec().rewrite_run(python("assert 0xC03A0019", after_recipe=check_first_literal_type(JavaType.Primitive.Int)))


# def test_byte_string_concatenation():
#     # language=python
#     RecipeSpec().rewrite_run(python("assert b'hello' b'world'", after_recipe=check_first_literal_type(JavaType.Primitive.String)))


def test_bigint():
    # language=python
    RecipeSpec().rewrite_run(python("assert 9223372036854775808", after_recipe=check_first_literal_type(JavaType.Primitive.Int)))


def test_single_quoted_string():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'foo'"))


def test_string_with_space():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'foo bar'"))


def test_string_with_escaped_quote():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'foo\\'bar'"))


def test_string_with_flags():
    # language=python
    RecipeSpec().rewrite_run(python("assert u'\u0394 (delta)'"))


def test_false_string_literal_concatenation():
    # language=python
    RecipeSpec().rewrite_run(python("""
           "a"
           b"b"
           """
           ))


def test_string_literal_concatenation_1():
    # language=python
    RecipeSpec().rewrite_run(python("assert 'a' 'b'"))


def test_string_literal_concatenation_2():
    # language=python
    RecipeSpec().rewrite_run(python(
        """ \
        assert ('a'
                'b'
                'c'
                )
        """
    ))


def test_string_literal_concatenation_with_comments():
    # language=python
    RecipeSpec().rewrite_run(python(
        """ \
        assert ('a'
                'b'
                # foo
                'c'
                )
        """
    ))


def test_bytes():
    # language=python
    RecipeSpec().rewrite_run(python("assert b'\x68\x65\x6c\x6c\x6f'"))


def test_multiline_string_with_flags():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        assert '''this is a
        multiline string
        '''
        """
    ))


def test_string_concatenation():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        raise Exception(
            'foo'
            'bar'
        )
        """
    ))


def test_double_quoted_string():
    # language=python
    RecipeSpec().rewrite_run(python('assert "foo"'))


def test_complex_number_lowercase():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1j"))


def test_complex_number_uppercase():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1J"))


def test_complex_number_uppercase_in_subscript():
    # language=python
    RecipeSpec().rewrite_run(python("x: Literal[1J]"))


def test_string_with_escaped_single_quote_in_type_hint():
    # language=python - escaped single quote in double-quoted string
    RecipeSpec().rewrite_run(python("x: Literal[\"\\'\"]\n"))


def test_string_with_escaped_backslash_in_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(r'x: Literal["\\"]'))


def test_string_with_escaped_newline_in_type_hint():
    # language=python
    RecipeSpec().rewrite_run(python(r'x: Literal["\n"]'))


def test_empty_string_concatenation():
    # language=python - empty string prefix followed by non-empty string
    RecipeSpec().rewrite_run(python("x = ''U'''w'''U''"))


def test_mixed_byte_string_prefix_concatenation():
    # language=python - rb and br prefixes should both be recognized as byte strings
    RecipeSpec().rewrite_run(python('x = rb"haha" br"ust go"'))


def test_string_followed_by_fstring_separate_lines():
    # language=python - string and f-string on separate lines should NOT be concatenated
    RecipeSpec().rewrite_run(python(
        '''\
class Foo:
    a = 2
    "str"
    f"{int}"
'''
    ))


def test_string_with_unicode_tamil():
    # language=python - Tamil script characters in string
    RecipeSpec().rewrite_run(python('x = "மெல்லினம்"'))


def test_string_with_superscript():
    # language=python - superscript characters in string
    RecipeSpec().rewrite_run(python('x = "²"'))


def test_string_with_unicode_mixed_superscript():
    # language=python - mixed German and superscript characters
    RecipeSpec().rewrite_run(python('x = "ä²ö"'))


def test_string_with_unicode_complex():
    # language=python - German chars mixed with multiple superscripts
    RecipeSpec().rewrite_run(python('x = "ää²¹öö"'))


def test_unicode_string_in_simple_tuple():
    # language=python - Tamil string in tuple
    RecipeSpec().rewrite_run(python('x = ("மெல்லினம்", y)'))


def test_unicode_string_in_nested_tuple():
    # language=python - Tamil string in nested tuple
    RecipeSpec().rewrite_run(python('x = (("மெல்லினம்", y),)'))


def test_unicode_string_in_tuple():
    # language=python - Unicode strings in tuple (as seen in data_provider decorator)
    RecipeSpec().rewrite_run(python(
        '''\
x = (
    ("மெல்லினம்", [NAME]),
    ("²", [ERRORTOKEN]),
    ("ä²ö", [NAME, ERRORTOKEN, NAME]),
    ("ää²¹öö", [NAME, ERRORTOKEN, NAME]),
)
'''
    ))


def test_unicode_string_data_provider_style():
    # language=python - Full data_provider style structure
    RecipeSpec().rewrite_run(python(
        '''\
@data_provider(
    (
        ("1foo1", [NUMBER, NAME]),
        ("மெல்லினம்", [NAME]),
        ("²", [ERRORTOKEN]),
        ("ä²ö", [NAME, ERRORTOKEN, NAME]),
        ("ää²¹öö", [NAME, ERRORTOKEN, NAME]),
    )
)
def test_token_types(self, code, types):
    actual_types = [t.type for t in _get_token_list(code)]
    assert actual_types == types + [ENDMARKER]
'''
    ))


def find_first(tree: Tree, clazz: Type[T]) -> T:
    class Find(PythonVisitor[list[Tree]]):
        def visit(self, t: Tree, p: list[Tree]) -> Optional[Tree]:
            if isinstance(t, clazz):
                p.append(t)
                return t
            return super().visit(t, p)

    found = []
    Find().visit(tree, found)
    return found[0]

def check_first_literal_type(expected_type):
    def after_recipe(cu):
        assert find_first(cu, Literal).type == expected_type
    return after_recipe

