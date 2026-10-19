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

"""A transfer its receiver fails to take is undone on both sides.

The sender of a GetObject stream counts the object, and every ref it assigned while sending
it, as received. A receiver that fails part-way holds neither, so without being told the
sender goes on to answer with changes to a tree, and bare refs to objects, the receiver
never built: one unreadable file used to fail every file after it. ``AbortGetObject`` is how
the receiver says so.
"""

import ast
from pathlib import Path
from uuid import uuid4

import pytest

from rewrite import InMemoryExecutionContext, Markers, random_id
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc import receive_queue, server
from rewrite.rpc.reference import ReferenceMap
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = 'org.openrewrite.python.tree.Py$CompilationUnit'


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source)).replace(source_path=Path("a.py"))


def _unreadable(stream):
    """The stream with its last tree made one no receiver has a codec for, so that reading
    fails well in, after refs have been recorded."""
    last = max(i for i, message in enumerate(stream)
               if (message.get('valueType') or '').startswith('org.openrewrite.java.tree.J$'))
    return stream[:last] + [{**stream[last], 'valueType': 'org.openrewrite.NoSuchTree'}] + stream[last + 1:]


@pytest.fixture
def transfers(monkeypatch):
    """Fresh tables for this side of the connection to its host."""
    for name in ("local_objects", "remote_objects", "remote_refs"):
        monkeypatch.setattr(server, name, {})
    monkeypatch.setattr(server, "local_refs", ReferenceMap())
    monkeypatch.setattr(server, "_last_sent", [])


def test_an_aborted_transfer_is_sent_whole_again(transfers):
    cu = _parse("x = 1\n")
    tree_id = str(cu.id)
    server.local_objects[tree_id] = cu
    request = {'id': tree_id, 'sourceFileType': _CU_TYPE}

    sent = server.handle_get_object(request)
    assert tree_id in server.remote_objects
    assert len(server.local_refs) > 0

    assert server.handle_request('AbortGetObject', {'id': tree_id}) is True
    assert tree_id not in server.remote_objects
    assert len(server.local_refs) == 0
    # not as no change to a tree the host dropped, nor citing refs it never recorded
    assert server.handle_get_object(request) == sent


def test_aborting_what_is_not_the_latest_transfer_drops_every_ref(transfers):
    first, second = _parse("x = 1\n"), _parse("y = 2\n")
    for cu in (first, second):
        server.local_objects[str(cu.id)] = cu
        server.handle_get_object({'id': str(cu.id), 'sourceFileType': _CU_TYPE})

    # which refs the first assigned is no longer known, and sending everything whole is always safe
    assert server.handle_abort_get_object({'id': str(first.id)}) is True
    assert len(server.local_refs) == 0
    assert str(first.id) not in server.remote_objects

    assert server.handle_abort_get_object({'id': str(first.id)}) is True
    assert server.handle_abort_get_object({'id': 'never sent'}) is True
    assert server.handle_abort_get_object({}) is True


@pytest.mark.parametrize("host_has_the_method", [True, False])
def test_a_failed_receive_is_rolled_back_and_reported(transfers, monkeypatch, host_has_the_method):
    cu = _parse("x = 1\ny = 2\n")
    tree_id = str(cu.id)
    stream = _unreadable(RpcSendQueue(_CU_TYPE).generate(cu, None))
    requests = []

    def send_request(method, params, timeout_seconds=30.0):
        requests.append((method, params.get('id')))
        if method == 'GetObject':
            return stream
        if not host_has_the_method:
            raise RuntimeError("Unknown method: AbortGetObject")
        return True

    monkeypatch.setattr(server, "send_request", send_request)
    earlier = object()
    server.remote_refs[1000] = earlier
    server.remote_objects[tree_id] = cu

    with pytest.raises(RuntimeError, match="No RPC codec registered on the Python side for 'org.openrewrite.NoSuchTree'"):
        server.get_object_from_java(tree_id, _CU_TYPE)

    assert requests == [('GetObject', tree_id), ('AbortGetObject', tree_id)]
    assert tree_id not in server.remote_objects
    if host_has_the_method:
        assert server.remote_refs == {1000: earlier}
    else:
        # a host that could not roll back goes on citing the refs it sent, so they are kept
        assert len(server.remote_refs) > 1
        assert server.remote_refs[1000] is earlier


