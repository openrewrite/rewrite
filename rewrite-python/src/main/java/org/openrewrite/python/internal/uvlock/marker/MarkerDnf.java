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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Operator;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Specifier;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner.Edge;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner.Edges;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner.Node;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner.Variable;

import java.util.*;

import static org.openrewrite.python.internal.uvlock.marker.MarkerExpression.onlyReleaseTrimmed;
import static org.openrewrite.python.internal.uvlock.marker.MarkerExpression.releaseVersion;
import static org.openrewrite.python.internal.uvlock.marker.MarkerInterner.*;

/**
 * A marker tree rendered as the disjunctive normal form uv prints, ported from uv-pep508
 * {@code marker/simplify.rs} and {@code marker/algebra/isop.rs} (uv 0.13.4). The clauses come from a
 * sum-of-products cover the decision diagram yields directly.
 */
final class MarkerDnf {

    private final MarkerInterner interner;

    private MarkerDnf(MarkerInterner interner) {
        this.interner = interner;
    }

    static List<List<MarkerExpression>> toDnf(MarkerInterner interner, int tree) {
        MarkerDnf dnf = new MarkerDnf(interner);
        Cover cover = dnf.isop(tree, tree, new HashMap<>()).getCover();
        List<List<MarkerExpression>> out = new ArrayList<>();
        dnf.collectCover(cover, out, new ArrayList<>());
        sort(out);
        return out;
    }

    // ---- the cover (isop.rs)

    /** A shared cover, expanded into clauses only when rendering. */
    @Value
    static class Cover {
        enum Kind { FALSE, TRUE, GUARD, BRANCHES }

        Kind kind;
        int guardNode;
        @Nullable List<Branch> branches;

        static final Cover FALSE_COVER = new Cover(Kind.FALSE, 0, null);
        static final Cover TRUE_COVER = new Cover(Kind.TRUE, 0, null);
    }

    /** A single-variable condition and the cover that follows it. */
    @Value
    static class Branch {
        Guard guard;
        Cover suffix;
    }

    /** A single-variable condition, either an interned node or the edges of a decision that lead to one child. */
    @Value
    static class Guard {
        int node;
        int child;
        boolean edge;

        static Guard node(int node) {
            return new Guard(node, 0, false);
        }

        static Guard edge(int node, int child) {
            return new Guard(node, child, true);
        }
    }

    @Value
    private static class Result {
        int function;
        Cover cover;
    }

    @Value
    private static class Cofactor {
        Guard guard;
        int lower;
        int upper;
    }

    // a cover and its function I, where lower => I => upper, so covered cases are don't-cares further down
    private Result isop(int lower, int upper, Map<Long, Result> cache) {
        if (lower == FALSE) {
            return new Result(FALSE, Cover.FALSE_COVER);
        }
        if (upper == TRUE) {
            return new Result(TRUE, Cover.TRUE_COVER);
        }
        long key = ((long) lower << 32) | (upper & 0xffffffffL);
        Result cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (lower == upper) {
            boolean terminalChildren = true;
            for (int child : interner.node(lower).getChildren().nodes()) {
                terminalChildren &= child == TRUE || child == FALSE;
            }
            if (terminalChildren) {
                return new Result(lower, new Cover(Cover.Kind.GUARD, lower, null));
            }
        }
        boolean exact = lower == upper;
        List<Cofactor> cofactors = coverCofactors(lower, upper);
        int commonUpper = TRUE;
        for (Cofactor c : cofactors) {
            commonUpper = interner.and(commonUpper, c.getUpper());
        }
        int commonLower = FALSE;
        int function = FALSE;
        LinkedHashMap<Integer, Branch> branches = new LinkedHashMap<>();
        for (Cofactor c : cofactors) {
            int required = interner.and(c.getLower(), not(commonUpper));
            Result specific = isop(required, c.getUpper(), cache);
            int remaining = interner.and(c.getLower(), not(specific.getFunction()));
            commonLower = interner.or(commonLower, remaining);
            if (specific.getFunction() == FALSE) {
                continue;
            }
            if (!exact) {
                int condition = guardNode(c.getGuard());
                function = interner.or(function, interner.and(condition, specific.getFunction()));
            }
            Branch existing = branches.get(specific.getFunction());
            if (existing == null) {
                branches.put(specific.getFunction(), new Branch(c.getGuard(), specific.getCover()));
            } else {
                int merged = interner.or(guardNode(existing.getGuard()), guardNode(c.getGuard()));
                branches.put(specific.getFunction(), new Branch(Guard.node(merged), existing.getSuffix()));
            }
        }
        Result common = isop(commonLower, commonUpper, cache);
        function = exact ? lower : interner.or(function, common.getFunction());
        if (common.getFunction() != FALSE) {
            // an existing entry keeps its position, as IndexMap::insert does
            branches.put(common.getFunction(), new Branch(Guard.node(TRUE), common.getCover()));
        }
        Result result = new Result(function, new Cover(Cover.Kind.BRANCHES, 0, new ArrayList<>(branches.values())));
        cache.put(key, result);
        return result;
    }

