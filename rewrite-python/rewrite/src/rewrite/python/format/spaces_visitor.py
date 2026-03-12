from typing import Optional, cast, List, TypeVar

import rewrite.java as j
from rewrite import Tree, list_map
from rewrite.java import (J, Assignment, JLeftPadded, AssignmentOperation, MemberReference, MethodInvocation,
                           MethodDeclaration, Empty, ArrayAccess, Space, If, Block, ClassDeclaration,
                           VariableDeclarations, JRightPadded, Import, ParameterizedType, Parentheses, Try,
                           ControlParentheses, TrailingComma)
from rewrite.python import (PythonVisitor, SpacesStyle, Binary, ChainedAssignment, Slice, CollectionLiteral,
                             DictLiteral, KeyValue, TypeHint, MultiImport, ExpressionTypeTree,
                             ComprehensionExpression, NamedArgument)
from rewrite.visitor import P, Cursor

J2 = TypeVar('J2', bound=J)


class SpacesVisitor(PythonVisitor):
    def __init__(self, style: SpacesStyle, stop_after: Optional[Tree] = None):
        self._style = style
        self._before_parentheses = style.before_parentheses
        self._stop_after = stop_after

    def visit_class_declaration(self, class_decl: ClassDeclaration, p: P) -> J:
        c = cast(ClassDeclaration, super().visit_class_declaration(class_decl, p))

        if c.padding.implements is not None:
            c = c.padding.replace(
                implements=space_before_container(c.padding.implements, self._style.before_parentheses.method_call)
            )

            param_size = len(c.padding.implements.elements)  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
            use_space = self._style.within.method_call_parentheses

            def _process_argument(index, arg, args_size):
                if index == 0:
                    arg = arg.replace(element=space_before(arg.element, use_space))
                else:
                    arg = arg.replace(element=space_before(arg.element, self._style.other.after_comma))

                if index == args_size - 1:
                    arg = space_after(arg, use_space)
                else:
                    arg = space_after(arg, self._style.other.before_comma)

                return arg

            if c.implements:
                c = c.padding.replace(
                    implements=c.padding.implements.padding.replace(  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
                        elements=list_map(lambda arg, index: _process_argument(index, arg, param_size),
                                          c.padding.implements.padding.elements)))  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
        return c

    def visit_method_declaration(self, method: MethodDeclaration, p: P) -> J:
        m: MethodDeclaration = cast(MethodDeclaration, super().visit_method_declaration(method, p))

        m = m.padding.replace(
            parameters=space_before_container(m.padding.parameters, self._style.before_parentheses.method_declaration)
        )

        param_size = len(m.parameters)
        use_space = self._style.within.method_call_parentheses

        def _process_argument(index, arg, args_size):
            if isinstance(arg.element, VariableDeclarations) and arg.element.type_expression:
                vd = cast(VariableDeclarations, arg.element)
                arg = arg.replace(element=vd.replace(type_expression=
                    space_before(vd.type_expression, self._style.other.after_colon))
                                       .padding.replace(variables=
                    list_map(lambda v: space_after_right_padded(v, self._style.other.before_colon),
                             vd.padding.variables))
                                       )

            if index == 0:
                arg = arg.replace(element=space_before(arg.element, use_space))
            else:
                arg = arg.replace(element=space_before(arg.element, self._style.other.after_comma))

            if index == args_size - 1:
                arg = space_after(arg, use_space)
            else:
                arg = space_after(arg, self._style.other.before_comma)
            return arg

        m = m.padding.replace(
            parameters=m.padding.parameters.padding.replace(
                elements=list_map(lambda arg, idx: _process_argument(idx, arg, param_size),
                                  m.padding.parameters.padding.elements)
            )
        )

        if m.return_type_expression is not None:
            m = m.replace(return_type_expression=space_before(m.return_type_expression, True))

        if m.padding.type_parameters is not None:
            raise NotImplementedError("Type parameters are not supported yet")

        return m.padding.replace(
            parameters=m.padding.parameters.replace(
                before=Space.SINGLE_SPACE if self._before_parentheses.method_declaration else Space.EMPTY
            )
        )

    def visit_catch(self, catch: Try.Catch, p: P) -> J:
        c = cast(Try.Catch, super().visit_catch(catch, p))
        return c

    def visit_control_parentheses(self, control_parens: ControlParentheses[J2], p: P) -> J:
        cp = cast(ControlParentheses[J2], super().visit_control_parentheses(control_parens, p))
        cp = space_before(cp, False)
        cp = cp.padding.replace(tree=cp.padding.tree.replace(element=space_before(cp.tree, True)))
        return cp

    def visit_named_argument(self, named: NamedArgument, p: P) -> J:
        a = cast(NamedArgument, super().visit_named_argument(named, p))
        if a.padding.value is not None:
            a = a.padding.replace(
                value=space_before_left_padded(a.padding.value, self._style.around_operators.eq_in_keyword_argument))
            return a.padding.replace(
                value=space_before_left_padded_element(a.padding.value, self._style.around_operators.eq_in_keyword_argument))
        return a

    @staticmethod
    def _part_of_method_header(cursor: Cursor) -> bool:
        if (c := cursor.parent_tree_cursor()) and isinstance(c.value, VariableDeclarations):
            return c.parent_tree_cursor() is not None and isinstance(c.parent_tree_cursor().value, MethodDeclaration)
        return False

    def visit_variable(self, variable: VariableDeclarations.NamedVariable, p: P) -> J:
        v = cast(VariableDeclarations.NamedVariable, super().visit_variable(variable, p))

        if not self._part_of_method_header(self.cursor):
            return v

        if v.padding.initializer is not None and v.padding.initializer.element is not None:
            use_space = self._style.around_operators.eq_in_named_parameter or v.variable_type is not None
            use_space |= self.cursor.first_enclosing_or_throw(VariableDeclarations).type_expression is not None

            v = v.padding.replace(
                initializer=space_before_left_padded(v.padding.initializer, use_space))
            v = v.padding.replace(
                initializer=space_before_left_padded_element(v.padding.initializer, use_space))
        return v

    def visit_block(self, block: Block, p: P) -> J:
        b = cast(Block, super().visit_block(block, p))
        b = space_before(b, self._style.other.before_colon)
        return b

    def visit_method_invocation(self, method: MethodInvocation, p: P) -> J:
        m: MethodInvocation = cast(MethodInvocation, super().visit_method_invocation(method, p))

        m = m.padding.replace(arguments=m.padding.arguments.replace(
            before=Space.SINGLE_SPACE if self._style.before_parentheses.method_call else Space.EMPTY))

        if not m.arguments or isinstance(m.arguments[0], Empty):
            use_space = self._style.within.empty_method_call_parentheses
            m = m.padding.replace(
                arguments=m.padding.arguments.padding.replace(
                    elements=list_map(lambda arg: arg.replace(element=space_before(arg.element, use_space)),
                                      m.padding.arguments.padding.elements)
                )
            )
        else:
            args_size = len(m.arguments)
            use_space = self._style.within.method_call_parentheses

            def _process_argument(index, arg, args_size, use_space):
                if index == 0:
                    arg = arg.replace(element=space_before(arg.element, use_space))
                else:
                    arg = arg.replace(element=space_before(arg.element, self._style.other.after_comma))

                if index == args_size - 1:
                    arg = space_after(arg, use_space)
                else:
                    arg = space_after(arg, self._style.other.before_comma)

                return arg

            m = m.padding.replace(
                arguments=m.padding.arguments.padding.replace(
                    elements=list_map(lambda arg, idx: _process_argument(idx, arg, args_size, use_space),
                                      m.padding.arguments.padding.elements)))

        return m

    def visit_array_access(self, array_access: ArrayAccess, p: P) -> J:
        a: ArrayAccess = cast(ArrayAccess, super().visit_array_access(array_access, p))
        use_space_within_brackets = self._style.within.brackets
        index_padding = a.dimension.padding.index
        element_prefix = update_space(index_padding.element.prefix, use_space_within_brackets)
        index_after = update_space(index_padding.after, use_space_within_brackets)

        a = a.replace(dimension=
            a.dimension.padding.replace(
                index=index_padding.replace(
                    element=index_padding.element.replace(prefix=element_prefix),
                    after=index_after,
                )
            ).replace(prefix=update_space(a.dimension.prefix, self._style.before_parentheses.left_bracket))
        )
        return a

    def visit_assignment(self, assignment: Assignment, p: P) -> J:
        a: Assignment = cast(Assignment, super().visit_assignment(assignment, p))

        if isinstance(self.cursor.parent_tree_cursor().value, j.Try.Resource):
            return a

        a = a.padding.replace(
            assignment=space_before_left_padded(a.padding.assignment, self._style.around_operators.assignment))
        a = a.padding.replace(
            assignment=a.padding.assignment.replace(
                element=space_before(a.padding.assignment.element, self._style.around_operators.assignment)))
        return a

    def visit_assignment_operation(self, assign_op: AssignmentOperation, p: P) -> J:
        a: AssignmentOperation = cast(AssignmentOperation, super().visit_assignment_operation(assign_op, p))
        operator: JLeftPadded = a.padding.operator
        a = a.padding.replace(
            operator=operator.replace(before=update_space(operator.before, self._style.around_operators.assignment)))
        return a.replace(assignment=space_before(a.assignment, self._style.around_operators.assignment))

    def visit_chained_assignment(self, chained: ChainedAssignment, p: P) -> J:
        a: ChainedAssignment = cast(ChainedAssignment, super().visit_chained_assignment(chained, p))
        a = a.padding.replace(
            variables=list_map(lambda v: v.replace(after=update_space(v.after, self._style.around_operators.assignment)), a.padding.variables))

        a = a.padding.replace(
            variables=list_map(lambda v, idx: v.replace(element=
                space_before(v.element, self._style.around_operators.assignment if idx >= 1 else False)), a.padding.variables))

        return a.replace(assignment=space_before(a.assignment, self._style.around_operators.assignment))

    def visit_member_reference(self, member_ref: MemberReference, p: P) -> J:
        m: MemberReference = cast(MemberReference, super().visit_member_reference(member_ref, p))

        if m.padding.type_parameters is not None:
            raise NotImplementedError("Type parameters are not supported yet")

        return m

    def _apply_binary_space_around(self, binary, use_space_around: bool):
        operator = binary.padding.operator
        binary = binary.padding.replace(
            operator=operator.replace(before=update_space(operator.before, use_space_around))
        )
        return binary.replace(right=space_before(binary.right, use_space_around))

    def visit_binary(self, binary: j.Binary, p: P) -> J:
        b: j.Binary = cast(j.Binary, super().visit_binary(binary, p))
        op: j.Binary.Type = b.operator

        if op in [j.Binary.Type.Addition, j.Binary.Type.Subtraction]:
            b = self._apply_binary_space_around(b, self._style.around_operators.additive)
        elif op in [j.Binary.Type.Multiplication, j.Binary.Type.Division, j.Binary.Type.Modulo]:
            b = self._apply_binary_space_around(b, self._style.around_operators.multiplicative)
        elif op in [j.Binary.Type.Equal, j.Binary.Type.NotEqual]:
            b = self._apply_binary_space_around(b, self._style.around_operators.equality)
        elif op in [j.Binary.Type.LessThan, j.Binary.Type.GreaterThan, j.Binary.Type.LessThanOrEqual,
                    j.Binary.Type.GreaterThanOrEqual]:
            b = self._apply_binary_space_around(b, self._style.around_operators.relational)
        elif op in [j.Binary.Type.BitAnd, j.Binary.Type.BitOr, j.Binary.Type.BitXor]:
            b = self._apply_binary_space_around(b, self._style.around_operators.bitwise)
        elif op in [j.Binary.Type.LeftShift, j.Binary.Type.RightShift, j.Binary.Type.UnsignedRightShift]:
            b = self._apply_binary_space_around(b, self._style.around_operators.shift)
        elif op in [j.Binary.Type.Or, j.Binary.Type.And]:
            b = self._apply_binary_space_around(b, True)
        else:
            raise NotImplementedError(f"Operation {op} is not supported yet")
        return b

    def visit_python_binary(self, binary: Binary, p: P) -> J:
        b: Binary = cast(Binary, super().visit_python_binary(binary, p))
        op: Binary.Type = b.operator

        if op == Binary.Type.In or op == Binary.Type.Is or op == Binary.Type.IsNot or op == Binary.Type.NotIn:
            b = self._apply_binary_space_around(b, True)
        elif op in [Binary.Type.FloorDivision, Binary.Type.MatrixMultiplication]:
            b = self._apply_binary_space_around(b, self._style.around_operators.multiplicative)
        elif op == Binary.Type.Power:
            b = self._apply_binary_space_around(b, self._style.around_operators.power)

        return b

    def visit_if(self, if_: If, p: P) -> J:
        if_: j.If = cast(If, super().visit_if(if_, p))

        if_ = if_.replace(if_condition=
            if_.if_condition.padding.replace(
                tree=space_after(if_.if_condition.padding.tree, self._style.other.before_colon))
        )
        return if_

    def visit_else(self, else_: If.Else, p: P) -> J:
        e: j.If.Else = cast(j.If.Else, super().visit_else(else_, p))
        e = e.padding.replace(body=space_before_right_padded_element(e.padding.body, self._style.other.before_colon))
        return e

    def visit_slice(self, slice_: Slice, p: P) -> J:
        s: Slice = cast(Slice, super().visit_slice(slice_, p))
        use_space = self._style.within.brackets

        if s.padding.start is not None:
            s = s.padding.replace(start=space_before_right_padded_element(s.padding.start, use_space))
            s = s.padding.replace(start=space_after_right_padded(s.padding.start, use_space))
        if s.padding.stop is not None:
            s = s.padding.replace(stop=space_before_right_padded_element(s.padding.stop, use_space))
            s = s.padding.replace(stop=space_after_right_padded(s.padding.stop, use_space))

            if s.padding.step is not None:
                s = s.padding.replace(step=space_before_right_padded_element(s.padding.step, use_space))
                s = s.padding.replace(step=space_after_right_padded(s.padding.step, use_space))
        return s

    def visit_for_each_loop(self, for_each: j.ForEachLoop, p: P) -> J:
        fl = cast(j.ForEachLoop, super().visit_for_each_loop(for_each, p))
        control = fl.control

        # Set single space before loop target e.g. for    i in...: <-> for i in ...:
        var_rp = control.padding.variable
        var_rp = var_rp.replace(element=space_before(var_rp.element, True))

        # Set single space before 'in' keyword
        var_rp = space_after_right_padded(var_rp, True)

        # Set single space before loop iterable e.g. for i in    []: <-> for i in []:
        iter_rp = control.padding.iterable
        iter_rp = iter_rp.replace(element=space_before(iter_rp.element, True))

        control = control.padding.replace(variable=var_rp, iterable=iter_rp)
        return fl.replace(control=control)

    def visit_parameterized_type(self, parameterized_type: ParameterizedType, p: P) -> J:
        pt = cast(ParameterizedType, super().visit_parameterized_type(parameterized_type, p))

        def _process_element(index, arg, last, use_space):
            if index == 0:
                arg = arg.replace(element=space_before(arg.element, use_space))
            else:
                arg = arg.replace(element=space_before(arg.element, self._style.other.after_comma))

            if last:
                arg = space_after(arg, use_space and arg.markers.find_first(TrailingComma) is None)
                arg = arg.replace(markers=arg.markers.compute_by_type(TrailingComma, self._remap_trailing_comma_space))
            else:
                arg = space_after(arg, self._style.other.before_comma)

            return arg

        pt = pt.padding.replace(
            type_parameters=pt.padding.type_parameters.padding.replace(  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
                elements=list_map(
                    lambda arg, idx: _process_element(idx, arg,
                                                      last=idx == len(pt.padding.type_parameters.padding.elements) - 1,  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
                                                      use_space=self._style.within.brackets),
                    pt.padding.type_parameters.padding.elements  # ty: ignore[unresolved-attribute]  # guarded by truthiness check above
                )
            )
        )

        return pt

    def visit_parentheses(self, parens: Parentheses, p: P) -> J:
        p2 = cast(Parentheses, super().visit_parentheses(parens, p))
        p2 = p2.padding.replace(tree=p2.padding.tree.replace(after=update_space(p2.padding.tree.after, False)))
        return p2

    def visit_collection_literal(self, collection: CollectionLiteral, p: P) -> J:
        cl = cast(CollectionLiteral, super().visit_collection_literal(collection, p))

        def _process_element(index, arg, args_size, use_space):
            if index == 0:
                arg = arg.replace(element=space_before(arg.element, use_space))
            else:
                arg = arg.replace(element=space_before(arg.element, self._style.other.after_comma))

            if index == args_size - 1:
                arg = space_after(arg, use_space and arg.markers.find_first(TrailingComma) is None)
                arg = arg.replace(markers=arg.markers.compute_by_type(TrailingComma, self._remap_trailing_comma_space))
            else:
                arg = space_after(arg, self._style.other.before_comma)

            return arg

        if cl.kind == CollectionLiteral.Kind.SET:
            _space_style = self._style.within.braces
        elif cl.kind == CollectionLiteral.Kind.LIST:
            _space_style = self._style.within.brackets
        elif cl.kind == CollectionLiteral.Kind.TUPLE:
            _space_style = self._style.within.brackets if self.cursor.first_enclosing(ExpressionTypeTree) else False

        cl = cl.padding.replace(
            elements=cl.padding.elements.padding.replace(
                elements=list_map(
                    lambda arg, idx: _process_element(idx, arg,
                                                      args_size=len(cl.padding.elements.padding.elements),
                                                      use_space=_space_style),
                    cl.padding.elements.padding.elements)))

        return cl

    def visit_dict_literal(self, dict_lit: DictLiteral, p: P) -> J:
        dl = cast(DictLiteral, super().visit_dict_literal(dict_lit, p))

        def _process_kv_pair(c: JRightPadded[KeyValue], idx: int, arg_size) -> JRightPadded[KeyValue]:
            if idx == 0 or idx == arg_size - 1:
                if idx == 0:
                    before_comma_style = self._style.other.before_comma
                else:
                    before_comma_style = self._style.within.braces and c.markers.find_first(TrailingComma) is None
                after_comma_style = self._style.within.braces if idx == 0 else self._style.other.after_comma
                c = space_after_right_padded(c, before_comma_style)
                c = space_before_right_padded_element(c, after_comma_style)
            else:
                c = space_after_right_padded(c, self._style.other.before_comma)
                c = space_before_right_padded_element(c, self._style.other.after_comma)

            if idx == arg_size - 1:
                c = c.replace(markers=c.markers.compute_by_type(TrailingComma, self._remap_trailing_comma_space))

            return c

        dl = dl.padding.replace(
            elements=dl.padding.elements.padding.replace(
                elements=list_map(lambda x, idx: _process_kv_pair(x, idx, len(dl.padding.elements.padding.elements)),
                                  dl.padding.elements.padding.elements)
            )
        )
        return dl

    def visit_multi_import(self, multi: MultiImport, p: P) -> J:
        mi: MultiImport = cast(MultiImport, super().visit_multi_import(multi, p))

        if mi.padding.from_ is not None:
            mi = mi.padding.replace(
                from_=space_before_right_padded_element(mi.padding.from_, True)
            )

        mi = mi.padding.replace(
            names=space_before_container(mi.padding.names, True)
        )

        _names = mi.padding.names

        _names = _names.padding.replace(
            elements=list_map(lambda x, idx: space_after_right_padded(x, self._style.other.before_comma),
                              _names.padding.elements)
        )
        _names = _names.padding.replace(
            elements=list_map(lambda x, idx: space_before_right_padded_element(x, self._style.other.after_comma and idx != 0),
                              _names.padding.elements)
        )

        return mi.padding.replace(names=_names)

    def visit_import(self, import_: Import, p: P) -> J:
        imp: Import = cast(Import, super().visit_import(import_, p))
        if imp.padding.alias:
            imp = imp.replace(alias=space_before(imp.alias, True))
            imp = imp.padding.replace(alias=space_before_left_padded(imp.padding.alias, True))
        return imp

    def visit_type_hint(self, hint: TypeHint, p: P) -> J:
        th: TypeHint = cast(TypeHint, super().visit_type_hint(hint, p))
        # Don't apply before_colon to return type annotations (handled by visit_method_declaration)
        parent = self.cursor.parent_tree_cursor()
        if not (parent and isinstance(parent.value, MethodDeclaration)):
            th = space_before(th, self._style.other.before_colon)
        th = th.replace(type_tree=space_before(th.type_tree, self._style.other.after_colon))
        return th

    def visit_expression_type_tree(self, expr_tree: ExpressionTypeTree, p: P) -> J:
        ett = cast(ExpressionTypeTree, super().visit_expression_type_tree(expr_tree, p))
        # Don't remove space when inside ClassDeclaration (handled by visit_class_declaration)
        parent = self.cursor.parent_tree_cursor()
        if not (parent and isinstance(parent.value, ClassDeclaration)):
            ett = space_before(ett, False)
        return ett

    def visit_comprehension_expression(self, comp: ComprehensionExpression, p: P) -> J:
        ce = cast(ComprehensionExpression, super().visit_comprehension_expression(comp, p))

        if ce.kind == ComprehensionExpression.Kind.LIST:
            ce = ce.replace(result=space_before(ce.result, self._style.within.brackets))
            ce = ce.replace(suffix=update_space(ce.suffix, self._style.within.brackets))
        elif ce.kind == ComprehensionExpression.Kind.GENERATOR:
            ce = ce.replace(result=space_before(ce.result, False))
            ce = ce.replace(suffix=update_space(ce.suffix, False))
        elif ce.kind in (ComprehensionExpression.Kind.SET, ComprehensionExpression.Kind.DICT):
            ce = ce.replace(result=space_before(ce.result, self._style.within.braces))
            ce = ce.replace(suffix=update_space(ce.suffix, self._style.within.braces))

        return ce

    def visit_key_value(self, kv: KeyValue, p: P) -> J:
        kv = cast(KeyValue, super().visit_key_value(kv, p))
        kv = kv.replace(value=space_before(kv.value, self._style.other.after_colon))
        return kv.padding.replace(key=space_after_right_padded(kv.padding.key, self._style.other.before_colon))

    def visit_comprehension_condition(self, condition: ComprehensionExpression.Condition, p: P) -> ComprehensionExpression.Condition:
        cond = super().visit_comprehension_condition(condition, p)
        cond = space_before(cond, True)
        cond = cond.replace(expression=space_before(cond.expression, True))
        return cond

    def visit_comprehension_clause(self, clause: ComprehensionExpression.Clause, p: P) -> ComprehensionExpression.Clause:
        cc = super().visit_comprehension_clause(clause, p)

        cc = space_before(cc, True)

        cc = cc.padding.replace(iterated_list=space_before_left_padded(cc.padding.iterated_list, True))
        cc = cc.replace(iterator_variable=space_before(cc.iterator_variable, True))
        cc = cc.padding.replace(iterated_list=space_before_left_padded_element(cc.padding.iterated_list, True))
        return cc

    def _remap_trailing_comma_space(self, tc: TrailingComma) -> TrailingComma:
        return tc.replace(suffix=update_space(tc.suffix, self._style.other.after_comma))


