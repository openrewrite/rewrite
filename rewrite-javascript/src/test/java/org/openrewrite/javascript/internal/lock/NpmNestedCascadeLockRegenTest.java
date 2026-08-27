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
 * The differential harness for the Phase B npm cascade's nested-placement awareness: a direct-dependency bump
 * whose changed closure keeps an edge to a transitive that is installed NESTED (a sibling holds the top-level
 * slot at a different version). Resolving that edge against only the top-level {@code node_modules/<name>}
 * entry mistook the unchanged nested edge for one that must move (or a brand-new add-during-bump) and failed
 * loud on a tree real npm reshapes cleanly. The engine now resolves each edge over the actual installed tree
 * (npm's hoisting walk), so a nested-satisfied edge is a no-op. The byte-exact test replays the fixture
 * OFFLINE (a stub {@code HttpSender} serves captured packuments/manifests) through {@link NativeLockEngine}
 * and asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real
 * {@code npm install --package-lock-only} (npm 11.6.2). Enable {@link #recordGoldensWithRealNpm()} to
 * re-derive/verify the goldens.
 */
class NpmNestedCascadeLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact nested-edge no-op bump (golden from real npm 11.6.2) --

    @Test
    void nestedTransitiveUnchangedOnBump() {
        // Bump @rollup/plugin-node-resolve 15.2.3 -> 15.2.4: the only closure change is a dropped
        // is-builtin-module edge (orphan-pruned with its private builtin-modules). Its @types/resolve edge
        // (pinned 1.20.2) stays satisfied by the copy NESTED under the plugin, because the project's own
        // top-level @types/resolve is 1.20.6 — a placement the old top-level-only read misclassified.
        assertByteExact("lock/npm/cascade-nested",
                new String[][]{{"@rollup/plugin-node-resolve", "15.2.3", "15.2.4"}});
    }

    private void assertByteExact(String dir, String[][] packages) {
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
                {"lock/npm/cascade-nested", "3"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
