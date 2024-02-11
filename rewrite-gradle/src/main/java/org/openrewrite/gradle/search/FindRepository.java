/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyPrinter;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindRepository extends Recipe {
    @Option(displayName = "Type",
            description = "The type of the artifact repository",
            example = "maven",
            required = false)
    @Nullable
    String type;

    @Option(displayName = "URL",
            description = "The url of the artifact repository",
            example = "https://repo.spring.io",
            required = false)
    @Nullable
    String url;

    @Option(displayName = "Purpose",
            description = "The purpose of this repository in terms of resolving project or plugin dependencies",
            valid = {"Project", "Plugin"},
            required = false)
    @Nullable
    Purpose purpose;

    @Override
    public String getDisplayName() {
        return "Find Gradle repository";
    }

    @Override
    public String getDescription() {
        return "Find a Gradle repository by url.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher pluginManagementMatcher = new MethodMatcher("RewriteSettings pluginManagement(..)");
        MethodMatcher buildscriptMatcher = new MethodMatcher("RewriteGradleProject buildscript(..)");
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (purpose == null) {
                    return new RepositoryVisitor().visitMethodInvocation(method, ctx);
                } else {
                    boolean isPluginBlock = pluginManagementMatcher.matches(method) || buildscriptMatcher.matches(method);
                    if ((purpose == Purpose.Project && !isPluginBlock)
                        || (purpose == Purpose.Plugin && isPluginBlock)) {
                        return new RepositoryVisitor().visitMethodInvocation(method, ctx);
                    }
                }

                return method;
            }
        });
    }

    private class RepositoryVisitor extends GroovyIsoVisitor<ExecutionContext> {
        private final MethodMatcher repositoryMatcher = new MethodMatcher("org.gradle.api.artifacts.dsl.RepositoryHandler *(..)", true);

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (!repositoryMatcher.matches(m)) {
                return m;
            }

            boolean match = type == null || m.getSimpleName().equals(type);

            if (url != null && !urlMatches(m, url)) {
                match = false;
            }

            if (!match) {
                return m;
            }

            return SearchResult.found(m);
        }

        private boolean urlMatches(J.MethodInvocation m, String url) {
            if (!(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }

            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }

            J.Block block = (J.Block) lambda.getBody();
            for (Statement statement : block.getStatements()) {
                if (statement instanceof J.Assignment || (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.Assignment)) {
                    J.Assignment assignment = (J.Assignment) (statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement);
                    if (assignment.getVariable() instanceof J.Identifier
                        && "url".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                        if (assignment.getAssignment() instanceof J.Literal
                            && url.equals(((J.Literal) assignment.getAssignment()).getValue())) {
                            return true;
                        } else if (assignment.getAssignment() instanceof J.MethodInvocation
                                   && ((J.MethodInvocation) assignment.getAssignment()).getSimpleName().equals("uri")
                                   && ((J.MethodInvocation) assignment.getAssignment()).getArguments().get(0) instanceof J.Literal
                                   && url.equals(((J.Literal) ((J.MethodInvocation) assignment.getAssignment()).getArguments().get(0)).getValue())) {
                            return true;
                        } else if (assignment.getAssignment() instanceof G.GString) {
                            String valueSource = assignment.getAssignment().withPrefix(Space.EMPTY).printTrimmed(new GroovyPrinter<>());
                            String testSource = ((G.GString) assignment.getAssignment()).getDelimiter() + url + ((G.GString) assignment.getAssignment()).getDelimiter();
                            return testSource.equals(valueSource);
                        }
                    }
                } else if (statement instanceof J.MethodInvocation || (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation)) {
                    J.MethodInvocation m1 = (J.MethodInvocation) (statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement);
                    if (m1.getSimpleName().equals("setUrl") || m1.getSimpleName().equals("url")) {
                        if (m1.getArguments().get(0) instanceof J.Literal
                            && url.equals(((J.Literal) m1.getArguments().get(0)).getValue())) {
                            return true;
                        } else if (m1.getArguments().get(0) instanceof J.MethodInvocation
                                   && ((J.MethodInvocation) m1.getArguments().get(0)).getSimpleName().equals("uri")
                                   && ((J.MethodInvocation) m1.getArguments().get(0)).getArguments().get(0) instanceof J.Literal
                                   && url.equals(((J.Literal) ((J.MethodInvocation) m1.getArguments().get(0)).getArguments().get(0)).getValue())) {
                            return true;
                        } else if (m1.getArguments().get(0) instanceof G.GString) {
                            G.GString value = (G.GString) m1.getArguments().get(0);
                            String valueSource = value.withPrefix(Space.EMPTY).printTrimmed(new GroovyPrinter<>());
                            String testSource = value.getDelimiter() + url + value.getDelimiter();
                            return testSource.equals(valueSource);
                        }
                    }
                }
            }

            return false;
        }
    }

    public enum Purpose {
        Project, Plugin
    }
}
