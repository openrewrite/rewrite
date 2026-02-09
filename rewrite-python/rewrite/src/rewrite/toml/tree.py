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


class TomlComment:
    __slots__ = ('_text', '_suffix', '_markers')

    def __init__(self, text: str, suffix: str, markers: Markers):
        self._text = text
        self._suffix = suffix
        self._markers = markers

    @property
    def text(self) -> str:
        return self._text

    @property
    def suffix(self) -> str:
        return self._suffix

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_text(self, text: str) -> TomlComment:
        if text is self._text:
            return self
        return TomlComment(text, self._suffix, self._markers)

    def with_suffix(self, suffix: str) -> TomlComment:
        if suffix is self._suffix:
            return self
        return TomlComment(self._text, suffix, self._markers)

    def with_markers(self, markers: Markers) -> TomlComment:
        if markers is self._markers:
            return self
        return TomlComment(self._text, self._suffix, markers)


class TomlSpace:
    __slots__ = ('_comments', '_whitespace')

    EMPTY: ClassVar[TomlSpace]
    SINGLE_SPACE: ClassVar[TomlSpace]

    def __init__(self, comments: List[TomlComment], whitespace: Optional[str]):
        self._comments = comments
        self._whitespace = whitespace

    @property
    def comments(self) -> List[TomlComment]:
        return self._comments

    @property
    def whitespace(self) -> str:
        return self._whitespace if self._whitespace is not None else ''

    def with_comments(self, comments: List[TomlComment]) -> TomlSpace:
        if comments is self._comments:
            return self
        return TomlSpace(comments, self._whitespace)

    def with_whitespace(self, whitespace: str) -> TomlSpace:
        if whitespace == self.whitespace:
            return self
        return TomlSpace(self._comments, whitespace if whitespace else None)

    def replace(self, **kwargs) -> TomlSpace:
        comments = kwargs.get('comments', kwargs.get('_comments', self._comments))
        whitespace = kwargs.get('whitespace', kwargs.get('_whitespace', self._whitespace))
        if comments is self._comments and whitespace is self._whitespace:
            return self
        return TomlSpace(comments, whitespace)


TomlSpace.EMPTY = TomlSpace([], None)
TomlSpace.SINGLE_SPACE = TomlSpace([], ' ')


class TomlRightPadded:
    __slots__ = ('_element', '_after', '_markers')

    def __init__(self, element: Any, after: TomlSpace, markers: Markers):
        self._element = element
        self._after = after
        self._markers = markers

    @property
    def element(self) -> Any:
        return self._element

    @property
    def after(self) -> TomlSpace:
        return self._after

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_element(self, element: Any) -> TomlRightPadded:
        if element is self._element:
            return self
        return TomlRightPadded(element, self._after, self._markers)

    def with_after(self, after: TomlSpace) -> TomlRightPadded:
        if after is self._after:
            return self
        return TomlRightPadded(self._element, after, self._markers)

    def with_markers(self, markers: Markers) -> TomlRightPadded:
        if markers is self._markers:
            return self
        return TomlRightPadded(self._element, self._after, markers)

    def replace(self, **kwargs) -> TomlRightPadded:
        element = kwargs.get('element', kwargs.get('_element', self._element))
        after = kwargs.get('after', kwargs.get('_after', self._after))
        markers = kwargs.get('markers', kwargs.get('_markers', self._markers))
        if element is self._element and after is self._after and markers is self._markers:
            return self
        return TomlRightPadded(element, after, markers)


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
