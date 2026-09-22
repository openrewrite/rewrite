from rewrite.python import CompilationUnit, Shebang
from rewrite.test import RecipeSpec, python


def _first_is_shebang(cu):
    assert isinstance(cu, CompilationUnit)
    first = cu.statements[0]
    assert isinstance(first, Shebang)
    assert first.text == "#!/usr/bin/env python3"


def test_shebang_at_start_of_file():
    # language=python
    RecipeSpec().rewrite_run(python(
        "#!/usr/bin/env python3\nprint(\"Hello, world!\")\n",
        after_recipe=_first_is_shebang,
    ))


def test_shebang_with_blank_line_after():
    # language=python
    RecipeSpec().rewrite_run(python(
        "#!/usr/bin/env python3\n\nx = 1\n",
        after_recipe=_first_is_shebang,
    ))


def test_shebang_followed_by_comment():
    # language=python
    RecipeSpec().rewrite_run(python(
        "#!/usr/bin/env python3\n# a comment\nx = 1\n",
        after_recipe=_first_is_shebang,
    ))


def test_shebang_only():
    # language=python
    RecipeSpec().rewrite_run(python(
        "#!/usr/bin/env python3\n",
        after_recipe=_first_is_shebang,
    ))


def test_shebang_windows_line_endings():
    # language=python
    RecipeSpec().rewrite_run(python("#!/usr/bin/env python3\r\nx = 1\r\n"))


def test_leading_comment_is_not_a_shebang():
    def check(cu):
        assert isinstance(cu, CompilationUnit)
        assert not isinstance(cu.statements[0], Shebang)

    # language=python
    RecipeSpec().rewrite_run(python(
        "# not a shebang\nx = 1\n",
        after_recipe=check,
    ))
