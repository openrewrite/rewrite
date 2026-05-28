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

class YarnClassicLockAdapterTest {

    @Test
    void convertsSingleTopLevelEntry() {
        String yarn = "# yarn lockfile v1\n" +
                "\n\n" +
                "lodash@^4.17.21:\n" +
                "  version \"4.17.21\"\n" +
                "  resolved \"https://registry.yarnpkg.com/lodash/-/lodash-4.17.21.tgz\"\n" +
                "  integrity sha512-x\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
    }

    @Test
    void convertsMultiKeyBlock() {
        // Two constraints resolving to the same version share a single block.
        String yarn = "lodash@^4.17.21, lodash@^4.17.0:\n" +
                "  version \"4.17.21\"\n" +
                "  resolved \"...\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
    }

    @Test
    void convertsScopedPackage() {
        String yarn = "\"@types/node@^20.0.0\":\n" +
                "  version \"20.0.0\"\n" +
                "  resolved \"...\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("@types/node");
        assertThat(result.getTopLevel()).containsKey("@types/node");
    }

    @Test
    void extractsTransitiveDependencies() {
        String yarn = "express@^4.18.0:\n" +
                "  version \"4.18.0\"\n" +
                "  resolved \"...\"\n" +
                "  dependencies:\n" +
                "    accepts \"^1.3.8\"\n" +
                "    body-parser \"^1.20.0\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        var express = result.getAll().get(0);
        assertThat(express.getDependencies())
                .extracting(d -> d.getName() + "@" + d.getVersionConstraint())
                .containsExactlyInAnyOrder("accepts@^1.3.8", "body-parser@^1.20.0");
    }

    @Test
    void extractsTransitiveDependencyWithScopedName() {
        // Scoped names in transitive deps require quoted keys in yarn.lock.
        String yarn = "express@^4.18.0:\n" +
                "  version \"4.18.0\"\n" +
                "  dependencies:\n" +
                "    \"@types/cookie\" \"^0.5.0\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        var express = result.getAll().get(0);
        assertThat(express.getDependencies())
                .extracting(d -> d.getName() + "@" + d.getVersionConstraint())
                .containsExactly("@types/cookie@^0.5.0");
    }

    @Test
    void preservesMultipleVersionsOfSameName() {
        // Real yarn.lock when a transitive dep needs an older version: two blocks
        // with different versions of the same package. First-wins gets top-level.
        String yarn = "lodash@^4.17.21:\n" +
                "  version \"4.17.21\"\n" +
                "\n" +
                "lodash@^3.10.0:\n" +
                "  version \"3.10.1\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        // Both versions in `all`...
        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("lodash@4.17.21", "lodash@3.10.1");
        // ...but only the first-seen version is top-level.
        assertThat(result.getTopLevel()).containsOnlyKeys("lodash");
        assertThat(result.getTopLevel().get("lodash").getVersion()).isEqualTo("4.17.21");
    }

    @Test
    void handlesConstraintsWithSpaces() {
        // ">=1.0 <2.0" is a valid yarn constraint; it's quoted in the key.
        String yarn = "\"foo@>=1.0 <2.0\":\n" +
                "  version \"1.5.0\"\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("foo");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("1.5.0");
    }

    @Test
    void skipsHeaderCommentsAndBlankLines() {
        String yarn = "# THIS IS AN AUTOGENERATED FILE.\n" +
                "# yarn lockfile v1\n" +
                "\n\n" +
                "# Yet another comment\n" +
                "lodash@^4.17.21:\n" +
                "  version \"4.17.21\"\n" +
                "\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
    }

    @Test
    void emptyLockfileProducesNoEntries() {
        String yarn = "# yarn lockfile v1\n\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).isEmpty();
        assertThat(result.getTopLevel()).isEmpty();
    }

    @Test
    void toleratesWindowsLineEndings() {
        String yarn = "# yarn lockfile v1\r\n" +
                "\r\n" +
                "lodash@^4.17.21:\r\n" +
                "  version \"4.17.21\"\r\n" +
                "  resolved \"...\"\r\n";
        String npm = YarnClassicLockAdapter.toNpmV3(yarn);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
    }
}
