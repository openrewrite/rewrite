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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for npm whole-closure resolve-and-patch. Each fixture is a real incremental install:
 * the golden {@code before} was recorded by {@code npm install --package-lock-only} of {@code pkg-before}, and the
 * golden {@code after} by re-running npm on the edited {@code pkg} in the same directory — so it is the incremental
 * truth, entries npm kept and entries it changed. Each test replays that edit through the whole
 * {@code NativeLockEngine.regenerate(...)} entirely OFFLINE (a stub {@code HttpSender} serves the captured
 * packuments/manifests under {@code http/}): the per-dependency proof defers, the engine resolves the closure
 * seeded by the before lock, diffs it, patches, and must reproduce {@code after} BYTE-IDENTICAL.
 * <p>
 * The goldens were produced with npm 11.6.2 against registry.npmjs.org; {@link #recordGoldensWithRealNpm()}
 * re-derives and verifies them. The abbreviated packuments ({@code {name, dist-tags, versions:{...:{}}}}) and
 * verbatim single-version manifests under {@code http/} were captured from the registry.
 */
class NpmResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void forkAdd() {
        // before installs debug@2.6.9 with ms@2.0.0 hoisted; adding ms@2.1.3 forks ms — npm hands the top slot to
        // the declared 2.1.3 (an in-place bump of the existing entry) and nests debug's 2.0.0 under debug.
        assertResolveAndPatch("lock/npm/resolve-fork", "before", "after",
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void transitiveForkAdd() {
        // before installs debug@2.2.0 (ms@0.7.1); adding humanize-ms pulls ms@^2.0.0 — a transitive fork, neither
        // version declared. The locked 0.7.1 keeps the top slot and humanize-ms's 2.1.3 nests under it.
        assertResolveAndPatch("lock/npm/resolve-transitive-fork", "before", "after",
                new String[][]{{"debug", "2.2.0"}, {"humanize-ms", "1.2.1"}, {"ms", "0.7.1"}, {"ms", "2.1.3"}});
    }

    @Test
    void forkWithPeerBearingMembersAdd() {
        // before installs @conciv/serve (node-server 2.0.12) + hono; adding @doxajs/http-hono (pins 2.0.11) forks
        // @hono/node-server with peer-bearing members. The locked 2.0.12 keeps the top slot, 2.0.11 nests, both
        // record peerDependencies verbatim, and hono stays flagged `peer: true`.
        assertResolveAndPatch("lock/npm/resolve-fork-peer", "before", "after",
                new String[][]{{"@conciv/serve", "0.0.17"}, {"@doxajs/http-hono", "0.1.0-alpha.31"},
                        {"@doxajs/core", "0.1.0-alpha.31"}, {"@doxajs/runtime", "0.1.0-alpha.31"},
                        {"@doxajs/manifest", "0.1.0-alpha.31"}, {"@hono/node-server", "2.0.11"},
                        {"@hono/node-server", "2.0.12"}, {"hono", "4.12.29"}, {"ws", "8.21.1"}});
    }

    @Test
    void satisfiedPeerAdd() {
        // before installs react alone; adding use-sync-external-store (whose react peer the lock already
        // satisfies) writes one entry with peerDependencies verbatim and flags react `peer: true`.
        assertResolveAndPatch("lock/npm/resolve-peer", "before", "after",
                new String[][]{{"react", "19.2.8"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerAddV2() {
        // The same edit into a lockfileVersion 2 lock also mirrors the add and the peer flag in the legacy tree.
        assertResolveAndPatch("lock/npm/resolve-peer", "before-v2", "after-v2",
                new String[][]{{"react", "19.2.8"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerWithMetaAdd() {
        // use-callback-ref declares two peers (react satisfied, @types/react optional and absent) plus
        // peerDependenciesMeta; the new entry records both objects verbatim and react gains `peer: true`.
        assertResolveAndPatch("lock/npm/resolve-peer-meta", "before", "after",
                new String[][]{{"react", "19.2.8"}, {"use-callback-ref", "1.3.3"}, {"tslib", "2.8.1"}});
    }

    @Test
    void satisfiedPeerWithMetaAddV2() {
        assertResolveAndPatch("lock/npm/resolve-peer-meta", "before-v2", "after-v2",
                new String[][]{{"react", "19.2.8"}, {"use-callback-ref", "1.3.3"}, {"tslib", "2.8.1"}});
    }

    @Test
    void peerAutoInstallIntoRootOnlyLock() {
        // before is a dependency-less project (a root-only lock); adding use-sync-external-store auto-installs its
        // react peer top-level (`peer: true`), npm 7+ behavior, both entries fresh.
        assertResolveAndPatch("lock/npm/resolve-peer-autoinstall", "before", "after",
                new String[][]{{"use-sync-external-store", "1.4.0"}, {"react", "19.2.8"}});
    }

    @Test
    void peerAutoInstallIntoRootOnlyLockV2() {
        assertResolveAndPatch("lock/npm/resolve-peer-autoinstall", "before-v2", "after-v2",
                new String[][]{{"use-sync-external-store", "1.4.0"}, {"react", "19.2.8"}});
    }

    @Test
    void rootPeerDeclaration() {
        // The root manifest turns into a library declaring peerDependencies.react: npm auto-installs react
        // top-level, flags it `peer: true`, and mirrors the scope verbatim in the root entry.
        assertResolveAndPatch("lock/npm/resolve-root-peer", "before", "after",
                new String[][]{{"react", "19.2.8"}});
    }

    @Test
    void rootPeerDeclarationV2() {
        assertResolveAndPatch("lock/npm/resolve-root-peer", "before-v2", "after-v2",
                new String[][]{{"react", "19.2.8"}});
    }

    @Test
    void devAndOptionalAdds() {
        // before installs once (prod); the edit adds supports-color (dev) and is-odd (optional) in one change.
        // The fresh entries carry `dev: true`/`optional: true` and the untouched prod pair keeps its bytes.
        assertResolveAndPatch("lock/npm/resolve-dev-optional", "before", "after",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void devAndOptionalAddsV2() {
        assertResolveAndPatch("lock/npm/resolve-dev-optional", "before-v2", "after-v2",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void aliasBump() {
        // react-is-18 aliases the real react-is, locked at 18.2.0; widening to ^18.3.1 bumps the entry in place
        // at its alias slot — the `name` field and the alias spec in the root deps stay byte-identical.
        assertResolveAndPatch("lock/npm/resolve-alias", "before", "after",
                new String[][]{{"react-is", "18.2.0"}, {"react-is", "18.3.1"}});
    }

    @Test
    void aliasBumpV2StaysDeferred() {
        // The v2 legacy tree records the alias as version "npm:react-is@18.2.0", which the patcher cannot yet
        // rewrite; the edit defers gracefully with the old lock untouched.
        routePackages("lock/npm/resolve-alias", new String[][]{{"react-is", "18.2.0"}, {"react-is", "18.3.1"}});
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource("lock/npm/resolve-alias/pkg"), resource("lock/npm/resolve-alias/pkg-before"),
                resource("lock/npm/resolve-alias/before-v2"), null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getLockFileContent()).isNull();
    }

    /**
     * Replay {@code dir}'s incremental edit offline through the whole engine and assert the patched lock equals
     * {@code dir/golden} byte-for-byte. Each distinct name maps to a packument route {@code http/<name>}; each
     * {@code {name, version}} to a manifest route {@code http/<name>-<version>}.
     */
    private void assertResolveAndPatch(String dir, String before, String golden, String[][] packages) {
        routePackages(dir, packages);
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource(dir + "/pkg"), resource(dir + "/pkg-before"), resource(dir + "/" + before),
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/" + golden));
    }

    private void routePackages(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
    }

    // --- live re-record / provenance check (disabled: needs npm + network) ---

    @Test
    @Disabled("live: runs real npm 11.6.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealNpm() throws Exception {
        assertNpmReproducesIncremental("lock/npm/resolve-fork", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-transitive-fork", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-fork-peer", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-peer", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-peer", "before-v2", "after-v2", "2");
        assertNpmReproducesIncremental("lock/npm/resolve-peer-meta", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-peer-meta", "before-v2", "after-v2", "2");
        assertNpmReproducesIncremental("lock/npm/resolve-peer-autoinstall", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-peer-autoinstall", "before-v2", "after-v2", "2");
        assertNpmReproducesIncremental("lock/npm/resolve-root-peer", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-root-peer", "before-v2", "after-v2", "2");
        assertNpmReproducesIncremental("lock/npm/resolve-dev-optional", "before", "after", "3");
        assertNpmReproducesIncremental("lock/npm/resolve-dev-optional", "before-v2", "after-v2", "2");
        assertNpmReproducesIncremental("lock/npm/resolve-alias", "before", "after", "3");
    }
}
