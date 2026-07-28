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

/**
 * An npm/node-semver range: a top-level OR ({@code ||}) of AND-groups, each a list of
 * {@link NodeComparator} primitives. The parse pipeline is a direct port of node-semver's
 * {@code classes/range.js} -- hyphen, caret ({@code ^}), tilde ({@code ~}), x-range ({@code x}/{@code *})
 * and star desugaring -- including npm's {@code -0} upper-bound convention (e.g. {@code ^1.2.3} ->
 * {@code >=1.2.3 <2.0.0-0}), which excludes {@code 2.0.0-alpha}.
 * <p>
 * {@link #test} applies node's prerelease-inclusion rule: a prerelease candidate satisfies an
 * AND-group only if some comparator in that group names the same {@code [major,minor,patch]} tuple
 * <em>and</em> itself carries a prerelease (unless {@code includePrerelease} is set).
 */
public final class NodeRange {

    private static final String XID = "0|[1-9]\\d*|x|X|\\*";
    private static final String PRE_ID = "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)";
    private static final String PRE = "(?:-(" + PRE_ID + "(?:\\." + PRE_ID + ")*))";
    private static final String BUILD = "(?:\\+[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*)";
    // node-semver XRANGEPLAIN: major, optional minor, optional patch, optional prerelease/build.
    private static final String XPLAIN =
            "[v=\\s]*(" + XID + ")(?:\\.(" + XID + ")(?:\\.(" + XID + ")" + PRE + "?" + BUILD + "?)?)?";

    private static final Pattern CARET = Pattern.compile("^(?:\\^)" + XPLAIN + "$");
    private static final Pattern TILDE = Pattern.compile("^(?:~>?)" + XPLAIN + "$");
    private static final Pattern XRANGE = Pattern.compile("^((?:<|>)?=?)\\s*" + XPLAIN + "$");
    private static final Pattern HYPHEN = Pattern.compile("^\\s*(" + XPLAIN + ")\\s+-\\s+(" + XPLAIN + ")\\s*$");
    private static final Pattern STAR = Pattern.compile("^(?:<|>)?=?\\s*\\*$");

    private static final int MAX_CACHE_SIZE = 4_096;
    private static final Map<String, NodeRange> CACHE = LruCache.bounded(MAX_CACHE_SIZE);
    private static final NodeRange INVALID = new NodeRange("", false, new ArrayList<>());

    private final String raw;
    private final boolean includePrerelease;
    private final List<List<NodeComparator>> set;

    private NodeRange(String raw, boolean includePrerelease, List<List<NodeComparator>> set) {
        this.raw = raw;
        this.includePrerelease = includePrerelease;
        this.set = set;
    }

    public static @Nullable NodeRange parse(@Nullable String range) {
        return parse(range, false);
    }

    public static @Nullable NodeRange parse(@Nullable String range, boolean includePrerelease) {
        if (range == null) {
            return null;
        }
        String key = (includePrerelease ? "1" : "0") + ' ' + range;
        NodeRange cached = CACHE.get(key);
        if (cached != null) {
            return cached == INVALID ? null : cached;
        }
        NodeRange parsed = doParse(range, includePrerelease);
        CACHE.put(key, parsed == null ? INVALID : parsed);
        return parsed;
    }

    private static @Nullable NodeRange doParse(String range, boolean includePrerelease) {
        List<List<NodeComparator>> set = new ArrayList<>();
        for (String group : range.trim().split("\\|\\|", -1)) {
            List<NodeComparator> comparators = parseGroup(group, includePrerelease);
            if (comparators == null) {
                return null;
            }
            set.add(comparators);
        }
        return new NodeRange(range, includePrerelease, set);
    }

    private static @Nullable List<NodeComparator> parseGroup(String groupStr, boolean incPre) {
        String g = trimOperators(hyphenReplace(groupStr.trim(), incPre)).trim();
        String[] pieces = g.isEmpty() ? new String[]{""} : g.split("\\s+");
        List<NodeComparator> comparators = new ArrayList<>();
        for (String piece : pieces) {
            String s = replaceCaret(piece, incPre);
            s = mapTokens(s, incPre, NodeRange::replaceTilde);
            s = mapTokens(s, incPre, NodeRange::replaceXRange);
            s = mapStars(s);
            String st = s.trim();
            if (st.isEmpty()) {
                NodeComparator c = NodeComparator.parse("");
                comparators.add(c);
            } else {
                for (String tok : st.split("\\s+")) {
                    NodeComparator c = NodeComparator.parse(tok);
                    if (c == null) {
                        return null;
                    }
                    comparators.add(c);
                }
            }
        }
        return comparators;
    }

    public boolean test(NodeVersion version) {
        for (List<NodeComparator> group : set) {
            if (testGroup(group, version)) {
                return true;
            }
        }
        return false;
    }

