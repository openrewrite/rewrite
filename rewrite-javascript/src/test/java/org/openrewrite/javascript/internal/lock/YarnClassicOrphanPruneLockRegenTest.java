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
 * A yarn-classic bump whose new version drops a dependency edge: the bumped block loses its
 * {@code dependencies:} section and every block left unreachable from the {@code package.json} roots is GC'd.
 * Byte-exact vs real yarn 1.22.22.
 */
class YarnClassicOrphanPruneLockRegenTest extends LockRegenTestSupport {

    @Test
    void orphanPrune() {
        // semver 7.5.4 -> 7.6.3 drops its lru-cache edge; lru-cache (and its private yallist) are then orphaned.
        String dir = "lock/yarn-classic/orphan-prune";
        routes.put(REG + "semver", resource(dir + "/http/semver"));
        routes.put(REG + "semver/7.5.4", resource(dir + "/http/semver-7.5.4"));
        routes.put(REG + "semver/7.6.3", resource(dir + "/http/semver-7.6.3"));

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg-after"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertYarnReproduces("lock/yarn-classic/orphan-prune/pkg-before", "lock/yarn-classic/orphan-prune/before");
        assertYarnReproduces("lock/yarn-classic/orphan-prune/pkg-after", "lock/yarn-classic/orphan-prune/after");
    }
}
