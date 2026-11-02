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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/core sources/semverUtils.ts (Yarn 4.10.3) over node-semver, as Yarn and pnpm 11.2.2 bundle it.
 */
public final class YarnpkgSemverUtils {

    private static final Pattern CLEAN_SEMVER_REGEXP = Pattern.compile("^(?:[\\sv=]*?)((0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
            "\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-]" +
            "[0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?)(?:\\s*)\\z");

    /**
     * A range node-semver parses: {@code new semver.Range(raw)}.
     */
    @Getter
    public static final class Range {
        private final String raw;

        /**
         * The comparator sets as node-semver formats them: {@code range.range}.
         */
        private final String range;

        Range(String raw, String range) {
            this.raw = raw;
            this.range = range;
        }

        public boolean test(String version) {
            try {
                return NodeSemver.satisfies(version, raw, NodeSemver.Options.DEFAULT);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * {@code validRange}: null for a range with a {@code :} or one node-semver does not parse.
     */
    public static @Nullable Range validRange(String potentialRange) {
        if (potentialRange.indexOf(':') != -1) {
            return null;
        }
        String range = NodeSemver.validRange(potentialRange, NodeSemver.Options.DEFAULT);
        return range == null ? null : new Range(potentialRange, range);
    }

    public static @Nullable String clean(String potentialVersion) {
        Matcher version = CLEAN_SEMVER_REGEXP.matcher(potentialVersion);
        return version.find() ? version.group(1) : null;
    }

    public static boolean valid(String version) {
        return NodeSemver.valid(version, NodeSemver.Options.DEFAULT) != null;
    }

    /**
     * {@code new SemVer(a).compare(b)}.
     */
    public static int compare(String a, String b) {
        try {
            return NodeSemver.compare(a, b, NodeSemver.Options.DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
    }

    private YarnpkgSemverUtils() {
    }

    /**
     * Under includePrerelease, or else with the version's prerelease dropped against each comparator set.
     */
    public static boolean satisfiesWithPrereleases(@Nullable String version, String range, boolean loose) {
        return satisfiesWithPrereleases(version, range, loose, NodeSemver.Release.V7_8_4);
    }

    /**
     * {@link #satisfiesWithPrereleases(String, String, boolean)} over the node-semver release {@code semver}, the one
     * the package manager bundles.
     */
    public static boolean satisfiesWithPrereleases(@Nullable String version, String range, boolean loose,
                                                   NodeSemver.Release semver) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        NodeSemver.Options options = (loose ? NodeSemver.Options.LOOSE_INCLUDE_PRERELEASE :
                NodeSemver.Options.INCLUDE_PRERELEASE).release(semver);
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
