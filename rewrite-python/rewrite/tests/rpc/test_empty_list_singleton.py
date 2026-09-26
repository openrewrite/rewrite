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

"""Empty received lists share a single immutable instance.

A received LST holds millions of empty child collections; ``receive_list`` returns
one shared frozen ``[]`` for all of them instead of allocating a fresh list per field.
"""
import pytest

from rewrite.rpc.receive_queue import RpcReceiveQueue, _EMPTY_LIST
from rewrite.rpc.send_queue import RpcSendQueue


def _round_trip_list(after, before):
    sq = RpcSendQueue()
    sq.send_list(after, before, lambda x: x)
    batch = list(sq.q)

    def pull():
        out = batch[:]
        batch.clear()
        return out

    rq = RpcReceiveQueue({}, None, pull)
    return rq.receive_list(before)


def test_empty_result_is_shared_singleton():
    # given / when
    first = _round_trip_list([], [])
    second = _round_trip_list([], [])

    # then
    assert first is _EMPTY_LIST
    assert second is _EMPTY_LIST


def test_singleton_is_a_list_for_the_sender():
    # given
    result = _round_trip_list([], [])

    # then
    assert isinstance(result, list)
    assert result == []


def test_singleton_rejects_in_place_mutation():
    # given
    result = _round_trip_list([], [])

    # then
    with pytest.raises(TypeError):
        result.append("x")


def test_non_empty_result_is_a_fresh_mutable_list():
    # given / when
    result = _round_trip_list(["A"], [])

    # then
    assert result is not _EMPTY_LIST
    assert result == ["A"]
    result.append("B")
    assert result == ["A", "B"]
