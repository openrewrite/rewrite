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
