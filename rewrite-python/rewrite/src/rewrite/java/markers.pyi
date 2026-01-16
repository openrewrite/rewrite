# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from rewrite import Marker
from rewrite.java.support_types import Space

class Semicolon(Marker):
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

class TrailingComma(Marker):
    _id: UUID
    _suffix: Space

    def __init__(
        self,
        _id: UUID,
        _suffix: Space,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        suffix: Space = ...,
    ) -> Self: ...

class OmitParentheses(Marker):
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
