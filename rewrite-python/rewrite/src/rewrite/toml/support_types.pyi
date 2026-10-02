# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, Dict, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')
T = TypeVar('T')
T2 = TypeVar('T2', bound='Toml')

from abc import ABC, abstractmethod
from enum import Enum
from rewrite import Markers, Tree, TreeVisitor
from rewrite.utils import replace_if_changed

class Toml(Tree):
    @property
    def prefix(self) -> Space: ...
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def accept_toml(self, v: 'TomlVisitor[P]', p: P) -> Optional[Any]: ...

class TomlKey(Toml):
    pass

class TomlValue(Toml):
    pass

class TomlType(ABC):
    class Primitive(Enum):
        Boolean: Primitive
        Float: Primitive
        Integer: Primitive
        LocalDate: Primitive
        LocalDateTime: Primitive
        LocalTime: Primitive
        OffsetDateTime: Primitive
        String: Primitive


@dataclass(frozen=True)
class Comment:
    _text: str
    _suffix: str
    _markers: Markers

    def replace(self, **kwargs: Any) -> 'Comment': ...

    @property
    def text(self) -> str: ...
    @property
    def suffix(self) -> str: ...
    @property
    def markers(self) -> Markers: ...

@dataclass(frozen=True)
class Space:
    EMPTY: ClassVar[Space]
    SINGLE_SPACE: ClassVar[Space]

    _comments: List[Comment]
    _whitespace: Optional[str]

    def replace(self, **kwargs: Any) -> 'Space': ...

    @classmethod
    def build(cls, comments: List[Comment], whitespace: Optional[str]) -> Space: ...

    @property
    def comments(self) -> List[Comment]: ...
    @property
    def whitespace(self) -> str: ...
    @property
    def indent(self) -> str: ...
    @property
    def last_whitespace(self) -> str: ...

    def is_empty(self) -> bool: ...

@dataclass(frozen=True)
class TomlRightPadded(Generic[T]):
    _element: T
    _after: Space
    _markers: Markers

    def replace(self, **kwargs: Any) -> 'TomlRightPadded[T]': ...

    @classmethod
    def get_elements(cls, padded_list: List[TomlRightPadded[T]]) -> List[T]: ...
    @classmethod
    def with_elements(cls, before: List[TomlRightPadded[T2]], elements: List[T2]) -> List[TomlRightPadded[T2]]: ...

    @property
    def element(self) -> T: ...
    @property
    def after(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...
