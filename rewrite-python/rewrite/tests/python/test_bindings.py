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
from rewrite.python.binding_utils import (ImportBindings, import_bindings, is_reference,
                                          resolves_in_scope)
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
        self.bindings: List[Tuple[str, str, Optional[str], bool]] = []

    def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> J:
        self.bindings = [(b.name, b.module, b.member, b.guarded) for b in import_bindings(cu)]
        return super().visit_compilation_unit(cu, p)

    def visit_identifier(self, ident: Identifier, p: Any) -> J:
        binding = import_bindings(self).reference(self.cursor, ident)
        if binding is not None:
            self.found.append((ident.simple_name, binding.module, binding.member))
        return ident


def _run(source: str, visitor: PythonVisitor[Any], attributed: bool = False) -> None:
    RecipeSpec(type_attribution=attributed).rewrite_run(
        python(source, after_recipe=lambda sf: visitor.visit(sf, None)))


def _references(source: str) -> List[Reference]:
    census = _Census()
    _run(source, census)
    return census.found


def _positions(source: str, predicate: Any = is_reference) -> List[bool]:
    """What ``predicate`` answers for each ``json`` identifier, in source order."""
    answers: List[bool] = []

    class Positions(PythonVisitor[Any]):
        def visit_identifier(self, ident: Identifier, p: Any) -> J:
            if ident.simple_name == 'json':
                answers.append(predicate(self.cursor, ident))
            return ident

    _run(source, Positions())
    return answers


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


def test_a_global_statement_declares_a_binding_rather_than_reading_it():
    assert _references("""
        import json
        def f():
            global json
            json = None
        """) == []


def test_attribution_changes_nothing_the_model_reports():
    source = """
        import json
        from os.path import join as j
        from . import sibling
        resp.json()
        json.dumps(j)
        x: 'json.Foo' = None
        """

    def arm(attributed: bool):
        census = _Census()
        _run(source, census, attributed)
        return census.found, census.bindings

    assert arm(False) == arm(True)


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


def test_a_target_the_statement_binds_is_not_a_reference():
    assert _references("""
        import json
        json = None
        (a, json) = f()
        for json in xs:
            pass
        """) == []


def test_del_reads_the_name_it_unbinds():
    # `del json` needs the name bound, so removing the import it names breaks the file.
    assert _references("import json\ndel json\n") == [('json', 'json', None)]


def test_an_undotted_case_label_captures_rather_than_matches():
    assert _positions("""
        match x:
            case json:
                pass
        """) == [False]
    assert _positions("""
        match x:
            case json.Loads():
                pass
        """) == [True]


def test_a_rename_follows_more_names_than_a_read_census():
    # A write and a `global` declaration name the binding without reading it; member and
    # keyword position name something else, which both predicates answer alike.
    source = """
        json = 1
        def f(resp, url):
            global json
            resp.json()
            post(url, json=1)
            return json
        """
    assert _positions(source) == [False, False, False, False, True]
    assert _positions(source, resolves_in_scope) == [True, True, False, False, True]


def test_a_comprehension_target_is_not_a_reference():
    # `reference()` filters this by scope, so `is_reference` is asked on its own.
    assert _positions("y = [json for json in xs]\n") == [True, False]


def test_a_dotted_target_reads_the_name_it_assigns_through():
    assert _references("""
        import json
        json.cache = {}
        json += 1
        """) == [('json', 'json', None), ('json', 'json', None)]


def test_the_last_import_of_a_name_in_source_order_is_the_one_in_force():
    assert _bindings("""
        from typing import TYPE_CHECKING
        if TYPE_CHECKING:
            from a import Bar
        from b import Bar
        """).for_name('Bar').module == 'b'

    # A guarded import binds no runtime name, so it does not displace one that does.
    assert _bindings("""
        from typing import TYPE_CHECKING
        from a import Bar
        if TYPE_CHECKING:
            from b import Bar
        """).for_name('Bar').module == 'a'


def test_a_wildcard_import_binds_no_name_of_its_own():
    bindings = _bindings("from json import *\n")
    assert [(b.name, b.member) for b in bindings] == [('*', '*')]
    assert bindings.for_name('*') is None


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
