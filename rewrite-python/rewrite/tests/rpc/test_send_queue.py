"""A changed ref-deduplicated slot is re-added under a fresh ref, never CHANGEd
(see RpcSendQueue._send_as_ref for why)."""
from rewrite.rpc.send_queue import RpcSendQueue


def test_changed_ref_slot_is_re_added_instead_of_changed():
    q = RpcSendQueue()

    q._send_as_ref("T1", None)
    q._send_as_ref("T2", "T1")
    # A repeat of the same transition dedups against the ref registered by the re-add
    q._send_as_ref("T2", "T1")

    assert [d['state'] for d in q.q] == ['ADD', 'ADD', 'ADD']
    assert q.q[0]['ref'] == 1 and q.q[0]['value'] == 'T1'
    assert q.q[1]['ref'] == 2 and q.q[1]['value'] == 'T2'
    assert q.q[2] == {'state': 'ADD', 'ref': 2}


def test_complex_value_has_no_wire_form():
    q = RpcSendQueue()

    assert q._get_primitive_value(1j) is None
    assert q._get_value_type(1j) is None


def test_value_without_a_wire_form_is_sent_as_null():
    # an ADD or CHANGE that carries nothing is, to the peer, a broken message or no change at all
    q = RpcSendQueue()

    q.send(1j, None)
    q.send(2j, 1j)
    q.send(float('inf'), 1.5)
    q.send_list([3j], [1j], lambda x: 0)

    assert q.q == [{'state': 'DELETE'}, {'state': 'DELETE'}, {'state': 'DELETE'},
                   {'state': 'CHANGE'}, {'state': 'CHANGE', 'value': [0]}, {'state': 'DELETE'}]


def test_change_to_a_padded_space_is_sent():
    # read through the padding of each side, so that the space is not compared with itself
    from rewrite import Markers
    from rewrite.java import JLeftPadded, JRightPadded, Space
    from rewrite.rpc.python_sender import PythonRpcSender

    sender = PythonRpcSender()
    for visit, padded in [
        (sender._visit_left_padded, lambda space: JLeftPadded(Space.EMPTY, space, Markers.EMPTY)),
        (sender._visit_right_padded, lambda space: JRightPadded(space, Space.EMPTY, Markers.EMPTY)),
    ]:
        before, after = padded(Space([], ' ')), padded(Space([], '  '))
        q = RpcSendQueue()

        q.send(after, before, lambda: visit(after, q))

        assert {'state': 'CHANGE', 'valueType': None, 'value': '  '} in q.q


def test_int_value_serializes_as_a_number_at_any_magnitude():
    q = RpcSendQueue()

    assert q._get_primitive_value(2 ** 63) == 2 ** 63
    # Past Jackson's 1000-character cap on a JSON number there is no wire form.
    assert q._get_primitive_value(10 ** 1000) is None


def test_changed_ref_list_item_is_re_added_instead_of_changed():
    q = RpcSendQueue()
    ident = lambda s: s[:1]

    q.send_list(["A1"], None, ident, as_ref=True)
    q.send_list(["A2"], ["A1"], ident, as_ref=True)

    states = [d['state'] for d in q.q]
    # first list: ADD (list) + positions + ADD (item, ref 1)
    # second list: CHANGE (list) + positions + ADD (item, ref 2)
    assert states == ['ADD', 'CHANGE', 'ADD', 'CHANGE', 'CHANGE', 'ADD']
    assert q.q[2]['ref'] == 1 and q.q[2]['value'] == 'A1'
    assert q.q[5]['ref'] == 2 and q.q[5]['value'] == 'A2'


def _states(q):
    return [d['state'] for d in q.q]


def test_reordered_elements_are_repositioned_not_resent():
    """The positions array is what lets a reorder cost one integer per element
    instead of re-sending the elements themselves."""
    a, b, c = "A", "B", "C"
    q = RpcSendQueue()

    q.send_list([c, a, b], [a, b, c], lambda x: x)

    assert _states(q) == ['CHANGE', 'CHANGE', 'NO_CHANGE', 'NO_CHANGE', 'NO_CHANGE']
    assert q.q[1]['value'] == [2, 0, 1]


def test_every_element_is_added_when_the_before_list_is_empty():
    q = RpcSendQueue()

    q.send_list(["A", "B"], [], lambda x: x)

    assert _states(q) == ['CHANGE', 'CHANGE', 'ADD', 'ADD']
    assert q.q[1]['value'] == [-1, -1]


def test_mixed_adds_and_removals():
    a, b, c, d = "A", "B", "C", "D"
    q = RpcSendQueue()

    q.send_list([a, "E", "F", c], [a, b, c, d], lambda x: x)

    assert _states(q) == ['CHANGE', 'CHANGE', 'NO_CHANGE', 'ADD', 'ADD', 'NO_CHANGE']
    assert q.q[1]['value'] == [0, -1, -1, 2]


def test_unchanged_list_is_no_change():
    before = ["A", "B"]
    q = RpcSendQueue()

    q.send_list(before, before, lambda x: x)

    assert _states(q) == ['NO_CHANGE']
