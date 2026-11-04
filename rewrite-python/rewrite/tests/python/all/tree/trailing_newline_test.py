import ast

from rewrite import Markers, random_id
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.markers import SuppressNewline
from rewrite.python.printer import PythonPrinter
from rewrite.test import RecipeSpec, python


def test_suppressed_newline():
    source = "x = 1\n"
    cu = ParserVisitor(source, None, None).visit_Module(ast.parse(source))
    cu = cu.replace(markers=Markers(random_id(), [SuppressNewline(random_id())]))

    assert PythonPrinter().print(cu) == "x = 1"
    # the source file prints itself into a capture of another class than the printer's own
    assert cu.print_all() == "x = 1"


def test_trailing_newline():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass
        """
    ))


def test_trailing_blank_line():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass

        """
    ))


def test_multiple_trailing_blank_lines():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class C:
            pass


        """
    ))


def test_trailing_blank_line_after_method():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Class:
            def __init__(self):
                print('hello')

        """
    ))


def test_trailing_blank_line_with_fstring_debug():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        class Class:
            def __init__(self):
                print(f"{self.attr=}")

        """
    ))
