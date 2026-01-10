# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from pathlib import Path
from typing import Any, List, Optional, Self
from uuid import UUID

from rewrite.java.support_types import J, Expression, Statement, TypedTree, TypeTree, NameTree, Loop, Space, \
    JRightPadded, JLeftPadded, JContainer
from rewrite.markers import Markers
from rewrite.python.support_types import Py
from rewrite.tree import Checksum, FileAttributes, SourceFile


class Async(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    statement: Statement

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        statement: Statement = ...,
    ) -> Self: ...

class Await(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class Binary(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    left: Expression
    operator: JLeftPadded[Type]
    negation: Optional[Space]
    right: Expression
    type: Optional[Any]

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
        type: Optional[Any] = ...,
    ) -> Self: ...

class ChainedAssignment(Py, Statement, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    variables: List[JRightPadded[Expression]]
    assignment: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        variables: List[JRightPadded[Expression]] = ...,
        assignment: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class CollectionLiteral(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    collection_kind: Kind
    elements: JContainer[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        collection_kind: Kind = ...,
        elements: JContainer[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class CompilationUnit(Py, SourceFile):
    id: UUID
    prefix: Space
    markers: Markers
    source_path: Path
    charset_bom_marked: bool
    imports: List[JRightPadded[Statement]]
    statements: List[JRightPadded[Statement]]
    eof: Space
    file_attributes: Optional[FileAttributes]
    charset_name: Optional[str]
    checksum: Optional[Checksum]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        source_path: Path = ...,
        charset_bom_marked: bool = ...,
        imports: List[JRightPadded[Statement]] = ...,
        statements: List[JRightPadded[Statement]] = ...,
        eof: Space = ...,
        file_attributes: Optional[FileAttributes] = ...,
        charset_name: Optional[str] = ...,
        checksum: Optional[Checksum] = ...,
    ) -> Self: ...

class ComprehensionExpression(Py, Expression):
    class Condition:
        id: UUID
        prefix: Space
        markers: Markers
        expression: Expression

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            expression: Expression = ...,
        ) -> Self: ...

    class Clause:
        id: UUID
        prefix: Space
        markers: Markers
        iterator_variable: Expression
        iterated_list: JLeftPadded[Expression]
        conditions: List[Condition]

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            iterator_variable: Expression = ...,
            iterated_list: JLeftPadded[Expression] = ...,
            conditions: List[Condition] = ...,
        ) -> Self: ...

    id: UUID
    prefix: Space
    markers: Markers
    comprehension_kind: Kind
    result: Expression
    clauses: List[Clause]
    suffix: Space
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        comprehension_kind: Kind = ...,
        result: Expression = ...,
        clauses: List[Clause] = ...,
        suffix: Space = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class Del(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    targets: List[JRightPadded[Expression]]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        targets: List[JRightPadded[Expression]] = ...,
    ) -> Self: ...

class DictLiteral(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    elements: JContainer[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        elements: JContainer[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class ErrorFrom(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    error: Expression
    from_: JLeftPadded[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        error: Expression = ...,
        from_: JLeftPadded[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class ExceptionType(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    exception_group: bool
    expression: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        exception_group: bool = ...,
        expression: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class ExpressionStatement(Py, Expression, Statement):
    id: UUID
    expression: Expression

    def replace(
        self,
        *,
        id: UUID = ...,
        expression: Expression = ...,
    ) -> Self: ...

class ExpressionTypeTree(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    reference: J

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        reference: J = ...,
    ) -> Self: ...

class FormattedString(Py, Expression, TypedTree):
    class Value:
        id: UUID
        prefix: Space
        markers: Markers
        expression: JRightPadded[Expression]
        debug: Optional[Space]
        conversion: Conversion
        format: Optional[Expression]

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            expression: JRightPadded[Expression] = ...,
            debug: Optional[Space] = ...,
            conversion: Conversion = ...,
            format: Optional[Expression] = ...,
        ) -> Self: ...

    id: UUID
    prefix: Space
    markers: Markers
    delimiter: str
    parts: List[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        delimiter: str = ...,
        parts: List[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class KeyValue(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    key: JRightPadded[Expression]
    value: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        key: JRightPadded[Expression] = ...,
        value: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class LiteralType(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    literal: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        literal: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class MatchCase(Py, Expression):
    class Pattern:
        id: UUID
        prefix: Space
        markers: Markers
        pattern_kind: Kind
        children: JContainer[Expression]
        type: Optional[Any]

        def replace(
            self,
            *,
            id: UUID = ...,
            prefix: Space = ...,
            markers: Markers = ...,
            pattern_kind: Kind = ...,
            children: JContainer[Expression] = ...,
            type: Optional[Any] = ...,
        ) -> Self: ...

    id: UUID
    prefix: Space
    markers: Markers
    pattern: Pattern
    guard: Optional[JLeftPadded[Expression]]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        pattern: Pattern = ...,
        guard: Optional[JLeftPadded[Expression]] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class MultiImport(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    from_: Optional[JRightPadded[NameTree]]
    parenthesized: bool
    names: JContainer[Statement]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        from_: Optional[JRightPadded[NameTree]] = ...,
        parenthesized: bool = ...,
        names: JContainer[Statement] = ...,
    ) -> Self: ...

class NamedArgument(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    name: J
    value: JLeftPadded[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        name: J = ...,
        value: JLeftPadded[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class Pass(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
    ) -> Self: ...

class Slice(Py, Expression, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    start: Optional[JRightPadded[Expression]]
    stop: Optional[JRightPadded[Expression]]
    step: Optional[JRightPadded[Expression]]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        start: Optional[JRightPadded[Expression]] = ...,
        stop: Optional[JRightPadded[Expression]] = ...,
        step: Optional[JRightPadded[Expression]] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class SpecialParameter(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    parameter_kind: Kind
    type_hint: Optional[TypeHint]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        parameter_kind: Kind = ...,
        type_hint: Optional[TypeHint] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class Star(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    star_kind: Kind
    expression: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        star_kind: Kind = ...,
        expression: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class StatementExpression(Py, Expression, Statement):
    id: UUID
    statement: Statement

    def replace(
        self,
        *,
        id: UUID = ...,
        statement: Statement = ...,
    ) -> Self: ...

class TrailingElseWrapper(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    statement: JRightPadded[Statement]
    else_block: J

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        statement: JRightPadded[Statement] = ...,
        else_block: J = ...,
    ) -> Self: ...

class TypeAlias(Py, Statement, TypedTree):
    id: UUID
    prefix: Space
    markers: Markers
    name: J
    type_parameters: Optional[JContainer[J]]
    value: JLeftPadded[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        name: J = ...,
        type_parameters: Optional[JContainer[J]] = ...,
        value: JLeftPadded[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class TypeHint(Py, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    type_tree: Expression
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        type_tree: Expression = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class TypeHintedExpression(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type_hint: TypeHint

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type_hint: TypeHint = ...,
    ) -> Self: ...

class UnionType(Py, Expression, TypeTree):
    id: UUID
    prefix: Space
    markers: Markers
    types: List[JRightPadded[Expression]]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        types: List[JRightPadded[Expression]] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...

class VariableScope(Py, Statement):
    id: UUID
    prefix: Space
    markers: Markers
    scope_kind: Kind
    names: List[JRightPadded[J]]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        scope_kind: Kind = ...,
        names: List[JRightPadded[J]] = ...,
    ) -> Self: ...

class YieldFrom(Py, Expression):
    id: UUID
    prefix: Space
    markers: Markers
    expression: JLeftPadded[Expression]
    type: Optional[Any]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: JLeftPadded[Expression] = ...,
        type: Optional[Any] = ...,
    ) -> Self: ...
