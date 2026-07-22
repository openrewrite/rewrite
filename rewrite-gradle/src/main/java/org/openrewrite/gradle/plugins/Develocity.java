/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.function.UnaryOperator;

final class Develocity {

    private Develocity() {
    }

    /**
     * Builds a visitor that, for each {@code develocity {}} block in a Gradle settings file, applies
     * {@code transform} to the block's body.
     */
    static TreeVisitor<?, ExecutionContext> settingsBlockVisitor(UnaryOperator<J.Block> transform) {
        return Preconditions.check(new IsSettingsGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!"develocity".equals(m.getSimpleName())) {
                    return m;
                }
                return m.withArguments(ListUtils.map(m.getArguments(), arg -> {
                    if (!(arg instanceof J.Lambda) || !(((J.Lambda) arg).getBody() instanceof J.Block)) {
                        return arg;
                    }
                    J.Lambda lambda = (J.Lambda) arg;
                    return lambda.withBody(transform.apply((J.Block) lambda.getBody()));
                }));
            }
        });
    }

    /**
     * Sets a string property assignment (e.g. {@code server = "..."}) within a {@code develocity} block:
     * updates the value when the property is present but differs, and otherwise inserts it, either right
     * after {@code insertAfter} or, when that is {@code null} or absent, as the first statement.
     */
    static J.Block setStringProperty(J.Block block, String name, String value, @Nullable String insertAfter) {
        List<Statement> statements = block.getStatements();

        int index = indexOfAssignment(statements, name);
        if (index >= 0) {
            J.Assignment existing = asAssignment(statements.get(index));
            if (isLiteral(existing.getAssignment(), value)) {
                return block;
            }
            J.Assignment updated = existing.withAssignment(literalLike(existing.getAssignment(), value));
            return block.withStatements(ListUtils.map(statements, (i, s) ->
                    i == index ? withAssignment(s, updated) : s));
        }

        int afterIndex = insertAfter == null ? -1 : indexOfAssignment(statements, insertAfter);
        int templateIndex = afterIndex >= 0 ? afterIndex : indexOfFirstAssignment(statements);
        if (templateIndex < 0) {
            return block;
        }
        int insertIndex = afterIndex >= 0 ? afterIndex + 1 : 0;
        Statement templateStatement = statements.get(templateIndex);
        J.Assignment template = asAssignment(templateStatement);

        J.Assignment created = template
                .withId(Tree.randomId())
                .withPrefix(templateStatement.getPrefix())
                .withVariable(((J.Identifier) template.getVariable()).withId(Tree.randomId()).withSimpleName(name))
                .withAssignment(literalLike(template.getAssignment(), value));

        return block.withStatements(ListUtils.insert(statements, (Statement) created, insertIndex));
    }

    private static int indexOfAssignment(List<Statement> statements, String name) {
        for (int i = 0; i < statements.size(); i++) {
            J.Assignment assignment = asAssignment(statements.get(i));
            if (assignment != null && assignment.getVariable() instanceof J.Identifier &&
                name.equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfFirstAssignment(List<Statement> statements) {
        for (int i = 0; i < statements.size(); i++) {
            if (asAssignment(statements.get(i)) != null) {
                return i;
            }
        }
        return -1;
    }

    private static J.@Nullable Assignment asAssignment(Statement statement) {
        if (statement instanceof J.Assignment) {
            return (J.Assignment) statement;
        }
        if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.Assignment) {
            return (J.Assignment) ((J.Return) statement).getExpression();
        }
        return null;
    }

    private static Statement withAssignment(Statement statement, J.Assignment assignment) {
        if (statement instanceof J.Return) {
            return ((J.Return) statement).withExpression(assignment);
        }
        return assignment;
    }

    private static boolean isLiteral(Expression expression, String value) {
        return expression instanceof J.Literal && value.equals(((J.Literal) expression).getValue());
    }

    private static J.Literal literalLike(Expression model, String value) {
        if (!(model instanceof J.Literal)) {
            throw new IllegalStateException("Expected a string literal to model the assignment");
        }
        String source = ((J.Literal) model).getValueSource();
        String quote = source != null && !source.isEmpty() ? source.substring(0, 1) : "\"";
        return ((J.Literal) model).withId(Tree.randomId()).withValue(value).withValueSource(quote + value + quote);
    }
}
