from rewrite import Markers
from rewrite.java import Block, P, J, MethodInvocation, Space, JRightPadded
from rewrite.python import MinimumViableSpacingVisitor, PythonVisitor
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


def test_semicolon():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a'); print('b')
            """
        ),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def test_statement_without_prefix():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a')
            """,
            """
            def foo():
                print('a')
            print('a')
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(DuplicateMethod()),
            from_visitor(MinimumViableSpacingVisitor())
        )
    )


def test_statement_with_semicolon():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a');
            """,
            """
            def foo():
                print('a');print('a');
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(DuplicateMethod()),
            from_visitor(MinimumViableSpacingVisitor())
        )
    )


class DuplicateMethod(PythonVisitor):
    def visit_block(self, block: Block, p: P) -> J:
        if block.statements and isinstance(block.statements[0], MethodInvocation):
            existing_rp = block.padding.statements[0]
            stmt = existing_rp.element.replace(prefix=Space.EMPTY)
            new_rp = JRightPadded(stmt, Space.EMPTY, existing_rp.markers)
            new_statements = list(block.padding.statements) + [new_rp]
            return block.padding.replace(statements=new_statements)
