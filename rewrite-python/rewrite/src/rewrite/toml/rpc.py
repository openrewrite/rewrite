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

"""RPC codecs for the TOML LST, mirroring ``org.openrewrite.toml.internal.rpc``.

The field order on the wire is that of ``TomlSender``/``TomlReceiver`` on the Java side.
"""

from __future__ import annotations

import math
import sys
from datetime import date, datetime, time
from pathlib import Path
from typing import TYPE_CHECKING, Any, Optional

from rewrite.utils import id_to_str

from .markers import ArrayTable, InlineTable
from .support_types import Comment, Space, Toml, TomlRightPadded, TomlType
from .tree import Array, Document, Empty, Identifier, KeyValue, Literal, Table

if TYPE_CHECKING:
    from rewrite.rpc.receive_queue import RpcReceiveQueue
    from rewrite.rpc.send_queue import RpcSendQueue

TOML_SOURCE_FILE_TYPE = 'org.openrewrite.toml.tree.Toml$Document'

_UNCHANGED = object()


def encode_value(value: Any) -> Any:
    """JSON has no literal for a non-finite float or a date/time, so those travel as their
    TOML spelling and ISO-8601 form respectively, to be decoded against the literal's type."""
    if isinstance(value, float):
        if math.isnan(value):
            return "nan"
        if math.isinf(value):
            return "inf" if value > 0 else "-inf"
    elif isinstance(value, (date, time)):
        # Interned so that an unchanged value is the same object on every send, and so NO_CHANGE.
        return sys.intern(value.isoformat())
    return value


def decode_value(type_: TomlType.Primitive, raw: Any) -> Any:
    if type_ == TomlType.Primitive.Integer:
        return int(raw)
    if type_ == TomlType.Primitive.Float:
        return float(raw)
    if type_ == TomlType.Primitive.LocalDate:
        return date.fromisoformat(raw)
    if type_ in (TomlType.Primitive.LocalDateTime, TomlType.Primitive.OffsetDateTime):
        return datetime.fromisoformat(raw)
    if type_ == TomlType.Primitive.LocalTime:
        return time.fromisoformat(raw)
    return raw


class TomlRpcReceiver:

    def visit(self, tree: Optional[Toml], q: RpcReceiveQueue) -> Optional[Toml]:
        if tree is None:
            return None
        tree = self._pre_visit(tree, q)
        if isinstance(tree, Document):
            return self._visit_document(tree, q)
        if isinstance(tree, Array):
            values = q.receive_list(tree.padding.values, lambda rp: self.receive_right_padded(rp, q))
            return q.apply(tree, _values=values)
        if isinstance(tree, Empty):
            return tree
        if isinstance(tree, Identifier):
            return q.apply(tree, _source=q.receive(tree.source), _name=q.receive(tree.name))
        if isinstance(tree, KeyValue):
            return q.apply(
                tree,
                _key=q.receive(tree.padding.key, lambda rp: self.receive_right_padded(rp, q)),
                _value=q.receive(tree.value, lambda v: self.visit(v, q)),
            )
        if isinstance(tree, Literal):
            return self._visit_literal(tree, q)
        if isinstance(tree, Table):
            return q.apply(
                tree,
                _name=q.receive(tree.padding.name, lambda rp: self.receive_right_padded(rp, q)),
                _values=q.receive_list(tree.padding.values, lambda rp: self.receive_right_padded(rp, q)),
            )
        raise ValueError(f"Unknown TOML tree type: {type(tree)}")

    def _pre_visit(self, t: Toml, q: RpcReceiveQueue) -> Toml:
        return q.apply(
            t,
            _id=q.receive(t._id),  # ty: ignore[unresolved-attribute]  # _id on concrete subclasses
            _prefix=q.receive(t.prefix, lambda s: self.receive_space(s, q)),
            _markers=q.receive_markers(t.markers),
        )

    def _visit_document(self, d: Document, q: RpcReceiveQueue) -> Document:
        before_path = str(d.source_path) if d.source_path is not None else None
        source_path = q.receive(before_path)
        return q.apply(
            d,
            _source_path=Path(source_path) if source_path != before_path else d.source_path,
            _charset_name=q.receive(d.charset_name),
            _charset_bom_marked=q.receive(d.charset_bom_marked),
            _checksum=q.receive(d.checksum),
            _file_attributes=q.receive(d.file_attributes),
            _values=q.receive_list(d.values, lambda v: self.visit(v, q)),
            _eof=q.receive(d.eof, lambda s: self.receive_space(s, q)),
        )

    def _visit_literal(self, literal: Literal, q: RpcReceiveQueue) -> Literal:
        type_name = q.receive(literal.type.name if literal.type is not None else None)
        type_ = TomlType.Primitive[type_name]
        source = q.receive(literal.source)
        raw = q.receive(_UNCHANGED)
        value = literal.value if raw is _UNCHANGED else decode_value(type_, raw)
        return q.apply(literal, _type=type_, _source=source, _value=value)

    def receive_space(self, space: Space, q: RpcReceiveQueue) -> Space:
        comments = q.receive_list(space.comments, lambda c: self._receive_comment(c, q))
        whitespace = q.receive(space.whitespace)
        if comments is space.comments and whitespace is space.whitespace:
            return space
        return Space.build(comments or [], whitespace)

    def _receive_comment(self, comment: Comment, q: RpcReceiveQueue) -> Comment:
        return Comment(
            q.receive_defined(comment.text),
            q.receive_defined(comment.suffix),
            q.receive_markers(comment.markers),
        )

    def receive_right_padded(self, rp: TomlRightPadded, q: RpcReceiveQueue) -> TomlRightPadded:
        return q.apply(
            rp,
            _element=q.receive(rp.element, lambda e: self.visit(e, q)),
            _after=q.receive(rp.after, lambda s: self.receive_space(s, q)),
            _markers=q.receive_markers(rp.markers),
        )


