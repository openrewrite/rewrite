/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.RemoveExtension;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class RemoveDevelocityConfiguration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove Develocity configuration";
    }

    @Override
    public String getDescription() {
        return "Remove the Develocity Gradle plugin and associated configuration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if ("buildCache".equals(m.getSimpleName()) && !m.getArguments().isEmpty()) {
                    Expression arg = m.getArguments().get(0);

                    if (arg instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) arg;
                        J.Block body = (J.Block) lambda.getBody();

                        List<Statement> originalStatements = body.getStatements();
                        List<Statement> filteredStatements = originalStatements.stream()
                                .filter(stmt -> !isRemoteCacheStatement(stmt))
                                .collect(toList());

                        if (filteredStatements.size() == originalStatements.size()) {
                            return m;
                        }

                        if (filteredStatements.isEmpty()) {
                            return null;
                        }

                        J.Block newBody = body.withStatements(filteredStatements);
                        J.Lambda newLambda = lambda.withBody(newBody);
                        return m.withArguments(singletonList(newLambda));
                    }
                }

                return m;
            }

            private boolean isRemoteCacheStatement(Statement stmt) {
                // Direct method invocation
                if (stmt instanceof J.MethodInvocation) {
                    return isRemoteCacheInvocation((J.MethodInvocation) stmt);
                }

                // J.Return wrapping (last statement in closure)
                if (stmt instanceof J.Return) {
                    J.Return returnStmt = (J.Return) stmt;
                    if (returnStmt.getExpression() instanceof J.MethodInvocation) {
                        return isRemoteCacheInvocation((J.MethodInvocation) returnStmt.getExpression());
                    }
                }

                return false;
            }

            private boolean isRemoteCacheInvocation(J.MethodInvocation method) {
                return "remote".equals(method.getSimpleName());
            }
        };
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new RemoveExtension("develocity"),
                new RemoveExtension("gradleEnterprise")
        );
    }
}