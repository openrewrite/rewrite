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

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyTreeWalkerTest {

    private static ResolvedDependency dep(String artifactId, ResolvedDependency... children) {
        return ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, "org.example", artifactId, "1.0", null))
          .dependencies(asList(children))
          .build();
    }

    /**
     * A chain of {@code depth} diamonds: reaching the deepest node takes {@code 2^depth} distinct
     * paths, but there are only {@code 3 * depth + 1} distinct nodes. Enumerating every path is what
     * OOMs {@code DependencyInsight} on ubiquitous transitive dependencies like jackson.
     */
    @Test
    void dedupsSharedSubtreesInsteadOfEnumeratingEveryPath() {
        int depth = 20; // 2^20 == 1,048,576 root->leaf paths

        ResolvedDependency junction = dep("junction-" + depth);
        for (int i = depth - 1; i >= 0; i--) {
            ResolvedDependency left = dep("left-" + i, junction);
            ResolvedDependency right = dep("right-" + i, junction);
            junction = dep("junction-" + i, left, right);
        }

        int expectedNodes = 3 * depth + 1;
        AtomicInteger visits = new AtomicInteger();
        DependencyTreeWalker.walk(junction, null, (matched, path) -> {
            if (visits.incrementAndGet() > 10_000) {
                throw new AssertionError("walker enumerated paths instead of deduping shared subtrees");
            }
        });

        assertThat(visits.get()).isEqualTo(expectedNodes);
    }

    @Test
    void terminatesOnCycles() {
        List<ResolvedDependency> aChildren = new ArrayList<>();
        ResolvedDependency a = ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, "org.example", "a", "1.0", null))
          .dependencies(aChildren)
          .build();
        aChildren.add(dep("b", a)); // a -> b -> a cycle

        List<String> visited = new ArrayList<>();
        DependencyTreeWalker.walk(a, null, (matched, path) -> visited.add(matched.getArtifactId()));

        assertThat(visited).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void reportsDirectDependencyAsLastPathElement() {
        ResolvedDependency leaf = dep("leaf");
        ResolvedDependency root = dep("root", dep("middle", leaf));

        DependencyTreeWalker.walk(root, null, (matched, path) -> {
            if ("leaf".equals(matched.getArtifactId())) {
                assertThat(path.getFirst().getArtifactId()).isEqualTo("leaf");
                assertThat(path.getLast().getArtifactId()).isEqualTo("root");
            }
        });
    }
}
