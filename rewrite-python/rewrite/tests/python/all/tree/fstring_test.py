import pytest

from rewrite.test import RecipeSpec, python


@pytest.mark.parametrize('style', ["'", '"', '"""', "'''"])
def test_delimiters(style: str):
    RecipeSpec().rewrite_run(python(f"a = f{style}foo{style}"))


def test_multiline():
    # language=python
    RecipeSpec().rewrite_run(python("""
        a = f'''foo
        {None}
        bar'''
        """
           ))


def test_concat_fstring_1():
    # language=python
    RecipeSpec().rewrite_run(python("""
         print(
             f"Foo. "
             f"Bar {None}"
         )
        """
           ))


def test_concat_fstring_2():
    # language=python
    RecipeSpec().rewrite_run(python("""
         print(
             f"Foo. "
             "Bar"
         )
        """
           ))


def test_concat_fstring_3():
    # language=python
    RecipeSpec().rewrite_run(python("""
         print(
             f"Foo. "
             "Bar"
             f"Baz {None}"
         )
        """
           ))


def test_concat_fstring_4():
    # language=python
    RecipeSpec().rewrite_run(python("""
         print(
             "Foo. "
             f"Bar"
         )
        """
           ))


def test_concat_fstring_5():
    # language=python
    RecipeSpec().rewrite_run(python("""
        def query41():
            return (
                "SELECT * "
                "FROM table "
                f"WHERE var = {var}"
            )
        """
           ))


def test_concat_fstring_6():
    # language=python
    RecipeSpec().rewrite_run(python("""
        f'{dict((x, x) for x in range(11)) | dict((x, x) for x in range(11))}'
        f'{ dict((x, x) for x in range(3)) | dict((x, x) for x in range(3)) }'
        """
           ))


def test_concat_fstring_9():
    # language=python
    RecipeSpec().rewrite_run(python('print(f"Progress: {output.escaped_title[:30]:<30} {default_output:>161}")'))


def test_empty():
    # language=python
    RecipeSpec().rewrite_run(python("a = f''"))


def test_raw():
    # language=python
    RecipeSpec().rewrite_run(python("a = rf'raw'"))
    # language=python
    RecipeSpec().rewrite_run(python("a = Fr'raw'"))


def test_no_expr():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'foo'"))


def test_only_expr():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{None}'"))


def test_expr_with_prefix():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{ None}'"))


def test_expr_with_suffix():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{None }'"))


def test_embedded_expr():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'-{None}-'"))


def test_embedded_set():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'-{ {1, 2} }-'"))


def test_escaped_braces():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{{foo{{bar}}baz}}'"))


def test_escaped_braces_with_expr():
    # Test f-string with escaped braces surrounding an expression
    # f"${{{expr}}}" produces "${<value>}" - dollar, then escaped {, then expr, then escaped }
    # language=python
    RecipeSpec().rewrite_run(python(
        '''x = f"${{{name.upper()}}}"
'''
    ))


def test_debug():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{None=}'"))
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1=}'"))


def test_debug_with_trailing_whitespace():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1= !a}'"))


def test_debug_with_format_spec():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{last_error=:#x}'"))
    # debug `=` with format spec after other expressions
    # language=python
    RecipeSpec().rewrite_run(python("""a = f"kernel32.{name} ({last_error=:#x})" """))


def test_all_specifiers():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1=!a:0>6}'"))


def test_conversion():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{"foo"!a}'"""))
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{"foo"!s}'"""))
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{"foo"!r}'"""))


def test_conversion_and_format():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{"foo"!a:n}'"""))


def test_conversion_and_format_expr():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{"foo"!s:<{5*2}}'"""))


def test_nested_fstring_conversion_and_format_expr():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{f"foo"!s:<{5*2}}'"""))


def test_simple_format_spec():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1:n}'"))


def test_format_spec_with_precision_and_conversion():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1:.2f}'"))


def test_nested_fstring_expression():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{f"{1}"}'"""))


def test_nested_fstring_format():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{f"{1}"}'"""))


def test_format_value():
    # language=python
    RecipeSpec().rewrite_run(python("a = f'{1:.{2 + 3}f}'"))
    # language=python
    RecipeSpec().rewrite_run(python('''a = f"{'abc':>{2*3}}"'''))


def test_adjoining_expressions():
    # language=python
    RecipeSpec().rewrite_run(python("""a = f'{1}{0}'"""))


def test_nested_fstring_in_expression():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        name = "Alice"
        score = 87
        max_score = 100

        percentage = (score / max_score) * 100

        message = f"{name} scored {score}/{max_score}, which is {f'{percentage:.1f}'}% of the total."

        print(message)
        """
    ))
