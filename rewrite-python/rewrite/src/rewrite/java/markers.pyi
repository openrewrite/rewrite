# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID
import weakref

from rewrite import Marker
from rewrite.java.support_types import Space as Space

@dataclass(frozen=True)
class Semicolon(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...

@dataclass(frozen=True)
class TrailingComma(Marker):
    _id: UUID
    _suffix: Space

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...
    @property
    def suffix(self) -> Space: ...

@dataclass(frozen=True)
class OmitParentheses(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def id(self) -> UUID: ...
