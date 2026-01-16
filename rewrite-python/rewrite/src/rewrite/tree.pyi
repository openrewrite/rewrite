# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from pathlib import Path
from rewrite import TreeVisitor, ExecutionContext

class FileAttributes:
    creation_time: Optional[datetime]
    last_modified_time: Optional[datetime]
    last_access_time: Optional[datetime]
    is_readable: bool
    is_writable: bool
    is_executable: bool
    size: int

    def __init__(
        self,
        creation_time: Optional[datetime],
        last_modified_time: Optional[datetime],
        last_access_time: Optional[datetime],
        is_readable: bool,
        is_writable: bool,
        is_executable: bool,
        size: int,
    ) -> None: ...

    def replace(
        self,
        *,
        creation_time: Optional[datetime] = ...,
        last_modified_time: Optional[datetime] = ...,
        last_access_time: Optional[datetime] = ...,
        is_readable: bool = ...,
        is_writable: bool = ...,
        is_executable: bool = ...,
        size: int = ...,
    ) -> Self: ...

class Checksum:
    algorithm: str
    value: bytes

    def __init__(
        self,
        algorithm: str,
        value: bytes,
    ) -> None: ...

    def replace(
        self,
        *,
        algorithm: str = ...,
        value: bytes = ...,
    ) -> Self: ...
