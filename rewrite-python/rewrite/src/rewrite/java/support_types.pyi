# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from enum import Enum, auto
from rewrite import Markers
from rewrite import Tree, SourceFile, TreeVisitor
from rewrite.utils import replace_if_changed

class Comment(ABC):
    _text: str
    _suffix: str
    _markers: Markers

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
    _multiline: bool

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
    _comments: List[Comment]
    _whitespace: Optional[str]

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

class JRightPadded(Generic[T]):
    _element: T
    _after: Space
    _markers: Markers

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

class JLeftPadded(Generic[T]):
    _before: Space
    _element: T
    _markers: Markers

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
    _before: Space
    _elements: List[JRightPadded[J2]]
    _markers: Markers
    _padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]]
    _EMPTY: Optional[JContainer[J]]

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
