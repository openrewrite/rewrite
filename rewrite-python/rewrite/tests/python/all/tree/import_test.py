from rewrite.java import Import
from rewrite.python import CompilationUnit
from rewrite.test import RecipeSpec, python


def _assert_single_j_import(cu: CompilationUnit) -> None:
    assert len(cu.statements) == 1
    stmt = cu.statements[0]
    assert isinstance(stmt, Import), \
        f"Single import should be a J.Import, got {type(stmt).__name__}"


# noinspection PyUnresolvedReferences
def test_simple():
    # language=python
    RecipeSpec().rewrite_run(python("import io", after_recipe=_assert_single_j_import))


# noinspection PyUnresolvedReferences
def test_simple_with_alias():
    # language=python
    RecipeSpec().rewrite_run(python("import io as io"))


# noinspection PyUnresolvedReferences
def test_unicode_char_normalization():
    # language=python
    RecipeSpec().rewrite_run(python("from .main import MaµToMan"))


# noinspection PyUnresolvedReferences
def test_qualified():
    # language=python
    RecipeSpec().rewrite_run(python("import xml.dom", after_recipe=_assert_single_j_import))


# noinspection PyUnresolvedReferences
def test_multiple():
    # language=python
    RecipeSpec().rewrite_run(python("import xml.dom ,  io "))


# noinspection PyUnresolvedReferences
def test_from():
    # language=python
    RecipeSpec().rewrite_run(python("from io import StringIO as sio"))


# noinspection PyUnresolvedReferences
def test_from_parenthesized():
    # language=python
    RecipeSpec().rewrite_run(python("from io import (StringIO as sio)"))


# noinspection PyUnresolvedReferences
def test_from_parenthesized_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("from io import (StringIO as sio , )"))


# noinspection PyUnresolvedReferences
def test_relative_import_0():
    # language=python
    RecipeSpec().rewrite_run(python("from . import bar"))


# noinspection PyUnresolvedReferences
def test_relative_import_1():
    # language=python
    RecipeSpec().rewrite_run(python("from .foo import bar"))


# noinspection PyUnresolvedReferences
def test_relative_import_2():
    # language=python
    RecipeSpec().rewrite_run(python("from ..foo import bar"))


# noinspection PyUnresolvedReferences
def test_crlf():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        import foo
        import bar
        """.replace('\n', '\r\n')
    ))