    private int guardNode(Guard guard) {
        if (!guard.isEdge()) {
            return guard.getNode();
        }
        Node decision = interner.node(guard.getNode());
        Edges children = decision.getChildren();
        Edges edges;
        switch (children.getKind()) {
            case BOOLEAN:
                edges = Edges.fromBool(negate(children.getHigh(), guard.getNode()) == guard.getChild());
                break;
            case VERSION:
                edges = Edges.version(fromRange(guardRange(children.getVersions(), guard.getNode(), guard.getChild())));
                break;
            default:
                edges = Edges.string(fromRange(guardRange(children.getStrings(), guard.getNode(), guard.getChild())));
        }
        return interner.createNode(decision.getVar(), edges);
    }

    private List<Cofactor> coverCofactors(int lower, int upper) {
        if (lower == upper) {
            Node node = interner.node(lower);
            Edges children = node.getChildren();
            List<Cofactor> out = new ArrayList<>();
            if (children.getKind() == Edges.Kind.BOOLEAN) {
                for (int child : new int[]{children.getLow(), children.getHigh()}) {
                    int c = negate(child, lower);
                    out.add(new Cofactor(Guard.edge(lower, c), c, c));
                }
                return out;
            }
            Set<Integer> seen = new HashSet<>();
            for (int child : children.nodes()) {
                int c = negate(child, lower);
                if (seen.add(c)) {
                    out.add(new Cofactor(Guard.edge(lower, c), c, c));
                }
            }
            return out;
        }
        Node lowerNode = lower == TRUE || lower == FALSE ? null : interner.node(lower);
        Node upperNode = upper == TRUE || upper == FALSE ? null : interner.node(upper);
        Variable variable;
        Node first;
        if (lowerNode == null) {
            variable = upperNode.getVar();
            first = upperNode;
        } else if (upperNode == null) {
            variable = lowerNode.getVar();
            first = lowerNode;
        } else if (lowerNode.getVar().compareTo(upperNode.getVar()) <= 0) {
            variable = lowerNode.getVar();
            first = lowerNode;
        } else {
            variable = upperNode.getVar();
            first = upperNode;
        }
        boolean lowerHas = lowerNode != null && lowerNode.getVar().equals(variable);
        boolean upperHas = upperNode != null && upperNode.getVar().equals(variable);
        List<Cofactor> out = new ArrayList<>();
        switch (first.getChildren().getKind()) {
            case BOOLEAN: {
                int lowerLow = lowerHas ? negate(lowerNode.getChildren().getLow(), lower) : lower;
                int lowerHigh = lowerHas ? negate(lowerNode.getChildren().getHigh(), lower) : lower;
                int upperLow = upperHas ? negate(upperNode.getChildren().getLow(), upper) : upper;
                int upperHigh = upperHas ? negate(upperNode.getChildren().getHigh(), upper) : upper;
                int guard = interner.createNode(variable, Edges.fromBool(true));
                out.add(new Cofactor(Guard.node(not(guard)), lowerLow, upperLow));
                out.add(new Cofactor(Guard.node(guard), lowerHigh, upperHigh));
                return out;
            }
            case VERSION: {
                for (Region<PythonVersion> r : rangeCofactors(lower, lowerHas ? lowerNode.getChildren().getVersions()
                        : null,
                        upper, upperHas ? upperNode.getChildren().getVersions() : null)) {
                    if (r.lower == FALSE) {
                        out.add(new Cofactor(Guard.node(FALSE), r.lower, r.upper));
                        continue;
                    }
                    int guard = interner.createNode(variable, Edges.version(fromRange(r.range)));
                    out.add(new Cofactor(Guard.node(guard), r.lower, r.upper));
                }
                return out;
            }
            default: {
                for (Region<String> r : rangeCofactors(lower, lowerHas ? lowerNode.getChildren().getStrings() : null,
                        upper, upperHas ? upperNode.getChildren().getStrings() : null)) {
                    if (r.lower == FALSE) {
                        out.add(new Cofactor(Guard.node(FALSE), r.lower, r.upper));
                        continue;
                    }
                    int guard = interner.createNode(variable, Edges.string(fromRange(r.range)));
                    out.add(new Cofactor(Guard.node(guard), r.lower, r.upper));
                }
                return out;
            }
        }
    }

