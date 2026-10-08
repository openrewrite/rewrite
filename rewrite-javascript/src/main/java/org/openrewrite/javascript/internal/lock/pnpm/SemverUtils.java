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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.semver.NodeSemver;

/**
 * Port of @yarnpkg/core's semverUtils {@code satisfiesWithPrereleases} as pnpm 11.2.2 bundles it.
 */
final class SemverUtils {

    private SemverUtils() {
    }

    /**
     * The range under includePrerelease, else the version without its prerelease against each comparator set.
     */
    static boolean satisfiesWithPrereleases(@Nullable String version, String range, boolean loose) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        NodeSemver.Options options = loose ? NodeSemver.Options.LOOSE_INCLUDE_PRERELEASE :
                NodeSemver.Options.INCLUDE_PRERELEASE;
        String formatted;
        try {
            formatted = NodeSemver.validRange(range, options);
        } catch (IllegalArgumentException e) {
            return false;
        }
        String semverVersion = NodeSemver.valid(version, options);
        if (formatted == null || semverVersion == null) {
            return false;
        }
        if (NodeSemver.satisfies(semverVersion, formatted, options)) {
            return true;
        }
        int dash = semverVersion.indexOf('-');
        String release = dash < 0 ? semverVersion : semverVersion.substring(0, dash);
        for (String comparatorSet : formatted.split("\\|\\|")) {
            boolean every = true;
            for (String comparator : comparatorSet.trim().split(" ")) {
                every &= comparator.isEmpty() || "*".equals(comparator) ||
                         NodeSemver.satisfies(release, comparator, options);
            }
            if (every) {
                return true;
            }
        }
        return false;
    }
}
