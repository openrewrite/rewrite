import io
from abc import ABC, abstractmethod
from dataclasses import dataclass, replace
from pathlib import Path
from time import time_ns
from typing import Iterable, Optional, Callable, TypeVar, Any, cast, IO
from uuid import UUID

from .execution import ExecutionContext, InMemoryExecutionContext
from .markers import Markers, ParseExceptionResult
from .result import Result
from .tree import SourceFile, Tree, PrintOutputCapture, PrinterFactory, FileAttributes, Checksum
from .utils import random_id
from .visitor import TreeVisitor, Cursor


@dataclass(frozen=True, eq=False)
class ParserInput:
    _path: Path

    @property
    def path(self) -> Path:
        return self._path

    _file_attributes: Optional[FileAttributes]

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes

    _synthetic: bool

    @property
    def synthetic(self) -> bool:
        return self._synthetic

    _source: Callable[[], IO[Any]]

    @property
    def source(self) -> Callable[[], IO[Any]]:
        return self._source


P = TypeVar('P')


# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ParseError(SourceFile):
    @classmethod
    def build(cls, parser: 'Parser', input: ParserInput, relative_to: Optional[Path], ctx: ExecutionContext, exception: Exception,
              erroneous: Optional[SourceFile] = None) -> 'ParseError':
        return cls(random_id(),
                   Markers(random_id(), [ParseExceptionResult.build(parser, exception)]),
                   input.path.relative_to(relative_to) if relative_to else input.path,
                   input.file_attributes, parser.get_charset(ctx), False,
                   None,
                   input.source().read(),
                   erroneous)

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> 'ParseError':
        return self if id is self._id else replace(self, _id=id)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> 'ParseError':
        return self if markers is self._markers else replace(self, _markers=markers)

    _source_path: Path

    @property
    def source_path(self) -> Path:
        return self._source_path

    def with_source_path(self, source_path: Path) -> 'ParseError':
        return self if source_path is self._source_path else replace(self, _source_path=source_path)

    _file_attributes: Optional[FileAttributes]

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes

    def with_file_attributes(self, file_attributes: Optional[FileAttributes]) -> 'ParseError':
        return self if file_attributes is self._file_attributes else replace(self, _file_attributes=file_attributes)

    _charset_name: Optional[str]

    @property
    def charset_name(self) -> Optional[str]:
        return self._charset_name

    def with_charset_name(self, charset_name: Optional[str]) -> 'ParseError':
        return self if charset_name is self._charset_name else replace(self, _charset_name=charset_name)

    _charset_bom_marked: bool

    @property
    def charset_bom_marked(self) -> bool:
        return self._charset_bom_marked

    def with_charset_bom_marked(self, charset_bom_marked: bool) -> 'ParseError':
        return self if charset_bom_marked is self._charset_bom_marked else replace(self,
                                                                                   _charset_bom_marked=charset_bom_marked)

    _checksum: Optional[Checksum]

    @property
    def checksum(self) -> Optional[Checksum]:
        return self._checksum

    def with_checksum(self, checksum: Optional[Checksum]) -> 'ParseError':
        return self if checksum is self._checksum else replace(self, _checksum=checksum)

    _text: str

    @property
    def text(self) -> str:
        return self._text

    def with_text(self, text: str) -> 'ParseError':
        return self if text is self._text else replace(self, _text=text)

    _erroneous: Optional[SourceFile]

    @property
    def erroneous(self) -> Optional[SourceFile]:
        return self._erroneous

    def with_erroneous(self, erroneous: Optional[SourceFile]) -> 'ParseError':
        return self if erroneous is self._erroneous else replace(self, _erroneous=erroneous)

    def printer(self, cursor: Cursor) -> TreeVisitor[Tree, PrintOutputCapture[P]]:
        return PrinterFactory.current().create_printer(cursor)  # ty: ignore[possibly-missing-attribute]  # PrinterFactory.current() is always set

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        return v.is_adaptable_to(ParseErrorVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        return cast(ParseErrorVisitor, v).visit_parse_error(self, p)


class ParseErrorVisitor(TreeVisitor[Tree, P]):
    def is_acceptable(self, source_file: SourceFile, p: P) -> bool:
        return isinstance(source_file, ParseError)

    def visit_parse_error(self, e: ParseError, p: P) -> ParseError:
        return e.replace(markers=self.visit_markers(e.markers, p))  # ty: ignore[unresolved-attribute]


class Parser(ABC):
    @abstractmethod
    def parse_inputs(self, sources: Iterable[ParserInput], relative_to: Optional[Path],
                     ctx: ExecutionContext) -> Iterable[SourceFile]:
        pass

    @abstractmethod
    def accept(self, path: Path) -> bool:
        pass

    @abstractmethod
    def source_path_from_source_text(self, prefix: Path, source_code: str) -> Path:
        pass

    def parse(self, source_files: Iterable[Path], relative_to: Optional[Path], ctx: ExecutionContext) -> Iterable[
        SourceFile]:
        inputs = [ParserInput(path, None, False, lambda: io.FileIO(path)) for path in source_files]
        return self.parse_inputs(inputs, relative_to, ctx)

    def parse_strings(self, *sources: str) -> Iterable[SourceFile]:
        return self.__parse_strings_with_context(InMemoryExecutionContext(), *sources)

    def __parse_strings_with_context(self, ctx: ExecutionContext, *sources: str) -> Iterable[SourceFile]:
        inputs = [ParserInput(self.source_path_from_source_text(Path(str(time_ns())), source),
                              None,
                              True,
                              lambda: io.StringIO(source)) for source in sources]
        return self.parse_inputs(inputs, None, ctx)

    def accept_input(self, parser_input: ParserInput) -> bool:
        return parser_input.synthetic or self.accept(parser_input.path)

    def accepted_inputs(self, inputs: Iterable[ParserInput]) -> Iterable[ParserInput]:
        return filter(self.accept_input, inputs)

    def reset(self):
        return self

    def get_charset(self, ctx: ExecutionContext) -> str:
        return cast(str, ctx.get_message(ExecutionContext.CHARSET, 'utf-8'))


class ParserBuilder(ABC):
    _source_file_type: type

    def __init__(self, source_file_type: type):
        self._source_file_type = source_file_type

    @property
    def source_file_type(self) -> type:
        return self._source_file_type

    _dsl_name: Optional[str]

    @property
    def dsl_name(self) -> Optional[str]:
        return self._dsl_name

    @abstractmethod
    def build(self) -> Parser:
        pass


def require_print_equals_input(parser: Parser, source_file: SourceFile, parser_input: ParserInput,
                               relative_to: Optional[Path], ctx: ExecutionContext) -> SourceFile:
    required = ctx.get_message(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, True)
    if (required and not source_file.print_equals_input(parser_input, ctx)):
        diff = Result.diff(
            parser_input.source().read(),
            source_file.print_all(),
            parser_input.path
        )
        return (ParseError.build(parser, parser_input, relative_to, ctx,
                                 Exception(f"{source_file.source_path} is not print idempotent. \n{diff}"),
                                 source_file))
    return source_file
