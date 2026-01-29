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

"""Tests to verify replace() returns the same object when nothing changes.

This is critical for performance - visitor traversals call replace() on every
node, and if nothing changes, we should return the same object to avoid
unnecessary allocations and GC pressure.
"""

import pytest
from uuid import uuid4

from rewrite import Markers, random_id
from rewrite.markers import SearchResult
from rewrite.java.support_types import Space, TextComment, JRightPadded


class TestReplaceIdentity:
    """Tests that replace() returns self when values are unchanged."""

    def test_markers_replace_same_values_returns_same_object(self):
        """Markers.replace() should return self when nothing changes."""
        markers = Markers(random_id(), [])

        # Replace with the exact same values
        result = markers.replace(id=markers.id, markers=markers.markers)

        assert result is markers, (
            "Markers.replace() should return the same object when nothing changes. "
            f"Original id: {id(markers)}, Result id: {id(result)}"
        )

    def test_markers_replace_same_id_returns_same_object(self):
        """Markers.replace() with same id should return self."""
        markers = Markers(random_id(), [])

        result = markers.replace(id=markers.id)

        assert result is markers, (
            "Markers.replace(id=same_id) should return the same object"
        )

    def test_markers_replace_same_markers_list_returns_same_object(self):
        """Markers.replace() with same markers list should return self."""
        markers = Markers(random_id(), [])

        result = markers.replace(markers=markers.markers)

        assert result is markers, (
            "Markers.replace(markers=same_list) should return the same object"
        )

    def test_markers_replace_different_value_returns_new_object(self):
        """Markers.replace() with different value should return new object."""
        markers = Markers(random_id(), [])
        new_id = random_id()

        result = markers.replace(id=new_id)

        assert result is not markers, (
            "Markers.replace() with different id should return a new object"
        )
        assert result.id == new_id

    def test_space_replace_same_values_returns_same_object(self):
        """Space.replace() should return self when nothing changes."""
        space = Space([], " ")

        result = space.replace(comments=space.comments, whitespace=space.whitespace)

        assert result is space, (
            "Space.replace() should return the same object when nothing changes. "
            f"Original id: {id(space)}, Result id: {id(result)}"
        )

    def test_search_result_replace_same_values_returns_same_object(self):
        """SearchResult.replace() should return self when nothing changes."""
        sr = SearchResult(random_id(), "test description")

        result = sr.replace(id=sr.id, description=sr.description)

        assert result is sr, (
            "SearchResult.replace() should return the same object when nothing changes"
        )

    def test_jright_padded_replace_same_values_returns_same_object(self):
        """JRightPadded.replace() should return self when nothing changes."""
        from rewrite.java.tree import Identifier

        space = Space.EMPTY
        markers = Markers.EMPTY
        ident = Identifier(random_id(), space, markers, [], "test", None, None)
        padded = JRightPadded(ident, space, markers)

        result = padded.replace(element=padded.element, after=padded.after, markers=padded.markers)

        assert result is padded, (
            "JRightPadded.replace() should return the same object when nothing changes"
        )


class TestListMapIdentity:
    """Tests that list_map returns original list when nothing changes."""

    def test_utils_list_map_unchanged_returns_same_list(self):
        """utils.list_map should return original list when nothing changes."""
        from rewrite.utils import list_map

        original = [1, 2, 3, 4, 5]
        result = list_map(lambda x: x, original)

        assert result is original, (
            "list_map should return the same list when nothing changes"
        )

    def test_utils_list_map_changed_returns_new_list(self):
        """utils.list_map should return new list when something changes."""
        from rewrite.utils import list_map

        original = [1, 2, 3, 4, 5]
        result = list_map(lambda x: x * 2, original)

        assert result is not original
        assert result == [2, 4, 6, 8, 10]

    def test_java_visitor_list_map_unchanged_returns_same_list(self):
        """java/visitor.py list_map should return original list when unchanged."""
        from rewrite.java.visitor import list_map

        original = [1, 2, 3, 4, 5]
        result = list_map(lambda x: x, original)

        # This test documents the CURRENT behavior - it may fail if list_map
        # doesn't preserve identity (which would be a bug to fix)
        assert result is original, (
            "java/visitor.py list_map should return the same list when nothing changes"
        )

    def test_python_visitor_list_map_unchanged_returns_same_list(self):
        """python/visitor.py list_map should return original list when unchanged."""
        from rewrite.python.visitor import list_map

        original = [1, 2, 3, 4, 5]
        result = list_map(lambda x: x, original)

        assert result is original, (
            "python/visitor.py list_map should return the same list when nothing changes"
        )


class TestReplacePerformanceContract:
    """Tests documenting the performance contract for replace()."""

    def test_replace_no_args_returns_same_object(self):
        """replace() with no args should definitely return self."""
        markers = Markers(random_id(), [])

        # No arguments means nothing could have changed
        result = markers.replace()

        assert result is markers, (
            "replace() with no arguments should always return self"
        )

    def test_visitor_pattern_no_change_preserves_identity(self):
        """Simulates a visitor that makes no changes - should preserve identity."""
        from rewrite.java.tree import Identifier
        from rewrite.java.support_types import Space

        space = Space.EMPTY
        markers = Markers.EMPTY
        ident = Identifier(random_id(), space, markers, [], "unchanged", None, None)

        # Simulate what a visitor does when nothing changes
        new_prefix = space  # Same object
        new_markers = markers  # Same object

        result = ident.replace(prefix=new_prefix, markers=new_markers)

        assert result is ident, (
            "When a visitor makes no changes, replace() should return the original object. "
            "This is critical for performance - a visitor pass over 10,000 nodes should not "
            "allocate 10,000 new objects if nothing changes."
        )
