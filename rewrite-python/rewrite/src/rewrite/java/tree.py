from __future__ import annotations

import weakref
from dataclasses import dataclass
from rewrite.utils import replace_if_changed
from enum import Enum
from pathlib import Path
from typing import List, Optional, TYPE_CHECKING, Generic
from uuid import UUID

if TYPE_CHECKING:
    from .visitor import JavaVisitor
from .support_types import *
from rewrite import Checksum, FileAttributes, SourceFile, Tree, TreeVisitor, Markers, Cursor, PrintOutputCapture, PrinterFactory

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class AnnotatedType(Expression, TypeTree):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _type_expression: TypeTree

    @property
    def type_expression(self) -> TypeTree:
        return self._type_expression


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_annotated_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Annotation(Expression):
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


    _annotation_type: NameTree

    @property
    def annotation_type(self) -> NameTree:
        return self._annotation_type


    _arguments: Optional[JContainer[Expression]]

    @property
    def arguments(self) -> Optional[List[Expression]]:
        return self._arguments.elements if self._arguments else None


    @dataclass
    class PaddingHelper:
        _t: Annotation

        @property
        def arguments(self) -> Optional[JContainer[Expression]]:
            return self._t._arguments

        def replace(self, **kwargs) -> Annotation:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Annotation.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Annotation.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_annotation(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ArrayAccess(Expression, TypedTree):
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


    _indexed: Expression

    @property
    def indexed(self) -> Expression:
        return self._indexed


    _dimension: ArrayDimension

    @property
    def dimension(self) -> ArrayDimension:
        return self._dimension


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_array_access(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ArrayType(TypeTree, Expression):
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


    _element_type: TypeTree

    @property
    def element_type(self) -> TypeTree:
        return self._element_type


    _annotations: Optional[List[Annotation]]

    @property
    def annotations(self) -> Optional[List[Annotation]]:
        return self._annotations


    _dimension: Optional[JLeftPadded[Space]]

    @property
    def dimension(self) -> Optional[JLeftPadded[Space]]:
        return self._dimension


    _type: JavaType

    @property
    def type(self) -> JavaType:
        return self._type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_array_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Assert(Statement):
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


    _condition: Expression

    @property
    def condition(self) -> Expression:
        return self._condition


    _detail: Optional[JLeftPadded[Expression]]

    @property
    def detail(self) -> Optional[JLeftPadded[Expression]]:
        return self._detail


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_assert(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Assignment(Statement, Expression, TypedTree):
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


    _variable: Expression

    @property
    def variable(self) -> Expression:
        return self._variable


    _assignment: JLeftPadded[Expression]

    @property
    def assignment(self) -> Expression:
        return self._assignment.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: Assignment

        @property
        def assignment(self) -> JLeftPadded[Expression]:
            return self._t._assignment

        def replace(self, **kwargs) -> Assignment:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Assignment.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Assignment.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_assignment(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class AssignmentOperation(Statement, Expression, TypedTree):
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


    _variable: Expression

    @property
    def variable(self) -> Expression:
        return self._variable


    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element


    _assignment: Expression

    @property
    def assignment(self) -> Expression:
        return self._assignment


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    class Type(Enum):
        Addition = 0
        BitAnd = 1
        BitOr = 2
        BitXor = 3
        Division = 4
        Exponentiation = 5
        FloorDivision = 6
        LeftShift = 7
        MatrixMultiplication = 8
        Modulo = 9
        Multiplication = 10
        RightShift = 11
        Subtraction = 12
        UnsignedRightShift = 13

    @dataclass
    class PaddingHelper:
        _t: AssignmentOperation

        @property
        def operator(self) -> JLeftPadded[AssignmentOperation.Type]:
            return self._t._operator

        def replace(self, **kwargs) -> AssignmentOperation:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = AssignmentOperation.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = AssignmentOperation.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_assignment_operation(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Binary(Expression, TypedTree):
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


    _right: Expression

    @property
    def right(self) -> Expression:
        return self._right


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    class Type(Enum):
        Addition = 0
        Subtraction = 1
        Multiplication = 2
        Division = 3
        Modulo = 4
        LessThan = 5
        GreaterThan = 6
        LessThanOrEqual = 7
        GreaterThanOrEqual = 8
        Equal = 9
        NotEqual = 10
        BitAnd = 11
        BitOr = 12
        BitXor = 13
        LeftShift = 14
        RightShift = 15
        UnsignedRightShift = 16
        Or = 17
        And = 18

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

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_binary(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Block(Statement):
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


    _static: JRightPadded[bool]

    @property
    def static(self) -> bool:
        return self._static.element


    _statements: List[JRightPadded[Statement]]

    @property
    def statements(self) -> List[Statement]:
        return JRightPadded.get_elements(self._statements)


    _end: Space

    @property
    def end(self) -> Space:
        return self._end


    @dataclass
    class PaddingHelper:
        _t: Block

        @property
        def static(self) -> JRightPadded[bool]:
            return self._t._static

        @property
        def statements(self) -> List[JRightPadded[Statement]]:
            return self._t._statements

        def replace(self, **kwargs) -> Block:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Block.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Block.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_block(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Break(Statement):
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


    _label: Optional[Identifier]

    @property
    def label(self) -> Optional[Identifier]:
        return self._label


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_break(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Case(Statement):
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


    _type: Type

    @property
    def type(self) -> Type:
        return self._type


    _case_labels: JContainer[J]

    @property
    def case_labels(self) -> List[J]:
        return self._case_labels.elements


    _statements: JContainer[Statement]

    @property
    def statements(self) -> List[Statement]:
        return self._statements.elements


    _body: Optional[JRightPadded[J]]

    @property
    def body(self) -> Optional[J]:
        return self._body.element if self._body else None


    _guard: Optional[Expression]

    @property
    def guard(self) -> Optional[Expression]:
        return self._guard


    class Type(Enum):
        Statement = 0
        Rule = 1

    @dataclass
    class PaddingHelper:
        _t: Case

        @property
        def case_labels(self) -> JContainer[J]:
            return self._t._case_labels

        @property
        def statements(self) -> JContainer[Statement]:
            return self._t._statements

        @property
        def body(self) -> Optional[JRightPadded[J]]:
            return self._t._body

        def replace(self, **kwargs) -> Case:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Case.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Case.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_case(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ClassDeclaration(Statement, TypedTree):
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


    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations


    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers


    _kind: Kind


    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name


    _type_parameters: Optional[JContainer[TypeParameter]]

    @property
    def type_parameters(self) -> Optional[List[TypeParameter]]:
        return self._type_parameters.elements if self._type_parameters else None


    _primary_constructor: Optional[JContainer[Statement]]

    @property
    def primary_constructor(self) -> Optional[List[Statement]]:
        return self._primary_constructor.elements if self._primary_constructor else None


    _extends: Optional[JLeftPadded[TypeTree]]

    @property
    def extends(self) -> Optional[TypeTree]:
        return self._extends.element if self._extends else None


    _implements: Optional[JContainer[TypeTree]]

    @property
    def implements(self) -> Optional[List[TypeTree]]:
        return self._implements.elements if self._implements else None


    _permits: Optional[JContainer[TypeTree]]

    @property
    def permits(self) -> Optional[List[TypeTree]]:
        return self._permits.elements if self._permits else None


    _body: Block

    @property
    def body(self) -> Block:
        return self._body


    _type: Optional[JavaType.FullyQualified]

    @property
    def type(self) -> Optional[JavaType.FullyQualified]:
        return self._type


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Kind(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> ClassDeclaration.Kind:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> ClassDeclaration.Kind:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> ClassDeclaration.Kind:
            return self if markers is self._markers else replace(self, _markers=markers)

        _annotations: List[Annotation]

        @property
        def annotations(self) -> List[Annotation]:
            return self._annotations

        def with_annotations(self, annotations: List[Annotation]) -> ClassDeclaration.Kind:
            return self if annotations is self._annotations else replace(self, _annotations=annotations)

        _type: Type

        @property
        def type(self) -> Type:
            return self._type

        def with_type(self, type: Type) -> ClassDeclaration.Kind:
            return self if type is self._type else replace(self, _type=type)

        class Type(Enum):
            Class = 0
            Enum = 1
            Interface = 2
            Annotation = 3
            Record = 4
            Value = 5

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_class_declaration_kind(self, p)

    @dataclass
    class PaddingHelper:
        _t: ClassDeclaration

        @property
        def kind(self) -> ClassDeclaration.Kind:
            return self._t._kind

        @property
        def type_parameters(self) -> Optional[JContainer[TypeParameter]]:
            return self._t._type_parameters

        @property
        def primary_constructor(self) -> Optional[JContainer[Statement]]:
            return self._t._primary_constructor

        @property
        def extends(self) -> Optional[JLeftPadded[TypeTree]]:
            return self._t._extends

        @property
        def implements(self) -> Optional[JContainer[TypeTree]]:
            return self._t._implements

        @property
        def permits(self) -> Optional[JContainer[TypeTree]]:
            return self._t._permits

        def replace(self, **kwargs) -> ClassDeclaration:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ClassDeclaration.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ClassDeclaration.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_class_declaration(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class CompilationUnit(JavaSourceFile, SourceFile):
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


    _package_declaration: Optional[JRightPadded[Package]]

    @property
    def package_declaration(self) -> Optional[Package]:
        return self._package_declaration.element if self._package_declaration else None


    _imports: List[JRightPadded[Import]]

    @property
    def imports(self) -> List[Import]:
        return JRightPadded.get_elements(self._imports)


    _classes: List[ClassDeclaration]

    @property
    def classes(self) -> List[ClassDeclaration]:
        return self._classes


    _eof: Space

    @property
    def eof(self) -> Space:
        return self._eof


    @dataclass
    class PaddingHelper:
        _t: CompilationUnit

        @property
        def package_declaration(self) -> Optional[JRightPadded[Package]]:
            return self._t._package_declaration

        @property
        def imports(self) -> List[JRightPadded[Import]]:
            return self._t._imports

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

    def printer(self, cursor: Cursor) -> TreeVisitor[Tree, PrintOutputCapture[P]]:
        if factory := PrinterFactory.current():
            return factory.create_printer(cursor)
        raise NotImplementedError("JavaPrinter is not yet implemented. Use PrinterFactory to provide a printer.")

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_compilation_unit(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Continue(Statement):
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


    _label: Optional[Identifier]

    @property
    def label(self) -> Optional[Identifier]:
        return self._label


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_continue(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class DoWhileLoop(Loop):
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


    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element


    _while_condition: JLeftPadded[ControlParentheses[Expression]]

    @property
    def while_condition(self) -> ControlParentheses[Expression]:
        return self._while_condition.element


    @dataclass
    class PaddingHelper:
        _t: DoWhileLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        @property
        def while_condition(self) -> JLeftPadded[ControlParentheses[Expression]]:
            return self._t._while_condition

        def replace(self, **kwargs) -> DoWhileLoop:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = DoWhileLoop.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = DoWhileLoop.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_do_while_loop(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Empty(Statement, Expression, TypeTree):
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


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_empty(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class EnumValue(J):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name


    _initializer: Optional[NewClass]

    @property
    def initializer(self) -> Optional[NewClass]:
        return self._initializer


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_enum_value(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class EnumValueSet(Statement):
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


    _enums: List[JRightPadded[EnumValue]]

    @property
    def enums(self) -> List[EnumValue]:
        return JRightPadded.get_elements(self._enums)


    _terminated_with_semicolon: bool

    @property
    def terminated_with_semicolon(self) -> bool:
        return self._terminated_with_semicolon


    @dataclass
    class PaddingHelper:
        _t: EnumValueSet

        @property
        def enums(self) -> List[JRightPadded[EnumValue]]:
            return self._t._enums

        def replace(self, **kwargs) -> EnumValueSet:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = EnumValueSet.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = EnumValueSet.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_enum_value_set(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class FieldAccess(TypeTree, Expression, Statement):
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


    _target: Expression

    @property
    def target(self) -> Expression:
        return self._target


    _name: JLeftPadded[Identifier]

    @property
    def name(self) -> Identifier:
        return self._name.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: FieldAccess

        @property
        def name(self) -> JLeftPadded[Identifier]:
            return self._t._name

        def replace(self, **kwargs) -> FieldAccess:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = FieldAccess.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = FieldAccess.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_field_access(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ForEachLoop(Loop):
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


    _control: Control

    @property
    def control(self) -> Control:
        return self._control


    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Control(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> ForEachLoop.Control:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> ForEachLoop.Control:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> ForEachLoop.Control:
            return self if markers is self._markers else replace(self, _markers=markers)

        _variable: JRightPadded[Statement]

        @property
        def variable(self) -> Statement:
            return self._variable.element

        def with_variable(self, variable: Statement) -> ForEachLoop.Control:
            return self.padding.replace(variable=self._variable.replace(element=variable))

        _iterable: JRightPadded[Expression]

        @property
        def iterable(self) -> Expression:
            return self._iterable.element

        def with_iterable(self, iterable: Expression) -> ForEachLoop.Control:
            return self.padding.replace(iterable=self._iterable.replace(element=iterable))

        @dataclass
        class PaddingHelper:
            _t: ForEachLoop.Control

            @property
            def variable(self) -> JRightPadded[Statement]:
                return self._t._variable

            @property
            def iterable(self) -> JRightPadded[Expression]:
                return self._t._iterable

            def replace(self, **kwargs) -> ForEachLoop.Control:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = ForEachLoop.Control.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = ForEachLoop.Control.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_for_each_control(self, p)

    @dataclass
    class PaddingHelper:
        _t: ForEachLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        def replace(self, **kwargs) -> ForEachLoop:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ForEachLoop.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ForEachLoop.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_for_each_loop(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ForLoop(Loop):
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


    _control: Control

    @property
    def control(self) -> Control:
        return self._control


    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Control(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> ForLoop.Control:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> ForLoop.Control:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> ForLoop.Control:
            return self if markers is self._markers else replace(self, _markers=markers)

        _init: List[JRightPadded[Statement]]

        @property
        def init(self) -> List[Statement]:
            return JRightPadded.get_elements(self._init)

        def with_init(self, init: List[Statement]) -> ForLoop.Control:
            return self.padding.replace(init=JRightPadded.merge_elements(self._init, init))

        _condition: JRightPadded[Expression]

        @property
        def condition(self) -> Expression:
            return self._condition.element

        def with_condition(self, condition: Expression) -> ForLoop.Control:
            return self.padding.replace(condition=self._condition.replace(element=condition))

        _update: List[JRightPadded[Statement]]

        @property
        def update(self) -> List[Statement]:
            return JRightPadded.get_elements(self._update)

        def with_update(self, update: List[Statement]) -> ForLoop.Control:
            return self.padding.replace(update=JRightPadded.merge_elements(self._update, update))

        @dataclass
        class PaddingHelper:
            _t: ForLoop.Control

            @property
            def init(self) -> List[JRightPadded[Statement]]:
                return self._t._init

            @property
            def condition(self) -> JRightPadded[Expression]:
                return self._t._condition

            @property
            def update(self) -> List[JRightPadded[Statement]]:
                return self._t._update

            def replace(self, **kwargs) -> ForLoop.Control:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = ForLoop.Control.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = ForLoop.Control.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_for_control(self, p)

    @dataclass
    class PaddingHelper:
        _t: ForLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        def replace(self, **kwargs) -> ForLoop:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ForLoop.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ForLoop.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_for_loop(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ParenthesizedTypeTree(TypeTree, Expression):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _parenthesized_type: Parentheses[TypeTree]

    @property
    def parenthesized_type(self) -> Parentheses[TypeTree]:
        return self._parenthesized_type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_parenthesized_type_tree(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Identifier(TypeTree, Expression):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _simple_name: str

    @property
    def simple_name(self) -> str:
        return self._simple_name


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    _field_type: Optional[JavaType.Variable]

    @property
    def field_type(self) -> Optional[JavaType.Variable]:
        return self._field_type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_identifier(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class If(Statement):
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


    _if_condition: ControlParentheses[Expression]

    @property
    def if_condition(self) -> ControlParentheses[Expression]:
        return self._if_condition


    _then_part: JRightPadded[Statement]

    @property
    def then_part(self) -> Statement:
        return self._then_part.element


    _else_part: Optional[Else]

    @property
    def else_part(self) -> Optional[Else]:
        return self._else_part


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Else(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> If.Else:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> If.Else:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> If.Else:
            return self if markers is self._markers else replace(self, _markers=markers)

        _body: JRightPadded[Statement]

        @property
        def body(self) -> Statement:
            return self._body.element

        def with_body(self, body: Statement) -> If.Else:
            return self.padding.replace(body=self._body.replace(element=body))

        @dataclass
        class PaddingHelper:
            _t: If.Else

            @property
            def body(self) -> JRightPadded[Statement]:
                return self._t._body

            def replace(self, **kwargs) -> If.Else:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = If.Else.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = If.Else.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_else(self, p)

    @dataclass
    class PaddingHelper:
        _t: If

        @property
        def then_part(self) -> JRightPadded[Statement]:
            return self._t._then_part

        def replace(self, **kwargs) -> If:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = If.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = If.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_if(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Import(Statement):
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


    _static: JLeftPadded[bool]

    @property
    def static(self) -> bool:
        return self._static.element


    _qualid: FieldAccess

    @property
    def qualid(self) -> FieldAccess:
        return self._qualid


    _alias: Optional[JLeftPadded[Identifier]]

    @property
    def alias(self) -> Optional[Identifier]:
        return self._alias.element if self._alias else None


    @dataclass
    class PaddingHelper:
        _t: Import

        @property
        def static(self) -> JLeftPadded[bool]:
            return self._t._static

        @property
        def alias(self) -> Optional[JLeftPadded[Identifier]]:
            return self._t._alias

        def replace(self, **kwargs) -> Import:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Import.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Import.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_import(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class InstanceOf(Expression, TypedTree):
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


    _expression: JRightPadded[Expression]

    @property
    def expression(self) -> Expression:
        return self._expression.element


    _clazz: J

    @property
    def clazz(self) -> J:
        return self._clazz


    _pattern: Optional[J]

    @property
    def pattern(self) -> Optional[J]:
        return self._pattern


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: InstanceOf

        @property
        def expression(self) -> JRightPadded[Expression]:
            return self._t._expression

        def replace(self, **kwargs) -> InstanceOf:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = InstanceOf.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = InstanceOf.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_instance_of(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class DeconstructionPattern(TypedTree):
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


    _deconstructor: Expression

    @property
    def deconstructor(self) -> Expression:
        return self._deconstructor


    _nested: JContainer[J]

    @property
    def nested(self) -> List[J]:
        return self._nested.elements


    _type: JavaType

    @property
    def type(self) -> JavaType:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: DeconstructionPattern

        @property
        def nested(self) -> JContainer[J]:
            return self._t._nested

        def replace(self, **kwargs) -> DeconstructionPattern:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = DeconstructionPattern.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = DeconstructionPattern.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_deconstruction_pattern(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class IntersectionType(TypeTree, Expression):
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


    _bounds: JContainer[TypeTree]

    @property
    def bounds(self) -> List[TypeTree]:
        return self._bounds.elements


    @dataclass
    class PaddingHelper:
        _t: IntersectionType

        @property
        def bounds(self) -> JContainer[TypeTree]:
            return self._t._bounds

        def replace(self, **kwargs) -> IntersectionType:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = IntersectionType.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = IntersectionType.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_intersection_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Label(Statement):
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


    _label: JRightPadded[Identifier]

    @property
    def label(self) -> Identifier:
        return self._label.element


    _statement: Statement

    @property
    def statement(self) -> Statement:
        return self._statement


    @dataclass
    class PaddingHelper:
        _t: Label

        @property
        def label(self) -> JRightPadded[Identifier]:
            return self._t._label

        def replace(self, **kwargs) -> Label:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Label.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Label.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_label(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Lambda(Statement, Expression, TypedTree):
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


    _parameters: Parameters

    @property
    def parameters(self) -> Parameters:
        return self._parameters


    _arrow: Space

    @property
    def arrow(self) -> Space:
        return self._arrow


    _body: J

    @property
    def body(self) -> J:
        return self._body


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Parameters(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> Lambda.Parameters:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> Lambda.Parameters:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> Lambda.Parameters:
            return self if markers is self._markers else replace(self, _markers=markers)

        _parenthesized: bool

        @property
        def parenthesized(self) -> bool:
            return self._parenthesized

        def with_parenthesized(self, parenthesized: bool) -> Lambda.Parameters:
            return self if parenthesized is self._parenthesized else replace(self, _parenthesized=parenthesized)

        _parameters: List[JRightPadded[J]]

        @property
        def parameters(self) -> List[J]:
            return JRightPadded.get_elements(self._parameters)

        def with_parameters(self, parameters: List[J]) -> Lambda.Parameters:
            return self.padding.replace(parameters=JRightPadded.merge_elements(self._parameters, parameters))

        @dataclass
        class PaddingHelper:
            _t: Lambda.Parameters

            @property
            def parameters(self) -> List[JRightPadded[J]]:
                return self._t._parameters

            def replace(self, **kwargs) -> Lambda.Parameters:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = Lambda.Parameters.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = Lambda.Parameters.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_lambda_parameters(self, p)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_lambda(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Literal(Expression, TypedTree):
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


    _value: Optional[object]

    @property
    def value(self) -> Optional[object]:
        return self._value


    _value_source: Optional[str]

    @property
    def value_source(self) -> Optional[str]:
        return self._value_source


    _unicode_escapes: Optional[List[UnicodeEscape]]

    @property
    def unicode_escapes(self) -> Optional[List[UnicodeEscape]]:
        return self._unicode_escapes


    _type: JavaType.Primitive

    @property
    def type(self) -> JavaType.Primitive:
        return self._type


    @dataclass
    class UnicodeEscape:
        _value_source_index: int

        @property
        def value_source_index(self) -> int:
            return self._value_source_index

        def with_value_source_index(self, value_source_index: int) -> Literal.UnicodeEscape:
            return self if value_source_index is self._value_source_index else replace(self, _value_source_index=value_source_index)

        _code_point: str

        @property
        def code_point(self) -> str:
            return self._code_point

        def with_code_point(self, code_point: str) -> Literal.UnicodeEscape:
            return self if code_point is self._code_point else replace(self, _code_point=code_point)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_literal(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MemberReference(TypedTree, MethodCall):
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


    _containing: JRightPadded[Expression]

    @property
    def containing(self) -> Expression:
        return self._containing.element


    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements if self._type_parameters else None


    _reference: JLeftPadded[Identifier]

    @property
    def reference(self) -> Identifier:
        return self._reference.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type


    _variable_type: Optional[JavaType.Variable]

    @property
    def variable_type(self) -> Optional[JavaType.Variable]:
        return self._variable_type


    @dataclass
    class PaddingHelper:
        _t: MemberReference

        @property
        def containing(self) -> JRightPadded[Expression]:
            return self._t._containing

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        @property
        def reference(self) -> JLeftPadded[Identifier]:
            return self._t._reference

        def replace(self, **kwargs) -> MemberReference:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MemberReference.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MemberReference.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_member_reference(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MethodDeclaration(Statement, TypedTree):
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


    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations


    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers


    _type_parameters: Optional[TypeParameters]

    _return_type_expression: Optional[TypeTree]

    @property
    def return_type_expression(self) -> Optional[TypeTree]:
        return self._return_type_expression


    _name_annotations: List[Annotation]

    @property
    def name_annotations(self) -> List[Annotation]:
        return self._name_annotations

    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name

    _parameters: JContainer[Statement]

    @property
    def parameters(self) -> List[Statement]:
        return self._parameters.elements


    _throws: Optional[JContainer[NameTree]]

    @property
    def throws(self) -> Optional[List[NameTree]]:
        return self._throws.elements if self._throws else None


    _body: Optional[Block]

    @property
    def body(self) -> Optional[Block]:
        return self._body


    _default_value: Optional[JLeftPadded[Expression]]

    @property
    def default_value(self) -> Optional[Expression]:
        return self._default_value.element if self._default_value else None


    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type


    @dataclass
    class PaddingHelper:
        _t: MethodDeclaration

        @property
        def type_parameters(self) -> Optional[TypeParameters]:
            return self._t._type_parameters

        @property
        def name(self) -> Identifier:
            return self._t._name

        @property
        def name_annotations(self) -> List[Annotation]:
            return self._t._name_annotations

        @property
        def parameters(self) -> JContainer[Statement]:
            return self._t._parameters

        @property
        def throws(self) -> Optional[JContainer[NameTree]]:
            return self._t._throws

        @property
        def default_value(self) -> Optional[JLeftPadded[Expression]]:
            return self._t._default_value

        def replace(self, **kwargs) -> MethodDeclaration:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MethodDeclaration.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MethodDeclaration.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    @dataclass
    class AnnotationsHelper:
        _t: MethodDeclaration

        @property
        def type_parameters(self) -> Optional[TypeParameters]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[TypeParameters]) -> MethodDeclaration:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

        @property
        def name(self) -> Identifier:
            return self._t._name

        def with_name(self, name: Identifier) -> MethodDeclaration:
            return self._t if self._t._name is name else replace(self._t, _name=name)

        @property
        def name_annotations(self) -> List[Annotation]:
            return self._t._name_annotations

        def with_name_annotations(self, name_annotations: List[Annotation]) -> MethodDeclaration:
            return self._t if self._t._name_annotations is name_annotations else replace(self._t, _name_annotations=name_annotations)

        @property
        def parameters(self) -> JContainer[Statement]:
            return self._t._parameters

        def with_parameters(self, parameters: JContainer[Statement]) -> MethodDeclaration:
            return self._t if self._t._parameters is parameters else replace(self._t, _parameters=parameters)

        @property
        def throws(self) -> Optional[JContainer[NameTree]]:
            return self._t._throws

        def with_throws(self, throws: Optional[JContainer[NameTree]]) -> MethodDeclaration:
            return self._t if self._t._throws is throws else replace(self._t, _throws=throws)

        @property
        def default_value(self) -> Optional[JLeftPadded[Expression]]:
            return self._t._default_value

        def with_default_value(self, default_value: Optional[JLeftPadded[Expression]]) -> MethodDeclaration:
            return self._t if self._t._default_value is default_value else replace(self._t, _default_value=default_value)

    _annotations: Optional[weakref.ReferenceType[AnnotationsHelper]] = None

    @property
    def annotations(self) -> AnnotationsHelper:
        if self._annotations is None:
            p = MethodDeclaration.AnnotationsHelper(self)
            object.__setattr__(self, '_annotations', weakref.ref(p))
        else:
            p = self._annotations()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MethodDeclaration.AnnotationsHelper(self)
                object.__setattr__(self, '_annotations', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_method_declaration(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MethodInvocation(Statement, TypedTree, MethodCall):
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


    _select: Optional[JRightPadded[Expression]]

    @property
    def select(self) -> Optional[Expression]:
        return self._select.element if self._select else None


    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements if self._type_parameters else None


    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name

    _arguments: JContainer[Expression]

    @property
    def arguments(self) -> List[Expression]:
        return self._arguments.elements


    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type


    @dataclass
    class PaddingHelper:
        _t: MethodInvocation

        @property
        def select(self) -> Optional[JRightPadded[Expression]]:
            return self._t._select

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        @property
        def arguments(self) -> JContainer[Expression]:
            return self._t._arguments

        def replace(self, **kwargs) -> MethodInvocation:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MethodInvocation.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MethodInvocation.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_method_invocation(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Modifier(J):
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


    _keyword: Optional[str]

    @property
    def keyword(self) -> Optional[str]:
        return self._keyword


    _type: Type

    @property
    def type(self) -> Type:
        return self._type


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    class Type(Enum):
        Default = 0
        Public = 1
        Protected = 2
        Private = 3
        Abstract = 4
        Static = 5
        Final = 6
        Sealed = 7
        NonSealed = 8
        Transient = 9
        Volatile = 10
        Synchronized = 11
        Native = 12
        Strictfp = 13
        Async = 14
        Reified = 15
        Inline = 16
        LanguageExtension = 17

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_modifier(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class MultiCatch(TypeTree):
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


    _alternatives: List[JRightPadded[NameTree]]

    @property
    def alternatives(self) -> List[NameTree]:
        return JRightPadded.get_elements(self._alternatives)


    @dataclass
    class PaddingHelper:
        _t: MultiCatch

        @property
        def alternatives(self) -> List[JRightPadded[NameTree]]:
            return self._t._alternatives

        def replace(self, **kwargs) -> MultiCatch:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = MultiCatch.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = MultiCatch.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_multi_catch(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class NewArray(Expression, TypedTree):
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


    _type_expression: Optional[TypeTree]

    @property
    def type_expression(self) -> Optional[TypeTree]:
        return self._type_expression


    _dimensions: List[ArrayDimension]

    @property
    def dimensions(self) -> List[ArrayDimension]:
        return self._dimensions


    _initializer: Optional[JContainer[Expression]]

    @property
    def initializer(self) -> Optional[List[Expression]]:
        return self._initializer.elements if self._initializer else None


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: NewArray

        @property
        def initializer(self) -> Optional[JContainer[Expression]]:
            return self._t._initializer

        def replace(self, **kwargs) -> NewArray:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = NewArray.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = NewArray.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_new_array(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ArrayDimension(J):
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


    _index: JRightPadded[Expression]

    @property
    def index(self) -> Expression:
        return self._index.element


    @dataclass
    class PaddingHelper:
        _t: ArrayDimension

        @property
        def index(self) -> JRightPadded[Expression]:
            return self._t._index

        def replace(self, **kwargs) -> ArrayDimension:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ArrayDimension.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ArrayDimension.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_array_dimension(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class NewClass(Statement, TypedTree, MethodCall):
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


    _enclosing: Optional[JRightPadded[Expression]]

    @property
    def enclosing(self) -> Optional[Expression]:
        return self._enclosing.element if self._enclosing else None


    _new: Space

    @property
    def new(self) -> Space:
        return self._new


    _clazz: Optional[TypeTree]

    @property
    def clazz(self) -> Optional[TypeTree]:
        return self._clazz


    _arguments: JContainer[Expression]

    @property
    def arguments(self) -> List[Expression]:
        return self._arguments.elements


    _body: Optional[Block]

    @property
    def body(self) -> Optional[Block]:
        return self._body


    _constructor_type: Optional[JavaType.Method]

    @property
    def constructor_type(self) -> Optional[JavaType.Method]:
        return self._constructor_type


    @dataclass
    class PaddingHelper:
        _t: NewClass

        @property
        def enclosing(self) -> Optional[JRightPadded[Expression]]:
            return self._t._enclosing

        @property
        def arguments(self) -> JContainer[Expression]:
            return self._t._arguments

        def replace(self, **kwargs) -> NewClass:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = NewClass.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = NewClass.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_new_class(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class NullableType(TypeTree, Expression):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _type_tree: JRightPadded[TypeTree]

    @property
    def type_tree(self) -> TypeTree:
        return self._type_tree.element


    @dataclass
    class PaddingHelper:
        _t: NullableType

        @property
        def type_tree(self) -> JRightPadded[TypeTree]:
            return self._t._type_tree

        def replace(self, **kwargs) -> NullableType:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = NullableType.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = NullableType.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_nullable_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Package(Statement):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_package(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ParameterizedType(TypeTree, Expression):
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


    _clazz: NameTree

    @property
    def clazz(self) -> NameTree:
        return self._clazz


    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements if self._type_parameters else None


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: ParameterizedType

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        def replace(self, **kwargs) -> ParameterizedType:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ParameterizedType.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ParameterizedType.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_parameterized_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Parentheses(Expression, Generic[J2]):
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


    _tree: JRightPadded[J2]

    @property
    def tree(self) -> J2:
        return self._tree.element

    @property
    def type(self) -> Optional[JavaType]:
        if isinstance(self.tree, (Expression, TypedTree)):
            return self.tree.type
        return None

    @dataclass
    class PaddingHelper:
        _t: Parentheses[J2]

        @property
        def tree(self) -> JRightPadded[J2]:
            return self._t._tree

        def replace(self, **kwargs) -> Parentheses[J2]:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Parentheses[J2].PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Parentheses[J2].PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_parentheses(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ControlParentheses(Expression, Generic[J2]):
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


    _tree: JRightPadded[J2]

    @property
    def tree(self) -> J2:
        return self._tree.element


    @dataclass
    class PaddingHelper:
        _t: ControlParentheses[J2]

        @property
        def tree(self) -> JRightPadded[J2]:
            return self._t._tree

        def replace(self, **kwargs) -> ControlParentheses[J2]:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = ControlParentheses[J2].PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = ControlParentheses[J2].PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_control_parentheses(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Primitive(TypeTree, Expression):
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


    _type: JavaType.Primitive

    @property
    def type(self) -> JavaType.Primitive:
        return self._type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_primitive(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Return(Statement):
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


    _expression: Optional[Expression]

    @property
    def expression(self) -> Optional[Expression]:
        return self._expression


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_return(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Switch(Statement):
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


    _selector: ControlParentheses[Expression]

    @property
    def selector(self) -> ControlParentheses[Expression]:
        return self._selector


    _cases: Block

    @property
    def cases(self) -> Block:
        return self._cases


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_switch(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class SwitchExpression(Expression, TypedTree):
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


    _selector: ControlParentheses[Expression]

    @property
    def selector(self) -> ControlParentheses[Expression]:
        return self._selector


    _cases: Block

    @property
    def cases(self) -> Block:
        return self._cases


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_switch_expression(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Synchronized(Statement):
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


    _lock: ControlParentheses[Expression]

    @property
    def lock(self) -> ControlParentheses[Expression]:
        return self._lock


    _body: Block

    @property
    def body(self) -> Block:
        return self._body


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_synchronized(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Ternary(Expression, Statement, TypedTree):
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


    _condition: Expression

    @property
    def condition(self) -> Expression:
        return self._condition


    _true_part: JLeftPadded[Expression]

    @property
    def true_part(self) -> Expression:
        return self._true_part.element


    _false_part: JLeftPadded[Expression]

    @property
    def false_part(self) -> Expression:
        return self._false_part.element


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    @dataclass
    class PaddingHelper:
        _t: Ternary

        @property
        def true_part(self) -> JLeftPadded[Expression]:
            return self._t._true_part

        @property
        def false_part(self) -> JLeftPadded[Expression]:
            return self._t._false_part

        def replace(self, **kwargs) -> Ternary:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Ternary.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Ternary.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_ternary(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Throw(Statement):
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


    _exception: Expression

    @property
    def exception(self) -> Expression:
        return self._exception


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_throw(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Try(Statement):
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


    _resources: Optional[JContainer[Resource]]

    @property
    def resources(self) -> Optional[List[Resource]]:
        return self._resources.elements if self._resources else None


    _body: Block

    @property
    def body(self) -> Block:
        return self._body


    _catches: List[Catch]

    @property
    def catches(self) -> List[Catch]:
        return self._catches


    _finally: Optional[JLeftPadded[Block]]

    @property
    def finally_(self) -> Optional[Block]:
        return self._finally.element if self._finally else None


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Resource(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> Try.Resource:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> Try.Resource:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> Try.Resource:
            return self if markers is self._markers else replace(self, _markers=markers)

        _variable_declarations: TypedTree

        @property
        def variable_declarations(self) -> TypedTree:
            return self._variable_declarations

        def with_variable_declarations(self, variable_declarations: TypedTree) -> Try.Resource:
            return self if variable_declarations is self._variable_declarations else replace(self, _variable_declarations=variable_declarations)

        _terminated_with_semicolon: bool

        @property
        def terminated_with_semicolon(self) -> bool:
            return self._terminated_with_semicolon

        def with_terminated_with_semicolon(self, terminated_with_semicolon: bool) -> Try.Resource:
            return self if terminated_with_semicolon is self._terminated_with_semicolon else replace(self, _terminated_with_semicolon=terminated_with_semicolon)

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_try_resource(self, p)

    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Catch(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> Try.Catch:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> Try.Catch:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> Try.Catch:
            return self if markers is self._markers else replace(self, _markers=markers)

        _parameter: ControlParentheses[VariableDeclarations]

        @property
        def parameter(self) -> ControlParentheses[VariableDeclarations]:
            return self._parameter

        def with_parameter(self, parameter: ControlParentheses[VariableDeclarations]) -> Try.Catch:
            return self if parameter is self._parameter else replace(self, _parameter=parameter)

        _body: Block

        @property
        def body(self) -> Block:
            return self._body

        def with_body(self, body: Block) -> Try.Catch:
            return self if body is self._body else replace(self, _body=body)

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_catch(self, p)

    @dataclass
    class PaddingHelper:
        _t: Try

        @property
        def resources(self) -> Optional[JContainer[Try.Resource]]:
            return self._t._resources

        @property
        def finally_(self) -> Optional[JLeftPadded[Block]]:
            return self._t._finally

        def replace(self, **kwargs) -> Try:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Try.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Try.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_try(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeCast(Expression, TypedTree):
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


    _clazz: ControlParentheses[TypeTree]

    @property
    def clazz(self) -> ControlParentheses[TypeTree]:
        return self._clazz


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_type_cast(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeParameter(J):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers


    _name: Expression

    @property
    def name(self) -> Expression:
        return self._name


    _bounds: Optional[JContainer[TypeTree]]

    @property
    def bounds(self) -> Optional[List[TypeTree]]:
        return self._bounds.elements if self._bounds else None


    @dataclass
    class PaddingHelper:
        _t: TypeParameter

        @property
        def bounds(self) -> Optional[JContainer[TypeTree]]:
            return self._t._bounds

        def replace(self, **kwargs) -> TypeParameter:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = TypeParameter.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = TypeParameter.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_type_parameter(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeParameters(J):
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


    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations


    _type_parameters: List[JRightPadded[TypeParameter]]

    @property
    def type_parameters(self) -> List[TypeParameter]:
        return JRightPadded.get_elements(self._type_parameters)


    @dataclass
    class PaddingHelper:
        _t: TypeParameters

        @property
        def type_parameters(self) -> List[JRightPadded[TypeParameter]]:
            return self._t._type_parameters

        def replace(self, **kwargs) -> TypeParameters:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = TypeParameters.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = TypeParameters.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_type_parameters(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Unary(Statement, Expression, TypedTree):
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


    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element


    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression


    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type


    class Type(Enum):
        PreIncrement = 0
        PreDecrement = 1
        PostIncrement = 2
        PostDecrement = 3
        Positive = 4
        Negative = 5
        Complement = 6
        Not = 7

    @dataclass
    class PaddingHelper:
        _t: Unary

        @property
        def operator(self) -> JLeftPadded[Unary.Type]:
            return self._t._operator

        def replace(self, **kwargs) -> Unary:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Unary.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Unary.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_unary(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class VariableDeclarations(Statement, TypedTree):
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


    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations


    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers


    _type_expression: Optional[TypeTree]

    @property
    def type_expression(self) -> Optional[TypeTree]:
        return self._type_expression


    _varargs: Optional[Space]

    @property
    def varargs(self) -> Optional[Space]:
        return self._varargs


    _dimensions_before_name: List[JLeftPadded[Space]]

    @property
    def dimensions_before_name(self) -> List[JLeftPadded[Space]]:
        return self._dimensions_before_name


    _variables: List[JRightPadded[NamedVariable]]

    @property
    def variables(self) -> List[NamedVariable]:
        return JRightPadded.get_elements(self._variables)


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class NamedVariable(NameTree):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> VariableDeclarations.NamedVariable:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> VariableDeclarations.NamedVariable:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> VariableDeclarations.NamedVariable:
            return self if markers is self._markers else replace(self, _markers=markers)

        _name: Identifier

        @property
        def name(self) -> Identifier:
            return self._name

        def with_name(self, name: Identifier) -> VariableDeclarations.NamedVariable:
            return self if name is self._name else replace(self, _name=name)

        _dimensions_after_name: List[JLeftPadded[Space]]

        @property
        def dimensions_after_name(self) -> List[JLeftPadded[Space]]:
            return self._dimensions_after_name

        def with_dimensions_after_name(self, dimensions_after_name: List[JLeftPadded[Space]]) -> VariableDeclarations.NamedVariable:
            return self if dimensions_after_name is self._dimensions_after_name else replace(self, _dimensions_after_name=dimensions_after_name)

        _initializer: Optional[JLeftPadded[Expression]]

        @property
        def initializer(self) -> Optional[Expression]:
            return self._initializer.element if self._initializer else None

        def with_initializer(self, initializer: Optional[Expression]) -> VariableDeclarations.NamedVariable:
            return self.padding.replace(initializer=self._initializer.replace(element=initializer) if self._initializer else None)

        _variable_type: Optional[JavaType.Variable]

        @property
        def variable_type(self) -> Optional[JavaType.Variable]:
            return self._variable_type

        def with_variable_type(self, variable_type: Optional[JavaType.Variable]) -> VariableDeclarations.NamedVariable:
            return self if variable_type is self._variable_type else replace(self, _variable_type=variable_type)

        @dataclass
        class PaddingHelper:
            _t: VariableDeclarations.NamedVariable

            @property
            def initializer(self) -> Optional[JLeftPadded[Expression]]:
                return self._t._initializer

            def replace(self, **kwargs) -> VariableDeclarations.NamedVariable:
                return replace_if_changed(self._t, **kwargs)

        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

        @property
        def padding(self) -> PaddingHelper:
            if self._padding is None:
                p = VariableDeclarations.NamedVariable.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
            else:
                p = self._padding()
                # noinspection PyProtectedMember
                if p is None or p._t != self:
                    p = VariableDeclarations.NamedVariable.PaddingHelper(self)
                    object.__setattr__(self, '_padding', weakref.ref(p))
            return p

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_variable(self, p)

    @dataclass
    class PaddingHelper:
        _t: VariableDeclarations

        @property
        def variables(self) -> List[JRightPadded[VariableDeclarations.NamedVariable]]:
            return self._t._variables

        def replace(self, **kwargs) -> VariableDeclarations:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = VariableDeclarations.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = VariableDeclarations.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_variable_declarations(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class WhileLoop(Loop):
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


    _condition: ControlParentheses[Expression]

    @property
    def condition(self) -> ControlParentheses[Expression]:
        return self._condition


    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element


    @dataclass
    class PaddingHelper:
        _t: WhileLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        def replace(self, **kwargs) -> WhileLoop:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = WhileLoop.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = WhileLoop.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_while_loop(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Wildcard(Expression, TypeTree):
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


    _bound: Optional[JLeftPadded[Bound]]

    @property
    def bound(self) -> Optional[Bound]:
        return self._bound.element if self._bound else None


    _bounded_type: Optional[NameTree]

    @property
    def bounded_type(self) -> Optional[NameTree]:
        return self._bounded_type


    class Bound(Enum):
        Extends = 0
        Super = 1

    @dataclass
    class PaddingHelper:
        _t: Wildcard

        @property
        def bound(self) -> Optional[JLeftPadded[Wildcard.Bound]]:
            return self._t._bound

        def replace(self, **kwargs) -> Wildcard:
            return replace_if_changed(self._t, **kwargs)

    _padding: Optional[weakref.ReferenceType[PaddingHelper]] = None

    @property
    def padding(self) -> PaddingHelper:
        if self._padding is None:
            p = Wildcard.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = Wildcard.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_wildcard(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Yield(Statement):
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


    _implicit: bool

    @property
    def implicit(self) -> bool:
        return self._implicit


    _value: Expression

    @property
    def value(self) -> Expression:
        return self._value


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_yield(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Unknown(Statement, Expression, TypeTree):
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


    _source: Source

    @property
    def source(self) -> Source:
        return self._source


    # noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
    @dataclass(frozen=True, eq=False)
    class Source(J):
        _id: UUID

        @property
        def id(self) -> UUID:
            return self._id

        def with_id(self, id: UUID) -> Unknown.Source:
            return self if id is self._id else replace(self, _id=id)

        _prefix: Space

        @property
        def prefix(self) -> Space:
            return self._prefix

        def with_prefix(self, prefix: Space) -> Unknown.Source:
            return self if prefix is self._prefix else replace(self, _prefix=prefix)

        _markers: Markers

        @property
        def markers(self) -> Markers:
            return self._markers

        def with_markers(self, markers: Markers) -> Unknown.Source:
            return self if markers is self._markers else replace(self, _markers=markers)

        _text: str

        @property
        def text(self) -> str:
            return self._text

        def with_text(self, text: str) -> Unknown.Source:
            return self if text is self._text else replace(self, _text=text)

        def accept_java(self, v: JavaVisitor[P], p: P) -> J:
            return v.visit_unknown_source(self, p)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_unknown(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Erroneous(Statement, Expression):
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


    _text: str

    @property
    def text(self) -> str:
        return self._text


    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_erroneous(self, p)
