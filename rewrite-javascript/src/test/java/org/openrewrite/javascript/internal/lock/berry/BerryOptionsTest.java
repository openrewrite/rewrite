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

import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
        BerryOptions options = BerryOptions.load(files, HostEnvironment.of(Collections.emptyMap()));
        assertThat(options.get("compressionLevel")).isEqualTo(3);
        assertThat(options.get("enableTransparentWorkspaces")).isEqualTo(false);
        assertThat(BerryOptions.load(files,
                HostEnvironment.of(Collections.singletonMap("YARN_COMPRESSION_LEVEL", "mixed")))
                .get("compressionLevel")).isEqualTo("mixed");
    }

    // Configuration.findRcFiles walks from where Yarn runs: a member's own rc file is the nearest
    @Test
    void theRcFileWhereYarnRunsIsNearerThanTheProjects() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../.yarnrc.yml", "compressionLevel: 5\nenableTransparentWorkspaces: false\n");
        files.put(".yarnrc.yml", "compressionLevel: 3\ndefaultProtocol: \"jsr:\"\n");
        files.put("packages/.yarnrc.yml", "compressionLevel: 2\n");
        files.put("packages/a/.yarnrc.yml", "compressionLevel: 1\n");
        BerryOptions options = BerryOptions.load(files, HostEnvironment.of(Collections.emptyMap()));
        assertThat(options.get("compressionLevel")).isEqualTo(1);
        assertThat(options.get("defaultProtocol")).isEqualTo("jsr:");
        assertThat(options.get("enableTransparentWorkspaces")).isEqualTo(false);
    }

    // open-member-yarnrc-chain-run-in-member: Yarn 4.10.3 run in packages/a reads no sibling's rc file, nor one below
    @Test
    void findRcFilesKeepsTheDirectoryYarnRunsInAndThoseAboveIt() {
        Map<String, String> files = new LinkedHashMap<>();
        for (String path : new String[]{"../.yarnrc.yml", "../x/.yarnrc.yml", ".yarnrc.yml", "packages/.yarnrc.yml",
          "packages/a/.yarnrc.yml", "packages/b/.yarnrc.yml", "packages/a/b/.yarnrc.yml"}) {
            files.put(path, "");
        }
        assertThat(BerryOptions.findRcFiles("packages/a", files)).containsOnlyKeys("../.yarnrc.yml", ".yarnrc.yml",
          "packages/.yarnrc.yml", "packages/a/.yarnrc.yml");
        assertThat(BerryOptions.findRcFiles("", files)).containsOnlyKeys("../.yarnrc.yml", ".yarnrc.yml");
    }

    @Test
    void mapSettingsMergeAcrossFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".yarnrc.yml", "catalog:\n  a: ^1.0.0\n");
        files.put("../.yarnrc.yml", "catalog:\n  a: ^2.0.0\n  b: ^3.0.0\n");
        assertThat(BerryOptions.load(files, HostEnvironment.of(Collections.emptyMap())).getMap("catalog").toString())
                .isEqualTo("{a=^1.0.0, b=^3.0.0}");
    }

    @Test
    void rejectsAValueItsDefinitionDoesNotAllow() {
        // Yarn 4.10.3 prints "Internal Error: Invalid value, expected one of mixed, 0, ..., 9 in <path>/.yarnrc.yml"
        assertThatThrownBy(() -> BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 10\n"),
                HostEnvironment.of(Collections.emptyMap())))
                .isInstanceOf(JsError.class)
                .hasMessageContaining("Invalid value, expected one of mixed, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9");
    }

    @Test
    void replacesEnvironmentVariables() {
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"${REGISTRY:-https://fallback.example}\"\n"),
                        HostEnvironment.of(Collections.emptyMap()));
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://fallback.example");
    }

    @Test
    void anUnknownSettingIsTheReleasesToJudge() {
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"p\", \"packageManager\": \"yarn@4.5.3\"}")
                .configFile(".yarnrc.yml", "catalog:\n  ms: ^2.1.0\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY);
        YarnRelease release = regenerator.release();
        assertThat(release.getVersion()).isEqualTo("4.5.3");
        assertThatThrownBy(() -> regenerator.load(release, Collections.emptyMap()))
                .isInstanceOf(BerryOptions.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: catalog");
        assertThat(regenerator.load(YarnRelease.of("4.10.3"), Collections.emptyMap()).getProject().configuration
                .getRelease().hasPlugin("@yarnpkg/plugin-catalog")).isTrue();
    }

    @Test
    void eachLoadStartsFromTheSettingsTheSourcesSet() {
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock("__metadata:\n  version: 6\n  cacheKey: 8\n")
                .manifest("", "{\"name\": \"p\", \"packageManager\": \"yarn@4.5.3\"}")
                .configFile(".yarnrc.yml", "catalog:\n  ms: ^2.1.0\n")
                .env(Collections.singletonMap("YARN_ENABLE_STRICT_SETTINGS", "false"))
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY);
        YarnBerryLockRegenerator.Loaded first = regenerator.load(YarnRelease.of("4.5.3"), Collections.emptyMap());
        assertThat(first.getProject().configuration.getOptions().getMap("catalog")).isEmpty();
        assertThat(first.getMigratedSettings()).containsOnlyKeys("enableGlobalCache", "compressionLevel");

        YarnBerryLockRegenerator.Loaded second = regenerator.load(YarnRelease.of("4.10.3"), Collections.emptyMap());
        assertThat(second.getProject().configuration.getOptions().getMap("catalog")).containsEntry("ms", "^2.1.0");
        assertThat(second.getMigratedSettings()).isEqualTo(first.getMigratedSettings());
    }

    @Test
    void theReleaseIsNamedByPackageManagerOrByYarnPathsFileName() {
        assertThat(BerryOptions.packageManagerVersion("yarn@4.10.3+sha512.abc")).isEqualTo("4.10.3");
        assertThat(BerryOptions.packageManagerVersion("yarn@^4.10.0")).isNull();
        assertThat(BerryOptions.packageManagerVersion(null)).isNull();
        assertThat(BerryOptions.yarnPathVersion(".yarn/releases/yarn-4.10.3.cjs")).isEqualTo("4.10.3");
        assertThat(BerryOptions.yarnPathVersion(".yarn/releases/yarn.cjs")).isNull();
    }

    /**
     * Yarn 4.10.3 under corepack: an rc file's {@code enableStrictSettings} never
     * reaches the pass that reads unknown keys; the environment's does.
     */
    @Test
    void onlyTheEnvironmentTurnsStrictSettingsOff() {
        Set<String> settings = YarnRelease.of("4.10.3").getSettings();
        assertThatThrownBy(() -> BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "enableStrictSettings: false\nfooUnknownSetting: 1\ncompressionLevel: 3\n"),
                        HostEnvironment.of(Collections.emptyMap()))
                .restrictTo(settings))
                .isInstanceOf(BerryOptions.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: fooUnknownSetting");

        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "fooUnknownSetting: 1\ncompressionLevel: 3\n"),
                HostEnvironment.of(Collections.singletonMap("YARN_ENABLE_STRICT_SETTINGS", "false")));
        options.restrictTo(settings);
        assertThat(options.get("compressionLevel")).isEqualTo(3);

        Map<String, String> env = new LinkedHashMap<>();
        env.put("YARN_ENABLE_STRICT_SETTINGS", "false");
        env.put("YARN_FOO_UNKNOWN", "1");
        BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 3\n"), HostEnvironment.of(env))
                .restrictTo(settings);

        assertThatThrownBy(() -> BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "enableStrictSettings: false\nfooUnknownSetting: 1\n"),
                HostEnvironment.of(Collections.singletonMap("YARN_ENABLE_STRICT_SETTINGS", "true")))
                        .restrictTo(settings))
                .isInstanceOf(BerryOptions.UsageError.class);
    }

    /**
     * A number setting is JavaScript's {@code parseInt}, hexadecimal after {@code 0x}; Yarn 4.10.3's answers.
     */
    @Test
    void readsANumberAsParseIntDoes() {
        String[][] rows = {
                {"\"0x3\"", "3"}, {"\"0X9\"", "9"}, {"\" 0x3\"", "3"}, {"\"+3\"", "3"}, {"\"3.9\"", "3"},
                {"\"0b1\"", "0"}, {"\"1e1\"", "1"}, {"\"4abc\"", "4"}};
        for (String[] row : rows) {
            assertThat(options("compressionLevel: " + row[0] + "\n").get("compressionLevel")).as(row[0])
                    .isEqualTo(Integer.valueOf(row[1]));
        }
        for (String rejected : new String[]{"\"0x\"", "\"-0x3\""}) {
            assertThatThrownBy(() -> options("compressionLevel: " + rejected + "\n")).as(rejected)
                    .isInstanceOf(JsError.class)
                    .hasMessageContaining("Invalid value, expected one of mixed, 0, 1");
        }
        assertThat(BerryOptions.load(Collections.emptyMap(),
                HostEnvironment.of(Collections.singletonMap("YARN_COMPRESSION_LEVEL", "0x5")))
                .get("compressionLevel")).isEqualTo(5);
        assertThat(BerryOptions.parseInt("\u3000\u2028-12")).isEqualTo(-12);
        assertThat(BerryOptions.parseInt("99999999999")).isEqualTo(99999999999d);
    }

    private static BerryOptions options(String yarnrc) {
        return BerryOptions.load(Collections.singletonMap(".yarnrc.yml", yarnrc),
                HostEnvironment.of(Collections.emptyMap()));
    }
}
