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
import org.openrewrite.javascript.internal.lock.resolve.PnpmResolver;
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for the pnpm {@link PnpmResolver} (ADR 0012). Each test replays a fixture — an edited
 * {@code package.json} and recorded registry HTTP — through {@code PnpmResolver.resolve(...)} entirely OFFLINE (a
 * stub {@code HttpSender} serves the captured packuments/manifests under each fixture's {@code http/}), then
 * asserts the lock it produces is BYTE-IDENTICAL to a golden {@code after} recorded from a real
 * {@code pnpm install --lockfile-only}. The resolver resolves the whole closure from scratch, so the golden is a
 * full pnpm-lock.yaml (not a patch), and byte-identity is the whole contract.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org; {@link #recordGoldensWithRealPnpm()}
 * re-derives and verifies them. The abbreviated packuments and verbatim single-version manifests under
 * {@code http/} were captured from the registry.
 */
class PnpmResolverLockRegenTest extends LockRegenTestSupport {

    @Test
    void cleanClosureV9() {
        // is-odd@3.0.1 -> is-number@6.0.0: a flat content-addressed closure.
        assertResolveByteExact("lock/pnpm/resolve-clean", "after", null,
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void directlyDeclaredForkV9() {
        // root declares is-number@^7.0.0 and is-odd@^3.0.1 (needs is-number@^6.0.0); is-number forks — pnpm keeps
        // both 6.0.0 and 7.0.0 content-addressed side by side, no nesting.
        assertResolveByteExact("lock/pnpm/resolve-fork", "after", null,
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"is-number", "7.0.0"}});
    }

    @Test
    void satisfiedPeerV9() {
        // use-sync-external-store@1.4.0 peers react, satisfied by the directly-declared react@19.0.0. pnpm keys the
        // consumer snapshot use-sync-external-store@1.4.0(react@19.0.0) and materializes react as a snapshot dep,
        // records the raw `peerDependencies` on its packages entry, and suffixes its importer version.
        assertResolveByteExact("lock/pnpm/resolve-peer", "after", null,
                new String[][]{{"react", "19.0.0"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void nestedMultiPeerV9() {
        // @floating-ui/react-dom peers react + react-dom; react-dom itself peers react. The suffix nests
        // (react-dom@19.0.0(react@19.0.0)) inside the consumer key and orders the two peers by rendered reference.
        assertResolveByteExact("lock/pnpm/resolve-peer-nested", "after", null,
                new String[][]{{"react", "19.0.0"}, {"react-dom", "19.0.0"}, {"scheduler", "0.25.0"},
                        {"@floating-ui/react-dom", "2.1.2"}, {"@floating-ui/dom", "1.8.0"},
                        {"@floating-ui/core", "1.8.0"}, {"@floating-ui/utils", "0.2.12"}});
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

        assertThat(new PnpmResolver().resolve(request)).isEqualTo(resource(dir + "/" + golden));
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        assertPnpmReproduces("lock/pnpm/resolve-clean/pkg", "lock/pnpm/resolve-clean/after");
        assertPnpmReproduces("lock/pnpm/resolve-fork/pkg", "lock/pnpm/resolve-fork/after");
        assertPnpmReproduces("lock/pnpm/resolve-peer/pkg", "lock/pnpm/resolve-peer/after");
        assertPnpmReproduces("lock/pnpm/resolve-peer-nested/pkg", "lock/pnpm/resolve-peer-nested/after");
    }
}
