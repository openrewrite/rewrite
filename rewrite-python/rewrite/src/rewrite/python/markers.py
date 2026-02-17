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
        BACKTICK = 4  # Python 2 repr: `x`

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
            elif self == Quoted.Style.BACKTICK:
                return "`"
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


@dataclass(frozen=True, eq=False)
class PrintSyntax(Marker):
    """Marker indicating a J.MethodInvocation represents a Python 2 print statement.

    In Python 2, print is a statement:
        print "hello"           # simple print
        print >> stderr, "err"  # print to file (has_destination=True)
        print x,                # trailing comma suppresses newline
    """
    _id: UUID
    _has_destination: bool
    _trailing_comma: bool

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'PrintSyntax':
        return self if id_ is self._id else replace(self, _id=id_)

    @property
    def has_destination(self) -> bool:
        return self._has_destination

    def with_has_destination(self, has_destination: bool) -> 'PrintSyntax':
        return self if has_destination is self._has_destination else replace(self, _has_destination=has_destination)

    @property
    def trailing_comma(self) -> bool:
        return self._trailing_comma

    def with_trailing_comma(self, trailing_comma: bool) -> 'PrintSyntax':
        return self if trailing_comma is self._trailing_comma else replace(self, _trailing_comma=trailing_comma)


@dataclass(frozen=True, eq=False)
class ExecSyntax(Marker):
    """Marker indicating a J.MethodInvocation represents a Python 2 exec statement.

    In Python 2, exec is a statement:
        exec code                    # simple form
        exec code in globals         # with globals dict
        exec code in globals, locals # with globals and locals dicts
    """
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'ExecSyntax':
        return self if id_ is self._id else replace(self, _id=id_)