def space_before(j: J2, add_space: bool) -> J2:
    prefix: Space = j.prefix

    if prefix.comments or ('\n' in prefix.whitespace):
        return j

    if add_space and not_single_space(prefix.whitespace):
        return j.replace(prefix=prefix.replace(whitespace=" "))
    elif not add_space and only_spaces_and_not_empty(prefix.whitespace):
        return j.replace(prefix=prefix.replace(whitespace=""))

    return j


def space_before_container(container: j.JContainer, add_space: bool) -> j.JContainer:
    if container.before.comments:
        comments: List[j.Comment] = space_last_comment_suffix(container.before.comments, add_space)
        return container.replace(before=container.before.replace(comments=comments))

    if add_space and not_single_space(container.before.whitespace):
        return container.replace(before=container.before.replace(whitespace=" "))
    elif not add_space and only_spaces_and_not_empty(container.before.whitespace):
        return container.replace(before=container.before.replace(whitespace=""))
    else:
        return container


def space_before_left_padded_element(left: j.JLeftPadded, add_space: bool) -> j.JLeftPadded:
    return left.replace(element=space_before(left.element, add_space))


def space_before_right_padded_element(right: j.JRightPadded, add_space: bool) -> j.JRightPadded:
    return right.replace(element=space_before(right.element, add_space))