def test_a_transfer_the_host_failed_to_send_is_not_aborted(transfers, monkeypatch):
    requests = []

    def send_request(method, params, timeout_seconds=30.0):
        requests.append(method)
        raise RuntimeError("the host could not send it")

    monkeypatch.setattr(server, "send_request", send_request)

    with pytest.raises(RuntimeError, match="the host could not send it"):
        server.get_object_from_java("T", _CU_TYPE)

    assert requests == ['GetObject']


def test_refs_of_a_transfer_the_host_failed_part_way_through_are_dropped(transfers, monkeypatch):
    cu = _parse("x = 1\ny = 2\n")
    stream = RpcSendQueue(_CU_TYPE).generate(cu, None)
    # every message but the last, which the host fails before it can send
    pages = [stream[:-1]]
    monkeypatch.setattr(server, "_issue_request", lambda method, params: "next page")

    def send_request(method, params, timeout_seconds=30.0):
        return pages.pop()

    def await_response(request_id, method, timeout_seconds=30.0):
        raise RuntimeError("the host could not send the rest")

    monkeypatch.setattr(server, "send_request", send_request)
    monkeypatch.setattr(server, "_await_response", await_response)

    with pytest.raises(RuntimeError, match="the host could not send the rest"):
        server.get_object_from_java(str(cu.id), _CU_TYPE)

    assert server.remote_refs == {}


@pytest.fixture
def hub(monkeypatch):
    """Fresh hub tables, and a host that must not be asked for anything."""
    for name in ("_hub_tree", "_hub_served", "_hub_send_refs", "_hub_recv_refs", "_hub_send_checkpoint",
                 "_hub_recv_checkpoint", "_hub_last_served", "local_objects", "_ref_checkpoints",
                 "_local_ref_checkpoints"):
        monkeypatch.setattr(server, name, {})

    def send_request(method, params, timeout_seconds=30.0):
        pytest.fail(f"{method} was relayed to the host")

    monkeypatch.setattr(server, "send_request", send_request)


def test_a_child_that_fails_to_take_a_tree_is_served_it_whole_again(hub, monkeypatch):
    cu = _parse("x = 1\n")
    tree_id, bundle = str(cu.id), "pkg"
    monkeypatch.setattr(server, "get_object_from_java", lambda obj_id, source_file_type=None: cu)
    server._hub_acquire(tree_id, _CU_TYPE)
    request = {'id': tree_id, 'sourceFileType': _CU_TYPE}

    served = server._serve_child_object('GetObject', request, bundle)

    # it is the facade that served the child: relayed, the host would undo a transfer of its own
    assert server._serve_child_object('AbortGetObject', {'id': tree_id}, bundle) is True
    assert (bundle, tree_id) not in server._hub_served
    assert len(server._hub_send_refs[bundle]) == 0
    assert server._serve_child_object('GetObject', request, bundle) == served


class _ChildWithAnUnreadableEdit:
    """A child whose first answer cannot be read, and which undoes it when told to."""

    def __init__(self, served, edit):
        self._baseline, self._edit = served, edit
        self.refs = ReferenceMap()
        self.requests = []

    def request(self, bundle, method, params):
        self.requests.append(method)
        if method == 'AbortGetObject':
            self.refs.clear()
            self._baseline = None
            return True
        stream = RpcSendQueue(params['sourceFileType'], self.refs).generate(self._edit, self._baseline)
        self._baseline = self._edit
        return _unreadable(stream) if self.requests == ['GetObject'] else stream


