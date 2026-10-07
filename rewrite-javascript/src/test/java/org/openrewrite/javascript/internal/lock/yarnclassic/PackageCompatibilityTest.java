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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/package-compatibility.js. Its case for yarn's prerelease semantics is not ported: the
 * engine's yarn is 1.22.22, never a prerelease, for which satisfiesWithPrereleases answers as satisfies does.
 */
class PackageCompatibilityTest {

    @ParameterizedTest(name = "{1} on node {2}, loose {3}")
    @CsvSource({
            "node, ^5.0.0, 5.1.0, true, true",
            "node, ^5.0.0, 5.01.0, true, true",
            "node, ^5.0.0, 5.01.0, false, false",
            "node, ^0.13.0, 5.0.0, true, true",
            "node, ^0.12.0, 5.0.0, true, true",
            "node, ^0.11.0, 5.0.0, true, true",
            "node, ^0.10.0, 5.0.0, true, true",
            "node, ^0.9.0, 5.0.0, true, false",
            "node, ^0.12.0, 0.12.0, true, true",
            "node, ^0.12.0, 0.11.0, true, false",
            "node, ^1.3.0, 1.4.1-20180208.2355, true, false",
    })
    void nodeSemverSemantics(String name, String range, String actual, boolean loose, boolean satisfied) {
        assertThat(PackageCompatibility.testEngine(name, range, actual, loose)).isEqualTo(satisfied);
    }

    @Test
    void shouldCheckAnOsOrCpuRequirementUnlessIgnorePlatform() {
        assertThat(PackageCompatibility.shouldCheck(manifest("os", Collections.singletonList("darwin")), false,
                false)).isTrue();
        assertThat(PackageCompatibility.shouldCheck(manifest("cpu", Collections.singletonList("i32")), false,
                false)).isTrue();
        assertThat(PackageCompatibility.shouldCheck(new Manifest(), false, false)).isFalse();
        Manifest empty = manifest("os", Collections.emptyList());
        empty.put("cpu", Collections.emptyList());
        assertThat(PackageCompatibility.shouldCheck(empty, false, false)).isFalse();
        Manifest both = manifest("cpu", Collections.singletonList("i32"));
        both.put("os", Arrays.asList("darwin"));
        assertThat(PackageCompatibility.shouldCheck(both, false, true)).isFalse();
    }

    @Test
    void shouldCheckEnginesUnlessIgnoreEngines() {
        Map<String, Object> engines = new LinkedHashMap<>();
        engines.put("node", ">= 10");
        assertThat(PackageCompatibility.shouldCheck(manifest("engines", engines), false, false)).isTrue();
        assertThat(PackageCompatibility.shouldCheck(new Manifest(), false, false)).isFalse();
        assertThat(PackageCompatibility.shouldCheck(manifest("engines", engines), true, false)).isFalse();
    }

    private static Manifest manifest(String key, Object value) {
        Manifest manifest = new Manifest();
        manifest.put(key, value);
        return manifest;
    }
}
