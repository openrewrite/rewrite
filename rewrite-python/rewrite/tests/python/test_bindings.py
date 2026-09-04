# Copyright 2026 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Tests for the binding model: import_bindings, ImportBindings, is_reference."""

from typing import Any, List, Optional, Tuple

from rewrite.java.tree import Identifier, J, MethodDeclaration
from rewrite.python.binding_utils import ImportBindings, import_bindings
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

Reference = Tuple[str, str, Optional[str]]


class _Census(PythonVisitor[Any]):
    """Every identifier the file's module-scope imports resolve, as ``(spelling, module,
    member)``."""

    def __init__(self) -> None:
        super().__init__()
        self.found: List[Reference] = []

    def visit_identifier(self, ident: Identifier, p: Any) -> J:
        binding = import_bindings(self).reference(self.cursor, ident)
        if binding is not None:
            self.found.append((ident.simple_name, binding.module, binding.member))
        return ident


def _run(source: str, visitor: PythonVisitor[Any]) -> None:
    RecipeSpec(type_attribution=False).rewrite_run(
        python(source, after_recipe=lambda sf: visitor.visit(sf, None)))


def _references(source: str) -> List[Reference]:
    census = _Census()
    _run(source, census)
    return census.found


def _bindings(source: str) -> ImportBindings:
    captured: List[ImportBindings] = []

    class Capture(PythonVisitor[Any]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> J:
            captured.append(import_bindings(cu))
            return cu

    _run(source, Capture())
    return captured[0]


def test_a_name_in_member_position_is_not_a_reference():
    assert _references("""
        import json
        resp.json()
        post(url, json=body)
        item.payload.json
        json.dumps(x)
        """) == [('json', 'json', None)]


def test_a_local_binding_shadows_the_import_it_matches():
    assert _references("""
        import json
        def send(json):
            return json.dumps(x)
        """) == []


def test_a_receiverless_call_names_the_member_it_was_imported_as():
    assert _references("""
        from json import dumps as encode
        encode(x)
        resp.encode()
        """) == [('encode', 'json', 'dumps')]


def test_an_import_statement_binds_its_names_rather_than_reading_them():
    assert _references("""
        import os.path
        from os.path import join
        os.path.join(a, b)
        """) == [('os', 'os.path', None)]


def test_bindings_record_the_local_name_module_and_member():
    bindings = _bindings("""
        import json
        import os.path
        import numpy as np
        from datetime import datetime as dt
        """)
    assert [(b.name, b.module, b.member) for b in bindings] == [
        ('json', 'json', None),
        ('os', 'os.path', None),
        ('np', 'numpy', None),
        ('dt', 'datetime', 'datetime'),
    ]


def test_for_module_finds_every_name_bound_to_a_module():
    bindings = _bindings("""
        import json
        import json as j
        from json import dumps
        """)
    assert [b.name for b in bindings.for_module('json')] == ['json', 'j', 'dumps']
    assert [b.name for b in bindings.for_module('json', 'dumps')] == ['dumps']


def test_a_relative_import_is_not_the_module_of_the_same_name():
    bindings = _bindings("""
        from .locale import setlocale
        from . import locale
        """)
    assert bindings.for_module('locale') == ()
    assert [b.module for b in bindings] == ['.locale', '.']


def test_only_the_imports_of_the_module_scope_bind():
    bindings = _bindings("""
        from typing import TYPE_CHECKING
        try:
            import ujson as json
        except ImportError:
            import json
        if TYPE_CHECKING:
            from decimal import Decimal
        """)
    assert [(b.name, b.guarded) for b in bindings] == [('TYPE_CHECKING', False), ('Decimal', True)]


def test_a_statement_list_scans_the_imports_of_an_inner_block():
    captured: List[List[str]] = []

    class Capture(PythonVisitor[Any]):
        def visit_method_declaration(self, method: MethodDeclaration, p: Any) -> J:
            captured.append([b.name for b in import_bindings(method.body.statements)])
            return method

    _run("""
        def f():
            from json import dumps
            return dumps(x)
        """, Capture())
    assert captured == [['dumps']]
