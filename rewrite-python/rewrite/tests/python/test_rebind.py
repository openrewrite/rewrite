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

"""A rebind's attribution, compared with a fresh parse of the code it produces."""

import ast
import dataclasses
import shutil
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

import pytest

from rewrite import InMemoryExecutionContext, InMemoryLargeSourceSet
from rewrite.java.support_types import J, JavaType
from rewrite.java.tree import Import
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.binding import maybe_bind, maybe_rebind
from rewrite.python.recipes.change_import import ChangeImport
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.ty_client import TyTypesClient
from rewrite.python.visitor import PythonVisitor
from rewrite.recipe import Recipe
from rewrite.test import RecipeSpec, dedent, from_visitor, python

requires_ty = pytest.mark.skipif(shutil.which('ty-types') is None,
                                 reason="ty-types CLI is not installed")

# The target re-exports its classes from a private submodule, as most packages do.
_PACKAGES = {
    'legacy_http/__init__.py': '''
        class Headers:
            def append(self, name: str, value: str) -> None: ...
        class Http:
            def post(self, url: str, headers: Headers) -> Headers: ...
        def make_headers() -> Headers: ...
        NAMES = ["a"]
        ''',
    'acme_common/__init__.py': '',
    'acme_common/http/__init__.py': '''
        from acme_common.http._client import HttpClient, HttpHeaders, make_http_headers, NAMES
        ''',
    'acme_common/http/_client.py': '''
        class HttpHeaders:
            def append(self, name: str, value: str) -> None: ...
        class HttpClient:
            def post(self, url: str, headers: HttpHeaders) -> HttpHeaders: ...
        def make_http_headers() -> HttpHeaders: ...
        NAMES = ["a"]
        ''',
}


def _signature(type_: Optional[JavaType], seen: frozenset = frozenset()) -> str:
    """``type_`` with every name its signature reaches, as Java's ``ChangeType`` walks it."""
    if type_ is None:
        return 'None'
    if isinstance(type_, JavaType.Unknown):
        return 'Unknown'
    if id(type_) in seen:
        return '...'
    seen = seen | {id(type_)}

    def nested(inner: Optional[JavaType]) -> str:
        return _signature(inner, seen)

    if isinstance(type_, JavaType.Method):
        params = ', '.join(nested(p) for p in type_.parameter_types or [])
        return f'{nested(type_.declaring_type)}#{type_.name}({params}) -> {nested(type_.return_type)}'
    if isinstance(type_, JavaType.Parameterized):
        return f"{nested(type_.type)}[{', '.join(nested(p) for p in type_.type_parameters or [])}]"
    if isinstance(type_, JavaType.Variable):
        return f'{nested(type_.owner)}.{type_.name}: {nested(type_.type)}'
    if isinstance(type_, JavaType.Union):
        return ' | '.join(nested(b) for b in type_.bounds)
    if isinstance(type_, JavaType.FullyQualified):
        return type_.fully_qualified_name
    if isinstance(type_, JavaType.Primitive):
        return type_.name
    return type(type_).__name__


def _attribution(cu: CompilationUnit) -> List[str]:
    """Every type slot of every tree outside the imports, in visiting order."""
    rows: List[str] = []

    class Collect(PythonVisitor[None]):
        def visit(self, tree: Any, p: None, parent: Any = None) -> Any:
            if isinstance(tree, (Import, MultiImport)):
                return tree
            if isinstance(tree, J):
                rows.extend(f'{type(tree).__name__}.{f.name}: {_signature(getattr(tree, f.name))}'
                            for f in dataclasses.fields(tree)
                            if isinstance(getattr(tree, f.name, None), JavaType))
            return super().visit(tree, p) if parent is None else super().visit(tree, p, parent)

    Collect().visit(cu, None)
    return rows


def _parse(source: str, packages: Dict[str, str]) -> CompilationUnit:
    """``source`` parsed as ``client.py`` beside ``packages``, so every parse names the file's own
    module alike."""
    root = Path(tempfile.mkdtemp())
    try:
        for path, text in {**packages, 'client.py': source}.items():
            (root / path).parent.mkdir(parents=True, exist_ok=True)
            (root / path).write_text(dedent(text))
        client = TyTypesClient()
        client.initialize(str(root))
        try:
            return ParserVisitor(dedent(source), str(root / 'client.py'), client).visit_Module(
                ast.parse(dedent(source)))
        finally:
            client.shutdown()
    finally:
        shutil.rmtree(root, ignore_errors=True)


