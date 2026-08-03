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

/**
 * An npm/node-semver range: a top-level OR ({@code ||}) of AND-groups, each a list of
 * {@link NodeComparand} primitives. The parse pipeline is a direct port of node-semver's
 * {@code classes/range.js}: hyphen, caret ({@code ^}), tilde ({@code ~}), x-range
 * ({@code x}/{@code *}) and star desugaring — each rewrite living on the corresponding range class
 * ({@link HyphenRange}, {@link CaretRange}, {@link TildeRange}, {@link XRange}) — including npm's
 * {@code -0} upper-bound convention (e.g. {@code ^1.2.3} desugars to {@code >=1.2.3 <2.0.0-0}),
 * which excludes {@code 2.0.0-alpha}.
 * <p>
 * {@link #test} applies node's prerelease-inclusion rule: a prerelease candidate satisfies an
 * AND-group only if some comparand in that group names the same {@code [major,minor,patch]} tuple
 * <em>and</em> itself carries a prerelease (unless {@code includePrerelease} is set).
 * <p>
 * In the {@link Semver.Ecosystem#NODE NODE} selector chain this class is the terminal member,
 * handling what no single range class does: unions, space-separated AND-groups, primitive
 * comparators and exact versions. Single-token sugar dispatches to the range classes themselves,
 * which delegate their npm-mode evaluation back to an instance of this class.
 */
class UnionRange implements VersionComparator {

    private static final int MAX_CACHE_SIZE = 4_096;
    private static final Map<String, UnionRange> CACHE = LruCache.bounded(MAX_CACHE_SIZE);
    private static final UnionRange INVALID = new UnionRange("", false, new ArrayList<>(), null);

    private final String raw;
    private final boolean includePrerelease;
    private final List<List<NodeComparand>> set;

    private final @Nullable String metadataPattern;

    private UnionRange(String raw, boolean includePrerelease, List<List<NodeComparand>> set,
                       @Nullable String metadataPattern) {
        this.raw = raw;
        this.includePrerelease = includePrerelease;
        this.set = set;
        this.metadataPattern = metadataPattern;
    }

    static Validated<VersionComparator> build(String pattern, @Nullable String metadataPattern) {
        UnionRange range = parse(pattern, false);
        if (range == null) {
            return Validated.invalid("nodeRange", pattern, "not a node-semver range");
        }
        return Validated.valid("nodeRange", metadataPattern == null ? range :
                new UnionRange(range.raw, false, range.set, metadataPattern));
    }

    static @Nullable UnionRange parse(@Nullable String range, boolean includePrerelease) {
        if (range == null) {
            return null;
        }
        String key = (includePrerelease ? "1" : "0") + ' ' + range;
        UnionRange cached = CACHE.get(key);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        UnionRange parsed = doParse(range, includePrerelease);
        CACHE.put(key, parsed == null ? INVALID : parsed);
        return parsed;
    }

    private static @Nullable UnionRange doParse(String range, boolean includePrerelease) {
        List<List<NodeComparand>> set = new ArrayList<>();
        for (String group : range.trim().split("\\|\\|", -1)) {
            List<NodeComparand> comparands = parseGroup(group, includePrerelease);
            if (comparands == null) {
                return null;
            }
            set.add(comparands);
        }
        return new UnionRange(range, includePrerelease, set, null);
    }

