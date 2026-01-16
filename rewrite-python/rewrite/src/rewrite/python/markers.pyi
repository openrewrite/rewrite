# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID

from enum import Enum
from rewrite import Marker

class KeywordArguments(Marker):
    id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...

    def with_id(self, id_: UUID) -> 'KeywordArguments': ...

class KeywordOnlyArguments(Marker):
    id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...

    def with_id(self, id_: UUID) -> 'KeywordOnlyArguments': ...

class Quoted(Marker):
    id: UUID
    style: Style

    def __init__(
        self,
        _id: UUID,
        _style: Style,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        style: Style = ...,
    ) -> Self: ...

    def with_id(self, id_: UUID) -> Quoted: ...
    def with_style(self, style: Style) -> Quoted: ...

class SuppressNewline(Marker):
    id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...

    def with_id(self, id_: UUID) -> 'SuppressNewline': ...
