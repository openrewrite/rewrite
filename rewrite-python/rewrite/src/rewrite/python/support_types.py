from __future__ import annotations

from typing import TypeVar, Any, Optional, TYPE_CHECKING

from rewrite import TreeVisitor
from rewrite.java.tree import J

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

