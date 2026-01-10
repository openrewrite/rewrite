from pathlib import Path
from typing import Optional, Any, cast, Type, Callable
from uuid import UUID

from rewrite_remote import Receiver, ReceiverContext, T, ReceiverFactory

from rewrite import Cursor, Checksum, FileAttributes, Tree
from rewrite.java import *
from rewrite.python import *
from rewrite.python.visitor import PythonVisitor
from . import extensions


class PythonReceiver(Receiver):
    def fork(self, ctx: ReceiverContext) -> ReceiverContext:
        return ctx.fork(self.Visitor(), self.Factory())

    def receive(self, before: Optional[T], ctx: ReceiverContext) -> Any:
        forked = self.fork(ctx)
        return forked.visitor.visit(before, forked)

    # noinspection DuplicatedCode,PyMethodFirstArgAssignment,PyTypeChecker
    class Visitor(PythonVisitor[ReceiverContext]):
        def visit(self, tree: Optional[Tree], ctx: ReceiverContext, parent: Optional[Cursor] = None) -> Optional[J]:
            if parent is not None:
                self.cursor = parent

            self.cursor = Cursor(self.cursor, tree)

            tree = ctx.receive_node(cast(J, tree), ctx.receive_tree)

            self.cursor = self.cursor.parent
            return cast(J, tree)

        def visit_async(self, async_: Async, ctx: ReceiverContext) -> J:
            async_ = async_.with_id(ctx.receive_value(async_.id, UUID))
            async_ = async_.with_prefix(ctx.receive_node(async_.prefix, PythonReceiver.receive_space))
            async_ = async_.with_markers(ctx.receive_node(async_.markers, ctx.receive_markers))
            async_ = async_.with_statement(ctx.receive_node(async_.statement, ctx.receive_tree))
            return async_

        def visit_await(self, await_: Await, ctx: ReceiverContext) -> J:
            await_ = await_.with_id(ctx.receive_value(await_.id, UUID))
            await_ = await_.with_prefix(ctx.receive_node(await_.prefix, PythonReceiver.receive_space))
            await_ = await_.with_markers(ctx.receive_node(await_.markers, ctx.receive_markers))
            await_ = await_.with_expression(ctx.receive_node(await_.expression, ctx.receive_tree))
            await_ = await_.with_type(ctx.receive_value(await_.type, JavaType))
            return await_

        def visit_python_binary(self, binary: Binary, ctx: ReceiverContext) -> J:
            binary = binary.with_id(ctx.receive_value(binary.id, UUID))
            binary = binary.with_prefix(ctx.receive_node(binary.prefix, PythonReceiver.receive_space))
            binary = binary.with_markers(ctx.receive_node(binary.markers, ctx.receive_markers))
            binary = binary.with_left(ctx.receive_node(binary.left, ctx.receive_tree))
            binary = binary.padding.with_operator(ctx.receive_node(binary.padding.operator, PythonReceiver.left_padded_value_receiver(Binary.Type)))
            binary = binary.with_negation(ctx.receive_node(binary.negation, PythonReceiver.receive_space))
            binary = binary.with_right(ctx.receive_node(binary.right, ctx.receive_tree))
            binary = binary.with_type(ctx.receive_value(binary.type, JavaType))
            return binary

        def visit_chained_assignment(self, chained_assignment: ChainedAssignment, ctx: ReceiverContext) -> J:
            chained_assignment = chained_assignment.with_id(ctx.receive_value(chained_assignment.id, UUID))
            chained_assignment = chained_assignment.with_prefix(ctx.receive_node(chained_assignment.prefix, PythonReceiver.receive_space))
            chained_assignment = chained_assignment.with_markers(ctx.receive_node(chained_assignment.markers, ctx.receive_markers))
            chained_assignment = chained_assignment.padding.with_variables(ctx.receive_nodes(chained_assignment.padding.variables, PythonReceiver.receive_right_padded_tree))
            chained_assignment = chained_assignment.with_assignment(ctx.receive_node(chained_assignment.assignment, ctx.receive_tree))
            chained_assignment = chained_assignment.with_type(ctx.receive_value(chained_assignment.type, JavaType))
            return chained_assignment

        def visit_exception_type(self, exception_type: ExceptionType, ctx: ReceiverContext) -> J:
            exception_type = exception_type.with_id(ctx.receive_value(exception_type.id, UUID))
            exception_type = exception_type.with_prefix(ctx.receive_node(exception_type.prefix, PythonReceiver.receive_space))
            exception_type = exception_type.with_markers(ctx.receive_node(exception_type.markers, ctx.receive_markers))
            exception_type = exception_type.with_type(ctx.receive_value(exception_type.type, JavaType))
            exception_type = exception_type.with_exception_group(ctx.receive_value(exception_type.exception_group, bool))
            exception_type = exception_type.with_expression(ctx.receive_node(exception_type.expression, ctx.receive_tree))
            return exception_type

        def visit_python_for_loop(self, for_loop: ForLoop, ctx: ReceiverContext) -> J:
            for_loop = for_loop.with_id(ctx.receive_value(for_loop.id, UUID))
            for_loop = for_loop.with_prefix(ctx.receive_node(for_loop.prefix, PythonReceiver.receive_space))
            for_loop = for_loop.with_markers(ctx.receive_node(for_loop.markers, ctx.receive_markers))
            for_loop = for_loop.with_target(ctx.receive_node(for_loop.target, ctx.receive_tree))
            for_loop = for_loop.padding.with_iterable(ctx.receive_node(for_loop.padding.iterable, PythonReceiver.receive_left_padded_tree))
            for_loop = for_loop.padding.with_body(ctx.receive_node(for_loop.padding.body, PythonReceiver.receive_right_padded_tree))
            return for_loop

        def visit_literal_type(self, literal_type: LiteralType, ctx: ReceiverContext) -> J:
            literal_type = literal_type.with_id(ctx.receive_value(literal_type.id, UUID))
            literal_type = literal_type.with_prefix(ctx.receive_node(literal_type.prefix, PythonReceiver.receive_space))
            literal_type = literal_type.with_markers(ctx.receive_node(literal_type.markers, ctx.receive_markers))
            literal_type = literal_type.with_literal(ctx.receive_node(literal_type.literal, ctx.receive_tree))
            literal_type = literal_type.with_type(ctx.receive_value(literal_type.type, JavaType))
            return literal_type

        def visit_type_hint(self, type_hint: TypeHint, ctx: ReceiverContext) -> J:
            type_hint = type_hint.with_id(ctx.receive_value(type_hint.id, UUID))
            type_hint = type_hint.with_prefix(ctx.receive_node(type_hint.prefix, PythonReceiver.receive_space))
            type_hint = type_hint.with_markers(ctx.receive_node(type_hint.markers, ctx.receive_markers))
            type_hint = type_hint.with_type_tree(ctx.receive_node(type_hint.type_tree, ctx.receive_tree))
            type_hint = type_hint.with_type(ctx.receive_value(type_hint.type, JavaType))
            return type_hint

        def visit_compilation_unit(self, compilation_unit: CompilationUnit, ctx: ReceiverContext) -> J:
            compilation_unit = compilation_unit.with_id(ctx.receive_value(compilation_unit.id, UUID))
            compilation_unit = compilation_unit.with_prefix(ctx.receive_node(compilation_unit.prefix, PythonReceiver.receive_space))
            compilation_unit = compilation_unit.with_markers(ctx.receive_node(compilation_unit.markers, ctx.receive_markers))
            compilation_unit = compilation_unit.with_source_path(ctx.receive_value(compilation_unit.source_path, Path))
            compilation_unit = compilation_unit.with_file_attributes(ctx.receive_value(compilation_unit.file_attributes, FileAttributes))
            compilation_unit = compilation_unit.with_charset_name(ctx.receive_value(compilation_unit.charset_name, str))
            compilation_unit = compilation_unit.with_charset_bom_marked(ctx.receive_value(compilation_unit.charset_bom_marked, bool))
            compilation_unit = compilation_unit.with_checksum(ctx.receive_value(compilation_unit.checksum, Checksum))
            compilation_unit = compilation_unit.padding.with_imports(ctx.receive_nodes(compilation_unit.padding.imports, PythonReceiver.receive_right_padded_tree))
            compilation_unit = compilation_unit.padding.with_statements(ctx.receive_nodes(compilation_unit.padding.statements, PythonReceiver.receive_right_padded_tree))
            compilation_unit = compilation_unit.with_eof(ctx.receive_node(compilation_unit.eof, PythonReceiver.receive_space))
            return compilation_unit

        def visit_expression_statement(self, expression_statement: ExpressionStatement, ctx: ReceiverContext) -> J:
            expression_statement = expression_statement.with_id(ctx.receive_value(expression_statement.id, UUID))
            expression_statement = expression_statement.with_expression(ctx.receive_node(expression_statement.expression, ctx.receive_tree))
            return expression_statement

        def visit_expression_type_tree(self, expression_type_tree: ExpressionTypeTree, ctx: ReceiverContext) -> J:
            expression_type_tree = expression_type_tree.with_id(ctx.receive_value(expression_type_tree.id, UUID))
            expression_type_tree = expression_type_tree.with_prefix(ctx.receive_node(expression_type_tree.prefix, PythonReceiver.receive_space))
            expression_type_tree = expression_type_tree.with_markers(ctx.receive_node(expression_type_tree.markers, ctx.receive_markers))
            expression_type_tree = expression_type_tree.with_reference(ctx.receive_node(expression_type_tree.reference, ctx.receive_tree))
            return expression_type_tree

        def visit_statement_expression(self, statement_expression: StatementExpression, ctx: ReceiverContext) -> J:
            statement_expression = statement_expression.with_id(ctx.receive_value(statement_expression.id, UUID))
            statement_expression = statement_expression.with_statement(ctx.receive_node(statement_expression.statement, ctx.receive_tree))
            return statement_expression

        def visit_multi_import(self, multi_import: MultiImport, ctx: ReceiverContext) -> J:
            multi_import = multi_import.with_id(ctx.receive_value(multi_import.id, UUID))
            multi_import = multi_import.with_prefix(ctx.receive_node(multi_import.prefix, PythonReceiver.receive_space))
            multi_import = multi_import.with_markers(ctx.receive_node(multi_import.markers, ctx.receive_markers))
            multi_import = multi_import.padding.with_from(ctx.receive_node(multi_import.padding.from_, PythonReceiver.receive_right_padded_tree))
            multi_import = multi_import.with_parenthesized(ctx.receive_value(multi_import.parenthesized, bool))
            multi_import = multi_import.padding.with_names(ctx.receive_node(multi_import.padding.names, PythonReceiver.receive_container))
            return multi_import

        def visit_key_value(self, key_value: KeyValue, ctx: ReceiverContext) -> J:
            key_value = key_value.with_id(ctx.receive_value(key_value.id, UUID))
            key_value = key_value.with_prefix(ctx.receive_node(key_value.prefix, PythonReceiver.receive_space))
            key_value = key_value.with_markers(ctx.receive_node(key_value.markers, ctx.receive_markers))
            key_value = key_value.padding.with_key(ctx.receive_node(key_value.padding.key, PythonReceiver.receive_right_padded_tree))
            key_value = key_value.with_value(ctx.receive_node(key_value.value, ctx.receive_tree))
            key_value = key_value.with_type(ctx.receive_value(key_value.type, JavaType))
            return key_value

        def visit_dict_literal(self, dict_literal: DictLiteral, ctx: ReceiverContext) -> J:
            dict_literal = dict_literal.with_id(ctx.receive_value(dict_literal.id, UUID))
            dict_literal = dict_literal.with_prefix(ctx.receive_node(dict_literal.prefix, PythonReceiver.receive_space))
            dict_literal = dict_literal.with_markers(ctx.receive_node(dict_literal.markers, ctx.receive_markers))
            dict_literal = dict_literal.padding.with_elements(ctx.receive_node(dict_literal.padding.elements, PythonReceiver.receive_container))
            dict_literal = dict_literal.with_type(ctx.receive_value(dict_literal.type, JavaType))
            return dict_literal

        def visit_collection_literal(self, collection_literal: CollectionLiteral, ctx: ReceiverContext) -> J:
            collection_literal = collection_literal.with_id(ctx.receive_value(collection_literal.id, UUID))
            collection_literal = collection_literal.with_prefix(ctx.receive_node(collection_literal.prefix, PythonReceiver.receive_space))
            collection_literal = collection_literal.with_markers(ctx.receive_node(collection_literal.markers, ctx.receive_markers))
            collection_literal = collection_literal.with_kind(ctx.receive_value(collection_literal.kind, CollectionLiteral.Kind))
            collection_literal = collection_literal.padding.with_elements(ctx.receive_node(collection_literal.padding.elements, PythonReceiver.receive_container))
            collection_literal = collection_literal.with_type(ctx.receive_value(collection_literal.type, JavaType))
            return collection_literal

        def visit_formatted_string(self, formatted_string: FormattedString, ctx: ReceiverContext) -> J:
            formatted_string = formatted_string.with_id(ctx.receive_value(formatted_string.id, UUID))
            formatted_string = formatted_string.with_prefix(ctx.receive_node(formatted_string.prefix, PythonReceiver.receive_space))
            formatted_string = formatted_string.with_markers(ctx.receive_node(formatted_string.markers, ctx.receive_markers))
            formatted_string = formatted_string.with_delimiter(ctx.receive_value(formatted_string.delimiter, str))
            formatted_string = formatted_string.with_parts(ctx.receive_nodes(formatted_string.parts, ctx.receive_tree))
            return formatted_string

        def visit_formatted_string_value(self, value: FormattedString.Value, ctx: ReceiverContext) -> J:
            value = value.with_id(ctx.receive_value(value.id, UUID))
            value = value.with_prefix(ctx.receive_node(value.prefix, PythonReceiver.receive_space))
            value = value.with_markers(ctx.receive_node(value.markers, ctx.receive_markers))
            value = value.padding.with_expression(ctx.receive_node(value.padding.expression, PythonReceiver.receive_right_padded_tree))
            value = value.padding.with_debug(ctx.receive_node(value.padding.debug, PythonReceiver.right_padded_value_receiver(bool)))
            value = value.with_conversion(ctx.receive_value(value.conversion, FormattedString.Value.Conversion))
            value = value.with_format(ctx.receive_node(value.format, ctx.receive_tree))
            return value

        def visit_pass(self, pass_: Pass, ctx: ReceiverContext) -> J:
            pass_ = pass_.with_id(ctx.receive_value(pass_.id, UUID))
            pass_ = pass_.with_prefix(ctx.receive_node(pass_.prefix, PythonReceiver.receive_space))
            pass_ = pass_.with_markers(ctx.receive_node(pass_.markers, ctx.receive_markers))
            return pass_

        def visit_trailing_else_wrapper(self, trailing_else_wrapper: TrailingElseWrapper, ctx: ReceiverContext) -> J:
            trailing_else_wrapper = trailing_else_wrapper.with_id(ctx.receive_value(trailing_else_wrapper.id, UUID))
            trailing_else_wrapper = trailing_else_wrapper.with_prefix(ctx.receive_node(trailing_else_wrapper.prefix, PythonReceiver.receive_space))
            trailing_else_wrapper = trailing_else_wrapper.with_markers(ctx.receive_node(trailing_else_wrapper.markers, ctx.receive_markers))
            trailing_else_wrapper = trailing_else_wrapper.with_statement(ctx.receive_node(trailing_else_wrapper.statement, ctx.receive_tree))
            trailing_else_wrapper = trailing_else_wrapper.padding.with_else_block(ctx.receive_node(trailing_else_wrapper.padding.else_block, PythonReceiver.receive_left_padded_tree))
            return trailing_else_wrapper

        def visit_comprehension_expression(self, comprehension_expression: ComprehensionExpression, ctx: ReceiverContext) -> J:
            comprehension_expression = comprehension_expression.with_id(ctx.receive_value(comprehension_expression.id, UUID))
            comprehension_expression = comprehension_expression.with_prefix(ctx.receive_node(comprehension_expression.prefix, PythonReceiver.receive_space))
            comprehension_expression = comprehension_expression.with_markers(ctx.receive_node(comprehension_expression.markers, ctx.receive_markers))
            comprehension_expression = comprehension_expression.with_kind(ctx.receive_value(comprehension_expression.kind, ComprehensionExpression.Kind))
            comprehension_expression = comprehension_expression.with_result(ctx.receive_node(comprehension_expression.result, ctx.receive_tree))
            comprehension_expression = comprehension_expression.with_clauses(ctx.receive_nodes(comprehension_expression.clauses, ctx.receive_tree))
            comprehension_expression = comprehension_expression.with_suffix(ctx.receive_node(comprehension_expression.suffix, PythonReceiver.receive_space))
            comprehension_expression = comprehension_expression.with_type(ctx.receive_value(comprehension_expression.type, JavaType))
            return comprehension_expression

        def visit_comprehension_condition(self, condition: ComprehensionExpression.Condition, ctx: ReceiverContext) -> J:
            condition = condition.with_id(ctx.receive_value(condition.id, UUID))
            condition = condition.with_prefix(ctx.receive_node(condition.prefix, PythonReceiver.receive_space))
            condition = condition.with_markers(ctx.receive_node(condition.markers, ctx.receive_markers))
            condition = condition.with_expression(ctx.receive_node(condition.expression, ctx.receive_tree))
            return condition

        def visit_comprehension_clause(self, clause: ComprehensionExpression.Clause, ctx: ReceiverContext) -> J:
            clause = clause.with_id(ctx.receive_value(clause.id, UUID))
            clause = clause.with_prefix(ctx.receive_node(clause.prefix, PythonReceiver.receive_space))
            clause = clause.with_markers(ctx.receive_node(clause.markers, ctx.receive_markers))
            clause = clause.padding.with_async(ctx.receive_node(clause.padding.async_, PythonReceiver.right_padded_value_receiver(bool)))
            clause = clause.with_iterator_variable(ctx.receive_node(clause.iterator_variable, ctx.receive_tree))
            clause = clause.padding.with_iterated_list(ctx.receive_node(clause.padding.iterated_list, PythonReceiver.receive_left_padded_tree))
            clause = clause.with_conditions(ctx.receive_nodes(clause.conditions, ctx.receive_tree))
            return clause

        def visit_type_alias(self, type_alias: TypeAlias, ctx: ReceiverContext) -> J:
            type_alias = type_alias.with_id(ctx.receive_value(type_alias.id, UUID))
            type_alias = type_alias.with_prefix(ctx.receive_node(type_alias.prefix, PythonReceiver.receive_space))
            type_alias = type_alias.with_markers(ctx.receive_node(type_alias.markers, ctx.receive_markers))
            type_alias = type_alias.with_name(ctx.receive_node(type_alias.name, ctx.receive_tree))
            type_alias = type_alias.padding.with_value(ctx.receive_node(type_alias.padding.value, PythonReceiver.receive_left_padded_tree))
            type_alias = type_alias.with_type(ctx.receive_value(type_alias.type, JavaType))
            return type_alias

        def visit_yield_from(self, yield_from: YieldFrom, ctx: ReceiverContext) -> J:
            yield_from = yield_from.with_id(ctx.receive_value(yield_from.id, UUID))
            yield_from = yield_from.with_prefix(ctx.receive_node(yield_from.prefix, PythonReceiver.receive_space))
            yield_from = yield_from.with_markers(ctx.receive_node(yield_from.markers, ctx.receive_markers))
            yield_from = yield_from.with_expression(ctx.receive_node(yield_from.expression, ctx.receive_tree))
            yield_from = yield_from.with_type(ctx.receive_value(yield_from.type, JavaType))
            return yield_from

        def visit_union_type(self, union_type: UnionType, ctx: ReceiverContext) -> J:
            union_type = union_type.with_id(ctx.receive_value(union_type.id, UUID))
            union_type = union_type.with_prefix(ctx.receive_node(union_type.prefix, PythonReceiver.receive_space))
            union_type = union_type.with_markers(ctx.receive_node(union_type.markers, ctx.receive_markers))
            union_type = union_type.padding.with_types(ctx.receive_nodes(union_type.padding.types, PythonReceiver.receive_right_padded_tree))
            union_type = union_type.with_type(ctx.receive_value(union_type.type, JavaType))
            return union_type

        def visit_variable_scope(self, variable_scope: VariableScope, ctx: ReceiverContext) -> J:
            variable_scope = variable_scope.with_id(ctx.receive_value(variable_scope.id, UUID))
            variable_scope = variable_scope.with_prefix(ctx.receive_node(variable_scope.prefix, PythonReceiver.receive_space))
            variable_scope = variable_scope.with_markers(ctx.receive_node(variable_scope.markers, ctx.receive_markers))
            variable_scope = variable_scope.with_kind(ctx.receive_value(variable_scope.kind, VariableScope.Kind))
            variable_scope = variable_scope.padding.with_names(ctx.receive_nodes(variable_scope.padding.names, PythonReceiver.receive_right_padded_tree))
            return variable_scope

        def visit_del(self, del_: Del, ctx: ReceiverContext) -> J:
            del_ = del_.with_id(ctx.receive_value(del_.id, UUID))
            del_ = del_.with_prefix(ctx.receive_node(del_.prefix, PythonReceiver.receive_space))
            del_ = del_.with_markers(ctx.receive_node(del_.markers, ctx.receive_markers))
            del_ = del_.padding.with_targets(ctx.receive_nodes(del_.padding.targets, PythonReceiver.receive_right_padded_tree))
            return del_

        def visit_special_parameter(self, special_parameter: SpecialParameter, ctx: ReceiverContext) -> J:
            special_parameter = special_parameter.with_id(ctx.receive_value(special_parameter.id, UUID))
            special_parameter = special_parameter.with_prefix(ctx.receive_node(special_parameter.prefix, PythonReceiver.receive_space))
            special_parameter = special_parameter.with_markers(ctx.receive_node(special_parameter.markers, ctx.receive_markers))
            special_parameter = special_parameter.with_kind(ctx.receive_value(special_parameter.kind, SpecialParameter.Kind))
            special_parameter = special_parameter.with_type_hint(ctx.receive_node(special_parameter.type_hint, ctx.receive_tree))
            special_parameter = special_parameter.with_type(ctx.receive_value(special_parameter.type, JavaType))
            return special_parameter

        def visit_star(self, star: Star, ctx: ReceiverContext) -> J:
            star = star.with_id(ctx.receive_value(star.id, UUID))
            star = star.with_prefix(ctx.receive_node(star.prefix, PythonReceiver.receive_space))
            star = star.with_markers(ctx.receive_node(star.markers, ctx.receive_markers))
            star = star.with_kind(ctx.receive_value(star.kind, Star.Kind))
            star = star.with_expression(ctx.receive_node(star.expression, ctx.receive_tree))
            star = star.with_type(ctx.receive_value(star.type, JavaType))
            return star

        def visit_named_argument(self, named_argument: NamedArgument, ctx: ReceiverContext) -> J:
            named_argument = named_argument.with_id(ctx.receive_value(named_argument.id, UUID))
            named_argument = named_argument.with_prefix(ctx.receive_node(named_argument.prefix, PythonReceiver.receive_space))
            named_argument = named_argument.with_markers(ctx.receive_node(named_argument.markers, ctx.receive_markers))
            named_argument = named_argument.with_name(ctx.receive_node(named_argument.name, ctx.receive_tree))
            named_argument = named_argument.padding.with_value(ctx.receive_node(named_argument.padding.value, PythonReceiver.receive_left_padded_tree))
            named_argument = named_argument.with_type(ctx.receive_value(named_argument.type, JavaType))
            return named_argument

        def visit_type_hinted_expression(self, type_hinted_expression: TypeHintedExpression, ctx: ReceiverContext) -> J:
            type_hinted_expression = type_hinted_expression.with_id(ctx.receive_value(type_hinted_expression.id, UUID))
            type_hinted_expression = type_hinted_expression.with_prefix(ctx.receive_node(type_hinted_expression.prefix, PythonReceiver.receive_space))
            type_hinted_expression = type_hinted_expression.with_markers(ctx.receive_node(type_hinted_expression.markers, ctx.receive_markers))
            type_hinted_expression = type_hinted_expression.with_expression(ctx.receive_node(type_hinted_expression.expression, ctx.receive_tree))
            type_hinted_expression = type_hinted_expression.with_type_hint(ctx.receive_node(type_hinted_expression.type_hint, ctx.receive_tree))
            type_hinted_expression = type_hinted_expression.with_type(ctx.receive_value(type_hinted_expression.type, JavaType))
            return type_hinted_expression

        def visit_error_from(self, error_from: ErrorFrom, ctx: ReceiverContext) -> J:
            error_from = error_from.with_id(ctx.receive_value(error_from.id, UUID))
            error_from = error_from.with_prefix(ctx.receive_node(error_from.prefix, PythonReceiver.receive_space))
            error_from = error_from.with_markers(ctx.receive_node(error_from.markers, ctx.receive_markers))
            error_from = error_from.with_error(ctx.receive_node(error_from.error, ctx.receive_tree))
            error_from = error_from.padding.with_from(ctx.receive_node(error_from.padding.from_, PythonReceiver.receive_left_padded_tree))
            error_from = error_from.with_type(ctx.receive_value(error_from.type, JavaType))
            return error_from

        def visit_match_case(self, match_case: MatchCase, ctx: ReceiverContext) -> J:
            match_case = match_case.with_id(ctx.receive_value(match_case.id, UUID))
            match_case = match_case.with_prefix(ctx.receive_node(match_case.prefix, PythonReceiver.receive_space))
            match_case = match_case.with_markers(ctx.receive_node(match_case.markers, ctx.receive_markers))
            match_case = match_case.with_pattern(ctx.receive_node(match_case.pattern, ctx.receive_tree))
            match_case = match_case.padding.with_guard(ctx.receive_node(match_case.padding.guard, PythonReceiver.receive_left_padded_tree))
            match_case = match_case.with_type(ctx.receive_value(match_case.type, JavaType))
            return match_case

        def visit_match_case_pattern(self, pattern: MatchCase.Pattern, ctx: ReceiverContext) -> J:
            pattern = pattern.with_id(ctx.receive_value(pattern.id, UUID))
            pattern = pattern.with_prefix(ctx.receive_node(pattern.prefix, PythonReceiver.receive_space))
            pattern = pattern.with_markers(ctx.receive_node(pattern.markers, ctx.receive_markers))
            pattern = pattern.with_kind(ctx.receive_value(pattern.kind, MatchCase.Pattern.Kind))
            pattern = pattern.padding.with_children(ctx.receive_node(pattern.padding.children, PythonReceiver.receive_container))
            pattern = pattern.with_type(ctx.receive_value(pattern.type, JavaType))
            return pattern

        def visit_slice(self, slice: Slice, ctx: ReceiverContext) -> J:
            slice = slice.with_id(ctx.receive_value(slice.id, UUID))
            slice = slice.with_prefix(ctx.receive_node(slice.prefix, PythonReceiver.receive_space))
            slice = slice.with_markers(ctx.receive_node(slice.markers, ctx.receive_markers))
            slice = slice.padding.with_start(ctx.receive_node(slice.padding.start, PythonReceiver.receive_right_padded_tree))
            slice = slice.padding.with_stop(ctx.receive_node(slice.padding.stop, PythonReceiver.receive_right_padded_tree))
            slice = slice.padding.with_step(ctx.receive_node(slice.padding.step, PythonReceiver.receive_right_padded_tree))
            return slice

        def visit_annotated_type(self, annotated_type: AnnotatedType, ctx: ReceiverContext) -> J:
            annotated_type = annotated_type.with_id(ctx.receive_value(annotated_type.id, UUID))
            annotated_type = annotated_type.with_prefix(ctx.receive_node(annotated_type.prefix, PythonReceiver.receive_space))
            annotated_type = annotated_type.with_markers(ctx.receive_node(annotated_type.markers, ctx.receive_markers))
            annotated_type = annotated_type.with_annotations(ctx.receive_nodes(annotated_type.annotations, ctx.receive_tree))
            annotated_type = annotated_type.with_type_expression(ctx.receive_node(annotated_type.type_expression, ctx.receive_tree))
            return annotated_type

        def visit_annotation(self, annotation: Annotation, ctx: ReceiverContext) -> J:
            annotation = annotation.with_id(ctx.receive_value(annotation.id, UUID))
            annotation = annotation.with_prefix(ctx.receive_node(annotation.prefix, PythonReceiver.receive_space))
            annotation = annotation.with_markers(ctx.receive_node(annotation.markers, ctx.receive_markers))
            annotation = annotation.with_annotation_type(ctx.receive_node(annotation.annotation_type, ctx.receive_tree))
            annotation = annotation.padding.with_arguments(ctx.receive_node(annotation.padding.arguments, PythonReceiver.receive_container))
            return annotation

        def visit_array_access(self, array_access: ArrayAccess, ctx: ReceiverContext) -> J:
            array_access = array_access.with_id(ctx.receive_value(array_access.id, UUID))
            array_access = array_access.with_prefix(ctx.receive_node(array_access.prefix, PythonReceiver.receive_space))
            array_access = array_access.with_markers(ctx.receive_node(array_access.markers, ctx.receive_markers))
            array_access = array_access.with_indexed(ctx.receive_node(array_access.indexed, ctx.receive_tree))
            array_access = array_access.with_dimension(ctx.receive_node(array_access.dimension, ctx.receive_tree))
            array_access = array_access.with_type(ctx.receive_value(array_access.type, JavaType))
            return array_access

        def visit_array_type(self, array_type: ArrayType, ctx: ReceiverContext) -> J:
            array_type = array_type.with_id(ctx.receive_value(array_type.id, UUID))
            array_type = array_type.with_prefix(ctx.receive_node(array_type.prefix, PythonReceiver.receive_space))
            array_type = array_type.with_markers(ctx.receive_node(array_type.markers, ctx.receive_markers))
            array_type = array_type.with_element_type(ctx.receive_node(array_type.element_type, ctx.receive_tree))
            array_type = array_type.with_annotations(ctx.receive_nodes(array_type.annotations, ctx.receive_tree))
            array_type = array_type.with_dimension(ctx.receive_node(array_type.dimension, PythonReceiver.left_padded_node_receiver(Space)))
            array_type = array_type.with_type(ctx.receive_value(array_type.type, JavaType))
            return array_type

        def visit_assert(self, assert_: Assert, ctx: ReceiverContext) -> J:
            assert_ = assert_.with_id(ctx.receive_value(assert_.id, UUID))
            assert_ = assert_.with_prefix(ctx.receive_node(assert_.prefix, PythonReceiver.receive_space))
            assert_ = assert_.with_markers(ctx.receive_node(assert_.markers, ctx.receive_markers))
            assert_ = assert_.with_condition(ctx.receive_node(assert_.condition, ctx.receive_tree))
            assert_ = assert_.with_detail(ctx.receive_node(assert_.detail, PythonReceiver.receive_left_padded_tree))
            return assert_

        def visit_assignment(self, assignment: Assignment, ctx: ReceiverContext) -> J:
            assignment = assignment.with_id(ctx.receive_value(assignment.id, UUID))
            assignment = assignment.with_prefix(ctx.receive_node(assignment.prefix, PythonReceiver.receive_space))
            assignment = assignment.with_markers(ctx.receive_node(assignment.markers, ctx.receive_markers))
            assignment = assignment.with_variable(ctx.receive_node(assignment.variable, ctx.receive_tree))
            assignment = assignment.padding.with_assignment(ctx.receive_node(assignment.padding.assignment, PythonReceiver.receive_left_padded_tree))
            assignment = assignment.with_type(ctx.receive_value(assignment.type, JavaType))
            return assignment

        def visit_assignment_operation(self, assignment_operation: AssignmentOperation, ctx: ReceiverContext) -> J:
            assignment_operation = assignment_operation.with_id(ctx.receive_value(assignment_operation.id, UUID))
            assignment_operation = assignment_operation.with_prefix(ctx.receive_node(assignment_operation.prefix, PythonReceiver.receive_space))
            assignment_operation = assignment_operation.with_markers(ctx.receive_node(assignment_operation.markers, ctx.receive_markers))
            assignment_operation = assignment_operation.with_variable(ctx.receive_node(assignment_operation.variable, ctx.receive_tree))
            assignment_operation = assignment_operation.padding.with_operator(ctx.receive_node(assignment_operation.padding.operator, PythonReceiver.left_padded_value_receiver(AssignmentOperation.Type)))
            assignment_operation = assignment_operation.with_assignment(ctx.receive_node(assignment_operation.assignment, ctx.receive_tree))
            assignment_operation = assignment_operation.with_type(ctx.receive_value(assignment_operation.type, JavaType))
            return assignment_operation

        def visit_binary(self, binary: Binary, ctx: ReceiverContext) -> J:
            binary = binary.with_id(ctx.receive_value(binary.id, UUID))
            binary = binary.with_prefix(ctx.receive_node(binary.prefix, PythonReceiver.receive_space))
            binary = binary.with_markers(ctx.receive_node(binary.markers, ctx.receive_markers))
            binary = binary.with_left(ctx.receive_node(binary.left, ctx.receive_tree))
            binary = binary.padding.with_operator(ctx.receive_node(binary.padding.operator, PythonReceiver.left_padded_value_receiver(Binary.Type)))
            binary = binary.with_right(ctx.receive_node(binary.right, ctx.receive_tree))
            binary = binary.with_type(ctx.receive_value(binary.type, JavaType))
            return binary

        def visit_block(self, block: Block, ctx: ReceiverContext) -> J:
            block = block.with_id(ctx.receive_value(block.id, UUID))
            block = block.with_prefix(ctx.receive_node(block.prefix, PythonReceiver.receive_space))
            block = block.with_markers(ctx.receive_node(block.markers, ctx.receive_markers))
            block = block.padding.with_static(ctx.receive_node(block.padding.static, PythonReceiver.right_padded_value_receiver(bool)))
            block = block.padding.with_statements(ctx.receive_nodes(block.padding.statements, PythonReceiver.receive_right_padded_tree))
            block = block.with_end(ctx.receive_node(block.end, PythonReceiver.receive_space))
            return block

        def visit_break(self, break_: Break, ctx: ReceiverContext) -> J:
            break_ = break_.with_id(ctx.receive_value(break_.id, UUID))
            break_ = break_.with_prefix(ctx.receive_node(break_.prefix, PythonReceiver.receive_space))
            break_ = break_.with_markers(ctx.receive_node(break_.markers, ctx.receive_markers))
            break_ = break_.with_label(ctx.receive_node(break_.label, ctx.receive_tree))
            return break_

        def visit_case(self, case: Case, ctx: ReceiverContext) -> J:
            case = case.with_id(ctx.receive_value(case.id, UUID))
            case = case.with_prefix(ctx.receive_node(case.prefix, PythonReceiver.receive_space))
            case = case.with_markers(ctx.receive_node(case.markers, ctx.receive_markers))
            case = case.with_type(ctx.receive_value(case.type, Case.Type))
            case = case.padding.with_case_labels(ctx.receive_node(case.padding.case_labels, PythonReceiver.receive_container))
            case = case.padding.with_statements(ctx.receive_node(case.padding.statements, PythonReceiver.receive_container))
            case = case.padding.with_body(ctx.receive_node(case.padding.body, PythonReceiver.receive_right_padded_tree))
            case = case.with_guard(ctx.receive_node(case.guard, ctx.receive_tree))
            return case

        def visit_class_declaration(self, class_declaration: ClassDeclaration, ctx: ReceiverContext) -> J:
            class_declaration = class_declaration.with_id(ctx.receive_value(class_declaration.id, UUID))
            class_declaration = class_declaration.with_prefix(ctx.receive_node(class_declaration.prefix, PythonReceiver.receive_space))
            class_declaration = class_declaration.with_markers(ctx.receive_node(class_declaration.markers, ctx.receive_markers))
            class_declaration = class_declaration.with_leading_annotations(ctx.receive_nodes(class_declaration.leading_annotations, ctx.receive_tree))
            class_declaration = class_declaration.with_modifiers(ctx.receive_nodes(class_declaration.modifiers, ctx.receive_tree))
            class_declaration = class_declaration.padding.with_kind(ctx.receive_node(class_declaration.padding.kind, ctx.receive_tree))
            class_declaration = class_declaration.with_name(ctx.receive_node(class_declaration.name, ctx.receive_tree))
            class_declaration = class_declaration.padding.with_type_parameters(ctx.receive_node(class_declaration.padding.type_parameters, PythonReceiver.receive_container))
            class_declaration = class_declaration.padding.with_primary_constructor(ctx.receive_node(class_declaration.padding.primary_constructor, PythonReceiver.receive_container))
            class_declaration = class_declaration.padding.with_extends(ctx.receive_node(class_declaration.padding.extends, PythonReceiver.receive_left_padded_tree))
            class_declaration = class_declaration.padding.with_implements(ctx.receive_node(class_declaration.padding.implements, PythonReceiver.receive_container))
            class_declaration = class_declaration.padding.with_permits(ctx.receive_node(class_declaration.padding.permits, PythonReceiver.receive_container))
            class_declaration = class_declaration.with_body(ctx.receive_node(class_declaration.body, ctx.receive_tree))
            class_declaration = class_declaration.with_type(ctx.receive_value(class_declaration.type, JavaType.FullyQualified))
            return class_declaration

        def visit_class_declaration_kind(self, kind: ClassDeclaration.Kind, ctx: ReceiverContext) -> J:
            kind = kind.with_id(ctx.receive_value(kind.id, UUID))
            kind = kind.with_prefix(ctx.receive_node(kind.prefix, PythonReceiver.receive_space))
            kind = kind.with_markers(ctx.receive_node(kind.markers, ctx.receive_markers))
            kind = kind.with_annotations(ctx.receive_nodes(kind.annotations, ctx.receive_tree))
            kind = kind.with_type(ctx.receive_value(kind.type, ClassDeclaration.Kind.Type))
            return kind

        def visit_continue(self, continue_: Continue, ctx: ReceiverContext) -> J:
            continue_ = continue_.with_id(ctx.receive_value(continue_.id, UUID))
            continue_ = continue_.with_prefix(ctx.receive_node(continue_.prefix, PythonReceiver.receive_space))
            continue_ = continue_.with_markers(ctx.receive_node(continue_.markers, ctx.receive_markers))
            continue_ = continue_.with_label(ctx.receive_node(continue_.label, ctx.receive_tree))
            return continue_

        def visit_do_while_loop(self, do_while_loop: DoWhileLoop, ctx: ReceiverContext) -> J:
            do_while_loop = do_while_loop.with_id(ctx.receive_value(do_while_loop.id, UUID))
            do_while_loop = do_while_loop.with_prefix(ctx.receive_node(do_while_loop.prefix, PythonReceiver.receive_space))
            do_while_loop = do_while_loop.with_markers(ctx.receive_node(do_while_loop.markers, ctx.receive_markers))
            do_while_loop = do_while_loop.padding.with_body(ctx.receive_node(do_while_loop.padding.body, PythonReceiver.receive_right_padded_tree))
            do_while_loop = do_while_loop.padding.with_while_condition(ctx.receive_node(do_while_loop.padding.while_condition, PythonReceiver.receive_left_padded_tree))
            return do_while_loop

        def visit_empty(self, empty: Empty, ctx: ReceiverContext) -> J:
            empty = empty.with_id(ctx.receive_value(empty.id, UUID))
            empty = empty.with_prefix(ctx.receive_node(empty.prefix, PythonReceiver.receive_space))
            empty = empty.with_markers(ctx.receive_node(empty.markers, ctx.receive_markers))
            return empty

        def visit_enum_value(self, enum_value: EnumValue, ctx: ReceiverContext) -> J:
            enum_value = enum_value.with_id(ctx.receive_value(enum_value.id, UUID))
            enum_value = enum_value.with_prefix(ctx.receive_node(enum_value.prefix, PythonReceiver.receive_space))
            enum_value = enum_value.with_markers(ctx.receive_node(enum_value.markers, ctx.receive_markers))
            enum_value = enum_value.with_annotations(ctx.receive_nodes(enum_value.annotations, ctx.receive_tree))
            enum_value = enum_value.with_name(ctx.receive_node(enum_value.name, ctx.receive_tree))
            enum_value = enum_value.with_initializer(ctx.receive_node(enum_value.initializer, ctx.receive_tree))
            return enum_value

        def visit_enum_value_set(self, enum_value_set: EnumValueSet, ctx: ReceiverContext) -> J:
            enum_value_set = enum_value_set.with_id(ctx.receive_value(enum_value_set.id, UUID))
            enum_value_set = enum_value_set.with_prefix(ctx.receive_node(enum_value_set.prefix, PythonReceiver.receive_space))
            enum_value_set = enum_value_set.with_markers(ctx.receive_node(enum_value_set.markers, ctx.receive_markers))
            enum_value_set = enum_value_set.padding.with_enums(ctx.receive_nodes(enum_value_set.padding.enums, PythonReceiver.receive_right_padded_tree))
            enum_value_set = enum_value_set.with_terminated_with_semicolon(ctx.receive_value(enum_value_set.terminated_with_semicolon, bool))
            return enum_value_set

        def visit_field_access(self, field_access: FieldAccess, ctx: ReceiverContext) -> J:
            field_access = field_access.with_id(ctx.receive_value(field_access.id, UUID))
            field_access = field_access.with_prefix(ctx.receive_node(field_access.prefix, PythonReceiver.receive_space))
            field_access = field_access.with_markers(ctx.receive_node(field_access.markers, ctx.receive_markers))
            field_access = field_access.with_target(ctx.receive_node(field_access.target, ctx.receive_tree))
            field_access = field_access.padding.with_name(ctx.receive_node(field_access.padding.name, PythonReceiver.receive_left_padded_tree))
            field_access = field_access.with_type(ctx.receive_value(field_access.type, JavaType))
            return field_access

        def visit_for_each_loop(self, for_each_loop: ForEachLoop, ctx: ReceiverContext) -> J:
            for_each_loop = for_each_loop.with_id(ctx.receive_value(for_each_loop.id, UUID))
            for_each_loop = for_each_loop.with_prefix(ctx.receive_node(for_each_loop.prefix, PythonReceiver.receive_space))
            for_each_loop = for_each_loop.with_markers(ctx.receive_node(for_each_loop.markers, ctx.receive_markers))
            for_each_loop = for_each_loop.with_control(ctx.receive_node(for_each_loop.control, ctx.receive_tree))
            for_each_loop = for_each_loop.padding.with_body(ctx.receive_node(for_each_loop.padding.body, PythonReceiver.receive_right_padded_tree))
            return for_each_loop

        def visit_for_each_control(self, control: ForEachLoop.Control, ctx: ReceiverContext) -> J:
            control = control.with_id(ctx.receive_value(control.id, UUID))
            control = control.with_prefix(ctx.receive_node(control.prefix, PythonReceiver.receive_space))
            control = control.with_markers(ctx.receive_node(control.markers, ctx.receive_markers))
            control = control.padding.with_variable(ctx.receive_node(control.padding.variable, PythonReceiver.receive_right_padded_tree))
            control = control.padding.with_iterable(ctx.receive_node(control.padding.iterable, PythonReceiver.receive_right_padded_tree))
            return control

        def visit_for_loop(self, for_loop: ForLoop, ctx: ReceiverContext) -> J:
            for_loop = for_loop.with_id(ctx.receive_value(for_loop.id, UUID))
            for_loop = for_loop.with_prefix(ctx.receive_node(for_loop.prefix, PythonReceiver.receive_space))
            for_loop = for_loop.with_markers(ctx.receive_node(for_loop.markers, ctx.receive_markers))
            for_loop = for_loop.with_control(ctx.receive_node(for_loop.control, ctx.receive_tree))
            for_loop = for_loop.padding.with_body(ctx.receive_node(for_loop.padding.body, PythonReceiver.receive_right_padded_tree))
            return for_loop

        def visit_for_control(self, control: ForLoop.Control, ctx: ReceiverContext) -> J:
            control = control.with_id(ctx.receive_value(control.id, UUID))
            control = control.with_prefix(ctx.receive_node(control.prefix, PythonReceiver.receive_space))
            control = control.with_markers(ctx.receive_node(control.markers, ctx.receive_markers))
            control = control.padding.with_init(ctx.receive_nodes(control.padding.init, PythonReceiver.receive_right_padded_tree))
            control = control.padding.with_condition(ctx.receive_node(control.padding.condition, PythonReceiver.receive_right_padded_tree))
            control = control.padding.with_update(ctx.receive_nodes(control.padding.update, PythonReceiver.receive_right_padded_tree))
            return control

        def visit_parenthesized_type_tree(self, parenthesized_type_tree: ParenthesizedTypeTree, ctx: ReceiverContext) -> J:
            parenthesized_type_tree = parenthesized_type_tree.with_id(ctx.receive_value(parenthesized_type_tree.id, UUID))
            parenthesized_type_tree = parenthesized_type_tree.with_prefix(ctx.receive_node(parenthesized_type_tree.prefix, PythonReceiver.receive_space))
            parenthesized_type_tree = parenthesized_type_tree.with_markers(ctx.receive_node(parenthesized_type_tree.markers, ctx.receive_markers))
            parenthesized_type_tree = parenthesized_type_tree.with_annotations(ctx.receive_nodes(parenthesized_type_tree.annotations, ctx.receive_tree))
            parenthesized_type_tree = parenthesized_type_tree.with_parenthesized_type(ctx.receive_node(parenthesized_type_tree.parenthesized_type, ctx.receive_tree))
            return parenthesized_type_tree

        def visit_identifier(self, identifier: Identifier, ctx: ReceiverContext) -> J:
            identifier = identifier.with_id(ctx.receive_value(identifier.id, UUID))
            identifier = identifier.with_prefix(ctx.receive_node(identifier.prefix, PythonReceiver.receive_space))
            identifier = identifier.with_markers(ctx.receive_node(identifier.markers, ctx.receive_markers))
            identifier = identifier.with_annotations(ctx.receive_nodes(identifier.annotations, ctx.receive_tree))
            identifier = identifier.with_simple_name(ctx.receive_value(identifier.simple_name, str))
            identifier = identifier.with_type(ctx.receive_value(identifier.type, JavaType))
            identifier = identifier.with_field_type(ctx.receive_value(identifier.field_type, JavaType.Variable))
            return identifier

        def visit_if(self, if_: If, ctx: ReceiverContext) -> J:
            if_ = if_.with_id(ctx.receive_value(if_.id, UUID))
            if_ = if_.with_prefix(ctx.receive_node(if_.prefix, PythonReceiver.receive_space))
            if_ = if_.with_markers(ctx.receive_node(if_.markers, ctx.receive_markers))
            if_ = if_.with_if_condition(ctx.receive_node(if_.if_condition, ctx.receive_tree))
            if_ = if_.padding.with_then_part(ctx.receive_node(if_.padding.then_part, PythonReceiver.receive_right_padded_tree))
            if_ = if_.with_else_part(ctx.receive_node(if_.else_part, ctx.receive_tree))
            return if_

        def visit_else(self, else_: If.Else, ctx: ReceiverContext) -> J:
            else_ = else_.with_id(ctx.receive_value(else_.id, UUID))
            else_ = else_.with_prefix(ctx.receive_node(else_.prefix, PythonReceiver.receive_space))
            else_ = else_.with_markers(ctx.receive_node(else_.markers, ctx.receive_markers))
            else_ = else_.padding.with_body(ctx.receive_node(else_.padding.body, PythonReceiver.receive_right_padded_tree))
            return else_

        def visit_import(self, import_: Import, ctx: ReceiverContext) -> J:
            import_ = import_.with_id(ctx.receive_value(import_.id, UUID))
            import_ = import_.with_prefix(ctx.receive_node(import_.prefix, PythonReceiver.receive_space))
            import_ = import_.with_markers(ctx.receive_node(import_.markers, ctx.receive_markers))
            import_ = import_.padding.with_static(ctx.receive_node(import_.padding.static, PythonReceiver.left_padded_value_receiver(bool)))
            import_ = import_.with_qualid(ctx.receive_node(import_.qualid, ctx.receive_tree))
            import_ = import_.padding.with_alias(ctx.receive_node(import_.padding.alias, PythonReceiver.receive_left_padded_tree))
            return import_

        def visit_instance_of(self, instance_of: InstanceOf, ctx: ReceiverContext) -> J:
            instance_of = instance_of.with_id(ctx.receive_value(instance_of.id, UUID))
            instance_of = instance_of.with_prefix(ctx.receive_node(instance_of.prefix, PythonReceiver.receive_space))
            instance_of = instance_of.with_markers(ctx.receive_node(instance_of.markers, ctx.receive_markers))
            instance_of = instance_of.padding.with_expression(ctx.receive_node(instance_of.padding.expression, PythonReceiver.receive_right_padded_tree))
            instance_of = instance_of.with_clazz(ctx.receive_node(instance_of.clazz, ctx.receive_tree))
            instance_of = instance_of.with_pattern(ctx.receive_node(instance_of.pattern, ctx.receive_tree))
            instance_of = instance_of.with_type(ctx.receive_value(instance_of.type, JavaType))
            return instance_of

        def visit_deconstruction_pattern(self, deconstruction_pattern: DeconstructionPattern, ctx: ReceiverContext) -> J:
            deconstruction_pattern = deconstruction_pattern.with_id(ctx.receive_value(deconstruction_pattern.id, UUID))
            deconstruction_pattern = deconstruction_pattern.with_prefix(ctx.receive_node(deconstruction_pattern.prefix, PythonReceiver.receive_space))
            deconstruction_pattern = deconstruction_pattern.with_markers(ctx.receive_node(deconstruction_pattern.markers, ctx.receive_markers))
            deconstruction_pattern = deconstruction_pattern.with_deconstructor(ctx.receive_node(deconstruction_pattern.deconstructor, ctx.receive_tree))
            deconstruction_pattern = deconstruction_pattern.padding.with_nested(ctx.receive_node(deconstruction_pattern.padding.nested, PythonReceiver.receive_container))
            deconstruction_pattern = deconstruction_pattern.with_type(ctx.receive_value(deconstruction_pattern.type, JavaType))
            return deconstruction_pattern

        def visit_intersection_type(self, intersection_type: IntersectionType, ctx: ReceiverContext) -> J:
            intersection_type = intersection_type.with_id(ctx.receive_value(intersection_type.id, UUID))
            intersection_type = intersection_type.with_prefix(ctx.receive_node(intersection_type.prefix, PythonReceiver.receive_space))
            intersection_type = intersection_type.with_markers(ctx.receive_node(intersection_type.markers, ctx.receive_markers))
            intersection_type = intersection_type.padding.with_bounds(ctx.receive_node(intersection_type.padding.bounds, PythonReceiver.receive_container))
            return intersection_type

        def visit_label(self, label: Label, ctx: ReceiverContext) -> J:
            label = label.with_id(ctx.receive_value(label.id, UUID))
            label = label.with_prefix(ctx.receive_node(label.prefix, PythonReceiver.receive_space))
            label = label.with_markers(ctx.receive_node(label.markers, ctx.receive_markers))
            label = label.padding.with_label(ctx.receive_node(label.padding.label, PythonReceiver.receive_right_padded_tree))
            label = label.with_statement(ctx.receive_node(label.statement, ctx.receive_tree))
            return label

        def visit_lambda(self, lambda_: Lambda, ctx: ReceiverContext) -> J:
            lambda_ = lambda_.with_id(ctx.receive_value(lambda_.id, UUID))
            lambda_ = lambda_.with_prefix(ctx.receive_node(lambda_.prefix, PythonReceiver.receive_space))
            lambda_ = lambda_.with_markers(ctx.receive_node(lambda_.markers, ctx.receive_markers))
            lambda_ = lambda_.with_parameters(ctx.receive_node(lambda_.parameters, ctx.receive_tree))
            lambda_ = lambda_.with_arrow(ctx.receive_node(lambda_.arrow, PythonReceiver.receive_space))
            lambda_ = lambda_.with_body(ctx.receive_node(lambda_.body, ctx.receive_tree))
            lambda_ = lambda_.with_type(ctx.receive_value(lambda_.type, JavaType))
            return lambda_

        def visit_lambda_parameters(self, parameters: Lambda.Parameters, ctx: ReceiverContext) -> J:
            parameters = parameters.with_id(ctx.receive_value(parameters.id, UUID))
            parameters = parameters.with_prefix(ctx.receive_node(parameters.prefix, PythonReceiver.receive_space))
            parameters = parameters.with_markers(ctx.receive_node(parameters.markers, ctx.receive_markers))
            parameters = parameters.with_parenthesized(ctx.receive_value(parameters.parenthesized, bool))
            parameters = parameters.padding.with_parameters(ctx.receive_nodes(parameters.padding.parameters, PythonReceiver.receive_right_padded_tree))
            return parameters

        def visit_literal(self, literal: Literal, ctx: ReceiverContext) -> J:
            literal = literal.with_id(ctx.receive_value(literal.id, UUID))
            literal = literal.with_prefix(ctx.receive_node(literal.prefix, PythonReceiver.receive_space))
            literal = literal.with_markers(ctx.receive_node(literal.markers, ctx.receive_markers))
            literal = literal.with_value(ctx.receive_value(literal.value, object))
            literal = literal.with_value_source(ctx.receive_value(literal.value_source, str))
            literal = literal.with_unicode_escapes(ctx.receive_values(literal.unicode_escapes, Literal.UnicodeEscape))
            literal = literal.with_type(ctx.receive_value(literal.type, JavaType.Primitive))
            return literal

        def visit_member_reference(self, member_reference: MemberReference, ctx: ReceiverContext) -> J:
            member_reference = member_reference.with_id(ctx.receive_value(member_reference.id, UUID))
            member_reference = member_reference.with_prefix(ctx.receive_node(member_reference.prefix, PythonReceiver.receive_space))
            member_reference = member_reference.with_markers(ctx.receive_node(member_reference.markers, ctx.receive_markers))
            member_reference = member_reference.padding.with_containing(ctx.receive_node(member_reference.padding.containing, PythonReceiver.receive_right_padded_tree))
            member_reference = member_reference.padding.with_type_parameters(ctx.receive_node(member_reference.padding.type_parameters, PythonReceiver.receive_container))
            member_reference = member_reference.padding.with_reference(ctx.receive_node(member_reference.padding.reference, PythonReceiver.receive_left_padded_tree))
            member_reference = member_reference.with_type(ctx.receive_value(member_reference.type, JavaType))
            member_reference = member_reference.with_method_type(ctx.receive_value(member_reference.method_type, JavaType.Method))
            member_reference = member_reference.with_variable_type(ctx.receive_value(member_reference.variable_type, JavaType.Variable))
            return member_reference

        def visit_method_declaration(self, method_declaration: MethodDeclaration, ctx: ReceiverContext) -> J:
            method_declaration = method_declaration.with_id(ctx.receive_value(method_declaration.id, UUID))
            method_declaration = method_declaration.with_prefix(ctx.receive_node(method_declaration.prefix, PythonReceiver.receive_space))
            method_declaration = method_declaration.with_markers(ctx.receive_node(method_declaration.markers, ctx.receive_markers))
            method_declaration = method_declaration.with_leading_annotations(ctx.receive_nodes(method_declaration.leading_annotations, ctx.receive_tree))
            method_declaration = method_declaration.with_modifiers(ctx.receive_nodes(method_declaration.modifiers, ctx.receive_tree))
            method_declaration = method_declaration.annotations.with_type_parameters(ctx.receive_node(method_declaration.annotations.type_parameters, ctx.receive_tree))
            method_declaration = method_declaration.with_return_type_expression(ctx.receive_node(method_declaration.return_type_expression, ctx.receive_tree))
            method_declaration = method_declaration.annotations.with_name(ctx.receive_node(method_declaration.annotations.name, PythonReceiver.receive_method_identifier_with_annotations))
            method_declaration = method_declaration.padding.with_parameters(ctx.receive_node(method_declaration.padding.parameters, PythonReceiver.receive_container))
            method_declaration = method_declaration.padding.with_throws(ctx.receive_node(method_declaration.padding.throws, PythonReceiver.receive_container))
            method_declaration = method_declaration.with_body(ctx.receive_node(method_declaration.body, ctx.receive_tree))
            method_declaration = method_declaration.padding.with_default_value(ctx.receive_node(method_declaration.padding.default_value, PythonReceiver.receive_left_padded_tree))
            method_declaration = method_declaration.with_method_type(ctx.receive_value(method_declaration.method_type, JavaType.Method))
            return method_declaration

        def visit_method_invocation(self, method_invocation: MethodInvocation, ctx: ReceiverContext) -> J:
            method_invocation = method_invocation.with_id(ctx.receive_value(method_invocation.id, UUID))
            method_invocation = method_invocation.with_prefix(ctx.receive_node(method_invocation.prefix, PythonReceiver.receive_space))
            method_invocation = method_invocation.with_markers(ctx.receive_node(method_invocation.markers, ctx.receive_markers))
            method_invocation = method_invocation.padding.with_select(ctx.receive_node(method_invocation.padding.select, PythonReceiver.receive_right_padded_tree))
            method_invocation = method_invocation.padding.with_type_parameters(ctx.receive_node(method_invocation.padding.type_parameters, PythonReceiver.receive_container))
            method_invocation = method_invocation.with_name(ctx.receive_node(method_invocation.name, ctx.receive_tree))
            method_invocation = method_invocation.padding.with_arguments(ctx.receive_node(method_invocation.padding.arguments, PythonReceiver.receive_container))
            method_invocation = method_invocation.with_method_type(ctx.receive_value(method_invocation.method_type, JavaType.Method))
            return method_invocation

        def visit_modifier(self, modifier: Modifier, ctx: ReceiverContext) -> J:
            modifier = modifier.with_id(ctx.receive_value(modifier.id, UUID))
            modifier = modifier.with_prefix(ctx.receive_node(modifier.prefix, PythonReceiver.receive_space))
            modifier = modifier.with_markers(ctx.receive_node(modifier.markers, ctx.receive_markers))
            modifier = modifier.with_keyword(ctx.receive_value(modifier.keyword, str))
            modifier = modifier.with_type(ctx.receive_value(modifier.type, Modifier.Type))
            modifier = modifier.with_annotations(ctx.receive_nodes(modifier.annotations, ctx.receive_tree))
            return modifier

        def visit_multi_catch(self, multi_catch: MultiCatch, ctx: ReceiverContext) -> J:
            multi_catch = multi_catch.with_id(ctx.receive_value(multi_catch.id, UUID))
            multi_catch = multi_catch.with_prefix(ctx.receive_node(multi_catch.prefix, PythonReceiver.receive_space))
            multi_catch = multi_catch.with_markers(ctx.receive_node(multi_catch.markers, ctx.receive_markers))
            multi_catch = multi_catch.padding.with_alternatives(ctx.receive_nodes(multi_catch.padding.alternatives, PythonReceiver.receive_right_padded_tree))
            return multi_catch

        def visit_new_array(self, new_array: NewArray, ctx: ReceiverContext) -> J:
            new_array = new_array.with_id(ctx.receive_value(new_array.id, UUID))
            new_array = new_array.with_prefix(ctx.receive_node(new_array.prefix, PythonReceiver.receive_space))
            new_array = new_array.with_markers(ctx.receive_node(new_array.markers, ctx.receive_markers))
            new_array = new_array.with_type_expression(ctx.receive_node(new_array.type_expression, ctx.receive_tree))
            new_array = new_array.with_dimensions(ctx.receive_nodes(new_array.dimensions, ctx.receive_tree))
            new_array = new_array.padding.with_initializer(ctx.receive_node(new_array.padding.initializer, PythonReceiver.receive_container))
            new_array = new_array.with_type(ctx.receive_value(new_array.type, JavaType))
            return new_array

        def visit_array_dimension(self, array_dimension: ArrayDimension, ctx: ReceiverContext) -> J:
            array_dimension = array_dimension.with_id(ctx.receive_value(array_dimension.id, UUID))
            array_dimension = array_dimension.with_prefix(ctx.receive_node(array_dimension.prefix, PythonReceiver.receive_space))
            array_dimension = array_dimension.with_markers(ctx.receive_node(array_dimension.markers, ctx.receive_markers))
            array_dimension = array_dimension.padding.with_index(ctx.receive_node(array_dimension.padding.index, PythonReceiver.receive_right_padded_tree))
            return array_dimension

        def visit_new_class(self, new_class: NewClass, ctx: ReceiverContext) -> J:
            new_class = new_class.with_id(ctx.receive_value(new_class.id, UUID))
            new_class = new_class.with_prefix(ctx.receive_node(new_class.prefix, PythonReceiver.receive_space))
            new_class = new_class.with_markers(ctx.receive_node(new_class.markers, ctx.receive_markers))
            new_class = new_class.padding.with_enclosing(ctx.receive_node(new_class.padding.enclosing, PythonReceiver.receive_right_padded_tree))
            new_class = new_class.with_new(ctx.receive_node(new_class.new, PythonReceiver.receive_space))
            new_class = new_class.with_clazz(ctx.receive_node(new_class.clazz, ctx.receive_tree))
            new_class = new_class.padding.with_arguments(ctx.receive_node(new_class.padding.arguments, PythonReceiver.receive_container))
            new_class = new_class.with_body(ctx.receive_node(new_class.body, ctx.receive_tree))
            new_class = new_class.with_constructor_type(ctx.receive_value(new_class.constructor_type, JavaType.Method))
            return new_class

        def visit_nullable_type(self, nullable_type: NullableType, ctx: ReceiverContext) -> J:
            nullable_type = nullable_type.with_id(ctx.receive_value(nullable_type.id, UUID))
            nullable_type = nullable_type.with_prefix(ctx.receive_node(nullable_type.prefix, PythonReceiver.receive_space))
            nullable_type = nullable_type.with_markers(ctx.receive_node(nullable_type.markers, ctx.receive_markers))
            nullable_type = nullable_type.with_annotations(ctx.receive_nodes(nullable_type.annotations, ctx.receive_tree))
            nullable_type = nullable_type.padding.with_type_tree(ctx.receive_node(nullable_type.padding.type_tree, PythonReceiver.receive_right_padded_tree))
            return nullable_type

        def visit_package(self, package: Package, ctx: ReceiverContext) -> J:
            package = package.with_id(ctx.receive_value(package.id, UUID))
            package = package.with_prefix(ctx.receive_node(package.prefix, PythonReceiver.receive_space))
            package = package.with_markers(ctx.receive_node(package.markers, ctx.receive_markers))
            package = package.with_expression(ctx.receive_node(package.expression, ctx.receive_tree))
            package = package.with_annotations(ctx.receive_nodes(package.annotations, ctx.receive_tree))
            return package

        def visit_parameterized_type(self, parameterized_type: ParameterizedType, ctx: ReceiverContext) -> J:
            parameterized_type = parameterized_type.with_id(ctx.receive_value(parameterized_type.id, UUID))
            parameterized_type = parameterized_type.with_prefix(ctx.receive_node(parameterized_type.prefix, PythonReceiver.receive_space))
            parameterized_type = parameterized_type.with_markers(ctx.receive_node(parameterized_type.markers, ctx.receive_markers))
            parameterized_type = parameterized_type.with_clazz(ctx.receive_node(parameterized_type.clazz, ctx.receive_tree))
            parameterized_type = parameterized_type.padding.with_type_parameters(ctx.receive_node(parameterized_type.padding.type_parameters, PythonReceiver.receive_container))
            parameterized_type = parameterized_type.with_type(ctx.receive_value(parameterized_type.type, JavaType))
            return parameterized_type

        def visit_parentheses(self, parentheses: Parentheses[J2], ctx: ReceiverContext) -> J:
            parentheses = parentheses.with_id(ctx.receive_value(parentheses.id, UUID))
            parentheses = parentheses.with_prefix(ctx.receive_node(parentheses.prefix, PythonReceiver.receive_space))
            parentheses = parentheses.with_markers(ctx.receive_node(parentheses.markers, ctx.receive_markers))
            parentheses = parentheses.padding.with_tree(ctx.receive_node(parentheses.padding.tree, PythonReceiver.receive_right_padded_tree))
            return parentheses

        def visit_control_parentheses(self, control_parentheses: ControlParentheses[J2], ctx: ReceiverContext) -> J:
            control_parentheses = control_parentheses.with_id(ctx.receive_value(control_parentheses.id, UUID))
            control_parentheses = control_parentheses.with_prefix(ctx.receive_node(control_parentheses.prefix, PythonReceiver.receive_space))
            control_parentheses = control_parentheses.with_markers(ctx.receive_node(control_parentheses.markers, ctx.receive_markers))
            control_parentheses = control_parentheses.padding.with_tree(ctx.receive_node(control_parentheses.padding.tree, PythonReceiver.receive_right_padded_tree))
            return control_parentheses

        def visit_primitive(self, primitive: Primitive, ctx: ReceiverContext) -> J:
            primitive = primitive.with_id(ctx.receive_value(primitive.id, UUID))
            primitive = primitive.with_prefix(ctx.receive_node(primitive.prefix, PythonReceiver.receive_space))
            primitive = primitive.with_markers(ctx.receive_node(primitive.markers, ctx.receive_markers))
            primitive = primitive.with_type(ctx.receive_value(primitive.type, JavaType.Primitive))
            return primitive

        def visit_return(self, return_: Return, ctx: ReceiverContext) -> J:
            return_ = return_.with_id(ctx.receive_value(return_.id, UUID))
            return_ = return_.with_prefix(ctx.receive_node(return_.prefix, PythonReceiver.receive_space))
            return_ = return_.with_markers(ctx.receive_node(return_.markers, ctx.receive_markers))
            return_ = return_.with_expression(ctx.receive_node(return_.expression, ctx.receive_tree))
            return return_

        def visit_switch(self, switch: Switch, ctx: ReceiverContext) -> J:
            switch = switch.with_id(ctx.receive_value(switch.id, UUID))
            switch = switch.with_prefix(ctx.receive_node(switch.prefix, PythonReceiver.receive_space))
            switch = switch.with_markers(ctx.receive_node(switch.markers, ctx.receive_markers))
            switch = switch.with_selector(ctx.receive_node(switch.selector, ctx.receive_tree))
            switch = switch.with_cases(ctx.receive_node(switch.cases, ctx.receive_tree))
            return switch

        def visit_switch_expression(self, switch_expression: SwitchExpression, ctx: ReceiverContext) -> J:
            switch_expression = switch_expression.with_id(ctx.receive_value(switch_expression.id, UUID))
            switch_expression = switch_expression.with_prefix(ctx.receive_node(switch_expression.prefix, PythonReceiver.receive_space))
            switch_expression = switch_expression.with_markers(ctx.receive_node(switch_expression.markers, ctx.receive_markers))
            switch_expression = switch_expression.with_selector(ctx.receive_node(switch_expression.selector, ctx.receive_tree))
            switch_expression = switch_expression.with_cases(ctx.receive_node(switch_expression.cases, ctx.receive_tree))
            switch_expression = switch_expression.with_type(ctx.receive_value(switch_expression.type, JavaType))
            return switch_expression

        def visit_synchronized(self, synchronized: Synchronized, ctx: ReceiverContext) -> J:
            synchronized = synchronized.with_id(ctx.receive_value(synchronized.id, UUID))
            synchronized = synchronized.with_prefix(ctx.receive_node(synchronized.prefix, PythonReceiver.receive_space))
            synchronized = synchronized.with_markers(ctx.receive_node(synchronized.markers, ctx.receive_markers))
            synchronized = synchronized.with_lock(ctx.receive_node(synchronized.lock, ctx.receive_tree))
            synchronized = synchronized.with_body(ctx.receive_node(synchronized.body, ctx.receive_tree))
            return synchronized

        def visit_ternary(self, ternary: Ternary, ctx: ReceiverContext) -> J:
            ternary = ternary.with_id(ctx.receive_value(ternary.id, UUID))
            ternary = ternary.with_prefix(ctx.receive_node(ternary.prefix, PythonReceiver.receive_space))
            ternary = ternary.with_markers(ctx.receive_node(ternary.markers, ctx.receive_markers))
            ternary = ternary.with_condition(ctx.receive_node(ternary.condition, ctx.receive_tree))
            ternary = ternary.padding.with_true_part(ctx.receive_node(ternary.padding.true_part, PythonReceiver.receive_left_padded_tree))
            ternary = ternary.padding.with_false_part(ctx.receive_node(ternary.padding.false_part, PythonReceiver.receive_left_padded_tree))
            ternary = ternary.with_type(ctx.receive_value(ternary.type, JavaType))
            return ternary

        def visit_throw(self, throw: Throw, ctx: ReceiverContext) -> J:
            throw = throw.with_id(ctx.receive_value(throw.id, UUID))
            throw = throw.with_prefix(ctx.receive_node(throw.prefix, PythonReceiver.receive_space))
            throw = throw.with_markers(ctx.receive_node(throw.markers, ctx.receive_markers))
            throw = throw.with_exception(ctx.receive_node(throw.exception, ctx.receive_tree))
            return throw

        def visit_try(self, try_: Try, ctx: ReceiverContext) -> J:
            try_ = try_.with_id(ctx.receive_value(try_.id, UUID))
            try_ = try_.with_prefix(ctx.receive_node(try_.prefix, PythonReceiver.receive_space))
            try_ = try_.with_markers(ctx.receive_node(try_.markers, ctx.receive_markers))
            try_ = try_.padding.with_resources(ctx.receive_node(try_.padding.resources, PythonReceiver.receive_container))
            try_ = try_.with_body(ctx.receive_node(try_.body, ctx.receive_tree))
            try_ = try_.with_catches(ctx.receive_nodes(try_.catches, ctx.receive_tree))
            try_ = try_.padding.with_finally(ctx.receive_node(try_.padding.finally_, PythonReceiver.receive_left_padded_tree))
            return try_

        def visit_try_resource(self, resource: Try.Resource, ctx: ReceiverContext) -> J:
            resource = resource.with_id(ctx.receive_value(resource.id, UUID))
            resource = resource.with_prefix(ctx.receive_node(resource.prefix, PythonReceiver.receive_space))
            resource = resource.with_markers(ctx.receive_node(resource.markers, ctx.receive_markers))
            resource = resource.with_variable_declarations(ctx.receive_node(resource.variable_declarations, ctx.receive_tree))
            resource = resource.with_terminated_with_semicolon(ctx.receive_value(resource.terminated_with_semicolon, bool))
            return resource

        def visit_catch(self, catch: Try.Catch, ctx: ReceiverContext) -> J:
            catch = catch.with_id(ctx.receive_value(catch.id, UUID))
            catch = catch.with_prefix(ctx.receive_node(catch.prefix, PythonReceiver.receive_space))
            catch = catch.with_markers(ctx.receive_node(catch.markers, ctx.receive_markers))
            catch = catch.with_parameter(ctx.receive_node(catch.parameter, ctx.receive_tree))
            catch = catch.with_body(ctx.receive_node(catch.body, ctx.receive_tree))
            return catch

        def visit_type_cast(self, type_cast: TypeCast, ctx: ReceiverContext) -> J:
            type_cast = type_cast.with_id(ctx.receive_value(type_cast.id, UUID))
            type_cast = type_cast.with_prefix(ctx.receive_node(type_cast.prefix, PythonReceiver.receive_space))
            type_cast = type_cast.with_markers(ctx.receive_node(type_cast.markers, ctx.receive_markers))
            type_cast = type_cast.with_clazz(ctx.receive_node(type_cast.clazz, ctx.receive_tree))
            type_cast = type_cast.with_expression(ctx.receive_node(type_cast.expression, ctx.receive_tree))
            return type_cast

        def visit_type_parameter(self, type_parameter: TypeParameter, ctx: ReceiverContext) -> J:
            type_parameter = type_parameter.with_id(ctx.receive_value(type_parameter.id, UUID))
            type_parameter = type_parameter.with_prefix(ctx.receive_node(type_parameter.prefix, PythonReceiver.receive_space))
            type_parameter = type_parameter.with_markers(ctx.receive_node(type_parameter.markers, ctx.receive_markers))
            type_parameter = type_parameter.with_annotations(ctx.receive_nodes(type_parameter.annotations, ctx.receive_tree))
            type_parameter = type_parameter.with_modifiers(ctx.receive_nodes(type_parameter.modifiers, ctx.receive_tree))
            type_parameter = type_parameter.with_name(ctx.receive_node(type_parameter.name, ctx.receive_tree))
            type_parameter = type_parameter.padding.with_bounds(ctx.receive_node(type_parameter.padding.bounds, PythonReceiver.receive_container))
            return type_parameter

        def visit_type_parameters(self, type_parameters: TypeParameters, ctx: ReceiverContext) -> J:
            type_parameters = type_parameters.with_id(ctx.receive_value(type_parameters.id, UUID))
            type_parameters = type_parameters.with_prefix(ctx.receive_node(type_parameters.prefix, PythonReceiver.receive_space))
            type_parameters = type_parameters.with_markers(ctx.receive_node(type_parameters.markers, ctx.receive_markers))
            type_parameters = type_parameters.with_annotations(ctx.receive_nodes(type_parameters.annotations, ctx.receive_tree))
            type_parameters = type_parameters.padding.with_type_parameters(ctx.receive_nodes(type_parameters.padding.type_parameters, PythonReceiver.receive_right_padded_tree))
            return type_parameters

        def visit_unary(self, unary: Unary, ctx: ReceiverContext) -> J:
            unary = unary.with_id(ctx.receive_value(unary.id, UUID))
            unary = unary.with_prefix(ctx.receive_node(unary.prefix, PythonReceiver.receive_space))
            unary = unary.with_markers(ctx.receive_node(unary.markers, ctx.receive_markers))
            unary = unary.padding.with_operator(ctx.receive_node(unary.padding.operator, PythonReceiver.left_padded_value_receiver(Unary.Type)))
            unary = unary.with_expression(ctx.receive_node(unary.expression, ctx.receive_tree))
            unary = unary.with_type(ctx.receive_value(unary.type, JavaType))
            return unary

        def visit_variable_declarations(self, variable_declarations: VariableDeclarations, ctx: ReceiverContext) -> J:
            variable_declarations = variable_declarations.with_id(ctx.receive_value(variable_declarations.id, UUID))
            variable_declarations = variable_declarations.with_prefix(ctx.receive_node(variable_declarations.prefix, PythonReceiver.receive_space))
            variable_declarations = variable_declarations.with_markers(ctx.receive_node(variable_declarations.markers, ctx.receive_markers))
            variable_declarations = variable_declarations.with_leading_annotations(ctx.receive_nodes(variable_declarations.leading_annotations, ctx.receive_tree))
            variable_declarations = variable_declarations.with_modifiers(ctx.receive_nodes(variable_declarations.modifiers, ctx.receive_tree))
            variable_declarations = variable_declarations.with_type_expression(ctx.receive_node(variable_declarations.type_expression, ctx.receive_tree))
            variable_declarations = variable_declarations.with_varargs(ctx.receive_node(variable_declarations.varargs, PythonReceiver.receive_space))
            variable_declarations = variable_declarations.with_dimensions_before_name(ctx.receive_nodes(variable_declarations.dimensions_before_name, PythonReceiver.left_padded_node_receiver(Space)))
            variable_declarations = variable_declarations.padding.with_variables(ctx.receive_nodes(variable_declarations.padding.variables, PythonReceiver.receive_right_padded_tree))
            return variable_declarations

        def visit_variable(self, named_variable: VariableDeclarations.NamedVariable, ctx: ReceiverContext) -> J:
            named_variable = named_variable.with_id(ctx.receive_value(named_variable.id, UUID))
            named_variable = named_variable.with_prefix(ctx.receive_node(named_variable.prefix, PythonReceiver.receive_space))
            named_variable = named_variable.with_markers(ctx.receive_node(named_variable.markers, ctx.receive_markers))
            named_variable = named_variable.with_name(ctx.receive_node(named_variable.name, ctx.receive_tree))
            named_variable = named_variable.with_dimensions_after_name(ctx.receive_nodes(named_variable.dimensions_after_name, PythonReceiver.left_padded_node_receiver(Space)))
            named_variable = named_variable.padding.with_initializer(ctx.receive_node(named_variable.padding.initializer, PythonReceiver.receive_left_padded_tree))
            named_variable = named_variable.with_variable_type(ctx.receive_value(named_variable.variable_type, JavaType.Variable))
            return named_variable

        def visit_while_loop(self, while_loop: WhileLoop, ctx: ReceiverContext) -> J:
            while_loop = while_loop.with_id(ctx.receive_value(while_loop.id, UUID))
            while_loop = while_loop.with_prefix(ctx.receive_node(while_loop.prefix, PythonReceiver.receive_space))
            while_loop = while_loop.with_markers(ctx.receive_node(while_loop.markers, ctx.receive_markers))
            while_loop = while_loop.with_condition(ctx.receive_node(while_loop.condition, ctx.receive_tree))
            while_loop = while_loop.padding.with_body(ctx.receive_node(while_loop.padding.body, PythonReceiver.receive_right_padded_tree))
            return while_loop

        def visit_wildcard(self, wildcard: Wildcard, ctx: ReceiverContext) -> J:
            wildcard = wildcard.with_id(ctx.receive_value(wildcard.id, UUID))
            wildcard = wildcard.with_prefix(ctx.receive_node(wildcard.prefix, PythonReceiver.receive_space))
            wildcard = wildcard.with_markers(ctx.receive_node(wildcard.markers, ctx.receive_markers))
            wildcard = wildcard.padding.with_bound(ctx.receive_node(wildcard.padding.bound, PythonReceiver.left_padded_value_receiver(Wildcard.Bound)))
            wildcard = wildcard.with_bounded_type(ctx.receive_node(wildcard.bounded_type, ctx.receive_tree))
            return wildcard

        def visit_yield(self, yield_: Yield, ctx: ReceiverContext) -> J:
            yield_ = yield_.with_id(ctx.receive_value(yield_.id, UUID))
            yield_ = yield_.with_prefix(ctx.receive_node(yield_.prefix, PythonReceiver.receive_space))
            yield_ = yield_.with_markers(ctx.receive_node(yield_.markers, ctx.receive_markers))
            yield_ = yield_.with_implicit(ctx.receive_value(yield_.implicit, bool))
            yield_ = yield_.with_value(ctx.receive_node(yield_.value, ctx.receive_tree))
            return yield_

        def visit_unknown(self, unknown: Unknown, ctx: ReceiverContext) -> J:
            unknown = unknown.with_id(ctx.receive_value(unknown.id, UUID))
            unknown = unknown.with_prefix(ctx.receive_node(unknown.prefix, PythonReceiver.receive_space))
            unknown = unknown.with_markers(ctx.receive_node(unknown.markers, ctx.receive_markers))
            unknown = unknown.with_source(ctx.receive_node(unknown.source, ctx.receive_tree))
            return unknown

        def visit_unknown_source(self, source: Unknown.Source, ctx: ReceiverContext) -> J:
            source = source.with_id(ctx.receive_value(source.id, UUID))
            source = source.with_prefix(ctx.receive_node(source.prefix, PythonReceiver.receive_space))
            source = source.with_markers(ctx.receive_node(source.markers, ctx.receive_markers))
            source = source.with_text(ctx.receive_value(source.text, str))
            return source

        def visit_erroneous(self, erroneous: Erroneous, ctx: ReceiverContext) -> J:
            erroneous = erroneous.with_id(ctx.receive_value(erroneous.id, UUID))
            erroneous = erroneous.with_prefix(ctx.receive_node(erroneous.prefix, PythonReceiver.receive_space))
            erroneous = erroneous.with_markers(ctx.receive_node(erroneous.markers, ctx.receive_markers))
            erroneous = erroneous.with_text(ctx.receive_value(erroneous.text, str))
            return erroneous

    # noinspection PyTypeChecker
    class Factory(ReceiverFactory):
        def create(self, type: str, ctx: ReceiverContext) -> Tree:
            if type in ["rewrite.python.tree.Async", "org.openrewrite.python.tree.Py$Async"]:
                return Async(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Await", "org.openrewrite.python.tree.Py$Await"]:
                return Await(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Binary", "org.openrewrite.python.tree.Py$Binary"]:
                return Binary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(Binary.Type)),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ChainedAssignment", "org.openrewrite.python.tree.Py$ChainedAssignment"]:
                return ChainedAssignment(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ExceptionType", "org.openrewrite.python.tree.Py$ExceptionType"]:
                return ExceptionType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, JavaType),
                    ctx.receive_value(None, bool),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.ForLoop", "org.openrewrite.python.tree.Py$ForLoop"]:
                return ForLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.LiteralType", "org.openrewrite.python.tree.Py$LiteralType"]:
                return LiteralType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.TypeHint", "org.openrewrite.python.tree.Py$TypeHint"]:
                return TypeHint(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.CompilationUnit", "org.openrewrite.python.tree.Py$CompilationUnit"]:
                from rewrite.python import CompilationUnit as PyCompilationUnit
                return PyCompilationUnit(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, Path),
                    ctx.receive_value(None, FileAttributes),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, bool),
                    ctx.receive_value(None, Checksum),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space)
                )

            if type in ["rewrite.python.tree.ExpressionStatement", "org.openrewrite.python.tree.Py$ExpressionStatement"]:
                return ExpressionStatement(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.ExpressionTypeTree", "org.openrewrite.python.tree.Py$ExpressionTypeTree"]:
                return ExpressionTypeTree(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.StatementExpression", "org.openrewrite.python.tree.Py$StatementExpression"]:
                return StatementExpression(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.MultiImport", "org.openrewrite.python.tree.Py$MultiImport"]:
                return MultiImport(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_value(None, bool),
                    ctx.receive_node(None, PythonReceiver.receive_container)
                )

            if type in ["rewrite.python.tree.KeyValue", "org.openrewrite.python.tree.Py$KeyValue"]:
                return KeyValue(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.DictLiteral", "org.openrewrite.python.tree.Py$DictLiteral"]:
                return DictLiteral(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.CollectionLiteral", "org.openrewrite.python.tree.Py$CollectionLiteral"]:
                return CollectionLiteral(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, CollectionLiteral.Kind),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.FormattedString", "org.openrewrite.python.tree.Py$FormattedString"]:
                return FormattedString(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.FormattedString.Value", "org.openrewrite.python.tree.Py$FormattedString$Value"]:
                return FormattedString.Value(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.right_padded_value_receiver(bool)),
                    ctx.receive_value(None, FormattedString.Value.Conversion),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Pass", "org.openrewrite.python.tree.Py$Pass"]:
                return Pass(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers)
                )

            if type in ["rewrite.python.tree.TrailingElseWrapper", "org.openrewrite.python.tree.Py$TrailingElseWrapper"]:
                return TrailingElseWrapper(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.python.tree.ComprehensionExpression", "org.openrewrite.python.tree.Py$ComprehensionExpression"]:
                return ComprehensionExpression(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, ComprehensionExpression.Kind),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ComprehensionExpression.Condition", "org.openrewrite.python.tree.Py$ComprehensionExpression$Condition"]:
                return ComprehensionExpression.Condition(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.ComprehensionExpression.Clause", "org.openrewrite.python.tree.Py$ComprehensionExpression$Clause"]:
                return ComprehensionExpression.Clause(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.right_padded_value_receiver(bool)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.TypeAlias", "org.openrewrite.python.tree.Py$TypeAlias"]:
                return TypeAlias(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.YieldFrom", "org.openrewrite.python.tree.Py$YieldFrom"]:
                return YieldFrom(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.UnionType", "org.openrewrite.python.tree.Py$UnionType"]:
                return UnionType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.VariableScope", "org.openrewrite.python.tree.Py$VariableScope"]:
                return VariableScope(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, VariableScope.Kind),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Del", "org.openrewrite.python.tree.Py$Del"]:
                return Del(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.SpecialParameter", "org.openrewrite.python.tree.Py$SpecialParameter"]:
                return SpecialParameter(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, SpecialParameter.Kind),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Star", "org.openrewrite.python.tree.Py$Star"]:
                return Star(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, Star.Kind),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.NamedArgument", "org.openrewrite.python.tree.Py$NamedArgument"]:
                return NamedArgument(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.TypeHintedExpression", "org.openrewrite.python.tree.Py$TypeHintedExpression"]:
                return TypeHintedExpression(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ErrorFrom", "org.openrewrite.python.tree.Py$ErrorFrom"]:
                return ErrorFrom(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.MatchCase", "org.openrewrite.python.tree.Py$MatchCase"]:
                return MatchCase(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.MatchCase.Pattern", "org.openrewrite.python.tree.Py$MatchCase$Pattern"]:
                return MatchCase.Pattern(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, MatchCase.Pattern.Kind),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Slice", "org.openrewrite.python.tree.Py$Slice"]:
                return Slice(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.AnnotatedType", "org.openrewrite.java.tree.J$AnnotatedType"]:
                return AnnotatedType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Annotation", "org.openrewrite.java.tree.J$Annotation"]:
                return Annotation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container)
                )

            if type in ["rewrite.python.tree.ArrayAccess", "org.openrewrite.java.tree.J$ArrayAccess"]:
                return ArrayAccess(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ArrayType", "org.openrewrite.java.tree.J$ArrayType"]:
                return ArrayType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Assert", "org.openrewrite.java.tree.J$Assert"]:
                return Assert(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.python.tree.Assignment", "org.openrewrite.java.tree.J$Assignment"]:
                return Assignment(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.AssignmentOperation", "org.openrewrite.java.tree.J$AssignmentOperation"]:
                return AssignmentOperation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(AssignmentOperation.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Binary", "org.openrewrite.java.tree.J$Binary"]:
                return Binary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(Binary.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Block", "org.openrewrite.java.tree.J$Block"]:
                return Block(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.right_padded_value_receiver(bool)),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space)
                )

            if type in ["rewrite.python.tree.Break", "org.openrewrite.java.tree.J$Break"]:
                return Break(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Case", "org.openrewrite.java.tree.J$Case"]:
                return Case(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, Case.Type),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.ClassDeclaration", "org.openrewrite.java.tree.J$ClassDeclaration"]:
                return ClassDeclaration(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType.FullyQualified)
                )

            if type in ["rewrite.python.tree.ClassDeclaration.Kind", "org.openrewrite.java.tree.J$ClassDeclaration$Kind"]:
                return ClassDeclaration.Kind(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_value(None, ClassDeclaration.Kind.Type)
                )

            if type in ["rewrite.python.tree.Continue", "org.openrewrite.java.tree.J$Continue"]:
                return Continue(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.DoWhileLoop", "org.openrewrite.java.tree.J$DoWhileLoop"]:
                return DoWhileLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.python.tree.Empty", "org.openrewrite.java.tree.J$Empty"]:
                return Empty(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers)
                )

            if type in ["rewrite.python.tree.EnumValue", "org.openrewrite.java.tree.J$EnumValue"]:
                return EnumValue(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.EnumValueSet", "org.openrewrite.java.tree.J$EnumValueSet"]:
                return EnumValueSet(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_value(None, bool)
                )

            if type in ["rewrite.python.tree.FieldAccess", "org.openrewrite.java.tree.J$FieldAccess"]:
                return FieldAccess(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ForEachLoop", "org.openrewrite.java.tree.J$ForEachLoop"]:
                return ForEachLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.ForEachLoop.Control", "org.openrewrite.java.tree.J$ForEachLoop$Control"]:
                return ForEachLoop.Control(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.ForLoop", "org.openrewrite.java.tree.J$ForLoop"]:
                return ForLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.ForLoop.Control", "org.openrewrite.java.tree.J$ForLoop$Control"]:
                return ForLoop.Control(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.ParenthesizedTypeTree", "org.openrewrite.java.tree.J$ParenthesizedTypeTree"]:
                return ParenthesizedTypeTree(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Identifier", "org.openrewrite.java.tree.J$Identifier"]:
                return Identifier(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, JavaType),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.python.tree.If", "org.openrewrite.java.tree.J$If"]:
                return If(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.If.Else", "org.openrewrite.java.tree.J$If$Else"]:
                return If.Else(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Import", "org.openrewrite.java.tree.J$Import"]:
                return Import(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(bool)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.python.tree.InstanceOf", "org.openrewrite.java.tree.J$InstanceOf"]:
                return InstanceOf(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.DeconstructionPattern", "org.openrewrite.java.tree.J$DeconstructionPattern"]:
                return DeconstructionPattern(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.IntersectionType", "org.openrewrite.java.tree.J$IntersectionType"]:
                return IntersectionType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_container)
                )

            if type in ["rewrite.python.tree.Label", "org.openrewrite.java.tree.J$Label"]:
                return Label(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Lambda", "org.openrewrite.java.tree.J$Lambda"]:
                return Lambda(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Lambda.Parameters", "org.openrewrite.java.tree.J$Lambda$Parameters"]:
                return Lambda.Parameters(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, bool),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Literal", "org.openrewrite.java.tree.J$Literal"]:
                return Literal(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, object),
                    ctx.receive_value(None, str),
                    ctx.receive_values(None, Literal.UnicodeEscape),
                    ctx.receive_value(None, JavaType.Primitive)
                )

            if type in ["rewrite.python.tree.MemberReference", "org.openrewrite.java.tree.J$MemberReference"]:
                return MemberReference(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType),
                    ctx.receive_value(None, JavaType.Method),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.python.tree.MethodDeclaration", "org.openrewrite.java.tree.J$MethodDeclaration"]:
                return MethodDeclaration(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_method_identifier_with_annotations),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.python.tree.MethodInvocation", "org.openrewrite.java.tree.J$MethodInvocation"]:
                return MethodInvocation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.python.tree.Modifier", "org.openrewrite.java.tree.J$Modifier"]:
                return Modifier(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, Modifier.Type),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.MultiCatch", "org.openrewrite.java.tree.J$MultiCatch"]:
                return MultiCatch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.NewArray", "org.openrewrite.java.tree.J$NewArray"]:
                return NewArray(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.ArrayDimension", "org.openrewrite.java.tree.J$ArrayDimension"]:
                return ArrayDimension(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.NewClass", "org.openrewrite.java.tree.J$NewClass"]:
                return NewClass(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.python.tree.NullableType", "org.openrewrite.java.tree.J$NullableType"]:
                return NullableType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Package", "org.openrewrite.java.tree.J$Package"]:
                return Package(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.ParameterizedType", "org.openrewrite.java.tree.J$ParameterizedType"]:
                return ParameterizedType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Parentheses", "org.openrewrite.java.tree.J$Parentheses"]:
                return Parentheses[J2](
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.ControlParentheses", "org.openrewrite.java.tree.J$ControlParentheses"]:
                return ControlParentheses[J2](
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Primitive", "org.openrewrite.java.tree.J$Primitive"]:
                return Primitive(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, JavaType.Primitive)
                )

            if type in ["rewrite.python.tree.Return", "org.openrewrite.java.tree.J$Return"]:
                return Return(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Switch", "org.openrewrite.java.tree.J$Switch"]:
                return Switch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.SwitchExpression", "org.openrewrite.java.tree.J$SwitchExpression"]:
                return SwitchExpression(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Synchronized", "org.openrewrite.java.tree.J$Synchronized"]:
                return Synchronized(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Ternary", "org.openrewrite.java.tree.J$Ternary"]:
                return Ternary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.Throw", "org.openrewrite.java.tree.J$Throw"]:
                return Throw(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Try", "org.openrewrite.java.tree.J$Try"]:
                return Try(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.python.tree.Try.Resource", "org.openrewrite.java.tree.J$Try$Resource"]:
                return Try.Resource(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, bool)
                )

            if type in ["rewrite.python.tree.Try.Catch", "org.openrewrite.java.tree.J$Try$Catch"]:
                return Try.Catch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.TypeCast", "org.openrewrite.java.tree.J$TypeCast"]:
                return TypeCast(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.TypeParameter", "org.openrewrite.java.tree.J$TypeParameter"]:
                return TypeParameter(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_container)
                )

            if type in ["rewrite.python.tree.TypeParameters", "org.openrewrite.java.tree.J$TypeParameters"]:
                return TypeParameters(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Unary", "org.openrewrite.java.tree.J$Unary"]:
                return Unary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(Unary.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.python.tree.VariableDeclarations", "org.openrewrite.java.tree.J$VariableDeclarations"]:
                return VariableDeclarations(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_nodes(None, PythonReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_nodes(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.VariableDeclarations.NamedVariable", "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable"]:
                return VariableDeclarations.NamedVariable(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, PythonReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_node(None, PythonReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.python.tree.WhileLoop", "org.openrewrite.java.tree.J$WhileLoop"]:
                return WhileLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, PythonReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.python.tree.Wildcard", "org.openrewrite.java.tree.J$Wildcard"]:
                return Wildcard(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, PythonReceiver.left_padded_value_receiver(Wildcard.Bound)),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Yield", "org.openrewrite.java.tree.J$Yield"]:
                return Yield(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, bool),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Unknown", "org.openrewrite.java.tree.J$Unknown"]:
                return Unknown(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.python.tree.Unknown.Source", "org.openrewrite.java.tree.J$Unknown$Source"]:
                return Unknown.Source(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str)
                )

            if type in ["rewrite.python.tree.Erroneous", "org.openrewrite.java.tree.J$Erroneous"]:
                return Erroneous(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, PythonReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str)
                )

            raise NotImplementedError("No factory method for type: " + type)

    @classmethod
    def receive_method_identifier_with_annotations(cls, before: MethodDeclaration.IdentifierWithAnnotations, type: Optional[str], ctx: ReceiverContext) -> MethodDeclaration.IdentifierWithAnnotations:
        if before is not None:
            before = before.with_identifier(ctx.receive_node(before.identifier, ctx.receive_tree))
            before = before.with_annotations(ctx.receive_nodes(before.annotations, ctx.receive_tree))
        else:
            before = MethodDeclaration.IdentifierWithAnnotations(
                ctx.receive_node(None, ctx.receive_tree),
                ctx.receive_nodes(None, ctx.receive_tree)
            )
        return before

    @classmethod
    def receive_container(cls, container: Optional[JContainer[T]], type: Optional[str], ctx: ReceiverContext) -> JContainer[T]:
        return extensions.receive_container(container, type, ctx)

    @classmethod
    def left_padded_value_receiver(cls, type_: Type) -> Callable[[Optional[JLeftPadded[T]], Optional[str], ReceiverContext], JLeftPadded[T]]:
        return extensions.left_padded_value_receiver(type_)

    @classmethod
    def left_padded_node_receiver(cls, type_: Type) -> Callable[[Optional[JLeftPadded[T]], Optional[str], ReceiverContext], JLeftPadded[T]]:
        return extensions.left_padded_node_receiver(type_)

    @classmethod
    def receive_left_padded_tree(cls, left_padded: Optional[JLeftPadded[T]], type: Optional[str], ctx: ReceiverContext) -> JLeftPadded[T]:
        return extensions.receive_left_padded_tree(left_padded, type, ctx)

    @classmethod
    def right_padded_value_receiver(cls, type_: Type) -> Callable[[Optional[JRightPadded[T]], Optional[str], ReceiverContext], JRightPadded[T]]:
        return extensions.right_padded_value_receiver(type_)

    @classmethod
    def right_padded_node_receiver(cls, type_: Type) -> Callable[[Optional[JRightPadded[T]], Optional[str], ReceiverContext], JRightPadded[T]]:
        return extensions.right_padded_node_receiver(type_)

    @classmethod
    def receive_right_padded_tree(cls, right_padded: Optional[JRightPadded[T]], type: Optional[str], ctx: ReceiverContext) -> JRightPadded[T]:
        return extensions.receive_right_padded_tree(right_padded, type, ctx)

    @classmethod
    def receive_space(cls, space: Optional[Space], type: Optional[str], ctx: ReceiverContext) -> Space:
        return extensions.receive_space(space, type, ctx)
