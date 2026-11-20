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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What each Yarn 4.x release does otherwise than 4.10.3, as {@link YarnRelease} answers it, each case from the
 * release's source at its {@code @yarnpkg/cli/<version>} tag.
 */
class YarnReleaseTest {

    private static YarnRelease release(String version) {
        return requireNonNull(YarnRelease.of(version), version);
    }

    private static Configuration configuration(String version, String yarnrc, Map<String, String> env) {
        YarnRelease release = release(version);
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml", yarnrc),
                HostEnvironment.of(env), release);
        options.restrictTo(release);
        return options;
    }

    @ParameterizedTest(name = "{0}: lockfile {1}")
    @CsvSource({"4.13.0,8", "4.14.0,9", "4.14.1,9", "4.15.0,10", "4.18.1,10"})
    void writesTheLockfileVersionOfItsProject(String version, int lockfileVersion) {
        assertThat(release(version).getLockfileVersion()).isEqualTo(lockfileVersion);
    }

    /**
     * plugin-npm's index.ts: a number of minutes until 4.11.0 ({@code parseInt}, so {@code 7d} is 7), a duration
     * after, which defaults to {@code 1d} from 4.15.0.
     */
    @ParameterizedTest(name = "{0}: npmMinimalAgeGate {1} is {2}")
    @CsvSource({"4.10.3,7d,7", "4.11.0,7d,10080", "4.11.0,1.5h,90", "4.11.0,90,90", "4.14.1,,0", "4.15.0,,1440"})
    void readsTheAgeGateAsItsReleaseDefinesIt(String version, String gate, int minutes) {
        Configuration options = configuration(version, gate == null ? "" : "npmMinimalAgeGate: \"" + gate + "\"\n",
                Collections.emptyMap());
        assertThat(((Number) requireNonNull(options.get("npmMinimalAgeGate"))).doubleValue()).isEqualTo(minutes);
    }

    @Test
    void aDurationItCannotParseIsAnError() {
        assertThatThrownBy(() -> configuration("4.11.0", "npmMinimalAgeGate: soon\n", Collections.emptyMap()))
                .isInstanceOf(JsError.class)
                .hasMessage("Couldn't parse \"soon\" as a duration in .yarnrc.yml");
    }

    /**
     * npmConfigUtils {@code getMinimalAgeGate} (4.17.0): a scope's entry holds the gate, its default included.
     */
    @Test
    void aScopeGatesItsPackagesFrom4170() {
        String yarnrc = "npmMinimalAgeGate: 0\nnpmScopes:\n  acme:\n    npmRegistryServer: https://x.example\n";
        Configuration later = configuration("4.17.0", yarnrc, Collections.emptyMap());
        assertThat(NpmHttpUtils.getMinimalAgeGate(StructUtils.makeIdent("acme", "a"), later)).isEqualTo(1440);
        assertThat(NpmHttpUtils.getMinimalAgeGate(StructUtils.makeIdent(null, "a"), later)).isEqualTo(0);
        Configuration earlier = configuration("4.16.0", yarnrc, Collections.emptyMap());
        assertThat(NpmHttpUtils.getMinimalAgeGate(StructUtils.makeIdent("acme", "a"), earlier)).isEqualTo(0);
        assertThatThrownBy(() -> configuration("4.16.0", "npmScopes:\n  acme:\n    npmMinimalAgeGate: 1d\n",
                Collections.emptyMap()).checkShapeKeys(release("4.16.0")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized configuration settings found: npmScopes['acme']" +
                                        ".npmMinimalAgeGate");
    }

    /**
     * A setting the release does not define is an unknown one: {@code catalog} before 4.10.0.
     */
    @Test
    void aSettingTheReleaseLacksIsUnknown() {
        assertThatThrownBy(() -> configuration("4.9.4", "catalog:\n  ms:\n", Collections.emptyMap()))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: catalog - run \"yarn " +
                                        "config\"");
        assertThatThrownBy(() -> configuration("4.10.0", "catalog:\n  ms:\n", Collections.emptyMap()))
                .isInstanceOf(JsError.class)
                .hasMessageContaining("Non-nullable configuration settings \"catalog['ms']\" cannot be set to null");
    }

    /**
     * Configuration.ts's {@code IGNORED_ENV_VARIABLES}: Yarn 1's {@code YARN_IGNORE_SCRIPTS} from 4.15.0.
     */
    @Test
    void yarnClassicsIgnoreScriptsIsNoSettingFrom4150() {
        Map<String, String> env = Collections.singletonMap("YARN_IGNORE_SCRIPTS", "true");
        assertThatThrownBy(() -> configuration("4.14.1", "", env))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: ignoreScripts");
        configuration("4.15.0", "", env);
    }

    /**
     * miscUtils {@code replaceEnvVariables}: a backslash keeps a reference from 4.11.0; 4.13.0 nests defaults and
     * rejects what it does not read.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
            "4.10.3|\\${HOME}|\\/home",
            "4.11.0|\\${HOME}|${HOME}",
            "4.13.0|\\${HOME}|${HOME}",
            "4.13.0|${MISSING:-${HOME}}|/home",
            "4.13.0|${MISSING-fallback}|fallback",
            "4.13.0|a}b|a}b",
    })
    void substitutesTheEnvironmentAsItsReleaseDoes(String version, String value, String expected) {
        HostEnvironment env = HostEnvironment.of(Collections.singletonMap("HOME", "/home"));
        assertThat(MiscUtils.replaceEnvVariables(value, env, release(version))).isEqualTo(expected);
    }

    @Test
    void anUnreadSubstitutionIsAUsageErrorFrom4130() {
        HostEnvironment env = HostEnvironment.of(Collections.emptyMap());
        assertThatThrownBy(() -> MiscUtils.replaceEnvVariables("${1}", env, release("4.13.0")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessage("Invalid environment variable substitution syntax: ${1}");
        assertThatThrownBy(() -> MiscUtils.replaceEnvVariables("${A:-b", env, release("4.13.0")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessage("Incomplete variable substitution in input: ${A:-b");
    }

    /**
     * gitUtils {@code validateRepoUrl} (4.14.0): the repository as git reads it, against
     * {@code approvedGitRepositories}.
     */
    @Test
    void aGitRepositoryNeedsApprovalFrom4140() {
        Configuration none = configuration("4.14.0", "", Collections.emptyMap());
        assertThatThrownBy(() -> GitResolver.validateRepoUrl("github:jonschlinkert/is-number#7.0.0", none))
                .isInstanceOf(ReportError.class)
                .hasMessage("Request to 'https://github.com/jonschlinkert/is-number.git' has been blocked because it " +
                            "doesn't match any of the patterns in 'approvedGitRepositories'");
        Configuration all = configuration("4.14.0", "approvedGitRepositories:\n  - \"**\"\n",
                Collections.emptyMap());
        assertThat(GitResolver.validateRepoUrl("git+https://github.com/jonschlinkert/is-even.git", all))
                .isEqualTo("https://github.com/jonschlinkert/is-even.git");
        Configuration one = configuration("4.14.0", "approvedGitRepositories:\n  - \"https://github.com/acme/**\"\n",
                Collections.emptyMap());
        assertThat(GitResolver.validateRepoUrl("acme/tools#main", one)).isEqualTo("https://github.com/acme/tools.git");
        assertThatThrownBy(() -> GitResolver.validateRepoUrl("other/tools", one)).isInstanceOf(ReportError.class);
    }

    /**
     * npmHttpUtils {@code getIdentUrl} and NpmSemverFetcher {@code getLocatorUrl} encode from 4.15.0.
     */
    @Test
    void registryPathsAreEncodedFrom4150() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent("types", "node"), "npm:1.0.0+build.1");
        assertThat(NpmHttpUtils.getLocatorUrl(locator, release("4.14.1")))
                .isEqualTo("/@types%2fnode/-/node-1.0.0+build.1.tgz");
        assertThat(NpmHttpUtils.getLocatorUrl(locator, release("4.15.0")))
                .isEqualTo("/@types%2fnode/-/node-1.0.0%2Bbuild.1.tgz");
    }

    /**
     * plugin-compat's builtin patches: the hash= makePatchHash gives a version, which the patch text of 4.17.1 changed
     * for resolve and of several releases for typescript (recorded in the sweep's locks).
     */
    @ParameterizedTest(name = "{0}: {1}@{2} hash={3}")
    @CsvSource({
            "4.17.0,resolve,1.22.8,c3c19d", "4.17.1,resolve,1.22.8,9bd1a5",
            "4.0.0,typescript,5.6.3,29ae49", "4.0.2,typescript,5.6.3,e012d7", "4.2.0,typescript,5.6.3,b45daf",
            "4.14.0,typescript,5.6.3,8c6c40"
    })
    void hashesItsBuiltinPatchAsItsTextDoes(String version, String name, String packageVersion, String hash) {
        assertThat(release(version).compatPatchHash(name, packageVersion)).isEqualTo(hash);
    }

    /**
     * install.ts's {@code LOCKFILE_MIGRATION_RULES} over a version 8 lock: none in 4.13.0, the git and scripts settings
     * from 4.14.0 and the age gate from 4.15.0, written to the project's .yarnrc.yml as stringifySyml writes it.
     */
    @Test
    void migratesTheSettingsOfItsRelease() {
        String lock = "__metadata:\n  version: 8\n  cacheKey: 10c0\n";
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock(lock)
                .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.15.0\"}")
                .configFile(".yarnrc.yml", "enableTelemetry: false\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY);
        assertThat(regenerator.load(release("4.13.0"), Collections.emptyMap()).getMigratedSettings()).isEmpty();
        assertThat(regenerator.load(release("4.14.0"), Collections.emptyMap()).getMigratedSettings())
                .containsOnlyKeys("approvedGitRepositories", "enableScripts");
        Map<String, Object> settings = new LinkedHashMap<>(regenerator.load(release("4.15.0"),
                Collections.emptyMap()).getMigratedSettings());
        assertThat(regenerator.updateConfiguration("enableTelemetry: false\n", settings))
                .isEqualTo("approvedGitRepositories:\n  - \"**\"\n\nenableScripts: true\n\nenableTelemetry: false\n\n" +
                           "npmMinimalAgeGate: 0\n");
    }

    /**
     * The release the NodeRuntime marker names installs, whatever the project pins; one the data does not hold is
     * cannot-know.
     */
    @Test
    void theMarkersReleaseInstalls() {
        YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"p\",\"packageManager\":\"yarn@4.5.3\"}")
                .now(Instant.EPOCH);
        assertThat(new YarnBerryLockRegenerator(inputs.build(), BerryScenarios.NO_REGISTRY).release().getVersion())
                .isEqualTo("4.5.3");
        assertThat(new YarnBerryLockRegenerator(inputs.release("4.15.0").build(), BerryScenarios.NO_REGISTRY)
                .release().getVersion()).isEqualTo("4.15.0");
        assertThatThrownBy(() -> new YarnBerryLockRegenerator(inputs.release("3.8.7").build(),
                BerryScenarios.NO_REGISTRY).release())
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the data of Yarn 3.8.7");
    }

    @Test
    void theProvenReleasesAreRuns() {
        assertThat(YarnReleases.isProven("4.10.3")).isTrue();
        assertThat(YarnReleases.ranges(YarnReleases.proven())).isEqualTo(YarnReleases.PROVEN);
        assertThat(YarnReleases.ranges(Arrays.asList("4.0.0", "4.0.1", "4.10.3"))).isEqualTo("4.0.0 - 4.0.1, 4.10.3");
    }
}
