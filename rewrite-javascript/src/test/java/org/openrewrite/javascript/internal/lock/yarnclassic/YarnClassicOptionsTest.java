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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The config the registries read from the environment, as yarn 1.22.22's own registries answered from its bundle.
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

    private static Map<String, String> env(String... keysAndValues) {
        Map<String, String> env = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            env.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return env;
    }
}