def space_last_comment_suffix(comments: List[j.Comment], add_space: bool) -> List[j.Comment]:
    return list_map(lambda c, i: space_suffix(c, add_space) if i == len(comments) - 1 else c, comments)


def space_suffix(comment: j.Comment, add_space: bool) -> j.Comment:
    if add_space and not_single_space(comment.suffix):
        return comment.replace(suffix=" ")
    elif not add_space and only_spaces_and_not_empty(comment.suffix):
        return comment.replace(suffix="")
    else:
        return comment


def space_before_left_padded(j: JLeftPadded[J2], add_space) -> JLeftPadded[J2]:
    space: Space = j.before
    if space.comments or '\\' in space.whitespace:
        return j

    if add_space and not_single_space(space.whitespace):
        return j.replace(before=space.replace(whitespace=" "))
    elif not add_space and only_spaces_and_not_empty(space.whitespace):
        return j.replace(before=space.replace(whitespace=""))
    return j


def space_after(j: J2, add_space: bool) -> J2:
    space: Space = cast(Space, j.after)  # ty: ignore[unresolved-attribute]  # J2 is JRightPadded at call site
    if space.comments or '\\' in space.whitespace:
        return j

    if add_space and not_single_space(space.whitespace):
        return j.replace(after=space.replace(whitespace=" "))
    elif not add_space and only_spaces_and_not_empty(space.whitespace):
        return j.replace(after=space.replace(whitespace=""))
    return j


def space_after_right_padded(right: JRightPadded[J2], add_space: bool) -> JRightPadded[J2]:
    space: Space = right.after
    if space.comments or '\\' in space.whitespace:
        return right

    if add_space and not_single_space(space.whitespace):
        return right.replace(after=space.replace(whitespace=" "))
    elif not add_space and only_spaces_and_not_empty(space.whitespace):
        return right.replace(after=space.replace(whitespace=""))
    return right


def update_space(s: Space, have_space: bool) -> Space:
    if s.comments:
        return s

    if have_space and not_single_space(s.whitespace):
        return s.replace(whitespace=" ")
    elif not have_space and only_spaces_and_not_empty(s.whitespace):
        return s.replace(whitespace="")
    else:
        return s


def only_spaces(s: Optional[str]) -> bool:
    return s is not None and all(c in {' ', '\t'} for c in s)


def only_spaces_and_not_empty(s: Optional[str]) -> bool:
    return bool(s) and only_spaces(s)


def not_single_space(s: Optional[str]) -> bool:
    return s is not None and only_spaces(s) and s != " "
