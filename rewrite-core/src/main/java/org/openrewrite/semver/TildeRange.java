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

import static java.lang.Integer.parseInt;

/**
 * Allows patch-level changes if a minor version is specified on the comparator. Allows minor-level changes if not.
 * <a href="https://github.com/npm/node-semver#tilde-ranges-123-12-1">Tilde ranges</a>.
 */
public class TildeRange extends LatestRelease {
    private static final Pattern TILDE_RANGE_PATTERN = Pattern.compile("~(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private final String upperExclusive;
    private final String lower;

    private TildeRange(String lower, String upperExclusive, String metadataPattern) {
        super(metadataPattern);
        this.lower = lower;
        this.upperExclusive = upperExclusive;
    }

    @Override
    public boolean isValid(String version) {
        return super.isValid(version) &&
                super.compare(version, upperExclusive) < 0 &&
                super.compare(version, lower) >= 0;
    }

    public static Validated build(String pattern, String metadataPattern) {
        Matcher matcher = TILDE_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("tildeRange", pattern, "not a tilde range");
        }

        String major = matcher.group(1);
        String minor = matcher.group(2);
        String patch = matcher.group(3);

        String lower;
        String upper;

        if (minor == null) {
            lower = major + ".0.0";
            upper = (parseInt(major) + 1) + ".0.0";
        } else if (patch == null) {
            lower = major + "." + minor + ".0";
            upper = major + "." + (parseInt(minor) + 1) + ".0";
        } else {
            lower = major + "." + minor + "." + patch;
            upper = major + "." + (parseInt(minor) + 1) + ".0";
        }

        return Validated.valid("tildeRange", new TildeRange(lower, upper, metadataPattern));
    }
}
