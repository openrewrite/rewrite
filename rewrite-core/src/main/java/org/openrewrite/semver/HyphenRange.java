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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.semver.Semver.isVersion;

/**
 * <a href="https://github.com/npm/node-semver#hyphen-ranges-xyz---abc">Hyphen ranges</a>.
 */
public class HyphenRange extends LatestRelease {
    private static final Pattern HYPHEN_RANGE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)\\s*-\\s*(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)");

    /**
     * The npm hyphen grammar; unlike {@link #HYPHEN_RANGE_PATTERN}, no 4th components, whitespace
     * around the hyphen required. Groups 1-5 are the "from" bound and its major/minor/patch/prerelease;
     * groups 6-10 the "to" bound.
     */
    private static final Pattern NODE_HYPHEN_PATTERN = Pattern.compile(
            "^\\s*(" + NodeComparand.XRANGE_PLAIN + ")\\s+-\\s+(" + NodeComparand.XRANGE_PLAIN + ")\\s*$");

    private final String upper;
    private final String lower;

    /** Non-null when this instance applies exact npm semantics, which live on the delegate. */
    private final @Nullable UnionRange node;

    private HyphenRange(String lower, String upper, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.lower = lower;
        this.upper = upper;
        this.node = null;
    }

    private HyphenRange(UnionRange node, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.lower = "";
        this.upper = "";
        this.node = node;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        if (node != null) {
            return node.isValidVersion(version, getMetadataPattern());
        }
        return super.isValid(currentVersion, version) &&
                super.compare(currentVersion, version, upper) <= 0 &&
                super.compare(currentVersion, version, lower) >= 0;
    }

    public static Validated<HyphenRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = HYPHEN_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches()) {
            return Validated.invalid("hyphenRange", pattern, "not a hyphen range");
        }
        return Validated.valid("hyphenRange", new HyphenRange(matcher.group(1), matcher.group(5), metadataPattern));
    }

    /** A hyphen range with exact npm semantics ({@code 1.2 - 2.3} -> {@code >=1.2.0 <2.4.0-0}), for the {@link Semver.Ecosystem#NODE} chain. */
    static Validated<VersionComparator> buildNode(String pattern, @Nullable String metadataPattern) {
        if (!NODE_HYPHEN_PATTERN.matcher(pattern.trim()).matches()) {
            return Validated.invalid("hyphenRange", pattern, "not a node hyphen range");
        }
        UnionRange node = UnionRange.parse(pattern, false);
        if (node == null) {
            return Validated.invalid("hyphenRange", pattern, "not a valid node range");
        }
        return Validated.valid("hyphenRange", new HyphenRange(node, metadataPattern));
    }

    /** Port of node-semver {@code range.js} {@code hyphenReplace}; non-hyphen groups pass through. */
    static String hyphenReplace(String group, boolean incPre) {
        Matcher m = NODE_HYPHEN_PATTERN.matcher(group);
        if (!m.matches()) {
            return group;
        }
        String from = hyphenFrom(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), incPre);
        String to = hyphenTo(m.group(6), m.group(7), m.group(8), m.group(9), m.group(10), incPre);
        return (from + " " + to).trim();
    }

    private static String hyphenFrom(String from, String fM, String fm, String fp, @Nullable String fpr, boolean incPre) {
        if (NodeComparand.isX(fM)) {
            return "";
        }
        if (NodeComparand.isX(fm)) {
            return ">=" + fM + ".0.0" + (incPre ? "-0" : "");
        }
        if (NodeComparand.isX(fp)) {
            return ">=" + fM + "." + fm + ".0" + (incPre ? "-0" : "");
        }
        if (fpr != null) {
            return ">=" + from;
        }
        return ">=" + from + (incPre ? "-0" : "");
    }

    private static String hyphenTo(String to, String tM, String tm, String tp, @Nullable String tpr, boolean incPre) {
        if (NodeComparand.isX(tM)) {
            return "";
        }
        if (NodeComparand.isX(tm)) {
            return "<" + NodeComparand.incr(tM) + ".0.0-0";
        }
        if (NodeComparand.isX(tp)) {
            return "<" + tM + "." + NodeComparand.incr(tm) + ".0-0";
        }
        if (tpr != null) {
            return "<=" + tM + "." + tm + "." + tp + "-" + tpr;
        }
        if (incPre) {
            return "<" + tM + "." + tm + "." + NodeComparand.incr(tp) + "-0";
        }
        return "<=" + to;
    }

    @Override
    public String toString() {
        return node != null ? node.toString() : super.toString();
    }

    @Override
    public int compare(@Nullable String currentVersion, String v1, String v2) {
        if (node != null) {
            return ParsedVersion.compareLenient(v1, v2);
        }
        Validated<HyphenRange> maybeHyphenRangeV1 = build(v1, null);
        Validated<HyphenRange> maybeHyphenRangeV2 = build(v2, null);
        if (maybeHyphenRangeV1.isValid() && maybeHyphenRangeV2.isValid()) {
            HyphenRange hyphenRangeV1 = requireNonNull(maybeHyphenRangeV1.getValue());
            HyphenRange hyphenRangeV2 = requireNonNull(maybeHyphenRangeV2.getValue());
            int compare = super.compare(currentVersion, hyphenRangeV1.upper, hyphenRangeV2.upper);
            if (compare != 0) {
                return compare;
            }

            return super.compare(currentVersion, hyphenRangeV1.lower, hyphenRangeV2.lower);
        } else if (maybeHyphenRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            HyphenRange hyphenRangeV1 = requireNonNull(maybeHyphenRangeV1.getValue());
            int compare = super.compare(currentVersion, hyphenRangeV1.upper, v2);
            if (compare < 0) {
                return compare;
            }

            compare = super.compare(currentVersion, hyphenRangeV1.lower, v2);
            return Math.max(compare, 0);

        } else if (maybeHyphenRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            HyphenRange hyphenRangeV2 = requireNonNull(maybeHyphenRangeV2.getValue());
            int compare = super.compare(currentVersion, v1, hyphenRangeV2.upper);
            if (compare > 0) {
                return compare;
            }

            compare = super.compare(currentVersion, v1, hyphenRangeV2.lower);
            return Math.min(compare, 0);
        }

        return super.compare(currentVersion, v1, v2);
    }
}
