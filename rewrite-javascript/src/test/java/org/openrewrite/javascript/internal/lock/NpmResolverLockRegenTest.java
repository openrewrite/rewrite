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

    @Test
    void transitiveForkV3() {
        // root declares debug@2.2.0 (needs ms@0.7.1) and humanize-ms@1.2.1 (needs ms@^2.0.0); ms forks
        // transitively — neither version is directly declared. npm hoists the version required by the
        // alphabetically-first requirer (debug's ms@0.7.1) and nests humanize-ms's ms@2.1.3 under humanize-ms.
        assertResolveByteExact("lock/npm/resolve-transitive-fork", "after", null,
                new String[][]{{"debug", "2.2.0"}, {"humanize-ms", "1.2.1"}, {"ms", "0.7.1"}, {"ms", "2.1.3"}});
    }

    @Test
    void satisfiedPeerProviderV3() {
        // use-sync-external-store peer-depends on react, which root also declares top-level. The peer is a
        // constraint already met (no new node); npm records peerDependencies verbatim and flags react `peer: true`.
        assertResolveByteExact("lock/npm/resolve-peer", "after", null,
                new String[][]{{"react", "19.2.8"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerProviderV2() {
        // The same closure into a lockfileVersion 2 lock: react keeps `peer: true` in the legacy tree, and the
        // peer-only declarer gets an empty `requires` (npm omits the peer edge but still emits the field).
        assertResolveByteExact("lock/npm/resolve-peer", "after-v2", "{\"lockfileVersion\":2}",
                new String[][]{{"react", "19.2.8"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerWithMetaV3() {
        // use-callback-ref declares two peers (react satisfied top-level, @types/react optional and absent) plus
        // peerDependenciesMeta; npm records both peer objects verbatim and flags react `peer: true`.
        assertResolveByteExact("lock/npm/resolve-peer-meta", "after", null,
                new String[][]{{"react", "19.2.8"}, {"use-callback-ref", "1.3.3"}, {"tslib", "2.8.1"}});
    }

    @Test
    void devAndOptionalClosureV3() {
        // once (prod) hoists unflagged; supports-color+has-flag (dev) get `dev: true`; is-odd+is-number (optional)
        // get `optional: true`. Root records all three scopes verbatim.
        assertResolveByteExact("lock/npm/resolve-dev-optional", "after", null,
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void devAndOptionalClosureV2() {
        // The same closure into a lockfileVersion 2 lock carries the dev/optional flags into the legacy tree too.
        assertResolveByteExact("lock/npm/resolve-dev-optional", "after-v2", "{\"lockfileVersion\":2}",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void peerAutoInstallV3() {
        // use-sync-external-store peer-depends on react, which root does NOT declare; npm 7+ auto-installs react
        // top-level and flags it `peer: true`. The peer resolves to a single leaf version — the cleanest slice.
        assertResolveByteExact("lock/npm/resolve-peer-autoinstall", "after", null,
                new String[][]{{"use-sync-external-store", "1.4.0"}, {"react", "19.2.8"}});
    }

    @Test
    void peerAutoInstallV2() {
        // The same closure into a lockfileVersion 2 lock: react is auto-installed into the legacy tree too
        // (`peer: true`, no requires), and the declarer keeps an empty `requires` (its only edge is the omitted peer).
        assertResolveByteExact("lock/npm/resolve-peer-autoinstall", "after-v2", "{\"lockfileVersion\":2}",
                new String[][]{{"use-sync-external-store", "1.4.0"}, {"react", "19.2.8"}});
    }

    @Test
    void satisfiedPeerWithMetaV2() {
        // The same closure as lockfileVersion 2: the declarer's `requires` keeps only its regular dep (tslib), the
        // peer edges omitted, and react keeps `peer: true` in the legacy tree.
        assertResolveByteExact("lock/npm/resolve-peer-meta", "after-v2", "{\"lockfileVersion\":2}",
                new String[][]{{"react", "19.2.8"}, {"use-callback-ref", "1.3.3"}, {"tslib", "2.8.1"}});
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
        assertNpmReproduces("lock/npm/resolve-dev-optional/pkg", "lock/npm/resolve-dev-optional/after", "3");
        assertNpmReproduces("lock/npm/resolve-dev-optional/pkg", "lock/npm/resolve-dev-optional/after-v2", "2");
        assertNpmReproduces("lock/npm/resolve-fork/pkg", "lock/npm/resolve-fork/after", "3");
        assertNpmReproduces("lock/npm/resolve-transitive-fork/pkg", "lock/npm/resolve-transitive-fork/after", "3");
        assertNpmReproduces("lock/npm/resolve-peer/pkg", "lock/npm/resolve-peer/after", "3");
        assertNpmReproduces("lock/npm/resolve-peer/pkg", "lock/npm/resolve-peer/after-v2", "2");
        assertNpmReproduces("lock/npm/resolve-peer-meta/pkg", "lock/npm/resolve-peer-meta/after", "3");
        assertNpmReproduces("lock/npm/resolve-peer-meta/pkg", "lock/npm/resolve-peer-meta/after-v2", "2");
        assertNpmReproduces("lock/npm/resolve-peer-autoinstall/pkg", "lock/npm/resolve-peer-autoinstall/after", "3");
        assertNpmReproduces("lock/npm/resolve-peer-autoinstall/pkg", "lock/npm/resolve-peer-autoinstall/after-v2", "2");
    }
}
