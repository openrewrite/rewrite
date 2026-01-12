# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Python visitor for traversing and transforming Python LST nodes."""

from __future__ import annotations

from typing import TYPE_CHECKING, Any, Optional, TypeVar, cast

from rewrite.java.support_types import (
    J,
    Expression,
    Statement,
    Space,
    JRightPadded,
    JLeftPadded,
    JContainer,
)
from rewrite.java.visitor import JavaVisitor
from rewrite.python.support_types import Py, PySpace
from rewrite.tree import SourceFile

if TYPE_CHECKING:
    from rewrite.visitor import Cursor
    from rewrite.python.tree import (
        Async,
        Await,
        Binary,
        ChainedAssignment,
        CollectionLiteral,
        CompilationUnit,
        ComprehensionExpression,
        Del,
        DictLiteral,
        ErrorFrom,
        ExceptionType,
        ExpressionStatement,
        ExpressionTypeTree,
        FormattedString,
        KeyValue,
        LiteralType,
        MatchCase,
        MultiImport,
        NamedArgument,
        Pass,
        Slice,
        SpecialParameter,
        Star,
        StatementExpression,
        TrailingElseWrapper,
        TypeAlias,
        TypeHint,
        TypeHintedExpression,
        UnionType,
        VariableScope,
        YieldFrom,
    )

P = TypeVar("P")
T = TypeVar("T")


def list_map(func, items):
    """Apply a function to each item in a list, returning original if unchanged."""
    if items is None:
        return None
    result = [func(item) for item in items]
    if all(r is o for r, o in zip(result, items)):
        return items
    return result


