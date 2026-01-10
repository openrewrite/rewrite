from __future__ import annotations

import os
import threading
from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional, Any, TypeVar, cast, TYPE_CHECKING, Generic, ClassVar, Callable, Type
from uuid import UUID

from .markers import Markers
from .style import NamedStyles, Style

if TYPE_CHECKING:
    from rewrite import TreeVisitor, ExecutionContext
    from .markers import Marker
    from .parser import ParserInput
    from .visitor import Cursor

P = TypeVar('P')


class Tree(ABC):
    @property
    @abstractmethod
    def id(self) -> UUID:
        ...

    @abstractmethod
    def with_id(self, id: UUID) -> Tree:
        ...

    @property
    @abstractmethod
    def markers(self) -> Markers:
        ...

    @abstractmethod
    def with_markers(self, markers: Markers) -> Tree:
        ...

    @abstractmethod
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        ...

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        return v.default_value(self, p)

    def print(self, cursor: 'Cursor', capture: 'PrintOutputCapture[P]') -> str:
        self.printer(cursor).visit(self, capture, cursor)
        return capture.get_out()

    def printer(self, cursor: 'Cursor') -> 'TreeVisitor[Any, PrintOutputCapture[P]]':
        return cursor.first_enclosing_or_throw(SourceFile).printer(cursor)

    def is_scope(self, tree: Optional[Tree]) -> bool:
        return tree and tree.id == self.id

    def __eq__(self, other: object) -> bool:
        if self.__class__ == other.__class__:
            return self.id == cast(Tree, other).id
        return False

    def __hash__(self) -> int:
        return hash(self.id)


class PrinterFactory(ABC):
    _thread_local = threading.local()

    @classmethod
    def current(cls) -> Optional[PrinterFactory]:
        return getattr(PrinterFactory._thread_local, 'context', None)

    def set_current(self):
        PrinterFactory._thread_local.context = self

    @abstractmethod
    def create_printer(self, cursor: Cursor) -> TreeVisitor[Any, PrintOutputCapture[P]]:
        ...


S = TypeVar('S', bound=Style)

class SourceFile(Tree):
    @property
    @abstractmethod
    def charset_name(self) -> Optional[str]:
        ...

    @property
    @abstractmethod
    def source_path(self) -> Path:
        ...

    @abstractmethod
    def with_source_path(self, source_path: Path) -> SourceFile:
        ...

    @property
    @abstractmethod
    def file_attributes(self) -> Optional[FileAttributes]:
        ...

    @abstractmethod
    def with_file_attributes(self, file_attributes: Optional[FileAttributes]) -> SourceFile:
        ...

    def print_all(self) -> str:
        from .visitor import Cursor
        return self.print(Cursor(None, Cursor.ROOT_VALUE), PrintOutputCapture(0))

    def print_equals_input(self, input: 'ParserInput', ctx: ExecutionContext) -> bool:
        printed = self.print_all()
        return printed == input.source().read()

    def get_style(self, style: Type[S]) -> Optional[S]:
        return NamedStyles.merge(style, self.markers.find_all(NamedStyles))


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
    def from_path(path: Path) -> Optional[FileAttributes]:
        if path.exists():
            try:
                # Get file stats
                stat = path.stat()
                creation_time = datetime.fromtimestamp(stat.st_ctime)
                last_modified_time = datetime.fromtimestamp(stat.st_mtime)
                last_access_time = datetime.fromtimestamp(stat.st_atime)

                is_readable = os.access(path, os.R_OK)
                is_writable = os.access(path, os.W_OK)
                is_executable = os.access(path, os.X_OK)
                size = stat.st_size

                return FileAttributes(creation_time, last_access_time, last_modified_time, is_readable, is_writable,
                                      is_executable, size)
            except OSError:
                pass
        return None


@dataclass(frozen=True)
class Checksum:
    algorithm: str
    value: bytes


class PrintOutputCapture(Generic[P]):
    @dataclass
    class MarkerPrinter(ABC):
        DEFAULT: ClassVar['PrintOutputCapture.MarkerPrinter'] = None  # type: ignore

        def before_syntax(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str:
            return ""

        def before_prefix(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str:
            return ""

        def after_syntax(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str:
            return ""

    def __init__(self, p: P, marker_printer: Optional['PrintOutputCapture.MarkerPrinter'] = None):
        self._context = p
        self._marker_printer = marker_printer or PrintOutputCapture.MarkerPrinter.DEFAULT
        self._out = []

    def get_out(self) -> str:
        return ''.join(self._out)

    def append(self, text: Optional[str] = None) -> 'PrintOutputCapture[P]':
        if text and len(text) > 0:
            self._out.append(text)
        return self

    def append_char(self, c: str) -> 'PrintOutputCapture[P]':
        if len(c) == 1:
            self._out.append(c)
        return self

    def clone(self) -> 'PrintOutputCapture[P]':
        return PrintOutputCapture(self._context, self._marker_printer)

    @property
    def marker_printer(self) -> MarkerPrinter:
        return self._marker_printer


@dataclass
class _DefaultMarkerPrinter(PrintOutputCapture.MarkerPrinter):
    def before_syntax(self, marker: 'Marker', cursor: 'Cursor', comment_wrapper: Callable[[str], str]) -> str:
        return marker.print(cursor, comment_wrapper, False)


PrintOutputCapture.MarkerPrinter.DEFAULT = _DefaultMarkerPrinter()
