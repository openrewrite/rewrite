from __future__ import annotations

import traceback
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List, ClassVar, cast, TYPE_CHECKING, Callable, TypeVar, Type, Optional, Dict, Any
from uuid import UUID

if TYPE_CHECKING:
    from .parser import Parser
    from .visitor import Cursor

from .utils import random_id, list_map, replace_if_changed


class Marker(ABC):
    @property
    @abstractmethod
    def id(self) -> UUID:
        ...

    def replace(self, **kwargs) -> 'Marker':
        """Replace fields on this marker, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)

    def print(self, cursor: 'Cursor', comment_wrapper: Callable[[str], str], verbose: bool) -> str:
        return ''

    def __eq__(self, other: object) -> bool:
        if self.__class__ == other.__class__:
            return self.id == cast(Marker, other).id
        return False

    def __hash__(self) -> int:
        return hash(self.id)


M = TypeVar('M', bound=Marker)


@dataclass(frozen=True, eq=False)
class Markers:
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    _markers: List[Marker]

    @property
    def markers(self) -> List[Marker]:
        return self._markers

    def replace(self, **kwargs) -> 'Markers':
        """Replace fields on this Markers instance, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)

    def find_first(self, cls: Type[M]) -> Optional[M]:
        for marker in self.markers:
            if isinstance(marker, cls):
                return marker
        return None

    def find_all(self, cls: Type[M]) -> List[M]:
        return [m for m in self.markers if isinstance(m, cls)]

    def compute_by_type(self, cls: Type[M], remap_fn: Callable[[M], Marker]) -> Markers:
        """
        Replace all markers of the given type with the result of the function.

        :param cls: type of the markers to remap
        :param remap_fn: function to remap the marker
        :return: new Markers instance with the updated markers, or the same instance if no markers were updated
        """
        return self.replace(markers=list_map(lambda m: remap_fn(m) if isinstance(m, cls) else m, self.markers))

    EMPTY: ClassVar[Markers]

    def __eq__(self, other: object) -> bool:
        if self.__class__ == other.__class__:
            return self.id == cast(Markers, other).id
        return False

    def __hash__(self) -> int:
        return hash(self.id)

    @classmethod
    def build(cls, id: UUID, markers: List[Marker]) -> Markers:
        return Markers(id, markers)


Markers.EMPTY = Markers(random_id(), [])


@dataclass(frozen=True, eq=False)
class SearchResult(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    _description: Optional[str]

    @property
    def description(self) -> Optional[str]:
        return self._description


@dataclass(frozen=True, eq=False)
class UnknownJavaMarker(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    _data: Dict[str, Any]

    @property
    def data(self) -> Dict[str, Any]:
        return self._data


@dataclass(frozen=True, eq=False)
class ParseExceptionResult(Marker):
    @classmethod
    def build(cls, parser: 'Parser', exception: Exception) -> ParseExceptionResult:
        exc_type, exc_value, exc_tb = type(exception), exception, exception.__traceback__
        return cls(random_id(), type(parser).__name__, exc_type.__name__,
                   ''.join(traceback.format_exception(exc_type, exc_value, exc_tb)))

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    _parser_type: str

    @property
    def parser_type(self) -> str:
        return self._parser_type

    _exception_type: str

    @property
    def exception_type(self) -> str:
        return self._exception_type

    _message: str

    @property
    def message(self) -> str:
        return self._message
