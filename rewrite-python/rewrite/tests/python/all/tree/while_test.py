import ast

from rewrite.java import Space
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.test import RecipeSpec, python


def test_while():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(i):
            while i < 6:
                i += 1
        """
    ))


def test_while_else():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        def test(i):
            while i < 6:
                i += 1
            else:
                i = 10
        """
    ))


def test_loop_whose_body_is_no_block():
    source = "while a:\n    x = 1\nfor i in a:\n    y = 2\n"
    cu = ParserVisitor(source, None, None).visit_Module(ast.parse(source))

    statements = []
    for statement in cu.padding.statements:
        loop = statement.element
        only = loop.body.statements[0].replace(_prefix=Space([], ' '))
        statements.append(statement.replace(element=loop.padding.replace(_body=loop.padding.body.replace(element=only))))

    # the colon a block would have printed, and `=` because the assignment is still a statement
    assert PythonPrinter().print(cu.padding.replace(_statements=statements)) == "while a: x = 1\nfor i in a: y = 2\n"
