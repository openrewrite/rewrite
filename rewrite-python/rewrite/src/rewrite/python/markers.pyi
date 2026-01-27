# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID
import weakref

from enum import Enum
from rewrite import Marker

@dataclass(frozen=True)
class KeywordArguments(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...

    def with_id(self, id_: UUID) -> 'KeywordArguments': ...

@dataclass(frozen=True)
class KeywordOnlyArguments(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...

    def with_id(self, id_: UUID) -> 'KeywordOnlyArguments': ...

@dataclass(frozen=True)
class Quoted(Marker):
    class Style(Enum):
        SINGLE: Style
        DOUBLE: Style
        TRIPLE_SINGLE: Style
        TRIPLE_DOUBLE: Style

    _id: UUID
    _style: Style

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...
    @property
    def style(self) -> Style: ...

    def with_id(self, id_: UUID) -> Quoted: ...
    def with_style(self, style: Style) -> Quoted: ...

@dataclass(frozen=True)
class SuppressNewline(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...

    def with_id(self, id_: UUID) -> 'SuppressNewline': ...
