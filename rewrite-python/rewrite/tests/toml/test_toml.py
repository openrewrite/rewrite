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

"""The TOML LST: printing, visiting and the RPC codecs.

Python has no TOML parser (documents are parsed on the Java side), so the trees
here are built by hand. Parity with the Java codecs is covered by
PythonTomlRecipeIntegTest.
"""
import json
import math
from datetime import UTC, date, datetime, time, timedelta, timezone
from pathlib import Path

import pytest

from rewrite import Markers, SearchResult
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue
from rewrite.toml import (
    Array,
    ArrayTable,
    Comment,
    Document,
    Identifier,
    InlineTable,
    KeyValue,
    Literal,
    Space,
    Table,
    TomlRightPadded,
    TomlType,
    TomlVisitor,
)
from rewrite.toml.rpc import TOML_SOURCE_FILE_TYPE, decode_value, encode_value
from rewrite.utils import random_id

P = TomlType.Primitive

SOURCE = '''# leading comment
title = "TOML" # trailing
count = 42
pos = inf
when = 1979-05-27T07:32:00-08:00
points = [ 1, 2 ]
inline = { x = 1 }

[owner]
name = "Tom"

[[products]]
sku = 7
'''


def _space(whitespace: str = '', *comments: Comment) -> Space:
    return Space.build(list(comments), whitespace)


def _padded(element, after: str = '') -> TomlRightPadded:
    return TomlRightPadded(element, _space(after), Markers.EMPTY)


def _ident(name: str, prefix: str = '') -> Identifier:
    return Identifier(random_id(), _space(prefix), Markers.EMPTY, name, name)


def _lit(type_: TomlType.Primitive, source: str, value, prefix: str = ' ') -> Literal:
    return Literal(random_id(), _space(prefix), Markers.EMPTY, type_, source, value)


def _kv(key: str, value, prefix=None) -> KeyValue:
    return KeyValue(random_id(), prefix if isinstance(prefix, Space) else _space('\n' if prefix is None else prefix),
                    Markers.EMPTY, _padded(_ident(key), ' '), value)


def _table(name, values, prefix: str, marker=None) -> Table:
    markers = Markers(random_id(), [marker(random_id())]) if marker else Markers.EMPTY
    return Table(random_id(), _space(prefix), markers, _padded(_ident(name)) if name else None, values)


def _document() -> Document:
    return Document(
        random_id(), Path('pyproject.toml'), Space.EMPTY, Markers.EMPTY, 'UTF-8', False, None, None,
        [
            _kv('title', _lit(P.String, '"TOML"', 'TOML'),
                _space('', Comment(' leading comment', '\n', Markers.EMPTY))),
            _kv('count', _lit(P.Integer, '42', 42), _space(' ', Comment(' trailing', '\n', Markers.EMPTY))),
            _kv('pos', _lit(P.Float, 'inf', math.inf)),
            _kv('when', _lit(P.OffsetDateTime, '1979-05-27T07:32:00-08:00',
                             datetime(1979, 5, 27, 7, 32, tzinfo=timezone(timedelta(hours=-8))))),
            _kv('points', Array(random_id(), _space(' '), Markers.EMPTY, [
                _padded(_lit(P.Integer, '1', 1)),
                _padded(_lit(P.Integer, '2', 2), ' '),
            ])),
            _kv('inline', _table(None, [_padded(_kv('x', _lit(P.Integer, '1', 1), ' '), ' ')], ' ', InlineTable)),
            _table('owner', [_padded(_kv('name', _lit(P.String, '"Tom"', 'Tom')))], '\n\n'),
            _table('products', [_padded(_kv('sku', _lit(P.Integer, '7', 7)))], '\n\n', ArrayTable),
        ],
        _space('\n'),
    )


def _wire(data):
    # The transport carries JSON, which is what the receiver is written against.
    data = json.loads(json.dumps(data))

    def pull():
        out = data[:]
        data.clear()
        return out

    return pull


