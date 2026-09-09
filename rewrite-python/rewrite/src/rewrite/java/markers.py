from __future__ import annotations

from dataclasses import dataclass, replace
from rewrite.utils import lst_dataclass
from uuid import UUID

from rewrite import Marker
from rewrite.java.support_types import Space


@lst_dataclass
class Semicolon(Marker):
    _id: UUID


@lst_dataclass
class TrailingComma(Marker):
    _id: UUID

    _suffix: Space

    @property
    def suffix(self) -> Space:
        return self._suffix


@lst_dataclass
class OmitParentheses(Marker):
    _id: UUID

