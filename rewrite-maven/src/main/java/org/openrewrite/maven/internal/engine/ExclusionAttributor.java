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
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Pom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * The {@code effectiveExclusions} reporting post-pass (DESIGN §4.2). Walks each scope's resolved tree, accumulating the
 * exclusions declared along the path root→leaf; for every declared dependency of a node's pom that an accumulated
 * exclusion prunes, the excluded coordinate is attributed to the <em>shallowest</em> ancestor that declared that
 * exclusion — the same attribution the legacy BFS records inline via its {@code includedByMap} walk. Matching is Maven's
 * stock exact/{@code *} semantics (the glob superset is a removed parity bug), so this only reports what the stock
 * {@code ExclusionDependencySelector} actually pruned from the verbose graph.
 */
final class ExclusionAttributor {

    private final Function<GroupArtifactVersion, Pom> pomForGav;

    ExclusionAttributor(Function<GroupArtifactVersion, Pom> pomForGav) {
        this.pomForGav = pomForGav;
    }

    void attribute(List<Node> roots) {
        for (Node root : roots) {
            visit(root, Collections.emptyList());
        }
    }

    private void visit(Node node, List<Owner> inherited) {
        List<Owner> active = inherited;
        List<GroupArtifact> own = node.requested.getExclusions();
        if (own != null && !own.isEmpty()) {
            active = new ArrayList<>(inherited);
            for (GroupArtifact exclusion : own) {
                active.add(new Owner(exclusion, node));
            }
        }

        if (!active.isEmpty()) {
            Pom pom = pomForGav.apply(node.gav);
            if (pom != null) {
                for (Dependency declared : pom.getDependencies()) {
                    String groupId = declared.getGroupId() == null ? "" : declared.getGroupId();
                    String artifactId = declared.getArtifactId();
                    Owner shallowest = shallowestExcluding(active, groupId, artifactId);
                    if (shallowest != null) {
                        shallowest.node.effectiveExclusions.add(new GroupArtifact(groupId, artifactId));
                    }
                }
            }
        }

        for (Node child : node.children) {
            visit(child, active);
        }
    }

    // The first (root-most) exclusion that matches, then the first owner declaring that exclusion — matching legacy's
    // shallowest-declaring-ancestor attribution.
    private static @Nullable Owner shallowestExcluding(List<Owner> active, String groupId, String artifactId) {
        for (Owner candidate : active) {
            if (matches(candidate.exclusion, groupId, artifactId)) {
                for (Owner owner : active) {
                    if (owner.exclusion.equals(candidate.exclusion)) {
                        return owner;
                    }
                }
                return candidate;
            }
        }
        return null;
    }

    private static boolean matches(GroupArtifact exclusion, String groupId, String artifactId) {
        return matches(exclusion.getGroupId(), groupId) && matches(exclusion.getArtifactId(), artifactId);
    }

    private static boolean matches(String pattern, String value) {
        return "*".equals(pattern) || pattern.equals(value);
    }

    private static final class Owner {
        final GroupArtifact exclusion;
        final Node node;

        Owner(GroupArtifact exclusion, Node node) {
            this.exclusion = exclusion;
            this.node = node;
        }
    }
}
