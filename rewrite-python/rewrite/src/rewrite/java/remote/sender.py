from __future__ import annotations

from operator import attrgetter
from typing import Optional, cast

from rewrite_remote import Sender, SenderContext

from rewrite import Tree, Cursor
from rewrite.java import *
from rewrite.java.support_types import *
from . import extensions


class JavaSender(Sender):
    def send(self, after: T, before: Optional[T], ctx: SenderContext) -> None:
        visitor = self.Visitor()
        visitor.visit(after, ctx.fork(visitor, before))

    class Visitor(JavaVisitor[SenderContext]):
        def visit(self, tree: Optional[Tree], ctx: SenderContext, parent: Optional[Cursor] = None) -> J:
            if parent is not None:
                self.cursor = parent

            self.cursor = Cursor(self.cursor, tree)
            ctx.send_node(tree, lambda x: x, ctx.send_tree)
            self.cursor = self.cursor.parent

            return cast(J, tree)

        def visit_annotated_type(self, annotated_type: AnnotatedType, ctx: SenderContext) -> J:
            ctx.send_value(annotated_type, attrgetter('_id'))
            ctx.send_node(annotated_type, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(annotated_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(annotated_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(annotated_type, attrgetter('_type_expression'), ctx.send_tree)
            return annotated_type

        def visit_annotation(self, annotation: Annotation, ctx: SenderContext) -> J:
            ctx.send_value(annotation, attrgetter('_id'))
            ctx.send_node(annotation, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(annotation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(annotation, attrgetter('_annotation_type'), ctx.send_tree)
            ctx.send_node(annotation, attrgetter('_arguments'), JavaSender.send_container)
            return annotation

        def visit_array_access(self, array_access: ArrayAccess, ctx: SenderContext) -> J:
            ctx.send_value(array_access, attrgetter('_id'))
            ctx.send_node(array_access, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(array_access, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_access, attrgetter('_indexed'), ctx.send_tree)
            ctx.send_node(array_access, attrgetter('_dimension'), ctx.send_tree)
            ctx.send_typed_value(array_access, attrgetter('_type'))
            return array_access

        def visit_array_type(self, array_type: ArrayType, ctx: SenderContext) -> J:
            ctx.send_value(array_type, attrgetter('_id'))
            ctx.send_node(array_type, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(array_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_type, attrgetter('_element_type'), ctx.send_tree)
            ctx.send_nodes(array_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(array_type, attrgetter('_dimension'), JavaSender.send_left_padded)
            ctx.send_typed_value(array_type, attrgetter('_type'))
            return array_type

        def visit_assert(self, assert_: Assert, ctx: SenderContext) -> J:
            ctx.send_value(assert_, attrgetter('_id'))
            ctx.send_node(assert_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(assert_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assert_, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(assert_, attrgetter('_detail'), JavaSender.send_left_padded)
            return assert_

        def visit_assignment(self, assignment: Assignment, ctx: SenderContext) -> J:
            ctx.send_value(assignment, attrgetter('_id'))
            ctx.send_node(assignment, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(assignment, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assignment, attrgetter('_variable'), ctx.send_tree)
            ctx.send_node(assignment, attrgetter('_assignment'), JavaSender.send_left_padded)
            ctx.send_typed_value(assignment, attrgetter('_type'))
            return assignment

        def visit_assignment_operation(self, assignment_operation: AssignmentOperation, ctx: SenderContext) -> J:
            ctx.send_value(assignment_operation, attrgetter('_id'))
            ctx.send_node(assignment_operation, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(assignment_operation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(assignment_operation, attrgetter('_variable'), ctx.send_tree)
            ctx.send_node(assignment_operation, attrgetter('_operator'), JavaSender.send_left_padded)
            ctx.send_node(assignment_operation, attrgetter('_assignment'), ctx.send_tree)
            ctx.send_typed_value(assignment_operation, attrgetter('_type'))
            return assignment_operation

        def visit_binary(self, binary: Binary, ctx: SenderContext) -> J:
            ctx.send_value(binary, attrgetter('_id'))
            ctx.send_node(binary, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(binary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(binary, attrgetter('_left'), ctx.send_tree)
            ctx.send_node(binary, attrgetter('_operator'), JavaSender.send_left_padded)
            ctx.send_node(binary, attrgetter('_right'), ctx.send_tree)
            ctx.send_typed_value(binary, attrgetter('_type'))
            return binary

        def visit_block(self, block: Block, ctx: SenderContext) -> J:
            ctx.send_value(block, attrgetter('_id'))
            ctx.send_node(block, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(block, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(block, attrgetter('_static'), JavaSender.send_right_padded)
            ctx.send_nodes(block, attrgetter('_statements'), JavaSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(block, attrgetter('_end'), JavaSender.send_space)
            return block

        def visit_break(self, break_: Break, ctx: SenderContext) -> J:
            ctx.send_value(break_, attrgetter('_id'))
            ctx.send_node(break_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(break_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(break_, attrgetter('_label'), ctx.send_tree)
            return break_

        def visit_case(self, case: Case, ctx: SenderContext) -> J:
            ctx.send_value(case, attrgetter('_id'))
            ctx.send_node(case, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(case, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(case, attrgetter('_type'))
            ctx.send_node(case, attrgetter('_case_labels'), JavaSender.send_container)
            ctx.send_node(case, attrgetter('_statements'), JavaSender.send_container)
            ctx.send_node(case, attrgetter('_body'), JavaSender.send_right_padded)
            ctx.send_node(case, attrgetter('_guard'), ctx.send_tree)
            return case

        def visit_class_declaration(self, class_declaration: ClassDeclaration, ctx: SenderContext) -> J:
            ctx.send_value(class_declaration, attrgetter('_id'))
            ctx.send_node(class_declaration, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(class_declaration, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(class_declaration, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(class_declaration, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(class_declaration, attrgetter('_kind'), ctx.send_tree)
            ctx.send_node(class_declaration, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(class_declaration, attrgetter('_type_parameters'), JavaSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_primary_constructor'), JavaSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_extends'), JavaSender.send_left_padded)
            ctx.send_node(class_declaration, attrgetter('_implements'), JavaSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_permits'), JavaSender.send_container)
            ctx.send_node(class_declaration, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(class_declaration, attrgetter('_type'))
            return class_declaration

        def visit_class_declaration_kind(self, kind: ClassDeclaration.Kind, ctx: SenderContext) -> J:
            ctx.send_value(kind, attrgetter('_id'))
            ctx.send_node(kind, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(kind, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(kind, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_value(kind, attrgetter('_type'))
            return kind

        def visit_compilation_unit(self, compilation_unit: CompilationUnit, ctx: SenderContext) -> J:
            ctx.send_value(compilation_unit, attrgetter('_id'))
            ctx.send_node(compilation_unit, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(compilation_unit, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(compilation_unit, attrgetter('_source_path'))
            ctx.send_typed_value(compilation_unit, attrgetter('_file_attributes'))
            ctx.send_value(compilation_unit, attrgetter('_charset_name'))
            ctx.send_value(compilation_unit, attrgetter('_charset_bom_marked'))
            ctx.send_typed_value(compilation_unit, attrgetter('_checksum'))
            ctx.send_node(compilation_unit, attrgetter('_package_declaration'), JavaSender.send_right_padded)
            ctx.send_nodes(compilation_unit, attrgetter('_imports'), JavaSender.send_right_padded, lambda t: t.element.id)
            ctx.send_nodes(compilation_unit, attrgetter('_classes'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(compilation_unit, attrgetter('_eof'), JavaSender.send_space)
            return compilation_unit

        def visit_continue(self, continue_: Continue, ctx: SenderContext) -> J:
            ctx.send_value(continue_, attrgetter('_id'))
            ctx.send_node(continue_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(continue_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(continue_, attrgetter('_label'), ctx.send_tree)
            return continue_

        def visit_do_while_loop(self, do_while_loop: DoWhileLoop, ctx: SenderContext) -> J:
            ctx.send_value(do_while_loop, attrgetter('_id'))
            ctx.send_node(do_while_loop, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(do_while_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(do_while_loop, attrgetter('_body'), JavaSender.send_right_padded)
            ctx.send_node(do_while_loop, attrgetter('_while_condition'), JavaSender.send_left_padded)
            return do_while_loop

        def visit_empty(self, empty: Empty, ctx: SenderContext) -> J:
            ctx.send_value(empty, attrgetter('_id'))
            ctx.send_node(empty, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(empty, attrgetter('_markers'), ctx.send_markers)
            return empty

        def visit_enum_value(self, enum_value: EnumValue, ctx: SenderContext) -> J:
            ctx.send_value(enum_value, attrgetter('_id'))
            ctx.send_node(enum_value, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(enum_value, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(enum_value, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(enum_value, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(enum_value, attrgetter('_initializer'), ctx.send_tree)
            return enum_value

        def visit_enum_value_set(self, enum_value_set: EnumValueSet, ctx: SenderContext) -> J:
            ctx.send_value(enum_value_set, attrgetter('_id'))
            ctx.send_node(enum_value_set, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(enum_value_set, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(enum_value_set, attrgetter('_enums'), JavaSender.send_right_padded, lambda t: t.element.id)
            ctx.send_value(enum_value_set, attrgetter('_terminated_with_semicolon'))
            return enum_value_set

        def visit_field_access(self, field_access: FieldAccess, ctx: SenderContext) -> J:
            ctx.send_value(field_access, attrgetter('_id'))
            ctx.send_node(field_access, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(field_access, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(field_access, attrgetter('_target'), ctx.send_tree)
            ctx.send_node(field_access, attrgetter('_name'), JavaSender.send_left_padded)
            ctx.send_typed_value(field_access, attrgetter('_type'))
            return field_access

        def visit_for_each_loop(self, for_each_loop: ForEachLoop, ctx: SenderContext) -> J:
            ctx.send_value(for_each_loop, attrgetter('_id'))
            ctx.send_node(for_each_loop, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(for_each_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(for_each_loop, attrgetter('_control'), ctx.send_tree)
            ctx.send_node(for_each_loop, attrgetter('_body'), JavaSender.send_right_padded)
            return for_each_loop

        def visit_for_each_control(self, control: ForEachLoop.Control, ctx: SenderContext) -> J:
            ctx.send_value(control, attrgetter('_id'))
            ctx.send_node(control, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(control, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(control, attrgetter('_variable'), JavaSender.send_right_padded)
            ctx.send_node(control, attrgetter('_iterable'), JavaSender.send_right_padded)
            return control

        def visit_for_loop(self, for_loop: ForLoop, ctx: SenderContext) -> J:
            ctx.send_value(for_loop, attrgetter('_id'))
            ctx.send_node(for_loop, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(for_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(for_loop, attrgetter('_control'), ctx.send_tree)
            ctx.send_node(for_loop, attrgetter('_body'), JavaSender.send_right_padded)
            return for_loop

        def visit_for_control(self, control: ForLoop.Control, ctx: SenderContext) -> J:
            ctx.send_value(control, attrgetter('_id'))
            ctx.send_node(control, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(control, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(control, attrgetter('_init'), JavaSender.send_right_padded, lambda t: t.element.id)
            ctx.send_node(control, attrgetter('_condition'), JavaSender.send_right_padded)
            ctx.send_nodes(control, attrgetter('_update'), JavaSender.send_right_padded, lambda t: t.element.id)
            return control

        def visit_parenthesized_type_tree(self, parenthesized_type_tree: ParenthesizedTypeTree, ctx: SenderContext) -> J:
            ctx.send_value(parenthesized_type_tree, attrgetter('_id'))
            ctx.send_node(parenthesized_type_tree, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(parenthesized_type_tree, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(parenthesized_type_tree, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(parenthesized_type_tree, attrgetter('_parenthesized_type'), ctx.send_tree)
            return parenthesized_type_tree

        def visit_identifier(self, identifier: Identifier, ctx: SenderContext) -> J:
            ctx.send_value(identifier, attrgetter('_id'))
            ctx.send_node(identifier, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(identifier, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(identifier, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_value(identifier, attrgetter('_simple_name'))
            ctx.send_typed_value(identifier, attrgetter('_type'))
            ctx.send_typed_value(identifier, attrgetter('_field_type'))
            return identifier

        def visit_if(self, if_: If, ctx: SenderContext) -> J:
            ctx.send_value(if_, attrgetter('_id'))
            ctx.send_node(if_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(if_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(if_, attrgetter('_if_condition'), ctx.send_tree)
            ctx.send_node(if_, attrgetter('_then_part'), JavaSender.send_right_padded)
            ctx.send_node(if_, attrgetter('_else_part'), ctx.send_tree)
            return if_

        def visit_else(self, else_: If.Else, ctx: SenderContext) -> J:
            ctx.send_value(else_, attrgetter('_id'))
            ctx.send_node(else_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(else_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(else_, attrgetter('_body'), JavaSender.send_right_padded)
            return else_

        def visit_import(self, import_: Import, ctx: SenderContext) -> J:
            ctx.send_value(import_, attrgetter('_id'))
            ctx.send_node(import_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(import_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(import_, attrgetter('_static'), JavaSender.send_left_padded)
            ctx.send_node(import_, attrgetter('_qualid'), ctx.send_tree)
            ctx.send_node(import_, attrgetter('_alias'), JavaSender.send_left_padded)
            return import_

        def visit_instance_of(self, instance_of: InstanceOf, ctx: SenderContext) -> J:
            ctx.send_value(instance_of, attrgetter('_id'))
            ctx.send_node(instance_of, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(instance_of, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(instance_of, attrgetter('_expression'), JavaSender.send_right_padded)
            ctx.send_node(instance_of, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(instance_of, attrgetter('_pattern'), ctx.send_tree)
            ctx.send_typed_value(instance_of, attrgetter('_type'))
            return instance_of

        def visit_deconstruction_pattern(self, deconstruction_pattern: DeconstructionPattern, ctx: SenderContext) -> J:
            ctx.send_value(deconstruction_pattern, attrgetter('_id'))
            ctx.send_node(deconstruction_pattern, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(deconstruction_pattern, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(deconstruction_pattern, attrgetter('_deconstructor'), ctx.send_tree)
            ctx.send_node(deconstruction_pattern, attrgetter('_nested'), JavaSender.send_container)
            ctx.send_typed_value(deconstruction_pattern, attrgetter('_type'))
            return deconstruction_pattern

        def visit_intersection_type(self, intersection_type: IntersectionType, ctx: SenderContext) -> J:
            ctx.send_value(intersection_type, attrgetter('_id'))
            ctx.send_node(intersection_type, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(intersection_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(intersection_type, attrgetter('_bounds'), JavaSender.send_container)
            return intersection_type

        def visit_label(self, label: Label, ctx: SenderContext) -> J:
            ctx.send_value(label, attrgetter('_id'))
            ctx.send_node(label, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(label, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(label, attrgetter('_label'), JavaSender.send_right_padded)
            ctx.send_node(label, attrgetter('_statement'), ctx.send_tree)
            return label

        def visit_lambda(self, lambda_: Lambda, ctx: SenderContext) -> J:
            ctx.send_value(lambda_, attrgetter('_id'))
            ctx.send_node(lambda_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(lambda_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(lambda_, attrgetter('_parameters'), ctx.send_tree)
            ctx.send_node(lambda_, attrgetter('_arrow'), JavaSender.send_space)
            ctx.send_node(lambda_, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(lambda_, attrgetter('_type'))
            return lambda_

        def visit_lambda_parameters(self, parameters: Lambda.Parameters, ctx: SenderContext) -> J:
            ctx.send_value(parameters, attrgetter('_id'))
            ctx.send_node(parameters, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(parameters, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(parameters, attrgetter('_parenthesized'))
            ctx.send_nodes(parameters, attrgetter('_parameters'), JavaSender.send_right_padded, lambda t: t.element.id)
            return parameters

        def visit_literal(self, literal: Literal, ctx: SenderContext) -> J:
            ctx.send_value(literal, attrgetter('_id'))
            ctx.send_node(literal, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(literal, attrgetter('_markers'), ctx.send_markers)
            ctx.send_typed_value(literal, attrgetter('_value'))
            ctx.send_value(literal, attrgetter('_value_source'))
            ctx.send_values(literal, attrgetter('_unicode_escapes'), lambda x: x)
            ctx.send_value(literal, attrgetter('_type'))
            return literal

        def visit_member_reference(self, member_reference: MemberReference, ctx: SenderContext) -> J:
            ctx.send_value(member_reference, attrgetter('_id'))
            ctx.send_node(member_reference, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(member_reference, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(member_reference, attrgetter('_containing'), JavaSender.send_right_padded)
            ctx.send_node(member_reference, attrgetter('_type_parameters'), JavaSender.send_container)
            ctx.send_node(member_reference, attrgetter('_reference'), JavaSender.send_left_padded)
            ctx.send_typed_value(member_reference, attrgetter('_type'))
            ctx.send_typed_value(member_reference, attrgetter('_method_type'))
            ctx.send_typed_value(member_reference, attrgetter('_variable_type'))
            return member_reference

        def visit_method_declaration(self, method_declaration: MethodDeclaration, ctx: SenderContext) -> J:
            ctx.send_value(method_declaration, attrgetter('_id'))
            ctx.send_node(method_declaration, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(method_declaration, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(method_declaration, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(method_declaration, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(method_declaration, attrgetter('_type_parameters'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_return_type_expression'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_name'), self.send_method_identifier_with_annotations)
            ctx.send_node(method_declaration, attrgetter('_parameters'), JavaSender.send_container)
            ctx.send_node(method_declaration, attrgetter('_throws'), JavaSender.send_container)
            ctx.send_node(method_declaration, attrgetter('_body'), ctx.send_tree)
            ctx.send_node(method_declaration, attrgetter('_default_value'), JavaSender.send_left_padded)
            ctx.send_typed_value(method_declaration, attrgetter('_method_type'))
            return method_declaration

        def send_method_identifier_with_annotations(self, identifier_with_annotations: MethodDeclaration.IdentifierWithAnnotations, ctx: SenderContext) -> None:
            ctx.send_node(identifier_with_annotations, attrgetter('identifier'), ctx.send_tree)
            ctx.send_nodes(identifier_with_annotations, attrgetter('annotations'), ctx.send_tree, attrgetter('id'))

        def visit_method_invocation(self, method_invocation: MethodInvocation, ctx: SenderContext) -> J:
            ctx.send_value(method_invocation, attrgetter('_id'))
            ctx.send_node(method_invocation, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(method_invocation, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(method_invocation, attrgetter('_select'), JavaSender.send_right_padded)
            ctx.send_node(method_invocation, attrgetter('_type_parameters'), JavaSender.send_container)
            ctx.send_node(method_invocation, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(method_invocation, attrgetter('_arguments'), JavaSender.send_container)
            ctx.send_typed_value(method_invocation, attrgetter('_method_type'))
            return method_invocation

        def visit_modifier(self, modifier: Modifier, ctx: SenderContext) -> J:
            ctx.send_value(modifier, attrgetter('_id'))
            ctx.send_node(modifier, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(modifier, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(modifier, attrgetter('_keyword'))
            ctx.send_value(modifier, attrgetter('_type'))
            ctx.send_nodes(modifier, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            return modifier

        def visit_multi_catch(self, multi_catch: MultiCatch, ctx: SenderContext) -> J:
            ctx.send_value(multi_catch, attrgetter('_id'))
            ctx.send_node(multi_catch, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(multi_catch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(multi_catch, attrgetter('_alternatives'), JavaSender.send_right_padded, lambda t: t.element.id)
            return multi_catch

        def visit_new_array(self, new_array: NewArray, ctx: SenderContext) -> J:
            ctx.send_value(new_array, attrgetter('_id'))
            ctx.send_node(new_array, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(new_array, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(new_array, attrgetter('_type_expression'), ctx.send_tree)
            ctx.send_nodes(new_array, attrgetter('_dimensions'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(new_array, attrgetter('_initializer'), JavaSender.send_container)
            ctx.send_typed_value(new_array, attrgetter('_type'))
            return new_array

        def visit_array_dimension(self, array_dimension: ArrayDimension, ctx: SenderContext) -> J:
            ctx.send_value(array_dimension, attrgetter('_id'))
            ctx.send_node(array_dimension, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(array_dimension, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(array_dimension, attrgetter('_index'), JavaSender.send_right_padded)
            return array_dimension

        def visit_new_class(self, new_class: NewClass, ctx: SenderContext) -> J:
            ctx.send_value(new_class, attrgetter('_id'))
            ctx.send_node(new_class, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(new_class, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(new_class, attrgetter('_enclosing'), JavaSender.send_right_padded)
            ctx.send_node(new_class, attrgetter('_new'), JavaSender.send_space)
            ctx.send_node(new_class, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(new_class, attrgetter('_arguments'), JavaSender.send_container)
            ctx.send_node(new_class, attrgetter('_body'), ctx.send_tree)
            ctx.send_typed_value(new_class, attrgetter('_constructor_type'))
            return new_class

        def visit_nullable_type(self, nullable_type: NullableType, ctx: SenderContext) -> J:
            ctx.send_value(nullable_type, attrgetter('_id'))
            ctx.send_node(nullable_type, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(nullable_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(nullable_type, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(nullable_type, attrgetter('_type_tree'), JavaSender.send_right_padded)
            return nullable_type

        def visit_package(self, package: Package, ctx: SenderContext) -> J:
            ctx.send_value(package, attrgetter('_id'))
            ctx.send_node(package, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(package, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(package, attrgetter('_expression'), ctx.send_tree)
            ctx.send_nodes(package, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            return package

        def visit_parameterized_type(self, parameterized_type: ParameterizedType, ctx: SenderContext) -> J:
            ctx.send_value(parameterized_type, attrgetter('_id'))
            ctx.send_node(parameterized_type, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(parameterized_type, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(parameterized_type, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(parameterized_type, attrgetter('_type_parameters'), JavaSender.send_container)
            ctx.send_typed_value(parameterized_type, attrgetter('_type'))
            return parameterized_type

        def visit_parentheses(self, parentheses: Parentheses[J2], ctx: SenderContext) -> J:
            ctx.send_value(parentheses, attrgetter('_id'))
            ctx.send_node(parentheses, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(parentheses, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(parentheses, attrgetter('_tree'), JavaSender.send_right_padded)
            return parentheses

        def visit_control_parentheses(self, control_parentheses: ControlParentheses[J2], ctx: SenderContext) -> J:
            ctx.send_value(control_parentheses, attrgetter('_id'))
            ctx.send_node(control_parentheses, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(control_parentheses, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(control_parentheses, attrgetter('_tree'), JavaSender.send_right_padded)
            return control_parentheses

        def visit_primitive(self, primitive: Primitive, ctx: SenderContext) -> J:
            ctx.send_value(primitive, attrgetter('_id'))
            ctx.send_node(primitive, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(primitive, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(primitive, attrgetter('_type'))
            return primitive

        def visit_return(self, return_: Return, ctx: SenderContext) -> J:
            ctx.send_value(return_, attrgetter('_id'))
            ctx.send_node(return_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(return_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(return_, attrgetter('_expression'), ctx.send_tree)
            return return_

        def visit_switch(self, switch: Switch, ctx: SenderContext) -> J:
            ctx.send_value(switch, attrgetter('_id'))
            ctx.send_node(switch, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(switch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(switch, attrgetter('_selector'), ctx.send_tree)
            ctx.send_node(switch, attrgetter('_cases'), ctx.send_tree)
            return switch

        def visit_switch_expression(self, switch_expression: SwitchExpression, ctx: SenderContext) -> J:
            ctx.send_value(switch_expression, attrgetter('_id'))
            ctx.send_node(switch_expression, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(switch_expression, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(switch_expression, attrgetter('_selector'), ctx.send_tree)
            ctx.send_node(switch_expression, attrgetter('_cases'), ctx.send_tree)
            ctx.send_typed_value(switch_expression, attrgetter('_type'))
            return switch_expression

        def visit_synchronized(self, synchronized: Synchronized, ctx: SenderContext) -> J:
            ctx.send_value(synchronized, attrgetter('_id'))
            ctx.send_node(synchronized, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(synchronized, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(synchronized, attrgetter('_lock'), ctx.send_tree)
            ctx.send_node(synchronized, attrgetter('_body'), ctx.send_tree)
            return synchronized

        def visit_ternary(self, ternary: Ternary, ctx: SenderContext) -> J:
            ctx.send_value(ternary, attrgetter('_id'))
            ctx.send_node(ternary, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(ternary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(ternary, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(ternary, attrgetter('_true_part'), JavaSender.send_left_padded)
            ctx.send_node(ternary, attrgetter('_false_part'), JavaSender.send_left_padded)
            ctx.send_typed_value(ternary, attrgetter('_type'))
            return ternary

        def visit_throw(self, throw: Throw, ctx: SenderContext) -> J:
            ctx.send_value(throw, attrgetter('_id'))
            ctx.send_node(throw, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(throw, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(throw, attrgetter('_exception'), ctx.send_tree)
            return throw

        def visit_try(self, try_: Try, ctx: SenderContext) -> J:
            ctx.send_value(try_, attrgetter('_id'))
            ctx.send_node(try_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(try_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(try_, attrgetter('_resources'), JavaSender.send_container)
            ctx.send_node(try_, attrgetter('_body'), ctx.send_tree)
            ctx.send_nodes(try_, attrgetter('_catches'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(try_, attrgetter('_finally'), JavaSender.send_left_padded)
            return try_

        def visit_try_resource(self, resource: Try.Resource, ctx: SenderContext) -> J:
            ctx.send_value(resource, attrgetter('_id'))
            ctx.send_node(resource, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(resource, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(resource, attrgetter('_variable_declarations'), ctx.send_tree)
            ctx.send_value(resource, attrgetter('_terminated_with_semicolon'))
            return resource

        def visit_catch(self, catch: Try.Catch, ctx: SenderContext) -> J:
            ctx.send_value(catch, attrgetter('_id'))
            ctx.send_node(catch, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(catch, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(catch, attrgetter('_parameter'), ctx.send_tree)
            ctx.send_node(catch, attrgetter('_body'), ctx.send_tree)
            return catch

        def visit_type_cast(self, type_cast: TypeCast, ctx: SenderContext) -> J:
            ctx.send_value(type_cast, attrgetter('_id'))
            ctx.send_node(type_cast, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(type_cast, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(type_cast, attrgetter('_clazz'), ctx.send_tree)
            ctx.send_node(type_cast, attrgetter('_expression'), ctx.send_tree)
            return type_cast

        def visit_type_parameter(self, type_parameter: TypeParameter, ctx: SenderContext) -> J:
            ctx.send_value(type_parameter, attrgetter('_id'))
            ctx.send_node(type_parameter, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(type_parameter, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(type_parameter, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(type_parameter, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(type_parameter, attrgetter('_name'), ctx.send_tree)
            ctx.send_node(type_parameter, attrgetter('_bounds'), JavaSender.send_container)
            return type_parameter

        def visit_type_parameters(self, type_parameters: TypeParameters, ctx: SenderContext) -> J:
            ctx.send_value(type_parameters, attrgetter('_id'))
            ctx.send_node(type_parameters, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(type_parameters, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(type_parameters, attrgetter('_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(type_parameters, attrgetter('_type_parameters'), JavaSender.send_right_padded, lambda t: t.element.id)
            return type_parameters

        def visit_unary(self, unary: Unary, ctx: SenderContext) -> J:
            ctx.send_value(unary, attrgetter('_id'))
            ctx.send_node(unary, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(unary, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(unary, attrgetter('_operator'), JavaSender.send_left_padded)
            ctx.send_node(unary, attrgetter('_expression'), ctx.send_tree)
            ctx.send_typed_value(unary, attrgetter('_type'))
            return unary

        def visit_variable_declarations(self, variable_declarations: VariableDeclarations, ctx: SenderContext) -> J:
            ctx.send_value(variable_declarations, attrgetter('_id'))
            ctx.send_node(variable_declarations, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(variable_declarations, attrgetter('_markers'), ctx.send_markers)
            ctx.send_nodes(variable_declarations, attrgetter('_leading_annotations'), ctx.send_tree, attrgetter('id'))
            ctx.send_nodes(variable_declarations, attrgetter('_modifiers'), ctx.send_tree, attrgetter('id'))
            ctx.send_node(variable_declarations, attrgetter('_type_expression'), ctx.send_tree)
            ctx.send_node(variable_declarations, attrgetter('_varargs'), JavaSender.send_space)
            ctx.send_nodes(variable_declarations, attrgetter('_dimensions_before_name'), JavaSender.send_left_padded, lambda t: t)
            ctx.send_nodes(variable_declarations, attrgetter('_variables'), JavaSender.send_right_padded, lambda t: t.element.id)
            return variable_declarations

        def visit_variable(self, named_variable: VariableDeclarations.NamedVariable, ctx: SenderContext) -> J:
            ctx.send_value(named_variable, attrgetter('_id'))
            ctx.send_node(named_variable, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(named_variable, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(named_variable, attrgetter('_name'), ctx.send_tree)
            ctx.send_nodes(named_variable, attrgetter('_dimensions_after_name'), JavaSender.send_left_padded, lambda t: t)
            ctx.send_node(named_variable, attrgetter('_initializer'), JavaSender.send_left_padded)
            ctx.send_typed_value(named_variable, attrgetter('_variable_type'))
            return named_variable

        def visit_while_loop(self, while_loop: WhileLoop, ctx: SenderContext) -> J:
            ctx.send_value(while_loop, attrgetter('_id'))
            ctx.send_node(while_loop, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(while_loop, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(while_loop, attrgetter('_condition'), ctx.send_tree)
            ctx.send_node(while_loop, attrgetter('_body'), JavaSender.send_right_padded)
            return while_loop

        def visit_wildcard(self, wildcard: Wildcard, ctx: SenderContext) -> J:
            ctx.send_value(wildcard, attrgetter('_id'))
            ctx.send_node(wildcard, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(wildcard, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(wildcard, attrgetter('_bound'), JavaSender.send_left_padded)
            ctx.send_node(wildcard, attrgetter('_bounded_type'), ctx.send_tree)
            return wildcard

        def visit_yield(self, yield_: Yield, ctx: SenderContext) -> J:
            ctx.send_value(yield_, attrgetter('_id'))
            ctx.send_node(yield_, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(yield_, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(yield_, attrgetter('_implicit'))
            ctx.send_node(yield_, attrgetter('_value'), ctx.send_tree)
            return yield_

        def visit_unknown(self, unknown: Unknown, ctx: SenderContext) -> J:
            ctx.send_value(unknown, attrgetter('_id'))
            ctx.send_node(unknown, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(unknown, attrgetter('_markers'), ctx.send_markers)
            ctx.send_node(unknown, attrgetter('_source'), ctx.send_tree)
            return unknown

        def visit_unknown_source(self, source: Unknown.Source, ctx: SenderContext) -> J:
            ctx.send_value(source, attrgetter('_id'))
            ctx.send_node(source, attrgetter('_prefix'), JavaSender.send_space)
            ctx.send_node(source, attrgetter('_markers'), ctx.send_markers)
            ctx.send_value(source, attrgetter('_text'))
            return source

        def visit_erroneous(self, erroneous: Erroneous, ctx: SenderContext) -> J:
            ctx.send_value(erroneous, attrgetter('_id'))
            ctx.send_node(erroneous, attrgetter('_prefix'), JavaSender.send_space)
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
