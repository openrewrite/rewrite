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

import org.jspecify.annotations.Nullable;
import org.openrewrite.semver.NodeSemver;

/**
 * Port of @yarnpkg/core's semverUtils {@code satisfiesWithPrereleases}, as Yarn 4.10.3 and pnpm 11.2.2 bundle it.
 */
public final class YarnpkgSemverUtils {

    private YarnpkgSemverUtils() {
    }

    /**
     * Under includePrerelease, or else with the version's prerelease dropped against each comparator set.
     */
    public static boolean satisfiesWithPrereleases(@Nullable String version, String range, boolean loose) {
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
        String release = withoutPrerelease(semverVersion);
        for (String set : formatted.split("\\|\\|")) {
            boolean all = true;
            for (String comparator : set.trim().split(" ")) {
                if (!comparatorTest(comparator, release, loose)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    private static boolean comparatorTest(String comparator, String version, boolean loose) {
        if (comparator.isEmpty() || "*".equals(comparator)) {
            return true;
        }
        int i = 0;
        while (i < comparator.length() && "<>=".indexOf(comparator.charAt(i)) >= 0) {
            i++;
        }
        String operator = comparator.substring(0, i);
        // the comparator's SemVer was made without includePrerelease, so cmp re-parses it from its version string
        // and the prerelease Yarn cleared is back
        int cmp = NodeSemver.compare(version, comparator.substring(i),
                loose ? NodeSemver.Options.LOOSE : NodeSemver.Options.DEFAULT);
        switch (operator) {
            case "":
            case "=":
                return cmp == 0;
            case ">":
                return cmp > 0;
            case ">=":
                return cmp >= 0;
            case "<":
                return cmp < 0;
            case "<=":
                return cmp <= 0;
            default:
                throw new IllegalStateException("Unexpected comparator operator (" + operator + ")");
        }
    }

    private static String withoutPrerelease(String version) {
        int build = version.indexOf('+');
        String core = build == -1 ? version : version.substring(0, build);
        int pre = core.indexOf('-');
        return pre == -1 ? core : core.substring(0, pre);
    }
}
