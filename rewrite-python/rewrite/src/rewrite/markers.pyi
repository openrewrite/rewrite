# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

M = TypeVar('M', bound=Marker)

from abc import ABC, abstractmethod

class Marker(ABC):
    @property
    def id(self) -> UUID: ...
    def print(self, cursor: 'Cursor', comment_wrapper: Callable[[str], str], verbose: bool) -> str: ...
    def replace(self, **kwargs: Any) -> 'Marker': ...

@dataclass(frozen=True)
class Markers:
    EMPTY: ClassVar[Markers]

    _id: UUID
    _markers: List[Marker]

    def replace(self, **kwargs: Any) -> 'Markers': ...

    @classmethod
    def build(cls, id: UUID, markers: List[Marker]) -> Markers: ...

    @property
    def id(self) -> UUID: ...
    @property
    def markers(self) -> List[Marker]: ...

    def find_first(self, cls: Type[M]) -> Optional[M]: ...
    def find_all(self, cls: Type[M]) -> List[M]: ...
    def compute_by_type(self, cls: Type[M], remap_fn: Callable[[M], Marker]) -> Markers: ...

@dataclass(frozen=True)
class SearchResult(Marker):
    _id: UUID
    _description: Optional[str]

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...
    @property
    def description(self) -> Optional[str]: ...

@dataclass(frozen=True)
class UnknownJavaMarker(Marker):
    _id: UUID
    _data: Dict[str, Any]

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...
    @property
    def data(self) -> Dict[str, Any]: ...

@dataclass(frozen=True)
class ParseExceptionResult(Marker):
    _id: UUID
    _parser_type: str
    _exception_type: str
    _message: str

    def replace(self, **kwargs: Any) -> Self: ...

    @classmethod
    def build(cls, parser: 'Parser', exception: Exception) -> ParseExceptionResult: ...

    @property
    def id(self) -> UUID: ...
    @property
    def parser_type(self) -> str: ...
    @property
    def exception_type(self) -> str: ...
    @property
    def message(self) -> str: ...