def test_a_pull_the_facade_fails_to_take_is_undone_in_the_child_too(hub, monkeypatch):
    cu, edit = _parse("x = 1\n"), _parse("x = 1\ny = 2\n")
    tree_id, bundle = str(cu.id), "pkg"
    monkeypatch.setattr(server, "get_object_from_java", lambda obj_id, source_file_type=None: cu)
    server._hub_acquire(tree_id, _CU_TYPE)
    server._hub_serve_child(bundle, tree_id, _CU_TYPE)
    child = _ChildWithAnUnreadableEdit(cu, edit)

    with pytest.raises(RuntimeError, match="No RPC codec registered on the Python side"):
        server._hub_pull_child_edit(child, bundle, tree_id, _CU_TYPE)

    assert child.requests == ['GetObject', 'AbortGetObject']
    assert server._hub_recv_refs[bundle] == {}
    assert (bundle, tree_id) not in server._hub_served
    assert server._hub_tree[tree_id] is cu

    server._hub_pull_child_edit(child, bundle, tree_id, _CU_TYPE)
    assert PythonPrinter().print(server._hub_tree[tree_id]) == "x = 1\ny = 2\n"


class _ChildWithoutTheMethod:
    def request(self, bundle, method, params):
        if method == 'AbortGetObject':
            raise RuntimeError("child error for AbortGetObject: Unknown method: AbortGetObject")
        return _unreadable(RpcSendQueue(params['sourceFileType']).generate(_parse("x = 1\ny = 2\n"), None))


def test_a_child_that_cannot_roll_back_keeps_its_baseline_and_refs_here(hub, monkeypatch):
    cu = _parse("x = 1\n")
    tree_id, bundle = str(cu.id), "pkg"
    monkeypatch.setattr(server, "get_object_from_java", lambda obj_id, source_file_type=None: cu)
    server._hub_acquire(tree_id, _CU_TYPE)
    server._hub_serve_child(bundle, tree_id, _CU_TYPE)

    with pytest.raises(RuntimeError, match="No RPC codec registered on the Python side"):
        server._hub_pull_child_edit(_ChildWithoutTheMethod(), bundle, tree_id, _CU_TYPE)

    # it goes on answering against what it was served, and citing the refs it sent
    assert server._hub_served[(bundle, tree_id)] is cu
    assert len(server._hub_recv_refs[bundle]) > 0


def _find_source_files():
    from rewrite.rpc.rpc_recipe import JavaRecipeVisitor, prepare_java_recipe
    return JavaRecipeVisitor(prepare_java_recipe('org.openrewrite.FindSourceFiles', {'filePattern': '**/*.py'}))


@pytest.mark.requires_java_rpc
def test_a_host_that_fails_to_take_a_tree_is_sent_it_whole_again(java_rpc):
    find = _find_source_files()
    cu = _parse("x = 1\ny = 2\n")
    # a marker the host has no class to build, on the last statement so the transfer is well under way
    padded = cu.padding.statements
    unreceivable = cu.padding.replace(_statements=padded[:-1] + [padded[-1].replace(element=padded[-1].element.replace(
        _markers=Markers(random_id(), [{'kind': 'org.openrewrite.NoSuchMarker', 'id': str(uuid4())}])))])

    with pytest.raises(RuntimeError, match="NoSuchMarker"):
        find.visit(unreceivable, InMemoryExecutionContext())

    found = find.visit(cu, InMemoryExecutionContext())
    assert PythonPrinter().print(found) == "/*~~>*/x = 1\ny = 2\n"


@pytest.mark.requires_java_rpc
def test_a_tree_this_side_fails_to_take_is_sent_to_it_whole_again(java_rpc, monkeypatch):
    find = _find_source_files()
    cu = _parse("x = 1\ny = 2\n")
    tree_id = str(cu.id)

    def unreadable(marker, q):
        raise ValueError("unreadable")

    # the host's edit arrives as a change to the tree it was sent, and reading it fails once under way
    with monkeypatch.context() as broken:
        broken.setitem(receive_queue._codecs[''], 'SearchResult', unreadable)
        with pytest.raises(ValueError, match="unreadable"):
            find.visit(cu, InMemoryExecutionContext())
    assert server.remote_refs == {}
    assert tree_id not in server.remote_objects

    found = server.get_object_from_java(tree_id, _CU_TYPE)
    assert PythonPrinter().print(found) == "/*~~>*/x = 1\ny = 2\n"
