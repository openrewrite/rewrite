# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')
T = TypeVar('T')
J2 = TypeVar('J2', bound=J)

from rewrite import TreeVisitor, Markers
from rewrite.java.tree import J
from rewrite.java import Comment

class Py(J):
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def accept_python(self, v: 'PythonVisitor[P]', p: P) -> Optional['J']: ...
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...

@dataclass(frozen=True)
class PyComment(Comment):
    _aligned_to_indent: bool

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def aligned_to_indent(self) -> bool: ...
    @property
    def multiline(self) -> bool: ...

    def with_aligned_to_indent(self, aligned_to_indent: bool) -> Comment: ...
