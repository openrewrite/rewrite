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
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.singletonList;
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
        Validated<Object> v = super.validate();
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

                DependencyGraph dependencyGraph = new DependencyGraph();
                Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths = collectDependencyPaths(gp, dependencyGraph);

                // configuration -> dependency which is or transitively depends on search target -> search target
                Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency = new HashMap<>();
                Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency = new HashMap<>();

                // Collect and process all matching dependencies
                Map<String, List<DependencyInfo>> matchingDependencies = collectMatchingDependencies(gp, sourceFile);
                processMatchingDependencies(matchingDependencies, dependencyPaths, dependencyGraph,
                        dependenciesInUse, configurationToDirectDependency, directDependencyToTargetDependency,
                        projectName, sourceSetName, ctx);

                // Check if any dependencies were found at all (including those without paths)
                if (matchingDependencies.isEmpty()) {
                    return sourceFile;
                }

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

            private void processDependency(ResolvedDependency directDep, ResolvedDependency dep, String configName,
                                          DependencyGraph dependencyGraph,
                                          Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths,
                                          DependenciesInUse dependenciesInUse,
                                          Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                                          Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                                          String projectName, String sourceSetName, ExecutionContext ctx) {
                GroupArtifactVersion requestedGav = new GroupArtifactVersion(directDep.getGroupId(), directDep.getArtifactId(), directDep.getVersion());
                GroupArtifactVersion targetGav = new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                configurationToDirectDependency.computeIfAbsent(configName, EMPTY).add(requestedGav);
                directDependencyToTargetDependency.computeIfAbsent(requestedGav, EMPTY).add(targetGav);

                String depGraph = buildDependencyGraph(dependencyGraph, dep, dependencyPaths, configName);

                dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                        projectName,
                        sourceSetName,
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        dep.getVersion(),
                        dep.getDatedSnapshotVersion(),
                        dep.getRequested().getScope(),
                        dep.getDepth(),
                        depGraph
                ));
            }

            private void processMultiplePaths(ResolvedGroupArtifactVersion gav,
                                             Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths,
                                             DependencyGraph dependencyGraph,
                                             DependenciesInUse dependenciesInUse,
                                             Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                                             Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                                             String projectName, String sourceSetName, ExecutionContext ctx) {
                List<DependencyGraph.DependencyPath> paths = dependencyPaths.get(gav);
                if (paths == null || paths.isEmpty()) {
                    return;
                }

                // Group paths by their immediate parent and keep the longest path for each parent
                Map<String, DependencyGraph.DependencyPath> pathsByParent = groupPathsByParent(paths);

                // Sort paths by depth (deepest first) for consistent ordering
                List<DependencyGraph.DependencyPath> uniquePaths = new ArrayList<>(pathsByParent.values());
                uniquePaths.sort((p1, p2) -> Integer.compare(p2.getPath().size(), p1.getPath().size()));

                // Create a row for each unique path
                for (DependencyGraph.DependencyPath path : uniquePaths) {
                    createDataTableRow(gav, path, dependencyGraph, dependenciesInUse,
                            configurationToDirectDependency, directDependencyToTargetDependency,
                            projectName, sourceSetName, ctx);
                }
            }

            private Map<String, DependencyGraph.DependencyPath> groupPathsByParent(List<DependencyGraph.DependencyPath> paths) {
                Map<String, DependencyGraph.DependencyPath> pathsByParent = new HashMap<>();
                for (DependencyGraph.DependencyPath path : paths) {
                    List<DependencyGraph.DependencyNode> nodes = path.getPath();
                    if (nodes.isEmpty()) {
                        continue;
                    }

                    String parentKey = determineParentKey(nodes, path.getScope());

                    // Keep the longest path for each parent
                    DependencyGraph.DependencyPath existing = pathsByParent.get(parentKey);
                    if (existing == null || path.getPath().size() > existing.getPath().size()) {
                        pathsByParent.put(parentKey, path);
                    }
                }
                return pathsByParent;
            }

            private String determineParentKey(List<DependencyGraph.DependencyNode> nodes, String configName) {
                if (nodes.size() == 1) {
                    // Direct dependency - include configuration to avoid deduplicating across independent configurations
                    return "direct:" + configName;
                } else {
                    // nodes.get(0) is the target dependency itself
                    // nodes.get(1) is its immediate parent
                    DependencyGraph.DependencyNode parentNode = nodes.get(1);
                    return parentNode.getGroupId() + ":" + parentNode.getArtifactId() + ":" + parentNode.getVersion() + ":" + configName;
                }
            }

            private void createDataTableRow(ResolvedGroupArtifactVersion gav,
                                           DependencyGraph.DependencyPath path,
                                           DependencyGraph dependencyGraph,
                                           DependenciesInUse dependenciesInUse,
                                           Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                                           Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                                           String projectName, String sourceSetName, ExecutionContext ctx) {
                List<DependencyGraph.DependencyNode> nodes = path.getPath();
                String configName = path.getScope();
                int depth = nodes.size() - 1; // Depth is path length minus 1

                // Find the direct dependency (the last node before the configuration)
                DependencyGraph.DependencyNode directDepNode = nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);

                if (directDepNode != null) {
                    updateDependencyMappings(gav, directDepNode, configName,
                            configurationToDirectDependency, directDependencyToTargetDependency);
                }

                // Build the dependency graph string for this specific path
                String depGraph = buildIsolatedPathGraph(gav, path, depth, configName, dependencyGraph);

                dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                        projectName,
                        sourceSetName,
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion(),
                        gav.getDatedSnapshotVersion(),
                        configName,
                        depth,
                        depGraph
                ));
            }

            private void updateDependencyMappings(ResolvedGroupArtifactVersion gav,
                                                 DependencyGraph.DependencyNode directDepNode,
                                                 String configName,
                                                 Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                                                 Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency) {
                GroupArtifactVersion requestedGav = new GroupArtifactVersion(
                        directDepNode.getGroupId(),
                        directDepNode.getArtifactId(),
                        directDepNode.getVersion());
                GroupArtifactVersion targetGav = new GroupArtifactVersion(
                        gav.getGroupId(),
                        gav.getArtifactId(),
                        gav.getVersion());
                configurationToDirectDependency.computeIfAbsent(configName, EMPTY).add(requestedGav);
                directDependencyToTargetDependency.computeIfAbsent(requestedGav, EMPTY).add(targetGav);
            }

            private String buildIsolatedPathGraph(ResolvedGroupArtifactVersion gav,
                                                 DependencyGraph.DependencyPath path,
                                                 int depth,
                                                 String configName,
                                                 DependencyGraph dependencyGraph) {
                // Create a temporary map with only this specific path
                Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> singlePathMap = new HashMap<>();
                singlePathMap.put(gav, singletonList(path));
                return dependencyGraph.buildDependencyGraph(gav, singlePathMap, depth, configName);
            }

            private Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> collectDependencyPaths(GradleProject gp, DependencyGraph dependencyGraph) {
                Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths = new HashMap<>();
                // Collect paths from all matching configurations, not just the first one
                gp.getConfigurations().stream()
                        .filter(this::matchesConfiguration)
                        .forEach(c -> dependencyGraph.collectGradleDependencyPaths(c.getDirectResolved(), dependencyPaths, c.getName()));
                return dependencyPaths;
            }

            private Map<String, List<DependencyInfo>> collectMatchingDependencies(GradleProject gp, SourceFile sourceFile) {
                Map<String, List<DependencyInfo>> matchingDependencies = new HashMap<>();
                @Nullable VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;

                for (GradleDependencyConfiguration c : gp.getConfigurations()) {
                    if (!matchesConfiguration(c)) {
                        continue;
                    }
                    for (ResolvedDependency resolvedDependency : c.getDirectResolved()) {
                        if (!resolvedDependency.isDirect()) {
                            continue;
                        }
                        List<ResolvedDependency> nestedMatchingDependencies = resolvedDependency.findDependencies(groupIdPattern, artifactIdPattern);
                        for (ResolvedDependency dep : nestedMatchingDependencies) {
                            if (versionComparator != null && !versionComparator.isValid(null, dep.getVersion())) {
                                continue;
                            }
                            String gavKey = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
                            matchingDependencies.computeIfAbsent(gavKey, k -> new ArrayList<>())
                                    .add(new DependencyInfo(resolvedDependency, dep, c.getName()));
                        }
                    }
                }
                return matchingDependencies;
            }

            private void processMatchingDependencies(Map<String, List<DependencyInfo>> matchingDependencies,
                                                    Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths,
                                                    DependencyGraph dependencyGraph,
                                                    DependenciesInUse dependenciesInUse,
                                                    Map<String, Set<GroupArtifactVersion>> configurationToDirectDependency,
                                                    Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency,
                                                    String projectName, String sourceSetName, ExecutionContext ctx) {
                for (Map.Entry<String, List<DependencyInfo>> entry : matchingDependencies.entrySet()) {
                    List<DependencyInfo> depInfos = entry.getValue();
                    if (depInfos.isEmpty()) {
                        continue;
                    }

                    // Use the first dependency info to get the GAV (they all have the same GAV)
                    DependencyInfo firstInfo = depInfos.get(0);
                    ResolvedGroupArtifactVersion gav = firstInfo.getDep().getGav();

                    // Process paths for this dependency
                    processMultiplePaths(gav, dependencyPaths, dependencyGraph, dependenciesInUse,
                            configurationToDirectDependency, directDependencyToTargetDependency,
                            projectName, sourceSetName, ctx);
                }
            }

            private String buildDependencyGraph(DependencyGraph dependencyGraph, ResolvedDependency dep, Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> dependencyPaths, String configurationName) {
                return dependencyGraph.buildDependencyGraph(
                        dep.getGav(),
                        dependencyPaths,
                        dep.getDepth(),
                        configurationName
                );
            }
        };
    }

    @Value
    private static class DependencyInfo {
        ResolvedDependency directDep;
        ResolvedDependency dep;
        String configName;
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
                        configurationToDirectDependency.get(m.getSimpleName());
                        Set<GroupArtifactVersion> mark = directDependencyToTargetDependency.get(configurationGav.get());
                        if (mark == null) {
                            return null;
                        }
                        individuallyMarkedDependencies.add(configurationGav.get());
                        String resultText = mark.stream()
                                .map(target -> target.getGroupId() + ":" + target.getArtifactId() + ":" + target.getVersion())
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
