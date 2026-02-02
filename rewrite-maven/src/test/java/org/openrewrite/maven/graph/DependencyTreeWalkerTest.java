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
package org.openrewrite.maven.graph;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyTreeWalkerTest {

    /**
     * Creates a diamond dependency graph:
     * <pre>
     *       A
     *      / \
     *     B   C
     *      \ /
     *       D
     *      / \
     *     E   F
     * </pre>
     * walk() visits all paths - diamond patterns visit shared nodes multiple times (once per path).
     * This preserves the "all paths" semantics needed for dependency insight counting.
     */
    @Test
    void walkVisitsAllPaths() {
        ResolvedDependency diamond = createDiamondGraph();

        Map<String, Integer> visitCount = new HashMap<>();
        DependencyTreeWalker.walk(diamond, null, (dep, path) -> {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            visitCount.merge(key, 1, Integer::sum);
        });

        // Root nodes visited once
        assertThat(visitCount).containsEntry("com.example:a", 1);
        assertThat(visitCount).containsEntry("com.example:b", 1);
        assertThat(visitCount).containsEntry("com.example:c", 1);
        // Diamond shared nodes visited twice (once via B, once via C)
        assertThat(visitCount).containsEntry("com.example:d", 2);
        assertThat(visitCount).containsEntry("com.example:e", 2);
        assertThat(visitCount).containsEntry("com.example:f", 2);
    }

    /**
     * Using SKIP_SAME_COORDINATES visits each node exactly once via its first encountered path.
     * Use this mode for recipes that only need to know "is dependency present?"
     * rather than "how many paths lead to this dependency?"
     */
    @Test
    void skipSameCoordinatesVisitsEachNodeOnce() {
        ResolvedDependency diamond = createDiamondGraph();

        Map<String, Integer> visitCount = new HashMap<>();
        DependencyTreeWalker.walk(diamond, null, (dep, path) -> {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            visitCount.merge(key, 1, Integer::sum);
            return DependencyTreeWalker.TraversalControl.SKIP_SAME_COORDINATES;
        });

        // Each node visited exactly once
        assertThat(visitCount.values()).allMatch(count -> count == 1,
                "All nodes should be visited exactly once with SKIP_SAME_COORDINATES, but found: " + visitCount);
    }

    /**
     * Using HALT stops traversal immediately.
     */
    @Test
    void haltStopsTraversal() {
        ResolvedDependency diamond = createDiamondGraph();

        Map<String, Integer> visitCount = new HashMap<>();
        DependencyTreeWalker.walk(diamond, null, (dep, path) -> {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            visitCount.merge(key, 1, Integer::sum);
            // Stop after visiting 'b'
            if ("b".equals(dep.getArtifactId())) {
                return DependencyTreeWalker.TraversalControl.HALT;
            }
            return DependencyTreeWalker.TraversalControl.CONTINUE;
        });

        // Should have visited a, b, then stopped (depth-first: a -> b -> halt)
        assertThat(visitCount).containsEntry("com.example:a", 1);
        assertThat(visitCount).containsEntry("com.example:b", 1);
        // c, d, e, f should not be visited
        assertThat(visitCount).doesNotContainKey("com.example:c");
    }

    private ResolvedDependency createDiamondGraph() {
        // Build shared subtree: D -> [E, F]
        ResolvedDependency e = dependency("com.example", "e", "1.0", List.of());
        ResolvedDependency f = dependency("com.example", "f", "1.0", List.of());
        ResolvedDependency d = dependency("com.example", "d", "1.0", List.of(e, f));

        // B and C both depend on the same D instance (simulating diamond)
        ResolvedDependency b = dependency("com.example", "b", "1.0", List.of(d));
        ResolvedDependency c = dependency("com.example", "c", "1.0", List.of(d));

        // A depends on both B and C
        return dependency("com.example", "a", "1.0", List.of(b, c));
    }

    /**
     * Tests that cycle detection still works - we should not infinite loop on cycles.
     */
    @Test
    void cycleDetectionPreventsInfiniteLoop() {
        // Create a cycle: A -> B -> C -> A
        ResolvedDependency a = dependency("com.example", "a", "1.0", List.of());
        ResolvedDependency c = dependency("com.example", "c", "1.0", List.of(a));
        ResolvedDependency b = dependency("com.example", "b", "1.0", List.of(c));
        // Now make A depend on B (creating cycle)
        a = a.withDependencies(List.of(b));

        Map<String, Integer> visitCount = new HashMap<>();
        DependencyTreeWalker.walk(a, null, (dep, path) -> {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            visitCount.merge(key, 1, Integer::sum);
        });

        // Should complete without infinite loop, each node visited once
        assertThat(visitCount).containsEntry("com.example:a", 1);
        assertThat(visitCount).containsEntry("com.example:b", 1);
        assertThat(visitCount).containsEntry("com.example:c", 1);
    }

    /**
     * Tests walking from multiple roots where roots share subtrees.
     * Shared subtrees are visited once per root, preserving all path information.
     */
    @Test
    void multipleRootsWithSharedSubtrees() {
        // Shared subtree
        ResolvedDependency shared = dependency("com.example", "shared", "1.0", List.of());
        ResolvedDependency common = dependency("com.example", "common", "1.0", List.of(shared));

        // Two roots both depending on common
        ResolvedDependency root1 = dependency("com.example", "root1", "1.0", List.of(common));
        ResolvedDependency root2 = dependency("com.example", "root2", "1.0", List.of(common));

        Map<String, Integer> visitCount = new HashMap<>();
        DependencyTreeWalker.walk(List.of(root1, root2), null, (dep, path) -> {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            visitCount.merge(key, 1, Integer::sum);
        });

        // Each root is visited once
        assertThat(visitCount).containsEntry("com.example:root1", 1);
        assertThat(visitCount).containsEntry("com.example:root2", 1);
        // Shared subtrees are visited once per root (2 roots = 2 visits)
        assertThat(visitCount).containsEntry("com.example:common", 2);
        assertThat(visitCount).containsEntry("com.example:shared", 2);
    }

    private static ResolvedDependency dependency(String groupId, String artifactId, String version,
                                                  List<ResolvedDependency> dependencies) {
        ResolvedGroupArtifactVersion gav = new ResolvedGroupArtifactVersion(
                null, groupId, artifactId, version, null);
        Dependency requested = Dependency.builder()
                .gav(new GroupArtifactVersion(groupId, artifactId, version))
                .build();
        return ResolvedDependency.builder()
                .gav(gav)
                .requested(requested)
                .dependencies(dependencies)
                .build();
    }
}