def _run(recipes: Sequence[Recipe], before: str, packages: Dict[str, str] = _PACKAGES) -> CompilationUnit:
    """``recipes`` run one after another over ``before`` parsed beside ``packages``."""
    cu = _parse(before, packages)
    for recipe in recipes:
        for result in recipe.run(InMemoryLargeSourceSet([cu]), InMemoryExecutionContext()):
            if result._after is not None:
                cu = result._after
    return cu


def _assert_attributed_as_parsed(recipes: Sequence[Recipe], before: str, after: str,
                                 packages: Dict[str, str] = _PACKAGES) -> None:
    """Expects ``recipes`` to turn ``before`` into ``after``, with every type in the result read
    as a parse of ``after`` attributes it."""
    cu = _run(recipes, before, packages)
    assert cu.print_all() == dedent(after)
    assert _attribution(cu) == _attribution(_parse(after, packages))


@requires_ty
def test_moved_members_are_attributed_as_a_parse_of_the_result():
    _assert_attributed_as_parsed(
        [ChangeImport(old_module='legacy_http', old_name='Http',
                      new_module='acme_common.http', new_name='HttpClient',
                      new_declaring_module='acme_common.http._client'),
         ChangeImport(old_module='legacy_http', old_name='Headers',
                      new_module='acme_common.http', new_name='HttpHeaders',
                      new_declaring_module='acme_common.http._client'),
         ChangeImport(old_module='legacy_http', old_name='make_headers',
                      new_module='acme_common.http', new_name='make_http_headers',
                      new_declaring_module='acme_common.http._client')],
        '''
        from legacy_http import Http, Headers, make_headers

        def send(http: Http, headers: list[Headers]) -> None:
            r = http.post("u", headers[0])
            h = make_headers()
            h.append("a", "b")
            client = Http()
        ''',
        '''
        from acme_common.http import HttpClient, HttpHeaders, make_http_headers

        def send(http: HttpClient, headers: list[HttpHeaders]) -> None:
            r = http.post("u", headers[0])
            h = make_http_headers()
            h.append("a", "b")
            client = HttpClient()
        ''',
    )


@requires_ty
def test_a_member_read_through_its_module_is_attributed_as_a_parse_of_the_result():
    _assert_attributed_as_parsed(
        [ChangeImport(old_module='legacy_http', old_name='Http',
                      new_module='acme_common.http', new_name='HttpClient',
                      new_declaring_module='acme_common.http._client')],
        '''
        import legacy_http
        client = legacy_http.Http()
        cls = legacy_http.Http
        ''',
        '''
        import acme_common.http
        client = acme_common.http.HttpClient()
        cls = acme_common.http.HttpClient
        ''',
    )

    # The file's own name for the new module spells the reference, and its type names the module.
    _assert_attributed_as_parsed(
        [ChangeImport(old_module='legacy_http', old_name='Http',
                      new_module='acme_common.http', new_name='HttpClient',
                      new_declaring_module='acme_common.http._client')],
        '''
        import legacy_http
        import acme_common.http as h
        client = legacy_http.Http()
        ''',
        '''
        import acme_common.http as h
        client = h.HttpClient()
        ''',
    )


@requires_ty
def test_a_moved_constant_leaves_the_type_of_its_value_alone():
    _assert_attributed_as_parsed(
        [ChangeImport(old_module='legacy_http', old_name='NAMES', new_module='acme_common.http',
                      new_declaring_module='acme_common.http._client')],
        '''
        from legacy_http import NAMES
        s = ["x"]
        n = NAMES
        ''',
        '''
        from acme_common.http import NAMES
        s = ["x"]
        n = NAMES
        ''',
    )


