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

from __future__ import annotations

from typing import Optional, TypeVar

from rewrite import SourceFile
from rewrite.utils import list_map
from rewrite.visitor import TreeVisitor

from .support_types import Space, Toml, TomlRightPadded
from .tree import Array, Document, Empty, Identifier, KeyValue, Literal, Table

P = TypeVar('P')


class TomlVisitor(TreeVisitor[Toml, P]):
    """Base visitor for TOML LST nodes, mirroring ``org.openrewrite.toml.TomlVisitor``."""

    def is_acceptable(self, source_file: SourceFile, p: P) -> bool:
        return isinstance(source_file, Document)

    def visit_array(self, array: Array, p: P) -> Toml:
        array = array.replace(prefix=self.visit_space(array.prefix, p))
        array = array.replace(markers=self.visit_markers(array.markers, p))
        return array.padding.replace(values=list_map(lambda v: self.visit_right_padded(v, p), array.padding.values))

    def visit_document(self, document: Document, p: P) -> Toml:
        document = document.replace(prefix=self.visit_space(document.prefix, p))
        document = document.replace(markers=self.visit_markers(document.markers, p))
        document = document.replace(values=list_map(lambda v: self.visit(v, p), document.values))
        return document.replace(eof=self.visit_space(document.eof, p))

    def visit_empty(self, empty: Empty, p: P) -> Toml:
        empty = empty.replace(prefix=self.visit_space(empty.prefix, p))
        return empty.replace(markers=self.visit_markers(empty.markers, p))

    def visit_identifier(self, identifier: Identifier, p: P) -> Toml:
        identifier = identifier.replace(prefix=self.visit_space(identifier.prefix, p))
        return identifier.replace(markers=self.visit_markers(identifier.markers, p))

    def visit_key_value(self, key_value: KeyValue, p: P) -> Toml:
        key_value = key_value.replace(prefix=self.visit_space(key_value.prefix, p))
        key_value = key_value.replace(markers=self.visit_markers(key_value.markers, p))
        key_value = key_value.padding.replace(key=self.visit_right_padded(key_value.padding.key, p))
        return key_value.replace(value=self.visit(key_value.value, p))

    def visit_literal(self, literal: Literal, p: P) -> Toml:
        literal = literal.replace(prefix=self.visit_space(literal.prefix, p))
        return literal.replace(markers=self.visit_markers(literal.markers, p))

    def visit_table(self, table: Table, p: P) -> Toml:
        table = table.replace(prefix=self.visit_space(table.prefix, p))
        table = table.replace(markers=self.visit_markers(table.markers, p))
        table = table.padding.replace(name=self.visit_right_padded(table.padding.name, p))
        return table.padding.replace(values=list_map(lambda v: self.visit_right_padded(v, p), table.padding.values))

    def visit_space(self, space: Space, p: P) -> Space:
        return space

    def visit_right_padded(self, right: Optional[TomlRightPadded], p: P) -> Optional[TomlRightPadded]:
        if right is None:
            return None
        element = right.element
        if isinstance(element, Toml):
            element = self.visit(element, p)
            if element is None:
                return None
        after = self.visit_space(right.after, p)
        markers = self.visit_markers(right.markers, p)
        if element is right.element and after is right.after and markers is right.markers:
            return right
        return right.replace(element=element, after=after, markers=markers)


class _TreeVisitorAsTomlVisitor(TomlVisitor):
    """Adapts a generic ``TreeVisitor`` as a ``TomlVisitor``; see ``TreeVisitor.adapt``."""

    def __init__(self, wrapped: TreeVisitor):
        self._wrapped = wrapped

    @property
    def _cursor(self):
        return self._wrapped._cursor

    @_cursor.setter
    def _cursor(self, value):
        self._wrapped._cursor = value

    @property
    def _visit_count(self):
        return self._wrapped._visit_count

    @_visit_count.setter
    def _visit_count(self, value):
        self._wrapped._visit_count = value

    @property
    def _after_visit(self):
        return self._wrapped._after_visit

    @_after_visit.setter
    def _after_visit(self, value):
        self._wrapped._after_visit = value

    def pre_visit(self, tree, p):
        return self._wrapped.pre_visit(tree, p)

    def post_visit(self, tree, p):
        return self._wrapped.post_visit(tree, p)

    def default_value(self, tree, p):
        return self._wrapped.default_value(tree, p)

    def is_acceptable(self, source_file, p):
        return self._wrapped.is_acceptable(source_file, p)


TreeVisitor.register_adapter(TomlVisitor, _TreeVisitorAsTomlVisitor)
