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
 * Allows patch-level changes if a minor version is specified on the comparator. Allows minor-level changes if not.
 * <a href="https://github.com/npm/node-semver#tilde-ranges-123-12-1">Tilde ranges</a>.
 */
public class TildeRange extends LatestRelease {
    private static final Pattern TILDE_RANGE_PATTERN = Pattern.compile("~(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private final String upperExclusive;
    private final String lower;
    private final boolean requireRelease;

    private TildeRange(String lower, String upperExclusive, @Nullable String metadataPattern, boolean requireRelease) {
        super(metadataPattern);
        this.lower = lower;
        this.upperExclusive = upperExclusive;
        this.requireRelease = requireRelease;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return VersionComparator.checkVersion(version, getMetadataPattern(), requireRelease) &&
                super.compare(currentVersion, version, upperExclusive) < 0 &&
                super.compare(currentVersion, version, lower) >= 0;
    }

    public static Validated<TildeRange> build(String pattern, @Nullable String metadataPattern) {
        return build(pattern, metadataPattern, false);
    }
    public static Validated<TildeRange> build(String pattern, @Nullable String metadataPattern, boolean requireRelease) {
        Matcher matcher = TILDE_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("tildeRange", pattern, "not a tilde range");
        }

        String major = matcher.group(1);
        String minor = matcher.group(2);
        String patch = matcher.group(3);
        String micro = matcher.group(4);

        String lower;
        String upper;

        if (minor == null) {
            lower = major;
            upper = Integer.toString(parseInt(major) + 1);
        } else if (patch == null) {
            lower = major + "." + minor;
            upper = major + "." + (parseInt(minor) + 1);
        } else if (micro == null) {
            lower = major + "." + minor + "." + patch;
            upper = major + "." + (parseInt(minor) + 1);
        } else {
            lower = major + "." + minor + "." + patch + "." + micro;
            upper = major + "." + minor + "." + (parseInt(patch) + 1);
        }

        return Validated.valid("tildeRange", new TildeRange(lower, upper, metadataPattern, requireRelease));
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        Validated<TildeRange> maybeTildeRangeV1 = build(v1, null);
        Validated<TildeRange> maybeTildeRangeV2 = build(v2, null);
        if (maybeTildeRangeV1.isValid() && maybeTildeRangeV2.isValid()) {
            TildeRange tildeRangeV1 = requireNonNull(maybeTildeRangeV1.getValue());
            TildeRange tildeRangeV2 = requireNonNull(maybeTildeRangeV2.getValue());
            int compare = super.compare(currentVersion, tildeRangeV1.upperExclusive, tildeRangeV2.upperExclusive);
            if (compare != 0) {
                return compare;
            }

            return super.compare(currentVersion, tildeRangeV1.lower, tildeRangeV2.lower);
        } else if (maybeTildeRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            TildeRange tildeRangeV1 = requireNonNull(maybeTildeRangeV1.getValue());
            int compare = super.compare(currentVersion, tildeRangeV1.upperExclusive, v2);
            if (compare < 0) {
                return compare;
            } else if (compare == 0) {
                return -1;
            }

            compare = super.compare(currentVersion, tildeRangeV1.lower, v2);
            return Math.max(compare, 0);
        } else if (maybeTildeRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            TildeRange tildeRangeV2 = requireNonNull(maybeTildeRangeV2.getValue());
            int compare = super.compare(currentVersion, v1, tildeRangeV2.upperExclusive);
            if (compare > 0) {
                return compare;
            } else if (compare == 0) {
                return 1;
            }

            compare = super.compare(currentVersion, v1, tildeRangeV2.lower);
            return Math.min(compare, 0);
        }

        return super.compare(currentVersion, v1, v2);
    }
}
