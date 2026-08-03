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

import lombok.EqualsAndHashCode;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.internal.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

@UtilityClass
public class Semver {

    /**
     * The versioning semantics a selector string should be interpreted under. The same string can be
     * valid in both ecosystems with different meanings ({@code ~1.2.3.4} is a valid Maven tilde
     * range but invalid npm; {@code ^16.8.0 || ^17.0.0} is a valid npm union but invalid Maven), so
     * callers say which they want.
     */
    public enum Ecosystem {
        /**
         * The historical Maven/Gradle-flavored interpretation: 4th/5th numeric components, the
         * {@code .RELEASE}/{@code .FINAL}/{@code .GA} suffixes, {@code [1.5,2)} interval notation,
         * Gradle {@code +} dynamic versions, and a qualifier ladder for prerelease ordering.
         */
        MAVEN,

        /**
         * Exact npm/node-semver semantics: strict SemVer 2.0.0 versions (three components,
         * section 11 prerelease precedence, build metadata ignored), npm's range grammar including
         * {@code ||} unions, space-separated intersections, x-ranges and partials, {@code -0}
         * exclusive upper bounds, and npm's prerelease gating rule. The {@code latest.*} selector
         * keywords remain available.
         */
        NODE
    }

    /**
     * Memoizes {@link #validate(String, String, Ecosystem)} keyed on its
     * {@code (toVersion, metadataPattern, ecosystem)} arguments. A version selector is a recipe
     * parameter, so it is constant for the duration of a run yet {@code validate} is otherwise
     * re-invoked for every dependency and every visit. Each invocation runs the full chain of
     * selector-detection regexes (one {@code Pattern.matcher} per comparator type), which dominates
     * {@code java.util.regex} CPU time during large recipe runs. Caching collapses the thousands of
     * repeated calls to one computation per distinct selector.
     * <p>
     * Both valid <em>and</em> invalid results are cached: an {@link Validated} carries its validation
     * errors, and re-deriving them is just as costly. The cached {@link VersionComparator}
     * implementations store only final parsed fields and are immutable, so sharing them across the
     * {@link java.util.concurrent.ForkJoinPool} that runs recipes is safe.
     */
    private static final Map<CacheKey, Validated<VersionComparator>> VALIDATE_CACHE = LruCache.bounded(1_024);

    private static final LatestRelease MAVEN_PRECEDENCE = new LatestRelease(null);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isVersion(@Nullable String version) {
        if (StringUtils.isBlank(version)) {
            return false;
        }
        return ParsedVersion.parse(version).matches();
    }

    /**
     * Validates the given version against an optional pattern under the historical
     * {@link Ecosystem#MAVEN} interpretation. See {@link #validate(String, String, Ecosystem)} for
     * npm-exact semantics.
     * <p>
     * The {@code metadataPattern} is interpreted first as a regular expression. If that fails to
     * compile, it is treated as a glob (where {@code *} matches any run of characters and {@code ?}
     * matches any single character) and converted to an equivalent regex via
     * {@link StringUtils#globToRegex(String)}. This lets simple patterns like {@code "+backpatch*"}
     * work without needing to be regex-escaped.
     *
     * @param toVersion       the version to validate. Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used.
     * @param metadataPattern optional metadata appended to the version. Allows version selection to be extended beyond the original Node Semver semantics. So for example,
     *                        setting 'version' to "25-29" can be paired with a metadata pattern of "-jre" to select Guava 29.0-jre. Accepts either a regex or a glob.
     * @return the validation result
     */
    public static Validated<VersionComparator> validate(String toVersion, @Nullable String metadataPattern) {
        return validate(toVersion, metadataPattern, Ecosystem.MAVEN);
    }

