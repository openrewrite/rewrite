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
 * A parsed version string, memoized in a bounded LRU: the {@link VersionComparator#RELEASE_PATTERN} match and the
 * strict SemVer 2.0.0 view (node-semver's {@code FULL} grammar), since neither grammar subsumes the other.
 */
final class ParsedVersion {

    // node-semver re.js grammar fragments, shared with the npm range classes.
    static final String NUMERIC_ID = "0|[1-9]\\d*";
    static final String PRERELEASE_ID = "(?:" + NUMERIC_ID + "|\\d*[a-zA-Z-][0-9a-zA-Z-]*)";
    static final String PRERELEASE_GROUP = "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))";
    static final String BUILD = "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)";

    static final String PRERELEASE_ID_LOOSE = "(?:\\d*[a-zA-Z-][0-9a-zA-Z-]*|\\d+)";
    static final String PRERELEASE_GROUP_LOOSE = "(?:-?(" + PRERELEASE_ID_LOOSE + "(?:\\." + PRERELEASE_ID_LOOSE +
            ")*))";
    static final String LOOSE_PLAIN = "[v=\\s]*(?:\\d+)\\.(?:\\d+)\\.(?:\\d+)" + PRERELEASE_GROUP_LOOSE + "?" + BUILD +
            "?";

    // Port of node-semver re.js FULL (major.minor.patch, no leading zeros, prerelease and build split).
    private static final Pattern STRICT_PATTERN = Pattern.compile(
            "^v?(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")\\.(" + NUMERIC_ID + ")" +
                    "(?:-(" + PRERELEASE_ID + "(?:\\." + PRERELEASE_ID + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    // node-semver re.js LOOSE: leading v/=/whitespace, leading zeros, and a prerelease without its hyphen.
    private static final Pattern LOOSE_PATTERN = Pattern.compile(
            "^[v=\\s]*(\\d+)\\.(\\d+)\\.(\\d+)" +
                    "(?:-?(" + PRERELEASE_ID_LOOSE + "(?:\\." + PRERELEASE_ID_LOOSE + ")*))?" +
                    "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private static final int MAX_LENGTH = 256;
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    private static final Map<String, ParsedVersion> LOOSE_CACHE = LruCache.bounded(1_024);

    // Producer backpatches in the OSERA shape, e.g. 2.14.1.1-osera-00001 or 5.2.19.RELEASE-backpatch-00001.
    private static final Pattern BACKPATCH = Pattern.compile("(.+)-(?:osera|backpatch)-\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC = Pattern.compile("[0-9]+");

    /** Above the distinct version strings of a large multi-module upgrade, a few hundred kilobytes retained. */
    private static final int MAX_CACHE_SIZE = 4_096;

    private static final Map<String, ParsedVersion> CACHE = LruCache.bounded(MAX_CACHE_SIZE);

    private static final ParsedVersion NO_MATCH = new ParsedVersion(false, null, null, false, null,
            false, 0, 0, 0, emptyList(), emptyList());

    private final boolean matches;

    /**
     * {@link VersionComparator#RELEASE_PATTERN}'s numeric groups 1..5, each null when absent; null when it did not
     * match.
     */
    private final @Nullable String @Nullable [] groups;

    private final @Nullable String qualifier;

    private final boolean preReleaseEnding;

    private final @Nullable String patchedVersion;

    private final boolean strictSemver;

    private final long strictMajor;
    private final long strictMinor;
    private final long strictPatch;

    // Each element is a Long (numeric identifier) or a String (alphanumeric identifier).
    private final List<Object> strictPrerelease;
    private final List<String> strictBuild;

    private ParsedVersion(boolean matches, @Nullable String @Nullable [] groups, @Nullable String qualifier,
                          boolean preReleaseEnding, @Nullable String patchedVersion, boolean strictSemver,
                          long strictMajor, long strictMinor, long strictPatch, List<Object> strictPrerelease,
                          List<String> strictBuild) {
        this.matches = matches;
        this.groups = groups;
        this.qualifier = qualifier;
        this.preReleaseEnding = preReleaseEnding;
        this.patchedVersion = patchedVersion;
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
        String patchedVersion = null;
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
            Matcher backpatch = BACKPATCH.matcher(version);
            if (backpatch.matches()) {
                patchedVersion = backpatch.group(1);
            }
        }

        ParsedVersion strict = nodeView(STRICT_PATTERN, version, matches, groups, qualifier, preReleaseEnding,
                patchedVersion);
        if (strict != null) {
            return strict;
        }
        if (!matches) {
            return NO_MATCH;
        }
        return new ParsedVersion(true, groups, qualifier, preReleaseEnding, patchedVersion,
                false, 0, 0, 0, emptyList(), emptyList());
    }

    /**
     * node-semver's {@code new SemVer(version, loose)}: the strict view of {@link #parse}, or the
     * LOOSE grammar's when {@code loose}. Unparseable input yields a non-strict result.
     */
    static ParsedVersion parseNode(String version, boolean loose) {
        if (!loose) {
            return parse(version);
        }
        ParsedVersion cached = LOOSE_CACHE.get(version);
        if (cached != null) {
            return cached;
        }
        ParsedVersion parsed = nodeView(LOOSE_PATTERN, version, false, null, null, false, null);
        if (parsed == null) {
            parsed = NO_MATCH;
        }
        LOOSE_CACHE.put(version, parsed);
        return parsed;
    }

    private static @Nullable ParsedVersion nodeView(Pattern grammar, String version, boolean matches,
                                                    @Nullable String @Nullable [] groups, @Nullable String qualifier,
                                                    boolean preReleaseEnding, @Nullable String patchedVersion) {
        if (version.length() > MAX_LENGTH) {
            return null;
        }
        Matcher m = grammar.matcher(version.trim());
        if (!m.matches()) {
            return null;
        }
        // node-semver rejects components beyond Number.MAX_SAFE_INTEGER.
        try {
            long major = Long.parseLong(m.group(1));
            long minor = Long.parseLong(m.group(2));
            long patch = Long.parseLong(m.group(3));
            if (major > MAX_SAFE_INTEGER || minor > MAX_SAFE_INTEGER || patch > MAX_SAFE_INTEGER) {
                return null;
            }
            List<Object> prerelease = emptyList();
            if (m.group(4) != null) {
                prerelease = new ArrayList<>();
                for (String id : m.group(4).split("\\.")) {
                    prerelease.add(numericIdentifier(id));
                }
            }
            List<String> build = emptyList();
            if (m.group(5) != null) {
                build = new ArrayList<>();
                for (String id : m.group(5).split("\\.")) {
                    build.add(id);
                }
            }
            return new ParsedVersion(matches, groups, qualifier, preReleaseEnding, patchedVersion,
                    true, major, minor, patch, prerelease, build);
        } catch (NumberFormatException overflow) {
            return null;
        }
    }

    private static Object numericIdentifier(String id) {
        if (isNumeric(id)) {
            try {
                long num = Long.parseLong(id);
                if (num < MAX_SAFE_INTEGER) {
                    return num;
                }
            } catch (NumberFormatException ignored) {
                // a numeric identifier beyond long range stays alphanumeric, as in node-semver
            }
        }
        return id;
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
     * The 1-based {@link VersionComparator#RELEASE_PATTERN} group (major, minor, patch, micro, fifth), or {@code null}
     * when it is absent or the version did not match.
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

    /**
     * @return the version with its backpatch qualifier removed ({@code 5.2.19.RELEASE} for
     * {@code 5.2.19.RELEASE-osera-00001}), or {@code null} if the version is not a backpatch.
     */
    @Nullable String patchedVersion() {
        return patchedVersion;
    }

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

    /**
     * @return the strict view's prerelease identifiers, each a {@code Long} (numeric) or a
     * {@code String} (alphanumeric); empty when there is none or the version is not strict.
     */
    List<Object> strictPrerelease() {
        return strictPrerelease;
    }

    boolean hasPrerelease() {
        return !strictPrerelease.isEmpty();
    }

    /**
     * @return the canonical strict rendering: {@code v} prefix and whitespace dropped, build
     * metadata retained.
     */
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
     * node-semver's {@code SemVer.version}: the strict rendering without build metadata.
     */
    String nodeVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append(strictMajor).append('.').append(strictMinor).append('.').append(strictPatch);
        if (!strictPrerelease.isEmpty()) {
            sb.append('-').append(join(strictPrerelease));
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

    /**
     * node-semver's {@code SemVer.compareBuild}: the build identifiers in order, a missing one lowest, the first two
     * that differ as strings deciding through {@code compareIdentifiers}.
     */
    int compareBuild(ParsedVersion o) {
        for (int i = 0; ; i++) {
            if (i >= strictBuild.size() && i >= o.strictBuild.size()) {
                return 0;
            }
            if (i >= o.strictBuild.size()) {
                return 1;
            }
            if (i >= strictBuild.size()) {
                return -1;
            }
            String a = strictBuild.get(i);
            String b = o.strictBuild.get(i);
            if (!a.equals(b)) {
                return compareStringIdentifiers(a, b);
            }
        }
    }

    // node-semver compareIdentifiers over two strings: numeric ones as numbers, below any other
    private static int compareStringIdentifiers(String a, String b) {
        boolean anum = NUMERIC.matcher(a).matches();
        boolean bnum = NUMERIC.matcher(b).matches();
        if (anum && bnum) {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        }
        if (anum) {
            return -1;
        }
        if (bnum) {
            return 1;
        }
        return a.compareTo(b) < 0 ? -1 : 1;
    }

    // node-semver compareIdentifiers: numeric < alphanumeric; numerics compare as numbers, others as ASCII.
    /**
     * Strict SemVer precedence over version strings; node-semver's {@code compare} contract.
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
     * A total order over any strings: strict pairs by precedence, strict above non-semver, non-semver lexicographic, so
     * {@link VersionComparator#upgrade} stays safe when the current version is a range expression.
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
