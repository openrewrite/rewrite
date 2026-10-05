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
package org.openrewrite.python.internal.pep508;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.openrewrite.python.internal.pep508.Pep508Parser.Comparison;
import static org.openrewrite.python.internal.pep508.Pep508Parser.Operand;

/**
 * A parsed PEP 508 environment marker, ported from pypa/packaging's {@code markers.Marker}
 * with three-valued evaluation: comparisons against unknown environment variables yield
 * null, {@code false and unknown} is false, {@code true or unknown} is true, and unknown
 * propagates otherwise. {@link #toString()} re-emits the pipenv-normalized single-quoted form.
 */
public class Marker {
    // Keys whose comparisons are attempted as PEP 440 specifier containment first.
    private static final Set<String> VERSION_KEYS = new HashSet<>(Arrays.asList(
            "implementation_version", "platform_release", "python_full_version", "python_version"));

    private static final Set<String> SET_VALUED_KEYS = new HashSet<>(Arrays.asList(
            "extras", "dependency_groups"));

    // Nested list structure per packaging: Comparison | "and" | "or" | List.
    private final List<Object> markers;

    Marker(List<Object> markers) {
        this.markers = normalizeExtraValues(markers);
    }

    public static @Nullable Marker parse(@Nullable String marker) {
        if (marker == null) {
            return null;
        }
        try {
            return new Marker(Pep508Parser.parseMarker(marker));
        } catch (Pep508Parser.SyntaxException e) {
            return null;
        }
    }

    /**
     * Evaluates against the environment; null when the result depends on an unknown variable.
     */
    public @Nullable Boolean evaluate(MarkerEnvironment environment) {
        return evaluateList(markers, environment);
    }

