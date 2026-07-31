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
 * The differential harness for Phase B npm {@code funding} write-through: a direct-dependency bump whose new
 * version adds (or changes) a string-form {@code funding} field. npm records it in the {@code packages} entry
 * as {@code {"url": "<string>"}} at its sorted position (the object group, after {@code engines}/{@code bin});
 * the engine writes it through instead of failing loud, reusing the same object-member graft as engines (T13).
 * Each byte-exact test replays a fixture entirely OFFLINE (a stub {@code HttpSender} serves captured
 * packuments/manifests) through {@link NativeLockEngine} and asserts the emitted lock is BYTE-IDENTICAL to a
 * golden {@code after} recorded from a real {@code npm install --package-lock-only}.
 * <p>
 * The goldens were produced with npm 11.6.2. To re-derive/verify them, enable {@link #recordGoldensWithRealNpm()}.
 * The isolated fixture bumps {@code is-stream 2.0.0 -> 2.0.1}, whose sole surface delta is the added funding.
 */
class NpmFundingWriteThroughLockRegenTest extends LockRegenTestSupport {

    // --- byte-exact funding write-through (goldens from real npm 11.6.2) ---

    @Test
    void fundingChangeV3() {
        // Bump is-stream 2.0.0 -> 2.0.1: the sole surface delta is a string funding field npm records as
        // {"url": ...} in the packages entry, sorted into the object group after `engines`.
        assertFundingByteExact("lock/npm/funding-change",
                new String[][]{{"is-stream", "2.0.0", "2.0.1"}});
    }

    @Test
    void fundingChangeV2() {
        // The same add into a lockfileVersion 2 lock: funding lands only in the `packages` entry; the legacy
        // `dependencies` tree carries just version/resolved/integrity (npm writes no funding there).
        assertFundingByteExact("lock/npm/funding-change-v2",
                new String[][]{{"is-stream", "2.0.0", "2.0.1"}});
    }

    // --- non-string funding npm reshapes: fail loud rather than mis-serialize ---

    @Test
    void nonStringFundingFailsLoud() {
        // Bump funky 1.0.0 -> 2.0.0, whose new version declares an object-form funding. Only the string form
        // is byte-reproducible (npm reshapes object/array funding), so the write-through refuses.
        routes.put(REG + "funky", "{\"versions\":{\"1.0.0\":{},\"2.0.0\":{}}}");
        routes.put(REG + "funky/1.0.0", "{\"name\":\"funky\",\"version\":\"1.0.0\"}");
        routes.put(REG + "funky/2.0.0",
                "{\"name\":\"funky\",\"version\":\"2.0.0\"," +
                        "\"funding\":{\"type\":\"opencollective\",\"url\":\"https://opencollective.com/funky\"}," +
                        "\"dist\":{\"tarball\":\"https://registry.npmjs.org/funky/-/funky-2.0.0.tgz\"," +
                        "\"integrity\":\"sha512-FUNKY2\"}}");

        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"dependencies\": {\"funky\": \"^1.0.0\"}},\n" +
                "    \"node_modules/funky\": {\"version\": \"1.0.0\", \"resolved\": \"https://registry.npmjs.org/funky/-/funky-1.0.0.tgz\", \"integrity\": \"sha512-FUNKY1\"}\n" +
                "  }\n" +
                "}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Npm,
                "{\"dependencies\":{\"funky\":\"^2.0.0\"}}",
                "{\"dependencies\":{\"funky\":\"^1.0.0\"}}",
                lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("non-string form");
    }

    private void assertFundingByteExact(String dir, String[][] packages) {
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
                {"lock/npm/funding-change", "3"},
                {"lock/npm/funding-change-v2", "2"},
        };
        for (String[] fixture : fixtures) {
            assertNpmReproduces(fixture[0] + "/pkg-before", fixture[0] + "/before", fixture[1]);
            assertNpmReproduces(fixture[0] + "/pkg-after", fixture[0] + "/after", fixture[1]);
        }
    }
}
