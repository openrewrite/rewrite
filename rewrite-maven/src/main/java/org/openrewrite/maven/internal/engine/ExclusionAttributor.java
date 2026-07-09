/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.internal.engine.DependencyGraphMapper.Node;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * The {@code effectiveExclusions} reporting post-pass (DESIGN §4.2). Walks each scope's resolved tree, accumulating the
 * exclusions declared along the path root→leaf; for every effective (parent-merged) declared dependency of a node that
 * an accumulated exclusion prunes, the excluded coordinate is attributed to the <em>shallowest</em> ancestor that
 * declared that exclusion — the same attribution the legacy BFS records inline via its {@code includedByMap} walk. The
 * per-node exclusion set is the one Maven actually applied (requested + dependencyManagement-sourced, taken from the
 * aether node), and the enumerated dependencies are the effective set (a parent-inherited dependency included), so the
 * report matches legacy's {@code getRequestedDependencies()} walk over accumulated exclusions. Matching is Maven's stock
 * exact/{@code *} semantics (the glob superset is a removed parity bug), so this reports what the stock
 * {@code ExclusionDependencySelector} actually pruned from the verbose graph.
 */
final class ExclusionAttributor {

    private final Function<GroupArtifactVersion, List<GroupArtifact>> declaredDepsForGav;

    ExclusionAttributor(Function<GroupArtifactVersion, List<GroupArtifact>> declaredDepsForGav) {
        this.declaredDepsForGav = declaredDepsForGav;
    }

    void attribute(List<Node> roots) {
        for (Node root : roots) {
            visit(root, Collections.emptyList());
        }
    }

    // {@code ancestors} is the path from the root down to (but excluding) {@code node}, root-most first.
    private void visit(Node node, List<Node> ancestors) {
        // The set that prunes this node's children: its own effective exclusions (requested + managed) plus every
        // ancestor's, accumulated deepest-first to mirror legacy's ListUtils.concatAll(child, parent) order.
        List<GroupArtifact> accumulated = new ArrayList<>(node.ownExclusions);
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            accumulated.addAll(ancestors.get(i).ownExclusions);
        }

        if (!accumulated.isEmpty()) {
            for (GroupArtifact declared : declaredDepsForGav.apply(node.gav)) {
                GroupArtifact matched = firstMatch(accumulated, declared.getGroupId(), declared.getArtifactId());
                if (matched != null) {
                    attributionTarget(node, ancestors, matched).effectiveExclusions.add(declared);
                }
            }
        }

        List<Node> childAncestors = new ArrayList<>(ancestors);
        childAncestors.add(node);
        for (Node child : node.children) {
            visit(child, childAncestors);
        }
    }

    // Attribute a pruned coordinate to the shallowest ancestor in the unbroken chain (starting at the node whose child
    // matched, walking up) whose *requested* exclusions declare the matching exclusion — legacy's includedByMap walk.
    private static Node attributionTarget(Node node, List<Node> ancestors, GroupArtifact exclusion) {
        Node declaredOn = node;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Node ancestor = ancestors.get(i);
            if (ancestor.requestedExclusions.contains(exclusion)) {
                declaredOn = ancestor;
            } else {
                break;
            }
        }
        return declaredOn;
    }

    private static @Nullable GroupArtifact firstMatch(List<GroupArtifact> exclusions, String groupId, String artifactId) {
        for (GroupArtifact exclusion : exclusions) {
            if (matches(exclusion.getGroupId(), groupId) && matches(exclusion.getArtifactId(), artifactId)) {
                return exclusion;
            }
        }
        return null;
    }

    private static boolean matches(String pattern, String value) {
        return "*".equals(pattern) || pattern.equals(value);
    }
}