    private static @Nullable Boolean evaluateList(List<Object> markers, MarkerEnvironment env) {
        // Groups of and-ed items, or-ed together, as in packaging's _evaluate_markers.
        List<List<@Nullable Boolean>> groups = new ArrayList<>();
        groups.add(new ArrayList<>());
        for (Object marker : markers) {
            if (marker instanceof List) {
                //noinspection unchecked
                groups.get(groups.size() - 1).add(evaluateList((List<Object>) marker, env));
            } else if (marker instanceof Comparison) {
                groups.get(groups.size() - 1).add(evaluateComparison((Comparison) marker, env));
            } else if ("or".equals(marker)) {
                groups.add(new ArrayList<>());
            }
        }
        Boolean result = false;
        for (List<@Nullable Boolean> group : groups) {
            result = or3(result, and3(group));
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        return result;
    }

    private static @Nullable Boolean and3(List<@Nullable Boolean> items) {
        Boolean result = true;
        for (Boolean item : items) {
            if (Boolean.FALSE.equals(item)) {
                return false;
            }
            if (item == null) {
                result = null;
            }
        }
        return result;
    }

    private static @Nullable Boolean or3(@Nullable Boolean a, @Nullable Boolean b) {
        if (Boolean.TRUE.equals(a) || Boolean.TRUE.equals(b)) {
            return true;
        }
        if (a == null || b == null) {
            return null;
        }
        return false;
    }

    private static @Nullable Boolean evaluateComparison(Comparison comparison, MarkerEnvironment env) {
        String key;
        String lhs;
        String rhs;
        if (comparison.lhs.variable) {
            key = comparison.lhs.value;
            lhs = env.get(key);
            rhs = comparison.rhs.value;
            if (lhs == null) {
                return null;
            }
        } else {
            lhs = comparison.lhs.value;
            key = comparison.rhs.value;
            rhs = env.get(key);
            if (rhs == null) {
                return null;
            }
        }
        if (SET_VALUED_KEYS.contains(key)) {
            // extras/dependency_groups sets are not modeled in the lock environment.
            return null;
        }
        if ("extra".equals(key)) {
            lhs = Pep508Requirement.canonicalize(lhs);
            rhs = Pep508Requirement.canonicalize(rhs);
        }
        return evalOp(lhs, comparison.op, rhs, key);
    }

    // Port of packaging's _eval_op; undefined comparisons yield unknown instead of raising.
    private static @Nullable Boolean evalOp(String lhs, String op, String rhs, String key) {
        if (VERSION_KEYS.contains(key)) {
            PythonVersionSpecifier spec = PythonVersionSpecifier.parse(op + rhs);
            if (spec != null) {
                if ("===".equals(spec.getOperator())) {
                    return lhs.toLowerCase(Locale.ROOT).equals(spec.getVersion().toLowerCase(Locale.ROOT));
                }
                PythonVersion version = PythonVersion.parse(lhs);
                return version != null && spec.contains(version);
            }
        }
        switch (op) {
            case "in":
                return rhs.contains(lhs);
            case "not in":
                return !rhs.contains(lhs);
            case "<":
            case ">":
                return false;
            case "<=":
            case ">=":
            case "==":
                return lhs.equals(rhs);
            case "!=":
                return !lhs.equals(rhs);
            default:
                return null;
        }
    }

    // Port of packaging's _normalize_extra_values: extra literals are PEP 503-normalized
    // at parse time so equal markers stringify identically.
    private static List<Object> normalizeExtraValues(List<Object> markers) {
        List<Object> result = new ArrayList<>(markers.size());
        for (Object marker : markers) {
            result.add(normalizeExtras(marker));
        }
        return result;
    }

    private static Object normalizeExtras(Object marker) {
        if (marker instanceof List) {
            //noinspection unchecked
            return normalizeExtraValues((List<Object>) marker);
        }
        if (!(marker instanceof Comparison)) {
            return marker;
        }
        Comparison c = (Comparison) marker;
        Operand lhs = c.lhs;
        Operand rhs = c.rhs;
        if (lhs.variable && "extra".equals(lhs.value) && !rhs.variable) {
            rhs = new Operand(Pep508Requirement.canonicalize(rhs.value), false);
        } else if (rhs.variable && "extra".equals(rhs.value) && !lhs.variable) {
            lhs = new Operand(Pep508Requirement.canonicalize(lhs.value), false);
        } else if (rhs.variable && SET_VALUED_KEYS.contains(rhs.value) && !lhs.variable) {
            lhs = new Operand(Pep508Requirement.canonicalize(lhs.value), false);
        }
        return lhs == c.lhs && rhs == c.rhs ? c : new Comparison(lhs, c.op, rhs);
    }

    /**
     * Re-emits the marker in normalized form with single-quoted values, matching pipenv's
     * lock file normalization.
     */
    @Override
    public String toString() {
        return format(markers, true);
    }

    private static String format(Object marker, boolean first) {
        if (marker instanceof List) {
            List<?> list = (List<?>) marker;
            // Unwrap a redundant [[...]] wrapper, preserving nesting context.
            if (list.size() == 1 && (list.get(0) instanceof List || list.get(0) instanceof Comparison)) {
                return format(list.get(0), first);
            }
            StringBuilder inner = new StringBuilder();
            for (Object item : list) {
                if (inner.length() > 0) {
                    inner.append(' ');
                }
                inner.append(format(item, false));
            }
            return first ? inner.toString() : "(" + inner + ")";
        }
        if (marker instanceof Comparison) {
            Comparison c = (Comparison) marker;
            return serialize(c.lhs) + " " + c.op + " " + serialize(c.rhs);
        }
        return (String) marker;
    }

    private static String serialize(Operand operand) {
        if (operand.variable) {
            return operand.value;
        }
        if (operand.value.indexOf('\'') >= 0) {
            if (operand.value.indexOf('"') >= 0) {
                // PEP 508 strings have no escape syntax; no quoting can represent this value
                throw new IllegalArgumentException(
                        "Marker value contains both quote characters and cannot be serialized: " + operand.value);
            }
            return "\"" + operand.value + "\"";
        }
        return "'" + operand.value + "'";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof Marker && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
