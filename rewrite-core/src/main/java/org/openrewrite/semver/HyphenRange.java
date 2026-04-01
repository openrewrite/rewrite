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

import static java.util.Objects.requireNonNull;
import static org.openrewrite.semver.Semver.isVersion;

/**
 * <a href="https://github.com/npm/node-semver#hyphen-ranges-xyz---abc">Hyphen ranges</a>.
 */
public class HyphenRange extends LatestRelease {
    private static final Pattern HYPHEN_RANGE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)\\s*-\\s*(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)");

    private final String upper;
    private final String lower;

    private HyphenRange(String lower, String upper, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return super.isValid(currentVersion, version) &&
                super.compare(currentVersion, version, upper) <= 0 &&
                super.compare(currentVersion, version, lower) >= 0;
    }

    public static Validated<HyphenRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = HYPHEN_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("hyphenRange", pattern, "not a hyphen range");
        }
        return Validated.valid("hyphenRange", new HyphenRange(matcher.group(1), matcher.group(5), metadataPattern));
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        Validated<HyphenRange> maybeHyphenRangeV1 = build(v1, null);
        Validated<HyphenRange> maybeHyphenRangeV2 = build(v2, null);
        if (maybeHyphenRangeV1.isValid() && maybeHyphenRangeV2.isValid()) {
            HyphenRange hyphenRangeV1 = requireNonNull(maybeHyphenRangeV1.getValue());
            HyphenRange hyphenRangeV2 = requireNonNull(maybeHyphenRangeV2.getValue());
            int compare = super.compare(currentVersion, hyphenRangeV1.upper, hyphenRangeV2.upper);
            if (compare != 0) {
                return compare;
            }

            return super.compare(currentVersion, hyphenRangeV1.lower, hyphenRangeV2.lower);
        } else if (maybeHyphenRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            HyphenRange hyphenRangeV1 = requireNonNull(maybeHyphenRangeV1.getValue());
            int compare = super.compare(currentVersion, hyphenRangeV1.upper, v2);
            if (compare < 0) {
                return compare;
            }

            compare = super.compare(currentVersion, hyphenRangeV1.lower, v2);
            return Math.max(compare, 0);

        } else if (maybeHyphenRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            HyphenRange hyphenRangeV2 = requireNonNull(maybeHyphenRangeV2.getValue());
            int compare = super.compare(currentVersion, v1, hyphenRangeV2.upper);
            if (compare > 0) {
                return compare;
            }

            compare = super.compare(currentVersion, v1, hyphenRangeV2.lower);
            return Math.min(compare, 0);
        }

        return super.compare(currentVersion, v1, v2);
    }
}
