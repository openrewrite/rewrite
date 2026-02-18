from rewrite.test import RecipeSpec, python


def test_for_tuple_unpacking_simple():
    # language=python
    RecipeSpec().rewrite_run(python(
        "for a, b in items:\n    pass\n"
    ))


def test_for_tuple_unpacking_with_parentheses():
    # language=python
    RecipeSpec().rewrite_run(python(
        "for (a, b) in items:\n    pass\n"
    ))


def test_for_tuple_unpacking_multiline():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
for (
    window,
    in_entry,
) in buf_out:
    pass
"""
    ))


def test_for_tuple_unpacking_multiline_no_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
for (
    window,
    in_entry
) in buf_out:
    pass
"""
    ))
