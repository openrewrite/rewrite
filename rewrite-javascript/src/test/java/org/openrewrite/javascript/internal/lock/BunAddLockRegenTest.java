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
 * The differential harness for Phase B bun adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code bun.lock}, the recipe's add edit, and recorded registry HTTP —
 * through {@link NativeLockEngine} entirely OFFLINE (a stub {@code HttpSender} serves the captured
 * packuments/manifests), then asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code bun install --lockfile-only}. Byte-identity is the whole contract: the engine either
 * reproduces exactly what bun would write or fails loud.
 * <p>
 * The goldens were produced with bun 1.3.10 against registry.npmjs.org. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealBun()}: it re-runs a real bun over every committed {@code pkg-before} and
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} — proving
 * both goldens are genuine bun output (bun reproduces {@code after} from {@code before + edit}). The minimal
 * packuments and verbatim single-version manifests under each fixture's {@code http/} were captured from the
 * registry (see {@code NpmClosureAddLockRegenTest} for the throwaway node script).
 */
class BunAddLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact adds (goldens from real bun 1.3.10) -------------------

    @Test
    void leafAdd() {
        // is-number@7.0.0 is a scalar-only leaf (empty metadata tuple); it inserts before the existing ms.
        assertAddByteExact("lock/bun/add-leaf", new String[][]{{"is-number", "7.0.0"}});
    }

    @Test
    void cleanClosureAdd() {
        // is-odd@3.0.1 -> is-number@6.0.0: a clean two-member closure, both hoisting top-level, no conflict.
        // is-odd's tuple records { "dependencies": { "is-number": "^6.0.0" } }; is-number's is an empty leaf.
        assertAddByteExact("lock/bun/add-closure",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    // --- reverse-dependent nest (Phase B I5) -----------------------------

    @Test
    void nestOldVersionUnderReverseDependent() {
        // Root deps debug@2.6.9 (which pins ms@2.0.0) + ms. Bumping ms -> 2.1.3 keeps ms@2.1.3 top-level and
        // relocates ms@2.0.0 to the "debug/ms" tuple (appended after the top-level entries) — debug's copy.
        assertAddByteExact("lock/bun/nest-basic", new String[][]{{"ms", "2.0.0"}, {"ms", "2.1.3"}});
    }

    @Test
    void addNestsConflictingTransitiveUnderParent() {
        // Lock has ms@2.1.3 top-level (root pins it exactly). Adding debug@2.6.9 (needs ms@2.0.0) builds a
        // fresh "debug/ms" tuple for ms@2.0.0, leaving the top-level ms@2.1.3 untouched.
        assertAddByteExact("lock/bun/add-nest", new String[][]{{"debug", "2.6.9"}, {"ms", "2.0.0"}});
    }

    // --- fail loud (a peer in the closure, or a transitive conflict, defers) ---

    @Test
    void peerCarryingAddFailsLoud() {
        // bun records peerDependencies in the tuple and auto-installs non-optional peers; neither the
        // deps-or-empty placement models, so any peer defers.
        routes.put(REG + "has-peer", "{\"name\":\"has-peer\",\"dist-tags\":{},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "has-peer/1.0.0",
                "{\"name\":\"has-peer\",\"version\":\"1.0.0\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/has-peer/-/has-peer-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PEER\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                editedPkg("has-peer", "^1.0.0"),
                resource("lock/bun/add-leaf/pkg-before"),
                resource("lock/bun/add-leaf/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        // The surgical add gate defers on the peer surface (the resolver fallback hits the unrouted sibling first).
        assertThat(result.getFailure().getDetail()).contains("declares peerDependencies").contains("has-peer");
    }

    @Test
    void conflictingTransitiveAddFailsLoud() {
        // Adding is-odd (needs is-number ^6.0.0) into a lock that already pins is-number 7.0.0 top-level:
        // bun would nest a second copy (is-odd/is-number), a fork the flat placement refuses to guess.
        routes.put(REG + "is-odd", resource("lock/bun/add-closure/http/is-odd"));
        routes.put(REG + "is-odd/3.0.1", resource("lock/bun/add-closure/http/is-odd-3.0.1"));

        String original = resource("lock/bun/add-leaf/pkg-after"); // bun-leaf: is-number ^7.0.0, ms ^2.1.3
        String edited = "{\n  \"name\": \"bun-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"is-number\": \"^7.0.0\",\n    \"is-odd\": \"^3.0.1\",\n    \"ms\": \"^2.1.3\"\n  }\n}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                edited, original, resource("lock/bun/add-leaf/after"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-number").contains("nest");
    }

    private static String editedPkg(String name, String range) {
        return "{\n  \"name\": \"bun-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"" + name + "\": \"" + range + "\",\n    \"ms\": \"^2.1.3\"\n  }\n}\n";
    }

    /**
     * Replay {@code dir}'s fixture offline and assert the engine output equals {@code dir/after} byte-for-byte.
     * Each {@code {name, version}} maps to two recorded routes: {@code http/<name>} (packument) and
     * {@code http/<name>-<version>} (manifest).
     */
    private void assertAddByteExact(String dir, String[][] packages) {
        for (String[] pkg : packages) {
            String route = REG + pkg[0].replace("/", "%2F");
            routes.put(route, resource(dir + "/http/" + pkg[0]));
            routes.put(route + "/" + pkg[1], resource(dir + "/http/" + pkg[0] + "-" + pkg[1]));
        }
        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        String[] fixtures = {"lock/bun/add-leaf", "lock/bun/add-closure", "lock/bun/nest-basic", "lock/bun/add-nest"};
        for (String fixture : fixtures) {
            assertBunReproduces(fixture + "/pkg-before", fixture + "/before");
            assertBunReproduces(fixture + "/pkg-after", fixture + "/after");
        }
    }
}
