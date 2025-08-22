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
        return LatestRelease.RELEASE_PATTERN.matcher(version).matches();
    }

    /**
     * Validates the given version against an optional pattern
     *
     * @param toVersion       the version to validate. Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used.
     * @param metadataPattern optional metadata appended to the version. Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     *                        Setting 'version' to "25-29" can be paired with a metadata pattern of "-jre" to select Guava 29.0-jre
     * @return the validation result
     */
    public static Validated<VersionComparator> validate(String toVersion, @Nullable String metadataPattern) {
        return Validated.<VersionComparator, String>testNone(
                "metadataPattern",
                "must be a valid regular expression",
                metadataPattern, metadata -> {
                    try {
                        if (metadata != null) {
                            Pattern.compile(metadata);
                        }
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                }
        ).and(Validated.<VersionComparator>none()
                .or(LatestRelease.buildLatestRelease(toVersion, metadataPattern))
                .or(LatestIntegration.build(toVersion, metadataPattern))
                .or(LatestMinor.build(toVersion, metadataPattern))
                .or(LatestPatch.build(toVersion, metadataPattern))
                .or(HyphenRange.build(toVersion, metadataPattern))
                .or(XRange.build(toVersion, metadataPattern))
                .or(TildeRange.build(toVersion, metadataPattern))
                .or(CaretRange.build(toVersion, metadataPattern))
                .or(SetRange.build(toVersion, metadataPattern))
                .or(ExactVersionWithPattern.build(toVersion, metadataPattern))
                .or(ExactVersion.build(toVersion))
        );
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
        }
        if (!isVersion(version2)) {
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
            int maj1 = Integer.parseInt(major1);
            int maj2 = Integer.parseInt(major2);
            if (maj1 != maj2) return maj1 > maj2 ? version1 : version2;

            int min1 = Integer.parseInt(minor1);
            int min2 = Integer.parseInt(minor2);
            if (min1 != min2) return min1 > min2 ? version1 : version2;

            String[] parts1 = version1.split("[.-]");
            String[] parts2 = version2.split("[.-]");
            int patch1 = parts1.length > 2 && parts1[2].matches("\\d+") ? Integer.parseInt(parts1[2]) : 0;
            int patch2 = parts2.length > 2 && parts2[2].matches("\\d+") ? Integer.parseInt(parts2[2]) : 0;
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
