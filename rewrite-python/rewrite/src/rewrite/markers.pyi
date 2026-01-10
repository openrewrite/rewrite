# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, List, Optional, Self
from uuid import UUID


class Markers:
    id: UUID
    markers: List[Marker]

    def replace(
        self,
        *,
        id: UUID = ...,
        markers: List[Marker] = ...,
    ) -> Self: ...

class SearchResult(Marker):
    id: UUID
    description: Optional[str]

    def replace(
        self,
        *,
        id: UUID = ...,
        description: Optional[str] = ...,
    ) -> Self: ...

class ParseExceptionResult(Marker):
    id: UUID
    parser_type: str
    exception_type: str
    message: str

    def replace(
        self,
        *,
        id: UUID = ...,
        parser_type: str = ...,
        exception_type: str = ...,
        message: str = ...,
    ) -> Self: ...

class UnknownJavaMarker(Marker):
    id: UUID
    data: Dict[str, Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        data: Dict[str, Any] = ...,
    ) -> Self: ...
