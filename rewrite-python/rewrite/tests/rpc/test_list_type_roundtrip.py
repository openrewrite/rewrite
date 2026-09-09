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

"""The concrete sequence type (list vs tuple) must not affect the list diff protocol.

``send_list`` resolves the list-level state from nullability alone, never from
``type(after) != type(before)``: two non-null sequences always diff as CHANGE with
positions into the before sequence. A type comparison there would emit ADD, which the
receiver answers by resetting its before to an empty list — desyncing every position.
"""
from rewrite.rpc.receive_queue import RpcReceiveQueue
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


def test_tuple_before_list_after():
    assert _round_trip_list(["A"], ("A", "B")) == ["A"]


def test_list_before_tuple_after():
    assert _round_trip_list(("A", "B"), ["A"]) == ["A", "B"]
