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
import org.openrewrite.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * An npm range as node-semver models it: an OR ({@code ||}) of AND-groups of {@link NodeComparand}
 * clauses, parsed by a direct port of node's {@code classes/range.js}. A prerelease only satisfies
 * a group when some clause names the same major.minor.patch tuple with a prerelease of its own.
 */
class UnionRange implements VersionComparator {

    private static final int MAX_CACHE_SIZE = 4_096;
    private static final Map<String, UnionRange> CACHE = LruCache.bounded(MAX_CACHE_SIZE);
    private static final UnionRange INVALID = new UnionRange("", new ArrayList<>(), null);

    private final String raw;
    private final List<List<NodeComparand>> set;

    private final @Nullable String metadataPattern;

    private UnionRange(String raw, List<List<NodeComparand>> set, @Nullable String metadataPattern) {
        this.raw = raw;
        this.set = set;
        this.metadataPattern = metadataPattern;
    }

    static Validated<VersionComparator> build(String pattern, @Nullable String metadataPattern) {
        UnionRange range = parse(pattern);
        if (range == null) {
            return Validated.invalid("nodeRange", pattern, "not a node-semver range");
        }
        return Validated.valid("nodeRange", metadataPattern == null ? range :
                new UnionRange(range.raw, range.set, metadataPattern));
    }

    static @Nullable UnionRange parse(@Nullable String range) {
        if (range == null) {
            return null;
        }
        UnionRange cached = CACHE.get(range);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        UnionRange parsed = doParse(range);
        CACHE.put(range, parsed == null ? INVALID : parsed);
        return parsed;
    }

    private static @Nullable UnionRange doParse(String range) {
        List<List<NodeComparand>> set = new ArrayList<>();
        for (String group : range.trim().split("\\|\\|", -1)) {
            List<NodeComparand> comparands = parseGroup(group);
            if (comparands == null) {
                return null;
            }
            set.add(comparands);
        }
        return new UnionRange(range, set, null);
    }

    private static @Nullable List<NodeComparand> parseGroup(String groupStr) {
        String g = trimOperators(HyphenRange.hyphenReplace(groupStr.trim())).trim();
        String[] pieces = g.isEmpty() ? new String[]{""} : g.split("\\s+");
        List<NodeComparand> comparands = new ArrayList<>();
        for (String piece : pieces) {
            String s = CaretRange.replaceCaret(piece);
            s = mapTokens(s, TildeRange::replaceTilde);
            s = mapTokens(s, XRange::replaceXRange);
            s = mapTokens(s, XRange::replaceStar);
            String st = s.trim();
            if (st.isEmpty()) {
                comparands.add(NodeComparand.parse(""));
            } else {
                for (String tok : st.split("\\s+")) {
                    NodeComparand c = NodeComparand.parse(tok);
                    if (c == null) {
                        return null;
                    }
                    comparands.add(c);
                }
            }
        }
        return comparands;
    }

    private static String mapTokens(String s, UnaryOperator<String> fn) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(fn.apply(part));
        }
        return sb.toString();
    }

    private static String trimOperators(String s) {
        String out = s.replaceAll("(~>?|\\^)\\s+", "$1");
        return out.replaceAll("(<=?|>=?|=)\\s+", "$1");
    }

    boolean test(ParsedVersion version) {
        for (List<NodeComparand> group : set) {
            if (testGroup(group, version)) {
                return true;
            }
        }
        return false;
    }

    private boolean testGroup(List<NodeComparand> group, ParsedVersion version) {
        for (NodeComparand c : group) {
            if (!c.test(version)) {
                return false;
            }
        }
        if (version.hasPrerelease()) {
            // A prerelease only satisfies the group if some comparand names the same
            // major.minor.patch tuple and itself carries a prerelease.
            for (NodeComparand c : group) {
                if (c.isAny()) {
                    continue;
                }
                ParsedVersion s = c.getVersion();
                if (s.hasPrerelease() &&
                        s.strictMajor() == version.strictMajor() &&
                        s.strictMinor() == version.strictMinor() &&
                        s.strictPatch() == version.strictPatch()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    boolean isValidVersion(String version, @Nullable String metadataPattern) {
        ParsedVersion v = ParsedVersion.parse(version);
        if (!v.isStrictSemver() || !test(v)) {
            return false;
        }
        return metadataPattern == null ||
                VersionComparator.checkVersion(v.strictToString(), metadataPattern, false);
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        return isValidVersion(version, metadataPattern);
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        return ParsedVersion.compareLenient(v1, v2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < set.size(); i++) {
            if (i > 0) {
                sb.append("||");
            }
            StringBuilder group = new StringBuilder();
            for (NodeComparand c : set.get(i)) {
                String v = c.toString();
                if (v.isEmpty()) {
                    continue;
                }
                if (group.length() > 0) {
                    group.append(' ');
                }
                group.append(v);
            }
            sb.append(group);
        }
        return sb.toString();
    }

    // --- corpus-facing operations (exercised by the node-semver conformance suite) ---

    static boolean satisfies(String version, String range) {
        ParsedVersion v = ParsedVersion.parse(version);
        UnionRange r = parse(range);
        return v.isStrictSemver() && r != null && r.test(v);
    }

    static @Nullable String minSatisfying(Collection<String> versions, String range) {
        UnionRange r = parse(range);
        if (r == null) {
            return null;
        }
        String min = null;
        ParsedVersion minVersion = null;
        for (String candidate : versions) {
            ParsedVersion v = ParsedVersion.parse(candidate);
            if (v.isStrictSemver() && r.test(v) && (minVersion == null || v.comparePrecedence(minVersion) < 0)) {
                min = candidate;
                minVersion = v;
            }
        }
        return min;
    }
}