    private boolean testGroup(List<NodeComparator> group, NodeVersion version) {
        for (NodeComparator c : group) {
            if (!c.test(version)) {
                return false;
            }
        }
        if (version.isPrerelease() && !includePrerelease) {
            // A prerelease only satisfies the group if some comparator names the same
            // major.minor.patch tuple and itself carries a prerelease.
            for (NodeComparator c : group) {
                if (c.isAny()) {
                    continue;
                }
                NodeVersion s = c.getSemver();
                if (s.isPrerelease() &&
                        s.getMajor() == version.getMajor() &&
                        s.getMinor() == version.getMinor() &&
                        s.getPatch() == version.getPatch()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    List<List<NodeComparator>> getSet() {
        return set;
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < set.size(); i++) {
            if (i > 0) {
                sb.append("||");
            }
            StringBuilder group = new StringBuilder();
            for (NodeComparator c : set.get(i)) {
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

    // --- desugaring (port of node-semver range.js) ---

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

    private static String mapStars(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(STAR.matcher(part).matches() ? "" : part);
        }
        return sb.toString();
    }

    private static String trimOperators(String s) {
        String out = s.replaceAll("(~>?|\\^)\\s+", "$1");
        return out.replaceAll("(<=?|>=?|=)\\s+", "$1");
    }

    private static boolean isX(@Nullable String id) {
        return id == null || "x".equalsIgnoreCase(id) || "*".equals(id);
    }

    private static String incr(String num) {
        return Integer.toString(Integer.parseInt(num) + 1);
    }

    private static String replaceCaret(String token, boolean incPre) {
        Matcher m = CARET.matcher(token);
        if (!m.matches()) {
            return token;
        }
        String mj = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        String z = incPre ? "-0" : "";
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
                    ">=" + mj + "." + mn + "." + p + z + " <" + mj + "." + mn + "." + incr(p) + "-0" :
                    ">=" + mj + "." + mn + "." + p + z + " <" + mj + "." + incr(mn) + ".0-0";
        }
        return ">=" + mj + "." + mn + "." + p + z + " <" + incr(mj) + ".0.0-0";
    }

    private static String replaceTilde(String token, boolean incPre) {
        Matcher m = TILDE.matcher(token);
        if (!m.matches()) {
            return token;
        }
        String mj = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        String z = incPre ? "-0" : "";
        if (isX(mj)) {
            return "";
        }
        if (isX(mn)) {
            return ">=" + mj + ".0.0" + z + " <" + incr(mj) + ".0.0-0";
        }
        if (isX(p)) {
            return ">=" + mj + "." + mn + ".0" + z + " <" + mj + "." + incr(mn) + ".0-0";
        }
        if (pr != null) {
            return ">=" + mj + "." + mn + "." + p + "-" + pr + " <" + mj + "." + incr(mn) + ".0-0";
        }
        return ">=" + mj + "." + mn + "." + p + z + " <" + mj + "." + incr(mn) + ".0-0";
    }

    private static String replaceXRange(String token, boolean incPre) {
        Matcher m = XRANGE.matcher(token.trim());
        if (!m.matches()) {
            return token;
        }
        String gtlt = m.group(1);
        String mj = m.group(2), mn = m.group(3), p = m.group(4);
        boolean xM = isX(mj);
        boolean xm = xM || isX(mn);
        boolean xp = xm || isX(p);
        boolean anyX = xp;

        if ("=".equals(gtlt) && anyX) {
            gtlt = "";
        }
        String pr = incPre ? "-0" : "";

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
        return token;
    }

    private static String hyphenReplace(String group, boolean incPre) {
        Matcher m = HYPHEN.matcher(group);
        if (!m.matches()) {
            return group;
        }
        String from = hyphenFrom(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), incPre);
        String to = hyphenTo(m.group(6), m.group(7), m.group(8), m.group(9), m.group(10), incPre);
        return (from + " " + to).trim();
    }

    private static String hyphenFrom(String from, String fM, String fm, String fp, @Nullable String fpr, boolean incPre) {
        if (isX(fM)) {
            return "";
        }
        if (isX(fm)) {
            return ">=" + fM + ".0.0" + (incPre ? "-0" : "");
        }
        if (isX(fp)) {
            return ">=" + fM + "." + fm + ".0" + (incPre ? "-0" : "");
        }
        if (fpr != null) {
            return ">=" + from;
        }
        return ">=" + from + (incPre ? "-0" : "");
    }

    private static String hyphenTo(String to, String tM, String tm, String tp, @Nullable String tpr, boolean incPre) {
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
        if (incPre) {
            return "<" + tM + "." + tm + "." + incr(tp) + "-0";
        }
        return "<=" + to;
    }
}
