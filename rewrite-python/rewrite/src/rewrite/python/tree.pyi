# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID

from enum import Enum
from pathlib import Path
from rewrite import Checksum, FileAttributes, SourceFile, Tree, TreeVisitor, Markers, Cursor, PrintOutputCapture, PrinterFactory
from rewrite.java import *
from rewrite.python.support_types import Py

class Async(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    statement: Statement

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Await(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Binary(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    left: Expression
    operator: JLeftPadded[Type]
    negation: Optional[Space]
    right: Expression
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

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
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ChainedAssignment(Py, Statement, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    variables: List[JRightPadded[Expression]]
    assignment: Expression
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _variables: List[JRightPadded[Expression]],
        _assignment: Expression,
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ExceptionType(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    type_: Optional[JavaType]
    exception_group: bool
    expression: Expression

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class LiteralType(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    literal: Expression
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class TypeHint(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    type_tree: Expression
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class CompilationUnit(Py, JavaSourceFile, SourceFile):
    id: UUID
    prefix: Space
    markers: Markers
    source_path: Path
    file_attributes: Optional[FileAttributes]
    charset_name: Optional[str]
    charset_bom_marked: bool
    checksum: Optional[Checksum]
    imports: List[JRightPadded[Import]]
    statements: List[JRightPadded[Statement]]
    eof: Space
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

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
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def printer(self, cursor: Cursor) -> TreeVisitor: ...
    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ExpressionStatement(Py, Expression, Statement):
    id: UUID
    expression: Expression

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ExpressionTypeTree(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    reference: J

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class StatementExpression(Py, Expression, Statement):
    id: UUID
    statement: Statement

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class MultiImport(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    from_: Optional[JRightPadded[NameTree]]
    parenthesized: bool
    names: JContainer[Import]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _from: Optional[JRightPadded[NameTree]],
        _parenthesized: bool,
        _names: JContainer[Import],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class KeyValue(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    key: JRightPadded[Expression]
    value: Expression
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _key: JRightPadded[Expression],
        _value: Expression,
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class DictLiteral(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    elements: JContainer[Expression]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _elements: JContainer[Expression],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        elements: JContainer[Expression] = ...,
        type_: Optional[JavaType] = ...,
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class CollectionLiteral(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    kind: Kind
    elements: JContainer[Expression]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _elements: JContainer[Expression],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class FormattedString(Py, Expression, TypedTree):
    class Value(Py, Expression, TypedTree):
        id: UUID
        prefix: Space
        markers: Markers
        expression: JRightPadded[Expression]
        debug: Optional[JRightPadded[bool]]
        conversion: Optional[Conversion]
        format: Optional[Expression]
        padding: Optional[weakref.ReferenceType[PaddingHelper]]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _expression: JRightPadded[Expression],
            _debug: Optional[JRightPadded[bool]],
            _conversion: Optional[Conversion],
            _format: Optional[Expression],
            _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
            padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
        ) -> Self: ...

        def with_id(self, id: UUID) -> FormattedString.Value: ...
        def with_prefix(self, prefix: Space) -> FormattedString.Value: ...
        def with_markers(self, markers: Markers) -> FormattedString.Value: ...
        def with_expression(self, expression: Expression) -> FormattedString.Value: ...
        def with_debug(self, debug: Optional[bool]) -> FormattedString.Value: ...
        def with_conversion(self, conversion: Optional[Conversion]) -> FormattedString.Value: ...
        def with_format(self, format: Optional[Expression]) -> FormattedString.Value: ...
        def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

    id: UUID
    prefix: Space
    markers: Markers
    delimiter: str
    parts: List[Expression]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Pass(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class TrailingElseWrapper(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    statement: Statement
    else_block: JLeftPadded[Block]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _statement: Statement,
        _else_block: JLeftPadded[Block],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        statement: Statement = ...,
        else_block: JLeftPadded[Block] = ...,
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ComprehensionExpression(Py, Expression):
    class Condition(Py):
        id: UUID
        prefix: Space
        markers: Markers
        expression: Expression

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

        def with_id(self, id: UUID) -> ComprehensionExpression.Condition: ...
        def with_prefix(self, prefix: Space) -> ComprehensionExpression.Condition: ...
        def with_markers(self, markers: Markers) -> ComprehensionExpression.Condition: ...
        def with_expression(self, expression: Expression) -> ComprehensionExpression.Condition: ...
        def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

    class Clause(Py):
        id: UUID
        prefix: Space
        markers: Markers
        async_: Optional[JRightPadded[bool]]
        iterator_variable: Expression
        iterated_list: JLeftPadded[Expression]
        conditions: Optional[List[ComprehensionExpression.Condition]]
        padding: Optional[weakref.ReferenceType[PaddingHelper]]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _async: Optional[JRightPadded[bool]],
            _iterator_variable: Expression,
            _iterated_list: JLeftPadded[Expression],
            _conditions: Optional[List[ComprehensionExpression.Condition]],
            _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
            padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
        ) -> Self: ...

        def with_id(self, id: UUID) -> ComprehensionExpression.Clause: ...
        def with_prefix(self, prefix: Space) -> ComprehensionExpression.Clause: ...
        def with_markers(self, markers: Markers) -> ComprehensionExpression.Clause: ...
        def with_async(self, async_: Optional[bool]) -> ComprehensionExpression.Clause: ...
        def with_iterator_variable(self, iterator_variable: Expression) -> ComprehensionExpression.Clause: ...
        def with_iterated_list(self, iterated_list: Expression) -> ComprehensionExpression.Clause: ...
        def with_conditions(self, conditions: Optional[List[ComprehensionExpression.Condition]]) -> ComprehensionExpression.Clause: ...
        def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

    id: UUID
    prefix: Space
    markers: Markers
    kind: Kind
    result: Expression
    clauses: List[Clause]
    suffix: Space
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class TypeAlias(Py, Statement, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    name: Identifier
    type_parameters: Optional[JContainer[j.TypeParameter]]
    value: JLeftPadded[J]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _name: Identifier,
        _type_parameters: Optional[JContainer[j.TypeParameter]],
        _value: JLeftPadded[J],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class YieldFrom(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class UnionType(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    types: List[JRightPadded[Expression]]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _types: List[JRightPadded[Expression]],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        types: List[JRightPadded[Expression]] = ...,
        type_: Optional[JavaType] = ...,
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class VariableScope(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    kind: Kind
    names: List[JRightPadded[Identifier]]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _kind: Kind,
        _names: List[JRightPadded[Identifier]],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        kind: Kind = ...,
        names: List[JRightPadded[Identifier]] = ...,
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Del(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    targets: List[JRightPadded[Expression]]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _targets: List[JRightPadded[Expression]],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> None: ...

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        targets: List[JRightPadded[Expression]] = ...,
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class SpecialParameter(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    kind: Kind
    type_hint: Optional[TypeHint]
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Star(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    kind: Kind
    expression: Expression
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class NamedArgument(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    name: Identifier
    value: JLeftPadded[Expression]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _name: Identifier,
        _value: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class TypeHintedExpression(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type_hint: TypeHint
    type_: Optional[JavaType]

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

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class ErrorFrom(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    error: Expression
    from_: JLeftPadded[Expression]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _error: Expression,
        _from: JLeftPadded[Expression],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class MatchCase(Py, Expression):
    class Pattern(Py, Expression):
        id: UUID
        prefix: Space
        markers: Markers
        kind: Kind
        children: JContainer[J]
        type_: Optional[JavaType]
        padding: Optional[weakref.ReferenceType[PaddingHelper]]

        def __init__(
            self,
            _id: UUID,
            _prefix: Space,
            _markers: Markers,
            _kind: Kind,
            _children: JContainer[J],
            _type: Optional[JavaType],
            _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
            padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
        ) -> Self: ...

        def with_id(self, id: UUID) -> MatchCase.Pattern: ...
        def with_prefix(self, prefix: Space) -> MatchCase.Pattern: ...
        def with_markers(self, markers: Markers) -> MatchCase.Pattern: ...
        def with_kind(self, kind: Kind) -> MatchCase.Pattern: ...
        def with_children(self, children: List[J]) -> MatchCase.Pattern: ...
        def with_type(self, type: Optional[JavaType]) -> MatchCase.Pattern: ...
        def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

    id: UUID
    prefix: Space
    markers: Markers
    pattern: Pattern
    guard: Optional[JLeftPadded[Expression]]
    type_: Optional[JavaType]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _pattern: Pattern,
        _guard: Optional[JLeftPadded[Expression]],
        _type: Optional[JavaType],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...

class Slice(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    start: Optional[JRightPadded[Expression]]
    stop: Optional[JRightPadded[Expression]]
    step: Optional[JRightPadded[Expression]]
    padding: Optional[weakref.ReferenceType[PaddingHelper]]

    def __init__(
        self,
        _id: UUID,
        _prefix: Space,
        _markers: Markers,
        _start: Optional[JRightPadded[Expression]],
        _stop: Optional[JRightPadded[Expression]],
        _step: Optional[JRightPadded[Expression]],
        _padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
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
        padding: Optional[weakref.ReferenceType[PaddingHelper]] = ...,
    ) -> Self: ...

    def accept_python(self, v: PythonVisitor[P], p: P) -> J: ...
