# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional, Self
from uuid import UUID

from enum import Enum
from pathlib import Path
from rewrite import Checksum, FileAttributes, SourceFile, Tree, TreeVisitor, Markers, Cursor, PrintOutputCapture, PrinterFactory
from rewrite.java import *
from rewrite.python.support_types import Py

class Async(Py, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _statement: Statement

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _statement: Statement,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        statement: Statement = ...,
    ) -> Self: ...

class Await(Py, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: Expression
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: Expression,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class Binary(Py, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _left: Expression
    _operator: JLeftPadded[Type]
    _negation: Optional[Space]
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
        _negation: Optional[Space],
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
        negation: Optional[Space] = ...,
        right: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ChainedAssignment(Py, Statement, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _variables: List[JRightPadded[Expression]]
    _assignment: Expression
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _variables: List[JRightPadded[Expression]],
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
        variables: List[JRightPadded[Expression]] = ...,
        assignment: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ExceptionType(Py, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type: Optional[JavaType]
    _exception_group: bool
    _expression: Expression

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _type: Optional[JavaType],
        _exception_group: bool,
        _expression: Expression,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_: Optional[JavaType] = ...,
        exception_group: bool = ...,
        expression: Expression = ...,
    ) -> Self: ...

class LiteralType(Py, Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _literal: Expression
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _literal: Expression,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        literal: Expression = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class TypeHint(Py, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _type_tree: Expression
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _type_tree: Expression,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_tree: Expression = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class CompilationUnit(Py, JavaSourceFile, SourceFile):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _source_path: Path
    _file_attributes: Optional[FileAttributes]
    _charset_name: Optional[str]
    _charset_bom_marked: bool
    _checksum: Optional[Checksum]
    _imports: List[JRightPadded[Import]]
    _statements: List[JRightPadded[Statement]]
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
        _imports: List[JRightPadded[Import]],
        _statements: List[JRightPadded[Statement]],
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
        imports: List[JRightPadded[Import]] = ...,
        statements: List[JRightPadded[Statement]] = ...,
        eof: Space = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ExpressionStatement(Py, Expression, Statement):
    _id: UUID
    _expression: Expression

    def __init__(
        self,
        _id: UUID,
        _expression: Expression,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        expression: Expression = ...,
    ) -> Self: ...

class ExpressionTypeTree(Py, Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _reference: J

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _reference: J,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        reference: J = ...,
    ) -> Self: ...

class StatementExpression(Py, Expression, Statement):
    _id: UUID
    _statement: Statement

    def __init__(
        self,
        _id: UUID,
        _statement: Statement,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        statement: Statement = ...,
    ) -> Self: ...

class MultiImport(Py, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _from: Optional[JRightPadded[NameTree]]
    _parenthesized: bool
    _names: JContainer[Import]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _from: Optional[JRightPadded[NameTree]],
        _parenthesized: bool,
        _names: JContainer[Import],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        from_: Optional[JRightPadded[NameTree]] = ...,
        parenthesized: bool = ...,
        names: JContainer[Import] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class KeyValue(Py, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _key: JRightPadded[Expression]
    _value: Expression
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _key: JRightPadded[Expression],
        _value: Expression,
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        key: JRightPadded[Expression] = ...,
        value: Expression = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class DictLiteral(Py, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _elements: JContainer[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _elements: JContainer[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        elements: JContainer[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class CollectionLiteral(Py, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _kind: Kind
    _elements: JContainer[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _elements: JContainer[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        elements: JContainer[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class FormattedString(Py, Expression, TypedTree):
    class Value(Py, Expression, TypedTree):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _expression: JRightPadded[Expression]
        _debug: Optional[JRightPadded[bool]]
        _conversion: Optional[Conversion]
        _format: Optional[Expression]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _expression: JRightPadded[Expression],
            _debug: Optional[JRightPadded[bool]],
            _conversion: Optional[Conversion],
            _format: Optional[Expression],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            expression: JRightPadded[Expression] = ...,
            debug: Optional[JRightPadded[bool]] = ...,
            conversion: Optional[Conversion] = ...,
            format: Optional[Expression] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _delimiter: str
    _parts: List[Expression]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _delimiter: str,
        _parts: List[Expression],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        delimiter: str = ...,
        parts: List[Expression] = ...,
    ) -> Self: ...

class Pass(Py, Statement):
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

class TrailingElseWrapper(Py, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _statement: Statement
    _else_block: JLeftPadded[Block]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _statement: Statement,
        _else_block: JLeftPadded[Block],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        statement: Statement = ...,
        else_block: JLeftPadded[Block] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class ComprehensionExpression(Py, Expression):
    class Condition(Py):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _expression: Expression

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _expression: Expression,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            expression: Expression = ...,
        ) -> Self: ...

    class Clause(Py):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _async: Optional[JRightPadded[bool]]
        _iterator_variable: Expression
        _iterated_list: JLeftPadded[Expression]
        _conditions: Optional[List[ComprehensionExpression.Condition]]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _async: Optional[JRightPadded[bool]],
            _iterator_variable: Expression,
            _iterated_list: JLeftPadded[Expression],
            _conditions: Optional[List[ComprehensionExpression.Condition]],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            async_: Optional[JRightPadded[bool]] = ...,
            iterator_variable: Expression = ...,
            iterated_list: JLeftPadded[Expression] = ...,
            conditions: Optional[List[ComprehensionExpression.Condition]] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _kind: Kind
    _result: Expression
    _clauses: List[Clause]
    _suffix: Space
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _result: Expression,
        _clauses: List[Clause],
        _suffix: Space,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        result: Expression = ...,
        clauses: List[Clause] = ...,
        suffix: Space = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class TypeAlias(Py, Statement, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _name: Identifier
    _type_parameters: Optional[JContainer[j.TypeParameter]]
    _value: JLeftPadded[J]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _name: Identifier,
        _type_parameters: Optional[JContainer[j.TypeParameter]],
        _value: JLeftPadded[J],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        name: Identifier = ...,
        type_parameters: Optional[JContainer[j.TypeParameter]] = ...,
        value: JLeftPadded[J] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class YieldFrom(Py, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: Expression
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: Expression,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class UnionType(Py, Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _types: List[JRightPadded[Expression]]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _types: List[JRightPadded[Expression]],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        types: List[JRightPadded[Expression]] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class VariableScope(Py, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _kind: Kind
    _names: List[JRightPadded[Identifier]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _names: List[JRightPadded[Identifier]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        names: List[JRightPadded[Identifier]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Del(Py, Statement):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _targets: List[JRightPadded[Expression]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _targets: List[JRightPadded[Expression]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        targets: List[JRightPadded[Expression]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class SpecialParameter(Py, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _kind: Kind
    _type_hint: Optional[TypeHint]
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _type_hint: Optional[TypeHint],
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        type_hint: Optional[TypeHint] = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class Star(Py, Expression, TypeTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _kind: Kind
    _expression: Expression
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _expression: Expression,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        expression: Expression = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class NamedArgument(Py, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _name: Identifier
    _value: JLeftPadded[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _name: Identifier,
        _value: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        name: Identifier = ...,
        value: JLeftPadded[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class TypeHintedExpression(Py, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _expression: Expression
    _type_hint: TypeHint
    _type: Optional[JavaType]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _expression: Expression,
        _type_hint: TypeHint,
        _type: Optional[JavaType],
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type_hint: TypeHint = ...,
        type_: Optional[JavaType] = ...,
    ) -> Self: ...

class ErrorFrom(Py, Expression):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _error: Expression
    _from: JLeftPadded[Expression]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _error: Expression,
        _from: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        error: Expression = ...,
        from_: JLeftPadded[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class MatchCase(Py, Expression):
    class Pattern(Py, Expression):
        _id: UUID
        _prefix: Space
        _markers: Markers
        _kind: Kind
        _children: JContainer[J]
        _type: Optional[JavaType]
        _padding: weakref.ReferenceType[PaddingHelper]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _kind: Kind,
            _children: JContainer[J],
            _type: Optional[JavaType],
            _padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> None: ...

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            kind: Kind = ...,
            children: JContainer[J] = ...,
            type_: Optional[JavaType] = ...,
            padding: weakref.ReferenceType[PaddingHelper] = ...,
        ) -> Self: ...

    _id: UUID
    _prefix: Space
    _markers: Markers
    _pattern: Pattern
    _guard: Optional[JLeftPadded[Expression]]
    _type: Optional[JavaType]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _pattern: Pattern,
        _guard: Optional[JLeftPadded[Expression]],
        _type: Optional[JavaType],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        pattern: Pattern = ...,
        guard: Optional[JLeftPadded[Expression]] = ...,
        type_: Optional[JavaType] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...

class Slice(Py, Expression, TypedTree):
    _id: UUID
    _prefix: Space
    _markers: Markers
    _start: Optional[JRightPadded[Expression]]
    _stop: Optional[JRightPadded[Expression]]
    _step: Optional[JRightPadded[Expression]]
    _padding: weakref.ReferenceType[PaddingHelper]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _start: Optional[JRightPadded[Expression]],
        _stop: Optional[JRightPadded[Expression]],
        _step: Optional[JRightPadded[Expression]],
        _padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        start: Optional[JRightPadded[Expression]] = ...,
        stop: Optional[JRightPadded[Expression]] = ...,
        step: Optional[JRightPadded[Expression]] = ...,
        padding: weakref.ReferenceType[PaddingHelper] = ...,
    ) -> Self: ...
