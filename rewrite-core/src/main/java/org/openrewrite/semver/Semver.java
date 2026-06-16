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

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;

import java.util.Scanner;
import java.util.regex.Pattern;

@UtilityClass
public class Semver {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isVersion(@Nullable String version) {
        if (StringUtils.isBlank(version)) {
            return false;
        }
        return ParsedVersion.parse(version).matches();
    }

    /**
     * Validates the given version against an optional pattern.
     * <p>
     * The {@code metadataPattern} is interpreted first as a regular expression. If that fails to
     * compile, it is treated as a glob (where {@code *} matches any run of characters and {@code ?}
     * matches any single character) and converted to an equivalent regex via
     * {@link StringUtils#globToRegex(String)}. This lets simple patterns like {@code "+backpatch*"}
     * work without needing to be regex-escaped.
     *
     * @param toVersion       the version to validate. Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used.
     * @param metadataPattern optional metadata appended to the version. Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     *                        setting 'version' to "25-29" can be paired with a metadata pattern of "-jre" to select Guava 29.0-jre. Accepts either a regex or a glob.
     * @return the validation result
     */
    public static Validated<VersionComparator> validate(String toVersion, @Nullable String metadataPattern) {
        String canonicalPattern = canonicalizeMetadataPattern(metadataPattern);
        return Validated.<VersionComparator, String>testNone(
                "metadataPattern",
                "must be a valid regular expression or glob",
                metadataPattern, metadata -> metadata == null || canonicalPattern != null
        ).and(Validated.<VersionComparator>none()
                .or(LatestRelease.buildLatestRelease(toVersion, canonicalPattern))
                .or(LatestIntegration.build(toVersion, canonicalPattern))
                .or(LatestMinor.build(toVersion, canonicalPattern))
                .or(LatestPatch.build(toVersion, canonicalPattern))
                .or(HyphenRange.build(toVersion, canonicalPattern))
                .or(XRange.build(toVersion, canonicalPattern))
                .or(TildeRange.build(toVersion, canonicalPattern))
                .or(CaretRange.build(toVersion, canonicalPattern))
                .or(SetRange.build(toVersion, canonicalPattern))
                .or(ExactVersionWithPattern.build(toVersion, canonicalPattern))
                .or(ExactVersion.build(toVersion))
        );
    }

    private static @Nullable String canonicalizeMetadataPattern(@Nullable String metadataPattern) {
        if (metadataPattern == null) {
            return null;
        }
        try {
            Pattern.compile(metadataPattern);
            return metadataPattern;
        } catch (Throwable regexFailure) {
            String asRegex = StringUtils.globToRegex(metadataPattern);
            try {
                if (asRegex != null) {
                    Pattern.compile(asRegex);
                    return asRegex;
                }
            } catch (Throwable ignored) {
                // fall through
            }
            return null;
        }
    }

    public static String majorVersion(String version) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        if (scanner.hasNext()) {
            return scanner.next();
        }
        return version;
    }

    public static String minorVersion(String version) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        if (scanner.hasNext()) {
            scanner.next();
        }
        if (scanner.hasNext()) {
            String minor = scanner.next();
            if (StringUtils.isNumeric(minor)) {
                return minor;
            }
        }
        return version;
    }

    public static @Nullable String max(@Nullable String version1, @Nullable String version2) {
        if (!isVersion(version1)) {
            return StringUtils.isBlank(version2) ? null : version2;
        } else if (!isVersion(version2)) {
            return version1;
        }

        String major1 = Semver.majorVersion(version1);
        String major2 = Semver.majorVersion(version2);
        String minor1 = Semver.minorVersion(version1);
        String minor2 = Semver.minorVersion(version2);

        if (!StringUtils.isNumeric(major1) || !StringUtils.isNumeric(major2) ||
                !StringUtils.isNumeric(minor1) || !StringUtils.isNumeric(minor2)) {
            if (version1.equals(version2)) {
                return version1;
            }
            return version1.compareTo(version2) >= 0 ? version1 : version2;
        }
        try {
            long maj1 = Long.parseLong(major1);
            long maj2 = Long.parseLong(major2);
            if (maj1 != maj2) return maj1 > maj2 ? version1 : version2;

            long min1 = Long.parseLong(minor1);
            long min2 = Long.parseLong(minor2);
            if (min1 != min2) return min1 > min2 ? version1 : version2;

            String[] parts1 = version1.split("[.-]");
            String[] parts2 = version2.split("[.-]");
            long patch1 = parts1.length > 2 && parts1[2].matches("\\d+") ? Long.parseLong(parts1[2]) : 0;
            long patch2 = parts2.length > 2 && parts2[2].matches("\\d+") ? Long.parseLong(parts2[2]) : 0;
            if (patch1 != patch2) return patch1 > patch2 ? version1 : version2;

            String label1 = parts1.length > 3 ? parts1[3].toLowerCase() : "";
            String label2 = parts2.length > 3 ? parts2[3].toLowerCase() : "";

            if (label1.isEmpty() && !label2.isEmpty()) return version1;
            if (!label1.isEmpty() && label2.isEmpty()) return version2;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error comparing version number \"" + version1 + "\" to \"" + version2 + "\"", e);
        }
        return version1;
    }
}
