from __future__ import annotations

import weakref
from dataclasses import dataclass, replace
from enum import Enum
from pathlib import Path
from typing import List, Optional, TYPE_CHECKING
from uuid import UUID

if TYPE_CHECKING:
    from .visitor import JavaVisitor
from . import extensions
from .support_types import *
from rewrite import Checksum, FileAttributes, SourceFile, Tree, TreeVisitor, Markers, Cursor, PrintOutputCapture, PrinterFactory

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class AnnotatedType(Expression, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> AnnotatedType:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> AnnotatedType:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> AnnotatedType:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> AnnotatedType:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _type_expression: TypeTree

    @property
    def type_expression(self) -> TypeTree:
        return self._type_expression

    def with_type_expression(self, type_expression: TypeTree) -> AnnotatedType:
        return self if type_expression is self._type_expression else replace(self, _type_expression=type_expression)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_annotated_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Annotation(Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Annotation:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Annotation:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Annotation:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotation_type: NameTree

    @property
    def annotation_type(self) -> NameTree:
        return self._annotation_type

    def with_annotation_type(self, annotation_type: NameTree) -> Annotation:
        return self if annotation_type is self._annotation_type else replace(self, _annotation_type=annotation_type)

    _arguments: Optional[JContainer[Expression]]

    @property
    def arguments(self) -> Optional[List[Expression]]:
        return self._arguments.elements

    def with_arguments(self, arguments: Optional[List[Expression]]) -> Annotation:
        return self.padding.with_arguments(JContainer.with_elements_nullable(self._arguments, arguments))

    @dataclass
    class PaddingHelper:
        _t: Annotation

        @property
        def arguments(self) -> Optional[JContainer[Expression]]:
            return self._t._arguments

        def with_arguments(self, arguments: Optional[JContainer[Expression]]) -> Annotation:
            return self._t if self._t._arguments is arguments else replace(self._t, _arguments=arguments)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Annotation.PaddingHelper
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

    def with_id(self, id: UUID) -> ArrayAccess:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ArrayAccess:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ArrayAccess:
        return self if markers is self._markers else replace(self, _markers=markers)

    _indexed: Expression

    @property
    def indexed(self) -> Expression:
        return self._indexed

    def with_indexed(self, indexed: Expression) -> ArrayAccess:
        return self if indexed is self._indexed else replace(self, _indexed=indexed)

    _dimension: ArrayDimension

    @property
    def dimension(self) -> ArrayDimension:
        return self._dimension

    def with_dimension(self, dimension: ArrayDimension) -> ArrayAccess:
        return self if dimension is self._dimension else replace(self, _dimension=dimension)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> ArrayAccess:
        return self if type is self._type else replace(self, _type=type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_array_access(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ArrayType(TypeTree, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> ArrayType:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ArrayType:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ArrayType:
        return self if markers is self._markers else replace(self, _markers=markers)

    _element_type: TypeTree

    @property
    def element_type(self) -> TypeTree:
        return self._element_type

    def with_element_type(self, element_type: TypeTree) -> ArrayType:
        return self if element_type is self._element_type else replace(self, _element_type=element_type)

    _annotations: Optional[List[Annotation]]

    @property
    def annotations(self) -> Optional[List[Annotation]]:
        return self._annotations

    def with_annotations(self, annotations: Optional[List[Annotation]]) -> ArrayType:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _dimension: Optional[JLeftPadded[Space]]

    @property
    def dimension(self) -> Optional[JLeftPadded[Space]]:
        return self._dimension

    def with_dimension(self, dimension: Optional[JLeftPadded[Space]]) -> ArrayType:
        return self if dimension is self._dimension else replace(self, _dimension=dimension)

    _type: JavaType

    @property
    def type(self) -> JavaType:
        return self._type

    def with_type(self, type: JavaType) -> ArrayType:
        return self if type is self._type else replace(self, _type=type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_array_type(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Assert(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Assert:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Assert:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Assert:
        return self if markers is self._markers else replace(self, _markers=markers)

    _condition: Expression

    @property
    def condition(self) -> Expression:
        return self._condition

    def with_condition(self, condition: Expression) -> Assert:
        return self if condition is self._condition else replace(self, _condition=condition)

    _detail: Optional[JLeftPadded[Expression]]

    @property
    def detail(self) -> Optional[JLeftPadded[Expression]]:
        return self._detail

    def with_detail(self, detail: Optional[JLeftPadded[Expression]]) -> Assert:
        return self if detail is self._detail else replace(self, _detail=detail)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_assert(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Assignment(Statement, Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Assignment:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Assignment:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Assignment:
        return self if markers is self._markers else replace(self, _markers=markers)

    _variable: Expression

    @property
    def variable(self) -> Expression:
        return self._variable

    def with_variable(self, variable: Expression) -> Assignment:
        return self if variable is self._variable else replace(self, _variable=variable)

    _assignment: JLeftPadded[Expression]

    @property
    def assignment(self) -> Expression:
        return self._assignment.element

    def with_assignment(self, assignment: Expression) -> Assignment:
        return self.padding.with_assignment(JLeftPadded.with_element(self._assignment, assignment))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Assignment:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: Assignment

        @property
        def assignment(self) -> JLeftPadded[Expression]:
            return self._t._assignment

        def with_assignment(self, assignment: JLeftPadded[Expression]) -> Assignment:
            return self._t if self._t._assignment is assignment else replace(self._t, _assignment=assignment)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Assignment.PaddingHelper
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

    def with_id(self, id: UUID) -> AssignmentOperation:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> AssignmentOperation:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> AssignmentOperation:
        return self if markers is self._markers else replace(self, _markers=markers)

    _variable: Expression

    @property
    def variable(self) -> Expression:
        return self._variable

    def with_variable(self, variable: Expression) -> AssignmentOperation:
        return self if variable is self._variable else replace(self, _variable=variable)

    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element

    def with_operator(self, operator: Type) -> AssignmentOperation:
        return self.padding.with_operator(JLeftPadded.with_element(self._operator, operator))

    _assignment: Expression

    @property
    def assignment(self) -> Expression:
        return self._assignment

    def with_assignment(self, assignment: Expression) -> AssignmentOperation:
        return self if assignment is self._assignment else replace(self, _assignment=assignment)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> AssignmentOperation:
        return self if type is self._type else replace(self, _type=type)

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

        def with_operator(self, operator: JLeftPadded[AssignmentOperation.Type]) -> AssignmentOperation:
            return self._t if self._t._operator is operator else replace(self._t, _operator=operator)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: AssignmentOperation.PaddingHelper
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

    def with_id(self, id: UUID) -> Binary:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Binary:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Binary:
        return self if markers is self._markers else replace(self, _markers=markers)

    _left: Expression

    @property
    def left(self) -> Expression:
        return self._left

    def with_left(self, left: Expression) -> Binary:
        return self if left is self._left else replace(self, _left=left)

    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element

    def with_operator(self, operator: Type) -> Binary:
        return self.padding.with_operator(JLeftPadded.with_element(self._operator, operator))

    _right: Expression

    @property
    def right(self) -> Expression:
        return self._right

    def with_right(self, right: Expression) -> Binary:
        return self if right is self._right else replace(self, _right=right)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Binary:
        return self if type is self._type else replace(self, _type=type)

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

        def with_operator(self, operator: JLeftPadded[Binary.Type]) -> Binary:
            return self._t if self._t._operator is operator else replace(self._t, _operator=operator)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Binary.PaddingHelper
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

    def with_id(self, id: UUID) -> Block:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Block:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Block:
        return self if markers is self._markers else replace(self, _markers=markers)

    _static: JRightPadded[bool]

    @property
    def static(self) -> bool:
        return self._static.element

    def with_static(self, static: bool) -> Block:
        return self.padding.with_static(JRightPadded.with_element(self._static, static))

    _statements: List[JRightPadded[Statement]]

    @property
    def statements(self) -> List[Statement]:
        return JRightPadded.get_elements(self._statements)

    def with_statements(self, statements: List[Statement]) -> Block:
        return self.padding.with_statements(JRightPadded.with_elements(self._statements, statements))

    _end: Space

    @property
    def end(self) -> Space:
        return self._end

    def with_end(self, end: Space) -> Block:
        return self if end is self._end else replace(self, _end=end)

    @dataclass
    class PaddingHelper:
        _t: Block

        @property
        def static(self) -> JRightPadded[bool]:
            return self._t._static

        def with_static(self, static: JRightPadded[bool]) -> Block:
            return self._t if self._t._static is static else replace(self._t, _static=static)

        @property
        def statements(self) -> List[JRightPadded[Statement]]:
            return self._t._statements

        def with_statements(self, statements: List[JRightPadded[Statement]]) -> Block:
            return self._t if self._t._statements is statements else replace(self._t, _statements=statements)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Block.PaddingHelper
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

    def get_coordinates(self) -> CoordinateBuilder.Block:
        return CoordinateBuilder.Block(self)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Break(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Break:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Break:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Break:
        return self if markers is self._markers else replace(self, _markers=markers)

    _label: Optional[Identifier]

    @property
    def label(self) -> Optional[Identifier]:
        return self._label

    def with_label(self, label: Optional[Identifier]) -> Break:
        return self if label is self._label else replace(self, _label=label)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_break(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Case(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Case:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Case:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Case:
        return self if markers is self._markers else replace(self, _markers=markers)

    _type: Type

    @property
    def type(self) -> Type:
        return self._type

    def with_type(self, type: Type) -> Case:
        return self if type is self._type else replace(self, _type=type)

    _case_labels: JContainer[J]

    @property
    def case_labels(self) -> List[J]:
        return self._case_labels.elements

    def with_case_labels(self, case_labels: List[J]) -> Case:
        return self.padding.with_case_labels(JContainer.with_elements(self._case_labels, case_labels))

    _statements: JContainer[Statement]

    @property
    def statements(self) -> List[Statement]:
        return self._statements.elements

    def with_statements(self, statements: List[Statement]) -> Case:
        return self.padding.with_statements(JContainer.with_elements(self._statements, statements))

    _body: Optional[JRightPadded[J]]

    @property
    def body(self) -> Optional[J]:
        return self._body.element if self._body else None

    def with_body(self, body: Optional[J]) -> Case:
        return self.padding.with_body(JRightPadded.with_element(self._body, body))

    _guard: Optional[Expression]

    @property
    def guard(self) -> Optional[Expression]:
        return self._guard

    def with_guard(self, guard: Optional[Expression]) -> Case:
        return self if guard is self._guard else replace(self, _guard=guard)

    class Type(Enum):
        Statement = 0
        Rule = 1

    @dataclass
    class PaddingHelper:
        _t: Case

        @property
        def case_labels(self) -> JContainer[J]:
            return self._t._case_labels

        def with_case_labels(self, case_labels: JContainer[J]) -> Case:
            return self._t if self._t._case_labels is case_labels else replace(self._t, _case_labels=case_labels)

        @property
        def statements(self) -> JContainer[Statement]:
            return self._t._statements

        def with_statements(self, statements: JContainer[Statement]) -> Case:
            return self._t if self._t._statements is statements else replace(self._t, _statements=statements)

        @property
        def body(self) -> Optional[JRightPadded[J]]:
            return self._t._body

        def with_body(self, body: Optional[JRightPadded[J]]) -> Case:
            return self._t if self._t._body is body else replace(self._t, _body=body)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Case.PaddingHelper
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

    def with_id(self, id: UUID) -> ClassDeclaration:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ClassDeclaration:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ClassDeclaration:
        return self if markers is self._markers else replace(self, _markers=markers)

    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations

    def with_leading_annotations(self, leading_annotations: List[Annotation]) -> ClassDeclaration:
        return self if leading_annotations is self._leading_annotations else replace(self, _leading_annotations=leading_annotations)

    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers

    def with_modifiers(self, modifiers: List[Modifier]) -> ClassDeclaration:
        return self if modifiers is self._modifiers else replace(self, _modifiers=modifiers)

    _kind: Kind

    def with_kind(self, kind: Kind) -> ClassDeclaration:
        return self if kind is self._kind else replace(self, _kind=kind)

    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name

    def with_name(self, name: Identifier) -> ClassDeclaration:
        return self if name is self._name else replace(self, _name=name)

    _type_parameters: Optional[JContainer[TypeParameter]]

    @property
    def type_parameters(self) -> Optional[List[TypeParameter]]:
        return self._type_parameters.elements

    def with_type_parameters(self, type_parameters: Optional[List[TypeParameter]]) -> ClassDeclaration:
        return self.padding.with_type_parameters(JContainer.with_elements_nullable(self._type_parameters, type_parameters))

    _primary_constructor: Optional[JContainer[Statement]]

    @property
    def primary_constructor(self) -> Optional[List[Statement]]:
        return self._primary_constructor.elements

    def with_primary_constructor(self, primary_constructor: Optional[List[Statement]]) -> ClassDeclaration:
        return self.padding.with_primary_constructor(JContainer.with_elements_nullable(self._primary_constructor, primary_constructor))

    _extends: Optional[JLeftPadded[TypeTree]]

    @property
    def extends(self) -> Optional[TypeTree]:
        return self._extends.element if self._extends else None

    def with_extends(self, extends: Optional[TypeTree]) -> ClassDeclaration:
        return self.padding.with_extends(JLeftPadded.with_element(self._extends, extends))

    _implements: Optional[JContainer[TypeTree]]

    @property
    def implements(self) -> Optional[List[TypeTree]]:
        return self._implements.elements

    def with_implements(self, implements: Optional[List[TypeTree]]) -> ClassDeclaration:
        return self.padding.with_implements(JContainer.with_elements_nullable(self._implements, implements))

    _permits: Optional[JContainer[TypeTree]]

    @property
    def permits(self) -> Optional[List[TypeTree]]:
        return self._permits.elements

    def with_permits(self, permits: Optional[List[TypeTree]]) -> ClassDeclaration:
        return self.padding.with_permits(JContainer.with_elements_nullable(self._permits, permits))

    _body: Block

    @property
    def body(self) -> Block:
        return self._body

    def with_body(self, body: Block) -> ClassDeclaration:
        return self if body is self._body else replace(self, _body=body)

    _type: Optional[JavaType.FullyQualified]

    @property
    def type(self) -> Optional[JavaType.FullyQualified]:
        return self._type

    def with_type(self, type: Optional[JavaType.FullyQualified]) -> ClassDeclaration:
        return self if type is self._type else replace(self, _type=type)

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

        def with_kind(self, kind: ClassDeclaration.Kind) -> ClassDeclaration:
            return self._t if self._t._kind is kind else replace(self._t, _kind=kind)

        @property
        def type_parameters(self) -> Optional[JContainer[TypeParameter]]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[JContainer[TypeParameter]]) -> ClassDeclaration:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

        @property
        def primary_constructor(self) -> Optional[JContainer[Statement]]:
            return self._t._primary_constructor

        def with_primary_constructor(self, primary_constructor: Optional[JContainer[Statement]]) -> ClassDeclaration:
            return self._t if self._t._primary_constructor is primary_constructor else replace(self._t, _primary_constructor=primary_constructor)

        @property
        def extends(self) -> Optional[JLeftPadded[TypeTree]]:
            return self._t._extends

        def with_extends(self, extends: Optional[JLeftPadded[TypeTree]]) -> ClassDeclaration:
            return self._t if self._t._extends is extends else replace(self._t, _extends=extends)

        @property
        def implements(self) -> Optional[JContainer[TypeTree]]:
            return self._t._implements

        def with_implements(self, implements: Optional[JContainer[TypeTree]]) -> ClassDeclaration:
            return self._t if self._t._implements is implements else replace(self._t, _implements=implements)

        @property
        def permits(self) -> Optional[JContainer[TypeTree]]:
            return self._t._permits

        def with_permits(self, permits: Optional[JContainer[TypeTree]]) -> ClassDeclaration:
            return self._t if self._t._permits is permits else replace(self._t, _permits=permits)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ClassDeclaration.PaddingHelper
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

    def with_id(self, id: UUID) -> CompilationUnit:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> CompilationUnit:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> CompilationUnit:
        return self if markers is self._markers else replace(self, _markers=markers)

    _source_path: Path

    @property
    def source_path(self) -> Path:
        return self._source_path

    def with_source_path(self, source_path: Path) -> CompilationUnit:
        return self if source_path is self._source_path else replace(self, _source_path=source_path)

    _file_attributes: Optional[FileAttributes]

    @property
    def file_attributes(self) -> Optional[FileAttributes]:
        return self._file_attributes

    def with_file_attributes(self, file_attributes: Optional[FileAttributes]) -> CompilationUnit:
        return self if file_attributes is self._file_attributes else replace(self, _file_attributes=file_attributes)

    _charset_name: Optional[str]

    @property
    def charset_name(self) -> Optional[str]:
        return self._charset_name

    def with_charset_name(self, charset_name: Optional[str]) -> CompilationUnit:
        return self if charset_name is self._charset_name else replace(self, _charset_name=charset_name)

    _charset_bom_marked: bool

    @property
    def charset_bom_marked(self) -> bool:
        return self._charset_bom_marked

    def with_charset_bom_marked(self, charset_bom_marked: bool) -> CompilationUnit:
        return self if charset_bom_marked is self._charset_bom_marked else replace(self, _charset_bom_marked=charset_bom_marked)

    _checksum: Optional[Checksum]

    @property
    def checksum(self) -> Optional[Checksum]:
        return self._checksum

    def with_checksum(self, checksum: Optional[Checksum]) -> CompilationUnit:
        return self if checksum is self._checksum else replace(self, _checksum=checksum)

    _package_declaration: Optional[JRightPadded[Package]]

    @property
    def package_declaration(self) -> Optional[Package]:
        return self._package_declaration.element if self._package_declaration else None

    def with_package_declaration(self, package_declaration: Optional[Package]) -> CompilationUnit:
        return self.padding.with_package_declaration(JRightPadded.with_element(self._package_declaration, package_declaration))

    _imports: List[JRightPadded[Import]]

    @property
    def imports(self) -> List[Import]:
        return JRightPadded.get_elements(self._imports)

    def with_imports(self, imports: List[Import]) -> CompilationUnit:
        return self.padding.with_imports(JRightPadded.with_elements(self._imports, imports))

    _classes: List[ClassDeclaration]

    @property
    def classes(self) -> List[ClassDeclaration]:
        return self._classes

    def with_classes(self, classes: List[ClassDeclaration]) -> CompilationUnit:
        return self if classes is self._classes else replace(self, _classes=classes)

    _eof: Space

    @property
    def eof(self) -> Space:
        return self._eof

    def with_eof(self, eof: Space) -> CompilationUnit:
        return self if eof is self._eof else replace(self, _eof=eof)

    @dataclass
    class PaddingHelper:
        _t: CompilationUnit

        @property
        def package_declaration(self) -> Optional[JRightPadded[Package]]:
            return self._t._package_declaration

        def with_package_declaration(self, package_declaration: Optional[JRightPadded[Package]]) -> CompilationUnit:
            return self._t if self._t._package_declaration is package_declaration else replace(self._t, _package_declaration=package_declaration)

        @property
        def imports(self) -> List[JRightPadded[Import]]:
            return self._t._imports

        def with_imports(self, imports: List[JRightPadded[Import]]) -> CompilationUnit:
            return self._t if self._t._imports is imports else replace(self._t, _imports=imports)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: CompilationUnit.PaddingHelper
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
        from .printer import JavaPrinter
        return JavaPrinter[PrintOutputCapture[P]]()

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_compilation_unit(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Continue(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Continue:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Continue:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Continue:
        return self if markers is self._markers else replace(self, _markers=markers)

    _label: Optional[Identifier]

    @property
    def label(self) -> Optional[Identifier]:
        return self._label

    def with_label(self, label: Optional[Identifier]) -> Continue:
        return self if label is self._label else replace(self, _label=label)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_continue(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class DoWhileLoop(Loop):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> DoWhileLoop:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> DoWhileLoop:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> DoWhileLoop:
        return self if markers is self._markers else replace(self, _markers=markers)

    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element

    def with_body(self, body: Statement) -> DoWhileLoop:
        return self.padding.with_body(JRightPadded.with_element(self._body, body))

    _while_condition: JLeftPadded[ControlParentheses[Expression]]

    @property
    def while_condition(self) -> ControlParentheses[Expression]:
        return self._while_condition.element

    def with_while_condition(self, while_condition: ControlParentheses[Expression]) -> DoWhileLoop:
        return self.padding.with_while_condition(JLeftPadded.with_element(self._while_condition, while_condition))

    @dataclass
    class PaddingHelper:
        _t: DoWhileLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        def with_body(self, body: JRightPadded[Statement]) -> DoWhileLoop:
            return self._t if self._t._body is body else replace(self._t, _body=body)

        @property
        def while_condition(self) -> JLeftPadded[ControlParentheses[Expression]]:
            return self._t._while_condition

        def with_while_condition(self, while_condition: JLeftPadded[ControlParentheses[Expression]]) -> DoWhileLoop:
            return self._t if self._t._while_condition is while_condition else replace(self._t, _while_condition=while_condition)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: DoWhileLoop.PaddingHelper
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

    def with_id(self, id: UUID) -> Empty:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Empty:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Empty:
        return self if markers is self._markers else replace(self, _markers=markers)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_empty(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class EnumValue(J):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> EnumValue:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> EnumValue:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> EnumValue:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> EnumValue:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name

    def with_name(self, name: Identifier) -> EnumValue:
        return self if name is self._name else replace(self, _name=name)

    _initializer: Optional[NewClass]

    @property
    def initializer(self) -> Optional[NewClass]:
        return self._initializer

    def with_initializer(self, initializer: Optional[NewClass]) -> EnumValue:
        return self if initializer is self._initializer else replace(self, _initializer=initializer)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_enum_value(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class EnumValueSet(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> EnumValueSet:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> EnumValueSet:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> EnumValueSet:
        return self if markers is self._markers else replace(self, _markers=markers)

    _enums: List[JRightPadded[EnumValue]]

    @property
    def enums(self) -> List[EnumValue]:
        return JRightPadded.get_elements(self._enums)

    def with_enums(self, enums: List[EnumValue]) -> EnumValueSet:
        return self.padding.with_enums(JRightPadded.with_elements(self._enums, enums))

    _terminated_with_semicolon: bool

    @property
    def terminated_with_semicolon(self) -> bool:
        return self._terminated_with_semicolon

    def with_terminated_with_semicolon(self, terminated_with_semicolon: bool) -> EnumValueSet:
        return self if terminated_with_semicolon is self._terminated_with_semicolon else replace(self, _terminated_with_semicolon=terminated_with_semicolon)

    @dataclass
    class PaddingHelper:
        _t: EnumValueSet

        @property
        def enums(self) -> List[JRightPadded[EnumValue]]:
            return self._t._enums

        def with_enums(self, enums: List[JRightPadded[EnumValue]]) -> EnumValueSet:
            return self._t if self._t._enums is enums else replace(self._t, _enums=enums)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: EnumValueSet.PaddingHelper
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

    def with_id(self, id: UUID) -> FieldAccess:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> FieldAccess:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> FieldAccess:
        return self if markers is self._markers else replace(self, _markers=markers)

    _target: Expression

    @property
    def target(self) -> Expression:
        return self._target

    def with_target(self, target: Expression) -> FieldAccess:
        return self if target is self._target else replace(self, _target=target)

    _name: JLeftPadded[Identifier]

    @property
    def name(self) -> Identifier:
        return self._name.element

    def with_name(self, name: Identifier) -> FieldAccess:
        return self.padding.with_name(JLeftPadded.with_element(self._name, name))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> FieldAccess:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: FieldAccess

        @property
        def name(self) -> JLeftPadded[Identifier]:
            return self._t._name

        def with_name(self, name: JLeftPadded[Identifier]) -> FieldAccess:
            return self._t if self._t._name is name else replace(self._t, _name=name)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: FieldAccess.PaddingHelper
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

    def with_id(self, id: UUID) -> ForEachLoop:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ForEachLoop:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ForEachLoop:
        return self if markers is self._markers else replace(self, _markers=markers)

    _control: Control

    @property
    def control(self) -> Control:
        return self._control

    def with_control(self, control: Control) -> ForEachLoop:
        return self if control is self._control else replace(self, _control=control)

    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element

    def with_body(self, body: Statement) -> ForEachLoop:
        return self.padding.with_body(JRightPadded.with_element(self._body, body))

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
            return self.padding.with_variable(JRightPadded.with_element(self._variable, variable))

        _iterable: JRightPadded[Expression]

        @property
        def iterable(self) -> Expression:
            return self._iterable.element

        def with_iterable(self, iterable: Expression) -> ForEachLoop.Control:
            return self.padding.with_iterable(JRightPadded.with_element(self._iterable, iterable))

        @dataclass
        class PaddingHelper:
            _t: ForEachLoop.Control

            @property
            def variable(self) -> JRightPadded[Statement]:
                return self._t._variable

            def with_variable(self, variable: JRightPadded[Statement]) -> ForEachLoop.Control:
                return self._t if self._t._variable is variable else replace(self._t, _variable=variable)

            @property
            def iterable(self) -> JRightPadded[Expression]:
                return self._t._iterable

            def with_iterable(self, iterable: JRightPadded[Expression]) -> ForEachLoop.Control:
                return self._t if self._t._iterable is iterable else replace(self._t, _iterable=iterable)

        _padding: weakref.ReferenceType[PaddingHelper] = None

        @property
        def padding(self) -> PaddingHelper:
            p: ForEachLoop.Control.PaddingHelper
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

        def with_body(self, body: JRightPadded[Statement]) -> ForEachLoop:
            return self._t if self._t._body is body else replace(self._t, _body=body)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ForEachLoop.PaddingHelper
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

    def with_id(self, id: UUID) -> ForLoop:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ForLoop:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ForLoop:
        return self if markers is self._markers else replace(self, _markers=markers)

    _control: Control

    @property
    def control(self) -> Control:
        return self._control

    def with_control(self, control: Control) -> ForLoop:
        return self if control is self._control else replace(self, _control=control)

    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element

    def with_body(self, body: Statement) -> ForLoop:
        return self.padding.with_body(JRightPadded.with_element(self._body, body))

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
            return self.padding.with_init(JRightPadded.with_elements(self._init, init))

        _condition: JRightPadded[Expression]

        @property
        def condition(self) -> Expression:
            return self._condition.element

        def with_condition(self, condition: Expression) -> ForLoop.Control:
            return self.padding.with_condition(JRightPadded.with_element(self._condition, condition))

        _update: List[JRightPadded[Statement]]

        @property
        def update(self) -> List[Statement]:
            return JRightPadded.get_elements(self._update)

        def with_update(self, update: List[Statement]) -> ForLoop.Control:
            return self.padding.with_update(JRightPadded.with_elements(self._update, update))

        @dataclass
        class PaddingHelper:
            _t: ForLoop.Control

            @property
            def init(self) -> List[JRightPadded[Statement]]:
                return self._t._init

            def with_init(self, init: List[JRightPadded[Statement]]) -> ForLoop.Control:
                return self._t if self._t._init is init else replace(self._t, _init=init)

            @property
            def condition(self) -> JRightPadded[Expression]:
                return self._t._condition

            def with_condition(self, condition: JRightPadded[Expression]) -> ForLoop.Control:
                return self._t if self._t._condition is condition else replace(self._t, _condition=condition)

            @property
            def update(self) -> List[JRightPadded[Statement]]:
                return self._t._update

            def with_update(self, update: List[JRightPadded[Statement]]) -> ForLoop.Control:
                return self._t if self._t._update is update else replace(self._t, _update=update)

        _padding: weakref.ReferenceType[PaddingHelper] = None

        @property
        def padding(self) -> PaddingHelper:
            p: ForLoop.Control.PaddingHelper
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

        def with_body(self, body: JRightPadded[Statement]) -> ForLoop:
            return self._t if self._t._body is body else replace(self._t, _body=body)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ForLoop.PaddingHelper
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

    def with_id(self, id: UUID) -> ParenthesizedTypeTree:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ParenthesizedTypeTree:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ParenthesizedTypeTree:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> ParenthesizedTypeTree:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _parenthesized_type: Parentheses[TypeTree]

    @property
    def parenthesized_type(self) -> Parentheses[TypeTree]:
        return self._parenthesized_type

    def with_parenthesized_type(self, parenthesized_type: Parentheses[TypeTree]) -> ParenthesizedTypeTree:
        return self if parenthesized_type is self._parenthesized_type else replace(self, _parenthesized_type=parenthesized_type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_parenthesized_type_tree(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Identifier(TypeTree, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Identifier:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Identifier:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Identifier:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> Identifier:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _simple_name: str

    @property
    def simple_name(self) -> str:
        return self._simple_name

    def with_simple_name(self, simple_name: str) -> Identifier:
        return self if simple_name is self._simple_name else replace(self, _simple_name=simple_name)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Identifier:
        return self if type is self._type else replace(self, _type=type)

    _field_type: Optional[JavaType.Variable]

    @property
    def field_type(self) -> Optional[JavaType.Variable]:
        return self._field_type

    def with_field_type(self, field_type: Optional[JavaType.Variable]) -> Identifier:
        return self if field_type is self._field_type else replace(self, _field_type=field_type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_identifier(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class If(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> If:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> If:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> If:
        return self if markers is self._markers else replace(self, _markers=markers)

    _if_condition: ControlParentheses[Expression]

    @property
    def if_condition(self) -> ControlParentheses[Expression]:
        return self._if_condition

    def with_if_condition(self, if_condition: ControlParentheses[Expression]) -> If:
        return self if if_condition is self._if_condition else replace(self, _if_condition=if_condition)

    _then_part: JRightPadded[Statement]

    @property
    def then_part(self) -> Statement:
        return self._then_part.element

    def with_then_part(self, then_part: Statement) -> If:
        return self.padding.with_then_part(JRightPadded.with_element(self._then_part, then_part))

    _else_part: Optional[Else]

    @property
    def else_part(self) -> Optional[Else]:
        return self._else_part

    def with_else_part(self, else_part: Optional[Else]) -> If:
        return self if else_part is self._else_part else replace(self, _else_part=else_part)

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
            return self.padding.with_body(JRightPadded.with_element(self._body, body))

        @dataclass
        class PaddingHelper:
            _t: If.Else

            @property
            def body(self) -> JRightPadded[Statement]:
                return self._t._body

            def with_body(self, body: JRightPadded[Statement]) -> If.Else:
                return self._t if self._t._body is body else replace(self._t, _body=body)

        _padding: weakref.ReferenceType[PaddingHelper] = None

        @property
        def padding(self) -> PaddingHelper:
            p: If.Else.PaddingHelper
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

        def with_then_part(self, then_part: JRightPadded[Statement]) -> If:
            return self._t if self._t._then_part is then_part else replace(self._t, _then_part=then_part)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: If.PaddingHelper
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

    def with_id(self, id: UUID) -> Import:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Import:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Import:
        return self if markers is self._markers else replace(self, _markers=markers)

    _static: JLeftPadded[bool]

    @property
    def static(self) -> bool:
        return self._static.element

    def with_static(self, static: bool) -> Import:
        return self.padding.with_static(JLeftPadded.with_element(self._static, static))

    _qualid: FieldAccess

    @property
    def qualid(self) -> FieldAccess:
        return self._qualid

    def with_qualid(self, qualid: FieldAccess) -> Import:
        return self if qualid is self._qualid else replace(self, _qualid=qualid)

    _alias: Optional[JLeftPadded[Identifier]]

    @property
    def alias(self) -> Optional[Identifier]:
        return self._alias.element if self._alias else None

    def with_alias(self, alias: Optional[Identifier]) -> Import:
        return self.padding.with_alias(JLeftPadded.with_element(self._alias, alias))

    @dataclass
    class PaddingHelper:
        _t: Import

        @property
        def static(self) -> JLeftPadded[bool]:
            return self._t._static

        def with_static(self, static: JLeftPadded[bool]) -> Import:
            return self._t if self._t._static is static else replace(self._t, _static=static)

        @property
        def alias(self) -> Optional[JLeftPadded[Identifier]]:
            return self._t._alias

        def with_alias(self, alias: Optional[JLeftPadded[Identifier]]) -> Import:
            return self._t if self._t._alias is alias else replace(self._t, _alias=alias)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Import.PaddingHelper
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

    def with_id(self, id: UUID) -> InstanceOf:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> InstanceOf:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> InstanceOf:
        return self if markers is self._markers else replace(self, _markers=markers)

    _expression: JRightPadded[Expression]

    @property
    def expression(self) -> Expression:
        return self._expression.element

    def with_expression(self, expression: Expression) -> InstanceOf:
        return self.padding.with_expression(JRightPadded.with_element(self._expression, expression))

    _clazz: J

    @property
    def clazz(self) -> J:
        return self._clazz

    def with_clazz(self, clazz: J) -> InstanceOf:
        return self if clazz is self._clazz else replace(self, _clazz=clazz)

    _pattern: Optional[J]

    @property
    def pattern(self) -> Optional[J]:
        return self._pattern

    def with_pattern(self, pattern: Optional[J]) -> InstanceOf:
        return self if pattern is self._pattern else replace(self, _pattern=pattern)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> InstanceOf:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: InstanceOf

        @property
        def expression(self) -> JRightPadded[Expression]:
            return self._t._expression

        def with_expression(self, expression: JRightPadded[Expression]) -> InstanceOf:
            return self._t if self._t._expression is expression else replace(self._t, _expression=expression)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: InstanceOf.PaddingHelper
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

    def with_id(self, id: UUID) -> DeconstructionPattern:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> DeconstructionPattern:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> DeconstructionPattern:
        return self if markers is self._markers else replace(self, _markers=markers)

    _deconstructor: Expression

    @property
    def deconstructor(self) -> Expression:
        return self._deconstructor

    def with_deconstructor(self, deconstructor: Expression) -> DeconstructionPattern:
        return self if deconstructor is self._deconstructor else replace(self, _deconstructor=deconstructor)

    _nested: JContainer[J]

    @property
    def nested(self) -> List[J]:
        return self._nested.elements

    def with_nested(self, nested: List[J]) -> DeconstructionPattern:
        return self.padding.with_nested(JContainer.with_elements(self._nested, nested))

    _type: JavaType

    @property
    def type(self) -> JavaType:
        return self._type

    def with_type(self, type: JavaType) -> DeconstructionPattern:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: DeconstructionPattern

        @property
        def nested(self) -> JContainer[J]:
            return self._t._nested

        def with_nested(self, nested: JContainer[J]) -> DeconstructionPattern:
            return self._t if self._t._nested is nested else replace(self._t, _nested=nested)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: DeconstructionPattern.PaddingHelper
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

    def with_id(self, id: UUID) -> IntersectionType:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> IntersectionType:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> IntersectionType:
        return self if markers is self._markers else replace(self, _markers=markers)

    _bounds: JContainer[TypeTree]

    @property
    def bounds(self) -> List[TypeTree]:
        return self._bounds.elements

    def with_bounds(self, bounds: List[TypeTree]) -> IntersectionType:
        return self.padding.with_bounds(JContainer.with_elements(self._bounds, bounds))

    @dataclass
    class PaddingHelper:
        _t: IntersectionType

        @property
        def bounds(self) -> JContainer[TypeTree]:
            return self._t._bounds

        def with_bounds(self, bounds: JContainer[TypeTree]) -> IntersectionType:
            return self._t if self._t._bounds is bounds else replace(self._t, _bounds=bounds)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: IntersectionType.PaddingHelper
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

    def with_id(self, id: UUID) -> Label:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Label:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Label:
        return self if markers is self._markers else replace(self, _markers=markers)

    _label: JRightPadded[Identifier]

    @property
    def label(self) -> Identifier:
        return self._label.element

    def with_label(self, label: Identifier) -> Label:
        return self.padding.with_label(JRightPadded.with_element(self._label, label))

    _statement: Statement

    @property
    def statement(self) -> Statement:
        return self._statement

    def with_statement(self, statement: Statement) -> Label:
        return self if statement is self._statement else replace(self, _statement=statement)

    @dataclass
    class PaddingHelper:
        _t: Label

        @property
        def label(self) -> JRightPadded[Identifier]:
            return self._t._label

        def with_label(self, label: JRightPadded[Identifier]) -> Label:
            return self._t if self._t._label is label else replace(self._t, _label=label)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Label.PaddingHelper
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

    def with_id(self, id: UUID) -> Lambda:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Lambda:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Lambda:
        return self if markers is self._markers else replace(self, _markers=markers)

    _parameters: Parameters

    @property
    def parameters(self) -> Parameters:
        return self._parameters

    def with_parameters(self, parameters: Parameters) -> Lambda:
        return self if parameters is self._parameters else replace(self, _parameters=parameters)

    _arrow: Space

    @property
    def arrow(self) -> Space:
        return self._arrow

    def with_arrow(self, arrow: Space) -> Lambda:
        return self if arrow is self._arrow else replace(self, _arrow=arrow)

    _body: J

    @property
    def body(self) -> J:
        return self._body

    def with_body(self, body: J) -> Lambda:
        return self if body is self._body else replace(self, _body=body)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Lambda:
        return self if type is self._type else replace(self, _type=type)

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
            return self.padding.with_parameters(JRightPadded.with_elements(self._parameters, parameters))

        @dataclass
        class PaddingHelper:
            _t: Lambda.Parameters

            @property
            def parameters(self) -> List[JRightPadded[J]]:
                return self._t._parameters

            def with_parameters(self, parameters: List[JRightPadded[J]]) -> Lambda.Parameters:
                return self._t if self._t._parameters is parameters else replace(self._t, _parameters=parameters)

        _padding: weakref.ReferenceType[PaddingHelper] = None

        @property
        def padding(self) -> PaddingHelper:
            p: Lambda.Parameters.PaddingHelper
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

    def with_id(self, id: UUID) -> Literal:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Literal:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Literal:
        return self if markers is self._markers else replace(self, _markers=markers)

    _value: Optional[object]

    @property
    def value(self) -> Optional[object]:
        return self._value

    def with_value(self, value: Optional[object]) -> Literal:
        return self if value is self._value else replace(self, _value=value)

    _value_source: Optional[str]

    @property
    def value_source(self) -> Optional[str]:
        return self._value_source

    def with_value_source(self, value_source: Optional[str]) -> Literal:
        return self if value_source is self._value_source else replace(self, _value_source=value_source)

    _unicode_escapes: Optional[List[UnicodeEscape]]

    @property
    def unicode_escapes(self) -> Optional[List[UnicodeEscape]]:
        return self._unicode_escapes

    def with_unicode_escapes(self, unicode_escapes: Optional[List[UnicodeEscape]]) -> Literal:
        return self if unicode_escapes is self._unicode_escapes else replace(self, _unicode_escapes=unicode_escapes)

    _type: JavaType.Primitive

    @property
    def type(self) -> JavaType.Primitive:
        return self._type

    def with_type(self, type: JavaType.Primitive) -> Literal:
        return self if type is self._type else replace(self, _type=type)

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

    def with_id(self, id: UUID) -> MemberReference:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> MemberReference:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> MemberReference:
        return self if markers is self._markers else replace(self, _markers=markers)

    _containing: JRightPadded[Expression]

    @property
    def containing(self) -> Expression:
        return self._containing.element

    def with_containing(self, containing: Expression) -> MemberReference:
        return self.padding.with_containing(JRightPadded.with_element(self._containing, containing))

    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements

    def with_type_parameters(self, type_parameters: Optional[List[Expression]]) -> MemberReference:
        return self.padding.with_type_parameters(JContainer.with_elements_nullable(self._type_parameters, type_parameters))

    _reference: JLeftPadded[Identifier]

    @property
    def reference(self) -> Identifier:
        return self._reference.element

    def with_reference(self, reference: Identifier) -> MemberReference:
        return self.padding.with_reference(JLeftPadded.with_element(self._reference, reference))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> MemberReference:
        return self if type is self._type else replace(self, _type=type)

    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type

    def with_method_type(self, method_type: Optional[JavaType.Method]) -> MemberReference:
        return self if method_type is self._method_type else replace(self, _method_type=method_type)

    _variable_type: Optional[JavaType.Variable]

    @property
    def variable_type(self) -> Optional[JavaType.Variable]:
        return self._variable_type

    def with_variable_type(self, variable_type: Optional[JavaType.Variable]) -> MemberReference:
        return self if variable_type is self._variable_type else replace(self, _variable_type=variable_type)

    @dataclass
    class PaddingHelper:
        _t: MemberReference

        @property
        def containing(self) -> JRightPadded[Expression]:
            return self._t._containing

        def with_containing(self, containing: JRightPadded[Expression]) -> MemberReference:
            return self._t if self._t._containing is containing else replace(self._t, _containing=containing)

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[JContainer[Expression]]) -> MemberReference:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

        @property
        def reference(self) -> JLeftPadded[Identifier]:
            return self._t._reference

        def with_reference(self, reference: JLeftPadded[Identifier]) -> MemberReference:
            return self._t if self._t._reference is reference else replace(self._t, _reference=reference)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: MemberReference.PaddingHelper
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

    def with_id(self, id: UUID) -> MethodDeclaration:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> MethodDeclaration:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> MethodDeclaration:
        return self if markers is self._markers else replace(self, _markers=markers)

    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations

    def with_leading_annotations(self, leading_annotations: List[Annotation]) -> MethodDeclaration:
        return self if leading_annotations is self._leading_annotations else replace(self, _leading_annotations=leading_annotations)

    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers

    def with_modifiers(self, modifiers: List[Modifier]) -> MethodDeclaration:
        return self if modifiers is self._modifiers else replace(self, _modifiers=modifiers)

    _type_parameters: Optional[TypeParameters]

    _return_type_expression: Optional[TypeTree]

    @property
    def return_type_expression(self) -> Optional[TypeTree]:
        return self._return_type_expression

    def with_return_type_expression(self, return_type_expression: Optional[TypeTree]) -> MethodDeclaration:
        return self if return_type_expression is self._return_type_expression else replace(self, _return_type_expression=return_type_expression)

    _name: IdentifierWithAnnotations

    @property
    def name(self) -> Identifier:
        return self._name.identifier

    def with_name(self, name: Identifier) -> MethodDeclaration:
        return self.padding.with_name(MethodDeclaration.IdentifierWithAnnotations(name, self._name.annotations))

    _parameters: JContainer[Statement]

    @property
    def parameters(self) -> List[Statement]:
        return self._parameters.elements

    def with_parameters(self, parameters: List[Statement]) -> MethodDeclaration:
        return self.padding.with_parameters(JContainer.with_elements(self._parameters, parameters))

    _throws: Optional[JContainer[NameTree]]

    @property
    def throws(self) -> Optional[List[NameTree]]:
        return self._throws.elements

    def with_throws(self, throws: Optional[List[NameTree]]) -> MethodDeclaration:
        return self.padding.with_throws(JContainer.with_elements_nullable(self._throws, throws))

    _body: Optional[Block]

    @property
    def body(self) -> Optional[Block]:
        return self._body

    def with_body(self, body: Optional[Block]) -> MethodDeclaration:
        return self if body is self._body else replace(self, _body=body)

    _default_value: Optional[JLeftPadded[Expression]]

    @property
    def default_value(self) -> Optional[Expression]:
        return self._default_value.element if self._default_value else None

    def with_default_value(self, default_value: Optional[Expression]) -> MethodDeclaration:
        return self.padding.with_default_value(JLeftPadded.with_element(self._default_value, default_value))

    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type

    def with_method_type(self, method_type: Optional[JavaType.Method]) -> MethodDeclaration:
        return self if method_type is self._method_type else replace(self, _method_type=method_type)

    @dataclass
    class IdentifierWithAnnotations:
        _identifier: Identifier

        @property
        def identifier(self) -> Identifier:
            return self._identifier

        def with_identifier(self, identifier: Identifier) -> MethodDeclaration.IdentifierWithAnnotations:
            return self if identifier is self._identifier else replace(self, _identifier=identifier)

        _annotations: List[Annotation]

        @property
        def annotations(self) -> List[Annotation]:
            return self._annotations

        def with_annotations(self, annotations: List[Annotation]) -> MethodDeclaration.IdentifierWithAnnotations:
            return self if annotations is self._annotations else replace(self, _annotations=annotations)

    @dataclass
    class PaddingHelper:
        _t: MethodDeclaration

        @property
        def type_parameters(self) -> Optional[TypeParameters]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[TypeParameters]) -> MethodDeclaration:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

        @property
        def name(self) -> MethodDeclaration.IdentifierWithAnnotations:
            return self._t._name

        def with_name(self, name: MethodDeclaration.IdentifierWithAnnotations) -> MethodDeclaration:
            return self._t if self._t._name is name else replace(self._t, _name=name)

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

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: MethodDeclaration.PaddingHelper
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
        def name(self) -> MethodDeclaration.IdentifierWithAnnotations:
            return self._t._name

        def with_name(self, name: MethodDeclaration.IdentifierWithAnnotations) -> MethodDeclaration:
            return self._t if self._t._name is name else replace(self._t, _name=name)

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

    _annotations: weakref.ReferenceType[AnnotationsHelper] = None

    @property
    def annotations(self) -> AnnotationsHelper:
        p: MethodDeclaration.AnnotationsHelper
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

    def with_id(self, id: UUID) -> MethodInvocation:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> MethodInvocation:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> MethodInvocation:
        return self if markers is self._markers else replace(self, _markers=markers)

    _select: Optional[JRightPadded[Expression]]

    @property
    def select(self) -> Optional[Expression]:
        return self._select.element if self._select else None

    def with_select(self, select: Optional[Expression]) -> MethodInvocation:
        return self.padding.with_select(JRightPadded.with_element(self._select, select))

    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements

    def with_type_parameters(self, type_parameters: Optional[List[Expression]]) -> MethodInvocation:
        return self.padding.with_type_parameters(JContainer.with_elements_nullable(self._type_parameters, type_parameters))

    _name: Identifier

    @property
    def name(self) -> Identifier:
        return self._name

    def with_name(self, name: Identifier) -> MethodInvocation:
        return extensions.with_name(self, name)

    _arguments: JContainer[Expression]

    @property
    def arguments(self) -> List[Expression]:
        return self._arguments.elements

    def with_arguments(self, arguments: List[Expression]) -> MethodInvocation:
        return self.padding.with_arguments(JContainer.with_elements(self._arguments, arguments))

    _method_type: Optional[JavaType.Method]

    @property
    def method_type(self) -> Optional[JavaType.Method]:
        return self._method_type

    def with_method_type(self, method_type: Optional[JavaType.Method]) -> MethodInvocation:
        return self if method_type is self._method_type else replace(self, _method_type=method_type)

    @dataclass
    class PaddingHelper:
        _t: MethodInvocation

        @property
        def select(self) -> Optional[JRightPadded[Expression]]:
            return self._t._select

        def with_select(self, select: Optional[JRightPadded[Expression]]) -> MethodInvocation:
            return self._t if self._t._select is select else replace(self._t, _select=select)

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[JContainer[Expression]]) -> MethodInvocation:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

        @property
        def arguments(self) -> JContainer[Expression]:
            return self._t._arguments

        def with_arguments(self, arguments: JContainer[Expression]) -> MethodInvocation:
            return self._t if self._t._arguments is arguments else replace(self._t, _arguments=arguments)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: MethodInvocation.PaddingHelper
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

    def with_id(self, id: UUID) -> Modifier:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Modifier:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Modifier:
        return self if markers is self._markers else replace(self, _markers=markers)

    _keyword: Optional[str]

    @property
    def keyword(self) -> Optional[str]:
        return self._keyword

    def with_keyword(self, keyword: Optional[str]) -> Modifier:
        return self if keyword is self._keyword else replace(self, _keyword=keyword)

    _type: Type

    @property
    def type(self) -> Type:
        return self._type

    def with_type(self, type: Type) -> Modifier:
        return self if type is self._type else replace(self, _type=type)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> Modifier:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

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

    def with_id(self, id: UUID) -> MultiCatch:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> MultiCatch:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> MultiCatch:
        return self if markers is self._markers else replace(self, _markers=markers)

    _alternatives: List[JRightPadded[NameTree]]

    @property
    def alternatives(self) -> List[NameTree]:
        return JRightPadded.get_elements(self._alternatives)

    def with_alternatives(self, alternatives: List[NameTree]) -> MultiCatch:
        return self.padding.with_alternatives(JRightPadded.with_elements(self._alternatives, alternatives))

    @dataclass
    class PaddingHelper:
        _t: MultiCatch

        @property
        def alternatives(self) -> List[JRightPadded[NameTree]]:
            return self._t._alternatives

        def with_alternatives(self, alternatives: List[JRightPadded[NameTree]]) -> MultiCatch:
            return self._t if self._t._alternatives is alternatives else replace(self._t, _alternatives=alternatives)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: MultiCatch.PaddingHelper
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

    def with_id(self, id: UUID) -> NewArray:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> NewArray:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> NewArray:
        return self if markers is self._markers else replace(self, _markers=markers)

    _type_expression: Optional[TypeTree]

    @property
    def type_expression(self) -> Optional[TypeTree]:
        return self._type_expression

    def with_type_expression(self, type_expression: Optional[TypeTree]) -> NewArray:
        return self if type_expression is self._type_expression else replace(self, _type_expression=type_expression)

    _dimensions: List[ArrayDimension]

    @property
    def dimensions(self) -> List[ArrayDimension]:
        return self._dimensions

    def with_dimensions(self, dimensions: List[ArrayDimension]) -> NewArray:
        return self if dimensions is self._dimensions else replace(self, _dimensions=dimensions)

    _initializer: Optional[JContainer[Expression]]

    @property
    def initializer(self) -> Optional[List[Expression]]:
        return self._initializer.elements

    def with_initializer(self, initializer: Optional[List[Expression]]) -> NewArray:
        return self.padding.with_initializer(JContainer.with_elements_nullable(self._initializer, initializer))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> NewArray:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: NewArray

        @property
        def initializer(self) -> Optional[JContainer[Expression]]:
            return self._t._initializer

        def with_initializer(self, initializer: Optional[JContainer[Expression]]) -> NewArray:
            return self._t if self._t._initializer is initializer else replace(self._t, _initializer=initializer)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: NewArray.PaddingHelper
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

    def with_id(self, id: UUID) -> ArrayDimension:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ArrayDimension:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ArrayDimension:
        return self if markers is self._markers else replace(self, _markers=markers)

    _index: JRightPadded[Expression]

    @property
    def index(self) -> Expression:
        return self._index.element

    def with_index(self, index: Expression) -> ArrayDimension:
        return self.padding.with_index(JRightPadded.with_element(self._index, index))

    @dataclass
    class PaddingHelper:
        _t: ArrayDimension

        @property
        def index(self) -> JRightPadded[Expression]:
            return self._t._index

        def with_index(self, index: JRightPadded[Expression]) -> ArrayDimension:
            return self._t if self._t._index is index else replace(self._t, _index=index)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ArrayDimension.PaddingHelper
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

    def with_id(self, id: UUID) -> NewClass:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> NewClass:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> NewClass:
        return self if markers is self._markers else replace(self, _markers=markers)

    _enclosing: Optional[JRightPadded[Expression]]

    @property
    def enclosing(self) -> Optional[Expression]:
        return self._enclosing.element if self._enclosing else None

    def with_enclosing(self, enclosing: Optional[Expression]) -> NewClass:
        return self.padding.with_enclosing(JRightPadded.with_element(self._enclosing, enclosing))

    _new: Space

    @property
    def new(self) -> Space:
        return self._new

    def with_new(self, new: Space) -> NewClass:
        return self if new is self._new else replace(self, _new=new)

    _clazz: Optional[TypeTree]

    @property
    def clazz(self) -> Optional[TypeTree]:
        return self._clazz

    def with_clazz(self, clazz: Optional[TypeTree]) -> NewClass:
        return self if clazz is self._clazz else replace(self, _clazz=clazz)

    _arguments: JContainer[Expression]

    @property
    def arguments(self) -> List[Expression]:
        return self._arguments.elements

    def with_arguments(self, arguments: List[Expression]) -> NewClass:
        return self.padding.with_arguments(JContainer.with_elements(self._arguments, arguments))

    _body: Optional[Block]

    @property
    def body(self) -> Optional[Block]:
        return self._body

    def with_body(self, body: Optional[Block]) -> NewClass:
        return self if body is self._body else replace(self, _body=body)

    _constructor_type: Optional[JavaType.Method]

    @property
    def constructor_type(self) -> Optional[JavaType.Method]:
        return self._constructor_type

    def with_constructor_type(self, constructor_type: Optional[JavaType.Method]) -> NewClass:
        return self if constructor_type is self._constructor_type else replace(self, _constructor_type=constructor_type)

    @dataclass
    class PaddingHelper:
        _t: NewClass

        @property
        def enclosing(self) -> Optional[JRightPadded[Expression]]:
            return self._t._enclosing

        def with_enclosing(self, enclosing: Optional[JRightPadded[Expression]]) -> NewClass:
            return self._t if self._t._enclosing is enclosing else replace(self._t, _enclosing=enclosing)

        @property
        def arguments(self) -> JContainer[Expression]:
            return self._t._arguments

        def with_arguments(self, arguments: JContainer[Expression]) -> NewClass:
            return self._t if self._t._arguments is arguments else replace(self._t, _arguments=arguments)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: NewClass.PaddingHelper
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

    def with_id(self, id: UUID) -> NullableType:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> NullableType:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> NullableType:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> NullableType:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _type_tree: JRightPadded[TypeTree]

    @property
    def type_tree(self) -> TypeTree:
        return self._type_tree.element

    def with_type_tree(self, type_tree: TypeTree) -> NullableType:
        return self.padding.with_type_tree(JRightPadded.with_element(self._type_tree, type_tree))

    @dataclass
    class PaddingHelper:
        _t: NullableType

        @property
        def type_tree(self) -> JRightPadded[TypeTree]:
            return self._t._type_tree

        def with_type_tree(self, type_tree: JRightPadded[TypeTree]) -> NullableType:
            return self._t if self._t._type_tree is type_tree else replace(self._t, _type_tree=type_tree)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: NullableType.PaddingHelper
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

    def with_id(self, id: UUID) -> Package:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Package:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Package:
        return self if markers is self._markers else replace(self, _markers=markers)

    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression

    def with_expression(self, expression: Expression) -> Package:
        return self if expression is self._expression else replace(self, _expression=expression)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> Package:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_package(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class ParameterizedType(TypeTree, Expression):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> ParameterizedType:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ParameterizedType:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ParameterizedType:
        return self if markers is self._markers else replace(self, _markers=markers)

    _clazz: NameTree

    @property
    def clazz(self) -> NameTree:
        return self._clazz

    def with_clazz(self, clazz: NameTree) -> ParameterizedType:
        return self if clazz is self._clazz else replace(self, _clazz=clazz)

    _type_parameters: Optional[JContainer[Expression]]

    @property
    def type_parameters(self) -> Optional[List[Expression]]:
        return self._type_parameters.elements

    def with_type_parameters(self, type_parameters: Optional[List[Expression]]) -> ParameterizedType:
        return self.padding.with_type_parameters(JContainer.with_elements_nullable(self._type_parameters, type_parameters))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> ParameterizedType:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: ParameterizedType

        @property
        def type_parameters(self) -> Optional[JContainer[Expression]]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: Optional[JContainer[Expression]]) -> ParameterizedType:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ParameterizedType.PaddingHelper
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

    def with_id(self, id: UUID) -> Parentheses[J2]:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Parentheses[J2]:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Parentheses[J2]:
        return self if markers is self._markers else replace(self, _markers=markers)

    _tree: JRightPadded[J2]

    @property
    def tree(self) -> J2:
        return self._tree.element

    def with_tree(self, tree: J2) -> Parentheses[J2]:
        return self.padding.with_tree(JRightPadded.with_element(self._tree, tree))

    @property
    def type(self) -> JavaType:
        return self.tree.type

    @dataclass
    class PaddingHelper:
        _t: Parentheses[J2]

        @property
        def tree(self) -> JRightPadded[J2]:
            return self._t._tree

        def with_tree(self, tree: JRightPadded[J2]) -> Parentheses[J2]:
            return self._t if self._t._tree is tree else replace(self._t, _tree=tree)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Parentheses[J2].PaddingHelper
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

    def with_id(self, id: UUID) -> ControlParentheses[J2]:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> ControlParentheses[J2]:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> ControlParentheses[J2]:
        return self if markers is self._markers else replace(self, _markers=markers)

    _tree: JRightPadded[J2]

    @property
    def tree(self) -> J2:
        return self._tree.element

    def with_tree(self, tree: J2) -> ControlParentheses[J2]:
        return self.padding.with_tree(JRightPadded.with_element(self._tree, tree))

    @dataclass
    class PaddingHelper:
        _t: ControlParentheses[J2]

        @property
        def tree(self) -> JRightPadded[J2]:
            return self._t._tree

        def with_tree(self, tree: JRightPadded[J2]) -> ControlParentheses[J2]:
            return self._t if self._t._tree is tree else replace(self._t, _tree=tree)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: ControlParentheses[J2].PaddingHelper
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

    def with_id(self, id: UUID) -> Primitive:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Primitive:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Primitive:
        return self if markers is self._markers else replace(self, _markers=markers)

    _type: JavaType.Primitive

    @property
    def type(self) -> JavaType.Primitive:
        return self._type

    def with_type(self, type: JavaType.Primitive) -> Primitive:
        return self if type is self._type else replace(self, _type=type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_primitive(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Return(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Return:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Return:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Return:
        return self if markers is self._markers else replace(self, _markers=markers)

    _expression: Optional[Expression]

    @property
    def expression(self) -> Optional[Expression]:
        return self._expression

    def with_expression(self, expression: Optional[Expression]) -> Return:
        return self if expression is self._expression else replace(self, _expression=expression)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_return(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Switch(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Switch:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Switch:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Switch:
        return self if markers is self._markers else replace(self, _markers=markers)

    _selector: ControlParentheses[Expression]

    @property
    def selector(self) -> ControlParentheses[Expression]:
        return self._selector

    def with_selector(self, selector: ControlParentheses[Expression]) -> Switch:
        return self if selector is self._selector else replace(self, _selector=selector)

    _cases: Block

    @property
    def cases(self) -> Block:
        return self._cases

    def with_cases(self, cases: Block) -> Switch:
        return self if cases is self._cases else replace(self, _cases=cases)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_switch(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class SwitchExpression(Expression, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> SwitchExpression:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> SwitchExpression:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> SwitchExpression:
        return self if markers is self._markers else replace(self, _markers=markers)

    _selector: ControlParentheses[Expression]

    @property
    def selector(self) -> ControlParentheses[Expression]:
        return self._selector

    def with_selector(self, selector: ControlParentheses[Expression]) -> SwitchExpression:
        return self if selector is self._selector else replace(self, _selector=selector)

    _cases: Block

    @property
    def cases(self) -> Block:
        return self._cases

    def with_cases(self, cases: Block) -> SwitchExpression:
        return self if cases is self._cases else replace(self, _cases=cases)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> SwitchExpression:
        return self if type is self._type else replace(self, _type=type)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_switch_expression(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Synchronized(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Synchronized:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Synchronized:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Synchronized:
        return self if markers is self._markers else replace(self, _markers=markers)

    _lock: ControlParentheses[Expression]

    @property
    def lock(self) -> ControlParentheses[Expression]:
        return self._lock

    def with_lock(self, lock: ControlParentheses[Expression]) -> Synchronized:
        return self if lock is self._lock else replace(self, _lock=lock)

    _body: Block

    @property
    def body(self) -> Block:
        return self._body

    def with_body(self, body: Block) -> Synchronized:
        return self if body is self._body else replace(self, _body=body)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_synchronized(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Ternary(Expression, Statement, TypedTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Ternary:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Ternary:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Ternary:
        return self if markers is self._markers else replace(self, _markers=markers)

    _condition: Expression

    @property
    def condition(self) -> Expression:
        return self._condition

    def with_condition(self, condition: Expression) -> Ternary:
        return self if condition is self._condition else replace(self, _condition=condition)

    _true_part: JLeftPadded[Expression]

    @property
    def true_part(self) -> Expression:
        return self._true_part.element

    def with_true_part(self, true_part: Expression) -> Ternary:
        return self.padding.with_true_part(JLeftPadded.with_element(self._true_part, true_part))

    _false_part: JLeftPadded[Expression]

    @property
    def false_part(self) -> Expression:
        return self._false_part.element

    def with_false_part(self, false_part: Expression) -> Ternary:
        return self.padding.with_false_part(JLeftPadded.with_element(self._false_part, false_part))

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Ternary:
        return self if type is self._type else replace(self, _type=type)

    @dataclass
    class PaddingHelper:
        _t: Ternary

        @property
        def true_part(self) -> JLeftPadded[Expression]:
            return self._t._true_part

        def with_true_part(self, true_part: JLeftPadded[Expression]) -> Ternary:
            return self._t if self._t._true_part is true_part else replace(self._t, _true_part=true_part)

        @property
        def false_part(self) -> JLeftPadded[Expression]:
            return self._t._false_part

        def with_false_part(self, false_part: JLeftPadded[Expression]) -> Ternary:
            return self._t if self._t._false_part is false_part else replace(self._t, _false_part=false_part)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Ternary.PaddingHelper
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

    def with_id(self, id: UUID) -> Throw:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Throw:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Throw:
        return self if markers is self._markers else replace(self, _markers=markers)

    _exception: Expression

    @property
    def exception(self) -> Expression:
        return self._exception

    def with_exception(self, exception: Expression) -> Throw:
        return self if exception is self._exception else replace(self, _exception=exception)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_throw(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Try(Statement):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Try:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Try:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Try:
        return self if markers is self._markers else replace(self, _markers=markers)

    _resources: Optional[JContainer[Resource]]

    @property
    def resources(self) -> Optional[List[Resource]]:
        return self._resources.elements

    def with_resources(self, resources: Optional[List[Resource]]) -> Try:
        return self.padding.with_resources(JContainer.with_elements_nullable(self._resources, resources))

    _body: Block

    @property
    def body(self) -> Block:
        return self._body

    def with_body(self, body: Block) -> Try:
        return self if body is self._body else replace(self, _body=body)

    _catches: List[Catch]

    @property
    def catches(self) -> List[Catch]:
        return self._catches

    def with_catches(self, catches: List[Catch]) -> Try:
        return self if catches is self._catches else replace(self, _catches=catches)

    _finally: Optional[JLeftPadded[Block]]

    @property
    def finally_(self) -> Optional[Block]:
        return self._finally.element if self._finally else None

    def with_finally(self, finally_: Optional[Block]) -> Try:
        return self.padding.with_finally(JLeftPadded.with_element(self._finally, finally_))

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

        def with_resources(self, resources: Optional[JContainer[Try.Resource]]) -> Try:
            return self._t if self._t._resources is resources else replace(self._t, _resources=resources)

        @property
        def finally_(self) -> Optional[JLeftPadded[Block]]:
            return self._t._finally

        def with_finally(self, finally_: Optional[JLeftPadded[Block]]) -> Try:
            return self._t if self._t._finally is finally_ else replace(self._t, _finally=finally_)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Try.PaddingHelper
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

    def with_id(self, id: UUID) -> TypeCast:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> TypeCast:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> TypeCast:
        return self if markers is self._markers else replace(self, _markers=markers)

    _clazz: ControlParentheses[TypeTree]

    @property
    def clazz(self) -> ControlParentheses[TypeTree]:
        return self._clazz

    def with_clazz(self, clazz: ControlParentheses[TypeTree]) -> TypeCast:
        return self if clazz is self._clazz else replace(self, _clazz=clazz)

    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression

    def with_expression(self, expression: Expression) -> TypeCast:
        return self if expression is self._expression else replace(self, _expression=expression)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_type_cast(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class TypeParameter(J):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> TypeParameter:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> TypeParameter:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> TypeParameter:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> TypeParameter:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers

    def with_modifiers(self, modifiers: List[Modifier]) -> TypeParameter:
        return self if modifiers is self._modifiers else replace(self, _modifiers=modifiers)

    _name: Expression

    @property
    def name(self) -> Expression:
        return self._name

    def with_name(self, name: Expression) -> TypeParameter:
        return self if name is self._name else replace(self, _name=name)

    _bounds: Optional[JContainer[TypeTree]]

    @property
    def bounds(self) -> Optional[List[TypeTree]]:
        return self._bounds.elements

    def with_bounds(self, bounds: Optional[List[TypeTree]]) -> TypeParameter:
        return self.padding.with_bounds(JContainer.with_elements_nullable(self._bounds, bounds))

    @dataclass
    class PaddingHelper:
        _t: TypeParameter

        @property
        def bounds(self) -> Optional[JContainer[TypeTree]]:
            return self._t._bounds

        def with_bounds(self, bounds: Optional[JContainer[TypeTree]]) -> TypeParameter:
            return self._t if self._t._bounds is bounds else replace(self._t, _bounds=bounds)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: TypeParameter.PaddingHelper
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

    def with_id(self, id: UUID) -> TypeParameters:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> TypeParameters:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> TypeParameters:
        return self if markers is self._markers else replace(self, _markers=markers)

    _annotations: List[Annotation]

    @property
    def annotations(self) -> List[Annotation]:
        return self._annotations

    def with_annotations(self, annotations: List[Annotation]) -> TypeParameters:
        return self if annotations is self._annotations else replace(self, _annotations=annotations)

    _type_parameters: List[JRightPadded[TypeParameter]]

    @property
    def type_parameters(self) -> List[TypeParameter]:
        return JRightPadded.get_elements(self._type_parameters)

    def with_type_parameters(self, type_parameters: List[TypeParameter]) -> TypeParameters:
        return self.padding.with_type_parameters(JRightPadded.with_elements(self._type_parameters, type_parameters))

    @dataclass
    class PaddingHelper:
        _t: TypeParameters

        @property
        def type_parameters(self) -> List[JRightPadded[TypeParameter]]:
            return self._t._type_parameters

        def with_type_parameters(self, type_parameters: List[JRightPadded[TypeParameter]]) -> TypeParameters:
            return self._t if self._t._type_parameters is type_parameters else replace(self._t, _type_parameters=type_parameters)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: TypeParameters.PaddingHelper
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

    def with_id(self, id: UUID) -> Unary:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Unary:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Unary:
        return self if markers is self._markers else replace(self, _markers=markers)

    _operator: JLeftPadded[Type]

    @property
    def operator(self) -> Type:
        return self._operator.element

    def with_operator(self, operator: Type) -> Unary:
        return self.padding.with_operator(JLeftPadded.with_element(self._operator, operator))

    _expression: Expression

    @property
    def expression(self) -> Expression:
        return self._expression

    def with_expression(self, expression: Expression) -> Unary:
        return self if expression is self._expression else replace(self, _expression=expression)

    _type: Optional[JavaType]

    @property
    def type(self) -> Optional[JavaType]:
        return self._type

    def with_type(self, type: Optional[JavaType]) -> Unary:
        return self if type is self._type else replace(self, _type=type)

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

        def with_operator(self, operator: JLeftPadded[Unary.Type]) -> Unary:
            return self._t if self._t._operator is operator else replace(self._t, _operator=operator)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Unary.PaddingHelper
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

    def with_id(self, id: UUID) -> VariableDeclarations:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> VariableDeclarations:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> VariableDeclarations:
        return self if markers is self._markers else replace(self, _markers=markers)

    _leading_annotations: List[Annotation]

    @property
    def leading_annotations(self) -> List[Annotation]:
        return self._leading_annotations

    def with_leading_annotations(self, leading_annotations: List[Annotation]) -> VariableDeclarations:
        return self if leading_annotations is self._leading_annotations else replace(self, _leading_annotations=leading_annotations)

    _modifiers: List[Modifier]

    @property
    def modifiers(self) -> List[Modifier]:
        return self._modifiers

    def with_modifiers(self, modifiers: List[Modifier]) -> VariableDeclarations:
        return self if modifiers is self._modifiers else replace(self, _modifiers=modifiers)

    _type_expression: Optional[TypeTree]

    @property
    def type_expression(self) -> Optional[TypeTree]:
        return self._type_expression

    def with_type_expression(self, type_expression: Optional[TypeTree]) -> VariableDeclarations:
        return self if type_expression is self._type_expression else replace(self, _type_expression=type_expression)

    _varargs: Optional[Space]

    @property
    def varargs(self) -> Optional[Space]:
        return self._varargs

    def with_varargs(self, varargs: Optional[Space]) -> VariableDeclarations:
        return self if varargs is self._varargs else replace(self, _varargs=varargs)

    _dimensions_before_name: List[JLeftPadded[Space]]

    @property
    def dimensions_before_name(self) -> List[JLeftPadded[Space]]:
        return self._dimensions_before_name

    def with_dimensions_before_name(self, dimensions_before_name: List[JLeftPadded[Space]]) -> VariableDeclarations:
        return self if dimensions_before_name is self._dimensions_before_name else replace(self, _dimensions_before_name=dimensions_before_name)

    _variables: List[JRightPadded[NamedVariable]]

    @property
    def variables(self) -> List[NamedVariable]:
        return JRightPadded.get_elements(self._variables)

    def with_variables(self, variables: List[NamedVariable]) -> VariableDeclarations:
        return self.padding.with_variables(JRightPadded.with_elements(self._variables, variables))

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
            return self.padding.with_initializer(JLeftPadded.with_element(self._initializer, initializer))

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

            def with_initializer(self, initializer: Optional[JLeftPadded[Expression]]) -> VariableDeclarations.NamedVariable:
                return self._t if self._t._initializer is initializer else replace(self._t, _initializer=initializer)

        _padding: weakref.ReferenceType[PaddingHelper] = None

        @property
        def padding(self) -> PaddingHelper:
            p: VariableDeclarations.NamedVariable.PaddingHelper
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

        def with_variables(self, variables: List[JRightPadded[VariableDeclarations.NamedVariable]]) -> VariableDeclarations:
            return self._t if self._t._variables is variables else replace(self._t, _variables=variables)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: VariableDeclarations.PaddingHelper
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

    def with_id(self, id: UUID) -> WhileLoop:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> WhileLoop:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> WhileLoop:
        return self if markers is self._markers else replace(self, _markers=markers)

    _condition: ControlParentheses[Expression]

    @property
    def condition(self) -> ControlParentheses[Expression]:
        return self._condition

    def with_condition(self, condition: ControlParentheses[Expression]) -> WhileLoop:
        return self if condition is self._condition else replace(self, _condition=condition)

    _body: JRightPadded[Statement]

    @property
    def body(self) -> Statement:
        return self._body.element

    def with_body(self, body: Statement) -> WhileLoop:
        return self.padding.with_body(JRightPadded.with_element(self._body, body))

    @dataclass
    class PaddingHelper:
        _t: WhileLoop

        @property
        def body(self) -> JRightPadded[Statement]:
            return self._t._body

        def with_body(self, body: JRightPadded[Statement]) -> WhileLoop:
            return self._t if self._t._body is body else replace(self._t, _body=body)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: WhileLoop.PaddingHelper
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

    def with_id(self, id: UUID) -> Wildcard:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Wildcard:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Wildcard:
        return self if markers is self._markers else replace(self, _markers=markers)

    _bound: Optional[JLeftPadded[Bound]]

    @property
    def bound(self) -> Optional[Bound]:
        return self._bound.element if self._bound else None

    def with_bound(self, bound: Optional[Bound]) -> Wildcard:
        return self.padding.with_bound(JLeftPadded.with_element(self._bound, bound))

    _bounded_type: Optional[NameTree]

    @property
    def bounded_type(self) -> Optional[NameTree]:
        return self._bounded_type

    def with_bounded_type(self, bounded_type: Optional[NameTree]) -> Wildcard:
        return self if bounded_type is self._bounded_type else replace(self, _bounded_type=bounded_type)

    class Bound(Enum):
        Extends = 0
        Super = 1

    @dataclass
    class PaddingHelper:
        _t: Wildcard

        @property
        def bound(self) -> Optional[JLeftPadded[Wildcard.Bound]]:
            return self._t._bound

        def with_bound(self, bound: Optional[JLeftPadded[Wildcard.Bound]]) -> Wildcard:
            return self._t if self._t._bound is bound else replace(self._t, _bound=bound)

    _padding: weakref.ReferenceType[PaddingHelper] = None

    @property
    def padding(self) -> PaddingHelper:
        p: Wildcard.PaddingHelper
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

    def with_id(self, id: UUID) -> Yield:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Yield:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Yield:
        return self if markers is self._markers else replace(self, _markers=markers)

    _implicit: bool

    @property
    def implicit(self) -> bool:
        return self._implicit

    def with_implicit(self, implicit: bool) -> Yield:
        return self if implicit is self._implicit else replace(self, _implicit=implicit)

    _value: Expression

    @property
    def value(self) -> Expression:
        return self._value

    def with_value(self, value: Expression) -> Yield:
        return self if value is self._value else replace(self, _value=value)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_yield(self, p)

# noinspection PyShadowingBuiltins,PyShadowingNames,DuplicatedCode
@dataclass(frozen=True, eq=False)
class Unknown(Statement, Expression, TypeTree):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id: UUID) -> Unknown:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Unknown:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Unknown:
        return self if markers is self._markers else replace(self, _markers=markers)

    _source: Source

    @property
    def source(self) -> Source:
        return self._source

    def with_source(self, source: Source) -> Unknown:
        return self if source is self._source else replace(self, _source=source)

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

    def with_id(self, id: UUID) -> Erroneous:
        return self if id is self._id else replace(self, _id=id)

    _prefix: Space

    @property
    def prefix(self) -> Space:
        return self._prefix

    def with_prefix(self, prefix: Space) -> Erroneous:
        return self if prefix is self._prefix else replace(self, _prefix=prefix)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def with_markers(self, markers: Markers) -> Erroneous:
        return self if markers is self._markers else replace(self, _markers=markers)

    _text: str

    @property
    def text(self) -> str:
        return self._text

    def with_text(self, text: str) -> Erroneous:
        return self if text is self._text else replace(self, _text=text)

    def accept_java(self, v: JavaVisitor[P], p: P) -> J:
        return v.visit_erroneous(self, p)
