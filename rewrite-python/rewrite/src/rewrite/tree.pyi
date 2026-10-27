# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, Callable, Type, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')
S = TypeVar('S', bound=Style)

from abc import ABC, abstractmethod
from datetime import datetime
from pathlib import Path
from .markers import Markers as Markers
from .style import NamedStyles as NamedStyles, Style as Style
from .utils import id_to_int as id_to_int, replace_if_changed as replace_if_changed
from rewrite import TreeVisitor, ExecutionContext
from .markers import Marker
from .parser import ParserInput
from .visitor import Cursor

class Tree(ABC):
    @property
    def id(self) -> UUID: ...
    @property
    @abstractmethod
    def markers(self) -> Markers: ...
    @abstractmethod
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def print(self, cursor: 'Cursor', capture: 'PrintOutputCapture[P]') -> str: ...
    def printer(self, cursor: 'Cursor') -> 'TreeVisitor[Any, PrintOutputCapture[P]]': ...
    def is_scope(self, tree: Optional[Tree]) -> bool: ...
    def replace(self, **kwargs: Any) -> Self: ...

class PrinterFactory(ABC):
    @classmethod
    def current(cls) -> Optional[PrinterFactory]: ...
    @abstractmethod
    def create_printer(self, cursor: Cursor) -> TreeVisitor[Any, PrintOutputCapture[P]]: ...
    def set_current(self) -> Any: ...

class SourceFile(Tree):
    @property
    @abstractmethod
    def charset_name(self) -> Optional[str]: ...
    @property
    @abstractmethod
    def source_path(self) -> Path: ...
    @property
    @abstractmethod
    def file_attributes(self) -> Optional[FileAttributes]: ...
    def print_all(self) -> str: ...
    def print_equals_input(self, input: 'ParserInput', ctx: ExecutionContext) -> bool: ...
    def get_style(self, style: Type[S]) -> Optional[S]: ...

class PrintOutputCapture(Generic[P]):
    @dataclass
    class MarkerPrinter(ABC):
        DEFAULT: ClassVar[Optional['PrintOutputCapture.MarkerPrinter']]
        SEARCH_MARKERS_ONLY: ClassVar[Optional['PrintOutputCapture.MarkerPrinter']]
        VERBOSE: ClassVar[Optional['PrintOutputCapture.MarkerPrinter']]
        FENCED: ClassVar[Optional['PrintOutputCapture.MarkerPrinter']]
        SANITIZED: ClassVar[Optional['PrintOutputCapture.MarkerPrinter']]


        def before_syntax(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str: ...
        def before_prefix(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str: ...
        def after_syntax(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str: ...

    @property
    def marker_printer(self) -> MarkerPrinter: ...
    def get_out(self) -> str: ...
    def append(self, text: Optional[str]=...) -> 'PrintOutputCapture[P]': ...
    def append_char(self, c: str) -> 'PrintOutputCapture[P]': ...
    def clone(self) -> 'PrintOutputCapture[P]': ...
    def get_marker_printer(self) -> MarkerPrinter: ...

@dataclass(frozen=True)
class FileAttributes:
    creation_time: Optional[datetime]
    last_modified_time: Optional[datetime]
    last_access_time: Optional[datetime]
    is_readable: bool
    is_writable: bool
    is_executable: bool
    size: int


    @staticmethod
    def from_path(path: Path) -> Optional[FileAttributes]: ...

@dataclass(frozen=True)
class Checksum:
    algorithm: str
    value: bytes

