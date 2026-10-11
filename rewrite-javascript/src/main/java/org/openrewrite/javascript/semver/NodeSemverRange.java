/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.semver;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver.Options;
import org.openrewrite.javascript.semver.NodeSemver.Release;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of node-semver classes/range.js: an OR of AND-groups of {@link NodeSemverComparator} clauses; a prerelease
 * satisfies a group only through a clause on its own tuple, unless {@code includePrerelease}.
 */
final class NodeSemverRange {

    private static final String XRANGE_ID = NodeSemverVersion.NUMERIC_ID + "|x|X|\\*";

    // node-semver XRANGEPLAIN; groups: major, minor, patch, prerelease.
    private static final String XRANGE_PLAIN =
            "[v=\\s]*(" + XRANGE_ID + ")(?:\\.(" + XRANGE_ID + ")(?:\\.(" + XRANGE_ID + ")" +
                    NodeSemverVersion.PRERELEASE_GROUP + "?" + NodeSemverVersion.BUILD + "?)?)?";

    private static final String XRANGE_ID_LOOSE = "\\d+|x|X|\\*";

    // node-semver XRANGEPLAINLOOSE.
    private static final String XRANGE_PLAIN_LOOSE =
            "[v=\\s]*(" + XRANGE_ID_LOOSE + ")(?:\\.(" + XRANGE_ID_LOOSE + ")(?:\\.(" + XRANGE_ID_LOOSE + ")" +
                    NodeSemverVersion.PRERELEASE_GROUP_LOOSE + "?" + NodeSemverVersion.BUILD + "?)?)?";

    // node-semver re.js fragments used by Range.parseRange.
    private static final Pattern BUILD_STRIP = Pattern.compile("\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*");
    private static final Pattern COMPARATOR_TRIM = Pattern.compile(
            "(\\s*)((?:<|>)?=?)\\s*(" + NodeSemverVersion.LOOSE_PLAIN + "|" + XRANGE_PLAIN + ")");
    private static final Pattern TILDE_TRIM = Pattern.compile("(\\s*)(?:~>?)\\s+");
    private static final Pattern CARET_TRIM = Pattern.compile("(\\s*)(?:\\^)\\s+");
    private static final Pattern STAR = Pattern.compile("(<|>)?=?\\s*\\*");
    private static final Pattern DERIVED_UPPER_BOUND = Pattern.compile("(^|\\s)<([0-9]+\\.[0-9]+\\.[0-9]+)-0(?=\\s|$)");
    private static final Pattern GTE0 = Pattern.compile("^\\s*>=\\s*0\\.0\\.0\\s*$");
    private static final Pattern GTE0_PRE = Pattern.compile("^\\s*>=\\s*0\\.0\\.0-0\\s*$");

    // The caret, tilde and x-range grammars; groups: (operator,) major, minor, patch, prerelease.
    private static final Pattern CARET = Pattern.compile("^(?:\\^)" + XRANGE_PLAIN + "$");
    private static final Pattern CARET_LOOSE = Pattern.compile("^(?:\\^)" + XRANGE_PLAIN_LOOSE + "$");
    private static final Pattern TILDE = Pattern.compile("^(?:~>?)" + XRANGE_PLAIN + "$");
    private static final Pattern TILDE_LOOSE = Pattern.compile("^(?:~>?)" + XRANGE_PLAIN_LOOSE + "$");
    private static final Pattern X_RANGE = Pattern.compile("^((?:<|>)?=?)\\s*" + XRANGE_PLAIN + "$");
    private static final Pattern X_RANGE_LOOSE = Pattern.compile("^((?:<|>)?=?)\\s*" + XRANGE_PLAIN_LOOSE + "$");

    // The hyphen grammar; groups 1-5 = "from" bound and its major/minor/patch/prerelease, 6-10 = "to".
    private static final Pattern HYPHEN = Pattern.compile(
            "^\\s*(" + XRANGE_PLAIN + ")\\s+-\\s+(" + XRANGE_PLAIN + ")\\s*$");
    private static final Pattern HYPHEN_LOOSE = Pattern.compile(
            "^\\s*(" + XRANGE_PLAIN_LOOSE + ")\\s+-\\s+(" + XRANGE_PLAIN_LOOSE + ")\\s*$");

    private static final Map<String, NodeSemverRange> CACHE = NodeSemverVersion.lru(4_096);
    private static final NodeSemverRange INVALID = new NodeSemverRange(Options.DEFAULT, new ArrayList<>());

    private final Options options;
    private final List<List<NodeSemverComparator>> set;

    private NodeSemverRange(Options options, List<List<NodeSemverComparator>> set) {
        this.options = options;
        this.set = set;
    }

