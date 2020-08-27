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
 * <a href="https://github.com/npm/node-semver#hyphen-ranges-xyz---abc">Hyphen ranges</a>.
 */
public class HyphenRange extends LatestRelease {
    private static final Pattern HYPHEN_RANGE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?(\\.\\d+)?)\\s*-\\s*(\\d+(\\.\\d+)?(\\.\\d+)?)");

    private final String upper;
    private final String lower;

    private HyphenRange(String lower, String upper, String metadataPattern) {
        super(metadataPattern);
        this.lower = fillPartialVersionWithZeroes(lower);
        this.upper = fillPartialVersionWithZeroes(upper);
    }

    private static String fillPartialVersionWithZeroes(String version) {
        if (version.chars().filter(c -> c == '.').count() < 2) {
            return fillPartialVersionWithZeroes(version + ".0");
        }
        return version;
    }

    @Override
    public boolean isValid(String version) {
        return super.isValid(version) &&
                super.compare(version, upper) <= 0 &&
                super.compare(version, lower) >= 0;
    }

    public static Validated build(String pattern, String metadataPattern) {
        Matcher matcher = HYPHEN_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("hyphenRange", pattern, "not a hyphen range");
        }
        return Validated.valid("hyphenRange", new HyphenRange(matcher.group(1), matcher.group(4), metadataPattern));
    }
}
