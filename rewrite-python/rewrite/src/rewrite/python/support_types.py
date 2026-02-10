from __future__ import annotations

from dataclasses import replace, dataclass
from enum import Enum
from typing import TypeVar, Any, Optional, TYPE_CHECKING

from rewrite import TreeVisitor, Markers
from rewrite.java.tree import J
from rewrite.java import Comment

if TYPE_CHECKING:
    from .visitor import PythonVisitor

P = TypeVar('P')


class Py(J):
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from .visitor import PythonVisitor
        return self.accept_python(v.adapt(Py, PythonVisitor), p)

    def accept_python(self, v: 'PythonVisitor[P]', p: P) -> Optional['J']:
        ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from .visitor import PythonVisitor
        return v.is_adaptable_to(PythonVisitor)


T = TypeVar('T')
J2 = TypeVar('J2', bound=J)


@dataclass(frozen=True)
class PyComment(Comment):
    _aligned_to_indent: bool

    @property
    def aligned_to_indent(self) -> bool:
        return self._aligned_to_indent

    def with_aligned_to_indent(self, aligned_to_indent: bool) -> Comment:
        return self if aligned_to_indent is self._aligned_to_indent else replace(self,
                                                                                 _aligned_to_indent=aligned_to_indent)

    @property
    def multiline(self) -> bool:
        return False

    # IMPORTANT: This explicit constructor aligns the parameter order with the Java side
    def __init__(self, _text: str, _suffix: str, _aligned_to_indent: bool, _markers: Markers) -> None:
        object.__setattr__(self, '_text', _text)
        object.__setattr__(self, '_suffix', _suffix)
        object.__setattr__(self, '_aligned_to_indent', _aligned_to_indent)
        object.__setattr__(self, '_markers', _markers)
