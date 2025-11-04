/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.graph;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.List;
import java.util.Map;

public class DependencyGraph {

    private final MavenDependencyGraphBuilder mavenBuilder = new MavenDependencyGraphBuilder();
    private final GradleDependencyGraphBuilder gradleBuilder = new GradleDependencyGraphBuilder();

    @Value
    public static class DependencyPath {
        List<DependencyNode> path;
        String scope; // Maven scope or Gradle configuration
    }

    @Getter
    @AllArgsConstructor
    public static class DependencyNode {
        String groupId;
        String artifactId;
        String version;
        String scope; // Maven scope or Gradle configuration
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static class GradleDependencyNode extends DependencyNode {
        ResolvedDependency resolvedDependency;

        public GradleDependencyNode(String groupId, String artifactId, String version, String scope, ResolvedDependency resolvedDependency) {
            super(groupId, artifactId, version, scope);
            this.resolvedDependency = resolvedDependency;
        }
    }

    /**
     * Collects dependency paths for Maven dependencies.
     */
    public void collectMavenDependencyPaths(List<ResolvedDependency> dependencies,
                                           Map<ResolvedGroupArtifactVersion, List<DependencyPath>> paths,
                                           String scope) {
        mavenBuilder.collectDependencyPaths(dependencies, paths, scope);
    }

    /**
     * Collects dependency paths for Gradle dependencies.
     */
    public void collectGradleDependencyPaths(List<ResolvedDependency> dependencies,
                                            Map<ResolvedGroupArtifactVersion, List<DependencyPath>> paths,
                                            String scope) {
        gradleBuilder.collectDependencyPaths(dependencies, paths, scope);
    }

    /**
     * Builds a complete dependency graph for the given GAV based on collected paths.
     * Automatically determines whether to build a direct or inverse dependency tree.
     *
     * @param gav The dependency to build a graph for
     * @param projectPaths Collected dependency paths
     * @param minDepth Minimum depth of the dependency (0 for direct, >0 for transitive)
     * @param scopeOrConfiguration Maven scope name or Gradle configuration name to use as fallback
     */
    public String buildDependencyGraph(ResolvedGroupArtifactVersion gav,
                                      Map<ResolvedGroupArtifactVersion, List<DependencyPath>> projectPaths,
                                      int minDepth,
                                      String scopeOrConfiguration) {
        if (projectPaths == null || !projectPaths.containsKey(gav)) {
            // Fallback format
            return formatDependency(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()) +
                   "\n\\--- " + scopeOrConfiguration;
        }

        List<DependencyPath> paths = projectPaths.get(gav);
        if (paths.isEmpty()) {
            return formatDependency(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()) +
                   "\n\\--- " + scopeOrConfiguration;
        }

        // Determine scope/configuration from the first path
        String actualScopeOrConfig = determineScopeOrConfig(paths, scopeOrConfiguration);

        if (minDepth == 0) {
            // Direct dependency
            return buildDirectDependencyGraph(gav, paths, actualScopeOrConfig);
        }

        // Transitive dependency - build inverse tree
        return buildInverseDependencyTree(gav, projectPaths, actualScopeOrConfig);
    }

    private String determineScopeOrConfig(List<DependencyPath> paths, String defaultScopeOrConfig) {
        if (!paths.isEmpty()) {
            DependencyPath firstPath = paths.get(0);
            if (firstPath.getScope() != null && !firstPath.getScope().isEmpty()) {
                return firstPath.getScope();
            }
        }
        return defaultScopeOrConfig;
    }

    private String buildDirectDependencyGraph(ResolvedGroupArtifactVersion gav,
                                             List<DependencyPath> paths,
                                             String scopeOrConfig) {
        DependencyPath firstPath = paths.get(0);

        // For Gradle direct dependencies with resolved dependency info
        if (firstPath.getPath().size() == 1 && firstPath.getPath().get(0) instanceof GradleDependencyNode) {
            GradleDependencyNode node = (GradleDependencyNode) firstPath.getPath().get(0);
            if (node.getResolvedDependency() != null) {
                return render(node.getResolvedDependency(), scopeOrConfig);
            }
        }

        // Fallback to simple format
        return formatDependency(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()) +
               "\n\\--- " + scopeOrConfig;
    }

    /**
     * Renders a direct dependency graph showing just the dependency and its configuration.
     */
    public String render(ResolvedDependency dependency, String configuration) {
        return formatDependency(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()) +
               "\n\\--- " + configuration;
    }

    /**
     * Builds an inverse dependency tree showing the path from a (potentially transitive) dependency
     * up to the configuration that includes it.
     */
    public String buildInverseDependencyTree(ResolvedGroupArtifactVersion gav,
                                            Map<ResolvedGroupArtifactVersion, List<DependencyPath>> projectPaths,
                                            String scopeOrConfiguration) {
        List<DependencyPath> paths = projectPaths.get(gav);
        if (paths == null || paths.isEmpty()) {
            return "";
        }

        StringBuilder tree = new StringBuilder();
        tree.append(formatDependency(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()));

        DependencyPath path = paths.get(0); // Use the first path if multiple exist

        if (!path.getPath().isEmpty()) {
            appendDependencyPath(tree, path.getPath(), scopeOrConfiguration);
        }

        return tree.toString();
    }

    private void appendDependencyPath(StringBuilder tree, List<DependencyNode> nodes, String scopeOrConfiguration) {
        if (nodes.isEmpty()) {
            appendIndentedLine(tree, 0, scopeOrConfiguration);
            return;
        }

        // The first node is always the dependency itself (already added to tree), skip it
        // Add all parent dependencies
        for (int i = 1; i < nodes.size(); i++) {
            DependencyNode parent = nodes.get(i);
            appendIndentedLine(tree, i - 1, formatDependency(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()));
        }

        // Add the configuration at the end
        appendIndentedLine(tree, Math.max(0, nodes.size() - 1), scopeOrConfiguration);
    }

    private void appendIndentedLine(StringBuilder tree, int depth, String content) {
        tree.append("\n");
        for (int i = 0; i < depth; i++) {
            tree.append("     ");
        }
        tree.append("\\--- ").append(content);
    }

    private String formatDependency(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }
}
