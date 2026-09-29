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
 * yarn classic (1.22.22) {@code resolutions}. A plain key reaches transitive requirers only, and
 * {@code parent/child} only below a directly declared parent. Whenever yarn re-resolves, every resolution key is
 * also requested at the top level as {@code child@value}, with its whole closure, even when nothing else needs it;
 * when every requested pattern is already locked and satisfied, yarn leaves the lock alone.
 */
class YarnClassicOverrideLockRegenTest extends OverrideLockRegenTestSupport {

    @Test
    void parentScopedResolutionAppliesBelowADirectParent() {
        assertMatchesClassic("anchored");
    }

    /** kind-of is transitive, and classic anchors a parent at the project, so nothing moves. */
    @Test
    void parentScopedResolutionIsInertBelowATransitiveParent() {
        assertMatchesClassic("unanchored");
    }

    /** The requester's pattern and the resolution's share one entry. */
    @Test
    void rangeResolutionJoinsTheEntryHeader() {
        assertMatchesClassic("range-value");
    }

    @Test
    void changedResolutionMovesThePackage() {
        assertMatchesClassic("change");
    }

    @Test
    void removedResolutionRestoresTheRequestedRange() {
        assertMatchesClassic("remove");
    }

    /** Classic never resolves a direct dependency through a resolution. */
    @Test
    void resolutionOfADirectDependencyIsInert() {
        assertMatchesClassic("direct");
    }

    @Test
    void addedDependencyReceivesAnExistingResolution() {
        assertMatchesClassic("with-dep-add");
    }

    /** Classic reads {@code **}{@code /is-buffer} exactly as the plain key, request included. */
    @Test
    void globResolutionIsThePlainKey() {
        assertMatchesClassic("glob-with-dep-add");
    }

    /** A dependency add makes yarn re-resolve, which requests the unreached resolution with its closure. */
    @Test
    void reResolvingRequestsAnUnreachedResolutionWithItsClosure() {
        assertMatchesClassic("seed-with-deps");
    }

    /** The same request for an inert parent-scoped key, alongside the copy its real requirer resolves. */
    @Test
    void reResolvingRequestsAnInertParentScopedResolution() {
        assertMatchesClassic("seed-path");
    }

    /** A resolution added without a re-resolve was never requested; the next dependency change requests it. */
    @Test
    void dependencyChangeRequestsAResolutionAnEarlierInstallSkipped() {
        assertMatchesClassic("late-seed");
    }

    /** Every remaining pattern is still locked, but a changed dependency list still makes yarn rewrite the lock. */
    @Test
    void removedDependencyIsDroppedAlongsideResolutions() {
        assertMatchesClassic("removal");
    }

    /** Every requested pattern is still locked and satisfied, so yarn leaves the lock alone. */
    @Test
    void unreachedResolutionAloneLeavesTheLock() {
        assertMatchesClassic("unreached-add");
    }

    /** Classic ignores a version-qualified parent, so the lock is left alone. */
    @Test
    void versionQualifiedParentResolutionIsIgnored() {
        assertMatchesClassic("versioned-parent");
    }

    private void assertMatchesClassic(String scenario) {
        assertMatchesTool(PackageManager.YarnClassic, "lock/yarn-classic/overrides/" + scenario);
    }
}
