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
 * The differential harness for the npm ADD-DURING-BUMP slice: a direct-dependency bump whose new version pulls a
 * brand-new transitive into the closure. The engine resolves and hoists that fresh subtree via the same placement
 * logic as a closure add (each member byte-exact-or-fail-loud), and grafts the new edge into the bumped entry's
 * {@code dependencies} map. Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender}
 * serves captured packuments/manifests) through {@link NativeLockEngine} and asserts the emitted lock is
 * BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only} (npm
 * 11.6.2). Enable {@link #recordGoldensWithRealNpm()} to re-derive/verify the goldens.
 */
class NpmAddDuringBumpLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact add-during-bump (goldens from real npm 11.6.2) --------

    @Test
    void singleNewLeafV3() {
        // Bump p-limit 1.1.0 -> 1.2.0: 1.2.0 introduces a brand-new `p-try` ^1.0.0 edge (1.1.0 had none). p-try is
        // a leaf; it hoists to a fresh node_modules/p-try, and p-limit's entry gains its `dependencies` map.
        assertByteExact("lock/npm/add-during-bump",
                new String[][]{{"p-limit", "1.1.0", "1.2.0"}, {"p-try", "1.0.0"}});
    }

    @Test
    void singleNewLeafV2() {
        // The same add-during-bump into a lockfileVersion 2 lock: the packages entries AND the legacy
        // `dependencies` tree update together (p-limit gains its `requires` map, p-try's minimal entry inserts).
        assertByteExact("lock/npm/add-during-bump-v2",
                new String[][]{{"p-limit", "1.1.0", "1.2.0"}, {"p-try", "1.0.0"}});
    }

    // --- conflict with an existing placement fails loud -------------------

    @Test
    void newTransitiveSubtreeConflictsWithInstalledTopLevelFailsLoud() {
        // alpha 1.0.0 -> 2.0.0 pulls a new `beta` ^1.0.0; beta needs gamma ^2.0.0, but gamma@1.0.0 already holds
        // the top-level slot (a transitive of `other`). npm would nest gamma@2.0.0 under beta; the minimal engine
        // cannot reproduce that byte-for-byte, so it defers rather than emit a wrong lock.
        routes.put(REG + "alpha", "{\"name\":\"alpha\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "alpha/1.0.0", "{\"name\":\"alpha\",\"version\":\"1.0.0\"}");
        routes.put(REG + "alpha/2.0.0",
                "{\"name\":\"alpha\",\"version\":\"2.0.0\",\"dependencies\":{\"beta\":\"^1.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/alpha/-/alpha-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-ALPHA2\"}}");
        routes.put(REG + "beta", "{\"name\":\"beta\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "beta/1.0.0",
                "{\"name\":\"beta\",\"version\":\"1.0.0\",\"dependencies\":{\"gamma\":\"^2.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-BETA1\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\", \"other\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\"},\n" +
                "    \"node_modules/gamma\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/gamma/-/gamma-1.0.0.tgz\", \"integrity\": \"sha512-GAMMA1\"},\n" +
                "    \"node_modules/other\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/other/-/other-1.0.0.tgz\", \"integrity\": \"sha512-OTHER1\", \"dependencies\": {\"gamma\": \"^1.0.0\"}}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^2.0.0\",\"other\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\",\"other\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("gamma");
        assertThat(result.getFailure().getDetail()).contains("does not satisfy the added");
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, v...}} maps to a packument route ({@code http/<name>}) and one manifest route per version.
     */
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
                {"lock/npm/add-during-bump", "3"},
                {"lock/npm/add-during-bump-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
