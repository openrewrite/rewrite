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

import java.util.Collection;
import java.util.List;

/**
 * The public node-semver facade the native Node lock engine consumes. Every method is a thin wrapper
 * over {@link NodeVersion}/{@link NodeRange}; this is a <em>separate entry point</em> from Maven's
 * {@code Semver.validate} selector chain (A1) -- node range parsing never routes through the Maven
 * comparator, which has incompatible prerelease and 4th-component semantics.
 */
public final class NodeSemver {

    private NodeSemver() {
    }

    public static boolean satisfies(String version, String range) {
        return satisfies(version, range, false);
    }

    public static boolean satisfies(String version, String range, boolean includePrerelease) {
        NodeVersion v = NodeVersion.parse(version);
        NodeRange r = NodeRange.parse(range, includePrerelease);
        return v != null && r != null && r.test(v);
    }

    /**
     * @return {@code true} if {@code range} parses to a valid npm range.
     */
    public static boolean validRange(String range) {
        return NodeRange.parse(range) != null;
    }

    /**
     * @return the highest of {@code versions} satisfying {@code range}, in its original spelling, or
     * {@code null} if none satisfy.
     */
    public static @Nullable String maxSatisfying(Collection<String> versions, String range) {
        return maxSatisfying(versions, range, false);
    }

    public static @Nullable String maxSatisfying(Collection<String> versions, String range, boolean includePrerelease) {
        NodeRange r = NodeRange.parse(range, includePrerelease);
        if (r == null) {
            return null;
        }
        String best = null;
        NodeVersion bestVersion = null;
        for (String candidate : versions) {
            NodeVersion v = NodeVersion.parse(candidate);
            if (v == null || !r.test(v)) {
                continue;
            }
            if (bestVersion == null || v.compareTo(bestVersion) > 0) {
                best = candidate;
                bestVersion = v;
            }
        }
        return best;
    }

    /**
     * @return the lowest of {@code versions} satisfying {@code range}, in its original spelling, or
     * {@code null} if none satisfy.
     */
    public static @Nullable String minSatisfying(Collection<String> versions, String range) {
        return minSatisfying(versions, range, false);
    }

    public static @Nullable String minSatisfying(Collection<String> versions, String range, boolean includePrerelease) {
        NodeRange r = NodeRange.parse(range, includePrerelease);
        if (r == null) {
            return null;
        }
        String best = null;
        NodeVersion bestVersion = null;
        for (String candidate : versions) {
            NodeVersion v = NodeVersion.parse(candidate);
            if (v == null || !r.test(v)) {
                continue;
            }
            if (bestVersion == null || v.compareTo(bestVersion) < 0) {
                best = candidate;
                bestVersion = v;
            }
        }
        return best;
    }

    /**
     * @return {@code true} if {@code version} is greater than every version {@code range} allows.
     */
    public static boolean gtr(String version, String range) {
        return outside(version, range, true);
    }

    /**
     * @return {@code true} if {@code version} is less than every version {@code range} allows.
     */
    public static boolean ltr(String version, String range) {
        return outside(version, range, false);
    }

    public static int compare(String v1, String v2) {
        NodeVersion a = NodeVersion.parse(v1);
        NodeVersion b = NodeVersion.parse(v2);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Invalid version: " + (a == null ? v1 : v2));
        }
        return a.compareTo(b);
    }

    // Port of node-semver functions/outside.js (gtr = hilo '>', ltr = hilo '<').
    private static boolean outside(String version, String range, boolean gtr) {
        NodeVersion v = NodeVersion.parse(version);
        NodeRange r = NodeRange.parse(range);
        if (v == null || r == null || r.test(v)) {
            return false;
        }
        NodeComparator.Op comp = gtr ? NodeComparator.Op.GT : NodeComparator.Op.LT;
        NodeComparator.Op ecomp = gtr ? NodeComparator.Op.GTE : NodeComparator.Op.LTE;
        for (List<NodeComparator> comparators : r.getSet()) {
            NodeComparator high = null;
            NodeComparator low = null;
            for (NodeComparator c0 : comparators) {
                NodeComparator c = c0.isAny() ? NodeComparator.GTE_ZERO : c0;
                if (high == null) {
                    high = c;
                    low = c;
                }
                int ch = c.getSemver().compareTo(high.getSemver());
                if (gtr ? ch > 0 : ch < 0) {
                    high = c;
                } else {
                    int cl = c.getSemver().compareTo(low.getSemver());
                    if (gtr ? cl < 0 : cl > 0) {
                        low = c;
                    }
                }
            }
            if (high.getOp() == comp || high.getOp() == ecomp) {
                return false;
            }
            int vLow = v.compareTo(low.getSemver());
            boolean ltefn = gtr ? vLow <= 0 : vLow >= 0;
            boolean ltfn = gtr ? vLow < 0 : vLow > 0;
            if ((low.getOp() == NodeComparator.Op.EQ || low.getOp() == comp) && ltefn) {
                return false;
            }
            if (low.getOp() == ecomp && ltfn) {
                return false;
            }
        }
        return true;
    }
}
