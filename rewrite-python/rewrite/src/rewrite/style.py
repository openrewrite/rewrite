from __future__ import annotations

from abc import ABC
from dataclasses import dataclass, replace
from typing import TypeVar, Type, Iterable, Optional, Set, cast
from uuid import UUID

from .markers import Marker
from .utils import random_id


class Style(ABC):
    def merge(self, lower_precedence: Style) -> Style:
        return self

    def apply_defaults(self) -> Style:
        return self


S = TypeVar('S', bound=Style)


@dataclass(frozen=True)
class NamedStyles(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> NamedStyles:
        return self if id is self._id else replace(self, _id=id)

    _name: str
    _display_name: str
    _description: Optional[str]
    _tags: Set[str]
    _styles: Iterable[Style]

    @classmethod
    def merge(cls, style_type: Type[S], named_styles: Iterable[NamedStyles]) -> Optional[S]:
        merged: Optional[S] = None
        for named_style in named_styles:
            styles = named_style._styles
            if styles is not None:
                for style in styles:
                    if isinstance(style, style_type):
                        style = style.apply_defaults()
                        if not merged:
                            merged = cast(S, style)
                        else:
                            merged = cast(S, merged.merge(style))
        return merged

    @classmethod
    def build(cls, *styles: Style, name: str = "Default", display_name: str = "Default", description: Optional[str] = None) -> NamedStyles:
        return NamedStyles(random_id(), name, display_name, description, set(), styles)


@dataclass(frozen=True)
class GeneralFormatStyle(Style):
    _use_crlf_new_lines: bool

    @property
    def use_crlf_new_lines(self) -> bool:
        return self._use_crlf_new_lines

    def with_use_crlf_new_lines(self, use_crlf_new_lines: bool) -> GeneralFormatStyle:
        return self if use_crlf_new_lines is self._use_crlf_new_lines else replace(self, _use_crlf_new_lines=use_crlf_new_lines)
