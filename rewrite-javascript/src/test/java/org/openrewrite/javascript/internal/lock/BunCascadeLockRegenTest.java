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
 * The bun analogue of {@link NpmCascadeLockRegenTest}: a direct-dependency bump whose new version forces a
 * shared transitive to move. Bumping debug ^2.6.9 -> ^3.0.0 resolves debug 2.6.9 -> 3.2.7, whose manifest now
 * requires ms ^2.1.1; the locked ms 2.0.0 no longer satisfies it, so ms moves to 2.1.3. The engine rewrites the
 * debug tuple (locator, its own {@code dependencies} metadata ms 2.0.0 -> ^2.1.1, integrity), the ms tuple
 * (locator + integrity), and the importer edge, byte-exact against a golden recorded from real bun 1.3.10.
 */
class BunCascadeLockRegenTest extends LockRegenTestSupport {

    @Test
    void bumpForcesSharedTransitiveMove() {
        route("debug", "2.6.9", "3.2.7");
        route("ms", "2.0.0", "2.1.3");

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource("lock/bun/cascade/pkg-after"),
                resource("lock/bun/cascade/pkg-before"),
                resource("lock/bun/cascade/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/bun/cascade/after"));
    }

    @Test
    void sharedTransitiveMoveFailsLoud() {
        // A second top-level package (serve-static) also depends on ms, so moving ms would fork; defer.
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n  \"workspaces\": {\n" +
                "    \"\": {\n      \"name\": \"shared\",\n      \"dependencies\": {\n" +
                "        \"debug\": \"^2.6.9\",\n        \"other\": \"1.0.0\",\n      },\n    },\n  },\n  \"packages\": {\n" +
                "    \"debug\": [\"debug@2.6.9\", \"\", { \"dependencies\": { \"ms\": \"2.0.0\" } }, \"sha512-D\"],\n" +
                "    \"ms\": [\"ms@2.0.0\", \"\", {}, \"sha512-M\"],\n" +
                "    \"other\": [\"other@1.0.0\", \"\", { \"dependencies\": { \"ms\": \"2.0.0\" } }, \"sha512-O\"],\n" +
                "  }\n}\n";
        String before = "{\n  \"name\": \"shared\",\n  \"dependencies\": {\n    \"debug\": \"^2.6.9\",\n    \"other\": \"1.0.0\"\n  }\n}\n";
        String after = "{\n  \"name\": \"shared\",\n  \"dependencies\": {\n    \"debug\": \"^3.0.0\",\n    \"other\": \"1.0.0\"\n  }\n}\n";
        route("debug", "2.6.9", "3.2.7");
        route("ms", "2.0.0", "2.1.3");

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                after, before, lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("ms").contains("more than the upgraded debug");
    }

    /** Register the abbreviated packument plus each version's manifest for {@code name}. */
    private void route(String name, String... versions) {
        routes.put(REG + name, resource("lock/bun/cascade/http/" + name));
        for (String version : versions) {
            routes.put(REG + name + "/" + version, resource("lock/bun/cascade/http/" + name + "-" + version));
        }
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        assertBunReproduces("lock/bun/cascade/pkg-before", "lock/bun/cascade/before");
        assertBunReproduces("lock/bun/cascade/pkg-after", "lock/bun/cascade/after");
    }
}
