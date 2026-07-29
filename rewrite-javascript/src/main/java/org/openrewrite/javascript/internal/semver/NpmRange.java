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
package org.openrewrite.javascript.internal.semver;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A node-semver range: {@code ||}-separated comparator sets with {@code ^}, {@code ~},
 * x-ranges, hyphen ranges, and the prerelease-exclusion rule. The desugaring pipeline
 * (hyphen → trims → caret → tilde → x-range → star → GTE0) is ported line-for-line from
 * npm/node-semver {@code classes/range.js} in strict (non-loose) mode, and validated
 * against node-semver's range-include/range-exclude fixture corpus.
 */
public final class NpmRange {

    private static final String XRANGE_ID = "[0-9]+|x|X|\\*";
    private static final String XRANGE_PLAIN = "[v=\\s]*(" + XRANGE_ID + ")" +
            "(?:\\.(" + XRANGE_ID + ")" +
            "(?:\\.(" + XRANGE_ID + ")" +
            "(?:" + NpmVersion.PRERELEASE + ")?" +
            NpmVersion.BUILD + "?" +
            ")?)?";
    private static final String GTLT = "((?:<|>)?=?)";

    private static final Pattern HYPHEN_RANGE = Pattern.compile(
            "^\\s*(" + XRANGE_PLAIN + ")\\s+-\\s+(" + XRANGE_PLAIN + ")\\s*$");
    private static final Pattern COMPARATOR_TRIM = Pattern.compile(
            "(\\s*)" + GTLT + "\\s*(" + XRANGE_PLAIN + ")");
    private static final Pattern TILDE_TRIM = Pattern.compile("(\\s*)~>?\\s+");
    private static final Pattern CARET_TRIM = Pattern.compile("(\\s*)\\^\\s+");
    private static final Pattern TILDE = Pattern.compile("^(?:~>?)" + XRANGE_PLAIN + "$");
    private static final Pattern CARET = Pattern.compile("^(?:\\^)" + XRANGE_PLAIN + "$");
    private static final Pattern XRANGE = Pattern.compile("^" + GTLT + "\\s*" + XRANGE_PLAIN + "$");
    private static final Pattern STAR = Pattern.compile("(<|>)?=?\\s*\\*");
    private static final Pattern GTE0 = Pattern.compile("^\\s*>=\\s*0\\.0\\.0\\s*$");
    private static final Pattern BUILD_STRIP = Pattern.compile(NpmVersion.BUILD);

    private final List<List<NpmComparator>> comparatorSets;
    private final String raw;

    private NpmRange(List<List<NpmComparator>> comparatorSets, String raw) {
        this.comparatorSets = comparatorSets;
        this.raw = raw;
    }

    /** Parse a range, returning {@code null} when it is not a valid strict-mode semver range. */
    public static @Nullable NpmRange parse(@Nullable String range) {
        if (range == null) {
            return null;
        }
        List<List<NpmComparator>> sets = new ArrayList<>();
        for (String set : range.trim().split("\\|\\|", -1)) {
            List<NpmComparator> comparators = parseComparatorSet(set.trim());
            if (comparators == null) {
                return null;
            }
            sets.add(comparators);
        }
        return sets.isEmpty() ? null : new NpmRange(sets, range.trim());
    }

    private static @Nullable List<NpmComparator> parseComparatorSet(String set) {
        String range = BUILD_STRIP.matcher(set).replaceAll("");
        Matcher hyphen = HYPHEN_RANGE.matcher(range);
        if (hyphen.matches()) {
            range = hyphenReplace(hyphen);
        }
        range = replaceAll(COMPARATOR_TRIM, range, m -> m.group(1) + m.group(2) + m.group(3));
        range = TILDE_TRIM.matcher(range).replaceAll("$1~");
        range = CARET_TRIM.matcher(range).replaceAll("$1^");

        StringBuilder joined = new StringBuilder();
        for (String comp : range.trim().split("\\s+", -1)) {
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(parseComparator(comp));
        }
        List<NpmComparator> out = new ArrayList<>();
        for (String comp : joined.toString().trim().split("\\s+", -1)) {
            comp = GTE0.matcher(comp).replaceAll("");
            if (comp.isEmpty()) {
                continue;
            }
            NpmComparator parsed = NpmComparator.parse(comp);
            if (parsed == null) {
                return null;
            }
            out.add(parsed);
        }
        if (out.isEmpty()) {
            out.add(NpmComparator.ANY);
        }
        return out;
    }

