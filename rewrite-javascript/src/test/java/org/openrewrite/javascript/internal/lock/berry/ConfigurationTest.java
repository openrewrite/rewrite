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
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/yarnpkg-core/tests/Configuration.test.ts, the cases about settings that reach a lock; secrets hiding
 * and {@code overwrite} merging reach none and are not ported.
 */
class ConfigurationTest {

    @Test
    void tagRegexpAllowsAllValidTags() {
        for (String tag : new String[]{"canary", "latest", "next", "legacy_v1"}) {
            assertThat(Configuration.TAG_REGEXP.matcher(tag).find()).as(tag).isTrue();
        }
    }

    @Test
    void replacesEnvVariables() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("ENV_AUTH_TOKEN", "AAA-BBB-CCC");
        env.put("EMPTY_VARIABLE", "");
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml", "npmScopes:\n" +
                "  onlyEnv:\n    npmAuthToken: \"${ENV_AUTH_TOKEN}\"\n" +
                "  multipleEnvs:\n    npmAuthToken: \"${ENV_AUTH_TOKEN}-separator-${ENV_AUTH_TOKEN}\"\n" +
                "  envInString:\n    npmAuthToken: \"beforeEnv-${ENV_AUTH_TOKEN}-after-env\"\n" +
                "  envSetWithFallback:\n    npmAuthToken: \"${ENV_AUTH_TOKEN-fallback-value}\"\n" +
                "  unsetEnvWithFallback:\n    npmAuthToken: \"${NOT_EXISTING_ENV-fallback-value}\"\n" +
                "  unsetEnvWithEmptyFallback:\n    npmAuthToken: \"${NOT_EXISTING_ENV-}\"\n" +
                "  emptyEnvWithStrictFallback:\n    npmAuthToken: \"${EMPTY_VARIABLE-fallback-value}\"\n" +
                "  emptyEnvWithFallback:\n    npmAuthToken: \"${EMPTY_VARIABLE:-fallback-for-empty-value}\"\n" +
                "  emptyEnvWithEmptyFallback:\n    npmAuthToken: \"${EMPTY_VARIABLE:-}\"\n"), HostEnvironment.of(env));
        assertThat(scope(options, "onlyEnv").get("npmAuthToken")).isEqualTo("AAA-BBB-CCC");
        assertThat(scope(options, "multipleEnvs").get("npmAuthToken"))
                .isEqualTo("AAA-BBB-CCC-separator-AAA-BBB-CCC");
        assertThat(scope(options, "envInString").get("npmAuthToken")).isEqualTo("beforeEnv-AAA-BBB-CCC-after-env");
        assertThat(scope(options, "envSetWithFallback").get("npmAuthToken")).isEqualTo("AAA-BBB-CCC");
        assertThat(scope(options, "unsetEnvWithFallback").get("npmAuthToken")).isEqualTo("fallback-value");
        assertThat(scope(options, "unsetEnvWithEmptyFallback").get("npmAuthToken")).isEqualTo("");
        assertThat(scope(options, "emptyEnvWithStrictFallback").get("npmAuthToken")).isEqualTo("");
        assertThat(scope(options, "emptyEnvWithFallback").get("npmAuthToken")).isEqualTo("fallback-for-empty-value");
        assertThat(scope(options, "emptyEnvWithEmptyFallback").get("npmAuthToken")).isEqualTo("");
    }

    @Test
    void forbidsUnsetVariables() {
        assertThatThrownBy(() -> Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "npmScopes:\n  onlyEnv:\n    npmAuthToken: \"${A_VARIABLE_THAT_DEFINITELY_DOESNT_EXIST}\"\n"),
                HostEnvironment.of(Collections.emptyMap())))
                .isInstanceOf(Configuration.UsageError.class);
    }

    @Test
    void handlesBooleanVariablesCorrectly() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("TRUE_VARIABLE", "true");
        env.put("FALSE_VARIABLE", "false");
        env.put("ONE_VARIABLE", "1");
        env.put("ZERO_VARIABLE", "0");
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml", "npmScopes:\n" +
                "  true:\n    npmAlwaysAuth: \"${TRUE_VARIABLE}\"\n" +
                "  false:\n    npmAlwaysAuth: \"${FALSE_VARIABLE}\"\n" +
                "  one:\n    npmAlwaysAuth: \"${ONE_VARIABLE}\"\n" +
                "  zero:\n    npmAlwaysAuth: \"${ZERO_VARIABLE}\"\n" +
                "  defaultTrue:\n    npmAlwaysAuth: \"${NOT_EXISTING_ENV-true}\"\n" +
                "  defaultFalse:\n    npmAlwaysAuth: \"${NOT_EXISTING_ENV-false}\"\n"), HostEnvironment.of(env));
        assertThat(scope(options, "true").get("npmAlwaysAuth")).isEqualTo(true);
        assertThat(scope(options, "false").get("npmAlwaysAuth")).isEqualTo(false);
        assertThat(scope(options, "one").get("npmAlwaysAuth")).isEqualTo(true);
        assertThat(scope(options, "zero").get("npmAlwaysAuth")).isEqualTo(false);
        assertThat(scope(options, "defaultTrue").get("npmAlwaysAuth")).isEqualTo(true);
        assertThat(scope(options, "defaultFalse").get("npmAlwaysAuth")).isEqualTo(false);
    }

    @Test
    void mergesMapProperties() {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"https://foo.server\"\nnpmScopes:\n  foo:\n    npmAuthToken: token for foo\n"),
                HostEnvironment.of(Collections.emptyMap()));
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("npmRegistryServer", "http://bar.server");
        Map<String, Object> scopes = new LinkedHashMap<>();
        scopes.put("foo", Collections.singletonMap("npmAlwaysAuth", true));
        scopes.put("bar", Collections.singletonMap("npmAlwaysAuth", true));
        second.put("npmScopes", scopes);
        options.use("second file", second);
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://foo.server");
        assertThat(scope(options, "foo").get("npmAuthToken")).isEqualTo("token for foo");
        assertThat(scope(options, "foo").get("npmAlwaysAuth")).isEqualTo(false);
        assertThat(scope(options, "bar").get("npmAlwaysAuth")).isEqualTo(true);
    }

    @Test
    void ignoresYarnRegistryIfSet() {
        Configuration options = Configuration.load(Collections.emptyMap(),
                HostEnvironment.of(Collections.singletonMap("YARN_REGISTRY", "https://example.org")));
        options.restrictTo(YarnRelease.of("4.10.3"));
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://registry.yarnpkg.com");
    }

    /**
     * Yarn 4.10.3 under corepack runs {@code yarn config get compressionLevel} with each of these set and rejects
     * {@code YARN_COMMAND}, which only Yarn 1 read.
     */
    @Test
    void ignoresTheEnvironmentVariablesConfigurationTsLists() {
        for (String variable : new String[]{"YARN_IS_TEST_ENV", "YARN_INJECT_NPM_USER", "YARN_INJECT_NPM_PASSWORD",
                "YARN_INJECT_NPM_2FA_TOKEN", "YARN_ZIP_DATA_EPILOGUE", "YARN_CACHE_CHECKPOINT_OVERRIDE",
                "YARN_CACHE_VERSION_OVERRIDE", "YARN_LOCKFILE_VERSION_OVERRIDE", "YARN_OS_OVERRIDE",
                "YARN_CPU_OVERRIDE", "YARN_LIBC_OVERRIDE", "YARN_BIN_FOLDER", "YARN_VERSION", "YARN_FLAGS",
                "YARN_PROFILE", "YARN_GPG", "YARN_IGNORE_NODE", "YARN_WRAP_OUTPUT", "YARN_HOME", "YARN_CONF_DIR",
                "YARN_REGISTRY", "YARN_IGNORE_CWD"}) {
            Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 3\n"),
                    HostEnvironment.of(Collections.singletonMap(variable, "1")));
            options.restrictTo(YarnRelease.of("4.10.3"));
            assertThat(options.get("compressionLevel")).as(variable).isEqualTo(3);
        }
        assertThatThrownBy(() ->
                Configuration.load(Collections.emptyMap(), HostEnvironment.of(Collections.singletonMap("YARN_COMMAND",
                "x"))).restrictTo(YarnRelease.of("4.10.3")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: command");
    }

    /**
     * The key camelcase 5.3.1 gives a lower-cased variable name; every expectation is its own answer.
     */
    @Test
    void namesAnEnvironmentVariableAsCamelcaseDoes() {
        String[][] rows = {{"inject_npm_2fa_token", "injectNpm2FaToken"}, {"npm_registry_server", "npmRegistryServer"},
                {"_foo", "foo"}, {"a__b", "aB"}, {"foo_", "foo"}, {"x", "x"}, {"foo_\u00E9x", "foo_\u00E9x"},
                {"a.b-c d", "aBCD"}, {"12ab_3cd", "12Ab3Cd"}, {" a_b ", "aB"}, {"a_1", "a1"}};
        for (String[] row : rows) {
            assertThat(Configuration.camelcase(row[0])).as(row[0]).isEqualTo(row[1]);
        }
    }

    /**
     * "should merge mergeable array properties" for a string array that does not concatenate: Yarn 4.10.3 keeps the
     * environment's {@code ["env"]} over an rc file's {@code [rc]}.
     */
    @Test
    void doesNotMergeAnArrayThatDoesNotConcatenate() {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "npmPreapprovedPackages:\n  - example.com\n"), HostEnvironment.of(Collections.emptyMap()));
        options.use("second file", Collections.singletonMap("npmPreapprovedPackages",
                Collections.singletonList("evil.com")));
        assertThat(options.get("npmPreapprovedPackages")).isEqualTo(Collections.singletonList("example.com"));

        assertThat(Configuration.load(Collections.singletonMap(".yarnrc.yml", "npmPreapprovedPackages: [rc]\n"),
                HostEnvironment.of(Collections.singletonMap("YARN_NPM_PREAPPROVED_PACKAGES", "env")))
                        .get("npmPreapprovedPackages"))
                .isEqualTo(Collections.singletonList("env"));
    }

    /**
     * "Multiple RC files": three nested rc files, the middle one with {@code onConflict} directives, read from the
     * nearest directory as {@code Configuration.find} does; Yarn 4.10.3 answers the same over these files.
     */
    @Test
    void resolvesTheStringsOfMultipleRcFiles() {
        Configuration options = load(
                "defaultLanguageName: foo\ndefaultProtocol: foo\nchecksumBehavior: foo\nnpmPublishRegistry: foo\n",
                "defaultLanguageName: bar\n" +
                "defaultProtocol:\n  onConflict: reset\n  value: bar\n" +
                "checksumBehavior:\n  onConflict: extend\n  value: bar\n" +
                "npmPublishRegistry:\n  onConflict: reset\n  value: bar\n",
                "npmPublishRegistry: baz\n");
        assertThat(options.get("defaultLanguageName")).isEqualTo("bar");
        assertThat(options.get("defaultProtocol")).isEqualTo("bar");
        assertThat(options.get("checksumBehavior")).isEqualTo("bar");
        assertThat(options.get("npmPublishRegistry")).isEqualTo("baz");
    }

    @Test
    void resolvesTheStringArraysOfMultipleRcFiles() {
        String far = "npmPreapprovedPackages: [foo]\n";
        assertThat(load(far, "npmPreapprovedPackages: [bar]\n", "").get("npmPreapprovedPackages"))
                .isEqualTo(Arrays.asList("foo", "bar"));
        assertThat(load(far, "npmPreapprovedPackages:\n  onConflict: reset\n  value: [bar]\n", "")
                .get("npmPreapprovedPackages")).isEqualTo(Collections.singletonList("bar"));
        assertThat(load(far, "npmPreapprovedPackages:\n  onConflict: extend\n  value: [bar]\n", "")
                .get("npmPreapprovedPackages")).isEqualTo(Arrays.asList("foo", "bar"));
        assertThat(load(far, "npmPreapprovedPackages:\n  onConflict: hardReset\n  value: [bar]\n",
                "npmPreapprovedPackages: [baz]\n").get("npmPreapprovedPackages"))
                .isEqualTo(Collections.singletonList("baz"));
    }

    /**
     * The test plugin's shape {@code {number, string}} is a scope's {@code {npmAlwaysAuth, npmRegistryServer}}
     * here: {@code 0} is {@code false}, {@code 2} is {@code true}, {@code "default"} the default registry.
     */
    @Test
    void resolvesTheMapsOfMultipleRcFiles() {
        String far = "npmScopes:\n  foo:\n    npmRegistryServer: foo\n";
        String bar = "    bar:\n      npmAlwaysAuth: true\n      npmRegistryServer: bar\n";
        assertThat(scopes(load(far, "npmScopes:\n  bar:\n    npmAlwaysAuth: true\n    npmRegistryServer: bar\n",
                ""))).isEqualTo("{foo=false foo, bar=true bar}");
        assertThat(scopes(load(far, "npmScopes:\n  onConflict: reset\n  value:\n" + bar, "")))
                .isEqualTo("{bar=true bar}");
        assertThat(scopes(load(far, "npmScopes:\n  onConflict: extend\n  value:\n" + bar, "")))
                .isEqualTo("{foo=false foo, bar=true bar}");
        assertThat(scopes(load(far, "npmScopes:\n  onConflict: hardReset\n  value:\n" + bar,
                "npmScopes:\n  baz:\n    npmRegistryServer: baz\n"))).isEqualTo("{baz=false baz}");
    }

    @Test
    void resolvesTheShapesOfMultipleRcFiles() {
        String far = "npmScopes:\n  s:\n    npmRegistryServer: foo\n";
        String value = "      npmAlwaysAuth: true\n";
        String defaultRegistry = "https://registry.yarnpkg.com";
        assertThat(scopes(load(far, "npmScopes:\n  s:\n    npmAlwaysAuth: true\n", "")))
                .isEqualTo("{s=true foo}");
        assertThat(scopes(load(far, "npmScopes:\n  s:\n    onConflict: reset\n    value:\n" + value, "")))
                .isEqualTo("{s=true " + defaultRegistry + "}");
        assertThat(scopes(load(far, "npmScopes:\n  s:\n    onConflict: extend\n    value:\n" + value, "")))
                .isEqualTo("{s=true foo}");
        assertThat(scopes(load(far, "npmScopes:\n  s:\n    onConflict: hardReset\n    value:\n" + value,
                "npmScopes:\n  s:\n    npmRegistryServer: baz\n"))).isEqualTo("{s=false baz}");
    }

    /**
     * Configuration.find puts the home directory's rc file first, under every rc file of the project
     * ({@code user-yarnrc-registry-bump}, {@code user-yarnrc-project-yarnrc-bump}).
     */
    @Test
    void theHomeRcFileIsReadUnderTheProjects() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("~/.yarnrc.yml", "npmRegistryServer: \"https://registry.npmmirror.com\"\n" +
                                   "npmScopes:\n  types:\n    npmRegistryServer: \"https://registry.npmmirror.com\"\n");
        files.put("../.yarnrc.yml", "npmScopes:\n  types:\n    npmAlwaysAuth: true\n");
        Configuration home = Configuration.load(files, HostEnvironment.of(Collections.emptyMap()));
        assertThat(home.getString("npmRegistryServer")).isEqualTo("https://registry.npmmirror.com");
        assertThat(scopes(home)).isEqualTo("{types=true https://registry.npmmirror.com}");

        files.put(".yarnrc.yml", "npmRegistryServer: \"https://registry.npmjs.org\"\n");
        assertThat(Configuration.load(files, HostEnvironment.of(Collections.emptyMap()))
                .getString("npmRegistryServer")).isEqualTo("https://registry.npmjs.org");
    }

    /**
     * Under YARN_RC_FILENAME, Yarn 4.10.3 still reads the home directory's .yarnrc.yml and not a home file of that
     * name ({@code yarn config get npmRegistryServer} answers so).
     */
    @Test
    void theHomeRcFileKeepsItsNameUnderYarnRcFilename() {
        HostEnvironment env = HostEnvironment.of(Collections.singletonMap("YARN_RC_FILENAME", ".custom.yml"));
        String mirror = "npmRegistryServer: \"https://registry.npmmirror.com\"\n";

        assertThat(Configuration.load(Collections.singletonMap("~/.yarnrc.yml", mirror), env)
                .getString("npmRegistryServer")).isEqualTo("https://registry.npmmirror.com");
        assertThat(Configuration.load(Collections.singletonMap("~/.custom.yml", mirror), env)
                .getString("npmRegistryServer")).isEqualTo("https://registry.yarnpkg.com");
        assertThat(Configuration.load(Collections.singletonMap(".custom.yml", mirror), env)
                .getString("npmRegistryServer")).isEqualTo("https://registry.npmmirror.com");
    }

    private static Configuration load(String far, String mid, String near) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../../.yarnrc.yml", far);
        files.put("../.yarnrc.yml", mid);
        if (!near.isEmpty()) {
            files.put(".yarnrc.yml", near);
        }
        return Configuration.load(files, HostEnvironment.of(Collections.emptyMap()));
    }

    private static String scopes(Configuration options) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> scope : options.getMap("npmScopes").entrySet()) {
            @SuppressWarnings("unchecked") Map<String, Object> shape = (Map<String, Object>) scope.getValue();
            out.put(scope.getKey(), shape.get("npmAlwaysAuth") + " " + shape.get("npmRegistryServer"));
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scope(Configuration options, String scope) {
        return (Map<String, Object>) options.getMap("npmScopes").get(scope);
    }


    @Test
    void defaults() {
        Configuration options = Configuration.defaults();
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
        Configuration options = Configuration.load(files, HostEnvironment.of(Collections.emptyMap()));
        assertThat(options.get("compressionLevel")).isEqualTo(3);
        assertThat(options.get("enableTransparentWorkspaces")).isEqualTo(false);
        assertThat(Configuration.load(files,
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
        Configuration options = Configuration.load(files, HostEnvironment.of(Collections.emptyMap()));
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
        assertThat(Configuration.findRcFiles("packages/a", files)).containsOnlyKeys("../.yarnrc.yml", ".yarnrc.yml",
          "packages/.yarnrc.yml", "packages/a/.yarnrc.yml");
        assertThat(Configuration.findRcFiles("", files)).containsOnlyKeys("../.yarnrc.yml", ".yarnrc.yml");
    }

    @Test
    void mapSettingsMergeAcrossFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".yarnrc.yml", "catalog:\n  a: ^1.0.0\n");
        files.put("../.yarnrc.yml", "catalog:\n  a: ^2.0.0\n  b: ^3.0.0\n");
        assertThat(Configuration.load(files, HostEnvironment.of(Collections.emptyMap())).getMap("catalog").toString())
                .isEqualTo("{a=^1.0.0, b=^3.0.0}");
    }

    @Test
    void rejectsAValueItsDefinitionDoesNotAllow() {
        // Yarn 4.10.3 prints "Internal Error: Invalid value, expected one of mixed, 0, ..., 9 in <path>/.yarnrc.yml"
        assertThatThrownBy(() -> Configuration.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 10\n"),
                HostEnvironment.of(Collections.emptyMap())))
                .isInstanceOf(JsError.class)
                .hasMessageContaining("Invalid value, expected one of mixed, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9");
    }

    @Test
    void replacesEnvironmentVariables() {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
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
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: catalog - run \"yarn " +
                                        "config -v\" to see the list of settings supported in Yarn");
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
        assertThat(first.getProject().configuration.getMap("catalog")).isEmpty();
        assertThat(first.getMigratedSettings()).containsOnlyKeys("enableGlobalCache", "compressionLevel");

        YarnBerryLockRegenerator.Loaded second = regenerator.load(YarnRelease.of("4.10.3"), Collections.emptyMap());
        assertThat(second.getProject().configuration.getMap("catalog")).containsEntry("ms", "^2.1.0");
        assertThat(second.getMigratedSettings()).isEqualTo(first.getMigratedSettings());
    }

    @Test
    void theReleaseYarnPathNamesIsInItsFileName() {
        assertThat(Configuration.yarnPathVersion(".yarn/releases/yarn-4.10.3.cjs")).isEqualTo("4.10.3");
        assertThat(Configuration.yarnPathVersion(".yarn/releases/yarn.cjs")).isNull();
    }

    /**
     * Yarn 4.10.3 under corepack: an rc file's {@code enableStrictSettings} never
     * reaches the pass that reads unknown keys; the environment's does.
     */
    @Test
    void onlyTheEnvironmentTurnsStrictSettingsOff() {
        YarnRelease release = YarnRelease.of("4.10.3");
        assertThatThrownBy(() -> Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "enableStrictSettings: false\nfooUnknownSetting: 1\ncompressionLevel: 3\n"),
                        HostEnvironment.of(Collections.emptyMap()))
                .restrictTo(release))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessageStartingWith("Unrecognized or legacy configuration settings found: fooUnknownSetting - " +
                                        "run \"yarn config\" to see the list of settings supported in Yarn");

        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "fooUnknownSetting: 1\ncompressionLevel: 3\n"),
                HostEnvironment.of(Collections.singletonMap("YARN_ENABLE_STRICT_SETTINGS", "false")));
        options.restrictTo(release);
        assertThat(options.get("compressionLevel")).isEqualTo(3);

        Map<String, String> env = new LinkedHashMap<>();
        env.put("YARN_ENABLE_STRICT_SETTINGS", "false");
        env.put("YARN_FOO_UNKNOWN", "1");
        Configuration.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 3\n"), HostEnvironment.of(env))
                .restrictTo(release);

        assertThatThrownBy(() -> Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "enableStrictSettings: false\nfooUnknownSetting: 1\n"),
                HostEnvironment.of(Collections.singletonMap("YARN_ENABLE_STRICT_SETTINGS", "true")))
                        .restrictTo(release))
                .isInstanceOf(Configuration.UsageError.class);
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
        assertThat(Configuration.load(Collections.emptyMap(),
                HostEnvironment.of(Collections.singletonMap("YARN_COMPRESSION_LEVEL", "0x5")))
                .get("compressionLevel")).isEqualTo(5);
        assertThat(options("npmMinimalAgeGate: \"\\u3000\\u2028-12\"\n").get("npmMinimalAgeGate")).isEqualTo(-12);
        assertThat(options("npmMinimalAgeGate: \"99999999999\"\n").get("npmMinimalAgeGate")).isEqualTo(99999999999d);
    }

    /**
     * Yarn 4.10.3 (open-option-supported-architectures-null-element, -catalog-entry-null,
     * -package-extension-dependency-null): null is non-nullable unless nullable or the default, else not a string.
     */
    @Test
    void aNullValueIsRejectedAsItsDefinitionSays() {
        assertThatThrownBy(() -> options("supportedArchitectures:\n  os:\n    -\n"))
                .isInstanceOf(JsError.class)
                .hasMessage("Expected configuration setting \"supportedArchitectures.os[0]\" to be a string, got " +
                            "object in .yarnrc.yml");
        assertThatThrownBy(() -> options("catalog:\n  ms:\n"))
                .isInstanceOf(JsError.class)
                .hasMessage("Non-nullable configuration settings \"catalog['ms']\" cannot be set to null in " +
                            ".yarnrc.yml");
        assertThatThrownBy(() -> options("packageExtensions:\n  \"ms@*\":\n    dependencies:\n      lodash:\n"))
                .isInstanceOf(JsError.class)
                .hasMessageStartingWith("Non-nullable configuration settings " +
                                        "\"packageExtensions['ms@*'].dependencies['lodash']\" cannot be set to null");
        assertThatThrownBy(() -> options("npmPublishRegistry:\n"))
                .isInstanceOf(JsError.class)
                .hasMessageStartingWith("Expected configuration setting \"npmPublishRegistry\" to be a string");
    }

    /**
     * Yarn 4.10.3 and 4.5.3 (open-option-npm-scopes-unknown-key, -by-4.5.3, -before-yarn-path): the release yarnPath
     * picks, read before any other setting, words the error for a key a shape lacks.
     */
    @Test
    void theReleaseThatReadsTheSettingsWordsAnUnknownKeyOfAShape() {
        String scope = "npmScopes:\n  foo:\n    npmRegistryServr: \"https://registry.npmjs.org\"\n";
        String message = "Unrecognized configuration settings found: npmScopes['foo'].npmRegistryServr - run \"%s\" " +
                         "to see the list of settings supported in Yarn in .yarnrc.yml";
        Configuration options = options(scope + "yarnPath: .yarn/releases/yarn-4.5.3.cjs\n");
        assertThat(options.get("yarnPath")).isEqualTo(".yarn/releases/yarn-4.5.3.cjs");
        assertThatThrownBy(() -> options.restrictTo(YarnRelease.of("4.5.3")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessage(String.format(message, "yarn config -v"));
        assertThatThrownBy(() -> options(scope).checkShapeKeys(YarnRelease.of("4.10.3")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessage(String.format(message, "yarn config"));
    }

    /**
     * Yarn 4.10.3: an npmRegistries key loses its trailing slash before its value is read, so an error names the key
     * so, and of two spellings in one file the later stands.
     */
    @Test
    void npmRegistriesNormalizesItsKeys() {
        assertThatThrownBy(() -> options("npmRegistries:\n  \"http://127.0.0.1:4898/\":\n    unknownField: true\n")
                .checkShapeKeys(YarnRelease.of("4.10.3")))
                .isInstanceOf(Configuration.UsageError.class)
                .hasMessage("Unrecognized configuration settings found: npmRegistries['http://127.0.0.1:4898']" +
                            ".unknownField - run \"yarn config\" to see the list of settings supported in Yarn in " +
                            ".yarnrc.yml");
        assertThatThrownBy(() -> options("npmRegistries:\n  \"//127.0.0.1:4898/\":\n    npmAuthToken: [\"a\"]\n"))
                .isInstanceOf(JsError.class)
                .hasMessage("Non-array configuration settings \"npmRegistries['//127.0.0.1:4898'].npmAuthToken\" " +
                            "cannot be an array in .yarnrc.yml");
        Map<String, Object> registries = options("npmRegistries:\n  \"//h\":\n    npmAuthToken: first\n" +
                                                 "  \"//g/\":\n    npmAuthToken: g\n  \"//h/\":\n" +
                                                 "    npmAuthToken: second\n").getMap("npmRegistries");
        assertThat(registries).containsOnlyKeys("//h", "//g");
        assertThat(registries.keySet()).containsExactly("//h", "//g");
        assertThat(registries.get("//h").toString()).contains("npmAuthToken=second");
        assertThat(options("npmScopes:\n  \"s/\":\n    npmAuthToken: t\n").getMap("npmScopes")).containsOnlyKeys("s/");
    }

    private static Configuration options(String yarnrc) {
        return Configuration.load(Collections.singletonMap(".yarnrc.yml", yarnrc),
                HostEnvironment.of(Collections.emptyMap()));
    }
}
