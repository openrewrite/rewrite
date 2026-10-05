/*
 * Copyright 2026 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * node-semver's functions with its {@code loose} and {@code includePrerelease} options, exactly as
 * npm evaluates them. Unlike {@link Semver#validate(String, String, Semver.Ecosystem)} with
 * {@link Semver.Ecosystem#NODE}, no {@code latest.*} selector keywords are recognized.
 */
@UtilityClass
public class NodeSemver {

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Options {
        public static final Options DEFAULT = new Options(false, false);
        public static final Options LOOSE = new Options(true, false);
        public static final Options INCLUDE_PRERELEASE = new Options(false, true);
        public static final Options LOOSE_INCLUDE_PRERELEASE = new Options(true, true);

        private final boolean loose;
        private final boolean includePrerelease;

        public static Options of(boolean loose, boolean includePrerelease) {
            return loose ? (includePrerelease ? LOOSE_INCLUDE_PRERELEASE : LOOSE) :
                    (includePrerelease ? INCLUDE_PRERELEASE : DEFAULT);
        }
    }

    /**
     * @return the normalized version, or {@code null} when it does not parse.
     */
    public static @Nullable String valid(String version, Options options) {
        ParsedVersion v = ParsedVersion.parseNode(version, options.loose);
        return v.isStrictSemver() ? v.nodeVersion() : null;
    }

    /**
     * @return the range's canonical form ({@code *} for any version), or {@code null} when invalid.
     */
    public static @Nullable String validRange(String range, Options options) {
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose);
        if (r == null) {
            return null;
        }
        String formatted = r.range();
        return formatted.isEmpty() ? "*" : formatted;
    }

    public static boolean satisfies(String version, String range, Options options) {
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose);
        return r != null && r.test(version, options.loose);
    }

    /**
     * @throws IllegalArgumentException when either range is invalid, as node-semver throws.
     */
    public static boolean intersects(String range1, String range2, Options options) {
        UnionRange r1 = UnionRange.parse(range1, options.includePrerelease, options.loose);
        UnionRange r2 = UnionRange.parse(range2, options.includePrerelease, options.loose);
        if (r1 == null || r2 == null) {
            throw new IllegalArgumentException("Invalid SemVer Range: " + (r1 == null ? range1 : range2));
        }
        return r1.intersects(r2);
    }

    /**
     * @throws IllegalArgumentException when either version is invalid, as node-semver throws.
     */
    public static int compare(String v1, String v2, Options options) {
        ParsedVersion a = ParsedVersion.parseNode(v1, options.loose);
        ParsedVersion b = ParsedVersion.parseNode(v2, options.loose);
        if (!a.isStrictSemver() || !b.isStrictSemver()) {
            throw new IllegalArgumentException("Invalid Version: " + (!a.isStrictSemver() ? v1 : v2));
        }
        return a.comparePrecedence(b);
    }
}
