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
import org.openrewrite.internal.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

public interface VersionComparator extends Comparator<String> {
    Pattern RELEASE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.\\d+)*(?<qualifier>[-.+].*?$)?");
    String[] RELEASE_SUFFIXES = new String[]{".final", ".ga", ".release"};
    Pattern PRE_RELEASE_ENDING = Pattern.compile("[.-](alpha|a|beta|b|milestone|m|rc|cr|snapshot)[.-]?\\d*$", Pattern.CASE_INSENSITIVE);

    boolean isValid(@Nullable String currentVersion, String version);

    @Deprecated
    @Override
    default int compare(String v1, String v2) {
        return compare(null, v1, v2);
    }

    int compare(@Nullable String currentVersion, String v1, String v2);

    /**
     * The highest of {@code availableVersions} this selector admits (per {@link #isValid} with no
     * current version), by this comparator's ordering. The winner is returned in its original
     * spelling and the first-seen candidate wins ties, so selection is deterministic for a fixed
     * iteration order. Unlike {@link #upgrade}, the result is not constrained to exceed any current
     * version.
     */
    default Optional<String> maxSatisfying(Collection<String> availableVersions) {
        String best = null;
        for (String candidate : availableVersions) {
            if (isValid(null, candidate) && (best == null || compare(null, candidate, best) > 0)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    default Optional<String> upgrade(String currentVersion, Collection<String> availableVersions) {
        boolean seen = false;
        String best = null;
        for (String availableVersion : availableVersions) {
            if (isValid(currentVersion, availableVersion)) {
                if (compare(currentVersion, currentVersion, availableVersion) <= 0) {
                    if (!seen || compare(currentVersion, availableVersion, best) > 0) {
                        seen = true;
                        best = availableVersion;
                    }
                }
            }
        }
        return (seen ? Optional.of(best) : Optional.<String>empty())
                .filter(v -> !v.equals(currentVersion));
    }

    static boolean checkVersion(String version, @Nullable String metadataPattern, boolean requireRelease) {
        ParsedVersion parsed = ParsedVersion.parse(version);
        if (!parsed.matches()) {
            return false;
        }
        if (requireRelease && parsed.isPreReleaseEnding()) {
            return false;
        }

        boolean requireMeta = !StringUtils.isNullOrEmpty(metadataPattern);
        String versionMeta = parsed.qualifier();
        if (requireMeta) {
            return versionMeta != null && versionMeta.matches(metadataPattern);
        } else if (versionMeta == null) {
            return true;
        } else if (requireRelease) {
            String lowercaseVersionMeta = versionMeta.toLowerCase();
            for (String suffix : RELEASE_SUFFIXES) {
                if (suffix.equals(lowercaseVersionMeta)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

}
