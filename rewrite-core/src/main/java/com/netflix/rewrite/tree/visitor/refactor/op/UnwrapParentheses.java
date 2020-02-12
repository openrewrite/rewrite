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
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * NOTE: Should be possible with {@link RefactorVisitor#transform(Class, Tree, Function)},
 * but can't get the types right so it doesn't throw {@link ClassCastException}, so we'll unwrap
 * manually for each place a parentheses expression can occur for now.
 */
public class UnwrapParentheses extends ScopedRefactorVisitor {
    private static final Function<Expression, Expression> UNWRAP_PARENS = p -> p instanceof Tr.Parentheses ?
            ((Tr.Parentheses<?>) p).getTree().withFormatting(p.getFormatting()) :
            p;

    public UnwrapParentheses(UUID scope) {
        super(scope);
    }

    @Override
    public List<AstTransform> visitArrayAccess(Tr.ArrayAccess arrayAccess) {
        List<AstTransform> changes = maybeTransform(scope.equals(arrayAccess.getIndexed().getId()),
                super.visitArrayAccess(arrayAccess),
                transform(arrayAccess.getIndexed(), UNWRAP_PARENS));

        if(scope.equals(arrayAccess.getDimension().getIndex().getId())) {
            changes.addAll(transform(arrayAccess.getDimension().getIndex(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssert(Tr.Assert azzert) {
        return maybeTransform(scope.equals(azzert.getCondition().getId()),
                super.visitAssert(azzert),
                transform(azzert.getCondition(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitAssign(Tr.Assign assign) {
        List<AstTransform> changes = maybeTransform(scope.equals(assign.getAssignment().getId()),
                super.visitAssign(assign),
                transform(assign.getAssignment(), UNWRAP_PARENS));

        if(scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitAssignOp(Tr.AssignOp assign) {
        List<AstTransform> changes = maybeTransform(scope.equals(assign.getAssignment().getId()),
                super.visitAssignOp(assign),
                transform(assign.getAssignment(), UNWRAP_PARENS));

        if(scope.equals(assign.getVariable().getId())) {
            changes.addAll(transform(assign.getVariable(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitBinary(Tr.Binary binary) {
        List<AstTransform> changes = maybeTransform(scope.equals(binary.getLeft().getId()),
                super.visitBinary(binary),
                transform(binary.getLeft(), UNWRAP_PARENS));

        if(scope.equals(binary.getRight().getId())) {
            changes.addAll(transform(binary.getRight(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitCase(Tr.Case caze) {
        return maybeTransform(caze.getPattern() != null && scope.equals(caze.getPattern().getId()),
                super.visitCase(caze),
                transform(caze.getPattern(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitFieldAccess(Tr.FieldAccess fieldAccess) {
        return maybeTransform(scope.equals(fieldAccess.getTarget().getId()),
                super.visitFieldAccess(fieldAccess),
                transform(fieldAccess.getTarget(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitForEachLoop(Tr.ForEachLoop forEachLoop) {
        return maybeTransform(scope.equals(forEachLoop.getControl().getIterable().getId()),
                super.visitForEachLoop(forEachLoop),
                transform(forEachLoop.getControl().getIterable(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitForLoop(Tr.ForLoop forLoop) {
        return maybeTransform(scope.equals(forLoop.getControl().getCondition().getId()),
                super.visitForLoop(forLoop),
                transform(forLoop.getControl().getCondition(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitIf(Tr.If iff) {
        return maybeTransform(scope.equals(iff.getIfCondition().getTree().getId()),
                super.visitIf(iff),
                transform(iff.getIfCondition().getTree(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitInstanceOf(Tr.InstanceOf instanceOf) {
        return maybeTransform(scope.equals(instanceOf.getExpr().getId()),
                super.visitInstanceOf(instanceOf),
                transform(instanceOf.getExpr(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitMemberReference(Tr.MemberReference memberRef) {
        return maybeTransform(scope.equals(memberRef.getContaining().getId()),
                super.visitMemberReference(memberRef),
                transform(memberRef.getContaining(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation methodInvocation) {
        return maybeTransform(methodInvocation.getSelect() != null && scope.equals(methodInvocation.getSelect().getId()),
                super.visitMethodInvocation(methodInvocation),
                transform(methodInvocation.getSelect(), UNWRAP_PARENS));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<AstTransform> visitNewArray(Tr.NewArray newArray) {
        return maybeTransform(newArray.getDimensions().stream().anyMatch(d -> d.getSize().getId().equals(scope)),
                super.visitNewArray(newArray),
                transform(newArray, na -> na.withDimensions(na.getDimensions().stream()
                    .map(dim -> dim.getSize().getId().equals(scope) ?
                            dim.withSize(((Tr.Parentheses<Expression>) dim.getSize()).getTree()) :
                            dim
                    )
                    .collect(toList()))));
    }

    @Override
    public List<AstTransform> visitReturn(Tr.Return retrn) {
        return maybeTransform(retrn.getExpr() != null && scope.equals(retrn.getExpr().getId()),
                super.visitReturn(retrn),
                transform(retrn.getExpr(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitTernary(Tr.Ternary ternary) {
        List<AstTransform> changes = maybeTransform(scope.equals(ternary.getCondition().getId()),
                super.visitTernary(ternary),
                transform(ternary.getCondition(), UNWRAP_PARENS));

        if(scope.equals(ternary.getTruePart().getId())) {
            changes.addAll(transform(ternary.getTruePart(), UNWRAP_PARENS));
        }

        if(scope.equals(ternary.getFalsePart().getId())) {
            changes.addAll(transform(ternary.getFalsePart(), UNWRAP_PARENS));
        }

        return changes;
    }

    @Override
    public List<AstTransform> visitThrow(Tr.Throw thrown) {
        return maybeTransform(scope.equals(thrown.getException().getId()),
                super.visitThrow(thrown),
                transform(thrown.getException(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitTypeCast(Tr.TypeCast typeCast) {
        return maybeTransform(scope.equals(typeCast.getExpr().getId()),
                super.visitTypeCast(typeCast),
                transform(typeCast.getExpr(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitUnary(Tr.Unary unary) {
        return maybeTransform(scope.equals(unary.getExpr().getId()),
                super.visitUnary(unary),
                transform(unary.getExpr(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitVariable(Tr.VariableDecls.NamedVar variable) {
        return maybeTransform(variable.getInitializer() != null && scope.equals(variable.getInitializer().getId()),
                super.visitVariable(variable),
                transform(variable.getInitializer(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitDoWhileLoop(Tr.DoWhileLoop doWhileLoop) {
        return maybeTransform(scope.equals(doWhileLoop.getCondition().getTree().getId()),
                super.visitDoWhileLoop(doWhileLoop),
                transform(doWhileLoop.getCondition().getTree(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitWhileLoop(Tr.WhileLoop whileLoop) {
        return maybeTransform(scope.equals(whileLoop.getCondition().getTree().getId()),
                super.visitWhileLoop(whileLoop),
                transform(whileLoop.getCondition().getTree(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitSwitch(Tr.Switch switzh) {
        return maybeTransform(scope.equals(switzh.getSelector().getTree().getId()),
                super.visitSwitch(switzh),
                transform(switzh.getSelector().getTree(), UNWRAP_PARENS));
    }

    @Override
    public List<AstTransform> visitSynchronized(Tr.Synchronized synch) {
        return maybeTransform(scope.equals(synch.getLock().getTree().getId()),
                super.visitSynchronized(synch),
                transform(synch.getLock().getTree(), UNWRAP_PARENS));
    }
}
