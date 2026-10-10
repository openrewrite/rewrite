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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The registries bun 1.3.10 asks with the host's registries as the user's .npmrc, recorded with a scratch HOME whose
 * .npmrc names a mirror ({@code lock/bun/open-user-registry-*}).
 */
class BunOptionsTest {

    private static final String MIRROR = "https://registry.npmmirror.com/";
    private static final String OTHER = "https://registry.yarnpkg.com/";

    @Test
    void theUsersNpmrcNamesTheRegistries() {
        BunOptions options = BunOptions.load(null, user(MIRROR, MIRROR), null, HostEnvironment.UNKNOWN);

        assertThat(options.getRegistry()).isEqualTo(MIRROR);
        assertThat(options.scopeForPackageName("@types/ms")).isEqualTo(MIRROR);
        assertThat(BunOptions.load(null, user(BunOptions.DEFAULT_REGISTRY, MIRROR), null, HostEnvironment.UNKNOWN)
          .scopeForPackageName("@types/ms")).isEqualTo(MIRROR);
    }

    @Test
    void theProjectsNpmrcSetsEachKeyOverIt() {
        BunOptions options = BunOptions.load(null, user(MIRROR, MIRROR), "registry=" + OTHER + "\n",
          HostEnvironment.UNKNOWN);

        assertThat(options.getRegistry()).isEqualTo(OTHER);
        assertThat(options.scopeForPackageName("@types/ms")).isEqualTo(MIRROR);
    }

    // the user's .npmrc loads after bunfig.toml
    @Test
    void itNamesTheRegistryOverTheProjectsBunfig() {
        String bunfig = "[install]\nregistry = \"" + OTHER + "\"\n";

        assertThat(BunOptions.load(bunfig, user(MIRROR, null), null, HostEnvironment.UNKNOWN).getRegistry())
          .isEqualTo(MIRROR);
    }

    // the host's registries cannot tell a user .npmrc naming npm's own registry from one naming none
    @Test
    void aHostDefaultOfNpmsOwnRegistryNamesNone() {
        String bunfig = "[install]\nregistry = \"" + OTHER + "\"\n";

        assertThat(BunOptions.load(bunfig, user(BunOptions.DEFAULT_REGISTRY, null), null, HostEnvironment.UNKNOWN)
          .getRegistry()).isEqualTo(OTHER);
    }

    private static NodeRegistries user(String registry, @Nullable String types) {
        Map<String, NodeRegistry> byScope = new LinkedHashMap<>();
        if (types != null) {
            byScope.put("@types", registry("@types", types));
        }
        return new NodeRegistries(registry(null, registry), byScope, null, null, null);
    }

    private static NodeRegistry registry(@Nullable String scope, String url) {
        return new NodeRegistry(scope, url, null, null, null, null, false, null, true, false);
    }
}