class PythonVisitor(JavaVisitor[P]):
    """
    Base visitor for Python LST nodes.

    Extends JavaVisitor to handle Python-specific syntax while inheriting
    visit methods for Java constructs (identifiers, literals, etc.) that
    are reused in Python.
    """

    def is_acceptable(self, source_file: SourceFile, p: P) -> bool:
        """Check if this visitor can handle the given source file."""
        return isinstance(source_file, Py)

    # -------------------------------------------------------------------------
    # Python-specific visit methods
    # -------------------------------------------------------------------------

    def visit_async(self, async_: Async, p: P) -> J:
        """Visit an async statement."""
        async_ = async_.replace(
            prefix=self.visit_space(async_.prefix, PySpace.Location.ASYNC_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(async_, p))
        if not isinstance(temp_stmt, type(async_)):
            return temp_stmt
        async_ = cast("Async", temp_stmt)
        async_ = async_.replace(markers=self.visit_markers(async_.markers, p))
        async_ = async_.replace(
            statement=self.visit_and_cast(async_.statement, Statement, p)
        )
        return async_

    def visit_await(self, await_: Await, p: P) -> J:
        """Visit an await expression."""
        await_ = await_.replace(
            prefix=self.visit_space(await_.prefix, PySpace.Location.AWAIT_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(await_, p))
        if not isinstance(temp_expr, type(await_)):
            return temp_expr
        await_ = cast("Await", temp_expr)
        await_ = await_.replace(markers=self.visit_markers(await_.markers, p))
        await_ = await_.replace(
            expression=self.visit_and_cast(await_.expression, Expression, p)
        )
        return await_

    def visit_python_binary(self, binary: Binary, p: P) -> J:
        """Visit a Python-specific binary expression."""
        binary = binary.replace(
            prefix=self.visit_space(binary.prefix, PySpace.Location.BINARY_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(binary, p))
        if not isinstance(temp_expr, type(binary)):
            return temp_expr
        binary = cast("Binary", temp_expr)
        binary = binary.replace(markers=self.visit_markers(binary.markers, p))
        binary = binary.replace(
            left=self.visit_and_cast(binary.left, Expression, p)
        )
        binary = binary.replace(
            operator=self.visit_left_padded(binary.operator, None, p)
        )
        binary = binary.replace(
            negation=self.visit_space(binary.negation, PySpace.Location.BINARY_NEGATION, p)
        )
        binary = binary.replace(
            right=self.visit_and_cast(binary.right, Expression, p)
        )
        return binary

    def visit_chained_assignment(self, chained: ChainedAssignment, p: P) -> J:
        """Visit a chained assignment statement."""
        chained = chained.replace(
            prefix=self.visit_space(chained.prefix, PySpace.Location.CHAINED_ASSIGNMENT_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(chained, p))
        if not isinstance(temp_stmt, type(chained)):
            return temp_stmt
        chained = cast("ChainedAssignment", temp_stmt)
        chained = chained.replace(markers=self.visit_markers(chained.markers, p))
        chained = chained.padding.replace(variables=
            list_map(
                lambda v: self.visit_right_padded(v, None, p),
                chained.padding.variables
            )
        )
        chained = chained.replace(
            assignment=self.visit_and_cast(chained.assignment, Expression, p)
        )
        return chained

    def visit_collection_literal(self, collection: CollectionLiteral, p: P) -> J:
        """Visit a collection literal (list, set, tuple)."""
        collection = collection.replace(
            prefix=self.visit_space(collection.prefix, PySpace.Location.COLLECTION_LITERAL_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(collection, p))
        if not isinstance(temp_expr, type(collection)):
            return temp_expr
        collection = cast("CollectionLiteral", temp_expr)
        collection = collection.replace(markers=self.visit_markers(collection.markers, p))
        collection = collection.replace(
            elements=self.visit_container(collection.padding.elements, None, p)
        )
        return collection

    def visit_compilation_unit(self, cu: CompilationUnit, p: P) -> J:
        """Visit a Python compilation unit (module)."""
        cu = cu.replace(prefix=self.visit_space(cu.prefix, Space.Location.ANY, p))
        cu = cu.replace(markers=self.visit_markers(cu.markers, p))
        cu = cu.padding.replace(imports=
            list_map(
                lambda v: self.visit_right_padded(v, None, p),
                cu.padding.imports
            )
        )
        cu = cu.padding.replace(statements=
            list_map(
                lambda v: self.visit_right_padded(v, None, p),
                cu.padding.statements
            )
        )
        cu = cu.replace(eof=self.visit_space(cu.eof, Space.Location.ANY, p))
        return cu

    def visit_comprehension_expression(self, comp: ComprehensionExpression, p: P) -> J:
        """Visit a comprehension expression."""
        comp = comp.replace(
            prefix=self.visit_space(comp.prefix, PySpace.Location.COMPREHENSION_EXPRESSION_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(comp, p))
        if not isinstance(temp_expr, type(comp)):
            return temp_expr
        comp = cast("ComprehensionExpression", temp_expr)
        comp = comp.replace(markers=self.visit_markers(comp.markers, p))
        comp = comp.replace(
            result=self.visit_and_cast(comp.result, Expression, p)
        )
        comp = comp.replace(
            clauses=list_map(
                lambda c: self.visit_comprehension_clause(c, p),
                comp.clauses
            )
        )
        return comp

    def visit_comprehension_clause(self, clause: ComprehensionExpression.Clause, p: P) -> ComprehensionExpression.Clause:
        """Visit a comprehension clause."""
        clause = clause.replace(
            prefix=self.visit_space(clause.prefix, None, p)
        )
        clause = clause.replace(markers=self.visit_markers(clause.markers, p))
        clause = clause.replace(
            iterator_variable=self.visit_and_cast(clause.iterator_variable, Expression, p)
        )
        clause = clause.replace(
            iterated_list=self.visit_left_padded(clause.iterated_list, None, p)
        )
        clause = clause.replace(
            conditions=list_map(
                lambda c: self.visit_comprehension_condition(c, p),
                clause.conditions
            )
        )
        return clause

    def visit_comprehension_condition(self, condition: ComprehensionExpression.Condition, p: P) -> ComprehensionExpression.Condition:
        """Visit a comprehension condition."""
        condition = condition.replace(
            prefix=self.visit_space(condition.prefix, None, p)
        )
        condition = condition.replace(markers=self.visit_markers(condition.markers, p))
        condition = condition.replace(
            expression=self.visit_and_cast(condition.expression, Expression, p)
        )
        return condition

    def visit_del(self, del_: Del, p: P) -> J:
        """Visit a del statement."""
        del_ = del_.replace(
            prefix=self.visit_space(del_.prefix, PySpace.Location.DEL_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(del_, p))
        if not isinstance(temp_stmt, type(del_)):
            return temp_stmt
        del_ = cast("Del", temp_stmt)
        del_ = del_.replace(markers=self.visit_markers(del_.markers, p))
        del_ = del_.padding.replace(targets=
            list_map(
                lambda t: self.visit_right_padded(t, None, p),
                del_.padding.targets
            )
        )
        return del_

    def visit_dict_literal(self, dict_lit: DictLiteral, p: P) -> J:
        """Visit a dictionary literal."""
        dict_lit = dict_lit.replace(
            prefix=self.visit_space(dict_lit.prefix, PySpace.Location.DICT_LITERAL_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(dict_lit, p))
        if not isinstance(temp_expr, type(dict_lit)):
            return temp_expr
        dict_lit = cast("DictLiteral", temp_expr)
        dict_lit = dict_lit.replace(markers=self.visit_markers(dict_lit.markers, p))
        dict_lit = dict_lit.replace(
            elements=self.visit_container(dict_lit.padding.elements, None, p)
        )
        return dict_lit

    def visit_error_from(self, error_from: ErrorFrom, p: P) -> J:
        """Visit an error from expression (raise X from Y)."""
        error_from = error_from.replace(
            prefix=self.visit_space(error_from.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(error_from, p))
        if not isinstance(temp_expr, type(error_from)):
            return temp_expr
        error_from = cast("ErrorFrom", temp_expr)
        error_from = error_from.replace(markers=self.visit_markers(error_from.markers, p))
        error_from = error_from.replace(
            error=self.visit_and_cast(error_from.error, Expression, p)
        )
        error_from = error_from.replace(
            from_=self.visit_left_padded(error_from.from_, None, p)
        )
        return error_from

    def visit_exception_type(self, exc_type: ExceptionType, p: P) -> J:
        """Visit an exception type in an except clause."""
        exc_type = exc_type.replace(
            prefix=self.visit_space(exc_type.prefix, None, p)
        )
        exc_type = exc_type.replace(markers=self.visit_markers(exc_type.markers, p))
        exc_type = exc_type.replace(
            expression=self.visit_and_cast(exc_type.expression, Expression, p)
        )
        return exc_type

    def visit_expression_statement(self, expr_stmt: ExpressionStatement, p: P) -> J:
        """Visit an expression used as a statement."""
        temp_stmt = cast(Statement, self.visit_statement(expr_stmt, p))
        if not isinstance(temp_stmt, type(expr_stmt)):
            return temp_stmt
        expr_stmt = cast("ExpressionStatement", temp_stmt)
        temp_expr = cast(Expression, self.visit_expression(expr_stmt, p))
        if not isinstance(temp_expr, type(expr_stmt)):
            return temp_expr
        expr_stmt = cast("ExpressionStatement", temp_expr)
        expr_stmt = expr_stmt.replace(
            expression=self.visit_and_cast(expr_stmt.expression, Expression, p)
        )
        return expr_stmt

    def visit_expression_type_tree(self, expr_tree: ExpressionTypeTree, p: P) -> J:
        """Visit an expression used as a type tree."""
        expr_tree = expr_tree.replace(
            prefix=self.visit_space(expr_tree.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(expr_tree, p))
        if not isinstance(temp_expr, type(expr_tree)):
            return temp_expr
        expr_tree = cast("ExpressionTypeTree", temp_expr)
        expr_tree = expr_tree.replace(markers=self.visit_markers(expr_tree.markers, p))
        expr_tree = expr_tree.replace(
            reference=self.visit_and_cast(expr_tree.reference, J, p)
        )
        return expr_tree

    def visit_formatted_string(self, f_string: FormattedString, p: P) -> J:
        """Visit an f-string."""
        f_string = f_string.replace(
            prefix=self.visit_space(f_string.prefix, PySpace.Location.FORMATTED_STRING_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(f_string, p))
        if not isinstance(temp_expr, type(f_string)):
            return temp_expr
        f_string = cast("FormattedString", temp_expr)
        f_string = f_string.replace(markers=self.visit_markers(f_string.markers, p))
        f_string = f_string.replace(
            parts=list_map(
                lambda part: self.visit_and_cast(part, Expression, p),
                f_string.parts
            )
        )
        return f_string

    def visit_formatted_string_value(self, value: FormattedString.Value, p: P) -> J:
        """Visit a value embedded in an f-string."""
        value = value.replace(
            prefix=self.visit_space(value.prefix, None, p)
        )
        value = value.replace(markers=self.visit_markers(value.markers, p))
        value = value.replace(
            expression=self.visit_right_padded(value.expression, None, p)
        )
        if value.format is not None:
            value = value.replace(
                format=self.visit_and_cast(value.format, Expression, p)
            )
        return value

    def visit_key_value(self, kv: KeyValue, p: P) -> J:
        """Visit a key-value pair."""
        kv = kv.replace(
            prefix=self.visit_space(kv.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(kv, p))
        if not isinstance(temp_expr, type(kv)):
            return temp_expr
        kv = cast("KeyValue", temp_expr)
        kv = kv.replace(markers=self.visit_markers(kv.markers, p))
        kv = kv.replace(
            key=self.visit_right_padded(kv.key, None, p)
        )
        kv = kv.replace(
            value=self.visit_and_cast(kv.value, Expression, p)
        )
        return kv

    def visit_literal_type(self, lit_type: LiteralType, p: P) -> J:
        """Visit a literal used as a type annotation."""
        lit_type = lit_type.replace(
            prefix=self.visit_space(lit_type.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(lit_type, p))
        if not isinstance(temp_expr, type(lit_type)):
            return temp_expr
        lit_type = cast("LiteralType", temp_expr)
        lit_type = lit_type.replace(markers=self.visit_markers(lit_type.markers, p))
        lit_type = lit_type.replace(
            literal=self.visit_and_cast(lit_type.literal, Expression, p)
        )
        return lit_type

    def visit_match_case(self, case: MatchCase, p: P) -> J:
        """Visit a match case."""
        case = case.replace(
            prefix=self.visit_space(case.prefix, PySpace.Location.MATCH_CASE_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(case, p))
        if not isinstance(temp_expr, type(case)):
            return temp_expr
        case = cast("MatchCase", temp_expr)
        case = case.replace(markers=self.visit_markers(case.markers, p))
        case = case.replace(
            pattern=self.visit_match_case_pattern(case.pattern, p)
        )
        if case.guard is not None:
            case = case.replace(
                guard=self.visit_left_padded(case.guard, None, p)
            )
        return case

    def visit_match_case_pattern(self, pattern: MatchCase.Pattern, p: P) -> MatchCase.Pattern:
        """Visit a match case pattern."""
        pattern = pattern.replace(
            prefix=self.visit_space(pattern.prefix, None, p)
        )
        pattern = pattern.replace(markers=self.visit_markers(pattern.markers, p))
        pattern = pattern.replace(
            children=self.visit_container(pattern.padding.children, None, p)
        )
        return pattern

    def visit_multi_import(self, multi: MultiImport, p: P) -> J:
        """Visit a multi-name import statement."""
        multi = multi.replace(
            prefix=self.visit_space(multi.prefix, PySpace.Location.MULTI_IMPORT_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(multi, p))
        if not isinstance(temp_stmt, type(multi)):
            return temp_stmt
        multi = cast("MultiImport", temp_stmt)
        multi = multi.replace(markers=self.visit_markers(multi.markers, p))
        if multi.from_ is not None:
            multi = multi.replace(
                from_=self.visit_right_padded(multi.from_, None, p)
            )
        multi = multi.replace(
            names=self.visit_container(multi.padding.names, None, p)
        )
        return multi

    def visit_named_argument(self, named: NamedArgument, p: P) -> J:
        """Visit a named argument."""
        named = named.replace(
            prefix=self.visit_space(named.prefix, PySpace.Location.NAMED_ARGUMENT_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(named, p))
        if not isinstance(temp_expr, type(named)):
            return temp_expr
        named = cast("NamedArgument", temp_expr)
        named = named.replace(markers=self.visit_markers(named.markers, p))
        named = named.replace(
            name=self.visit_and_cast(named.name, J, p)
        )
        named = named.replace(
            value=self.visit_left_padded(named.value, None, p)
        )
        return named

    def visit_pass(self, pass_: Pass, p: P) -> J:
        """Visit a pass statement."""
        pass_ = pass_.replace(
            prefix=self.visit_space(pass_.prefix, PySpace.Location.PASS_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(pass_, p))
        if not isinstance(temp_stmt, type(pass_)):
            return temp_stmt
        pass_ = cast("Pass", temp_stmt)
        pass_ = pass_.replace(markers=self.visit_markers(pass_.markers, p))
        return pass_

    def visit_slice(self, slice_: Slice, p: P) -> J:
        """Visit a slice expression."""
        slice_ = slice_.replace(
            prefix=self.visit_space(slice_.prefix, PySpace.Location.SLICE_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(slice_, p))
        if not isinstance(temp_expr, type(slice_)):
            return temp_expr
        slice_ = cast("Slice", temp_expr)
        slice_ = slice_.replace(markers=self.visit_markers(slice_.markers, p))
        if slice_.start is not None:
            slice_ = slice_.replace(
                start=self.visit_right_padded(slice_.start, None, p)
            )
        if slice_.stop is not None:
            slice_ = slice_.replace(
                stop=self.visit_right_padded(slice_.stop, None, p)
            )
        if slice_.step is not None:
            slice_ = slice_.replace(
                step=self.visit_right_padded(slice_.step, None, p)
            )
        return slice_

    def visit_special_parameter(self, special: SpecialParameter, p: P) -> J:
        """Visit a special parameter (*args or **kwargs)."""
        special = special.replace(
            prefix=self.visit_space(special.prefix, None, p)
        )
        special = special.replace(markers=self.visit_markers(special.markers, p))
        if special.type_hint is not None:
            special = special.replace(
                type_hint=self.visit_and_cast(special.type_hint, Any, p)
            )
        return special

    def visit_star(self, star: Star, p: P) -> J:
        """Visit a star expression (*x or **x)."""
        star = star.replace(
            prefix=self.visit_space(star.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(star, p))
        if not isinstance(temp_expr, type(star)):
            return temp_expr
        star = cast("Star", temp_expr)
        star = star.replace(markers=self.visit_markers(star.markers, p))
        star = star.replace(
            expression=self.visit_and_cast(star.expression, Expression, p)
        )
        return star

    def visit_statement_expression(self, stmt_expr: StatementExpression, p: P) -> J:
        """Visit a statement used as an expression."""
        temp_stmt = cast(Statement, self.visit_statement(stmt_expr, p))
        if not isinstance(temp_stmt, type(stmt_expr)):
            return temp_stmt
        stmt_expr = cast("StatementExpression", temp_stmt)
        temp_expr = cast(Expression, self.visit_expression(stmt_expr, p))
        if not isinstance(temp_expr, type(stmt_expr)):
            return temp_expr
        stmt_expr = cast("StatementExpression", temp_expr)
        stmt_expr = stmt_expr.replace(
            statement=self.visit_and_cast(stmt_expr.statement, Statement, p)
        )
        return stmt_expr

    def visit_trailing_else_wrapper(self, wrapper: TrailingElseWrapper, p: P) -> J:
        """Visit a trailing else wrapper."""
        wrapper = wrapper.replace(
            prefix=self.visit_space(wrapper.prefix, None, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(wrapper, p))
        if not isinstance(temp_stmt, type(wrapper)):
            return temp_stmt
        wrapper = cast("TrailingElseWrapper", temp_stmt)
        wrapper = wrapper.replace(markers=self.visit_markers(wrapper.markers, p))
        wrapper = wrapper.replace(
            statement=self.visit_right_padded(wrapper.statement, None, p)
        )
        wrapper = wrapper.replace(
            else_block=self.visit_and_cast(wrapper.else_block, J, p)
        )
        return wrapper

    def visit_type_alias(self, alias: TypeAlias, p: P) -> J:
        """Visit a type alias statement."""
        alias = alias.replace(
            prefix=self.visit_space(alias.prefix, PySpace.Location.TYPE_ALIAS_PREFIX, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(alias, p))
        if not isinstance(temp_stmt, type(alias)):
            return temp_stmt
        alias = cast("TypeAlias", temp_stmt)
        alias = alias.replace(markers=self.visit_markers(alias.markers, p))
        alias = alias.replace(
            name=self.visit_and_cast(alias.name, J, p)
        )
        if alias.padding.type_parameters is not None:
            alias = alias.replace(
                type_parameters=self.visit_container(alias.padding.type_parameters, None, p)
            )
        alias = alias.replace(
            value=self.visit_left_padded(alias.value, None, p)
        )
        return alias

    def visit_type_hint(self, hint: TypeHint, p: P) -> J:
        """Visit a type hint annotation."""
        hint = hint.replace(
            prefix=self.visit_space(hint.prefix, PySpace.Location.TYPE_HINT_PREFIX, p)
        )
        hint = hint.replace(markers=self.visit_markers(hint.markers, p))
        hint = hint.replace(
            type_tree=self.visit_and_cast(hint.type_tree, Expression, p)
        )
        return hint

    def visit_type_hinted_expression(self, hinted: TypeHintedExpression, p: P) -> J:
        """Visit a type-hinted expression."""
        hinted = hinted.replace(
            prefix=self.visit_space(hinted.prefix, None, p)
        )
        temp_expr = cast(Expression, self.visit_expression(hinted, p))
        if not isinstance(temp_expr, type(hinted)):
            return temp_expr
        hinted = cast("TypeHintedExpression", temp_expr)
        hinted = hinted.replace(markers=self.visit_markers(hinted.markers, p))
        hinted = hinted.replace(
            expression=self.visit_and_cast(hinted.expression, Expression, p)
        )
        hinted = hinted.replace(
            type_hint=self.visit_and_cast(hinted.type_hint, Any, p)
        )
        return hinted

    def visit_union_type(self, union: UnionType, p: P) -> J:
        """Visit a union type."""
        union = union.replace(
            prefix=self.visit_space(union.prefix, PySpace.Location.UNION_TYPE_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(union, p))
        if not isinstance(temp_expr, type(union)):
            return temp_expr
        union = cast("UnionType", temp_expr)
        union = union.replace(markers=self.visit_markers(union.markers, p))
        union = union.padding.replace(types=
            list_map(
                lambda t: self.visit_right_padded(t, None, p),
                union.padding.types
            )
        )
        return union

    def visit_variable_scope(self, scope: VariableScope, p: P) -> J:
        """Visit a variable scope declaration (global/nonlocal)."""
        scope = scope.replace(
            prefix=self.visit_space(scope.prefix, None, p)
        )
        temp_stmt = cast(Statement, self.visit_statement(scope, p))
        if not isinstance(temp_stmt, type(scope)):
            return temp_stmt
        scope = cast("VariableScope", temp_stmt)
        scope = scope.replace(markers=self.visit_markers(scope.markers, p))
        scope = scope.padding.replace(names=
            list_map(
                lambda n: self.visit_right_padded(n, None, p),
                scope.padding.names
            )
        )
        return scope

    def visit_yield_from(self, yield_from: YieldFrom, p: P) -> J:
        """Visit a yield from expression."""
        yield_from = yield_from.replace(
            prefix=self.visit_space(yield_from.prefix, PySpace.Location.YIELD_FROM_PREFIX, p)
        )
        temp_expr = cast(Expression, self.visit_expression(yield_from, p))
        if not isinstance(temp_expr, type(yield_from)):
            return temp_expr
        yield_from = cast("YieldFrom", temp_expr)
        yield_from = yield_from.replace(markers=self.visit_markers(yield_from.markers, p))
        yield_from = yield_from.replace(
            expression=self.visit_left_padded(yield_from.expression, None, p)
        )
        return yield_from

    # -------------------------------------------------------------------------
    # Helper methods for visiting padded elements
    # -------------------------------------------------------------------------

    def visit_left_padded(
        self,
        left: Optional[JLeftPadded[T]],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JLeftPadded[T]]:
        """Visit a left-padded element."""
        if left is None:
            return None
        before = self.visit_space(left.before, loc, p)
        # Visit element if it's a Tree
        element = left.element
        if isinstance(element, J):
            element = self.visit(element, p)
        if before is left.before and element is left.element:
            return left
        return left.replace(before=before, element=element)

    def visit_right_padded(
        self,
        right: Optional[JRightPadded[T]],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JRightPadded[T]]:
        """Visit a right-padded element."""
        if right is None:
            return None
        # Visit element if it's a Tree
        element = right.element
        if isinstance(element, J):
            element = self.visit(element, p)
        after = self.visit_space(right.after, loc, p)
        if element is right.element and after is right.after:
            return right
        return right.replace(element=element, after=after)

    def visit_container(
        self,
        container: Optional[JContainer[T]],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JContainer[T]]:
        """Visit a container of elements."""
        if container is None:
            return None
        before = self.visit_space(container.before, loc, p)
        elements = list_map(
            lambda e: self.visit_right_padded(e, loc, p),
            container.elements
        )
        if before is container.before and elements is container.elements:
            return container
        return container.replace(before=before).padding.replace(elements=JRightPadded.merge_elements(container.padding.elements, elements))

    # -------------------------------------------------------------------------
    # Placeholder methods for inherited JavaVisitor functionality
    # -------------------------------------------------------------------------

    def visit_statement(self, stmt: Statement, p: P) -> Statement:
        """Visit a statement. Override to intercept all statements."""
        return stmt

    def visit_expression(self, expr: Expression, p: P) -> Expression:
        """Visit an expression. Override to intercept all expressions."""
        return expr

    def visit_and_cast(self, tree: Optional[Any], type_: type, p: P) -> Optional[Any]:
        """Visit a tree and cast to the expected type."""
        if tree is None:
            return None
        result = self.visit(tree, p)
        return result

    def visit(self, tree: Any, p: P, parent: Optional['Cursor'] = None) -> Any:
        """Visit a tree node."""
        if parent is not None:
            self._cursor = parent
        if tree is None:
            return None
        if isinstance(tree, Py):
            return tree.accept_python(self, p)
        return super().visit(tree, p, parent)
