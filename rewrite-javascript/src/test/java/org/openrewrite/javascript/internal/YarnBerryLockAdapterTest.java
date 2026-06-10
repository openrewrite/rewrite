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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YarnBerryLockAdapterTest {

    @Test
    void convertsSingleTopLevelEntry() {
        String yarn = "__metadata:\n" +
                "  version: 6\n" +
                "  cacheKey: 8c0\n" +
                "\n" +
                "\"lodash@npm:^4.17.21\":\n" +
                "  version: 4.17.21\n" +
                "  resolution: \"lodash@npm:4.17.21\"\n" +
                "  checksum: sha512-x\n" +
                "  languageName: node\n" +
                "  linkType: hard\n";
        String npm = YarnBerryLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
    }

    @Test
    void convertsScopedPackage() {
        String yarn = "\"@types/node@npm:^20.0.0\":\n" +
                "  version: 20.0.0\n" +
                "  resolution: \"@types/node@npm:20.0.0\"\n";
        String npm = YarnBerryLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("@types/node");
        assertThat(result.getTopLevel()).containsKey("@types/node");
    }

    @Test
    void extractsTransitiveDependenciesAndStripsProtocol() {
        String yarn = "\"express@npm:^4.18.0\":\n" +
                "  version: 4.18.0\n" +
                "  resolution: \"express@npm:4.18.0\"\n" +
                "  dependencies:\n" +
                "    accepts: \"npm:^1.3.8\"\n" +
                "    body-parser: \"npm:^1.20.0\"\n";
        String npm = YarnBerryLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        var express = result.getAll().get(0);
        assertThat(express.getDependencies())
                .extracting(d -> d.getName() + "@" + d.getVersionConstraint())
                .containsExactlyInAnyOrder("accepts@^1.3.8", "body-parser@^1.20.0");
    }

    @Test
    void preservesMultipleVersionsOfSameName() {
        String yarn = "__metadata:\n" +
                "  version: 6\n" +
                "\n" +
                "\"lodash@npm:^4.17.21\":\n" +
                "  version: 4.17.21\n" +
                "  resolution: \"lodash@npm:4.17.21\"\n" +
                "\n" +
                "\"lodash@npm:^3.10.0\":\n" +
                "  version: 3.10.1\n" +
                "  resolution: \"lodash@npm:3.10.1\"\n";
        String npm = YarnBerryLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("lodash@4.17.21", "lodash@3.10.1");
        assertThat(result.getTopLevel()).containsOnlyKeys("lodash");
    }

    @Test
    void skipsMetadataBlock() {
        String yarn = "__metadata:\n" +
                "  version: 6\n" +
                "  cacheKey: 8c0\n";
        String npm = YarnBerryLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).isEmpty();
    }

    @Test
    void throwsOnMalformedYaml() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> YarnBerryLockAdapter.toNpmV3("not: valid: yaml: content: here:"))
                .isInstanceOf(RuntimeException.class);
    }
}
