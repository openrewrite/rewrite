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

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Deletes a configuration property that Gradle deprecated without offering a replacement, in whichever of the
 * equivalent Gradle DSL spellings it is written: {@code prop = value}, the Groovy setter-call {@code prop value},
 * {@code prop.set(value)} and {@code setProp(value)}.
 * <p>
 * A property name alone is too weak a signal to delete on, so a removal additionally requires the surrounding
 * configuration block to identify the plugin or task type that owns the property. {@code contextToken} is tested
 * against the method names, receivers, type references and string arguments of every enclosing method invocation,
 * so it sees {@code pmd} in {@code pmd { }}, {@code Pmd} in {@code tasks.withType(Pmd) { }} and {@code pmdMain}
 * in {@code tasks.named("pmdMain") { }} alike.
 */
@RequiredArgsConstructor
class RemoveDeprecatedPropertyVisitor extends JavaVisitor<ExecutionContext> {

    private static final String REMOVED_STATEMENT = "removedDeprecatedProperty";

    private final String propertyName;
    private final Predicate<String> contextToken;

    @Override
    public @Nullable J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        if (isStatement() && assignsProperty(a.getVariable())) {
            return removeStatement();
        }
        return a;
    }

    @Override
    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
        if (isStatement() && m.getArguments().size() == 1 && setsProperty(m)) {
            return removeStatement();
        }
        return removeIfEmptiedConfigurationBlock(m);
    }

    private boolean setsProperty(J.MethodInvocation m) {
        if (m.getSelect() == null) {
            return (propertyName.equals(m.getSimpleName()) || setterName().equals(m.getSimpleName())) &&
                    inConfigurationContext();
        }
        if ("set".equals(m.getSimpleName())) {
            return assignsProperty(m.getSelect());
        }
        return setterName().equals(m.getSimpleName()) && namesContext(m.getSelect());
    }

    private boolean assignsProperty(Expression variable) {
        if (variable instanceof J.Identifier) {
            return propertyName.equals(((J.Identifier) variable).getSimpleName()) && inConfigurationContext();
        }
        if (variable instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) variable;
            return propertyName.equals(fieldAccess.getSimpleName()) &&
                    (namesContext(fieldAccess.getTarget()) || inConfigurationContext());
        }
        return false;
    }

    private boolean namesContext(@Nullable Expression expression) {
        if (expression instanceof J.Identifier) {
            return contextToken.test(((J.Identifier) expression).getSimpleName());
        }
        if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            return contextToken.test(fieldAccess.getSimpleName()) || namesContext(fieldAccess.getTarget());
        }
        if (expression instanceof J.MethodInvocation) {
            return describesContext((J.MethodInvocation) expression);
        }
        return false;
    }

    private boolean inConfigurationContext() {
        Iterator<Object> enclosing = getCursor().getParentTreeCursor()
                .getPath(J.MethodInvocation.class::isInstance);
        while (enclosing.hasNext()) {
            if (describesContext((J.MethodInvocation) enclosing.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an invocation identifies the plugin or task owning the property, either by its own name, by its
     * receiver, or by an argument naming the type or task as {@code withType(Pmd)} and {@code named("pmdMain")} do.
     */
    private boolean describesContext(J.MethodInvocation m) {
        if (contextToken.test(m.getSimpleName()) || namesContext(m.getSelect())) {
            return true;
        }
        for (Expression argument : m.getArguments()) {
            if (argument instanceof J.Literal) {
                Object value = ((J.Literal) argument).getValue();
                if (value instanceof String && contextToken.test((String) value)) {
                    return true;
                }
            } else if (!(argument instanceof J.Lambda) && namesContext(unwrapClassLiteral(argument))) {
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

    /**
     * Whether this element can be deleted outright, which is true when it is one of the statements of a block or,
     * as top level configuration in a build script is, one of the statements of the compilation unit. Groovy and
     * Kotlin both wrap statement-position expressions in a transparent statement node, and Groovy additionally
     * wraps the last statement of a closure in an implicit {@link J.Return}; both have to be looked through.
     */
    private boolean isStatement() {
        Cursor parent = getCursor().getParentTreeCursor();
        while (parent.getValue() instanceof G.ExpressionStatement ||
                parent.getValue() instanceof K.ExpressionStatement ||
                parent.getValue() instanceof K.StatementExpression ||
                parent.getValue() instanceof J.Return) {
            parent = parent.getParentTreeCursor();
        }
        return parent.getValue() instanceof J.Block || parent.getValue() instanceof JavaSourceFile;
    }

    /**
     * Removing the expression of an implicit return leaves a bare {@code return}, so drop the whole return.
     */
    @Override
    public @Nullable J visitReturn(J.Return retrn, ExecutionContext ctx) {
        J.Return r = (J.Return) super.visitReturn(retrn, ctx);
        if (retrn.getExpression() != null && r.getExpression() == null) {
            return null;
        }
        return r;
    }

    private @Nullable J removeStatement() {
        Cursor parent = getCursor().getParentTreeCursor();
        parent.putMessageOnFirstEnclosing(J.MethodInvocation.class, REMOVED_STATEMENT, true);
        return null;
    }

    /**
     * Deleting the last statement of a configuration block leaves behind an empty {@code pmd { }} that only existed
     * to hold the removed property, so drop the block too. Blocks that were already empty are left alone, so the
     * recipe never reports a change on a build that had nothing to migrate.
     */
    private @Nullable J removeIfEmptiedConfigurationBlock(J.MethodInvocation m) {
        if (!Boolean.TRUE.equals(getCursor().getMessage(REMOVED_STATEMENT)) || !isStatement() ||
                m.getArguments().isEmpty() || !describesContext(m)) {
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
