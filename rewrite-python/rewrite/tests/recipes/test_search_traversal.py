# Copyright 2026 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Reachability tests for the search preconditions.

Every match sits below a decorated function, a nested class and a method, and
then below an f-string comprehension, a ``with`` or an ``except`` block. A
traversal that stops short of any of those answers "skip the file" for a file
the gated recipe has to change.
"""

import ast
import logging
import os
import shutil
import tempfile

import pytest

from rewrite import InMemoryExecutionContext, random_id
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.preconditions import uses_type
from rewrite.python.search import UsesImport, UsesMethod, UsesType

NESTED_SOURCE = '''\
import functools
from contextlib import suppress


@functools.cache
def outer(items):
    class Inner:
        @property
        def label(self) -> str:
            return f"{[v.strip() for v in items]!r}"

        def scan(self, values):
            with suppress(ValueError):
                try:
                    return values.pop()
                except KeyError:
                    from calendar import isleap

                    return isleap(2026)
            return None

    return Inner()
'''


def _parse_attributed(source: str):
    """``source`` parsed with type attribution, as UsesType needs types."""
    workspace = tempfile.mkdtemp()
    try:
        file_path = os.path.join(workspace, "nested.py")
        with open(file_path, "w") as f:
            f.write(source)
        from rewrite.python.ty_client import TyTypesClient

        client = TyTypesClient()
        if not client.initialize(workspace):
            pytest.skip("ty-types is unavailable, so there is no attribution to search")
        try:
            return ParserVisitor(source, file_path, client).visit(ast.parse(source))
        finally:
            client.shutdown()
    finally:
        shutil.rmtree(workspace, ignore_errors=True)


@pytest.fixture(scope="module")
def cu():
    return _parse_attributed(NESTED_SOURCE)


def _matches(visitor, cu) -> bool:
    # A precondition matches by returning a SearchResult-marked (new) tree,
    # which is what Preconditions.Check keys off of.
    return visitor.visit(cu, InMemoryExecutionContext()) is not cu


def test_uses_type_reaches_nested_type(cu):
    assert _matches(UsesType("contextlib.suppress"), cu)
    assert not _matches(UsesType("datetime.datetime"), cu)


def test_uses_method_reaches_call_in_fstring_comprehension(cu):
    assert _matches(UsesMethod("*..* strip(..)"), cu)
    assert not _matches(UsesMethod("*..* unused(..)"), cu)


def test_uses_method_can_widen_to_calls_that_have_no_declaring_type():
    src = "MyHelper.do_something(1)\n"
    unattributed = ParserVisitor(src, "unattributed.py", None).visit(ast.parse(src))
    pattern = "com.foo.MyHelper do_something(..)"
    assert not _matches(UsesMethod(pattern), unattributed)
    assert _matches(UsesMethod(pattern, match_unknown_types=True), unattributed)


def test_uses_import_reaches_import_in_except_block(cu):
    assert _matches(UsesImport("calendar"), cu)
    assert not _matches(UsesImport("datetime"), cu)


def test_tree_too_deep_to_visit_answers_no_match(caplog):
    # A parse balances concatenations, so the depth is built by hand.
    src = "x = 'a' 'b'\n"
    cu = ParserVisitor(src, "deep.py", None).visit(ast.parse(src))
    statement = cu.padding.statements[0]
    assignment = statement.element.padding.assignment
    concatenation = assignment.element
    for _ in range(3000):
        concatenation = concatenation.replace(id=random_id(), left=concatenation)
    deep = cu.padding.replace(_statements=[statement.replace(
        _element=statement.element.padding.replace(_assignment=assignment.replace(_element=concatenation)))])

    with caplog.at_level(logging.WARNING, logger="rewrite.python.search"):
        assert not _matches(UsesImport("os"), deep)

    assert "nests too deeply" in caplog.text


def test_uses_type_reads_attribution_off_expression_only_nodes(cu):
    # `list` is attributed to the comprehension itself, a `Py` node that is an
    # `Expression` without being a `TypedTree`.
    assert _matches(UsesType("list"), cu)


def test_uses_type_reaches_a_type_only_used_as_an_optional():
    optional_only = _parse_attributed(
        "import re\n\ndef first(s: str):\n    m = re.match('a', s)\n    return m\n")
    assert _matches(UsesType("re.Match"), optional_only)


def test_uses_type_matches_a_subtype_of_the_named_type():
    subclass_only = _parse_attributed("class MyError(ValueError):\n    pass\n")
    assert _matches(uses_type("Exception").local_visitor, subclass_only)

    assert _matches(uses_type("Exc*on").local_visitor, subclass_only)

    bound_only = _parse_attributed("def f[T: ValueError](x: T):\n    return x\n")
    assert _matches(uses_type("Exc*on").local_visitor, bound_only)


def test_uses_type_with_check_assignability_matches_types_of_called_methods():
    # `tzinfo` appears only as the type of `now`'s `tz` parameter.
    implicit_only = _parse_attributed("from datetime import datetime\nnow = datetime.now()\n")
    assert not _matches(uses_type("datetime.tzinfo").local_visitor, implicit_only)
    assert _matches(uses_type("datetime.tzinfo", check_assignability=True).local_visitor, implicit_only)


def test_uses_type_with_check_assignability_reads_a_call_whose_method_has_no_parameter_types():
    # The constructor call of a class with no `__init__` is attributed a method type with no parameter types.
    no_init = _parse_attributed("class Registry:\n    pass\n\nRegistry()\n")
    assert not _matches(uses_type("decimal.Decimal", check_assignability=True).local_visitor, no_init)


def test_uses_type_glob_that_covers_the_root_name_does_not_match_every_type():
    assert not _matches(uses_type("o*", check_assignability=True).local_visitor,
                        _parse_attributed("class MyError(ValueError):\n    pass\n"))
