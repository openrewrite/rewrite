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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;

import java.util.regex.Matcher;

public class LatestRelease implements VersionComparator {

    @Nullable
    private final String metadataPattern;

    public LatestRelease(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    protected @Nullable String getMetadataPattern() {
        return metadataPattern;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return VersionComparator.checkVersion(version, metadataPattern, true);
    }

    static String normalizeVersion(String version) {
        int lastDotIdx = version.lastIndexOf('.');
        for (String suffix : RELEASE_SUFFIXES) {
            if (version.regionMatches(true, lastDotIdx, suffix, 0, suffix.length())) {
                version = version.substring(0, lastDotIdx);
                break;
            }
        }

        int versionParts = countVersionParts(version);

        if (versionParts <= 2) {
            String[] versionAndMetadata = version.split("(?=[-+])");
            for (; versionParts <= 2; versionParts++) {
                versionAndMetadata[0] += ".0";
            }
            version = versionAndMetadata.length > 1 ? versionAndMetadata[0] + versionAndMetadata[1] :
                    versionAndMetadata[0];
        }

        return version;
    }

    static int countVersionParts(String version) {
        int count = 0;
        int len = version.length();
        int lastSepIdx = -1;
        for (int i = 0; i < len; i++) {
            char c = version.charAt(i);
            if (c == '.' || c == '-' || c == '$') {
                if (lastSepIdx == i - 1) {
                    return count;
                }
                lastSepIdx = i;
            } else if (lastSepIdx == i - 1) {
                if (!Character.isDigit(c)) {
                    break;
                }
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        if (v1.equalsIgnoreCase(v2)) {
            return 0;
        } else if (v1.equalsIgnoreCase("LATEST") || v1.equalsIgnoreCase("latest.integration")) {
            return 1;
        } else if (v2.equalsIgnoreCase("LATEST") || v2.equalsIgnoreCase("latest.integration")) {
            return -1;
        } else if (v1.equalsIgnoreCase("RELEASE") || v1.equalsIgnoreCase("latest.release")) {
            return 1;
        } else if (v2.equalsIgnoreCase("RELEASE") || v2.equalsIgnoreCase("latest.release")) {
            return -1;
        }

        String nv1 = normalizeVersion(v1);
        String nv2 = normalizeVersion(v2);

        int vp1 = countVersionParts(nv1);
        int vp2 = countVersionParts(nv2);

        if (vp1 > vp2) {
            StringBuilder nv2Builder = new StringBuilder(nv2);
            for (int i = vp2; i < vp1; i++) {
                nv2Builder.append(".0");
            }
            nv2 = nv2Builder.toString();
        } else if (vp2 > vp1) {
            StringBuilder nv1Builder = new StringBuilder(nv1);
            for (int i = vp1; i < vp2; i++) {
                nv1Builder.append(".0");
            }
            nv1 = nv1Builder.toString();
        }

        // Remove the metadata pattern from the normalized versions, this only impacts the comparison when all version
        // parts are the same:
        //
        // HyphenRange [25-28] should include "28-jre" and "28-android" as possible candidates.
        String normalized1 = metadataPattern == null ? nv1 : nv1.replaceAll(metadataPattern, "");
        String normalized2 = metadataPattern == null ? nv2 : nv2.replaceAll(metadataPattern, "");

        Matcher v1Gav = VersionComparator.RELEASE_PATTERN.matcher(normalized1);
        Matcher v2Gav = VersionComparator.RELEASE_PATTERN.matcher(normalized2);

        v1Gav.find();
        v2Gav.find();

        try {
            for (int i = 1; i <= Math.max(vp1, vp2); i++) {
                String v1Part = v1Gav.group(i);
                String v2Part = v2Gav.group(i);
                if (v1Part == null) {
                    return v2Part == null ? normalized1.compareTo(normalized2) : -1;
                } else if (v2Part == null) {
                    return 1;
                }

                long diff = Long.parseLong(v1Part) - Long.parseLong(v2Part);
                if (diff != 0) {
                    // squish the long to fit into an int; all that matters is whether the return value is pos/neg/zero
                    return (int) (diff / Math.abs(diff));
                }
            }
        } catch (IllegalStateException exception) {
            // Provide a better error message if an error is thrown while getting groups from the regular expression.
            throw new IllegalStateException("Illegal state while comparing versions : [" + nv1 + "] and [" + nv2 + "]. Metadata = [" + metadataPattern + "]", exception);
        }

        // When all numeric parts are equal, we need to handle pre-release versions properly
        // A pre-release version should be considered less than a release version
        // e.g., "3.5.0-RC1" < "3.5.0"
        int v1Prio = qualifierPriority(v1Gav.group(6));
        int v2Prio = qualifierPriority(v2Gav.group(6));

        if (v1Prio != v2Prio) {
            return Integer.compare(v1Prio, v2Prio);
        }

        // Both are either pre-release or release versions, do string comparison
        return normalized1.compareTo(normalized2);
    }

    private static int qualifierPriority(@Nullable String suffix) {
        String qualifier = extractQualifier(suffix);
        switch (qualifier) {
            case "alpha":
            case "a":
                return 1;
            case "beta":
            case "b":
                return 2;
            case "milestone":
            case "m":
                return 3;
            case "rc":
            case "cr":
                return 4;
            case "snapshot":
                return 5;
            case "":
            case "ga":
            case "final":
            case "release":
                return 6;
            case "sp":
                return 7;
            default:
                return 8;
        }
    }

    private static String extractQualifier(@Nullable String suffix) {
        if (suffix == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < suffix.length(); i++) {
            if (Character.isLetter(suffix.charAt(i))) {
                builder.append(Character.toLowerCase(suffix.charAt(i)));
            } else {
                break;
            }
        }
        return builder.toString();
    }

    public static Validated<LatestRelease> buildLatestRelease(String toVersion, @Nullable String metadataPattern) {
        return "latest.release".equalsIgnoreCase(toVersion) || "latest.major".equalsIgnoreCase(toVersion) ?
                Validated.valid("latestRelease", new LatestRelease(metadataPattern)) :
                Validated.invalid("latestRelease", toVersion, "not latest release");
    }
}
