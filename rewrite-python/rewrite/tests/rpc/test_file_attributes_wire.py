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

"""A source file's fileAttributes is a codec field, so it arrives as a typed ADD followed by one
message per sub-field. Consuming only the ADD leaves the rest to be read as later fields."""
import rewrite.rpc.python_receiver  # noqa: F401  (registers the codec under test)
from rewrite.rpc.receive_queue import RpcReceiveQueue

_FILE_ATTRIBUTES = 'org.openrewrite.FileAttributes'


def _queue(batch):
    remaining = batch[:]

    def pull():
        out = remaining[:]
        remaining.clear()
        return out

    return RpcReceiveQueue({}, None, pull)


def test_file_attributes_consumes_every_sub_field():
    q = _queue([
        {'state': 'ADD', 'valueType': _FILE_ATTRIBUTES},
        {'state': 'ADD', 'value': '2026-08-16T10:15:30.123456789+02:00[Europe/Berlin]'},
        {'state': 'NO_CHANGE'},
        {'state': 'NO_CHANGE'},
        {'state': 'ADD', 'value': True},
        {'state': 'ADD', 'value': True},
        {'state': 'ADD', 'value': False},
        {'state': 'ADD', 'value': 42},
        {'state': 'ADD', 'value': 'the next field'},
    ])

    # Discarded rather than parsed: datetime holds neither Java's nanoseconds nor its zone id,
    # so keeping the value would only degrade it (see _register_file_attributes_codec).
    assert q.receive(None) is None
    assert q.receive(None) == 'the next field'


def test_null_file_attributes_consumes_one_message():
    q = _queue([
        {'state': 'NO_CHANGE'},
        {'state': 'ADD', 'value': 'the next field'},
    ])

    assert q.receive(None) is None
    assert q.receive(None) == 'the next field'
