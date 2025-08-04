/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.gradle.internal.Dependency;
import org.openrewrite.gradle.internal.DependencyStringNotationConverter;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.time.Duration;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyConfiguration extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New configuration",
            description = "A dependency configuration container.",
            example = "api")
    String newConfiguration;

    @Option(displayName = "Dependency configuration",
            description = "The dependency configuration to search for dependencies in.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Change a Gradle dependency configuration";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s` to `%s`", groupId, artifactId, newConfiguration);
    }

    @Override
    public String getDescription() {
        return "A common example is the need to change `compile` to `api`/`implementation` as " +
               "[part of the move](https://docs.gradle.org/current/userguide/upgrading_version_6.html) to Gradle 7.x and later.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(DependencyMatcher.build(groupId + ":" + artifactId));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            // Still need to be able to change the configuration for project dependencies which are not yet supported by the `GradleDependency.Matcher`
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher()
                        .configuration(configuration);

                if (!gradleDependencyMatcher.get(getCursor()).isPresent() && !matchesOtherDependency(m)) {
                    return m;
                }


                DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
                List<Expression> args = m.getArguments();
                if (args.get(0) instanceof J.Literal) {
                    J.Literal arg = (J.Literal) args.get(0);
                    if (!(arg.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency = DependencyStringNotationConverter.parse((String) arg.getValue());
                    if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else if (args.get(0) instanceof G.GString) {
                    G.GString gString = (G.GString) args.get(0);
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return m;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    if (!(groupArtifact.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else if (args.get(0) instanceof G.MapEntry) {
                    if (args.size() < 2) {
                        return m;
                    }

                    String groupId = null;
                    String artifactId = null;
                    for (Expression e : args) {
                        if (!(e instanceof G.MapEntry)) {
                            continue;
                        }
                        G.MapEntry arg = (G.MapEntry) e;
                        if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                            continue;
                        }
                        J.Literal key = (J.Literal) arg.getKey();
                        J.Literal value = (J.Literal) arg.getValue();
                        if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String keyValue = (String) key.getValue();
                        String valueValue = (String) value.getValue();
                        if ("group".equals(keyValue)) {
                            groupId = valueValue;
                        } else if ("name".equals(keyValue)) {
                            artifactId = valueValue;
                        }
                    }

                    if (artifactId == null || !dependencyMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                } else if (args.get(0) instanceof G.MapLiteral) {
                    if (args.size() < 2) {
                        return m;
                    }

                    G.MapLiteral map = (G.MapLiteral) args.get(0);
                    String groupId = null;
                    String artifactId = null;
                    for (G.MapEntry arg : map.getElements()) {
                        if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                            continue;
                        }
                        J.Literal key = (J.Literal) arg.getKey();
                        J.Literal value = (J.Literal) arg.getValue();
                        if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                            continue;
                        }
                        String keyValue = (String) key.getValue();
                        String valueValue = (String) value.getValue();
                        if ("group".equals(keyValue)) {
                            groupId = valueValue;
                        } else if ("name".equals(keyValue)) {
                            artifactId = valueValue;
                        }
                    }

                    if (artifactId == null || !dependencyMatcher.matches(groupId, artifactId)) {
                        return m;
                    }
                } else if (args.get(0) instanceof J.MethodInvocation) {
                    J.MethodInvocation inner = (J.MethodInvocation) args.get(0);
                    if (!("project".equals(inner.getSimpleName()) || "platform".equals(inner.getSimpleName()) || "enforcedPlatform".equals(inner.getSimpleName()))) {
                        return m;
                    }
                    List<Expression> innerArgs = inner.getArguments();
                    if (!(innerArgs.get(0) instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal value = (J.Literal) innerArgs.get(0);
                    if (!(value.getValue() instanceof String)) {
                        return m;
                    }

                    Dependency dependency;
                    if ("project".equals(inner.getSimpleName())) {
                        dependency = new Dependency("", ((String) value.getValue()).substring(1), null, null, null);
                    } else {
                        dependency = DependencyStringNotationConverter.parse((String) value.getValue());
                    }

                    if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return m;
                    }
                } else {
                    return m;
                }

                if (newConfiguration.equals(m.getSimpleName())) {
                    return m;
                }

                return m.withName(m.getName().withSimpleName(newConfiguration));
            }

            private boolean matchesOtherDependency(J.MethodInvocation m) {
                if (!dependencyDsl.matches(m)) {
                    return false;
                }

                if (m.getArguments().isEmpty() || !(m.getArguments().get(0) instanceof J.MethodInvocation)) {
                    return false;
                }

                J.MethodInvocation inner = (J.MethodInvocation) m.getArguments().get(0);
                if (!("project".equals(inner.getSimpleName()) || "platform".equals(inner.getSimpleName()) || "enforcedPlatform".equals(inner.getSimpleName()))) {
                    return false;
                }

                return StringUtils.isBlank(configuration) || configuration.equals(m.getSimpleName());
            }
        });
    }
}
