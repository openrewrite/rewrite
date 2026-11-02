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
package org.openrewrite.javascript.internal.registry;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.NodeRegistry;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class NodeRegistriesTest {

    private static final NodeRegistries REGISTRIES = new NodeRegistries(
            new NodeRegistry(null, "https://registry.example.com/npm/", "default-token", null, null, null, false,
                    null, false, false),
            singletonMap("@corp", new NodeRegistry("@corp", "https://corp.example.com", "corp-token", null, null,
                    null, false, null, true, false)),
            null, null, null);

    /**
     * npm 11.17.0's getAuth picks credentials by the request's URL; pnpm's getAuthHeaderByURI does too. Where npm
     * sends the default registry's to the same host outside its path, the host's go nowhere but under a registry.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "lodash|https://registry.example.com/npm|default-token|https://registry.example.com/npm/",
            "lodash|https://registry.example.com/npm/|default-token|https://registry.example.com/npm/",
            "lodash|https://registry.example.com/npm/lodash/-/lodash-4.17.21.tgz|default-token|" +
            "https://registry.example.com/npm/lodash/-/lodash-4.17.21.tgz",
            "lodash|https://registry.example.com/npm/mirror/|default-token|https://registry.example.com/npm/mirror/",
            "lodash|https://registry.example.com/npm-other/||https://registry.example.com/npm-other/",
            "lodash|https://registry.example.com/||https://registry.example.com/",
            "lodash|https://registry.yarnpkg.com||https://registry.yarnpkg.com",
            "@corp/a|https://corp.example.com/|corp-token|https://corp.example.com",
            "@corp/a|https://registry.example.com/npm/|default-token|https://registry.example.com/npm/",
            "lodash|https://corp.example.com/lodash/-/lodash-4.17.21.tgz|corp-token|" +
            "https://corp.example.com/lodash/-/lodash-4.17.21.tgz",
    })
    void byTheRequestsUrl(String name, String url, @Nullable String token, String requestedFrom) {
        assertSends(REGISTRIES.registryAt(name, url), name, token, requestedFrom);
    }

    // npm 11.17.0's getAuth: a default registry without credentials under one with them sends the outer one's
    @Test
    void aRegistryWithoutCredentialsUnderOneWithThemSendsTheOuterOnes() {
        NodeRegistries nested = new NodeRegistries(
                new NodeRegistry(null, "https://r.example.com/npm/", null, null, null, null, false, null, true, false),
                singletonMap("@corp", new NodeRegistry("@corp", "https://r.example.com/", "corp-token", null, null,
                        null, false, null, true, false)),
                null, null, null);
        assertSends(nested.registryAt("lodash", "https://r.example.com/npm/lodash"), "lodash", "corp-token",
                "https://r.example.com/npm/lodash");
    }

    /**
     * The Yarn and Bun adapters' rule: the registry the package's scope names, its credentials only to its URLs.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "lodash|https://registry.example.com/npm/|default-token|https://registry.example.com/npm/",
            "lodash|https://registry.example.com/npm/lodash/-/lodash-4.17.21.tgz|default-token|" +
            "https://registry.example.com/npm/lodash/-/lodash-4.17.21.tgz",
            "lodash|https://registry.example.com/npm-other/||https://registry.example.com/npm-other/",
            "@corp/a|https://corp.example.com/|corp-token|https://corp.example.com",
            "@corp/a|https://registry.example.com/npm/||https://registry.example.com/npm/",
            "lodash|https://corp.example.com/lodash/-/lodash-4.17.21.tgz||" +
            "https://corp.example.com/lodash/-/lodash-4.17.21.tgz",
    })
    void byThePackagesScope(String name, String url, @Nullable String token, String requestedFrom) {
        assertSends(REGISTRIES.registryFor(name, url), name, token, requestedFrom);
    }

    private static void assertSends(NodeRegistry registry, String name, @Nullable String token, String requestedFrom) {
        assertThat(registry.getAuthToken()).isEqualTo(token);
        assertThat(registry.getUrl()).isEqualTo(requestedFrom);
        NodeRegistry answering = "corp-token".equals(token) ? REGISTRIES.getByScope().get("@corp") :
                "default-token".equals(token) ? REGISTRIES.getDefaultRegistry() : REGISTRIES.registryFor(name);
        assertThat(registry.isStrictSsl()).isEqualTo(answering.isStrictSsl());
    }
}
