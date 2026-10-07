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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/yarnpkg-core/tests/Configuration.test.ts, the cases about settings that reach a lock: the tag
 * pattern, environment interpolation, map merging and the ignored environment variables. Not ported: secrets
 * hiding, mergeable arrays
 * ({@code logFilters}, {@code unsafeHttpWhitelist}), {@code overwrite} merging and the multiple-rc-file listing
 * of {@code yarn config}, none of which reaches a lock.
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
                "  emptyEnvWithEmptyFallback:\n    npmAuthToken: \"${EMPTY_VARIABLE:-}\"\n"), env);
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
                Collections.emptyMap()))
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
                "  defaultFalse:\n    npmAlwaysAuth: \"${NOT_EXISTING_ENV-false}\"\n"), env);
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
                Collections.emptyMap());
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
                Collections.singletonMap("YARN_REGISTRY", "https://example.org"));
        options.restrictTo(YarnRelease.of("4.10.3").getSettings());
        assertThat(options.get("npmRegistryServer")).isEqualTo("https://registry.yarnpkg.com");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scope(BerryOptions options, String scope) {
        return (Map<String, Object>) options.getMap("npmScopes").get(scope);
    }
}
