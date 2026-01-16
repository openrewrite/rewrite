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

"""Java visitor for traversing and transforming Java LST nodes."""

from __future__ import annotations

from typing import TypeVar, Optional, Any, List

from rewrite.java.support_types import J, Space, JRightPadded, JLeftPadded, JContainer, Expression, Statement
from rewrite.utils import list_map
from rewrite.visitor import TreeVisitor

from rewrite.java import tree as j

P = TypeVar("P")


class JavaVisitor(TreeVisitor[J, P]):
    """
    Base visitor for Java LST nodes.

    This visitor provides visit methods for all Java AST node types.
    Subclass this to implement Java-specific transformations.
    """

    def visit_expression(self, expr: Expression, p: P) -> J:
        """Visit an expression. Override to intercept all expressions."""
        return expr

    def visit_statement(self, stmt: Statement, p: P) -> J:
        """Visit a statement. Override to intercept all statements."""
        return stmt

    def visit_space(self, space: Optional[Space], loc: Optional[Any], p: P) -> Space:
        """Visit a space (whitespace and comments)."""
        if space is None:
            return Space.EMPTY
        return space

    def visit_and_cast(self, tree: Optional[Any], type_: type, p: P) -> Optional[Any]:
        """Visit a tree and cast to the expected type."""
        if tree is None:
            return None
        return self.visit(tree, p)

    def visit_left_padded(
        self,
        left: Optional[JLeftPadded],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JLeftPadded]:
        """Visit a left-padded element."""
        if left is None:
            return None
        before = self.visit_space(left.before, loc, p)
        element = left.element
        if isinstance(element, J):
            element = self.visit(element, p)
            if element is None:
                return None
        if before is left.before and element is left.element:
            return left
        return left.replace(before=before, element=element)

    def visit_right_padded(
        self,
        right: Optional[JRightPadded],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JRightPadded]:
        """Visit a right-padded element."""
        if right is None:
            return None
        element = right.element
        if isinstance(element, J):
            element = self.visit(element, p)
            if element is None:
                return None
        after = self.visit_space(right.after, loc, p)
        if element is right.element and after is right.after:
            return right
        return right.replace(element=element, after=after)

    def visit_container(
        self,
        container: Optional[JContainer],
        loc: Optional[Any],
        p: P,
    ) -> Optional[JContainer]:
        """Visit a container of elements."""
        if container is None:
            return None
        before = self.visit_space(container.before, loc, p)
        elements = list_map(
            lambda e: self.visit_right_padded(e, loc, p),
            container.padding.elements
        )
        # Filter out None elements (deleted by visitor)
        elements = [e for e in elements if e is not None]
        if before is container.before and elements == list(container.padding.elements):
            return container
        return container.replace(before=before).padding.replace(elements=elements)

    # -------------------------------------------------------------------------
    # Java tree visitor methods
    # -------------------------------------------------------------------------

    def visit_annotated_type(self, annotated_type: j.AnnotatedType, p: P) -> J:
        annotated_type = annotated_type.replace(
            prefix=self.visit_space(annotated_type.prefix, Space.Location.ANNOTATED_TYPE_PREFIX, p)
        )
        temp_expr = self.visit_expression(annotated_type, p)
        if not isinstance(temp_expr, j.AnnotatedType):
            return temp_expr
        annotated_type = temp_expr
        annotated_type = annotated_type.replace(markers=self.visit_markers(annotated_type.markers, p))
        annotated_type = annotated_type.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), annotated_type.annotations)
        )
        annotated_type = annotated_type.replace(
            type_expression=self.visit_and_cast(annotated_type.type_expression, j.TypeTree, p)
        )
        return annotated_type

    def visit_annotation(self, annotation: j.Annotation, p: P) -> J:
        annotation = annotation.replace(
            prefix=self.visit_space(annotation.prefix, Space.Location.ANNOTATION_PREFIX, p)
        )
        temp_expr = self.visit_expression(annotation, p)
        if not isinstance(temp_expr, j.Annotation):
            return temp_expr
        annotation = temp_expr
        annotation = annotation.replace(markers=self.visit_markers(annotation.markers, p))
        annotation = annotation.replace(
            annotation_type=self.visit_and_cast(annotation.annotation_type, j.NameTree, p)
        )
        annotation = annotation.replace(
            arguments=self.visit_container(annotation.padding.arguments, JContainer.Location.ANNOTATION_ARGUMENTS, p)
        )
        return annotation

    def visit_array_access(self, array_access: j.ArrayAccess, p: P) -> J:
        array_access = array_access.replace(
            prefix=self.visit_space(array_access.prefix, Space.Location.ARRAY_ACCESS_PREFIX, p)
        )
        temp_expr = self.visit_expression(array_access, p)
        if not isinstance(temp_expr, j.ArrayAccess):
            return temp_expr
        array_access = temp_expr
        array_access = array_access.replace(markers=self.visit_markers(array_access.markers, p))
        array_access = array_access.replace(
            indexed=self.visit_and_cast(array_access.indexed, Expression, p)
        )
        array_access = array_access.replace(
            dimension=self.visit_and_cast(array_access.dimension, j.ArrayDimension, p)
        )
        return array_access

    def visit_array_dimension(self, array_dimension: j.ArrayDimension, p: P) -> J:
        array_dimension = array_dimension.replace(
            prefix=self.visit_space(array_dimension.prefix, Space.Location.DIMENSION_PREFIX, p)
        )
        array_dimension = array_dimension.replace(markers=self.visit_markers(array_dimension.markers, p))
        array_dimension = array_dimension.padding.replace(
            index=self.visit_right_padded(array_dimension.padding.index, JRightPadded.Location.ARRAY_INDEX, p)
        )
        return array_dimension

    def visit_array_type(self, array_type: j.ArrayType, p: P) -> J:
        array_type = array_type.replace(
            prefix=self.visit_space(array_type.prefix, Space.Location.ARRAY_TYPE_PREFIX, p)
        )
        temp_expr = self.visit_expression(array_type, p)
        if not isinstance(temp_expr, j.ArrayType):
            return temp_expr
        array_type = temp_expr
        array_type = array_type.replace(markers=self.visit_markers(array_type.markers, p))
        array_type = array_type.replace(
            element_type=self.visit_and_cast(array_type.element_type, j.TypeTree, p)
        )
        if array_type.annotations:
            array_type = array_type.replace(
                annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), array_type.annotations)
            )
        array_type = array_type.padding.replace(
            dimension=self.visit_left_padded(array_type.padding.dimension, JLeftPadded.Location.ARRAY_TYPE_DIMENSION, p)
        )
        return array_type

    def visit_assert(self, assert_: j.Assert, p: P) -> J:
        assert_ = assert_.replace(
            prefix=self.visit_space(assert_.prefix, Space.Location.ASSERT_PREFIX, p)
        )
        temp_stmt = self.visit_statement(assert_, p)
        if not isinstance(temp_stmt, j.Assert):
            return temp_stmt
        assert_ = temp_stmt
        assert_ = assert_.replace(markers=self.visit_markers(assert_.markers, p))
        assert_ = assert_.replace(
            condition=self.visit_and_cast(assert_.condition, Expression, p)
        )
        if assert_.detail:
            assert_ = assert_.replace(
                detail=self.visit_left_padded(assert_.detail, JLeftPadded.Location.ASSERT_DETAIL, p)
            )
        return assert_

    def visit_assignment(self, assignment: j.Assignment, p: P) -> J:
        assignment = assignment.replace(
            prefix=self.visit_space(assignment.prefix, Space.Location.ASSIGNMENT_PREFIX, p)
        )
        temp_stmt = self.visit_statement(assignment, p)
        if not isinstance(temp_stmt, j.Assignment):
            return temp_stmt
        assignment = temp_stmt
        temp_expr = self.visit_expression(assignment, p)
        if not isinstance(temp_expr, j.Assignment):
            return temp_expr
        assignment = temp_expr
        assignment = assignment.replace(markers=self.visit_markers(assignment.markers, p))
        assignment = assignment.replace(
            variable=self.visit_and_cast(assignment.variable, Expression, p)
        )
        assignment = assignment.padding.replace(
            assignment=self.visit_left_padded(assignment.padding.assignment, JLeftPadded.Location.ASSIGNMENT, p)
        )
        return assignment

    def visit_assignment_operation(self, assign_op: j.AssignmentOperation, p: P) -> J:
        assign_op = assign_op.replace(
            prefix=self.visit_space(assign_op.prefix, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p)
        )
        temp_stmt = self.visit_statement(assign_op, p)
        if not isinstance(temp_stmt, j.AssignmentOperation):
            return temp_stmt
        assign_op = temp_stmt
        temp_expr = self.visit_expression(assign_op, p)
        if not isinstance(temp_expr, j.AssignmentOperation):
            return temp_expr
        assign_op = temp_expr
        assign_op = assign_op.replace(markers=self.visit_markers(assign_op.markers, p))
        assign_op = assign_op.replace(
            variable=self.visit_and_cast(assign_op.variable, Expression, p)
        )
        assign_op = assign_op.padding.replace(
            operator=self.visit_left_padded(assign_op.padding.operator, JLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p)
        )
        assign_op = assign_op.replace(
            assignment=self.visit_and_cast(assign_op.assignment, Expression, p)
        )
        return assign_op

    def visit_binary(self, binary: j.Binary, p: P) -> J:
        binary = binary.replace(
            prefix=self.visit_space(binary.prefix, Space.Location.BINARY_PREFIX, p)
        )
        temp_expr = self.visit_expression(binary, p)
        if not isinstance(temp_expr, j.Binary):
            return temp_expr
        binary = temp_expr
        binary = binary.replace(markers=self.visit_markers(binary.markers, p))
        binary = binary.replace(
            left=self.visit_and_cast(binary.left, Expression, p)
        )
        binary = binary.padding.replace(
            operator=self.visit_left_padded(binary.padding.operator, JLeftPadded.Location.BINARY_OPERATOR, p)
        )
        binary = binary.replace(
            right=self.visit_and_cast(binary.right, Expression, p)
        )
        return binary

    def visit_block(self, block: j.Block, p: P) -> J:
        block = block.replace(
            prefix=self.visit_space(block.prefix, Space.Location.BLOCK_PREFIX, p)
        )
        temp_stmt = self.visit_statement(block, p)
        if not isinstance(temp_stmt, j.Block):
            return temp_stmt
        block = temp_stmt
        block = block.replace(markers=self.visit_markers(block.markers, p))
        block = block.padding.replace(
            static=self.visit_right_padded(block.padding.static, JRightPadded.Location.STATIC_INIT, p)
        )
        block = block.padding.replace(
            statements=list_map(
                lambda stmt: self.visit_right_padded(stmt, JRightPadded.Location.BLOCK_STATEMENT, p),
                block.padding.statements
            )
        )
        block = block.replace(
            end=self.visit_space(block.end, Space.Location.BLOCK_END, p)
        )
        return block

    def visit_break(self, break_: j.Break, p: P) -> J:
        break_ = break_.replace(
            prefix=self.visit_space(break_.prefix, Space.Location.BREAK_PREFIX, p)
        )
        temp_stmt = self.visit_statement(break_, p)
        if not isinstance(temp_stmt, j.Break):
            return temp_stmt
        break_ = temp_stmt
        break_ = break_.replace(markers=self.visit_markers(break_.markers, p))
        if break_.label:
            break_ = break_.replace(
                label=self.visit_and_cast(break_.label, j.Identifier, p)
            )
        return break_

    def visit_case(self, case: j.Case, p: P) -> J:
        case = case.replace(
            prefix=self.visit_space(case.prefix, Space.Location.CASE_PREFIX, p)
        )
        temp_stmt = self.visit_statement(case, p)
        if not isinstance(temp_stmt, j.Case):
            return temp_stmt
        case = temp_stmt
        case = case.replace(markers=self.visit_markers(case.markers, p))
        case = case.replace(
            case_labels=self.visit_container(case.padding.case_labels, JContainer.Location.CASE_EXPRESSION, p)
        )
        case = case.replace(
            statements=self.visit_container(case.padding.statements, JContainer.Location.CASE, p)
        )
        case = case.padding.replace(
            body=self.visit_right_padded(case.padding.body, JRightPadded.Location.CASE_BODY, p)
        )
        return case

    def visit_class_declaration(self, class_decl: j.ClassDeclaration, p: P) -> J:
        class_decl = class_decl.replace(
            prefix=self.visit_space(class_decl.prefix, Space.Location.CLASS_DECLARATION_PREFIX, p)
        )
        temp_stmt = self.visit_statement(class_decl, p)
        if not isinstance(temp_stmt, j.ClassDeclaration):
            return temp_stmt
        class_decl = temp_stmt
        class_decl = class_decl.replace(markers=self.visit_markers(class_decl.markers, p))
        class_decl = class_decl.replace(
            leading_annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), class_decl.leading_annotations)
        )
        class_decl = class_decl.replace(
            modifiers=list_map(lambda m: self.visit_and_cast(m, j.Modifier, p), class_decl.modifiers)
        )
        class_decl = class_decl.replace(
            kind=self.visit_and_cast(class_decl.padding.kind, j.ClassDeclaration.Kind, p)
        )
        class_decl = class_decl.replace(
            name=self.visit_and_cast(class_decl.name, j.Identifier, p)
        )
        class_decl = class_decl.replace(
            type_parameters=self.visit_container(class_decl.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, p)
        )
        class_decl = class_decl.replace(
            primary_constructor=self.visit_container(class_decl.padding.primary_constructor, JContainer.Location.RECORD_STATE_VECTOR, p)
        )
        class_decl = class_decl.padding.replace(
            extends=self.visit_left_padded(class_decl.padding.extends, JLeftPadded.Location.EXTENDS, p)
        )
        class_decl = class_decl.replace(
            implements=self.visit_container(class_decl.padding.implements, JContainer.Location.IMPLEMENTS, p)
        )
        class_decl = class_decl.replace(
            permits=self.visit_container(class_decl.padding.permits, JContainer.Location.PERMITS, p)
        )
        class_decl = class_decl.replace(
            body=self.visit_and_cast(class_decl.body, j.Block, p)
        )
        return class_decl

    def visit_class_declaration_kind(self, kind: j.ClassDeclaration.Kind, p: P) -> J:
        kind = kind.replace(
            prefix=self.visit_space(kind.prefix, Space.Location.CLASS_KIND, p)
        )
        kind = kind.replace(markers=self.visit_markers(kind.markers, p))
        kind = kind.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), kind.annotations)
        )
        return kind

    def visit_continue(self, continue_: j.Continue, p: P) -> J:
        continue_ = continue_.replace(
            prefix=self.visit_space(continue_.prefix, Space.Location.CONTINUE_PREFIX, p)
        )
        temp_stmt = self.visit_statement(continue_, p)
        if not isinstance(temp_stmt, j.Continue):
            return temp_stmt
        continue_ = temp_stmt
        continue_ = continue_.replace(markers=self.visit_markers(continue_.markers, p))
        if continue_.label:
            continue_ = continue_.replace(
                label=self.visit_and_cast(continue_.label, j.Identifier, p)
            )
        return continue_

    def visit_control_parentheses(self, control_parens: j.ControlParentheses, p: P) -> J:
        control_parens = control_parens.replace(
            prefix=self.visit_space(control_parens.prefix, Space.Location.CONTROL_PARENTHESES_PREFIX, p)
        )
        temp_expr = self.visit_expression(control_parens, p)
        if not isinstance(temp_expr, j.ControlParentheses):
            return temp_expr
        control_parens = temp_expr
        control_parens = control_parens.replace(markers=self.visit_markers(control_parens.markers, p))
        control_parens = control_parens.padding.replace(
            tree=self.visit_right_padded(control_parens.padding.tree, JRightPadded.Location.PARENTHESES, p)
        )
        return control_parens

    def visit_do_while_loop(self, do_while: j.DoWhileLoop, p: P) -> J:
        do_while = do_while.replace(
            prefix=self.visit_space(do_while.prefix, Space.Location.DO_WHILE_PREFIX, p)
        )
        temp_stmt = self.visit_statement(do_while, p)
        if not isinstance(temp_stmt, j.DoWhileLoop):
            return temp_stmt
        do_while = temp_stmt
        do_while = do_while.replace(markers=self.visit_markers(do_while.markers, p))
        do_while = do_while.padding.replace(
            body=self.visit_right_padded(do_while.padding.body, JRightPadded.Location.WHILE_BODY, p)
        )
        do_while = do_while.padding.replace(
            while_condition=self.visit_left_padded(do_while.padding.while_condition, JLeftPadded.Location.WHILE_CONDITION, p)
        )
        return do_while

    def visit_empty(self, empty: j.Empty, p: P) -> J:
        empty = empty.replace(
            prefix=self.visit_space(empty.prefix, Space.Location.EMPTY_PREFIX, p)
        )
        temp_stmt = self.visit_statement(empty, p)
        if not isinstance(temp_stmt, j.Empty):
            return temp_stmt
        empty = temp_stmt
        temp_expr = self.visit_expression(empty, p)
        if not isinstance(temp_expr, j.Empty):
            return temp_expr
        empty = temp_expr
        empty = empty.replace(markers=self.visit_markers(empty.markers, p))
        return empty

    def visit_enum_value(self, enum_value: j.EnumValue, p: P) -> J:
        enum_value = enum_value.replace(
            prefix=self.visit_space(enum_value.prefix, Space.Location.ENUM_VALUE_PREFIX, p)
        )
        enum_value = enum_value.replace(markers=self.visit_markers(enum_value.markers, p))
        enum_value = enum_value.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), enum_value.annotations)
        )
        enum_value = enum_value.replace(
            name=self.visit_and_cast(enum_value.name, j.Identifier, p)
        )
        if enum_value.initializer:
            enum_value = enum_value.replace(
                initializer=self.visit_and_cast(enum_value.initializer, j.NewClass, p)
            )
        return enum_value

    def visit_enum_value_set(self, enum_set: j.EnumValueSet, p: P) -> J:
        enum_set = enum_set.replace(
            prefix=self.visit_space(enum_set.prefix, Space.Location.ENUM_VALUE_SET_PREFIX, p)
        )
        temp_stmt = self.visit_statement(enum_set, p)
        if not isinstance(temp_stmt, j.EnumValueSet):
            return temp_stmt
        enum_set = temp_stmt
        enum_set = enum_set.replace(markers=self.visit_markers(enum_set.markers, p))
        enum_set = enum_set.padding.replace(
            enums=list_map(
                lambda e: self.visit_right_padded(e, JRightPadded.Location.ENUM_VALUE, p),
                enum_set.padding.enums
            )
        )
        return enum_set

    def visit_field_access(self, field_access: j.FieldAccess, p: P) -> J:
        field_access = field_access.replace(
            prefix=self.visit_space(field_access.prefix, Space.Location.FIELD_ACCESS_PREFIX, p)
        )
        temp_stmt = self.visit_statement(field_access, p)
        if not isinstance(temp_stmt, j.FieldAccess):
            return temp_stmt
        field_access = temp_stmt
        temp_expr = self.visit_expression(field_access, p)
        if not isinstance(temp_expr, j.FieldAccess):
            return temp_expr
        field_access = temp_expr
        field_access = field_access.replace(markers=self.visit_markers(field_access.markers, p))
        field_access = field_access.replace(
            target=self.visit_and_cast(field_access.target, Expression, p)
        )
        field_access = field_access.padding.replace(
            name=self.visit_left_padded(field_access.padding.name, JLeftPadded.Location.FIELD_ACCESS_NAME, p)
        )
        return field_access

    def visit_for_each_loop(self, for_each: j.ForEachLoop, p: P) -> J:
        for_each = for_each.replace(
            prefix=self.visit_space(for_each.prefix, Space.Location.FOR_EACH_LOOP_PREFIX, p)
        )
        temp_stmt = self.visit_statement(for_each, p)
        if not isinstance(temp_stmt, j.ForEachLoop):
            return temp_stmt
        for_each = temp_stmt
        for_each = for_each.replace(markers=self.visit_markers(for_each.markers, p))
        for_each = for_each.replace(
            control=self.visit_and_cast(for_each.control, j.ForEachLoop.Control, p)
        )
        for_each = for_each.padding.replace(
            body=self.visit_right_padded(for_each.padding.body, JRightPadded.Location.FOR_BODY, p)
        )
        return for_each

    def visit_for_each_control(self, control: j.ForEachLoop.Control, p: P) -> J:
        control = control.replace(
            prefix=self.visit_space(control.prefix, Space.Location.FOR_EACH_CONTROL_PREFIX, p)
        )
        control = control.replace(markers=self.visit_markers(control.markers, p))
        control = control.padding.replace(
            variable=self.visit_right_padded(control.padding.variable, JRightPadded.Location.FOREACH_VARIABLE, p)
        )
        control = control.padding.replace(
            iterable=self.visit_right_padded(control.padding.iterable, JRightPadded.Location.FOREACH_ITERABLE, p)
        )
        return control

    def visit_for_loop(self, for_loop: j.ForLoop, p: P) -> J:
        for_loop = for_loop.replace(
            prefix=self.visit_space(for_loop.prefix, Space.Location.FOR_PREFIX, p)
        )
        temp_stmt = self.visit_statement(for_loop, p)
        if not isinstance(temp_stmt, j.ForLoop):
            return temp_stmt
        for_loop = temp_stmt
        for_loop = for_loop.replace(markers=self.visit_markers(for_loop.markers, p))
        for_loop = for_loop.replace(
            control=self.visit_and_cast(for_loop.control, j.ForLoop.Control, p)
        )
        for_loop = for_loop.padding.replace(
            body=self.visit_right_padded(for_loop.padding.body, JRightPadded.Location.FOR_BODY, p)
        )
        return for_loop

    def visit_for_control(self, control: j.ForLoop.Control, p: P) -> J:
        control = control.replace(
            prefix=self.visit_space(control.prefix, Space.Location.FOR_CONTROL_PREFIX, p)
        )
        control = control.replace(markers=self.visit_markers(control.markers, p))
        control = control.padding.replace(
            init=list_map(
                lambda i: self.visit_right_padded(i, JRightPadded.Location.FOR_INIT, p),
                control.padding.init
            )
        )
        control = control.padding.replace(
            condition=self.visit_right_padded(control.padding.condition, JRightPadded.Location.FOR_CONDITION, p)
        )
        control = control.padding.replace(
            update=list_map(
                lambda u: self.visit_right_padded(u, JRightPadded.Location.FOR_UPDATE, p),
                control.padding.update
            )
        )
        return control

    def visit_identifier(self, ident: j.Identifier, p: P) -> J:
        ident = ident.replace(
            prefix=self.visit_space(ident.prefix, Space.Location.IDENTIFIER_PREFIX, p)
        )
        temp_expr = self.visit_expression(ident, p)
        if not isinstance(temp_expr, j.Identifier):
            return temp_expr
        ident = temp_expr
        ident = ident.replace(markers=self.visit_markers(ident.markers, p))
        if ident.annotations:
            ident = ident.replace(
                annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), ident.annotations)
            )
        return ident

    def visit_if(self, if_: j.If, p: P) -> J:
        if_ = if_.replace(
            prefix=self.visit_space(if_.prefix, Space.Location.IF_PREFIX, p)
        )
        temp_stmt = self.visit_statement(if_, p)
        if not isinstance(temp_stmt, j.If):
            return temp_stmt
        if_ = temp_stmt
        if_ = if_.replace(markers=self.visit_markers(if_.markers, p))
        if_ = if_.replace(
            if_condition=self.visit_and_cast(if_.if_condition, j.ControlParentheses, p)
        )
        if_ = if_.padding.replace(
            then_part=self.visit_right_padded(if_.padding.then_part, JRightPadded.Location.IF_THEN, p)
        )
        if if_.else_part:
            if_ = if_.replace(
                else_part=self.visit_and_cast(if_.else_part, j.If.Else, p)
            )
        return if_

    def visit_else(self, else_: j.If.Else, p: P) -> J:
        else_ = else_.replace(
            prefix=self.visit_space(else_.prefix, Space.Location.ELSE_PREFIX, p)
        )
        else_ = else_.replace(markers=self.visit_markers(else_.markers, p))
        else_ = else_.padding.replace(
            body=self.visit_right_padded(else_.padding.body, JRightPadded.Location.IF_ELSE, p)
        )
        return else_

    def visit_import(self, import_: j.Import, p: P) -> J:
        import_ = import_.replace(
            prefix=self.visit_space(import_.prefix, Space.Location.IMPORT_PREFIX, p)
        )
        temp_stmt = self.visit_statement(import_, p)
        if not isinstance(temp_stmt, j.Import):
            return temp_stmt
        import_ = temp_stmt
        import_ = import_.replace(markers=self.visit_markers(import_.markers, p))
        import_ = import_.padding.replace(
            static=self.visit_left_padded(import_.padding.static, JLeftPadded.Location.STATIC_IMPORT, p)
        )
        import_ = import_.replace(
            qualid=self.visit_and_cast(import_.qualid, j.FieldAccess, p)
        )
        if import_.padding.alias:
            import_ = import_.padding.replace(
                alias=self.visit_left_padded(import_.padding.alias, JLeftPadded.Location.IMPORT_ALIAS_PREFIX, p)
            )
        return import_

    def visit_instance_of(self, instance_of: j.InstanceOf, p: P) -> J:
        instance_of = instance_of.replace(
            prefix=self.visit_space(instance_of.prefix, Space.Location.INSTANCEOF_PREFIX, p)
        )
        temp_expr = self.visit_expression(instance_of, p)
        if not isinstance(temp_expr, j.InstanceOf):
            return temp_expr
        instance_of = temp_expr
        instance_of = instance_of.replace(markers=self.visit_markers(instance_of.markers, p))
        instance_of = instance_of.padding.replace(
            expression=self.visit_right_padded(instance_of.padding.expression, JRightPadded.Location.INSTANCEOF, p)
        )
        instance_of = instance_of.replace(
            clazz=self.visit_and_cast(instance_of.clazz, J, p)
        )
        if instance_of.pattern:
            instance_of = instance_of.replace(
                pattern=self.visit_and_cast(instance_of.pattern, J, p)
            )
        return instance_of

    def visit_label(self, label: j.Label, p: P) -> J:
        label = label.replace(
            prefix=self.visit_space(label.prefix, Space.Location.LABEL_PREFIX, p)
        )
        temp_stmt = self.visit_statement(label, p)
        if not isinstance(temp_stmt, j.Label):
            return temp_stmt
        label = temp_stmt
        label = label.replace(markers=self.visit_markers(label.markers, p))
        label = label.padding.replace(
            label=self.visit_right_padded(label.padding.label, JRightPadded.Location.LABEL, p)
        )
        label = label.replace(
            statement=self.visit_and_cast(label.statement, Statement, p)
        )
        return label

    def visit_lambda(self, lambda_: j.Lambda, p: P) -> J:
        lambda_ = lambda_.replace(
            prefix=self.visit_space(lambda_.prefix, Space.Location.LAMBDA_PREFIX, p)
        )
        temp_stmt = self.visit_statement(lambda_, p)
        if not isinstance(temp_stmt, j.Lambda):
            return temp_stmt
        lambda_ = temp_stmt
        temp_expr = self.visit_expression(lambda_, p)
        if not isinstance(temp_expr, j.Lambda):
            return temp_expr
        lambda_ = temp_expr
        lambda_ = lambda_.replace(markers=self.visit_markers(lambda_.markers, p))
        lambda_ = lambda_.replace(
            parameters=self.visit_and_cast(lambda_.parameters, j.Lambda.Parameters, p)
        )
        lambda_ = lambda_.replace(
            arrow=self.visit_space(lambda_.arrow, Space.Location.LAMBDA_ARROW_PREFIX, p)
        )
        lambda_ = lambda_.replace(
            body=self.visit_and_cast(lambda_.body, J, p)
        )
        return lambda_

    def visit_lambda_parameters(self, params: j.Lambda.Parameters, p: P) -> J:
        params = params.replace(
            prefix=self.visit_space(params.prefix, Space.Location.LAMBDA_PARAMETERS_PREFIX, p)
        )
        params = params.replace(markers=self.visit_markers(params.markers, p))
        params = params.padding.replace(
            parameters=list_map(
                lambda param: self.visit_right_padded(param, JRightPadded.Location.LAMBDA_PARAM, p),
                params.padding.parameters
            )
        )
        return params

    def visit_literal(self, literal: j.Literal, p: P) -> J:
        literal = literal.replace(
            prefix=self.visit_space(literal.prefix, Space.Location.LITERAL_PREFIX, p)
        )
        temp_expr = self.visit_expression(literal, p)
        if not isinstance(temp_expr, j.Literal):
            return temp_expr
        literal = temp_expr
        literal = literal.replace(markers=self.visit_markers(literal.markers, p))
        return literal

    def visit_member_reference(self, member_ref: j.MemberReference, p: P) -> J:
        member_ref = member_ref.replace(
            prefix=self.visit_space(member_ref.prefix, Space.Location.MEMBER_REFERENCE_PREFIX, p)
        )
        temp_expr = self.visit_expression(member_ref, p)
        if not isinstance(temp_expr, j.MemberReference):
            return temp_expr
        member_ref = temp_expr
        member_ref = member_ref.replace(markers=self.visit_markers(member_ref.markers, p))
        member_ref = member_ref.padding.replace(
            containing=self.visit_right_padded(member_ref.padding.containing, JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p)
        )
        member_ref = member_ref.replace(
            type_parameters=self.visit_container(member_ref.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, p)
        )
        member_ref = member_ref.padding.replace(
            reference=self.visit_left_padded(member_ref.padding.reference, JLeftPadded.Location.MEMBER_REFERENCE_NAME, p)
        )
        return member_ref

    def visit_method_declaration(self, method: j.MethodDeclaration, p: P) -> J:
        method = method.replace(
            prefix=self.visit_space(method.prefix, Space.Location.METHOD_DECLARATION_PREFIX, p)
        )
        temp_stmt = self.visit_statement(method, p)
        if not isinstance(temp_stmt, j.MethodDeclaration):
            return temp_stmt
        method = temp_stmt
        method = method.replace(markers=self.visit_markers(method.markers, p))
        method = method.replace(
            leading_annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), method.leading_annotations)
        )
        method = method.replace(
            modifiers=list_map(lambda m: self.visit_and_cast(m, j.Modifier, p), method.modifiers)
        )
        if method.padding.type_parameters:
            method = method.replace(
                type_parameters=self.visit_and_cast(method.padding.type_parameters, j.TypeParameters, p)
            )
        if method.return_type_expression:
            method = method.replace(
                return_type_expression=self.visit_and_cast(method.return_type_expression, j.TypeTree, p)
            )
        method = method.replace(
            name=self.visit_and_cast(method.padding.name, j.Identifier, p)
        )
        method = method.replace(
            parameters=self.visit_container(method.padding.parameters, JContainer.Location.METHOD_DECLARATION_PARAMETERS, p)
        )
        if method.padding.throws:
            method = method.replace(
                throws=self.visit_container(method.padding.throws, JContainer.Location.THROWS, p)
            )
        if method.body:
            method = method.replace(
                body=self.visit_and_cast(method.body, j.Block, p)
            )
        method = method.padding.replace(
            default_value=self.visit_left_padded(method.padding.default_value, JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p)
        )
        return method

    def visit_method_invocation(self, method: j.MethodInvocation, p: P) -> J:
        method = method.replace(
            prefix=self.visit_space(method.prefix, Space.Location.METHOD_INVOCATION_PREFIX, p)
        )
        temp_stmt = self.visit_statement(method, p)
        if not isinstance(temp_stmt, j.MethodInvocation):
            return temp_stmt
        method = temp_stmt
        temp_expr = self.visit_expression(method, p)
        if not isinstance(temp_expr, j.MethodInvocation):
            return temp_expr
        method = temp_expr
        method = method.replace(markers=self.visit_markers(method.markers, p))
        method = method.padding.replace(
            select=self.visit_right_padded(method.padding.select, JRightPadded.Location.METHOD_SELECT, p)
        )
        method = method.replace(
            type_parameters=self.visit_container(method.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, p)
        )
        method = method.replace(
            name=self.visit_and_cast(method.name, j.Identifier, p)
        )
        method = method.replace(
            arguments=self.visit_container(method.padding.arguments, JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p)
        )
        return method

    def visit_modifier(self, modifier: j.Modifier, p: P) -> J:
        modifier = modifier.replace(
            prefix=self.visit_space(modifier.prefix, Space.Location.MODIFIER_PREFIX, p)
        )
        modifier = modifier.replace(markers=self.visit_markers(modifier.markers, p))
        modifier = modifier.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), modifier.annotations)
        )
        return modifier

    def visit_multi_catch(self, multi_catch: j.MultiCatch, p: P) -> J:
        multi_catch = multi_catch.replace(
            prefix=self.visit_space(multi_catch.prefix, Space.Location.MULTI_CATCH_PREFIX, p)
        )
        multi_catch = multi_catch.replace(markers=self.visit_markers(multi_catch.markers, p))
        multi_catch = multi_catch.padding.replace(
            alternatives=list_map(
                lambda alt: self.visit_right_padded(alt, JRightPadded.Location.CATCH_ALTERNATIVE, p),
                multi_catch.padding.alternatives
            )
        )
        return multi_catch

    def visit_new_array(self, new_array: j.NewArray, p: P) -> J:
        new_array = new_array.replace(
            prefix=self.visit_space(new_array.prefix, Space.Location.NEW_ARRAY_PREFIX, p)
        )
        temp_expr = self.visit_expression(new_array, p)
        if not isinstance(temp_expr, j.NewArray):
            return temp_expr
        new_array = temp_expr
        new_array = new_array.replace(markers=self.visit_markers(new_array.markers, p))
        if new_array.type_expression:
            new_array = new_array.replace(
                type_expression=self.visit_and_cast(new_array.type_expression, j.TypeTree, p)
            )
        new_array = new_array.replace(
            dimensions=list_map(lambda d: self.visit_and_cast(d, j.ArrayDimension, p), new_array.dimensions)
        )
        new_array = new_array.replace(
            initializer=self.visit_container(new_array.padding.initializer, JContainer.Location.NEW_ARRAY_INITIALIZER, p)
        )
        return new_array

    def visit_new_class(self, new_class: j.NewClass, p: P) -> J:
        new_class = new_class.replace(
            prefix=self.visit_space(new_class.prefix, Space.Location.NEW_CLASS_PREFIX, p)
        )
        temp_stmt = self.visit_statement(new_class, p)
        if not isinstance(temp_stmt, j.NewClass):
            return temp_stmt
        new_class = temp_stmt
        temp_expr = self.visit_expression(new_class, p)
        if not isinstance(temp_expr, j.NewClass):
            return temp_expr
        new_class = temp_expr
        new_class = new_class.replace(markers=self.visit_markers(new_class.markers, p))
        if new_class.padding.enclosing:
            new_class = new_class.padding.replace(
                enclosing=self.visit_right_padded(new_class.padding.enclosing, JRightPadded.Location.NEW_CLASS_ENCLOSING, p)
            )
        new_class = new_class.replace(
            new=self.visit_space(new_class.new, Space.Location.NEW_PREFIX, p)
        )
        if new_class.clazz:
            new_class = new_class.replace(
                clazz=self.visit_and_cast(new_class.clazz, j.TypeTree, p)
            )
        new_class = new_class.replace(
            arguments=self.visit_container(new_class.padding.arguments, JContainer.Location.NEW_CLASS_ARGUMENTS, p)
        )
        if new_class.body:
            new_class = new_class.replace(
                body=self.visit_and_cast(new_class.body, j.Block, p)
            )
        return new_class

    def visit_parameterized_type(self, parameterized_type: j.ParameterizedType, p: P) -> J:
        parameterized_type = parameterized_type.replace(
            prefix=self.visit_space(parameterized_type.prefix, Space.Location.PARAMETERIZED_TYPE_PREFIX, p)
        )
        temp_expr = self.visit_expression(parameterized_type, p)
        if not isinstance(temp_expr, j.ParameterizedType):
            return temp_expr
        parameterized_type = temp_expr
        parameterized_type = parameterized_type.replace(markers=self.visit_markers(parameterized_type.markers, p))
        parameterized_type = parameterized_type.replace(
            clazz=self.visit_and_cast(parameterized_type.clazz, j.NameTree, p)
        )
        parameterized_type = parameterized_type.replace(
            type_parameters=self.visit_container(parameterized_type.padding.type_parameters, JContainer.Location.TYPE_PARAMETERS, p)
        )
        return parameterized_type

    def visit_parentheses(self, parens: j.Parentheses, p: P) -> J:
        parens = parens.replace(
            prefix=self.visit_space(parens.prefix, Space.Location.PARENTHESES_PREFIX, p)
        )
        temp_expr = self.visit_expression(parens, p)
        if not isinstance(temp_expr, j.Parentheses):
            return temp_expr
        parens = temp_expr
        parens = parens.replace(markers=self.visit_markers(parens.markers, p))
        parens = parens.padding.replace(
            tree=self.visit_right_padded(parens.padding.tree, JRightPadded.Location.PARENTHESES, p)
        )
        return parens

    def visit_primitive(self, primitive: j.Primitive, p: P) -> J:
        primitive = primitive.replace(
            prefix=self.visit_space(primitive.prefix, Space.Location.PRIMITIVE_PREFIX, p)
        )
        temp_expr = self.visit_expression(primitive, p)
        if not isinstance(temp_expr, j.Primitive):
            return temp_expr
        primitive = temp_expr
        primitive = primitive.replace(markers=self.visit_markers(primitive.markers, p))
        return primitive

    def visit_return(self, return_: j.Return, p: P) -> J:
        return_ = return_.replace(
            prefix=self.visit_space(return_.prefix, Space.Location.RETURN_PREFIX, p)
        )
        temp_stmt = self.visit_statement(return_, p)
        if not isinstance(temp_stmt, j.Return):
            return temp_stmt
        return_ = temp_stmt
        return_ = return_.replace(markers=self.visit_markers(return_.markers, p))
        if return_.expression:
            return_ = return_.replace(
                expression=self.visit_and_cast(return_.expression, Expression, p)
            )
        return return_

    def visit_switch(self, switch: j.Switch, p: P) -> J:
        switch = switch.replace(
            prefix=self.visit_space(switch.prefix, Space.Location.SWITCH_PREFIX, p)
        )
        temp_stmt = self.visit_statement(switch, p)
        if not isinstance(temp_stmt, j.Switch):
            return temp_stmt
        switch = temp_stmt
        switch = switch.replace(markers=self.visit_markers(switch.markers, p))
        switch = switch.replace(
            selector=self.visit_and_cast(switch.selector, j.ControlParentheses, p)
        )
        switch = switch.replace(
            cases=self.visit_and_cast(switch.cases, j.Block, p)
        )
        return switch

    def visit_switch_expression(self, switch_expr: j.SwitchExpression, p: P) -> J:
        switch_expr = switch_expr.replace(
            prefix=self.visit_space(switch_expr.prefix, Space.Location.SWITCH_EXPRESSION_PREFIX, p)
        )
        temp_expr = self.visit_expression(switch_expr, p)
        if not isinstance(temp_expr, j.SwitchExpression):
            return temp_expr
        switch_expr = temp_expr
        switch_expr = switch_expr.replace(markers=self.visit_markers(switch_expr.markers, p))
        switch_expr = switch_expr.replace(
            selector=self.visit_and_cast(switch_expr.selector, j.ControlParentheses, p)
        )
        switch_expr = switch_expr.replace(
            cases=self.visit_and_cast(switch_expr.cases, j.Block, p)
        )
        return switch_expr

    def visit_synchronized(self, sync: j.Synchronized, p: P) -> J:
        sync = sync.replace(
            prefix=self.visit_space(sync.prefix, Space.Location.SYNCHRONIZED_PREFIX, p)
        )
        temp_stmt = self.visit_statement(sync, p)
        if not isinstance(temp_stmt, j.Synchronized):
            return temp_stmt
        sync = temp_stmt
        sync = sync.replace(markers=self.visit_markers(sync.markers, p))
        sync = sync.replace(
            lock=self.visit_and_cast(sync.lock, j.ControlParentheses, p)
        )
        sync = sync.replace(
            body=self.visit_and_cast(sync.body, j.Block, p)
        )
        return sync

    def visit_ternary(self, ternary: j.Ternary, p: P) -> J:
        ternary = ternary.replace(
            prefix=self.visit_space(ternary.prefix, Space.Location.TERNARY_PREFIX, p)
        )
        temp_stmt = self.visit_statement(ternary, p)
        if not isinstance(temp_stmt, j.Ternary):
            return temp_stmt
        ternary = temp_stmt
        temp_expr = self.visit_expression(ternary, p)
        if not isinstance(temp_expr, j.Ternary):
            return temp_expr
        ternary = temp_expr
        ternary = ternary.replace(markers=self.visit_markers(ternary.markers, p))
        ternary = ternary.replace(
            condition=self.visit_and_cast(ternary.condition, Expression, p)
        )
        ternary = ternary.padding.replace(
            true_part=self.visit_left_padded(ternary.padding.true_part, JLeftPadded.Location.TERNARY_TRUE, p)
        )
        ternary = ternary.padding.replace(
            false_part=self.visit_left_padded(ternary.padding.false_part, JLeftPadded.Location.TERNARY_FALSE, p)
        )
        return ternary

    def visit_throw(self, throw: j.Throw, p: P) -> J:
        throw = throw.replace(
            prefix=self.visit_space(throw.prefix, Space.Location.THROW_PREFIX, p)
        )
        temp_stmt = self.visit_statement(throw, p)
        if not isinstance(temp_stmt, j.Throw):
            return temp_stmt
        throw = temp_stmt
        throw = throw.replace(markers=self.visit_markers(throw.markers, p))
        throw = throw.replace(
            exception=self.visit_and_cast(throw.exception, Expression, p)
        )
        return throw

    def visit_try(self, try_: j.Try, p: P) -> J:
        try_ = try_.replace(
            prefix=self.visit_space(try_.prefix, Space.Location.TRY_PREFIX, p)
        )
        temp_stmt = self.visit_statement(try_, p)
        if not isinstance(temp_stmt, j.Try):
            return temp_stmt
        try_ = temp_stmt
        try_ = try_.replace(markers=self.visit_markers(try_.markers, p))
        try_ = try_.replace(
            resources=self.visit_container(try_.padding.resources, JContainer.Location.TRY_RESOURCES, p)
        )
        try_ = try_.replace(
            body=self.visit_and_cast(try_.body, j.Block, p)
        )
        try_ = try_.replace(
            catches=list_map(lambda c: self.visit_and_cast(c, j.Try.Catch, p), try_.catches)
        )
        if try_.padding.finally_:
            try_ = try_.replace(
                finally_=self.visit_left_padded(try_.padding.finally_, JLeftPadded.Location.TRY_FINALLY, p)
            )
        return try_

    def visit_try_resource(self, resource: j.Try.Resource, p: P) -> J:
        resource = resource.replace(
            prefix=self.visit_space(resource.prefix, Space.Location.TRY_RESOURCE, p)
        )
        resource = resource.replace(markers=self.visit_markers(resource.markers, p))
        resource = resource.replace(
            variable_declarations=self.visit_and_cast(resource.variable_declarations, j.TypedTree, p)
        )
        return resource

    def visit_catch(self, catch: j.Try.Catch, p: P) -> J:
        catch = catch.replace(
            prefix=self.visit_space(catch.prefix, Space.Location.CATCH_PREFIX, p)
        )
        catch = catch.replace(markers=self.visit_markers(catch.markers, p))
        catch = catch.replace(
            parameter=self.visit_and_cast(catch.parameter, j.ControlParentheses, p)
        )
        catch = catch.replace(
            body=self.visit_and_cast(catch.body, j.Block, p)
        )
        return catch

    def visit_type_cast(self, type_cast: j.TypeCast, p: P) -> J:
        type_cast = type_cast.replace(
            prefix=self.visit_space(type_cast.prefix, Space.Location.TYPE_CAST_PREFIX, p)
        )
        temp_expr = self.visit_expression(type_cast, p)
        if not isinstance(temp_expr, j.TypeCast):
            return temp_expr
        type_cast = temp_expr
        type_cast = type_cast.replace(markers=self.visit_markers(type_cast.markers, p))
        type_cast = type_cast.replace(
            clazz=self.visit_and_cast(type_cast.clazz, j.ControlParentheses, p)
        )
        type_cast = type_cast.replace(
            expression=self.visit_and_cast(type_cast.expression, Expression, p)
        )
        return type_cast

    def visit_type_parameter(self, type_param: j.TypeParameter, p: P) -> J:
        type_param = type_param.replace(
            prefix=self.visit_space(type_param.prefix, Space.Location.TYPE_PARAMETER_PREFIX, p)
        )
        type_param = type_param.replace(markers=self.visit_markers(type_param.markers, p))
        type_param = type_param.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), type_param.annotations)
        )
        type_param = type_param.replace(
            modifiers=list_map(lambda m: self.visit_and_cast(m, j.Modifier, p), type_param.modifiers)
        )
        type_param = type_param.replace(
            name=self.visit_and_cast(type_param.name, j.Identifier, p)
        )
        type_param = type_param.replace(
            bounds=self.visit_container(type_param.padding.bounds, JContainer.Location.TYPE_BOUND, p)
        )
        return type_param

    def visit_type_parameters(self, type_params: j.TypeParameters, p: P) -> J:
        type_params = type_params.replace(
            prefix=self.visit_space(type_params.prefix, Space.Location.TYPE_PARAMETERS_PREFIX, p)
        )
        type_params = type_params.replace(markers=self.visit_markers(type_params.markers, p))
        type_params = type_params.replace(
            annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), type_params.annotations)
        )
        type_params = type_params.padding.replace(
            type_parameters=list_map(
                lambda tp: self.visit_right_padded(tp, JRightPadded.Location.TYPE_PARAMETER, p),
                type_params.padding.type_parameters
            )
        )
        return type_params

    def visit_unary(self, unary: j.Unary, p: P) -> J:
        unary = unary.replace(
            prefix=self.visit_space(unary.prefix, Space.Location.UNARY_PREFIX, p)
        )
        temp_stmt = self.visit_statement(unary, p)
        if not isinstance(temp_stmt, j.Unary):
            return temp_stmt
        unary = temp_stmt
        temp_expr = self.visit_expression(unary, p)
        if not isinstance(temp_expr, j.Unary):
            return temp_expr
        unary = temp_expr
        unary = unary.replace(markers=self.visit_markers(unary.markers, p))
        unary = unary.padding.replace(
            operator=self.visit_left_padded(unary.padding.operator, JLeftPadded.Location.UNARY_OPERATOR, p)
        )
        unary = unary.replace(
            expression=self.visit_and_cast(unary.expression, Expression, p)
        )
        return unary

    def visit_variable_declarations(self, var_decls: j.VariableDeclarations, p: P) -> J:
        var_decls = var_decls.replace(
            prefix=self.visit_space(var_decls.prefix, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p)
        )
        temp_stmt = self.visit_statement(var_decls, p)
        if not isinstance(temp_stmt, j.VariableDeclarations):
            return temp_stmt
        var_decls = temp_stmt
        var_decls = var_decls.replace(markers=self.visit_markers(var_decls.markers, p))
        var_decls = var_decls.replace(
            leading_annotations=list_map(lambda a: self.visit_and_cast(a, j.Annotation, p), var_decls.leading_annotations)
        )
        var_decls = var_decls.replace(
            modifiers=list_map(lambda m: self.visit_and_cast(m, j.Modifier, p), var_decls.modifiers)
        )
        if var_decls.type_expression:
            var_decls = var_decls.replace(
                type_expression=self.visit_and_cast(var_decls.type_expression, j.TypeTree, p)
            )
        if var_decls.varargs:
            var_decls = var_decls.replace(
                varargs=self.visit_space(var_decls.varargs, Space.Location.VARARGS, p)
            )
        var_decls = var_decls.padding.replace(
            variables=list_map(
                lambda v: self.visit_right_padded(v, JRightPadded.Location.NAMED_VARIABLE, p),
                var_decls.padding.variables
            )
        )
        return var_decls

    def visit_variable(self, variable: j.VariableDeclarations.NamedVariable, p: P) -> J:
        variable = variable.replace(
            prefix=self.visit_space(variable.prefix, Space.Location.VARIABLE_PREFIX, p)
        )
        variable = variable.replace(markers=self.visit_markers(variable.markers, p))
        variable = variable.replace(
            name=self.visit_and_cast(variable.name, j.Identifier, p)
        )
        variable = variable.replace(
            dimensions_after_name=list_map(
                lambda d: self.visit_left_padded(d, JLeftPadded.Location.VARIABLE_INITIALIZER, p),
                variable.dimensions_after_name
            )
        )
        variable = variable.padding.replace(
            initializer=self.visit_left_padded(variable.padding.initializer, JLeftPadded.Location.VARIABLE_INITIALIZER, p)
        )
        return variable

    def visit_while_loop(self, while_loop: j.WhileLoop, p: P) -> J:
        while_loop = while_loop.replace(
            prefix=self.visit_space(while_loop.prefix, Space.Location.WHILE_PREFIX, p)
        )
        temp_stmt = self.visit_statement(while_loop, p)
        if not isinstance(temp_stmt, j.WhileLoop):
            return temp_stmt
        while_loop = temp_stmt
        while_loop = while_loop.replace(markers=self.visit_markers(while_loop.markers, p))
        while_loop = while_loop.replace(
            condition=self.visit_and_cast(while_loop.condition, j.ControlParentheses, p)
        )
        while_loop = while_loop.padding.replace(
            body=self.visit_right_padded(while_loop.padding.body, JRightPadded.Location.WHILE_BODY, p)
        )
        return while_loop

    def visit_wildcard(self, wildcard: j.Wildcard, p: P) -> J:
        wildcard = wildcard.replace(
            prefix=self.visit_space(wildcard.prefix, Space.Location.WILDCARD_PREFIX, p)
        )
        temp_expr = self.visit_expression(wildcard, p)
        if not isinstance(temp_expr, j.Wildcard):
            return temp_expr
        wildcard = temp_expr
        wildcard = wildcard.replace(markers=self.visit_markers(wildcard.markers, p))
        wildcard = wildcard.padding.replace(
            bound=self.visit_left_padded(wildcard.padding.bound, JLeftPadded.Location.WILDCARD_BOUND, p)
        )
        if wildcard.bounded_type:
            wildcard = wildcard.replace(
                bounded_type=self.visit_and_cast(wildcard.bounded_type, j.NameTree, p)
            )
        return wildcard

    def visit_yield(self, yield_: j.Yield, p: P) -> J:
        yield_ = yield_.replace(
            prefix=self.visit_space(yield_.prefix, Space.Location.YIELD_PREFIX, p)
        )
        temp_stmt = self.visit_statement(yield_, p)
        if not isinstance(temp_stmt, j.Yield):
            return temp_stmt
        yield_ = temp_stmt
        yield_ = yield_.replace(markers=self.visit_markers(yield_.markers, p))
        yield_ = yield_.replace(
            value=self.visit_and_cast(yield_.value, Expression, p)
        )
        return yield_

    def visit_unknown(self, unknown: j.Unknown, p: P) -> J:
        unknown = unknown.replace(
            prefix=self.visit_space(unknown.prefix, Space.Location.UNKNOWN_PREFIX, p)
        )
        temp_stmt = self.visit_statement(unknown, p)
        if not isinstance(temp_stmt, j.Unknown):
            return temp_stmt
        unknown = temp_stmt
        temp_expr = self.visit_expression(unknown, p)
        if not isinstance(temp_expr, j.Unknown):
            return temp_expr
        unknown = temp_expr
        unknown = unknown.replace(markers=self.visit_markers(unknown.markers, p))
        unknown = unknown.replace(
            source=self.visit_and_cast(unknown.source, j.Unknown.Source, p)
        )
        return unknown

    def visit_unknown_source(self, source: j.Unknown.Source, p: P) -> J:
        source = source.replace(
            prefix=self.visit_space(source.prefix, Space.Location.UNKNOWN_SOURCE_PREFIX, p)
        )
        source = source.replace(markers=self.visit_markers(source.markers, p))
        return source

    def visit_erroneous(self, erroneous: j.Erroneous, p: P) -> J:
        erroneous = erroneous.replace(
            prefix=self.visit_space(erroneous.prefix, Space.Location.ERRONEOUS_PREFIX, p)
        )
        temp_stmt = self.visit_statement(erroneous, p)
        if not isinstance(temp_stmt, j.Erroneous):
            return temp_stmt
        erroneous = temp_stmt
        temp_expr = self.visit_expression(erroneous, p)
        if not isinstance(temp_expr, j.Erroneous):
            return temp_expr
        erroneous = temp_expr
        erroneous = erroneous.replace(markers=self.visit_markers(erroneous.markers, p))
        return erroneous
