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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The config the registries read from the environment and the .npmrc, as yarn 1.22.22's own registries answered
 * from its bundle, and the cases of its own tests of the registries and util/env-replace.js.
 */
class YarnClassicOptionsTest {

    static Stream<Arguments> environments() {
        return Stream.of(
                Arguments.of(env("YARN_REGISTRY", "https://r.example/"), "{\"registry\":\"https://r.example/\"}"),
                Arguments.of(env("yarn_ignore_optional", "true", "YARN_PRODUCTION", "false"),
                        "{\"ignore-optional\":true,\"production\":false}"),
                Arguments.of(env("YARN_A__B_C", "x"), "{\"a\":{\"b-c\":\"x\"}}"),
                Arguments.of(env("YARN___A", "x"), "{\"\":{\"a\":\"x\"}}"),
                Arguments.of(env("YARN_A___B", "x"), "{\"a\":{\"-b\":\"x\"}}"),
                Arguments.of(env("YARN_", "x"), "{}"),
                Arguments.of(env("YARN_A__2", "x"), "{\"a\":[null,null,\"x\"]}"),
                Arguments.of(env("YARN_FOO", "x", "YARN_FOO__0", "y"),
                        "TypeError: Cannot assign to read only property '0' of string 'x'"),
                Arguments.of(env("YARN_REGISTRY", "a", "yarn_registry", "b"), "{\"registry\":\"b\"}"),
                Arguments.of(env("npm_config__auth", "x", "npm_config_@scope:registry", "https://s.example/",
                                "NPM_CONFIG_REGISTRY", "r"),
                        "{\"_auth\":\"x\",\"@scope:registry\":\"https://s.example/\",\"registry\":\"r\"}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("environments")
    void readsTheEnvironmentIntoTheNpmConfig(Map<String, String> env, String npmConfig) {
        String answer;
        try {
            answer = Js.jsonStringify(YarnClassicOptions.load((String) null, null, env).getNpmConfig());
        } catch (JsError e) {
            answer = e.getName() + ": " + e.getMessage();
        }
        assertThat(answer).isEqualTo(npmConfig);
    }

    static Stream<Arguments> registries() {
        return Stream.of(
                Arguments.of(env(), "left-pad", "https://registry.yarnpkg.com"),
                Arguments.of(env(), "@scope/x", "https://registry.yarnpkg.com"),
                Arguments.of(env("npm_config_registry", "http://127.0.0.1:9/"), "left-pad", "http://127.0.0.1:9/"),
                Arguments.of(env("npm_config_@scope:registry", "https://s.example/"), "@scope/x", "https://s.example/"),
                Arguments.of(env("npm_config_@scope:registry", "https://s.example/"), "left-pad",
                        "https://registry.yarnpkg.com"),
                Arguments.of(env("YARN_REGISTRY", "https://r.example/"), "left-pad", "https://r.example/"),
                Arguments.of(env("YARN_REGISTRY", "https://r.example/", "npm_config_registry", "https://n.example/"),
                        "left-pad", "https://n.example/"),
                Arguments.of(env("npm_config_undefined", "https://u.example/"), "left-pad",
                        "https://registry.yarnpkg.com"),
                Arguments.of(env("npm_config_undefined", "https://u.example/"), "@scope/x", "https://u.example/"),
                Arguments.of(env(), "https://registry.npmjs.org/left-pad", "https://registry.yarnpkg.com"),
                Arguments.of(env("npm_config_registry", "https://n.example/"), "https://n.example/left-pad",
                        "https://n.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/"), "HTTPS://n.example/left-pad",
                        "https://n.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/"), "//n.example/left-pad",
                        "https://n.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/"), "https://o.example/x",
                        "https://n.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/", "npm_config_@s:registry",
                        "https://s.example/"), "https://s.example/@s%2fx", "https://s.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/", "npm_config_@s:registry",
                        "https://s.example/"), "https://o.example/@s/x", "https://s.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/", "npm_config_@s:registry",
                        "https://s.example/"), "https://o.example/x", "https://n.example/"),
                Arguments.of(env("npm_config_registry", "https://n.example/", "npm_config_@s:registry",
                        "https://s.example/"), "https://n.example/@s/x", "https://n.example/"),
                Arguments.of(env("npm_config_a:registry:b", "https://ab.example/"), "https://ab.example/x",
                        "https://ab.example/"),
                Arguments.of(env("npm_config_a:registry:b", "https://ab.example/"), "x",
                        "https://registry.yarnpkg.com"),
                Arguments.of(env("npm_config_//h.example/:registry", "https://h.example/"), "https://h.example/x",
                        "https://registry.yarnpkg.com"),
                Arguments.of(env("npm_config_registry", "//p.example/"), "//p.example/x", "//p.example/"),
                Arguments.of(env("npm_config_registry", "//p.example/"), "http://p.example/x", "//p.example/"),
                Arguments.of(env("npm_config_registry", "true"), "https://x", "true"),
                Arguments.of(env("npm_config_registry", "true"), "true", "true"),
                Arguments.of(env("YARN_REGISTRY", "https://r.example/"), "https://r.example/x", "https://r.example/"),
                Arguments.of(env("npm_config_@s:registry", "https://s.example/", "npm_config_registry",
                        "https://s.example/"), "https://s.example/x", "https://s.example/")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("registries")
    void requestsMetadataFromTheRegistryYarnDoes(Map<String, String> env, String packageIdent, String registry) {
        assertThat(YarnClassicOptions.load((String) null, null, env).getRegistry(packageIdent)).isEqualTo(registry);
    }

    /**
     * An empty registry is the first an ident starts with and stops the search; yarn 1.22.22's bundled NpmRegistry
     * over a project .npmrc answered each.
     */
    static Stream<Arguments> npmrcRegistries() {
        return Stream.of(
                Arguments.of("registry=https://a/\n@s:registry=https://a/b/\n", env(),
                        "https://a/b/x https://a/x @s/x x", "https://a/ https://a/ https://a/b/ https://a/"),
                Arguments.of("@s:registry=https://a/b/\nregistry=https://a/\n", env(),
                        "https://a/b/x https://a/x @s/x x", "https://a/b/ https://a/ https://a/b/ https://a/"),
                Arguments.of("registry=\n", env(), "https://x/y x //x/y",
                        "https://registry.yarnpkg.com https://registry.yarnpkg.com https://registry.yarnpkg.com"),
                Arguments.of("registry=\n@s:registry=https://s/\n", env(), "https://s/x https://q/@s/x",
                        "https://registry.yarnpkg.com https://s/"),
                Arguments.of("@s:registry=https://e/s/\n", env("npm_config_registry", "https://e/"),
                        "https://e/s/x https://e/x", "https://e/s/ https://e/"),
                Arguments.of("registry=https://a/\n", env("npm_config_registry", "https://b/"),
                        "https://b/x https://a/x", "https://b/ https://b/"),
                Arguments.of("registry=false\n", env(), "https://x/y x",
                        "https://registry.yarnpkg.com https://registry.yarnpkg.com"),
                Arguments.of("registry=true\n", env(), "https://x/y x", "true true"),
                Arguments.of("@a:registry=https://h/a/\n@b:registry=https://h/\n", env(), "https://h/a/x https://h/x",
                        "https://h/a/ https://h/"),
                Arguments.of("registry=https://n/\n", env("YARN_REGISTRY", "https://y/"), "https://y/x x",
                        "https://y/ https://y/"),
                Arguments.of(null, env("npm_config_registry", ""), "https://x/y x",
                        "https://registry.yarnpkg.com https://registry.yarnpkg.com"),
                Arguments.of("registry = https://sp/ \n", env(), "https://sp/x x", "https://sp/ https://sp/")
        );
    }

    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("npmrcRegistries")
    void requestsMetadataFromTheRegistryAnNpmrcNames(@Nullable String npmrc, Map<String, String> env,
                                                     String packageIdents, String registries) {
        YarnClassicOptions options = YarnClassicOptions.load((String) null, npmrc, env);
        List<String> answers = new ArrayList<>();
        for (String ident : packageIdents.split(" ")) {
            answers.add(options.getRegistry(ident));
        }
        assertThat(String.join(" ", answers)).isEqualTo(registries);
    }

    /**
     * yarn 1.22.22's __tests__/util/env-replace.js.
     */
    @Test
    void replacesATokenThatExistsInTheEnvironment() {
        Map<String, String> env = env("a", "token");
        assertThat(YarnClassicOptions.envReplace("test ${a} replacement", env)).isEqualTo("test token replacement");
        assertThat(YarnClassicOptions.envReplace("${a} replacement", env)).isEqualTo("token replacement");
        assertThat(YarnClassicOptions.envReplace("${a}", env)).isEqualTo("token");
    }

    @Test
    void doesNotReplaceATokenThatDoesNotExistInTheEnvironment() {
        assertThatThrownBy(() -> YarnClassicOptions.envReplace("${a} replacement", env("b", "token")))
                .isInstanceOf(JsError.class)
                .hasMessage("Failed to replace env in config: ${a}");
    }

    @Test
    void doesNotReplaceATokenPrefixedWithABackslash() {
        assertThat(YarnClassicOptions.envReplace("\\${a} replacement", env("a", "token")))
                .isEqualTo("\\${a} replacement");
    }

    /**
     * yarn 1.22.22's __tests__/registries/npm-registry.js, getScope functional test.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "normal|''",
            "@scopedNoPkg|''",
            "@scoped/notescaped|@scoped",
            "not@scope/pkg|''",
            "@scope?query=true|''",
            "@scope%2fpkg|@scope",
            "@scope%2fpkg%2fext|@scope",
            "@scope%2fpkg?query=true|@scope",
            "@scope%2fpkg%2f1.2.3|@scope",
            "http://foo.bar:80/normal|''",
            "http://foo.bar:80/@scopedNoPkg|''",
            "http://foo.bar:80/@scoped/notescaped|@scoped",
            "http://foo.bar:80/@scoped/notescaped/download/@scoped/notescaped-1.0.0.tgz|@scoped",
            "http://foo.bar:80/not@scope/pkg|''",
    })
    void matchesScopeCorrectly(String pathname, String scope) {
        assertThat(YarnClassicOptions.getScope(pathname)).isEqualTo(scope);
    }

    /**
     * yarn 1.22.22's __tests__/registries/npm-registry.js, environment variables functional test.
     */
    @Test
    void correctlyEscapesEnvironmentConfigVariables() {
        Map<String, String> env = env("npm_config_always_auth", "true", "npm_config__auth", "auth",
                "npm_config__authtoken", "authToken", "npm_config__username", "username", "npm_config__password",
                "password");
        Map<String, @Nullable Object> config = YarnClassicOptions.load((String) null, null, env).getNpmConfig();
        assertThat(config).containsEntry("always-auth", true)
                .containsEntry("_auth", "auth")
                .containsEntry("_authtoken", "authToken")
                .containsEntry("_username", "username")
                .containsEntry("_password", "password");
    }

    private static Map<String, String> env(String... keysAndValues) {
        Map<String, String> env = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            env.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return env;
    }
}
