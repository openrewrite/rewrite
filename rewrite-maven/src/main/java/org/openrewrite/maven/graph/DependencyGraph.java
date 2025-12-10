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
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.*;

import static java.util.Collections.emptySet;

@RequiredArgsConstructor
public class DependencyGraph {
    @Nullable Node root;

    @Getter
    int size = 0;

    /**
     * Append a leaf-to-root dependency path to this dependency graph.
     */
    public void append(String configOrScope, Collection<ResolvedDependency> path) {
        if (path.isEmpty()) {
            return;
        }

        Iterator<ResolvedDependency> iterator = path.iterator();
        ResolvedDependency dependency = iterator.next();
        String id = formatDependency(dependency.getGav());
        if (root == null) {
            root = createEmptyNode(id);
        } else if (!root.getId().equals(id)) {
            throw new IllegalStateException("Dependency path is for a different root");
        }

        size++;

        Node parent = root;
        while (iterator.hasNext()) {
            dependency = iterator.next();
            id = formatDependency(dependency.getGav());
            Node child = null;
            for (Node node : parent.getChildren()) {
                if (node.getId().equals(id)) {
                    child = node;
                    break;
                }
            }
            if (child == null) {
                child = createEmptyNode(id);
                parent.getChildren().add(child);
            }
            parent = child;
        }
        parent.getChildren().add(new ConfigurationNode(configOrScope));
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

    private DependencyNode createEmptyNode(String id) {
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
