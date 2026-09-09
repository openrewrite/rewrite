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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ResolvedDependencyTest {

    private static ResolvedDependency dep(String groupId, String artifactId, List<GroupArtifact> exclusions, ResolvedDependency... children) {
        return ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, groupId, artifactId, "1.0", null))
          .requested(Dependency.builder()
            .gav(new GroupArtifactVersion(groupId, artifactId, "1.0"))
            .exclusions(exclusions)
            .build())
          .dependencies(List.of(children))
          .build();
    }

    private static ResolvedDependency dep(String groupId, String artifactId, ResolvedDependency... children) {
        return dep(groupId, artifactId, emptyList(), children);
    }

    @Test
    void findDirectExactMatch() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.apache.commons", "commons-lang3"));
        ResolvedDependency found = root.findDependency("org.apache.commons", "commons-lang3");
        assertThat(found).isNotNull();
        assertThat(found.getArtifactId()).isEqualTo("commons-lang3");
    }

    @Test
    void selfMatchReturnsThis() {
        ResolvedDependency root = dep("com.example", "root");
        assertThat(root.findDependency("com.example", "root")).isSameAs(root);
    }

    @Test
    void exactMatchIsCaseInsensitiveLikeGlob() {
        // matchesGlob is case-insensitive; the exact-match fast path must preserve that.
        ResolvedDependency root = dep("com.example", "root",
          dep("org.apache.commons", "commons-lang3"));
        assertThat(root.findDependency("ORG.APACHE.COMMONS", "Commons-Lang3")).isNotNull();
    }

    @Test
    void noMatchReturnsNull() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.apache.commons", "commons-lang3"));
        assertThat(root.findDependency("com.google.guava", "guava")).isNull();
    }

    @Test
    void globMatchStillWorks() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.apache.commons", "commons-lang3"),
          dep("org.apache.logging.log4j", "log4j-core"));
        assertThat(root.findDependencies("org.apache.*", "*"))
          .extracting(ResolvedDependency::getArtifactId)
          .containsExactly("commons-lang3", "log4j-core");
    }

    @Test
    void findsTransitiveDependency() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.springframework", "spring-core",
            dep("org.springframework", "spring-jcl")));
        assertThat(root.findDependency("org.springframework", "spring-jcl")).isNotNull();
    }

    @Test
    void exclusionHidesTransitiveMatch() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.springframework", "spring-core",
            singletonList(new GroupArtifact("commons-logging", "commons-logging")),
            dep("commons-logging", "commons-logging")));
        assertThat(root.findDependency("commons-logging", "commons-logging")).isNull();
        assertThat(root.findDependencies("commons-logging", "commons-logging")).isEmpty();
    }

    @Test
    void exclusionWithGlobPattern() {
        ResolvedDependency root = dep("com.example", "root",
          dep("org.springframework", "spring-core",
            singletonList(new GroupArtifact("commons-logging", "*")),
            dep("commons-logging", "commons-logging")));
        assertThat(root.findDependency("commons-logging", "commons-logging")).isNull();
    }

    @Test
    void findDependenciesIncludesSelfAndDescendants() {
        ResolvedDependency root = dep("org.apache.commons", "commons-lang3",
          dep("org.apache.commons", "commons-lang3-extra"));
        assertThat(root.findDependencies("org.apache.commons", "*"))
          .extracting(ResolvedDependency::getArtifactId)
          .containsExactly("commons-lang3", "commons-lang3-extra");
    }

    @Test
    void diamondGraphDeduplicatesSharedNonMatchingSubtree() {
        // 'target' sits below 'shared', and 'shared' is reachable through both 'a' and 'b'.
        ResolvedDependency target = dep("org.slf4j", "slf4j-api");
        ResolvedDependency shared = dep("shared", "shared", target);
        ResolvedDependency root = dep("com.example", "root",
          dep("a", "a", shared),
          dep("b", "b", shared));
        assertThat(root.findDependency("org.slf4j", "slf4j-api")).isSameAs(target);
        // The shared, non-matching node is visited once, so the match below it is reported once.
        assertThat(root.findDependencies("org.slf4j", "slf4j-api")).hasSize(1);
    }

    @Test
    void wildcardPatternMatchesEverything() {
        ResolvedDependency root = dep("com.example", "root",
          dep("a", "a"),
          dep("b", "b"));
        assertThat(root.findDependencies("*", "*")).hasSize(3);
    }
}