    static @Nullable NodeSemverRange parse(@Nullable String range, Options options) {
        if (range == null) {
            return null;
        }
        String key = (options.includePrerelease ? "1" : "0") + (options.loose ? "1" : "0") +
                options.release.ordinal() + ' ' + range;
        NodeSemverRange cached = CACHE.get(key);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        NodeSemverRange parsed = doParse(range, options);
        CACHE.put(key, parsed == null ? INVALID : parsed);
        return parsed;
    }

    // Port of node-semver classes/range.js constructor; 5.5.0 keeps every group as parsed.
    private static @Nullable NodeSemverRange doParse(String range, Options options) {
        String raw = range.trim().replaceAll("\\s+", " ");
        List<List<NodeSemverComparator>> set = new ArrayList<>();
        for (String group : raw.split("\\|\\|", -1)) {
            List<NodeSemverComparator> comparators = parseRange(group.trim(), options);
            if (comparators == null) {
                return null;
            }
            if (!comparators.isEmpty()) {
                set.add(comparators);
            }
        }
        if (set.isEmpty()) {
            return null;
        }
        if (set.size() > 1 && !v5(options)) {
            List<NodeSemverComparator> first = set.get(0);
            List<List<NodeSemverComparator>> notNull = new ArrayList<>();
            for (List<NodeSemverComparator> c : set) {
                if (!c.get(0).isNullSet()) {
                    notNull.add(c);
                }
            }
            set = notNull;
            if (set.isEmpty()) {
                set.add(first);
            } else if (set.size() > 1) {
                for (List<NodeSemverComparator> c : set) {
                    if (c.size() == 1 && c.get(0).isAny()) {
                        set = new ArrayList<>();
                        set.add(c);
                        break;
                    }
                }
            }
        }
        return new NodeSemverRange(options, set);
    }

