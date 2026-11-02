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

"""Every marker this package can put on a tree has to reach the host and come back.

A marker type without a codec used to be sent as nothing at all. The host stored null in
its place, and what the marker recorded, the Python 2 spelling of a ``raise`` for one,
was lost to every tree written to disk.
"""

import ast
import importlib
import inspect
import pkgutil
import sys
from dataclasses import fields, is_dataclass
from pathlib import Path

import pytest

import rewrite
from rewrite import random_id
from rewrite.java import Space
from rewrite.java.markers import OmitParentheses, Semicolon, TrailingComma
from rewrite.markers import (
    Marker, Markers, MarkupDebug, MarkupError, MarkupInfo, MarkupWarn, ParseExceptionResult,
    RecipesThatMadeChanges, RecipeThatMadeChanges, SearchResult, UnknownJavaMarker,
)
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.markers import (
    ExecSyntax, KeywordArguments, KeywordOnlyArguments, LegacyNotEqual, PrintSyntax,
    PythonResolutionResult, Quoted, RaiseTuple, SuppressNewline, TupleExceptClause,
)
from rewrite.python.style import IntelliJ, SpacesStyle
from rewrite.python.template.replacement import SubstitutedValue
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue
from rewrite.style import GeneralFormatStyle, NamedStyles

_CU_TYPE = 'org.openrewrite.python.tree.Py$CompilationUnit'

# One of every marker type that travels, with no field left at a value a lost field would also have.
_SAMPLES = [
    OmitParentheses(random_id()),
    Semicolon(random_id()),
    TrailingComma(random_id(), Space([], ' ')),
    MarkupDebug(random_id(), 'message', 'detail'),
    MarkupError(random_id(), 'message', 'detail'),
    MarkupInfo(random_id(), 'message', 'detail'),
    MarkupWarn(random_id(), 'message', 'detail'),
    ParseExceptionResult(random_id(), 'PythonParser', 'SyntaxError', 'message', 'Py.Pass'),
    RecipesThatMadeChanges(random_id(), [[RecipeThatMadeChanges('org.openrewrite.text.Find')]]),
    SearchResult(random_id(), 'found'),
    ExecSyntax(random_id()),
    KeywordArguments(random_id()),
    KeywordOnlyArguments(random_id()),
    LegacyNotEqual(random_id()),
    PrintSyntax(random_id(), True, True),
    PythonResolutionResult(
        random_id(), 'name', '1.0', 'description', 'MIT', 'pyproject.toml', '>=3.12', 'hatchling',
        [PythonResolutionResult.Dependency('build', '>=1', None, None, None)],
        [PythonResolutionResult.Dependency('requests', '>=2', ['socks'], 'python_version > "3"', None)],
        # the two maps cross as plain JSON, which this side holds as it arrived
        {'dev': [{'name': 'requests', 'versionConstraint': '>=2', 'extras': ['socks'],
                  'resolved': {'name': 'requests', 'version': '2.31.0', 'dependencies': []}}]},
        {'test': [{'name': 'pytest'}]},
        [], [],
        [PythonResolutionResult.ResolvedDependency('requests', '2.31.0', None, [])],
        PythonResolutionResult.PackageManager.Uv,
        [PythonResolutionResult.SourceIndex('pypi', 'https://pypi.org/simple', True)],
    ),
    Quoted(random_id(), Quoted.Style.BACKTICK),
    RaiseTuple(random_id()),
    SuppressNewline(random_id()),
    TupleExceptClause(random_id()),
    NamedStyles.build(
        IntelliJ.spaces().with_other(IntelliJ.spaces().other.with_before_comma(True)),
        IntelliJ.tabs_and_indents(), IntelliJ.blank_lines(), IntelliJ.wrapping_and_braces(), IntelliJ.other(),
        GeneralFormatStyle(True),
        name='name', display_name='display name', description='description'),
    IntelliJ(),
]

# Nothing in this package attaches an UnknownJavaMarker, and a SubstitutedValue lives only within
# Template.apply. Neither names a host type to be sent as, so the send queue refuses them rather than lose them.
_CANNOT_TRAVEL = {SubstitutedValue, UnknownJavaMarker}


def _marker_types():
    """Every concrete marker class the package defines."""
    for module in pkgutil.walk_packages(rewrite.__path__, 'rewrite.'):
        importlib.import_module(module.name)

    found, pending = set(), [Marker]
    while pending:
        for marker_type in pending.pop().__subclasses__():
            pending.append(marker_type)
            owner = sys.modules[marker_type.__module__]
            for name in marker_type.__qualname__.split('.'):
                owner = getattr(owner, name, None)
            # a dataclass given slots is a new class, and the one it replaced lingers as a subclass
            if owner is marker_type and not inspect.isabstract(marker_type):
                found.add(marker_type)
    return found


