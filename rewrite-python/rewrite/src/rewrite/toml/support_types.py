# Copyright 2026 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Language-wide TOML LST types, mirroring ``org.openrewrite.toml.tree`` on the Java side."""

from __future__ import annotations

from abc import ABC, abstractmethod
from enum import Enum
from typing import TYPE_CHECKING, Any, ClassVar, Dict, Generic, List, Optional, TypeVar
from uuid import UUID

from rewrite import Markers, Tree, TreeVisitor
from rewrite.utils import lst_dataclass, lst_value_dataclass, replace_if_changed

if TYPE_CHECKING:
    from .visitor import TomlVisitor

P = TypeVar('P')
T = TypeVar('T')
T2 = TypeVar('T2', bound='Toml')

_TomlVisitor: Any = None


def _toml_visitor() -> Any:
    global _TomlVisitor
    if _TomlVisitor is None:
        from .visitor import TomlVisitor
        _TomlVisitor = TomlVisitor
    return _TomlVisitor


class Toml(Tree):
    __slots__ = ()

    @property
    @abstractmethod
    def prefix(self) -> Space:
        ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        return v.is_adaptable_to(_toml_visitor())

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        return self.accept_toml(v.adapt(Toml, _toml_visitor()), p)

    def accept_toml(self, v: 'TomlVisitor[P]', p: P) -> Optional[Any]:
        return v.default_value(self, p)


class TomlKey(Toml):
    __slots__ = ()


class TomlValue(Toml):
    __slots__ = ()


class TomlType(ABC):
    class Primitive(Enum):
        Boolean = 'Boolean'
        Float = 'Float'
        Integer = 'Integer'
        LocalDate = 'LocalDate'
        LocalDateTime = 'LocalDateTime'
        LocalTime = 'LocalTime'
        OffsetDateTime = 'OffsetDateTime'
        String = 'String'


@lst_value_dataclass
class Comment:
    _text: str

    @property
    def text(self) -> str:
        return self._text

    _suffix: str

    @property
    def suffix(self) -> str:
        return self._suffix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def replace(self, **kwargs) -> 'Comment':
        return replace_if_changed(self, **kwargs)


@lst_value_dataclass
class Space:
    _comments: List[Comment]

    @property
    def comments(self) -> List[Comment]:
        return self._comments

    _whitespace: Optional[str]

    @property
    def whitespace(self) -> str:
        return self._whitespace if self._whitespace is not None else ""

    def replace(self, **kwargs) -> 'Space':
        return replace_if_changed(self, **kwargs)

    def is_empty(self) -> bool:
        return len(self._comments) == 0 and not self._whitespace

    @classmethod
    def build(cls, comments: List[Comment], whitespace: Optional[str]) -> Space:
        if not comments:
            if not whitespace:
                return cls.EMPTY
            if whitespace == ' ':
                return cls.SINGLE_SPACE
        return cls(comments, whitespace)

    @property
    def indent(self) -> str:
        whitespace = self.last_whitespace
        last_newline = whitespace.rfind('\n')
        return whitespace if last_newline == -1 else whitespace[last_newline + 1:]

    @property
    def last_whitespace(self) -> str:
        if self._comments:
            return self._comments[-1].suffix
        return self.whitespace

    EMPTY: ClassVar[Space]
    SINGLE_SPACE: ClassVar[Space]


Space.EMPTY = Space([], '')
Space.SINGLE_SPACE = Space([], ' ')


@lst_dataclass
class TomlRightPadded(Generic[T]):
    _element: T

    @property
    def element(self) -> T:
        return self._element

    _after: Space

    @property
    def after(self) -> Space:
        return self._after

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def replace(self, **kwargs) -> 'TomlRightPadded[T]':
        return replace_if_changed(self, **kwargs)

    @classmethod
    def get_elements(cls, padded_list: List[TomlRightPadded[T]]) -> List[T]:
        return [x.element for x in padded_list]

    @classmethod
    def with_elements(cls, before: List[TomlRightPadded[T2]],
                      elements: List[T2]) -> List[TomlRightPadded[T2]]:
        if len(elements) == len(before) and all(b.element is e for b, e in zip(before, elements)):
            return before
        before_by_id: Dict[UUID, TomlRightPadded[T2]] = {b.element.id: b for b in before}
        after: List[TomlRightPadded[T2]] = []
        for e in elements:
            found = before_by_id.get(e.id)
            after.append(found.replace(element=e) if found is not None
                         else TomlRightPadded(e, Space.EMPTY, Markers.EMPTY))
        return after
