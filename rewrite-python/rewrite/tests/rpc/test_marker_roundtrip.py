# Copyright 2025 the original author or authors.
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

"""Regression test for the RPC diff round-trip of a *modified* Python LST.

A Java recipe that edits a Python LST (e.g. adds a ``SearchResult`` marker)
sends the edited tree back to Python as a delta against the original. Python's
receiver then applies that delta and prints the result. Issue #8093: after the
LST memory-footprint rework (#7958) ids became a lazily-reconstructed UUID, so
``marker.id`` raised ``TypeError`` for a factory-created marker (``_id is None``)
on the ADD path. That broke RPC stream alignment and printed an empty file.

These tests exercise the same ``RpcSendQueue`` -> ``RpcReceiveQueue`` path
directly (no Java needed) so they run fast and never hang.
"""
import ast
from dataclasses import fields, is_dataclass

from rewrite import random_id
from rewrite.java.tree import MethodInvocation
from rewrite.markers import Markers, ParseExceptionResult, SearchResult
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue
from rewrite.utils import replace_if_changed

_CU_TYPE = 'org.openrewrite.python.tree.Py$CompilationUnit'


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source))


def _find_first(root, cls):
    seen = set()
    stack = [root]
    while stack:
        o = stack.pop()
        if id(o) in seen:
            continue
        seen.add(id(o))
        if isinstance(o, cls):
            return o
        if is_dataclass(o):
            stack.extend(getattr(o, f.name, None) for f in fields(o))
        elif isinstance(o, (list, tuple)):
            stack.extend(o)
    return None


def _replace_node(o, old, new):
    if o is old:
        return new
    if is_dataclass(o):
        changes = {}
        for f in fields(o):
            v = getattr(o, f.name, None)
            nv = _replace_node(v, old, new)
            if nv is not v:
                changes[f.name] = nv
        return replace_if_changed(o, **changes) if changes else o
    if isinstance(o, list):
        nl = [_replace_node(x, old, new) for x in o]
        return nl if any(a is not b for a, b in zip(nl, o)) else o
    return o


def _rpc_round_trip(before, after):
    """Serialise ``after`` as a delta against ``before`` and rebuild it on the receiver."""
    data = RpcSendQueue(_CU_TYPE).generate(after, before)
    batch = list(data)

    def pull():
        out = batch[:]
        batch.clear()
        return out

    queue = RpcReceiveQueue({}, _CU_TYPE, pull)
    return PythonRpcReceiver().receive(before, queue)


def _add_marker_to_first_call(cu, marker):
    call = _find_first(cu, MethodInvocation)
    assert call is not None, "expected a method invocation in the source"
    edited = replace_if_changed(call, _markers=Markers(random_id(), [marker]))
    return _replace_node(cu, call, edited)


def test_search_result_marker_round_trip_preserves_content():
    # Issue #8093: adding a SearchResult marker via RPC printed an empty file.
    cu = _parse('def f():\n    print("hello")\n')
    edited = _add_marker_to_first_call(cu, SearchResult(random_id(), "found"))

    expected = 'def f():\n    /*~~(found)~~>*/print("hello")\n'
    assert PythonPrinter().print(edited) == expected

    rebuilt = _rpc_round_trip(cu, edited)
    assert PythonPrinter().print(rebuilt) == expected


def test_parse_exception_result_marker_round_trip_preserves_content():
    # ParseExceptionResult uses the same factory (``_id is None`` on ADD).
    cu = _parse('x = obj.foo()\n')
    marker = ParseExceptionResult(
        _id=random_id(),
        _parser_type="PythonParser",
        _exception_type="RuntimeException",
        _message="boom",
        _tree_type=None,
    )
    edited = _add_marker_to_first_call(cu, marker)

    rebuilt = _rpc_round_trip(cu, edited)
    assert PythonPrinter().print(rebuilt) == PythonPrinter().print(edited)

    rebuilt_marker = _find_first(rebuilt, MethodInvocation).markers.find_first(ParseExceptionResult)
    assert rebuilt_marker is not None
    assert rebuilt_marker.message == "boom"
