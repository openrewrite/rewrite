# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID

from pathlib import Path
from rewrite import TreeVisitor, ExecutionContext

class Tree(ABC):
    @property
    def id(self) -> UUID: ...
    @property
    def markers(self) -> Markers: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def print(self, cursor: 'Cursor', capture: 'PrintOutputCapture[P]') -> str: ...
    def printer(self, cursor: 'Cursor') -> 'TreeVisitor[Any, PrintOutputCapture[P]]': ...
    def is_scope(self, tree: Optional[Tree]) -> bool: ...

class PrinterFactory(ABC):
    pass

class SourceFile(Tree):
    @property
    def charset_name(self) -> Optional[str]: ...
    @property
    def source_path(self) -> Path: ...
    @property
    def file_attributes(self) -> Optional[FileAttributes]: ...
    def print_all(self) -> str: ...
    def print_equals_input(self, input: 'ParserInput', ctx: ExecutionContext) -> bool: ...
    def get_style(self, style: Type[S]) -> Optional[S]: ...

class PrintOutputCapture(Generic[P]):
    @property
    def marker_printer(self) -> MarkerPrinter: ...
    def get_out(self) -> str: ...
    def append(self, text: Optional[str]) -> 'PrintOutputCapture[P]': ...
    def append_char(self, c: str) -> 'PrintOutputCapture[P]': ...
    def clone(self) -> 'PrintOutputCapture[P]': ...
    def get_marker_printer(self) -> MarkerPrinter: ...

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