def _round_trip(marker):
    source = "x = 1\n"
    cu = ParserVisitor(source, None, None).visit_Module(ast.parse(source)).replace(
        source_path=Path("test.py"), markers=Markers(random_id(), [marker]))
    batch = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = batch[:]
        batch.clear()
        return out

    return PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull)).markers.markers[0]


def _content(value):
    """What a marker holds, laid out so that nested values compare by their fields."""
    if is_dataclass(value):
        return type(value), {field.name: _content(getattr(value, field.name)) for field in fields(value)}
    if isinstance(value, (list, tuple)):
        return [_content(item) for item in value]
    if isinstance(value, dict):
        # in a plain value the host writes object ids of its own, and leaves out what is null
        return {key: _content(item) for key, item in value.items() if item is not None and not key.startswith('@')}
    return value


def test_every_marker_type_is_accounted_for():
    assert {type(sample) for sample in _SAMPLES} | _CANNOT_TRAVEL == _marker_types()


@pytest.mark.parametrize("sample", _SAMPLES, ids=lambda sample: type(sample).__qualname__)
def test_marker_survives_the_wire(sample):
    assert _content(_round_trip(sample)) == _content(sample)


def test_marker_without_a_wire_form_is_refused():
    with pytest.raises(TypeError, match="No RPC codec"):
        _round_trip(UnknownJavaMarker(random_id(), {}))


def test_styles_arrive_as_the_host_serializes_them():
    host_only = {'@c': 'org.openrewrite.java.style.EmptyForIteratorPadStyle', '@ref': 3, 'space': True}
    received = RpcReceiveQueue({}, _CU_TYPE, lambda: [
        {'state': 'ADD', 'valueType': 'org.openrewrite.style.NamedStyles', 'value': {
            '@c': 'org.openrewrite.style.NamedStyles', '@ref': 1,
            'id': '5ed0b6a1-6f0c-4a5e-9d55-0a6f1e7c2b11', 'name': 'name', 'displayName': 'display name', 'tags': ['a'],
            'styles': [
                {'@c': 'org.openrewrite.python.style.OtherStyle', '@ref': 2, 'useContinuationIndent': {
                    'methodCallArguments': True, 'methodDeclarationParameters': False,
                    'collectionsAndComprehensions': True}},
                host_only,
                {'@c': 'org.openrewrite.style.GeneralFormatStyle', '@ref': 4, 'useCRLFNewLines': True},
            ]}},
    ]).receive(None)

    assert type(received) is NamedStyles
    assert (received._name, received._display_name, received._description, received._tags) == \
           ('name', 'display name', None, {'a'})
    other, kept, general = received._styles
    assert other.use_continuation_indent.method_call_arguments
    assert not other.use_continuation_indent.method_declaration_parameters
    assert general.use_crlf_new_lines
    # a style only the host has a class for goes back to it as it came
    assert kept is host_only
    assert _round_trip(received)._styles[1] == host_only


@pytest.mark.requires_java_rpc
def test_markers_reach_the_host_and_come_back(java_rpc):
    """The host has to build each marker, which it cannot from a type it has no class or codec for."""
    # with a search result already there, the host's recipe would have nothing to add
    attached = [sample for sample in _SAMPLES if not isinstance(sample, SearchResult)]

    def returned(cu):
        # having added its search result, the host sends the whole list back as it holds it
        back = cu.markers.markers[:len(attached)]
        assert all(theirs is not ours for theirs, ours in zip(back, attached))
        assert [_content(marker) for marker in back] == [_content(marker) for marker in attached]
        assert cu.markers.find_first(SearchResult) is not None

    _find_source_files_on_the_host(attached, returned)


@pytest.mark.requires_java_rpc
def test_generic_marker_comes_back_as_it_was_sent(java_rpc):
    """A marker neither side has a type for travels as a bare map, which a TypeScript peer builds freely."""
    generic = {'kind': 'org.openrewrite.rpc.RpcMarker', 'id': '5ed0b6a1-6f0c-4a5e-9d55-0a6f1e7c2b11', 'note': 'kept'}

    def returned(cu):
        back = cu.markers.markers[0]
        assert back is not generic
        assert {key: value for key, value in back.items() if not key.startswith('@')} == generic

    _find_source_files_on_the_host([generic], returned)


def _find_source_files_on_the_host(markers, returned):
    from rewrite.rpc.rpc_recipe import RpcRecipe
    from rewrite.test import RecipeSpec, python

    RecipeSpec(recipe=RpcRecipe('org.openrewrite.FindSourceFiles', filePattern='**/*.py')).rewrite_run(python(
        "x = 1\n", lambda actual: actual,
        before_recipe=lambda cu: cu.replace(markers=Markers(random_id(), markers)), after_recipe=returned))
