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

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static Validated build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = SET_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("setRange", pattern, "not a set range");
        }
        return Validated.valid("setRange", new SetRange(matcher.group(2), "[".equals(matcher.group(1)), matcher.group(6), "]".equals(matcher.group(10)), metadataPattern));
    }
}
