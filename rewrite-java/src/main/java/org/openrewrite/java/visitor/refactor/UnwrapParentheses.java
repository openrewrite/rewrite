/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.UUID;

public class UnwrapParentheses extends ScopedJavaRefactorVisitor {
    public UnwrapParentheses(UUID scope) {
        super(scope);
    }

    @Override
    public String getName() {
        return "core.UnwrapParentheses";
    }

    @Override
    public J visitExpression(Expression expr) {
        return isScope(expr) ? unwrapParens(expr) : expr;
    }

    //    @Override
//    public J visitArrayAccess(J.ArrayAccess arrayAccess) {
//        J.ArrayAccess a = refactor(arrayAccess, super::visitArrayAccess);
//
//        if (isScope(arrayAccess.getIndexed())) {
//            a = a.withIndexed(unwrapParens(a.getIndexed()));
//        }
//
//        if (isScope(arrayAccess.getDimension().getIndex())) {
//            a = a.withDimension(a.getDimension().withIndex(unwrapParens(a.getDimension().getIndex())));
//        }
//
//        return a;
//    }
//
//    @Override
//    public J visitAssert(J.Assert azzert) {
//        J.Assert a = refactor(azzert, super::visitAssert);
//        return isScope(azzert.getCondition()) ?
//                a.withCondition(unwrapParens(a.getCondition())) : a;
//    }
//
//    @Override
//    public J visitAssign(J.Assign assign) {
//        J.Assign a = refactor(assign, super::visitAssign);
//
//        if (isScope(assign.getAssignment())) {
//            a = a.withAssignment(unwrapParens(a.getAssignment()));
//        }
//
//        if (isScope(assign.getVariable())) {
//            a = a.withVariable(unwrapParens(a.getVariable()));
//        }
//
//        return a;
//    }
//
//    @Override
//    public J visitAssignOp(J.AssignOp assign) {
//        J.AssignOp a = refactor(assign, super::visitAssignOp);
//
//        if (isScope(assign.getAssignment())) {
//            a = a.withAssignment(unwrapParens(a.getAssignment()));
//        }
//
//        if (isScope(assign.getVariable())) {
//            a = a.withVariable(unwrapParens(a.getVariable()));
//        }
//
//        return a;
//    }
//
//    @Override
//    public J visitBinary(J.Binary binary) {
//        J.Binary b = refactor(binary, super::visitBinary);
//
//        if (isScope(binary.getLeft())) {
//            b = b.withLeft(unwrapParens(b.getLeft()));
//        }
//
//        if (isScope(binary.getRight())) {
//            b = b.withRight(unwrapParens(b.getRight()));
//        }
//
//        return b;
//    }
//
//    @Override
//    public J visitCase(J.Case caze) {
//        J.Case c = refactor(caze, super::visitCase);
//        return isScope(caze.getPattern()) ? c.withPattern(unwrapParens(caze.getPattern())) : c;
//    }
//
//    @Override
//    public J visitFieldAccess(J.FieldAccess fieldAccess) {
//        J.FieldAccess f = refactor(fieldAccess, super::visitFieldAccess);
//        return isScope(f.getTarget()) ? f.withTarget(unwrapParens(f.getTarget())) : f;
//    }
//
//    @Override
//    public J visitForEachLoop(J.ForEachLoop forEachLoop) {
//        J.ForEachLoop f = refactor(forEachLoop, super::visitForEachLoop);
//        J.ForEachLoop.Control control = f.getControl();
//        return isScope(control.getIterable()) ?
//                f.withControl(control.withIterable(unwrapParens(control.getIterable()))) : f;
//    }
//
//    @Override
//    public J visitForLoop(J.ForLoop forLoop) {
//        J.ForLoop f = refactor(forLoop, super::visitForLoop);
//        J.ForLoop.Control control = f.getControl();
//        return isScope(control.getCondition()) ?
//                f.withControl(control.withCondition(unwrapParens(control.getCondition()))) : f;
//    }
//
//    @Override
//    public J visitIf(J.If iff) {
//        J.If i = refactor(iff, super::visitIf);
//        J.Parentheses<Expression> ifCondition = i.getIfCondition();
//        return isScope(ifCondition.getTree()) ?
//                i.withIfCondition(ifCondition.withTree(unwrapParens(ifCondition.getTree()))) : i;
//    }
//
//    @Override
//    public J visitInstanceOf(J.InstanceOf instanceOf) {
//        J.InstanceOf i = refactor(instanceOf, super::visitInstanceOf);
//        return isScope(i.getExpr()) ? i.withExpr(unwrapParens(i.getExpr())) : i;
//    }
//
//    @Override
//    public J visitMemberReference(J.MemberReference memberRef) {
//        J.MemberReference m = refactor(memberRef, super::visitMemberReference);
//        return isScope(m.getContaining()) ? m.withContaining(unwrapParens(m.getContaining()));
//    }
//
//    @Override
//    public J visitMethodInvocation(J.MethodInvocation methodInvocation) {
//        J.MethodInvocation m = refactor(methodInvocation, super::visitMethodInvocation);
//        return isScope(m.getSelect()) ? m.withSelect(unwrapParens(m.getSelect())) : m;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public J visitNewArray(J.NewArray newArray) {
//        J.NewArray n = refactor(newArray, super::visitNewArray);
//
//        if(newArray.getDimensions().stream().anyMatch(d -> isScope(d.getSize()))) {
//            n = n.withDimensions(n.getDimensions().stream()
//                    .map(dim -> isScope(dim.getSize()) ?
//                            dim.withSize(((J.Parentheses<Expression>) dim.getSize()).getTree()) :
//                            dim
//                    )
//                    .collect(toList()));
//        }
//
//        return n;
//    }

//    @Override
//    public J visitReturn(J.Return retrn) {
//
//
//        return maybeTransform(retrn,
//                retrn.getExpr() != null && scope.equals(retrn.getExpr().getId()),
//                super::visitReturn,
//                J.Return::getExpr,
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitTernary(J.Ternary ternary) {
//        List<AstTransform> changes = maybeTransform(ternary,
//                scope.equals(ternary.getCondition().getId()),
//                super::visitTernary,
//                J.Ternary::getCondition,
//                UNWRAP_PARENS);
//
//        if (scope.equals(ternary.getTruePart().getId())) {
//            changes.addAll(transform(ternary.getTruePart(), UNWRAP_PARENS));
//        }
//
//        if (scope.equals(ternary.getFalsePart().getId())) {
//            changes.addAll(transform(ternary.getFalsePart(), UNWRAP_PARENS));
//        }
//
//        return changes;
//    }
//
//    @Override
//    public J visitThrow(J.Throw thrown) {
//        return maybeTransform(thrown,
//                scope.equals(thrown.getException().getId()),
//                super::visitThrow,
//                J.Throw::getException,
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitTypeCast(J.TypeCast typeCast) {
//        return maybeTransform(typeCast,
//                scope.equals(typeCast.getExpr().getId()),
//                super::visitTypeCast,
//                J.TypeCast::getExpr,
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitUnary(J.Unary unary) {
//        return maybeTransform(unary,
//                scope.equals(unary.getExpr().getId()),
//                super::visitUnary,
//                J.Unary::getExpr,
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitVariable(J.VariableDecls.NamedVar variable) {
//        return maybeTransform(variable,
//                variable.getInitializer() != null && scope.equals(variable.getInitializer().getId()),
//                super::visitVariable,
//                J.VariableDecls.NamedVar::getInitializer,
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
//        return maybeTransform(doWhileLoop,
//                scope.equals(doWhileLoop.getWhileCondition().getCondition().getTree().getId()),
//                super::visitDoWhileLoop,
//                w -> w.getWhileCondition().getCondition().getTree(),
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitWhileLoop(J.WhileLoop whileLoop) {
//        return maybeTransform(whileLoop,
//                scope.equals(whileLoop.getCondition().getTree().getId()),
//                super::visitWhileLoop,
//                w -> w.getCondition().getTree(),
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitSwitch(J.Switch switzh) {
//        return maybeTransform(switzh,
//                scope.equals(switzh.getSelector().getTree().getId()),
//                super::visitSwitch,
//                s -> s.getSelector().getTree(),
//                UNWRAP_PARENS);
//    }
//
//    @Override
//    public J visitSynchronized(J.Synchronized synch) {
//        return maybeTransform(synch,
//                scope.equals(synch.getLock().getTree().getId()),
//                super::visitSynchronized,
//                s -> s.getLock().getTree(),
//                UNWRAP_PARENS);
//    }

    private Expression unwrapParens(@Nullable Expression e) {
        return e instanceof J.Parentheses ?
                ((J.Parentheses<?>) e).getTree().withFormatting(e.getFormatting()) : e;
    }
}
