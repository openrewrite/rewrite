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
 * The bun analogue of {@link NpmOrphanPruneLockRegenTest}: a bump whose new version drops a dependency edge.
 * Bumping semver 7.5.4 -> 7.6.3 drops the lru-cache edge (and lru-cache's own yallist), so the engine removes
 * the {@code dependencies} map from semver's tuple metadata (keeping its {@code bin} sibling) and GCs the now
 * unreachable lru-cache and yallist tuples, byte-exact against a golden recorded from real bun 1.3.10.
 */
class BunOrphanPruneLockRegenTest extends LockRegenTestSupport {

    @Test
    void bumpDropsEdgeAndGarbageCollects() {
        routes.put(REG + "semver", resource("lock/bun/orphan-prune/http/semver"));
        routes.put(REG + "semver/7.5.4", resource("lock/bun/orphan-prune/http/semver-7.5.4"));
        routes.put(REG + "semver/7.6.3", resource("lock/bun/orphan-prune/http/semver-7.6.3"));

        Result result = NativeLockEngine.regenerate(PackageManager.Bun,
                resource("lock/bun/orphan-prune/pkg-after"),
                resource("lock/bun/orphan-prune/pkg-before"),
                resource("lock/bun/orphan-prune/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource("lock/bun/orphan-prune/after"));
    }

    // --- live re-record / provenance check (disabled: needs bun + network) ---

    @Test
    @Disabled("live: runs real bun 1.3.10 against registry.npmjs.org to re-derive and verify the goldens")
    void recordGoldensWithRealBun() throws Exception {
        assertBunReproduces("lock/bun/orphan-prune/pkg-before", "lock/bun/orphan-prune/before");
        assertBunReproduces("lock/bun/orphan-prune/pkg-after", "lock/bun/orphan-prune/after");
    }
}