    private static final class Region<T extends Comparable<T>> {
        Ranges<T> range;
        final int lower;
        final int upper;

        Region(Ranges<T> range, int lower, int upper) {
            this.range = range;
            this.lower = lower;
            this.upper = upper;
        }
    }

    // intersect two sorted partitions, grouping the regions whose cofactors match
    private static <T extends Comparable<T>> List<Region<T>> rangeCofactors(
            int lower, @Nullable List<Edge<T>> lowerEdges, int upper, @Nullable List<Edge<T>> upperEdges) {
        List<Edge<T>> left = lowerEdges != null ? lowerEdges :
                Collections.singletonList(new Edge<>(Ranges.<T>full(), lower));
        int leftParent = lowerEdges != null ? lower : TRUE;
        List<Edge<T>> right = upperEdges != null ? upperEdges :
                Collections.singletonList(new Edge<>(Ranges.<T>full(), upper));
        int rightParent = upperEdges != null ? upper : TRUE;
        LinkedHashMap<Long, Region<T>> regions = new LinkedHashMap<>();
        int l = 0;
        int r = 0;
        while (l < left.size() && r < right.size()) {
            Edge<T> le = left.get(l);
            Edge<T> re = right.get(r);
            Ranges<T> intersection = le.getRange().intersection(re.getRange());
            if (!intersection.isEmpty()) {
                int lc = negate(le.getNode(), leftParent);
                int rc = negate(re.getNode(), rightParent);
                long key = ((long) lc << 32) | (rc & 0xffffffffL);
                Region<T> existing = regions.get(key);
                if (existing != null) {
                    existing.range = existing.range.union(intersection);
                } else {
                    regions.put(key, new Region<>(intersection, lc, rc));
                }
            }
            Ranges.Segment<T> lb = le.getRange().boundingRange();
            Ranges.Segment<T> rb = re.getRange().boundingRange();
            if (lb == null) {
                l++;
            } else if (rb == null) {
                r++;
            } else {
                int cmp = Ranges.compareEnds(lb.getEnd(), rb.getEnd());
                if (cmp < 0) {
                    l++;
                } else if (cmp > 0) {
                    r++;
                } else {
                    l++;
                    r++;
                }
            }
        }
        return new ArrayList<>(regions.values());
    }

    // ---- rendering the cover (simplify.rs)

    private void collectCover(Cover cover, List<List<MarkerExpression>> dnf, List<MarkerExpression> path) {
        switch (cover.getKind()) {
            case FALSE:
                return;
            case TRUE:
                if (!path.isEmpty()) {
                    dnf.add(new ArrayList<>(path));
                }
                return;
            case GUARD:
                collectGuard(Guard.node(cover.getGuardNode()), Cover.TRUE_COVER, dnf, path);
                return;
            default:
                for (Branch b : cover.getBranches()) {
                    collectGuard(b.getGuard(), b.getSuffix(), dnf, path);
                }
        }
    }

    private void collectGuard(Guard guard, Cover suffix, List<List<MarkerExpression>> dnf,
            List<MarkerExpression> path) {
        int tree = guard.getNode();
        int child = guard.isEdge() ? guard.getChild() : TRUE;
        if (tree == FALSE) {
            return;
        }
        if (tree == TRUE) {
            collectCover(suffix, dnf, path);
            return;
        }
        Node node = interner.node(tree);
        Variable var = node.getVar();
        Edges edges = node.getChildren();
        int size = path.size();
        switch (var.getKind()) {
            case VERSION: {
                Ranges<PythonVersion> range = guardRange(edges.getVersions(), tree, child);
                MarkerExpression.VersionKey key = var.getVersion().key();
                List<PythonVersion> excluded = rangeInequality(range);
                if (excluded != null) {
                    for (PythonVersion v : excluded) {
                        path.add(new MarkerExpression.Version(key, new Specifier(Specifier.Op.NOT_EQUAL, v)));
                    }
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                    return;
                }
                Specifier star = starRangeSpecifier(range);
                if (star != null) {
                    path.add(new MarkerExpression.Version(key, star));
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                    return;
                }
                for (Ranges.Segment<PythonVersion> bounds : range.segments()) {
                    for (Specifier s : fromReleaseOnlyBounds(bounds)) {
                        path.add(new MarkerExpression.Version(key, s));
                    }
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                }
                return;
            }
            case VERSION_STRING: {
                Ranges<PythonVersion> range = guardRange(edges.getVersions(), tree, child);
                for (Ranges.Segment<PythonVersion> s : range.segments()) {
                    for (Object[] opValue : fromBounds(
                            mapBound(s.getStart()), mapBound(s.getEnd()))) {
                        path.add(new MarkerExpression.Str(var.getString().key(), (Operator) opValue[0],
                                (String) opValue[1]));
                    }
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                }
                return;
            }
            case STRING: {
                Ranges<String> range = guardRange(edges.getStrings(), tree, child);
                List<String> excluded = rangeInequality(range);
                if (excluded != null) {
                    for (String v : excluded) {
                        path.add(new MarkerExpression.Str(var.getString().key(), Operator.NOT_EQUAL, v));
                    }
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                    return;
                }
                for (Ranges.Segment<String> s : range.segments()) {
                    for (Object[] opValue : fromBounds(s.getStart(), s.getEnd())) {
                        path.add(new MarkerExpression.Str(var.getString().key(), (Operator) opValue[0],
                                (String) opValue[1]));
                    }
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                }
                return;
            }
            default:
                // the boolean variables (in, contains, extra and list membership) render their high edge first
                for (boolean value : new boolean[]{true, false}) {
                    int edgeChild = negate(value ? edges.getHigh() : edges.getLow(), tree);
                    if (edgeChild != child) {
                        continue;
                    }
                    path.add(booleanExpression(var, value));
                    collectCover(suffix, dnf, path);
                    truncate(path, size);
                }
        }
    }