class TomlRpcSender:

    def visit(self, tree: Toml, q: RpcSendQueue) -> None:
        self._pre_visit(tree, q)
        if isinstance(tree, Document):
            self._visit_document(tree, q)
        elif isinstance(tree, Array):
            q.get_and_send_list(tree, lambda x: x.padding.values,
                                lambda rp: id_to_str(rp.element._id),
                                lambda rp: self.send_right_padded(rp, q))
        elif isinstance(tree, Identifier):
            q.get_and_send(tree, lambda x: x.source)
            q.get_and_send(tree, lambda x: x.name)
        elif isinstance(tree, KeyValue):
            q.get_and_send(tree, lambda x: x.padding.key, lambda rp: self.send_right_padded(rp, q))
            q.get_and_send(tree, lambda x: x.value, lambda v: self.visit(v, q))
        elif isinstance(tree, Literal):
            q.get_and_send(tree, lambda x: x.type.name)
            q.get_and_send(tree, lambda x: x.source)
            q.get_and_send(tree, lambda x: encode_value(x.value))
        elif isinstance(tree, Table):
            q.get_and_send(tree, lambda x: x.padding.name, lambda rp: self.send_right_padded(rp, q))
            q.get_and_send_list(tree, lambda x: x.padding.values,
                                lambda rp: id_to_str(rp.element._id),
                                lambda rp: self.send_right_padded(rp, q))

    def _pre_visit(self, t: Toml, q: RpcSendQueue) -> None:
        q.get_and_send(t, lambda x: id_to_str(x._id))  # ty: ignore[unresolved-attribute]  # _id on concrete subclasses
        q.get_and_send(t, lambda x: x.prefix, lambda s: self.send_space(s, q))
        q.get_and_send_as_ref(t, lambda x: x.markers, lambda m: _send_markers(m, q))

    def _visit_document(self, d: Document, q: RpcSendQueue) -> None:
        q.get_and_send(d, lambda x: str(x.source_path))
        q.get_and_send(d, lambda x: x.charset_name)
        q.get_and_send(d, lambda x: x.charset_bom_marked)
        q.get_and_send(d, lambda x: x.checksum)
        q.get_and_send(d, lambda x: x.file_attributes)
        q.get_and_send_list(d, lambda x: x.values, lambda v: id_to_str(v._id), lambda v: self.visit(v, q))
        q.get_and_send(d, lambda x: x.eof, lambda s: self.send_space(s, q))

    def send_space(self, space: Space, q: RpcSendQueue) -> None:
        q.get_and_send_list(space, lambda x: x.comments, lambda c: c.text + c.suffix,
                            lambda c: self._send_comment(c, q))
        q.get_and_send(space, lambda x: x.whitespace)

    def _send_comment(self, comment: Comment, q: RpcSendQueue) -> None:
        q.get_and_send(comment, lambda x: x.text)
        q.get_and_send(comment, lambda x: x.suffix)
        q.get_and_send_as_ref(comment, lambda x: x.markers, lambda m: _send_markers(m, q))

    def send_right_padded(self, rp: TomlRightPadded, q: RpcSendQueue) -> None:
        q.get_and_send(rp, lambda x: x.element, lambda e: self.visit(e, q))
        q.get_and_send(rp, lambda x: x.after, lambda s: self.send_space(s, q))
        q.get_and_send_as_ref(rp, lambda x: x.markers, lambda m: _send_markers(m, q))


def _send_markers(markers, q: RpcSendQueue) -> None:
    from rewrite.rpc.python_receiver import _get_sender
    _get_sender()._visit_markers(markers, q)


_receiver = TomlRpcReceiver()
_sender = TomlRpcSender()


def send_toml(tree: Toml, q: RpcSendQueue) -> None:
    _sender.visit(tree, q)


def register_toml_codecs() -> None:
    from rewrite.rpc.receive_queue import make_dataclass_factory, register_codec_with_both_names

    def register(java_type: str, cls: type, codec, sender=None) -> None:
        # Scoped to TOML documents: several of these class names (Space, Comment, Literal,
        # Identifier, KeyValue, ...) are also LST classes of other languages.
        register_codec_with_both_names(java_type, cls, codec, make_dataclass_factory(cls), sender,
                                       source_file_type=TOML_SOURCE_FILE_TYPE)

    for cls in (Array, Document, Empty, Identifier, KeyValue, Literal, Table):
        register(f'org.openrewrite.toml.tree.Toml${cls.__name__}', cls,
                 lambda t, q: _receiver.visit(t, q), lambda t, q: _sender.visit(t, q))
    register('org.openrewrite.toml.tree.Space', Space,
             lambda s, q: _receiver.receive_space(s, q), lambda s, q: _sender.send_space(s, q))
    register('org.openrewrite.toml.tree.Comment', Comment,
             lambda c, q: _receiver._receive_comment(c, q), lambda c, q: _sender._send_comment(c, q))
    register('org.openrewrite.toml.tree.TomlRightPadded', TomlRightPadded,
             lambda rp, q: _receiver.receive_right_padded(rp, q), lambda rp, q: _sender.send_right_padded(rp, q))

    for marker in (ArrayTable, InlineTable):
        register(f'org.openrewrite.toml.marker.{marker.__name__}', marker,
                 lambda m, q: q.apply(m, _id=q.receive(id_to_str(m._id) if m._id is not None else None)),
                 lambda m, q: q.get_and_send(m, lambda x: id_to_str(x._id)))
