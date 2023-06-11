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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;


@Value
@EqualsAndHashCode(callSuper = true)
public class DependencyInsight extends Recipe {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. If not specified, all configurations will be searched.",
            example = "compileClasspath",
            required = false)
    @Nullable
    String configuration;

    @Override
    public String getDisplayName() {
        return "Gradle dependency insight";
    }

    @Override
    public String getDescription() {
        return "Find direct and transitive dependencies matching a group, artifact, and optionally a configuration name. " +
               "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                Optional<GradleProject> maybeGradleProject = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (!maybeGradleProject.isPresent()) {
                    return sourceFile;
                }
                GradleProject gp = maybeGradleProject.get();
                String projectName = sourceFile.getMarkers()
                        .findFirst(JavaProject.class)
                        .map(JavaProject::getProjectName)
                        .orElse("");
                String sourceSetName = sourceFile.getMarkers()
                        .findFirst(JavaSourceSet.class)
                        .map(JavaSourceSet::getName)
                        .orElse("main");
                // configuration -> dependency which is or transitively depends on search target -> search target
                Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency = new HashMap<>();
                Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency = new HashMap<>();
                for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                    if (configuration == null || configuration.isEmpty() || c.getName().equals(configuration)) {
                        for (ResolvedDependency resolvedDependency : c.getResolved()) {
                            ResolvedDependency dep = resolvedDependency.findDependency(groupIdPattern, artifactIdPattern);
                            if (dep != null) {
                                GroupArtifactVersion requestedGav = new GroupArtifactVersion(resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(), resolvedDependency.getVersion());
                                GroupArtifactVersion targetGav = new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                                configurationToDirectDependency.compute(c.getName(), (k, v) -> {
                                    if (v == null) {
                                        v = new LinkedHashSet<>();
                                    }
                                    v.add(requestedGav);
                                    return v;
                                });
                                directDependencyToTargetDependency.compute(requestedGav, (k, v) -> {
                                    if (v == null) {
                                        v = new LinkedHashSet<>();
                                    }
                                    v.add(targetGav);
                                    return v;
                                });
                                dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                                        projectName,
                                        sourceSetName,
                                        dep.getGroupId(),
                                        dep.getArtifactId(),
                                        dep.getVersion(),
                                        dep.getDatedSnapshotVersion(),
                                        dep.getRequested().getScope(),
                                        dep.getDepth()
                                ));
                            }
                        }
                    }
                }
                if (directDependencyToTargetDependency.isEmpty()) {
                    return sourceFile;
                }
                // Non-resolvable configurations may contain the requested which has been found to transitively depend on the target
                for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                    if (configurationToDirectDependency.containsKey(c.getName())) {
                        continue;
                    }
                    for (Dependency dependency : c.getRequested()) {
                        if (directDependencyToTargetDependency.containsKey(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))) {
                            configurationToDirectDependency.compute(c.getName(), (k, v) -> {
                                if (v == null) {
                                    v = new LinkedHashSet<>();
                                }
                                v.add(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
                                return v;
                            });
                        }
                    }
                }
                return new MarkIndividualDependency(configurationToDirectDependency, directDependencyToTargetDependency)
                        .visitNonNull(sourceFile, ctx);
            }
        };
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    private static class MarkIndividualDependency extends JavaIsoVisitor<ExecutionContext> {

        Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency;
        Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!DEPENDENCY_CONFIGURATION_MATCHER.matches(m) || !configurationToDirectDependency.containsKey(m.getSimpleName()) ||
                m.getArguments().isEmpty()) {
                return m;
            }

            Expression arg = m.getArguments().get(0);
            if (arg instanceof J.Literal && ((J.Literal) arg).getValue() instanceof String) {
                String[] gav = ((String) ((J.Literal) arg).getValue()).split(":");
                if (gav.length < 2) {
                    return m;
                }
                String groupId = gav[0];
                String artifactId = gav[1];
                //noinspection DuplicatedCode
                Optional<GroupArtifactVersion> maybeMatch = configurationToDirectDependency.get(m.getSimpleName()).stream()
                        .filter(dep -> Objects.equals(dep.getGroupId(), groupId) && Objects.equals(dep.getArtifactId(), artifactId))
                        .findAny();
                if (!maybeMatch.isPresent()) {
                    return m;
                }
                GroupArtifactVersion direct = maybeMatch.get();
                if (groupId.equals(direct.getGroupId()) && artifactId.equals(direct.getArtifactId())) {
                    String resultText = directDependencyToTargetDependency.get(direct).stream()
                            .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                            .collect(Collectors.joining(","));
                    return SearchResult.found(m, resultText);
                }
            } else if (arg instanceof G.MapEntry) {
                String groupId = null;
                String artifactId = null;
                for (Expression argExp : m.getArguments()) {
                    if (!(argExp instanceof G.MapEntry)) {
                        continue;
                    }
                    G.MapEntry gavPart = (G.MapEntry) argExp;
                    if (!(gavPart.getKey() instanceof J.Literal)) {
                        continue;
                    }
                    String key = (String) ((J.Literal) gavPart.getKey()).getValue();
                    if ("group".equals(key)) {
                        groupId = (String) ((J.Literal) gavPart.getValue()).getValue();
                    } else if ("name".equals(key)) {
                        artifactId = (String) ((J.Literal) gavPart.getValue()).getValue();
                    }
                    if (groupId != null && artifactId != null) {
                        break;
                    }
                }

                String finalGroupId = groupId;
                String finalArtifactId = artifactId;
                //noinspection DuplicatedCode
                Optional<GroupArtifactVersion> maybeMatch = configurationToDirectDependency.get(m.getSimpleName()).stream()
                        .filter(dep -> Objects.equals(dep.getGroupId(), finalGroupId) && Objects.equals(dep.getArtifactId(), finalArtifactId))
                        .findAny();
                if (!maybeMatch.isPresent()) {
                    return m;
                }
                GroupArtifactVersion direct = maybeMatch.get();
                if (groupId.equals(direct.getGroupId()) && artifactId.equals(direct.getArtifactId())) {
                    String resultText = directDependencyToTargetDependency.get(direct).stream()
                            .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                            .collect(Collectors.joining(","));
                    return SearchResult.found(m, resultText);
                }
            }
            return m;
        }
    }
}
