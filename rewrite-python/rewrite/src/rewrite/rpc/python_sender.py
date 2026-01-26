"""
Python RPC Sender that mirrors Java's PythonSender structure.

This uses the visitor pattern with pre_visit handling common fields (id, prefix, markers)
and type-specific visit methods handling only additional fields.
"""
from typing import Any

from rewrite import Markers
from rewrite.java import Space, JRightPadded, JLeftPadded, JContainer, J
from rewrite.python import CompilationUnit
from rewrite.python.tree import (
    Async, Await, Binary, ChainedAssignment, ExceptionType,
    LiteralType, TypeHint, ExpressionStatement, ExpressionTypeTree,
    StatementExpression, MultiImport, KeyValue, DictLiteral, CollectionLiteral,
    FormattedString, Pass, TrailingElseWrapper, ComprehensionExpression,
    TypeAlias, YieldFrom, UnionType, VariableScope, Del, SpecialParameter,
    Star, NamedArgument, TypeHintedExpression, ErrorFrom, MatchCase, Slice
)


class PythonRpcSender:
    """Sender that mirrors Java's PythonSender for RPC serialization."""

    def send(self, after: Any, before: Any, q: 'RpcSendQueue') -> None:
        """Entry point for sending an object."""
        from rewrite.rpc.send_queue import RpcObjectState
        from rewrite.rpc.receive_queue import get_java_type_name

        if before is after:
            q.put({'state': RpcObjectState.NO_CHANGE})
            return

        if after is None:
            q.put({'state': RpcObjectState.DELETE})
            return

        if before is None:
            # ADD for new object
            value_type = get_java_type_name(type(after)) if hasattr(after, '__class__') else None
            q.put({'state': RpcObjectState.ADD, 'valueType': value_type})
            q._before = None
            self._visit(after, q)
        else:
            # CHANGE for existing object
            value_type = get_java_type_name(type(after)) if hasattr(after, '__class__') else None
            q.put({'state': RpcObjectState.CHANGE, 'valueType': value_type})
            q._before = before
            self._visit(after, q)

    def _visit(self, tree: Any, q: 'RpcSendQueue') -> None:
        """Visit a tree node, dispatching to appropriate visitor method."""
        if tree is None:
            return

        # First handle common J fields via pre_visit
        # ExpressionStatement and StatementExpression delegate prefix/markers to their child, so skip them
        if isinstance(tree, J):
            if isinstance(tree, ExpressionStatement):
                # Only send id; prefix/markers are part of expression
                q.get_and_send(tree, lambda x: x.id)
            elif isinstance(tree, StatementExpression):
                # Only send id; prefix/markers are part of statement
                q.get_and_send(tree, lambda x: x.id)
            else:
                self._pre_visit(tree, q)

        # Then dispatch to type-specific visitor
        tree_type = type(tree).__name__

        if isinstance(tree, CompilationUnit):
            self._visit_compilation_unit(tree, q)
        elif isinstance(tree, Async):
            self._visit_async(tree, q)
        elif isinstance(tree, Await):
            self._visit_await(tree, q)
        elif isinstance(tree, Binary):
            self._visit_binary(tree, q)
        elif isinstance(tree, ChainedAssignment):
            self._visit_chained_assignment(tree, q)
        elif isinstance(tree, ExceptionType):
            self._visit_exception_type(tree, q)
        elif isinstance(tree, LiteralType):
            self._visit_literal_type(tree, q)
        elif isinstance(tree, TypeHint):
            self._visit_type_hint(tree, q)
        elif isinstance(tree, ExpressionStatement):
            self._visit_expression_statement(tree, q)
        elif isinstance(tree, ExpressionTypeTree):
            self._visit_expression_type_tree(tree, q)
        elif isinstance(tree, StatementExpression):
            self._visit_statement_expression(tree, q)
        elif isinstance(tree, MultiImport):
            self._visit_multi_import(tree, q)
        elif isinstance(tree, KeyValue):
            self._visit_key_value(tree, q)
        elif isinstance(tree, DictLiteral):
            self._visit_dict_literal(tree, q)
        elif isinstance(tree, CollectionLiteral):
            self._visit_collection_literal(tree, q)
        elif isinstance(tree, FormattedString.Value):
            self._visit_formatted_string_value(tree, q)
        elif isinstance(tree, FormattedString):
            self._visit_formatted_string(tree, q)
        elif isinstance(tree, Pass):
            self._visit_pass(tree, q)
        elif isinstance(tree, TrailingElseWrapper):
            self._visit_trailing_else_wrapper(tree, q)
        elif isinstance(tree, ComprehensionExpression.Condition):
            self._visit_comprehension_condition(tree, q)
        elif isinstance(tree, ComprehensionExpression.Clause):
            self._visit_comprehension_clause(tree, q)
        elif isinstance(tree, ComprehensionExpression):
            self._visit_comprehension_expression(tree, q)
        elif isinstance(tree, TypeAlias):
            self._visit_type_alias(tree, q)
        elif isinstance(tree, YieldFrom):
            self._visit_yield_from(tree, q)
        elif isinstance(tree, UnionType):
            self._visit_union_type(tree, q)
        elif isinstance(tree, VariableScope):
            self._visit_variable_scope(tree, q)
        elif isinstance(tree, Del):
            self._visit_del(tree, q)
        elif isinstance(tree, SpecialParameter):
            self._visit_special_parameter(tree, q)
        elif isinstance(tree, Star):
            self._visit_star(tree, q)
        elif isinstance(tree, NamedArgument):
            self._visit_named_argument(tree, q)
        elif isinstance(tree, TypeHintedExpression):
            self._visit_type_hinted_expression(tree, q)
        elif isinstance(tree, ErrorFrom):
            self._visit_error_from(tree, q)
        elif isinstance(tree, MatchCase.Pattern):
            self._visit_match_case_pattern(tree, q)
        elif isinstance(tree, MatchCase):
            self._visit_match_case(tree, q)
        elif isinstance(tree, Slice):
            self._visit_slice(tree, q)
        elif isinstance(tree, J):
            # Delegate to Java visitor for Java types
            self._visit_java(tree, q)

    def _pre_visit(self, j: J, q: 'RpcSendQueue') -> None:
        """Handle common J fields: id, prefix, markers."""
        q.get_and_send(j, lambda x: x.id)
        q.get_and_send(j, lambda x: x.prefix, lambda space: self._visit_space(space, q))
        q.get_and_send(j, lambda x: x.markers, lambda markers: self._visit_markers(markers, q))

    def _visit_compilation_unit(self, cu: CompilationUnit, q: 'RpcSendQueue') -> None:
        """Visit CompilationUnit - only non-common fields."""
        q.get_and_send(cu, lambda x: str(x.source_path))
        q.get_and_send(cu, lambda x: x.charset_name)
        q.get_and_send(cu, lambda x: x.charset_bom_marked)
        q.get_and_send(cu, lambda x: x.checksum)
        q.get_and_send(cu, lambda x: x.file_attributes)
        q.get_and_send_list(cu, lambda x: x.padding.imports,
                           lambda stmt: stmt.element.id,
                           lambda stmt: self._visit_right_padded(stmt, q))
        q.get_and_send_list(cu, lambda x: x.padding.statements,
                           lambda stmt: stmt.element.id,
                           lambda stmt: self._visit_right_padded(stmt, q))
        q.get_and_send(cu, lambda x: x.eof, lambda space: self._visit_space(space, q))

    def _visit_async(self, async_: Async, q: 'RpcSendQueue') -> None:
        q.get_and_send(async_, lambda x: x.statement, lambda el: self._visit(el, q))

    def _visit_await(self, await_: Await, q: 'RpcSendQueue') -> None:
        q.get_and_send(await_, lambda x: x.expression, lambda el: self._visit(el, q))
        q.get_and_send(await_, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_binary(self, binary: Binary, q: 'RpcSendQueue') -> None:
        q.get_and_send(binary, lambda x: x.left, lambda el: self._visit(el, q))
        q.get_and_send(binary, lambda x: x.padding.operator, lambda el: self._visit_left_padded(el, q))
        q.get_and_send(binary, lambda x: x.negation, lambda space: self._visit_space(space, q))
        q.get_and_send(binary, lambda x: x.right, lambda el: self._visit(el, q))
        q.get_and_send(binary, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_chained_assignment(self, ca: ChainedAssignment, q: 'RpcSendQueue') -> None:
        q.get_and_send_list(ca, lambda x: x.padding.variables,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))
        q.get_and_send(ca, lambda x: x.assignment, lambda el: self._visit(el, q))
        q.get_and_send(ca, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_exception_type(self, et: ExceptionType, q: 'RpcSendQueue') -> None:
        q.get_and_send(et, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)
        q.get_and_send(et, lambda x: x.exception_group)
        q.get_and_send(et, lambda x: x.expression, lambda el: self._visit(el, q))

    def _visit_literal_type(self, lt: LiteralType, q: 'RpcSendQueue') -> None:
        q.get_and_send(lt, lambda x: x.literal, lambda el: self._visit(el, q))
        q.get_and_send(lt, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_type_hint(self, th: TypeHint, q: 'RpcSendQueue') -> None:
        q.get_and_send(th, lambda x: x.type_tree, lambda el: self._visit(el, q))
        q.get_and_send(th, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_expression_statement(self, es: ExpressionStatement, q: 'RpcSendQueue') -> None:
        q.get_and_send(es, lambda x: x.expression, lambda el: self._visit(el, q))

    def _visit_expression_type_tree(self, ett: ExpressionTypeTree, q: 'RpcSendQueue') -> None:
        q.get_and_send(ett, lambda x: x.reference, lambda el: self._visit(el, q))

    def _visit_statement_expression(self, se: StatementExpression, q: 'RpcSendQueue') -> None:
        q.get_and_send(se, lambda x: x.statement, lambda el: self._visit(el, q))

    def _visit_multi_import(self, mi: MultiImport, q: 'RpcSendQueue') -> None:
        q.get_and_send(mi, lambda x: x.padding.from_, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(mi, lambda x: x.parenthesized)
        q.get_and_send(mi, lambda x: x.padding.names, lambda el: self._visit_container(el, q))

    def _visit_key_value(self, kv: KeyValue, q: 'RpcSendQueue') -> None:
        q.get_and_send(kv, lambda x: x.padding.key, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(kv, lambda x: x.value, lambda el: self._visit(el, q))
        q.get_and_send(kv, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_dict_literal(self, dl: DictLiteral, q: 'RpcSendQueue') -> None:
        q.get_and_send(dl, lambda x: x.padding.elements, lambda el: self._visit_container(el, q))
        q.get_and_send(dl, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_collection_literal(self, cl: CollectionLiteral, q: 'RpcSendQueue') -> None:
        q.get_and_send(cl, lambda x: x.kind)
        q.get_and_send(cl, lambda x: x.padding.elements, lambda el: self._visit_container(el, q))
        q.get_and_send(cl, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_formatted_string(self, fs: FormattedString, q: 'RpcSendQueue') -> None:
        q.get_and_send(fs, lambda x: x.delimiter)
        q.get_and_send_list(fs, lambda x: x.parts,
                           lambda el: el.id,
                           lambda el: self._visit(el, q))
        q.get_and_send(fs, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_formatted_string_value(self, v: FormattedString.Value, q: 'RpcSendQueue') -> None:
        q.get_and_send(v, lambda x: x.padding.expression, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(v, lambda x: x.padding.debug, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(v, lambda x: x.conversion)
        q.get_and_send(v, lambda x: x.format, lambda el: self._visit(el, q))

    def _visit_pass(self, pass_: Pass, q: 'RpcSendQueue') -> None:
        # No additional fields beyond id/prefix/markers
        pass

    def _visit_trailing_else_wrapper(self, tew: TrailingElseWrapper, q: 'RpcSendQueue') -> None:
        q.get_and_send(tew, lambda x: x.statement, lambda el: self._visit(el, q))
        q.get_and_send(tew, lambda x: x.padding.else_block, lambda el: self._visit_left_padded(el, q))

    def _visit_comprehension_expression(self, ce: ComprehensionExpression, q: 'RpcSendQueue') -> None:
        q.get_and_send(ce, lambda x: x.kind)
        q.get_and_send(ce, lambda x: x.result, lambda el: self._visit(el, q))
        q.get_and_send_list(ce, lambda x: ce.clauses,
                           lambda el: el.id,
                           lambda el: self._visit(el, q))
        q.get_and_send(ce, lambda x: x.suffix, lambda space: self._visit_space(space, q))
        q.get_and_send(ce, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_comprehension_condition(self, cc: ComprehensionExpression.Condition, q: 'RpcSendQueue') -> None:
        q.get_and_send(cc, lambda x: x.expression, lambda el: self._visit(el, q))

    def _visit_comprehension_clause(self, cc: ComprehensionExpression.Clause, q: 'RpcSendQueue') -> None:
        q.get_and_send(cc, lambda x: x.padding.async_, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(cc, lambda x: x.iterator_variable, lambda el: self._visit(el, q))
        q.get_and_send(cc, lambda x: x.padding.iterated_list, lambda el: self._visit_left_padded(el, q))
        q.get_and_send_list(cc, lambda x: cc.conditions,
                           lambda el: el.id,
                           lambda el: self._visit(el, q))

    def _visit_type_alias(self, ta: TypeAlias, q: 'RpcSendQueue') -> None:
        q.get_and_send(ta, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send(ta, lambda x: x.padding.value, lambda el: self._visit_left_padded(el, q))
        q.get_and_send(ta, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_yield_from(self, yf: YieldFrom, q: 'RpcSendQueue') -> None:
        q.get_and_send(yf, lambda x: x.expression, lambda el: self._visit(el, q))
        q.get_and_send(yf, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_union_type(self, ut: UnionType, q: 'RpcSendQueue') -> None:
        q.get_and_send_list(ut, lambda x: x.padding.types,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))
        q.get_and_send(ut, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_variable_scope(self, vs: VariableScope, q: 'RpcSendQueue') -> None:
        q.get_and_send(vs, lambda x: x.kind)
        q.get_and_send_list(vs, lambda x: x.padding.names,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))

    def _visit_del(self, del_: Del, q: 'RpcSendQueue') -> None:
        q.get_and_send_list(del_, lambda x: x.padding.targets,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))

    def _visit_special_parameter(self, sp: SpecialParameter, q: 'RpcSendQueue') -> None:
        q.get_and_send(sp, lambda x: x.kind)
        q.get_and_send(sp, lambda x: x.type_hint, lambda el: self._visit(el, q))
        q.get_and_send(sp, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_star(self, star: Star, q: 'RpcSendQueue') -> None:
        q.get_and_send(star, lambda x: x.kind)
        q.get_and_send(star, lambda x: x.expression, lambda el: self._visit(el, q))
        q.get_and_send(star, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_named_argument(self, na: NamedArgument, q: 'RpcSendQueue') -> None:
        q.get_and_send(na, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send(na, lambda x: x.padding.value, lambda el: self._visit_left_padded(el, q))
        q.get_and_send(na, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_type_hinted_expression(self, the: TypeHintedExpression, q: 'RpcSendQueue') -> None:
        q.get_and_send(the, lambda x: x.expression, lambda el: self._visit(el, q))
        q.get_and_send(the, lambda x: x.type_hint, lambda el: self._visit(el, q))
        q.get_and_send(the, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_error_from(self, ef: ErrorFrom, q: 'RpcSendQueue') -> None:
        q.get_and_send(ef, lambda x: x.error, lambda el: self._visit(el, q))
        q.get_and_send(ef, lambda x: x.padding.from_, lambda el: self._visit_left_padded(el, q))
        q.get_and_send(ef, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_match_case(self, mc: MatchCase, q: 'RpcSendQueue') -> None:
        q.get_and_send(mc, lambda x: x.pattern, lambda el: self._visit(el, q))
        q.get_and_send(mc, lambda x: x.padding.guard, lambda el: self._visit_left_padded(el, q))
        q.get_and_send(mc, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_match_case_pattern(self, p: MatchCase.Pattern, q: 'RpcSendQueue') -> None:
        q.get_and_send(p, lambda x: x.kind)
        q.get_and_send(p, lambda x: x.padding.children, lambda el: self._visit_container(el, q))
        q.get_and_send(p, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_slice(self, slice_: Slice, q: 'RpcSendQueue') -> None:
        q.get_and_send(slice_, lambda x: x.padding.start, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(slice_, lambda x: x.padding.stop, lambda el: self._visit_right_padded(el, q))
        q.get_and_send(slice_, lambda x: x.padding.step, lambda el: self._visit_right_padded(el, q))
        # Python's Slice doesn't have a type field, but Java expects one (always null)
        q.get_and_send(slice_, lambda x: None)

    def _visit_java(self, j: J, q: 'RpcSendQueue') -> None:
        """Handle Java tree types that Python extends."""
        from rewrite.java.tree import (
            Identifier, Literal, MethodInvocation, FieldAccess,
            ArrayAccess, ArrayDimension, Block, If, Try, MethodDeclaration,
            ClassDeclaration, VariableDeclarations, Return, Assignment,
            AssignmentOperation, Unary, Ternary, Lambda, Empty, Throw,
            Assert, Break, Continue, WhileLoop, ForEachLoop, Switch, Case, Annotation, Import,
            Binary, Parentheses, ControlParentheses, NewArray, Modifier, Yield,
            ParameterizedType, TypeParameter, TypeParameters
        )

        # For Java types, we need to handle their specific fields
        if isinstance(j, Identifier):
            self._visit_identifier(j, q)
        elif isinstance(j, Literal):
            self._visit_literal(j, q)
        elif isinstance(j, Import):
            self._visit_import(j, q)
        elif isinstance(j, FieldAccess):
            self._visit_field_access(j, q)
        elif isinstance(j, Block):
            self._visit_block(j, q)
        elif isinstance(j, MethodInvocation):
            self._visit_method_invocation(j, q)
        elif isinstance(j, Unary):
            self._visit_j_unary(j, q)
        elif isinstance(j, Binary):
            self._visit_j_binary(j, q)
        elif isinstance(j, Assignment):
            self._visit_j_assignment(j, q)
        elif isinstance(j, AssignmentOperation):
            self._visit_j_assignment_operation(j, q)
        elif isinstance(j, Return):
            self._visit_j_return(j, q)
        elif isinstance(j, If):
            self._visit_j_if(j, q)
        elif isinstance(j, If.Else):
            self._visit_j_else(j, q)
        elif isinstance(j, WhileLoop):
            self._visit_j_while_loop(j, q)
        elif isinstance(j, ForEachLoop.Control):
            self._visit_j_for_each_control(j, q)
        elif isinstance(j, ForEachLoop):
            self._visit_j_for_each_loop(j, q)
        elif isinstance(j, Try):
            self._visit_j_try(j, q)
        elif isinstance(j, Try.Catch):
            self._visit_j_catch(j, q)
        elif isinstance(j, Try.Resource):
            self._visit_j_try_resource(j, q)
        elif isinstance(j, Throw):
            self._visit_j_throw(j, q)
        elif isinstance(j, Assert):
            self._visit_j_assert(j, q)
        elif isinstance(j, Break):
            self._visit_j_break(j, q)
        elif isinstance(j, Continue):
            self._visit_j_continue(j, q)
        elif isinstance(j, Empty):
            self._visit_j_empty(j, q)
        elif isinstance(j, Ternary):
            self._visit_j_ternary(j, q)
        elif isinstance(j, Lambda):
            self._visit_j_lambda(j, q)
        elif isinstance(j, Lambda.Parameters):
            self._visit_j_lambda_parameters(j, q)
        elif isinstance(j, VariableDeclarations):
            self._visit_j_variable_declarations(j, q)
        elif isinstance(j, VariableDeclarations.NamedVariable):
            self._visit_j_named_variable(j, q)
        elif isinstance(j, ClassDeclaration):
            self._visit_j_class_declaration(j, q)
        elif isinstance(j, ClassDeclaration.Kind):
            self._visit_j_class_declaration_kind(j, q)
        elif isinstance(j, MethodDeclaration):
            self._visit_j_method_declaration(j, q)
        elif isinstance(j, Switch):
            self._visit_j_switch(j, q)
        elif isinstance(j, Case):
            self._visit_j_case(j, q)
        elif isinstance(j, ArrayAccess):
            self._visit_j_array_access(j, q)
        elif isinstance(j, ArrayDimension):
            self._visit_j_array_dimension(j, q)
        elif isinstance(j, NewArray):
            self._visit_j_new_array(j, q)
        elif isinstance(j, Annotation):
            self._visit_j_annotation(j, q)
        elif isinstance(j, Parentheses):
            self._visit_j_parentheses(j, q)
        elif isinstance(j, ControlParentheses):
            self._visit_j_control_parentheses(j, q)
        elif isinstance(j, Modifier):
            self._visit_j_modifier(j, q)
        elif isinstance(j, Yield):
            self._visit_j_yield(j, q)
        elif isinstance(j, ParameterizedType):
            self._visit_j_parameterized_type(j, q)
        elif isinstance(j, TypeParameter):
            self._visit_j_type_parameter(j, q)
        elif isinstance(j, TypeParameters):
            self._visit_j_type_parameters(j, q)

    def _visit_identifier(self, ident, q: 'RpcSendQueue') -> None:
        # Java Identifier sends: annotations (list), simpleName, type (ref), fieldType (ref)
        q.get_and_send_list(ident, lambda x: x.annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send(ident, lambda x: x.simple_name)
        q.get_and_send(ident, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)
        q.get_and_send(ident, lambda x: x.field_type)

    def _visit_literal(self, lit, q: 'RpcSendQueue') -> None:
        q.get_and_send(lit, lambda x: x.value)
        q.get_and_send(lit, lambda x: x.value_source)
        q.get_and_send(lit, lambda x: x.unicode_escapes)
        q.get_and_send(lit, lambda x: x.type, lambda t: self._visit_type(t, q))

    def _visit_import(self, imp, q: 'RpcSendQueue') -> None:
        # Java Import fields: static (left-padded), qualid (tree), alias (left-padded)
        q.get_and_send(imp, lambda x: x.padding.static, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(imp, lambda x: x.qualid, lambda el: self._visit(el, q))
        q.get_and_send(imp, lambda x: x.padding.alias, lambda lp: self._visit_left_padded(lp, q))

    def _visit_field_access(self, fa, q: 'RpcSendQueue') -> None:
        # Java FieldAccess fields: target (tree), name (left-padded), type (ref)
        q.get_and_send(fa, lambda x: x.target, lambda el: self._visit(el, q))
        q.get_and_send(fa, lambda x: x.padding.name, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(fa, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_method_invocation(self, mi, q: 'RpcSendQueue') -> None:
        # Java MethodInvocation fields
        q.get_and_send(mi, lambda x: x.padding.select, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send(mi, lambda x: x.padding.type_parameters, lambda c: self._visit_container(c, q))
        q.get_and_send(mi, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send(mi, lambda x: x.padding.arguments, lambda c: self._visit_container(c, q))
        # method_type: serialize the JavaType.Method with all its fields
        q.get_and_send(mi, lambda x: x.method_type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_block(self, block, q: 'RpcSendQueue') -> None:
        # static is JRightPadded[bool], not just bool
        q.get_and_send(block, lambda x: x.padding.static, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send_list(block, lambda x: x.padding.statements,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))
        q.get_and_send(block, lambda x: x.end, lambda space: self._visit_space(space, q))

    # Additional Java types

    def _visit_j_unary(self, unary, q: 'RpcSendQueue') -> None:
        # Java Unary: operator (left-padded), expression, type
        q.get_and_send(unary, lambda x: x.padding.operator, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(unary, lambda x: x.expression, lambda el: self._visit(el, q))
        q.get_and_send(unary, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_binary(self, binary, q: 'RpcSendQueue') -> None:
        # Java Binary: left, operator (left-padded), right, type
        q.get_and_send(binary, lambda x: x.left, lambda el: self._visit(el, q))
        q.get_and_send(binary, lambda x: x.padding.operator, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(binary, lambda x: x.right, lambda el: self._visit(el, q))
        q.get_and_send(binary, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_assignment(self, assign, q: 'RpcSendQueue') -> None:
        # Java Assignment: variable, assignment (left-padded), type
        q.get_and_send(assign, lambda x: x.variable, lambda el: self._visit(el, q))
        q.get_and_send(assign, lambda x: x.padding.assignment, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(assign, lambda x: x.type, lambda t: self._visit_type(t, q))

    def _visit_j_assignment_operation(self, assign, q: 'RpcSendQueue') -> None:
        # Java AssignmentOperation: variable, operator (left-padded), assignment, type
        q.get_and_send(assign, lambda x: x.variable, lambda el: self._visit(el, q))
        q.get_and_send(assign, lambda x: x.padding.operator, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(assign, lambda x: x.assignment, lambda el: self._visit(el, q))
        q.get_and_send(assign, lambda x: x.type, lambda t: self._visit_type(t, q))

    def _visit_j_return(self, ret, q: 'RpcSendQueue') -> None:
        # Java Return: expression
        q.get_and_send(ret, lambda x: x.expression, lambda el: self._visit(el, q))

    def _visit_j_if(self, if_stmt, q: 'RpcSendQueue') -> None:
        # Java If: ifCondition, thenPart (right-padded), elsePart
        q.get_and_send(if_stmt, lambda x: x.if_condition, lambda el: self._visit(el, q))
        q.get_and_send(if_stmt, lambda x: x.padding.then_part, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send(if_stmt, lambda x: x.else_part, lambda el: self._visit(el, q))

    def _visit_j_else(self, else_stmt, q: 'RpcSendQueue') -> None:
        # Java If.Else: body (right-padded)
        q.get_and_send(else_stmt, lambda x: x.padding.body, lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_while_loop(self, while_loop, q: 'RpcSendQueue') -> None:
        # Java WhileLoop: condition, body (right-padded)
        q.get_and_send(while_loop, lambda x: x.condition, lambda el: self._visit(el, q))
        q.get_and_send(while_loop, lambda x: x.padding.body, lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_for_each_loop(self, for_each_loop, q: 'RpcSendQueue') -> None:
        # Java ForEachLoop: control, body (right-padded)
        q.get_and_send(for_each_loop, lambda x: x.control, lambda el: self._visit(el, q))
        q.get_and_send(for_each_loop, lambda x: x.padding.body, lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_for_each_control(self, control, q: 'RpcSendQueue') -> None:
        # Java ForEachLoop.Control: variable (right-padded), iterable (right-padded)
        q.get_and_send(control, lambda x: x.padding.variable, lambda rp: self._visit_right_padded(rp, q))
        q.get_and_send(control, lambda x: x.padding.iterable, lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_try(self, try_stmt, q: 'RpcSendQueue') -> None:
        # Java Try: resources (container), body, catches (list), finally (left-padded)
        q.get_and_send(try_stmt, lambda x: x.padding.resources if hasattr(x.padding, 'resources') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(try_stmt, lambda x: x.body, lambda el: self._visit(el, q))
        q.get_and_send_list(try_stmt, lambda x: x.catches,
                           lambda el: el.id,
                           lambda el: self._visit(el, q))
        q.get_and_send(try_stmt, lambda x: x.padding.finally_ if hasattr(x.padding, 'finally_') else None,
                      lambda lp: self._visit_left_padded(lp, q) if lp else None)

    def _visit_j_catch(self, catch, q: 'RpcSendQueue') -> None:
        # Java Try.Catch: parameter, body
        q.get_and_send(catch, lambda x: x.parameter, lambda el: self._visit(el, q))
        q.get_and_send(catch, lambda x: x.body, lambda el: self._visit(el, q))

    def _visit_j_try_resource(self, resource, q: 'RpcSendQueue') -> None:
        # Java Try.Resource: variableDeclarations, terminatedWithSemicolon
        q.get_and_send(resource, lambda x: x.variable_declarations, lambda el: self._visit(el, q))
        q.get_and_send(resource, lambda x: x.terminated_with_semicolon)

    def _visit_j_throw(self, throw, q: 'RpcSendQueue') -> None:
        # Java Throw: exception
        q.get_and_send(throw, lambda x: x.exception, lambda el: self._visit(el, q))

    def _visit_j_assert(self, assert_stmt, q: 'RpcSendQueue') -> None:
        # Java Assert: condition, detail (left-padded)
        q.get_and_send(assert_stmt, lambda x: x.condition, lambda el: self._visit(el, q))
        q.get_and_send(assert_stmt, lambda x: x.detail, lambda lp: self._visit_left_padded(lp, q))

    def _visit_j_break(self, break_stmt, q: 'RpcSendQueue') -> None:
        # Java Break: label
        q.get_and_send(break_stmt, lambda x: x.label, lambda el: self._visit(el, q))

    def _visit_j_continue(self, continue_stmt, q: 'RpcSendQueue') -> None:
        # Java Continue: label
        q.get_and_send(continue_stmt, lambda x: x.label, lambda el: self._visit(el, q))

    def _visit_j_empty(self, empty, q: 'RpcSendQueue') -> None:
        # Java Empty: no additional fields
        pass

    def _visit_j_ternary(self, ternary, q: 'RpcSendQueue') -> None:
        # Java Ternary: condition, truePart (left-padded), falsePart (left-padded), type
        q.get_and_send(ternary, lambda x: x.condition, lambda el: self._visit(el, q))
        q.get_and_send(ternary, lambda x: x.padding.true_part, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(ternary, lambda x: x.padding.false_part, lambda lp: self._visit_left_padded(lp, q))
        q.get_and_send(ternary, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_parameterized_type(self, param_type, q: 'RpcSendQueue') -> None:
        # Java ParameterizedType: clazz, typeParameters (container), type
        q.get_and_send(param_type, lambda x: x.clazz, lambda el: self._visit(el, q))
        q.get_and_send(param_type, lambda x: x.padding.type_parameters if hasattr(x.padding, 'type_parameters') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(param_type, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_lambda(self, lam, q: 'RpcSendQueue') -> None:
        # Java Lambda: params, arrow, body, type
        q.get_and_send(lam, lambda x: x.parameters, lambda el: self._visit(el, q))
        q.get_and_send(lam, lambda x: x.arrow, lambda space: self._visit_space(space, q))
        q.get_and_send(lam, lambda x: x.body, lambda el: self._visit(el, q))
        q.get_and_send(lam, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_lambda_parameters(self, params, q: 'RpcSendQueue') -> None:
        # Java Lambda.Parameters: parenthesized, parameters (list of right-padded)
        q.get_and_send(params, lambda x: x.parenthesized)
        q.get_and_send_list(params, lambda x: x.padding.parameters,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))

    def _visit_j_variable_declarations(self, var_decl, q: 'RpcSendQueue') -> None:
        # Java VariableDeclarations: leadingAnnotations, modifiers, typeExpression, varargs, variables
        q.get_and_send_list(var_decl, lambda x: x.leading_annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send_list(var_decl, lambda x: x.modifiers,
                           lambda m: m.id,
                           lambda m: self._visit(m, q))
        q.get_and_send(var_decl, lambda x: x.type_expression, lambda el: self._visit(el, q))
        q.get_and_send(var_decl, lambda x: x.varargs, lambda space: self._visit_space(space, q))
        q.get_and_send_list(var_decl, lambda x: x.padding.variables,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))

    def _visit_j_named_variable(self, var, q: 'RpcSendQueue') -> None:
        # Java VariableDeclarations.NamedVariable: declarator (name in Python), dimensionsAfterName, initializer (left-padded), variableType
        # Python has _name: Identifier directly, Java's getDeclarator() returns VariableDeclarator (Identifier|Literal)
        q.get_and_send(var, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send_list(var, lambda x: x.dimensions_after_name,
                           lambda el: str(el.element),
                           lambda el: self._visit_left_padded(el, q))
        q.get_and_send(var, lambda x: x.padding.initializer if hasattr(x.padding, 'initializer') else None,
                      lambda lp: self._visit_left_padded(lp, q) if lp else None)
        q.get_and_send(var, lambda x: x.variable_type)

    def _visit_j_class_declaration(self, class_decl, q: 'RpcSendQueue') -> None:
        # Java ClassDeclaration: leadingAnnotations, modifiers, kind, name, typeParameters, primaryConstructor, extends, implements, permits, body
        q.get_and_send_list(class_decl, lambda x: x.leading_annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send_list(class_decl, lambda x: x.modifiers,
                           lambda m: m.id,
                           lambda m: self._visit(m, q))
        q.get_and_send(class_decl, lambda x: x.padding.kind, lambda el: self._visit(el, q))
        q.get_and_send(class_decl, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send(class_decl, lambda x: x.padding.type_parameters if hasattr(x.padding, 'type_parameters') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(class_decl, lambda x: x.primary_constructor if hasattr(x, 'primary_constructor') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(class_decl, lambda x: x.padding.extends if hasattr(x.padding, 'extends') else None,
                      lambda lp: self._visit_left_padded(lp, q) if lp else None)
        q.get_and_send(class_decl, lambda x: x.padding.implements if hasattr(x.padding, 'implements') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(class_decl, lambda x: x.padding.permits if hasattr(x.padding, 'permits') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(class_decl, lambda x: x.body, lambda el: self._visit(el, q))

    def _visit_j_class_declaration_kind(self, kind, q: 'RpcSendQueue') -> None:
        # Java ClassDeclaration.Kind: preVisit IS automatically called by _visit before this method
        # Java sends: id, prefix, markers (via preVisit), then annotations (list), type (enum)
        q.get_and_send_list(kind, lambda x: x.annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send(kind, lambda x: x.type)  # This is J.ClassDeclaration.Kind.Type enum, not JavaType

    def _visit_j_method_declaration(self, method, q: 'RpcSendQueue') -> None:
        # Java MethodDeclaration: leadingAnnotations, modifiers, typeParameters, returnTypeExpression, name annotations, name, parameters, throws, body, defaultValue, methodType
        q.get_and_send_list(method, lambda x: x.leading_annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send_list(method, lambda x: x.modifiers,
                           lambda m: m.id,
                           lambda m: self._visit(m, q))
        q.get_and_send(method, lambda x: x.padding.type_parameters if hasattr(x.padding, 'type_parameters') else None,
                      lambda el: self._visit(el, q) if el else None)
        q.get_and_send(method, lambda x: x.return_type_expression, lambda el: self._visit(el, q))
        # Simplified model: nameAnnotations and name are separate fields (like TypeScript)
        q.get_and_send_list(method, lambda x: x.name_annotations if x.name_annotations else [],
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send(method, lambda x: x.name, lambda el: self._visit(el, q) if el else None)
        q.get_and_send(method, lambda x: x.padding.parameters,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(method, lambda x: x.padding.throws if hasattr(x.padding, 'throws') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(method, lambda x: x.body, lambda el: self._visit(el, q))
        q.get_and_send(method, lambda x: x.padding.default_value if hasattr(x.padding, 'default_value') else None,
                      lambda lp: self._visit_left_padded(lp, q) if lp else None)
        q.get_and_send(method, lambda x: x.method_type)

    def _visit_j_switch(self, switch, q: 'RpcSendQueue') -> None:
        # Java Switch: selector, cases (Block, not container!)
        q.get_and_send(switch, lambda x: x.selector, lambda el: self._visit(el, q))
        q.get_and_send(switch, lambda x: x.cases, lambda el: self._visit(el, q))

    def _visit_j_case(self, case, q: 'RpcSendQueue') -> None:
        # Java Case: type (enum), caseLabels (container), statements (container), body (right-padded), guard (expression)
        q.get_and_send(case, lambda x: x.type)  # This is J.Case.Type enum, not JavaType
        q.get_and_send(case, lambda x: x.padding.case_labels,
                      lambda c: self._visit_container(c, q))
        q.get_and_send(case, lambda x: x.padding.statements,
                      lambda c: self._visit_container(c, q))
        q.get_and_send(case, lambda x: x.padding.body,
                      lambda rp: self._visit_right_padded(rp, q) if rp else None)
        q.get_and_send(case, lambda x: x.guard, lambda el: self._visit(el, q) if el else None)

    def _visit_j_array_access(self, arr, q: 'RpcSendQueue') -> None:
        # Java ArrayAccess: indexed, dimension
        q.get_and_send(arr, lambda x: x.indexed, lambda el: self._visit(el, q))
        q.get_and_send(arr, lambda x: x.dimension, lambda el: self._visit(el, q))

    def _visit_j_array_dimension(self, dim, q: 'RpcSendQueue') -> None:
        # Java ArrayDimension: index (right-padded)
        q.get_and_send(dim, lambda x: x.padding.index, lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_new_array(self, new_arr, q: 'RpcSendQueue') -> None:
        # Java NewArray: typeExpression, dimensions, initializer (container), type
        q.get_and_send(new_arr, lambda x: x.type_expression, lambda el: self._visit(el, q))
        q.get_and_send_list(new_arr, lambda x: x.dimensions,
                           lambda d: id(d),
                           lambda d: self._visit(d, q))
        q.get_and_send(new_arr, lambda x: x.padding.initializer if hasattr(x.padding, 'initializer') else None,
                      lambda c: self._visit_container(c, q) if c else None)
        q.get_and_send(new_arr, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_annotation(self, annot, q: 'RpcSendQueue') -> None:
        # Java Annotation: annotationType, arguments (container)
        q.get_and_send(annot, lambda x: x.annotation_type, lambda el: self._visit(el, q))
        q.get_and_send(annot, lambda x: x.padding.arguments if hasattr(x.padding, 'arguments') else None,
                      lambda c: self._visit_container(c, q) if c else None)

    def _visit_j_parentheses(self, parens, q: 'RpcSendQueue') -> None:
        # Java Parentheses: tree (right-padded)
        q.get_and_send(parens, lambda x: x.padding.tree,
                      lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_control_parentheses(self, parens, q: 'RpcSendQueue') -> None:
        # Java ControlParentheses: tree (right-padded)
        q.get_and_send(parens, lambda x: x.padding.tree,
                      lambda rp: self._visit_right_padded(rp, q))

    def _visit_j_modifier(self, mod, q: 'RpcSendQueue') -> None:
        # Java Modifier: keyword, type (enum), annotations
        q.get_and_send(mod, lambda x: x.keyword)
        q.get_and_send(mod, lambda x: x.type)  # This is J.Modifier.Type enum, not JavaType
        q.get_and_send_list(mod, lambda x: x.annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))

    def _visit_j_yield(self, yield_stmt, q: 'RpcSendQueue') -> None:
        # Java Yield: implicit (bool), value (Expression)
        q.get_and_send(yield_stmt, lambda x: x.implicit)
        q.get_and_send(yield_stmt, lambda x: x.value, lambda el: self._visit(el, q))

    def _visit_j_parameterized_type(self, pt, q: 'RpcSendQueue') -> None:
        # Java ParameterizedType: clazz (NameTree), typeParameters (JContainer), type (JavaType ref)
        q.get_and_send(pt, lambda x: x.clazz, lambda el: self._visit(el, q))
        q.get_and_send(pt, lambda x: x.padding.type_parameters, lambda c: self._visit_container(c, q))
        q.get_and_send(pt, lambda x: x.type, lambda t: self._visit_type(t, q) if t else None)

    def _visit_j_type_parameter(self, tp, q: 'RpcSendQueue') -> None:
        # Java TypeParameter: annotations (list), modifiers (list), name (Expression), bounds (JContainer)
        q.get_and_send_list(tp, lambda x: x.annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send_list(tp, lambda x: x.modifiers,
                           lambda m: m.id,
                           lambda m: self._visit(m, q))
        q.get_and_send(tp, lambda x: x.name, lambda el: self._visit(el, q))
        q.get_and_send(tp, lambda x: x.padding.bounds if hasattr(x, 'padding') and hasattr(x.padding, 'bounds') else None,
                      lambda c: self._visit_container(c, q) if c else None)

    def _visit_j_type_parameters(self, tps, q: 'RpcSendQueue') -> None:
        # Java TypeParameters: annotations (list), typeParameters (list of JRightPadded)
        q.get_and_send_list(tps, lambda x: x.annotations,
                           lambda a: a.id,
                           lambda a: self._visit(a, q))
        q.get_and_send_list(tps, lambda x: x.padding.type_parameters,
                           lambda rp: rp.element.id,
                           lambda rp: self._visit_right_padded(rp, q))

    # Helper methods for Space, JRightPadded, JLeftPadded, JContainer

    def _visit_markers(self, markers: Markers, q: 'RpcSendQueue') -> None:
        """Visit a Markers object.

        Markers implements RpcCodec in Java and sends: id, then markers list (as ref).
        """
        if markers is None:
            return
        q.get_and_send(markers, lambda x: x.id)
        # Send markers list as ref - for now send as regular list
        # Java uses getAndSendListAsRef but we'll use regular list for simplicity
        q.get_and_send_list(markers, lambda x: x.markers,
                           lambda m: m.id,
                           None)  # No on_change - each marker is sent as-is

    def _visit_type(self, java_type, q: 'RpcSendQueue') -> None:
        """Visit a JavaType object (Primitive, Class, Method, Variable, etc.).

        This is called INSIDE a callback, so we just send the type's fields.
        The outer call already sent the ADD/CHANGE with valueType.
        """
        from rewrite.java.support_types import JavaType as JT

        if java_type is None:
            return

        if isinstance(java_type, JT.Primitive):
            # For Primitive types, send the keyword
            # JavaTypeSender.visitPrimitive sends: q.getAndSend(primitive, JavaType.Primitive::getKeyword)
            keyword_map = {
                JT.Primitive.Boolean: 'boolean',
                JT.Primitive.Byte: 'byte',
                JT.Primitive.Char: 'char',
                JT.Primitive.Double: 'double',
                JT.Primitive.Float: 'float',
                JT.Primitive.Int: 'int',
                JT.Primitive.Long: 'long',
                JT.Primitive.Short: 'short',
                JT.Primitive.Void: 'void',
                JT.Primitive.String: 'String',  # Java expects 'String' not 'java.lang.String'
                JT.Primitive.None_: '',  # Java expects empty string for None
                JT.Primitive.Null: 'null',
            }
            keyword = keyword_map.get(java_type, str(java_type.name).lower())
            # Send keyword as a primitive value field
            q.get_and_send(java_type, lambda x, kw=keyword: kw)

        elif isinstance(java_type, JT.Method):
            # Method: declaringType, name, flagsBitMap, returnType, parameterNames,
            #         parameterTypes, thrownExceptions, annotations, defaultValue, declaredFormalTypeNames
            q.get_and_send(java_type, lambda x: x._declaring_type, lambda t: self._visit_type(t, q))
            q.get_and_send(java_type, lambda x: x._name)
            q.get_and_send(java_type, lambda x: x._flags_bit_map)
            q.get_and_send(java_type, lambda x: x._return_type, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: x._parameter_names or [], lambda s: s, None)
            q.get_and_send_list(java_type, lambda x: x._parameter_types or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: x._thrown_exceptions or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: x._annotations or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: x._default_value or [], lambda s: s, None)
            q.get_and_send_list(java_type, lambda x: x._declared_formal_type_names or [], lambda s: s, None)

        elif isinstance(java_type, JT.Class):
            # Class: flagsBitMap, kind, fullyQualifiedName, typeParameters, supertype,
            #        owningClass, annotations, interfaces, members, methods
            q.get_and_send(java_type, lambda x: getattr(x, '_flags_bit_map', 0))
            q.get_and_send(java_type, lambda x: getattr(x, '_kind', JT.FullyQualified.Kind.Class))
            q.get_and_send(java_type, lambda x: getattr(x, '_fully_qualified_name', ''))
            q.get_and_send_list(java_type, lambda x: getattr(x, '_type_parameters', None) or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send(java_type, lambda x: getattr(x, '_supertype', None), lambda t: self._visit_type(t, q))
            q.get_and_send(java_type, lambda x: getattr(x, '_owning_class', None), lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: getattr(x, '_annotations', None) or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: getattr(x, '_interfaces', None) or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: getattr(x, '_members', None) or [], self._type_signature, lambda t: self._visit_type(t, q))
            q.get_and_send_list(java_type, lambda x: getattr(x, '_methods', None) or [], self._type_signature, lambda t: self._visit_type(t, q))

        elif isinstance(java_type, JT.Unknown):
            # Unknown has no additional fields
            pass

    def _type_signature(self, java_type) -> str:
        """Generate a signature string for a JavaType, used as list item identifier."""
        from rewrite.java.support_types import JavaType as JT

        if java_type is None:
            return ''
        if isinstance(java_type, JT.Primitive):
            return java_type.name
        if isinstance(java_type, JT.Class):
            return getattr(java_type, '_fully_qualified_name', str(id(java_type)))
        if isinstance(java_type, JT.Method):
            declaring = getattr(java_type, '_declaring_type', None)
            declaring_name = self._type_signature(declaring) if declaring else ''
            return f"{declaring_name}#{java_type._name}"
        return str(id(java_type))

    def _visit_space(self, space: Space, q: 'RpcSendQueue') -> None:
        """Visit a Space object.

        Order matters! Java sends: comments, then whitespace.
        """
        if space is None:
            return
        # Comments first (Java order), then whitespace
        q.get_and_send_list(space, lambda x: x.comments,
                           lambda c: c.text + c.suffix,  # Java uses text+suffix as id
                           lambda c: self._visit_comment(c, q))
        q.get_and_send(space, lambda x: x.whitespace)

    def _visit_comment(self, comment, q: 'RpcSendQueue') -> None:
        """Visit a Comment object (TextComment for Python)."""
        q.get_and_send(comment, lambda x: x.multiline)
        q.get_and_send(comment, lambda x: x.text)
        q.get_and_send(comment, lambda x: x.suffix)
        q.get_and_send(comment, lambda x: x.markers)

    def _visit_right_padded(self, rp: JRightPadded, q: 'RpcSendQueue') -> None:
        """Visit a JRightPadded wrapper."""
        if rp is None:
            return
        # Handle element based on its type (matching Java's visitRightPadded)
        el = rp.element
        if isinstance(el, J):
            q.get_and_send(rp, lambda x: x.element, lambda el: self._visit(el, q))
        elif isinstance(el, Space):
            q.get_and_send(rp, lambda x: x.element, lambda space: self._visit_space(space, q))
        else:
            # Primitives (bool, etc.) - send without callback
            q.get_and_send(rp, lambda x: x.element)
        q.get_and_send(rp, lambda x: x.after, lambda space: self._visit_space(space, q))
        q.get_and_send(rp, lambda x: x.markers)

    def _visit_left_padded(self, lp: JLeftPadded, q: 'RpcSendQueue') -> None:
        """Visit a JLeftPadded wrapper."""
        if lp is None:
            return
        q.get_and_send(lp, lambda x: x.before, lambda space: self._visit_space(space, q))
        # Handle element based on its type (matching Java's visitLeftPadded)
        el = lp.element
        if isinstance(el, J):
            q.get_and_send(lp, lambda x: x.element, lambda el: self._visit(el, q))
        elif isinstance(el, Space):
            q.get_and_send(lp, lambda x: x.element, lambda space: self._visit_space(space, q))
        else:
            # Primitives (enums, etc.) - send without callback
            q.get_and_send(lp, lambda x: x.element)
        q.get_and_send(lp, lambda x: x.markers)

    def _visit_container(self, container: JContainer, q: 'RpcSendQueue') -> None:
        """Visit a JContainer wrapper."""
        if container is None:
            return
        q.get_and_send(container, lambda x: x.before, lambda space: self._visit_space(space, q))
        # Use padding.elements to get JRightPadded list, not unwrapped elements
        q.get_and_send_list(container, lambda x: x.padding.elements,
                           lambda el: el.element.id,
                           lambda el: self._visit_right_padded(el, q))
        q.get_and_send(container, lambda x: x.markers)
