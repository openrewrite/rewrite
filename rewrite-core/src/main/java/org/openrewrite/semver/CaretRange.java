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
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.semver.Semver.isVersion;

/**
 * Allows changes that do not modify the left-most non-zero element in the [major, minor, patch] tuple.
 * <a href="https://github.com/npm/node-semver#caret-ranges-123-025-004">Caret ranges</a>.
 */
public class CaretRange extends LatestRelease {
    private static final Pattern CARET_RANGE_PATTERN = Pattern.compile("\\^(\\d+)(?:\\.([*xX]|\\d+))?(?:\\.([*xX]|\\d+))?(?:\\.([*xX]|\\d+))?");

    private final String upperExclusive;
    private final String lower;

    private CaretRange(String lower, String upperExclusive, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.lower = lower;
        this.upperExclusive = upperExclusive;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return super.isValid(currentVersion, version) &&
                super.compare(currentVersion, version, upperExclusive) < 0 &&
                super.compare(currentVersion, version, lower) >= 0;
    }

    public static Validated<CaretRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = CARET_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("caretRange", pattern, "not a caret range");
        }

        String major = matcher.group(1);
        String minor = normalizeWildcard(matcher.group(2));
        String patch = normalizeWildcard(matcher.group(3));
        String micro = normalizeWildcard(matcher.group(4));

        if ("*".equals(minor) && (matcher.group(3) != null || matcher.group(4) != null)) {
            return Validated.invalid("caretRange", pattern, "not a caret range: nothing can follow a wildcard");
        } else if ("*".equals(patch) && (matcher.group(4) != null)) {
            return Validated.invalid("caretRange", pattern, "not a caret range: nothing can follow a wildcard");
        }

        String lower;
        String upper;

        if (minor == null) {
            // A missing patch value will desugar to the number 0, but will allow flexibility
            // within that value, even if the major and minor versions are both 0.
            lower = major;
        } else if (patch == null) {
            // A missing minor and patch values will desugar to zero, but also allow flexibility
            // within those values, even if the major version is zero.
            lower = major + "." + minor;
        } else if (micro == null) {
            lower = major + "." + minor + "." + patch;
        } else {
            lower = major + "." + minor + "." + patch + "." + micro;
        }

        if (minor == null) {
            upper = Integer.toString(parseInt(major) + 1);
        } else if (patch == null) {
            if ("0".equals(major)) {
                upper = "0." + (parseInt(minor) + 1);
            } else {
                upper = (parseInt(major) + 1) + ".0";
            }
        } else if (micro == null) {
            if ("0".equals(major)) {
                upper = "0." + (parseInt(minor) + 1) + ".0";
            } else {
                upper = (parseInt(major) + 1) + ".0.0";
            }
        } else {
            if ("0".equals(major)) {
                upper = "0." + (parseInt(minor) + 1) + ".0.0";
            } else {
                upper = (parseInt(major) + 1) + ".0.0.0";
            }
        }

        return Validated.valid("caretRange", new CaretRange(lower, upper, metadataPattern));
    }

    private static @Nullable String normalizeWildcard(@Nullable String part) {
        return "*".equals(part) || "x".equals(part) || "X".equals(part) ? null : part;
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        Validated<CaretRange> maybeCaretV1 = build(v1, null);
        Validated<CaretRange> maybeCaretV2 = build(v2, null);
        if (maybeCaretV1.isValid() && maybeCaretV2.isValid()) {
            CaretRange caretV1 = requireNonNull(maybeCaretV1.getValue());
            CaretRange caretV2 = requireNonNull(maybeCaretV2.getValue());
            int compare = super.compare(currentVersion, caretV1.upperExclusive, caretV2.upperExclusive);
            if (compare != 0) {
                return compare;
            }

            return super.compare(currentVersion, caretV1.lower, caretV2.lower);
        } else if (maybeCaretV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            CaretRange caretV1 = requireNonNull(maybeCaretV1.getValue());
            int compare = super.compare(currentVersion, caretV1.upperExclusive, v2);
            if (compare < 0) {
                return compare;
            } else if (compare == 0) {
                return -1;
            }

            compare = super.compare(currentVersion, caretV1.lower, v2);
            return Math.max(compare, 0);
        } else if (maybeCaretV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            CaretRange caretV2 = requireNonNull(maybeCaretV2.getValue());
            int compare = super.compare(currentVersion, v1, caretV2.upperExclusive);
            if (compare > 0) {
                return compare;
            } else if (compare == 0) {
                return 1;
            }

            compare = super.compare(currentVersion, v1, caretV2.lower);
            return Math.min(compare, 0);
        }

        return super.compare(currentVersion, v1, v2);
    }
}
