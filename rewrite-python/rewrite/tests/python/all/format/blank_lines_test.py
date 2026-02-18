from rewrite.java import Space
from rewrite.python import IntelliJ, NormalizeFormatVisitor
from rewrite.python.format import BlankLinesVisitor
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


def test_remove_leading_module_blank_lines():
    rewrite_run(
        # language=python
        python(
            """


            print('foo')
            """,
            """print('foo')
            """
        ),
        spec=RecipeSpec()
        .with_recipe(from_visitor(BlankLinesVisitor(IntelliJ.blank_lines())))
    )


def test_blank_lines_between_top_level_declarations():
    rewrite_run(
        # language=python
        python(
            """\
            class Foo:
                pass
            class Bar:
                pass
            def f():
                pass
            """,
            """\
            class Foo:
                pass


            class Bar:
                pass


            def f():
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(IntelliJ.blank_lines()))
        )
    )


def test_blank_lines_between_class_methods():
    rewrite_run(
        # language=python
        python(
            """\
            class Foo:
                def foo(self):
                    pass
                def bar(self):
                    pass
                class Nested:
                    pass
            """,
            """\
            class Foo:
                def foo(self):
                    pass

                def bar(self):
                    pass

                class Nested:
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(IntelliJ.blank_lines()))
        )
    )


def test_blank_lines_after_top_level_imports():
    style = IntelliJ.blank_lines()
    style = style.with_minimum(
        style.minimum.with_after_top_level_imports(3)
    )
    rewrite_run(
        # language=python
        python(
            """\
            import os
            import sys
            class Foo:
                pass
            """,
            """\
            import os
            import sys



            class Foo:
                pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(style))
        )
    )


def test_local_imports():
    style = IntelliJ.blank_lines()
    style = style.with_minimum(
        style.minimum.with_after_local_imports(1)
    )
    rewrite_run(
        # language=python
        python(
            """\
            class Foo:
                import os
                print('1')
                import sys
                print('2')
            """,
            """\
            class Foo:
                import os

                print('1')
                import sys

                print('2')
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(style))
        )
    )


def test_before_first_method():
    style = IntelliJ.blank_lines()
    style = style.with_minimum(
        style.minimum.with_before_first_method(2)
    )
    rewrite_run(
        # language=python
        python(
            """\
            class Foo:
                def __init__(self):
                    pass


            class Bar:
                x = 1
                def __init__(self):
                    pass
            """,
            """\
            class Foo:


                def __init__(self):
                    pass


            class Bar:
                x = 1

                def __init__(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(style))
        )
    )


def test_max_lines_in_code():
    style = IntelliJ.blank_lines()
    style = style.with_keep_maximum(
        style.keep_maximum.with_in_code(1)
    )
    rewrite_run(
        # language=python
        python(
            """\
            def foo():
                print('foo')


                print('bar')
            """,
            """\
            def foo():
                print('foo')

                print('bar')
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(BlankLinesVisitor(style))
        )
    )
