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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The registries' config from the environment and the .npmrc as yarn 1.22.22's bundle answered (the test resources'
 * {@code generate/bundle-rows.js} prints the rows), and the cases of yarn's tests of the registries and env-replace.
 */
class ConfigTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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
            answer = Js.jsonStringify(YarnClassicScenarios.config(Collections.emptyMap(), env).getNpmConfig());
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
        assertThat(YarnClassicScenarios.config(Collections.emptyMap(), env).getRegistry(packageIdent))
          .isEqualTo(registry);
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
        Config options = YarnClassicScenarios.config(npmrc == null ? Collections.emptyMap() :
                Collections.singletonMap(".npmrc", npmrc), env);
        List<String> answers = new ArrayList<>();
        for (String ident : packageIdents.split(" ")) {
            answers.add(options.getRegistry(ident));
        }
        assertThat(String.join(" ", answers)).isEqualTo(registries);
    }

    /**
     * yarn 1.22.22's __tests__/registries/npm-registry.js, getPossibleConfigLocations: the project's directory, the
     * home directory's and each directory above the project's.
     */
    @Test
    void searchesRecursivelyToHomeDirectory() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../../.yarnrc", "");
        assertThat(Config.getPossibleConfigLocations("npmrc", files))
                .containsExactly(".npmrc", "~/.npmrc", ".npmrc", "../.npmrc", "../../.npmrc");
    }

    @Test
    void theNearestConfigFileWins() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".npmrc", "registry=https://a/\n");
        files.put("../.npmrc", "registry=https://b/\n@s:registry=https://b/s/\n");
        files.put("../../.npmrc", "@s:registry=https://c/s/\n@t:registry=https://c/t/\n");
        files.put("../.yarnrc", "registry \"https://y/\"\n");
        files.put("../../.yarnrc", "registry \"https://z/\"\nsave-prefix \"~\"\n");
        Config options = YarnClassicScenarios.config(files, env());
        assertThat(options.getRegistry("x")).isEqualTo("https://a/");
        assertThat(options.getRegistry("@s/x")).isEqualTo("https://b/s/");
        assertThat(options.getRegistry("@t/x")).isEqualTo("https://c/t/");
        assertThat(options.getOption("registry")).isEqualTo("https://y/");
        assertThat(options.getOption("save-prefix")).isEqualTo("~");
        assertThat(options.getNpmConfig()).containsOnlyKeys("@s:registry", "@t:registry", "registry");
    }

    /**
     * rc.js reads each directory's .yarnrc then .yarnrc.yml from the top down: the nearer line wins.
     */
    @Test
    void theNearestFlagLineWins() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../../.yarnrc", "--ignore-engines true\n");
        files.put("../.yarnrc", "--ignore-engines false\n");
        assertThat(flags(files).isIgnoreEngines()).isFalse();

        files.clear();
        files.put("../.yarnrc", "--ignore-engines false\n");
        files.put("../.yarnrc.yml", "\"--ignore-engines\": true\n");
        files.put(".yarnrc", "--ignore-engines true\n");
        assertThat(flags(files).isIgnoreEngines()).isTrue();

        files.clear();
        files.put("../.yarnrc.yml", "\"--registry\": \"https://r/\"\n");
        assertThat(YarnClassicScenarios.config(files, env()).getRegistry("x")).isEqualTo("https://r/");
    }

    /**
     * rc.js {@code getRcConfigForCwd} of yarn 1.22.22's bundle over a project two directories below its repository:
     * the directory keys resolved against their file's directory, here relative to the project.
     */
    static Stream<Arguments> rcDirectoryKeys() {
        return Stream.of(
                rcRow("{yarn-path=.yarn/releases/yarn-1.22.22.cjs}", ".yarnrc",
                        "yarn-path \".yarn/releases/yarn-1.22.22.cjs\""),
                rcRow("{yarn-path=.yarn/releases/yarn-1.22.22.cjs}", ".yarnrc",
                        "yarn-path \"./.yarn/releases/yarn-1.22.22.cjs\""),
                rcRow("{yarn-path=../yarn-wrapper.js}", "../.yarnrc", "yarn-path \"./yarn-wrapper.js\""),
                rcRow("{yarn-path=../../tools/yarn.js}", "../../.yarnrc", "yarn-path \"tools/yarn.js\""),
                rcRow("{yarn-path=../.yarn/releases/yarn-4.10.3.cjs}", "../.yarnrc.yml",
                        "yarnPath: .yarn/releases/yarn-4.10.3.cjs\nnodeLinker: node-modules"),
                rcRow("{yarn-path=x.cjs}", ".yarnrc.yml", "yarnPath: ./x.cjs"),
                rcRow("{yarn-path=../y.js}", "../.yarnrc.yml", "yarn-path: y.js"),
                rcRow("{yarn-path=/opt/yarn/yarn-1.22.22.js}", ".yarnrc", "yarn-path \"/opt/yarn/yarn-1.22.22.js\""),
                rcRow("{yarn-path=../../../x/yarn.js}", ".yarnrc", "yarn-path \"../../../x/yarn.js\""),
                rcRow("{cache-folder=cache}", ".yarnrc", "cache-folder \"./cache/\""),
                rcRow("{--cache-folder=c}", ".yarnrc", "--cache-folder \"c\""),
                rcRow("{--install.modules-folder=../nm}", "../.yarnrc", "--install.modules-folder \"nm\""),
                rcRow("{yarn-path=b.js}", "../.yarnrc", "yarn-path \"a.js\"", ".yarnrc", "yarn-path \"b.js\""),
                rcRow("{yarn-path=b.cjs}", ".yarnrc", "yarn-path \"a.js\"", ".yarnrc.yml", "yarnPath: b.cjs"),
                rcRow("{yarnPath=./z.js}", ".yarnrc", "yarnPath \"./z.js\""),
                rcRow("TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of type string. " +
                      "Received type boolean (true)", ".yarnrc", "yarn-path true"),
                rcRow("TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of type string. " +
                      "Received type number (5)", ".yarnrc", "yarn-path 5"),
                rcRow("TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of type string. " +
                      "Received type boolean (false)", "../.yarnrc", "cache-folder false"),
                rcRow("{yarn-path=.}", ".yarnrc", "yarn-path \"\"")
        );
    }

    private static Arguments rcRow(String rc, String... filesAndTexts) {
        Map<String, String> files = new LinkedHashMap<>();
        for (int i = 0; i < filesAndTexts.length; i += 2) {
            files.put(filesAndTexts[i], filesAndTexts[i + 1] + "\n");
        }
        return Arguments.of(files, rc);
    }

    @ParameterizedTest
    @MethodSource("rcDirectoryKeys")
    void resolvesTheDirectoryKeysAgainstTheirFile(Map<String, String> files, String rc) {
        String actual;
        try {
            actual = Rc.getRcConfigForCwd(files, Collections.emptyMap()).toString();
        } catch (JsError e) {
            actual = e.toString();
        }
        assertThat(actual).isEqualTo(rc);
    }

    /**
     * yarn 1.22.22's __tests__/rc.js, prefixed arguments: a line for another command does not reach install.
     */
    @Test
    void addsCommandNamePrefixedArguments() {
        Install.Flags flags = flags(Collections.singletonMap(".yarnrc",
                "--add.ignore-engines true\n--install.ignore-optional true\n"));
        assertThat(flags.isIgnoreEngines()).isFalse();
        assertThat(flags.isIgnoreOptional()).isTrue();
    }

    /**
     * yarn 1.22.22's __tests__/rc.js, --cwd inside .yarnrc: yarn installs in that directory instead.
     */
    @Test
    void aCwdLineMovesTheInstallElsewhere() {
        assertThatThrownBy(() -> YarnClassicScenarios.config(Collections.singletonMap(".yarnrc", "--cwd foo\n"), env()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("--cwd foo");
    }

    /**
     * yarn 1.22.22's __tests__/util/env-replace.js.
     */
    @Test
    void replacesATokenThatExistsInTheEnvironment() {
        Map<String, String> env = env("a", "token");
        assertThat(Config.envReplace("test ${a} replacement", env)).isEqualTo("test token replacement");
        assertThat(Config.envReplace("${a} replacement", env)).isEqualTo("token replacement");
        assertThat(Config.envReplace("${a}", env)).isEqualTo("token");
    }

    @Test
    void doesNotReplaceATokenThatDoesNotExistInTheEnvironment() {
        assertThatThrownBy(() -> Config.envReplace("${a} replacement", env("b", "token")))
                .isInstanceOf(JsError.class)
                .hasMessage("Failed to replace env in config: ${a}");
    }

    @Test
    void doesNotReplaceATokenPrefixedWithABackslash() {
        assertThat(Config.envReplace("\\${a} replacement", env("a", "token")))
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
        assertThat(Config.getScope(pathname)).isEqualTo(scope);
    }

    static Stream<Arguments> requestAuthorizations() throws IOException {
        List<Arguments> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(requireNonNull(
                ConfigTest.class.getResourceAsStream("request-authorization.jsonl")), StandardCharsets.UTF_8))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                JsonNode row = JSON.readTree(line);
                rows.add(Arguments.of(row.get("npmrc").asText(), row.get("name").asText(), row.get("url").asText(),
                        row.get("authorization").isNull() ? null : row.get("authorization").asText()));
            }
        }
        return rows.stream();
    }

    /**
     * The Authorization header yarn 1.22.22's own NpmRegistry.request sent for each .npmrc, package and url (the test
     * resources' {@code generate/request-authorization.js}): unscoped packages only under always-auth.
     */
    @ParameterizedTest(name = "{1} {2}")
    @MethodSource("requestAuthorizations")
    void sendsTheCredentialsYarnSends(String npmrc, String name, String url, @Nullable String authorization) {
        Config config = YarnClassicScenarios.config(Collections.singletonMap(".npmrc", npmrc), env());
        assertThat(config.getAuthorization(NpmRegistry.escapeName(name), url)).isEqualTo(authorization);
    }

    /**
     * yarn 1.22.22's __tests__/registries/npm-registry.js, environment variables functional test.
     */
    @Test
    void correctlyEscapesEnvironmentConfigVariables() {
        Map<String, String> env = env("npm_config_always_auth", "true", "npm_config__auth", "auth",
                "npm_config__authtoken", "authToken", "npm_config__username", "username", "npm_config__password",
                "password");
        Map<String, @Nullable Object> config = YarnClassicScenarios.config(Collections.emptyMap(), env).getNpmConfig();
        assertThat(config).containsEntry("always-auth", true)
                .containsEntry("_auth", "auth")
                .containsEntry("_authtoken", "authToken")
                .containsEntry("_username", "username")
                .containsEntry("_password", "password");
    }

    // normalizeFlags of the flags the .yarnrc chain gives an install, as Install reads them
    private static Install.Flags flags(Map<String, String> configFiles) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .configFiles(configFiles).env(env()).build();
        Map<String, @Nullable Object> flags = YarnClassicLockRegenerator.start(inputs);
        return new Install.Flags(new Config(inputs, flags), flags);
    }

    private static Map<String, String> env(String... keysAndValues) {
        Map<String, String> env = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            env.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return env;
    }
}
