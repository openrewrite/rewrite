# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import List, Optional, Self

from rewrite.markers import Markers
from rewrite.tree import Tree


class Comment(ABC):
    text: str
    suffix: str
    markers: Markers

    def replace(
        self,
        *,
        text: str = ...,
        suffix: str = ...,
        markers: Markers = ...,
    ) -> Self: ...

class TextComment(Comment):
    _multiline: bool

    def replace(
        self,
        *,
        _multiline: bool = ...,
    ) -> Self: ...

class Space:
    comments: List[Comment]
    whitespace: Optional[str]

    def replace(
        self,
        *,
        comments: List[Comment] = ...,
        whitespace: Optional[str] = ...,
    ) -> Self: ...

class JRightPadded(Generic[T]):
    element: T
    after: Space
    markers: Markers

    def replace(
        self,
        *,
        element: T = ...,
        after: Space = ...,
        markers: Markers = ...,
    ) -> Self: ...

class JLeftPadded(Generic[T]):
    before: Space
    element: T
    markers: Markers

    def replace(
        self,
        *,
        before: Space = ...,
        element: T = ...,
        markers: Markers = ...,
    ) -> Self: ...

class JContainer(Generic[T]):
    before: Space
    elements: List[JRightPadded[T]]
    markers: Markers

    def replace(
        self,
        *,
        before: Space = ...,
        elements: List[JRightPadded[T]] = ...,
        markers: Markers = ...,
    ) -> Self: ...
