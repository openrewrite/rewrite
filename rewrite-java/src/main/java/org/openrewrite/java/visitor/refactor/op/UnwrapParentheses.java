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
package org.openrewrite.java.visitor.refactor.op;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.visitor.refactor.AstTransform;
import org.openrewrite.java.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class UnwrapParentheses extends ScopedRefactorVisitor {
    private static final Function<Expression, Expression> UNWRAP_PARENS = p -> p instanceof J.Parentheses ?
            ((J.Parentheses<?>) p).getTree().withFormatting(p.getFormatting()) :
            p;

    public UnwrapParentheses(UUID scope) {
        super(scope);
    }

    @Override
    public String getRuleName() {
        return "core.UnwrapParentheses";
    }

    @Override
    public List<AstTransform> visitArrayAccess(J.ArrayAccess arrayAccess) {
        List<AstTransform> changes = maybeTransform(arrayAccess,
                scope.equals(arrayAccess.getIndexed().getId()),
                super::visitArrayAccess,
                J.ArrayAccess::getIndexed,
                UNWRAP_PARENS);

        if (scope.equals(arrayAccess.getDimension().getIndex().getId())) {
            changes.addAll(transform(arrayAccess.getDimension().getIndex(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssert(J.Assert azzert) {
        return maybeTransform(azzert,
                scope.equals(azzert.getCondition().getId()),
                super::visitAssert,
                J.Assert::getCondition,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitAssign(J.Assign assign) {
        List<AstTransform> changes = maybeTransform(assign,
                scope.equals(assign.getAssignment().getId()),
                super::visitAssign,
                J.Assign::getAssignment,
                UNWRAP_PARENS);

        if (scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssignOp(J.AssignOp assign) {
        List<AstTransform> changes = maybeTransform(assign,
                scope.equals(assign.getAssignment().getId()),
                super::visitAssignOp,
                J.AssignOp::getAssignment,
                UNWRAP_PARENS);

        if (scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitBinary(J.Binary binary) {
        List<AstTransform> changes = maybeTransform(binary,
                scope.equals(binary.getLeft().getId()),
                super::visitBinary,
                J.Binary::getLeft,
                UNWRAP_PARENS);

        if (scope.equals(binary.getRight().getId())) {
            changes.addAll(transform(binary.getRight(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitCase(J.Case caze) {
        return maybeTransform(caze,
                caze.getPattern() != null && scope.equals(caze.getPattern().getId()),
                super::visitCase,
                J.Case::getPattern,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitFieldAccess(J.FieldAccess fieldAccess) {
        return maybeTransform(fieldAccess,
                scope.equals(fieldAccess.getTarget().getId()),
                super::visitFieldAccess,
                J.FieldAccess::getTarget,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitForEachLoop(J.ForEachLoop forEachLoop) {
        return maybeTransform(forEachLoop,
                scope.equals(forEachLoop.getControl().getIterable().getId()),
                super::visitForEachLoop,
                f -> f.getControl().getIterable(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitForLoop(J.ForLoop forLoop) {
        return maybeTransform(forLoop,
                scope.equals(forLoop.getControl().getCondition().getId()),
                super::visitForLoop,
                f -> f.getControl().getCondition(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitIf(J.If iff) {
        return maybeTransform(iff,
                scope.equals(iff.getIfCondition().getTree().getId()),
                super::visitIf,
                i -> i.getIfCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitInstanceOf(J.InstanceOf instanceOf) {
        return maybeTransform(instanceOf,
                scope.equals(instanceOf.getExpr().getId()),
                super::visitInstanceOf,
                J.InstanceOf::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitMemberReference(J.MemberReference memberRef) {
        return maybeTransform(memberRef,
                scope.equals(memberRef.getContaining().getId()),
                super::visitMemberReference,
                J.MemberReference::getContaining,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation methodInvocation) {
        return maybeTransform(methodInvocation,
                methodInvocation.getSelect() != null && scope.equals(methodInvocation.getSelect().getId()),
                super::visitMethodInvocation,
                J.MethodInvocation::getSelect,
                UNWRAP_PARENS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AstTransform> visitNewArray(J.NewArray newArray) {
        return maybeTransform(newArray,
                newArray.getDimensions().stream().anyMatch(d -> d.getSize().getId().equals(scope)),
                super::visitNewArray,
                na -> na
                        .withDimensions(na.getDimensions().stream()
                                .map(dim -> dim.getSize().getId().equals(scope) ?
                                        dim.withSize(((J.Parentheses<Expression>) dim.getSize()).getTree()) :
                                        dim
                                )
                                .collect(toList())
                        )
        );
    }

    @Override
    public List<AstTransform> visitReturn(J.Return retrn) {
        return maybeTransform(retrn,
                retrn.getExpr() != null && scope.equals(retrn.getExpr().getId()),
                super::visitReturn,
                J.Return::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitTernary(J.Ternary ternary) {
        List<AstTransform> changes = maybeTransform(ternary,
                scope.equals(ternary.getCondition().getId()),
                super::visitTernary,
                J.Ternary::getCondition,
                UNWRAP_PARENS);

        if (scope.equals(ternary.getTruePart().getId())) {
            changes.addAll(transform(ternary.getTruePart(), UNWRAP_PARENS));
        }

        if (scope.equals(ternary.getFalsePart().getId())) {
            changes.addAll(transform(ternary.getFalsePart(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitThrow(J.Throw thrown) {
        return maybeTransform(thrown,
                scope.equals(thrown.getException().getId()),
                super::visitThrow,
                J.Throw::getException,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitTypeCast(J.TypeCast typeCast) {
        return maybeTransform(typeCast,
                scope.equals(typeCast.getExpr().getId()),
                super::visitTypeCast,
                J.TypeCast::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitUnary(J.Unary unary) {
        return maybeTransform(unary,
                scope.equals(unary.getExpr().getId()),
                super::visitUnary,
                J.Unary::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitVariable(J.VariableDecls.NamedVar variable) {
        return maybeTransform(variable,
                variable.getInitializer() != null && scope.equals(variable.getInitializer().getId()),
                super::visitVariable,
                J.VariableDecls.NamedVar::getInitializer,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return maybeTransform(doWhileLoop,
                scope.equals(doWhileLoop.getWhileCondition().getCondition().getTree().getId()),
                super::visitDoWhileLoop,
                w -> w.getWhileCondition().getCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitWhileLoop(J.WhileLoop whileLoop) {
        return maybeTransform(whileLoop,
                scope.equals(whileLoop.getCondition().getTree().getId()),
                super::visitWhileLoop,
                w -> w.getCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitSwitch(J.Switch switzh) {
        return maybeTransform(switzh,
                scope.equals(switzh.getSelector().getTree().getId()),
                super::visitSwitch,
                s -> s.getSelector().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitSynchronized(J.Synchronized synch) {
        return maybeTransform(synch,
                scope.equals(synch.getLock().getTree().getId()),
                super::visitSynchronized,
                s -> s.getLock().getTree(),
                UNWRAP_PARENS);
    }
}
