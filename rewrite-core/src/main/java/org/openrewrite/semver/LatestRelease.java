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

import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.Scanner;
import java.util.regex.Matcher;

import static java.lang.Integer.parseInt;

public class LatestRelease implements VersionComparator {
    @Nullable
    private final String metadataPattern;

    public LatestRelease(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        Matcher matcher = VersionComparator.RELEASE_PATTERN.matcher(normalizeVersion(version));
        if (!matcher.matches() || PRE_RELEASE_ENDING.matcher(version).find()) {
            return false;
        }
        boolean requireMeta = !StringUtils.isNullOrEmpty(metadataPattern);
        String versionMeta = matcher.group(6);
        if (requireMeta) {
            return versionMeta != null && versionMeta.matches(metadataPattern);
        } else {
            return versionMeta == null;
        }
    }

    static String normalizeVersion(String version) {
        if (version.endsWith(".RELEASE")) {
            return version.substring(0, version.length() - ".RELEASE".length());
        } else if (version.endsWith(".FINAL") || version.endsWith(".Final")) {
            return version.substring(0, version.length() - ".FINAL".length());
        }

        long versionParts = countVersionParts(version);

        if (versionParts <= 2) {
            String[] versionAndMetadata = version.split("(?=[-+])");
            for (; versionParts <= 2; versionParts++) {
                versionAndMetadata[0] += ".0";
            }
            version = versionAndMetadata[0] + (versionAndMetadata.length > 1 ?
                    versionAndMetadata[1] : "");
        }

        return version;
    }

    static long countVersionParts(String version) {
        long count = 0;
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        while (scanner.hasNext()) {
            String part = scanner.next();
            if (part.isEmpty() || !Character.isDigit(part.charAt(0))) {
                break;
            }
            count++;
        }
        return count;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        StringBuilder nv1 = new StringBuilder(normalizeVersion(v1));
        StringBuilder nv2 = new StringBuilder(normalizeVersion(v2));

        long vp1 = countVersionParts(nv1.toString());
        long vp2 = countVersionParts(nv2.toString());

        long abs = Math.abs(vp1 - vp2);
        if (vp1 > vp2) {
            for (int i = 1; i <= abs; i++) {
                nv2.append(".0");
            }
        } else if (vp2 > vp1) {
            for (int i = 1; i <= abs; i++) {
                nv1.append(".0");
            }
        }

        Matcher v1Gav = VersionComparator.RELEASE_PATTERN.matcher(nv1.toString());
        Matcher v2Gav = VersionComparator.RELEASE_PATTERN.matcher(nv2.toString());

        v1Gav.matches();
        v2Gav.matches();

        // Remove the metadata pattern from the normalized versions, this only impacts the comparison when all version
        // parts are the same:
        //
        // HyphenRange [25-28] should include "28-jre" and "28-android" as possible candidates.
        String normalized1 = metadataPattern == null ? nv1.toString() : nv1.toString().replace(metadataPattern, "");
        String normalized2 = metadataPattern == null ? nv2.toString() : nv1.toString().replace(metadataPattern, "");

        try {
            for (int i = 1; i <= Math.max(vp1, vp2); i++) {
                String v1Part = v1Gav.group(i);
                String v2Part = v2Gav.group(i);
                if (v1Part == null) {
                    return v2Part == null ? normalized1.compareTo(normalized2) : -1;
                } else if (v2Part == null) {
                    return 1;
                }

                int diff = parseInt(v1Part) - parseInt(v2Part);
                if (diff != 0) {
                    return diff;
                }
            }
        } catch (IllegalStateException exception) {
            // Provide a better error message if an error is thrown while getting groups from the regular expression.
            throw new IllegalStateException("Illegal state while comparing versions : [" + nv1 + "] and [" + nv2 + "]. Metadata = [" + metadataPattern + "]", exception);
        }

        return normalized1.compareTo(normalized2);
    }

    public static Validated build(String toVersion, @Nullable String metadataPattern) {
        return "latest.release".equalsIgnoreCase(toVersion) ?
                Validated.valid("latestRelease", new LatestRelease(metadataPattern)) :
                Validated.invalid("latestRelease", toVersion, "not latest release");
    }
}
