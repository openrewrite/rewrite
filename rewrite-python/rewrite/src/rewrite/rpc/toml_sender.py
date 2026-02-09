"""
TOML RPC Sender that mirrors Java's TomlSender structure.

This uses the visitor pattern with pre_visit handling common fields (id, prefix, markers)
and type-specific visit methods handling only additional fields.
"""
from typing import Any

from rewrite import Markers
from rewrite.toml.tree import (
    Document, Array, Table, KeyValue, Literal, Identifier, Empty,
    TomlSpace, TomlRightPadded, TomlComment
)


class TomlRpcSender:
    """Sender that mirrors Java's TomlSender for RPC serialization."""

    def send(self, after: Any, before: Any, q: 'RpcSendQueue') -> None:
        """Entry point for sending an object."""
        from rewrite.rpc.send_queue import RpcObjectState
        from rewrite.rpc.receive_queue import get_java_type_name

        if before is after:
            q.put({'state': RpcObjectState.NO_CHANGE})
            return

        if after is None:
            q.put({'state': RpcObjectState.DELETE})
            return

        if before is None:
            value_type = get_java_type_name(type(after)) if hasattr(after, '__class__') else None
            q.put({'state': RpcObjectState.ADD, 'valueType': value_type})
            q._before = None
            self._visit(after, q)
        else:
            value_type = get_java_type_name(type(after)) if hasattr(after, '__class__') else None
            q.put({'state': RpcObjectState.CHANGE, 'valueType': value_type})
            q._before = before
            self._visit(after, q)

    def _visit(self, tree: Any, q: 'RpcSendQueue') -> None:
        """Visit a tree node, dispatching to appropriate visitor method."""
        if tree is None:
            return

        self._pre_visit(tree, q)

        if isinstance(tree, Document):
            self._visit_document(tree, q)
        elif isinstance(tree, Array):
            self._visit_array(tree, q)
        elif isinstance(tree, Table):
            self._visit_table(tree, q)
        elif isinstance(tree, KeyValue):
            self._visit_key_value(tree, q)
        elif isinstance(tree, Literal):
            self._visit_literal(tree, q)
        elif isinstance(tree, Identifier):
            self._visit_identifier(tree, q)
        elif isinstance(tree, Empty):
            self._visit_empty(tree, q)

    def _pre_visit(self, tree: Any, q: 'RpcSendQueue') -> None:
        """Handle common fields: id, prefix, markers."""
        q.get_and_send(tree, lambda x: x.id)
        q.get_and_send(tree, lambda x: x.prefix, lambda space: self._visit_space(space, q))
        q.get_and_send(tree, lambda x: x.markers, lambda markers: self._visit_markers(markers, q))

    def _visit_document(self, doc: Document, q: 'RpcSendQueue') -> None:
        q.get_and_send(doc, lambda x: str(x.source_path))
        q.get_and_send(doc, lambda x: x.charset_name)
        q.get_and_send(doc, lambda x: x.charset_bom_marked)
        q.get_and_send(doc, lambda x: x.checksum)
        q.get_and_send(doc, lambda x: x.file_attributes)
        q.get_and_send_list(doc, lambda x: x.values,
                           lambda v: v.id,
                           lambda v: self._visit(v, q))
        q.get_and_send(doc, lambda x: x.eof, lambda space: self._visit_space(space, q))

    def _visit_array(self, array: Array, q: 'RpcSendQueue') -> None:
        q.get_and_send_list(array, lambda x: x.values,
                           lambda j: j.element.id,
                           lambda j: self._visit_right_padded(j, q))

    def _visit_table(self, table: Table, q: 'RpcSendQueue') -> None:
        q.get_and_send(table, lambda x: x.name, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send_list(table, lambda x: x.values,
                           lambda j: j.element.id,
                           lambda j: self._visit_right_padded(j, q))

    def _visit_key_value(self, kv: KeyValue, q: 'RpcSendQueue') -> None:
        q.get_and_send(kv, lambda x: x.key, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send(kv, lambda x: x.value, lambda j: self._visit(j, q))

    def _visit_literal(self, literal: Literal, q: 'RpcSendQueue') -> None:
        q.get_and_send(literal, lambda x: x.type)
        q.get_and_send(literal, lambda x: x.source)
        q.get_and_send(literal, lambda x: x.value)

    def _visit_identifier(self, identifier: Identifier, q: 'RpcSendQueue') -> None:
        q.get_and_send(identifier, lambda x: x.source)
        q.get_and_send(identifier, lambda x: x.name)

    def _visit_empty(self, empty: Empty, q: 'RpcSendQueue') -> None:
        # No additional fields beyond id/prefix/markers
        pass

    def _visit_space(self, space: TomlSpace, q: 'RpcSendQueue') -> None:
        """Visit a TomlSpace object."""
        if space is None:
            return
        q.get_and_send_list(space, lambda x: x.comments,
                           lambda c: c.text + c.suffix,
                           lambda c: self._visit_comment(c, q))
        q.get_and_send(space, lambda x: x.whitespace)

    def _visit_comment(self, comment: TomlComment, q: 'RpcSendQueue') -> None:
        """Visit a TOML Comment object."""
        q.get_and_send(comment, lambda x: x.text)
        q.get_and_send(comment, lambda x: x.suffix)
        q.get_and_send(comment, lambda x: x.markers)

    def _visit_right_padded(self, rp: TomlRightPadded, q: 'RpcSendQueue') -> None:
        """Visit a TomlRightPadded wrapper."""
        if rp is None:
            return
        q.get_and_send(rp, lambda x: x.element, lambda el: self._visit(el, q))
        q.get_and_send(rp, lambda x: x.after, lambda space: self._visit_space(space, q))
        q.get_and_send(rp, lambda x: x.markers)

    def _visit_markers(self, markers: Markers, q: 'RpcSendQueue') -> None:
        """Visit a Markers object."""
        if markers is None:
            return
        q.get_and_send(markers, lambda x: x.id)
        q.get_and_send_list(markers, lambda x: x.markers,
                           lambda m: m.id,
                           None)