    /**
     * Validates the given version selector against an optional metadata pattern under the given
     * ecosystem's semantics. Under {@link Ecosystem#NODE}, the {@code latest.*} selector keywords
     * are recognized and everything else must be a valid npm range ({@code ^1.2.3},
     * {@code >=1.2.9 <2.0.0}, {@code ^16.8.0 || ^17.0.0}, {@code 1.2.x}, {@code 1.2.3 - 2},
     * {@code *}, ...); strings that are not npm ranges — dist-tags like {@code latest}, protocol
     * specifiers like {@code workspace:*} or {@code npm:foo@1.2.3}, git URLs, Maven-only shapes like
     * {@code ~1.2.3.4} or {@code [1.5,2)} — are invalid.
     */
    public static Validated<VersionComparator> validate(String toVersion, @Nullable String metadataPattern, Ecosystem ecosystem) {
        CacheKey key = new CacheKey(toVersion, metadataPattern, ecosystem);
        Validated<VersionComparator> cached = VALIDATE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Validated<VersionComparator> result = doValidate(toVersion, metadataPattern, ecosystem);
        VALIDATE_CACHE.put(key, result);
        return result;
    }

    private static Validated<VersionComparator> doValidate(String toVersion, @Nullable String metadataPattern, Ecosystem ecosystem) {
        String canonicalPattern = canonicalizeMetadataPattern(metadataPattern);
        Validated<VersionComparator> metadataValidation = Validated.<VersionComparator, String>testNone(
                "metadataPattern",
                "must be a valid regular expression or glob",
                metadataPattern, metadata -> metadata == null || canonicalPattern != null
        );
        if (ecosystem == Ecosystem.NODE) {
            return metadataValidation.and(Validated.<VersionComparator>none()
                    .or(LatestRelease.buildLatestRelease(toVersion, canonicalPattern))
                    .or(LatestIntegration.build(toVersion, canonicalPattern))
                    .or(LatestMinor.build(toVersion, canonicalPattern))
                    .or(LatestPatch.build(toVersion, canonicalPattern))
                    .or(HyphenRange.buildNode(toVersion, canonicalPattern))
                    .or(XRange.buildNode(toVersion, canonicalPattern))
                    .or(TildeRange.buildNode(toVersion, canonicalPattern))
                    .or(CaretRange.buildNode(toVersion, canonicalPattern))
                    .or(UnionRange.build(toVersion, canonicalPattern))
            );
        }
        return metadataValidation.and(Validated.<VersionComparator>none()
                .or(LatestRelease.buildLatestRelease(toVersion, canonicalPattern))
                .or(LatestIntegration.build(toVersion, canonicalPattern))
                .or(LatestMinor.build(toVersion, canonicalPattern))
                .or(LatestPatch.build(toVersion, canonicalPattern))
                .or(HyphenRange.build(toVersion, canonicalPattern))
                .or(XRange.build(toVersion, canonicalPattern))
                .or(TildeRange.build(toVersion, canonicalPattern))
                .or(CaretRange.build(toVersion, canonicalPattern))
                .or(SetRange.build(toVersion, canonicalPattern))
                .or(ExactVersionWithPattern.build(toVersion, canonicalPattern))
                .or(ExactVersion.build(toVersion))
        );
    }

    /**
     * Whether {@code version} is admitted by {@code selector} under the given ecosystem's
     * semantics; {@code false} when the selector is invalid (mirroring node-semver, where a
     * non-range constraint satisfies nothing).
     */
    public static boolean satisfies(String version, String selector, Ecosystem ecosystem) {
        Validated<VersionComparator> validated = validate(selector, null, ecosystem);
        if (!validated.isValid()) {
            return false;
        }
        //noinspection DataFlowIssue
        return validated.getValue().isValid(null, version);
    }

    /**
     * The highest of {@code versions} admitted by {@code selector} under the given ecosystem's
     * semantics, returned in its original spelling (the first-seen candidate wins ties);
     * {@code null} when the selector is invalid or nothing satisfies it.
     */
    public static @Nullable String maxSatisfying(Collection<String> versions, String selector, Ecosystem ecosystem) {
        Validated<VersionComparator> validated = validate(selector, null, ecosystem);
        if (!validated.isValid()) {
            return null;
        }
        //noinspection DataFlowIssue
        return validated.getValue().maxSatisfying(versions).orElse(null);
    }

