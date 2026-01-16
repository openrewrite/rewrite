# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from enum import Enum
from rewrite import Marker

class KeywordArguments(Marker):
    _id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...

class KeywordOnlyArguments(Marker):
    _id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...

class Quoted(Marker):
    _id: UUID
    _style: Style

    def __init__(
        self,
        _id: UUID,
        _style: Style,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        style: Style = ...,
    ) -> Self: ...

class SuppressNewline(Marker):
    _id: UUID

    def __init__(
        self,
        _id: UUID,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
    ) -> Self: ...
