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
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "npmScopes:\n" +
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
        assertThatThrownBy(() -> BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmScopes:\n  onlyEnv:\n    npmAuthToken: \"${A_VARIABLE_THAT_DEFINITELY_DOESNT_EXIST}\"\n"),
                HostEnvironment.of(Collections.emptyMap())))
                .isInstanceOf(BerryOptions.UsageError.class);
    }

    @Test
    void handlesBooleanVariablesCorrectly() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("TRUE_VARIABLE", "true");
        env.put("FALSE_VARIABLE", "false");
        env.put("ONE_VARIABLE", "1");
        env.put("ZERO_VARIABLE", "0");
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "npmScopes:\n" +
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
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
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
        BerryOptions options = BerryOptions.load(Collections.emptyMap(),
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
            BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "compressionLevel: 3\n"),
                    HostEnvironment.of(Collections.singletonMap(variable, "1")));
            options.restrictTo(YarnRelease.of("4.10.3"));
            assertThat(options.get("compressionLevel")).as(variable).isEqualTo(3);
        }
        assertThatThrownBy(() ->
                BerryOptions.load(Collections.emptyMap(), HostEnvironment.of(Collections.singletonMap("YARN_COMMAND",
                "x"))).restrictTo(YarnRelease.of("4.10.3")))
                .isInstanceOf(BerryOptions.UsageError.class)
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
            assertThat(BerryOptions.camelcase(row[0])).as(row[0]).isEqualTo(row[1]);
        }
    }

    /**
     * "should merge mergeable array properties" for a string array that does not concatenate: Yarn 4.10.3 keeps the
     * environment's {@code ["env"]} over an rc file's {@code [rc]}.
     */
    @Test
    void doesNotMergeAnArrayThatDoesNotConcatenate() {
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmPreapprovedPackages:\n  - example.com\n"), HostEnvironment.of(Collections.emptyMap()));
        options.use("second file", Collections.singletonMap("npmPreapprovedPackages",
                Collections.singletonList("evil.com")));
        assertThat(options.get("npmPreapprovedPackages")).isEqualTo(Collections.singletonList("example.com"));

        assertThat(BerryOptions.load(Collections.singletonMap(".yarnrc.yml", "npmPreapprovedPackages: [rc]\n"),
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
        BerryOptions options = load(
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

    private static BerryOptions load(String far, String mid, String near) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../../.yarnrc.yml", far);
        files.put("../.yarnrc.yml", mid);
        if (!near.isEmpty()) {
            files.put(".yarnrc.yml", near);
        }
        return BerryOptions.load(files, HostEnvironment.of(Collections.emptyMap()));
    }

    private static String scopes(BerryOptions options) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> scope : options.getMap("npmScopes").entrySet()) {
            @SuppressWarnings("unchecked") Map<String, Object> shape = (Map<String, Object>) scope.getValue();
            out.put(scope.getKey(), shape.get("npmAlwaysAuth") + " " + shape.get("npmRegistryServer"));
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scope(BerryOptions options, String scope) {
        return (Map<String, Object>) options.getMap("npmScopes").get(scope);
    }
}