@requires_ty
def test_a_whole_module_move_carries_its_members_types():
    packages = {**_PACKAGES, 'acme_http/__init__.py': _PACKAGES['acme_common/http/_client.py']
                .replace('HttpClient', 'Http').replace('HttpHeaders', 'Headers')
                .replace('make_http_headers', 'make_headers')}
    _assert_attributed_as_parsed(
        [ChangeImport(old_module='legacy_http', new_module='acme_http')],
        '''
        import legacy_http
        client = legacy_http.Http()
        h = legacy_http.make_headers()
        ''',
        '''
        import acme_http
        client = acme_http.Http()
        h = acme_http.make_headers()
        ''',
        packages,
    )


@requires_ty
def test_a_second_binding_of_the_member_stays_and_keeps_its_types():
    cu = _run([ChangeImport(old_module='legacy_http', old_name='Http', new_module='acme_common.http',
                            new_name='HttpClient')],
              '''
              from legacy_http import Http
              from legacy_http import Http as H
              h = H()
              ''')
    assert cu.print_all() == dedent('''
        from legacy_http import Http as H
        from acme_common.http import HttpClient
        h = H()
        ''')
    # `H` goes on reading the old class, so renaming the type would re-attribute it too.
    assert 'Assignment._type: legacy_http.Http' in _attribution(cu)


def _visiting(record: List[Optional[str]], *calls: Any) -> RecipeSpec:
    """A spec whose recipe makes ``calls`` on visiting a file, recording what each answers."""

    class Calls(PythonVisitor[Any]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> Any:
            record.extend(call(self) for call in calls)
            return cu

    return RecipeSpec(recipe=from_visitor(Calls()), type_attribution=False)


def test_the_moved_binding_keeps_its_name_where_the_new_one_is_spelled():
    answers: List[Optional[str]] = []
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                              to_member='HttpClient')).rewrite_run(
        python(
            '''
            from legacy import Http
            class HttpClient: ...
            h = Http()
            ''',
            '''
            from acme import HttpClient as Http
            class HttpClient: ...
            h = Http()
            '''))
    # A name already binding the very member the binding moves to is no collision.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                              to_member='HttpClient')).rewrite_run(
        python(
            '''
            from acme import HttpClient
            from legacy import Http
            h = Http(HttpClient())
            ''',
            '''
            from acme import HttpClient
            h = HttpClient(HttpClient())
            '''))
    # A parameter of that name would capture the renamed reference inside its function.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                              to_member='HttpClient')).rewrite_run(
        python(
            '''
            from acme import HttpClient
            from legacy import Http
            def f(HttpClient):
                return Http(HttpClient)
            ''',
            '''
            from acme import HttpClient as Http, HttpClient
            def f(HttpClient):
                return Http(HttpClient)
            '''))
    # Nor is it one where a rebind queued earlier in the visit moves that import away.
    _visiting(answers,
              lambda v: maybe_rebind(v, 'acme', 'other', from_member='HttpClient'),
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                     to_member='HttpClient')).rewrite_run(
        python(
            '''
            from acme import HttpClient
            from legacy import Http
            h = Http(HttpClient())
            ''',
            '''
            from other import HttpClient
            from acme import HttpClient as Http
            h = Http(HttpClient())
            '''))
    # A pinned alias the file spells elsewhere would capture its references, so nothing moves.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                              alias='HttpClient')).rewrite_run(
        python('''
            from legacy import Http
            class HttpClient: ...
            '''))
    assert answers == ['Http', 'HttpClient', 'Http', 'HttpClient', 'Http', None]


def test_rebinds_in_one_visit_end_in_one_import():
    answers: List[Optional[str]] = []
    _visiting(answers,
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http', to_member='HttpClient'),
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Headers',
                                     to_member='HttpHeaders')).rewrite_run(
        python(
            '''
            from legacy import Http, Headers
            h = Http(Headers())
            ''',
            '''
            from acme import HttpClient, HttpHeaders
            h = HttpClient(HttpHeaders())
            '''))
    assert answers == ['HttpClient', 'HttpHeaders']

    # A repeated call answers as the first did, which is the name the file ends up binding.
    answers.clear()
    _visiting(answers,
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http', to_member='HttpClient'),
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http', to_member='HttpClient')
              ).rewrite_run(
        python(
            '''
            from legacy import Http
            h = Http()
            ''',
            '''
            from acme import HttpClient
            h = HttpClient()
            '''))
    assert answers == ['HttpClient', 'HttpClient']

    # The new module's import follows the rebind writing its references, even where an
    # earlier `maybe_bind` queued that same import before anything read it.
    answers.clear()
    _visiting(answers,
              lambda v: maybe_bind(v, 'acme'),
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http')).rewrite_run(
        python(
            '''
            import legacy
            h = legacy.Http()
            ''',
            '''
            import acme
            h = acme.Http()
            '''))
    assert answers == ['acme', 'acme.Http']


