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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Expression;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import com.netflix.rewrite.tree.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class UnwrapParentheses extends ScopedRefactorVisitor {
    private static final Function<Expression, Expression> UNWRAP_PARENS = p -> p instanceof Tr.Parentheses ?
            ((Tr.Parentheses<?>) p).getTree().withFormatting(p.getFormatting()) :
            p;

    public UnwrapParentheses(UUID scope) {
        super(scope);
    }

    @Override
    public String getRuleName() {
        return "core.UnwrapParentheses";
    }

    @Override
    public List<AstTransform> visitArrayAccess(Tr.ArrayAccess arrayAccess) {
        List<AstTransform> changes = maybeTransform(arrayAccess,
                scope.equals(arrayAccess.getIndexed().getId()),
                super::visitArrayAccess,
                Tr.ArrayAccess::getIndexed,
                UNWRAP_PARENS);

        if (scope.equals(arrayAccess.getDimension().getIndex().getId())) {
            changes.addAll(transform(arrayAccess.getDimension().getIndex(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssert(Tr.Assert azzert) {
        return maybeTransform(azzert,
                scope.equals(azzert.getCondition().getId()),
                super::visitAssert,
                Tr.Assert::getCondition,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitAssign(Tr.Assign assign) {
        List<AstTransform> changes = maybeTransform(assign,
                scope.equals(assign.getAssignment().getId()),
                super::visitAssign,
                Tr.Assign::getAssignment,
                UNWRAP_PARENS);

        if (scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssignOp(Tr.AssignOp assign) {
        List<AstTransform> changes = maybeTransform(assign,
                scope.equals(assign.getAssignment().getId()),
                super::visitAssignOp,
                Tr.AssignOp::getAssignment,
                UNWRAP_PARENS);

        if (scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitBinary(Tr.Binary binary) {
        List<AstTransform> changes = maybeTransform(binary,
                scope.equals(binary.getLeft().getId()),
                super::visitBinary,
                Tr.Binary::getLeft,
                UNWRAP_PARENS);

        if (scope.equals(binary.getRight().getId())) {
            changes.addAll(transform(binary.getRight(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitCase(Tr.Case caze) {
        return maybeTransform(caze,
                caze.getPattern() != null && scope.equals(caze.getPattern().getId()),
                super::visitCase,
                Tr.Case::getPattern,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitFieldAccess(Tr.FieldAccess fieldAccess) {
        return maybeTransform(fieldAccess,
                scope.equals(fieldAccess.getTarget().getId()),
                super::visitFieldAccess,
                Tr.FieldAccess::getTarget,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitForEachLoop(Tr.ForEachLoop forEachLoop) {
        return maybeTransform(forEachLoop,
                scope.equals(forEachLoop.getControl().getIterable().getId()),
                super::visitForEachLoop,
                f -> f.getControl().getIterable(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitForLoop(Tr.ForLoop forLoop) {
        return maybeTransform(forLoop,
                scope.equals(forLoop.getControl().getCondition().getId()),
                super::visitForLoop,
                f -> f.getControl().getCondition(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitIf(Tr.If iff) {
        return maybeTransform(iff,
                scope.equals(iff.getIfCondition().getTree().getId()),
                super::visitIf,
                i -> i.getIfCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitInstanceOf(Tr.InstanceOf instanceOf) {
        return maybeTransform(instanceOf,
                scope.equals(instanceOf.getExpr().getId()),
                super::visitInstanceOf,
                Tr.InstanceOf::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitMemberReference(Tr.MemberReference memberRef) {
        return maybeTransform(memberRef,
                scope.equals(memberRef.getContaining().getId()),
                super::visitMemberReference,
                Tr.MemberReference::getContaining,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation methodInvocation) {
        return maybeTransform(methodInvocation,
                methodInvocation.getSelect() != null && scope.equals(methodInvocation.getSelect().getId()),
                super::visitMethodInvocation,
                Tr.MethodInvocation::getSelect,
                UNWRAP_PARENS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AstTransform> visitNewArray(Tr.NewArray newArray) {
        return maybeTransform(newArray,
                newArray.getDimensions().stream().anyMatch(d -> d.getSize().getId().equals(scope)),
                super::visitNewArray,
                na -> na
                        .withDimensions(na.getDimensions().stream()
                                .map(dim -> dim.getSize().getId().equals(scope) ?
                                        dim.withSize(((Tr.Parentheses<Expression>) dim.getSize()).getTree()) :
                                        dim
                                )
                                .collect(toList())
                        )
        );
    }

    @Override
    public List<AstTransform> visitReturn(Tr.Return retrn) {
        return maybeTransform(retrn,
                retrn.getExpr() != null && scope.equals(retrn.getExpr().getId()),
                super::visitReturn,
                Tr.Return::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitTernary(Tr.Ternary ternary) {
        List<AstTransform> changes = maybeTransform(ternary,
                scope.equals(ternary.getCondition().getId()),
                super::visitTernary,
                Tr.Ternary::getCondition,
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
    public List<AstTransform> visitThrow(Tr.Throw thrown) {
        return maybeTransform(thrown,
                scope.equals(thrown.getException().getId()),
                super::visitThrow,
                Tr.Throw::getException,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitTypeCast(Tr.TypeCast typeCast) {
        return maybeTransform(typeCast,
                scope.equals(typeCast.getExpr().getId()),
                super::visitTypeCast,
                Tr.TypeCast::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitUnary(Tr.Unary unary) {
        return maybeTransform(unary,
                scope.equals(unary.getExpr().getId()),
                super::visitUnary,
                Tr.Unary::getExpr,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitVariable(Tr.VariableDecls.NamedVar variable) {
        return maybeTransform(variable,
                variable.getInitializer() != null && scope.equals(variable.getInitializer().getId()),
                super::visitVariable,
                Tr.VariableDecls.NamedVar::getInitializer,
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitDoWhileLoop(Tr.DoWhileLoop doWhileLoop) {
        return maybeTransform(doWhileLoop,
                scope.equals(doWhileLoop.getWhileCondition().getCondition().getTree().getId()),
                super::visitDoWhileLoop,
                w -> w.getWhileCondition().getCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitWhileLoop(Tr.WhileLoop whileLoop) {
        return maybeTransform(whileLoop,
                scope.equals(whileLoop.getCondition().getTree().getId()),
                super::visitWhileLoop,
                w -> w.getCondition().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitSwitch(Tr.Switch switzh) {
        return maybeTransform(switzh,
                scope.equals(switzh.getSelector().getTree().getId()),
                super::visitSwitch,
                s -> s.getSelector().getTree(),
                UNWRAP_PARENS);
    }

    @Override
    public List<AstTransform> visitSynchronized(Tr.Synchronized synch) {
        return maybeTransform(synch,
                scope.equals(synch.getLock().getTree().getId()),
                super::visitSynchronized,
                s -> s.getLock().getTree(),
                UNWRAP_PARENS);
    }
}
