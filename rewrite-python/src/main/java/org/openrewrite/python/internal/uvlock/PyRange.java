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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;

import java.util.Objects;

/**
 * The lock's requires-python interval; upper is null when unbounded.
 */
final class PyRange {
    @Nullable PythonVersion lower;
    boolean lowerExclusive;
    @Nullable PythonVersion upper;
    boolean upperInclusive;

    boolean sameBounds(PyRange other) {
        return Objects.equals(lower, other.lower) && lowerExclusive == other.lowerExclusive &&
                Objects.equals(upper, other.upper) && upperInclusive == other.upperInclusive;
    }

    static PyRange parse(String requiresPython) {
        PyRange range = new PyRange();
        PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requiresPython);
        if (set == null) {
            return range;
        }
        for (PythonVersionSpecifier spec : set.getSpecifiers()) {
            String op = spec.getOperator();
            String version = spec.getVersion();
            boolean wildcard = version.endsWith(".*");
            PythonVersion v = PythonVersion.parse(wildcard ? version.substring(0, version.length() - 2) : version);
            if (v == null) {
                continue;
            }
            if (">=".equals(op) || ">".equals(op)) {
                if (range.lower == null || v.compareTo(range.lower) > 0) {
                    range.lower = v;
                    range.lowerExclusive = ">".equals(op);
                }
            } else if ("<".equals(op) || "<=".equals(op)) {
                if (range.upper == null || v.compareTo(range.upper) < 0) {
                    range.upper = v;
                    range.upperInclusive = "<=".equals(op);
                }
            } else if ("~=".equals(op) || ("==".equals(op) && wildcard)) {
                if (range.lower == null || v.compareTo(range.lower) > 0) {
                    range.lower = v;
                    range.lowerExclusive = false;
                }
                // ~=X.Y admits up to X+1, ==X.Y.* only up to X.(Y+1)
                PythonVersion next = wildcard ? bumpLastRelease(v) : bumpTrailingRelease(v);
                if (next != null && (range.upper == null || next.compareTo(range.upper) < 0)) {
                    range.upper = next;
                    range.upperInclusive = false;
                }
            } else if ("==".equals(op)) {
                range.lower = v;
                range.lowerExclusive = false;
                range.upper = v;
                range.upperInclusive = true;
            }
        }
        return range;
    }

    static @Nullable PythonVersion bumpTrailingRelease(PythonVersion v) {
        long[] release = v.getRelease();
        if (release.length < 2) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < release.length - 1; i++) {
            if (i > 0) {
                b.append('.');
            }
            b.append(i == release.length - 2 ? release[i] + 1 : release[i]);
        }
        return PythonVersion.parse(b.toString());
    }

    static @Nullable PythonVersion bumpLastRelease(PythonVersion v) {
        long[] release = v.getRelease();
        if (release.length == 0) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < release.length; i++) {
            if (i > 0) {
                b.append('.');
            }
            b.append(i == release.length - 1 ? release[i] + 1 : release[i]);
        }
        return PythonVersion.parse(b.toString());
    }

    /** The versions in both ranges, or null when there are none. */
    @Nullable PyRange intersect(PyRange other) {
        PyRange r = new PyRange();
        if (lower == null || (other.lower != null && other.lower.compareTo(lower) > 0)) {
            r.lower = other.lower;
            r.lowerExclusive = other.lowerExclusive;
        } else {
            r.lower = lower;
            r.lowerExclusive = lowerExclusive || (other.lower != null && other.lower.compareTo(lower) == 0 && other.lowerExclusive);
        }
        if (upper == null || (other.upper != null && other.upper.compareTo(upper) < 0)) {
            r.upper = other.upper;
            r.upperInclusive = other.upperInclusive;
        } else {
            r.upper = upper;
            r.upperInclusive = upperInclusive && (other.upper == null || other.upper.compareTo(upper) != 0 || other.upperInclusive);
        }
        if (r.lower != null && r.upper != null) {
            int cmp = r.lower.compareTo(r.upper);
            if (cmp > 0 || (cmp == 0 && (r.lowerExclusive || !r.upperInclusive))) {
                return null;
            }
        }
        return r;
    }

    boolean withinUpperBound(PythonVersion version) {
        if (upper == null) {
            return true;
        }
        int cmp = version.compareTo(upper);
        return cmp < 0 || (cmp == 0 && upperInclusive);
    }

    String describe() {
        return lower != null ? ">=" + lower : "(unbounded)";
    }
}
