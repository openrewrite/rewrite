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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

/**
 * bun (1.3.10) overrides: flat keys from {@code overrides} and {@code resolutions}, applied to every requirer
 * including a direct dependency, and recorded sorted in the lock's own {@code overrides} object.
 */
class BunOverrideLockRegenTest extends OverrideLockRegenTestSupport {

    @Test
    void addedOverridesAreAppliedAndRecordedSorted() {
        assertMatchesBun("order");
    }

    @Test
    void resolutionsAreRecordedAsOverrides() {
        assertMatchesBun("add-resolution");
    }

    @Test
    void overridesWinOverResolutionsForTheSamePackage() {
        assertMatchesBun("both");
    }

    @Test
    void changedOverrideMovesThePackage() {
        assertMatchesBun("change");
    }

    @Test
    void removedOverrideRestoresTheRequestedRange() {
        assertMatchesBun("remove");
    }

    @Test
    void overrideAppliesToADirectDependency() {
        assertMatchesBun("direct");
    }

    @Test
    void addedDependencyReceivesAnExistingOverride() {
        assertMatchesBun("with-dep-add");
    }

    /** The added override reaches nothing, but bun still records it, which an in-place patch of the add would miss. */
    @Test
    void unreachedOverrideAddedWithADependencyIsRecorded() {
        assertMatchesBun("unrelated-override-with-add");
    }

    private void assertMatchesBun(String scenario) {
        assertMatchesTool(PackageManager.Bun, "lock/bun/overrides/" + scenario);
    }
}
