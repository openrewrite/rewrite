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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecUtilsTest {

    private static final HostEnvironment NO_ENV = HostEnvironment.of(Collections.emptyMap());

    /**
     * parsePackageJSON with devEngines.packageManager: the spec it reads, or the usage error corepack 0.35.0 stops on
     * ({@code open-corepack-devengines-mismatch}); a warning leaves {@code packageManager} as it is.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"packageManager\":\"yarn@4.10.3\"}|yarn@4.10.3",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"4.10.3\"}}}|yarn@4.10.3",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"}}}|yarn@*",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"," +
            "\"version\":\"^4.10.0\"}}}|yarn@4.10.3",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"," +
            "\"version\":\"4.5.3\"}}}|\"packageManager\" field is set to \"yarn@4.10.3\" which does not match the " +
            "value defined in \"devEngines.packageManager\" for \"yarn\" of \"4.5.3\" (corepack)",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"}}}|" +
            "\"packageManager\" field is set to \"yarn@4.10.3\" which does not match the " +
            "\"devEngines.packageManager\" field set to \"pnpm\" (corepack)",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"," +
            "\"onFail\":\"warn\"}}}|yarn@4.10.3",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"," +
            "\"onFail\":\"ignore\"}}}|yarn@4.10.3",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"@yarn\"}}}|The value of devEngines.packageManager.name " +
            "\"@yarn\" is not a supported string value (corepack)",
            "{\"devEngines\":{\"packageManager\":{\"version\":\"4\"}}}|The value of devEngines.packageManager.name " +
            "undefined is not a supported string value (corepack)",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"^^1\"}}}|The value of " +
            "devEngines.packageManager.version \"^^1\" is not a valid semver range (corepack)",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":\"yarn\"}}|yarn@4.10.3",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":[{\"name\":\"pnpm\"}]}}|" +
            "yarn@4.10.3"
    })
    void readsTheSpecAPackageJsonNames(String manifest, String spec) {
        String answer;
        try {
            Object raw = SpecUtils.parsePackageJSON(EcmaScript.jsonParse(manifest));
            answer = raw == EcmaScript.UNDEFINED ? "undefined" : String.valueOf(raw);
        } catch (Rejected e) {
            answer = e.getMessage();
        }
        assertThat(answer).isEqualTo(spec);
    }

    /**
     * loadSpec walks from where the package manager runs up through every directory the host holds, past one without
     * packageManager ({@code open-member-yarnrc-run-in-member}) and above the project's own.
     */
    @Test
    void walksUpPastAPackageJsonWithoutPackageManager() {
        Map<String, String> manifests = new LinkedHashMap<>();
        manifests.put("packages/a", "{\"name\":\"a\"}");
        manifests.put("", "{\"name\":\"root\"}");
        manifests.put("../..", "{\"packageManager\":\"pnpm@9.15.0\"}");

        SpecUtils.Lookup lookup = SpecUtils.loadSpec("packages/a", 2, manifests::get);

        assertThat(lookup.getTarget()).isEqualTo("../../package.json");
        assertThat(lookup.getSource()).isEqualTo("../../../../package.json");
        assertThat(lookup.getRawPmSpec()).isEqualTo("pnpm@9.15.0");
        assertThat(SpecUtils.loadSpec("packages/a", 1, manifests::get).getRawPmSpec()).isEqualTo(EcmaScript.UNDEFINED);
    }

    /**
     * findProjectSpec refuses a project that names another package manager ({@code open-corepack-member-names-pnpm})
     * unless COREPACK_ENABLE_STRICT is 0, and falls back when none is named.
     */
    @Test
    void refusesAProjectThatNamesAnotherPackageManager() {
        Map<String, String> manifests = new LinkedHashMap<>();
        manifests.put("packages/a", "{\"name\":\"a\",\"packageManager\":\"pnpm@9.15.0\"}");
        manifests.put("", "{\"packageManager\":\"yarn@4.10.3\"}");

        assertThatThrownBy(() -> SpecUtils.findProjectSpec("packages/a", 0, manifests::get, "yarn",
                HostEnvironment.of(Collections.emptyMap())))
                .isInstanceOf(Rejected.class)
                .hasMessage("This project is configured to use pnpm because packages/a/package.json has a " +
                            "\"packageManager\" field (corepack)");
        assertThat(SpecUtils.findProjectSpec("packages/a", 0, manifests::get, "yarn",
                HostEnvironment.of(Collections.singletonMap("COREPACK_ENABLE_STRICT", "0")))).isNull();
        assertThat(SpecUtils.findProjectSpec("", 0, manifests::get, "yarn", HostEnvironment.of(Collections.emptyMap())))
                .isEqualTo(new SpecUtils.Descriptor("yarn", "4.10.3"));
        assertThat(SpecUtils.findProjectSpec("", 0, dir -> nothing(), "yarn",
                HostEnvironment.of(Collections.emptyMap()))).isNull();
    }

    /**
     * parseSpec with an exact version enforced, as findProjectSpec runs it: {@code yarn@1} is corepack's usage error
     * ({@code open-corepack-member-names-range} for a range), a URL for a known package manager too.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "yarn@4.10.3|yarn 4.10.3",
            "yarn@4.10.3+sha224.abc|yarn 4.10.3+sha224.abc",
            "yarn@1|Invalid package manager specification in package.json (yarn@1); expected a semver version " +
            "(corepack)",
            "yarn@^4.10.0|Invalid package manager specification in package.json (yarn@^4.10.0); expected a semver " +
            "version (corepack)",
            "yarn|No version specified for yarn in \"packageManager\" of package.json (corepack)",
            "bun@1.3.10|Unsupported package manager specification (bun@1.3.10) (corepack)",
            "yarn@https://example.com/yarn.tgz|Illegal use of URL for known package manager. Instead, select a " +
            "specific version, or set COREPACK_ENABLE_UNSAFE_CUSTOM_URLS=1 in your environment " +
            "(yarn@https://example.com/yarn.tgz) (corepack)"
    })
    void parsesTheSpecCorepackRuns(String raw, String answer) {
        String actual;
        try {
            SpecUtils.Descriptor spec = SpecUtils.parseSpec(raw, "package.json", true, NO_ENV);
            actual = spec.getName() + " " + spec.getRange();
        } catch (Rejected e) {
            actual = e.getMessage();
        }
        assertThat(actual).isEqualTo(answer);
    }

    /**
     * What {@code corepack yarn --version} runs over each package.json (corepack 0.35.0, cache empty or not): the
     * release installVersion starts, or corepack's usage error (it names another package manager's file by full path).
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"packageManager\":\"yarn@4.10.3\"}|4.10.3",
            "{\"packageManager\":\"yarn@v4.10.3\"}|4.10.3",
            "{\"packageManager\":\"yarn@4.10.3 \"}|4.10.3",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"4.10.3\"}}}|4.10.3",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"}}}|Invalid package manager specification in " +
            "package.json (yarn@*); expected a semver version (corepack)",
            "{\"devEngines\":{\"packageManager\":{\"name\":\"yarn\",\"version\":\"^4.10.0\"}}}|Invalid package " +
            "manager specification in package.json (yarn@^4.10.0); expected a semver version (corepack)",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"," +
            "\"version\":\"^4.10.0\"}}}|4.10.3",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"," +
            "\"onFail\":\"warn\"}}}|4.10.3",
            "{\"packageManager\":\"yarn@4.10.3\",\"devEngines\":{\"packageManager\":\"yarn\"}}|4.10.3",
            "{\"packageManager\":\"yarn@1\"}|Invalid package manager specification in package.json (yarn@1); " +
            "expected a semver version (corepack)",
            "{\"packageManager\":\"yarn\"}|No version specified for yarn in \"packageManager\" of package.json " +
            "(corepack)",
            "{\"packageManager\":\"bun@1.3.10\"}|Unsupported package manager specification (bun@1.3.10) (corepack)",
            "{\"packageManager\":null}|Invalid package manager specification in package.json; expected a string " +
            "(corepack)",
            "{\"packageManager\":4}|Invalid package manager specification in package.json; expected a string " +
            "(corepack)",
            "{\"packageManager\":\"pnpm@9.15.0\"}|This project is configured to use pnpm because package.json has a " +
            "\"packageManager\" field (corepack)"
    })
    void runsTheReleaseCorepackRuns(String manifest, String answer) {
        String actual;
        try {
            SpecUtils.Descriptor spec = SpecUtils.findProjectSpec("", 0, dir -> dir.isEmpty() ? manifest : null,
                    "yarn", HostEnvironment.of(Collections.emptyMap()));
            actual = spec == null ? "none" : String.valueOf(SpecUtils.installVersion(spec));
        } catch (Rejected e) {
            actual = e.getMessage();
        }
        assertThat(actual).isEqualTo(answer);
    }

    /**
     * The package manager a root names is the spec its lookup finds, packageManager over devEngines; none without one.
     */
    @Test
    void theSpecALookupFindsNamesThePackageManager() {
        String both = "{\"packageManager\":\"yarn@1.22.22\",\"devEngines\":{\"packageManager\":{\"name\":\"yarn\"}}}";
        assertThat(SpecUtils.loadSpec("", 0, dir -> both).getSpec(true, NO_ENV))
                .isEqualTo(new SpecUtils.Descriptor("yarn", "1.22.22"));
        assertThat(SpecUtils.loadSpec("", 0, dir -> "{\"name\":\"p\"}").getSpec(true, NO_ENV)).isNull();
        assertThat(SpecUtils.loadSpec("", 0, dir -> null).getSpec(true, NO_ENV)).isNull();
        assertThatThrownBy(() -> SpecUtils.loadSpec("", 0, dir -> "{\"packageManager\":\"yarn@1\"}")
                .getSpec(true, NO_ENV))
                .isInstanceOf(Rejected.class)
                .hasMessageContaining("expected a semver version");
    }

    /**
     * corepack takes a URL for a known package manager only under COREPACK_ENABLE_UNSAFE_CUSTOM_URLS=1 and runs what
     * it downloads (4.10.3, for repo.yarnpkg.com's yarn.js); {@code true} is refused as an unset variable is.
     */
    @Test
    void aUrlForAKnownPackageManagerNeedsTheUnsafeSetting() {
        String url = "https://repo.yarnpkg.com/4.10.3/packages/yarnpkg-cli/bin/yarn.js";
        SpecUtils.Descriptor spec = SpecUtils.parseSpec("yarn@" + url, "package.json", true,
                HostEnvironment.of(Collections.singletonMap("COREPACK_ENABLE_UNSAFE_CUSTOM_URLS", "1")));
        assertThat(spec).isEqualTo(new SpecUtils.Descriptor("yarn", url));
        assertThat(SpecUtils.installVersion(spec)).isNull();
        assertThatThrownBy(() -> SpecUtils.parseSpec("yarn@" + url, "package.json", true,
                HostEnvironment.of(Collections.singletonMap("COREPACK_ENABLE_UNSAFE_CUSTOM_URLS", "true"))))
                .isInstanceOf(Rejected.class)
                .hasMessageStartingWith("Illegal use of URL for known package manager.");
    }

    /**
     * Under COREPACK_ENABLE_PROJECT_SPEC=0 corepack runs its own default whatever the project names.
     */
    @Test
    void readsNoSpecWithoutTheProjectSpec() {
        assertThat(SpecUtils.findProjectSpec("", 0, dir -> "{\"packageManager\":\"pnpm@9.15.0\"}", "yarn",
                HostEnvironment.of(Collections.singletonMap("COREPACK_ENABLE_PROJECT_SPEC", "0")))).isNull();
    }

    private static @Nullable String nothing() {
        return null;
    }
}
