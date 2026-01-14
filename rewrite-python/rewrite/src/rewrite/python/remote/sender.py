from __future__ import annotations

from operator import attrgetter
from typing import Optional, cast

from rewrite_remote import Sender, SenderContext

from rewrite import Tree, Cursor
from rewrite.java import *
from rewrite.python import *
from . import extensions


class PythonSender(Sender):
    def send(self, after: T, before: Optional[T], ctx: SenderContext) -> None:
        visitor = self.Visitor()
        visitor.visit(after, ctx.fork(visitor, before))

    class Visitor(PythonVisitor[SenderContext]):
        def visit(self, tree: Optional[Tree], ctx: SenderContext, parent: Optional[Cursor] = None) -> Py:
            if parent is not None:
                self.cursor = parent

            self.cursor = Cursor(self.cursor, tree)
            ctx.send_node(tree, lambda x: x, ctx.send_tree)
            self.cursor = self.cursor.parent

            return cast(Py, tree)

        def visit_async(self, async_: Async, ctx: SenderContext) -> J:
            ctx.send_value(async_, attrgetter('_id'))
            ctx.send_node(async_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(async_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(async_, attrgetter('_statement'), ctx.send_tree)
            return async_

        def visit_await(self, await_: Await, ctx: SenderContext) -> J:
            ctx.send_value(await_, attrgetter('_id'))
            ctx.send_node(await_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(await_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(await_, attrgetter('_expression'), ctx.send_tree)
            ctx.send_typed_value(await_, attrgetter('_type'))
            return await_

        def visit_python_binary(self, binary: Binary, ctx: SenderContext) -> J:
            ctx.send_value(binary, attrgetter('_id'))
            ctx.send_node(binary, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(binary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(binary, attrgetter('_left'), ctx.send_tree)
            ctx.send_node(binary, attrgetter('_operator'), PythonSender.send_left_padded)
            ctx.send_node(binary, attrgetter('_negation'), PythonSender.send_space)
            ctx.send_node(binary, attrgetter('_right'), ctx.send_tree)
            ctx.send_typed_value(binary, attrgetter('_type'))
            return binary

        def visit_chained_assignment(self, chained_assignment: ChainedAssignment, ctx: SenderContext) -> J:
            ctx.send_value(chained_assignment, attrgetter('_id'))
            ctx.send_node(chained_assignment, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(chained_assignment, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(chained_assignment, attrgetter('_variables'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(chained_assignment, attrgetter('_assignment'), ctx.send_tree)
            ctx.send_typed_value(chained_assignment, attrgetter('_type'))
            return chained_assignment

        def visit_exception_type(self, exception_type: ExceptionType, ctx: SenderContext) -> J:
            ctx.send_value(exception_type, attrgetter('_id'))
            ctx.send_node(exception_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(exception_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_typed_value(exception_type, attrgetter('_type'))
            ctx.send_value(exception_type, attrgetter('_exception_group'))
            ctx.send_node(exception_type, attrgetter('_expression'), ctx.send_tree)
            return exception_type

        def visit_python_for_loop(self, for_loop: ForLoop, ctx: SenderContext) -> J:
            ctx.send_value(for_loop, attrgetter('_id'))
            ctx.send_node(for_loop, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(for_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(for_loop, attrgetter('_target'), ctx.send_tree)
            ctx.send_node(for_loop, attrgetter('_iterable'), PythonSender.send_left_padded)
            ctx.send_node(for_loop, attrgetter('_body'), PythonSender.send_right_padded)
            return for_loop

        def visit_literal_type(self, literal_type: LiteralType, ctx: SenderContext) -> J:
            ctx.send_value(literal_type, attrgetter('_id'))
            ctx.send_node(literal_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(literal_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(literal_type, attrgetter('_literal'), ctx.send_tree)
            ctx.send_typed_value(literal_type, attrgetter('_type'))
            return literal_type

        def visit_type_hint(self, type_hint: TypeHint, ctx: SenderContext) -> J:
            ctx.send_value(type_hint, attrgetter('_id'))
            ctx.send_node(type_hint, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_hint, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(type_hint, attrgetter('_type_tree'), ctx.send_tree)
            ctx.send_typed_value(type_hint, attrgetter('_type'))
            return type_hint

        def visit_compilation_unit(self, compilation_unit: CompilationUnit, ctx: SenderContext) -> J:
            ctx.send_value(compilation_unit, attrgetter('_id'))
            ctx.send_node(compilation_unit, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(compilation_unit, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(compilation_unit, attrgetter('_source_path'))
            ctx.send_value(compilation_unit, attrgetter('_charset_name'))
            ctx.send_value(compilation_unit, attrgetter('_charset_bom_marked'))
            ctx.send_typed_value(compilation_unit, attrgetter('_checksum'))
            ctx.send_typed_value(compilation_unit, attrgetter('_file_attributes'))
            ctx.send_nodes(compilation_unit, attrgetter('_imports'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_nodes(compilation_unit, attrgetter('_statements'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(compilation_unit, attrgetter('_eof'), PythonSender.send_space)
            return compilation_unit

        def visit_expression_statement(self, expression_statement: ExpressionStatement, ctx: SenderContext) -> J:
            ctx.send_value(expression_statement, attrgetter('_id'))
            ctx.send_node(expression_statement, attrgetter('_expression'), ctx.send_tree)
            return expression_statement

        def visit_expression_type_tree(self, expression_type_tree: ExpressionTypeTree, ctx: SenderContext) -> J:
            ctx.send_value(expression_type_tree, attrgetter('_id'))
            ctx.send_node(expression_type_tree, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(expression_type_tree, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(expression_type_tree, attrgetter('_reference'), ctx.send_tree)
            return expression_type_tree

        def visit_statement_expression(self, statement_expression: StatementExpression, ctx: SenderContext) -> J:
            ctx.send_value(statement_expression, attrgetter('_id'))
            ctx.send_node(statement_expression, attrgetter('_statement'), ctx.send_tree)
            return statement_expression

        def visit_multi_import(self, multi_import: MultiImport, ctx: SenderContext) -> J:
            ctx.send_value(multi_import, attrgetter('_id'))
            ctx.send_node(multi_import, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(multi_import, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(multi_import, attrgetter('_from'), PythonSender.send_right_padded)
            ctx.send_value(multi_import, attrgetter('_parenthesized'))
            ctx.send_node(multi_import, attrgetter('_names'), PythonSender.send_container)
            return multi_import

        def visit_key_value(self, key_value: KeyValue, ctx: SenderContext) -> J:
            ctx.send_value(key_value, attrgetter('_id'))
            ctx.send_node(key_value, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(key_value, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(key_value, attrgetter('_key'), PythonSender.send_right_padded)
            ctx.send_node(key_value, attrgetter('_value'), ctx.send_tree)
            ctx.send_typed_value(key_value, attrgetter('_type'))
            return key_value

        def visit_dict_literal(self, dict_literal: DictLiteral, ctx: SenderContext) -> J:
            ctx.send_value(dict_literal, attrgetter('_id'))
            ctx.send_node(dict_literal, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(dict_literal, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(dict_literal, attrgetter('_elements'), PythonSender.send_container)
            ctx.send_typed_value(dict_literal, attrgetter('_type'))
            return dict_literal

        def visit_collection_literal(self, collection_literal: CollectionLiteral, ctx: SenderContext) -> J:
            ctx.send_value(collection_literal, attrgetter('_id'))
            ctx.send_node(collection_literal, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(collection_literal, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(collection_literal, attrgetter('_kind'))
            ctx.send_node(collection_literal, attrgetter('_elements'), PythonSender.send_container)
            ctx.send_typed_value(collection_literal, attrgetter('_type'))
            return collection_literal

        def visit_formatted_string(self, formatted_string: FormattedString, ctx: SenderContext) -> J:
            ctx.send_value(formatted_string, attrgetter('_id'))
            ctx.send_node(formatted_string, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(formatted_string, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(formatted_string, attrgetter('_delimiter'))
            ctx.send_nodes(formatted_string, attrgetter('_parts'), ctx.send_tree, attrgetter('id'))
            return formatted_string

        def visit_formatted_string_value(self, value: FormattedString.Value, ctx: SenderContext) -> J:
            ctx.send_value(value, attrgetter('_id'))
            ctx.send_node(value, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(value, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(value, attrgetter('_expression'), PythonSender.send_right_padded)
            ctx.send_node(value, attrgetter('_debug'), PythonSender.send_right_padded)
            ctx.send_value(value, attrgetter('_conversion'))
            ctx.send_node(value, attrgetter('_format'), ctx.send_tree)
            return value

        def visit_pass(self, pass_: Pass, ctx: SenderContext) -> J:
            ctx.send_value(pass_, attrgetter('_id'))
            ctx.send_node(pass_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(pass_, attrgetter('_markers'), ctx.send_markers)
            return pass_

        def visit_trailing_else_wrapper(self, trailing_else_wrapper: TrailingElseWrapper, ctx: SenderContext) -> J:
            ctx.send_value(trailing_else_wrapper, attrgetter('_id'))
            ctx.send_node(trailing_else_wrapper, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(trailing_else_wrapper, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(trailing_else_wrapper, attrgetter('_statement'), ctx.send_tree)
            ctx.send_node(trailing_else_wrapper, attrgetter('_else_block'), PythonSender.send_left_padded)
            return trailing_else_wrapper

        def visit_comprehension_expression(self, comprehension_expression: ComprehensionExpression, ctx: SenderContext) -> J:
            ctx.send_value(comprehension_expression, attrgetter('_id'))
            ctx.send_node(comprehension_expression, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(comprehension_expression, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(comprehension_expression, attrgetter('_kind'))
            ctx.send_node(comprehension_expression, attrgetter('_result'), ctx.send_tree)
            ctx.send_nodes(comprehension_expression, attrgetter('_clauses'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(comprehension_expression, attrgetter('_suffix'), PythonSender.send_space)
            ctx.send_typed_value(comprehension_expression, attrgetter('_type'))
            return comprehension_expression

        def visit_comprehension_condition(self, condition: ComprehensionExpression.Condition, ctx: SenderContext) -> J:
            ctx.send_value(condition, attrgetter('_id'))
            ctx.send_node(condition, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(condition, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(condition, attrgetter('_expression'), ctx.send_tree)
            return condition

        def visit_comprehension_clause(self, clause: ComprehensionExpression.Clause, ctx: SenderContext) -> J:
            ctx.send_value(clause, attrgetter('_id'))
            ctx.send_node(clause, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(clause, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(clause, attrgetter('_async'), PythonSender.send_right_padded)
            ctx.send_node(clause, attrgetter('_iterator_variable'), ctx.send_tree)
            ctx.send_node(clause, attrgetter('_iterated_list'), PythonSender.send_left_padded)
            ctx.send_nodes(clause, attrgetter('_conditions'), ctx.send_tree, attrgetter('id'))
            return clause

        def visit_type_alias(self, type_alias: TypeAlias, ctx: SenderContext) -> J:
            ctx.send_value(type_alias, attrgetter('_id'))
            ctx.send_node(type_alias, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_alias, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(type_alias, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(type_alias, attrgetter('_value'), PythonSender.send_left_padded)
            ctx.send_typed_value(type_alias, attrgetter('_type'))
            return type_alias

        def visit_yield_from(self, yield_from: YieldFrom, ctx: SenderContext) -> J:
            ctx.send_value(yield_from, attrgetter('_id'))
            ctx.send_node(yield_from, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(yield_from, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(yield_from, attrgetter('_expression'), ctx.send_tree)
            ctx.send_typed_value(yield_from, attrgetter('_type'))
            return yield_from

        def visit_union_type(self, union_type: UnionType, ctx: SenderContext) -> J:
            ctx.send_value(union_type, attrgetter('_id'))
            ctx.send_node(union_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(union_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(union_type, attrgetter('_types'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_typed_value(union_type, attrgetter('_type'))
            return union_type

        def visit_variable_scope(self, variable_scope: VariableScope, ctx: SenderContext) -> J:
            ctx.send_value(variable_scope, attrgetter('_id'))
            ctx.send_node(variable_scope, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(variable_scope, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(variable_scope, attrgetter('_kind'))
            ctx.send_nodes(variable_scope, attrgetter('_names'), PythonSender.send_right_padded, lambda t: t.element.id)
            return variable_scope

        def visit_del(self, del_: Del, ctx: SenderContext) -> J:
            ctx.send_value(del_, attrgetter('_id'))
            ctx.send_node(del_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(del_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(del_, attrgetter('_targets'), PythonSender.send_right_padded, lambda t: t.element.id)
            return del_

        def visit_special_parameter(self, special_parameter: SpecialParameter, ctx: SenderContext) -> J:
            ctx.send_value(special_parameter, attrgetter('_id'))
            ctx.send_node(special_parameter, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(special_parameter, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(special_parameter, attrgetter('_kind'))
            ctx.send_node(special_parameter, attrgetter('_type_hint'), ctx.send_tree)
            ctx.send_typed_value(special_parameter, attrgetter('_type'))
            return special_parameter

        def visit_star(self, star: Star, ctx: SenderContext) -> J:
            ctx.send_value(star, attrgetter('_id'))
            ctx.send_node(star, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(star, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(star, attrgetter('_kind'))
            ctx.send_node(star, attrgetter('_expression'), ctx.send_tree)
            ctx.send_typed_value(star, attrgetter('_type'))
            return star

        def visit_named_argument(self, named_argument: NamedArgument, ctx: SenderContext) -> J:
            ctx.send_value(named_argument, attrgetter('_id'))
            ctx.send_node(named_argument, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(named_argument, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(named_argument, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(named_argument, attrgetter('_value'), PythonSender.send_left_padded)
            ctx.send_typed_value(named_argument, attrgetter('_type'))
            return named_argument

        def visit_type_hinted_expression(self, type_hinted_expression: TypeHintedExpression, ctx: SenderContext) -> J:
            ctx.send_value(type_hinted_expression, attrgetter('_id'))
            ctx.send_node(type_hinted_expression, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_hinted_expression, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(type_hinted_expression, attrgetter('_expression'), ctx.send_tree)
            ctx.send_node(type_hinted_expression, attrgetter('_type_hint'), ctx.send_tree)
            ctx.send_typed_value(type_hinted_expression, attrgetter('_type'))
            return type_hinted_expression

        def visit_error_from(self, error_from: ErrorFrom, ctx: SenderContext) -> J:
            ctx.send_value(error_from, attrgetter('_id'))
            ctx.send_node(error_from, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(error_from, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(error_from, attrgetter('_error'), ctx.send_tree)
            ctx.send_node(error_from, attrgetter('_from'), PythonSender.send_left_padded)
            ctx.send_typed_value(error_from, attrgetter('_type'))
            return error_from

        def visit_match_case(self, match_case: MatchCase, ctx: SenderContext) -> J:
            ctx.send_value(match_case, attrgetter('_id'))
            ctx.send_node(match_case, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(match_case, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(match_case, attrgetter('_pattern'), ctx.send_tree)
            ctx.send_node(match_case, attrgetter('_guard'), PythonSender.send_left_padded)
            ctx.send_typed_value(match_case, attrgetter('_type'))
            return match_case

        def visit_match_case_pattern(self, pattern: MatchCase.Pattern, ctx: SenderContext) -> J:
            ctx.send_value(pattern, attrgetter('_id'))
            ctx.send_node(pattern, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(pattern, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(pattern, attrgetter('_kind'))
            ctx.send_node(pattern, attrgetter('_children'), PythonSender.send_container)
            ctx.send_typed_value(pattern, attrgetter('_type'))
            return pattern

        def visit_slice(self, slice: Slice, ctx: SenderContext) -> J:
            ctx.send_value(slice, attrgetter('_id'))
            ctx.send_node(slice, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(slice, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(slice, attrgetter('_start'), PythonSender.send_right_padded)
            ctx.send_node(slice, attrgetter('_stop'), PythonSender.send_right_padded)
            ctx.send_node(slice, attrgetter('_step'), PythonSender.send_right_padded)
            return slice

        def visit_annotated_type(self, annotated_type: AnnotatedType, ctx: SenderContext) -> J:
            ctx.send_value(annotated_type, attrgetter('_id'))
            ctx.send_node(annotated_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(annotated_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(annotated_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(annotated_type, attrgetter('_type_expression'), ctx.send_tree)
            return annotated_type

        def visit_annotation(self, annotation: Annotation, ctx: SenderContext) -> J:
            ctx.send_value(annotation, attrgetter('_id'))
            ctx.send_node(annotation, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(annotation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(annotation, attrgetter('_annotation_type'), ctx.send_tree)
            ctx.send_node(annotation, attrgetter('_arguments'), PythonSender.send_container)
            return annotation

        def visit_array_access(self, array_access: ArrayAccess, ctx: SenderContext) -> J:
            ctx.send_value(array_access, attrgetter('_id'))
            ctx.send_node(array_access, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(array_access, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_access, attrgetter('_indexed'), ctx.send_tree)
            ctx.send_node(array_access, attrgetter('_dimension'), ctx.send_tree)
            ctx.send_typed_value(array_access, attrgetter('_type'))
            return array_access

        def visit_array_type(self, array_type: ArrayType, ctx: SenderContext) -> J:
            ctx.send_value(array_type, attrgetter('_id'))
            ctx.send_node(array_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(array_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_type, attrgetter('_element_type'), ctx.send_tree)
            ctx.send_nodes(array_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(array_type, attrgetter('_dimension'), PythonSender.send_left_padded)
            ctx.send_typed_value(array_type, attrgetter('_type'))
            return array_type

        def visit_assert(self, assert_: Assert, ctx: SenderContext) -> J:
            ctx.send_value(assert_, attrgetter('_id'))
            ctx.send_node(assert_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(assert_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assert_, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(assert_, attrgetter('_detail'), PythonSender.send_left_padded)
            return assert_

        def visit_assignment(self, assignment: Assignment, ctx: SenderContext) -> J:
            ctx.send_value(assignment, attrgetter('_id'))
            ctx.send_node(assignment, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(assignment, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assignment, attrgetter('_variable'), ctx.send_tree)
            ctx.send_node(assignment, attrgetter('_assignment'), PythonSender.send_left_padded)
            ctx.send_typed_value(assignment, attrgetter('_type'))
            return assignment

        def visit_assignment_operation(self, assignment_operation: AssignmentOperation, ctx: SenderContext) -> J:
            ctx.send_value(assignment_operation, attrgetter('_id'))
            ctx.send_node(assignment_operation, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(assignment_operation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assignment_operation, attrgetter('_variable'), ctx.send_tree)
            ctx.send_node(assignment_operation, attrgetter('_operator'), PythonSender.send_left_padded)
            ctx.send_node(assignment_operation, attrgetter('_assignment'), ctx.send_tree)
            ctx.send_typed_value(assignment_operation, attrgetter('_type'))
            return assignment_operation

        def visit_binary(self, binary: Binary, ctx: SenderContext) -> J:
            ctx.send_value(binary, attrgetter('_id'))
            ctx.send_node(binary, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(binary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(binary, attrgetter('_left'), ctx.send_tree)
            ctx.send_node(binary, attrgetter('_operator'), PythonSender.send_left_padded)
            ctx.send_node(binary, attrgetter('_right'), ctx.send_tree)
            ctx.send_typed_value(binary, attrgetter('_type'))
            return binary

        def visit_block(self, block: Block, ctx: SenderContext) -> J:
            ctx.send_value(block, attrgetter('_id'))
            ctx.send_node(block, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(block, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(block, attrgetter('_static'), PythonSender.send_right_padded)
            ctx.send_nodes(block, attrgetter('_statements'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(block, attrgetter('_end'), PythonSender.send_space)
            return block

        def visit_break(self, break_: Break, ctx: SenderContext) -> J:
            ctx.send_value(break_, attrgetter('_id'))
            ctx.send_node(break_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(break_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(break_, attrgetter('_label'), ctx.send_tree)
            return break_

        def visit_case(self, case: Case, ctx: SenderContext) -> J:
            ctx.send_value(case, attrgetter('_id'))
            ctx.send_node(case, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(case, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(case, attrgetter('_type'))
            ctx.send_node(case, attrgetter('_case_labels'), PythonSender.send_container)
            ctx.send_node(case, attrgetter('_statements'), PythonSender.send_container)
            ctx.send_node(case, attrgetter('_body'), PythonSender.send_right_padded)
            ctx.send_node(case, attrgetter('_guard'), ctx.send_tree)
            return case

        def visit_class_declaration(self, class_declaration: ClassDeclaration, ctx: SenderContext) -> J:
            ctx.send_value(class_declaration, attrgetter('_id'))
            ctx.send_node(class_declaration, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(class_declaration, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(class_declaration, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(class_declaration, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(class_declaration, attrgetter('_kind'), ctx.send_tree)
            ctx.send_node(class_declaration, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(class_declaration, attrgetter('_type_parameters'), PythonSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_primary_constructor'), PythonSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_extends'), PythonSender.send_left_padded)
            ctx.send_node(class_declaration, attrgetter('_implements'), PythonSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_permits'), PythonSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(class_declaration, attrgetter('_type'))
            return class_declaration

        def visit_class_declaration_kind(self, kind: ClassDeclaration.Kind, ctx: SenderContext) -> J:
            ctx.send_value(kind, attrgetter('_id'))
            ctx.send_node(kind, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(kind, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(kind, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_value(kind, attrgetter('_type'))
            return kind

        def visit_continue(self, continue_: Continue, ctx: SenderContext) -> J:
            ctx.send_value(continue_, attrgetter('_id'))
            ctx.send_node(continue_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(continue_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(continue_, attrgetter('_label'), ctx.send_tree)
            return continue_

        def visit_do_while_loop(self, do_while_loop: DoWhileLoop, ctx: SenderContext) -> J:
            ctx.send_value(do_while_loop, attrgetter('_id'))
            ctx.send_node(do_while_loop, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(do_while_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(do_while_loop, attrgetter('_body'), PythonSender.send_right_padded)
            ctx.send_node(do_while_loop, attrgetter('_while_condition'), PythonSender.send_left_padded)
            return do_while_loop

        def visit_empty(self, empty: Empty, ctx: SenderContext) -> J:
            ctx.send_value(empty, attrgetter('_id'))
            ctx.send_node(empty, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(empty, attrgetter('_markers'), ctx.send_markers)
            return empty

        def visit_enum_value(self, enum_value: EnumValue, ctx: SenderContext) -> J:
            ctx.send_value(enum_value, attrgetter('_id'))
            ctx.send_node(enum_value, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(enum_value, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(enum_value, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(enum_value, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(enum_value, attrgetter('_initializer'), ctx.send_tree)
            return enum_value

        def visit_enum_value_set(self, enum_value_set: EnumValueSet, ctx: SenderContext) -> J:
            ctx.send_value(enum_value_set, attrgetter('_id'))
            ctx.send_node(enum_value_set, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(enum_value_set, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(enum_value_set, attrgetter('_enums'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_value(enum_value_set, attrgetter('_terminated_with_semicolon'))
            return enum_value_set

        def visit_field_access(self, field_access: FieldAccess, ctx: SenderContext) -> J:
            ctx.send_value(field_access, attrgetter('_id'))
            ctx.send_node(field_access, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(field_access, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(field_access, attrgetter('_target'), ctx.send_tree)
            ctx.send_node(field_access, attrgetter('_name'), PythonSender.send_left_padded)
            ctx.send_typed_value(field_access, attrgetter('_type'))
            return field_access

        def visit_for_each_loop(self, for_each_loop: ForEachLoop, ctx: SenderContext) -> J:
            ctx.send_value(for_each_loop, attrgetter('_id'))
            ctx.send_node(for_each_loop, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(for_each_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(for_each_loop, attrgetter('_control'), ctx.send_tree)
            ctx.send_node(for_each_loop, attrgetter('_body'), PythonSender.send_right_padded)
            return for_each_loop

        def visit_for_each_control(self, control: ForEachLoop.Control, ctx: SenderContext) -> J:
            ctx.send_value(control, attrgetter('_id'))
            ctx.send_node(control, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(control, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(control, attrgetter('_variable'), PythonSender.send_right_padded)
            ctx.send_node(control, attrgetter('_iterable'), PythonSender.send_right_padded)
            return control

        def visit_for_loop(self, for_loop: ForLoop, ctx: SenderContext) -> J:
            ctx.send_value(for_loop, attrgetter('_id'))
            ctx.send_node(for_loop, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(for_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(for_loop, attrgetter('_control'), ctx.send_tree)
            ctx.send_node(for_loop, attrgetter('_body'), PythonSender.send_right_padded)
            return for_loop

        def visit_for_control(self, control: ForLoop.Control, ctx: SenderContext) -> J:
            ctx.send_value(control, attrgetter('_id'))
            ctx.send_node(control, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(control, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(control, attrgetter('_init'), PythonSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(control, attrgetter('_condition'), PythonSender.send_right_padded)
            ctx.send_nodes(control, attrgetter('_update'), PythonSender.send_right_padded, lambda t: t.element.id)
            return control

        def visit_parenthesized_type_tree(self, parenthesized_type_tree: ParenthesizedTypeTree, ctx: SenderContext) -> J:
            ctx.send_value(parenthesized_type_tree, attrgetter('_id'))
            ctx.send_node(parenthesized_type_tree, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(parenthesized_type_tree, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(parenthesized_type_tree, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(parenthesized_type_tree, attrgetter('_parenthesized_type'), ctx.send_tree)
            return parenthesized_type_tree

        def visit_identifier(self, identifier: Identifier, ctx: SenderContext) -> J:
            ctx.send_value(identifier, attrgetter('_id'))
            ctx.send_node(identifier, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(identifier, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(identifier, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_value(identifier, attrgetter('_simple_name'))
            ctx.send_typed_value(identifier, attrgetter('_type'))
            ctx.send_typed_value(identifier, attrgetter('_field_type'))
            return identifier

        def visit_if(self, if_: If, ctx: SenderContext) -> J:
            ctx.send_value(if_, attrgetter('_id'))
            ctx.send_node(if_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(if_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(if_, attrgetter('_if_condition'), ctx.send_tree)
            ctx.send_node(if_, attrgetter('_then_part'), PythonSender.send_right_padded)
            ctx.send_node(if_, attrgetter('_else_part'), ctx.send_tree)
            return if_

        def visit_else(self, else_: If.Else, ctx: SenderContext) -> J:
            ctx.send_value(else_, attrgetter('_id'))
            ctx.send_node(else_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(else_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(else_, attrgetter('_body'), PythonSender.send_right_padded)
            return else_

        def visit_import(self, import_: Import, ctx: SenderContext) -> J:
            ctx.send_value(import_, attrgetter('_id'))
            ctx.send_node(import_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(import_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(import_, attrgetter('_static'), PythonSender.send_left_padded)
            ctx.send_node(import_, attrgetter('_qualid'), ctx.send_tree)
            ctx.send_node(import_, attrgetter('_alias'), PythonSender.send_left_padded)
            return import_

        def visit_instance_of(self, instance_of: InstanceOf, ctx: SenderContext) -> J:
            ctx.send_value(instance_of, attrgetter('_id'))
            ctx.send_node(instance_of, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(instance_of, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(instance_of, attrgetter('_expression'), PythonSender.send_right_padded)
            ctx.send_node(instance_of, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(instance_of, attrgetter('_pattern'), ctx.send_tree)
            ctx.send_typed_value(instance_of, attrgetter('_type'))
            return instance_of

        def visit_deconstruction_pattern(self, deconstruction_pattern: DeconstructionPattern, ctx: SenderContext) -> J:
            ctx.send_value(deconstruction_pattern, attrgetter('_id'))
            ctx.send_node(deconstruction_pattern, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(deconstruction_pattern, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(deconstruction_pattern, attrgetter('_deconstructor'), ctx.send_tree)
            ctx.send_node(deconstruction_pattern, attrgetter('_nested'), PythonSender.send_container)
            ctx.send_typed_value(deconstruction_pattern, attrgetter('_type'))
            return deconstruction_pattern

        def visit_intersection_type(self, intersection_type: IntersectionType, ctx: SenderContext) -> J:
            ctx.send_value(intersection_type, attrgetter('_id'))
            ctx.send_node(intersection_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(intersection_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(intersection_type, attrgetter('_bounds'), PythonSender.send_container)
            return intersection_type

        def visit_label(self, label: Label, ctx: SenderContext) -> J:
            ctx.send_value(label, attrgetter('_id'))
            ctx.send_node(label, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(label, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(label, attrgetter('_label'), PythonSender.send_right_padded)
            ctx.send_node(label, attrgetter('_statement'), ctx.send_tree)
            return label

        def visit_lambda(self, lambda_: Lambda, ctx: SenderContext) -> J:
            ctx.send_value(lambda_, attrgetter('_id'))
            ctx.send_node(lambda_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(lambda_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(lambda_, attrgetter('_parameters'), ctx.send_tree)
            ctx.send_node(lambda_, attrgetter('_arrow'), PythonSender.send_space)
            ctx.send_node(lambda_, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(lambda_, attrgetter('_type'))
            return lambda_

        def visit_lambda_parameters(self, parameters: Lambda.Parameters, ctx: SenderContext) -> J:
            ctx.send_value(parameters, attrgetter('_id'))
            ctx.send_node(parameters, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(parameters, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(parameters, attrgetter('_parenthesized'))
            ctx.send_nodes(parameters, attrgetter('_parameters'), PythonSender.send_right_padded, lambda t: t.element.id)
            return parameters

        def visit_literal(self, literal: Literal, ctx: SenderContext) -> J:
            ctx.send_value(literal, attrgetter('_id'))
            ctx.send_node(literal, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(literal, attrgetter('_markers'), ctx.send_markers)
            ctx.send_typed_value(literal, attrgetter('_value'))
            ctx.send_value(literal, attrgetter('_value_source'))
            ctx.send_values(literal, attrgetter('_unicode_escapes'), lambda x: x)
            ctx.send_value(literal, attrgetter('_type'))
            return literal

        def visit_member_reference(self, member_reference: MemberReference, ctx: SenderContext) -> J:
            ctx.send_value(member_reference, attrgetter('_id'))
            ctx.send_node(member_reference, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(member_reference, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(member_reference, attrgetter('_containing'), PythonSender.send_right_padded)
            ctx.send_node(member_reference, attrgetter('_type_parameters'), PythonSender.send_container)
            ctx.send_node(member_reference, attrgetter('_reference'), PythonSender.send_left_padded)
            ctx.send_typed_value(member_reference, attrgetter('_type'))
            ctx.send_typed_value(member_reference, attrgetter('_method_type'))
            ctx.send_typed_value(member_reference, attrgetter('_variable_type'))
            return member_reference

        def visit_method_declaration(self, method_declaration: MethodDeclaration, ctx: SenderContext) -> J:
            ctx.send_value(method_declaration, attrgetter('_id'))
            ctx.send_node(method_declaration, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(method_declaration, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(method_declaration, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(method_declaration, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(method_declaration, attrgetter('_type_parameters'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_return_type_expression'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_name'), self.send_method_identifier_with_annotations)
            ctx.send_node(method_declaration, attrgetter('_parameters'), PythonSender.send_container)
            ctx.send_node(method_declaration, attrgetter('_throws'), PythonSender.send_container)
            ctx.send_node(method_declaration, attrgetter('_body'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_default_value'), PythonSender.send_left_padded)
            ctx.send_typed_value(method_declaration, attrgetter('_method_type'))
            return method_declaration

        def send_method_identifier_with_annotations(self, identifier_with_annotations: MethodDeclaration.IdentifierWithAnnotations, ctx: SenderContext) -> None:
            ctx.send_node(identifier_with_annotations, attrgetter('identifier'), ctx.send_tree)
            ctx.send_nodes(identifier_with_annotations, attrgetter('annotations'), ctx.send_tree, attrgetter('id'))

        def visit_method_invocation(self, method_invocation: MethodInvocation, ctx: SenderContext) -> J:
            ctx.send_value(method_invocation, attrgetter('_id'))
            ctx.send_node(method_invocation, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(method_invocation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(method_invocation, attrgetter('_select'), PythonSender.send_right_padded)
            ctx.send_node(method_invocation, attrgetter('_type_parameters'), PythonSender.send_container)
            ctx.send_node(method_invocation, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(method_invocation, attrgetter('_arguments'), PythonSender.send_container)
            ctx.send_typed_value(method_invocation, attrgetter('_method_type'))
            return method_invocation

        def visit_modifier(self, modifier: Modifier, ctx: SenderContext) -> J:
            ctx.send_value(modifier, attrgetter('_id'))
            ctx.send_node(modifier, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(modifier, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(modifier, attrgetter('_keyword'))
            ctx.send_value(modifier, attrgetter('_type'))
            ctx.send_nodes(modifier, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            return modifier

        def visit_multi_catch(self, multi_catch: MultiCatch, ctx: SenderContext) -> J:
            ctx.send_value(multi_catch, attrgetter('_id'))
            ctx.send_node(multi_catch, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(multi_catch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(multi_catch, attrgetter('_alternatives'), PythonSender.send_right_padded, lambda t: t.element.id)
            return multi_catch

        def visit_new_array(self, new_array: NewArray, ctx: SenderContext) -> J:
            ctx.send_value(new_array, attrgetter('_id'))
            ctx.send_node(new_array, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(new_array, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(new_array, attrgetter('_type_expression'), ctx.send_tree)
            ctx.send_nodes(new_array, attrgetter('_dimensions'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(new_array, attrgetter('_initializer'), PythonSender.send_container)
            ctx.send_typed_value(new_array, attrgetter('_type'))
            return new_array

        def visit_array_dimension(self, array_dimension: ArrayDimension, ctx: SenderContext) -> J:
            ctx.send_value(array_dimension, attrgetter('_id'))
            ctx.send_node(array_dimension, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(array_dimension, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_dimension, attrgetter('_index'), PythonSender.send_right_padded)
            return array_dimension

        def visit_new_class(self, new_class: NewClass, ctx: SenderContext) -> J:
            ctx.send_value(new_class, attrgetter('_id'))
            ctx.send_node(new_class, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(new_class, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(new_class, attrgetter('_enclosing'), PythonSender.send_right_padded)
            ctx.send_node(new_class, attrgetter('_new'), PythonSender.send_space)
            ctx.send_node(new_class, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(new_class, attrgetter('_arguments'), PythonSender.send_container)
            ctx.send_node(new_class, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(new_class, attrgetter('_constructor_type'))
            return new_class

        def visit_nullable_type(self, nullable_type: NullableType, ctx: SenderContext) -> J:
            ctx.send_value(nullable_type, attrgetter('_id'))
            ctx.send_node(nullable_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(nullable_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(nullable_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(nullable_type, attrgetter('_type_tree'), PythonSender.send_right_padded)
            return nullable_type

        def visit_package(self, package: Package, ctx: SenderContext) -> J:
            ctx.send_value(package, attrgetter('_id'))
            ctx.send_node(package, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(package, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(package, attrgetter('_expression'), ctx.send_tree)
            ctx.send_nodes(package, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            return package

        def visit_parameterized_type(self, parameterized_type: ParameterizedType, ctx: SenderContext) -> J:
            ctx.send_value(parameterized_type, attrgetter('_id'))
            ctx.send_node(parameterized_type, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(parameterized_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(parameterized_type, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(parameterized_type, attrgetter('_type_parameters'), PythonSender.send_container)
            ctx.send_typed_value(parameterized_type, attrgetter('_type'))
            return parameterized_type

        def visit_parentheses(self, parentheses: Parentheses[J2], ctx: SenderContext) -> J:
            ctx.send_value(parentheses, attrgetter('_id'))
            ctx.send_node(parentheses, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(parentheses, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(parentheses, attrgetter('_tree'), PythonSender.send_right_padded)
            return parentheses

        def visit_control_parentheses(self, control_parentheses: ControlParentheses[J2], ctx: SenderContext) -> J:
            ctx.send_value(control_parentheses, attrgetter('_id'))
            ctx.send_node(control_parentheses, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(control_parentheses, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(control_parentheses, attrgetter('_tree'), PythonSender.send_right_padded)
            return control_parentheses

        def visit_primitive(self, primitive: Primitive, ctx: SenderContext) -> J:
            ctx.send_value(primitive, attrgetter('_id'))
            ctx.send_node(primitive, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(primitive, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(primitive, attrgetter('_type'))
            return primitive

        def visit_return(self, return_: Return, ctx: SenderContext) -> J:
            ctx.send_value(return_, attrgetter('_id'))
            ctx.send_node(return_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(return_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(return_, attrgetter('_expression'), ctx.send_tree)
            return return_

        def visit_switch(self, switch: Switch, ctx: SenderContext) -> J:
            ctx.send_value(switch, attrgetter('_id'))
            ctx.send_node(switch, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(switch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(switch, attrgetter('_selector'), ctx.send_tree)
            ctx.send_node(switch, attrgetter('_cases'), ctx.send_tree)
            return switch

        def visit_switch_expression(self, switch_expression: SwitchExpression, ctx: SenderContext) -> J:
            ctx.send_value(switch_expression, attrgetter('_id'))
            ctx.send_node(switch_expression, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(switch_expression, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(switch_expression, attrgetter('_selector'), ctx.send_tree)
            ctx.send_node(switch_expression, attrgetter('_cases'), ctx.send_tree)
            ctx.send_typed_value(switch_expression, attrgetter('_type'))
            return switch_expression

        def visit_synchronized(self, synchronized: Synchronized, ctx: SenderContext) -> J:
            ctx.send_value(synchronized, attrgetter('_id'))
            ctx.send_node(synchronized, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(synchronized, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(synchronized, attrgetter('_lock'), ctx.send_tree)
            ctx.send_node(synchronized, attrgetter('_body'), ctx.send_tree)
            return synchronized

        def visit_ternary(self, ternary: Ternary, ctx: SenderContext) -> J:
            ctx.send_value(ternary, attrgetter('_id'))
            ctx.send_node(ternary, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(ternary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(ternary, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(ternary, attrgetter('_true_part'), PythonSender.send_left_padded)
            ctx.send_node(ternary, attrgetter('_false_part'), PythonSender.send_left_padded)
            ctx.send_typed_value(ternary, attrgetter('_type'))
            return ternary

        def visit_throw(self, throw: Throw, ctx: SenderContext) -> J:
            ctx.send_value(throw, attrgetter('_id'))
            ctx.send_node(throw, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(throw, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(throw, attrgetter('_exception'), ctx.send_tree)
            return throw

        def visit_try(self, try_: Try, ctx: SenderContext) -> J:
            ctx.send_value(try_, attrgetter('_id'))
            ctx.send_node(try_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(try_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(try_, attrgetter('_resources'), PythonSender.send_container)
            ctx.send_node(try_, attrgetter('_body'), ctx.send_tree)
            ctx.send_nodes(try_, attrgetter('_catches'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(try_, attrgetter('_finally'), PythonSender.send_left_padded)
            return try_

        def visit_try_resource(self, resource: Try.Resource, ctx: SenderContext) -> J:
            ctx.send_value(resource, attrgetter('_id'))
            ctx.send_node(resource, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(resource, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(resource, attrgetter('_variable_declarations'), ctx.send_tree)
            ctx.send_value(resource, attrgetter('_terminated_with_semicolon'))
            return resource

        def visit_catch(self, catch: Try.Catch, ctx: SenderContext) -> J:
            ctx.send_value(catch, attrgetter('_id'))
            ctx.send_node(catch, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(catch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(catch, attrgetter('_parameter'), ctx.send_tree)
            ctx.send_node(catch, attrgetter('_body'), ctx.send_tree)
            return catch

        def visit_type_cast(self, type_cast: TypeCast, ctx: SenderContext) -> J:
            ctx.send_value(type_cast, attrgetter('_id'))
            ctx.send_node(type_cast, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_cast, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(type_cast, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(type_cast, attrgetter('_expression'), ctx.send_tree)
            return type_cast

        def visit_type_parameter(self, type_parameter: TypeParameter, ctx: SenderContext) -> J:
            ctx.send_value(type_parameter, attrgetter('_id'))
            ctx.send_node(type_parameter, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_parameter, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(type_parameter, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(type_parameter, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(type_parameter, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(type_parameter, attrgetter('_bounds'), PythonSender.send_container)
            return type_parameter

        def visit_type_parameters(self, type_parameters: TypeParameters, ctx: SenderContext) -> J:
            ctx.send_value(type_parameters, attrgetter('_id'))
            ctx.send_node(type_parameters, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(type_parameters, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(type_parameters, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(type_parameters, attrgetter('_type_parameters'), PythonSender.send_right_padded, lambda t: t.element.id)
            return type_parameters

        def visit_unary(self, unary: Unary, ctx: SenderContext) -> J:
            ctx.send_value(unary, attrgetter('_id'))
            ctx.send_node(unary, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(unary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(unary, attrgetter('_operator'), PythonSender.send_left_padded)
            ctx.send_node(unary, attrgetter('_expression'), ctx.send_tree)
            ctx.send_typed_value(unary, attrgetter('_type'))
            return unary

        def visit_variable_declarations(self, variable_declarations: VariableDeclarations, ctx: SenderContext) -> J:
            ctx.send_value(variable_declarations, attrgetter('_id'))
            ctx.send_node(variable_declarations, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(variable_declarations, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(variable_declarations, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(variable_declarations, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(variable_declarations, attrgetter('_type_expression'), ctx.send_tree)
            ctx.send_node(variable_declarations, attrgetter('_varargs'), PythonSender.send_space)
            ctx.send_nodes(variable_declarations, attrgetter('_dimensions_before_name'), PythonSender.send_left_padded, lambda t: t)
            ctx.send_nodes(variable_declarations, attrgetter('_variables'), PythonSender.send_right_padded, lambda t: t.element.id)
            return variable_declarations

        def visit_variable(self, named_variable: VariableDeclarations.NamedVariable, ctx: SenderContext) -> J:
            ctx.send_value(named_variable, attrgetter('_id'))
            ctx.send_node(named_variable, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(named_variable, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(named_variable, attrgetter('_name'), ctx.send_tree)
            ctx.send_nodes(named_variable, attrgetter('_dimensions_after_name'), PythonSender.send_left_padded, lambda t: t)
            ctx.send_node(named_variable, attrgetter('_initializer'), PythonSender.send_left_padded)
            ctx.send_typed_value(named_variable, attrgetter('_variable_type'))
            return named_variable

        def visit_while_loop(self, while_loop: WhileLoop, ctx: SenderContext) -> J:
            ctx.send_value(while_loop, attrgetter('_id'))
            ctx.send_node(while_loop, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(while_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(while_loop, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(while_loop, attrgetter('_body'), PythonSender.send_right_padded)
            return while_loop

        def visit_wildcard(self, wildcard: Wildcard, ctx: SenderContext) -> J:
            ctx.send_value(wildcard, attrgetter('_id'))
            ctx.send_node(wildcard, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(wildcard, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(wildcard, attrgetter('_bound'), PythonSender.send_left_padded)
            ctx.send_node(wildcard, attrgetter('_bounded_type'), ctx.send_tree)
            return wildcard

        def visit_yield(self, yield_: Yield, ctx: SenderContext) -> J:
            ctx.send_value(yield_, attrgetter('_id'))
            ctx.send_node(yield_, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(yield_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(yield_, attrgetter('_implicit'))
            ctx.send_node(yield_, attrgetter('_value'), ctx.send_tree)
            return yield_

        def visit_unknown(self, unknown: Unknown, ctx: SenderContext) -> J:
            ctx.send_value(unknown, attrgetter('_id'))
            ctx.send_node(unknown, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(unknown, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(unknown, attrgetter('_source'), ctx.send_tree)
            return unknown

        def visit_unknown_source(self, source: Unknown.Source, ctx: SenderContext) -> J:
            ctx.send_value(source, attrgetter('_id'))
            ctx.send_node(source, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(source, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(source, attrgetter('_text'))
            return source

        def visit_erroneous(self, erroneous: Erroneous, ctx: SenderContext) -> J:
            ctx.send_value(erroneous, attrgetter('_id'))
            ctx.send_node(erroneous, attrgetter('_prefix'), PythonSender.send_space)
            ctx.send_node(erroneous, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(erroneous, attrgetter('_text'))
            return erroneous

    @classmethod
    def send_container(cls, container: JContainer[T], ctx: SenderContext):
        extensions.send_container(container, ctx)

    @classmethod
    def send_left_padded(cls, left_padded: JLeftPadded[T], ctx: SenderContext):
        extensions.send_left_padded(left_padded, ctx)

    @classmethod
    def send_right_padded(cls, right_padded: JRightPadded[T], ctx: SenderContext):
        extensions.send_right_padded(right_padded, ctx)

    @classmethod
    def send_space(cls, space: Space, ctx: SenderContext):
        extensions.send_space(space, ctx)
