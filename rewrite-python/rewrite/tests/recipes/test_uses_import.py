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

"""Tests for the in-process UsesImport search visitor and uses_import helper.

UsesImport gates on the as-written import path (not type attribution), so these
tests parse with no ty-types client -- the point of UsesImport is that it works
without type resolution.
"""

import ast

from rewrite import InMemoryExecutionContext
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.preconditions import uses_import
from rewrite.python.search import UsesImport, _import_path_matches


def _cu(src: str):
    return ParserVisitor(src, "test.py", None).visit(ast.parse(src))


def _matches(module: str, src: str) -> bool:
    cu = _cu(src)
    # visit() returns a SearchResult-marked (new) tree on match, the same tree
    # otherwise, which is exactly what Preconditions.Check keys off of.
    return UsesImport(module).visit(cu, InMemoryExecutionContext()) is not cu


class TestUsesImport:
    def test_from_import_matches_module(self):
        assert _matches("datetime", "from datetime import datetime\n")

    def test_plain_import_matches(self):
        assert _matches("datetime", "import datetime\n")

    def test_aliased_import_matches(self):
        assert _matches("datetime", "import datetime as dt\n")

    def test_multi_name_from_import(self):
        assert _matches("typing", "from typing import Dict, List, Optional\n")

    def test_comma_import_matches_each(self):
        assert _matches("calendar", "import os, calendar, sys\n")

    def test_submodule_matches_parent_query(self):
        assert _matches("os", "import os.path\n")

    def test_parent_import_matches_submodule_query(self):
        # `import os` makes `os.path` reachable, so a recipe targeting os.path
        # must still be gated in.
        assert _matches("os.path", "import os\n")

    def test_from_dotted_module(self):
        assert _matches("os.path", "from os.path import join\n")

    def test_canonicalization_safe_typing_list(self):
        # ty-types canonicalizes List -> list, erasing typing.List from TypesInUse;
        # uses_import reads syntax and matches regardless.
        assert _matches("typing", "from typing import List\nx: List = []\n")

    def test_removed_symbol_still_matches(self):
        # ty-types cannot resolve a removed symbol, so it produces no attribution;
        # uses_import still matches on the import statement.
        assert _matches("base64", "from base64 import encodestring\n")

    def test_no_match_when_not_imported(self):
        assert not _matches("datetime", "import os\nx = 1\n")

    def test_no_match_on_partial_module_name(self):
        # `os` must not match `ossaudiodev` (dotted-boundary aware).
        assert not _matches("os", "import ossaudiodev\n")

    def test_from_imported_name_is_not_treated_as_module(self):
        # `from os import path` imports the *name* path; querying module `path`
        # must not match (only the `from` module `os` is a module here).
        assert not _matches("path", "from os import path\n")

    def test_helper_ships_java_recipe_identity(self):
        ref = uses_import("datetime")
        assert ref.recipe_name == "org.openrewrite.python.search.UsesImport"
        assert ref.options == {"module": "datetime"}
        assert isinstance(ref.local_visitor, UsesImport)


def test_import_path_matches_unit():
    assert _import_path_matches("os", "os")
    assert _import_path_matches("os.path", "os")
    assert _import_path_matches("os", "os.path")
    assert not _import_path_matches("ossaudiodev", "os")
    assert not _import_path_matches("os", "")
    assert not _import_path_matches("", "os")
