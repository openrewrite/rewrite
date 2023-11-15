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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.util.*;

@SuppressWarnings("DuplicatedCode")
@Value
@EqualsAndHashCode(callSuper = true)
public class ExcludeTransitiveDependency extends Recipe {

    private static final MethodMatcher DEPENDENCY_DSL = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Option(displayName = "Group",
            description = "The group of the dependency to exclude. This is the first part of a dependency coordinate.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The artifact of the dependency to exclude. This is the second part of a dependency coordinate.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Configuration",
            description = "Configuration to add the exclusion to or `null` to add to all configurations.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Add transitive dependency exclusion";
    }

    @Override
    public String getDescription() {
        return "Add a gradle dependency exclusion to a `build.gradle` file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        GroovyIsoVisitor<ExecutionContext> visitor = new GroovyIsoVisitor<ExecutionContext>() {
            final Map<String, Set<GroupArtifact>> dependenciesRequiringExclusion = new HashMap<>();

            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                if (!cu.getMarkers().findFirst(GradleProject.class).isPresent()) {
                    return cu;
                }

                GradleProject gp = cu.getMarkers().findFirst(GradleProject.class).get();
                GroupArtifact exclusion = new GroupArtifact(groupId, artifactId);

                for (GradleDependencyConfiguration conf : gp.getConfigurations()) {
                    if (configuration != null && !conf.getName().equals(configuration)) {
                        continue;
                    }
                    for (Dependency requested : conf.getRequested()) {
                        if (requested.getExclusions().contains(exclusion) || requested.getGroupId() == null) {
                            continue;
                        }
                        ResolvedDependency resolved = conf.findResolvedDependency(requested.getGroupId(), requested.getArtifactId());
                        if (resolved != null && resolved.findDependency(groupId, artifactId) != null) {
                            dependenciesRequiringExclusion.computeIfAbsent(conf.getName(), k -> new HashSet<>())
                                    .add(new GroupArtifact(requested.getGroupId(), requested.getArtifactId()));
                        }
                    }
                }

                if (dependenciesRequiringExclusion.isEmpty()) {
                    return cu;
                }

                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (DEPENDENCY_DSL.matches(m) && dependenciesRequiringExclusion.containsKey(m.getSimpleName())) {
                    Expression firstArgument = m.getArguments().get(0);
                    if (firstArgument instanceof J.Literal || firstArgument instanceof G.GString || firstArgument instanceof G.MapEntry) {
                        return maybeAddExclusion(m.getSimpleName(), m);
                    } else if (firstArgument instanceof J.MethodInvocation &&
                               (((J.MethodInvocation) firstArgument).getSimpleName().equals("platform")
                                || ((J.MethodInvocation) firstArgument).getSimpleName().equals("enforcedPlatform"))) {
                        return maybeAddExclusion(m.getSimpleName(), (J.MethodInvocation) firstArgument);
                    }
                }

                return m;
            }

            private J.MethodInvocation maybeAddExclusion(String configuration, J.MethodInvocation dependency) {
                if (dependency.getArguments().get(0) instanceof G.GString) {
                    G.GString gString = (G.GString) dependency.getArguments().get(0);
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return dependency;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    if (!(groupArtifact.getValue() instanceof String)) {
                        return dependency;
                    }
                    org.openrewrite.gradle.util.Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dependenciesRequiringExclusion.get(configuration).contains(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()))) {
                        return addExclusion(dependency);
                    }
                } else if (dependency.getArguments().get(0) instanceof J.Literal) {
                    org.openrewrite.gradle.util.Dependency dep = DependencyStringNotationConverter.parse((String) ((J.Literal) dependency.getArguments().get(0)).getValue());
                    if (dependenciesRequiringExclusion.get(configuration).contains(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()))) {
                        return addExclusion(dependency);
                    }
                } else if (dependency.getArguments().get(0) instanceof G.MapEntry) {
                    String groupId = null;
                    String artifactId = null;

                    for (Expression e : dependency.getArguments()) {
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

                    if (dependenciesRequiringExclusion.get(configuration).contains(new GroupArtifact(groupId, artifactId))) {
                        return addExclusion(dependency);
                    }
                }

                return dependency;
            }

            private J.MethodInvocation addExclusion(J.MethodInvocation dependency) {
                // FIXME add exclusion
                return SearchResult.found(dependency);
            }
        };

        return Preconditions.check(new IsBuildGradle<>(), visitor);
    }
}
