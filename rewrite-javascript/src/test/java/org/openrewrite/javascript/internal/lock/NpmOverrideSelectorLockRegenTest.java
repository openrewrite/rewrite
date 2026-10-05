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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scoped npm overrides: a version-qualified parent key ({@code "is-odd@^0.1.0": {...}}) and a scope that reaches
 * below the parent's direct dependencies ({@code is-even -> is-odd -> is-number}). Each case adds the override to a
 * lock recorded from a real {@code npm install --package-lock-only} (npm 11.16.0), replays OFFLINE, and asserts
 * the emitted lock is BYTE-IDENTICAL to npm's, or that it refuses where npm fails or needs a second copy.
 */
class NpmOverrideSelectorLockRegenTest extends LockRegenTestSupport {

    private static final String FIXTURE = "lock/npm/override-selectors/";
    private static final String[] MANIFESTS = {"is-buffer@1.1.6", "is-buffer@2.0.0", "is-even@1.0.0",
            "is-number@3.0.0", "is-number@6.0.0", "is-number@7.0.0", "is-odd@0.1.2", "is-odd@3.0.1",
            "kind-of@3.0.4", "kind-of@3.2.2", "kind-of@6.0.3"};

    @BeforeEach
    void routes() {
        for (String nameVersion : MANIFESTS) {
            int at = nameVersion.lastIndexOf('@');
            String name = nameVersion.substring(0, at);
            routes.put(REG + name, resource(FIXTURE + "http/" + name));
            routes.put(REG + name + "/" + nameVersion.substring(at + 1),
                    resource(FIXTURE + "http/" + name + "-" + nameVersion.substring(at + 1)));
        }
    }

    @Test
    void versionQualifiedParentAppliesWhenItsRangeMatches() {
        assertMatchesNpm("version-selector-transitive");
    }

    @Test
    void versionQualifiedParentIsInertWhenItsRangeDoesNotMatch() {
        assertMatchesNpm("version-selector-transitive-miss");
    }

    @Test
    void versionQualifiedDirectParentIsInertWhenItsRangeDoesNotMatch() {
        // No EOVERRIDE: the key's range does not match the declared is-odd@3.0.1, so npm ignores the rule.
        assertMatchesNpm("version-selector-miss");
    }

    @Test
    void versionQualifiedParentConflictingWithADirectDependencyRefuses() {
        // A parent key also overrides the parent itself to the key's range, so npm fails with EOVERRIDE.
        Result result = regenerate("version-selector-eoverride");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-odd").contains("direct dependency");
    }

    @Test
    void scopedOverrideReachesBelowTheParent() {
        assertMatchesNpm("scoped-deep");
    }

    @Test
    void scopedOverrideReachableOutsideItsParentRefuses() {
        // A root is-odd@3 also requires is-number: npm keeps is-number@6 for it and nests is-number@7 under is-even,
        // a second copy this engine does not place.
        Result result = regenerate("scoped-deep-escapes");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("is-number");
    }

    /*
     * A parent key's rule follows the requirer's edge, not the parent's version: is-number requires kind-of@^3.0.2,
     * which intersects each selector below. A kind-of the lock already holds stays put even where it misses the
     * selector; a fresh one resolves against the selector, which npm substitutes for the edge's range.
     */
    @Test
    void exactParentSelectorAppliesToALockedParentOutsideIt() {
        assertMatchesNpm("self-exact-locked");
    }

    @Test
    void exactParentSelectorSteersAFreshParent() {
        assertMatchesNpm("self-exact-fresh");
    }

    @Test
    void broadParentSelectorSteersAFreshParent() {
        assertMatchesNpm("self-broad-fresh");
    }

    private void assertMatchesNpm(String scenario) {
        Result result = regenerate(scenario);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + scenario + "/after"));
    }

    private Result regenerate(String scenario) {
        return NativeLockEngine.regenerate(PackageManager.Npm, resource(FIXTURE + scenario + "/pkg-after"),
                resource(FIXTURE + scenario + "/pkg-before"), resource(FIXTURE + scenario + "/before"), null,
                Paths.get("package.json"), ctx);
    }
}
