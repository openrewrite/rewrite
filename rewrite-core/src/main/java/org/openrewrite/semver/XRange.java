/*
 * Copyright 2020 the original author or authors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Any of X, x, or * may be used to "stand in" for one of the numeric values in the [major, minor, patch] tuple.
 * <a href="https://github.com/npm/node-semver#x-ranges-12x-1x-12-">X-Ranges</a>.
 */
public class XRange extends LatestRelease {
    private static final Pattern X_RANGE_PATTERN = Pattern.compile("([*xX]|\\d+)(?:\\.([*xX]|\\d+)(?:\\.([*xX]|\\d+))?)?");

    private final String major;
    private final String minor;
    private final String patch;

    XRange(String major, String minor, String patch, String metadataPattern) {
        super(metadataPattern);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public boolean isValid(String version) {
        if (!super.isValid(version)) {
            return false;
        }

        if (major.equals("*")) {
            return true;
        }

        Matcher gav = VersionComparator.RELEASE_PATTERN.matcher(normalizeVersion(version));
        gav.matches();

        if (!gav.group(1).equals(major)) {
            return false;
        }

        if (minor.equals("*")) {
            return true;
        } else if (gav.group(2) == null || !gav.group(2).equals(minor)) {
            return false;
        }

        if (patch.equals("*")) {
            return true;
        }

        return gav.group(3) == null || !gav.group(3).equals(patch);
    }

    public static Validated build(String pattern, String metadataPattern) {
        Matcher matcher = X_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches() || !(pattern.contains("x") || pattern.contains("X") || pattern.contains("*"))) {
            return Validated.invalid("xRange", pattern, "not an x-range");
        }

        String major = normalizeWildcard(matcher.group(1));
        String minor = normalizeWildcard(matcher.group(2) == null ? "0" : matcher.group(2));
        String patch = normalizeWildcard(matcher.group(3) == null ? "0" : matcher.group(3));

        if (major.equals("*") && (matcher.group(2) != null || matcher.group(3) != null)) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        } else if (minor.equals("*") && matcher.group(3) != null) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        }

        return Validated.valid("xRange", new XRange(major, minor, patch, metadataPattern));
    }

    private static String normalizeWildcard(String part) {
        return part.equals("*") || part.equals("x") || part.equals("X") ? "*" : part;
    }
}
