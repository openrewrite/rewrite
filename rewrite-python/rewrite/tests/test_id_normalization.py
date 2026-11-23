# Copyright 2026 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Constructor id normalization for LST nodes, markers, and Markers.

Ids are stored internally as 128-bit ints (see ``random_id``), but ``_id`` is
part of the public positional constructor surface: external recipes (and some
internal helpers) construct nodes passing ``uuid4()`` or another node's ``.id``
property. Constructors must normalize either form to the int representation;
otherwise the next ``.id`` access raises and equality silently breaks.
"""

from uuid import uuid4

from rewrite import Markers
from rewrite.java import Identifier, Space
from rewrite.markers import SearchResult
from rewrite.utils import random_id


def _identifier(id_) -> Identifier:
    return Identifier(id_, Space.EMPTY, Markers.EMPTY, [], "x", None, None)


class TestTreeIdNormalization:

    def test_constructor_accepts_uuid(self):
        uid = uuid4()
        ident = _identifier(uid)
        assert ident._id == uid.int
        assert ident.id == uid

    def test_constructor_accepts_int(self):
        raw = random_id()
        ident = _identifier(raw)
        assert ident._id == raw
        assert ident.id.int == raw

    def test_equality_and_hash_across_id_forms(self):
        uid = uuid4()
        from_uuid = _identifier(uid)
        from_int = _identifier(uid.int)
        assert from_uuid == from_int
        assert hash(from_uuid) == hash(from_int)


class TestMarkerIdNormalization:

    def test_marker_constructor_accepts_uuid(self):
        uid = uuid4()
        marker = SearchResult(uid, "found")
        assert marker._id == uid.int
        assert marker.id == uid

    def test_markers_constructor_accepts_uuid(self):
        uid = uuid4()
        markers = Markers(uid, [])
        assert markers._id == uid.int
        assert markers.id == uid

    def test_search_result_found_yields_usable_markers(self):
        # SearchResult.found rebuilds Markers passing the public `.id` (a UUID);
        # the result must survive a subsequent `.id` access.
        ident = _identifier(random_id())
        found = SearchResult.found(ident, "match")
        assert found.markers.id == ident.markers.id
        assert found.markers.find_first(SearchResult) is not None


class TestRpcPlaceholderConstruction:
    """The RPC receiver constructs placeholder instances with every field None
    (see ``make_dataclass_factory``) and fills them in field by field. The id
    normalization hooks must let a None id pass through untouched."""

    def test_tree_factory_accepts_none_id(self):
        from rewrite.rpc.receive_queue import make_dataclass_factory
        placeholder = make_dataclass_factory(Identifier)()
        assert placeholder._id is None
        uid = uuid4()
        assert placeholder.replace(_id=uid.int).id == uid

    def test_marker_factory_accepts_none_id(self):
        from rewrite.rpc.receive_queue import make_dataclass_factory
        placeholder = make_dataclass_factory(SearchResult)()
        assert placeholder._id is None

    def test_markers_factory_accepts_none_id(self):
        from rewrite.rpc.receive_queue import make_dataclass_factory
        placeholder = make_dataclass_factory(Markers)()
        assert placeholder._id is None
