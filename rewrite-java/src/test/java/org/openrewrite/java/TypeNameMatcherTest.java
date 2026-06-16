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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeNameMatcherTest {

    @Test
    void doubleDotStarFollowedBySuffixHonorsSuffix() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("..*.remove");
        assertThat(matcher.matches("java.util.List.remove")).isTrue();
        assertThat(matcher.matches("java.util.Map.remove")).isTrue();
        assertThat(matcher.matches("com.example.Foo.bar")).isFalse();
        assertThat(matcher.matches("org.springframework.boot.SpringApplication.run")).isFalse();
    }

    @Test
    void doubleDotStarFollowedByNestedTypeSuffix() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("..*.Builder");
        assertThat(matcher.matches("com.foo.Bar.Builder")).isTrue();
        assertThat(matcher.matches("com.foo.Bar.Baz")).isFalse();
    }

    @Test
    void doubleDotStarAtEndMatchesAnyNonEmptyRemainder() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("java.util..*");
        assertThat(matcher.matches("java.util.List")).isTrue();
        assertThat(matcher.matches("java.util.concurrent.ConcurrentMap")).isTrue();
    }

    @Test
    void doubleDotSuffix() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("..List.remove");
        assertThat(matcher.matches("java.util.List.remove")).isTrue();
        assertThat(matcher.matches("java.util.List.add")).isFalse();
    }

    @Test
    void leadingFullWildcardSuffix() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("*..List.remove");
        assertThat(matcher.matches("java.util.List.remove")).isTrue();
        assertThat(matcher.matches("java.util.List.add")).isFalse();
    }

    @Test
    void singleSegmentWildcardSuffix() {
        TypeNameMatcher matcher = TypeNameMatcher.fromPattern("java.util.*.remove");
        assertThat(matcher.matches("java.util.List.remove")).isTrue();
        assertThat(matcher.matches("java.util.List.add")).isFalse();
    }
}
