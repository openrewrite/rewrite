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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.RemoveExtension;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;

public class RemoveDevelocityConfiguration extends Recipe {

    protected static final MethodMatcher BUILD_CACHE_MATCHER = new MethodMatcher("*..* buildCache(..)");

    @Getter
    final String displayName = "Remove Develocity configuration";

    @Getter
    final String description = "Remove the Develocity Gradle plugin and associated configuration.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(BUILD_CACHE_MATCHER),
                new GroovyIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                        if (BUILD_CACHE_MATCHER.matches(m) &&
                                m.getArguments().size() == 1 &&
                                m.getArguments().get(0) instanceof J.Lambda) {
                            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                            J.Block body = (J.Block) lambda.getBody();

                            List<Statement> filteredStatements = ListUtils.filter(body.getStatements(), this::retainStatement);
                            if (filteredStatements.isEmpty()) {
                                return null;
                            }
                            return m.withArguments(singletonList(lambda.withBody(body.withStatements(filteredStatements))));
                        }

                        return m;
                    }

                    private boolean retainStatement(Statement stmt) {
                        if (stmt instanceof J.Return && ((J.Return) stmt).getExpression() instanceof J.MethodInvocation) {
                            // Unpack J.Return wrapping last statement in closure
                            stmt = (J.MethodInvocation) ((J.Return) stmt).getExpression();
                        }
                        if (!(stmt instanceof J.MethodInvocation)) {
                            return true;
                        }
                        J.MethodInvocation remote = (J.MethodInvocation) stmt;
                        if (!"remote".equals(remote.getSimpleName())) {
                            return true;
                        }
                        if (remote.getArguments().isEmpty() || !(remote.getArguments().get(0) instanceof J.FieldAccess)) {
                            return true;
                        }
                        J.FieldAccess fieldAccess = (J.FieldAccess) remote.getArguments().get(0);
                        if (!(fieldAccess.getTarget() instanceof J.Identifier)) {
                            return true;
                        }
                        J.Identifier target = (J.Identifier) fieldAccess.getTarget();
                        return !"develocity".equals(target.getSimpleName()) && !"gradleEnterprise".equals(target.getSimpleName());
                    }
                });
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new RemoveExtension("develocity"),
                new RemoveExtension("gradleEnterprise"));
    }
}
