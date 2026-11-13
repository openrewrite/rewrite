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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/registries/npm-registry.js, getRequestUrl functional test.
 */
class NpmRegistryTest {

    @Test
    void returnsPathnameWhenItIsAFullUrl() {
        String fullUrl = "HTTP://xn--xample-hva.com:80/foo/bar/baz";
        assertThat(NpmRegistry.getRequestUrl("https://my.registry.co", fullUrl)).isEqualTo(fullUrl);
    }

    @Test
    void correctlyHandlesRegistriesLackingATrailingSlash() {
        assertThat(NpmRegistry.getRequestUrl("https://my.registry.co/registry", "foo/bar/baz"))
                .isEqualTo("https://my.registry.co/registry/foo/bar/baz");
    }

    @ParameterizedTest
    @ValueSource(strings = {"registry.yarnpkg.com", "registry.npmjs.org", "registry.npmjs.com"})
    void enforcesLoadingPackagesThroughHttps(String host) {
        assertThat(NpmRegistry.getRequestUrl("http://" + host + "/registry", "foo/bar/baz"))
                .isEqualTo("https://" + host + "/registry/foo/bar/baz");
    }

    @Test
    void doesNotChangeTheProtocolForPackagesFromOtherRegistries() {
        assertThat(NpmRegistry.getRequestUrl("http://registry.mylittlepony.org/registry", "foo/bar/baz"))
                .isEqualTo("http://registry.mylittlepony.org/registry/foo/bar/baz");
    }

    @Test
    void escapesTheScopeSeparatorOnce() {
        assertThat(NpmRegistry.escapeName("@scope/name")).isEqualTo("@scope%2fname");
        assertThat(NpmRegistry.escapeName("name")).isEqualTo("name");
    }
}
