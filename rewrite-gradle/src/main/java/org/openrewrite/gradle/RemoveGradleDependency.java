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
import org.openrewrite.*;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
class RemoveGradleDependency extends Recipe {

    @Option(displayName = "The dependency configuration", description = "The dependency configuration to remove from.", example = "api", required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;



    @Override
    public String getDisplayName() {
        return "Remove a Gradle dependency";
    }

    @Override
    public String getDescription() {
        return "Removes a single dependency from the dependencies section of the `build.gradle`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {

            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());

            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation decl = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (dependencyDsl.matches(decl)) {
                    if ((configuration != null && !configuration.isEmpty()) && decl.getSimpleName().equals(configuration)) {
                        List<Expression> declArguments = decl.getArguments();

                        if (declArguments.get(0) instanceof J.Literal) {
                            J.Literal stringLiteralArgument = (J.Literal) declArguments.get(0);
                            String argumentValue = (String) stringLiteralArgument.getValue();

                            Dependency dependency = DependencyStringNotationConverter.parse(argumentValue);

                            if (depMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                                return null;
                            }

                        } else if (declArguments.get(0) instanceof G.MapEntry) {
                            String groupId = null;
                            String artifactId = null;
                            String version = null;

                            for (Expression e : declArguments) {
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
                                } else if ("version".equals(keyValue)) {
                                    version = valueValue;
                                }
                            }
                            if (groupId == null || artifactId == null
                                    || (version == null && !depMatcher.matches(groupId, artifactId))
                                    || (version != null && !depMatcher.matches(groupId, artifactId, version))) {
                                return decl;
                            } else {
                                return null;
                            }
                        }
                    }
                }

                return decl;
            }
        });
    }
}
