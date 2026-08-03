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
 * The differential harness for Phase B npm orphan-prune: a direct-dependency bump whose new version DROPS a
 * {@code dependencies} edge. The now-unneeded transitive (and any transitive it privately held) is
 * garbage-collected from the lock instead of failing loud. Each byte-exact test replays a fixture entirely
 * OFFLINE (a stub {@code HttpSender} serves captured packuments/manifests) through {@link NativeLockEngine}
 * and asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real
 * {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2. To re-derive/verify them, enable {@link #recordGoldensWithRealNpm()}.
 * Fixtures were captured with the node script documented in {@link NpmClosureAddLockRegenTest}.
 */
class NpmOrphanPruneLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact orphan-prune bumps (goldens from real npm 11.6.2) -----

    @Test
    void orphanPruneV3() {
        // Bump semver 7.6.0 -> 7.8.5: semver 7.8.5 no longer depends on lru-cache, so lru-cache (and its
        // private transitive yallist) are GC'd. semver's other metadata (license/bin/engines) is unchanged.
        assertOrphanPruneByteExact("lock/npm/orphan-prune",
                new String[][]{{"semver", "7.6.0", "7.8.5"}});
    }

    @Test
    void orphanPruneV2() {
        // The same prune into a lockfileVersion 2 lock: the packages entries AND the legacy `dependencies`
        // tree (semver's `requires.lru-cache` dropped, the lru-cache/yallist entries removed) update together.
        assertOrphanPruneByteExact("lock/npm/orphan-prune-v2",
                new String[][]{{"semver", "7.6.0", "7.8.5"}});
    }

    // --- byte-exact engines patching (T13) --------------------------

    @Test
    void enginesChangeV3() {
        // Bump has-flag 3.0.0 -> 4.0.0: a clean leaf bump whose only metadata delta is `engines`
        // (node >=4 -> >=8). The engine writes the new engines object through byte-exact.
        assertOrphanPruneByteExact("lock/npm/engines-change",
                new String[][]{{"has-flag", "3.0.0", "4.0.0"}});
    }

    // --- re-hoist safety: a dropped dep with a second placement fails loud ---

    @Test
    void duplicatePlacementFailsLoud() {
        // Bump alpha 1.0.0 -> 2.0.0, which drops its edge to shared. shared is present at TWO placements
        // (top-level 1.0.0 for beta, nested 2.0.0 under alpha) — removing one could re-hoist the other, so
        // the prune refuses rather than risk a non-byte-exact tree.
        routes.put(REG + "alpha", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0",
                "{\"name\":\"alpha\",\"version\":\"1.0.0\",\"dependencies\":{\"shared\":\"^2.0.0\"}}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\"," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\", \"beta\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\", \"dependencies\": {\"shared\": \"^2.0.0\"}},\n" +
                "    \"node_modules/alpha/node_modules/shared\": {\"version\": \"2.0.0\", \"resolved\": \"https://registry.npmjs.org/shared/-/shared-2.0.0.tgz\", \"integrity\": \"sha512-SHARED2\"},\n" +
                "    \"node_modules/beta\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\", \"integrity\": \"sha512-BETA1\", \"dependencies\": {\"shared\": \"^1.0.0\"}},\n" +
                "    \"node_modules/shared\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/shared/-/shared-1.0.0.tgz\", \"integrity\": \"sha512-SHARED1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^2.0.0\",\"beta\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\",\"beta\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("re-hoist");
    }

    @Test
    void sharedTransitiveKept() {
        // Bump alpha 1.0.0 -> 2.0.0, which drops its edge to shared. shared is still required by beta, so it
        // stays (reachability keeps it); only alpha's own edge is dropped, nothing is GC'd.
        routes.put(REG + "alpha", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0",
                "{\"name\":\"alpha\",\"version\":\"1.0.0\",\"dependencies\":{\"shared\":\"^1.0.0\"}}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\"," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\", \"beta\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\", \"dependencies\": {\"shared\": \"^1.0.0\"}},\n" +
                "    \"node_modules/beta\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\", \"integrity\": \"sha512-BETA1\", \"dependencies\": {\"shared\": \"^1.0.0\"}},\n" +
                "    \"node_modules/shared\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/shared/-/shared-1.0.0.tgz\", \"integrity\": \"sha512-SHARED1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^2.0.0\",\"beta\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\",\"beta\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        // shared stays (beta still requires it); only alpha's own `dependencies` edge is dropped.
        assertThat(result.getLockFileContent()).contains("node_modules/shared");
        assertThat(result.getLockFileContent()).contains(
                "\"node_modules/alpha\": {\"version\": \"2.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\", \"integrity\": \"sha512-ALPHA2\"}");
        assertThat(result.getLockFileContent()).contains(
                "\"node_modules/beta\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\", \"integrity\": \"sha512-BETA1\", \"dependencies\": {\"shared\": \"^1.0.0\"}}");
    }

    private void assertOrphanPruneByteExact(String dir, String[][] packages) {
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
                {"lock/npm/orphan-prune", "3"},
                {"lock/npm/orphan-prune-v2", "2"},
                {"lock/npm/engines-change", "3"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