def test_a_queued_module_import_claims_its_name():
    answers: List[Optional[str]] = []
    _visiting(answers,
              lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http'),
              lambda v: maybe_bind(v, 'other', alias='acme')).rewrite_run(
        python(
            '''
            import legacy
            h = legacy.Http()
            ''',
            '''
            import acme
            h = acme.Http()
            '''))
    assert answers == ['acme.Http', None]


def test_the_answered_module_is_imported_where_the_caller_writes_the_reference():
    class WritesTheAnswer(PythonVisitor[Any]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> Any:
            assert maybe_rebind(self, 'legacy', 'acme', from_member='Http') == 'acme.Http'
            return super().visit_compilation_unit(cu, p)

        def visit_method_invocation(self, method: Any, p: Any) -> Any:
            select = method.padding.select
            return method.padding.replace(_select=select.replace(
                _element=select.element.replace(_simple_name='acme')))

    RecipeSpec(recipe=from_visitor(WritesTheAnswer()), type_attribution=False).rewrite_run(
        python(
            '''
            import legacy
            h = legacy.Http()
            ''',
            '''
            import legacy
            import acme
            h = acme.Http()
            '''))


def test_the_import_a_move_rewrites_is_the_one_binding_it():
    answers: List[Optional[str]] = []
    # A relative import names its module with its leading dots.
    _visiting(answers, lambda v: maybe_rebind(v, '.legacy', 'acme', from_member='Http',
                                              to_member='HttpClient')).rewrite_run(
        python(
            '''
            from .legacy import Http
            h = Http()
            ''',
            '''
            from acme import HttpClient
            h = HttpClient()
            '''))

    # An alias spelling the new module's name still binds the member.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http',
                                              to_member='HttpClient')).rewrite_run(
        python(
            '''
            from legacy import Http as acme
            h = acme()
            ''',
            '''
            from acme import HttpClient as acme
            h = acme()
            '''))
    assert answers == ['HttpClient', 'acme']


def test_a_member_moves_only_where_it_is_read_through_its_module():
    answers: List[Optional[str]] = []
    # `os.sep` reads the `os` package, not the `os.path` module, which binds no name of its own.
    _visiting(answers, lambda v: maybe_rebind(v, 'os.path', 'posixpath', from_member='sep')).rewrite_run(
        python('''
            import os.path
            s = os.sep
            '''))

    # A parameter shadowing the module's name reads something else.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http')).rewrite_run(
        python('''
            import legacy
            def f(legacy):
                return legacy.Http
            '''))

    # Nothing reads the member, so there is no name to answer with.
    _visiting(answers, lambda v: maybe_rebind(v, 'legacy', 'acme', from_member='Http')).rewrite_run(
        python('''
            import legacy
            x = legacy.other()
            '''))
    assert answers == [None, None, None]


def test_maybe_bind_answers_with_the_files_name_for_a_module():
    answers: List[Optional[str]] = []
    _visiting(answers,
              lambda v: maybe_bind(v, 'acme.http'),
              lambda v: maybe_bind(v, 'acme.http', alias='h'),
              lambda v: maybe_bind(v, 'json'),
              lambda v: maybe_bind(v, 'os'),
              lambda v: maybe_bind(v, 'acme', 'Client'),
              lambda v: maybe_bind(v, 'a', 'X'),
              lambda v: maybe_bind(v, 'b', 'X')).rewrite_run(
        python('''
            import acme.http as h
            from typing import TYPE_CHECKING
            if TYPE_CHECKING:
                from acme import Client
            os = 1
            '''))
    # Nothing reads `json`, so its import waits for a reference the caller never wrote. A
    # type-checking import of `Client` binds the same member, so a runtime import may share its
    # name. `X` is claimed by the import queued for `a`.
    assert answers == ['h', 'h', 'json', None, 'Client', 'X', None]
