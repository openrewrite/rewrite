/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.semver;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;

import java.util.Scanner;

@Value
public class LatestPatch implements VersionComparator {
    @Nullable
    String metadataPattern;

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        Validated<? extends VersionComparator> validated = currentVersion == null ?
                LatestRelease.buildLatestRelease("latest.release", metadataPattern) :
                TildeRange.build(buildTildeRange(currentVersion), metadataPattern, true);

        if (validated.isValid()) {
            VersionComparator comparator = validated.getValue();
            if (comparator != null) {
                return comparator.isValid(currentVersion, version);
            }
        }
        return false;
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        if(currentVersion == null) {
            return new LatestRelease(null)
                    .compare(null, v1, v2);
        }

        //noinspection ConstantConditions
        return TildeRange.build(buildTildeRange(currentVersion), metadataPattern)
                .getValue()
                .compare(currentVersion, v1, v2);
    }

    private static String buildTildeRange(String currentVersion) {
        String major = Semver.majorVersion(currentVersion);
        String minor = minorSegment(currentVersion);
        if (minor != null && StringUtils.isNumeric(minor)) {
            // A numeric minor is present: hold both major and minor fixed and allow only
            // patch-level (and finer) changes, e.g. "1.2.3" -> "~1.2".
            return "~" + major + "." + minor;
        }
        if (minor != null && isXRangeWildcard(minor)) {
            // The minor position is an X-range wildcard (e.g. "2.x", "2.+"): the minor is
            // deliberately unspecified, so allow any minor within the major.
            return "~" + major;
        }
        // No minor segment at all (e.g. "1") or a non-numeric qualifier in the minor position
        // (e.g. "1.Final"): pin the minor to 0 so "latest.patch" stays within the patch range
        // instead of ranging across minor versions.
        return "~" + major + ".0";
    }

    /**
     * @return the raw second version segment (the "minor" position) of {@code version}, or
     * {@code null} if the version has no second segment. Unlike {@link Semver#minorVersion(String)}
     * this does not fall back to returning the whole version string when the minor is absent or
     * non-numeric, which would otherwise let "latest.patch" build a too-wide tilde range.
     */
    private static @Nullable String minorSegment(String version) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        if (scanner.hasNext()) {
            scanner.next();
        }
        return scanner.hasNext() ? scanner.next() : null;
    }

    private static boolean isXRangeWildcard(String segment) {
        char c = segment.charAt(0);
        return c == 'x' || c == 'X' || c == '*' || c == '+';
    }

    public static Validated<LatestPatch> build(String toVersion, @Nullable String metadataPattern) {
        return "latest.patch".equalsIgnoreCase(toVersion) ?
                Validated.valid("latestPatch", new LatestPatch(metadataPattern)) :
                Validated.invalid("latestPatch", toVersion, "not latest patch");
    }
}
