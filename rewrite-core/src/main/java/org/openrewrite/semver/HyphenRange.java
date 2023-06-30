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
import org.openrewrite.internal.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
}
