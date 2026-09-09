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
 * The differential harness for Phase B npm disjoint-add: adding a brand-new dependency whose closure names are
 * disjoint from every existing fork in the lock. npm leaves the forked/nested entries byte-identical and simply
 * inserts the new entries at their sorted positions, so the add resolves instead of failing loud on the mere
 * presence of a nested placement. An added name that <b>does</b> collide with a fork (a nested parent or child)
 * still fails loud, because npm could re-hoist or reshape that fork.
 * <p>
 * Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender} serves the added package's
 * captured packument/manifest; the existing forked entries are read straight from the lock) through
 * {@link NativeLockEngine} and asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code npm install --package-lock-only} (npm 11.6.2). Enable {@link #recordGoldensWithRealNpm()}
 * to re-derive/verify the goldens.
 */
class NpmDisjointAddLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact disjoint adds into a forked lock (goldens from real npm 11.6.2) ---

    @Test
    void disjointAddV3() {
        // The lock already forks ms (top-level 2.1.3 + nested 2.0.0 under debug@2.6.9). Adding the unrelated
        // leaf is-number@7.0.0 leaves both ms placements byte-identical; only the importer edge and a new
        // node_modules/is-number entry are inserted (sorted among the nested keys).
        assertDisjointAddByteExact("lock/npm/disjoint-add", "is-number", "7.0.0");
    }

    @Test
    void disjointAddV2() {
        // The same add into a lockfileVersion 2 lock: is-number lands in both the packages map and the legacy
        // dependencies tree; debug's nested ms subtree is untouched in both.
        assertDisjointAddByteExact("lock/npm/disjoint-add-v2", "is-number", "7.0.0");
    }

    // --- a name that collides with the fork still fails loud -------------

    @Test
    void entangledAddFailsLoud() {
        // beta is only present nested under alpha. Adding beta at top-level could make npm re-hoist the fork, so
        // the disjoint relaxation refuses: only adds disjoint from every nested name are byte-reproducible.
        routes.put(REG + "beta", "{\"name\":\"beta\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "beta/1.0.0",
                "{\"name\":\"beta\",\"version\":\"1.0.0\"," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-BETA1\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"alpha\": \"^1.0.0\"}},\n" +
                "    \"node_modules/alpha\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/alpha/-/alpha-1.0.0.tgz\", \"integrity\": \"sha512-ALPHA1\"},\n" +
                "    \"node_modules/alpha/node_modules/beta\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/beta/-/beta-1.0.0.tgz\", \"integrity\": \"sha512-BETA1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"alpha\":\"^1.0.0\",\"beta\":\"^1.0.0\"}}",
                "{\"dependencies\":{\"alpha\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("nested placement");
    }

    private void assertDisjointAddByteExact(String dir, String name, String version) {
        routes.put(REG + name.replace("/", "%2F"), resource(dir + "/http/" + name));
        routes.put(REG + name.replace("/", "%2F") + "/" + version, resource(dir + "/http/" + name + "-" + version));
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
                {"lock/npm/disjoint-add", "3"},
                {"lock/npm/disjoint-add-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
