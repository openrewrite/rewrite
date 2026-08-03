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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.openrewrite.semver.VersionComparator.PRE_RELEASE_ENDING;
import static org.openrewrite.semver.VersionComparator.RELEASE_PATTERN;

/**
 * The cached result of parsing a version string, offering two views of the same input: the
 * Maven-flavored {@link VersionComparator#RELEASE_PATTERN} match and the strict SemVer 2.0.0 view
 * (node-semver's {@code FULL} grammar, with the prerelease identifier list kept for
 * {@link #comparePrecedence}). Neither grammar subsumes the other ({@code 1.2.3.4} matches only the
 * release pattern, {@code v1.2.3} only the strict one), so both are computed for every parse.
 * <p>
 * Version selection evaluates a selector against the <em>full</em> published version list of a
 * dependency. Without caching, every candidate allocates a fresh {@link Matcher} (and its
 * capture-group array). Because parsing is pure and the same version strings recur across
 * dependencies and, at scale, across the modules of a large multi-module build, the parse results
 * are memoized here. This removes both the {@code Matcher} and the regex-group allocations for
 * repeat parses, which dominate allocation during a large multi-module dependency upgrade.
 * <p>
 * The cache is a small bounded LRU. The win comes from re-use <em>across</em> {@code upgrade(...)}
 * calls (the same version list is re-evaluated for each module of a multi-module build), so the
 * cache must outlive a single call; the LRU caps the retained footprint and evicts cold entries. The
 * distinct version-<em>string</em> set stays small even for broad upgrades because version numbers
 * repeat across the artifacts of a release train (e.g. {@code spring-boot-starter:3.0.0} and
 * {@code spring-boot-actuator:3.0.0} share the string {@code "3.0.0"}), so the working set fits
 * within the bound without thrashing.
 */
final class ParsedVersion {

    /** A numeric component with no leading zeros, per node-semver {@code re.js}; shared with the npm range grammar. */
    static final String NUMERIC_ID = "0|[1-9]\\d*";

    /** A prerelease identifier: numeric (no leading zeros) or alphanumeric. */
    static final String PRERELEASE_ID = "(?:" + NUMERIC_ID + "|\\d*[a-zA-Z-][0-9a-zA-Z-]*)";

    // Port of node-semver re.js FULL (major.minor.patch, no leading zeros, prerelease and build split).
    private static final Pattern STRICT_PATTERN = Pattern.compile(
            "^v?(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")" +
                    "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    /**
     * Upper bound on retained entries. Comfortably larger than the distinct version-string working
     * set of a large multi-module upgrade (hundreds to low thousands), while keeping the retained
     * footprint to a few hundred kilobytes.
     */
    private static final int MAX_CACHE_SIZE = 4_096;

    private static final Map<String, ParsedVersion> CACHE = LruCache.bounded(MAX_CACHE_SIZE);

    private static final ParsedVersion NO_MATCH = new ParsedVersion(false, null, null, false,
            false, 0, 0, 0, emptyList(), emptyList());

    private final boolean matches;

    /**
     * {@link VersionComparator#RELEASE_PATTERN} numeric capture groups 1..5 (major, minor, patch,
     * micro, and a fifth component), each {@code null} when the corresponding group is absent.
     * {@code null} as a whole when the version did not match.
     */
    private final @Nullable String @Nullable [] groups;

    private final @Nullable String qualifier;

    private final boolean preReleaseEnding;

    private final boolean strictSemver;

    private final long strictMajor;
    private final long strictMinor;
    private final long strictPatch;

    // Each element is a Long (numeric identifier) or a String (alphanumeric identifier).
    private final List<Object> strictPrerelease;
    private final List<String> strictBuild;

    private ParsedVersion(boolean matches, @Nullable String @Nullable [] groups, @Nullable String qualifier,
                          boolean preReleaseEnding, boolean strictSemver, long strictMajor, long strictMinor,
                          long strictPatch, List<Object> strictPrerelease, List<String> strictBuild) {
        this.matches = matches;
        this.groups = groups;
        this.qualifier = qualifier;
        this.preReleaseEnding = preReleaseEnding;
        this.strictSemver = strictSemver;
        this.strictMajor = strictMajor;
        this.strictMinor = strictMinor;
        this.strictPatch = strictPatch;
        this.strictPrerelease = strictPrerelease;
        this.strictBuild = strictBuild;
    }

    static ParsedVersion parse(String version) {
        ParsedVersion cached = CACHE.get(version);
        if (cached != null) {
            return cached;
        }
        ParsedVersion parsed = doParse(version);
        CACHE.put(version, parsed);
        return parsed;
    }

    private static ParsedVersion doParse(String version) {
        Matcher matcher = RELEASE_PATTERN.matcher(version);
        boolean matches = matcher.matches();
        @Nullable String[] groups = null;
        String qualifier = null;
        boolean preReleaseEnding = false;
        if (matches) {
            groups = new String[]{
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5)
            };
            qualifier = matcher.group("qualifier");
            preReleaseEnding = PRE_RELEASE_ENDING.matcher(version).find();
        }

