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
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The credentials bun 1.3.10 sends with the user's .npmrc holding them, recorded against a registry that logs each
 * request's Authorization header: a credential reaches every registry on its host, a later one's winning.
 */
class BunRegistryManifestsTest {

    private static final String NPM = "https://r.example/npm/";
    private static final String CORP = "https://r.example/corp/";

    @Test
    void aTokenReachesTheRegistryItIsFor() {
        NodeRegistry scope = manifests(registry(null, NPM, "user-token"), null).scope("ms", NPM);

        assertThat(scope.getUrl()).isEqualTo(NPM);
        assertThat(scope.getAuthToken()).isEqualTo("user-token");
    }

    // npm sends none there: its credentials are by path
    @Test
    void aTokenReachesAnotherPathOnItsHost() {
        assertThat(manifests(registry(null, NPM, "user-token"), null).scope("ms", "https://r.example/other/")
          .getAuthToken()).isEqualTo("user-token");
    }

    @Test
    void aScopesTokenReachesTheDefaultOnItsHost() {
        BunRegistryManifests manifests = manifests(registry(null, NPM, null), registry("@types", CORP, "corp-token"));

        assertThat(manifests.scope("ms", NPM).getAuthToken()).isEqualTo("corp-token");
        assertThat(manifests.scope("@types/ms", CORP).getAuthToken()).isEqualTo("corp-token");
    }

    @Test
    void theLaterTokenOnAHostWins() {
        BunRegistryManifests manifests = manifests(registry(null, NPM, "npm-token"),
          registry("@types", CORP, "corp-token"));

        assertThat(manifests.scope("ms", NPM).getAuthToken()).isEqualTo("corp-token");
        assertThat(manifests.scope("@types/ms", CORP).getAuthToken()).isEqualTo("corp-token");
    }

    @Test
    void anotherHostGetsNone() {
        NodeRegistry scope = manifests(registry(null, NPM, "user-token"), null)
          .scope("ms", "https://registry.npmmirror.com/");

        assertThat(scope.getUrl()).isEqualTo("https://registry.npmmirror.com/");
        assertThat(scope.getAuthToken()).isNull();
        assertThat(scope.getUsername()).isNull();
    }

    // a host's own key, //r.example/:_authToken, read from the user's .npmrc as the host reads it
    @Test
    void aHostsTokenReachesTheRegistryUnderIt(@TempDir Path home) {
        NodeRegistries user = HostRegistries.fromUserNpmrc("registry=" + NPM + "\n//r.example/:_authToken=host-token\n",
          home);

        assertThat(manifests(user).scope("ms", NPM).getAuthToken()).isEqualTo("host-token");
    }

    private static BunRegistryManifests manifests(NodeRegistry defaultRegistry, @Nullable NodeRegistry scoped) {
        Map<String, NodeRegistry> byScope = new LinkedHashMap<>();
        if (scoped != null) {
            byScope.put(scoped.getScope(), scoped);
        }
        return manifests(new NodeRegistries(defaultRegistry, byScope, null, null, null));
    }

    private static BunRegistryManifests manifests(NodeRegistries registries) {
        return new BunRegistryManifests(registries, new NpmRegistryClient(request -> {
            throw new AssertionError("no request: " + request.getUrl());
        }));
    }

    private static NodeRegistry registry(@Nullable String scope, String url, @Nullable String token) {
        return new NodeRegistry(scope, url, token, null, null, null, false, null, true, false);
    }
}
