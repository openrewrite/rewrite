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
 * The differential harness for bumping a direct dependency that is already present at MULTIPLE versions in the
 * lock (the "large multi-version fork" gate). The root declares {@code ajv} at the top level while {@code table}
 * holds its own older {@code ajv@6} nested under {@code node_modules/table/node_modules/ajv}. Bumping the root's
 * {@code ajv} re-scopes to the top-level copy and moves only it; the nested fork stays byte-identical. Each test
 * replays a fixture through {@link NativeLockEngine} entirely OFFLINE and asserts the emitted lock is
 * BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2 against registry.npmjs.org; {@link #recordGoldensWithRealNpm()}
 * re-derives and verifies them. Only {@code ajv} touches the network (its top-level copy is the only thing that
 * moves); {@code table} and its closure are read from the lock, never re-resolved.
 */
class NpmBumpForkedLockRegenTest extends LockRegenTestSupport {

    @Test
    void bumpTopLevelCopyLeavesNestedForkByteIdentical() {
        assertBumpForkedByteExact("lock/npm/bump-forked");
    }

    /** Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte. */
    private void assertBumpForkedByteExact(String dir) {
        routes.put(REG + "ajv", resource(dir + "/http/ajv"));
        routes.put(REG + "ajv/8.17.1", resource(dir + "/http/ajv-8.17.1"));
        routes.put(REG + "ajv/8.20.0", resource(dir + "/http/ajv-8.20.0"));

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
                {"lock/npm/bump-forked", "3"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
