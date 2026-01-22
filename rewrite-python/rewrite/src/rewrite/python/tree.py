from __future__ import annotations

import weakref
from dataclasses import dataclass, replace as dataclass_replace
from rewrite.utils import replace_if_changed
from enum import Enum
from pathlib import Path
from typing import List, Optional, TYPE_CHECKING
from uuid import UUID

if TYPE_CHECKING:
    from .visitor import PythonVisitor
from rewrite import Checksum, FileAttributes, SourceFile, TreeVisitor, Markers, Cursor, PrinterFactory
# Explicit imports from rewrite.java (excluding Binary, CompilationUnit which are redefined here)
from rewrite.java import (
    J, JavaType, JContainer, JLeftPadded, JRightPadded, Space,
    JavaSourceFile, TypeTree, TypedTree, NameTree, Expression, Statement,
    Block, Identifier, Import, TypeParameter,
)
from rewrite.python.support_types import Py, P

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Async(Py, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _statement: Statement

    @property
    def statement(self) -> Statement:
        return self._statement


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_async(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Await(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_await(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Binary(Py, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _left: Expression

    @property
    def left(self) -> Expression:
        return self._left


    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element


    _negation: Optional[Space]

    @property
    def negation(self) -> Optional[Space]:
        return self._negation


    _right: Expression

    @property
    def right(self) -> Expression:
        return self._right


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    class Type(Enum):
        In = 0
        Is = 1
        IsNot = 2
        NotIn = 3
        FloorDivision = 4
        MatrixMultiplication = 5
        Power = 6
        StringConcatenation = 7

    @dataclass
    class PaddingHelper:
        _t: Binary

        @property
        def operator(self) -> JLeftPadded[Binary.Type]:
            return self._t._operator

        def replace(self, **kwargs) -> Binary:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Binary.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Binary.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_python_binary(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ChainedAssignment(Py, Statement, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _variables: List[JRightPadded[Expression]]

    @property
    def variables(self) -> List[Expression]:
        return JRightPadded.get_elements(self._variables)


    _assignment: Expression

    @property
    def assignment(self) -> Expression:
        return self._assignment


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: ChainedAssignment

        @property
        def variables(self) -> List[JRightPadded[Expression]]:
            return self._t._variables

        def replace(self, **kwargs) -> ChainedAssignment:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ChainedAssignment.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ChainedAssignment.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_chained_assignment(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ExceptionType(Py, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    _exception_group: bool

    @property
    def exception_group(self) -> bool:
        return self._exception_group


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_exception_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class LiteralType(Py, Expression, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _literal: Expression

    @property
    def literal(self) -> Expression:
        return self._literal


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_literal_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeHint(Py, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _type_tree: Expression

    @property
    def type_tree(self) -> Expression:
        return self._type_tree


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_type_hint(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class CompilationUnit(Py, JavaSourceFile, SourceFile):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _source_path: Path

    @property
    def source_path(self) -> Path:
        return self._source_path


    _file_attributes: Optional[FileAttributes]

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes


    _charset_name: Optional[str]

    @property
    def charset_name(self) -> Optional[str]:
        return self._charset_name


    _charset_bom_marked: bool

    @property
    def charset_bom_marked(self) -> bool:
        return self._charset_bom_marked


    _checksum: Optional[Checksum]

    @property
    def checksum(self) -> Optional[Checksum]:
        return self._checksum


    _imports: List[JRightPadded[Import]]

    @property
    def imports(self) -> List[Import]:
        return JRightPadded.get_elements(self._imports)


    _statements: List[JRightPadded[Statement]]

    @property
    def statements(self) -> List[Statement]:
        return JRightPadded.get_elements(self._statements)


    _eof: Space

    @property
    def eof(self) -> Space:
        return self._eof


    @dataclass
    class PaddingHelper:
        _t: CompilationUnit

        @property
        def imports(self) -> List[JRightPadded[Import]]:
            return self._t._imports

        @property
        def statements(self) -> List[JRightPadded[Statement]]:
            return self._t._statements

        def replace(self, **kwargs) -> CompilationUnit:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = CompilationUnit.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = CompilationUnit.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def printer(self, cursor: Cursor) -> TreeVisitor:
        # PythonPrinter has TreeVisitor interface but doesn't extend it
        if factory := PrinterFactory.current():
            return factory.create_printer(cursor)
        from .printer import PythonPrinter
        return PythonPrinter()  # ty: ignore[invalid-return-type]

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_compilation_unit(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ExpressionStatement(Py, Expression, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    @property
    def prefix(self) -> Space:
        return self._expression.prefix

    @property
    def markers(self) -> Markers:
        return self._expression.markers

    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_expression_statement(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ExpressionTypeTree(Py, Expression, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _reference: J

    @property
    def reference(self) -> J:
        return self._reference


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_expression_type_tree(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class StatementExpression(Py, Expression, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    @property
    def prefix(self) -> Space:
        return self._statement.prefix

    @property
    def markers(self) -> Markers:
        return self._statement.markers

    _statement: Statement

    @property
    def statement(self) -> Statement:
        return self._statement


    def replace(self, **kwargs) -> 'StatementExpression':
        """Replace fields, handling delegated prefix/markers specially."""
        # Handle delegated properties by modifying the inner statement
        if 'prefix' in kwargs:
            new_statement = self._statement.replace(prefix=kwargs.pop('prefix'))  # ty: ignore[unresolved-attribute]  # Statement base class doesn't have replace
            kwargs['statement'] = new_statement
        if 'markers' in kwargs:
            new_statement = kwargs.get('statement', self._statement).replace(markers=kwargs.pop('markers'))
            kwargs['statement'] = new_statement
        # Map remaining kwargs
        mapped = {}
        for key, value in kwargs.items():
            if not key.startswith('_') and hasattr(self, f'_{key}'):
                mapped[f'_{key}'] = value
            else:
                mapped[key] = value
        if not mapped:
            return self
        return dataclass_replace(self, **mapped)

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_statement_expression(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MultiImport(Py, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _from: Optional[JRightPadded[NameTree]]

    @property
    def from_(self) -> Optional[NameTree]:
        return self._from.element if self._from else None


    _parenthesized: bool

    @property
    def parenthesized(self) -> bool:
        return self._parenthesized


    _names: JContainer[Import]

    @property
    def names(self) -> List[Import]:
        return self._names.elements


    @dataclass
    class PaddingHelper:
        _t: MultiImport

        @property
        def from_(self) -> Optional[JRightPadded[NameTree]]:
            return self._t._from

        @property
        def names(self) -> JContainer[Import]:
            return self._t._names

        def replace(self, **kwargs) -> MultiImport:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MultiImport.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MultiImport.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_multi_import(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class KeyValue(Py, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _key: JRightPadded[Expression]

    @property
    def key(self) -> Expression:
        return self._key.element


    _value: Expression

    @property
    def value(self) -> Expression:
        return self._value


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: KeyValue

        @property
        def key(self) -> JRightPadded[Expression]:
            return self._t._key

        def replace(self, **kwargs) -> KeyValue:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = KeyValue.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = KeyValue.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_key_value(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class DictLiteral(Py, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _elements: JContainer[Expression]

    @property
    def elements(self) -> List[Expression]:
        return self._elements.elements


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: DictLiteral

        @property
        def elements(self) -> JContainer[Expression]:
            return self._t._elements

        def replace(self, **kwargs) -> DictLiteral:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = DictLiteral.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = DictLiteral.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_dict_literal(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class CollectionLiteral(Py, Expression, TypedTree):
    class Kind(Enum):
        LIST = 0
        SET = 1
        TUPLE = 2

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _kind: Kind

    @property
    def kind(self) -> Kind:
        return self._kind


    _elements: JContainer[Expression]

    @property
    def elements(self) -> List[Expression]:
        return self._elements.elements


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: CollectionLiteral

        @property
        def elements(self) -> JContainer[Expression]:
            return self._t._elements

        def replace(self, **kwargs) -> CollectionLiteral:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = CollectionLiteral.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = CollectionLiteral.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_collection_literal(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class FormattedString(Py, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _delimiter: str

    @property
    def delimiter(self) -> str:
        return self._delimiter


    _parts: List[Expression]

    @property
    def parts(self) -> List[Expression]:
        return self._parts


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Value(Py, Expression, TypedTree):
        class Conversion(Enum):
            STR = 0
            REPR = 1
            ASCII = 2

        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> FormattedString.Value:
            return self if id is self._id else dataclass_replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> FormattedString.Value:
            return self if prefix is self._prefix else dataclass_replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> FormattedString.Value:
            return self if markers is self._markers else dataclass_replace(self, _markers=markers)

        _expression: JRightPadded[Expression]

        @property
        def expression(self) -> Expression:
            return self._expression.element

        def with_expression(self, expression: Expression) -> FormattedString.Value:
            return self.padding.replace(expression=self._expression.replace(element=expression))

        _debug: Optional[JRightPadded[bool]]

        @property
        def debug(self) -> Optional[bool]:
            return self._debug.element if self._debug else None

        def with_debug(self, debug: Optional[bool]) -> FormattedString.Value:
            return self.padding.replace(debug=self._debug.replace(element=debug) if self._debug else None)

        _conversion: Optional[Conversion]

        @property
        def conversion(self) -> Optional[Conversion]:
            return self._conversion

        def with_conversion(self, conversion: Optional[Conversion]) -> FormattedString.Value:
            return self if conversion is self._conversion else dataclass_replace(self, _conversion=conversion)

        _format: Optional[Expression]

        @property
        def format(self) -> Optional[Expression]:
            return self._format

        def with_format(self, format: Optional[Expression]) -> FormattedString.Value:
            return self if format is self._format else dataclass_replace(self, _format=format)

        @dataclass
        class PaddingHelper:
            _t: FormattedString.Value

            @property
            def expression(self) -> JRightPadded[Expression]:
                return self._t._expression

            @property
            def debug(self) -> Optional[JRightPadded[bool]]:
                return self._t._debug

            def replace(self, **kwargs) -> FormattedString.Value:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = FormattedString.Value.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = FormattedString.Value.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_python(self, v: PythonVisitor[P], p: P) -> J:
            return v.visit_formatted_string_value(self, p)

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_formatted_string(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Pass(Py, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_pass(self, p)  # ty: ignore[invalid-return-type]  # visitor returns J|None

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TrailingElseWrapper(Py, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _statement: Statement

    @property
    def statement(self) -> Statement:
        return self._statement


    _else_block: JLeftPadded[Block]

    @property
    def else_block(self) -> Block:
        return self._else_block.element


    @dataclass
    class PaddingHelper:
        _t: TrailingElseWrapper

        @property
        def else_block(self) -> JLeftPadded[Block]:
            return self._t._else_block

        def replace(self, **kwargs) -> TrailingElseWrapper:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = TrailingElseWrapper.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = TrailingElseWrapper.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_trailing_else_wrapper(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ComprehensionExpression(Py, Expression):
    class Kind(Enum):
        LIST = 0
        SET = 1
        DICT = 2
        GENERATOR = 3

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _kind: Kind

    @property
    def kind(self) -> Kind:
        return self._kind


    _result: Expression

    @property
    def result(self) -> Expression:
        return self._result


    _clauses: List[Clause]

    @property
    def clauses(self) -> List[Clause]:
        return self._clauses


    _suffix: Space

    @property
    def suffix(self) -> Space:
        return self._suffix


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Condition(Py):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> ComprehensionExpression.Condition:
            return self if id is self._id else dataclass_replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> ComprehensionExpression.Condition:
            return self if prefix is self._prefix else dataclass_replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> ComprehensionExpression.Condition:
            return self if markers is self._markers else dataclass_replace(self, _markers=markers)

        _expression: Expression

        @property
        def expression(self) -> Expression:
            return self._expression

        def with_expression(self, expression: Expression) -> ComprehensionExpression.Condition:
            return self if expression is self._expression else dataclass_replace(self, _expression=expression)

        def accept_python(self, v: PythonVisitor[P], p: P) -> J:
            return v.visit_comprehension_condition(self, p)

    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Clause(Py):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> ComprehensionExpression.Clause:
            return self if id is self._id else dataclass_replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> ComprehensionExpression.Clause:
            return self if prefix is self._prefix else dataclass_replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> ComprehensionExpression.Clause:
            return self if markers is self._markers else dataclass_replace(self, _markers=markers)

        _async: Optional[JRightPadded[bool]]

        @property
        def async_(self) -> Optional[bool]:
            return self._async.element if self._async else None

        def with_async(self, async_: Optional[bool]) -> ComprehensionExpression.Clause:
            return self.padding.replace(async_=self._async.replace(element=async_) if self._async else None)

        _iterator_variable: Expression

        @property
        def iterator_variable(self) -> Expression:
            return self._iterator_variable

        def with_iterator_variable(self, iterator_variable: Expression) -> ComprehensionExpression.Clause:
            return self if iterator_variable is self._iterator_variable else dataclass_replace(self, _iterator_variable=iterator_variable)

        _iterated_list: JLeftPadded[Expression]

        @property
        def iterated_list(self) -> Expression:
            return self._iterated_list.element

        def with_iterated_list(self, iterated_list: Expression) -> ComprehensionExpression.Clause:
            return self.padding.replace(iterated_list=self._iterated_list.replace(element=iterated_list))

        _conditions: Optional[List[ComprehensionExpression.Condition]]

        @property
        def conditions(self) -> Optional[List[ComprehensionExpression.Condition]]:
            return self._conditions

        def with_conditions(self, conditions: Optional[List[ComprehensionExpression.Condition]]) -> ComprehensionExpression.Clause:
            return self if conditions is self._conditions else dataclass_replace(self, _conditions=conditions)

        @dataclass
        class PaddingHelper:
            _t: ComprehensionExpression.Clause

            @property
            def async_(self) -> Optional[JRightPadded[bool]]:
                return self._t._async

            @property
            def iterated_list(self) -> JLeftPadded[Expression]:
                return self._t._iterated_list

            def replace(self, **kwargs) -> ComprehensionExpression.Clause:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = ComprehensionExpression.Clause.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = ComprehensionExpression.Clause.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_python(self, v: PythonVisitor[P], p: P) -> J:
            return v.visit_comprehension_clause(self, p)

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_comprehension_expression(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeAlias(Py, Statement, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name


    _type_parameters: Optional[JContainer[TypeParameter]]

    @property
    def type_parameters(self) -> Optional[List[TypeParameter]]:
        return self._type_parameters.elements if self._type_parameters else None


    _value: JLeftPadded[J]

    @property
    def value(self) -> J:
        return self._value.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: TypeAlias

        @property
        def type_parameters(self) -> Optional[JContainer[TypeParameter]]:
            return self._t._type_parameters

        @property
        def value(self) -> JLeftPadded[J]:
            return self._t._value

        def replace(self, **kwargs) -> TypeAlias:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = TypeAlias.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = TypeAlias.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_type_alias(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class YieldFrom(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_yield_from(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class UnionType(Py, Expression, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _types: List[JRightPadded[Expression]]

    @property
    def types(self) -> List[Expression]:
        return JRightPadded.get_elements(self._types)


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: UnionType

        @property
        def types(self) -> List[JRightPadded[Expression]]:
            return self._t._types

        def replace(self, **kwargs) -> UnionType:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = UnionType.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = UnionType.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_union_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class VariableScope(Py, Statement):
    class Kind(Enum):
        GLOBAL = 0
        NONLOCAL = 1

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _kind: Kind

    @property
    def kind(self) -> Kind:
        return self._kind


    _names: List[JRightPadded[Identifier]]

    @property
    def names(self) -> List[Identifier]:
        return JRightPadded.get_elements(self._names)


    @dataclass
    class PaddingHelper:
        _t: VariableScope

        @property
        def names(self) -> List[JRightPadded[Identifier]]:
            return self._t._names

        def replace(self, **kwargs) -> VariableScope:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = VariableScope.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = VariableScope.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_variable_scope(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Del(Py, Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _targets: List[JRightPadded[Expression]]

    @property
    def targets(self) -> List[Expression]:
        return JRightPadded.get_elements(self._targets)


    @dataclass
    class PaddingHelper:
        _t: Del

        @property
        def targets(self) -> List[JRightPadded[Expression]]:
            return self._t._targets

        def replace(self, **kwargs) -> Del:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Del.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Del.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_del(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class SpecialParameter(Py, TypeTree):
    class Kind(Enum):
        KWARGS = 0
        ARGS = 1

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _kind: Kind

    @property
    def kind(self) -> Kind:
        return self._kind


    _type_hint: Optional[TypeHint]

    @property
    def type_hint(self) -> Optional[TypeHint]:
        return self._type_hint


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_special_parameter(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Star(Py, Expression, TypeTree):
    class Kind(Enum):
        LIST = 0
        DICT = 1

    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _kind: Kind

    @property
    def kind(self) -> Kind:
        return self._kind


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_star(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class NamedArgument(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name


    _value: JLeftPadded[Expression]

    @property
    def value(self) -> Expression:
        return self._value.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: NamedArgument

        @property
        def value(self) -> JLeftPadded[Expression]:
            return self._t._value

        def replace(self, **kwargs) -> NamedArgument:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = NamedArgument.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = NamedArgument.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_named_argument(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeHintedExpression(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    _type_hint: TypeHint

    @property
    def type_hint(self) -> TypeHint:
        return self._type_hint


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_type_hinted_expression(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ErrorFrom(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _error: Expression

    @property
    def error(self) -> Expression:
        return self._error


    _from: JLeftPadded[Expression]

    @property
    def from_(self) -> Expression:
        return self._from.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: ErrorFrom

        @property
        def from_(self) -> JLeftPadded[Expression]:
            return self._t._from

        def replace(self, **kwargs) -> ErrorFrom:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ErrorFrom.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ErrorFrom.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_error_from(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MatchCase(Py, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _pattern: Pattern

    @property
    def pattern(self) -> Pattern:
        return self._pattern


    _guard: Optional[JLeftPadded[Expression]]

    @property
    def guard(self) -> Optional[Expression]:
        return self._guard.element if self._guard else None


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: MatchCase

        @property
        def guard(self) -> Optional[JLeftPadded[Expression]]:
            return self._t._guard

        def replace(self, **kwargs) -> MatchCase:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MatchCase.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MatchCase.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Pattern(Py, Expression):
        class Kind(Enum):
            AS = 0
            CAPTURE = 1
            CLASS = 2
            DOUBLE_STAR = 3
            GROUP = 4
            KEY_VALUE = 5
            KEYWORD = 6
            LITERAL = 7
            MAPPING = 8
            OR = 9
            SEQUENCE = 10
            SEQUENCE_LIST = 11
            SEQUENCE_TUPLE = 12
            STAR = 13
            VALUE = 14
            WILDCARD = 15

        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> MatchCase.Pattern:
            return self if id is self._id else dataclass_replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> MatchCase.Pattern:
            return self if prefix is self._prefix else dataclass_replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> MatchCase.Pattern:
            return self if markers is self._markers else dataclass_replace(self, _markers=markers)

        _kind: Kind

        @property
        def kind(self) -> Kind:
            return self._kind

        def with_kind(self, kind: Kind) -> MatchCase.Pattern:
            return self if kind is self._kind else dataclass_replace(self, _kind=kind)

        _children: JContainer[J]

        @property
        def children(self) -> List[J]:
            return self._children.elements

        def with_children(self, children: List[J]) -> MatchCase.Pattern:
            return self.padding.replace(children=self._children.padding.replace(elements=JRightPadded.merge_elements(self._children.padding.elements, children)))

        _type: Optional[JavaType]

        @property
        def type(self) -> Optional[JavaType]:
            return self._type

        def with_type(self, type: Optional[JavaType]) -> MatchCase.Pattern:
            return self if type is self._type else dataclass_replace(self, _type=type)

        @dataclass
        class PaddingHelper:
            _t: MatchCase.Pattern

            @property
            def children(self) -> JContainer[J]:
                return self._t._children

            def replace(self, **kwargs) -> MatchCase.Pattern:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = MatchCase.Pattern.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = MatchCase.Pattern.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_python(self, v: PythonVisitor[P], p: P) -> J:
            return v.visit_match_case_pattern(self, p)

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_match_case(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Slice(Py, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id


    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix


    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers


    _start: Optional[JRightPadded[Expression]]

    @property
    def start(self) -> Optional[Expression]:
        return self._start.element if self._start else None


    _stop: Optional[JRightPadded[Expression]]

    @property
    def stop(self) -> Optional[Expression]:
        return self._stop.element if self._stop else None


    _step: Optional[JRightPadded[Expression]]

    @property
    def step(self) -> Optional[Expression]:
        return self._step.element if self._step else None


    @dataclass
    class PaddingHelper:
        _t: Slice

        @property
        def start(self) -> Optional[JRightPadded[Expression]]:
            return self._t._start

        @property
        def stop(self) -> Optional[JRightPadded[Expression]]:
            return self._t._stop

        @property
        def step(self) -> Optional[JRightPadded[Expression]]:
            return self._t._step

        def replace(self, **kwargs) -> Slice:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Slice.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Slice.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_python(self, v: PythonVisitor[P], p: P) -> J:
        return v.visit_slice(self, p)
