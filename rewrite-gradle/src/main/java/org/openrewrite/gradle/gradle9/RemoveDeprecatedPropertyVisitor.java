/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.gradle9;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;

/**
 * Deletes a configuration property Gradle deprecated without offering a replacement, written as any of
 * {@code prop = value}, {@code prop value}, {@code prop.set(value)} or {@code setProp(value)}, and only where
 * {@code ownerToken} matches the surrounding configuration block that identifies the owning plugin or task.
 */
@RequiredArgsConstructor
class RemoveDeprecatedPropertyVisitor extends JavaVisitor<ExecutionContext> {

    private static final String REMOVED_STATEMENT = "removedDeprecatedProperty";

    private static final Set<String> TASK_CREATING_METHODS =
            new HashSet<>(asList("create", "maybeCreate", "register", "task"));

    private final String propertyName;
    private final Predicate<String> ownerToken;

    @Override
    public @Nullable J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        if (isDeletableStatement() && assignsProperty(a.getVariable())) {
            return deleteAndRecordRemoval();
        }
        return a;
    }

    @Override
    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
        if (isDeletableStatement() && m.getArguments().size() == 1 && !(m.getArguments().get(0) instanceof J.Lambda) &&
                setsProperty(m)) {
            return deleteAndRecordRemoval();
        }
        return deleteIfLeftEmptyByRemoval(m);
    }

    @Override
    public @Nullable J visitReturn(J.Return retrn, ExecutionContext ctx) {
        J.Return r = (J.Return) super.visitReturn(retrn, ctx);
        if (retrn.getExpression() != null && r.getExpression() == null) {
            return null;
        }
        return r;
    }

    private boolean setsProperty(J.MethodInvocation m) {
        if (m.getSelect() == null) {
            return (propertyName.equals(m.getSimpleName()) || setterName().equals(m.getSimpleName())) &&
                    isInsideOwnerBlock();
        }
        if ("set".equals(m.getSimpleName())) {
            return assignsProperty(m.getSelect());
        }
        return setterName().equals(m.getSimpleName()) && namesOwner(m.getSelect());
    }

    private boolean assignsProperty(Expression variable) {
        if (variable instanceof J.Identifier) {
            return propertyName.equals(((J.Identifier) variable).getSimpleName()) && isInsideOwnerBlock();
        }
        if (variable instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) variable;
            return propertyName.equals(fieldAccess.getSimpleName()) &&
                    (namesOwner(fieldAccess.getTarget()) || isInsideOwnerBlock());
        }
        return false;
    }

    private boolean namesOwner(@Nullable Expression expression) {
        if (expression instanceof J.Identifier) {
            return ownerToken.test(((J.Identifier) expression).getSimpleName());
        }
        if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            return ownerToken.test(fieldAccess.getSimpleName()) || namesOwner(fieldAccess.getTarget());
        }
        if (expression instanceof J.MethodInvocation) {
            return identifiesOwner((J.MethodInvocation) expression);
        }
        return false;
    }

    private boolean isInsideOwnerBlock() {
        Iterator<Object> enclosing = getCursor().getParentTreeCursor()
                .getPath(J.MethodInvocation.class::isInstance);
        while (enclosing.hasNext()) {
            if (identifiesOwner((J.MethodInvocation) enclosing.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean identifiesOwner(J.MethodInvocation m) {
        if (ownerToken.test(m.getSimpleName()) || namesOwner(m.getSelect())) {
            return true;
        }
        for (Expression argument : m.getArguments()) {
            if (argument instanceof J.Literal) {
                Object value = ((J.Literal) argument).getValue();
                if (value instanceof String && ownerToken.test((String) value)) {
                    return true;
                }
            } else if (!(argument instanceof J.Lambda) && namesOwner(unwrapClassLiteral(argument))) {
                return true;
            }
        }
        return false;
    }

    private static Expression unwrapClassLiteral(Expression argument) {
        if (argument instanceof J.FieldAccess && "class".equals(((J.FieldAccess) argument).getSimpleName())) {
            return ((J.FieldAccess) argument).getTarget();
        }
        return argument;
    }

    private String setterName() {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    private boolean isDeletableStatement() {
        Cursor parent = getCursor().getParentTreeCursor();
        while (isTransparentStatementWrapper(parent.getValue())) {
            parent = parent.getParentTreeCursor();
        }
        return parent.getValue() instanceof J.Block || parent.getValue() instanceof JavaSourceFile;
    }

    private static boolean isTransparentStatementWrapper(Object tree) {
        return tree instanceof G.ExpressionStatement ||
                tree instanceof K.ExpressionStatement ||
                tree instanceof K.StatementExpression ||
                tree instanceof J.Return;
    }

    private @Nullable J deleteAndRecordRemoval() {
        getCursor().getParentTreeCursor()
                .putMessageOnFirstEnclosing(J.MethodInvocation.class, REMOVED_STATEMENT, true);
        return null;
    }

    private @Nullable J deleteIfLeftEmptyByRemoval(J.MethodInvocation m) {
        if (!Boolean.TRUE.equals(getCursor().getMessage(REMOVED_STATEMENT)) || !isDeletableStatement() ||
                m.getArguments().isEmpty() || TASK_CREATING_METHODS.contains(m.getSimpleName()) ||
                !identifiesOwner(m)) {
            return m;
        }
        Expression last = m.getArguments().get(m.getArguments().size() - 1);
        if (!(last instanceof J.Lambda)) {
            return m;
        }
        J body = ((J.Lambda) last).getBody();
        if (body instanceof J.Block && ((J.Block) body).getStatements().isEmpty()) {
            return null;
        }
        return m;
    }
}
