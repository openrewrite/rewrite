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

"""
Python RPC Receiver that mirrors Java's PythonReceiver structure.

This uses the visitor pattern with pre_visit handling common fields (id, prefix, markers)
and type-specific visit methods handling only additional fields.
"""
from dataclasses import replace
from pathlib import Path
from typing import Any, Optional, TypeVar, List
from uuid import UUID

from rewrite import Markers
from rewrite.java import Space, JRightPadded, JLeftPadded, JContainer, J
from rewrite.python import CompilationUnit
from rewrite.python.support_types import Py
from rewrite.python.tree import (
    Async, Await, Binary, ChainedAssignment, ExceptionType,
    LiteralType, TypeHint, ExpressionStatement, ExpressionTypeTree,
    StatementExpression, MultiImport, KeyValue, DictLiteral, CollectionLiteral,
    FormattedString, Pass, TrailingElseWrapper, ComprehensionExpression,
    TypeAlias, YieldFrom, UnionType, VariableScope, Del, SpecialParameter,
    Star, NamedArgument, TypeHintedExpression, ErrorFrom, MatchCase, Slice
)
from rewrite.rpc.receive_queue import RpcReceiveQueue

T = TypeVar('T')


def _update_if_changed(obj: T, **kwargs) -> T:
    """Update object only if any field changed."""
    changed = False
    for key, value in kwargs.items():
        # Handle Python keyword avoidance (e.g., from_ -> _from)
        field_name = f'_{key.rstrip("_")}'
        if getattr(obj, field_name, getattr(obj, key, None)) is not value:
            changed = True
            break
    if not changed:
        return obj
    return replace(obj, **{f'_{k.rstrip("_")}': v for k, v in kwargs.items()})


