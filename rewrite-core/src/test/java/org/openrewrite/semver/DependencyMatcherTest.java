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
package org.openrewrite.semver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyMatcherTest {

    @Test
    void groupArtifact() {
        assertThat(DependencyMatcher.build("org.springframework.boot:*").isValid()).isTrue();
    }

    @Test
    void exactMatch() {
        DependencyMatcher matcher = DependencyMatcher.build("com.google.guava:guava").getValue();
        assertThat(matcher.matches("com.google.guava", "guava")).isTrue();
        assertThat(matcher.matches("com.google.guava", "guava-testlib")).isFalse();
        assertThat(matcher.matches("com.google.collect", "guava")).isFalse();
    }

    @Test
    void exactMatchIsCaseInsensitive() {
        // The exact fast path must preserve matchesGlob's case-insensitivity.
        DependencyMatcher matcher = DependencyMatcher.build("com.google.guava:guava").getValue();
        assertThat(matcher.matches("Com.Google.Guava", "Guava")).isTrue();
    }

    @Test
    void globMatch() {
        DependencyMatcher matcher = DependencyMatcher.build("com.google.*:gua?a").getValue();
        assertThat(matcher.matches("com.google.guava", "guava")).isTrue();
        assertThat(matcher.matches("com.google.collect", "guava")).isTrue();
        assertThat(matcher.matches("com.amazonaws", "guava")).isFalse();
        assertThat(matcher.matches("com.google.guava", "guava-testlib")).isFalse();
    }

    @Test
    void matchAll() {
        DependencyMatcher matcher = DependencyMatcher.build("*:*").getValue();
        assertThat(matcher.matches("com.google.guava", "guava")).isTrue();
        assertThat(matcher.matches(null, "guava")).isTrue();
        assertThat(matcher.matches("com.google.guava", null)).isTrue();
    }

    @Test
    void nullCoordinateAgainstExactPattern() {
        DependencyMatcher matcher = DependencyMatcher.build("com.google.guava:guava").getValue();
        assertThat(matcher.matches(null, "guava")).isFalse();
        assertThat(matcher.matches("com.google.guava", null)).isFalse();
    }

    @Test
    void withPatternRecomputesExactness() {
        // @With must route through the canonical constructor so derived flags stay consistent.
        DependencyMatcher matcher = DependencyMatcher.build("com.google.guava:guava").getValue()
                .withArtifactPattern("gua?a");
        assertThat(matcher.matches("com.google.guava", "guava")).isTrue();
        assertThat(matcher.matches("com.google.guava", "guaba")).isTrue();
        assertThat(matcher.matches("com.google.guava", "guava-testlib")).isFalse();
    }
}
