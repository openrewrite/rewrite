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

"""Tests for ``TreeVisitor.adapt`` and the language-visitor adapters.

A subclass of ``TreeVisitor`` that overrides only ``pre_visit`` / ``post_visit``
must be usable on language-specific LST nodes (Py, J). Without the adapter
mechanism, the LST node's ``accept`` would call language-specific dispatch
methods (``visit_compilation_unit``, ``visit_method_declaration``, ...) that
the bare ``TreeVisitor`` doesn't implement, raising ``AttributeError``.
"""

from dataclasses import fields
from uuid import UUID
from rewrite import random_id

from rewrite import Markers, TreeVisitor
from rewrite.java.support_types import Space
from rewrite.markers import SearchResult


def _empty_py_compilation_unit(*, markers: Markers = Markers.EMPTY):
    """Construct a minimal ``Py.CompilationUnit`` for visitor tests."""
    from rewrite.python.tree import CompilationUnit
    cu_fields = {f.name: None for f in fields(CompilationUnit) if f.init}
    cu_fields['_id'] = random_id()
    cu_fields['_markers'] = markers
    cu_fields['_prefix'] = Space.EMPTY
    cu_fields['_imports'] = []
    cu_fields['_statements'] = []
    cu_fields['_eof'] = Space.EMPTY
    cu_fields['_source_path'] = None
    cu_fields['_charset_name'] = None
    cu_fields['_charset_bom_marked'] = False
    cu_fields['_checksum'] = None
    cu_fields['_file_attributes'] = None
    return CompilationUnit(**cu_fields)


class TestTreeVisitorAdapt:
    def test_bare_tree_visitor_visits_python_compilation_unit(self):
        """A bare ``TreeVisitor`` must be able to visit a ``Py.CompilationUnit``.

        Regression: previously raised
        ``AttributeError: '...' object has no attribute 'visit_compilation_unit'``
        because ``TreeVisitor.adapt`` returned ``self`` and the language dispatch
        in ``Py.accept`` flowed into ``accept_python`` with a bare visitor.
        """
        visited = []

        class _Collector(TreeVisitor):
            def pre_visit(self, tree, p):
                visited.append(tree)
                return tree

        cu = _empty_py_compilation_unit()
        result = _Collector().visit(cu, None)

        assert result is cu
        assert cu in visited

    def test_pre_visit_runs_for_root_through_adapter(self):
        """``pre_visit`` must run on the root node with the adapter in place."""
        call_count = 0

        class _Counter(TreeVisitor):
            def pre_visit(self, tree, p):
                nonlocal call_count
                call_count += 1
                return tree

        _Counter().visit(_empty_py_compilation_unit(), None)
        assert call_count == 1

    def test_collect_search_result_ids_finds_marker_on_py_root(self):
        """End-to-end: the original ``_collect_search_result_ids`` (which uses a
        ``TreeVisitor`` subclass) must collect a ``SearchResult`` marker placed
        on a ``Py.CompilationUnit``."""
        from rewrite.rpc.server import _collect_search_result_ids

        sr_id = random_id()
        markers = Markers(random_id(), [SearchResult(sr_id, "test")])
        cu = _empty_py_compilation_unit(markers=markers)

        ids = _collect_search_result_ids(cu)
        # ids are stored internally as int; the collector emits canonical UUID strings
        assert ids == {str(UUID(int=sr_id))}

    def test_adapt_returns_self_for_already_correct_visitor_type(self):
        """``adapt`` is a no-op when the visitor already is-a target type."""
        from rewrite.python.visitor import PythonVisitor
        v = PythonVisitor()
        assert v.adapt(None, PythonVisitor) is v

    def test_adapt_unwraps_existing_adapter(self):
        """Cross-language traversal must not stack adapters: an adapter wrapping
        a generic visitor, when re-adapted to a different visitor type, should
        wrap the original generic visitor (not the existing adapter)."""
        from rewrite.java.visitor import JavaVisitor
        from rewrite.python.visitor import PythonVisitor

        class _Bare(TreeVisitor):
            pass

        bare = _Bare()
        java_adapter = bare.adapt(None, JavaVisitor)
        # Now adapt the Java adapter to a PythonVisitor — should wrap `bare`,
        # not `java_adapter`.
        python_adapter = java_adapter.adapt(None, PythonVisitor)
        assert isinstance(python_adapter, PythonVisitor)
        assert python_adapter._wrapped is bare
