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

    def print(self, cursor: 'Cursor', comment_wrapper: Callable[[str], str], verbose: bool) -> str:
        desc = self._description or ""
        return comment_wrapper(f"({desc})" if desc else "")


class Markup(Marker, ABC):
    """
    Base class for markup markers that provide visual indicators (warnings, errors, info, debug).

    Markup markers are used to annotate code with messages that can be displayed
    in various ways depending on the tooling.
    """

    @property
    @abstractmethod
    def message(self) -> str:
        """The primary message to display."""
        ...

    @property
    @abstractmethod
    def detail(self) -> Optional[str]:
        """Additional detail, shown in verbose mode."""
        ...

    def print(self, cursor: 'Cursor', comment_wrapper: Callable[[str], str], verbose: bool) -> str:
        if verbose and self.detail:
            return comment_wrapper(f"({self.detail})")
        return comment_wrapper(f"({self.message})")

    @staticmethod
    def warn(message: str, detail: Optional[str] = None) -> 'MarkupWarn':
        """Create a warning markup marker."""
        return MarkupWarn(random_id(), message, detail)

    @staticmethod
    def error(message: str, detail: Optional[str] = None) -> 'MarkupError':
        """Create an error markup marker."""
        return MarkupError(random_id(), message, detail)

    @staticmethod
    def info(message: str, detail: Optional[str] = None) -> 'MarkupInfo':
        """Create an info markup marker."""
        return MarkupInfo(random_id(), message, detail)

    @staticmethod
    def debug(message: str, detail: Optional[str] = None) -> 'MarkupDebug':
        """Create a debug markup marker."""
        return MarkupDebug(random_id(), message, detail)


@dataclass(frozen=True, eq=False)
class MarkupWarn(Markup):
    """Warning markup marker for deprecations and other warnings."""
    _id: UUID
    _message: str
    _detail: Optional[str] = None

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def message(self) -> str:
        return self._message

    @property
    def detail(self) -> Optional[str]:
        return self._detail


@dataclass(frozen=True, eq=False)
class MarkupError(Markup):
    """Error markup marker for errors and issues."""
    _id: UUID
    _message: str
    _detail: Optional[str] = None

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def message(self) -> str:
        return self._message

    @property
    def detail(self) -> Optional[str]:
        return self._detail


@dataclass(frozen=True, eq=False)
class MarkupInfo(Markup):
    """Info markup marker for informational messages."""
    _id: UUID
    _message: str
    _detail: Optional[str] = None

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def message(self) -> str:
        return self._message

    @property
    def detail(self) -> Optional[str]:
        return self._detail


@dataclass(frozen=True, eq=False)
class MarkupDebug(Markup):
    """Debug markup marker for debugging information."""
    _id: UUID
    _message: str
    _detail: Optional[str] = None

    @property
    def id(self) -> UUID:
        return self._id

    @property
    def message(self) -> str:
        return self._message

    @property
    def detail(self) -> Optional[str]:
        return self._detail


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

    _tree_type: Optional[str] = None

    @property
    def tree_type(self) -> Optional[str]:
        return self._tree_type
