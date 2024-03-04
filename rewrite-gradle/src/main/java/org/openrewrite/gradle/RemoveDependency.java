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

import io.micrometer.core.instrument.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "The dependency configuration",
            description = "The dependency configuration to remove from.",
            example = "api",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Remove a Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Removes a single dependency from the dependencies section of the `build.gradle`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            final MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");
            final DependencyMatcher dependencyMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                G.CompilationUnit g = (G.CompilationUnit) super.visitCompilationUnit(cu, ctx);
                if (g == cu) {
                    return cu;
                }

                Optional<GradleProject> maybeGp = g.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return cu;
                }

                GradleProject gp = maybeGp.get();
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    GradleDependencyConfiguration newGdc = gdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (dependencyMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            return null;
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(newGdc.getDirectResolved(), resolved -> {
                        if (dependencyMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            return null;
                        }
                        return resolved;
                    }));
                    nameToConfiguration.put(newGdc.getName(), newGdc);
                    anyChanged |= newGdc != gdc;
                }

                if (!anyChanged) {
                    // instance was changed, but no marker update is needed
                    return g;
                }

                return g.withMarkers(g.getMarkers().setByType(gp.withNameToConfiguration(nameToConfiguration)));
            }

            @Override
            public @Nullable J visitReturn(J.Return return_, ExecutionContext ctx) {
                boolean dependencyInvocation = return_.getExpression() instanceof J.MethodInvocation && dependencyDsl.matches((J.MethodInvocation) return_.getExpression());
                J.Return r = (J.Return) super.visitReturn(return_, ctx);
                if (dependencyInvocation && r.getExpression() == null) {
                    return null;
                }
                return r;
            }

            @Override
            public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (dependencyDsl.matches(m) && (StringUtils.isEmpty(configuration) || configuration.equals(m.getSimpleName()))) {
                    Expression firstArgument = m.getArguments().get(0);
                    if (firstArgument instanceof J.Literal || firstArgument instanceof G.GString || firstArgument instanceof G.MapEntry) {
                        return maybeRemoveDependency(m);
                    } else if (firstArgument instanceof J.MethodInvocation &&
                            (((J.MethodInvocation) firstArgument).getSimpleName().equals("platform")
                                    || ((J.MethodInvocation) firstArgument).getSimpleName().equals("enforcedPlatform"))) {
                        J after = maybeRemoveDependency((J.MethodInvocation) firstArgument);
                        if (after == null) {
                            return null;
                        }
                    }
                }

                return m;
            }

            private @Nullable J maybeRemoveDependency(J.MethodInvocation m) {
                if (m.getArguments().get(0) instanceof G.GString) {
                    G.GString gString = (G.GString) m.getArguments().get(0);
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return m;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    if (!(groupArtifact.getValue() instanceof String)) {
                        return m;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                        return null;
                    }
                } else if (m.getArguments().get(0) instanceof J.Literal) {
                    Dependency dependency = DependencyStringNotationConverter.parse((String) ((J.Literal) m.getArguments().get(0)).getValue());
                    if (dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
                        return null;
                    }
                } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                    String groupId = null;
                    String artifactId = null;

                    for (Expression e : m.getArguments()) {
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

                    if (groupId != null && artifactId != null && dependencyMatcher.matches(groupId, artifactId)) {
                        return null;
                    }
                }

                return m;
            }
        });
    }
}
