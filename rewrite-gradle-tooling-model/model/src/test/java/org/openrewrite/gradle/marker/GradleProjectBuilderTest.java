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
package org.openrewrite.gradle.marker;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class GradleProjectBuilderTest {

    /**
     * Test helper to create a mock ResolvedDependency for testing cycle detection.
     * This simulates what Gradle would return for a dependency with the given coordinates and children.
     */
    @SuppressWarnings("NullableProblems")
    private static ResolvedDependency createMockResolvedDependency(
            String group, String name, String version,
            ResolvedDependency... children) {
        return new ResolvedDependency() {
            @Override
            public String getName() {
                return group + ":" + name + ":" + version;
            }

            @Override
            public String getModuleGroup() {
                return group;
            }

            @Override
            public String getModuleName() {
                return name;
            }

            @Override
            public String getModuleVersion() {
                return version;
            }

            @Override
            public String getConfiguration() {
                return "compile";
            }

            @Override
            public ResolvedModuleVersion getModule() {
                //noinspection DataFlowIssue
                return null;
            }

            @Override
            public Set<ResolvedDependency> getChildren() {
                return new LinkedHashSet<>(Arrays.asList(children));
            }

            @Override
            public Set<ResolvedDependency> getParents() {
                return emptySet();
                return emptySet();
                return emptySet();
                return emptySet();
                return emptySet();
                return emptySet();
            }

            @Override
            public Set<ResolvedArtifact> getAllModuleArtifacts() {
                return Collections.emptySet();
            }

            @Override
            public Set<ResolvedArtifact> getParentArtifacts(ResolvedDependency parent) {
                return Collections.emptySet();
            }

            @Override
            public Set<ResolvedArtifact> getArtifacts(ResolvedDependency parent) {
                return Collections.emptySet();
            }

            @Override
            public Set<ResolvedArtifact> getAllArtifacts(ResolvedDependency parent) {
                return Collections.emptySet();
            }
        };
    }

    @Test
    void detectsDirectSelfReference() {
        // Create a dependency that references itself (A -> A)
        ResolvedDependency[] holder = new ResolvedDependency[1];
        ResolvedDependency selfRef = createMockResolvedDependency("com.example", "artifact-a", "1.0.0");
        holder[0] = createMockResolvedDependency("com.example", "artifact-a", "1.0.0", selfRef);
        selfRef = holder[0];

        List<String> detectedCycles = new ArrayList<>();
        Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
        List<GroupArtifactVersion> ancestorPath = new ArrayList<>();

        ancestorPath.add(new GroupArtifactVersion("com.example", "artifact-a", "1.0.0"));

        org.openrewrite.maven.tree.ResolvedDependency result = GradleProjectBuilder.resolved(selfRef, 0, resolvedCache, detectedCycles, ancestorPath);

        // Verify the cycle was detected
        assertThat(detectedCycles)
                .as("Should detect direct self-reference cycle")
                .hasSize(1)
                .first()
                .asString()
                .matches("com\\.example:artifact-a:1\\.0\\.0 -> com\\.example:artifact-a:1\\.0\\.0");

        // Verify the cycle was removed from the dependency tree
        assertThat(result.getDependencies())
                .as("Self-reference should be removed from dependencies")
                .isEmpty();
    }

    @Test
    void detectsTwoLevelCycle() {
        ResolvedDependency[] holderA = new ResolvedDependency[1];
        ResolvedDependency[] holderB = new ResolvedDependency[1];

        holderA[0] = createMockResolvedDependency("com.example", "artifact-a", "1.0.0");
        holderB[0] = createMockResolvedDependency("com.example", "artifact-b", "1.0.0", holderA[0]);
        holderA[0] = createMockResolvedDependency("com.example", "artifact-a", "1.0.0", holderB[0]);

        List<String> detectedCycles = new ArrayList<>();
        Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
        List<GroupArtifactVersion> ancestorPath = new ArrayList<>();

        ancestorPath.add(new GroupArtifactVersion("com.example", "artifact-a", "1.0.0"));

        org.openrewrite.maven.tree.ResolvedDependency result = GradleProjectBuilder.resolved(holderA[0], 0, resolvedCache, detectedCycles, ancestorPath);

        // Verify the cycle was detected
        assertThat(detectedCycles)
                .as("Should detect two-level cycle (A -> B -> A)")
                .hasSize(1)
                .first()
                .asString()
                .matches("com\\.example:artifact-a:1\\.0\\.0 -> com\\.example:artifact-b:1\\.0\\.0 -> com\\.example:artifact-a:1\\.0\\.0");

        // Verify the cycle was removed: A should have child B, but B should have no children
        assertThat(result.getDependencies())
                .as("A should have B as dependency")
                .hasSize(1);
        assertThat(result.getDependencies().getFirst().getArtifactId())
                .isEqualTo("artifact-b");
        assertThat(result.getDependencies().getFirst().getDependencies())
                .as("B should not have A as dependency (cycle removed)")
                .isEmpty();
    }

    @Test
    void detectsThreeLevelCycle() {
        ResolvedDependency[] holderA = new ResolvedDependency[1];
        ResolvedDependency[] holderB = new ResolvedDependency[1];
        ResolvedDependency[] holderC = new ResolvedDependency[1];

        holderA[0] = createMockResolvedDependency("com.example", "artifact-a", "1.0.0");
        holderC[0] = createMockResolvedDependency("com.example", "artifact-c", "1.0.0", holderA[0]);
        holderB[0] = createMockResolvedDependency("com.example", "artifact-b", "1.0.0", holderC[0]);
        holderA[0] = createMockResolvedDependency("com.example", "artifact-a", "1.0.0", holderB[0]);

        List<String> detectedCycles = new ArrayList<>();
        Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
        List<GroupArtifactVersion> ancestorPath = new ArrayList<>();

        ancestorPath.add(new GroupArtifactVersion("com.example", "artifact-a", "1.0.0"));

        org.openrewrite.maven.tree.ResolvedDependency result = GradleProjectBuilder.resolved(holderA[0], 0, resolvedCache, detectedCycles, ancestorPath);

        // Verify the cycle was detected
        assertThat(detectedCycles)
                .as("Should detect three-level cycle (A -> B -> C -> A)")
                .hasSize(1)
                .first()
                .asString()
                .matches("com\\.example:artifact-a:1\\.0\\.0 -> com\\.example:artifact-b:1\\.0\\.0 -> com\\.example:artifact-c:1\\.0\\.0 -> com\\.example:artifact-a:1\\.0\\.0");

        // Verify the cycle was removed: A -> B -> C, but C should not have A
        assertThat(result.getDependencies())
                .as("A should have B as dependency")
                .hasSize(1);
        org.openrewrite.maven.tree.ResolvedDependency b = result.getDependencies().getFirst();
        assertThat(b.getArtifactId()).isEqualTo("artifact-b");
        assertThat(b.getDependencies())
                .as("B should have C as dependency")
                .hasSize(1);
        org.openrewrite.maven.tree.ResolvedDependency c = b.getDependencies().getFirst();
        assertThat(c.getArtifactId()).isEqualTo("artifact-c");
        assertThat(c.getDependencies())
                .as("C should not have A as dependency (cycle removed)")
                .isEmpty();
    }

    @Test
    void doesNotDetectNonCyclicDependencies() {
        ResolvedDependency c = createMockResolvedDependency("com.example", "artifact-c", "1.0.0");
        ResolvedDependency b = createMockResolvedDependency("com.example", "artifact-b", "1.0.0", c);
        ResolvedDependency a = createMockResolvedDependency("com.example", "artifact-a", "1.0.0", b);

        List<String> detectedCycles = new ArrayList<>();
        Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
        List<GroupArtifactVersion> ancestorPath = new ArrayList<>();

        ancestorPath.add(new GroupArtifactVersion("com.example", "artifact-a", "1.0.0"));

        org.openrewrite.maven.tree.ResolvedDependency result = GradleProjectBuilder.resolved(a, 0, resolvedCache, detectedCycles, ancestorPath);

        // Verify no cycles were detected
        assertThat(detectedCycles)
                .as("Should not detect cycles in normal dependency tree")
                .isEmpty();

        // Verify the full dependency chain is preserved: A -> B -> C
        assertThat(result.getDependencies())
                .as("A should have B as dependency")
                .hasSize(1);
        org.openrewrite.maven.tree.ResolvedDependency bDep = result.getDependencies().getFirst();
        assertThat(bDep.getArtifactId()).isEqualTo("artifact-b");
        assertThat(bDep.getDependencies())
                .as("B should have C as dependency")
                .hasSize(1);
        assertThat(bDep.getDependencies().getFirst().getArtifactId()).isEqualTo("artifact-c");
        assertThat(bDep.getDependencies().getFirst().getDependencies())
                .as("C should have no dependencies")
                .isEmpty();
    }

    @Test
    void allowsDuplicateDependenciesAtDifferentLevels() {
        ResolvedDependency d = createMockResolvedDependency("com.example", "artifact-d", "1.0.0");
        ResolvedDependency b = createMockResolvedDependency("com.example", "artifact-b", "1.0.0", d);
        ResolvedDependency c = createMockResolvedDependency("com.example", "artifact-c", "1.0.0", d);
        ResolvedDependency a = createMockResolvedDependency("com.example", "artifact-a", "1.0.0", b, c);

        List<String> detectedCycles = new ArrayList<>();
        Map<ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache = new HashMap<>();
        List<GroupArtifactVersion> ancestorPath = new ArrayList<>();
        ancestorPath.add(new GroupArtifactVersion("com.example", "artifact-a", "1.0.0"));
        org.openrewrite.maven.tree.ResolvedDependency result = GradleProjectBuilder.resolved(a, 0, resolvedCache, detectedCycles, ancestorPath);

        // Verify no cycles were detected
        assertThat(detectedCycles)
                .as("Should not detect cycles in diamond dependency pattern")
                .isEmpty();

        // Verify diamond structure is preserved: A has both B and C, both have D
        assertThat(result.getDependencies())
                .as("A should have B and C as dependencies")
                .hasSize(2)
                .extracting(org.openrewrite.maven.tree.ResolvedDependency::getArtifactId)
                .containsExactlyInAnyOrder("artifact-b", "artifact-c");

        // Both B and C should have D as dependency
        for (org.openrewrite.maven.tree.ResolvedDependency child : result.getDependencies()) {
            assertThat(child.getDependencies())
                    .as(child.getArtifactId() + " should have D as dependency")
                    .hasSize(1);
            assertThat(child.getDependencies().getFirst().getArtifactId())
                    .isEqualTo("artifact-d");
        }
    }
}
