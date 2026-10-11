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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * An npm range as node-semver's {@code classes/range.js} parses it: an OR of AND-groups of {@link NodeComparand}
 * clauses; a prerelease satisfies a group only through a clause on its own tuple, unless {@code includePrerelease}.
 */
class UnionRange implements VersionComparator {

    private static final int MAX_CACHE_SIZE = 4_096;
    private static final Map<String, UnionRange> CACHE = LruCache.bounded(MAX_CACHE_SIZE);
    private static final UnionRange INVALID = new UnionRange("", false, new ArrayList<>(), null);

    // node-semver re.js fragments used by Range.parseRange.
    private static final Pattern BUILD_STRIP = Pattern.compile("\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*");
    private static final Pattern COMPARATOR_TRIM = Pattern.compile(
            "(\\s*)((?:<|>)?=?)\\s*(" + ParsedVersion.LOOSE_PLAIN + "|" + XRange.XRANGE_PLAIN + ")");
    private static final Pattern TILDE_TRIM = Pattern.compile("(\\s*)(?:~>?)\\s+");
    private static final Pattern CARET_TRIM = Pattern.compile("(\\s*)(?:\\^)\\s+");
    private static final Pattern STAR = Pattern.compile("(<|>)?=?\\s*\\*");
    private static final Pattern DERIVED_UPPER_BOUND = Pattern.compile("(^|\\s)<([0-9]+\\.[0-9]+\\.[0-9]+)-0(?=\\s|$)");
    private static final Pattern GTE0 = Pattern.compile("^\\s*>=\\s*0\\.0\\.0\\s*$");
    private static final Pattern GTE0_PRE = Pattern.compile("^\\s*>=\\s*0\\.0\\.0-0\\s*$");

    private final String raw;
    private final boolean includePrerelease;
    private final boolean loose;
    private final List<List<NodeComparand>> set;

    private final @Nullable String metadataPattern;

    private UnionRange(String raw, boolean includePrerelease, List<List<NodeComparand>> set,
                       @Nullable String metadataPattern) {
        this(raw, includePrerelease, false, set, metadataPattern);
    }

    private UnionRange(String raw, boolean includePrerelease, boolean loose, List<List<NodeComparand>> set,
                       @Nullable String metadataPattern) {
        this.raw = raw;
        this.includePrerelease = includePrerelease;
        this.loose = loose;
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
        return parse(range, includePrerelease, false);
    }

    static @Nullable UnionRange parse(@Nullable String range, boolean includePrerelease, boolean loose) {
        return parse(range, includePrerelease, loose, NodeSemver.Release.V7_8_4);
    }

    static @Nullable UnionRange parse(@Nullable String range, boolean includePrerelease, boolean loose,
                                      NodeSemver.Release release) {
        if (range == null) {
            return null;
        }
        String key = (includePrerelease ? "1" : "0") + (loose ? "1" : "0") + release.ordinal() + ' ' + range;
        UnionRange cached = CACHE.get(key);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        UnionRange parsed = doParse(range, includePrerelease, loose, release == NodeSemver.Release.V5_5_0);
        CACHE.put(key, parsed == null ? INVALID : parsed);
        return parsed;
    }

    // Port of node-semver classes/range.js constructor; 5.5.0 keeps every group as parsed.
    private static @Nullable UnionRange doParse(String range, boolean includePrerelease, boolean loose,
                                                boolean v5) {
        String raw = range.trim().replaceAll("\\s+", " ");
        List<List<NodeComparand>> set = new ArrayList<>();
        for (String group : raw.split("\\|\\|", -1)) {
            List<NodeComparand> comparands = parseRange(group.trim(), includePrerelease, loose, v5);
            if (comparands == null) {
                return null;
            }
            if (!comparands.isEmpty()) {
                set.add(comparands);
            }
        }
        if (set.isEmpty()) {
            return null;
        }
        if (set.size() > 1 && !v5) {
            List<NodeComparand> first = set.get(0);
            List<List<NodeComparand>> notNull = new ArrayList<>();
            for (List<NodeComparand> c : set) {
                if (!c.get(0).isNullSet()) {
                    notNull.add(c);
                }
            }
            set = notNull;
            if (set.isEmpty()) {
                set.add(first);
            } else if (set.size() > 1) {
                for (List<NodeComparand> c : set) {
                    if (c.size() == 1 && c.get(0).isAny()) {
                        set = new ArrayList<>();
                        set.add(c);
                        break;
                    }
                }
            }
        }
        return new UnionRange(range, includePrerelease, loose, set, null);
    }

    // Port of node-semver classes/range.js parseRange; null when a comparator is invalid. 5.5.0 keeps duplicate,
    // `>=0.0.0` and empty comparators and writes no `-0` on the upper bounds it derives.
    private static @Nullable List<NodeComparand> parseRange(String range, boolean incPre, boolean loose, boolean v5) {
        List<String> rangeList = comparators(range, incPre, loose, v5);
        if (v5) {
            List<NodeComparand> comparands = new ArrayList<>();
            for (String comp : rangeList) {
                NodeComparand c = NodeComparand.parse(comp, loose);
                if (c == null) {
                    return null;
                }
                comparands.add(c);
            }
            return comparands;
        }
        Map<String, NodeComparand> rangeMap = new LinkedHashMap<>();
        for (String comp : rangeList) {
            NodeComparand c = NodeComparand.parse(comp, loose);
            if (c == null) {
                return null;
            }
            if (c.isNullSet()) {
                List<NodeComparand> nullSet = new ArrayList<>();
                nullSet.add(c);
                return nullSet;
            }
            rangeMap.put(c.value(), c);
        }
        if (rangeMap.size() > 1) {
            rangeMap.remove("");
        }
        return new ArrayList<>(rangeMap.values());
    }

