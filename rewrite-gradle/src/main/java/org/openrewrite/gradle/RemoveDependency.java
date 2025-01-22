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
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.DependencyMatcher;

import java.util.HashMap;
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
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            final GradleDependency.Matcher gradleDependencyMatcher = new GradleDependency.Matcher()
                    .configuration(configuration)
                    .groupId(groupId)
                    .artifactId(artifactId);
            final DependencyMatcher depMatcher = requireNonNull(DependencyMatcher.build(groupId + ":" + artifactId).getValue());

            GradleProject gradleProject;

            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return cu;
                }

                gradleProject = maybeGp.get();

                G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                if (g != cu) {
                    g = g.withMarkers(g.getMarkers().setByType(updateGradleModel(gradleProject)));
                }
                return g;
            }

            @Override
            public J.@Nullable Return visitReturn(J.Return return_, ExecutionContext ctx) {
                boolean dependencyInvocation = return_.getExpression() instanceof J.MethodInvocation && gradleDependencyMatcher.get(return_.getExpression(), getCursor()).isPresent();
                J.Return r = super.visitReturn(return_, ctx);
                if (dependencyInvocation && r.getExpression() == null) {
                    //noinspection DataFlowIssue
                    return null;
                }
                return r;
            }

            @Override
            public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (gradleDependencyMatcher.get(getCursor()).isPresent()) {
                    return null;
                }

                return m;
            }

            private GradleProject updateGradleModel(GradleProject gp) {
                Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
                Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
                boolean anyChanged = false;
                for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                    if (!StringUtils.isBlank(configuration) && configuration.equals(gdc.getName())) {
                        newNameToConfiguration.put(gdc.getName(), gdc);
                        continue;
                    }

                    GradleDependencyConfiguration newGdc = gdc;
                    newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                        if (depMatcher.matches(requested.getGroupId(), requested.getArtifactId())) {
                            return null;
                        }
                        return requested;
                    }));
                    newGdc = newGdc.withDirectResolved(ListUtils.map(gdc.getDirectResolved(), resolved -> {
                        if (depMatcher.matches(resolved.getGroupId(), resolved.getArtifactId())) {
                            return null;
                        }
                        return resolved;
                    }));
                    anyChanged |= newGdc != gdc;
                    newNameToConfiguration.put(newGdc.getName(), newGdc);
                }
                if (anyChanged) {
                    gp = gp.withNameToConfiguration(newNameToConfiguration);
                }
                return gp;
            }
        });
    }
}
