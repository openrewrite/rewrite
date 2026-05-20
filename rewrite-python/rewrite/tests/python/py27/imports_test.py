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

"""Structural tests for Py2 ``import`` and ``from ... import`` statements."""

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor


def _stmt(source: str):
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()
    return cu.statements[0]


def _qualid_to_str(qualid) -> str:
    """Render a qualid (Identifier or FieldAccess chain) back to dotted form."""
    if isinstance(qualid, j.Identifier):
        return qualid.simple_name
    # j.FieldAccess: target is either Empty (single-name special form) or
    # a nested Identifier / FieldAccess.
    if isinstance(qualid.target, j.Empty):
        return qualid.name.simple_name
    return f"{_qualid_to_str(qualid.target)}.{qualid.name.simple_name}"


class TestImportName:
    def test_simple_import(self):
        stmt = _stmt("import os\n")
        assert isinstance(stmt, j.Import)
        assert _qualid_to_str(stmt.qualid) == "os"
        assert stmt.alias is None

    def test_dotted_import(self):
        stmt = _stmt("import os.path\n")
        assert isinstance(stmt, j.Import)
        assert _qualid_to_str(stmt.qualid) == "os.path"

    def test_deeply_dotted_import(self):
        stmt = _stmt("import a.b.c.d\n")
        assert isinstance(stmt, j.Import)
        assert _qualid_to_str(stmt.qualid) == "a.b.c.d"

    def test_import_with_alias(self):
        stmt = _stmt("import os as o\n")
        assert isinstance(stmt, j.Import)
        assert _qualid_to_str(stmt.qualid) == "os"
        assert isinstance(stmt.alias, j.Identifier)
        assert stmt.alias.simple_name == "o"

    def test_import_multiple_names_uses_multi_import(self):
        stmt = _stmt("import os, sys\n")
        assert isinstance(stmt, py.MultiImport)
        assert stmt.from_ is None
        assert len(stmt.names) == 2
        assert [_qualid_to_str(n.qualid) for n in stmt.names] == ["os", "sys"]

    def test_import_multiple_with_aliases(self):
        stmt = _stmt("import os as o, sys as s\n")
        assert isinstance(stmt, py.MultiImport)
        names = stmt.names
        assert [_qualid_to_str(n.qualid) for n in names] == ["os", "sys"]
        assert [n.alias.simple_name for n in names] == ["o", "s"]


class TestImportFrom:
    def test_from_single(self):
        stmt = _stmt("from os import path\n")
        assert isinstance(stmt, py.MultiImport)
        assert _qualid_to_str(stmt.from_) == "os"
        assert len(stmt.names) == 1
        assert _qualid_to_str(stmt.names[0].qualid) == "path"
        assert stmt.parenthesized is False

    def test_from_multiple(self):
        stmt = _stmt("from os import path, environ\n")
        assert isinstance(stmt, py.MultiImport)
        assert _qualid_to_str(stmt.from_) == "os"
        assert [_qualid_to_str(n.qualid) for n in stmt.names] == ["path", "environ"]

    def test_from_dotted_module(self):
        stmt = _stmt("from os.path import join\n")
        assert isinstance(stmt, py.MultiImport)
        assert _qualid_to_str(stmt.from_) == "os.path"
        assert _qualid_to_str(stmt.names[0].qualid) == "join"

    def test_from_with_alias(self):
        stmt = _stmt("from os import path as p, environ as e\n")
        assert isinstance(stmt, py.MultiImport)
        names = stmt.names
        assert [_qualid_to_str(n.qualid) for n in names] == ["path", "environ"]
        assert [n.alias.simple_name for n in names] == ["p", "e"]

    def test_from_parenthesized(self):
        stmt = _stmt("from os import (path, environ)\n")
        assert isinstance(stmt, py.MultiImport)
        assert stmt.parenthesized is True
        assert [_qualid_to_str(n.qualid) for n in stmt.names] == ["path", "environ"]

    def test_from_star(self):
        stmt = _stmt("from os import *\n")
        assert isinstance(stmt, py.MultiImport)
        assert _qualid_to_str(stmt.from_) == "os"
        assert len(stmt.names) == 1
        # The star is encoded as an Identifier with value '*' inside the qualid.
        assert stmt.names[0].qualid.name.simple_name == "*"


class TestRelativeImports:
    def test_from_dot_import(self):
        stmt = _stmt("from . import foo\n")
        assert isinstance(stmt, py.MultiImport)
        assert isinstance(stmt.from_, j.Identifier)
        assert stmt.from_.simple_name == "."
        assert _qualid_to_str(stmt.names[0].qualid) == "foo"

    def test_from_double_dot_import(self):
        stmt = _stmt("from .. import bar\n")
        assert isinstance(stmt, py.MultiImport)
        assert isinstance(stmt.from_, j.Identifier)
        assert stmt.from_.simple_name == ".."

    def test_from_dot_pkg_import(self):
        stmt = _stmt("from .pkg import x\n")
        assert isinstance(stmt, py.MultiImport)
        assert isinstance(stmt.from_, j.Identifier)
        assert stmt.from_.simple_name == ".pkg"


class TestImportRegressions:
    """No placeholder identifiers leak through these import shapes."""

    @pytest.mark.parametrize("src", [
        "import os\n",
        "import os.path\n",
        "import os as o\n",
        "import os, sys\n",
        "from os import path\n",
        "from os import path, environ\n",
        "from os import path as p\n",
        "from os.path import join\n",
        "from os import (path, environ)\n",
        "from os import *\n",
        "from . import foo\n",
        "from .. import bar\n",
    ])
    def test_no_placeholders(self, src):
        from tests.python.py27.expressions_test import _collect_placeholders
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        placeholders = list(_collect_placeholders(stmt))
        assert placeholders == [], f"unexpected placeholders in {src!r}: {placeholders}"
