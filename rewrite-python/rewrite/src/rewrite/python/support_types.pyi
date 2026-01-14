# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Self

from rewrite.java.support_types import J, Comment
from rewrite.markers import Marker


class PyComment(Comment):
    aligned_to_indent: bool

    def replace(
        self,
        *,
        aligned_to_indent: bool = ...,
    ) -> Self: ...

class KeywordArguments(Marker):
    id: uuid.UUID

    def replace(
        self,
        *,
        id: uuid.UUID = ...,
    ) -> Self: ...

class KeywordOnlyArguments(Marker):
    id: uuid.UUID

    def replace(
        self,
        *,
        id: uuid.UUID = ...,
    ) -> Self: ...

class Quoted(Marker):
    id: uuid.UUID
    style: Style

    def replace(
        self,
        *,
        id: uuid.UUID = ...,
        style: Style = ...,
    ) -> Self: ...
