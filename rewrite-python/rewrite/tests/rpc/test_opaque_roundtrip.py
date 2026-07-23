"""Opaque values (a type the remote side has a codec for but Python does not) are received as
`{'kind': <valueType>, **fields}`. The sender must be able to emit that shape back so it survives a
send->receive round-trip -- required for the facade to re-serialize a child's modified tree that
still carries Java-origin opaque markers."""

from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue


def _round_trip(value):
    q = RpcSendQueue(None)
    q.send(value, None)                 # serialize the value as the sender would a marker element
    q.q.append({'state': 'END_OF_OBJECT'})
    data = q.q
    it = iter([data])

    def pull_batch():
        try:
            batch = next(it)
            if batch and batch[-1].get('state') == 'END_OF_OBJECT':
                batch = batch[:-1]
            return batch
        except StopIteration:
            return []

    return RpcReceiveQueue({}, None, pull_batch).receive(None, None)


def test_opaque_value_round_trips():
    opaque = {'kind': 'com.example.SomeMarker', 'id': '3f2504e0-4f89-41d3-9a0c-0305e82c3301', 'x': 1}
    assert _round_trip(opaque) == opaque


def test_opaque_value_with_nested_fields_round_trips():
    opaque = {'kind': 'com.example.Nested', 'id': 'id-1', 'inner': {'a': 2, 'b': ['x', 'y']}}
    assert _round_trip(opaque) == opaque


def test_plain_dict_is_not_mistaken_for_opaque():
    # An ordinary dict (no 'kind') must not be tagged with a type -- unchanged from before the fix.
    assert RpcSendQueue(None)._get_value_type({'a': 1, 'b': 'two'}) is None
    assert RpcSendQueue(None)._get_value_type({'kind': 'com.example.Foo', 'id': 'x'}) == 'com.example.Foo'


def test_visit_markers_with_opaque_marker_serializes_without_crashing():
    from rewrite.markers import Markers
    from rewrite.rpc.python_sender import PythonRpcSender
    opaque = {'kind': 'com.example.SomeMarker', 'id': '3f2504e0-4f89-41d3-9a0c-0305e82c3301', 'x': 1}
    q = RpcSendQueue(None)
    PythonRpcSender()._visit_markers(Markers(0, [opaque]), q)  # previously raised here
    assert any(d.get('valueType') == 'com.example.SomeMarker' for d in q.q)
