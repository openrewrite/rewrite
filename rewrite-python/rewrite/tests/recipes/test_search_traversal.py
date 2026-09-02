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

from rewrite import InMemoryExecutionContext
from rewrite.python._parser_visitor import ParserVisitor
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


@pytest.fixture(scope="module")
def cu():
    """The fixture parsed with type attribution, as UsesType needs types."""
    workspace = tempfile.mkdtemp()
    try:
        file_path = os.path.join(workspace, "nested.py")
        with open(file_path, "w") as f:
            f.write(NESTED_SOURCE)
        from rewrite.python.ty_client import TyTypesClient

        client = TyTypesClient()
        if not client.initialize(workspace):
            pytest.skip("ty-types is unavailable, so there is no attribution to search")
        try:
            return ParserVisitor(NESTED_SOURCE, file_path, client).visit(
                ast.parse(NESTED_SOURCE)
            )
        finally:
            client.shutdown()
    finally:
        shutil.rmtree(workspace, ignore_errors=True)


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


def test_uses_import_reaches_import_in_except_block(cu):
    assert _matches(UsesImport("calendar"), cu)
    assert not _matches(UsesImport("datetime"), cu)


def test_tree_too_deep_to_visit_answers_no_match(caplog):
    # One Py.Binary per concatenated fragment, so this nests deeper than the
    # interpreter stack.
    src = "x = " + " ".join(f'"s{i}"' for i in range(3000)) + "\n"
    deep = ParserVisitor(src, "deep.py", None).visit(ast.parse(src))

    with caplog.at_level(logging.WARNING, logger="rewrite.python.search"):
        assert not _matches(UsesImport("os"), deep)

    assert "nests too deeply" in caplog.text


def test_uses_type_reads_attribution_off_expression_only_nodes(cu):
    # `list` is attributed to the comprehension itself, a `Py` node that is an
    # `Expression` without being a `TypedTree`.
    assert _matches(UsesType("list"), cu)
