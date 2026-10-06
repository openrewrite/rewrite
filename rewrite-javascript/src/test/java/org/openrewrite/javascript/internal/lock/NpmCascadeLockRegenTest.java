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
 * The differential harness for Phase B I3 npm cascade upgrades: a direct-dependency version bump whose new
 * version's {@code dependencies} constraint on a shared transitive changed such that the transitive must
 * MOVE to a new version. Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender}
 * serves captured packuments/manifests) through {@link NativeLockEngine} and asserts the emitted lock is
 * BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2. To re-derive/verify them, enable {@link #recordGoldensWithRealNpm()}:
 * it re-runs a real npm over every committed {@code pkg-before}/{@code pkg-after} and asserts the resulting
 * lock equals the committed {@code before}/{@code after}, proving both are genuine npm output (npm reproduces
 * {@code after} from {@code before + bump}). Fixtures were captured with the same node script documented in
 * {@link NpmClosureAddLockRegenTest}.
 */
class NpmCascadeLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact cascade upgrades (goldens from real npm 11.6.2) -------

    @Test
    void basicCascadeV3() {
        // Bump debug ^2.6.9 -> ^3.0.0: debug's `ms` constraint moves 2.0.0 -> ^2.1.1, forcing the shared
        // leaf ms to move 2.0.0 -> 2.1.3. No other reshaping (ms is a leaf with identical metadata).
        assertCascadeByteExact("lock/npm/cascade-basic",
                new String[][]{{"debug", "2.6.9", "3.2.7"}, {"ms", "2.0.0", "2.1.3"}});
    }

    @Test
    void basicCascadeV2() {
        // The same cascade into a lockfileVersion 2 lock: the packages entries AND the legacy `dependencies`
        // tree (debug's `requires.ms` re-pinned, ms's minimal entry moved) update together.
        assertCascadeByteExact("lock/npm/cascade-basic-v2",
                new String[][]{{"debug", "2.6.9", "3.2.7"}, {"ms", "2.0.0", "2.1.3"}});
    }

    // --- sideways cascade (reverse-edge conflict) fails loud --------------

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, v1, v2}} maps to a packument route ({@code http/<name>}) and one manifest route per
     * version ({@code http/<name>-<version>}) — both the old (bumped-from) and new (bumped-to) manifests.
     */
    private void assertCascadeByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            for (int i = 1; i < pkg.length; i++) {
                routes.put(route + "/" + pkg[i], resource(dir + "/http/" + pkg[0] + "-" + pkg[i]));
            }
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs npm + network) ---

    @Test
    @Disabled("live: runs real npm 11.6.2 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealNpm() throws Exception {
        String[][] fixtures = {
                {"lock/npm/cascade-basic", "3"},
                {"lock/npm/cascade-basic-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
