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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/plugin-npm/tests/NpmSemverFetcher.test.js, every {@code isConventionalTarballUrl} case,
 * npmHttpUtils.test.ts, and the registry a scope resolves to.
 */
class NpmHttpUtilsTest {

    private final Configuration configuration = BerryScenarios.restricted(Configuration.defaults(),
      YarnRelease.of("4.10.3"));
    private final String registry = configuration.getString("npmRegistryServer");

    @Test
    void detectsAConventionalPath() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent(null, "foo"), "npm:1.0.0");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, registry + "/foo/-/foo-1.0.0.tgz", configuration))
                .isTrue();
    }

    @Test
    void detectsAConventionalScopedPath() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent("scope", "foo"), "npm:1.0.0");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, registry + "/@scope/foo/-/foo-1.0.0.tgz",
                configuration)).isTrue();
    }

    @Test
    void detectsAConventionalEncodedScopedPath() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent("scope", "foo"), "npm:1.0.0");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, registry + "/@scope%2ffoo/-/foo-1.0.0.tgz",
                configuration)).isTrue();
    }

    @Test
    void detectsANonConventionalRegistry() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent(null, "foo"), "npm:1.0.0");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, "https://not-the-right-registry/foo/-/foo-1.0.0.tgz",
                configuration)).isFalse();
    }

    @Test
    void detectsANonConventionalPath() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent(null, "foo"), "npm:1.0.0");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, registry + "/archives/foo/foo-1.0.0.tgz",
                configuration)).isFalse();
    }

    @Test
    void takesTheNpmjsHostAsYarnpkg() {
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent(null, "ms"), "npm:2.1.3");
        assertThat(NpmHttpUtils.isConventionalTarballUrl(locator, "http://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
                configuration)).isTrue();
    }

    @Test
    void aScopeEntryDefaultsToThePublicRegistry() {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"https://mirror.example\"\nnpmScopes:\n  corp:\n    npmAuthToken: x\n"),
                HostEnvironment.of(Collections.emptyMap()));
        Configuration configuration = BerryScenarios.restricted(options, YarnRelease.of("4.10.3"));
        assertThat(NpmHttpUtils.getScopeRegistry(null, configuration)).isEqualTo("https://mirror.example");
        assertThat(NpmHttpUtils.getScopeRegistry("corp", configuration)).isEqualTo("https://registry.yarnpkg.com");
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", configuration)).isEqualTo("https://npm.jsr.io");
        assertThat(NpmHttpUtils.getIdentUrl(StructUtils.makeIdent("types", "node"))).isEqualTo("/@types%2fnode");
    }

    @Test
    void theJsrScopeIsTheProjectsRegistryBeforeYarn49() {
        Configuration options = Configuration.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"https://mirror.example\"\n"), HostEnvironment.of(Collections.emptyMap()));
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", BerryScenarios.restricted(options, YarnRelease.of("4.8.1"))))
                .isEqualTo("https://mirror.example");
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", BerryScenarios.restricted(options, YarnRelease.of("4.9.0"))))
                .isEqualTo("https://npm.jsr.io");
    }

    /**
     * packages/plugin-npm/tests/npmHttpUtils.test.ts: {@code get} requests the normalized registry followed by the
     * path, which is what {@link RegistrySource} is handed.
     */
    @Test
    void craftsTheFinalPathCorrectly() {
        for (String registry : new String[]{"https://example.org", "https://example.org/", "https://example.org/foo",
                "https://example.org/foo/"}) {
            assertThat(NpmHttpUtils.normalizeRegistry(registry) + "/bar").as(registry)
                    .isEqualTo(registry.replaceAll("/+$", "") + "/bar");
        }
    }

    /**
     * The Authorization header Yarn 4.10.3 sent for ms and @types/ms, metadata and tarball alike, under each home
     * .yarnrc.yml, recorded through a registry logging the header: a scoped package's on a best effort.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "npmAuthToken: top-token|-|Bearer top-token",
            "npmAuthToken: top-token\\nnpmAlwaysAuth: true|Bearer top-token|Bearer top-token",
            "npmRegistries:\\n  'http://127.0.0.1:4893':\\n    npmAuthToken: entry-token\\n    npmAlwaysAuth: true|" +
            "Bearer entry-token|Bearer entry-token",
            "npmRegistries:\\n  '//127.0.0.1:4893':\\n    npmAuthIdent: 'u:p'\\n    npmAlwaysAuth: true|Basic dTpw|" +
            "Basic dTpw",
            "npmScopes:\\n  types:\\n    npmRegistryServer: 'http://127.0.0.1:4893'\\n" +
            "    npmAuthToken: scope-token|-|Bearer scope-token"
    })
    void sendsTheCredentialsYarnSends(String settings, @Nullable String unscoped, @Nullable String scoped) {
        Configuration configuration = BerryScenarios.restricted(Configuration.load(Collections.singletonMap(
                ".yarnrc.yml", "npmRegistryServer: 'http://127.0.0.1:4893'\n" + settings.replace("\\n", "\n") +
                               "\n"), HostEnvironment.of(Collections.emptyMap())), YarnRelease.of("4.10.3"));
        String registry = NpmHttpUtils.getScopeRegistry(null, configuration);
        assertThat(NpmHttpUtils.getAuthenticationHeader(registry, null, configuration)).isEqualTo(unscoped);
        assertThat(NpmHttpUtils.getAuthenticationHeader(NpmHttpUtils.getScopeRegistry("types", configuration),
                "types", configuration)).isEqualTo(scoped);
    }

    /**
     * As above, a project and a home .yarnrc.yml: npmRegistries' keys lose a trailing slash (normalizeKeys), an entry
     * stands whole for its registry, and the files merge an entry under its key as written.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "npmRegistryServer: 'http://127.0.0.1:4897'\\nnpmRegistries:\\n  'http://127.0.0.1:4897/':\\n" +
            "    npmAuthToken: entry-token\\n    npmAlwaysAuth: true|-|Bearer entry-token|Bearer entry-token",
            "npmRegistryServer: 'http://127.0.0.1:4897'\\nnpmRegistries:\\n  '//127.0.0.1:4897/':\\n" +
            "    npmAuthToken: entry-token\\n    npmAlwaysAuth: true|-|Bearer entry-token|Bearer entry-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'\\nnpmRegistries:\\n  '//127.0.0.1:4898':\\n" +
            "    npmAuthToken: first-token\\n    npmAlwaysAuth: true\\n  '//127.0.0.1:4898/':\\n" +
            "    npmAuthToken: second-token\\n    npmAlwaysAuth: true|-|Bearer second-token|Bearer second-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'\\nnpmRegistries:\\n  '//127.0.0.1:4898/':\\n" +
            "    npmAuthToken: project-token|npmRegistries:\\n  '//127.0.0.1:4898':\\n    npmAuthToken: home-token\\n" +
            "    npmAlwaysAuth: true|-|Bearer project-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'\\nnpmRegistries:\\n  '//127.0.0.1:4898':\\n" +
            "    npmAuthToken: project-token|npmRegistries:\\n  '//127.0.0.1:4898':\\n    npmAuthToken: home-token\\n" +
            "    npmAlwaysAuth: true|Bearer project-token|Bearer project-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'\\nnpmAlwaysAuth: true|npmRegistries:\\n" +
            "  '//127.0.0.1:4898':\\n    npmAuthToken: home-token|-|Bearer home-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'|npmRegistries:\\n  '//127.0.0.1:4898':\\n" +
            "    npmAuthToken: home-token\\n    npmAlwaysAuth: true|Bearer home-token|Bearer home-token",
            "npmRegistryServer: 'http://127.0.0.1:4898'\\nnpmRegistries:\\n  '//127.0.0.1:4898':\\n" +
            "    npmAlwaysAuth: false|npmRegistries:\\n  '//127.0.0.1:4898':\\n    npmAuthToken: home-token\\n" +
            "    npmAlwaysAuth: true|-|Bearer home-token"
    })
    void readsTheRegistriesEntriesAsYarnDoes(String project, @Nullable String home, @Nullable String unscoped,
                                             @Nullable String scoped) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".yarnrc.yml", project.replace("\\n", "\n") + "\n");
        if (home != null) {
            files.put("~/.yarnrc.yml", home.replace("\\n", "\n") + "\n");
        }
        Configuration configuration = BerryScenarios.restricted(Configuration.load(files,
                HostEnvironment.of(Collections.emptyMap())), YarnRelease.of("4.10.3"));
        String registry = NpmHttpUtils.getScopeRegistry(null, configuration);
        assertThat(NpmHttpUtils.getAuthenticationHeader(registry, null, configuration)).isEqualTo(unscoped);
        assertThat(NpmHttpUtils.getAuthenticationHeader(registry, "types", configuration)).isEqualTo(scoped);
    }

    /**
     * npmAlwaysAuth without credentials: Yarn 4.10.3 fails the unscoped request with YN0033.
     */
    @Test
    void alwaysAuthWithoutCredentialsIsYarnsError() {
        Configuration configuration = BerryScenarios.restricted(Configuration.load(Collections.singletonMap(
                ".yarnrc.yml", "npmAlwaysAuth: true\n"), HostEnvironment.of(Collections.emptyMap())),
                YarnRelease.of("4.10.3"));
        assertThatThrownBy(() -> NpmHttpUtils.getAuthenticationHeader(registry, null, configuration))
                .isInstanceOf(ReportError.class)
                .hasMessage("No authentication configured for request");
        assertThat(NpmHttpUtils.getAuthenticationHeader(registry, "types", configuration)).isNull();
    }
}
