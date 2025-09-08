/*
 * Copyright 2022 the original author or authors.
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

public class SetRange extends LatestRelease {
    private static final Pattern SET_RANGE_PATTERN = Pattern.compile("([\\[(])(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)?\\s*,\\s*(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)?([\\])])");

    private final String upper;
    private final boolean upperClosed;
    private final String lower;
    private final boolean lowerClosed;

    private SetRange(String lower, boolean lowerClosed, String upper, boolean upperClosed, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.lower = lower;
        this.lowerClosed = lowerClosed;
        this.upper = upper;
        this.upperClosed = upperClosed;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return super.isValid(currentVersion, version) &&
                ((upperClosed && (upper == null || super.compare(currentVersion, version, upper) <= 0)) ||
                        (upper == null || super.compare(currentVersion, version, upper) < 0)) &&
                ((lowerClosed && (lower == null || super.compare(currentVersion, version, lower) >= 0)) ||
                        (lower == null || super.compare(currentVersion, version, lower) > 0));

    }

    public static Validated<SetRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = SET_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("setRange", pattern, "not a set range");
        }
        return Validated.valid("setRange", new SetRange(matcher.group(2), "[".equals(matcher.group(1)), matcher.group(6), "]".equals(matcher.group(10)), metadataPattern));
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        Validated<SetRange> maybeSetRangeV1 = build(v1, null);
        Validated<SetRange> maybeSetRangeV2 = build(v2, null);
        if (maybeSetRangeV1.isValid() && maybeSetRangeV2.isValid()) {
            SetRange setRangeV1 = requireNonNull(maybeSetRangeV1.getValue());
            SetRange setRangeV2 = requireNonNull(maybeSetRangeV2.getValue());
            int compare = super.compare(currentVersion, setRangeV1.upper, setRangeV2.upper);
            if (compare != 0) {
                return compare;
            }

            if (setRangeV1.upperClosed && !setRangeV2.upperClosed) {
                return 1;
            } else if (!setRangeV1.upperClosed && setRangeV2.upperClosed) {
                return -1;
            }

            compare = super.compare(currentVersion, setRangeV1.lower, setRangeV2.lower);
            if (compare != 0) {
                return compare;
            }

            if (setRangeV1.lowerClosed && !setRangeV2.lowerClosed) {
                return -1;
            } else if (!setRangeV1.lowerClosed && setRangeV2.lowerClosed) {
                return 1;
            }

            return 0;
        } else if (maybeSetRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            SetRange setRangeV1 = requireNonNull(maybeSetRangeV1.getValue());
            int compare = super.compare(currentVersion, setRangeV1.upper, v2);
            if (setRangeV1.upperClosed && compare < 0) {
                return compare;
            } else if (!setRangeV1.upperClosed && compare == 0) {
                return -1;
            }

            compare = super.compare(currentVersion, setRangeV1.lower, v2);
            if (setRangeV1.lowerClosed && compare > 0) {
                return compare;
            } else if (!setRangeV1.lowerClosed && compare == 0) {
                return 1;
            }

            return 0;
        } else if (maybeSetRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            SetRange setRangeV2 = requireNonNull(maybeSetRangeV2.getValue());
            int compare = super.compare(currentVersion, v1, setRangeV2.upper);
            if (setRangeV2.upperClosed && compare > 0) {
                return compare;
            } else if (!setRangeV2.upperClosed && compare == 0) {
                return 1;
            }

            compare = super.compare(currentVersion, v1, setRangeV2.lower);
            if (setRangeV2.lowerClosed && compare < 0) {
                return compare;
            } else if (!setRangeV2.lowerClosed && compare == 0) {
                return -1;
            }

            return 0;
        }

        return super.compare(currentVersion, v1, v2);
    }
}
