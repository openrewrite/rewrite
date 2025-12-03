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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveConfigurationFromGradleTask extends Recipe {

    @Option(displayName = "Task name",
            description = "The name of the Gradle task to modify.",
            example = "bootJar")
    String taskName;

    @Option(displayName = "Configuration",
            description = "The configuration statement to remove from the task. Supports glob expressions.",
            example = "loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC")
    String configuration;

    @Override
    public String getDisplayName() {
        return "Remove a configuration from a Gradle task";
    }

    @Override
    public String getDescription() {
        return "Removes a specific configuration statement from a Gradle task if it exists. " +
                "Works with both Groovy and Kotlin DSL build files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                // Only process Groovy and Kotlin Gradle build files
                return sourceFile instanceof G.CompilationUnit || sourceFile instanceof K.CompilationUnit;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                TaskMatch match = matchTaskConfiguration(mi);
                if (match != null) {
                    J.Lambda lambda = match.lambda;
                    J.Block body = (J.Block) lambda.getBody();

                    List<Statement> newStatements = new ArrayList<>();
                    boolean modified = false;

                    for (Statement statement : body.getStatements()) {
                        if (!matchesConfigurationToRemove(statement)) {
                            newStatements.add(statement);
                        } else {
                            modified = true;
                        }
                    }

                    if (modified) {
                        J.Block newBody = body.withStatements(newStatements);
                        J.Lambda newLambda = lambda.withBody(newBody);

                        List<Expression> newArgs = new ArrayList<>();
                        for (int i = 0; i < mi.getArguments().size(); i++) {
                            if (i == match.lambdaIndex) {
                                newArgs.add(newLambda);
                            } else {
                                newArgs.add(mi.getArguments().get(i));
                            }
                        }
                        mi = mi.withArguments(newArgs);
                    }
                }

                return mi;
            }

            private TaskMatch matchTaskConfiguration(J.MethodInvocation mi) {
                String methodName = mi.getSimpleName();

                if (taskName.equals(methodName)) {
                    if (!mi.getArguments().isEmpty() && mi.getArguments().get(0) instanceof J.Lambda) {
                        return new TaskMatch((J.Lambda) mi.getArguments().get(0), 0);
                    }
                }

                if ("named".equals(methodName) && mi.getSelect() != null) {
                    Expression select = mi.getSelect();
                    if (select instanceof J.Identifier && "tasks".equals(((J.Identifier) select).getSimpleName())) {
                        // Check if the first argument is our task name
                        if (!mi.getArguments().isEmpty()) {
                            Expression firstArg = mi.getArguments().get(0);
                            if (firstArg instanceof J.Literal) {
                                J.Literal literal = (J.Literal) firstArg;
                                if (taskName.equals(literal.getValue())) {
                                    // Look for lambda in subsequent arguments
                                    for (int i = 1; i < mi.getArguments().size(); i++) {
                                        if (mi.getArguments().get(i) instanceof J.Lambda) {
                                            return new TaskMatch((J.Lambda) mi.getArguments().get(i), i);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                return null;
            }

            private boolean matchesConfigurationToRemove(Statement statement) {
                // Normalize both the statement and the configuration to remove for comparison
                String statementStr = normalizeStatement(statement);
                String configStr = normalizeStatement(configuration);
                return StringUtils.matchesGlob(statementStr, configStr);
            }

            private String normalizeStatement(Object obj) {
                String str;
                if (obj instanceof Statement) {
                    str = ((Statement) obj).print(getCursor());
                } else {
                    str = obj.toString();
                }

                // Remove all whitespace and quotes for comparison
                return str.replaceAll("\\s+", "").replaceAll("['\"]", "");
            }
        });
    }

    private static class TaskMatch {
        J.Lambda lambda;
        int lambdaIndex;

        TaskMatch(J.Lambda lambda, int lambdaIndex) {
            this.lambda = lambda;
            this.lambdaIndex = lambdaIndex;
        }
    }
}
