# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID


class Marker(ABC):
    @property
    def id(self) -> UUID: ...
    def print(self, cursor: 'Cursor', comment_wrapper: Callable[[str], str], verbose: bool) -> str: ...

class Markers:
    EMPTY: ClassVar[Markers]

    id: UUID
    markers: List[Marker]

    def __init__(
        self,
        _id: UUID,
        _markers: List[Marker],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        markers: List[Marker] = ...,
    ) -> Self: ...

    @classmethod
    def build(cls, id: UUID, markers: List[Marker]) -> Markers: ...

    def find_first(self, cls: Type[M]) -> Optional[M]: ...
    def find_all(self, cls: Type[M]) -> List[M]: ...
    def compute_by_type(self, cls: Type[M], remap_fn: Callable[[M], Marker]) -> Markers: ...

class SearchResult(Marker):
    id: UUID
    description: Optional[str]

    def __init__(
        self,
        _id: UUID,
        _description: Optional[str],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        description: Optional[str] = ...,
    ) -> Self: ...

class UnknownJavaMarker(Marker):
    id: UUID
    data: Dict[str, Any]

    def __init__(
        self,
        _id: UUID,
        _data: Dict[str, Any],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        data: Dict[str, Any] = ...,
    ) -> Self: ...

class ParseExceptionResult(Marker):
    id: UUID
    parser_type: str
    exception_type: str
    message: str

    def __init__(
        self,
        _id: UUID,
        _parser_type: str,
        _exception_type: str,
        _message: str,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        parser_type: str = ...,
        exception_type: str = ...,
        message: str = ...,
    ) -> Self: ...

    @classmethod
    def build(cls, parser: 'Parser', exception: Exception) -> ParseExceptionResult: ...
