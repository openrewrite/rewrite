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

import org.openrewrite.internal.lang.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

public interface VersionComparator extends Comparator<String> {
    Pattern RELEASE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?([-.+].*)?");
    String[] RELEASE_SUFFIXES = new String[]{".final", ".ga", ".release"};
    Pattern PRE_RELEASE_ENDING = Pattern.compile("[.-](alpha|a|beta|b|milestone|m|rc|cr|snapshot)[.-]?\\d*$", Pattern.CASE_INSENSITIVE);

    boolean isValid(@Nullable String currentVersion, String version);

    @Deprecated
    @Override
    default int compare(String v1, String v2) {
        return compare(null, v1, v2);
    }

    int compare(@Nullable String currentVersion, String v1, String v2);

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
}