    private static @Nullable List<NodeComparand> parseGroup(String groupStr, boolean incPre) {
        String g = trimOperators(HyphenRange.hyphenReplace(groupStr.trim(), incPre)).trim();
        String[] pieces = g.isEmpty() ? new String[]{""} : g.split("\\s+");
        List<NodeComparand> comparands = new ArrayList<>();
        for (String piece : pieces) {
            String s = CaretRange.replaceCaret(piece, incPre);
            s = mapTokens(s, incPre, TildeRange::replaceTilde);
            s = mapTokens(s, incPre, XRange::replaceXRange);
            s = mapTokens(s, incPre, (token, ignored) -> XRange.replaceStar(token));
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

    private interface TokenFn {
        String apply(String token, boolean incPre);
    }

    private static String mapTokens(String s, boolean incPre, TokenFn fn) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(fn.apply(part, incPre));
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
        if (version.hasPrerelease() && !includePrerelease) {
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

    /**
     * Whether {@code version} is a strict SemVer version admitted by this range (with prerelease
     * gating) and, when a metadata pattern is given, whose qualifier matches it. The npm-mode range
     * classes delegate their {@code isValid} here.
     */
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

    static boolean satisfies(String version, String range, boolean includePrerelease) {
        ParsedVersion v = ParsedVersion.parse(version);
        UnionRange r = parse(range, includePrerelease);
        return v.isStrictSemver() && r != null && r.test(v);
    }

    static @Nullable String maxSatisfying(Collection<String> versions, String range, boolean includePrerelease) {
        return bestSatisfying(versions, range, includePrerelease, true);
    }

    static @Nullable String minSatisfying(Collection<String> versions, String range, boolean includePrerelease) {
        return bestSatisfying(versions, range, includePrerelease, false);
    }

    private static @Nullable String bestSatisfying(Collection<String> versions, String range,
                                                   boolean includePrerelease, boolean max) {
        UnionRange r = parse(range, includePrerelease);
        if (r == null) {
            return null;
        }
        String best = null;
        ParsedVersion bestVersion = null;
        for (String candidate : versions) {
            ParsedVersion v = ParsedVersion.parse(candidate);
            if (!v.isStrictSemver() || !r.test(v)) {
                continue;
            }
            int c = bestVersion == null ? 0 : v.comparePrecedence(bestVersion);
            if (bestVersion == null || (max ? c > 0 : c < 0)) {
                best = candidate;
                bestVersion = v;
            }
        }
        return best;
    }

    /**
     * @return {@code true} if {@code version} is greater than every version {@code range} allows.
     */
    static boolean gtr(String version, String range) {
        return outside(version, range, true);
    }

    /**
     * @return {@code true} if {@code version} is less than every version {@code range} allows.
     */
    static boolean ltr(String version, String range) {
        return outside(version, range, false);
    }

    // Port of node-semver functions/outside.js (gtr = hilo '>', ltr = hilo '<').
    private static boolean outside(String version, String range, boolean gtr) {
        ParsedVersion v = ParsedVersion.parse(version);
        UnionRange r = parse(range, false);
        if (!v.isStrictSemver() || r == null || r.test(v)) {
            return false;
        }
        NodeComparand.Op comp = gtr ? NodeComparand.Op.GT : NodeComparand.Op.LT;
        NodeComparand.Op ecomp = gtr ? NodeComparand.Op.GTE : NodeComparand.Op.LTE;
        for (List<NodeComparand> comparands : r.set) {
            NodeComparand high = null;
            NodeComparand low = null;
            for (NodeComparand c0 : comparands) {
                NodeComparand c = c0.isAny() ? NodeComparand.GTE_ZERO : c0;
                if (high == null) {
                    high = c;
                    low = c;
                }
                int ch = c.getVersion().comparePrecedence(high.getVersion());
                if (gtr ? ch > 0 : ch < 0) {
                    high = c;
                } else {
                    int cl = c.getVersion().comparePrecedence(low.getVersion());
                    if (gtr ? cl < 0 : cl > 0) {
                        low = c;
                    }
                }
            }
            if (high.getOp() == comp || high.getOp() == ecomp) {
                return false;
            }
            int vLow = v.comparePrecedence(low.getVersion());
            boolean ltefn = gtr ? vLow <= 0 : vLow >= 0;
            boolean ltfn = gtr ? vLow < 0 : vLow > 0;
            if ((low.getOp() == NodeComparand.Op.EQ || low.getOp() == comp) && ltefn) {
                return false;
            }
            if (low.getOp() == ecomp && ltfn) {
                return false;
            }
        }
        return true;
    }
}
