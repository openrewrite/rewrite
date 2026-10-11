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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The release a project runs and the lockfile migration rules, against Yarn's own runs: {@code yarn --version} under
 * corepack 0.35.0, the locks Yarn 4.14.0 and 4.18.1 write for an empty project, and the recorded migrations.
 */
class YarnBerryLockRegeneratorTest {

    private static final String YARN_PATH = "yarnPath: .yarn/releases/yarn-4.5.3.cjs\n";
    private static final Pattern LOCKFILE_VERSION = Pattern.compile("^__metadata:\\n {2}version: (\\d+)$",
            Pattern.MULTILINE);

    @Test
    void yarnPathWinsOverPackageManager() {
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.emptyMap())).isEqualTo("4.5.3");
        assertThat(release(null, YARN_PATH, Collections.emptyMap())).isEqualTo("4.5.3");
        assertThat(release("yarn@4.10.3", null, Collections.singletonMap("YARN_YARN_PATH",
                ".yarn/releases/yarn-4.5.3.cjs"))).isEqualTo("4.5.3");
    }

    @Test
    void ignorePathLeavesItToPackageManager() {
        assertThat(release("yarn@4.10.3", YARN_PATH + "ignorePath: true\n", Collections.emptyMap()))
                .isEqualTo("4.10.3");
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.singletonMap("YARN_IGNORE_PATH", "1")))
                .isEqualTo("4.10.3");
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.singletonMap("YARN_IGNORE_PATH", "0")))
                .isEqualTo("4.5.3");
        assertThat(release("yarn@4.10.3", null, Collections.emptyMap())).isEqualTo("4.10.3");
    }

    @Test
    void aReleaseFileWithoutAVersionIsUnknown() {
        assertThatThrownBy(() -> release("yarn@4.10.3", "yarnPath: .yarn/releases/yarn.cjs\n",
                Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining(".yarn/releases/yarn.cjs");
        assertThatThrownBy(() -> release(null, null, Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the Yarn release that runs");
    }

    /**
     * A release after 4.10.3 runs as it, with its own data and lockfile version (recorded: 8 through 4.13.0, 9 from
     * 4.14.0, 10 from 4.15.0); a release the data lacks is unknown.
     */
    @Test
    void aLaterReleaseRunsWithItsDataAndLockfileVersion() {
        for (String[] release : new String[][]{{"4.10.3", "8"}, {"4.11.0", "8"}, {"4.13.0", "8"}, {"4.14.0", "9"},
                {"4.14.1", "9"}, {"4.15.0", "10"}, {"4.18.1", "10"}}) {
            assertThat(release("yarn@" + release[0], null, Collections.emptyMap())).isEqualTo(release[0]);
            assertThat(requireNonNull(YarnRelease.of(release[0])).getLockfileVersion()).as(release[0])
                    .isEqualTo(Integer.parseInt(release[1]));
        }
        assertThat(release("yarn@4.10.3", "yarnPath: .yarn/releases/yarn-4.14.0.cjs\n", Collections.emptyMap()))
                .isEqualTo("4.14.0");
        assertThatThrownBy(() -> release("yarn@4.19.0", null, Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the data of Yarn 4.19.0");
        assertThat(release("yarn@4.0.0", null, Collections.emptyMap())).isEqualTo("4.0.0");
    }

    /**
     * corepack's findProjectSpec refuses a project naming another package manager, yarnPath or not
     * ({@code open-corepack-member-names-pnpm}); a version it accepts runs as semver parses it, cached or not.
     */
    @Test
    void corepackDecidesBeforeTheReleaseIsRead() {
        assertThatThrownBy(() -> release("pnpm@9.15.0", YARN_PATH, Collections.emptyMap()))
                .isInstanceOf(Rejected.class)
                .hasMessage("This project is configured to use pnpm because package.json has a \"packageManager\" " +
                            "field (corepack)");
        for (String spec : new String[]{"yarn@v4.10.3", "yarn@4.10.3 "}) {
            assertThat(release(spec, null, Collections.emptyMap())).as(spec).isEqualTo("4.10.3");
        }
        assertThatThrownBy(() -> release("pnpm@9.15.0", null, Collections.singletonMap(
                "COREPACK_ENABLE_PROJECT_SPEC", "0")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the Yarn release that runs");
        String url = "https://repo.yarnpkg.com/4.10.3/packages/yarnpkg-cli/bin/yarn.js";
        assertThatThrownBy(() -> release("yarn@" + url, null, Collections.emptyMap()))
                .isInstanceOf(Rejected.class)
                .hasMessageStartingWith("Illegal use of URL for known package manager.");
        assertThatThrownBy(() -> release("yarn@" + url, null, Collections.singletonMap(
                "COREPACK_ENABLE_UNSAFE_CUSTOM_URLS", "1")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the Yarn release at " + url + " (corepack downloads it)");
        assertThatThrownBy(() -> new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"p\", \"packageManager\": \"yarn@" + url + "\"}")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", ".corepack.env")))
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY).release())
                .isInstanceOf(CannotKnow.class)
                .hasMessage("the engine cannot know .corepack.env (its contents were not read)");
    }

    /**
     * install.ts runs Configuration.find, whose plugin pass rejects a setting the release does not know, before
     * autofixMergeConflicts reads the lock.
     */
    @Test
    void anUnknownSettingIsRejectedBeforeTheLocksMergeConflict() {
        String conflicted = "<<<<<<< HEAD\n=======\n>>>>>>> other\n";
        YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder()
                .lock(conflicted)
                .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"}")
                .now(Instant.EPOCH);

        Failure unknown = new YarnBerryLockRegenerator(inputs.configFile(".yarnrc.yml", "unknownSetting: true\n")
                .build(), BerryScenarios.NO_REGISTRY).regenerate().getFailure();
        Failure conflict = new YarnBerryLockRegenerator(inputs.clearConfigFiles().build(),
                BerryScenarios.NO_REGISTRY).regenerate().getFailure();

        assertThat(requireNonNull(unknown).getKind()).isEqualTo(Kind.PM_REJECTS);
        assertThat(unknown.getDetail()).contains("Usage Error: Unrecognized or legacy configuration settings found: " +
                                                 "unknownSetting");
        assertThat(requireNonNull(conflict).getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(conflict.getDetail()).contains("merge conflict");
    }

    /**
     * Of two problems, Yarn 4.10.3 reports the one Configuration.find meets first: the core settings, then (after the
     * third-party plugins run) the plugins' settings, then an unknown key; recorded over an empty lock.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "unknownSetting: true\\ncompressionLevel: nope|Internal Error: Invalid value, expected one of mixed, 0, " +
            "1, 2, 3, 4, 5, 6, 7, 8, 9 in .yarnrc.yml",
            "npmAlwaysAuth: nope\\ncompressionLevel: nope|Internal Error: Invalid value, expected one of mixed, 0, " +
            "1, 2, 3, 4, 5, 6, 7, 8, 9 in .yarnrc.yml",
            "unknownSetting: true\\nnpmScopes:\\n  foo:\\n    npmRegistryServr: https://x.example|Usage Error: " +
            "Unrecognized configuration settings found: npmScopes['foo'].npmRegistryServr",
            "supportedArchitectures:\\n  osx: [darwin]\\nunknownSetting: true|Usage Error: Unrecognized " +
            "configuration settings found: supportedArchitectures.osx",
            "npmScopes:\\n  foo:\\n    npmRegistryServr: https://x.example\\n" +
            "supportedArchitectures:\\n  osx: [darwin]|Usage Error: Unrecognized configuration settings found: " +
            "supportedArchitectures.osx"
    })
    void reportsTheProblemConfigurationFindMeetsFirst(String yarnrc, String reported) {
        Failure failure = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock("")
                .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"}")
                .configFile(".yarnrc.yml", yarnrc.replace("\\n", "\n") + "\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY).regenerate().getFailure();

        assertThat(requireNonNull(failure).getDetail()).contains(reported);
    }

    /**
     * Yarn 4.10.3 runs a third-party plugin before it reads the plugins' settings: beside a scope's unknown key it
     * reports the key, the plugin's own error or "Cannot redefine settings", as the plugin's code decides.
     */
    @Test
    void aThirdPartyPluginRunsBeforeThePluginsSettingsAreRead() {
        Failure failure = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock("")
                .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"}")
                .configFile(".yarnrc.yml", "npmScopes:\n  foo:\n    npmRegistryServr: https://x.example\n" +
                                           "plugins:\n  - .yarn/plugins/plugin-a.cjs\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY).regenerate().getFailure();

        assertThat(requireNonNull(failure).getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(failure.getDetail()).contains("what the plugin .yarn/plugins/plugin-a.cjs (.yarnrc.yml) does");
    }

    /**
     * An unknown key in the home directory's .yarnrc.yml is set aside, strict or not (Yarn 4.10.3 installs over it).
     */
    @Test
    void anUnknownKeyOfTheHomeRcFileIsSetAside() {
        Configuration options = Configuration.load(Collections.singletonMap(Configuration.HOME + ".yarnrc.yml",
                "unknownSetting: true\n"), HostEnvironment.of(Collections.emptyMap()));
        options.restrictTo(requireNonNull(YarnRelease.of("4.10.3")));
        assertThatThrownBy(() -> Configuration.load(Collections.singletonMap(".yarnrc.yml", "unknownSetting: true\n"),
                HostEnvironment.of(Collections.emptyMap())).restrictTo(requireNonNull(YarnRelease.of("4.10.3"))))
                .isInstanceOf(Configuration.UsageError.class);
    }

    /**
     * Yarn 4.10.3 and 4.5.3 over the locks 4.14.0 and 4.18.1 wrote for an empty project: version 8 again.
     */
    @Test
    void theRunningReleaseWritesItsOwnVersionOverALaterOnesLock() {
        String header = "# This file is generated by running \"yarn install\" inside your project.\n" +
                        "# Manual changes might be lost - proceed with caution!\n\n__metadata:\n  version: ";
        String rest = "\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n" +
                      "  resolution: \"p@workspace:.\"\n  languageName: unknown\n  linkType: soft\n";
        for (String version : new String[]{"9", "10"}) {
            YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(
                    YarnBerryLockRegenerator.Inputs.builder()
                            .lock(header + version + rest)
                            .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.10.3\"}\n")
                            .now(Instant.EPOCH)
                            .build(), BerryScenarios.NO_REGISTRY);
            Project project = regenerator.load(regenerator.release(), Collections.emptyMap()).getProject();
            assertThat(project.lockfileNeedsRefresh).isFalse();
            assertThat(regenerator.write(project)).isEqualTo(header + "8" + rest);
        }
    }

    /**
     * install.ts's lockfile migration rules over every lock older than version 8 a recorded phase starts from: the
     * settings they migrate are the {@code .yarnrc.yml} Yarn wrote at the end of that phase.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("migrations")
    void migratesTheSettingsYarnWrote(BerryScenarios.Lock lock) {
        BerryScenarios.Scenario scenario = lock.getScenario();
        boolean seed = "lock-seed".equals(lock.getFileName());
        Phase phase = seed ? Phase.BEFORE :
                scenario.has("refresh") ? Phase.REFRESH : Phase.AFTER;
        String written = scenario.file(seed ? "yarnrc-before.written" :
                phase == Phase.REFRESH ? "yarnrc-refresh.written" : "yarnrc.written");
        YarnBerryLockRegenerator.Inputs recorded = scenario.inputs(phase, lock.getText());
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock(recorded.getLock())
                .manifests(recorded.getManifests())
                .env(recorded.getEnv())
                .host(recorded.getHost())
                .now(recorded.getNow())
                .build(), BerryScenarios.NO_REGISTRY);
        Map<String, Object> migrated = regenerator.load(regenerator.release(), Collections.emptyMap())
                .getMigratedSettings();
        assertThat(migrated).isNotEmpty();
        assertThat(Syml.stringifySyml(new LinkedHashMap<>(migrated))).isEqualTo(written);
    }

    static Stream<BerryScenarios.Lock> migrations() {
        List<BerryScenarios.Lock> locks = new ArrayList<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            if (scenario.has("yarnrc") || !scenario.has("recorded-with")) {
                continue;
            }
            for (BerryScenarios.Lock lock : scenario.locks()) {
                Matcher version = LOCKFILE_VERSION.matcher(lock.getText());
                boolean older = lock.getText().contains("# yarn lockfile v1") ||
                                version.find() && Integer.parseInt(version.group(1)) < 8;
                if (older && ("lock-seed".equals(lock.getFileName()) ||
                              "before".equals(lock.getFileName()) && !scenario.has("lock-seed"))) {
                    locks.add(lock);
                }
            }
        }
        assertThat(locks).isNotEmpty();
        return locks.stream();
    }

    /**
     * install.ts's migration rules are the running release's: 4.15.0 over a version 8 lock (4.10.3 wrote it) writes
     * approvedGitRepositories, enableScripts and npmMinimalAgeGate to .yarnrc.yml, and compressionLevel and
     * enableGlobalCache only below version 8. Recorded: the .yarnrc.yml 4.15.0 wrote beside {@code nodeLinker}.
     */
    @Test
    void migratesALockWithTheRulesOfTheReleaseThatRuns() {
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock(LOCK_HEADER + "8\n  cacheKey: 10c0\n\n\"r@workspace:.\":\n  version: 0.0.0-use.local\n" +
                      "  resolution: \"r@workspace:.\"\n  languageName: unknown\n  linkType: soft\n")
                .manifest("", "{\"name\":\"r\",\"packageManager\":\"yarn@4.15.0\"}\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY);
        Map<String, Object> migrated = regenerator.load(regenerator.release(), Collections.emptyMap())
                .getMigratedSettings();
        assertThat(Syml.stringifySyml(new LinkedHashMap<>(migrated)))
                .isEqualTo("approvedGitRepositories:\n  - \"**\"\n\nenableScripts: true\n\nnpmMinimalAgeGate: 0\n");
    }

    /**
     * Yarn 4.15.0 holds a version back that was published under a day ago ({@code npmMinimalAgeGate} defaults to
     * {@code "1d"} from 4.15.0). Recorded with 4.15.0 an hour after q-fresh@1.1.0 was published: it locks 1.0.0.
     */
    @Test
    void appliesTheDefaultAgeGateOfTheReleaseThatRuns() {
        assertThat(addFreshDependency("4.15.0", "10", "")).isEqualTo(lockWithFresh("10"));
    }

    /**
     * From 4.14.0 {@code npmMinimalAgeGate} is a duration. Recorded with 4.14.0 and {@code "2h"}: it locks 1.0.0.
     */
    @Test
    void readsTheAgeGateAsADurationFrom4_14() {
        assertThat(addFreshDependency("4.14.0", "9", "npmMinimalAgeGate: \"2h\"\n")).isEqualTo(lockWithFresh("9"));
    }

    private static final String LOCK_HEADER = "# This file is generated by running \"yarn install\" inside your " +
                                              "project.\n# Manual changes might be lost - proceed with caution!\n\n" +
                                              "__metadata:\n  version: ";
    private static final String Q_PLAIN = "\"q-plain@npm:1.0.0\":\n  version: 1.0.0\n  resolution: " +
                                          "\"q-plain@npm:1.0.0\"\n  checksum: 10c0/dbfa55e9af5161c08e9db9359e9cd8dbd7" +
                                          "034941cada9da38a5b3f431ed1cc518911501d680e81f34213efcf697646f81fa74d3dd978" +
                                          "eab1d67ef7987f392825\n  languageName: node\n  linkType: hard\n\n";
    private static final String FRESH_PUBLISHED = "2026-10-11T16:06:44.000Z";

    private static String lockWithFresh(String version) {
        return LOCK_HEADER + version + "\n  cacheKey: 10c0\n\n\"q-fresh@npm:^1.0.0\":\n  version: 1.0.0\n" +
               "  resolution: \"q-fresh@npm:1.0.0\"\n  checksum: 10c0/4ea0f667b4b874d9928c02a6f68c8ecf852c67573db95a8635e" +
               "395ba825fd9a7d65bf5acf194d035455fb17ced2a85681b3569122b83345806bd632349668be5\n  languageName: node\n" +
               "  linkType: hard\n\n" + Q_PLAIN + "\"r@workspace:.\":\n  version: 0.0.0-use.local\n" +
               "  resolution: \"r@workspace:.\"\n  dependencies:\n    q-fresh: \"npm:^1.0.0\"\n" +
               "    q-plain: \"npm:1.0.0\"\n  languageName: unknown\n  linkType: soft\n";
    }

    private static @Nullable String addFreshDependency(String release, String lockfileVersion, String yarnrc) {
        String before = LOCK_HEADER + lockfileVersion + "\n  cacheKey: 10c0\n\n" + Q_PLAIN +
                        "\"r@workspace:.\":\n  version: 0.0.0-use.local\n  resolution: \"r@workspace:.\"\n" +
                        "  dependencies:\n    q-plain: \"npm:1.0.0\"\n  languageName: unknown\n  linkType: soft\n";
        Map<String, String> tarballs = new LinkedHashMap<>();
        tarballs.put("1.0.0", "H4sIAAAAAAACE+3TuwrCMBiG4cxeRcisbVJjC4IXUzSee7BpRRDv3VaduriooL7P8sGfIdNbpvNdunJhed9g64tc" +
                              "vJhuxdbettVfrW0ijJ1ENkm07e7GxGMjpBYf0Pg6raQUf+qs8jRzairVYbSsnF+roVRHV/lNkXdXE+hAq4vA" +
                              "b3p0H27yhTu19b/jj6f9R6bff/dM/x+QFYtm7wJ3Kouq9jMzIAkAAAAAAAAAAAAAAICvcgUyIL0fACgAAA==");
        tarballs.put("1.1.0", "H4sIAAAAAAACE+3TvQ6CMBhG4c5eRdNZodUqiYkXQ6Qq/gBSMCTGexfUycVFSdTzLG/ydeh0ini5i9cuLO4bbH2e" +
                              "iTfTrZm1t209r9Y2EsZOxzaKtO3uxswmRkgtelD7Ki6lFH/qrLL44NRcquNoVTq/UUOpTq70aZ51VxOYQKuL" +
                              "wG96dB+mWeKatv5P/PGy/7F57r97pv8eHPKk3rvANUVeVn5hBiQBAAAAAAAAAAAAAADwVa4gB032ACgAAA==");
        String packument = "{\"name\":\"q-fresh\",\"dist-tags\":{\"latest\":\"1.1.0\"},\"versions\":{" +
                           "\"1.0.0\":{\"name\":\"q-fresh\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":" +
                           "\"https://registry.yarnpkg.com/q-fresh/-/q-fresh-1.0.0.tgz\"}}," +
                           "\"1.1.0\":{\"name\":\"q-fresh\",\"version\":\"1.1.0\",\"dist\":{\"tarball\":" +
                           "\"https://registry.yarnpkg.com/q-fresh/-/q-fresh-1.1.0.tgz\"}}}," +
                           "\"time\":{\"created\":\"2020-01-01T00:00:00.000Z\",\"modified\":\"" + FRESH_PUBLISHED +
                           "\",\"1.0.0\":\"2020-01-01T00:00:00.000Z\",\"1.1.0\":\"" + FRESH_PUBLISHED + "\"}}";
        RegistrySource registry = new RegistrySource() {
            @Override
            public @Nullable String packument(String server, String name, boolean full,
                                              @Nullable String authorization) {
                return "q-fresh".equals(name) ? packument : null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version,
                                             @Nullable String authorization) {
                String tgz = "q-fresh".equals(name) ? tarballs.get(version) : null;
                return tgz == null ? null : Base64.getDecoder().decode(tgz);
            }
        };
        Result result = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock(before)
                .manifest("", "{\"name\":\"r\",\"packageManager\":\"yarn@" + release + "\",\"dependencies\":" +
                              "{\"q-plain\":\"1.0.0\",\"q-fresh\":\"^1.0.0\"}}\n")
                .configFile(".yarnrc.yml", "nodeLinker: node-modules\n" + yarnrc)
                .host(Host.builder().nodeVersion("v24.19.0").os("darwin").cpu("arm64").build())
                .now(Instant.parse(FRESH_PUBLISHED).plus(Duration.ofHours(1)))
                .build(), registry).regenerate();
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        return result.getLockFileContent();
    }

    private static String release(@Nullable String packageManager, @Nullable String yarnrc, Map<String, String> env) {
        YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder()
                .manifest("", packageManager == null ? "{\"name\": \"p\"}" :
                        "{\"name\": \"p\", \"packageManager\": \"" + packageManager + "\"}")
                .env(env)
                .now(Instant.EPOCH);
        if (yarnrc != null) {
            inputs.configFile(".yarnrc.yml", yarnrc);
        }
        return new YarnBerryLockRegenerator(inputs.build(), BerryScenarios.NO_REGISTRY).release().getVersion();
    }
}
