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

from typing import List, Optional

from rewrite import Cursor, Marker, Markers, PrintOutputCapture

from .markers import ArrayTable, InlineTable
from .support_types import Space, Toml, TomlRightPadded
from .tree import Array, Document, Empty, Identifier, KeyValue, Literal, Table
from .visitor import TomlVisitor


def _toml_marker_wrapper(out: str) -> str:
    return "~~" + out + ("~~" if out else "") + ">"


class TomlPrinter(TomlVisitor[PrintOutputCapture]):
    """Prints a TOML LST back to source, mirroring ``org.openrewrite.toml.internal.TomlPrinter``."""

    def visit_array(self, array: Array, p: PrintOutputCapture) -> Toml:
        self._before_syntax(array, p)
        p.append("[")
        self._visit_right_padded_list(array.padding.values, ",", p)
        p.append("]")
        self._after_syntax(array, p)
        return array

    def visit_document(self, document: Document, p: PrintOutputCapture) -> Toml:
        self._before_syntax(document, p)
        for value in document.values:
            self.visit(value, p)
        self.visit_space(document.eof, p)
        self._after_syntax(document, p)
        return document

    def visit_empty(self, empty: Empty, p: PrintOutputCapture) -> Toml:
        self._before_syntax(empty, p)
        self._after_syntax(empty, p)
        return empty

    def visit_identifier(self, identifier: Identifier, p: PrintOutputCapture) -> Toml:
        self._before_syntax(identifier, p)
        p.append(identifier.source)
        self._after_syntax(identifier, p)
        return identifier

    def visit_key_value(self, key_value: KeyValue, p: PrintOutputCapture) -> Toml:
        self._before_syntax(key_value, p)
        self._visit_right_padded(key_value.padding.key, p)
        p.append("=")
        self.visit(key_value.value, p)
        self._after_syntax(key_value, p)
        return key_value

    def visit_literal(self, literal: Literal, p: PrintOutputCapture) -> Toml:
        self._before_syntax(literal, p)
        p.append(literal.source)
        self._after_syntax(literal, p)
        return literal

    def visit_space(self, space: Space, p: PrintOutputCapture) -> Space:
        p.append(space.whitespace)
        for comment in space.comments:
            self.visit_markers(comment.markers, p)
            p.append("#").append(comment.text).append(comment.suffix)
        return space

    def visit_table(self, table: Table, p: PrintOutputCapture) -> Toml:
        self._before_syntax(table, p)
        if table.markers.find_first(InlineTable) is not None:
            p.append("{")
            self._visit_right_padded_list(table.padding.values, ",", p)
            p.append("}")
        elif table.markers.find_first(ArrayTable) is not None:
            p.append("[[")
            self._visit_right_padded(table.padding.name, p)
            p.append("]]")
            self._visit_right_padded_list(table.padding.values, "", p)
        else:
            p.append("[")
            self._visit_right_padded(table.padding.name, p)
            p.append("]")
            self._visit_right_padded_list(table.padding.values, "", p)
        self._after_syntax(table, p)
        return table

    def _visit_right_padded(self, node: Optional[TomlRightPadded], p: PrintOutputCapture) -> None:
        if node is None:
            return
        self.visit(node.element, p)
        self.visit_space(node.after, p)

    def _visit_right_padded_list(self, nodes: List[TomlRightPadded], suffix_between: str,
                                 p: PrintOutputCapture) -> None:
        for i, node in enumerate(nodes):
            self._visit_right_padded(node, p)
            if i < len(nodes) - 1:
                p.append(suffix_between)

    def _before_syntax(self, t: Toml, p: PrintOutputCapture) -> None:
        markers = _printable(t.markers)
        for marker in markers:
            p.append(p.marker_printer.before_prefix(marker, Cursor(self.cursor, marker), _toml_marker_wrapper))
        self.visit_space(t.prefix, p)
        self.visit_markers(t.markers, p)
        for marker in markers:
            p.append(p.marker_printer.before_syntax(marker, Cursor(self.cursor, marker), _toml_marker_wrapper))

    def _after_syntax(self, t: Toml, p: PrintOutputCapture) -> None:
        for marker in _printable(t.markers):
            p.append(p.marker_printer.after_syntax(marker, Cursor(self.cursor, marker), _toml_marker_wrapper))


def _printable(markers: Markers) -> List[Marker]:
    # A marker of a type Python has no codec for arrives as an opaque dict (see
    # RpcReceiveQueue._do_change), which no marker printer can render.
    return [m for m in markers.markers if isinstance(m, Marker)]
