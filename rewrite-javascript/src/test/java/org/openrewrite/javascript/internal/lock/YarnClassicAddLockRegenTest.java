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
 * The differential harness for Phase B yarn-classic adds. Each test replays a fixture — a before
 * {@code package.json}, a before {@code yarn.lock}, the recipe's add edit, and recorded registry HTTP —
 * through {@link NativeLockEngine} entirely OFFLINE (a stub {@code HttpSender} serves the captured
 * packuments/manifests), then asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded
 * from a real {@code yarn install}. Byte-identity is the whole contract: the engine either reproduces exactly
 * what yarn would write or fails loud.
 * <p>
 * The goldens were produced with yarn 1.22.22 against registry.yarnpkg.com. To re-derive/verify them, enable
 * {@link #recordGoldensWithRealYarn()}: it re-runs a real yarn over every committed {@code pkg-before} and
 * {@code pkg-after} and asserts the resulting lock equals the committed {@code before}/{@code after} — proving
 * both goldens are genuine yarn output (yarn reproduces {@code after} from {@code before + edit}). The minimal
 * packuments and verbatim single-version manifests under each fixture's {@code http/} are the same registry
 * responses the bun/npm harnesses capture (yarn mirrors the tarball host to registry.yarnpkg.com itself).
 */
class YarnClassicAddLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact adds (goldens from real yarn 1.22.22) -----------------

    @Test
    void leafAdd() {
        // is-number@7.0.0 is a leaf (its engines/bin never reach a yarn.lock block); it inserts before ms.
        assertAddByteExact("lock/yarn-classic/add-leaf", new String[][]{{"is-number", "7.0.0"}});
    }

    @Test
    void cleanClosureAdd() {
        // is-odd@3.0.1 -> is-number@6.0.0: a clean two-member closure. is-odd's block carries a dependencies
        // section (is-number "^6.0.0"); is-number's block header takes that range as its sole selector.
        assertAddByteExact("lock/yarn-classic/add-closure",
                new String[][]{{"is-odd", "3.0.1"}, {"is-number", "6.0.0"}});
    }

    // --- fail loud (a peer in the closure, or a transitive conflict, defers) ---

    @Test
    void peerCarryingAddFailsLoud() {
        // yarn resolves peers into further blocks the clean placement does not model, so any peer defers.
        routes.put(REG + "has-peer", "{\"name\":\"has-peer\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{}}}");
        routes.put(REG + "has-peer/1.0.0",
                "{\"name\":\"has-peer\",\"version\":\"1.0.0\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}," +
                        "\"dist\":{\"shasum\":\"1111111111111111111111111111111111111111\"," +
                        "\"tarball\":\"https://registry.npmjs.org/has-peer/-/has-peer-1.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PEER\"}}");

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                editedPkg("has-peer", "^1.0.0"),
                resource("lock/yarn-classic/add-leaf/pkg-before"),
                resource("lock/yarn-classic/add-leaf/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        // The surgical add gate defers on the peer surface (the resolver fallback hits the unrouted sibling first).
        assertThat(result.getFailure().getDetail()).contains("declares peerDependencies").contains("has-peer");
    }

    @Test
    void conflictingTransitiveAddFailsLoud() {
        // Adding is-odd (needs is-number ^6.0.0) into a lock that already has an is-number@7.0.0 block: yarn
        // would fork a second is-number block, the merged-selector/dedup nuance the clean placement refuses.
        routes.put(REG + "is-odd", resource("lock/yarn-classic/add-closure/http/is-odd"));
        routes.put(REG + "is-odd/3.0.1", resource("lock/yarn-classic/add-closure/http/is-odd-3.0.1"));

        String original = resource("lock/yarn-classic/add-leaf/pkg-after"); // is-number ^7.0.0, ms ^2.1.3
        String edited = "{\n  \"name\": \"yarn-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
                "    \"is-number\": \"^7.0.0\",\n    \"is-odd\": \"^3.0.1\",\n    \"ms\": \"^2.1.3\"\n  }\n}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                edited, original, resource("lock/yarn-classic/add-leaf/after"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-number").contains("already present");
    }

    private static String editedPkg(String name, String range) {
        return "{\n  \"name\": \"yarn-leaf\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n" +
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
        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg-after"),
                resource(dir + "/pkg-before"),
                resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    // --- live re-record / provenance check (disabled: needs yarn + network) ---

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        String[] fixtures = {"lock/yarn-classic/add-leaf", "lock/yarn-classic/add-closure"};
        for (String fixture : fixtures) {
            assertYarnReproduces(fixture + "/pkg-before", fixture + "/before");
            assertYarnReproduces(fixture + "/pkg-after", fixture + "/after");
        }
    }
}
