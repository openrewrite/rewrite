# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')

from enum import Enum
from pathlib import Path
from rewrite.markers import Markers
from rewrite.tree import Tree, SourceFile, Checksum, FileAttributes
from rewrite.utils import replace_if_changed
from rewrite import TreeVisitor

@dataclass(frozen=True)
class TomlComment:
    _text: str
    _suffix: str
    _markers: Markers

    def replace(self, **kwargs: Any) -> TomlComment: ...

    @property
    def text(self) -> str: ...
    @property
    def suffix(self) -> str: ...
    @property
    def markers(self) -> Markers: ...

@dataclass(frozen=True)
class TomlSpace:
    EMPTY: ClassVar[TomlSpace]
    SINGLE_SPACE: ClassVar[TomlSpace]

    _comments: List[TomlComment]
    _whitespace: Optional[str]

    def replace(self, **kwargs: Any) -> TomlSpace: ...

    @property
    def comments(self) -> List[TomlComment]: ...
    @property
    def whitespace(self) -> str: ...

@dataclass(frozen=True)
class TomlRightPadded:
    _element: Any
    _after: TomlSpace
    _markers: Markers

    def replace(self, **kwargs: Any) -> TomlRightPadded: ...

    @property
    def element(self) -> Any: ...
    @property
    def after(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...

class TomlPrimitive(Enum):
    Boolean: TomlPrimitive
    Float: TomlPrimitive
    Integer: TomlPrimitive
    LocalDate: TomlPrimitive
    LocalDateTime: TomlPrimitive
    LocalTime: TomlPrimitive
    OffsetDateTime: TomlPrimitive
    String: TomlPrimitive

class TomlValue:
    pass

class TomlKey:
    pass

@dataclass(frozen=True)
class Document(SourceFile, TomlValue):
    _id: UUID
    _source_path: Path
    _prefix: TomlSpace
    _markers: Markers
    _charset_name: Optional[str]
    _charset_bom_marked: bool
    _checksum: Optional[Checksum]
    _file_attributes: Optional[FileAttributes]
    _values: List
    _eof: TomlSpace

    def replace(self, **kwargs: Any) -> Document: ...

    @property
    def id(self) -> UUID: ...
    @property
    def source_path(self) -> Path: ...
    @property
    def prefix(self) -> TomlSpace: ...
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
    def values(self) -> List: ...
    @property
    def eof(self) -> TomlSpace: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class Array(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _values: List[TomlRightPadded]

    def replace(self, **kwargs: Any) -> Array: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def values(self) -> List[TomlRightPadded]: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class Table(Tree, TomlValue):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _name: Optional[TomlRightPadded]
    _values: List[TomlRightPadded]

    def replace(self, **kwargs: Any) -> Table: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def name(self) -> Optional[TomlRightPadded]: ...
    @property
    def values(self) -> List[TomlRightPadded]: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class KeyValue(Tree, TomlValue):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _key: TomlRightPadded
    _value: Any

    def replace(self, **kwargs: Any) -> KeyValue: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def key(self) -> TomlRightPadded: ...
    @property
    def value(self) -> Any: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class Literal(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _type: TomlPrimitive
    _source: str
    _value: Any

    def replace(self, **kwargs: Any) -> Literal: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def type(self) -> TomlPrimitive: ...
    @property
    def source(self) -> str: ...
    @property
    def value(self) -> Any: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class Identifier(Tree, TomlKey):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _source: str
    _name: str

    def replace(self, **kwargs: Any) -> Identifier: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def source(self) -> str: ...
    @property
    def name(self) -> str: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...

@dataclass(frozen=True)
class Empty(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers

    def replace(self, **kwargs: Any) -> Empty: ...

    @property
    def id(self) -> UUID: ...
    @property
    def prefix(self) -> TomlSpace: ...
    @property
    def markers(self) -> Markers: ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