    private static String parseComparator(String comp) {
        comp = replaceCaret(comp);
        comp = replaceTilde(comp);
        comp = replaceXRange(comp);
        comp = STAR.matcher(comp.trim()).replaceAll("");
        return comp;
    }

    private static boolean isX(@Nullable String id) {
        return id == null || id.isEmpty() || "x".equalsIgnoreCase(id) || "*".equals(id);
    }

    private static String hyphenReplace(Matcher m) {
        String from = m.group(1);
        String fM = m.group(2), fm = m.group(3), fp = m.group(4), fpr = m.group(5);
        String to = m.group(7);
        String tM = m.group(8), tm = m.group(9), tp = m.group(10), tpr = m.group(11);

        if (isX(fM)) {
            from = "";
        } else if (isX(fm)) {
            from = ">=" + fM + ".0.0";
        } else if (isX(fp)) {
            from = ">=" + fM + "." + fm + ".0";
        } else {
            from = ">=" + from;
        }

        if (isX(tM)) {
            to = "";
        } else if (isX(tm)) {
            to = "<" + (Long.parseLong(tM) + 1) + ".0.0-0";
        } else if (isX(tp)) {
            to = "<" + tM + "." + (Long.parseLong(tm) + 1) + ".0-0";
        } else if (tpr != null) {
            to = "<=" + tM + "." + tm + "." + tp + "-" + tpr;
        } else {
            to = "<=" + to;
        }
        return (from + " " + to).trim();
    }

    private static String replaceTilde(String comp) {
        Matcher m = TILDE.matcher(comp.trim());
        if (!m.matches()) {
            return comp;
        }
        String M = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        if (isX(M)) {
            return "";
        }
        if (isX(mn)) {
            return ">=" + M + ".0.0 <" + (Long.parseLong(M) + 1) + ".0.0-0";
        }
        if (isX(p)) {
            return ">=" + M + "." + mn + ".0 <" + M + "." + (Long.parseLong(mn) + 1) + ".0-0";
        }
        String base = pr != null ? M + "." + mn + "." + p + "-" + pr : M + "." + mn + "." + p;
        return ">=" + base + " <" + M + "." + (Long.parseLong(mn) + 1) + ".0-0";
    }

    private static String replaceCaret(String comp) {
        Matcher m = CARET.matcher(comp.trim());
        if (!m.matches()) {
            return comp;
        }
        String M = m.group(1), mn = m.group(2), p = m.group(3), pr = m.group(4);
        if (isX(M)) {
            return "";
        }
        if (isX(mn)) {
            return ">=" + M + ".0.0 <" + (Long.parseLong(M) + 1) + ".0.0-0";
        }
        if (isX(p)) {
            if ("0".equals(M)) {
                return ">=" + M + "." + mn + ".0 <" + M + "." + (Long.parseLong(mn) + 1) + ".0-0";
            }
            return ">=" + M + "." + mn + ".0 <" + (Long.parseLong(M) + 1) + ".0.0-0";
        }
        String base = pr != null ? M + "." + mn + "." + p + "-" + pr : M + "." + mn + "." + p;
        if ("0".equals(M)) {
            if ("0".equals(mn)) {
                return ">=" + base + " <" + M + "." + mn + "." + (Long.parseLong(p) + 1) + "-0";
            }
            return ">=" + base + " <" + M + "." + (Long.parseLong(mn) + 1) + ".0-0";
        }
        return ">=" + base + " <" + (Long.parseLong(M) + 1) + ".0.0-0";
    }

