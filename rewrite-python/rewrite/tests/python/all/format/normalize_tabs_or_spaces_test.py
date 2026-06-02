from rewrite.python import IntelliJ
from rewrite.python.format import NormalizeTabsOrSpacesVisitor
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


def test_tabs_to_spaces():
    style = IntelliJ.tabs_and_indents().with_use_tab_character(False).with_tab_size(3)
    # noinspection PyInconsistentIndentation
    rewrite_run(
        # language=python
        python(
            """
            class Foo:
            	# comment1
            	def f(self):
            		pass	# comment2	comment3
            """,
            """
            class Foo:
               # comment1
               def f(self):
                  pass	# comment2	comment3
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(NormalizeTabsOrSpacesVisitor(style))
        )
    )


def test_mixed_to_spaces():
    style = IntelliJ.tabs_and_indents().with_use_tab_character(False)
    # noinspection PyInconsistentIndentation
    rewrite_run(
        # language=python
        python(
            """
            class Foo:
                def f(self):
                	pass
            """,
            """
            class Foo:
                def f(self):
                    pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(NormalizeTabsOrSpacesVisitor(style))
        )
    )

def test_mixed_to_tabs():
    style = IntelliJ.tabs_and_indents().with_use_tab_character(True)
    # noinspection PyInconsistentIndentation
    rewrite_run(
        # language=python
        python(
            """
            class Foo:
                def f(self):
                	pass
            """,
            """
            class Foo:
            	def f(self):
            		pass
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(NormalizeTabsOrSpacesVisitor(style))
        )
    )
