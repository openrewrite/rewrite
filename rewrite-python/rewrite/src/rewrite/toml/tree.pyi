# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')

from pathlib import Path
from rewrite import Checksum, Cursor, FileAttributes, Markers, SourceFile, TreeVisitor
from rewrite.toml.support_types import Space as Space, Toml as Toml, TomlKey as TomlKey, TomlRightPadded as TomlRightPadded, TomlType as TomlType, TomlValue as TomlValue
from rewrite.utils import replace_if_changed
from .visitor import TomlVisitor

@dataclass(frozen=True)
class Array(Toml):
    @dataclass
    class PaddingHelper:
        _t: Array

        def replace(self, **kwargs: Any) -> Array: ...

        @property
        def values(self) -> List[TomlRightPadded[Toml]]: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _values: List[TomlRightPadded[Toml]]
    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def values(self) -> List[Toml]: ...
    @property
    def padding(self) -> PaddingHelper: ...

    def with_values(self, values: List[Toml]) -> Array: ...
    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class Document(Toml, SourceFile):
    _id: UUID
    _source_path: Path
    _prefix: Space
    _markers: Markers
    _charset_name: Optional[str]
    _charset_bom_marked: bool
    _checksum: Optional[Checksum]
    _file_attributes: Optional[FileAttributes]
    _values: List[TomlValue]
    _eof: Space


    @property
    def source_path(self) -> Path: ...
    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def charset_name(self) -> Optional[str]: ...
    @property
    def charset_bom_marked(self) -> bool: ...
    @property
    def checksum(self) -> Optional[Checksum]: ...
    @property
    def file_attributes(self) -> Optional[FileAttributes]: ...
    @property
    def values(self) -> List[TomlValue]: ...
    @property
    def eof(self) -> Space: ...

    def printer(self, cursor: Cursor) -> TreeVisitor[Any, Any]: ...
    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class Empty(Toml):
    _id: UUID
    _prefix: Space
    _markers: Markers


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class Identifier(TomlKey):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _source: str
    _name: str


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def source(self) -> str: ...
    @property
    def name(self) -> str: ...

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class KeyValue(TomlValue):
    @dataclass
    class PaddingHelper:
        _t: KeyValue

        def replace(self, **kwargs: Any) -> KeyValue: ...

        @property
        def key(self) -> TomlRightPadded[TomlKey]: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _key: TomlRightPadded[TomlKey]
    _value: Toml
    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def key(self) -> TomlKey: ...
    @property
    def value(self) -> Toml: ...
    @property
    def padding(self) -> PaddingHelper: ...

    def with_key(self, key: TomlKey) -> KeyValue: ...
    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class Literal(Toml):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type: TomlType.Primitive
    _source: str
    _value: Any


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def type(self) -> TomlType.Primitive: ...
    @property
    def source(self) -> str: ...
    @property
    def value(self) -> Any: ...

    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...

@dataclass(frozen=True)
class Table(TomlValue):
    @dataclass
    class PaddingHelper:
        _t: Table

        def replace(self, **kwargs: Any) -> Table: ...

        @property
        def name(self) -> Optional[TomlRightPadded[Identifier]]: ...
        @property
        def values(self) -> List[TomlRightPadded[Toml]]: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _name: Optional[TomlRightPadded[Identifier]]
    _values: List[TomlRightPadded[Toml]]
    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...


    @property
    def prefix(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def name(self) -> Optional[Identifier]: ...
    @property
    def values(self) -> List[Toml]: ...
    @property
    def padding(self) -> PaddingHelper: ...

    def with_values(self, values: List[Toml]) -> Table: ...
    def accept_toml(self, v: TomlVisitor[P], p: P) -> Toml: ...
