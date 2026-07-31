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
 * The differential harness for Phase B pnpm orphan-prune: a direct-dependency bump whose new version DROPS a
 * {@code dependencies} edge. The bumped package's snapshot loses the dropped edge (an emptied snapshot becomes
 * {@code {}}) and every {@code packages}/{@code snapshots} entry the drop leaves unreachable is garbage-collected
 * — mirroring the npm orphan-prune (T13) for pnpm's content-addressed graph (v9). Each byte-exact test replays a
 * fixture entirely OFFLINE (a stub {@code HttpSender} serves the bumped package's captured packument/manifests;
 * the orphaned entries are read straight from the lock) through {@link NativeLockEngine} and asserts the emitted
 * lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code pnpm install --lockfile-only}.
 * <p>
 * The goldens were produced with pnpm 11.2.2 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealPnpm()}.
 */
class PnpmOrphanPruneLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact orphan-prune bump (golden from real pnpm 11.2.2) ------

    @Test
    void orphanPruneV9() {
        // Bump semver 7.6.0 -> 7.8.5: semver 7.8.5 no longer depends on lru-cache, so semver's snapshot loses its
        // `lru-cache` edge (becoming {}) and lru-cache (plus its private transitive yallist) are GC'd from both
        // the packages and snapshots maps. semver's other metadata (engines/hasBin) is unchanged.
        assertOrphanPruneByteExact("lock/pnpm/orphan-prune",
                new String[][]{{"semver", "7.6.0", "7.8.5"}});
    }

    private void assertOrphanPruneByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            for (int i = 1; i < pkg.length; i++) {
                routes.put(route + "/" + pkg[i], resource(dir + "/http/" + pkg[0] + "-" + pkg[i]));
            }
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Pnpm,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs pnpm + network) ---

    @Test
    @Disabled("live: runs real pnpm 11.2.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealPnpm() throws Exception {
        String fixture = "lock/pnpm/orphan-prune";
        assertPnpmReproduces(fixture + "/pkg-before", fixture + "/before");
        assertPnpmReproduces(fixture + "/pkg-after", fixture + "/after");
    }
}
