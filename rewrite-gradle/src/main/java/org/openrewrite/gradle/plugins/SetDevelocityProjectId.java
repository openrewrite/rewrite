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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class SetDevelocityProjectId extends Recipe {

    @Override
    public String getDisplayName() {
        return "Set the Develocity `projectId`";
    }

    @Override
    public String getDescription() {
        return "Sets the `projectId` in the `develocity` block of a Gradle settings file, adding it after the " +
               "`server` assignment when absent or updating it when it differs. The `projectId` is used by newer " +
               "Develocity servers to associate build scans with a project.";
    }

    @Option(displayName = "Project ID",
            description = "The value to set for `projectId`.",
            example = "openrewrite")
    String projectId;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
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
                    J.Block block = (J.Block) lambda.getBody();
                    return lambda.withBody(setProjectId(block));
                }));
            }

            private J.Block setProjectId(J.Block block) {
                List<Statement> statements = block.getStatements();

                int projectIdIndex = indexOfAssignment(statements, "projectId");
                if (projectIdIndex >= 0) {
                    J.Assignment existing = asAssignment(statements.get(projectIdIndex));
                    if (isLiteral(existing.getAssignment(), projectId)) {
                        return block;
                    }
                    J.Assignment updated = existing.withAssignment(literalLike(existing.getAssignment()));
                    return block.withStatements(ListUtils.map(statements, (i, s) ->
                            i == projectIdIndex ? withAssignment(s, updated) : s));
                }

                int serverIndex = indexOfAssignment(statements, "server");
                int templateIndex = serverIndex >= 0 ? serverIndex : indexOfFirstAssignment(statements);
                if (templateIndex < 0) {
                    return block;
                }
                Statement templateStatement = statements.get(templateIndex);
                J.Assignment template = asAssignment(templateStatement);

                J.Assignment projectIdAssignment = template
                        .withId(Tree.randomId())
                        .withPrefix(templateStatement.getPrefix())
                        .withVariable(((J.Identifier) template.getVariable()).withId(Tree.randomId()).withSimpleName("projectId"))
                        .withAssignment(literalLike(template.getAssignment()));

                return block.withStatements(ListUtils.insert(statements, (Statement) projectIdAssignment, templateIndex + 1));
            }
        });
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

    private J.Literal literalLike(Expression model) {
        String quote = "\"";
        if (model instanceof J.Literal) {
            String source = ((J.Literal) model).getValueSource();
            if (source != null && !source.isEmpty()) {
                quote = source.substring(0, 1);
            }
            return ((J.Literal) model).withId(Tree.randomId()).withValue(projectId).withValueSource(quote + projectId + quote);
        }
        throw new IllegalStateException("Expected a string literal to model the projectId assignment");
    }
}
