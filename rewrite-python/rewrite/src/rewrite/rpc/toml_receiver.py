"""
TOML RPC Receiver that mirrors Java's TomlReceiver structure.

This uses the visitor pattern with pre_visit handling common fields (id, prefix, markers)
and type-specific visit methods handling only additional fields.
"""
from pathlib import Path
from typing import Any, Optional

from rewrite import Markers
from rewrite.utils import replace_if_changed
from rewrite.toml.tree import (
    Document, Array, Table, KeyValue, Literal, Identifier, Empty,
    TomlSpace, TomlRightPadded, TomlComment, TomlPrimitive
)
from rewrite.toml.markers import ArrayTable, InlineTable
from rewrite.rpc.receive_queue import RpcReceiveQueue


class TomlRpcReceiver:
    """Receiver that mirrors Java's TomlReceiver for RPC deserialization."""

    def receive(self, before: Any, q: RpcReceiveQueue) -> Any:
        """Entry point for receiving an object."""
        return q.receive(before)

    def _visit(self, tree: Any, q: RpcReceiveQueue) -> Any:
        """Visit a tree node, dispatching to appropriate visitor method."""
        if tree is None:
            return None

        tree = self._pre_visit(tree, q)

        if isinstance(tree, Document):
            return self._visit_document(tree, q)
        elif isinstance(tree, Array):
            return self._visit_array(tree, q)
        elif isinstance(tree, Table):
            return self._visit_table(tree, q)
        elif isinstance(tree, KeyValue):
            return self._visit_key_value(tree, q)
        elif isinstance(tree, Literal):
            return self._visit_literal(tree, q)
        elif isinstance(tree, Identifier):
            return self._visit_identifier(tree, q)
        elif isinstance(tree, Empty):
            return self._visit_empty(tree, q)

        return tree

    def _pre_visit(self, tree: Any, q: RpcReceiveQueue) -> Any:
        """Handle common fields: id, prefix, markers."""
        new_id = q.receive(tree.id)
        new_prefix = q.receive(tree.prefix)
        new_markers = q.receive_markers(tree.markers)

        changes = {}
        if new_id is not tree.id:
            changes['_id'] = new_id
        if new_prefix is not tree.prefix:
            changes['_prefix'] = new_prefix
        if new_markers is not tree.markers:
            changes['_markers'] = new_markers
        return replace_if_changed(tree, **changes) if changes else tree

    def _visit_document(self, doc: Document, q: RpcReceiveQueue) -> Document:
        source_path = q.receive(str(doc.source_path))
        charset_name = q.receive(doc.charset_name)
        charset_bom_marked = q.receive(doc.charset_bom_marked)
        checksum = q.receive(doc.checksum)
        file_attributes = q.receive(doc.file_attributes)
        values = q.receive_list(doc.values)
        eof = q.receive(doc.eof)

        return replace_if_changed(
            doc,
            source_path=Path(source_path) if source_path else doc.source_path,
            charset_name=charset_name,
            charset_bom_marked=charset_bom_marked,
            checksum=checksum,
            file_attributes=file_attributes,
            values=values,
            eof=eof
        )

    def _visit_array(self, array: Array, q: RpcReceiveQueue) -> Array:
        values = q.receive_list(array.values)
        return replace_if_changed(array, values=values)

    def _visit_table(self, table: Table, q: RpcReceiveQueue) -> Table:
        name = q.receive(table.name)
        values = q.receive_list(table.values)
        return replace_if_changed(table, name=name, values=values)

    def _visit_key_value(self, kv: KeyValue, q: RpcReceiveQueue) -> KeyValue:
        key = q.receive(kv.key)
        value = q.receive(kv.value)
        return replace_if_changed(kv, key=key, value=value)

    def _visit_literal(self, literal: Literal, q: RpcReceiveQueue) -> Literal:
        type_ = q.receive(literal.type)
        source = q.receive(literal.source)
        value = q.receive(literal.value)
        return replace_if_changed(literal, type=type_, source=source, value=value)

    def _visit_identifier(self, ident: Identifier, q: RpcReceiveQueue) -> Identifier:
        source = q.receive(ident.source)
        name = q.receive(ident.name)
        return replace_if_changed(ident, source=source, name=name)

    def _visit_empty(self, empty: Empty, q: RpcReceiveQueue) -> Empty:
        return empty

    def _receive_space(self, space: TomlSpace, q: RpcReceiveQueue) -> TomlSpace:
        """Receive a TomlSpace object."""
        if space is None:
            return TomlSpace.EMPTY

        comments = q.receive_list_defined(space.comments)
        whitespace = q.receive(space.whitespace)

        if comments is space.comments and whitespace is space.whitespace:
            return space

        return space.replace(comments=comments, whitespace=whitespace)

    def _receive_comment(self, comment: TomlComment, q: RpcReceiveQueue) -> TomlComment:
        """Receive a TOML Comment object."""
        if comment is None:
            text = q.receive_defined(None)
            suffix = q.receive_defined(None)
            markers = q.receive_markers(None)
            return TomlComment(text or '', suffix or '', markers or Markers.EMPTY)

        text = q.receive_defined(comment.text)
        suffix = q.receive_defined(comment.suffix)
        markers = q.receive_markers(comment.markers)
        return TomlComment(text, suffix, markers)

    def _receive_right_padded(self, rp: TomlRightPadded, q: RpcReceiveQueue) -> Optional[TomlRightPadded]:
        """Receive a TomlRightPadded wrapper."""
        if rp is None:
            return None

        element = q.receive(rp.element)
        after = q.receive_defined(rp.after)
        markers = q.receive_markers(rp.markers)

        if element is rp.element and after is rp.after and markers is rp.markers:
            return rp

        return rp.replace(element=element, after=after, markers=markers)


