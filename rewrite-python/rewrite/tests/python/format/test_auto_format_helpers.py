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

"""Unit tests for auto_format and maybe_auto_format helper functions."""

from rewrite.python.format import auto_format, maybe_auto_format
from rewrite.python.template.engine import TemplateEngine


class TestMaybeAutoFormat:
    """Tests for maybe_auto_format identity guard."""

    def test_same_object_returns_unchanged(self):
        """When before is after (same identity), return after without formatting."""
        tree = TemplateEngine.get_template_tree("x+1", {})
        result = maybe_auto_format(tree, tree, None)
        assert result is tree

    def test_different_object_no_cursor_skips_gracefully(self):
        """When before is not after but no cursor, skip formatting without crash."""
        before = TemplateEngine.get_template_tree("x + 1", {})
        TemplateEngine.clear_cache()
        after = TemplateEngine.get_template_tree("y + 2", {})

        result = maybe_auto_format(before, after, None)
        assert result is not None


class TestAutoFormat:
    """Tests for auto_format fallback behavior."""

    def test_no_cursor_returns_tree_unchanged(self):
        """auto_format without cursor skips formatting and returns tree."""
        tree = TemplateEngine.get_template_tree("x+1", {})
        result = auto_format(tree, None)
        assert result is tree
