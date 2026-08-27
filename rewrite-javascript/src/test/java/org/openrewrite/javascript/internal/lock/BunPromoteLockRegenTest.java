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
 * The bun analogue of {@link NpmPromoteLockRegenTest}: declaring an already-installed transitive as a direct
 * dependency. Bun keys {@code packages} by name and records no requirer, so a satisfying locked version just
 * gains the workspace importer edge — its tuple is untouched. Declaring ms (a transitive of debug, locked at
 * 2.0.0) with ^2.0.0 adds only {@code "ms": "^2.0.0"} to the importer, byte-exact against real bun 1.3.10.
 */
class BunPromoteLockRegenTest extends LockRegenTestSupport {

    @Test
    void promoteTransitiveAddsImporterEdgeOnly() {
        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource("lock/bun/promote/pkg-after"),
                resource("lock/bun/promote/pkg-before"),
                resource("lock/bun/promote/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/bun/promote/after"));
    }

    @Test
    void promoteExcludingLockedVersionFailsLoud() {
        // The lock pins ms 2.0.0 (a transitive of debug); declaring ms ^2.1.0 excludes it, so bun would fork/upgrade.
        String lock = "{\n  \"lockfileVersion\": 1,\n  \"configVersion\": 1,\n  \"workspaces\": {\n" +
                "    \"\": {\n      \"name\": \"promote-fix\",\n      \"dependencies\": {\n" +
                "        \"debug\": \"2.6.9\",\n      },\n    },\n  },\n  \"packages\": {\n" +
                "    \"debug\": [\"debug@2.6.9\", \"\", { \"dependencies\": { \"ms\": \"2.0.0\" } }, \"sha512-D\"],\n" +
                "    \"ms\": [\"ms@2.0.0\", \"\", {}, \"sha512-M\"],\n" +
                "  }\n}\n";
        String before = "{\n  \"name\": \"promote-fix\",\n  \"dependencies\": {\n    \"debug\": \"2.6.9\"\n  }\n}\n";
        String after = "{\n  \"name\": \"promote-fix\",\n  \"dependencies\": {\n    \"debug\": \"2.6.9\",\n    \"ms\": \"^2.1.0\"\n  }\n}\n";

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                after, before, lock, null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("ms").contains("fork");
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        assertBunReproduces("lock/bun/promote/pkg-before", "lock/bun/promote/before");
        assertBunReproduces("lock/bun/promote/pkg-after", "lock/bun/promote/after");
    }
}
