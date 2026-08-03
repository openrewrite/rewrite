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
 * Any of X, x, *, or + may be used to "stand in" for one of the numeric values in the [major, minor, patch] tuple.
 * <a href="https://github.com/npm/node-semver#x-ranges-12x-1x-12-">X-Ranges</a>.
 * The + wildcard is supported for Gradle-style dynamic versions (e.g., "2.+").
 */
public class XRange extends LatestRelease {
    private static final Pattern X_RANGE_PATTERN = Pattern.compile("([*xX+]|\\d+)(?:\\.([*xX+]|\\d+)(?:\\.([*xX+]|\\d+))?(?:\\.([*xX+]|\\d+))?)?");

    /**
     * The npm x-range grammar ({@code 1.2.x}, {@code 1.2}, {@code >=1.2}, {@code *}); unlike
     * {@link #X_RANGE_PATTERN}, no {@code +} wildcard, no 4th component, optional operator.
     * Groups: operator, major, minor, patch, prerelease.
     */
    private static final Pattern NODE_X_RANGE_PATTERN = Pattern.compile("^((?:<|>)?=?)\\s*" + NodeComparand.XRANGE_PLAIN + "$");

    private static final Pattern NODE_STAR_PATTERN = Pattern.compile("^(?:<|>)?=?\\s*\\*$");

    private final String major;
    private final String minor;
    private final String patch;
    private final String micro;

    /** Non-null when this instance applies exact npm semantics, which live on the delegate. */
    private final @Nullable UnionRange node;

    XRange(String major, String minor, String patch, String micro, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.micro = micro;
        this.node = null;
    }

    private XRange(UnionRange node, @Nullable String metadataPattern) {
        super(metadataPattern);
        this.major = "";
        this.minor = "";
        this.patch = "";
        this.micro = "";
        this.node = node;
    }

    @Override
    public boolean isValid(@Nullable String currentVersion, String version) {
        if (node != null) {
            return node.isValidVersion(version, getMetadataPattern());
        }
        if (!super.isValid(currentVersion, version)) {
            return false;
        }

        if ("*".equals(major)) {
            return true;
        }

        ParsedVersion gav = ParsedVersion.parse(normalizeVersion(version));

        if (!major.equals(gav.group(1))) {
            return false;
        }

        if ("*".equals(minor)) {
            return true;
        } else if (gav.group(2) == null || !gav.group(2).equals(minor)) {
            return false;
        }

        if ("*".equals(patch)) {
            return true;
        } else if (gav.group(3) == null || !gav.group(3).equals(patch)) {
            return false;
        }

        return gav.group(4) == null || !gav.group(4).equals(micro);
    }