# ============================================================================
# Codec registration - registers all TOML AST, support, and marker types
# ============================================================================

_toml_receiver = None
_toml_sender = None


def _get_receiver():
    global _toml_receiver
    if _toml_receiver is None:
        _toml_receiver = TomlRpcReceiver()
    return _toml_receiver


def _get_sender():
    global _toml_sender
    if _toml_sender is None:
        from rewrite.rpc.toml_sender import TomlRpcSender
        _toml_sender = TomlRpcSender()
    return _toml_sender


def _receive_toml(tree, q: RpcReceiveQueue):
    """Codec for receiving TOML tree nodes."""
    return _get_receiver()._visit(tree, q)


def _receive_toml_space(space, q: RpcReceiveQueue):
    """Codec for receiving TomlSpace objects."""
    return _get_receiver()._receive_space(space, q)


def _receive_toml_right_padded(rp, q: RpcReceiveQueue):
    """Codec for receiving TomlRightPadded objects."""
    return _get_receiver()._receive_right_padded(rp, q)


def _receive_toml_comment(comment, q: RpcReceiveQueue):
    """Codec for receiving TOML Comment objects."""
    return _get_receiver()._receive_comment(comment, q)


def _receive_array_table(marker: ArrayTable, q: RpcReceiveQueue) -> ArrayTable:
    """Codec for receiving ArrayTable marker."""
    new_id = q.receive_defined(marker.id)
    if new_id is marker.id:
        return marker
    return marker.replace(id=new_id)


def _receive_inline_table(marker: InlineTable, q: RpcReceiveQueue) -> InlineTable:
    """Codec for receiving InlineTable marker."""
    new_id = q.receive_defined(marker.id)
    if new_id is marker.id:
        return marker
    return marker.replace(id=new_id)


def _send_array_table(marker, q):
    """Codec for sending ArrayTable marker."""
    q.get_and_send(marker, lambda x: x.id)


def _send_inline_table(marker, q):
    """Codec for sending InlineTable marker."""
    q.get_and_send(marker, lambda x: x.id)


def _register_toml_tree_codecs():
    """Register codecs for all TOML AST types."""
    from rewrite.rpc.receive_queue import register_codec_with_both_names, make_dataclass_factory

    # TOML tree types
    for cls, java_name in [
        (Document, 'org.openrewrite.toml.tree.Toml$Document'),
        (Array, 'org.openrewrite.toml.tree.Toml$Array'),
        (Table, 'org.openrewrite.toml.tree.Toml$Table'),
        (KeyValue, 'org.openrewrite.toml.tree.Toml$KeyValue'),
        (Literal, 'org.openrewrite.toml.tree.Toml$Literal'),
        (Identifier, 'org.openrewrite.toml.tree.Toml$Identifier'),
        (Empty, 'org.openrewrite.toml.tree.Toml$Empty'),
    ]:
        register_codec_with_both_names(java_name, cls, _receive_toml, make_dataclass_factory(cls))


def _register_toml_support_type_codecs():
    """Register codecs for TOML support types."""
    from rewrite.rpc.receive_queue import register_codec_with_both_names

    # TomlSpace
    register_codec_with_both_names(
        'org.openrewrite.toml.tree.Space',
        TomlSpace,
        _receive_toml_space,
        lambda: TomlSpace.EMPTY
    )

    # TomlRightPadded
    register_codec_with_both_names(
        'org.openrewrite.toml.tree.TomlRightPadded',
        TomlRightPadded,
        _receive_toml_right_padded,
        lambda: TomlRightPadded(None, TomlSpace.EMPTY, Markers.EMPTY)
    )

    # TomlComment
    register_codec_with_both_names(
        'org.openrewrite.toml.tree.Comment',
        TomlComment,
        _receive_toml_comment,
        lambda: TomlComment('', '', Markers.EMPTY)
    )


def _register_toml_marker_codecs():
    """Register codecs for TOML marker types."""
    from uuid import uuid4
    from rewrite.rpc.receive_queue import register_codec_with_both_names

    register_codec_with_both_names(
        'org.openrewrite.toml.marker.ArrayTable',
        ArrayTable,
        _receive_array_table,
        lambda: ArrayTable(uuid4()),
        sender=_send_array_table
    )
    register_codec_with_both_names(
        'org.openrewrite.toml.marker.InlineTable',
        InlineTable,
        _receive_inline_table,
        lambda: InlineTable(uuid4()),
        sender=_send_inline_table
    )


# Register all TOML codecs on module import
_register_toml_tree_codecs()
_register_toml_support_type_codecs()
_register_toml_marker_codecs()