    // Port of node-semver classes/range.js parseRange; null when a comparator is invalid. 5.5.0 keeps duplicate,
    // `>=0.0.0` and empty comparators and writes no `-0` on the upper bounds it derives.
    private static @Nullable List<NodeSemverComparator> parseRange(String range, Options options) {
        List<String> rangeList = comparators(range, options);
        if (v5(options)) {
            List<NodeSemverComparator> comparators = new ArrayList<>();
            for (String comp : rangeList) {
                NodeSemverComparator c = NodeSemverComparator.parse(comp, options);
                if (c == null) {
                    return null;
                }
                comparators.add(c);
            }
            return comparators;
        }
        Map<String, NodeSemverComparator> rangeMap = new LinkedHashMap<>();
        for (String comp : rangeList) {
            NodeSemverComparator c = NodeSemverComparator.parse(comp, options);
            if (c == null) {
                return null;
            }
            if (c.isNullSet()) {
                List<NodeSemverComparator> nullSet = new ArrayList<>();
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
     * The TypeError node-semver's Range constructor throws for a range {@link #parse} rejects.
     */
    static String invalidRangeMessage(String range, Options options) {
        String raw = range.trim().replaceAll("\\s+", " ");
        for (String group : raw.split("\\|\\|", -1)) {
            for (String comp : comparators(group.trim(), options)) {
                if (NodeSemverComparator.parse(comp, options) == null) {
                    return "Invalid comparator: " + comp;
                }
            }
        }
        return "Invalid SemVer Range: " + raw;
    }

    private static boolean v5(Options options) {
        return options.release == Release.V5_5_0;
    }

    // the comparator strings parseRange makes of one group, before it parses them
    private static List<String> comparators(String range, Options options) {
        range = BUILD_STRIP.matcher(range).replaceAll("");
        range = derived(range, hyphenReplace(range, options), options);
        range = COMPARATOR_TRIM.matcher(range).replaceAll("$1$2$3");
        range = TILDE_TRIM.matcher(range).replaceAll("$1~");
        range = CARET_TRIM.matcher(range).replaceAll("$1^");

        String[] comps = range.split(" ", -1);
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < comps.length; i++) {
            if (i > 0) {
                joined.append(' ');
            }
            joined.append(parseComparator(comps[i], options));
        }
        List<String> rangeList = new ArrayList<>();
        for (String comp : joined.toString().split("\\s+", -1)) {
            rangeList.add(v5(options) ? comp.trim() :
                    (options.includePrerelease ? GTE0_PRE : GTE0).matcher(comp.trim()).replaceFirst(""));
        }
        if (options.loose) {
            rangeList.removeIf(comp -> !NodeSemverComparator.COMPARATOR_LOOSE.matcher(comp).find());
        }
        return rangeList;
    }

    private static String parseComparator(String comp, Options options) {
        comp = BUILD_STRIP.matcher(comp).replaceFirst("");
        comp = mapTokens(comp.trim(), token -> derived(token, replaceCaret(token, options), options));
        comp = mapTokens(comp.trim(), token -> derived(token, replaceTilde(token, options), options));
        comp = mapTokens(comp, token -> derived(token, replaceXRange(token, options), options));
        return STAR.matcher(comp.trim()).replaceFirst("");
    }

    // node-semver 5.5.0 derives `<2.0.0` where 7.x derives `<2.0.0-0`
    private static String derived(String original, String replaced, Options options) {
        return !v5(options) || replaced.equals(original) ? replaced :
                DERIVED_UPPER_BOUND.matcher(replaced).replaceAll("$1<$2");
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

    // Port of node-semver range.js replaceCaret; non-caret tokens pass through.
    private static String replaceCaret(String token, Options options) {
        Matcher m = (options.loose ? CARET_LOOSE : CARET).matcher(token);
        if (!m.matches()) {
            return token;
        }
        String mj = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        String z = options.includePrerelease ? "-0" : "";
        if (isX(mj)) {
            return "";
        }
        if (isX(mn)) {
            return ">=" + mj + ".0.0" + z + " <" + incr(mj) + ".0.0-0";
        }
        if (isX(p)) {
            return "0".equals(mj) ?
                    ">=" + mj + "." + mn + ".0" + z + " <" + mj + "." + incr(mn) + ".0-0" :
                    ">=" + mj + "." + mn + ".0" + z + " <" + incr(mj) + ".0.0-0";
        }
        if (pr != null) {
            if ("0".equals(mj)) {
                return "0".equals(mn) ?
                        ">=" + mj + "." + mn + "." + p + "-" + pr + " <" + mj + "." + mn + "." + incr(p) + "-0" :
                        ">=" + mj + "." + mn + "." + p + "-" + pr + " <" + mj + "." + incr(mn) + ".0-0";
            }
            return ">=" + mj + "." + mn + "." + p + "-" + pr + " <" + incr(mj) + ".0.0-0";
        }
        if ("0".equals(mj)) {
            return "0".equals(mn) ?
                    ">=" + mj + "." + mn + "." + p + " <" + mj + "." + mn + "." + incr(p) + "-0" :
                    ">=" + mj + "." + mn + "." + p + " <" + mj + "." + incr(mn) + ".0-0";
        }
        return ">=" + mj + "." + mn + "." + p + " <" + incr(mj) + ".0.0-0";
    }

    // Port of node-semver range.js replaceTilde; non-tilde tokens pass through.
    private static String replaceTilde(String token, Options options) {
        Matcher m = (options.loose ? TILDE_LOOSE : TILDE).matcher(token);
        if (!m.matches()) {
            return token;
        }
        String mj = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        if (isX(mj)) {
            return "";
        }
        if (isX(mn)) {
            return ">=" + mj + ".0.0 <" + incr(mj) + ".0.0-0";
        }
        if (isX(p)) {
            return ">=" + mj + "." + mn + ".0 <" + mj + "." + incr(mn) + ".0-0";
        }
        if (pr != null) {
            return ">=" + mj + "." + mn + "." + p + "-" + pr + " <" + mj + "." + incr(mn) + ".0-0";
        }
        return ">=" + mj + "." + mn + "." + p + " <" + mj + "." + incr(mn) + ".0-0";
    }

    // Port of node-semver range.js replaceXRange; tokens without wildcard or partial components pass through.
    private static String replaceXRange(String token, Options options) {
        String comp = token.trim();
        Matcher m = (options.loose ? X_RANGE_LOOSE : X_RANGE).matcher(comp);
        if (!m.matches()) {
            return comp;
        }
        String gtlt = m.group(1);
        String mj = m.group(2), mn = m.group(3), p = m.group(4);
        if ((isX(mj) && !isX(mn)) || (isX(mn) && p != null && !isX(p))) {
            return comp;
        }
        boolean xM = isX(mj);
        boolean xm = xM || isX(mn);
        boolean xp = xm || isX(p);
        boolean anyX = xp;

        if ("=".equals(gtlt) && anyX) {
            gtlt = "";
        }
        String pr = options.includePrerelease ? "-0" : "";

        if (xM) {
            return (">".equals(gtlt) || "<".equals(gtlt)) ? "<0.0.0-0" : "*";
        }
        if (!gtlt.isEmpty() && anyX) {
            if (xm) {
                mn = "0";
            }
            p = "0";
            if (">".equals(gtlt)) {
                gtlt = ">=";
                if (xm) {
                    mj = incr(mj);
                    mn = "0";
                } else {
                    mn = incr(mn);
                }
                p = "0";
            } else if ("<=".equals(gtlt)) {
                gtlt = "<";
                if (xm) {
                    mj = incr(mj);
                } else {
                    mn = incr(mn);
                }
            }
            if ("<".equals(gtlt)) {
                pr = "-0";
            }
            return gtlt + mj + "." + mn + "." + p + pr;
        }
        if (xm) {
            return ">=" + mj + ".0.0" + pr + " <" + incr(mj) + ".0.0-0";
        }
        if (xp) {
            return ">=" + mj + "." + mn + ".0" + pr + " <" + mj + "." + incr(mn) + ".0-0";
        }
        return comp;
    }

    // Port of node-semver range.js hyphenReplace; non-hyphen groups pass through.
    private static String hyphenReplace(String group, Options options) {
        Matcher m = (options.loose ? HYPHEN_LOOSE : HYPHEN).matcher(group);
        if (!m.matches()) {
            return group;
        }
        String from = hyphenFrom(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), options);
        String to = hyphenTo(m.group(6), m.group(7), m.group(8), m.group(9), m.group(10), options);
        return (from + " " + to).trim();
    }

    private static String hyphenFrom(String from, String fM, String fm, String fp, @Nullable String fpr,
                                     Options options) {
        String z = options.includePrerelease ? "-0" : "";
        if (isX(fM)) {
            return "";
        }
        if (isX(fm)) {
            return ">=" + fM + ".0.0" + z;
        }
        if (isX(fp)) {
            return ">=" + fM + "." + fm + ".0" + z;
        }
        if (fpr != null) {
            return ">=" + from;
        }
        return ">=" + from + z;
    }

    private static String hyphenTo(String to, String tM, String tm, String tp, @Nullable String tpr,
                                   Options options) {
        if (isX(tM)) {
            return "";
        }
        if (isX(tm)) {
            return "<" + incr(tM) + ".0.0-0";
        }
        if (isX(tp)) {
            return "<" + tM + "." + incr(tm) + ".0-0";
        }
        if (tpr != null) {
            return "<=" + tM + "." + tm + "." + tp + "-" + tpr;
        }
        if (options.includePrerelease) {
            return "<" + tM + "." + tm + "." + incr(tp) + "-0";
        }
        return "<=" + to;
    }

    // npm's wildcard test: x, X, * or an absent component.
    private static boolean isX(@Nullable String id) {
        return id == null || "x".equalsIgnoreCase(id) || "*".equals(id);
    }

    // num + 1; a result beyond long range fails strict parsing later, invalidating the range as node does.
    private static String incr(String num) {
        return new BigInteger(num).add(BigInteger.ONE).toString();
    }

    // Port of node-semver classes/range.js intersects.
    boolean intersects(NodeSemverRange range) {
        for (List<NodeSemverComparator> thisComparators : set) {
            if (!isSatisfiable(thisComparators)) {
                continue;
            }
            for (List<NodeSemverComparator> rangeComparators : range.set) {
                if (isSatisfiable(rangeComparators) && allIntersect(thisComparators, rangeComparators)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean allIntersect(List<NodeSemverComparator> thisComparators,
                                 List<NodeSemverComparator> rangeComparators) {
        for (NodeSemverComparator thisComparator : thisComparators) {
            for (NodeSemverComparator rangeComparator : rangeComparators) {
                if (!thisComparator.intersects(rangeComparator, options)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSatisfiable(List<NodeSemverComparator> comparators) {
        List<NodeSemverComparator> remaining = new ArrayList<>(comparators);
        NodeSemverComparator testComparator = remaining.remove(remaining.size() - 1);
        boolean result = true;
        while (result && !remaining.isEmpty()) {
            for (NodeSemverComparator other : remaining) {
                if (!testComparator.intersects(other, options)) {
                    result = false;
                    break;
                }
            }
            testComparator = remaining.remove(remaining.size() - 1);
        }
        return result;
    }

    // node-semver Range.test(string): the version parses under this range's options.
    boolean test(String version) {
        NodeSemverVersion v = NodeSemverVersion.parse(version, options);
        return v.isStrictSemver() && test(v);
    }

    List<List<NodeSemverComparator>> sets() {
        return set;
    }

    // node-semver Range.range.
    String range() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < set.size(); i++) {
            if (i > 0) {
                sb.append("||");
            }
            List<NodeSemverComparator> comps = set.get(i);
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

    boolean test(NodeSemverVersion version) {
        for (List<NodeSemverComparator> group : set) {
            if (testGroup(group, version)) {
                return true;
            }
        }
        return false;
    }

    private boolean testGroup(List<NodeSemverComparator> group, NodeSemverVersion version) {
        for (NodeSemverComparator c : group) {
            if (!c.test(version)) {
                return false;
            }
        }
        if (version.hasPrerelease() && !options.includePrerelease) {
            // A prerelease only satisfies the group if some comparator names the same
            // major.minor.patch tuple and itself carries a prerelease.
            for (NodeSemverComparator c : group) {
                if (c.isAny()) {
                    continue;
                }
                NodeSemverVersion s = c.getVersion();
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

    @Override
    public String toString() {
        return range();
    }
}
