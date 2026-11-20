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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * yarn 1.22.22's __tests__/package-resolver.js where it needs no network, and the find loop over root requests: what a
 * request resolves to, what it joins, and when the order the registry answers in decides what yarn writes.
 */
class PackageResolverTest {

    private static final String HEADER = "# yarn lockfile v1\n\n\n";

    @Test
    void addPatternDoesNotAddDuplicates() {
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), new Lockfile());
        Manifest manifest = new Manifest();
        manifest.put("name", "name");
        resolver.addPattern("patternOne", manifest);
        resolver.addPattern("patternTwo", manifest);
        resolver.addPattern("patternOne", manifest);
        assertThat(resolver.patternsByPackage.get("name")).isEqualTo(asList("patternOne", "patternTwo"));
    }

    /**
     * A version the registry lists without a tarball has no remote, which package-request.js asserts.
     */
    @Test
    void aVersionWithoutATarballHasNoRemote() {
        Map<String, String> registry = new LinkedHashMap<>();
        registry.put("a", packument("a", "1.0.0", null, "\"version\": \"1.0.0\""));
        assertThatThrownBy(() -> resolve(HEADER, registry, "a@^1.0.0"))
                .isInstanceOfSatisfying(JsError.class, e -> {
                    assertThat(e.getName()).isEqualTo(Js.INVARIANT_VIOLATION);
                    assertThat(e.getMessage()).isEqualTo("Missing remote");
                });
    }

    /**
     * node-semver 5.5.0's {@code maxSatisfying} over the versions as manifests hold them: {@code Range.test} fails a
     * falsy version and throws {@code Invalid Version} for one that does not parse.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "1.2.3|1.2.3",
            "null|null",
            "''|null",
            "1.0|TypeError: Invalid Version: 1.0",
    })
    void getHighestRangeVersionMatchReadsVersionsAsSemverDoes(@Nullable String version, @Nullable String outcome) {
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), new Lockfile());
        Manifest manifest = new Manifest();
        manifest.put("name", "p");
        manifest.put("version", version);
        resolver.addPattern("p@x", manifest);
        String answer;
        try {
            Manifest match = resolver.getHighestRangeVersionMatch("p", "1.2.3", null);
            answer = match == null ? null : String.valueOf(match.getVersion());
        } catch (JsError e) {
            answer = e.getName() + ": " + e.getMessage();
        }
        assertThat(answer).isEqualTo(outcome);
    }

    /**
     * Two patterns of one name resolve to one version from different remotes in one root request: under a locked parent
     * the lock's remote is written, under a parent resolved anew it races the registry's.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "locked parent|true|",
            "parent resolved anew|false|m@^1.0.0 (locked at 1.0.0, under a@^1.0.0) and m@1.0.0",
    })
    void reportsTheEntryTwoPatternsRaceFor(String label, boolean parentLocked, @Nullable String race) {
        String lock = HEADER +
                      (parentLocked ? "a@^1.0.0:\n  version \"1.0.0\"\n" +
                                      "  resolved \"https://registry.npmjs.org/a/-/a-1.0.0.tgz#a\"\n" +
                                      "  dependencies:\n    m \"^1.0.0\"\n\n" : "") +
                      "m@^1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.npmjs.org/m/-/m-1.0.0.tgz#m\"\n";
        Map<String, String> registry = new LinkedHashMap<>();
        registry.put("a", packument("a", "1.0.0", "https://registry.npmjs.org/a/-/a-1.0.0.tgz",
                "\"dependencies\": {\"m\": \"^1.0.0\"}"));
        registry.put("b", packument("b", "1.0.0", "https://registry.npmjs.org/b/-/b-1.0.0.tgz",
                "\"dependencies\": {\"m\": \"1.0.0\"}"));
        registry.put("c", packument("c", "1.0.0", "https://registry.npmjs.org/c/-/c-1.0.0.tgz",
                "\"dependencies\": {\"a\": \"^1.0.0\", \"b\": \"^1.0.0\"}"));
        registry.put("m", packument("m", "1.0.0", "https://registry.npmjs.org/m/-/m-1.0.0.tgz", ""));
        PackageResolver resolver = resolve(lock, registry, "c@^1.0.0");
        if (race == null) {
            assertThat(resolver.races).isEmpty();
        } else {
            assertThat(resolver.races).singleElement().asString().startsWith(race);
        }
    }

    private static PackageResolver resolve(String lock, Map<String, String> registry, String... roots) {
        Config options = YarnClassicScenarios.defaults();
        PackageResolver resolver = new PackageResolver(options, Lockfile.fromDirectory(lock),
                new ResolutionMap(options), YarnClassicScenarios.packuments((url, name) -> registry.get(name)),
                new ProjectFiles(Collections.emptyMap(), null), Host.UNKNOWN);
        List<DependencyRequestPattern> requests = new ArrayList<>();
        for (String root : roots) {
            requests.add(DependencyRequestPattern.builder().pattern(root).registry(NpmResolver.NPM_REGISTRY_ID)
                    .build());
        }
        resolver.init(requests, false, false, null);
        return resolver;
    }

    private static String packument(String name, String version, @Nullable String tarball, String fields) {
        String dist = tarball == null ? "" : ", \"dist\": {\"tarball\": \"" + tarball + "\", \"shasum\": \"" + name +
                                             "\"}";
        return "{\"name\": \"" + name + "\", \"dist-tags\": {\"latest\": \"" + version + "\"}, \"versions\": {\"" +
               version + "\": {\"name\": \"" + name + "\", \"version\": \"" + version + "\"" +
               (fields.isEmpty() ? "" : ", " + fields) + dist + "}}}";
    }
}
