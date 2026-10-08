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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * node-semver's functions with its {@code loose} and {@code includePrerelease} options, exactly as
 * npm evaluates them. Unlike {@link Semver#validate(String, String, Semver.Ecosystem)} with
 * {@link Semver.Ecosystem#NODE}, no {@code latest.*} selector keywords are recognized.
 */
@UtilityClass
public class NodeSemver {

    private static final int MAX_LENGTH = 256;

    /**
     * The node-semver release whose answers the functions give: 7.8.4, which npm bundles, or 5.5.0, which yarn
     * 1.22.22 bundles.
     */
    public enum Release {
        V5_5_0, V7_8_4
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Options {
        public static final Options DEFAULT = new Options(false, false, Release.V7_8_4);
        public static final Options LOOSE = new Options(true, false, Release.V7_8_4);
        public static final Options INCLUDE_PRERELEASE = new Options(false, true, Release.V7_8_4);
        public static final Options LOOSE_INCLUDE_PRERELEASE = new Options(true, true, Release.V7_8_4);

        /**
         * node-semver 5.5.0, whose functions take only {@code loose}.
         */
        public static final Options V5_5_0 = new Options(false, false, Release.V5_5_0);
        public static final Options V5_5_0_LOOSE = new Options(true, false, Release.V5_5_0);

        private final boolean loose;
        private final boolean includePrerelease;
        private final Release release;

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
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose, options.release);
        if (r == null) {
            return null;
        }
        String formatted = r.range();
        return formatted.isEmpty() ? "*" : formatted;
    }

    /**
     * @throws IllegalArgumentException under 5.5.0, for a version that does not parse, as its {@code Range.test}
     *                                  throws.
     */
    public static boolean satisfies(String version, String range, Options options) {
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose, options.release);
        return r != null && test(r, version, options);
    }

    private static boolean test(UnionRange r, String version, Options options) {
        if (options.release == Release.V5_5_0 && !version.isEmpty()) {
            if (version.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(tooLong());
            }
            if (!ParsedVersion.parseNode(version, options.loose).isStrictSemver()) {
                throw new IllegalArgumentException("Invalid Version: " + version);
            }
        }
        return r.test(version, options.loose);
    }

    /**
     * {@code satisfies} with the range as JSON package data may hold it, in any type: node-semver answers
     * false for every range its {@code Range} constructor rejects, a non-string among them.
     */
    public static boolean satisfies(String version, @Nullable Object range, Options options) {
        return range instanceof String && satisfies(version, (String) range, options);
    }

    /**
     * {@code satisfies} with the version as JSON package data may hold it, in any type: a falsy value
     * fails, and a truthy one that is not a string throws the TypeError node-semver raises on it.
     */
    public static boolean satisfies(@Nullable Object version, String range, Options options) {
        if (version instanceof String) {
            return satisfies((String) version, range, options);
        }
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose, options.release);
        if (r == null || !truthy(version)) {
            return false;
        }
        // Range.test hands the value to each set as it is: a comparator other than `*` makes a SemVer of it and
        // throws, and a set of `*` alone reads version.prerelease.length
        for (List<NodeComparand> set : r.sets()) {
            for (NodeComparand comparator : set) {
                if (!comparator.isAny()) {
                    throw new IllegalArgumentException(notASemVer(version, options));
                }
            }
            if (!truthy(prereleaseLength(version)) || options.includePrerelease) {
                return true;
            }
        }
        return false;
    }

    // version.prerelease.length of a JSON value
    private static @Nullable Object prereleaseLength(Object version) {
        if (!(version instanceof Map) || !((Map<?, ?>) version).containsKey("prerelease")) {
            throw new IllegalArgumentException("Cannot read properties of undefined (reading 'length')");
        }
        Object prerelease = ((Map<?, ?>) version).get("prerelease");
        if (prerelease == null) {
            throw new IllegalArgumentException("Cannot read properties of null (reading 'length')");
        }
        if (prerelease instanceof List) {
            return (double) ((List<?>) prerelease).size();
        }
        if (prerelease instanceof String) {
            return (double) ((String) prerelease).length();
        }
        return prerelease instanceof Map ? ((Map<?, ?>) prerelease).get("length") : null;
    }

    /**
     * @return the version with surrounding whitespace and leading {@code =} and {@code v} removed, normalized, or
     * {@code null} when it does not parse.
     */
    public static @Nullable String clean(String version, Options options) {
        int start = 0;
        int end = version.length();
        while (start < end && isJsWhitespace(version.charAt(start))) {
            start++;
        }
        while (end > start && isJsWhitespace(version.charAt(end - 1))) {
            end--;
        }
        String trimmed = version.substring(start, end);
        int i = 0;
        while (i < trimmed.length() && (trimmed.charAt(i) == '=' || trimmed.charAt(i) == 'v')) {
            i++;
        }
        return valid(trimmed.substring(i), options);
    }

    private static boolean isJsWhitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\uFEFF';
    }

    /**
     * @return the highest of the versions the range allows, the first of equal ones, or {@code null}.
     */
    public static @Nullable String maxSatisfying(Collection<String> versions, String range, Options options) {
        UnionRange r = UnionRange.parse(range, options.includePrerelease, options.loose, options.release);
        if (r == null) {
            return null;
        }
        String max = null;
        ParsedVersion maxVersion = null;
        for (String version : versions) {
            if (test(r, version, options)) {
                ParsedVersion v = ParsedVersion.parseNode(version, options.loose);
                if (maxVersion == null || maxVersion.comparePrecedence(v) < 0) {
                    max = version;
                    maxVersion = v;
                }
            }
        }
        return max;
    }

    /**
     * @throws IllegalArgumentException when either range is invalid, as node-semver throws.
     */
    public static boolean intersects(String range1, String range2, Options options) {
        UnionRange r1 = UnionRange.parse(range1, options.includePrerelease, options.loose, options.release);
        UnionRange r2 = UnionRange.parse(range2, options.includePrerelease, options.loose, options.release);
        if (r1 == null || r2 == null) {
            throw new IllegalArgumentException("Invalid SemVer Range: " + (r1 == null ? range1 : range2));
        }
        return r1.intersects(r2);
    }

    /**
     * @throws IllegalArgumentException when either version is invalid, as node-semver throws.
     */
    public static int compare(String v1, String v2, Options options) {
        return compare((Object) v1, v2, options);
    }

    /**
     * {@code compare} with the versions as JSON package data may hold them, in any type.
     *
     * @throws IllegalArgumentException when either version is invalid or not a string, as node-semver throws.
     */
    public static int compare(@Nullable Object v1, @Nullable Object v2, Options options) {
        ParsedVersion a = semver(v1, options);
        ParsedVersion b = semver(v2, options);
        return a.comparePrecedence(b);
    }

    /**
     * node-semver's {@code compareBuild}: {@code compare}, then the build metadata.
     *
     * @throws IllegalArgumentException when either version is invalid, as node-semver throws.
     */
    public static int compareBuild(String v1, String v2, Options options) {
        ParsedVersion a = semver(v1, options);
        ParsedVersion b = semver(v2, options);
        int c = a.comparePrecedence(b);
        return c != 0 ? c : a.compareBuild(b);
    }

    /**
     * node-semver's {@code rsort}: the list sorted in place from the highest version down by {@code compareBuild},
     * stable as {@code Array.prototype.sort} is.
     *
     * @throws IllegalArgumentException when a version it compares is invalid, as node-semver throws.
     */
    public static void rsort(List<String> versions, Options options) {
        versions.sort((a, b) -> compareBuild(b, a, options));
    }

    private static ParsedVersion semver(@Nullable Object version, Options options) {
        if (!(version instanceof String)) {
            throw new IllegalArgumentException(notASemVer(version, options));
        }
        if (((String) version).length() > MAX_LENGTH) {
            throw new IllegalArgumentException(tooLong());
        }
        ParsedVersion v = ParsedVersion.parseNode((String) version, options.loose);
        if (!v.isStrictSemver()) {
            throw new IllegalArgumentException("Invalid Version: " + version);
        }
        return v;
    }

    private static String tooLong() {
        return "version is longer than " + MAX_LENGTH + " characters";
    }

    // the TypeError of `new SemVer(version)` on a value that is not a string
    private static String notASemVer(@Nullable Object version, Options options) {
        if (options.release == Release.V5_5_0) {
            return "Invalid Version: " + jsString(version);
        }
        String type = version instanceof Number ? "number" : version instanceof Boolean ? "boolean" : "object";
        return "Invalid version. Must be a string. Got type \"" + type + "\".";
    }

    // String(value) of a JSON value
    private static String jsString(@Nullable Object value) {
        if (value instanceof Number) {
            return numberToString(((Number) value).doubleValue());
        }
        if (value instanceof List) {
            StringJoiner joined = new StringJoiner(",");
            for (Object item : (List<?>) value) {
                joined.add(item == null ? "" : jsString(item));
            }
            return joined.toString();
        }
        return value instanceof Map ? "[object Object]" : String.valueOf(value);
    }

    // Number.prototype.toString()
    private static String numberToString(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return Double.isNaN(d) ? "NaN" : d > 0 ? "Infinity" : "-Infinity";
        }
        if (d == 0) {
            return "0";
        }
        BigDecimal shortest = new BigDecimal(Double.toString(d)).stripTrailingZeros();
        String digits = shortest.unscaledValue().abs().toString();
        int n = digits.length() - shortest.scale();
        String sign = d < 0 ? "-" : "";
        if (n >= -5 && n <= 21) {
            return sign + shortest.abs().toPlainString();
        }
        String mantissa = digits.length() == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
        return sign + mantissa + "e" + (n - 1 >= 0 ? "+" : "-") + Math.abs(n - 1);
    }

    private static boolean truthy(@Nullable Object value) {
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return value != null && !Boolean.FALSE.equals(value);
    }
}