        Matcher strict = STRICT_PATTERN.matcher(version.trim());
        if (strict.matches()) {
            // node-semver permits large numerics; anything overflowing a long is treated as
            // unparseable (not strict) rather than crashing the caller.
            try {
                long major = Long.parseLong(strict.group(1));
                long minor = Long.parseLong(strict.group(2));
                long patch = Long.parseLong(strict.group(3));
                List<Object> prerelease = emptyList();
                if (strict.group(4) != null) {
                    prerelease = new ArrayList<>();
                    for (String id : strict.group(4).split("\\.")) {
                        prerelease.add(isNumeric(id) ? (Object) Long.parseLong(id) : id);
                    }
                }
                List<String> build = emptyList();
                if (strict.group(5) != null) {
                    build = new ArrayList<>();
                    for (String id : strict.group(5).split("\\.")) {
                        build.add(id);
                    }
                }
                return new ParsedVersion(matches, groups, qualifier, preReleaseEnding,
                        true, major, minor, patch, prerelease, build);
            } catch (NumberFormatException overflow) {
                // fall through to the non-strict result
            }
        }
        if (!matches) {
            return NO_MATCH;
        }
        return new ParsedVersion(true, groups, qualifier, preReleaseEnding, false, 0, 0, 0, emptyList(), emptyList());
    }

    private static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    boolean matches() {
        return matches;
    }

    /**
     * @param index a 1-based {@link VersionComparator#RELEASE_PATTERN} capture-group index in the
     *              range 1..5 (major, minor, patch, micro, fifth).
     * @return the captured numeric component, or {@code null} if that component is absent or the
     * version did not match the release pattern.
     */
    @Nullable String group(int index) {
        return groups == null ? null : groups[index - 1];
    }

    /**
     * @return the {@code qualifier} named group (the {@code [-.+]}-prefixed suffix), or {@code null}
     * if there is no qualifier or the version did not match the release pattern.
     */
    @Nullable String qualifier() {
        return qualifier;
    }

    /**
     * @return whether the version ends with a recognized pre-release qualifier (alpha, beta,
     * milestone, rc, snapshot, ...) per {@link VersionComparator#PRE_RELEASE_ENDING}.
     */
    boolean isPreReleaseEnding() {
        return preReleaseEnding;
    }

    /** Whether the version matched the strict SemVer 2.0.0 grammar. */
    boolean isStrictSemver() {
        return strictSemver;
    }

    long strictMajor() {
        return strictMajor;
    }

    long strictMinor() {
        return strictMinor;
    }

    long strictPatch() {
        return strictPatch;
    }

    /** The strict view's prerelease identifiers, each a {@code Long} or a {@code String}; empty when none or not strict. */
    List<Object> strictPrerelease() {
        return strictPrerelease;
    }

    boolean hasPrerelease() {
        return !strictPrerelease.isEmpty();
    }

    /** The canonical strict rendering: {@code v} prefix and whitespace dropped, build metadata retained. */
    String strictToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(strictMajor).append('.').append(strictMinor).append('.').append(strictPatch);
        if (!strictPrerelease.isEmpty()) {
            sb.append('-').append(join(strictPrerelease));
        }
        if (!strictBuild.isEmpty()) {
            sb.append('+').append(join(strictBuild));
        }
        return sb.toString();
    }

    /**
     * SemVer 2.0.0 section 11 precedence; build metadata ignored. Both versions must be
     * {@linkplain #isStrictSemver() strict}.
     */
    int comparePrecedence(ParsedVersion o) {
        int c = Long.compare(strictMajor, o.strictMajor);
        if (c != 0) {
            return c;
        }
        c = Long.compare(strictMinor, o.strictMinor);
        if (c != 0) {
            return c;
        }
        c = Long.compare(strictPatch, o.strictPatch);
        if (c != 0) {
            return c;
        }
        return comparePrerelease(o);
    }

    private int comparePrerelease(ParsedVersion o) {
        // A version with a prerelease has lower precedence than the same without one.
        if (strictPrerelease.isEmpty() && o.strictPrerelease.isEmpty()) {
            return 0;
        }
        if (strictPrerelease.isEmpty()) {
            return 1;
        }
        if (o.strictPrerelease.isEmpty()) {
            return -1;
        }
        int n = Math.min(strictPrerelease.size(), o.strictPrerelease.size());
        for (int i = 0; i < n; i++) {
            int c = compareIdentifiers(strictPrerelease.get(i), o.strictPrerelease.get(i));
            if (c != 0) {
                return c;
            }
        }
        // All shared identifiers equal: the longer prerelease list is higher.
        return Integer.compare(strictPrerelease.size(), o.strictPrerelease.size());
    }

    // node-semver compareIdentifiers: numeric < alphanumeric; numerics compare as numbers, others as ASCII.
    /**
     * Strict SemVer precedence over version strings; node-semver's {@code compare} contract.
     *
     * @throws IllegalArgumentException if either version is not strict SemVer
     */
    static int compareStrict(String v1, String v2) {
        ParsedVersion a = parse(v1);
        ParsedVersion b = parse(v2);
        if (!a.isStrictSemver() || !b.isStrictSemver()) {
            throw new IllegalArgumentException("Invalid version: " + (!a.isStrictSemver() ? v1 : v2));
        }
        return a.comparePrecedence(b);
    }

    /**
     * A total order over arbitrary strings: strict pairs by {@linkplain #comparePrecedence
     * precedence}, strict above non-semver, non-semver lexicographic. Keeps
     * {@link VersionComparator#upgrade} safe when the current "version" is itself a range
     * expression, as in a package.json constraint.
     */
    static int compareLenient(String v1, String v2) {
        ParsedVersion a = parse(v1);
        ParsedVersion b = parse(v2);
        if (a.isStrictSemver() && b.isStrictSemver()) {
            return a.comparePrecedence(b);
        }
        if (a.isStrictSemver()) {
            return 1;
        }
        if (b.isStrictSemver()) {
            return -1;
        }
        return v1.compareTo(v2);
    }

    private static int compareIdentifiers(Object a, Object b) {
        boolean aNum = a instanceof Long;
        boolean bNum = b instanceof Long;
        if (aNum && bNum) {
            return Long.compare((Long) a, (Long) b);
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        return ((String) a).compareTo((String) b);
    }

    private static String join(List<?> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
