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
 * The differential harness for pnpm whole-closure resolve-and-patch. Each fixture is a real incremental install:
 * {@code pkg-before} installed from scratch gives the golden {@code before} lock, and the edited {@code pkg}
 * re-installed in the same directory gives {@code after} — the incremental truth. Each test replays that edit
 * through the whole {@code NativeLockEngine.regenerate(...)} entirely OFFLINE (a stub {@code HttpSender} serves
 * the captured packuments/manifests under {@code http/}): the per-dependency proof defers, the engine resolves
 * the closure seeded by the before lock, diffs it against the lock, patches, and must reproduce {@code after}
 * BYTE-IDENTICAL.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org; {@link #recordGoldensWithRealPnpm()}
 * re-derives and verifies them.
 */
class PnpmResolveAndPatchLockRegenTest extends LockRegenTestSupport {

    @Test
    void forkAdd() {
        // before installs is-odd (is-number@6.0.0 transitive); adding is-number@^7.0.0 is pnpm's content-fork:
        // the new version's entries land beside the retained 6.0.0 and only the importer edge is new.
        assertResolveAndPatch("lock/pnpm/resolve-fork",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}, {"is-number", "7.0.0"}});
    }

    @Test
    void devAndOptionalAdds() {
        // before installs once (prod); the edit adds supports-color (dev) and is-odd (optional) in one change.
        // Fresh snapshots carry `optional: true` where optional-reachable; the untouched pair keeps its bytes.
        assertResolveAndPatch("lock/pnpm/resolve-dev-optional",
                new String[][]{{"once", "1.4.0"}, {"wrappy", "1.0.2"}, {"supports-color", "7.2.0"},
                        {"has-flag", "4.0.0"}, {"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    @Test
    void satisfiedPeerAdd() {
        // before installs react alone; adding use-sync-external-store records its peerDependencies block and a
        // peer-suffixed snapshot key use-sync-external-store@1.4.0(react@19.0.0).
        assertResolveAndPatch("lock/pnpm/resolve-peer",
                new String[][]{{"react", "19.0.0"}, {"use-sync-external-store", "1.4.0"}});
    }

    @Test
    void nestedMultiPeerAdd() {
        // before installs react + react-dom; adding @floating-ui/react-dom (peers react AND react-dom, the
        // latter itself peering react) nests the suffix: (react-dom@19.0.0(react@19.0.0)) inside the new keys.
        assertResolveAndPatch("lock/pnpm/resolve-peer-nested",
                new String[][]{{"react", "19.0.0"}, {"react-dom", "19.0.0"}, {"scheduler", "0.25.0"},
                        {"@floating-ui/react-dom", "2.1.2"}, {"@floating-ui/dom", "1.8.0"},
                        {"@floating-ui/core", "1.8.0"}, {"@floating-ui/utils", "0.2.12"}});
    }

    @Test
    void firstDependencyIntoRootOnlyLock() {
        // before is a dependency-less project (`.: {}` importer, no packages/snapshots sections); the first
        // declaration creates the importer scope and both sections.
        assertResolveAndPatch("lock/pnpm/resolve-rootonly", new String[][]{{"ms", "2.1.3"}});
    }

    private void assertResolveAndPatch(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource(dir + "/pkg"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        assertPnpmReproducesIncremental("lock/pnpm/resolve-fork");
        assertPnpmReproducesIncremental("lock/pnpm/resolve-dev-optional");
        assertPnpmReproducesIncremental("lock/pnpm/resolve-peer");
        assertPnpmReproducesIncremental("lock/pnpm/resolve-peer-nested");
        assertPnpmReproducesIncremental("lock/pnpm/resolve-rootonly");
    }
}
