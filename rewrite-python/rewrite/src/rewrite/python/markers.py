from __future__ import annotations

from dataclasses import dataclass, replace
from enum import Enum
from uuid import UUID

from rewrite import Marker


@dataclass(frozen=True, eq=False)
class KeywordArguments(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'KeywordArguments':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class KeywordOnlyArguments(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'KeywordOnlyArguments':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class Quoted(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> Quoted:
        return self if id_ is self._id else replace(self, _id=id_)

    _style: Style

    @property
    def style(self) -> Style:
        return self._style

    def with_style(self, style: Style) -> Quoted:
        return self if style is self._id else replace(self, _style=style)

    class Style(Enum):
        SINGLE = 0
        DOUBLE = 1
        TRIPLE_SINGLE = 2
        TRIPLE_DOUBLE = 3

        @property
        def quote(self) -> str:
            """Return the quote character(s) for this style."""
            if self == Quoted.Style.SINGLE:
                return "'"
            elif self == Quoted.Style.DOUBLE:
                return '"'
            elif self == Quoted.Style.TRIPLE_SINGLE:
                return "'''"
            elif self == Quoted.Style.TRIPLE_DOUBLE:
                return '"""'
            return ""


@dataclass(frozen=True, eq=False)
class SuppressNewline(Marker):
    """Marker to suppress trailing newline in compilation units."""
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'SuppressNewline':
        return self if id_ is self._id else replace(self, _id=id_)
