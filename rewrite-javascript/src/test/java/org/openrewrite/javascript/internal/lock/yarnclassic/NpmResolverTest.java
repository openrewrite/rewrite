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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The version a range picks from a packument, and the remote a picked version becomes; the test resources'
 * {@code generate/bundle-rows.js} prints the rows that hold yarn 1.22.22's own answers.
 */
class NpmResolverTest {

    private static final String VERSIONS = "{\"1.0.0\": {\"name\": \"p\", \"version\": \"1.0.0\"}, " +
                                           "\"1.2.0\": {\"name\": \"p\", \"version\": \"1.2.0\"}}";

    /**
     * A packument without a latest tag, recorded with yarn 1.22.22: it locked {@code left-pad@latest} at 1.2.0, the
     * last version key.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "latest, 1.2.0",
            "^1.1.0, 1.3.0",
            "1.1.x, 1.1.3",
    })
    void picksAVersionWithoutALatestTag(String range, String version) {
        Object picked = NpmResolver.findVersionInRegistryResponse(YarnClassicScenarios.defaults(), "left-pad", range,
                packument("{\"name\": \"left-pad\", \"dist-tags\": {}, \"versions\": {" +
                          "\"1.1.3\": {\"name\": \"left-pad\", \"version\": \"1.1.3\"}, " +
                          "\"1.3.0\": {\"name\": \"left-pad\", \"version\": \"1.3.0\"}, " +
                          "\"1.2.0\": {\"name\": \"left-pad\", \"version\": \"1.2.0\"}}}"));
        assertThat(Js.get(picked, "version")).isEqualTo(version);
    }

    /**
     * Malformed packuments, as yarn 1.22.22's own {@code findVersionInRegistryResponse} answered them.
     */
    static Stream<Arguments> malformed() {
        return Stream.of(
                Arguments.of("dist-tags a string", "\"latest\"", VERSIONS, "^1.0.0",
                        "TypeError: Cannot use 'in' operator to search for '^1.0.0' in latest"),
                Arguments.of("dist-tags a number", "5", VERSIONS, "^1.0.0",
                        "TypeError: Cannot use 'in' operator to search for '^1.0.0' in 5"),
                Arguments.of("dist-tags true", "true", VERSIONS, "^1.0.0",
                        "TypeError: Cannot use 'in' operator to search for '^1.0.0' in true"),
                Arguments.of("dist-tags an array", "[\"1.0.0\"]", VERSIONS, "0", "version 1.0.0"),
                Arguments.of("an array's length", "[\"1.0.0\"]", VERSIONS, "length",
                        "Couldn't find any versions for \"p\" that matches 1"),
                Arguments.of("an array's map", "[\"1.0.0\"]", VERSIONS, "map",
                        "Couldn't find any versions for \"p\" that matches function map() { [native code] }"),
                Arguments.of("an object's toString", "{\"latest\": \"1.2.0\"}", VERSIONS, "toString",
                        "Couldn't find any versions for \"p\" that matches function toString() { [native code] }"),
                Arguments.of("an object's __proto__", "{\"latest\": \"1.2.0\"}", VERSIONS, "__proto__",
                        "Couldn't find any versions for \"p\" that matches {}"),
                Arguments.of("a tag that is a number", "{\"next\": 5}", VERSIONS, "next",
                        "Couldn't find any versions for \"p\" that matches 5"),
                Arguments.of("versions a number", "{}", "5", "^1.0.0",
                        "No valid versions found for \"p\". The package may be unpublished."),
                Arguments.of("versions an empty array", "{}", "[]", "^1.0.0",
                        "No valid versions found for \"p\". The package may be unpublished."),
                Arguments.of("latest a number", "{\"latest\": 5}", VERSIONS, "^1.0.0",
                        "TypeError: Invalid Version: 5"),
                Arguments.of("latest a number, any range", "{\"latest\": 5}", VERSIONS, "*",
                        "TypeError: Cannot read properties of undefined (reading 'length')"),
                Arguments.of("latest a number, two sets", "{\"latest\": 5}", VERSIONS, "1.x || 2.x",
                        "TypeError: Invalid Version: 5"),
                Arguments.of("latest true", "{\"latest\": true}", VERSIONS, "^1.0.0",
                        "TypeError: Invalid Version: true"),
                Arguments.of("latest an object", "{\"latest\": {}}", VERSIONS, ">=1.0.0",
                        "TypeError: Invalid Version: [object Object]"),
                Arguments.of("latest an array", "{\"latest\": [1]}", VERSIONS, "~1",
                        "TypeError: Invalid Version: 1"),
                Arguments.of("latest an array, x", "{\"latest\": [1]}", VERSIONS, "x",
                        "TypeError: Cannot read properties of undefined (reading 'length')"),
                Arguments.of("latest not a version", "{\"latest\": \"garbage\"}", VERSIONS, "^1.0.0",
                        "TypeError: Invalid Version: garbage"),
                Arguments.of("latest a partial version", "{\"latest\": \"1.2\"}", VERSIONS, "*",
                        "TypeError: Invalid Version: 1.2"),
                Arguments.of("latest with =", "{\"latest\": \"=1.2.0\"}", VERSIONS, "1.2.0",
                        "TypeError: Invalid Version: =1.2.0"),
                Arguments.of("versions an array of one", "{}", "[\"1.0.0\"]", "^1.0.0",
                        "TypeError: Invalid Version: 0"),
                Arguments.of("a version key not a version", "{}", "{\"latest\": {\"version\": \"x\"}}", "^1.0.0",
                        "TypeError: Invalid Version: latest")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformed")
    void answersAMalformedPackumentAsYarnDoes(String label, String distTags, String versions, String range,
                                              String outcome) {
        Map<String, Object> body = packument("{\"name\": \"p\", \"dist-tags\": " + distTags + ", \"versions\": " +
                                             versions + "}");
        String answer;
        try {
            answer = "version " + Js.get(NpmResolver.findVersionInRegistryResponse(YarnClassicScenarios.defaults(),
                    "p", range, body), "version");
        } catch (MessageError e) {
            answer = e.getMessage();
        } catch (JsError e) {
            answer = e.getName() + ": " + e.getMessage();
        }
        assertThat(answer).isEqualTo(outcome);
    }

    /**
     * The registry left-pad 1.3.0's {@code resolved} names when yarn 1.22.22 ran live with each variable set
     * ({@code generate/registry-env-rows.js}).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "none, , https://registry.yarnpkg.com",
            "YARN_REGISTRY, https://registry.npmjs.org, https://registry.npmjs.org",
            "yarn_registry, https://registry.npmjs.org, https://registry.npmjs.org",
            "npm_config_registry, https://registry.npmjs.org, https://registry.yarnpkg.com",
            "NPM_CONFIG_REGISTRY, https://registry.npmjs.org, https://registry.yarnpkg.com",
    })
    void resolvesUnderTheRegistryTheEnvironmentNames(String variable, String value, String registry) {
        Map<String, String> env = value == null ? Collections.emptyMap() : Collections.singletonMap(variable, value);
        Manifest manifest = resolve(YarnClassicScenarios.config(Collections.emptyMap(), env), "left-pad@1.3.0",
                "{\"name\": \"left-pad\", \"dist-tags\": {\"latest\": \"1.3.0\"}, \"versions\": {\"1.3.0\": " +
                "{\"name\": \"left-pad\", \"version\": \"1.3.0\", \"dist\": {\"tarball\": " +
                "\"https://registry.npmjs.org/left-pad/-/left-pad-1.3.0.tgz\", " +
                "\"shasum\": \"5b8a3a7765dfe001261dde915589e782f8c94d1e\"}}}}");
        assertThat(requireNonNull(manifest.remote).resolved).isEqualTo(registry + "/left-pad/-/left-pad-1.3.0.tgz" +
                                                                       "#5b8a3a7765dfe001261dde915589e782f8c94d1e");
    }

    /**
     * npm-resolver.js swaps a locked entry's remote for the offline mirror's prebuilt package only when the entry
     * holds this platform's variant and the mirror holds its file, which the engine cannot see.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "no variants||reused",
            "the host's variant|left-pad-v1.3.0-darwin-arm64-137 abc|cannot-know left-pad-v1.3.0-darwin-arm64-137",
            "another platform's variant|left-pad-v1.3.0-linux-x64-137 abc|reused",
            "the host's variant without a mirror|left-pad-v1.3.0-darwin-arm64-137 abc|reused",
    })
    void reusesALockedEntryUnlessTheMirrorMayHoldItsPrebuiltVariant(String label, @Nullable String variant,
                                                                   String outcome) {
        String yarnrc = "experimental-pack-script-packages-in-mirror true\n" +
                        (label.endsWith("without a mirror") ? "" : "yarn-offline-mirror \"./mirror\"\n");
        Config options = YarnClassicScenarios.config(Collections.singletonMap(".yarnrc", yarnrc),
                Collections.emptyMap());
        String lock = "left-pad@1.3.0:\n  version \"1.3.0\"\n  resolved \"https://registry.yarnpkg.com/left-pad/-/" +
                      "left-pad-1.3.0.tgz#5b8a3a7765dfe001261dde915589e782f8c94d1e\"\n" +
                      "  integrity sha1-W4o6d2Xf4AEmHd6RVYnngvjJTR4=\n" +
                      (variant == null ? "" : "  prebuiltVariants:\n    " + variant + "\n");
        PackageResolver resolver = new PackageResolver(options, Lockfile.fromDirectory(lock),
                new ResolutionMap(options), YarnClassicScenarios.NO_REGISTRY,
                new ProjectFiles(Collections.emptyMap(), null), Host.builder().os("darwin").cpu("arm64").build());
        PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern("left-pad@1.3.0")
                .registry(NpmResolver.NPM_REGISTRY_ID).build(), resolver);
        String answer;
        try {
            EventLoop.Promise<Manifest> found = new NpmResolver(request, "left-pad", "1.3.0").resolve();
            resolver.loop.run();
            answer = requireNonNull(found.getValue()).getVersion() != null ? "reused" : "none";
        } catch (CannotKnow e) {
            answer = "cannot-know " + e.getSubject().replaceFirst("^.* prebuilt package (\\S+) .*$", "$1");
        }
        assertThat(answer).isEqualTo(outcome);
    }

    private static Manifest resolve(Config options, String pattern, String packument) {
        PackageResolver resolver = new PackageResolver(options, new Lockfile(), new ResolutionMap(options),
                YarnClassicScenarios.packuments((registry, name) -> packument),
                new ProjectFiles(Collections.emptyMap(), null), Host.UNKNOWN);
        PackageRequest request = new PackageRequest(DependencyRequestPattern.builder().pattern(pattern)
                .registry(NpmResolver.NPM_REGISTRY_ID).build(), resolver);
        NormalizedPattern normalized = NormalizedPattern.normalizePattern(pattern);
        EventLoop.Promise<Manifest> found = new NpmResolver(request, normalized.getName(), normalized.getRange())
                .resolve();
        resolver.loop.run();
        return requireNonNull(found.getValue());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> packument(String json) {
        return (Map<String, Object>) Js.jsonParse(json);
    }
}
