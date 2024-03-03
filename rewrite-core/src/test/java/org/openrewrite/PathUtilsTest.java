/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.PathUtils.matchesGlob;

class PathUtilsTest {
    @Test
    void globMatching() {
        // exact matches
        assertThat(matchesGlob(path("a"), null)).isFalse();
        assertThat(matchesGlob(path("a"), "")).isFalse();
        assertThat(matchesGlob(path(""), "")).isTrue();
        assertThat(matchesGlob(path("a"), "a")).isTrue();
        assertThat(matchesGlob(path("a/b/c"), "a/b/c")).isTrue();
        assertThat(matchesGlob(path("a\\b\\c"), "a\\b\\c")).isTrue();
        assertThat(matchesGlob(path("a/b/c"), "a\\b\\c")).isTrue();
        assertThat(matchesGlob(path("a\\b\\c"), "a/b/c")).isTrue();
        assertThat(matchesGlob(path("/test"), "/test")).isTrue();
        assertThat(matchesGlob(path("test"), "/test")).isFalse();
        assertThat(matchesGlob(path("/test"), "test")).isFalse();
        assertThat(matchesGlob(path("test/"), "test/")).isFalse(); // Trailing slash on path is not maintained
        assertThat(matchesGlob(path("test"), "test/")).isFalse();
        assertThat(matchesGlob(path("test/"), "test")).isTrue(); // Trailing slash on path is not maintained

        // matches with ?'s
        assertThat(matchesGlob(path("/test"), "/t?st")).isTrue();
        assertThat(matchesGlob(path("a/b/c"), "a/?/c")).isTrue();
        assertThat(matchesGlob(path("a/b/c"), "a/??/c")).isFalse();

        // matches with *'s
        assertThat(matchesGlob(path("test/"), "te*/")).isFalse(); // Trailing slash on path is not maintained
        assertThat(matchesGlob(path("/test"), "/te*")).isTrue();
        assertThat(matchesGlob(path("test/"), "*st/")).isFalse(); // Trailing slash on path is not maintained
        assertThat(matchesGlob(path("/test"), "/*st")).isTrue();
        assertThat(matchesGlob(path("test/"), "test/*")).isFalse();
        assertThat(matchesGlob(path("test/"), "test*")).isTrue(); // Trailing slash on path is not maintained
        assertThat(matchesGlob(path("/test"), "*/test")).isTrue();
        assertThat(matchesGlob(path("/test"), "*test")).isFalse();
        assertThat(matchesGlob(path("test/"), "test**")).isTrue(); // Trailing slash on path is not maintained
        assertThat(matchesGlob(path("test/a"), "test/*")).isTrue();
        assertThat(matchesGlob(path("test/a/b"), "test/*")).isFalse();
        assertThat(matchesGlob(path("a/test"), "*/test")).isTrue();
        assertThat(matchesGlob(path("a/b/test"), "*/test")).isFalse();
        assertThat(matchesGlob(path("a/test/b"), "a/*/b")).isTrue();
        assertThat(matchesGlob(path("a/test/test/b"), "a/*/b")).isFalse();
        assertThat(matchesGlob(path("test.txt"), "*/test.txt")).isFalse();
        assertThat(matchesGlob(path("a/test.txt"), "a/*.txt")).isTrue();
        assertThat(matchesGlob(path("a/test.txt"), "a/test.*")).isTrue();
        assertThat(matchesGlob(path("a/test.txt"), "a/*.*")).isTrue();
        assertThat(matchesGlob(path("a/test.test.txt"), "a/*.*")).isTrue();

        // matches with **'s
        assertThat(matchesGlob(path("test/"), "test/**")).isTrue();
        assertThat(matchesGlob(path("test/a"), "test/**")).isTrue();
        assertThat(matchesGlob(path("test/a/b"), "test/**")).isTrue();
        assertThat(matchesGlob(path("/test"), "**/test")).isTrue();
        assertThat(matchesGlob(path("a/test"), "**/test")).isTrue();
        assertThat(matchesGlob(path("a/b/test"), "**/test")).isTrue();
        assertThat(matchesGlob(path("a/test/b"), "a/**/b")).isTrue();
        assertThat(matchesGlob(path("a/test/test/b"), "a/**/b")).isTrue();
        assertThat(matchesGlob(path("a/b/test.txt"), "a/**/test.txt")).isTrue();

        // Exhaustive cases
        assertThat(matchesGlob(path("test/a/test/a/test"), "test/?/test/?/test")).isTrue();
        assertThat(matchesGlob(path("test/a/test/a/test"), "test/*/test/*/test")).isTrue();
        assertThat(matchesGlob(path("test/a/test/a/test"), "test/**/test/**/test")).isTrue();
        assertThat(matchesGlob(path("best/abc/test"), "?est/*/test")).isTrue();
        assertThat(matchesGlob(path("best/abc/test"), "?est/**/test")).isTrue();
        assertThat(matchesGlob(path("test/abc/test/abc/test/abc/test"), "test/**/test/**/test")).isTrue();
        assertThat(matchesGlob(path("test.txt"), "**/test.txt")).isTrue();
        assertThat(matchesGlob(path("test.txt"), "**/*.txt")).isTrue();
        assertThat(matchesGlob(path("test.txt"), "**/test.*")).isTrue();
        assertThat(matchesGlob(path("test.txt"), "**/*.*")).isTrue();
        assertThat(matchesGlob(path("a/b/test.txt"), "a/**/*.txt")).isTrue();
        assertThat(matchesGlob(path("a/b/test.txt"), "a/**/test.*")).isTrue();
        assertThat(matchesGlob(path("a/b/test.txt"), "a/**/*.*")).isTrue();
        assertThat(matchesGlob(path("a-test/a-test/test.txt"), "**/*-test/*-test/test.txt")).isTrue();
        assertThat(matchesGlob(path("a-test/test.txt"), "**/*-test/*-test/test.txt")).isFalse();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/3758")
    void eitherOr() {
        // matches with {}'s, used in for instance `"**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}"`
        assertThat(matchesGlob(path("test/"), "test/{foo,bar}")).isFalse();
        assertThat(matchesGlob(path("test/quz"), "test/{foo,bar}")).isFalse();
        assertThat(matchesGlob(path("test/foo"), "test/{foo,bar}")).isTrue();
        assertThat(matchesGlob(path("test/foo"), "test/{f*,bar}")).isTrue();
        assertThat(matchesGlob(path("test/bar"), "test/{foo,bar}")).isTrue();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3847")
    void negation() {
        assertThat(matchesGlob(path("test.txt"), "test.!(txt)")).isFalse();
        assertThat(matchesGlob(path("test.java"), "test.!(txt)")).isTrue();
        assertThat(matchesGlob(path("test/foo"), "test/!(foo|bar)")).isFalse();
        assertThat(matchesGlob(path("test/bar"), "test/!(foo|bar)")).isFalse();
        assertThat(matchesGlob(path("test/quz"), "test/!(foo|bar)")).isTrue();
        assertThat(matchesGlob(path("test/bar"), "test/!(f*|b*)")).isFalse();
    }

    @Test
    void combinedTest() {
        assertThat(matchesGlob(path("a/b/java"), "{a,b}/!(c)/java")).isTrue();
        assertThat(matchesGlob(path("a/c/java"), "{a,b}/!(c)/java")).isFalse();
        assertThat(matchesGlob(path("b/d/java"), "{a,b}/!(c)/java")).isTrue();
        assertThat(matchesGlob(path("b/c/java"), "{a,b}/!(c)/java")).isFalse();
        assertThat(matchesGlob(path("a/b/java"), "{a,b}/!(c)/java")).isTrue();
        assertThat(matchesGlob(path("a/java"), "!(a|b|c)/{java,txt}")).isFalse();
        assertThat(matchesGlob(path("b/java"), "!(a|b|c)/{java,txt}")).isFalse();
        assertThat(matchesGlob(path("c/java"), "!(a|b|c)/{java,txt}")).isFalse();
        assertThat(matchesGlob(path("d/java"), "!(a|b|c)/{java,txt}")).isTrue();
        assertThat(matchesGlob(path("d/txt"), "!(a|b|c)/{java,txt}")).isTrue();
        assertThat(matchesGlob(path("d/xml"), "!(a|b|c)/{java,txt}")).isFalse();
    }

    private static Path path(String path) {
        return Paths.get(path);
    }
}
