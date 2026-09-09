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
 * The differential harness for the npm MULTI-LEVEL cascade: a direct-dependency bump forces a shared transitive
 * to move, and that transitive's new version ALSO forces ITS OWN dependency to move, recursively. The engine
 * feeds each mover's changed edges back into the same forced-move worklist (each wave byte-exact-or-fail-loud).
 * Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender} serves captured
 * packuments/manifests) through {@link NativeLockEngine} and asserts the emitted lock is BYTE-IDENTICAL to a
 * golden {@code after} recorded from a real {@code npm install --package-lock-only} (npm 11.6.2). Enable
 * {@link #recordGoldensWithRealNpm()} to re-derive/verify the goldens.
 */
class NpmMultiLevelCascadeLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact two-level cascade (goldens from real npm 11.6.2) ------

    @Test
    void twoLevelCascadeV3() {
        // Bump ansi-styles ^3.2.1 -> ^4.0.0: ansi-styles 3.2.1 -> 4.3.0 moves its color-convert edge
        // ^1.9.0 -> ^2.0.1, forcing color-convert 1.9.3 -> 2.0.1; color-convert 2.0.1 in turn moves its
        // color-name edge 1.1.3 -> ~1.1.4, forcing the leaf color-name 1.1.3 -> 1.1.4. Three levels move, each
        // a single-requirer in-range clean-closure bump (metadata patched in place: ansi-styles engines+funding,
        // color-convert engines added).
        assertCascadeByteExact("lock/npm/cascade-multilevel",
                new String[][]{{"ansi-styles", "3.2.1", "4.3.0"}, {"color-convert", "1.9.3", "2.0.1"},
                        {"color-name", "1.1.3", "1.1.4"}});
    }

    @Test
    void twoLevelCascadeV2() {
        // The same multi-level cascade into a lockfileVersion 2 lock: the packages entries AND the legacy
        // `dependencies` tree (each mover's `requires` re-pinned, each moved entry advanced) update together.
        assertCascadeByteExact("lock/npm/cascade-multilevel-v2",
                new String[][]{{"ansi-styles", "3.2.1", "4.3.0"}, {"color-convert", "1.9.3", "2.0.1"},
                        {"color-name", "1.1.3", "1.1.4"}});
    }

    // --- opportunistic deeper bump fails loud -----------------------------

    @Test
    void deeperMoverAddingAnEdgeFailsLoud() {
        // alpha 1.0.0 -> 2.0.0 moves its `mid` edge ^1.0.0 -> ^2.0.0, forcing mid 1.0.0 -> 2.0.0. But mid 2.0.0
        // ADDS a brand-new `leaf` edge mid-cascade — npm would place a fresh subtree; a minimal engine cannot
        // reproduce that byte-for-byte, so it defers rather than emit a wrong lock.
        routes.put(REG + "alpha", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0",
                "{\"name\":\"alpha\",\"version\":\"1.0.0\",\"dependencies\":{\"mid\":\"^1.0.0\"}}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\",\"dependencies\":{\"mid\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");
        routes.put(REG + "mid", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "mid/1.0.0",
                "{\"name\":\"mid\",\"version\":\"1.0.0\"}");
        routes.put(REG + "mid/2.0.0",
                "{\"name\":\"mid\",\"version\":\"2.0.0\",\"dependencies\":{\"leaf\":\"^1.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/mid/-/mid-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-MID2\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\", \"dependencies\": {\"mid\": \"^1.0.0\"}},\n" +
                "    \"node_modules/mid\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/mid/-/mid-1.0.0.tgz\", \"integrity\": \"sha512-MID1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^2.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("mid");
        assertThat(result.getFailure().getDetail()).contains("adds/drops a dependency edge");
    }

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
                {"lock/npm/cascade-multilevel", "3"},
                {"lock/npm/cascade-multilevel-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