    /**
     * Compares two concrete versions under the given ecosystem's precedence rules.
     * {@link Ecosystem#NODE} applies SemVer 2.0.0 section 11 (build metadata ignored) and throws
     * {@link IllegalArgumentException} if either version is not strict SemVer;
     * {@link Ecosystem#MAVEN} applies the Maven-flavored ordering (suffix normalization and the
     * qualifier ladder) and is total over arbitrary strings.
     */
    public static int compare(String v1, String v2, Ecosystem ecosystem) {
        if (ecosystem == Ecosystem.NODE) {
            return ParsedVersion.compareStrict(v1, v2);
        }
        return MAVEN_PRECEDENCE.compare(null, v1, v2);
    }

    @EqualsAndHashCode
    private static final class CacheKey {
        private final String toVersion;

        @Nullable
        private final String metadataPattern;

        private final Ecosystem ecosystem;

        private CacheKey(String toVersion, @Nullable String metadataPattern, Ecosystem ecosystem) {
            this.toVersion = toVersion;
            this.metadataPattern = metadataPattern;
            this.ecosystem = ecosystem;
        }
    }

    private static @Nullable String canonicalizeMetadataPattern(@Nullable String metadataPattern) {
        if (metadataPattern == null) {
            return null;
        }
        try {
            Pattern.compile(metadataPattern);
            return metadataPattern;
        } catch (Throwable regexFailure) {
            String asRegex = StringUtils.globToRegex(metadataPattern);
            try {
                if (asRegex != null) {
                    Pattern.compile(asRegex);
                    return asRegex;
                }
            } catch (Throwable ignored) {
                // fall through
            }
            return null;
        }
    }

    public static String majorVersion(String version) {
        String major = versionSegment(version, 0);
        return major == null ? version : major;
    }

    public static String minorVersion(String version) {
        String minor = versionSegment(version, 1);
        return StringUtils.isNumeric(minor) ? minor : version;
    }

    /**
     * @return the raw version segment at {@code index} (0 = major, 1 = minor, ...), or {@code null} if absent.
     */
    static @Nullable String versionSegment(String version, int index) {
        Scanner scanner = new Scanner(version);
        scanner.useDelimiter("[.\\-$]");
        for (int i = 0; i < index; i++) {
            if (!scanner.hasNext()) {
                return null;
            }
            scanner.next();
        }
        return scanner.hasNext() ? scanner.next() : null;
    }

    public static @Nullable String max(@Nullable String version1, @Nullable String version2) {
        if (!isVersion(version1)) {
            return StringUtils.isBlank(version2) ? null : version2;
        } else if (!isVersion(version2)) {
            return version1;
        }

        String major1 = Semver.majorVersion(version1);
        String major2 = Semver.majorVersion(version2);
        String minor1 = Semver.minorVersion(version1);
        String minor2 = Semver.minorVersion(version2);

        if (!StringUtils.isNumeric(major1) || !StringUtils.isNumeric(major2) ||
                !StringUtils.isNumeric(minor1) || !StringUtils.isNumeric(minor2)) {
            if (version1.equals(version2)) {
                return version1;
            }
            return version1.compareTo(version2) >= 0 ? version1 : version2;
        }
        try {
            long maj1 = Long.parseLong(major1);
            long maj2 = Long.parseLong(major2);
            if (maj1 != maj2) return maj1 > maj2 ? version1 : version2;

            long min1 = Long.parseLong(minor1);
            long min2 = Long.parseLong(minor2);
            if (min1 != min2) return min1 > min2 ? version1 : version2;

            String[] parts1 = version1.split("[.-]");
            String[] parts2 = version2.split("[.-]");
            long patch1 = parts1.length > 2 && parts1[2].matches("\\d+") ? Long.parseLong(parts1[2]) : 0;
            long patch2 = parts2.length > 2 && parts2[2].matches("\\d+") ? Long.parseLong(parts2[2]) : 0;
            if (patch1 != patch2) return patch1 > patch2 ? version1 : version2;

            String label1 = parts1.length > 3 ? parts1[3].toLowerCase() : "";
            String label2 = parts2.length > 3 ? parts2[3].toLowerCase() : "";

            if (label1.isEmpty() && !label2.isEmpty()) return version1;
            if (!label1.isEmpty() && label2.isEmpty()) return version2;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error comparing version number \"" + version1 + "\" to \"" + version2 + "\"", e);
        }
        return version1;
    }
}