    public static Validated<XRange> build(String pattern, @Nullable String metadataPattern) {
        Matcher matcher = X_RANGE_PATTERN.matcher(pattern);
        if (!matcher.matches() || !(pattern.contains("x") || pattern.contains("X") || pattern.contains("*") || pattern.contains("+"))) {
            return Validated.invalid("xRange", pattern, "not an x-range");
        }

        String major = normalizeWildcard(matcher.group(1));
        String minor = normalizeWildcard(matcher.group(2) == null ? "0" : matcher.group(2));
        String patch = normalizeWildcard(matcher.group(3) == null ? "0" : matcher.group(3));
        String micro = normalizeWildcard(matcher.group(4) == null ? "0" : matcher.group(4));

        if ("*".equals(major) && (matcher.group(2) != null || matcher.group(3) != null || matcher.group(4) != null)) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        } else if ("*".equals(minor) && (matcher.group(3) != null || matcher.group(4) != null)) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        } else if ("*".equals(patch) && matcher.group(4) != null) {
            return Validated.invalid("xRange", pattern, "not an x-range: nothing can follow a wildcard");
        }

        return Validated.valid("xRange", new XRange(major, minor, patch, micro, metadataPattern));
    }

    private static String normalizeWildcard(String part) {
        return isWildcard(part) ? "*" : part;
    }

    /**
     * @return whether {@code segment} is a Maven/Gradle-flavored wildcard: {@code *}, {@code x},
     * {@code X}, or {@code +}. npm's notion (no {@code +}) is {@link NodeComparand#isX}.
     */
    static boolean isWildcard(String segment) {
        return "*".equals(segment) || "x".equals(segment) || "X".equals(segment) || "+".equals(segment);
    }

    /**
     * An x-range with exact npm semantics, for the {@link Semver.Ecosystem#NODE} chain. Requires an
     * operator or a wildcard/partial component, so bare exact versions fall through to {@link UnionRange}.
     */
    static Validated<VersionComparator> buildNode(String pattern, @Nullable String metadataPattern) {
        Matcher m = NODE_X_RANGE_PATTERN.matcher(pattern.trim());
        if (!m.matches() || (m.group(1).isEmpty() &&
                !(NodeComparand.isX(m.group(2)) || NodeComparand.isX(m.group(3)) || NodeComparand.isX(m.group(4))))) {
            return Validated.invalid("xRange", pattern, "not a node x-range");
        }
        UnionRange node = UnionRange.parse(pattern, false);
        if (node == null) {
            return Validated.invalid("xRange", pattern, "not a valid node range");
        }
        return Validated.valid("xRange", new XRange(node, metadataPattern));
    }

    /** Port of node-semver {@code range.js} {@code replaceXRange}; tokens without wildcard or partial components pass through. */
    static String replaceXRange(String token, boolean incPre) {
        Matcher m = NODE_X_RANGE_PATTERN.matcher(token.trim());
        if (!m.matches()) {
            return token;
        }
        String gtlt = m.group(1);
        String mj = m.group(2), mn = m.group(3), p = m.group(4);
        boolean xM = NodeComparand.isX(mj);
        boolean xm = xM || NodeComparand.isX(mn);
        boolean xp = xm || NodeComparand.isX(p);
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
                    mj = NodeComparand.incr(mj);
                    mn = "0";
                } else {
                    mn = NodeComparand.incr(mn);
                }
                p = "0";
            } else if ("<=".equals(gtlt)) {
                gtlt = "<";
                if (xm) {
                    mj = NodeComparand.incr(mj);
                } else {
                    mn = NodeComparand.incr(mn);
                }
            }
            if ("<".equals(gtlt)) {
                pr = "-0";
            }
            return gtlt + mj + "." + mn + "." + p + pr;
        }
        if (xm) {
            return ">=" + mj + ".0.0" + pr + " <" + NodeComparand.incr(mj) + ".0.0-0";
        }
        if (xp) {
            return ">=" + mj + "." + mn + ".0" + pr + " <" + mj + "." + NodeComparand.incr(mn) + ".0-0";
        }
        return token;
    }

    /** node-semver's star replacement: a bare or operator-prefixed {@code *} collapses to the ANY clause. */
    static String replaceStar(String token) {
        return NODE_STAR_PATTERN.matcher(token).matches() ? "" : token;
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
        Validated<XRange> maybeXRangeV1 = build(v1, null);
        Validated<XRange> maybeXRangeV2 = build(v2, null);
        if (maybeXRangeV1.isValid() && maybeXRangeV2.isValid()) {
            XRange xrangeV1 = requireNonNull(maybeXRangeV1.getValue());
            XRange xrangeV2 = requireNonNull(maybeXRangeV2.getValue());

            if ("*".equals(xrangeV1.major) && "*".equals(xrangeV2.major)) {
                return 0;
            } else if ("*".equals(xrangeV1.major)) {
                return 1;
            } else if ("*".equals(xrangeV2.major)) {
                return -1;
            }

            int compare = xrangeV1.major.compareTo(xrangeV2.major);
            if (compare != 0) {
                return compare;
            }

            if ("*".equals(xrangeV1.minor) && "*".equals(xrangeV2.minor)) {
                return 0;
            } else if ("*".equals(xrangeV1.minor)) {
                return 1;
            } else if ("*".equals(xrangeV2.minor)) {
                return -1;
            }

            compare =  xrangeV1.minor.compareTo(xrangeV2.minor);
            if (compare != 0) {
                return compare;
            }

            if ("*".equals(xrangeV1.patch) && "*".equals(xrangeV2.patch)) {
                return 0;
            } else if ("*".equals(xrangeV1.patch)) {
                return 1;
            } else if ("*".equals(xrangeV2.patch)) {
                return -1;
            }

            // Micro is guaranteed to be the same, so we can stop here
            return xrangeV1.patch.compareTo(xrangeV2.patch);
        } else if (maybeXRangeV1.isValid()) {
            if (!isVersion(v2)) {
                return 1;
            }

            XRange xrangeV1 = requireNonNull(maybeXRangeV1.getValue());
            if ("*".equals(xrangeV1.major)) {
                return 0;
            }

            ParsedVersion gav = ParsedVersion.parse(normalizeVersion(v2));

            if (!xrangeV1.major.equals(gav.group(1))) {
                return xrangeV1.major.compareTo(gav.group(1));
            }

            if ("*".equals(xrangeV1.minor)) {
                return 0;
            } else if (gav.group(2) == null || !xrangeV1.minor.equals(gav.group(2))) {
                return xrangeV1.minor.compareTo(gav.group(2));
            }

            if ("*".equals(xrangeV1.patch)) {
                return 0;
            } else if (gav.group(3) == null || !xrangeV1.patch.equals(gav.group(3))) {
                return xrangeV1.patch.compareTo(gav.group(3));
            }

            return 0;
        } else if (maybeXRangeV2.isValid()) {
            if (!isVersion(v1)) {
                return -1;
            }

            XRange xrangeV2 = requireNonNull(maybeXRangeV2.getValue());
            if ("*".equals(xrangeV2.major)) {
                return 0;
            }

            ParsedVersion gav = ParsedVersion.parse(normalizeVersion(v1));

            String major = gav.group(1);
            if (!xrangeV2.major.equals(major)) {
                return requireNonNull(major).compareTo(xrangeV2.major);
            }

            String minor = gav.group(2);
            if ("*".equals(xrangeV2.minor)) {
                return 0;
            } else if (minor == null || !minor.equals(xrangeV2.minor)) {
                return requireNonNull(minor).compareTo(xrangeV2.minor);
            }

            String patch = gav.group(3);
            if ("*".equals(xrangeV2.patch)) {
                return 0;
            } else if (patch == null || !patch.equals(xrangeV2.patch)) {
                return requireNonNull(patch).compareTo(xrangeV2.patch);
            }

            return 0;
        }

        return super.compare(currentVersion, v1, v2);
    }
}
