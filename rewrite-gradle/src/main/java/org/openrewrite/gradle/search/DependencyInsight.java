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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.graph.DependencyGraph;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(callSuper = false)
public class DependencyInsight extends Recipe {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");
    private static final MethodMatcher DEPENDENCY_CLOSURE_MATCHER = new MethodMatcher("RewriteGradleProject dependencies(..)");
    private static final Function<Object, Set<GroupArtifactVersion>> EMPTY = gav -> new HashSet<>();

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified resolved version. " +
                    "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                    "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

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
        return "Find direct and transitive dependencies matching a group, artifact, resolved version, and optionally a configuration name. " +
                "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate()
                .and(Validated.test(
                        "coordinates",
                        "groupIdPattern AND artifactIdPattern must not both be generic wildcards",
                        this,
                        r -> !("*".equals(r.groupIdPattern) && "*".equals(artifactIdPattern))
                ));
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile.getMarkers().findFirst(GradleProject.class).isPresent();
            }

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

                findMatchingDependencies(projectName, sourceSetName, gp, configurationToDirectDependency, directDependencyToTargetDependency, ctx);

                // Only proceed with marking if we actually processed some dependencies with paths
                if (directDependencyToTargetDependency.isEmpty()) {
                    return sourceFile;
                }

                // Non-resolvable configurations may contain the requested which has been found to transitively depend on the target
                for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                    if (configurationToDirectDependency.containsKey(c.getName())) {
                        continue;
                    }
                    for (Dependency dependency : c.getRequested()) {
                        GroupArtifactVersion gav = dependency.getGav();
                        Optional<GroupArtifactVersion> matchingGroupArtifact = directDependencyToTargetDependency.keySet().stream().filter(key -> key.equals(gav)).findFirst();
                        if (!matchingGroupArtifact.isPresent()) {
                            matchingGroupArtifact = directDependencyToTargetDependency.keySet().stream().filter(key -> key.asGroupArtifact().equals(gav.asGroupArtifact())).findFirst();
                        }

                        matchingGroupArtifact.ifPresent(version ->
                                configurationToDirectDependency.computeIfAbsent(c.getName(), EMPTY).add(version));
                    }
                }
                return new MarkIndividualDependency(configurationToDirectDependency, directDependencyToTargetDependency).attachMarkers(sourceFile, ctx);
            }

            private boolean matchesConfiguration(GradleDependencyConfiguration c) {
                return configuration == null || configuration.isEmpty() || c.getName().equals(configuration);
            }

            private void findMatchingDependencies(
                    String projectName,
                    String sourceSetName,
                    GradleProject gp,
                    Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                    Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                    ExecutionContext ctx
            ) {
                @Nullable VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;
                DependencyMatcher dependencyMatcher = new DependencyMatcher(groupIdPattern, artifactIdPattern, versionComparator);

                for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                    if (!matchesConfiguration(c)) {
                        continue;
                    }
                    for (ResolvedDependency resolvedDependency : c.getDirectResolved()) {
                        if (!resolvedDependency.isDirect()) {
                            continue;
                        }
                        findMatchingDependencies0(projectName, sourceSetName, c.getName(), dependencyMatcher, resolvedDependency, configurationToDirectDependency, directDependencyToTargetDependency, new ArrayDeque<>(), ctx);
                    }
                }
            }

            private void findMatchingDependencies0(
                    String projectName,
                    String sourceSetName,
                    String configurationName,
                    DependencyMatcher dependencyMatcher,
                    ResolvedDependency resolvedDependency,
                    Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                    Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                    Deque<ResolvedDependency> deque,
                    ExecutionContext ctx
            ) {
                if (deque.contains(resolvedDependency)) {
                    return;
                }

                deque.addFirst(resolvedDependency);

                for (ResolvedDependency next : resolvedDependency.getDependencies()) {
                    findMatchingDependencies0(projectName, sourceSetName, configurationName, dependencyMatcher, next, configurationToDirectDependency, directDependencyToTargetDependency, deque, ctx);
                }

                if (dependencyMatcher.matches(resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(), resolvedDependency.getVersion())) {
                    ResolvedDependency directDependency = requireNonNull(deque.peekLast());
                    GroupArtifactVersion requestedGav = directDependency.getGav().asGroupArtifactVersion();
                    configurationToDirectDependency.computeIfAbsent(configurationName, __ -> new HashSet<>()).add(requestedGav);
                    directDependencyToTargetDependency.computeIfAbsent(requestedGav, __ -> new HashSet<>()).add(resolvedDependency.getGav().asGroupArtifactVersion());
                    createDataTableRow(projectName, sourceSetName, configurationName, resolvedDependency.getGav(), new ArrayList<>(deque), ctx);
                }

                deque.removeFirst();
            }

            private void createDataTableRow(
                    String projectName,
                    String sourceSetName,
                    String configurationName,
                    ResolvedGroupArtifactVersion gav,
                    List<ResolvedDependency> dependencyPath,
                    ExecutionContext ctx
            ) {
                String dependencyGraph = DependencyGraph.render(configurationName, dependencyPath);
                dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                        projectName,
                        sourceSetName,
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion(),
                        gav.getDatedSnapshotVersion(),
                        configurationName,
                        dependencyPath.size() - 1,
                        dependencyGraph
                ));
            }
        };
    }

    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @Data
    private static class MarkIndividualDependency extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency;
        private final Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency;
        private final Set<GroupArtifactVersion> individuallyMarkedDependencies = new HashSet<>();

        public Tree attachMarkers(Tree before, ExecutionContext ctx) {
            Tree after = super.visitNonNull(before, ctx);
            if (after == before) {
                String resultText = directDependencyToTargetDependency.entrySet().stream()
                        .filter(target -> !individuallyMarkedDependencies.contains(target.getKey()))
                        .map(Map.Entry::getValue)
                        .flatMap(Set::stream)
                        .distinct()
                        .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                        .collect(joining(","));
                if (!resultText.isEmpty()) {
                    return SearchResult.found(after, resultText);
                }
            }
            return after;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (DEPENDENCY_CLOSURE_MATCHER.matches(m)) {
                String resultText = directDependencyToTargetDependency.entrySet().stream()
                        .filter(target -> !individuallyMarkedDependencies.contains(target.getKey()))
                        .map(Map.Entry::getValue)
                        .flatMap(Set::stream)
                        .distinct()
                        .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                        .sorted()
                        .collect(joining(","));
                if (!resultText.isEmpty()) {
                    directDependencyToTargetDependency.clear();
                    return SearchResult.found(m, resultText);
                }
            }

            if (configurationToDirectDependency.containsKey(m.getSimpleName())) {
                return new GradleDependency.Matcher().get(getCursor()).map(dependency -> {
                    ResolvedGroupArtifactVersion gav = dependency.getResolvedDependency().getGav();
                    Optional<GroupArtifactVersion> configurationGav = configurationToDirectDependency.get(m.getSimpleName()).stream()
                            .filter(dep -> dep.asGroupArtifact().equals(gav.asGroupArtifact()))
                            .findAny();
                    if (configurationGav.isPresent()) {
                        Set<GroupArtifactVersion> mark = directDependencyToTargetDependency.get(configurationGav.get());
                        if (mark == null) {
                            return null;
                        }
                        individuallyMarkedDependencies.add(configurationGav.get());
                        String resultText = mark.stream()
                                .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
                                .sorted()
                                .collect(joining(","));
                        if (!resultText.isEmpty()) {
                            return SearchResult.found(m, resultText);
                        }
                    }
                    return null;
                }).orElse(m);
            }
            return m;
        }
    }
}
