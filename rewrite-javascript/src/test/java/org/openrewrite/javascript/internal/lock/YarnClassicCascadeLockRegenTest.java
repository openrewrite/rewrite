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
 * The differential harness for a Phase B yarn-classic cascade: a direct-dep bump whose new manifest changes a
 * dependency constraint, forcing a currently-locked transitive to move. The bumped block re-pins the changed
 * constraint in its {@code dependencies:} section and the forced transitive's block re-heads its single selector
 * to the new range; both re-resolve (integrity/shasum from the registry, no tarball needed for classic). The
 * emitted lock must be BYTE-IDENTICAL to a golden recorded from a real {@code yarn install} (yarn 1.22.22).
 */
class YarnClassicCascadeLockRegenTest extends LockRegenTestSupport {

    @Test
    void cascade() {
        // debug ^2.6.9 -> ^3.0.0 (3.2.7) changes its ms dep "2.0.0" -> "^2.1.1", forcing ms 2.0.0 -> 2.1.3 and
        // re-heading its block ms@2.0.0: -> ms@^2.1.1:.
        String dir = "lock/yarn-classic/cascade";
        route(dir, "debug", "2.6.9");
        route(dir, "debug", "3.2.7");
        route(dir, "ms", "2.0.0");
        route(dir, "ms", "2.1.3");

        Result result = NativeLockEngine.regenerate(PackageManager.YarnClassic,
                resource(dir + "/pkg-after"), resource(dir + "/pkg-before"), resource(dir + "/before"),
                null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }

    private void route(String dir, String name, String version) {
        routes.put(REG + name, resource(dir + "/http/" + name));
        routes.put(REG + name + "/" + version, resource(dir + "/http/" + name + "-" + version));
    }

    @Test
    @Disabled("live: runs real yarn 1.22.22 against registry.yarnpkg.com to re-derive and verify the goldens")
    void recordGoldensWithRealYarn() throws Exception {
        assertYarnReproduces("lock/yarn-classic/cascade/pkg-before", "lock/yarn-classic/cascade/before");
        assertYarnReproduces("lock/yarn-classic/cascade/pkg-after", "lock/yarn-classic/cascade/after");
    }
}
