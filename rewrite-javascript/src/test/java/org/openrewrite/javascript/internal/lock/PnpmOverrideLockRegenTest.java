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
 * pnpm (10.34.5) {@code pnpm.overrides}: a plain key applies to every requirer, a direct dependency included, and
 * {@code parent>child} only to the parent's own dependencies. The lock records every declared key verbatim, in
 * declaration order, between {@code settings} and {@code importers}.
 */
class PnpmOverrideLockRegenTest extends OverrideLockRegenTestSupport {

    /** Also covers a scoped name's quoting and an unreached version-selector key, recorded but never applied. */
    @Test
    void addedOverridesAreAppliedAndRecordedInDeclarationOrder() {
        assertMatchesPnpm("order");
    }

    @Test
    void parentScopedOverrideAppliesToTheParentsDependency() {
        assertMatchesPnpm("scoped");
    }

    /** is-number requires kind-of, not is-buffer, so pnpm records the rule but changes nothing. */
    @Test
    void parentScopedOverrideDoesNotReachGrandchildren() {
        assertMatchesPnpm("deep-scoped");
    }

    @Test
    void changedOverrideMovesThePackage() {
        assertMatchesPnpm("change");
    }

    @Test
    void removedOverrideRestoresTheRequestedRange() {
        assertMatchesPnpm("remove");
    }

    /** pnpm rewrites the importer's specifier as well as its version. */
    @Test
    void overrideAppliesToADirectDependency() {
        assertMatchesPnpm("direct");
    }

    @Test
    void addedDependencyReceivesAnExistingOverride() {
        assertMatchesPnpm("with-dep-add");
    }

    @Test
    void unreachedOverrideAddedWithADependencyIsRecorded() {
        assertMatchesPnpm("unrelated-with-add");
    }

    /** pnpm also reads root-level {@code resolutions}, and records them in the same section. */
    @Test
    void rootResolutionsAreAppliedAndRecorded() {
        assertMatchesPnpm("resolutions");
    }

    /** {@code {...resolutions, ...pnpm.overrides}}: resolutions' keys first, pnpm.overrides winning in place. */
    @Test
    void pnpmOverridesWinOverResolutionsInTheMergedSection() {
        assertMatchesPnpm("both-fields");
    }

    private void assertMatchesPnpm(String scenario) {
        assertMatchesTool(PackageManager.Pnpm, "lock/pnpm/overrides/" + scenario);
    }
}
