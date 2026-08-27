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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Promoting an already-present transitive to a direct dependency in a classic {@code yarn.lock}. When the
 * locked version satisfies the newly declared range, yarn merges the new selector into the block's
 * {@code sortAlpha}-ordered header and adds no blocks — so the engine resolves it offline, without the registry.
 * Byte-exact vs real yarn 1.22.22.
 */
class YarnClassicPromoteLockRegenTest extends LockRegenTestSupport {

    @Test
    void mergeSelector() {
        // ms is a transitive of debug at ms@^2.1.3; adding ms@^2.0.0 (satisfied by 2.1.3) merges the header.
        assertPromotes("lock/yarn-classic/promote-merge");
    }

    @Test
    void alreadyDeclaredIsNoOp() {
        // Adding ms@^2.1.3, the exact selector already present, leaves the lock byte-identical.
        assertPromotes("lock/yarn-classic/promote-noop");
    }

    private void assertPromotes(String dir) {
        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg-after"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        for (String dir : new String[]{"lock/yarn-classic/promote-merge", "lock/yarn-classic/promote-noop"}) {
            assertYarnReproduces(dir + "/pkg-before", dir + "/before");
            assertYarnReproduces(dir + "/pkg-after", dir + "/after");
        }
    }
}