    private static MarkerExpression booleanExpression(Variable var, boolean value) {
        switch (var.getKind()) {
            case IN:
                return new MarkerExpression.Str(var.getString().key(), value ? Operator.IN : Operator.NOT_IN,
                        var.getValue());
            case CONTAINS:
                return new MarkerExpression.Str(var.getString().key(), value ? Operator.CONTAINS
                        : Operator.NOT_CONTAINS, var.getValue());
            case LIST:
                return new MarkerExpression.ListPair(var.getListKind(), var.getListKey(), var.getValue(), !value);
            default:
                return new MarkerExpression.Extra(var.getValue(), true, !value);
        }
    }

    private static Ranges.Bound<String> mapBound(Ranges.Bound<PythonVersion> b) {
        if (b.isUnbounded()) {
            return Ranges.Bound.unbounded();
        }
        return b.isIncluded() ? Ranges.Bound.included(b.getValue().toString())
                : Ranges.Bound.excluded(b.getValue().toString());
    }

    private static void truncate(List<MarkerExpression> path, int size) {
        while (path.size() > size) {
            path.remove(path.size() - 1);
        }
    }

    private static <T extends Comparable<T>> Ranges<T> guardRange(List<Edge<T>> edges, int parent, int child) {
        Ranges<T> range = Ranges.empty();
        for (Edge<T> e : edges) {
            if (negate(e.getNode(), parent) == child) {
                range = range.union(e.getRange());
            }
        }
        return range;
    }

    // MarkerOperator::from_bounds
    private static List<Object[]> fromBounds(Ranges.Bound<String> lower, Ranges.Bound<String> upper) {
        List<Object[]> out = new ArrayList<>();
        if (lower.isIncluded() && upper.isIncluded() && lower.getValue().equals(upper.getValue())) {
            out.add(new Object[]{Operator.EQUAL, lower.getValue()});
            return out;
        }
        if (lower.isExcluded() && upper.isExcluded() && lower.getValue().equals(upper.getValue())) {
            out.add(new Object[]{Operator.NOT_EQUAL, lower.getValue()});
            return out;
        }
        if (!lower.isUnbounded()) {
            out.add(new Object[]{lower.isIncluded() ? Operator.GREATER_EQUAL : Operator.GREATER_THAN,
                    lower.getValue()});
        }
        if (!upper.isUnbounded()) {
            out.add(new Object[]{upper.isIncluded() ? Operator.LESS_EQUAL : Operator.LESS_THAN, upper.getValue()});
        }
        return out;
    }

    /** The excluded values when the range is everything but a few points, as in {@code os_name != 'nt'}. */
    private static <T extends Comparable<T>> @Nullable List<T> rangeInequality(Ranges<T> range) {
        if (range.isEmpty()) {
            return null;
        }
        Ranges.Segment<T> bounding = range.boundingRange();
        if (!bounding.getStart().isUnbounded() || !bounding.getEnd().isUnbounded()) {
            return null;
        }
        List<T> excluded = new ArrayList<>();
        List<Ranges.Segment<T>> segments = range.segments();
        for (int i = 0; i + 1 < segments.size(); i++) {
            Ranges.Bound<T> end = segments.get(i).getEnd();
            Ranges.Bound<T> start = segments.get(i + 1).getStart();
            if (end.isExcluded() && start.isExcluded() && end.getValue().compareTo(start.getValue()) == 0) {
                excluded.add(end.getValue());
            } else {
                return null;
            }
        }
        return excluded;
    }

