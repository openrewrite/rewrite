# Copyright 2025 the original author or authors.
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
"""How ref ids are assigned, reused, and rolled back."""
from rewrite.rpc.reference import ReferenceMap


def test_ids_start_at_one_and_ascend():
    refs = ReferenceMap()
    assert refs.create(object()) == 1
    assert refs.create(object()) == 2


def test_an_object_keeps_the_id_it_was_given():
    refs = ReferenceMap()
    obj = object()
    assert refs.create(obj) == refs.get(obj)


def test_equal_but_distinct_objects_get_their_own_ids():
    refs = ReferenceMap()
    first, second = ['a'], ['a']
    assert refs.create(first) != refs.create(second)


def test_an_unsent_object_has_no_id():
    assert ReferenceMap().get(object()) is None


def test_an_id_recycled_by_the_allocator_is_not_mistaken_for_a_sent_object():
    refs = ReferenceMap()
    recycled = id(refs.create([1, 2, 3]) and [4, 5, 6])
    # Whatever now occupies that address, nothing was sent under it.
    assert all(refs.get(obj) is None for obj in ([4, 5, 6], [1, 2, 3]))
    assert isinstance(recycled, int)


def test_rollback_drops_only_what_came_after_the_snapshot():
    refs = ReferenceMap()
    kept = object()
    refs.create(kept)
    snapshot = refs.snapshot()
    dropped = object()
    refs.create(dropped)

    refs.rollback_to(snapshot)
    assert refs.get(kept) == 1
    assert refs.get(dropped) is None


def test_ids_resume_from_the_snapshot_after_a_rollback():
    refs = ReferenceMap()
    refs.create(object())
    snapshot = refs.snapshot()
    refs.create(object())
    refs.rollback_to(snapshot)

    assert refs.create(object()) == 2


def test_clear_restarts_the_sequence():
    refs = ReferenceMap()
    refs.create(object())
    refs.clear()

    assert len(refs) == 0
    assert refs.create(object()) == 1
