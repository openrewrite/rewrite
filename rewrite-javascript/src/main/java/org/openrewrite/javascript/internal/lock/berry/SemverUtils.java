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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.semver.NodeSemver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/core sources/semverUtils.ts (Yarn 4.10.3) over node-semver.
 */
final class SemverUtils {

    private static final Pattern CLEAN_SEMVER_REGEXP = Pattern.compile("^(?:[\\sv=]*?)((0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
            "\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-]" +
            "[0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?)(?:\\s*)\\z");

    private SemverUtils() {
    }

    /**
     * A range node-semver parses: {@code new semver.Range(raw)}.
     */
    @Getter
    static final class Range {
        private final String raw;

        /**
         * The comparator sets as node-semver formats them: {@code range.range}.
         */
        private final String range;

        Range(String raw, String range) {
            this.raw = raw;
            this.range = range;
        }

        boolean test(String version) {
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
    static @Nullable Range validRange(String potentialRange) {
        if (potentialRange.indexOf(':') != -1) {
            return null;
        }
        String range = NodeSemver.validRange(potentialRange, NodeSemver.Options.DEFAULT);
        return range == null ? null : new Range(potentialRange, range);
    }

    static @Nullable String clean(String potentialVersion) {
        Matcher version = CLEAN_SEMVER_REGEXP.matcher(potentialVersion);
        return version.find() ? version.group(1) : null;
    }

    static boolean valid(String version) {
        return NodeSemver.valid(version, NodeSemver.Options.DEFAULT) != null;
    }

    /**
     * {@code new SemVer(a).compare(b)}.
     */
    static int compare(String a, String b) {
        try {
            return NodeSemver.compare(a, b, NodeSemver.Options.DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
    }

}
