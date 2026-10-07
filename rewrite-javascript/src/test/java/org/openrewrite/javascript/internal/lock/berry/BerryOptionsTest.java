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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The settings that reach a lock, as Yarn 4.10.3's configuration reads them, and the release a project runs.
 */
class BerryOptionsTest {

    @Test
    void defaults() {
        BerryOptions options = BerryOptions.defaults();
        assertThat(options.get("compressionLevel")).isEqualTo(0);
        assertThat(options.get("defaultProtocol")).isEqualTo("npm:");
        assertThat(options.get("enableTransparentWorkspaces")).isEqualTo(true);
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://registry.yarnpkg.com");
    }

    @Test
    void theEnvironmentWinsOverTheFilesAndTheNearestFileOverTheFarthest() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".yarnrc.yml", "compressionLevel: 3\ndefaultProtocol: \"npm:\"\n");
        files.put("../.yarnrc.yml", "compressionLevel: 5\nenableTransparentWorkspaces: false\n");
        BerryOptions options = BerryOptions.load(files, Collections.emptyMap());
        assertThat(options.get("compressionLevel")).isEqualTo(3);
        assertThat(options.get("enableTransparentWorkspaces")).isEqualTo(false);
        assertThat(BerryOptions.load(files, Collections.singletonMap("YARN_COMPRESSION_LEVEL", "mixed"))
                .get("compressionLevel")).isEqualTo("mixed");
    }

    @Test
    void mapSettingsMergeAcrossFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".yarnrc.yml", "catalog:\n  a: ^1.0.0\n");
        files.put("../.yarnrc.yml", "catalog:\n  a: ^2.0.0\n  b: ^3.0.0\n");
        assertThat(BerryOptions.load(files, Collections.emptyMap()).getMap("catalog").toString())
                .isEqualTo("{a=^1.0.0, b=^3.0.0}");
    }

    @Test
    void rejectsAValueItsDefinitionDoesNotAllow() {
        assertThatThrownBy(() -> BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 10\n"),
                Collections.emptyMap()))
                .isInstanceOf(BerryOptions.UsageError.class)
                .hasMessageContaining("Invalid value, expected one of mixed, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9");
    }

    @Test
    void replacesEnvironmentVariables() {
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"${REGISTRY:-https://fallback.example}\"\n"), Collections.emptyMap());
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://fallback.example");
    }

    @Test
    void anUnknownSettingIsTheReleasesToJudge() {
        BerryLockRegenerator regenerator = new BerryLockRegenerator(BerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"p\", \"packageManager\": \"yarn@4.5.3\"}")
                .configFile(".yarnrc.yml", "catalog:\n  ms: ^2.1.0\n")
                .now(Instant.EPOCH)
                .build());
        YarnRelease release = regenerator.release();
        assertThat(release.getVersion()).isEqualTo("4.5.3");
        assertThatThrownBy(() -> regenerator.load(release))
                .isInstanceOf(BerryOptions.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: catalog");
        assertThat(regenerator.load(YarnRelease.of("4.10.3")).getProject().configuration.getRelease().isCatalogs())
                .isTrue();
    }

    @Test
    void theReleaseComesFromPackageManagerElseYarnPath() {
        assertThat(BerryOptions.yarnVersion("yarn@4.10.3+sha512.abc", null)).isEqualTo("4.10.3");
        assertThat(BerryOptions.yarnVersion(null, ".yarn/releases/yarn-4.10.3.cjs")).isEqualTo("4.10.3");
        assertThat(BerryOptions.yarnVersion("yarn@^4.10.0", null)).isNull();
        assertThat(BerryOptions.yarnVersion(null, null)).isNull();
        assertThatThrownBy(() -> new BerryLockRegenerator(BerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"p\"}").now(Instant.EPOCH).build()).release())
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the Yarn release that runs");
    }
}
