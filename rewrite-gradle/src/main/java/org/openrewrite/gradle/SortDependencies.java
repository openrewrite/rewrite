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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class SortDependencies extends Recipe {

    private static final MethodMatcher DEPENDENCIES_DSL_MATCHER = new MethodMatcher("* dependencies(..)");

    @Override
    public String getDisplayName() {
        return "Sort Gradle dependencies";
    }

    @Override
    public String getDescription() {
        return "Sort dependencies in `build.gradle` and `build.gradle.kts` files. " +
               "Dependencies are sorted alphabetically by configuration name (e.g. `api`, `implementation`), " +
               "then by groupId, then by artifactId.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!DEPENDENCIES_DSL_MATCHER.matches(m, true)) {
                    return m;
                }
                if (m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return m;
                }

                J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                if (!(lambda.getBody() instanceof J.Block)) {
                    return m;
                }

                J.Block body = (J.Block) lambda.getBody();
                List<Statement> statements = body.getStatements();
                if (statements.size() < 2) {
                    return m;
                }

                List<Statement> sorted = new ArrayList<>(statements);
                sorted.sort(dependencyComparator);

                // Check if order actually changed
                boolean changed = false;
                for (int i = 0; i < statements.size(); i++) {
                    if (statements.get(i) != sorted.get(i)) {
                        changed = true;
                        break;
                    }
                }

                if (!changed) {
                    return m;
                }

                // Keep each statement's prefix with the statement itself so that any preceding comments
                // (which the parser stores on the next statement's prefix) move along with the dependency.
                // Swap only the leading whitespace of the new first statement with the original first
                // statement's leading whitespace, to keep the block opening formatting consistent.
                Space originalFirstPrefix = statements.get(0).getPrefix();
                Space newFirstPrefix = sorted.get(0).getPrefix();
                sorted.set(0, sorted.get(0).withPrefix(newFirstPrefix.withWhitespace(originalFirstPrefix.getWhitespace())));
                // Apply the displaced leading whitespace to whichever sorted statement now follows the
                // original first statement, to preserve the block-internal blank-line layout.
                int originalFirstIndex = sorted.indexOf(statements.get(0));
                if (originalFirstIndex > 0) {
                    Space displacedPrefix = sorted.get(originalFirstIndex).getPrefix();
                    sorted.set(originalFirstIndex, sorted.get(originalFirstIndex)
                      .withPrefix(displacedPrefix.withWhitespace(newFirstPrefix.getWhitespace())));
                }

                body = body.withStatements(sorted);
                lambda = lambda.withBody(body);
                return m.withArguments(java.util.Collections.singletonList(lambda));
            }
        });
    }

    private static final Comparator<Statement> dependencyComparator = (s1, s2) -> {
        J.MethodInvocation d1 = extractMethodInvocation(s1);
        J.MethodInvocation d2 = extractMethodInvocation(s2);

        if (d1 == null && d2 == null) {
            return 0;
        }
        if (d1 == null) {
            return 1;
        }
        if (d2 == null) {
            return -1;
        }

        String config1 = d1.getSimpleName();
        String config2 = d2.getSimpleName();
        if (!config1.equals(config2)) {
            return config1.compareTo(config2);
        }

        String groupId1 = getEntry("group", d1).orElse("");
        String groupId2 = getEntry("group", d2).orElse("");
        if (!groupId1.equals(groupId2)) {
            return groupId1.compareTo(groupId2);
        }

        String artifactId1 = getEntry("name", d1).orElse("");
        String artifactId2 = getEntry("name", d2).orElse("");
        return artifactId1.compareTo(artifactId2);
    };

    private static J. @Nullable MethodInvocation extractMethodInvocation(Statement s) {
        if (s instanceof J.MethodInvocation) {
            return (J.MethodInvocation) s;
        }
        if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.MethodInvocation) {
            return (J.MethodInvocation) ((J.Return) s).getExpression();
        }
        return null;
    }

    private static Optional<String> getEntry(String entry, J.MethodInvocation invocation) {
        if (invocation.getArguments().isEmpty()) {
            return Optional.empty();
        }

        Expression firstArg = invocation.getArguments().get(0);

        // String notation: implementation("group:artifact:version")
        if (firstArg instanceof J.Literal) {
            Object value = ((J.Literal) firstArg).getValue();
            if (value == null) {
                return Optional.empty();
            }
            Dependency dependency = DependencyNotation.parse((String) value);
            if (dependency == null) {
                return Optional.empty();
            }
            switch (entry) {
                case "group":
                    return Optional.ofNullable(dependency.getGroupId());
                case "name":
                    return Optional.of(dependency.getArtifactId());
                case "version":
                    return Optional.ofNullable(dependency.getVersion());
            }
        }

        // Groovy map notation: implementation group: "x", name: "y", version: "z"
        if (firstArg instanceof G.MapEntry) {
            for (Expression e : invocation.getArguments()) {
                if (!(e instanceof G.MapEntry)) {
                    continue;
                }
                G.MapEntry mapEntry = (G.MapEntry) e;
                if (mapEntry.getKey() instanceof J.Literal && mapEntry.getValue() instanceof J.Literal) {
                    if (entry.equals(((J.Literal) mapEntry.getKey()).getValue())) {
                        return Optional.ofNullable((String) ((J.Literal) mapEntry.getValue()).getValue());
                    }
                }
            }
        }

        // Kotlin named arguments: implementation(group = "x", name = "y", version = "z")
        if (firstArg instanceof J.Assignment) {
            for (Expression e : invocation.getArguments()) {
                if (!(e instanceof J.Assignment)) {
                    continue;
                }
                J.Assignment assignment = (J.Assignment) e;
                if (assignment.getVariable() instanceof J.Identifier) {
                    String key = ((J.Identifier) assignment.getVariable()).getSimpleName();
                    if (entry.equals(key) && assignment.getAssignment() instanceof J.Literal) {
                        return Optional.ofNullable((String) ((J.Literal) assignment.getAssignment()).getValue());
                    }
                }
            }
        }

        // platform("group:artifact:version") or enforcedPlatform(...)
        if (firstArg instanceof J.MethodInvocation) {
            J.MethodInvocation inner = (J.MethodInvocation) firstArg;
            String name = inner.getSimpleName();
            if ("platform".equals(name) || "enforcedPlatform".equals(name)) {
                return getEntry(entry, inner);
            }
        }

        return Optional.empty();
    }
}
