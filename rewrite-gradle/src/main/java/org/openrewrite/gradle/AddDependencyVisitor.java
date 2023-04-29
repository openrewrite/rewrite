/*
 * Copyright 2022 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Validated;
import org.openrewrite.gradle.internal.InsertDependencyComparator;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class AddDependencyVisitor extends GroovyIsoVisitor<ExecutionContext> {
    private static final MethodMatcher DEPENDENCIES_DSL_MATCHER = new MethodMatcher("RewriteGradleProject dependencies(..)");
    private static final GradleParser GRADLE_PARSER = GradleParser.builder().build();

    private final String groupId;
    private final String artifactId;
    private final String version;

    @Nullable
    private final String versionPattern;

    @Nullable
    private final String configuration;

    @Nullable
    private final String classifier;

    @Nullable
    private final String extension;

    @Nullable
    private final Pattern familyRegex;

    @Override
    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
        G.CompilationUnit groovy = super.visitCompilationUnit(cu, ctx);
        boolean dependenciesBlockMissing = true;
        for (Statement statement : groovy.getStatements()) {
            if (statement instanceof J.MethodInvocation && DEPENDENCIES_DSL_MATCHER.matches((J.MethodInvocation) statement)) {
                dependenciesBlockMissing = false;
            }
        }

        Validated versionValidation = Semver.validate(version, versionPattern);
        if (versionValidation.isValid()) {
            @Nullable VersionComparator versionComparator = versionValidation.getValue();
        }

        if (dependenciesBlockMissing) {
            Statement dependenciesInvocation = GRADLE_PARSER.parse("dependencies {}")
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                    .getStatements().get(0);
            dependenciesInvocation = autoFormat(dependenciesInvocation, ctx, new Cursor(getCursor(), cu));
            groovy = groovy.withStatements(ListUtils.concat(groovy.getStatements(),
                    groovy.getStatements().isEmpty() ?
                            dependenciesInvocation :
                            dependenciesInvocation.withPrefix(Space.format("\n\n"))));
        }

        doAfterVisit(new InsertDependencyInOrder(configuration));

        return groovy;
    }

    @RequiredArgsConstructor
    private class InsertDependencyInOrder extends GroovyIsoVisitor<ExecutionContext> {
        private final String configuration;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!DEPENDENCIES_DSL_MATCHER.matches(m)) {
                return m;
            }

            J.Lambda dependenciesBlock = (J.Lambda) m.getArguments().get(0);
            if (!(dependenciesBlock.getBody() instanceof J.Block)) {
                return m;
            }

            J.Block body = (J.Block) dependenciesBlock.getBody();

            String codeTemplate;
            DependencyStyle style = autodetectDependencyStyle(body.getStatements());
            if (style == DependencyStyle.String) {
                codeTemplate = "dependencies {\n" +
                               configuration + " \"" + groupId + ":" + artifactId + ":" + version + (classifier == null ? "" : ":" + classifier) + (extension == null ? "" : "@" + extension) + "\"" +
                               "\n}";
            } else {
                codeTemplate = "dependencies {\n" +
                               configuration + " group: \"" + groupId + "\", name: \"" + artifactId + "\", version: \"" + version + "\"" + (classifier == null ? "" : ", classifier: \"" + classifier + "\"") + (extension == null ? "" : ", ext: \"" + extension + "\"") +
                               "\n}";
            }
            J.MethodInvocation addDependencyInvocation = requireNonNull((J.MethodInvocation) ((J.Return) (((J.Block) ((J.Lambda) ((J.MethodInvocation) GRADLE_PARSER.parse(codeTemplate)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"))
                    .getStatements().get(0)).getArguments().get(0)).getBody()).getStatements().get(0))).getExpression());
            addDependencyInvocation = autoFormat(addDependencyInvocation, ctx, new Cursor(getCursor(), body));
            InsertDependencyComparator dependencyComparator = new InsertDependencyComparator(body.getStatements(), addDependencyInvocation);

            List<Statement> statements = new ArrayList<>(body.getStatements());
            int i = 0;
            for (; i < body.getStatements().size(); i++) {
                Statement currentStatement = body.getStatements().get(i);
                if (dependencyComparator.compare(currentStatement, addDependencyInvocation) > 0) {
                    if (dependencyComparator.getBeforeDependency() != null) {
                        J.MethodInvocation beforeDependency = (J.MethodInvocation) (dependencyComparator.getBeforeDependency() instanceof J.Return ?
                                requireNonNull(((J.Return) dependencyComparator.getBeforeDependency()).getExpression()) :
                                dependencyComparator.getBeforeDependency());
                        if (i == 0) {
                            if (!addDependencyInvocation.getSimpleName().equals(beforeDependency.getSimpleName())) {
                                statements.set(i, currentStatement.withPrefix(Space.format("\n\n" + currentStatement.getPrefix().getIndent())));
                            }
                        } else {
                            Space originalPrefix = addDependencyInvocation.getPrefix();
                            if (currentStatement instanceof J.VariableDeclarations) {
                                J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) currentStatement;
                                if (variableDeclarations.getTypeExpression() != null) {
                                    addDependencyInvocation = addDependencyInvocation.withPrefix(variableDeclarations.getTypeExpression().getPrefix());
                                }
                            } else {
                                addDependencyInvocation = addDependencyInvocation.withPrefix(currentStatement.getPrefix());
                            }

                            if (addDependencyInvocation.getSimpleName().equals(beforeDependency.getSimpleName())) {
                                if (currentStatement instanceof J.VariableDeclarations) {
                                    J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) currentStatement;
                                    if (variableDeclarations.getTypeExpression() != null && !variableDeclarations.getTypeExpression().getPrefix().equals(originalPrefix)) {
                                        statements.set(i, variableDeclarations.withTypeExpression(variableDeclarations.getTypeExpression().withPrefix(originalPrefix)));
                                    }
                                } else if (!currentStatement.getPrefix().equals(originalPrefix)) {
                                    statements.set(i, currentStatement.withPrefix(originalPrefix));
                                }
                            }
                        }
                    }

                    statements.add(i, addDependencyInvocation);
                    break;
                }
            }
            if (body.getStatements().size() == i) {
                if (!body.getStatements().isEmpty()) {
                    J.Return lastStatement = (J.Return) statements.remove(i - 1);
                    statements.add(requireNonNull(lastStatement.getExpression()).withPrefix(lastStatement.getPrefix()));
                    if (lastStatement.getExpression() instanceof J.MethodInvocation && !((J.MethodInvocation) lastStatement.getExpression()).getSimpleName().equals(addDependencyInvocation.getSimpleName())) {
                        addDependencyInvocation = addDependencyInvocation.withPrefix(Space.format("\n\n" + addDependencyInvocation.getPrefix().getIndent()));
                    }
                }
                statements.add(addDependencyInvocation);
            }
            body = body.withStatements(statements);
            m = m.withArguments(Collections.singletonList(dependenciesBlock.withBody(body)));

            return m;
        }
    }

    enum DependencyStyle {
        Map, String
    }

    private DependencyStyle autodetectDependencyStyle(List<Statement> statements) {
        int string = 0;
        int map = 0;
        for (Statement statement : statements) {
            if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                J.MethodInvocation invocation = (J.MethodInvocation) ((J.Return) statement).getExpression();
                if (invocation.getArguments().get(0) instanceof J.Literal || invocation.getArguments().get(0) instanceof G.GString) {
                    string++;
                } else if (invocation.getArguments().get(0) instanceof G.MapEntry) {
                    map++;
                }
            } else if (statement instanceof J.MethodInvocation) {
                J.MethodInvocation invocation = (J.MethodInvocation) statement;
                if (invocation.getArguments().get(0) instanceof J.Literal || invocation.getArguments().get(0) instanceof G.GString) {
                    string++;
                } else if (invocation.getArguments().get(0) instanceof G.MapEntry) {
                    map++;
                }
            }
        }

        return string >= map ? DependencyStyle.String : DependencyStyle.Map;
    }
}
