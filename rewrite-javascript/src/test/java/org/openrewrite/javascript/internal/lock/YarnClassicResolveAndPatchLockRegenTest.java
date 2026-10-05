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
 * The differential harness for classic-yarn whole-closure resolve-and-patch. Each fixture is a real incremental
 * install: the golden {@code before} was recorded by {@code yarn install} of {@code pkg-before}, and the golden
 * {@code after} by re-running yarn on the edited {@code pkg} in the same directory — so it is the incremental
 * truth, blocks yarn kept and blocks it changed. Each test replays that edit through the whole
 * {@code NativeLockEngine.regenerate(...)} entirely OFFLINE (a stub {@code HttpSender} serves the captured
 * packuments/manifests under {@code http/}): the per-dependency proof defers, the engine resolves the closure
 * seeded by the before lock, diffs it against the flat blocks, patches, and must reproduce {@code after}
 * BYTE-IDENTICAL.
 * <p>
 * The goldens were produced with yarn 1.22.22 against registry.yarnpkg.com; {@link #recordGoldensWithRealYarn()}
 * re-derives and verifies them. The abbreviated packuments and verbatim single-version manifests under
 * {@code http/} were captured from registry.npmjs.org (yarn mirrors the tarball host to registry.yarnpkg.com
 * itself, as does the patcher).
 */
class YarnClassicResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void forkAdd() {
        // before installs debug@2.6.9 with an ms@2.0.0 block; adding ms@2.1.3 forks ms — yarn keeps both flat
        // blocks side by side, and the untouched debug/ms@2.0.0 blocks keep their bytes.
        assertResolveAndPatch("lock/yarn-classic/resolve-fork",
                new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void mergedSelectorAdd() {
        // before installs is-odd (is-number@^6.0.0 block); declaring is-number@6.0.0 resolves to the same
        // version, so yarn merges the new selector into the existing header: "is-number@6.0.0, is-number@^6.0.0".
        assertResolveAndPatch("lock/yarn-classic/resolve-merged",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void satisfiedPeerAdd() {
        // before installs react@18.2.0; adding use-sync-external-store (whose react peer the lock satisfies)
        // inserts one plain block — yarn v1 records no peer surface at all.
        assertResolveAndPatch("lock/yarn-classic/resolve-peer",
                new String[][]{{"use-sync-external-store", "1.2.2"}, {"react", "18.2.0"},
                        {"loose-envify", "1.4.0"}, {"js-tokens", "4.0.0"}});
    }

    @Test
    void devAndOptionalAdds() {
        // before installs once (prod); the edit adds supports-color (dev) and is-odd (optional) in one change.
        // yarn v1 marks no scope, so the four fresh blocks are indistinguishable from prod deps and the
        // untouched prod pair keeps its bytes.
        assertResolveAndPatch("lock/yarn-classic/resolve-dev-optional",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void closureAddIntoRootOnlyLock() {
        // before is a dependency-less project: yarn writes a bare-header yarn.lock. The edit declares the first
        // dependencies, whose closure inserts as sorted blocks after the header.
        assertResolveAndPatch("lock/yarn-classic/resolve-clean",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"ms", "2.1.3"}});
    }

    /**
     * Replay {@code dir}'s incremental edit offline through the whole engine and assert the patched lock equals
     * {@code dir/after} byte-for-byte. Each distinct name maps to a packument route {@code http/<name>}; each
     * {@code {name, version}} to a manifest route {@code http/<name>-<version>}.
     */
    private void assertResolveAndPatch(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs yarn + network) ---

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertYarnReproducesIncremental("lock/yarn-classic/resolve-fork", "before", "after");
        assertYarnReproducesIncremental("lock/yarn-classic/resolve-merged", "before", "after");
        assertYarnReproducesIncremental("lock/yarn-classic/resolve-peer", "before", "after");
        assertYarnReproducesIncremental("lock/yarn-classic/resolve-dev-optional", "before", "after");
        assertYarnReproducesIncremental("lock/yarn-classic/resolve-clean", "before", "after");
    }
}
