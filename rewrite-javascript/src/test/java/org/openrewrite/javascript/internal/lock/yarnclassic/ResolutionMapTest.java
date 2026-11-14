/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/util/parse-package-path.js and __tests__/util/misc.js.
 */
class ResolutionMapTest {

    @Test
    void parsePackagePath() {
        assertThat(ResolutionMap.parsePackagePath("foo/bar/baz")).containsExactly("foo", "bar", "baz");
        assertThat(ResolutionMap.parsePackagePath("@foo/bar/baz")).containsExactly("@foo/bar", "baz");
        assertThat(ResolutionMap.parsePackagePath("@foo/bar/@foo/baz")).containsExactly("@foo/bar", "@foo/baz");
        assertThat(ResolutionMap.parsePackagePath("invalidname/")).containsExactly("invalidname");
    }

    @Test
    void isValidPackagePath() {
        assertThat(ResolutionMap.isValidPackagePath("foo/bar/baz")).isTrue();
        assertThat(ResolutionMap.isValidPackagePath("@foo/bar/baz")).isTrue();
        assertThat(ResolutionMap.isValidPackagePath("@foo/bar/@foo/baz")).isTrue();
        assertThat(ResolutionMap.isValidPackagePath("invalidname/")).isFalse();
        assertThat(ResolutionMap.isValidPackagePath("invalidname//invalidname")).isFalse();
        assertThat(ResolutionMap.isValidPackagePath("invalidname/invalidname/*")).isFalse();
    }

    @Test
    void sortAlpha() {
        List<String> sorted = new ArrayList<>(Arrays.asList("foo@6.x", "foo@^6.5.0", "foo@~6.8.x", "foo@^6.7.0",
                "foo@~6.8.0", "foo@^6.8.0", "foo@6.8.0"));
        sorted.sort(Misc::sortAlpha);
        assertThat(sorted).containsExactly("foo@6.8.0", "foo@6.x", "foo@^6.5.0", "foo@^6.7.0", "foo@^6.8.0",
                "foo@~6.8.0", "foo@~6.8.x");
    }
}