def _round_trip(after: Document, sent_before: Document = None, received_before: Document = None) -> Document:
    data = list(RpcSendQueue(TOML_SOURCE_FILE_TYPE).generate(after, sent_before))
    return PythonRpcReceiver().receive(received_before, RpcReceiveQueue({}, TOML_SOURCE_FILE_TYPE, _wire(data)))


def _literals(doc: Document) -> dict:
    values = {}

    class _Collect(TomlVisitor):
        def visit_key_value(self, key_value, p):
            if isinstance(key_value.value, Literal):
                values[key_value.key.name] = key_value.value.value
            return super().visit_key_value(key_value, p)

    _Collect().visit(doc, None)
    return values


def test_prints_every_tree_type():
    # given
    doc = _document()

    # when
    printed = doc.print_all()

    # then
    assert printed == SOURCE


def test_prints_search_result_markers():
    # given
    doc = _document()
    owner = doc.values[6]
    doc = doc.replace(values=doc.values[:6] + [SearchResult.found(owner, 'found')] + doc.values[7:])

    # when
    printed = doc.print_all()

    # then
    assert '\n\n~~(found)~~>[owner]\n' in printed


def test_rpc_round_trip_preserves_tree_and_literal_values():
    # given
    doc = _document()

    # when
    received = _round_trip(doc)

    # then
    assert received.print_all() == SOURCE
    assert received.id == doc.id
    assert received.source_path == Path('pyproject.toml')
    assert _literals(received) == _literals(doc)
    assert received.values[5].value.markers.find_first(InlineTable) is not None
    assert received.values[7].markers.find_first(ArrayTable) is not None


def test_rpc_change_applies_only_the_edited_literal():
    # given
    before = _document()
    received_before = _round_trip(before)
    kv = before.values[1]
    edited = kv.replace(value=kv.value.replace(source='43', value=43))
    after = before.replace(values=[before.values[0], edited] + before.values[2:])

    # when
    received = _round_trip(after, before, received_before)

    # then
    assert received.print_all() == SOURCE.replace('count = 42', 'count = 43')
    assert received.values[1].value.value == 43
    assert received.values[6] is received_before.values[6]


@pytest.mark.parametrize('type_, value', [
    (P.String, 'text'),
    (P.Integer, 2 ** 63 - 1),
    (P.Boolean, False),
    (P.Float, 3.25),
    (P.Float, math.inf),
    (P.Float, -math.inf),
    (P.LocalDate, date(1979, 5, 27)),
    (P.LocalDateTime, datetime(1979, 5, 27, 7, 32)),
    (P.LocalTime, time(7, 32, 0, 999999)),
    (P.OffsetDateTime, datetime(1979, 5, 27, 7, 32, tzinfo=UTC)),
])
def test_literal_value_survives_json_encoding(type_, value):
    # when
    decoded = decode_value(type_, json.loads(json.dumps(encode_value(value))))

    # then
    assert decoded == value
    assert type(decoded) is type(value)


def test_nan_survives_json_encoding():
    # when
    decoded = decode_value(P.Float, json.loads(json.dumps(encode_value(math.nan))))

    # then
    assert math.isnan(decoded)


def test_generic_tree_visitor_traverses_toml():
    # given
    seen = []

    class _Names(TomlVisitor):
        def visit_identifier(self, identifier, p):
            seen.append(identifier.name)
            return super().visit_identifier(identifier, p)

    # when
    _Names().visit(_document(), None)

    # then
    assert seen == ['title', 'count', 'pos', 'when', 'points', 'inline', 'x', 'owner', 'name', 'products', 'sku']


def test_handle_print_prints_toml_document(monkeypatch):
    # given
    import rewrite.rpc.server as server
    doc = _document()
    monkeypatch.setattr(server, 'get_object_from_java', lambda obj_id, source_file_type=None: doc)

    # when
    printed = server.handle_print({'treeId': str(doc.id), 'sourceFileType': TOML_SOURCE_FILE_TYPE})

    # then
    assert printed == SOURCE
