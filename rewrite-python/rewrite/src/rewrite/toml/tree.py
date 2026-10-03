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

"""The TOML LST, mirroring ``org.openrewrite.toml.tree`` on the Java side."""

from __future__ import annotations

import weakref
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any, List, Optional, TypeVar
from uuid import UUID

from rewrite import Checksum, Cursor, FileAttributes, Markers, SourceFile, TreeVisitor
from rewrite.toml.support_types import Space, Toml, TomlKey, TomlRightPadded, TomlType, TomlValue
from rewrite.utils import lst_dataclass, replace_if_changed

if TYPE_CHECKING:
    from .visitor import TomlVisitor

P = TypeVar('P')


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Array(Toml):
    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _values: List[TomlRightPadded[Toml]]

    @property
    def values(self) -> List[Toml]:
        return TomlRightPadded.get_elements(self._values)

    def with_values(self, values: List[Toml]) -> Array:
        return self.padding.replace(values=TomlRightPadded.with_elements(self._values, values))

    @dataclass
    class PaddingHelper:
        _t: Array

        @property
        def values(self) -> List[TomlRightPadded[Toml]]:
            return self._t._values

        def replace(self, **kwargs) -> Array:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        p = self._padding() if self._padding is not None else None
        # noinspection PyProtectedMember
        if p is None or p._t is not self:
            p = Array.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_array(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Document(Toml, SourceFile):
    _id: UUID

    _source_path: Path

    @property
    def source_path(self) -> Path:
        return self._source_path

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _charset_name: Optional[str]

    @property
    def charset_name(self) -> Optional[str]:
        return self._charset_name

    _charset_bom_marked: bool

    @property
    def charset_bom_marked(self) -> bool:
        return self._charset_bom_marked

    _checksum: Optional[Checksum]

    @property
    def checksum(self) -> Optional[Checksum]:
        return self._checksum

    _file_attributes: Optional[FileAttributes]

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes

    _values: List[TomlValue]

    @property
    def values(self) -> List[TomlValue]:
        return self._values

    _eof: Space

    @property
    def eof(self) -> Space:
        return self._eof

    def printer(self, cursor: Cursor) -> TreeVisitor[Any, Any]:
        from .printer import TomlPrinter
        return TomlPrinter()

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_document(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Empty(Toml):
    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_empty(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Identifier(TomlKey):
    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _source: str

    @property
    def source(self) -> str:
        return self._source

    _name: str

    @property
    def name(self) -> str:
        return self._name

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_identifier(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class KeyValue(TomlValue):
    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _key: TomlRightPadded[TomlKey]

    @property
    def key(self) -> TomlKey:
        return self._key.element

    def with_key(self, key: TomlKey) -> KeyValue:
        return self.padding.replace(key=self._key.replace(element=key))

    _value: Toml

    @property
    def value(self) -> Toml:
        return self._value

    @dataclass
    class PaddingHelper:
        _t: KeyValue

        @property
        def key(self) -> TomlRightPadded[TomlKey]:
            return self._t._key

        def replace(self, **kwargs) -> KeyValue:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        p = self._padding() if self._padding is not None else None
        # noinspection PyProtectedMember
        if p is None or p._t is not self:
            p = KeyValue.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_key_value(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Literal(Toml):
    """A TOML scalar.

    ``value`` is a ``str``, ``bool``, ``int`` or ``float``, or for the date/time types a
    ``datetime.date`` (LocalDate), naive ``datetime.datetime`` (LocalDateTime),
    ``datetime.time`` (LocalTime) or timezone-aware ``datetime.datetime`` (OffsetDateTime).
    """

    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _type: TomlType.Primitive

    @property
    def type(self) -> TomlType.Primitive:
        return self._type

    _source: str

    @property
    def source(self) -> str:
        return self._source

    _value: Any

    @property
    def value(self) -> Any:
        return self._value

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_literal(self, p)


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@lst_dataclass
class Table(TomlValue):
    """A ``[table]``, an ``[[array.of.tables]]`` (``ArrayTable`` marker) or an
    inline ``{ table }`` (``InlineTable`` marker)."""

    _id: UUID

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    _name: Optional[TomlRightPadded[Identifier]]

    @property
    def name(self) -> Optional[Identifier]:
        return self._name.element if self._name is not None else None

    _values: List[TomlRightPadded[Toml]]

    @property
    def values(self) -> List[Toml]:
        return TomlRightPadded.get_elements(self._values)

    def with_values(self, values: List[Toml]) -> Table:
        return self.padding.replace(values=TomlRightPadded.with_elements(self._values, values))

    @dataclass
    class PaddingHelper:
        _t: Table

        @property
        def name(self) -> Optional[TomlRightPadded[Identifier]]:
            return self._t._name

        @property
        def values(self) -> List[TomlRightPadded[Toml]]:
            return self._t._values

        def replace(self, **kwargs) -> Table:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        p = self._padding() if self._padding is not None else None
        # noinspection PyProtectedMember
        if p is None or p._t is not self:
            p = Table.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml:
        return v.visit_table(self, p)
