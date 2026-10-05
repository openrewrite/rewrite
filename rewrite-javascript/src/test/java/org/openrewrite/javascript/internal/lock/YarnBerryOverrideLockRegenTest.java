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
 * yarn berry (4.9.2) {@code resolutions}: a plain key applies to every requirer, a direct dependency included, and
 * {@code parent/child} to the parent's own edges at any depth. The overridden entry is keyed by the resolution's
 * descriptor while each requirer keeps its original range; nothing else in the lock records the resolution.
 */
class YarnBerryOverrideLockRegenTest extends OverrideLockRegenTestSupport {

    /** kind-of is itself transitive: berry matches a parent anywhere, unlike classic. */
    @Test
    void parentScopedResolutionAppliesBelowATransitiveParent() {
        assertMatchesBerry("unanchored");
    }

    @Test
    void versionQualifiedParentResolutionApplies() {
        assertMatchesBerry("versioned-parent");
    }

    @Test
    void rangeResolutionKeysTheEntryByTheRange() {
        assertMatchesBerry("range-value");
    }

    @Test
    void changedResolutionMovesThePackage() {
        assertMatchesBerry("change");
    }

    @Test
    void removedResolutionRestoresTheRequestedRange() {
        assertMatchesBerry("remove");
    }

    /** The workspace keeps its declared range; only the entry moves. */
    @Test
    void resolutionAppliesToADirectDependency() {
        assertMatchesBerry("direct");
    }

    @Test
    void addedDependencyReceivesAnExistingResolution() {
        assertMatchesBerry("with-dep-add");
    }

    private void assertMatchesBerry(String scenario) {
        assertMatchesTool(PackageManager.YarnBerry, "lock/yarn-berry/overrides/" + scenario);
    }
}
