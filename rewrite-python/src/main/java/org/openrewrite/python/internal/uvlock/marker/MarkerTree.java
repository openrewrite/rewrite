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
package org.openrewrite.python.internal.uvlock.marker;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;

import java.util.List;
import java.util.function.Predicate;

/**
 * An immutable marker, as uv-pep508's {@code MarkerTree} in {@code marker/tree.rs} (uv 0.13.4). It is a node
 * of its interner's decision diagram, so two equal markers from one interner are the same tree.
 */
public final class MarkerTree {

    private final MarkerInterner interner;
    private final int id;

    MarkerTree(MarkerInterner interner, int id) {
        this.interner = interner;
        this.id = id;
    }

    /** The always-true marker of an interner, the starting point for parsing and combining. */
    public static MarkerTree trueIn(MarkerInterner interner) {
        return new MarkerTree(interner, MarkerInterner.TRUE);
    }

    public static MarkerTree parse(MarkerInterner interner, String marker) throws MarkerParser.MarkerSyntaxException {
        return new MarkerTree(interner, new MarkerParser(interner, marker).parse());
    }

    // node ids mean something only in the interner that made them
    private int sameInterner(MarkerTree other) {
        if (other.interner != interner) {
            throw new IllegalArgumentException("Markers from different interners cannot be combined");
        }
        return other.id;
    }

    int id() {
        return id;
    }

    MarkerInterner interner() {
        return interner;
    }

    public boolean isTrue() {
        return id == MarkerInterner.TRUE;
    }

    public boolean isFalse() {
        return id == MarkerInterner.FALSE;
    }

    public MarkerTree negate() {
        return new MarkerTree(interner, MarkerInterner.not(id));
    }

    public MarkerTree and(MarkerTree other) {
        return new MarkerTree(interner, interner.and(id, sameInterner(other)));
    }

    public MarkerTree or(MarkerTree other) {
        return new MarkerTree(interner, interner.or(id, sameInterner(other)));
    }

    public MarkerTree implies(MarkerTree consequent) {
        return negate().or(consequent);
    }

    /** Whether no environment satisfies both markers. */
    public boolean isDisjoint(MarkerTree other) {
        return interner.isDisjoint(id, sameInterner(other));
    }

    /** Whether the marker is unsatisfiable, counting pairs of values known never to hold together. */
    public boolean isUnreachable() {
        return interner.isUnreachable(id);
    }

    /** The marker simplified on the assumption that {@code python_full_version} lies within the bounds. */
    public MarkerTree simplifyPythonVersions(Ranges.Bound<PythonVersion> lower, Ranges.Bound<PythonVersion> upper) {
        return new MarkerTree(interner, interner.simplifyPythonVersions(id, lower, upper));
    }

    /** The marker conjoined with the Python version bounds. */
    public MarkerTree complexifyPythonVersions(Ranges.Bound<PythonVersion> lower, Ranges.Bound<PythonVersion> upper) {
        return new MarkerTree(interner, interner.complexifyPythonVersions(id, lower, upper));
    }

    /** This marker wherever {@code assumption} holds, simplified by leaving its value elsewhere open. */
    public MarkerTree restrict(MarkerTree assumption) {
        return new MarkerTree(interner, interner.restrict(id, sameInterner(assumption)));
    }

    /** The marker with {@code extra == x} taken as true for every extra the predicate accepts. */
    public MarkerTree simplifyExtrasWith(Predicate<String> isExtra) {
        return new MarkerTree(interner, interner.restrictBy(id, v ->
                v.getKind() == MarkerInterner.Variable.Kind.EXTRA && isExtra.test(v.getValue()) ? Boolean.TRUE : null));
    }

    /** The marker with {@code extra == x} taken as false for every extra the predicate accepts. */
    public MarkerTree simplifyNotExtrasWith(Predicate<String> isExtra) {
        return new MarkerTree(interner, interner.restrictBy(id, v ->
                v.getKind() == MarkerInterner.Variable.Kind.EXTRA && isExtra.test(v.getValue()) ? Boolean.FALSE
                        : null));
    }

    public MarkerTree withoutExtras() {
        return new MarkerTree(interner, interner.withoutExtras(id));
    }

    public MarkerTree onlyExtras() {
        return new MarkerTree(interner, interner.onlyExtras(id));
    }

    public List<List<MarkerExpression>> toDnf() {
        return MarkerDnf.toDnf(interner, id);
    }

    /** The marker as uv writes it, or null for the always-true marker. */
    public @Nullable String tryToString() {
        if (isTrue()) {
            return null;
        }
        if (isFalse()) {
            return "python_version < '0'";
        }
        List<List<MarkerExpression>> dnf = toDnf();
        if (dnf.size() == 1) {
            return join(dnf.get(0));
        }
        StringBuilder b = new StringBuilder();
        for (List<MarkerExpression> conjunction : dnf) {
            if (b.length() > 0) {
                b.append(" or ");
            }
            b.append(conjunction.size() == 1 ? join(conjunction) : "(" + join(conjunction) + ")");
        }
        return b.toString();
    }

    private static String join(List<MarkerExpression> conjunction) {
        StringBuilder b = new StringBuilder();
        for (MarkerExpression e : conjunction) {
            if (b.length() > 0) {
                b.append(" and ");
            }
            b.append(e);
        }
        return b.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof MarkerTree && ((MarkerTree) o).interner == interner
                && ((MarkerTree) o).id == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        String s = tryToString();
        return s == null ? "true" : s;
    }
}
