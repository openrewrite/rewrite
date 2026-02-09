from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import Optional, Any, TypeVar, List, TYPE_CHECKING, ClassVar
from uuid import UUID

from rewrite.markers import Markers
from rewrite.tree import Tree, SourceFile, Checksum, FileAttributes
from rewrite.utils import replace_if_changed

if TYPE_CHECKING:
    from rewrite import TreeVisitor

P = TypeVar('P')


@dataclass(frozen=True)
class TomlComment:
    _text: str
    _suffix: str
    _markers: Markers

    @property
    def text(self) -> str:
        return self._text

    @property
    def suffix(self) -> str:
        return self._suffix

    @property
    def markers(self) -> Markers:
        return self._markers

    def replace(self, **kwargs) -> TomlComment:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True)
class TomlSpace:
    _comments: List[TomlComment]
    _whitespace: Optional[str]

    EMPTY: ClassVar[TomlSpace]
    SINGLE_SPACE: ClassVar[TomlSpace]

    @property
    def comments(self) -> List[TomlComment]:
        return self._comments

    @property
    def whitespace(self) -> str:
        return self._whitespace if self._whitespace is not None else ''

    def replace(self, **kwargs) -> TomlSpace:
        return replace_if_changed(self, **kwargs)


TomlSpace.EMPTY = TomlSpace([], None)
TomlSpace.SINGLE_SPACE = TomlSpace([], ' ')


@dataclass(frozen=True)
class TomlRightPadded:
    _element: Any
    _after: TomlSpace
    _markers: Markers

    @property
    def element(self) -> Any:
        return self._element

    @property
    def after(self) -> TomlSpace:
        return self._after

    @property
    def markers(self) -> Markers:
        return self._markers

    def replace(self, **kwargs) -> TomlRightPadded:
        return replace_if_changed(self, **kwargs)


class TomlPrimitive(Enum):
    Boolean = 'Boolean'
    Float = 'Float'
    Integer = 'Integer'
    LocalDate = 'LocalDate'
    LocalDateTime = 'LocalDateTime'
    LocalTime = 'LocalTime'
    OffsetDateTime = 'OffsetDateTime'
    String = 'String'


# Marker mixin interfaces
class TomlValue:
    pass


class TomlKey:
    pass


@dataclass(frozen=True, eq=False)
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

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def source_path(self) -> Path:
        return self._source_path

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def charset_name(self) -> Optional[str]:
        return self._charset_name

    @property
    def charset_bom_marked(self) -> bool:
        return self._charset_bom_marked

    @property
    def checksum(self) -> Optional[Checksum]:
        return self._checksum

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes

    @property
    def values(self) -> List:
        return self._values

    @property
    def eof(self) -> TomlSpace:
        return self._eof

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_document(self, p)

    def replace(self, **kwargs) -> Document:
        return replace_if_changed(self, **kwargs)

    def printer(self, cursor):
        raise NotImplementedError("TOML printer not available in Python")


@dataclass(frozen=True, eq=False)
class Array(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _values: List[TomlRightPadded]

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def values(self) -> List[TomlRightPadded]:
        return self._values

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_array(self, p)

    def replace(self, **kwargs) -> Array:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class Table(Tree, TomlValue):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _name: Optional[TomlRightPadded]
    _values: List[TomlRightPadded]

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def name(self) -> Optional[TomlRightPadded]:
        return self._name

    @property
    def values(self) -> List[TomlRightPadded]:
        return self._values

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_table(self, p)

    def replace(self, **kwargs) -> Table:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class KeyValue(Tree, TomlValue):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _key: TomlRightPadded
    _value: Any

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def key(self) -> TomlRightPadded:
        return self._key

    @property
    def value(self) -> Any:
        return self._value

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_key_value(self, p)

    def replace(self, **kwargs) -> KeyValue:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class Literal(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _type: TomlPrimitive
    _source: str
    _value: Any

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def type(self) -> TomlPrimitive:
        return self._type

    @property
    def source(self) -> str:
        return self._source

    @property
    def value(self) -> Any:
        return self._value

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_literal(self, p)

    def replace(self, **kwargs) -> Literal:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class Identifier(Tree, TomlKey):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers
    _source: str
    _name: str

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    @property
    def source(self) -> str:
        return self._source

    @property
    def name(self) -> str:
        return self._name

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_identifier(self, p)

    def replace(self, **kwargs) -> Identifier:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class Empty(Tree):
    _id: UUID
    _prefix: TomlSpace
    _markers: Markers

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def prefix(self) -> TomlSpace:
        return self._prefix

    @property
    def markers(self) -> Markers:
        return self._markers

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from rewrite.toml.visitor import TomlVisitor
        return v.is_adaptable_to(TomlVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from rewrite.toml.visitor import TomlVisitor
        return v.adapt(Tree, TomlVisitor).visit_empty(self, p)

    def replace(self, **kwargs) -> Empty:
        return replace_if_changed(self, **kwargs)
