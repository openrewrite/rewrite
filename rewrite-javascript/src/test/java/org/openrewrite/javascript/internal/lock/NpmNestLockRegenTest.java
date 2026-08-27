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
 * The differential harness for Phase B npm reverse-dependent nesting (I5). Each test replays a fixture — a
 * before {@code package.json}, a before {@code package-lock.json}, the recipe's version-bump edit, and
 * recorded registry HTTP — through {@link NativeLockEngine} entirely OFFLINE, then asserts the emitted lock
 * is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * The scenario: the root directly depends on {@code debug@2.6.9} (which pins {@code ms@2.0.0} exactly) and on
 * {@code ms@2.0.0}. Bumping the root's {@code ms} to {@code 2.1.3} keeps {@code ms@2.1.3} at the top level and
 * nests {@code ms@2.0.0} under {@code node_modules/debug} — the copy debug's exact pin still requires. Only
 * {@code ms} routes are needed: debug is read from the lock, never re-resolved.
 * <p>
 * The goldens were produced with npm 11.6.2; enable {@link #recordGoldensWithRealNpm()} to re-derive and
 * verify them (see {@link NpmClosureAddLockRegenTest} for the capture recipe).
 */
class NpmNestLockRegenTest extends LockRegenTestSupport {

    @Test
    void nestOldVersionUnderReverseDependentV3() {
        assertNestByteExact("lock/npm/nest-basic");
    }

    @Test
    void nestOldVersionUnderReverseDependentV2() {
        // The same nest into a lockfileVersion 2 lock: the packages entry relocates and the legacy
        // dependencies tree grows debug.dependencies.ms (minimal version/resolved/integrity) two levels deep.
        assertNestByteExact("lock/npm/nest-basic-v2");
    }

    /** Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte. */
    private void assertNestByteExact(String dir) {
        routes.put(REG + "ms", resource(dir + "/http/ms"));
        routes.put(REG + "ms/2.0.0", resource(dir + "/http/ms-2.0.0"));
        routes.put(REG + "ms/2.1.3", resource(dir + "/http/ms-2.1.3"));

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
                {"lock/npm/nest-basic", "3"},
                {"lock/npm/nest-basic-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
