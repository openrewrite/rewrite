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

"""RecipesThatMadeChanges travels as recipe identity, one frame per stack entry."""
from uuid import UUID

import rewrite.rpc.python_receiver  # noqa: F401  (registers the marker codecs)
from rewrite.markers import RecipeIdentity, RecipesThatMadeChanges
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue
from rewrite.utils import random_id


def _send(marker):
    sq = RpcSendQueue()
    sq.send(marker, None)
    return list(sq.q)


def _receive(batch):
    remaining = batch[:]

    def pull():
        out = remaining[:]
        remaining.clear()
        return out

    return RpcReceiveQueue({}, None, pull).receive(None)


def test_recipe_stack_round_trips_as_identity():
    marker_id = random_id()
    marker = RecipesThatMadeChanges(marker_id, [[
        RecipeIdentity('org.openrewrite.text.ChangeText'),
        RecipeIdentity(
            'org.openrewrite.text.FindAndReplace',
            'Find and replace',
            'Find and replace `blacklist`',
            {'find': 'blacklist', 'regex': True},
            300000,
        ),
    ]])

    # Hop 1 exercises the receiver against a Java-shaped stream; hop 2 exercises this peer's
    # own sender against what its receiver produced.
    received = _receive(_send(marker))

    second_hop = _send(received)
    assert any(d.get('value') == 'org.openrewrite.text.FindAndReplace' for d in second_hop), \
        'second hop carried no recipe identity; the marker diffed as NO_CHANGE'
    received = _receive(second_hop)

    # `random_id()` yields the internal int form; `.id` reconstructs the UUID.
    assert received.id == UUID(int=marker_id)
    assert len(received.recipes) == 1
    assert len(received.recipes[0]) == 2

    # A frame carrying only a name must not acquire values from its neighbour.
    first = received.recipes[0][0]
    assert first.name == 'org.openrewrite.text.ChangeText'
    assert first.display_name is None
    assert first.options is None
    assert first.estimated_effort_per_occurrence_millis is None

    second = received.recipes[0][1]
    assert second.name == 'org.openrewrite.text.FindAndReplace'
    assert second.display_name == 'Find and replace'
    assert second.instance_name == 'Find and replace `blacklist`'
    assert second.options == {'find': 'blacklist', 'regex': True}
    assert second.estimated_effort_per_occurrence_millis == 300000
