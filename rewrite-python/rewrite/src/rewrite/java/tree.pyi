# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from enum import Enum
from pathlib import Path
from rewrite import Checksum, FileAttributes, SourceFile, Tree, TreeVisitor, Markers, Cursor, PrintOutputCapture, PrinterFactory

class AnnotatedType(Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _type_expression: TypeTree

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _type_expression: TypeTree,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        type_expression: TypeTree = ...,
    ) -> Self: ...

class Annotation(Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotation_type: NameTree
    _arguments: Optional[JContainer[Expression]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotation_type: NameTree,
        _arguments: Optional[JContainer[Expression]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotation_type: NameTree = ...,
        arguments: Optional[JContainer[Expression]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ArrayAccess(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _indexed: Expression
    _dimension: ArrayDimension
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _indexed: Expression,
        _dimension: ArrayDimension,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        indexed: Expression = ...,
        dimension: ArrayDimension = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class ArrayType(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _element_type: TypeTree
    _annotations: Optional[List[Annotation]]
    _dimension: Optional[JLeftPadded[Space]]
    _type: JavaType

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _element_type: TypeTree,
        _annotations: Optional[List[Annotation]],
        _dimension: Optional[JLeftPadded[Space]],
        _type: JavaType,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        element_type: TypeTree = ...,
        annotations: Optional[List[Annotation]] = ...,
        dimension: Optional[JLeftPadded[Space]] = ...,
        type_: JavaType = ...,
    ) -> Self: ...

class Assert(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _condition: Expression
    _detail: Optional[JLeftPadded[Expression]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _condition: Expression,
        _detail: Optional[JLeftPadded[Expression]],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        condition: Expression = ...,
        detail: Optional[JLeftPadded[Expression]] = ...,
    ) -> Self: ...

class Assignment(Statement, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _variable: Expression
    _assignment: JLeftPadded[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _variable: Expression,
        _assignment: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        variable: Expression = ...,
        assignment: JLeftPadded[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class AssignmentOperation(Statement, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _variable: Expression
    _operator: JLeftPadded[Type]
    _assignment: Expression
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _variable: Expression,
        _operator: JLeftPadded[Type],
        _assignment: Expression,
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        variable: Expression = ...,
        operator: JLeftPadded[Type] = ...,
        assignment: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Binary(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _left: Expression
    _operator: JLeftPadded[Type]
    _right: Expression
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _left: Expression,
        _operator: JLeftPadded[Type],
        _right: Expression,
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        left: Expression = ...,
        operator: JLeftPadded[Type] = ...,
        right: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Block(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _static: JRightPadded[bool]
    _statements: List[JRightPadded[Statement]]
    _end: Space
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _static: JRightPadded[bool],
        _statements: List[JRightPadded[Statement]],
        _end: Space,
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        static: JRightPadded[bool] = ...,
        statements: List[JRightPadded[Statement]] = ...,
        end: Space = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Break(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _label: Optional[Identifier]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _label: Optional[Identifier],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        label: Optional[Identifier] = ...,
    ) -> Self: ...

class Case(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type: Type
    _case_labels: JContainer[J]
    _statements: JContainer[Statement]
    _body: Optional[JRightPadded[J]]
    _guard: Optional[Expression]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _type: Type,
        _case_labels: JContainer[J],
        _statements: JContainer[Statement],
        _body: Optional[JRightPadded[J]],
        _guard: Optional[Expression],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_: Type = ...,
        case_labels: JContainer[J] = ...,
        statements: JContainer[Statement] = ...,
        body: Optional[JRightPadded[J]] = ...,
        guard: Optional[Expression] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ClassDeclaration(Statement, TypedTree):
    class Kind(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _annotations: List[Annotation]
        _type: Type

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _annotations: List[Annotation],
            _type: Type,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            annotations: List[Annotation] = ...,
            type_: Type = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _leading_annotations: List[Annotation]
    _modifiers: List[Modifier]
    _kind: Kind
    _name: Identifier
    _type_parameters: Optional[JContainer[TypeParameter]]
    _primary_constructor: Optional[JContainer[Statement]]
    _extends: Optional[JLeftPadded[TypeTree]]
    _implements: Optional[JContainer[TypeTree]]
    _permits: Optional[JContainer[TypeTree]]
    _body: Block
    _type: Optional[JavaType.FullyQualified]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _leading_annotations: List[Annotation],
        _modifiers: List[Modifier],
        _kind: Kind,
        _name: Identifier,
        _type_parameters: Optional[JContainer[TypeParameter]],
        _primary_constructor: Optional[JContainer[Statement]],
        _extends: Optional[JLeftPadded[TypeTree]],
        _implements: Optional[JContainer[TypeTree]],
        _permits: Optional[JContainer[TypeTree]],
        _body: Block,
        _type: Optional[JavaType.FullyQualified],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        leading_annotations: List[Annotation] = ...,
        modifiers: List[Modifier] = ...,
        kind: Kind = ...,
        name: Identifier = ...,
        type_parameters: Optional[JContainer[TypeParameter]] = ...,
        primary_constructor: Optional[JContainer[Statement]] = ...,
        extends: Optional[JLeftPadded[TypeTree]] = ...,
        implements: Optional[JContainer[TypeTree]] = ...,
        permits: Optional[JContainer[TypeTree]] = ...,
        body: Block = ...,
        type_: Optional[JavaType.FullyQualified] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class CompilationUnit(JavaSourceFile, SourceFile):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _source_path: Path
    _file_attributes: Optional[FileAttributes]
    _charset_name: Optional[str]
    _charset_bom_marked: bool
    _checksum: Optional[Checksum]
    _package_declaration: Optional[JRightPadded[Package]]
    _imports: List[JRightPadded[Import]]
    _classes: List[ClassDeclaration]
    _eof: Space
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _source_path: Path,
        _file_attributes: Optional[FileAttributes],
        _charset_name: Optional[str],
        _charset_bom_marked: bool,
        _checksum: Optional[Checksum],
        _package_declaration: Optional[JRightPadded[Package]],
        _imports: List[JRightPadded[Import]],
        _classes: List[ClassDeclaration],
        _eof: Space,
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        source_path: Path = ...,
        file_attributes: Optional[FileAttributes] = ...,
        charset_name: Optional[str] = ...,
        charset_bom_marked: bool = ...,
        checksum: Optional[Checksum] = ...,
        package_declaration: Optional[JRightPadded[Package]] = ...,
        imports: List[JRightPadded[Import]] = ...,
        classes: List[ClassDeclaration] = ...,
        eof: Space = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Continue(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _label: Optional[Identifier]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _label: Optional[Identifier],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        label: Optional[Identifier] = ...,
    ) -> Self: ...

class DoWhileLoop(Loop):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _body: JRightPadded[Statement]
    _while_condition: JLeftPadded[ControlParentheses[Expression]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _body: JRightPadded[Statement],
        _while_condition: JLeftPadded[ControlParentheses[Expression]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        body: JRightPadded[Statement] = ...,
        while_condition: JLeftPadded[ControlParentheses[Expression]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Empty(Statement, Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
    ) -> Self: ...

class EnumValue(J):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _name: Identifier
    _initializer: Optional[NewClass]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _name: Identifier,
        _initializer: Optional[NewClass],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        name: Identifier = ...,
        initializer: Optional[NewClass] = ...,
    ) -> Self: ...

class EnumValueSet(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _enums: List[JRightPadded[EnumValue]]
    _terminated_with_semicolon: bool
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _enums: List[JRightPadded[EnumValue]],
        _terminated_with_semicolon: bool,
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        enums: List[JRightPadded[EnumValue]] = ...,
        terminated_with_semicolon: bool = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class FieldAccess(TypeTree, Expression, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _target: Expression
    _name: JLeftPadded[Identifier]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _target: Expression,
        _name: JLeftPadded[Identifier],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        target: Expression = ...,
        name: JLeftPadded[Identifier] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ForEachLoop(Loop):
    class Control(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _variable: JRightPadded[Statement]
        _iterable: JRightPadded[Expression]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _variable: JRightPadded[Statement],
            _iterable: JRightPadded[Expression],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            variable: JRightPadded[Statement] = ...,
            iterable: JRightPadded[Expression] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _control: Control
    _body: JRightPadded[Statement]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _control: Control,
        _body: JRightPadded[Statement],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        control: Control = ...,
        body: JRightPadded[Statement] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ForLoop(Loop):
    class Control(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _init: List[JRightPadded[Statement]]
        _condition: JRightPadded[Expression]
        _update: List[JRightPadded[Statement]]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _init: List[JRightPadded[Statement]],
            _condition: JRightPadded[Expression],
            _update: List[JRightPadded[Statement]],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            init: List[JRightPadded[Statement]] = ...,
            condition: JRightPadded[Expression] = ...,
            update: List[JRightPadded[Statement]] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _control: Control
    _body: JRightPadded[Statement]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _control: Control,
        _body: JRightPadded[Statement],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        control: Control = ...,
        body: JRightPadded[Statement] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ParenthesizedTypeTree(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _parenthesized_type: Parentheses[TypeTree]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _parenthesized_type: Parentheses[TypeTree],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        parenthesized_type: Parentheses[TypeTree] = ...,
    ) -> Self: ...

class Identifier(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _simple_name: str
    _type: Optional[JavaType]
    _field_type: Optional[JavaType.Variable]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _simple_name: str,
        _type: Optional[JavaType],
        _field_type: Optional[JavaType.Variable],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        simple_name: str = ...,
        type_: Optional[JavaType] = ...,
        field_type: Optional[JavaType.Variable] = ...,
    ) -> Self: ...

class If(Statement):
    class Else(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _body: JRightPadded[Statement]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _body: JRightPadded[Statement],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            body: JRightPadded[Statement] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _if_condition: ControlParentheses[Expression]
    _then_part: JRightPadded[Statement]
    _else_part: Optional[Else]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _if_condition: ControlParentheses[Expression],
        _then_part: JRightPadded[Statement],
        _else_part: Optional[Else],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        if_condition: ControlParentheses[Expression] = ...,
        then_part: JRightPadded[Statement] = ...,
        else_part: Optional[Else] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Import(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _static: JLeftPadded[bool]
    _qualid: FieldAccess
    _alias: Optional[JLeftPadded[Identifier]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _static: JLeftPadded[bool],
        _qualid: FieldAccess,
        _alias: Optional[JLeftPadded[Identifier]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        static: JLeftPadded[bool] = ...,
        qualid: FieldAccess = ...,
        alias: Optional[JLeftPadded[Identifier]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class InstanceOf(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: JRightPadded[Expression]
    _clazz: J
    _pattern: Optional[J]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: JRightPadded[Expression],
        _clazz: J,
        _pattern: Optional[J],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: JRightPadded[Expression] = ...,
        clazz: J = ...,
        pattern: Optional[J] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class DeconstructionPattern(TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _deconstructor: Expression
    _nested: JContainer[J]
    _type: JavaType
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _deconstructor: Expression,
        _nested: JContainer[J],
        _type: JavaType,
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        deconstructor: Expression = ...,
        nested: JContainer[J] = ...,
        type_: JavaType = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class IntersectionType(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _bounds: JContainer[TypeTree]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _bounds: JContainer[TypeTree],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        bounds: JContainer[TypeTree] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Label(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _label: JRightPadded[Identifier]
    _statement: Statement
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _label: JRightPadded[Identifier],
        _statement: Statement,
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        label: JRightPadded[Identifier] = ...,
        statement: Statement = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Lambda(Statement, Expression, TypedTree):
    class Parameters(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _parenthesized: bool
        _parameters: List[JRightPadded[J]]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _parenthesized: bool,
            _parameters: List[JRightPadded[J]],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            parenthesized: bool = ...,
            parameters: List[JRightPadded[J]] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _parameters: Parameters
    _arrow: Space
    _body: J
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _parameters: Parameters,
        _arrow: Space,
        _body: J,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        parameters: Parameters = ...,
        arrow: Space = ...,
        body: J = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class Literal(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _value: Optional[object]
    _value_source: Optional[str]
    _unicode_escapes: Optional[List[UnicodeEscape]]
    _type: JavaType.Primitive

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _value: Optional[object],
        _value_source: Optional[str],
        _unicode_escapes: Optional[List[UnicodeEscape]],
        _type: JavaType.Primitive,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        value: Optional[object] = ...,
        value_source: Optional[str] = ...,
        unicode_escapes: Optional[List[UnicodeEscape]] = ...,
        type_: JavaType.Primitive = ...,
    ) -> Self: ...

class MemberReference(TypedTree, MethodCall):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _containing: JRightPadded[Expression]
    _type_parameters: Optional[JContainer[Expression]]
    _reference: JLeftPadded[Identifier]
    _type: Optional[JavaType]
    _method_type: Optional[JavaType.Method]
    _variable_type: Optional[JavaType.Variable]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _containing: JRightPadded[Expression],
        _type_parameters: Optional[JContainer[Expression]],
        _reference: JLeftPadded[Identifier],
        _type: Optional[JavaType],
        _method_type: Optional[JavaType.Method],
        _variable_type: Optional[JavaType.Variable],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        containing: JRightPadded[Expression] = ...,
        type_parameters: Optional[JContainer[Expression]] = ...,
        reference: JLeftPadded[Identifier] = ...,
        type_: Optional[JavaType] = ...,
        method_type: Optional[JavaType.Method] = ...,
        variable_type: Optional[JavaType.Variable] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class MethodDeclaration(Statement, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _leading_annotations: List[Annotation]
    _modifiers: List[Modifier]
    _type_parameters: Optional[TypeParameters]
    _return_type_expression: Optional[TypeTree]
    _name_annotations: List[Annotation]
    _name: Identifier
    _parameters: JContainer[Statement]
    _throws: Optional[JContainer[NameTree]]
    _body: Optional[Block]
    _default_value: Optional[JLeftPadded[Expression]]
    _method_type: Optional[JavaType.Method]
    _padding: weakref.ReferenceType[PaddingHelper]
    _annotations: weakref.ReferenceType[AnnotationsHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _leading_annotations: List[Annotation],
        _modifiers: List[Modifier],
        _type_parameters: Optional[TypeParameters],
        _return_type_expression: Optional[TypeTree],
        _name_annotations: List[Annotation],
        _name: Identifier,
        _parameters: JContainer[Statement],
        _throws: Optional[JContainer[NameTree]],
        _body: Optional[Block],
        _default_value: Optional[JLeftPadded[Expression]],
        _method_type: Optional[JavaType.Method],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
        _annotations: weakref.ReferenceType[AnnotationsHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        leading_annotations: List[Annotation] = ...,
        modifiers: List[Modifier] = ...,
        type_parameters: Optional[TypeParameters] = ...,
        return_type_expression: Optional[TypeTree] = ...,
        name_annotations: List[Annotation] = ...,
        name: Identifier = ...,
        parameters: JContainer[Statement] = ...,
        throws: Optional[JContainer[NameTree]] = ...,
        body: Optional[Block] = ...,
        default_value: Optional[JLeftPadded[Expression]] = ...,
        method_type: Optional[JavaType.Method] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
        annotations: weakref.ReferenceType[AnnotationsHelper] = ...,
    ) -> Self: ...

class MethodInvocation(Statement, TypedTree, MethodCall):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _select: Optional[JRightPadded[Expression]]
    _type_parameters: Optional[JContainer[Expression]]
    _name: Identifier
    _arguments: JContainer[Expression]
    _method_type: Optional[JavaType.Method]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _select: Optional[JRightPadded[Expression]],
        _type_parameters: Optional[JContainer[Expression]],
        _name: Identifier,
        _arguments: JContainer[Expression],
        _method_type: Optional[JavaType.Method],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        select: Optional[JRightPadded[Expression]] = ...,
        type_parameters: Optional[JContainer[Expression]] = ...,
        name: Identifier = ...,
        arguments: JContainer[Expression] = ...,
        method_type: Optional[JavaType.Method] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Modifier(J):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _keyword: Optional[str]
    _type: Type
    _annotations: List[Annotation]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _keyword: Optional[str],
        _type: Type,
        _annotations: List[Annotation],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        keyword: Optional[str] = ...,
        type_: Type = ...,
        annotations: List[Annotation] = ...,
    ) -> Self: ...

class MultiCatch(TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _alternatives: List[JRightPadded[NameTree]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _alternatives: List[JRightPadded[NameTree]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        alternatives: List[JRightPadded[NameTree]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class NewArray(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type_expression: Optional[TypeTree]
    _dimensions: List[ArrayDimension]
    _initializer: Optional[JContainer[Expression]]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _type_expression: Optional[TypeTree],
        _dimensions: List[ArrayDimension],
        _initializer: Optional[JContainer[Expression]],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_expression: Optional[TypeTree] = ...,
        dimensions: List[ArrayDimension] = ...,
        initializer: Optional[JContainer[Expression]] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ArrayDimension(J):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _index: JRightPadded[Expression]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _index: JRightPadded[Expression],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        index: JRightPadded[Expression] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class NewClass(Statement, TypedTree, MethodCall):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _enclosing: Optional[JRightPadded[Expression]]
    _new: Space
    _clazz: Optional[TypeTree]
    _arguments: JContainer[Expression]
    _body: Optional[Block]
    _constructor_type: Optional[JavaType.Method]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _enclosing: Optional[JRightPadded[Expression]],
        _new: Space,
        _clazz: Optional[TypeTree],
        _arguments: JContainer[Expression],
        _body: Optional[Block],
        _constructor_type: Optional[JavaType.Method],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        enclosing: Optional[JRightPadded[Expression]] = ...,
        new: Space = ...,
        clazz: Optional[TypeTree] = ...,
        arguments: JContainer[Expression] = ...,
        body: Optional[Block] = ...,
        constructor_type: Optional[JavaType.Method] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class NullableType(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _type_tree: JRightPadded[TypeTree]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _type_tree: JRightPadded[TypeTree],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        type_tree: JRightPadded[TypeTree] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Package(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: Expression
    _annotations: List[Annotation]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: Expression,
        _annotations: List[Annotation],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        annotations: List[Annotation] = ...,
    ) -> Self: ...

class ParameterizedType(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _clazz: NameTree
    _type_parameters: Optional[JContainer[Expression]]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _clazz: NameTree,
        _type_parameters: Optional[JContainer[Expression]],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        clazz: NameTree = ...,
        type_parameters: Optional[JContainer[Expression]] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Parentheses(Expression, Generic[J2]):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _tree: JRightPadded[J2]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _tree: JRightPadded[J2],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        tree: JRightPadded[J2] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ControlParentheses(Expression, Generic[J2]):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _tree: JRightPadded[J2]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _tree: JRightPadded[J2],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        tree: JRightPadded[J2] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Primitive(TypeTree, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type: JavaType.Primitive

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _type: JavaType.Primitive,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_: JavaType.Primitive = ...,
    ) -> Self: ...

class Return(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: Optional[Expression]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: Optional[Expression],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Optional[Expression] = ...,
    ) -> Self: ...

class Switch(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _selector: ControlParentheses[Expression]
    _cases: Block

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _selector: ControlParentheses[Expression],
        _cases: Block,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        selector: ControlParentheses[Expression] = ...,
        cases: Block = ...,
    ) -> Self: ...

class SwitchExpression(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _selector: ControlParentheses[Expression]
    _cases: Block
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _selector: ControlParentheses[Expression],
        _cases: Block,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        selector: ControlParentheses[Expression] = ...,
        cases: Block = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class Synchronized(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _lock: ControlParentheses[Expression]
    _body: Block

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _lock: ControlParentheses[Expression],
        _body: Block,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        lock: ControlParentheses[Expression] = ...,
        body: Block = ...,
    ) -> Self: ...

class Ternary(Expression, Statement, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _condition: Expression
    _true_part: JLeftPadded[Expression]
    _false_part: JLeftPadded[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _condition: Expression,
        _true_part: JLeftPadded[Expression],
        _false_part: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        condition: Expression = ...,
        true_part: JLeftPadded[Expression] = ...,
        false_part: JLeftPadded[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Throw(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _exception: Expression

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _exception: Expression,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        exception: Expression = ...,
    ) -> Self: ...

class Try(Statement):
    class Resource(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _variable_declarations: TypedTree
        _terminated_with_semicolon: bool

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _variable_declarations: TypedTree,
            _terminated_with_semicolon: bool,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            variable_declarations: TypedTree = ...,
            terminated_with_semicolon: bool = ...,
        ) -> Self: ...

    class Catch(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _parameter: ControlParentheses[VariableDeclarations]
        _body: Block

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _parameter: ControlParentheses[VariableDeclarations],
            _body: Block,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            parameter: ControlParentheses[VariableDeclarations] = ...,
            body: Block = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _resources: Optional[JContainer[Resource]]
    _body: Block
    _catches: List[Catch]
    _finally: Optional[JLeftPadded[Block]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _resources: Optional[JContainer[Resource]],
        _body: Block,
        _catches: List[Catch],
        _finally: Optional[JLeftPadded[Block]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        resources: Optional[JContainer[Resource]] = ...,
        body: Block = ...,
        catches: List[Catch] = ...,
        finally_: Optional[JLeftPadded[Block]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class TypeCast(Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _clazz: ControlParentheses[TypeTree]
    _expression: Expression

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _clazz: ControlParentheses[TypeTree],
        _expression: Expression,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        clazz: ControlParentheses[TypeTree] = ...,
        expression: Expression = ...,
    ) -> Self: ...

class TypeParameter(J):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _modifiers: List[Modifier]
    _name: Expression
    _bounds: Optional[JContainer[TypeTree]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _modifiers: List[Modifier],
        _name: Expression,
        _bounds: Optional[JContainer[TypeTree]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        modifiers: List[Modifier] = ...,
        name: Expression = ...,
        bounds: Optional[JContainer[TypeTree]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class TypeParameters(J):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _annotations: List[Annotation]
    _type_parameters: List[JRightPadded[TypeParameter]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _annotations: List[Annotation],
        _type_parameters: List[JRightPadded[TypeParameter]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        annotations: List[Annotation] = ...,
        type_parameters: List[JRightPadded[TypeParameter]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Unary(Statement, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _operator: JLeftPadded[Type]
    _expression: Expression
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _operator: JLeftPadded[Type],
        _expression: Expression,
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        operator: JLeftPadded[Type] = ...,
        expression: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class VariableDeclarations(Statement, TypedTree):
    class NamedVariable(NameTree):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _name: Identifier
        _dimensions_after_name: List[JLeftPadded[Space]]
        _initializer: Optional[JLeftPadded[Expression]]
        _variable_type: Optional[JavaType.Variable]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _name: Identifier,
            _dimensions_after_name: List[JLeftPadded[Space]],
            _initializer: Optional[JLeftPadded[Expression]],
            _variable_type: Optional[JavaType.Variable],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            name: Identifier = ...,
            dimensions_after_name: List[JLeftPadded[Space]] = ...,
            initializer: Optional[JLeftPadded[Expression]] = ...,
            variable_type: Optional[JavaType.Variable] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _leading_annotations: List[Annotation]
    _modifiers: List[Modifier]
    _type_expression: Optional[TypeTree]
    _varargs: Optional[Space]
    _dimensions_before_name: List[JLeftPadded[Space]]
    _variables: List[JRightPadded[NamedVariable]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _leading_annotations: List[Annotation],
        _modifiers: List[Modifier],
        _type_expression: Optional[TypeTree],
        _varargs: Optional[Space],
        _dimensions_before_name: List[JLeftPadded[Space]],
        _variables: List[JRightPadded[NamedVariable]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        leading_annotations: List[Annotation] = ...,
        modifiers: List[Modifier] = ...,
        type_expression: Optional[TypeTree] = ...,
        varargs: Optional[Space] = ...,
        dimensions_before_name: List[JLeftPadded[Space]] = ...,
        variables: List[JRightPadded[NamedVariable]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class WhileLoop(Loop):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _condition: ControlParentheses[Expression]
    _body: JRightPadded[Statement]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _condition: ControlParentheses[Expression],
        _body: JRightPadded[Statement],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        condition: ControlParentheses[Expression] = ...,
        body: JRightPadded[Statement] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Wildcard(Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _bound: Optional[JLeftPadded[Bound]]
    _bounded_type: Optional[NameTree]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _bound: Optional[JLeftPadded[Bound]],
        _bounded_type: Optional[NameTree],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        bound: Optional[JLeftPadded[Bound]] = ...,
        bounded_type: Optional[NameTree] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Yield(Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _implicit: bool
    _value: Expression

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _implicit: bool,
        _value: Expression,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        implicit: bool = ...,
        value: Expression = ...,
    ) -> Self: ...

class Unknown(Statement, Expression, TypeTree):
    class Source(J):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _text: str

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _text: str,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            text: str = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _source: Source

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _source: Source,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        source: Source = ...,
    ) -> Self: ...

class Erroneous(Statement, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _text: str

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _text: str,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        text: str = ...,
    ) -> Self: ...
