# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID

from enum import Enum, auto
from rewrite import Markers
from rewrite import Tree, SourceFile, TreeVisitor
from rewrite.utils import replace_if_changed

class J(Tree):
    @property
    def prefix(self) -> Space: ...
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def accept_java(self, v: 'JavaVisitor[P]', p: P) -> Optional['J']: ...

class JavaSourceFile(J, SourceFile):
    pass

class Expression(J):
    pass

class Statement(J):
    pass

class TypedTree(J):
    pass

class NameTree(TypedTree):
    pass

class TypeTree(NameTree):
    pass

class Loop(Statement):
    pass

class MethodCall(Expression):
    pass

class JavaType(ABC):
    pass

class Comment(ABC):
    text: str
    suffix: str
    markers: Markers

    def __init__(
        self,
        _text: str,
        _suffix: str,
        _markers: Markers,
    ) -> None: ...

    def replace(
        self,
        *,
        text: str = ...,
        suffix: str = ...,
        markers: Markers = ...,
    ) -> Self: ...

class TextComment(Comment):
    multiline: bool

    def __init__(
        self,
        _multiline: bool,
        _text: str,
        _suffix: str,
        _markers: Markers,
    ) -> None: ...

    def replace(
        self,
        *,
        multiline: bool = ...,
    ) -> Self: ...

class Space:
    EMPTY: ClassVar[Space]
    SINGLE_SPACE: ClassVar[Space]

    comments: List[Comment]
    whitespace: Optional[str]

    def __init__(
        self,
        _comments: List[Comment],
        _whitespace: Optional[str],
    ) -> None: ...

    def replace(
        self,
        *,
        comments: List[Comment] = ...,
        whitespace: Optional[str] = ...,
    ) -> Self: ...

    @classmethod
    def first_prefix(cls, trees: Optional[Iterable[J]]) -> Space: ...
    @classmethod
    def format_first_prefix(cls, trees: List[J2], prefix: Space) -> List[J2]: ...

    def is_empty(self) -> bool: ...

class JRightPadded(Generic[T]):
    element: T
    after: Space
    markers: Markers

    def __init__(
        self,
        _element: T,
        _after: Space,
        _markers: Markers,
    ) -> None: ...

    def replace(
        self,
        *,
        element: T = ...,
        after: Space = ...,
        markers: Markers = ...,
    ) -> Self: ...

    @classmethod
    def get_elements(cls, padded_list: List[JRightPadded[T]]) -> List[T]: ...
    @classmethod
    def merge_elements(cls, before: List[JRightPadded[J2]], elements: List[Union[J2, JRightPadded[J2]]]) -> List[JRightPadded[J2]]: ...

class JLeftPadded(Generic[T]):
    before: Space
    element: T
    markers: Markers

    def __init__(
        self,
        _before: Space,
        _element: T,
        _markers: Markers,
    ) -> None: ...

    def replace(
        self,
        *,
        before: Space = ...,
        element: T = ...,
        markers: Markers = ...,
    ) -> Self: ...

class JContainer(Generic[J2]):
    before: Space
    elements: List[JRightPadded[J2]]
    markers: Markers
    padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]]
    EMPTY: Optional[JContainer[J]]

    def __init__(
        self,
        _before: Space,
        _elements: List[JRightPadded[J2]],
        _markers: Markers,
        _padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]] = ...,
        _EMPTY: Optional[JContainer[J]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        before: Space = ...,
        elements: List[JRightPadded[J2]] = ...,
        markers: Markers = ...,
        padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]] = ...,
        EMPTY: Optional[JContainer[J]] = ...,
    ) -> Self: ...

    @classmethod
    def build_nullable(cls, before: Optional[JContainer[J2]], elements: Optional[List[J2]]) -> Optional[JContainer[J2]]: ...
    @classmethod
    def empty(cls) -> JContainer[J2]: ...