    /**
     * The TypeError node-semver 7.x's Range constructor throws for a range {@link #parse} rejects.
     */
    static String invalidRangeMessage(String range, boolean includePrerelease, boolean loose) {
        String raw = range.trim().replaceAll("\\s+", " ");
        for (String group : raw.split("\\|\\|", -1)) {
            for (String comp : comparators(group.trim(), includePrerelease, loose, false)) {
                if (NodeComparand.parse(comp, loose) == null) {
                    return "Invalid comparator: " + comp;
                }
            }
        }
        return "Invalid SemVer Range: " + raw;
    }

    // the comparator strings parseRange makes of one group, before it parses them
    private static List<String> comparators(String range, boolean incPre, boolean loose, boolean v5) {
        range = BUILD_STRIP.matcher(range).replaceAll("");
        range = v5 ? withoutDerivedPrerelease(range, HyphenRange.hyphenReplace(range, incPre, loose)) :
                HyphenRange.hyphenReplace(range, incPre, loose);
        range = COMPARATOR_TRIM.matcher(range).replaceAll("$1$2$3");
        range = TILDE_TRIM.matcher(range).replaceAll("$1~");
        range = CARET_TRIM.matcher(range).replaceAll("$1^");

        String[] comps = range.split(" ", -1);
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < comps.length; i++) {
            if (i > 0) {
                joined.append(' ');
            }
            joined.append(parseComparator(comps[i], incPre, loose, v5));
        }
        List<String> rangeList = new ArrayList<>();
        for (String comp : joined.toString().split("\\s+", -1)) {
            rangeList.add(v5 ? comp.trim() : (incPre ? GTE0_PRE : GTE0).matcher(comp.trim()).replaceFirst(""));
        }
        if (loose) {
            rangeList.removeIf(comp -> !NodeComparand.COMPARATOR_LOOSE.matcher(comp).find());
        }
        return rangeList;
    }

    private static String parseComparator(String comp, boolean incPre, boolean loose, boolean v5) {
        comp = BUILD_STRIP.matcher(comp).replaceFirst("");
        comp = mapTokens(comp.trim(), token -> derived(token, CaretRange.replaceCaret(token, incPre, loose), v5));
        comp = mapTokens(comp.trim(), token -> derived(token, TildeRange.replaceTilde(token, incPre, loose), v5));
        comp = mapTokens(comp, token -> derived(token, XRange.replaceXRange(token, incPre, loose), v5));
        return STAR.matcher(comp.trim()).replaceFirst("");
    }

    private static String derived(String token, String replaced, boolean v5) {
        return v5 ? withoutDerivedPrerelease(token, replaced) : replaced;
    }

    // node-semver 5.5.0 derives `<2.0.0` where 7.x derives `<2.0.0-0`
    private static String withoutDerivedPrerelease(String original, String replaced) {
        return replaced.equals(original) ? replaced : DERIVED_UPPER_BOUND.matcher(replaced).replaceAll("$1<$2");
    }

    private static String mapTokens(String s, UnaryOperator<String> fn) {
        StringBuilder sb = new StringBuilder();
        String[] parts = s.split("\\s+", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(fn.apply(parts[i]));
        }
        return sb.toString();
    }

    // Port of node-semver classes/range.js intersects.
    boolean intersects(UnionRange range) {
        for (List<NodeComparand> thisComparators : set) {
            if (!isSatisfiable(thisComparators)) {
                continue;
            }
            for (List<NodeComparand> rangeComparators : range.set) {
                if (isSatisfiable(rangeComparators) && allIntersect(thisComparators, rangeComparators)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean allIntersect(List<NodeComparand> thisComparators, List<NodeComparand> rangeComparators) {
        for (NodeComparand thisComparator : thisComparators) {
            for (NodeComparand rangeComparator : rangeComparators) {
                if (!thisComparator.intersects(rangeComparator, loose, includePrerelease)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSatisfiable(List<NodeComparand> comparators) {
        List<NodeComparand> remaining = new ArrayList<>(comparators);
        NodeComparand testComparator = remaining.remove(remaining.size() - 1);
        boolean result = true;
        while (result && !remaining.isEmpty()) {
            for (NodeComparand other : remaining) {
                if (!testComparator.intersects(other, loose, includePrerelease)) {
                    result = false;
                    break;
                }
            }
            testComparator = remaining.remove(remaining.size() - 1);
        }
        return result;
    }

    // node-semver Range.test(string): the version parses under this range's looseness.
    boolean test(String version, boolean loose) {
        ParsedVersion v = ParsedVersion.parseNode(version, loose);
        return v.isStrictSemver() && test(v);
    }

    List<List<NodeComparand>> sets() {
        return set;
    }

    // node-semver Range.range.
    String range() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < set.size(); i++) {
            if (i > 0) {
                sb.append("||");
            }
            List<NodeComparand> comps = set.get(i);
            StringBuilder group = new StringBuilder();
            for (int k = 0; k < comps.size(); k++) {
                if (k > 0) {
                    group.append(' ');
                }
                group.append(comps.get(k).value().trim());
            }
            sb.append(group.toString().trim());
        }
        return sb.toString();
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
        return range();
    }

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