    // the two-segment case of != X.Y.* that from_release_only_bounds does not cover
    private static @Nullable Specifier starRangeSpecifier(Ranges<PythonVersion> range) {
        List<Ranges.Segment<PythonVersion>> segments = range.segments();
        if (segments.size() != 2) {
            return null;
        }
        Ranges.Segment<PythonVersion> b1 = segments.get(0);
        Ranges.Segment<PythonVersion> b2 = segments.get(1);
        if (!(b1.getStart().isUnbounded() && b1.getEnd().isExcluded() && b2.getStart().isIncluded()
                && b2.getEnd().isUnbounded())) {
            return null;
        }
        long[] v1 = onlyReleaseTrimmed(b1.getEnd().getValue()).getRelease();
        long[] v2 = b2.getStart().getValue().getRelease();
        if (v1.length == 1 && v2.length == 2 && v2[0] == v1[0] && v2[1] == 1) {
            return new Specifier(Specifier.Op.NOT_EQUAL_STAR, releaseVersion(new long[]{v1[0], 0}, 2));
        }
        if (v1.length == 2 && v2.length == 2 && v2[0] == v1[0] && v2[1] == v1[1] + 1) {
            return new Specifier(Specifier.Op.NOT_EQUAL_STAR, b1.getEnd().getValue());
        }
        return null;
    }

    // VersionSpecifier::from_release_only_bounds for one segment
    private static List<Specifier> fromReleaseOnlyBounds(Ranges.Segment<PythonVersion> bounds) {
        List<Specifier> out = new ArrayList<>();
        Ranges.Bound<PythonVersion> lo = bounds.getStart();
        Ranges.Bound<PythonVersion> hi = bounds.getEnd();
        if (lo.isIncluded() && hi.isIncluded() && lo.getValue().equals(hi.getValue())) {
            out.add(new Specifier(Specifier.Op.EQUAL, lo.getValue()));
            return out;
        }
        if (lo.isIncluded() && hi.isExcluded()) {
            long[] v1 = onlyReleaseTrimmed(lo.getValue()).getRelease();
            long[] v2 = onlyReleaseTrimmed(hi.getValue()).getRelease();
            if (v1.length == 1 && v2.length == 2 && v2[0] == v1[0] && v2[1] == 1) {
                out.add(new Specifier(Specifier.Op.EQUAL_STAR, releaseVersion(new long[]{v1[0], 0}, 2)));
                return out;
            }
            if (v1.length == 2 && v2.length == 2 && v2[0] == v1[0] && v2[1] == v1[1] + 1) {
                out.add(new Specifier(Specifier.Op.EQUAL_STAR, releaseVersion(v1, 2)));
                return out;
            }
        }
        if (!lo.isUnbounded()) {
            out.add(new Specifier(lo.isIncluded() ? Specifier.Op.GREATER_THAN_EQUAL : Specifier.Op.GREATER_THAN,
                    lo.getValue()));
        }
        if (!hi.isUnbounded()) {
            out.add(new Specifier(hi.isIncluded() ? Specifier.Op.LESS_THAN_EQUAL : Specifier.Op.LESS_THAN,
                    hi.getValue()));
        }
        return out;
    }

    // clauses by expression kind, then the clauses themselves by their kinds, as uv sorts for stable output
    private static void sort(List<List<MarkerExpression>> dnf) {
        for (List<MarkerExpression> clause : dnf) {
            clause.sort(Comparator.comparing(MarkerDnf::kind));
        }
        dnf.sort((a, b) -> {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int c = kind(a.get(i)).compareTo(kind(b.get(i)));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(a.size(), b.size());
        });
    }

    private static Integer kind(MarkerExpression e) {
        if (e instanceof MarkerExpression.Version) {
            return ((MarkerExpression.Version) e).getKey().ordinal();
        }
        if (e instanceof MarkerExpression.VersionIn) {
            return 100 + ((MarkerExpression.VersionIn) e).getKey().ordinal();
        }
        if (e instanceof MarkerExpression.Str) {
            return 200 + ((MarkerExpression.Str) e).getKey().ordinal();
        }
        if (e instanceof MarkerExpression.ListPair) {
            return 300 + ((MarkerExpression.ListPair) e).getKey().ordinal();
        }
        return 400;
    }
}
