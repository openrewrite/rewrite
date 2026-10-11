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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Port of node-semver's functions with its {@code loose} and {@code includePrerelease} options: 7.8.4, which npm
 * 11.17.0 bundles, and 5.5.0, which yarn 1.22.22 bundles.
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

        final boolean loose;
        final boolean includePrerelease;
        final Release release;
    }

    /**
     * @return the normalized version, or {@code null} when it does not parse.
     */
    public static @Nullable String valid(String version, Options options) {
        NodeSemverVersion v = NodeSemverVersion.parse(version, options);
        return v.isStrictSemver() ? v.nodeVersion() : null;
    }

    /**
     * @return the range's canonical form ({@code *} for any version), or {@code null} when invalid.
     */
    public static @Nullable String validRange(String range, Options options) {
        NodeSemverRange r = NodeSemverRange.parse(range, options);
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
        NodeSemverRange r = NodeSemverRange.parse(range, options);
        return r != null && test(r, version, options);
    }

    private static boolean test(NodeSemverRange r, String version, Options options) {
        if (options.release == Release.V5_5_0 && !version.isEmpty()) {
            if (version.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(tooLong());
            }
            if (!NodeSemverVersion.parse(version, options).isStrictSemver()) {
                throw new IllegalArgumentException("Invalid Version: " + version);
            }
        }
        return r.test(version);
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
        NodeSemverRange r = NodeSemverRange.parse(range, options);
        if (r == null || !truthy(version)) {
            return false;
        }
        // Range.test hands the value to each set as it is: a comparator other than `*` makes a SemVer of it and
        // throws, and a set of `*` alone reads version.prerelease.length
        for (List<NodeSemverComparator> set : r.sets()) {
            for (NodeSemverComparator comparator : set) {
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
     * @return the version's prerelease identifiers, each a {@code Long} or a {@code String}, or {@code null} when it
     * has none or does not parse.
     */
    public static @Nullable List<Object> prerelease(String version, Options options) {
        NodeSemverVersion v = NodeSemverVersion.parse(version, options);
        return v.isStrictSemver() && v.hasPrerelease() ? v.strictPrerelease() : null;
    }

    /**
     * @return the highest of the versions the range allows, the first of equal ones, or {@code null}.
     */
    public static @Nullable String maxSatisfying(Collection<String> versions, String range, Options options) {
        NodeSemverRange r = NodeSemverRange.parse(range, options);
        if (r == null) {
            return null;
        }
        String max = null;
        NodeSemverVersion maxVersion = null;
        for (String version : versions) {
            if (test(r, version, options)) {
                NodeSemverVersion v = NodeSemverVersion.parse(version, options);
                if (maxVersion == null || maxVersion.comparePrecedence(v) < 0) {
                    max = version;
                    maxVersion = v;
                }
            }
        }
        return max;
    }

    /**
     * node-semver's {@code minSatisfying}: the lowest version the range admits, the first of equal ones; {@code null}
     * when none does or the range is invalid.
     */
    public static @Nullable String minSatisfying(Collection<String> versions, String range, Options options) {
        NodeSemverRange r = NodeSemverRange.parse(range, options);
        if (r == null) {
            return null;
        }
        String min = null;
        NodeSemverVersion minVersion = null;
        for (String version : versions) {
            if (test(r, version, options)) {
                NodeSemverVersion v = NodeSemverVersion.parse(version, options);
                if (minVersion == null || minVersion.comparePrecedence(v) > 0) {
                    min = version;
                    minVersion = v;
                }
            }
        }
        return min;
    }

    /**
     * node-semver's {@code minVersion}: the lowest version the range admits, or {@code null} when it admits none.
     * @throws IllegalArgumentException when the range is invalid, as node-semver throws.
     */
    public static @Nullable String minVersion(String range, Options options) {
        NodeSemverRange r = NodeSemverRange.parse(range, options);
        if (r == null) {
            throw new IllegalArgumentException(NodeSemverRange.invalidRangeMessage(range, options));
        }
        for (String lowest : new String[]{"0.0.0", "0.0.0-0"}) {
            if (r.test(NodeSemverVersion.parse(lowest, Options.DEFAULT))) {
                return lowest;
            }
        }
        NodeSemverVersion minver = null;
        for (List<NodeSemverComparator> comparators : r.sets()) {
            NodeSemverVersion setMin = null;
            for (NodeSemverComparator comparator : comparators) {
                if (comparator.isAny()) {
                    continue;
                }
                NodeSemverVersion compver = comparator.getVersion();
                String operator = comparator.operator();
                if (">".equals(operator)) {
                    String base = compver.strictMajor() + "." + compver.strictMinor() + ".";
                    compver = NodeSemverVersion.parse(compver.hasPrerelease() ?
                            base + compver.strictPatch() + "-" + join(compver.strictPrerelease()) + ".0" :
                            base + (compver.strictPatch() + 1), Options.DEFAULT);
                } else if (!operator.isEmpty() && !">=".equals(operator)) {
                    continue;
                }
                if (setMin == null || compver.comparePrecedence(setMin) > 0) {
                    setMin = compver;
                }
            }
            if (setMin != null && (minver == null || minver.comparePrecedence(setMin) > 0)) {
                minver = setMin;
            }
        }
        return minver != null && r.test(minver) ? minver.nodeVersion() : null;
    }

    private static String join(List<Object> identifiers) {
        StringJoiner joined = new StringJoiner(".");
        for (Object identifier : identifiers) {
            joined.add(String.valueOf(identifier));
        }
        return joined.toString();
    }

    /**
     * @throws IllegalArgumentException when either range is invalid, as node-semver throws.
     */
    public static boolean intersects(String range1, String range2, Options options) {
        NodeSemverRange r1 = NodeSemverRange.parse(range1, options);
        NodeSemverRange r2 = NodeSemverRange.parse(range2, options);
        if (r1 == null || r2 == null) {
            throw new IllegalArgumentException("Invalid SemVer Range: " + (r1 == null ? range1 : range2));
        }
        return r1.intersects(r2);
    }

    /**
     * node-semver's {@code subset}: whether every version {@code sub} admits is one {@code dom} admits.
     * @throws IllegalArgumentException when either range is invalid, as node-semver throws.
     */
    public static boolean subset(String sub, String dom, Options options) {
        if (sub.equals(dom)) {
            return true;
        }
        NodeSemverRange subRange = NodeSemverRange.parse(sub, options);
        NodeSemverRange domRange = NodeSemverRange.parse(dom, options);
        if (subRange == null || domRange == null) {
            throw new IllegalArgumentException("Invalid SemVer Range: " + (subRange == null ? sub : dom));
        }
        boolean sawNonNull = false;
        outer:
        for (List<NodeSemverComparator> simpleSub : subRange.sets()) {
            for (List<NodeSemverComparator> simpleDom : domRange.sets()) {
                Boolean isSub = simpleSubset(simpleSub, simpleDom, options);
                sawNonNull = sawNonNull || isSub != null;
                if (Boolean.TRUE.equals(isSub)) {
                    continue outer;
                }
            }
            if (sawNonNull) {
                return false;
            }
        }
        return true;
    }

    // null is the null set
    private static @Nullable Boolean simpleSubset(List<NodeSemverComparator> sub, List<NodeSemverComparator> dom,
                                                  Options options) {
        if (sub.size() == 1 && sub.get(0).isAny()) {
            if (dom.size() == 1 && dom.get(0).isAny()) {
                return true;
            }
            sub = Collections.singletonList(NodeSemverComparator.parse(
                    options.includePrerelease ? ">=0.0.0-0" : ">=0.0.0", Options.DEFAULT));
        }
        if (dom.size() == 1 && dom.get(0).isAny()) {
            if (options.includePrerelease) {
                return true;
            }
            dom = Collections.singletonList(NodeSemverComparator.GTE_ZERO);
        }
        List<NodeSemverComparator> eqSet = new ArrayList<>();
        NodeSemverComparator gt = null;
        NodeSemverComparator lt = null;
        for (NodeSemverComparator c : sub) {
            if (c.operator().startsWith(">")) {
                gt = higherGT(gt, c);
            } else if (c.operator().startsWith("<")) {
                lt = lowerLT(lt, c);
            } else {
                eqSet.add(c);
            }
        }
        if (eqSet.size() > 1) {
            return null;
        }
        Integer gtltComp = null;
        if (gt != null && lt != null) {
            gtltComp = gt.getVersion().comparePrecedence(lt.getVersion());
            if (gtltComp > 0) {
                return null;
            } else if (gtltComp == 0 && (!">=".equals(gt.operator()) || !"<=".equals(lt.operator()))) {
                return null;
            }
        }
        for (NodeSemverComparator eq : eqSet) {
            if (gt != null && !comparatorSatisfies(eq, gt, options)) {
                return null;
            }
            if (lt != null && !comparatorSatisfies(eq, lt, options)) {
                return null;
            }
            for (NodeSemverComparator c : dom) {
                if (!comparatorSatisfies(eq, c, options)) {
                    return false;
                }
            }
            return true;
        }
        boolean hasDomLT = false;
        boolean hasDomGT = false;
        NodeSemverVersion needDomLTPre = lt != null && !options.includePrerelease && lt.getVersion().hasPrerelease() ?
                lt.getVersion() : null;
        NodeSemverVersion needDomGTPre = gt != null && !options.includePrerelease && gt.getVersion().hasPrerelease() ?
                gt.getVersion() : null;
        if (needDomLTPre != null && needDomLTPre.strictPrerelease().size() == 1 && "<".equals(lt.operator()) &&
                Long.valueOf(0).equals(needDomLTPre.strictPrerelease().get(0))) {
            needDomLTPre = null;
        }
        for (NodeSemverComparator c : dom) {
            hasDomGT = hasDomGT || c.operator().startsWith(">");
            hasDomLT = hasDomLT || c.operator().startsWith("<");
            if (gt != null) {
                if (needDomGTPre != null && sameTupleWithPrerelease(c, needDomGTPre)) {
                    needDomGTPre = null;
                }
                if (c.operator().startsWith(">")) {
                    NodeSemverComparator higher = higherGT(gt, c);
                    if (higher == c && higher != gt) {
                        return false;
                    }
                } else if (">=".equals(gt.operator()) && !c.test(gt.getVersion())) {
                    return false;
                }
            }
            if (lt != null) {
                if (needDomLTPre != null && sameTupleWithPrerelease(c, needDomLTPre)) {
                    needDomLTPre = null;
                }
                if (c.operator().startsWith("<")) {
                    NodeSemverComparator lower = lowerLT(lt, c);
                    if (lower == c && lower != lt) {
                        return false;
                    }
                } else if ("<=".equals(lt.operator()) && !c.test(lt.getVersion())) {
                    return false;
                }
            }
            if (c.operator().isEmpty() && (lt != null || gt != null) && (gtltComp == null || gtltComp != 0)) {
                return false;
            }
        }
        if (gt != null && hasDomLT && lt == null && (gtltComp == null || gtltComp != 0)) {
            return false;
        }
        if (lt != null && hasDomGT && gt == null && (gtltComp == null || gtltComp != 0)) {
            return false;
        }
        return needDomGTPre == null && needDomLTPre == null;
    }

    private static boolean comparatorSatisfies(NodeSemverComparator version, NodeSemverComparator comparator,
                                               Options options) {
        NodeSemverRange range = NodeSemverRange.parse(comparator.value(), options);
        return range != null && range.test(version.getVersion());
    }

    private static boolean sameTupleWithPrerelease(NodeSemverComparator c, NodeSemverVersion v) {
        return !c.isAny() && c.getVersion().hasPrerelease() &&
               c.getVersion().strictMajor() == v.strictMajor() &&
               c.getVersion().strictMinor() == v.strictMinor() &&
               c.getVersion().strictPatch() == v.strictPatch();
    }

    // >=1.2.3 is lower than >1.2.3
    private static NodeSemverComparator higherGT(@Nullable NodeSemverComparator a, NodeSemverComparator b) {
        if (a == null) {
            return b;
        }
        int comp = a.getVersion().comparePrecedence(b.getVersion());
        return comp > 0 ? a : comp < 0 ? b : ">".equals(b.operator()) && ">=".equals(a.operator()) ? b : a;
    }

    // <=1.2.3 is higher than <1.2.3
    private static NodeSemverComparator lowerLT(@Nullable NodeSemverComparator a, NodeSemverComparator b) {
        if (a == null) {
            return b;
        }
        int comp = a.getVersion().comparePrecedence(b.getVersion());
        return comp < 0 ? a : comp > 0 ? b : "<".equals(b.operator()) && "<=".equals(a.operator()) ? b : a;
    }

    /**
     * @throws IllegalArgumentException when either version is invalid, as node-semver throws.
     */
    public static int compare(String v1, String v2, Options options) {
        return compare((Object) v1, v2, options);
    }

    /**
     * {@code compare} with the versions as JSON package data may hold them, in any type.
     * @throws IllegalArgumentException when either version is invalid or not a string, as node-semver throws.
     */
    public static int compare(@Nullable Object v1, @Nullable Object v2, Options options) {
        NodeSemverVersion a = semver(v1, options);
        NodeSemverVersion b = semver(v2, options);
        return a.comparePrecedence(b);
    }

    /**
     * node-semver's {@code compareBuild}: {@code compare}, then the build metadata.
     * @throws IllegalArgumentException when either version is invalid, as node-semver throws.
     */
    public static int compareBuild(String v1, String v2, Options options) {
        NodeSemverVersion a = semver(v1, options);
        NodeSemverVersion b = semver(v2, options);
        int c = a.comparePrecedence(b);
        return c != 0 ? c : a.compareBuild(b);
    }

    /**
     * node-semver's {@code rsort}: sorted in place, highest first by {@code compareBuild}, stably.
     * @throws IllegalArgumentException when a version it compares is invalid, as node-semver throws.
     */
    public static void rsort(List<String> versions, Options options) {
        versions.sort((a, b) -> compareBuild(b, a, options));
    }

    private static NodeSemverVersion semver(@Nullable Object version, Options options) {
        if (!(version instanceof String)) {
            throw new IllegalArgumentException(notASemVer(version, options));
        }
        if (((String) version).length() > MAX_LENGTH) {
            throw new IllegalArgumentException(tooLong());
        }
        NodeSemverVersion v = NodeSemverVersion.parse((String) version, options);
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
