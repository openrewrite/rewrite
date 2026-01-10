from __future__ import annotations

from dataclasses import dataclass, replace
from uuid import UUID

from rewrite import Marker
from rewrite.java.support_types import Space


@dataclass(frozen=True, eq=False)
class Semicolon(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> Semicolon:
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class TrailingComma(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> TrailingComma:
        return self if id_ is self._id else replace(self, _id=id_)

    _suffix: Space

    @property
    def suffix(self) -> Space:
        return self._suffix

    def with_suffix(self, suffix: Space) -> TrailingComma:
        return self if suffix is self._suffix else replace(self, _suffix=suffix)


@dataclass(frozen=True, eq=False)
class OmitParentheses(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> OmitParentheses:
        return self if id_ is self._id else replace(self, _id=id_)