class PythonRpcReceiver:
    """Receiver that mirrors Java's PythonReceiver for RPC deserialization."""

    def receive(self, before: Any, q: RpcReceiveQueue) -> Any:
        """Entry point for receiving an object."""
        return q.receive(before, lambda b: self._visit(b, q))

    def _visit(self, tree: Any, q: RpcReceiveQueue) -> Any:
        """Visit a tree node, dispatching to appropriate visitor method."""
        if tree is None:
            return None

        # First handle common J fields via pre_visit
        # ExpressionStatement and StatementExpression delegate prefix/markers to their child
        if isinstance(tree, J):
            if isinstance(tree, ExpressionStatement):
                # Only receive id; prefix/markers are part of expression
                new_id = q.receive(tree.id)
                tree = tree.replace(id=new_id) if new_id is not tree.id else tree
            elif isinstance(tree, StatementExpression):
                # Only receive id; prefix/markers are part of statement
                new_id = q.receive(tree.id)
                tree = tree.replace(id=new_id) if new_id is not tree.id else tree
            else:
                tree = self._pre_visit(tree, q)

        # Then dispatch to type-specific visitor
        if isinstance(tree, CompilationUnit):
            return self._visit_compilation_unit(tree, q)
        elif isinstance(tree, Async):
            return self._visit_async(tree, q)
        elif isinstance(tree, Await):
            return self._visit_await(tree, q)
        elif isinstance(tree, Binary):
            return self._visit_binary(tree, q)
        elif isinstance(tree, ChainedAssignment):
            return self._visit_chained_assignment(tree, q)
        elif isinstance(tree, ExceptionType):
            return self._visit_exception_type(tree, q)
        elif isinstance(tree, LiteralType):
            return self._visit_literal_type(tree, q)
        elif isinstance(tree, TypeHint):
            return self._visit_type_hint(tree, q)
        elif isinstance(tree, ExpressionStatement):
            return self._visit_expression_statement(tree, q)
        elif isinstance(tree, ExpressionTypeTree):
            return self._visit_expression_type_tree(tree, q)
        elif isinstance(tree, StatementExpression):
            return self._visit_statement_expression(tree, q)
        elif isinstance(tree, MultiImport):
            return self._visit_multi_import(tree, q)
        elif isinstance(tree, KeyValue):
            return self._visit_key_value(tree, q)
        elif isinstance(tree, DictLiteral):
            return self._visit_dict_literal(tree, q)
        elif isinstance(tree, CollectionLiteral):
            return self._visit_collection_literal(tree, q)
        elif isinstance(tree, FormattedString.Value):
            return self._visit_formatted_string_value(tree, q)
        elif isinstance(tree, FormattedString):
            return self._visit_formatted_string(tree, q)
        elif isinstance(tree, Pass):
            return self._visit_pass(tree, q)
        elif isinstance(tree, TrailingElseWrapper):
            return self._visit_trailing_else_wrapper(tree, q)
        elif isinstance(tree, ComprehensionExpression.Condition):
            return self._visit_comprehension_condition(tree, q)
        elif isinstance(tree, ComprehensionExpression.Clause):
            return self._visit_comprehension_clause(tree, q)
        elif isinstance(tree, ComprehensionExpression):
            return self._visit_comprehension_expression(tree, q)
        elif isinstance(tree, TypeAlias):
            return self._visit_type_alias(tree, q)
        elif isinstance(tree, YieldFrom):
            return self._visit_yield_from(tree, q)
        elif isinstance(tree, UnionType):
            return self._visit_union_type(tree, q)
        elif isinstance(tree, VariableScope):
            return self._visit_variable_scope(tree, q)
        elif isinstance(tree, Del):
            return self._visit_del(tree, q)
        elif isinstance(tree, SpecialParameter):
            return self._visit_special_parameter(tree, q)
        elif isinstance(tree, Star):
            return self._visit_star(tree, q)
        elif isinstance(tree, NamedArgument):
            return self._visit_named_argument(tree, q)
        elif isinstance(tree, TypeHintedExpression):
            return self._visit_type_hinted_expression(tree, q)
        elif isinstance(tree, ErrorFrom):
            return self._visit_error_from(tree, q)
        elif isinstance(tree, MatchCase.Pattern):
            return self._visit_match_case_pattern(tree, q)
        elif isinstance(tree, MatchCase):
            return self._visit_match_case(tree, q)
        elif isinstance(tree, Slice):
            return self._visit_slice(tree, q)
        elif isinstance(tree, J):
            # Delegate to Java receiver for Java types
            return self._visit_java(tree, q)

        return tree

    def _pre_visit(self, j: J, q: RpcReceiveQueue) -> J:
        """Handle common J fields: id, prefix, markers."""
        new_id = q.receive(j.id)
        new_prefix = q.receive(j.prefix, lambda space: self._receive_space(space, q))
        new_markers = q.receive_markers(j.markers)

        if new_id is j.id and new_prefix is j.prefix and new_markers is j.markers:
            return j

        result = j
        if new_id is not j.id:
            result = result.replace(id=new_id)
        if new_prefix is not j.prefix:
            result = result.replace(prefix=new_prefix)
        if new_markers is not j.markers:
            result = result.replace(markers=new_markers)
        return result

    def _visit_compilation_unit(self, cu: CompilationUnit, q: RpcReceiveQueue) -> CompilationUnit:
        """Visit CompilationUnit - only non-common fields."""
        source_path = q.receive(str(cu.source_path))
        charset_name = q.receive(cu.charset_name)
        charset_bom_marked = q.receive(cu.charset_bom_marked)
        checksum = q.receive(cu.checksum)
        file_attributes = q.receive(cu.file_attributes)
        imports = q.receive_list(
            cu.padding.imports,
            lambda rp: self._receive_right_padded(rp, q)
        )
        statements = q.receive_list(
            cu.padding.statements,
            lambda rp: self._receive_right_padded(rp, q)
        )
        eof = q.receive(cu.eof, lambda space: self._receive_space(space, q))

        return _update_if_changed(
            cu,
            source_path=Path(source_path) if source_path else cu.source_path,
            charset_name=charset_name,
            charset_bom_marked=charset_bom_marked,
            checksum=checksum,
            file_attributes=file_attributes,
            imports=imports,
            statements=statements,
            eof=eof
        )

    def _visit_async(self, async_: Async, q: RpcReceiveQueue) -> Async:
        statement = q.receive(async_.statement, lambda el: self._visit(el, q))
        return _update_if_changed(async_, statement=statement)

    def _visit_await(self, await_: Await, q: RpcReceiveQueue) -> Await:
        expression = q.receive(await_.expression, lambda el: self._visit(el, q))
        type_ = q.receive(await_.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(await_, expression=expression, type=type_)

    def _visit_binary(self, binary: Binary, q: RpcReceiveQueue) -> Binary:
        left = q.receive(binary.left, lambda el: self._visit(el, q))
        operator = q.receive(binary.padding.operator, lambda lp: self._receive_left_padded(lp, q))
        negation = q.receive(binary.negation, lambda space: self._receive_space(space, q))
        right = q.receive(binary.right, lambda el: self._visit(el, q))
        type_ = q.receive(binary.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(binary, left=left, operator=operator, negation=negation, right=right, type=type_)

    def _visit_chained_assignment(self, ca: ChainedAssignment, q: RpcReceiveQueue) -> ChainedAssignment:
        variables = q.receive_list(
            ca.padding.variables,
            lambda rp: self._receive_right_padded(rp, q)
        )
        assignment = q.receive(ca.assignment, lambda el: self._visit(el, q))
        type_ = q.receive(ca.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ca, variables=variables, assignment=assignment, type=type_)

    def _visit_exception_type(self, et: ExceptionType, q: RpcReceiveQueue) -> ExceptionType:
        type_ = q.receive(et.type, lambda t: self._receive_type(t, q))
        exception_group = q.receive(et.exception_group)
        expression = q.receive(et.expression, lambda el: self._visit(el, q))
        return _update_if_changed(et, type=type_, exception_group=exception_group, expression=expression)

    def _visit_literal_type(self, lt: LiteralType, q: RpcReceiveQueue) -> LiteralType:
        literal = q.receive(lt.literal, lambda el: self._visit(el, q))
        type_ = q.receive(lt.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(lt, literal=literal, type=type_)

    def _visit_type_hint(self, th: TypeHint, q: RpcReceiveQueue) -> TypeHint:
        type_tree = q.receive(th.type_tree, lambda el: self._visit(el, q))
        type_ = q.receive(th.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(th, type_tree=type_tree, type=type_)

    def _visit_expression_statement(self, es: ExpressionStatement, q: RpcReceiveQueue) -> ExpressionStatement:
        expression = q.receive(es.expression, lambda el: self._visit(el, q))
        return _update_if_changed(es, expression=expression)

    def _visit_expression_type_tree(self, ett: ExpressionTypeTree, q: RpcReceiveQueue) -> ExpressionTypeTree:
        reference = q.receive(ett.reference, lambda el: self._visit(el, q))
        return _update_if_changed(ett, reference=reference)

    def _visit_statement_expression(self, se: StatementExpression, q: RpcReceiveQueue) -> StatementExpression:
        statement = q.receive(se.statement, lambda el: self._visit(el, q))
        return _update_if_changed(se, statement=statement)

    def _visit_multi_import(self, mi: MultiImport, q: RpcReceiveQueue) -> MultiImport:
        from_ = q.receive(mi.padding.from_, lambda rp: self._receive_right_padded(rp, q))
        parenthesized = q.receive(mi.parenthesized)
        names = q.receive(mi.padding.names, lambda c: self._receive_container(c, q))
        return _update_if_changed(mi, from_=from_, parenthesized=parenthesized, names=names)

    def _visit_key_value(self, kv: KeyValue, q: RpcReceiveQueue) -> KeyValue:
        key = q.receive(kv.padding.key, lambda rp: self._receive_right_padded(rp, q))
        value = q.receive(kv.value, lambda el: self._visit(el, q))
        type_ = q.receive(kv.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(kv, key=key, value=value, type=type_)

    def _visit_dict_literal(self, dl: DictLiteral, q: RpcReceiveQueue) -> DictLiteral:
        elements = q.receive(dl.padding.elements, lambda c: self._receive_container(c, q))
        type_ = q.receive(dl.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(dl, elements=elements, type=type_)

    def _visit_collection_literal(self, cl: CollectionLiteral, q: RpcReceiveQueue) -> CollectionLiteral:
        kind = q.receive(cl.kind)
        elements = q.receive(cl.padding.elements, lambda c: self._receive_container(c, q))
        type_ = q.receive(cl.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(cl, kind=kind, elements=elements, type=type_)

    def _visit_formatted_string(self, fs: FormattedString, q: RpcReceiveQueue) -> FormattedString:
        delimiter = q.receive(fs.delimiter)
        parts = q.receive_list(fs.parts, lambda el: self._visit(el, q))
        type_ = q.receive(fs.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(fs, delimiter=delimiter, parts=parts, type=type_)

    def _visit_formatted_string_value(self, v: FormattedString.Value, q: RpcReceiveQueue) -> FormattedString.Value:
        expression = q.receive(v.padding.expression, lambda rp: self._receive_right_padded(rp, q))
        debug = q.receive(v.padding.debug, lambda rp: self._receive_right_padded(rp, q))
        conversion = q.receive(v.conversion)
        format_ = q.receive(v.format, lambda el: self._visit(el, q))
        return _update_if_changed(v, expression=expression, debug=debug, conversion=conversion, format=format_)

    def _visit_pass(self, pass_: Pass, q: RpcReceiveQueue) -> Pass:
        # No additional fields beyond id/prefix/markers
        return pass_

    def _visit_trailing_else_wrapper(self, tew: TrailingElseWrapper, q: RpcReceiveQueue) -> TrailingElseWrapper:
        statement = q.receive(tew.statement, lambda el: self._visit(el, q))
        else_block = q.receive(tew.padding.else_block, lambda lp: self._receive_left_padded(lp, q))
        return _update_if_changed(tew, statement=statement, else_block=else_block)

    def _visit_comprehension_expression(self, ce: ComprehensionExpression, q: RpcReceiveQueue) -> ComprehensionExpression:
        kind = q.receive(ce.kind)
        result = q.receive(ce.result, lambda el: self._visit(el, q))
        clauses = q.receive_list(ce.clauses, lambda el: self._visit(el, q))
        suffix = q.receive(ce.suffix, lambda space: self._receive_space(space, q))
        type_ = q.receive(ce.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ce, kind=kind, result=result, clauses=clauses, suffix=suffix, type=type_)

    def _visit_comprehension_condition(self, cc: ComprehensionExpression.Condition, q: RpcReceiveQueue) -> ComprehensionExpression.Condition:
        expression = q.receive(cc.expression, lambda el: self._visit(el, q))
        return _update_if_changed(cc, expression=expression)

    def _visit_comprehension_clause(self, cc: ComprehensionExpression.Clause, q: RpcReceiveQueue) -> ComprehensionExpression.Clause:
        async_ = q.receive(cc.padding.async_, lambda rp: self._receive_right_padded(rp, q))
        iterator_variable = q.receive(cc.iterator_variable, lambda el: self._visit(el, q))
        iterated_list = q.receive(cc.padding.iterated_list, lambda lp: self._receive_left_padded(lp, q))
        conditions = q.receive_list(cc.conditions, lambda el: self._visit(el, q))
        return _update_if_changed(cc, async_=async_, iterator_variable=iterator_variable, iterated_list=iterated_list, conditions=conditions)

    def _visit_type_alias(self, ta: TypeAlias, q: RpcReceiveQueue) -> TypeAlias:
        name = q.receive(ta.name, lambda el: self._visit(el, q))
        value = q.receive(ta.padding.value, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(ta.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ta, name=name, value=value, type=type_)

    def _visit_yield_from(self, yf: YieldFrom, q: RpcReceiveQueue) -> YieldFrom:
        expression = q.receive(yf.expression, lambda el: self._visit(el, q))
        type_ = q.receive(yf.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(yf, expression=expression, type=type_)

    def _visit_union_type(self, ut: UnionType, q: RpcReceiveQueue) -> UnionType:
        types = q.receive_list(
            ut.padding.types,
            lambda rp: self._receive_right_padded(rp, q)
        )
        type_ = q.receive(ut.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ut, types=types, type=type_)

    def _visit_variable_scope(self, vs: VariableScope, q: RpcReceiveQueue) -> VariableScope:
        kind = q.receive(vs.kind)
        names = q.receive_list(
            vs.padding.names,
            lambda rp: self._receive_right_padded(rp, q)
        )
        return _update_if_changed(vs, kind=kind, names=names)

    def _visit_del(self, del_: Del, q: RpcReceiveQueue) -> Del:
        targets = q.receive_list(
            del_.padding.targets,
            lambda rp: self._receive_right_padded(rp, q)
        )
        return _update_if_changed(del_, targets=targets)

    def _visit_special_parameter(self, sp: SpecialParameter, q: RpcReceiveQueue) -> SpecialParameter:
        kind = q.receive(sp.kind)
        type_hint = q.receive(sp.type_hint, lambda el: self._visit(el, q))
        type_ = q.receive(sp.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(sp, kind=kind, type_hint=type_hint, type=type_)

    def _visit_star(self, star: Star, q: RpcReceiveQueue) -> Star:
        kind = q.receive(star.kind)
        expression = q.receive(star.expression, lambda el: self._visit(el, q))
        type_ = q.receive(star.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(star, kind=kind, expression=expression, type=type_)

    def _visit_named_argument(self, na: NamedArgument, q: RpcReceiveQueue) -> NamedArgument:
        name = q.receive(na.name, lambda el: self._visit(el, q))
        value = q.receive(na.padding.value, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(na.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(na, name=name, value=value, type=type_)

    def _visit_type_hinted_expression(self, the: TypeHintedExpression, q: RpcReceiveQueue) -> TypeHintedExpression:
        expression = q.receive(the.expression, lambda el: self._visit(el, q))
        type_hint = q.receive(the.type_hint, lambda el: self._visit(el, q))
        type_ = q.receive(the.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(the, expression=expression, type_hint=type_hint, type=type_)

    def _visit_error_from(self, ef: ErrorFrom, q: RpcReceiveQueue) -> ErrorFrom:
        error = q.receive(ef.error, lambda el: self._visit(el, q))
        from_ = q.receive(ef.padding.from_, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(ef.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ef, error=error, from_=from_, type=type_)

    def _visit_match_case(self, mc: MatchCase, q: RpcReceiveQueue) -> MatchCase:
        pattern = q.receive(mc.pattern, lambda el: self._visit(el, q))
        guard = q.receive(mc.padding.guard, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(mc.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(mc, pattern=pattern, guard=guard, type=type_)

    def _visit_match_case_pattern(self, p: MatchCase.Pattern, q: RpcReceiveQueue) -> MatchCase.Pattern:
        kind = q.receive(p.kind)
        children = q.receive(p.padding.children, lambda c: self._receive_container(c, q))
        type_ = q.receive(p.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(p, kind=kind, children=children, type=type_)

    def _visit_slice(self, slice_: Slice, q: RpcReceiveQueue) -> Slice:
        start = q.receive(slice_.padding.start, lambda rp: self._receive_right_padded(rp, q))
        stop = q.receive(slice_.padding.stop, lambda rp: self._receive_right_padded(rp, q))
        step = q.receive(slice_.padding.step, lambda rp: self._receive_right_padded(rp, q))
        # Python's Slice doesn't have a type field, but Java sends one (always null)
        _ = q.receive(None)  # Consume the null type field
        return _update_if_changed(slice_, start=start, stop=stop, step=step)

    def _visit_java(self, j: J, q: RpcReceiveQueue) -> J:
        """Handle Java tree types that Python extends."""
        from rewrite.java.tree import (
            Identifier, Literal, MethodInvocation, FieldAccess,
            ArrayAccess, ArrayDimension, Block, If, Try, MethodDeclaration,
            ClassDeclaration, VariableDeclarations, Return, Assignment,
            AssignmentOperation, Unary, Ternary, Lambda, Empty, Throw,
            Assert, Break, Continue, WhileLoop, ForEachLoop, Switch, Case,
            Annotation, Import, Binary as JBinary, Parentheses, ControlParentheses,
            NewArray, Modifier, Yield
        )

        if isinstance(j, Identifier):
            return self._visit_identifier(j, q)
        elif isinstance(j, Literal):
            return self._visit_literal(j, q)
        elif isinstance(j, Import):
            return self._visit_import(j, q)
        elif isinstance(j, FieldAccess):
            return self._visit_field_access(j, q)
        elif isinstance(j, Block):
            return self._visit_block(j, q)
        elif isinstance(j, MethodInvocation):
            return self._visit_method_invocation(j, q)
        elif isinstance(j, Unary):
            return self._visit_j_unary(j, q)
        elif isinstance(j, JBinary):
            return self._visit_j_binary(j, q)
        elif isinstance(j, Assignment):
            return self._visit_j_assignment(j, q)
        elif isinstance(j, AssignmentOperation):
            return self._visit_j_assignment_operation(j, q)
        elif isinstance(j, Return):
            return self._visit_j_return(j, q)
        elif isinstance(j, If):
            return self._visit_j_if(j, q)
        elif isinstance(j, If.Else):
            return self._visit_j_else(j, q)
        elif isinstance(j, WhileLoop):
            return self._visit_j_while_loop(j, q)
        elif isinstance(j, ForEachLoop.Control):
            return self._visit_j_for_each_control(j, q)
        elif isinstance(j, ForEachLoop):
            return self._visit_j_for_each_loop(j, q)
        elif isinstance(j, Try):
            return self._visit_j_try(j, q)
        elif isinstance(j, Try.Catch):
            return self._visit_j_catch(j, q)
        elif isinstance(j, Try.Resource):
            return self._visit_j_try_resource(j, q)
        elif isinstance(j, Throw):
            return self._visit_j_throw(j, q)
        elif isinstance(j, Assert):
            return self._visit_j_assert(j, q)
        elif isinstance(j, Break):
            return self._visit_j_break(j, q)
        elif isinstance(j, Continue):
            return self._visit_j_continue(j, q)
        elif isinstance(j, Empty):
            return self._visit_j_empty(j, q)
        elif isinstance(j, Ternary):
            return self._visit_j_ternary(j, q)
        elif isinstance(j, Lambda):
            return self._visit_j_lambda(j, q)
        elif isinstance(j, Lambda.Parameters):
            return self._visit_j_lambda_parameters(j, q)
        elif isinstance(j, VariableDeclarations):
            return self._visit_j_variable_declarations(j, q)
        elif isinstance(j, VariableDeclarations.NamedVariable):
            return self._visit_j_named_variable(j, q)
        elif isinstance(j, ClassDeclaration):
            return self._visit_j_class_declaration(j, q)
        elif isinstance(j, ClassDeclaration.Kind):
            return self._visit_j_class_declaration_kind(j, q)
        elif isinstance(j, MethodDeclaration):
            return self._visit_j_method_declaration(j, q)
        elif isinstance(j, Switch):
            return self._visit_j_switch(j, q)
        elif isinstance(j, Case):
            return self._visit_j_case(j, q)
        elif isinstance(j, ArrayAccess):
            return self._visit_j_array_access(j, q)
        elif isinstance(j, ArrayDimension):
            return self._visit_j_array_dimension(j, q)
        elif isinstance(j, NewArray):
            return self._visit_j_new_array(j, q)
        elif isinstance(j, Annotation):
            return self._visit_j_annotation(j, q)
        elif isinstance(j, Parentheses):
            return self._visit_j_parentheses(j, q)
        elif isinstance(j, ControlParentheses):
            return self._visit_j_control_parentheses(j, q)
        elif isinstance(j, Modifier):
            return self._visit_j_modifier(j, q)
        elif isinstance(j, Yield):
            return self._visit_j_yield(j, q)

        return j

    def _visit_identifier(self, ident, q: RpcReceiveQueue):
        annotations = q.receive_list(ident.annotations, lambda a: self._visit(a, q))
        simple_name = q.receive(ident.simple_name)
        type_ = q.receive(ident.type, lambda t: self._receive_type(t, q))
        field_type = q.receive(ident.field_type)
        return _update_if_changed(ident, annotations=annotations, simple_name=simple_name, type=type_, field_type=field_type)

    def _visit_literal(self, lit, q: RpcReceiveQueue):
        value = q.receive(lit.value)
        value_source = q.receive(lit.value_source)
        unicode_escapes = q.receive(lit.unicode_escapes)
        type_ = q.receive(lit.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(lit, value=value, value_source=value_source, unicode_escapes=unicode_escapes, type=type_)

    def _visit_import(self, imp, q: RpcReceiveQueue):
        static = q.receive(imp.padding.static, lambda lp: self._receive_left_padded(lp, q))
        qualid = q.receive(imp.qualid, lambda el: self._visit(el, q))
        alias = q.receive(imp.padding.alias, lambda lp: self._receive_left_padded(lp, q))
        return _update_if_changed(imp, static=static, qualid=qualid, alias=alias)

    def _visit_field_access(self, fa, q: RpcReceiveQueue):
        target = q.receive(fa.target, lambda el: self._visit(el, q))
        name = q.receive(fa.padding.name, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(fa.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(fa, target=target, name=name, type=type_)

    def _visit_method_invocation(self, mi, q: RpcReceiveQueue):
        select = q.receive(mi.padding.select, lambda rp: self._receive_right_padded(rp, q))
        type_parameters = q.receive(mi.padding.type_parameters, lambda c: self._receive_container(c, q))
        name = q.receive(mi.name, lambda el: self._visit(el, q))
        arguments = q.receive(mi.padding.arguments, lambda c: self._receive_container(c, q))
        method_type = q.receive(mi.method_type)
        return _update_if_changed(mi, select=select, type_parameters=type_parameters, name=name, arguments=arguments, method_type=method_type)

    def _visit_block(self, block, q: RpcReceiveQueue):
        static = q.receive(block.padding.static, lambda rp: self._receive_right_padded(rp, q))
        statements = q.receive_list(
            block.padding.statements,
            lambda rp: self._receive_right_padded(rp, q)
        )
        end = q.receive(block.end, lambda space: self._receive_space(space, q))
        return _update_if_changed(block, static=static, statements=statements, end=end)

    def _visit_j_unary(self, unary, q: RpcReceiveQueue):
        operator = q.receive(unary.padding.operator, lambda lp: self._receive_left_padded(lp, q))
        expression = q.receive(unary.expression, lambda el: self._visit(el, q))
        type_ = q.receive(unary.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(unary, operator=operator, expression=expression, type=type_)

    def _visit_j_binary(self, binary, q: RpcReceiveQueue):
        left = q.receive(binary.left, lambda el: self._visit(el, q))
        operator = q.receive(binary.padding.operator, lambda lp: self._receive_left_padded(lp, q))
        right = q.receive(binary.right, lambda el: self._visit(el, q))
        type_ = q.receive(binary.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(binary, left=left, operator=operator, right=right, type=type_)

    def _visit_j_assignment(self, assign, q: RpcReceiveQueue):
        variable = q.receive(assign.variable, lambda el: self._visit(el, q))
        assignment = q.receive(assign.padding.assignment, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(assign.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(assign, variable=variable, assignment=assignment, type=type_)

    def _visit_j_assignment_operation(self, assign, q: RpcReceiveQueue):
        variable = q.receive(assign.variable, lambda el: self._visit(el, q))
        operator = q.receive(assign.padding.operator, lambda lp: self._receive_left_padded(lp, q))
        assignment = q.receive(assign.assignment, lambda el: self._visit(el, q))
        type_ = q.receive(assign.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(assign, variable=variable, operator=operator, assignment=assignment, type=type_)

    def _visit_j_return(self, ret, q: RpcReceiveQueue):
        expression = q.receive(ret.expression, lambda el: self._visit(el, q))
        return _update_if_changed(ret, expression=expression)

    def _visit_j_if(self, if_stmt, q: RpcReceiveQueue):
        if_condition = q.receive(if_stmt.if_condition, lambda el: self._visit(el, q))
        then_part = q.receive(if_stmt.padding.then_part, lambda rp: self._receive_right_padded(rp, q))
        else_part = q.receive(if_stmt.else_part, lambda el: self._visit(el, q))
        return _update_if_changed(if_stmt, if_condition=if_condition, then_part=then_part, else_part=else_part)

    def _visit_j_else(self, else_stmt, q: RpcReceiveQueue):
        body = q.receive(else_stmt.padding.body, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(else_stmt, body=body)

    def _visit_j_while_loop(self, while_loop, q: RpcReceiveQueue):
        condition = q.receive(while_loop.condition, lambda el: self._visit(el, q))
        body = q.receive(while_loop.padding.body, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(while_loop, condition=condition, body=body)

    def _visit_j_for_each_loop(self, for_each, q: RpcReceiveQueue):
        control = q.receive(for_each.control, lambda el: self._visit(el, q))
        body = q.receive(for_each.padding.body, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(for_each, control=control, body=body)

    def _visit_j_for_each_control(self, control, q: RpcReceiveQueue):
        variable = q.receive(control.padding.variable, lambda rp: self._receive_right_padded(rp, q))
        iterable = q.receive(control.padding.iterable, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(control, variable=variable, iterable=iterable)

    def _visit_j_try(self, try_stmt, q: RpcReceiveQueue):
        resources = q.receive(
            try_stmt.padding.resources if hasattr(try_stmt.padding, 'resources') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        body = q.receive(try_stmt.body, lambda el: self._visit(el, q))
        catches = q.receive_list(try_stmt.catches, lambda el: self._visit(el, q))
        finally_ = q.receive(
            try_stmt.padding.finally_ if hasattr(try_stmt.padding, 'finally_') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        return _update_if_changed(try_stmt, resources=resources, body=body, catches=catches, finally_=finally_)

    def _visit_j_catch(self, catch, q: RpcReceiveQueue):
        parameter = q.receive(catch.parameter, lambda el: self._visit(el, q))
        body = q.receive(catch.body, lambda el: self._visit(el, q))
        return _update_if_changed(catch, parameter=parameter, body=body)

    def _visit_j_try_resource(self, resource, q: RpcReceiveQueue):
        variable_declarations = q.receive(resource.variable_declarations, lambda el: self._visit(el, q))
        terminated_with_semicolon = q.receive(resource.terminated_with_semicolon)
        return _update_if_changed(resource, variable_declarations=variable_declarations, terminated_with_semicolon=terminated_with_semicolon)

    def _visit_j_throw(self, throw, q: RpcReceiveQueue):
        exception = q.receive(throw.exception, lambda el: self._visit(el, q))
        return _update_if_changed(throw, exception=exception)

    def _visit_j_assert(self, assert_stmt, q: RpcReceiveQueue):
        condition = q.receive(assert_stmt.condition, lambda el: self._visit(el, q))
        detail = q.receive(assert_stmt.detail, lambda lp: self._receive_left_padded(lp, q))
        return _update_if_changed(assert_stmt, condition=condition, detail=detail)

    def _visit_j_break(self, break_stmt, q: RpcReceiveQueue):
        label = q.receive(break_stmt.label, lambda el: self._visit(el, q))
        return _update_if_changed(break_stmt, label=label)

    def _visit_j_continue(self, continue_stmt, q: RpcReceiveQueue):
        label = q.receive(continue_stmt.label, lambda el: self._visit(el, q))
        return _update_if_changed(continue_stmt, label=label)

    def _visit_j_empty(self, empty, q: RpcReceiveQueue):
        # No additional fields
        return empty

    def _visit_j_ternary(self, ternary, q: RpcReceiveQueue):
        condition = q.receive(ternary.condition, lambda el: self._visit(el, q))
        true_part = q.receive(ternary.padding.true_part, lambda lp: self._receive_left_padded(lp, q))
        false_part = q.receive(ternary.padding.false_part, lambda lp: self._receive_left_padded(lp, q))
        type_ = q.receive(ternary.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(ternary, condition=condition, true_part=true_part, false_part=false_part, type=type_)

    def _visit_j_lambda(self, lam, q: RpcReceiveQueue):
        parameters = q.receive(lam.parameters, lambda el: self._visit(el, q))
        arrow = q.receive(lam.arrow, lambda space: self._receive_space(space, q))
        body = q.receive(lam.body, lambda el: self._visit(el, q))
        type_ = q.receive(lam.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(lam, parameters=parameters, arrow=arrow, body=body, type=type_)

    def _visit_j_lambda_parameters(self, params, q: RpcReceiveQueue):
        parenthesized = q.receive(params.parenthesized)
        parameters = q.receive_list(
            params.padding.parameters,
            lambda rp: self._receive_right_padded(rp, q)
        )
        return _update_if_changed(params, parenthesized=parenthesized, parameters=parameters)

    def _visit_j_variable_declarations(self, var_decl, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(var_decl.leading_annotations, lambda a: self._visit(a, q))
        modifiers = q.receive_list(var_decl.modifiers, lambda m: self._visit(m, q))
        type_expression = q.receive(var_decl.type_expression, lambda el: self._visit(el, q))
        varargs = q.receive(var_decl.varargs, lambda space: self._receive_space(space, q))
        variables = q.receive_list(
            var_decl.padding.variables,
            lambda rp: self._receive_right_padded(rp, q)
        )
        return _update_if_changed(var_decl, leading_annotations=leading_annotations, modifiers=modifiers,
                                  type_expression=type_expression, varargs=varargs, variables=variables)

    def _visit_j_named_variable(self, var, q: RpcReceiveQueue):
        name = q.receive(var.name, lambda el: self._visit(el, q))
        dimensions_after_name = q.receive_list(
            var.dimensions_after_name,
            lambda lp: self._receive_left_padded(lp, q)
        )
        initializer = q.receive(
            var.padding.initializer if hasattr(var.padding, 'initializer') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        variable_type = q.receive(var.variable_type)
        return _update_if_changed(var, name=name, dimensions_after_name=dimensions_after_name,
                                  initializer=initializer, variable_type=variable_type)

    def _visit_j_class_declaration(self, class_decl, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(class_decl.leading_annotations, lambda a: self._visit(a, q))
        modifiers = q.receive_list(class_decl.modifiers, lambda m: self._visit(m, q))
        kind = q.receive(class_decl.padding.kind, lambda el: self._visit(el, q))
        name = q.receive(class_decl.name, lambda el: self._visit(el, q))
        type_parameters = q.receive(
            class_decl.padding.type_parameters if hasattr(class_decl.padding, 'type_parameters') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        primary_constructor = q.receive(
            class_decl.primary_constructor if hasattr(class_decl, 'primary_constructor') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        extends = q.receive(
            class_decl.padding.extends if hasattr(class_decl.padding, 'extends') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        implements = q.receive(
            class_decl.padding.implements if hasattr(class_decl.padding, 'implements') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        permits = q.receive(
            class_decl.padding.permits if hasattr(class_decl.padding, 'permits') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        body = q.receive(class_decl.body, lambda el: self._visit(el, q))
        return _update_if_changed(class_decl, leading_annotations=leading_annotations, modifiers=modifiers,
                                  kind=kind, name=name, type_parameters=type_parameters,
                                  primary_constructor=primary_constructor, extends=extends, implements=implements,
                                  permits=permits, body=body)

    def _visit_j_class_declaration_kind(self, kind, q: RpcReceiveQueue):
        annotations = q.receive_list(kind.annotations, lambda a: self._visit(a, q))
        type_ = q.receive(kind.type)  # Enum type
        return _update_if_changed(kind, annotations=annotations, type=type_)

    def _visit_j_method_declaration(self, method, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(method.leading_annotations, lambda a: self._visit(a, q))
        modifiers = q.receive_list(method.modifiers, lambda m: self._visit(m, q))
        type_parameters = q.receive(
            method.padding.type_parameters if hasattr(method.padding, 'type_parameters') else None,
            lambda el: self._visit(el, q) if el else None
        )
        return_type_expression = q.receive(method.return_type_expression, lambda el: self._visit(el, q))
        name_annotations = q.receive_list(
            method.annotations.name.annotations if hasattr(method.annotations.name, 'annotations') else [],
            lambda a: self._visit(a, q)
        )
        name = q.receive(method.annotations.name.identifier, lambda el: self._visit(el, q))
        parameters = q.receive(method.padding.parameters, lambda c: self._receive_container(c, q) if c else None)
        throws = q.receive(
            method.padding.throws if hasattr(method.padding, 'throws') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        body = q.receive(method.body, lambda el: self._visit(el, q))
        default_value = q.receive(
            method.padding.default_value if hasattr(method.padding, 'default_value') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        method_type = q.receive(method.method_type)
        return _update_if_changed(method, leading_annotations=leading_annotations, modifiers=modifiers,
                                  type_parameters=type_parameters, return_type_expression=return_type_expression,
                                  parameters=parameters, throws=throws, body=body, default_value=default_value,
                                  method_type=method_type)

    def _visit_j_switch(self, switch, q: RpcReceiveQueue):
        selector = q.receive(switch.selector, lambda el: self._visit(el, q))
        cases = q.receive(switch.cases, lambda el: self._visit(el, q))
        return _update_if_changed(switch, selector=selector, cases=cases)

    def _visit_j_case(self, case, q: RpcReceiveQueue):
        type_ = q.receive(case.type)  # Enum type
        case_labels = q.receive(case.padding.case_labels, lambda c: self._receive_container(c, q))
        statements = q.receive(case.padding.statements, lambda c: self._receive_container(c, q))
        body = q.receive(case.padding.body, lambda rp: self._receive_right_padded(rp, q) if rp else None)
        guard = q.receive(case.guard, lambda el: self._visit(el, q) if el else None)
        return _update_if_changed(case, type=type_, case_labels=case_labels, statements=statements, body=body, guard=guard)

    def _visit_j_array_access(self, arr, q: RpcReceiveQueue):
        indexed = q.receive(arr.indexed, lambda el: self._visit(el, q))
        dimension = q.receive(arr.dimension, lambda el: self._visit(el, q))
        return _update_if_changed(arr, indexed=indexed, dimension=dimension)

    def _visit_j_array_dimension(self, dim, q: RpcReceiveQueue):
        index = q.receive(dim.padding.index, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(dim, index=index)

    def _visit_j_new_array(self, new_arr, q: RpcReceiveQueue):
        type_expression = q.receive(new_arr.type_expression, lambda el: self._visit(el, q))
        dimensions = q.receive_list(new_arr.dimensions, lambda d: self._visit(d, q))
        initializer = q.receive(
            new_arr.padding.initializer if hasattr(new_arr.padding, 'initializer') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        type_ = q.receive(new_arr.type, lambda t: self._receive_type(t, q))
        return _update_if_changed(new_arr, type_expression=type_expression, dimensions=dimensions,
                                  initializer=initializer, type=type_)

    def _visit_j_annotation(self, annot, q: RpcReceiveQueue):
        annotation_type = q.receive(annot.annotation_type, lambda el: self._visit(el, q))
        arguments = q.receive(
            annot.padding.arguments if hasattr(annot.padding, 'arguments') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        return _update_if_changed(annot, annotation_type=annotation_type, arguments=arguments)

    def _visit_j_parentheses(self, parens, q: RpcReceiveQueue):
        tree = q.receive(parens.padding.tree, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(parens, tree=tree)

    def _visit_j_control_parentheses(self, parens, q: RpcReceiveQueue):
        tree = q.receive(parens.padding.tree, lambda rp: self._receive_right_padded(rp, q))
        return _update_if_changed(parens, tree=tree)

    def _visit_j_modifier(self, mod, q: RpcReceiveQueue):
        keyword = q.receive(mod.keyword)
        type_ = q.receive(mod.type)  # Enum type
        annotations = q.receive_list(mod.annotations, lambda a: self._visit(a, q))
        return _update_if_changed(mod, keyword=keyword, type=type_, annotations=annotations)

    def _visit_j_yield(self, yield_stmt, q: RpcReceiveQueue):
        implicit = q.receive(yield_stmt.implicit)
        value = q.receive(yield_stmt.value, lambda el: self._visit(el, q))
        return _update_if_changed(yield_stmt, implicit=implicit, value=value)

    # Helper methods for Space, JRightPadded, JLeftPadded, JContainer

    def _receive_space(self, space: Space, q: RpcReceiveQueue) -> Space:
        """Receive a Space object."""
        if space is None:
            return Space.EMPTY

        comments = q.receive_list(space.comments, lambda c: self._receive_comment(c, q))
        whitespace = q.receive(space.whitespace)

        if comments is space.comments and whitespace is space.whitespace:
            return space

        return space.replace(comments=comments).replace(whitespace=whitespace)

    def _receive_comment(self, comment, q: RpcReceiveQueue):
        """Receive a Comment object."""
        from rewrite.python.support_types import PyComment
        from rewrite.java.support_types import TextComment
        from rewrite import Markers

        # Handle new comments (comment is None or a dict from _new_obj)
        if comment is None or isinstance(comment, dict):
            # For new comments, read all fields directly
            multiline = q.receive(None)
            text = q.receive(None)
            suffix = q.receive(None)
            markers = q.receive_markers(None)
            return TextComment(multiline, text, suffix, markers)

        multiline = q.receive(comment.multiline)
        text = q.receive(comment.text)
        suffix = q.receive(comment.suffix)
        markers = q.receive_markers(comment.markers)

        # PyComment has an additional field
        if isinstance(comment, PyComment):
            aligned_to_indent = q.receive(comment.aligned_to_indent)
            return PyComment(multiline, text, suffix, markers, aligned_to_indent)
        else:
            return TextComment(multiline, text, suffix, markers)

    def _receive_right_padded(self, rp: JRightPadded, q: RpcReceiveQueue) -> JRightPadded:
        """Receive a JRightPadded wrapper."""
        if rp is None:
            return None

        el = rp.element
        if isinstance(el, J):
            element = q.receive(el, lambda e: self._visit(e, q))
        elif isinstance(el, Space):
            element = q.receive(el, lambda space: self._receive_space(space, q))
        else:
            # Primitives
            element = q.receive(el)

        after = q.receive(rp.after, lambda space: self._receive_space(space, q))
        markers = q.receive(rp.markers)

        if element is rp.element and after is rp.after and markers is rp.markers:
            return rp

        return JRightPadded(element, after, markers)

    def _receive_left_padded(self, lp: JLeftPadded, q: RpcReceiveQueue) -> JLeftPadded:
        """Receive a JLeftPadded wrapper."""
        if lp is None:
            return None

        before = q.receive(lp.before, lambda space: self._receive_space(space, q))

        el = lp.element
        if isinstance(el, J):
            element = q.receive(el, lambda e: self._visit(e, q))
        elif isinstance(el, Space):
            element = q.receive(el, lambda space: self._receive_space(space, q))
        else:
            # Primitives (enums, etc.)
            element = q.receive(el)

        markers = q.receive(lp.markers)

        if before is lp.before and element is lp.element and markers is lp.markers:
            return lp

        return JLeftPadded(before, element, markers)

    def _receive_container(self, container: JContainer, q: RpcReceiveQueue) -> JContainer:
        """Receive a JContainer wrapper."""
        if container is None:
            return None

        before = q.receive(container.before, lambda space: self._receive_space(space, q))
        elements = q.receive_list(
            container.padding.elements,
            lambda rp: self._receive_right_padded(rp, q)
        )
        markers = q.receive(container.markers)

        if before is container.before and elements is container.padding.elements and markers is container.markers:
            return container

        return JContainer(before, elements, markers)

    def _receive_type(self, java_type, q: RpcReceiveQueue):
        """Receive a JavaType object."""
        # For now, just receive without transformation
        # Full type handling would need more work
        return java_type


# Register marker codecs
def _register_marker_codecs():
    """Register receive codecs for Java marker types."""
    from rewrite.java.markers import Semicolon, TrailingComma, OmitParentheses
    from rewrite.java.support_types import Space
    from rewrite.rpc.receive_queue import register_receive_codec

    def _receive_semicolon(semicolon: Semicolon, q: RpcReceiveQueue) -> Semicolon:
        new_id = q.receive(semicolon.id)
        if new_id is semicolon.id:
            return semicolon
        return semicolon.replace(id=new_id)

    def _receive_trailing_comma(trailing_comma: TrailingComma, q: RpcReceiveQueue) -> TrailingComma:
        new_id = q.receive(trailing_comma.id)
        new_suffix = q.receive(trailing_comma.suffix)
        if new_id is trailing_comma.id and new_suffix is trailing_comma.suffix:
            return trailing_comma
        result = trailing_comma
        if new_id is not trailing_comma.id:
            result = result.replace(id=new_id)
        if new_suffix is not trailing_comma.suffix:
            result = result.replace(suffix=new_suffix)
        return result

    def _receive_omit_parentheses(omit_paren: OmitParentheses, q: RpcReceiveQueue) -> OmitParentheses:
        new_id = q.receive(omit_paren.id)
        if new_id is omit_paren.id:
            return omit_paren
        return omit_paren.replace(id=new_id)

    from uuid import uuid4

    register_receive_codec(
        'org.openrewrite.java.marker.Semicolon',
        _receive_semicolon,
        lambda: Semicolon(uuid4())
    )
    register_receive_codec(
        'org.openrewrite.java.marker.TrailingComma',
        _receive_trailing_comma,
        lambda: TrailingComma(uuid4(), Space.EMPTY)
    )
    register_receive_codec(
        'org.openrewrite.java.marker.OmitParentheses',
        _receive_omit_parentheses,
        lambda: OmitParentheses(uuid4())
    )


# Register codecs on module import
_register_marker_codecs()
