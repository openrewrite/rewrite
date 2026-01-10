from pathlib import Path
from typing import Optional, Any, cast, Type, Callable
from uuid import UUID

from rewrite_remote import Receiver, ReceiverContext, T, ReceiverFactory

from rewrite import Cursor, Checksum, FileAttributes, Tree
from rewrite.java import *
from rewrite.java.support_types import *
from rewrite.java.visitor import JavaVisitor
from . import extensions


class JavaReceiver(Receiver):
    def fork(self, ctx: ReceiverContext) -> ReceiverContext:
        return ctx.fork(self.Visitor(), self.Factory())

    def receive(self, before: Optional[T], ctx: ReceiverContext) -> Any:
        forked = self.fork(ctx)
        return forked.visitor.visit(before, forked)

    # noinspection DuplicatedCode,PyMethodFirstArgAssignment,PyTypeChecker
    class Visitor(JavaVisitor[ReceiverContext]):
        def visit(self, tree: Optional[Tree], ctx: ReceiverContext, parent: Optional[Cursor] = None) -> Optional[J]:
            if parent is not None:
                self.cursor = parent

            self.cursor = Cursor(self.cursor, tree)

            tree = ctx.receive_node(cast(J, tree), ctx.receive_tree)

            self.cursor = self.cursor.parent
            return cast(J, tree)

        def visit_annotated_type(self, annotated_type: AnnotatedType, ctx: ReceiverContext) -> J:
            annotated_type = annotated_type.with_id(ctx.receive_value(annotated_type.id, UUID))
            annotated_type = annotated_type.with_prefix(ctx.receive_node(annotated_type.prefix, JavaReceiver.receive_space))
            annotated_type = annotated_type.with_markers(ctx.receive_node(annotated_type.markers, ctx.receive_markers))
            annotated_type = annotated_type.with_annotations(ctx.receive_nodes(annotated_type.annotations, ctx.receive_tree))
            annotated_type = annotated_type.with_type_expression(ctx.receive_node(annotated_type.type_expression, ctx.receive_tree))
            return annotated_type

        def visit_annotation(self, annotation: Annotation, ctx: ReceiverContext) -> J:
            annotation = annotation.with_id(ctx.receive_value(annotation.id, UUID))
            annotation = annotation.with_prefix(ctx.receive_node(annotation.prefix, JavaReceiver.receive_space))
            annotation = annotation.with_markers(ctx.receive_node(annotation.markers, ctx.receive_markers))
            annotation = annotation.with_annotation_type(ctx.receive_node(annotation.annotation_type, ctx.receive_tree))
            annotation = annotation.padding.with_arguments(ctx.receive_node(annotation.padding.arguments, JavaReceiver.receive_container))
            return annotation

        def visit_array_access(self, array_access: ArrayAccess, ctx: ReceiverContext) -> J:
            array_access = array_access.with_id(ctx.receive_value(array_access.id, UUID))
            array_access = array_access.with_prefix(ctx.receive_node(array_access.prefix, JavaReceiver.receive_space))
            array_access = array_access.with_markers(ctx.receive_node(array_access.markers, ctx.receive_markers))
            array_access = array_access.with_indexed(ctx.receive_node(array_access.indexed, ctx.receive_tree))
            array_access = array_access.with_dimension(ctx.receive_node(array_access.dimension, ctx.receive_tree))
            array_access = array_access.with_type(ctx.receive_value(array_access.type, JavaType))
            return array_access

        def visit_array_type(self, array_type: ArrayType, ctx: ReceiverContext) -> J:
            array_type = array_type.with_id(ctx.receive_value(array_type.id, UUID))
            array_type = array_type.with_prefix(ctx.receive_node(array_type.prefix, JavaReceiver.receive_space))
            array_type = array_type.with_markers(ctx.receive_node(array_type.markers, ctx.receive_markers))
            array_type = array_type.with_element_type(ctx.receive_node(array_type.element_type, ctx.receive_tree))
            array_type = array_type.with_annotations(ctx.receive_nodes(array_type.annotations, ctx.receive_tree))
            array_type = array_type.with_dimension(ctx.receive_node(array_type.dimension, JavaReceiver.left_padded_node_receiver(Space)))
            array_type = array_type.with_type(ctx.receive_value(array_type.type, JavaType))
            return array_type

        def visit_assert(self, assert_: Assert, ctx: ReceiverContext) -> J:
            assert_ = assert_.with_id(ctx.receive_value(assert_.id, UUID))
            assert_ = assert_.with_prefix(ctx.receive_node(assert_.prefix, JavaReceiver.receive_space))
            assert_ = assert_.with_markers(ctx.receive_node(assert_.markers, ctx.receive_markers))
            assert_ = assert_.with_condition(ctx.receive_node(assert_.condition, ctx.receive_tree))
            assert_ = assert_.with_detail(ctx.receive_node(assert_.detail, JavaReceiver.receive_left_padded_tree))
            return assert_

        def visit_assignment(self, assignment: Assignment, ctx: ReceiverContext) -> J:
            assignment = assignment.with_id(ctx.receive_value(assignment.id, UUID))
            assignment = assignment.with_prefix(ctx.receive_node(assignment.prefix, JavaReceiver.receive_space))
            assignment = assignment.with_markers(ctx.receive_node(assignment.markers, ctx.receive_markers))
            assignment = assignment.with_variable(ctx.receive_node(assignment.variable, ctx.receive_tree))
            assignment = assignment.padding.with_assignment(ctx.receive_node(assignment.padding.assignment, JavaReceiver.receive_left_padded_tree))
            assignment = assignment.with_type(ctx.receive_value(assignment.type, JavaType))
            return assignment

        def visit_assignment_operation(self, assignment_operation: AssignmentOperation, ctx: ReceiverContext) -> J:
            assignment_operation = assignment_operation.with_id(ctx.receive_value(assignment_operation.id, UUID))
            assignment_operation = assignment_operation.with_prefix(ctx.receive_node(assignment_operation.prefix, JavaReceiver.receive_space))
            assignment_operation = assignment_operation.with_markers(ctx.receive_node(assignment_operation.markers, ctx.receive_markers))
            assignment_operation = assignment_operation.with_variable(ctx.receive_node(assignment_operation.variable, ctx.receive_tree))
            assignment_operation = assignment_operation.padding.with_operator(ctx.receive_node(assignment_operation.padding.operator, JavaReceiver.left_padded_value_receiver(AssignmentOperation.Type)))
            assignment_operation = assignment_operation.with_assignment(ctx.receive_node(assignment_operation.assignment, ctx.receive_tree))
            assignment_operation = assignment_operation.with_type(ctx.receive_value(assignment_operation.type, JavaType))
            return assignment_operation

        def visit_binary(self, binary: Binary, ctx: ReceiverContext) -> J:
            binary = binary.with_id(ctx.receive_value(binary.id, UUID))
            binary = binary.with_prefix(ctx.receive_node(binary.prefix, JavaReceiver.receive_space))
            binary = binary.with_markers(ctx.receive_node(binary.markers, ctx.receive_markers))
            binary = binary.with_left(ctx.receive_node(binary.left, ctx.receive_tree))
            binary = binary.padding.with_operator(ctx.receive_node(binary.padding.operator, JavaReceiver.left_padded_value_receiver(Binary.Type)))
            binary = binary.with_right(ctx.receive_node(binary.right, ctx.receive_tree))
            binary = binary.with_type(ctx.receive_value(binary.type, JavaType))
            return binary

        def visit_block(self, block: Block, ctx: ReceiverContext) -> J:
            block = block.with_id(ctx.receive_value(block.id, UUID))
            block = block.with_prefix(ctx.receive_node(block.prefix, JavaReceiver.receive_space))
            block = block.with_markers(ctx.receive_node(block.markers, ctx.receive_markers))
            block = block.padding.with_static(ctx.receive_node(block.padding.static, JavaReceiver.right_padded_value_receiver(bool)))
            block = block.padding.with_statements(ctx.receive_nodes(block.padding.statements, JavaReceiver.receive_right_padded_tree))
            block = block.with_end(ctx.receive_node(block.end, JavaReceiver.receive_space))
            return block

        def visit_break(self, break_: Break, ctx: ReceiverContext) -> J:
            break_ = break_.with_id(ctx.receive_value(break_.id, UUID))
            break_ = break_.with_prefix(ctx.receive_node(break_.prefix, JavaReceiver.receive_space))
            break_ = break_.with_markers(ctx.receive_node(break_.markers, ctx.receive_markers))
            break_ = break_.with_label(ctx.receive_node(break_.label, ctx.receive_tree))
            return break_

        def visit_case(self, case: Case, ctx: ReceiverContext) -> J:
            case = case.with_id(ctx.receive_value(case.id, UUID))
            case = case.with_prefix(ctx.receive_node(case.prefix, JavaReceiver.receive_space))
            case = case.with_markers(ctx.receive_node(case.markers, ctx.receive_markers))
            case = case.with_type(ctx.receive_value(case.type, Case.Type))
            case = case.padding.with_case_labels(ctx.receive_node(case.padding.case_labels, JavaReceiver.receive_container))
            case = case.padding.with_statements(ctx.receive_node(case.padding.statements, JavaReceiver.receive_container))
            case = case.padding.with_body(ctx.receive_node(case.padding.body, JavaReceiver.receive_right_padded_tree))
            case = case.with_guard(ctx.receive_node(case.guard, ctx.receive_tree))
            return case

        def visit_class_declaration(self, class_declaration: ClassDeclaration, ctx: ReceiverContext) -> J:
            class_declaration = class_declaration.with_id(ctx.receive_value(class_declaration.id, UUID))
            class_declaration = class_declaration.with_prefix(ctx.receive_node(class_declaration.prefix, JavaReceiver.receive_space))
            class_declaration = class_declaration.with_markers(ctx.receive_node(class_declaration.markers, ctx.receive_markers))
            class_declaration = class_declaration.with_leading_annotations(ctx.receive_nodes(class_declaration.leading_annotations, ctx.receive_tree))
            class_declaration = class_declaration.with_modifiers(ctx.receive_nodes(class_declaration.modifiers, ctx.receive_tree))
            class_declaration = class_declaration.padding.with_kind(ctx.receive_node(class_declaration.padding.kind, ctx.receive_tree))
            class_declaration = class_declaration.with_name(ctx.receive_node(class_declaration.name, ctx.receive_tree))
            class_declaration = class_declaration.padding.with_type_parameters(ctx.receive_node(class_declaration.padding.type_parameters, JavaReceiver.receive_container))
            class_declaration = class_declaration.padding.with_primary_constructor(ctx.receive_node(class_declaration.padding.primary_constructor, JavaReceiver.receive_container))
            class_declaration = class_declaration.padding.with_extends(ctx.receive_node(class_declaration.padding.extends, JavaReceiver.receive_left_padded_tree))
            class_declaration = class_declaration.padding.with_implements(ctx.receive_node(class_declaration.padding.implements, JavaReceiver.receive_container))
            class_declaration = class_declaration.padding.with_permits(ctx.receive_node(class_declaration.padding.permits, JavaReceiver.receive_container))
            class_declaration = class_declaration.with_body(ctx.receive_node(class_declaration.body, ctx.receive_tree))
            class_declaration = class_declaration.with_type(ctx.receive_value(class_declaration.type, JavaType.FullyQualified))
            return class_declaration

        def visit_class_declaration_kind(self, kind: ClassDeclaration.Kind, ctx: ReceiverContext) -> J:
            kind = kind.with_id(ctx.receive_value(kind.id, UUID))
            kind = kind.with_prefix(ctx.receive_node(kind.prefix, JavaReceiver.receive_space))
            kind = kind.with_markers(ctx.receive_node(kind.markers, ctx.receive_markers))
            kind = kind.with_annotations(ctx.receive_nodes(kind.annotations, ctx.receive_tree))
            kind = kind.with_type(ctx.receive_value(kind.type, ClassDeclaration.Kind.Type))
            return kind

        def visit_compilation_unit(self, compilation_unit: CompilationUnit, ctx: ReceiverContext) -> J:
            compilation_unit = compilation_unit.with_id(ctx.receive_value(compilation_unit.id, UUID))
            compilation_unit = compilation_unit.with_prefix(ctx.receive_node(compilation_unit.prefix, JavaReceiver.receive_space))
            compilation_unit = compilation_unit.with_markers(ctx.receive_node(compilation_unit.markers, ctx.receive_markers))
            compilation_unit = compilation_unit.with_source_path(ctx.receive_value(compilation_unit.source_path, Path))
            compilation_unit = compilation_unit.with_file_attributes(ctx.receive_value(compilation_unit.file_attributes, FileAttributes))
            compilation_unit = compilation_unit.with_charset_name(ctx.receive_value(compilation_unit.charset_name, str))
            compilation_unit = compilation_unit.with_charset_bom_marked(ctx.receive_value(compilation_unit.charset_bom_marked, bool))
            compilation_unit = compilation_unit.with_checksum(ctx.receive_value(compilation_unit.checksum, Checksum))
            compilation_unit = compilation_unit.padding.with_package_declaration(ctx.receive_node(compilation_unit.padding.package_declaration, JavaReceiver.receive_right_padded_tree))
            compilation_unit = compilation_unit.padding.with_imports(ctx.receive_nodes(compilation_unit.padding.imports, JavaReceiver.receive_right_padded_tree))
            compilation_unit = compilation_unit.with_classes(ctx.receive_nodes(compilation_unit.classes, ctx.receive_tree))
            compilation_unit = compilation_unit.with_eof(ctx.receive_node(compilation_unit.eof, JavaReceiver.receive_space))
            return compilation_unit

        def visit_continue(self, continue_: Continue, ctx: ReceiverContext) -> J:
            continue_ = continue_.with_id(ctx.receive_value(continue_.id, UUID))
            continue_ = continue_.with_prefix(ctx.receive_node(continue_.prefix, JavaReceiver.receive_space))
            continue_ = continue_.with_markers(ctx.receive_node(continue_.markers, ctx.receive_markers))
            continue_ = continue_.with_label(ctx.receive_node(continue_.label, ctx.receive_tree))
            return continue_

        def visit_do_while_loop(self, do_while_loop: DoWhileLoop, ctx: ReceiverContext) -> J:
            do_while_loop = do_while_loop.with_id(ctx.receive_value(do_while_loop.id, UUID))
            do_while_loop = do_while_loop.with_prefix(ctx.receive_node(do_while_loop.prefix, JavaReceiver.receive_space))
            do_while_loop = do_while_loop.with_markers(ctx.receive_node(do_while_loop.markers, ctx.receive_markers))
            do_while_loop = do_while_loop.padding.with_body(ctx.receive_node(do_while_loop.padding.body, JavaReceiver.receive_right_padded_tree))
            do_while_loop = do_while_loop.padding.with_while_condition(ctx.receive_node(do_while_loop.padding.while_condition, JavaReceiver.receive_left_padded_tree))
            return do_while_loop

        def visit_empty(self, empty: Empty, ctx: ReceiverContext) -> J:
            empty = empty.with_id(ctx.receive_value(empty.id, UUID))
            empty = empty.with_prefix(ctx.receive_node(empty.prefix, JavaReceiver.receive_space))
            empty = empty.with_markers(ctx.receive_node(empty.markers, ctx.receive_markers))
            return empty

        def visit_enum_value(self, enum_value: EnumValue, ctx: ReceiverContext) -> J:
            enum_value = enum_value.with_id(ctx.receive_value(enum_value.id, UUID))
            enum_value = enum_value.with_prefix(ctx.receive_node(enum_value.prefix, JavaReceiver.receive_space))
            enum_value = enum_value.with_markers(ctx.receive_node(enum_value.markers, ctx.receive_markers))
            enum_value = enum_value.with_annotations(ctx.receive_nodes(enum_value.annotations, ctx.receive_tree))
            enum_value = enum_value.with_name(ctx.receive_node(enum_value.name, ctx.receive_tree))
            enum_value = enum_value.with_initializer(ctx.receive_node(enum_value.initializer, ctx.receive_tree))
            return enum_value

        def visit_enum_value_set(self, enum_value_set: EnumValueSet, ctx: ReceiverContext) -> J:
            enum_value_set = enum_value_set.with_id(ctx.receive_value(enum_value_set.id, UUID))
            enum_value_set = enum_value_set.with_prefix(ctx.receive_node(enum_value_set.prefix, JavaReceiver.receive_space))
            enum_value_set = enum_value_set.with_markers(ctx.receive_node(enum_value_set.markers, ctx.receive_markers))
            enum_value_set = enum_value_set.padding.with_enums(ctx.receive_nodes(enum_value_set.padding.enums, JavaReceiver.receive_right_padded_tree))
            enum_value_set = enum_value_set.with_terminated_with_semicolon(ctx.receive_value(enum_value_set.terminated_with_semicolon, bool))
            return enum_value_set

        def visit_field_access(self, field_access: FieldAccess, ctx: ReceiverContext) -> J:
            field_access = field_access.with_id(ctx.receive_value(field_access.id, UUID))
            field_access = field_access.with_prefix(ctx.receive_node(field_access.prefix, JavaReceiver.receive_space))
            field_access = field_access.with_markers(ctx.receive_node(field_access.markers, ctx.receive_markers))
            field_access = field_access.with_target(ctx.receive_node(field_access.target, ctx.receive_tree))
            field_access = field_access.padding.with_name(ctx.receive_node(field_access.padding.name, JavaReceiver.receive_left_padded_tree))
            field_access = field_access.with_type(ctx.receive_value(field_access.type, JavaType))
            return field_access

        def visit_for_each_loop(self, for_each_loop: ForEachLoop, ctx: ReceiverContext) -> J:
            for_each_loop = for_each_loop.with_id(ctx.receive_value(for_each_loop.id, UUID))
            for_each_loop = for_each_loop.with_prefix(ctx.receive_node(for_each_loop.prefix, JavaReceiver.receive_space))
            for_each_loop = for_each_loop.with_markers(ctx.receive_node(for_each_loop.markers, ctx.receive_markers))
            for_each_loop = for_each_loop.with_control(ctx.receive_node(for_each_loop.control, ctx.receive_tree))
            for_each_loop = for_each_loop.padding.with_body(ctx.receive_node(for_each_loop.padding.body, JavaReceiver.receive_right_padded_tree))
            return for_each_loop

        def visit_for_each_control(self, control: ForEachLoop.Control, ctx: ReceiverContext) -> J:
            control = control.with_id(ctx.receive_value(control.id, UUID))
            control = control.with_prefix(ctx.receive_node(control.prefix, JavaReceiver.receive_space))
            control = control.with_markers(ctx.receive_node(control.markers, ctx.receive_markers))
            control = control.padding.with_variable(ctx.receive_node(control.padding.variable, JavaReceiver.receive_right_padded_tree))
            control = control.padding.with_iterable(ctx.receive_node(control.padding.iterable, JavaReceiver.receive_right_padded_tree))
            return control

        def visit_for_loop(self, for_loop: ForLoop, ctx: ReceiverContext) -> J:
            for_loop = for_loop.with_id(ctx.receive_value(for_loop.id, UUID))
            for_loop = for_loop.with_prefix(ctx.receive_node(for_loop.prefix, JavaReceiver.receive_space))
            for_loop = for_loop.with_markers(ctx.receive_node(for_loop.markers, ctx.receive_markers))
            for_loop = for_loop.with_control(ctx.receive_node(for_loop.control, ctx.receive_tree))
            for_loop = for_loop.padding.with_body(ctx.receive_node(for_loop.padding.body, JavaReceiver.receive_right_padded_tree))
            return for_loop

        def visit_for_control(self, control: ForLoop.Control, ctx: ReceiverContext) -> J:
            control = control.with_id(ctx.receive_value(control.id, UUID))
            control = control.with_prefix(ctx.receive_node(control.prefix, JavaReceiver.receive_space))
            control = control.with_markers(ctx.receive_node(control.markers, ctx.receive_markers))
            control = control.padding.with_init(ctx.receive_nodes(control.padding.init, JavaReceiver.receive_right_padded_tree))
            control = control.padding.with_condition(ctx.receive_node(control.padding.condition, JavaReceiver.receive_right_padded_tree))
            control = control.padding.with_update(ctx.receive_nodes(control.padding.update, JavaReceiver.receive_right_padded_tree))
            return control

        def visit_parenthesized_type_tree(self, parenthesized_type_tree: ParenthesizedTypeTree, ctx: ReceiverContext) -> J:
            parenthesized_type_tree = parenthesized_type_tree.with_id(ctx.receive_value(parenthesized_type_tree.id, UUID))
            parenthesized_type_tree = parenthesized_type_tree.with_prefix(ctx.receive_node(parenthesized_type_tree.prefix, JavaReceiver.receive_space))
            parenthesized_type_tree = parenthesized_type_tree.with_markers(ctx.receive_node(parenthesized_type_tree.markers, ctx.receive_markers))
            parenthesized_type_tree = parenthesized_type_tree.with_annotations(ctx.receive_nodes(parenthesized_type_tree.annotations, ctx.receive_tree))
            parenthesized_type_tree = parenthesized_type_tree.with_parenthesized_type(ctx.receive_node(parenthesized_type_tree.parenthesized_type, ctx.receive_tree))
            return parenthesized_type_tree

        def visit_identifier(self, identifier: Identifier, ctx: ReceiverContext) -> J:
            identifier = identifier.with_id(ctx.receive_value(identifier.id, UUID))
            identifier = identifier.with_prefix(ctx.receive_node(identifier.prefix, JavaReceiver.receive_space))
            identifier = identifier.with_markers(ctx.receive_node(identifier.markers, ctx.receive_markers))
            identifier = identifier.with_annotations(ctx.receive_nodes(identifier.annotations, ctx.receive_tree))
            identifier = identifier.with_simple_name(ctx.receive_value(identifier.simple_name, str))
            identifier = identifier.with_type(ctx.receive_value(identifier.type, JavaType))
            identifier = identifier.with_field_type(ctx.receive_value(identifier.field_type, JavaType.Variable))
            return identifier

        def visit_if(self, if_: If, ctx: ReceiverContext) -> J:
            if_ = if_.with_id(ctx.receive_value(if_.id, UUID))
            if_ = if_.with_prefix(ctx.receive_node(if_.prefix, JavaReceiver.receive_space))
            if_ = if_.with_markers(ctx.receive_node(if_.markers, ctx.receive_markers))
            if_ = if_.with_if_condition(ctx.receive_node(if_.if_condition, ctx.receive_tree))
            if_ = if_.padding.with_then_part(ctx.receive_node(if_.padding.then_part, JavaReceiver.receive_right_padded_tree))
            if_ = if_.with_else_part(ctx.receive_node(if_.else_part, ctx.receive_tree))
            return if_

        def visit_else(self, else_: If.Else, ctx: ReceiverContext) -> J:
            else_ = else_.with_id(ctx.receive_value(else_.id, UUID))
            else_ = else_.with_prefix(ctx.receive_node(else_.prefix, JavaReceiver.receive_space))
            else_ = else_.with_markers(ctx.receive_node(else_.markers, ctx.receive_markers))
            else_ = else_.padding.with_body(ctx.receive_node(else_.padding.body, JavaReceiver.receive_right_padded_tree))
            return else_

        def visit_import(self, import_: Import, ctx: ReceiverContext) -> J:
            import_ = import_.with_id(ctx.receive_value(import_.id, UUID))
            import_ = import_.with_prefix(ctx.receive_node(import_.prefix, JavaReceiver.receive_space))
            import_ = import_.with_markers(ctx.receive_node(import_.markers, ctx.receive_markers))
            import_ = import_.padding.with_static(ctx.receive_node(import_.padding.static, JavaReceiver.left_padded_value_receiver(bool)))
            import_ = import_.with_qualid(ctx.receive_node(import_.qualid, ctx.receive_tree))
            import_ = import_.padding.with_alias(ctx.receive_node(import_.padding.alias, JavaReceiver.receive_left_padded_tree))
            return import_

        def visit_instance_of(self, instance_of: InstanceOf, ctx: ReceiverContext) -> J:
            instance_of = instance_of.with_id(ctx.receive_value(instance_of.id, UUID))
            instance_of = instance_of.with_prefix(ctx.receive_node(instance_of.prefix, JavaReceiver.receive_space))
            instance_of = instance_of.with_markers(ctx.receive_node(instance_of.markers, ctx.receive_markers))
            instance_of = instance_of.padding.with_expression(ctx.receive_node(instance_of.padding.expression, JavaReceiver.receive_right_padded_tree))
            instance_of = instance_of.with_clazz(ctx.receive_node(instance_of.clazz, ctx.receive_tree))
            instance_of = instance_of.with_pattern(ctx.receive_node(instance_of.pattern, ctx.receive_tree))
            instance_of = instance_of.with_type(ctx.receive_value(instance_of.type, JavaType))
            return instance_of

        def visit_deconstruction_pattern(self, deconstruction_pattern: DeconstructionPattern, ctx: ReceiverContext) -> J:
            deconstruction_pattern = deconstruction_pattern.with_id(ctx.receive_value(deconstruction_pattern.id, UUID))
            deconstruction_pattern = deconstruction_pattern.with_prefix(ctx.receive_node(deconstruction_pattern.prefix, JavaReceiver.receive_space))
            deconstruction_pattern = deconstruction_pattern.with_markers(ctx.receive_node(deconstruction_pattern.markers, ctx.receive_markers))
            deconstruction_pattern = deconstruction_pattern.with_deconstructor(ctx.receive_node(deconstruction_pattern.deconstructor, ctx.receive_tree))
            deconstruction_pattern = deconstruction_pattern.padding.with_nested(ctx.receive_node(deconstruction_pattern.padding.nested, JavaReceiver.receive_container))
            deconstruction_pattern = deconstruction_pattern.with_type(ctx.receive_value(deconstruction_pattern.type, JavaType))
            return deconstruction_pattern

        def visit_intersection_type(self, intersection_type: IntersectionType, ctx: ReceiverContext) -> J:
            intersection_type = intersection_type.with_id(ctx.receive_value(intersection_type.id, UUID))
            intersection_type = intersection_type.with_prefix(ctx.receive_node(intersection_type.prefix, JavaReceiver.receive_space))
            intersection_type = intersection_type.with_markers(ctx.receive_node(intersection_type.markers, ctx.receive_markers))
            intersection_type = intersection_type.padding.with_bounds(ctx.receive_node(intersection_type.padding.bounds, JavaReceiver.receive_container))
            return intersection_type

        def visit_label(self, label: Label, ctx: ReceiverContext) -> J:
            label = label.with_id(ctx.receive_value(label.id, UUID))
            label = label.with_prefix(ctx.receive_node(label.prefix, JavaReceiver.receive_space))
            label = label.with_markers(ctx.receive_node(label.markers, ctx.receive_markers))
            label = label.padding.with_label(ctx.receive_node(label.padding.label, JavaReceiver.receive_right_padded_tree))
            label = label.with_statement(ctx.receive_node(label.statement, ctx.receive_tree))
            return label

        def visit_lambda(self, lambda_: Lambda, ctx: ReceiverContext) -> J:
            lambda_ = lambda_.with_id(ctx.receive_value(lambda_.id, UUID))
            lambda_ = lambda_.with_prefix(ctx.receive_node(lambda_.prefix, JavaReceiver.receive_space))
            lambda_ = lambda_.with_markers(ctx.receive_node(lambda_.markers, ctx.receive_markers))
            lambda_ = lambda_.with_parameters(ctx.receive_node(lambda_.parameters, ctx.receive_tree))
            lambda_ = lambda_.with_arrow(ctx.receive_node(lambda_.arrow, JavaReceiver.receive_space))
            lambda_ = lambda_.with_body(ctx.receive_node(lambda_.body, ctx.receive_tree))
            lambda_ = lambda_.with_type(ctx.receive_value(lambda_.type, JavaType))
            return lambda_

        def visit_lambda_parameters(self, parameters: Lambda.Parameters, ctx: ReceiverContext) -> J:
            parameters = parameters.with_id(ctx.receive_value(parameters.id, UUID))
            parameters = parameters.with_prefix(ctx.receive_node(parameters.prefix, JavaReceiver.receive_space))
            parameters = parameters.with_markers(ctx.receive_node(parameters.markers, ctx.receive_markers))
            parameters = parameters.with_parenthesized(ctx.receive_value(parameters.parenthesized, bool))
            parameters = parameters.padding.with_parameters(ctx.receive_nodes(parameters.padding.parameters, JavaReceiver.receive_right_padded_tree))
            return parameters

        def visit_literal(self, literal: Literal, ctx: ReceiverContext) -> J:
            literal = literal.with_id(ctx.receive_value(literal.id, UUID))
            literal = literal.with_prefix(ctx.receive_node(literal.prefix, JavaReceiver.receive_space))
            literal = literal.with_markers(ctx.receive_node(literal.markers, ctx.receive_markers))
            literal = literal.with_value(ctx.receive_value(literal.value, object))
            literal = literal.with_value_source(ctx.receive_value(literal.value_source, str))
            literal = literal.with_unicode_escapes(ctx.receive_values(literal.unicode_escapes, Literal.UnicodeEscape))
            literal = literal.with_type(ctx.receive_value(literal.type, JavaType.Primitive))
            return literal

        def visit_member_reference(self, member_reference: MemberReference, ctx: ReceiverContext) -> J:
            member_reference = member_reference.with_id(ctx.receive_value(member_reference.id, UUID))
            member_reference = member_reference.with_prefix(ctx.receive_node(member_reference.prefix, JavaReceiver.receive_space))
            member_reference = member_reference.with_markers(ctx.receive_node(member_reference.markers, ctx.receive_markers))
            member_reference = member_reference.padding.with_containing(ctx.receive_node(member_reference.padding.containing, JavaReceiver.receive_right_padded_tree))
            member_reference = member_reference.padding.with_type_parameters(ctx.receive_node(member_reference.padding.type_parameters, JavaReceiver.receive_container))
            member_reference = member_reference.padding.with_reference(ctx.receive_node(member_reference.padding.reference, JavaReceiver.receive_left_padded_tree))
            member_reference = member_reference.with_type(ctx.receive_value(member_reference.type, JavaType))
            member_reference = member_reference.with_method_type(ctx.receive_value(member_reference.method_type, JavaType.Method))
            member_reference = member_reference.with_variable_type(ctx.receive_value(member_reference.variable_type, JavaType.Variable))
            return member_reference

        def visit_method_declaration(self, method_declaration: MethodDeclaration, ctx: ReceiverContext) -> J:
            method_declaration = method_declaration.with_id(ctx.receive_value(method_declaration.id, UUID))
            method_declaration = method_declaration.with_prefix(ctx.receive_node(method_declaration.prefix, JavaReceiver.receive_space))
            method_declaration = method_declaration.with_markers(ctx.receive_node(method_declaration.markers, ctx.receive_markers))
            method_declaration = method_declaration.with_leading_annotations(ctx.receive_nodes(method_declaration.leading_annotations, ctx.receive_tree))
            method_declaration = method_declaration.with_modifiers(ctx.receive_nodes(method_declaration.modifiers, ctx.receive_tree))
            method_declaration = method_declaration.annotations.with_type_parameters(ctx.receive_node(method_declaration.annotations.type_parameters, ctx.receive_tree))
            method_declaration = method_declaration.with_return_type_expression(ctx.receive_node(method_declaration.return_type_expression, ctx.receive_tree))
            method_declaration = method_declaration.annotations.with_name(ctx.receive_node(method_declaration.annotations.name, JavaReceiver.receive_method_identifier_with_annotations))
            method_declaration = method_declaration.padding.with_parameters(ctx.receive_node(method_declaration.padding.parameters, JavaReceiver.receive_container))
            method_declaration = method_declaration.padding.with_throws(ctx.receive_node(method_declaration.padding.throws, JavaReceiver.receive_container))
            method_declaration = method_declaration.with_body(ctx.receive_node(method_declaration.body, ctx.receive_tree))
            method_declaration = method_declaration.padding.with_default_value(ctx.receive_node(method_declaration.padding.default_value, JavaReceiver.receive_left_padded_tree))
            method_declaration = method_declaration.with_method_type(ctx.receive_value(method_declaration.method_type, JavaType.Method))
            return method_declaration

        def visit_method_invocation(self, method_invocation: MethodInvocation, ctx: ReceiverContext) -> J:
            method_invocation = method_invocation.with_id(ctx.receive_value(method_invocation.id, UUID))
            method_invocation = method_invocation.with_prefix(ctx.receive_node(method_invocation.prefix, JavaReceiver.receive_space))
            method_invocation = method_invocation.with_markers(ctx.receive_node(method_invocation.markers, ctx.receive_markers))
            method_invocation = method_invocation.padding.with_select(ctx.receive_node(method_invocation.padding.select, JavaReceiver.receive_right_padded_tree))
            method_invocation = method_invocation.padding.with_type_parameters(ctx.receive_node(method_invocation.padding.type_parameters, JavaReceiver.receive_container))
            method_invocation = method_invocation.with_name(ctx.receive_node(method_invocation.name, ctx.receive_tree))
            method_invocation = method_invocation.padding.with_arguments(ctx.receive_node(method_invocation.padding.arguments, JavaReceiver.receive_container))
            method_invocation = method_invocation.with_method_type(ctx.receive_value(method_invocation.method_type, JavaType.Method))
            return method_invocation

        def visit_modifier(self, modifier: Modifier, ctx: ReceiverContext) -> J:
            modifier = modifier.with_id(ctx.receive_value(modifier.id, UUID))
            modifier = modifier.with_prefix(ctx.receive_node(modifier.prefix, JavaReceiver.receive_space))
            modifier = modifier.with_markers(ctx.receive_node(modifier.markers, ctx.receive_markers))
            modifier = modifier.with_keyword(ctx.receive_value(modifier.keyword, str))
            modifier = modifier.with_type(ctx.receive_value(modifier.type, Modifier.Type))
            modifier = modifier.with_annotations(ctx.receive_nodes(modifier.annotations, ctx.receive_tree))
            return modifier

        def visit_multi_catch(self, multi_catch: MultiCatch, ctx: ReceiverContext) -> J:
            multi_catch = multi_catch.with_id(ctx.receive_value(multi_catch.id, UUID))
            multi_catch = multi_catch.with_prefix(ctx.receive_node(multi_catch.prefix, JavaReceiver.receive_space))
            multi_catch = multi_catch.with_markers(ctx.receive_node(multi_catch.markers, ctx.receive_markers))
            multi_catch = multi_catch.padding.with_alternatives(ctx.receive_nodes(multi_catch.padding.alternatives, JavaReceiver.receive_right_padded_tree))
            return multi_catch

        def visit_new_array(self, new_array: NewArray, ctx: ReceiverContext) -> J:
            new_array = new_array.with_id(ctx.receive_value(new_array.id, UUID))
            new_array = new_array.with_prefix(ctx.receive_node(new_array.prefix, JavaReceiver.receive_space))
            new_array = new_array.with_markers(ctx.receive_node(new_array.markers, ctx.receive_markers))
            new_array = new_array.with_type_expression(ctx.receive_node(new_array.type_expression, ctx.receive_tree))
            new_array = new_array.with_dimensions(ctx.receive_nodes(new_array.dimensions, ctx.receive_tree))
            new_array = new_array.padding.with_initializer(ctx.receive_node(new_array.padding.initializer, JavaReceiver.receive_container))
            new_array = new_array.with_type(ctx.receive_value(new_array.type, JavaType))
            return new_array

        def visit_array_dimension(self, array_dimension: ArrayDimension, ctx: ReceiverContext) -> J:
            array_dimension = array_dimension.with_id(ctx.receive_value(array_dimension.id, UUID))
            array_dimension = array_dimension.with_prefix(ctx.receive_node(array_dimension.prefix, JavaReceiver.receive_space))
            array_dimension = array_dimension.with_markers(ctx.receive_node(array_dimension.markers, ctx.receive_markers))
            array_dimension = array_dimension.padding.with_index(ctx.receive_node(array_dimension.padding.index, JavaReceiver.receive_right_padded_tree))
            return array_dimension

        def visit_new_class(self, new_class: NewClass, ctx: ReceiverContext) -> J:
            new_class = new_class.with_id(ctx.receive_value(new_class.id, UUID))
            new_class = new_class.with_prefix(ctx.receive_node(new_class.prefix, JavaReceiver.receive_space))
            new_class = new_class.with_markers(ctx.receive_node(new_class.markers, ctx.receive_markers))
            new_class = new_class.padding.with_enclosing(ctx.receive_node(new_class.padding.enclosing, JavaReceiver.receive_right_padded_tree))
            new_class = new_class.with_new(ctx.receive_node(new_class.new, JavaReceiver.receive_space))
            new_class = new_class.with_clazz(ctx.receive_node(new_class.clazz, ctx.receive_tree))
            new_class = new_class.padding.with_arguments(ctx.receive_node(new_class.padding.arguments, JavaReceiver.receive_container))
            new_class = new_class.with_body(ctx.receive_node(new_class.body, ctx.receive_tree))
            new_class = new_class.with_constructor_type(ctx.receive_value(new_class.constructor_type, JavaType.Method))
            return new_class

        def visit_nullable_type(self, nullable_type: NullableType, ctx: ReceiverContext) -> J:
            nullable_type = nullable_type.with_id(ctx.receive_value(nullable_type.id, UUID))
            nullable_type = nullable_type.with_prefix(ctx.receive_node(nullable_type.prefix, JavaReceiver.receive_space))
            nullable_type = nullable_type.with_markers(ctx.receive_node(nullable_type.markers, ctx.receive_markers))
            nullable_type = nullable_type.with_annotations(ctx.receive_nodes(nullable_type.annotations, ctx.receive_tree))
            nullable_type = nullable_type.padding.with_type_tree(ctx.receive_node(nullable_type.padding.type_tree, JavaReceiver.receive_right_padded_tree))
            return nullable_type

        def visit_package(self, package: Package, ctx: ReceiverContext) -> J:
            package = package.with_id(ctx.receive_value(package.id, UUID))
            package = package.with_prefix(ctx.receive_node(package.prefix, JavaReceiver.receive_space))
            package = package.with_markers(ctx.receive_node(package.markers, ctx.receive_markers))
            package = package.with_expression(ctx.receive_node(package.expression, ctx.receive_tree))
            package = package.with_annotations(ctx.receive_nodes(package.annotations, ctx.receive_tree))
            return package

        def visit_parameterized_type(self, parameterized_type: ParameterizedType, ctx: ReceiverContext) -> J:
            parameterized_type = parameterized_type.with_id(ctx.receive_value(parameterized_type.id, UUID))
            parameterized_type = parameterized_type.with_prefix(ctx.receive_node(parameterized_type.prefix, JavaReceiver.receive_space))
            parameterized_type = parameterized_type.with_markers(ctx.receive_node(parameterized_type.markers, ctx.receive_markers))
            parameterized_type = parameterized_type.with_clazz(ctx.receive_node(parameterized_type.clazz, ctx.receive_tree))
            parameterized_type = parameterized_type.padding.with_type_parameters(ctx.receive_node(parameterized_type.padding.type_parameters, JavaReceiver.receive_container))
            parameterized_type = parameterized_type.with_type(ctx.receive_value(parameterized_type.type, JavaType))
            return parameterized_type

        def visit_parentheses(self, parentheses: Parentheses[J2], ctx: ReceiverContext) -> J:
            parentheses = parentheses.with_id(ctx.receive_value(parentheses.id, UUID))
            parentheses = parentheses.with_prefix(ctx.receive_node(parentheses.prefix, JavaReceiver.receive_space))
            parentheses = parentheses.with_markers(ctx.receive_node(parentheses.markers, ctx.receive_markers))
            parentheses = parentheses.padding.with_tree(ctx.receive_node(parentheses.padding.tree, JavaReceiver.receive_right_padded_tree))
            return parentheses

        def visit_control_parentheses(self, control_parentheses: ControlParentheses[J2], ctx: ReceiverContext) -> J:
            control_parentheses = control_parentheses.with_id(ctx.receive_value(control_parentheses.id, UUID))
            control_parentheses = control_parentheses.with_prefix(ctx.receive_node(control_parentheses.prefix, JavaReceiver.receive_space))
            control_parentheses = control_parentheses.with_markers(ctx.receive_node(control_parentheses.markers, ctx.receive_markers))
            control_parentheses = control_parentheses.padding.with_tree(ctx.receive_node(control_parentheses.padding.tree, JavaReceiver.receive_right_padded_tree))
            return control_parentheses

        def visit_primitive(self, primitive: Primitive, ctx: ReceiverContext) -> J:
            primitive = primitive.with_id(ctx.receive_value(primitive.id, UUID))
            primitive = primitive.with_prefix(ctx.receive_node(primitive.prefix, JavaReceiver.receive_space))
            primitive = primitive.with_markers(ctx.receive_node(primitive.markers, ctx.receive_markers))
            primitive = primitive.with_type(ctx.receive_value(primitive.type, JavaType.Primitive))
            return primitive

        def visit_return(self, return_: Return, ctx: ReceiverContext) -> J:
            return_ = return_.with_id(ctx.receive_value(return_.id, UUID))
            return_ = return_.with_prefix(ctx.receive_node(return_.prefix, JavaReceiver.receive_space))
            return_ = return_.with_markers(ctx.receive_node(return_.markers, ctx.receive_markers))
            return_ = return_.with_expression(ctx.receive_node(return_.expression, ctx.receive_tree))
            return return_

        def visit_switch(self, switch: Switch, ctx: ReceiverContext) -> J:
            switch = switch.with_id(ctx.receive_value(switch.id, UUID))
            switch = switch.with_prefix(ctx.receive_node(switch.prefix, JavaReceiver.receive_space))
            switch = switch.with_markers(ctx.receive_node(switch.markers, ctx.receive_markers))
            switch = switch.with_selector(ctx.receive_node(switch.selector, ctx.receive_tree))
            switch = switch.with_cases(ctx.receive_node(switch.cases, ctx.receive_tree))
            return switch

        def visit_switch_expression(self, switch_expression: SwitchExpression, ctx: ReceiverContext) -> J:
            switch_expression = switch_expression.with_id(ctx.receive_value(switch_expression.id, UUID))
            switch_expression = switch_expression.with_prefix(ctx.receive_node(switch_expression.prefix, JavaReceiver.receive_space))
            switch_expression = switch_expression.with_markers(ctx.receive_node(switch_expression.markers, ctx.receive_markers))
            switch_expression = switch_expression.with_selector(ctx.receive_node(switch_expression.selector, ctx.receive_tree))
            switch_expression = switch_expression.with_cases(ctx.receive_node(switch_expression.cases, ctx.receive_tree))
            switch_expression = switch_expression.with_type(ctx.receive_value(switch_expression.type, JavaType))
            return switch_expression

        def visit_synchronized(self, synchronized: Synchronized, ctx: ReceiverContext) -> J:
            synchronized = synchronized.with_id(ctx.receive_value(synchronized.id, UUID))
            synchronized = synchronized.with_prefix(ctx.receive_node(synchronized.prefix, JavaReceiver.receive_space))
            synchronized = synchronized.with_markers(ctx.receive_node(synchronized.markers, ctx.receive_markers))
            synchronized = synchronized.with_lock(ctx.receive_node(synchronized.lock, ctx.receive_tree))
            synchronized = synchronized.with_body(ctx.receive_node(synchronized.body, ctx.receive_tree))
            return synchronized

        def visit_ternary(self, ternary: Ternary, ctx: ReceiverContext) -> J:
            ternary = ternary.with_id(ctx.receive_value(ternary.id, UUID))
            ternary = ternary.with_prefix(ctx.receive_node(ternary.prefix, JavaReceiver.receive_space))
            ternary = ternary.with_markers(ctx.receive_node(ternary.markers, ctx.receive_markers))
            ternary = ternary.with_condition(ctx.receive_node(ternary.condition, ctx.receive_tree))
            ternary = ternary.padding.with_true_part(ctx.receive_node(ternary.padding.true_part, JavaReceiver.receive_left_padded_tree))
            ternary = ternary.padding.with_false_part(ctx.receive_node(ternary.padding.false_part, JavaReceiver.receive_left_padded_tree))
            ternary = ternary.with_type(ctx.receive_value(ternary.type, JavaType))
            return ternary

        def visit_throw(self, throw: Throw, ctx: ReceiverContext) -> J:
            throw = throw.with_id(ctx.receive_value(throw.id, UUID))
            throw = throw.with_prefix(ctx.receive_node(throw.prefix, JavaReceiver.receive_space))
            throw = throw.with_markers(ctx.receive_node(throw.markers, ctx.receive_markers))
            throw = throw.with_exception(ctx.receive_node(throw.exception, ctx.receive_tree))
            return throw

        def visit_try(self, try_: Try, ctx: ReceiverContext) -> J:
            try_ = try_.with_id(ctx.receive_value(try_.id, UUID))
            try_ = try_.with_prefix(ctx.receive_node(try_.prefix, JavaReceiver.receive_space))
            try_ = try_.with_markers(ctx.receive_node(try_.markers, ctx.receive_markers))
            try_ = try_.padding.with_resources(ctx.receive_node(try_.padding.resources, JavaReceiver.receive_container))
            try_ = try_.with_body(ctx.receive_node(try_.body, ctx.receive_tree))
            try_ = try_.with_catches(ctx.receive_nodes(try_.catches, ctx.receive_tree))
            try_ = try_.padding.with_finally(ctx.receive_node(try_.padding.finally_, JavaReceiver.receive_left_padded_tree))
            return try_

        def visit_try_resource(self, resource: Try.Resource, ctx: ReceiverContext) -> J:
            resource = resource.with_id(ctx.receive_value(resource.id, UUID))
            resource = resource.with_prefix(ctx.receive_node(resource.prefix, JavaReceiver.receive_space))
            resource = resource.with_markers(ctx.receive_node(resource.markers, ctx.receive_markers))
            resource = resource.with_variable_declarations(ctx.receive_node(resource.variable_declarations, ctx.receive_tree))
            resource = resource.with_terminated_with_semicolon(ctx.receive_value(resource.terminated_with_semicolon, bool))
            return resource

        def visit_catch(self, catch: Try.Catch, ctx: ReceiverContext) -> J:
            catch = catch.with_id(ctx.receive_value(catch.id, UUID))
            catch = catch.with_prefix(ctx.receive_node(catch.prefix, JavaReceiver.receive_space))
            catch = catch.with_markers(ctx.receive_node(catch.markers, ctx.receive_markers))
            catch = catch.with_parameter(ctx.receive_node(catch.parameter, ctx.receive_tree))
            catch = catch.with_body(ctx.receive_node(catch.body, ctx.receive_tree))
            return catch

        def visit_type_cast(self, type_cast: TypeCast, ctx: ReceiverContext) -> J:
            type_cast = type_cast.with_id(ctx.receive_value(type_cast.id, UUID))
            type_cast = type_cast.with_prefix(ctx.receive_node(type_cast.prefix, JavaReceiver.receive_space))
            type_cast = type_cast.with_markers(ctx.receive_node(type_cast.markers, ctx.receive_markers))
            type_cast = type_cast.with_clazz(ctx.receive_node(type_cast.clazz, ctx.receive_tree))
            type_cast = type_cast.with_expression(ctx.receive_node(type_cast.expression, ctx.receive_tree))
            return type_cast

        def visit_type_parameter(self, type_parameter: TypeParameter, ctx: ReceiverContext) -> J:
            type_parameter = type_parameter.with_id(ctx.receive_value(type_parameter.id, UUID))
            type_parameter = type_parameter.with_prefix(ctx.receive_node(type_parameter.prefix, JavaReceiver.receive_space))
            type_parameter = type_parameter.with_markers(ctx.receive_node(type_parameter.markers, ctx.receive_markers))
            type_parameter = type_parameter.with_annotations(ctx.receive_nodes(type_parameter.annotations, ctx.receive_tree))
            type_parameter = type_parameter.with_modifiers(ctx.receive_nodes(type_parameter.modifiers, ctx.receive_tree))
            type_parameter = type_parameter.with_name(ctx.receive_node(type_parameter.name, ctx.receive_tree))
            type_parameter = type_parameter.padding.with_bounds(ctx.receive_node(type_parameter.padding.bounds, JavaReceiver.receive_container))
            return type_parameter

        def visit_type_parameters(self, type_parameters: TypeParameters, ctx: ReceiverContext) -> J:
            type_parameters = type_parameters.with_id(ctx.receive_value(type_parameters.id, UUID))
            type_parameters = type_parameters.with_prefix(ctx.receive_node(type_parameters.prefix, JavaReceiver.receive_space))
            type_parameters = type_parameters.with_markers(ctx.receive_node(type_parameters.markers, ctx.receive_markers))
            type_parameters = type_parameters.with_annotations(ctx.receive_nodes(type_parameters.annotations, ctx.receive_tree))
            type_parameters = type_parameters.padding.with_type_parameters(ctx.receive_nodes(type_parameters.padding.type_parameters, JavaReceiver.receive_right_padded_tree))
            return type_parameters

        def visit_unary(self, unary: Unary, ctx: ReceiverContext) -> J:
            unary = unary.with_id(ctx.receive_value(unary.id, UUID))
            unary = unary.with_prefix(ctx.receive_node(unary.prefix, JavaReceiver.receive_space))
            unary = unary.with_markers(ctx.receive_node(unary.markers, ctx.receive_markers))
            unary = unary.padding.with_operator(ctx.receive_node(unary.padding.operator, JavaReceiver.left_padded_value_receiver(Unary.Type)))
            unary = unary.with_expression(ctx.receive_node(unary.expression, ctx.receive_tree))
            unary = unary.with_type(ctx.receive_value(unary.type, JavaType))
            return unary

        def visit_variable_declarations(self, variable_declarations: VariableDeclarations, ctx: ReceiverContext) -> J:
            variable_declarations = variable_declarations.with_id(ctx.receive_value(variable_declarations.id, UUID))
            variable_declarations = variable_declarations.with_prefix(ctx.receive_node(variable_declarations.prefix, JavaReceiver.receive_space))
            variable_declarations = variable_declarations.with_markers(ctx.receive_node(variable_declarations.markers, ctx.receive_markers))
            variable_declarations = variable_declarations.with_leading_annotations(ctx.receive_nodes(variable_declarations.leading_annotations, ctx.receive_tree))
            variable_declarations = variable_declarations.with_modifiers(ctx.receive_nodes(variable_declarations.modifiers, ctx.receive_tree))
            variable_declarations = variable_declarations.with_type_expression(ctx.receive_node(variable_declarations.type_expression, ctx.receive_tree))
            variable_declarations = variable_declarations.with_varargs(ctx.receive_node(variable_declarations.varargs, JavaReceiver.receive_space))
            variable_declarations = variable_declarations.with_dimensions_before_name(ctx.receive_nodes(variable_declarations.dimensions_before_name, JavaReceiver.left_padded_node_receiver(Space)))
            variable_declarations = variable_declarations.padding.with_variables(ctx.receive_nodes(variable_declarations.padding.variables, JavaReceiver.receive_right_padded_tree))
            return variable_declarations

        def visit_variable(self, named_variable: VariableDeclarations.NamedVariable, ctx: ReceiverContext) -> J:
            named_variable = named_variable.with_id(ctx.receive_value(named_variable.id, UUID))
            named_variable = named_variable.with_prefix(ctx.receive_node(named_variable.prefix, JavaReceiver.receive_space))
            named_variable = named_variable.with_markers(ctx.receive_node(named_variable.markers, ctx.receive_markers))
            named_variable = named_variable.with_name(ctx.receive_node(named_variable.name, ctx.receive_tree))
            named_variable = named_variable.with_dimensions_after_name(ctx.receive_nodes(named_variable.dimensions_after_name, JavaReceiver.left_padded_node_receiver(Space)))
            named_variable = named_variable.padding.with_initializer(ctx.receive_node(named_variable.padding.initializer, JavaReceiver.receive_left_padded_tree))
            named_variable = named_variable.with_variable_type(ctx.receive_value(named_variable.variable_type, JavaType.Variable))
            return named_variable

        def visit_while_loop(self, while_loop: WhileLoop, ctx: ReceiverContext) -> J:
            while_loop = while_loop.with_id(ctx.receive_value(while_loop.id, UUID))
            while_loop = while_loop.with_prefix(ctx.receive_node(while_loop.prefix, JavaReceiver.receive_space))
            while_loop = while_loop.with_markers(ctx.receive_node(while_loop.markers, ctx.receive_markers))
            while_loop = while_loop.with_condition(ctx.receive_node(while_loop.condition, ctx.receive_tree))
            while_loop = while_loop.padding.with_body(ctx.receive_node(while_loop.padding.body, JavaReceiver.receive_right_padded_tree))
            return while_loop

        def visit_wildcard(self, wildcard: Wildcard, ctx: ReceiverContext) -> J:
            wildcard = wildcard.with_id(ctx.receive_value(wildcard.id, UUID))
            wildcard = wildcard.with_prefix(ctx.receive_node(wildcard.prefix, JavaReceiver.receive_space))
            wildcard = wildcard.with_markers(ctx.receive_node(wildcard.markers, ctx.receive_markers))
            wildcard = wildcard.padding.with_bound(ctx.receive_node(wildcard.padding.bound, JavaReceiver.left_padded_value_receiver(Wildcard.Bound)))
            wildcard = wildcard.with_bounded_type(ctx.receive_node(wildcard.bounded_type, ctx.receive_tree))
            return wildcard

        def visit_yield(self, yield_: Yield, ctx: ReceiverContext) -> J:
            yield_ = yield_.with_id(ctx.receive_value(yield_.id, UUID))
            yield_ = yield_.with_prefix(ctx.receive_node(yield_.prefix, JavaReceiver.receive_space))
            yield_ = yield_.with_markers(ctx.receive_node(yield_.markers, ctx.receive_markers))
            yield_ = yield_.with_implicit(ctx.receive_value(yield_.implicit, bool))
            yield_ = yield_.with_value(ctx.receive_node(yield_.value, ctx.receive_tree))
            return yield_

        def visit_unknown(self, unknown: Unknown, ctx: ReceiverContext) -> J:
            unknown = unknown.with_id(ctx.receive_value(unknown.id, UUID))
            unknown = unknown.with_prefix(ctx.receive_node(unknown.prefix, JavaReceiver.receive_space))
            unknown = unknown.with_markers(ctx.receive_node(unknown.markers, ctx.receive_markers))
            unknown = unknown.with_source(ctx.receive_node(unknown.source, ctx.receive_tree))
            return unknown

        def visit_unknown_source(self, source: Unknown.Source, ctx: ReceiverContext) -> J:
            source = source.with_id(ctx.receive_value(source.id, UUID))
            source = source.with_prefix(ctx.receive_node(source.prefix, JavaReceiver.receive_space))
            source = source.with_markers(ctx.receive_node(source.markers, ctx.receive_markers))
            source = source.with_text(ctx.receive_value(source.text, str))
            return source

        def visit_erroneous(self, erroneous: Erroneous, ctx: ReceiverContext) -> J:
            erroneous = erroneous.with_id(ctx.receive_value(erroneous.id, UUID))
            erroneous = erroneous.with_prefix(ctx.receive_node(erroneous.prefix, JavaReceiver.receive_space))
            erroneous = erroneous.with_markers(ctx.receive_node(erroneous.markers, ctx.receive_markers))
            erroneous = erroneous.with_text(ctx.receive_value(erroneous.text, str))
            return erroneous

    # noinspection PyTypeChecker
    class Factory(ReceiverFactory):
        def create(self, type: str, ctx: ReceiverContext) -> Tree:
            if type in ["rewrite.java.tree.AnnotatedType", "org.openrewrite.java.tree.J$AnnotatedType"]:
                return AnnotatedType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Annotation", "org.openrewrite.java.tree.J$Annotation"]:
                return Annotation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container)
                )

            if type in ["rewrite.java.tree.ArrayAccess", "org.openrewrite.java.tree.J$ArrayAccess"]:
                return ArrayAccess(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.ArrayType", "org.openrewrite.java.tree.J$ArrayType"]:
                return ArrayType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Assert", "org.openrewrite.java.tree.J$Assert"]:
                return Assert(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.java.tree.Assignment", "org.openrewrite.java.tree.J$Assignment"]:
                return Assignment(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.AssignmentOperation", "org.openrewrite.java.tree.J$AssignmentOperation"]:
                return AssignmentOperation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.left_padded_value_receiver(AssignmentOperation.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Binary", "org.openrewrite.java.tree.J$Binary"]:
                return Binary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.left_padded_value_receiver(Binary.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Block", "org.openrewrite.java.tree.J$Block"]:
                return Block(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.right_padded_value_receiver(bool)),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_space)
                )

            if type in ["rewrite.java.tree.Break", "org.openrewrite.java.tree.J$Break"]:
                return Break(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Case", "org.openrewrite.java.tree.J$Case"]:
                return Case(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, Case.Type),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.ClassDeclaration", "org.openrewrite.java.tree.J$ClassDeclaration"]:
                return ClassDeclaration(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType.FullyQualified)
                )

            if type in ["rewrite.java.tree.ClassDeclaration.Kind", "org.openrewrite.java.tree.J$ClassDeclaration$Kind"]:
                return ClassDeclaration.Kind(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_value(None, ClassDeclaration.Kind.Type)
                )

            if type in ["rewrite.java.tree.CompilationUnit", "org.openrewrite.java.tree.J$CompilationUnit"]:
                return CompilationUnit(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, Path),
                    ctx.receive_value(None, FileAttributes),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, bool),
                    ctx.receive_value(None, Checksum),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_space)
                )

            if type in ["rewrite.java.tree.Continue", "org.openrewrite.java.tree.J$Continue"]:
                return Continue(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.DoWhileLoop", "org.openrewrite.java.tree.J$DoWhileLoop"]:
                return DoWhileLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.java.tree.Empty", "org.openrewrite.java.tree.J$Empty"]:
                return Empty(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers)
                )

            if type in ["rewrite.java.tree.EnumValue", "org.openrewrite.java.tree.J$EnumValue"]:
                return EnumValue(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.EnumValueSet", "org.openrewrite.java.tree.J$EnumValueSet"]:
                return EnumValueSet(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_value(None, bool)
                )

            if type in ["rewrite.java.tree.FieldAccess", "org.openrewrite.java.tree.J$FieldAccess"]:
                return FieldAccess(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.ForEachLoop", "org.openrewrite.java.tree.J$ForEachLoop"]:
                return ForEachLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.ForEachLoop.Control", "org.openrewrite.java.tree.J$ForEachLoop$Control"]:
                return ForEachLoop.Control(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.ForLoop", "org.openrewrite.java.tree.J$ForLoop"]:
                return ForLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.ForLoop.Control", "org.openrewrite.java.tree.J$ForLoop$Control"]:
                return ForLoop.Control(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.ParenthesizedTypeTree", "org.openrewrite.java.tree.J$ParenthesizedTypeTree"]:
                return ParenthesizedTypeTree(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Identifier", "org.openrewrite.java.tree.J$Identifier"]:
                return Identifier(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, JavaType),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.java.tree.If", "org.openrewrite.java.tree.J$If"]:
                return If(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.If.Else", "org.openrewrite.java.tree.J$If$Else"]:
                return If.Else(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Import", "org.openrewrite.java.tree.J$Import"]:
                return Import(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.left_padded_value_receiver(bool)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.java.tree.InstanceOf", "org.openrewrite.java.tree.J$InstanceOf"]:
                return InstanceOf(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.DeconstructionPattern", "org.openrewrite.java.tree.J$DeconstructionPattern"]:
                return DeconstructionPattern(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.IntersectionType", "org.openrewrite.java.tree.J$IntersectionType"]:
                return IntersectionType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_container)
                )

            if type in ["rewrite.java.tree.Label", "org.openrewrite.java.tree.J$Label"]:
                return Label(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Lambda", "org.openrewrite.java.tree.J$Lambda"]:
                return Lambda(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Lambda.Parameters", "org.openrewrite.java.tree.J$Lambda$Parameters"]:
                return Lambda.Parameters(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, bool),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Literal", "org.openrewrite.java.tree.J$Literal"]:
                return Literal(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, object),
                    ctx.receive_value(None, str),
                    ctx.receive_values(None, Literal.UnicodeEscape),
                    ctx.receive_value(None, JavaType.Primitive)
                )

            if type in ["rewrite.java.tree.MemberReference", "org.openrewrite.java.tree.J$MemberReference"]:
                return MemberReference(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType),
                    ctx.receive_value(None, JavaType.Method),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.java.tree.MethodDeclaration", "org.openrewrite.java.tree.J$MethodDeclaration"]:
                return MethodDeclaration(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_method_identifier_with_annotations),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.java.tree.MethodInvocation", "org.openrewrite.java.tree.J$MethodInvocation"]:
                return MethodInvocation(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.java.tree.Modifier", "org.openrewrite.java.tree.J$Modifier"]:
                return Modifier(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str),
                    ctx.receive_value(None, Modifier.Type),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.MultiCatch", "org.openrewrite.java.tree.J$MultiCatch"]:
                return MultiCatch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.NewArray", "org.openrewrite.java.tree.J$NewArray"]:
                return NewArray(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.ArrayDimension", "org.openrewrite.java.tree.J$ArrayDimension"]:
                return ArrayDimension(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.NewClass", "org.openrewrite.java.tree.J$NewClass"]:
                return NewClass(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType.Method)
                )

            if type in ["rewrite.java.tree.NullableType", "org.openrewrite.java.tree.J$NullableType"]:
                return NullableType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Package", "org.openrewrite.java.tree.J$Package"]:
                return Package(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.ParameterizedType", "org.openrewrite.java.tree.J$ParameterizedType"]:
                return ParameterizedType(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Parentheses", "org.openrewrite.java.tree.J$Parentheses"]:
                return Parentheses[J2](
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.ControlParentheses", "org.openrewrite.java.tree.J$ControlParentheses"]:
                return ControlParentheses[J2](
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Primitive", "org.openrewrite.java.tree.J$Primitive"]:
                return Primitive(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, JavaType.Primitive)
                )

            if type in ["rewrite.java.tree.Return", "org.openrewrite.java.tree.J$Return"]:
                return Return(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Switch", "org.openrewrite.java.tree.J$Switch"]:
                return Switch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.SwitchExpression", "org.openrewrite.java.tree.J$SwitchExpression"]:
                return SwitchExpression(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Synchronized", "org.openrewrite.java.tree.J$Synchronized"]:
                return Synchronized(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Ternary", "org.openrewrite.java.tree.J$Ternary"]:
                return Ternary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.Throw", "org.openrewrite.java.tree.J$Throw"]:
                return Throw(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Try", "org.openrewrite.java.tree.J$Try"]:
                return Try(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.receive_container),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree)
                )

            if type in ["rewrite.java.tree.Try.Resource", "org.openrewrite.java.tree.J$Try$Resource"]:
                return Try.Resource(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, bool)
                )

            if type in ["rewrite.java.tree.Try.Catch", "org.openrewrite.java.tree.J$Try$Catch"]:
                return Try.Catch(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.TypeCast", "org.openrewrite.java.tree.J$TypeCast"]:
                return TypeCast(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.TypeParameter", "org.openrewrite.java.tree.J$TypeParameter"]:
                return TypeParameter(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_container)
                )

            if type in ["rewrite.java.tree.TypeParameters", "org.openrewrite.java.tree.J$TypeParameters"]:
                return TypeParameters(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Unary", "org.openrewrite.java.tree.J$Unary"]:
                return Unary(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.left_padded_value_receiver(Unary.Type)),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_value(None, JavaType)
                )

            if type in ["rewrite.java.tree.VariableDeclarations", "org.openrewrite.java.tree.J$VariableDeclarations"]:
                return VariableDeclarations(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_nodes(None, ctx.receive_tree),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_nodes(None, JavaReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_nodes(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.VariableDeclarations.NamedVariable", "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable"]:
                return VariableDeclarations.NamedVariable(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_nodes(None, JavaReceiver.left_padded_node_receiver(Space)),
                    ctx.receive_node(None, JavaReceiver.receive_left_padded_tree),
                    ctx.receive_value(None, JavaType.Variable)
                )

            if type in ["rewrite.java.tree.WhileLoop", "org.openrewrite.java.tree.J$WhileLoop"]:
                return WhileLoop(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree),
                    ctx.receive_node(None, JavaReceiver.receive_right_padded_tree)
                )

            if type in ["rewrite.java.tree.Wildcard", "org.openrewrite.java.tree.J$Wildcard"]:
                return Wildcard(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, JavaReceiver.left_padded_value_receiver(Wildcard.Bound)),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Yield", "org.openrewrite.java.tree.J$Yield"]:
                return Yield(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, bool),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Unknown", "org.openrewrite.java.tree.J$Unknown"]:
                return Unknown(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_node(None, ctx.receive_tree)
                )

            if type in ["rewrite.java.tree.Unknown.Source", "org.openrewrite.java.tree.J$Unknown$Source"]:
                return Unknown.Source(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
                    ctx.receive_node(None, ctx.receive_markers),
                    ctx.receive_value(None, str)
                )

            if type in ["rewrite.java.tree.Erroneous", "org.openrewrite.java.tree.J$Erroneous"]:
                return Erroneous(
                    ctx.receive_value(None, UUID),
                    ctx.receive_node(None, JavaReceiver.receive_space),
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
