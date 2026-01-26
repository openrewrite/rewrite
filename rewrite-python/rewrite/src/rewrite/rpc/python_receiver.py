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
from pathlib import Path
from typing import Any, Optional, TypeVar, List
from uuid import UUID

from rewrite import Markers
from rewrite.utils import replace_if_changed
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


class PythonRpcReceiver:
    """Receiver that mirrors Java's PythonReceiver for RPC deserialization."""

    def receive(self, before: Any, q: RpcReceiveQueue) -> Any:
        """Entry point for receiving an object."""
        # Codec registry handles type dispatch automatically
        return q.receive(before)

    def _visit(self, tree: Any, q: RpcReceiveQueue) -> Any:
        """Visit a tree node, dispatching to appropriate visitor method."""
        if tree is None:
            return None

        # First handle common J fields via pre_visit
        # Java's preVisit always sends id, prefix, markers for all J elements.
        # ExpressionStatement and StatementExpression delegate prefix/markers to their child,
        # but we still need to receive them to stay in sync with the queue.
        if isinstance(tree, J):
            if isinstance(tree, ExpressionStatement):
                # Java sends id, prefix, markers even though prefix/markers delegate to expression
                # We must receive them to stay in sync, but only use the id
                # Note: tree.prefix/markers would fail on fresh instances since _expression is None,
                # so we pass None as the before value - the RPC data contains the actual values anyway.
                new_id = q.receive(tree.id)
                q.receive(None)    # Receive but discard prefix - delegated to expression
                q.receive(None)    # Receive but discard markers - delegated to expression
                tree = tree.replace(id=new_id) if new_id is not tree.id else tree
            elif isinstance(tree, StatementExpression):
                # Java sends id, prefix, markers even though prefix/markers delegate to statement
                # We must receive them to stay in sync, but only use the id
                # Note: tree.prefix/markers would fail on fresh instances since _statement is None
                new_id = q.receive(tree.id)
                q.receive(None)    # Receive but discard prefix - delegated to statement
                q.receive(None)    # Receive but discard markers - delegated to statement
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
        new_prefix = q.receive(j.prefix)
        new_markers = q.receive_markers(j.markers)

        changes = {}
        if new_id is not j.id:
            changes['_id'] = new_id
        if new_prefix is not j.prefix:
            changes['_prefix'] = new_prefix
        if new_markers is not j.markers:
            changes['_markers'] = new_markers
        return replace_if_changed(j, **changes) if changes else j

    def _visit_compilation_unit(self, cu: CompilationUnit, q: RpcReceiveQueue) -> CompilationUnit:
        """Visit CompilationUnit - only non-common fields."""
        source_path = q.receive(str(cu.source_path))
        charset_name = q.receive(cu.charset_name)
        charset_bom_marked = q.receive(cu.charset_bom_marked)
        checksum = q.receive(cu.checksum)
        file_attributes = q.receive(cu.file_attributes)
        imports = q.receive_list(cu.padding.imports)
        statements = q.receive_list(cu.padding.statements)
        eof = q.receive(cu.eof)

        return replace_if_changed(
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
        statement = q.receive(async_.statement)
        return replace_if_changed(async_, statement=statement)

    def _visit_await(self, await_: Await, q: RpcReceiveQueue) -> Await:
        expression = q.receive(await_.expression)
        type_ = q.receive(await_.type)
        return replace_if_changed(await_, expression=expression, type=type_)

    def _visit_binary(self, binary: Binary, q: RpcReceiveQueue) -> Binary:
        left = q.receive(binary.left)
        operator = q.receive(binary.padding.operator)
        negation = q.receive(binary.negation)
        right = q.receive(binary.right)
        type_ = q.receive(binary.type)
        return replace_if_changed(binary, left=left, operator=operator, negation=negation, right=right, type=type_)

    def _visit_chained_assignment(self, ca: ChainedAssignment, q: RpcReceiveQueue) -> ChainedAssignment:
        variables = q.receive_list(ca.padding.variables)
        assignment = q.receive(ca.assignment)
        type_ = q.receive(ca.type)
        return replace_if_changed(ca, variables=variables, assignment=assignment, type=type_)

    def _visit_exception_type(self, et: ExceptionType, q: RpcReceiveQueue) -> ExceptionType:
        type_ = q.receive(et.type)
        exception_group = q.receive(et.exception_group)
        expression = q.receive(et.expression)
        return replace_if_changed(et, type=type_, exception_group=exception_group, expression=expression)

    def _visit_literal_type(self, lt: LiteralType, q: RpcReceiveQueue) -> LiteralType:
        literal = q.receive(lt.literal)
        type_ = q.receive(lt.type)
        return replace_if_changed(lt, literal=literal, type=type_)

    def _visit_type_hint(self, th: TypeHint, q: RpcReceiveQueue) -> TypeHint:
        type_tree = q.receive(th.type_tree)
        type_ = q.receive(th.type)
        return replace_if_changed(th, type_tree=type_tree, type=type_)

    def _visit_expression_statement(self, es: ExpressionStatement, q: RpcReceiveQueue) -> ExpressionStatement:
        expression = q.receive(es.expression)
        return replace_if_changed(es, expression=expression)

    def _visit_expression_type_tree(self, ett: ExpressionTypeTree, q: RpcReceiveQueue) -> ExpressionTypeTree:
        reference = q.receive(ett.reference)
        return replace_if_changed(ett, reference=reference)

    def _visit_statement_expression(self, se: StatementExpression, q: RpcReceiveQueue) -> StatementExpression:
        statement = q.receive(se.statement)
        return replace_if_changed(se, statement=statement)

    def _visit_multi_import(self, mi: MultiImport, q: RpcReceiveQueue) -> MultiImport:
        from_ = q.receive(mi.padding.from_)
        parenthesized = q.receive(mi.parenthesized)
        names = q.receive(mi.padding.names)
        return replace_if_changed(mi, from_=from_, parenthesized=parenthesized, names=names)

    def _visit_key_value(self, kv: KeyValue, q: RpcReceiveQueue) -> KeyValue:
        key = q.receive(kv.padding.key)
        value = q.receive(kv.value)
        type_ = q.receive(kv.type)
        return replace_if_changed(kv, key=key, value=value, type=type_)

    def _visit_dict_literal(self, dl: DictLiteral, q: RpcReceiveQueue) -> DictLiteral:
        elements = q.receive(dl.padding.elements)
        type_ = q.receive(dl.type)
        return replace_if_changed(dl, elements=elements, type=type_)

    def _visit_collection_literal(self, cl: CollectionLiteral, q: RpcReceiveQueue) -> CollectionLiteral:
        kind = q.receive(cl.kind)
        elements = q.receive(cl.padding.elements)
        type_ = q.receive(cl.type)
        return replace_if_changed(cl, kind=kind, elements=elements, type=type_)

    def _visit_formatted_string(self, fs: FormattedString, q: RpcReceiveQueue) -> FormattedString:
        delimiter = q.receive(fs.delimiter)
        parts = q.receive_list(fs.parts)
        type_ = q.receive(fs.type)
        return replace_if_changed(fs, delimiter=delimiter, parts=parts, type=type_)

    def _visit_formatted_string_value(self, v: FormattedString.Value, q: RpcReceiveQueue) -> FormattedString.Value:
        expression = q.receive(v.padding.expression)
        debug = q.receive(v.padding.debug)
        conversion = q.receive(v.conversion)
        format_ = q.receive(v.format)
        return replace_if_changed(v, expression=expression, debug=debug, conversion=conversion, format=format_)

    def _visit_pass(self, pass_: Pass, q: RpcReceiveQueue) -> Pass:
        # No additional fields beyond id/prefix/markers
        return pass_

    def _visit_trailing_else_wrapper(self, tew: TrailingElseWrapper, q: RpcReceiveQueue) -> TrailingElseWrapper:
        statement = q.receive(tew.statement)
        else_block = q.receive(tew.padding.else_block)
        return replace_if_changed(tew, statement=statement, else_block=else_block)

    def _visit_comprehension_expression(self, ce: ComprehensionExpression, q: RpcReceiveQueue) -> ComprehensionExpression:
        kind = q.receive(ce.kind)
        result = q.receive(ce.result)
        clauses = q.receive_list(ce.clauses)
        suffix = q.receive(ce.suffix)
        type_ = q.receive(ce.type)
        return replace_if_changed(ce, kind=kind, result=result, clauses=clauses, suffix=suffix, type=type_)

    def _visit_comprehension_condition(self, cc: ComprehensionExpression.Condition, q: RpcReceiveQueue) -> ComprehensionExpression.Condition:
        expression = q.receive(cc.expression)
        return replace_if_changed(cc, expression=expression)

    def _visit_comprehension_clause(self, cc: ComprehensionExpression.Clause, q: RpcReceiveQueue) -> ComprehensionExpression.Clause:
        async_ = q.receive(cc.padding.async_)
        iterator_variable = q.receive(cc.iterator_variable)
        iterated_list = q.receive(cc.padding.iterated_list)
        conditions = q.receive_list(cc.conditions)
        return replace_if_changed(cc, async_=async_, iterator_variable=iterator_variable, iterated_list=iterated_list, conditions=conditions)

    def _visit_type_alias(self, ta: TypeAlias, q: RpcReceiveQueue) -> TypeAlias:
        name = q.receive(ta.name)
        value = q.receive(ta.padding.value)
        type_ = q.receive(ta.type)
        return replace_if_changed(ta, name=name, value=value, type=type_)

    def _visit_yield_from(self, yf: YieldFrom, q: RpcReceiveQueue) -> YieldFrom:
        expression = q.receive(yf.expression)
        type_ = q.receive(yf.type)
        return replace_if_changed(yf, expression=expression, type=type_)

    def _visit_union_type(self, ut: UnionType, q: RpcReceiveQueue) -> UnionType:
        types = q.receive_list(ut.padding.types)
        type_ = q.receive(ut.type)
        return replace_if_changed(ut, types=types, type=type_)

    def _visit_variable_scope(self, vs: VariableScope, q: RpcReceiveQueue) -> VariableScope:
        kind = q.receive(vs.kind)
        names = q.receive_list(vs.padding.names)
        return replace_if_changed(vs, kind=kind, names=names)

    def _visit_del(self, del_: Del, q: RpcReceiveQueue) -> Del:
        targets = q.receive_list(del_.padding.targets)
        return replace_if_changed(del_, targets=targets)

    def _visit_special_parameter(self, sp: SpecialParameter, q: RpcReceiveQueue) -> SpecialParameter:
        kind = q.receive(sp.kind)
        type_hint = q.receive(sp.type_hint)
        type_ = q.receive(sp.type)
        return replace_if_changed(sp, kind=kind, type_hint=type_hint, type=type_)

    def _visit_star(self, star: Star, q: RpcReceiveQueue) -> Star:
        kind = q.receive(star.kind)
        expression = q.receive(star.expression)
        type_ = q.receive(star.type)
        return replace_if_changed(star, kind=kind, expression=expression, type=type_)

    def _visit_named_argument(self, na: NamedArgument, q: RpcReceiveQueue) -> NamedArgument:
        name = q.receive(na.name)
        value = q.receive(na.padding.value)
        type_ = q.receive(na.type)
        return replace_if_changed(na, name=name, value=value, type=type_)

    def _visit_type_hinted_expression(self, the: TypeHintedExpression, q: RpcReceiveQueue) -> TypeHintedExpression:
        expression = q.receive(the.expression)
        type_hint = q.receive(the.type_hint)
        type_ = q.receive(the.type)
        return replace_if_changed(the, expression=expression, type_hint=type_hint, type=type_)

    def _visit_error_from(self, ef: ErrorFrom, q: RpcReceiveQueue) -> ErrorFrom:
        error = q.receive(ef.error)
        from_ = q.receive(ef.padding.from_)
        type_ = q.receive(ef.type)
        return replace_if_changed(ef, error=error, from_=from_, type=type_)

    def _visit_match_case(self, mc: MatchCase, q: RpcReceiveQueue) -> MatchCase:
        pattern = q.receive(mc.pattern)
        guard = q.receive(mc.padding.guard)
        type_ = q.receive(mc.type)
        return replace_if_changed(mc, pattern=pattern, guard=guard, type=type_)

    def _visit_match_case_pattern(self, p: MatchCase.Pattern, q: RpcReceiveQueue) -> MatchCase.Pattern:
        kind = q.receive(p.kind)
        children = q.receive(p.padding.children)
        type_ = q.receive(p.type)
        return replace_if_changed(p, kind=kind, children=children, type=type_)

    def _visit_slice(self, slice_: Slice, q: RpcReceiveQueue) -> Slice:
        start = q.receive(slice_.padding.start)
        stop = q.receive(slice_.padding.stop)
        step = q.receive(slice_.padding.step)
        # Python's Slice doesn't have a type field, but Java sends one (always null)
        _ = q.receive(None)  # Consume the null type field
        return replace_if_changed(slice_, start=start, stop=stop, step=step)

    def _visit_java(self, j: J, q: RpcReceiveQueue) -> J:
        """Handle Java tree types that Python extends."""
        from rewrite.java.tree import (
            Identifier, Literal, MethodInvocation, FieldAccess,
            ArrayAccess, ArrayDimension, Block, If, Try, MethodDeclaration,
            ClassDeclaration, VariableDeclarations, Return, Assignment,
            AssignmentOperation, Unary, Ternary, Lambda, Empty, Throw,
            Assert, Break, Continue, WhileLoop, ForEachLoop, Switch, Case,
            Annotation, Import, Binary as JBinary, Parentheses, ControlParentheses,
            NewArray, Modifier, Yield, ParameterizedType, TypeParameter, TypeParameters
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
        elif isinstance(j, ParameterizedType):
            return self._visit_j_parameterized_type(j, q)
        elif isinstance(j, TypeParameter):
            return self._visit_j_type_parameter(j, q)
        elif isinstance(j, TypeParameters):
            return self._visit_j_type_parameters(j, q)

        return j

    def _visit_identifier(self, ident, q: RpcReceiveQueue):
        annotations = q.receive_list(ident.annotations)
        simple_name = q.receive(ident.simple_name)
        type_ = q.receive(ident.type)
        field_type = q.receive(ident.field_type)
        return replace_if_changed(ident, annotations=annotations, simple_name=simple_name, type=type_, field_type=field_type)

    def _visit_literal(self, lit, q: RpcReceiveQueue):
        value = q.receive(lit.value)
        value_source = q.receive(lit.value_source)
        unicode_escapes = q.receive(lit.unicode_escapes)
        type_ = q.receive(lit.type)
        return replace_if_changed(lit, value=value, value_source=value_source, unicode_escapes=unicode_escapes, type=type_)

    def _visit_import(self, imp, q: RpcReceiveQueue):
        static = q.receive(imp.padding.static)
        qualid = q.receive(imp.qualid)
        alias = q.receive(imp.padding.alias)
        return replace_if_changed(imp, static=static, qualid=qualid, alias=alias)

    def _visit_field_access(self, fa, q: RpcReceiveQueue):
        target = q.receive(fa.target)
        name = q.receive(fa.padding.name)
        type_ = q.receive(fa.type)
        return replace_if_changed(fa, target=target, name=name, type=type_)

    def _visit_method_invocation(self, mi, q: RpcReceiveQueue):
        select = q.receive(mi.padding.select)
        type_parameters = q.receive(mi.padding.type_parameters)
        name = q.receive(mi.name)
        arguments = q.receive(mi.padding.arguments)
        method_type = q.receive(mi.method_type)
        return replace_if_changed(mi, select=select, type_parameters=type_parameters, name=name, arguments=arguments, method_type=method_type)

    def _visit_block(self, block, q: RpcReceiveQueue):
        static = q.receive(block.padding.static)
        statements = q.receive_list(block.padding.statements)
        end = q.receive(block.end)
        return replace_if_changed(block, static=static, statements=statements, end=end)

    def _visit_j_unary(self, unary, q: RpcReceiveQueue):
        operator = q.receive(unary.padding.operator)
        expression = q.receive(unary.expression)
        type_ = q.receive(unary.type)
        return replace_if_changed(unary, operator=operator, expression=expression, type=type_)

    def _visit_j_binary(self, binary, q: RpcReceiveQueue):
        left = q.receive(binary.left)
        operator = q.receive(binary.padding.operator)
        right = q.receive(binary.right)
        type_ = q.receive(binary.type)
        return replace_if_changed(binary, left=left, operator=operator, right=right, type=type_)

    def _visit_j_assignment(self, assign, q: RpcReceiveQueue):
        variable = q.receive(assign.variable)
        assignment = q.receive(assign.padding.assignment)
        type_ = q.receive(assign.type)
        return replace_if_changed(assign, variable=variable, assignment=assignment, type=type_)

    def _visit_j_assignment_operation(self, assign, q: RpcReceiveQueue):
        variable = q.receive(assign.variable)
        operator = q.receive(assign.padding.operator)
        assignment = q.receive(assign.assignment)
        type_ = q.receive(assign.type)
        return replace_if_changed(assign, variable=variable, operator=operator, assignment=assignment, type=type_)

    def _visit_j_return(self, ret, q: RpcReceiveQueue):
        expression = q.receive(ret.expression)
        return replace_if_changed(ret, expression=expression)

    def _visit_j_if(self, if_stmt, q: RpcReceiveQueue):
        if_condition = q.receive(if_stmt.if_condition)
        then_part = q.receive(if_stmt.padding.then_part)
        else_part = q.receive(if_stmt.else_part)
        return replace_if_changed(if_stmt, if_condition=if_condition, then_part=then_part, else_part=else_part)

    def _visit_j_else(self, else_stmt, q: RpcReceiveQueue):
        body = q.receive(else_stmt.padding.body)
        return replace_if_changed(else_stmt, body=body)

    def _visit_j_while_loop(self, while_loop, q: RpcReceiveQueue):
        condition = q.receive(while_loop.condition)
        body = q.receive(while_loop.padding.body)
        return replace_if_changed(while_loop, condition=condition, body=body)

    def _visit_j_for_each_loop(self, for_each, q: RpcReceiveQueue):
        control = q.receive(for_each.control)
        body = q.receive(for_each.padding.body)
        return replace_if_changed(for_each, control=control, body=body)

    def _visit_j_for_each_control(self, control, q: RpcReceiveQueue):
        variable = q.receive(control.padding.variable)
        iterable = q.receive(control.padding.iterable)
        return replace_if_changed(control, variable=variable, iterable=iterable)

    def _visit_j_try(self, try_stmt, q: RpcReceiveQueue):
        resources = q.receive(
            try_stmt.padding.resources if hasattr(try_stmt.padding, 'resources') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        body = q.receive(try_stmt.body)
        catches = q.receive_list(try_stmt.catches)
        finally_ = q.receive(
            try_stmt.padding.finally_ if hasattr(try_stmt.padding, 'finally_') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        return replace_if_changed(try_stmt, resources=resources, body=body, catches=catches, finally_=finally_)

    def _visit_j_catch(self, catch, q: RpcReceiveQueue):
        parameter = q.receive(catch.parameter)
        body = q.receive(catch.body)
        return replace_if_changed(catch, parameter=parameter, body=body)

    def _visit_j_try_resource(self, resource, q: RpcReceiveQueue):
        variable_declarations = q.receive(resource.variable_declarations)
        terminated_with_semicolon = q.receive(resource.terminated_with_semicolon)
        return replace_if_changed(resource, variable_declarations=variable_declarations, terminated_with_semicolon=terminated_with_semicolon)

    def _visit_j_throw(self, throw, q: RpcReceiveQueue):
        exception = q.receive(throw.exception)
        return replace_if_changed(throw, exception=exception)

    def _visit_j_assert(self, assert_stmt, q: RpcReceiveQueue):
        condition = q.receive(assert_stmt.condition)
        detail = q.receive(assert_stmt.detail)
        return replace_if_changed(assert_stmt, condition=condition, detail=detail)

    def _visit_j_break(self, break_stmt, q: RpcReceiveQueue):
        label = q.receive(break_stmt.label)
        return replace_if_changed(break_stmt, label=label)

    def _visit_j_continue(self, continue_stmt, q: RpcReceiveQueue):
        label = q.receive(continue_stmt.label)
        return replace_if_changed(continue_stmt, label=label)

    def _visit_j_empty(self, empty, q: RpcReceiveQueue):
        # No additional fields
        return empty

    def _visit_j_ternary(self, ternary, q: RpcReceiveQueue):
        condition = q.receive(ternary.condition)
        true_part = q.receive(ternary.padding.true_part)
        false_part = q.receive(ternary.padding.false_part)
        type_ = q.receive(ternary.type)
        return replace_if_changed(ternary, condition=condition, true_part=true_part, false_part=false_part, type=type_)

    def _visit_j_lambda(self, lam, q: RpcReceiveQueue):
        parameters = q.receive(lam.parameters)
        arrow = q.receive(lam.arrow)
        body = q.receive(lam.body)
        type_ = q.receive(lam.type)
        return replace_if_changed(lam, parameters=parameters, arrow=arrow, body=body, type=type_)

    def _visit_j_lambda_parameters(self, params, q: RpcReceiveQueue):
        parenthesized = q.receive(params.parenthesized)
        parameters = q.receive_list(params.padding.parameters)
        return replace_if_changed(params, parenthesized=parenthesized, parameters=parameters)

    def _visit_j_variable_declarations(self, var_decl, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(var_decl.leading_annotations)
        modifiers = q.receive_list(var_decl.modifiers)
        type_expression = q.receive(var_decl.type_expression)
        varargs = q.receive(var_decl.varargs)
        variables = q.receive_list(var_decl.padding.variables)
        return replace_if_changed(var_decl, leading_annotations=leading_annotations, modifiers=modifiers,
                                  type_expression=type_expression, varargs=varargs, variables=variables)

    def _visit_j_named_variable(self, var, q: RpcReceiveQueue):
        name = q.receive(var.name)
        dimensions_after_name = q.receive_list(var.dimensions_after_name)
        initializer = q.receive(
            var.padding.initializer if hasattr(var.padding, 'initializer') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        variable_type = q.receive(var.variable_type)
        return replace_if_changed(var, name=name, dimensions_after_name=dimensions_after_name,
                                  initializer=initializer, variable_type=variable_type)

    def _visit_j_class_declaration(self, class_decl, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(class_decl.leading_annotations)
        modifiers = q.receive_list(class_decl.modifiers)
        kind = q.receive(class_decl.padding.kind)
        name = q.receive(class_decl.name)
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
        body = q.receive(class_decl.body)
        return replace_if_changed(class_decl, leading_annotations=leading_annotations, modifiers=modifiers,
                                  kind=kind, name=name, type_parameters=type_parameters,
                                  primary_constructor=primary_constructor, extends=extends, implements=implements,
                                  permits=permits, body=body)

    def _visit_j_class_declaration_kind(self, kind, q: RpcReceiveQueue):
        # Note: _pre_visit is already called by _visit before this method
        annotations = q.receive_list(kind.annotations)
        type_ = q.receive(kind.type)  # Enum type
        return replace_if_changed(kind, annotations=annotations, type=type_)

    def _visit_j_method_declaration(self, method, q: RpcReceiveQueue):
        leading_annotations = q.receive_list(method.leading_annotations)
        modifiers = q.receive_list(method.modifiers)
        type_parameters = q.receive(
            method.padding.type_parameters if hasattr(method.padding, 'type_parameters') else None,
            lambda el: self._visit(el, q) if el else None
        )
        return_type_expression = q.receive(method.return_type_expression)
        # Simplified model: nameAnnotations and name are separate fields (like TypeScript)
        name_annotations = q.receive_list(method.name_annotations if method.name_annotations else [])
        name = q.receive(method.name)
        parameters = q.receive(method.padding.parameters, lambda c: self._receive_container(c, q) if c else None)
        throws = q.receive(
            method.padding.throws if hasattr(method.padding, 'throws') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        body = q.receive(method.body)
        default_value = q.receive(
            method.padding.default_value if hasattr(method.padding, 'default_value') else None,
            lambda lp: self._receive_left_padded(lp, q) if lp else None
        )
        method_type = q.receive(method.method_type)
        return replace_if_changed(method, leading_annotations=leading_annotations, modifiers=modifiers,
                                  type_parameters=type_parameters, return_type_expression=return_type_expression,
                                  name_annotations=name_annotations, name=name,
                                  parameters=parameters, throws=throws, body=body, default_value=default_value,
                                  method_type=method_type)

    def _visit_j_switch(self, switch, q: RpcReceiveQueue):
        selector = q.receive(switch.selector)
        cases = q.receive(switch.cases)
        return replace_if_changed(switch, selector=selector, cases=cases)

    def _visit_j_case(self, case, q: RpcReceiveQueue):
        type_ = q.receive(case.type)  # Enum type
        case_labels = q.receive(case.padding.case_labels)
        statements = q.receive(case.padding.statements)
        body = q.receive(case.padding.body, lambda rp: self._receive_right_padded(rp, q) if rp else None)
        guard = q.receive(case.guard, lambda el: self._visit(el, q) if el else None)
        return replace_if_changed(case, type=type_, case_labels=case_labels, statements=statements, body=body, guard=guard)

    def _visit_j_array_access(self, arr, q: RpcReceiveQueue):
        indexed = q.receive(arr.indexed)
        dimension = q.receive(arr.dimension)
        return replace_if_changed(arr, indexed=indexed, dimension=dimension)

    def _visit_j_array_dimension(self, dim, q: RpcReceiveQueue):
        index = q.receive(dim.padding.index)
        return replace_if_changed(dim, index=index)

    def _visit_j_new_array(self, new_arr, q: RpcReceiveQueue):
        type_expression = q.receive(new_arr.type_expression)
        dimensions = q.receive_list(new_arr.dimensions)
        initializer = q.receive(
            new_arr.padding.initializer if hasattr(new_arr.padding, 'initializer') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        type_ = q.receive(new_arr.type)
        return replace_if_changed(new_arr, type_expression=type_expression, dimensions=dimensions,
                                  initializer=initializer, type=type_)

    def _visit_j_annotation(self, annot, q: RpcReceiveQueue):
        annotation_type = q.receive(annot.annotation_type)
        arguments = q.receive(
            annot.padding.arguments if hasattr(annot.padding, 'arguments') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        return replace_if_changed(annot, annotation_type=annotation_type, arguments=arguments)

    def _visit_j_parentheses(self, parens, q: RpcReceiveQueue):
        tree = q.receive(parens.padding.tree)
        return replace_if_changed(parens, tree=tree)

    def _visit_j_control_parentheses(self, parens, q: RpcReceiveQueue):
        tree = q.receive(parens.padding.tree)
        return replace_if_changed(parens, tree=tree)

    def _visit_j_modifier(self, mod, q: RpcReceiveQueue):
        keyword = q.receive(mod.keyword)
        type_ = q.receive(mod.type)  # Enum type
        annotations = q.receive_list(mod.annotations)
        return replace_if_changed(mod, keyword=keyword, type=type_, annotations=annotations)

    def _visit_j_yield(self, yield_stmt, q: RpcReceiveQueue):
        implicit = q.receive(yield_stmt.implicit)
        value = q.receive(yield_stmt.value)
        return replace_if_changed(yield_stmt, implicit=implicit, value=value)

    def _visit_j_parameterized_type(self, param_type, q: RpcReceiveQueue):
        clazz = q.receive(param_type.clazz)
        type_parameters = q.receive(
            param_type.padding.type_parameters if hasattr(param_type.padding, 'type_parameters') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        type_ = q.receive(param_type.type)
        return replace_if_changed(param_type, clazz=clazz, type_parameters=type_parameters, type=type_)

    def _visit_j_type_parameter(self, tp, q: RpcReceiveQueue):
        annotations = q.receive_list(tp.annotations)
        modifiers = q.receive_list(tp.modifiers)
        name = q.receive(tp.name)
        bounds = q.receive(
            tp.padding.bounds if hasattr(tp, 'padding') and hasattr(tp.padding, 'bounds') else None,
            lambda c: self._receive_container(c, q) if c else None
        )
        return replace_if_changed(tp, annotations=annotations, modifiers=modifiers, name=name, bounds=bounds)

    def _visit_j_type_parameters(self, tps, q: RpcReceiveQueue):
        annotations = q.receive_list(tps.annotations)
        type_parameters = q.receive_list(tps.padding.type_parameters, lambda rp: self._receive_right_padded(rp, q))
        return replace_if_changed(tps, annotations=annotations, type_parameters=type_parameters)

    # Helper methods for Space, JRightPadded, JLeftPadded, JContainer

    def _receive_space(self, space: Space, q: RpcReceiveQueue) -> Space:
        """Receive a Space object."""
        if space is None:
            return Space.EMPTY

        comments = q.receive_list_defined(space.comments)
        whitespace = q.receive(space.whitespace)

        if comments is space.comments and whitespace is space.whitespace:
            return space

        return space.replace(comments=comments).replace(whitespace=whitespace)

    def _receive_comment(self, comment, q: RpcReceiveQueue):
        """Receive a Comment object (TextComment only for Python)."""
        from rewrite.java.support_types import TextComment

        # Handle new comments (comment is None or a dict from _new_obj)
        if comment is None or isinstance(comment, dict):
            # For new comments, read all fields directly - all are non-optional
            multiline = q.receive_defined(None)
            text = q.receive_defined(None)
            suffix = q.receive_defined(None)
            markers = q.receive_markers(None)
            return TextComment(multiline, text, suffix, markers)

        multiline = q.receive_defined(comment.multiline)
        text = q.receive_defined(comment.text)
        suffix = q.receive_defined(comment.suffix)
        markers = q.receive_markers(comment.markers)

        return TextComment(multiline, text, suffix, markers)

    def _receive_right_padded(self, rp: JRightPadded, q: RpcReceiveQueue) -> Optional[JRightPadded]:
        """Receive a JRightPadded wrapper."""
        if rp is None:
            return None

        # Codec registry handles type dispatch automatically
        element = q.receive(rp.element)
        after = q.receive_defined(rp.after)
        markers = q.receive_markers(rp.markers)

        if element is rp.element and after is rp.after and markers is rp.markers:
            return rp

        return JRightPadded(element, after, markers)

    def _receive_left_padded(self, lp: JLeftPadded, q: RpcReceiveQueue) -> Optional[JLeftPadded]:
        """Receive a JLeftPadded wrapper."""
        if lp is None:
            return None

        # Codec registry handles type dispatch automatically
        before = q.receive_defined(lp.before)
        element = q.receive(lp.element)
        markers = q.receive_markers(lp.markers)

        if before is lp.before and element is lp.element and markers is lp.markers:
            return lp

        return JLeftPadded(before, element, markers)

    def _receive_container(self, container: JContainer, q: RpcReceiveQueue) -> Optional[JContainer]:
        """Receive a JContainer wrapper."""
        if container is None:
            return None

        # Codec registry handles type dispatch automatically
        before = q.receive_defined(container.before)
        elements = q.receive_list_defined(container.padding.elements)
        markers = q.receive_markers(container.markers)

        if before is container.before and elements is container.padding.elements and markers is container.markers:
            return container

        return JContainer(before, elements, markers)

    def _receive_type(self, java_type, q: RpcReceiveQueue):
        """Receive a JavaType object with expanded fields.

        This matches the sender's _visit_type which sends expanded type fields.
        The callback pattern ensures message counts match between sender and receiver.
        """
        from rewrite.java.support_types import JavaType as JT

        if java_type is None:
            return None

        if isinstance(java_type, JT.Primitive):
            # For Primitive types, receive the keyword
            keyword = q.receive(None)
            # Map keyword back to JavaType.Primitive enum
            keyword_to_primitive = {
                'boolean': JT.Primitive.Boolean,
                'byte': JT.Primitive.Byte,
                'char': JT.Primitive.Char,
                'double': JT.Primitive.Double,
                'float': JT.Primitive.Float,
                'int': JT.Primitive.Int,
                'long': JT.Primitive.Long,
                'short': JT.Primitive.Short,
                'void': JT.Primitive.Void,
                'String': JT.Primitive.String,
                '': JT.Primitive.None_,
                'null': JT.Primitive.Null,
            }
            return keyword_to_primitive.get(keyword, java_type)

        elif isinstance(java_type, JT.Method):
            # Method: declaringType, name, flagsBitMap, returnType, parameterNames,
            #         parameterTypes, thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
            declaring_type = q.receive(getattr(java_type, '_declaring_type', None),
                                        lambda t: self._receive_type(t, q))
            name = q.receive(getattr(java_type, '_name', ''))
            flags = q.receive(getattr(java_type, '_flags_bit_map', 0))
            return_type = q.receive(getattr(java_type, '_return_type', None),
                                     lambda t: self._receive_type(t, q))
            param_names = q.receive_list(getattr(java_type, '_parameter_names', None) or [])
            param_types = q.receive_list(getattr(java_type, '_parameter_types', None) or [],
                                          lambda t: self._receive_type(t, q))
            thrown = q.receive_list(getattr(java_type, '_thrown_exceptions', None) or [],
                                     lambda t: self._receive_type(t, q))
            annotations = q.receive_list(getattr(java_type, '_annotations', None) or [],
                                          lambda t: self._receive_type(t, q))
            default_value = q.receive_list(getattr(java_type, '_default_value', None) or [])
            formal_type_names = q.receive_list(getattr(java_type, '_declared_formal_type_names', None) or [])

            return JT.Method(
                _flags_bit_map=flags,
                _declaring_type=declaring_type,
                _name=name,
                _return_type=return_type,
                _parameter_names=param_names,
                _parameter_types=param_types,
                _thrown_exceptions=thrown,
                _annotations=annotations,
                _default_value=default_value,
                _declared_formal_type_names=formal_type_names,
            )

        elif isinstance(java_type, JT.Class):
            # Class: flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype,
            #        owningClass, annotations, interfaces, members, methods
            flags = q.receive(getattr(java_type, '_flags_bit_map', 0))
            kind = q.receive(getattr(java_type, '_kind', JT.FullyQualified.Kind.Class))
            fqn = q.receive(getattr(java_type, '_fully_qualified_name', ''))
            type_params = q.receive_list(getattr(java_type, '_type_parameters', None) or [],
                                          lambda t: self._receive_type(t, q))
            supertype = q.receive(getattr(java_type, '_supertype', None),
                                   lambda t: self._receive_type(t, q))
            owning_class = q.receive(getattr(java_type, '_owning_class', None),
                                      lambda t: self._receive_type(t, q))
            annotations = q.receive_list(getattr(java_type, '_annotations', None) or [],
                                          lambda t: self._receive_type(t, q))
            interfaces = q.receive_list(getattr(java_type, '_interfaces', None) or [],
                                         lambda t: self._receive_type(t, q))
            members = q.receive_list(getattr(java_type, '_members', None) or [],
                                      lambda t: self._receive_type(t, q))
            methods = q.receive_list(getattr(java_type, '_methods', None) or [],
                                      lambda t: self._receive_type(t, q))

            class_type = JT.Class()
            class_type._flags_bit_map = flags
            class_type._kind = kind
            class_type._fully_qualified_name = fqn
            class_type._type_parameters = type_params
            class_type._supertype = supertype
            class_type._owning_class = owning_class
            class_type._annotations = annotations
            class_type._interfaces = interfaces
            class_type._members = members
            class_type._methods = methods
            return class_type

        elif isinstance(java_type, JT.Unknown):
            # Unknown has no additional fields
            return java_type

        # Default: return as-is
        return java_type


# Register marker codecs
def _register_marker_codecs():
    """Register receive and send codecs for Java marker types."""
    from rewrite import Markers
    from rewrite.java.markers import Semicolon, TrailingComma, OmitParentheses
    from rewrite.java.support_types import Space
    from rewrite.rpc.receive_queue import register_codec_with_both_names, register_send_codec
    from rewrite.rpc.send_queue import RpcSendQueue

    # Markers send codec (Markers uses special handling in receive_queue, but needs send codec)
    def _send_markers(markers: Markers, q: RpcSendQueue) -> None:
        q.get_and_send(markers, lambda x: x.id)
        q.get_and_send_list(markers, lambda x: x.markers, lambda m: m.id, None)

    register_send_codec(Markers, _send_markers)

    # Receive codecs
    def _receive_semicolon(semicolon: Semicolon, q: RpcReceiveQueue) -> Semicolon:
        new_id = q.receive_defined(semicolon.id)
        if new_id is semicolon.id:
            return semicolon
        return semicolon.replace(id=new_id)

    def _receive_trailing_comma(trailing_comma: TrailingComma, q: RpcReceiveQueue) -> TrailingComma:
        new_id = q.receive_defined(trailing_comma.id)
        new_suffix = q.receive_defined(trailing_comma.suffix)
        if new_id is trailing_comma.id and new_suffix is trailing_comma.suffix:
            return trailing_comma
        result = trailing_comma
        if new_id is not trailing_comma.id:
            result = result.replace(id=new_id)
        if new_suffix is not trailing_comma.suffix:
            result = result.replace(suffix=new_suffix)
        return result

    def _receive_omit_parentheses(omit_paren: OmitParentheses, q: RpcReceiveQueue) -> OmitParentheses:
        new_id = q.receive_defined(omit_paren.id)
        if new_id is omit_paren.id:
            return omit_paren
        return omit_paren.replace(id=new_id)

    # Send codecs
    def _send_semicolon(marker: Semicolon, q: RpcSendQueue) -> None:
        q.get_and_send(marker, lambda x: x.id)

    def _send_trailing_comma(marker: TrailingComma, q: RpcSendQueue) -> None:
        q.get_and_send(marker, lambda x: x.id)
        q.get_and_send(marker, lambda x: x.suffix, lambda s: _get_sender()._visit_space(s, q))

    def _send_omit_parentheses(marker: OmitParentheses, q: RpcSendQueue) -> None:
        q.get_and_send(marker, lambda x: x.id)

    from uuid import uuid4

    # Use register_codec_with_both_names to ensure the sender can look up Java type names
    register_codec_with_both_names(
        'org.openrewrite.java.marker.Semicolon',
        Semicolon,
        _receive_semicolon,
        lambda: Semicolon(uuid4()),
        _send_semicolon
    )
    register_codec_with_both_names(
        'org.openrewrite.java.marker.TrailingComma',
        TrailingComma,
        _receive_trailing_comma,
        lambda: TrailingComma(uuid4(), Space.EMPTY),
        _send_trailing_comma
    )
    register_codec_with_both_names(
        'org.openrewrite.java.marker.OmitParentheses',
        OmitParentheses,
        _receive_omit_parentheses,
        lambda: OmitParentheses(uuid4()),
        _send_omit_parentheses
    )


# ============================================================================
# Codec registration - registers all AST, support, and marker types
# ============================================================================

# Shared receiver instance (stateless, so one instance works for all codecs)
_python_receiver = None
# Shared sender instance for marker codecs
_python_sender = None


def _get_receiver():
    """Lazily get the shared PythonRpcReceiver instance."""
    global _python_receiver
    if _python_receiver is None:
        _python_receiver = PythonRpcReceiver()
    return _python_receiver


def _get_sender():
    """Lazily get the shared PythonRpcSender instance."""
    global _python_sender
    if _python_sender is None:
        from rewrite.rpc.python_sender import PythonRpcSender
        _python_sender = PythonRpcSender()
    return _python_sender


def _receive_j(j, q: RpcReceiveQueue):
    """Codec for receiving J (Java) tree nodes."""
    return _get_receiver()._visit(j, q)


def _receive_space(space, q: RpcReceiveQueue):
    """Codec for receiving Space objects."""
    return _get_receiver()._receive_space(space, q)


def _receive_right_padded(rp, q: RpcReceiveQueue):
    """Codec for receiving JRightPadded objects."""
    return _get_receiver()._receive_right_padded(rp, q)


def _receive_left_padded(lp, q: RpcReceiveQueue):
    """Codec for receiving JLeftPadded objects."""
    return _get_receiver()._receive_left_padded(lp, q)


def _receive_container(c, q: RpcReceiveQueue):
    """Codec for receiving JContainer objects."""
    return _get_receiver()._receive_container(c, q)


def _receive_comment(comment, q: RpcReceiveQueue):
    """Codec for receiving Comment objects."""
    return _get_receiver()._receive_comment(comment, q)


def _receive_search_result(marker, q: RpcReceiveQueue):
    """Codec for receiving SearchResult marker."""
    from rewrite.markers import SearchResult
    from uuid import UUID

    # SearchResult sends: id, description
    before_id = str(marker.id) if marker and marker.id else None
    id_str = q.receive(before_id)
    description = q.receive(marker.description if marker else None)

    new_id = UUID(id_str) if id_str else (marker.id if marker else None)
    return SearchResult(_id=new_id, _description=description)


def _receive_parse_exception_result(marker, q: RpcReceiveQueue):
    """Codec for receiving ParseExceptionResult marker."""
    from rewrite.markers import ParseExceptionResult

    # ParseExceptionResult sends: id, parserType, message, exceptionType, treeType
    id_str = q.receive(str(marker.id) if marker else None)
    parser_type = q.receive(marker.parser_type if marker else None)
    message = q.receive(marker.message if marker else None)
    exception_type = q.receive(marker.exception_type if marker else None)
    tree_type = q.receive(marker.tree_type if marker else None)

    from uuid import UUID
    new_id = UUID(id_str) if id_str else (marker.id if marker else None)
    return ParseExceptionResult(
        id=new_id,
        parser_type=parser_type,
        message=message,
        exception_type=exception_type,
        tree_type=tree_type
    )


def _receive_style(style, q: RpcReceiveQueue):
    """Codec for receiving Style objects."""
    # For now, styles are passed through - full deserialization would need more work
    return style


def _receive_java_type_primitive(primitive, q: RpcReceiveQueue):
    """Codec for receiving JavaType.Primitive - consumes the keyword message.

    The sender sends an ADD with valueType='JavaType$Primitive', then sends
    the keyword as a primitive string value. We need to consume both and
    return the appropriate JavaType.Primitive enum value.
    """
    from rewrite.java.support_types import JavaType as JT

    # Consume the keyword message that the sender sends
    keyword = q.receive(None)

    # Map keyword back to JavaType.Primitive enum
    keyword_to_primitive = {
        'boolean': JT.Primitive.Boolean,
        'byte': JT.Primitive.Byte,
        'char': JT.Primitive.Char,
        'double': JT.Primitive.Double,
        'float': JT.Primitive.Float,
        'int': JT.Primitive.Int,
        'long': JT.Primitive.Long,
        'short': JT.Primitive.Short,
        'void': JT.Primitive.Void,
        'String': JT.Primitive.String,
        '': JT.Primitive.None_,
        'null': JT.Primitive.Null,
    }

    return keyword_to_primitive.get(keyword, primitive)


def _receive_java_type_unknown(unknown, q: RpcReceiveQueue):
    """Codec for receiving JavaType.Unknown - no additional fields to consume.

    JavaType.Unknown is a marker type used when type information is not available.
    The Java sender doesn't send any additional fields for Unknown types.
    """
    # No additional fields to consume - just return the unknown object
    return unknown


def _receive_java_type_method(method, q: RpcReceiveQueue):
    """Codec for receiving JavaType.Method - consumes all method fields."""
    from rewrite.java.support_types import JavaType as JT

    # Receive fields in the same order as JavaTypeSender.visitMethod:
    # declaringType, name, flagsBitMap, returnType, parameterNames,
    # parameterTypes, thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
    declaring_type = q.receive(method._declaring_type if method else None)
    name = q.receive(method._name if method else '')
    flags = q.receive(method._flags_bit_map if method else 0)
    return_type = q.receive(method._return_type if method else None)
    param_names = q.receive_list(method._parameter_names if method else None)
    param_types = q.receive_list(method._parameter_types if method else None)
    thrown = q.receive_list(method._thrown_exceptions if method else None)
    annotations = q.receive_list(method._annotations if method else None)
    default_value = q.receive_list(method._default_value if method else None)
    formal_type_names = q.receive_list(method._declared_formal_type_names if method else None)

    return JT.Method(
        _flags_bit_map=flags,
        _declaring_type=declaring_type,
        _name=name,
        _return_type=return_type,
        _parameter_names=param_names,
        _parameter_types=param_types,
        _thrown_exceptions=thrown,
        _annotations=annotations,
        _default_value=default_value,
        _declared_formal_type_names=formal_type_names,
    )


def _receive_java_type_class(cls, q: RpcReceiveQueue):
    """Codec for receiving JavaType.Class - consumes all class fields."""
    from rewrite.java.support_types import JavaType as JT

    # Receive fields in the same order as JavaTypeSender.visitClass:
    # flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype,
    # owningClass, annotations, interfaces, members, methods
    flags = q.receive(getattr(cls, '_flags_bit_map', 0) if cls else 0)
    kind = q.receive(getattr(cls, '_kind', JT.FullyQualified.Kind.Class) if cls else JT.FullyQualified.Kind.Class)
    fqn = q.receive(getattr(cls, '_fully_qualified_name', '') if cls else '')
    type_params = q.receive_list(getattr(cls, '_type_parameters', None) if cls else None)
    supertype = q.receive(getattr(cls, '_supertype', None) if cls else None)
    owning_class = q.receive(getattr(cls, '_owning_class', None) if cls else None)
    annotations = q.receive_list(getattr(cls, '_annotations', None) if cls else None)
    interfaces = q.receive_list(getattr(cls, '_interfaces', None) if cls else None)
    members = q.receive_list(getattr(cls, '_members', None) if cls else None)
    methods = q.receive_list(getattr(cls, '_methods', None) if cls else None)

    # Create a new Class instance and set attributes
    class_type = JT.Class()
    class_type._flags_bit_map = flags
    class_type._kind = kind
    class_type._fully_qualified_name = fqn
    class_type._type_parameters = type_params
    class_type._supertype = supertype
    class_type._owning_class = owning_class
    class_type._annotations = annotations
    class_type._interfaces = interfaces
    class_type._members = members
    class_type._methods = methods

    return class_type


def _register_java_type_codecs():
    """Register codecs for JavaType classes."""
    from rewrite.java.support_types import JavaType as JT
    from rewrite.rpc.receive_queue import register_codec_with_both_names, register_receive_codec, register_send_codec
    from rewrite.rpc.send_queue import RpcSendQueue

    # JavaType.Primitive - special handling to consume keyword
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JavaType$Primitive',
        JT.Primitive,
        _receive_java_type_primitive,
        lambda: JT.Primitive.None_  # Default factory returns None_ primitive
    )

    # JavaType.Unknown - no additional fields
    # Note: JavaType.Unknown is a nested class inside JavaType, not a standalone type
    # in Python's support_types.py, so we register by Java type name and Python name separately
    register_receive_codec(
        'org.openrewrite.java.tree.JavaType$Unknown',
        _receive_java_type_unknown,
        lambda: JT.Unknown()  # Factory creates a new Unknown instance
    )
    register_receive_codec(
        'Unknown',  # Python class name for _get_codec lookup
        _receive_java_type_unknown,
        lambda: JT.Unknown()
    )

    # JavaType.Method - full serialization of method type info
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JavaType$Method',
        JT.Method,
        _receive_java_type_method,
        lambda: JT.Method()  # Factory creates empty Method
    )

    # JavaType.Class - full serialization of class type info
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JavaType$Class',
        JT.Class,
        _receive_java_type_class,
        lambda: JT.Class()  # Factory creates empty Class
    )


def _register_tree_codecs():
    """Register codecs for all AST types using reflection."""
    from rewrite.python import tree as py_tree
    from rewrite.java import tree as j_tree
    from rewrite.rpc.receive_queue import (
        register_codec_with_both_names,
        get_all_tree_classes,
        make_dataclass_factory
    )

    # Register Py types (including nested like ComprehensionExpression.Clause)
    for name, cls in get_all_tree_classes(py_tree, 'rewrite.python.tree'):
        java_type = f'org.openrewrite.python.tree.Py${name.replace(".", "$")}'
        register_codec_with_both_names(java_type, cls, _receive_j, make_dataclass_factory(cls))

    # Register J types (including nested like ForEachLoop.Control, If.Else, etc.)
    for name, cls in get_all_tree_classes(j_tree, 'rewrite.java.tree'):
        java_type = f'org.openrewrite.java.tree.J${name.replace(".", "$")}'
        register_codec_with_both_names(java_type, cls, _receive_j, make_dataclass_factory(cls))


def _register_support_type_codecs():
    """Register codecs for support types."""
    from rewrite import Markers
    from rewrite.java import Space
    from rewrite.java.support_types import JRightPadded, JLeftPadded, JContainer, TextComment
    from rewrite.rpc.receive_queue import register_codec_with_both_names, make_dataclass_factory

    # Space
    register_codec_with_both_names(
        'org.openrewrite.java.tree.Space',
        Space,
        _receive_space,
        make_dataclass_factory(Space)
    )

    # JRightPadded, JLeftPadded, JContainer
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JRightPadded',
        JRightPadded,
        _receive_right_padded,
        make_dataclass_factory(JRightPadded)
    )
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JLeftPadded',
        JLeftPadded,
        _receive_left_padded,
        make_dataclass_factory(JLeftPadded)
    )
    register_codec_with_both_names(
        'org.openrewrite.java.tree.JContainer',
        JContainer,
        _receive_container,
        make_dataclass_factory(JContainer)
    )

    # TextComment - custom factory needed because the class has explicit __init__
    # that requires all parameters, but fields() only returns dataclass-declared fields
    register_codec_with_both_names(
        'org.openrewrite.java.tree.TextComment',
        TextComment,
        _receive_comment,
        lambda: TextComment(False, '', '', Markers.EMPTY)
    )


def _register_core_marker_codecs():
    """Register codecs for core marker types."""
    from rewrite.markers import Markers, ParseExceptionResult, SearchResult
    from rewrite.rpc.receive_queue import (
        register_codec_with_both_names,
        make_dataclass_factory,
        _receive_markers
    )

    # Core markers - Markers uses Markers.EMPTY as factory
    register_codec_with_both_names(
        'org.openrewrite.marker.Markers',
        Markers,
        _receive_markers,
        lambda: Markers.EMPTY
    )
    # SearchResult - has specific fields to receive
    register_codec_with_both_names(
        'org.openrewrite.marker.SearchResult',
        SearchResult,
        _receive_search_result,
        make_dataclass_factory(SearchResult)
    )
    # ParseExceptionResult - has specific fields to receive
    register_codec_with_both_names(
        'org.openrewrite.marker.ParseExceptionResult',
        ParseExceptionResult,
        _receive_parse_exception_result,
        make_dataclass_factory(ParseExceptionResult)
    )


def _register_style_codecs():
    """Register codecs for style types."""
    from rewrite.style import GeneralFormatStyle, NamedStyles
    from rewrite.rpc.receive_queue import register_codec_with_both_names, make_dataclass_factory

    for cls, java_name in [
        (GeneralFormatStyle, 'org.openrewrite.style.GeneralFormatStyle'),
        (NamedStyles, 'org.openrewrite.style.NamedStyles'),
    ]:
        register_codec_with_both_names(java_name, cls, _receive_style, make_dataclass_factory(cls))


# Register all codecs on module import
_register_marker_codecs()  # Existing marker codecs with full deserialization
_register_tree_codecs()
_register_support_type_codecs()
_register_java_type_codecs()  # JavaType.Primitive handling
_register_core_marker_codecs()
_register_style_codecs()
