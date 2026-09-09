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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The differential harness for bun whole-closure resolve-and-patch. Each fixture is a real incremental install:
 * the golden {@code before} was recorded by {@code bun install --lockfile-only} of {@code pkg-before}, and the
 * golden {@code after} by re-running bun on the edited {@code pkg} in the same directory — so it is the
 * incremental truth, entries bun kept and entries it changed. Each test replays that edit through the whole
 * {@code NativeLockEngine.regenerate(...)} entirely OFFLINE (a stub {@code HttpSender} serves the captured
 * packuments/manifests under {@code http/}): the per-dependency proof defers, the engine resolves the closure
 * seeded by the before lock, diffs it, patches, and must reproduce {@code after} BYTE-IDENTICAL.
 * <p>
 * The goldens were produced with bun 1.3.10 against registry.npmjs.org; {@link #recordGoldensWithRealBun()}
 * re-derives and verifies them. bun deletes an empty lockfile, so no dependency-less before lock exists; the
 * smallest real before is a single-entry lock.
 */
class BunResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void forkAdd() {
        // before installs debug@2.6.9 with ms@2.0.0 hoisted; adding ms@2.1.3 forks ms — bun hands the top slot
        // to the declared 2.1.3 (an in-place bump of the "ms" tuple) and nests debug's 2.0.0 as "debug/ms".
        assertResolveAndPatch("lock/bun/resolve-fork", "before", "after",
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void devAndOptionalAdds() {
        // before installs once (prod); the edit adds supports-color (dev) and is-odd (optional) in one change.
        // The workspace object gains devDependencies and optionalDependencies scopes; the tuples stay unflagged.
        assertResolveAndPatch("lock/bun/resolve-dev-optional", "before", "after",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void satisfiedPeerAdd() {
        // before installs react alone; adding use-sync-external-store (whose react peer the lock already
        // satisfies) writes one tuple carrying peerDependencies verbatim; the react tuple keeps its bytes.
        assertResolveAndPatch("lock/bun/resolve-peer", "before", "after",
                new String[][]{{"react", "19.2.8"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void satisfiedPeerWithMetaAdd() {
        // use-callback-ref declares two peers (react satisfied, @types/react optional and absent) plus
        // peerDependenciesMeta; bun flattens the meta into an "optionalPeers" array in the new tuple.
        assertResolveAndPatch("lock/bun/resolve-peer-meta", "before", "after",
                new String[][]{{"react", "19.2.8"}, {"use-callback-ref", "1.3.3"}, {"tslib", "2.8.1"}});
    }

    @Test
    void freshPeerProviderAdd() {
        // before installs only once; the edit adds react and use-sync-external-store together, so the peer is
        // satisfied by a co-added fresh provider rather than a locked one. The untouched pair keeps its bytes.
        assertResolveAndPatch("lock/bun/resolve-peer-fresh", "before", "after",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"react", "19.2.8"},
                        {"use-sync-external-store", "1.4.0"}});
    }

    /**
     * Replay {@code dir}'s incremental edit offline through the whole engine and assert the patched lock equals
     * {@code dir/golden} byte-for-byte. Each distinct name maps to a packument route {@code http/<name>}; each
     * {@code {name, version}} to a manifest route {@code http/<name>-<version>}.
     */
    private void assertResolveAndPatch(String dir, String before, String golden, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource(dir + "/pkg"), resource(dir + "/pkg-before"), resource(dir + "/" + before),
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/" + golden));
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        assertBunReproducesIncremental("lock/bun/resolve-fork", "before", "after");
        assertBunReproducesIncremental("lock/bun/resolve-dev-optional", "before", "after");
        assertBunReproducesIncremental("lock/bun/resolve-peer", "before", "after");
        assertBunReproducesIncremental("lock/bun/resolve-peer-meta", "before", "after");
        assertBunReproducesIncremental("lock/bun/resolve-peer-fresh", "before", "after");
    }
}
