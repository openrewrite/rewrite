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
 * The differential harness for the npm peer-satisfied fast-path: a direct-dependency bump whose new version only
 * changes {@code peerDependencies} (or {@code peerDependenciesMeta}), where the already-installed providers still
 * satisfy the new ranges. npm adds and moves nothing, so the closure is unchanged and only the bumped entry's peer
 * fields are rewritten; the engine writes them through instead of failing loud (as it did on any peer delta before).
 * Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender} serves captured
 * packuments/manifests for the bumped package only; the peer providers are read from the lock) through
 * {@link NativeLockEngine} and asserts the emitted lock is BYTE-IDENTICAL to a golden {@code after} recorded from a
 * real {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2. To re-derive/verify them, enable {@link #recordGoldensWithRealNpm()}.
 */
class NpmPeerSurfaceLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact peer write-through (goldens from real npm 11.6.2) ---

    @Test
    void peerRangeWidenedProviderSatisfies() {
        // Bump use-sync-external-store 1.2.2 -> 1.4.0: the sole surface delta widens its `react` peer range from
        // "^16 || ^17 || ^18" to "... || ^19". The installed react@18.3.1 satisfies both, so nothing moves.
        assertPeerByteExact("lock/npm/peer-widen", "use-sync-external-store", "1.2.2", "1.4.0");
    }

    @Test
    void optionalPeerRangeChangedProviderAbsent() {
        // Bump ws 8.11.0 -> 8.12.0: its `utf-8-validate` peer range changes ("^5.0.2" -> ">=5.0.2"). Both peers are
        // optional and neither is installed, so they are satisfied-if-absent and the graph is unchanged.
        assertPeerByteExact("lock/npm/peer-optional-absent", "ws", "8.11.0", "8.12.0");
    }

    // --- a non-optional peer that would need auto-installing / re-resolving: fail loud ---

    @Test
    void absentNonOptionalPeerFailsLoud() {
        // Bump gains a non-optional peer that is not installed; auto-installing a peer reshapes the graph.
        routes.put(REG + "needy", "{\"name\":\"needy\",\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "needy/1.0.0", "{\"name\":\"needy\",\"version\":\"1.0.0\"}");
        routes.put(REG + "needy/2.0.0",
                "{\"name\":\"needy\",\"version\":\"2.0.0\"," +
                        "\"peerDependencies\":{\"vue\":\"^3.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/needy/-/needy-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-NEEDY2\"}}");

        Result result = bump("needy", "^1.0.0", "^2.0.0");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peer vue is not installed");
    }

    @Test
    void peerProviderDoesNotSatisfyNewRangeFailsLoud() {
        // The installed provider no longer satisfies the narrowed peer range, so npm would resolve a new one.
        routes.put(REG + "picky", "{\"name\":\"picky\",\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "picky/1.0.0",
                "{\"name\":\"picky\",\"version\":\"1.0.0\",\"peerDependencies\":{\"react\":\"^17.0.0 || ^18.0.0\"}}");
        routes.put(REG + "picky/2.0.0",
                "{\"name\":\"picky\",\"version\":\"2.0.0\",\"peerDependencies\":{\"react\":\"^19.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/picky/-/picky-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-PICKY2\"}}");

        Result result = bump("picky", "^1.0.0", "^2.0.0");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("does not satisfy the new range");
    }

    @Test
    void peerBecomesOptionalFailsLoud() {
        // The peer list is unchanged but the new version marks react optional; npm may drop an auto-installed
        // provider, so the meta delta is not a safe write-through.
        routes.put(REG + "flippy", "{\"name\":\"flippy\",\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "flippy/1.0.0",
                "{\"name\":\"flippy\",\"version\":\"1.0.0\",\"peerDependencies\":{\"react\":\"^18.0.0\"}}");
        routes.put(REG + "flippy/2.0.0",
                "{\"name\":\"flippy\",\"version\":\"2.0.0\",\"peerDependencies\":{\"react\":\"^18.0.0\"}," +
                        "\"peerDependenciesMeta\":{\"react\":{\"optional\":true}}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/flippy/-/flippy-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-FLIPPY2\"}}");

        Result result = bump("flippy", "^1.0.0", "^2.0.0");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependenciesMeta changed");
    }

    @Test
    void droppedPeerFailsLoud() {
        // The new version drops the react-dom peer; if it was auto-installed only for this package npm removes it,
        // which the write-through cannot prove, so it defers.
        routes.put(REG + "shrinky", "{\"name\":\"shrinky\",\"dist-tags\":{\"latest\":\"2.0.0\"},\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "shrinky/1.0.0",
                "{\"name\":\"shrinky\",\"version\":\"1.0.0\",\"peerDependencies\":{\"react\":\"^18.0.0\",\"react-dom\":\"^18.0.0\"}}");
        routes.put(REG + "shrinky/2.0.0",
                "{\"name\":\"shrinky\",\"version\":\"2.0.0\",\"peerDependencies\":{\"react\":\"^18.0.0\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/shrinky/-/shrinky-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-SHRINKY2\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"shrinky\": \"^1.0.0\", \"react\": \"18.3.1\", \"react-dom\": \"18.3.1\"}},\n" +
                "    \"node_modules/react\": {\"version\": \"18.3.1\", \"resolved\": \"https://registry.npmjs.org/react/-/react-18.3.1.tgz\", \"integrity\": \"sha512-REACT\"},\n" +
                "    \"node_modules/react-dom\": {\"version\": \"18.3.1\", \"resolved\": \"https://registry.npmjs.org/react-dom/-/react-dom-18.3.1.tgz\", \"integrity\": \"sha512-REACTDOM\"},\n" +
                "    \"node_modules/shrinky\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/shrinky/-/shrinky-1.0.0.tgz\", \"integrity\": \"sha512-OLD\"}\n" +
                "  }\n" +
                "}\n";
        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"shrinky\":\"^2.0.0\",\"react\":\"18.3.1\",\"react-dom\":\"18.3.1\"}}",
                "{\"dependencies\":{\"shrinky\":\"^1.0.0\",\"react\":\"18.3.1\",\"react-dom\":\"18.3.1\"}}",
                lock, null, Paths.get("package.json"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependencies changed");
    }

    /** A minimal lock with the bumped package plus an installed react@18.3.1 peer provider (top-level). */
    private Result bump(String name, String oldConstraint, String newConstraint) {
        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"" + name + "\": \"" + oldConstraint + "\", \"react\": \"18.3.1\"}},\n" +
                "    \"node_modules/react\": {\"version\": \"18.3.1\", \"resolved\": \"https://registry.npmjs.org/react/-/react-18.3.1.tgz\", \"integrity\": \"sha512-REACT\"},\n" +
                "    \"node_modules/" + name + "\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/" + name + "/-/" + name + "-1.0.0.tgz\", \"integrity\": \"sha512-OLD\"}\n" +
                "  }\n" +
                "}\n";
        return NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"" + name + "\":\"" + newConstraint + "\",\"react\":\"18.3.1\"}}",
                "{\"dependencies\":{\"" + name + "\":\"" + oldConstraint + "\",\"react\":\"18.3.1\"}}",
                lock, null, Paths.get("package.json"), ctx);
    }

    private void assertPeerByteExact(String dir, String name, String oldV, String newV) {
        String route = REG + name.replace("/", "%2F");
        routes.put(route, resource(dir + "/http/" + name));
        routes.put(route + "/" + oldV, resource(dir + "/http/" + name + "-" + oldV));
        routes.put(route + "/" + newV, resource(dir + "/http/" + name + "-" + newV));

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
        String[] dirs = {"lock/npm/peer-widen", "lock/npm/peer-optional-absent"};
        for (String dir : dirs) {
            assertNpmReproduces(dir + "/pkg-before", dir + "/before", "3");
            assertNpmReproduces(dir + "/pkg-after", dir + "/after", "3");
        }
    }
}
