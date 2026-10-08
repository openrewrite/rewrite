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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/plugin-npm/tests/NpmSemverFetcher.test.js, every {@code isConventionalTarballUrl} case,
 * npmHttpUtils.test.ts, and the registry a scope resolves to.
 */
class NpmHttpUtilsTest {

    private final Configuration configuration = new Configuration(BerryOptions.defaults(), YarnRelease.of("4.10.3"));
    private final String registry = configuration.getOptions().getString("npmRegistryServer");

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
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"https://mirror.example\"\nnpmScopes:\n  corp:\n    npmAuthToken: x\n"),
                Collections.emptyMap());
        Configuration configuration = new Configuration(options, YarnRelease.of("4.10.3"));
        assertThat(NpmHttpUtils.getScopeRegistry(null, configuration)).isEqualTo("https://mirror.example");
        assertThat(NpmHttpUtils.getScopeRegistry("corp", configuration)).isEqualTo("https://registry.yarnpkg.com");
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", configuration)).isEqualTo("https://npm.jsr.io");
        assertThat(NpmHttpUtils.getIdentUrl(StructUtils.makeIdent("types", "node"))).isEqualTo("/@types%2fnode");
    }

    @Test
    void theJsrScopeIsTheProjectsRegistryBeforeYarn49() {
        BerryOptions options = BerryOptions.load(Collections.singletonMap(".yarnrc.yml",
                "npmRegistryServer: \"https://mirror.example\"\n"), Collections.emptyMap());
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", new Configuration(options, YarnRelease.of("4.8.1"))))
                .isEqualTo("https://mirror.example");
        assertThat(NpmHttpUtils.getScopeRegistry("jsr", new Configuration(options, YarnRelease.of("4.9.0"))))
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
}
