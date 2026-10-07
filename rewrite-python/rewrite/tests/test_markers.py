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

from rewrite import random_id
from rewrite.java import Empty, Space
from rewrite.markers import Markers, SearchResult


def _tree() -> Empty:
    return Empty(random_id(), Space.EMPTY, Markers.EMPTY)


def test_found_leaves_a_tree_with_an_equal_search_result_unchanged():
    once = SearchResult.found(_tree(), "matched")
    twice = SearchResult.found(once, "matched")

    assert twice is once
    assert len(twice.markers.find_all(SearchResult)) == 1


def test_found_keeps_search_results_with_distinct_descriptions():
    marked = SearchResult.found(SearchResult.found(_tree(), "first"), "second")

    assert [m.description for m in marked.markers.find_all(SearchResult)] == ["first", "second"]
