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
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.lock.resolve.YarnClassicResolver;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.Collections;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for the classic-yarn {@link YarnClassicResolver} (ADR 0012). Each test replays a
 * fixture — an edited {@code package.json} and recorded registry HTTP — through {@code YarnClassicResolver.resolve(...)}
 * entirely OFFLINE (a stub {@code HttpSender} serves the captured packuments/manifests under each fixture's
 * {@code http/}), then asserts the {@code yarn.lock} it produces is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code yarn install}. The resolver resolves the whole closure from scratch, so the golden is a full
 * lock (not a patch), and byte-identity is the whole contract.
 * <p>
 * The goldens were produced with yarn 1.22.22 against registry.yarnpkg.com; {@link #recordGoldensWithRealYarn()}
 * re-derives and verifies them. The abbreviated packuments and verbatim single-version manifests under {@code http/}
 * were captured from registry.npmjs.org (yarn mirrors the tarball host to registry.yarnpkg.com itself).
 */
class YarnClassicResolverLockRegenTest extends LockRegenTestSupport {

    @Test
    void cleanClosure() {
        // is-odd@^3.0.1 -> is-number@^6.0.0, plus ms@^2.1.3: three flat single-selector blocks.
        assertResolveByteExact("lock/yarn-classic/resolve-clean",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void mergedSelectors() {
        // root declares is-number@6.0.0 and is-odd@^3.0.1 (which needs is-number@^6.0.0): both ranges resolve to
        // 6.0.0, so is-number's block header merges "is-number@6.0.0, is-number@^6.0.0".
        assertResolveByteExact("lock/yarn-classic/resolve-merged",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void directlyDeclaredFork() {
        // root declares debug@2.6.9 (needs ms@2.0.0) and ms@2.1.3; ms forks into two flat blocks (no nesting).
        assertResolveByteExact("lock/yarn-classic/resolve-fork",
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the resolver output equals {@code dir/after} byte-for-byte.
     * Each distinct name maps to a packument route {@code http/<name>}; each {@code {name, version}} to a manifest
     * route {@code http/<name>-<version>}.
     */
    private void assertResolveByteExact(String dir, String[][] packages) {
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
                singletonMap("", resource(dir + "/pkg")), null, registries, client);

        assertThat(new YarnClassicResolver().resolve(request)).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs yarn + network) ---

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertYarnReproduces("lock/yarn-classic/resolve-clean/pkg", "lock/yarn-classic/resolve-clean/after");
        assertYarnReproduces("lock/yarn-classic/resolve-merged/pkg", "lock/yarn-classic/resolve-merged/after");
        assertYarnReproduces("lock/yarn-classic/resolve-fork/pkg", "lock/yarn-classic/resolve-fork/after");
    }
}
