from __future__ import annotations

from dataclasses import dataclass, replace
from uuid import UUID

from rewrite import Marker
from rewrite.java.support_types import Space


@dataclass(frozen=True, eq=False, slots=True)
class Semicolon(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


@dataclass(frozen=True, eq=False, slots=True)
class TrailingComma(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    _suffix: Space

    @property
    def suffix(self) -> Space:
        return self._suffix


@dataclass(frozen=True, eq=False, slots=True)
class OmitParentheses(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id
