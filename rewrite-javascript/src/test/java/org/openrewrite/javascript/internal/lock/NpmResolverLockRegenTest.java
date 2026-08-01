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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.resolve.NpmResolver;
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for the npm {@link NpmResolver} (ADR 0012). Each test replays a fixture — an edited
 * {@code package.json} and recorded registry HTTP — through {@code NpmResolver.resolve(...)} entirely OFFLINE (a
 * stub {@code HttpSender} serves the captured packuments/manifests under each fixture's {@code http/}), then
 * asserts the lock it produces is BYTE-IDENTICAL to a golden {@code after} recorded from a real
 * {@code npm install --package-lock-only}. The resolver resolves the whole closure from scratch, so the golden is
 * a full lock (not a patch), and byte-identity is the whole contract.
 * <p>
 * The goldens were produced with npm 11.6.2 against registry.npmjs.org; {@link #recordGoldensWithRealNpm()}
 * re-derives and verifies them. The abbreviated packuments ({@code {name, dist-tags, versions:{...:{}}}}) and
 * verbatim single-version manifests under {@code http/} were captured from the registry.
 */
class NpmResolverLockRegenTest extends LockRegenTestSupport {

    @Test
    void cleanClosureV3() {
        // is-odd@3.0.1 -> is-number@6.0.0: both hoist top-level (a flat closure).
        assertResolveByteExact("lock/npm/resolve-clean", "after", null,
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void cleanClosureV2() {
        // The same closure into a lockfileVersion 2 lock adds the legacy `dependencies` tree.
        assertResolveByteExact("lock/npm/resolve-clean", "after-v2", "{\"lockfileVersion\":2}",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void directlyDeclaredForkV3() {
        // root declares debug@2.6.9 (needs ms@2.0.0) and ms@2.1.3; ms forks — 2.1.3 hoists, 2.0.0 nests under debug.
        assertResolveByteExact("lock/npm/resolve-fork", "after", null,
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the resolver output equals {@code dir/golden} byte-for-byte.
     * Each distinct name maps to a packument route {@code http/<name>}; each {@code {name, version}} to a manifest
     * route {@code http/<name>-<version>}.
     */
    private void assertResolveByteExact(String dir, String golden, String existingLock, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
        NpmRegistryClient client = new NpmRegistryClient(HttpSenderExecutionContextView.view(ctx).getHttpSender());
        NodeRegistries registries = new NodeRegistries(
                new NodeRegistry(null, REG, null, null, null, null, false, null, true, false),
                Collections.emptyMap(), null, null, null);
        ResolveRequest request = new ResolveRequest(
                singletonMap("", resource(dir + "/pkg")), existingLock, registries, client);

        assertThat(new NpmResolver().resolve(request)).isEqualTo(resource(dir + "/" + golden));
    }

    // --- live re-record / provenance check (disabled: needs npm + network) ---

    @Test
    @Disabled("live: runs real npm 11.6.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealNpm() throws Exception {
        assertNpmReproduces("lock/npm/resolve-clean/pkg", "lock/npm/resolve-clean/after", "3");
        assertNpmReproduces("lock/npm/resolve-clean/pkg", "lock/npm/resolve-clean/after-v2", "2");
        assertNpmReproduces("lock/npm/resolve-fork/pkg", "lock/npm/resolve-fork/after", "3");
    }
}