    private static String replaceXRange(String comp) {
        Matcher m = XRANGE.matcher(comp.trim());
        if (!m.matches()) {
            return comp;
        }
        String gtlt = m.group(1);
        String M = m.group(2), mn = m.group(3), p = m.group(4);
        boolean xM = isX(M);
        boolean xm = xM || isX(mn);
        boolean xp = xm || isX(p);
        boolean anyX = xp;

        if ("=".equals(gtlt) && anyX) {
            gtlt = "";
        }
        if (xM) {
            if (">".equals(gtlt) || "<".equals(gtlt)) {
                return "<0.0.0-0";
            }
            return "*";
        }
        if (!gtlt.isEmpty() && anyX) {
            long major = Long.parseLong(M);
            long minor = xm ? 0 : Long.parseLong(mn);
            long patch = 0;
            if (">".equals(gtlt)) {
                gtlt = ">=";
                if (xm) {
                    major = major + 1;
                    minor = 0;
                } else {
                    minor = minor + 1;
                }
            } else if ("<=".equals(gtlt)) {
                gtlt = "<";
                if (xm) {
                    major = major + 1;
                } else {
                    minor = minor + 1;
                }
            }
            String pr = "<".equals(gtlt) ? "-0" : "";
            return gtlt + major + "." + minor + "." + patch + pr;
        }
        if (xm) {
            return ">=" + M + ".0.0 <" + (Long.parseLong(M) + 1) + ".0.0-0";
        }
        if (xp) {
            return ">=" + M + "." + mn + ".0 <" + M + "." + (Long.parseLong(mn) + 1) + ".0-0";
        }
        return comp;
    }

    private static String replaceAll(Pattern pattern, String input,
                                     java.util.function.Function<Matcher, String> replacer) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(replacer.apply(m)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** node-semver {@code satisfies(version, range)} with default options. */
    public boolean satisfies(NpmVersion version) {
        for (List<NpmComparator> set : comparatorSets) {
            if (testSet(set, version)) {
                return true;
            }
        }
        return false;
    }

    public boolean satisfies(@Nullable String version) {
        NpmVersion v = NpmVersion.parse(version);
        return v != null && satisfies(v);
    }

    private static boolean testSet(List<NpmComparator> set, NpmVersion version) {
        for (NpmComparator c : set) {
            if (!c.test(version)) {
                return false;
            }
        }
        if (version.hasPrerelease()) {
            // Prerelease versions only satisfy a set that mentions a prerelease
            // of the same [major, minor, patch] tuple.
            for (NpmComparator c : set) {
                NpmVersion cv = c.getVersion();
                if (cv != null && cv.hasPrerelease() && cv.sameTuple(version)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /** Highest satisfying version, or {@code null} when none satisfies. */
    public static @Nullable String maxSatisfying(Collection<String> versions, NpmRange range) {
        NpmVersion best = null;
        String bestRaw = null;
        for (String raw : versions) {
            NpmVersion v = NpmVersion.parse(raw);
            if (v == null || !range.satisfies(v)) {
                continue;
            }
            if (best == null || v.compareTo(best) > 0) {
                best = v;
                bestRaw = raw;
            }
        }
        return bestRaw;
    }

    /**
     * npm's version selection (npm-pick-manifest): the version tagged {@code latest}
     * wins when it satisfies the range, even when a higher satisfying version exists;
     * otherwise the highest satisfying version is chosen.
     */
    public static @Nullable String pickVersion(Collection<String> versions,
                                               @Nullable String latestTag,
                                               NpmRange range) {
        if (latestTag != null && versions.contains(latestTag) && range.satisfies(latestTag)) {
            return latestTag;
        }
        return maxSatisfying(versions, range);
    }

    @Override
    public String toString() {
        return raw;
    }
}
