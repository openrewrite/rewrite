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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;

import java.util.*;

import static java.util.Collections.emptySet;

/**
 * An inverse dependency graph rooted at a matched dependency: it shows, for one matched
 * coordinate, every direct dependency it is reachable from and the paths in between.
 * <p>
 * Nodes are shared by coordinate, so a dependency reached through many paths (a diamond) is
 * stored once — the graph is a DAG, not a per-path tree. This keeps construction and storage
 * {@code O(nodes + edges)} even when the number of distinct root→match paths is exponential,
 * while {@code getSize()} still reports that (potentially huge) path count via dynamic
 * programming rather than by enumerating the paths.
 */
@RequiredArgsConstructor
public class DependencyGraph {
    private final @Nullable Node root;

    @Getter
    private final int size;

    /**
     * Build one inverse dependency graph per matching dependency reachable from {@code roots}.
     *
     * @param configOrScope the scope (Maven) or configuration name (Gradle) the roots belong to
     * @param roots         the direct dependencies to search
     * @param matcher       the matcher identifying the dependencies of interest
     * @return matched coordinate → its inverse dependency graph, in the order matches are first seen
     */
    public static Map<ResolvedGroupArtifactVersion, DependencyGraph> build(
            String configOrScope, List<ResolvedDependency> roots, DependencyMatcher matcher) {
        Map<String, Node> nodes = new HashMap<>();
        Map<String, ResolvedGroupArtifactVersion> matched = new LinkedHashMap<>();
        Set<GroupArtifactVersion> explored = new HashSet<>();
        for (ResolvedDependency root : roots) {
            Node rootNode = intern(nodes, root.getGav());
            rootNode.getChildren().add(new ConfigurationNode(configOrScope));
            collect(root, rootNode, nodes, matched, explored, matcher);
        }

        Map<ResolvedGroupArtifactVersion, DependencyGraph> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, ResolvedGroupArtifactVersion> match : matched.entrySet()) {
            Node node = nodes.get(match.getKey());
            long paths = countPaths(node, new HashMap<>(), new HashSet<>());
            graphs.put(match.getValue(), new DependencyGraph(node, (int) Math.min(paths, Integer.MAX_VALUE)));
        }
        return graphs;
    }

    private static void collect(ResolvedDependency dependency, Node node,
                                Map<String, Node> nodes, Map<String, ResolvedGroupArtifactVersion> matched,
                                Set<GroupArtifactVersion> explored, DependencyMatcher matcher) {
        if (matcher.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
            matched.putIfAbsent(node.getId(), dependency.getGav());
        }
        // A single visited-set spanning the whole traversal: each subtree is walked once, and each
        // parent→child edge is recorded once, regardless of how many paths reach the child.
        if (!explored.add(dependency.getGav().asGroupArtifactVersion())) {
            return;
        }
        for (ResolvedDependency child : dependency.getDependencies()) {
            Node childNode = intern(nodes, child.getGav());
            childNode.getChildren().add(node); // inverse edge: the parent becomes a child in this graph
            collect(child, childNode, nodes, matched, explored, matcher);
        }
    }

    private static Node intern(Map<String, Node> nodes, ResolvedGroupArtifactVersion gav) {
        return nodes.computeIfAbsent(formatDependency(gav), DependencyGraph::createEmptyNode);
    }

    /**
     * Counts distinct paths from {@code node} down to the configuration/scope leaves — i.e. the
     * number of distinct root→match paths — by dynamic programming over the DAG, so a diamond graph
     * is counted in {@code O(nodes + edges)} rather than by enumerating exponentially many paths.
     */
    private static long countPaths(@Nullable Node node, Map<Node, Long> memo, Set<Node> onPath) {
        if (node instanceof ConfigurationNode) {
            return 1;
        }
        if (node == null || !onPath.add(node)) {
            return 0; // null guard, and break any cycle just as per-path traversal would
        }
        Long cached = memo.get(node);
        if (cached != null) {
            onPath.remove(node);
            return cached;
        }
        long total = 0;
        for (Node child : node.getChildren()) {
            total += countPaths(child, memo, onPath);
        }
        onPath.remove(node);
        memo.put(node, total);
        return total;
    }

    public @Nullable String print() {
        if (root == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(root.getId()).append("\n");
        Set<Node> visited = new HashSet<>();
        int i = 0;
        for (Node child : root.getChildren()) {
            print0(sb, new StringBuilder(), child, visited, i++ == root.getChildren().size() - 1);
        }
        return sb.toString();
    }

    private void print0(StringBuilder sb, StringBuilder prefix, Node node, Set<Node> visited, boolean lastChild) {
        boolean alreadySeen = !visited.add(node);

        sb.append(prefix).append(lastChild ? "\\--- " : "+--- ").append(node.getId());
        if (alreadySeen) {
            sb.append(" (*)\n");
            return;
        }
        sb.append("\n");

        prefix.append(lastChild ? "     " : "|    ");
        int i = 0;
        for (Node child : node.getChildren()) {
            print0(sb, prefix, child, visited, i++ == node.getChildren().size() - 1);
        }
        if (prefix.length() > 0) {
            prefix.setLength(prefix.length() - 5);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private static String formatDependency(ResolvedGroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    private static Node createEmptyNode(String id) {
        return new DependencyNode(id, new TreeSet<>());
    }

    private interface Node extends Comparable<Node> {
        String getId();

        Set<Node> getChildren();

        @Override
        default int compareTo(Node other) {
            if (this instanceof ConfigurationNode && other instanceof ConfigurationNode) {
                return getId().compareTo(other.getId());
            } else if (this instanceof ConfigurationNode) {
                return -1;
            } else if (other instanceof ConfigurationNode) {
                return 1;
            }
            return getId().compareTo(other.getId());
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class DependencyNode implements Node {
        @EqualsAndHashCode.Include
        String id;
        Set<Node> children;
    }

    @RequiredArgsConstructor
    @Getter
    private static class ConfigurationNode implements Node {
        final String id;
        Set<Node> children = emptySet();
    }
}
