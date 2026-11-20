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
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.CanonicalString;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.CanonicalVersion;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.ListPair;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Operator;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Specifier;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.StringKey;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.VersionKey;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import static org.openrewrite.python.internal.uvlock.marker.MarkerExpression.onlyReleaseTrimmed;
import static org.openrewrite.python.internal.uvlock.marker.MarkerExpression.releaseVersion;

/**
 * Marker trees as reduced ordered algebraic decision diagrams with complemented edges, ported from uv-pep508
 * {@code marker/algebra.rs} (uv 0.13.4). Nodes are interned, so equal markers are the same node.
 * A node id's lowest bit marks a complement, and ids 0 and 1 are the constants true and false.
 * An interner is not thread-safe, so each thread builds markers in its own.
 */
public final class MarkerInterner {

    static final int TRUE = 0;
    static final int FALSE = 1;

    private final List<Node> nodes = new ArrayList<>();
    private final Map<Node, Integer> unique = new HashMap<>();
    private final Map<Long, Integer> andCache = new HashMap<>();
    private @Nullable Integer exclusions;

    /**
     * Whether a conjunction of values that never hold together is false, as it is in uv's algebra. An interner built
     * without it keeps such a conjunction. That tells a marker uv would rewrite from one only this pruning would
     * change, which a lock written before uv pruned them still carries.
     */
    private final boolean pruneIncompatibleValues;

    public MarkerInterner() {
        this(true);
    }

    public MarkerInterner(boolean pruneIncompatibleValues) {
        this.pruneIncompatibleValues = pruneIncompatibleValues;
    }

    // ---- node ids

    static int not(int id) {
        return id ^ 1;
    }

    static boolean isComplement(int id) {
        return (id & 1) == 1;
    }

    /** The child id as seen from a complemented parent. */
    static int negate(int child, int parent) {
        return isComplement(parent) ? not(child) : child;
    }

    private static int newId(int index, boolean complement) {
        return ((index + 1) << 1) | (complement ? 1 : 0);
    }

    private static int index(int id) {
        return (id >> 1) - 1;
    }

    static @Nullable Integer andTrivial(int x, int y) {
        if (x == TRUE) {
            return y;
        }
        if (y == TRUE) {
            return x;
        }
        if (x == y) {
            return x;
        }
        if (x == FALSE || y == FALSE || not(x) == y) {
            return FALSE;
        }
        return null;
    }

    Node node(int id) {
        return nodes.get(index(id));
    }

    // ---- variables, nodes and edges

    /**
     * A decision variable. The declaration order of {@link Kind}, and then of its fields, is the diagram's variable
     * order.
     */
    @Value
    static class Variable implements Comparable<Variable> {
        enum Kind { STRING, VERSION_STRING, VERSION, IN, CONTAINS, EXTRA, LIST }

        Kind kind;
        @Nullable CanonicalString string;
        @Nullable CanonicalVersion version;
        @Nullable String value;
        ListPair.@Nullable Kind listKind;
        MarkerExpression.@Nullable ListKey listKey;

        static Variable string(CanonicalString key) {
            return new Variable(Kind.STRING, key, null, null, null, null);
        }

        static Variable versionString(CanonicalString key) {
            return new Variable(Kind.VERSION_STRING, key, null, null, null, null);
        }

        static Variable version(CanonicalVersion key) {
            return new Variable(Kind.VERSION, null, key, null, null, null);
        }

        static Variable in(CanonicalString key, String value) {
            return new Variable(Kind.IN, key, null, value, null, null);
        }

        static Variable contains(CanonicalString key, String value) {
            return new Variable(Kind.CONTAINS, key, null, value, null, null);
        }

        static Variable extra(String name) {
            return new Variable(Kind.EXTRA, null, null, name, null, null);
        }

        static Variable list(ListPair.Kind kind, MarkerExpression.ListKey key, String value) {
            return new Variable(Kind.LIST, null, null, value, kind, key);
        }

        boolean isConflicting() {
            return kind == Kind.STRING && string != null && string.isConflicting();
        }

        @Override
        public int compareTo(Variable o) {
            int c = kind.compareTo(o.kind);
            if (c != 0) {
                return c;
            }
            switch (kind) {
                case STRING:
                case VERSION_STRING:
                    return string.compareTo(o.string);
                case VERSION:
                    return version.compareTo(o.version);
                case IN:
                case CONTAINS:
                    c = string.compareTo(o.string);
                    return c != 0 ? c : value.compareTo(o.value);
                case EXTRA:
                    return value.compareTo(o.value);
                default:
                    c = listKind.compareTo(o.listKind);
                    if (c != 0) {
                        return c;
                    }
                    if (listKind == ListPair.Kind.ARBITRARY) {
                        c = listKey.compareTo(o.listKey);
                        if (c != 0) {
                            return c;
                        }
                    }
                    return value.compareTo(o.value);
            }
        }
    }

    /** One edge of a version or string decision, with the values it covers and the child it leads to. */
    @Value
    static class Edge<T extends Comparable<T>> {
        Ranges<T> range;
        int node;
    }

    /**
     * The outgoing edges of a decision node. A range variable's edges are disjoint ranges covering its values, and a
     * boolean's are high and low.
     */
    @Value
    static class Edges {
        enum Kind { VERSION, STRING, BOOLEAN }

        Kind kind;
        @Nullable List<Edge<PythonVersion>> versions;
        @Nullable List<Edge<String>> strings;
        int high;
        int low;

        static Edges version(List<Edge<PythonVersion>> edges) {
            return new Edges(Kind.VERSION, edges, null, 0, 0);
        }

        static Edges string(List<Edge<String>> edges) {
            return new Edges(Kind.STRING, null, edges, 0, 0);
        }

        static Edges bool(int high, int low) {
            return new Edges(Kind.BOOLEAN, null, null, high, low);
        }

        static Edges fromBool(boolean complemented) {
            return complemented ? bool(TRUE, FALSE) : bool(FALSE, TRUE);
        }

        List<Integer> nodes() {
            List<Integer> out = new ArrayList<>();
            if (kind == Kind.BOOLEAN) {
                out.add(high);
                out.add(low);
            } else if (kind == Kind.VERSION) {
                for (Edge<PythonVersion> e : versions) {
                    out.add(e.getNode());
                }
            } else {
                for (Edge<String> e : strings) {
                    out.add(e.getNode());
                }
            }
            return out;
        }

        Edges not() {
            return map(TRUE, MarkerInterner::not, false);
        }

        /**
         * The children mapped by {@code f} after restoring their sign under {@code parent}, with equal neighbours
         * coalesced.
         */
        Edges map(int parent, IntUnaryOperator f) {
            return map(parent, f, true);
        }

        private Edges map(int parent, IntUnaryOperator f, boolean coalesce) {
            switch (kind) {
                case BOOLEAN:
                    int low2 = f.applyAsInt(negate(low, parent));
                    int high2 = f.applyAsInt(negate(high, parent));
                    return bool(high2, low2);
                case VERSION:
                    return version(mapRanges(versions, parent, f, coalesce));
                default:
                    return string(mapRanges(strings, parent, f, coalesce));
            }
        }

        private static <T extends Comparable<T>> List<Edge<T>> mapRanges(List<Edge<T>> edges, int parent,
                                                                         IntUnaryOperator f, boolean coalesce) {
            List<Edge<T>> mapped = new ArrayList<>(edges.size());
            for (Edge<T> e : edges) {
                int child = f.applyAsInt(negate(e.getNode(), parent));
                if (coalesce && !mapped.isEmpty()) {
                    Edge<T> previous = mapped.get(mapped.size() - 1);
                    if (previous.getNode() == child && canConjoin(previous.getRange(), e.getRange())) {
                        mapped.set(mapped.size() - 1, new Edge<>(previous.getRange().union(e.getRange()), child));
                        continue;
                    }
                }
                mapped.add(new Edge<>(e.getRange(), child));
            }
            return mapped;
        }

        /** The two variables' edges split into their intersections, each child the result of {@code apply}. */
        Edges apply(int parent, Edges right, int rightParent, BiFunction<Integer, Integer, Integer> apply) {
            switch (kind) {
                case VERSION:
                    return version(applyRanges(versions, parent, right.versions, rightParent, apply));
                case STRING:
                    return string(applyRanges(strings, parent, right.strings, rightParent, apply));
                default:
                    int h = apply.apply(negate(high, parent), negate(right.high, rightParent));
                    int l = apply.apply(negate(low, parent), negate(right.low, rightParent));
                    return bool(h, l);
            }
        }

        private static <T extends Comparable<T>> List<Edge<T>> applyRanges(
                List<Edge<T>> left, int leftParent, List<Edge<T>> right, int rightParent,
                BiFunction<Integer, Integer, Integer> apply) {
            List<Edge<T>> combined = new ArrayList<>();
            for (Edge<T> l : left) {
                for (Edge<T> r : right) {
                    Ranges<T> intersection = r.getRange().intersection(l.getRange());
                    if (intersection.isEmpty()) {
                        continue;
                    }
                    int node = apply.apply(negate(l.getNode(), leftParent), negate(r.getNode(), rightParent));
                    if (!combined.isEmpty()) {
                        Edge<T> last = combined.get(combined.size() - 1);
                        if (last.getNode() == node && canConjoin(last.getRange(), intersection)) {
                            combined.set(combined.size() - 1, new Edge<>(last.getRange().union(intersection), node));
                            continue;
                        }
                    }
                    combined.add(new Edge<>(intersection, node));
                }
            }
            return combined;
        }
    }

    /** A decision node, with its variable and the edges for its values. */
    @Value
    static class Node {
        Variable var;
        Edges children;
    }

    // ---- interning

    int createNode(Variable var, Edges children) {
        List<Integer> kids = children.nodes();
        int first = kids.get(0);
        boolean flipped = false;
        // the first child edge is never complemented, so a node and its complement share one representation
        if (isComplement(first)) {
            children = children.not();
            first = not(first);
            flipped = true;
        }
        boolean allSame = true;
        for (int n : children.nodes()) {
            allSame &= n == first;
        }
        if (allSame) {
            return flipped ? not(first) : first;
        }
        Node node = new Node(var, children);
        Integer existing = unique.get(node);
        int id;
        if (existing != null) {
            id = existing;
        } else {
            nodes.add(node);
            id = newId(nodes.size() - 1, false);
            unique.put(node, id);
        }
        return flipped ? not(id) : id;
    }

    /** The decision node for one expression. */
    int expression(MarkerExpression expr) {
        if (expr instanceof MarkerExpression.Version) {
            MarkerExpression.Version v = (MarkerExpression.Version) expr;
            Specifier specifier = v.getSpecifier();
            if (v.getKey() == VersionKey.PYTHON_VERSION) {
                Object full = pythonVersionToFullVersion(specifier.onlyRelease());
                if (full instanceof Integer) {
                    return (Integer) full;
                }
                return createNode(Variable.version(CanonicalVersion.PYTHON_FULL_VERSION),
                        fromSpecifier((Specifier) full));
            }
            CanonicalVersion key = v.getKey() == VersionKey.IMPLEMENTATION_VERSION ?
                    CanonicalVersion.IMPLEMENTATION_VERSION : CanonicalVersion.PYTHON_FULL_VERSION;
            return createNode(Variable.version(key), fromSpecifier(specifier));
        }
        if (expr instanceof MarkerExpression.VersionIn) {
            MarkerExpression.VersionIn v = (MarkerExpression.VersionIn) expr;
            if (v.getKey() == VersionKey.PYTHON_VERSION) {
                Object edges = fromPythonVersions(v.getVersions(), v.isNotIn());
                if (edges instanceof Integer) {
                    return (Integer) edges;
                }
                return createNode(Variable.version(CanonicalVersion.PYTHON_FULL_VERSION), (Edges) edges);
            }
            CanonicalVersion key = v.getKey() == VersionKey.IMPLEMENTATION_VERSION ?
                    CanonicalVersion.IMPLEMENTATION_VERSION : CanonicalVersion.PYTHON_FULL_VERSION;
            return createNode(Variable.version(key), fromVersions(v.getVersions(), v.isNotIn()));
        }
        if (expr instanceof MarkerExpression.Str) {
            MarkerExpression.Str s = (MarkerExpression.Str) expr;
            CanonicalString canonical = s.getKey().canonical;
            switch (s.getOperator()) {
                case IN:
                    return createNode(Variable.in(canonical, s.getValue()), Edges.fromBool(true));
                case NOT_IN:
                    return createNode(Variable.in(canonical, s.getValue()), Edges.fromBool(false));
                case CONTAINS:
                    return createNode(Variable.contains(canonical, s.getValue()), Edges.fromBool(true));
                case NOT_CONTAINS:
                    return createNode(Variable.contains(canonical, s.getValue()), Edges.fromBool(false));
                default:
                    break;
            }
            String value = s.getValue();
            // platform_system is normalized to sys_platform where the two values are exactly equivalent
            if (s.getKey() == StringKey.PLATFORM_SYSTEM) {
                String sys = platformSystemToSysPlatform(value);
                if (sys != null) {
                    canonical = CanonicalString.SYS_PLATFORM;
                    value = sys;
                }
            }
            int string = createNode(Variable.string(canonical), fromString(canonical, s.getOperator(), value));
            // Darwin kernel releases are dotted versions, so platform_release compares version-aware there
            Specifier.Op op = s.getOperator().toPep440();
            if (canonical == CanonicalString.PLATFORM_RELEASE && op != null) {
                Specifier specifier = Specifier.fromPattern(op, value);
                if (specifier != null) {
                    int version = createNode(Variable.versionString(canonical), fromSpecifier(specifier));
                    int darwin = expression(new MarkerExpression.Str(StringKey.SYS_PLATFORM, Operator.EQUAL, "darwin"));
                    int v = and(darwin, version);
                    int rest = and(not(darwin), string);
                    return or(v, rest);
                }
            }
            return string;
        }
        if (expr instanceof ListPair) {
            ListPair p = (ListPair) expr;
            return createNode(Variable.list(p.getKind(), p.getKey(), p.getValue()), Edges.fromBool(!p.isNotIn()));
        }
        MarkerExpression.Extra e = (MarkerExpression.Extra) expr;
        if (!e.isValid()) {
            return FALSE;
        }
        return createNode(Variable.extra(e.getName()), Edges.fromBool(!e.isNotEqual()));
    }

    private static @Nullable String platformSystemToSysPlatform(String value) {
        switch (value) {
            case "Windows": return "win32";
            case "Darwin": return "darwin";
            case "Linux": return "linux";
            case "AIX": return "aix";
            case "Emscripten": return "emscripten";
            case "Android": return "android";
            default: return null;
        }
    }

    // ---- operations

    int or(int x, int y) {
        return not(and(not(x), not(y)));
    }

    int and(int x, int y) {
        Integer trivial = andTrivial(x, y);
        return trivial != null ? trivial : andNontrivial(x, y);
    }

    private int andNontrivial(int xi, int yi) {
        long key = xi < yi ? ((long) xi << 32) | (yi & 0xffffffffL) : ((long) yi << 32) | (xi & 0xffffffffL);
        Integer cached = andCache.get(key);
        if (cached != null) {
            return cached;
        }
        Node x = node(xi);
        Node y = node(yi);
        boolean conflicts = pruneIncompatibleValues && x.getVar().isConflicting() && y.getVar().isConflicting();
        Variable func;
        Edges children;
        int cmp = x.getVar().compareTo(y.getVar());
        if (cmp < 0) {
            children = x.getChildren().map(xi, n -> and(n, yi));
            func = x.getVar();
        } else if (cmp > 0) {
            children = y.getChildren().map(yi, n -> and(n, xi));
            func = y.getVar();
        } else {
            children = x.getChildren().apply(xi, y.getChildren(), yi, this::and);
            func = x.getVar();
        }
        int node = createNode(func, children);
        // a combination of known incompatible values is false
        if (conflicts && disjointness(node, not(exclusions()))) {
            node = FALSE;
        }
        andCache.put(key, node);
        return node;
    }

    boolean isDisjoint(int xi, int yi) {
        Integer trivial = andTrivial(xi, yi);
        if (trivial != null) {
            return trivial == FALSE;
        }
        Node x = node(xi);
        Node y = node(yi);
        if (x.getVar().isConflicting() && y.getVar().isConflicting()) {
            return and(xi, yi) == FALSE;
        }
        return disjointByExpansion(xi, x, yi, y);
    }

    // disjointness without the known-incompatibility check, which would otherwise recurse through and
    private boolean disjointness(int xi, int yi) {
        if (xi == FALSE || yi == FALSE) {
            return true;
        }
        if (xi == TRUE || yi == TRUE) {
            return false;
        }
        if (xi == yi) {
            return false;
        }
        if (not(xi) == yi) {
            return true;
        }
        return disjointByExpansion(xi, node(xi), yi, node(yi));
    }

    private boolean disjointByExpansion(int xi, Node x, int yi, Node y) {
        int cmp = x.getVar().compareTo(y.getVar());
        if (cmp < 0) {
            for (int child : x.getChildren().nodes()) {
                if (!disjointness(negate(child, xi), yi)) {
                    return false;
                }
            }
            return true;
        }
        if (cmp > 0) {
            for (int child : y.getChildren().nodes()) {
                if (!disjointness(negate(child, yi), xi)) {
                    return false;
                }
            }
            return true;
        }
        Edges l = x.getChildren();
        Edges r = y.getChildren();
        switch (l.getKind()) {
            case VERSION:
                return disjointRanges(l.getVersions(), xi, r.getVersions(), yi);
            case STRING:
                return disjointRanges(l.getStrings(), xi, r.getStrings(), yi);
            default:
                return disjointness(negate(l.getHigh(), xi), negate(r.getHigh(), yi)) &&
                        disjointness(negate(l.getLow(), xi), negate(r.getLow(), yi));
        }
    }

    private <T extends Comparable<T>> boolean disjointRanges(List<Edge<T>> left, int leftParent, List<Edge<T>> right,
            int rightParent) {
        for (Edge<T> l : left) {
            for (Edge<T> r : right) {
                if (r.getRange().isDisjoint(l.getRange())) {
                    continue;
                }
                if (!disjointness(negate(l.getNode(), leftParent), negate(r.getNode(), rightParent))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The tree with each boolean variable {@code f} decides fixed to that value. */
    int restrictBy(int i, Function<Variable, @Nullable Boolean> f) {
        return restrictByCached(i, f, new HashMap<>());
    }

    private int restrictByCached(int i, Function<Variable, @Nullable Boolean> f, Map<Integer, Integer> cache) {
        if (i == TRUE || i == FALSE) {
            return i;
        }
        Integer cached = cache.get(i);
        if (cached != null) {
            return cached;
        }
        Node node = node(i);
        int result;
        Boolean value = node.getChildren().getKind() == Edges.Kind.BOOLEAN ? f.apply(node.getVar()) : null;
        if (value != null) {
            int child = value ? node.getChildren().getHigh() : node.getChildren().getLow();
            result = restrictByCached(negate(child, i), f, cache);
        } else {
            result = createNode(node.getVar(), node.getChildren().map(i, n -> restrictByCached(n, f, cache)));
        }
        cache.put(i, result);
        return result;
    }

    /** {@code value} wherever {@code assumption} holds, simplified by leaving its value elsewhere open. */
    int restrict(int value, int assumption) {
        return restrictCached(value, assumption, new HashMap<>());
    }

    private int restrictCached(int value, int assumption, Map<Long, Integer> cache) {
        if (assumption == TRUE || value == TRUE || value == FALSE) {
            return value;
        }
        if (assumption == FALSE) {
            return FALSE;
        }
        if (value == assumption) {
            return TRUE;
        }
        if (value == not(assumption)) {
            return FALSE;
        }
        long key = ((long) value << 32) | (assumption & 0xffffffffL);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Node valueNode = node(value);
        Node assumptionNode = node(assumption);
        int result;
        int cmp = valueNode.getVar().compareTo(assumptionNode.getVar());
        if (cmp < 0) {
            result = createNode(valueNode.getVar(), valueNode.getChildren().map(value, v -> restrictCached(v,
                    assumption, cache)));
        } else if (cmp > 0) {
            // the value does not depend on this variable, so it is quantified out of the assumption
            int quantified = FALSE;
            for (int child : assumptionNode.getChildren().nodes()) {
                quantified = or(quantified, negate(child, assumption));
            }
            result = restrictCached(value, quantified, cache);
        } else {
            int[] fallback = {-1};
            valueNode.getChildren().apply(value, assumptionNode.getChildren(), assumption, (v, a) -> {
                if (a == FALSE) {
                    return FALSE;
                }
                int r = restrictCached(v, a, cache);
                if (fallback[0] < 0) {
                    fallback[0] = r;
                }
                return r;
            });
            if (fallback[0] < 0) {
                return FALSE;
            }
            result = createNode(valueNode.getVar(), valueNode.getChildren().apply(value, assumptionNode.getChildren(),
                    assumption,
                    (v, a) -> a == FALSE ? fallback[0] : restrictCached(v, a, cache)));
        }
        cache.put(key, result);
        return result;
    }

    /** The tree with every {@code extra} decision taken as true on all its branches. */
    int withoutExtras(int i) {
        return withoutExtrasCached(i, new HashMap<>());
    }

    private int withoutExtrasCached(int i, Map<Integer, Integer> cache) {
        if (i == TRUE || i == FALSE) {
            return i;
        }
        Integer cached = cache.get(i);
        if (cached != null) {
            return cached;
        }
        Node node = node(i);
        int result;
        if (node.getVar().getKind() == Variable.Kind.EXTRA) {
            int merged = FALSE;
            for (int child : node.getChildren().nodes()) {
                merged = or(merged, negate(child, i));
            }
            result = merged == TRUE ? TRUE : withoutExtrasCached(merged, cache);
        } else {
            result = createNode(node.getVar(), node.getChildren().map(i, n -> withoutExtrasCached(n, cache)));
        }
        cache.put(i, result);
        return result;
    }

    /** The tree with every decision but {@code extra} ones taken as true on all its branches. */
    int onlyExtras(int i) {
        if (i == TRUE || i == FALSE) {
            return i;
        }
        Node node = node(i);
        if (node.getVar().getKind() != Variable.Kind.EXTRA) {
            int merged = FALSE;
            for (int child : node.getChildren().nodes()) {
                merged = or(merged, negate(child, i));
            }
            return merged == TRUE ? TRUE : onlyExtras(merged);
        }
        return createNode(node.getVar(), node.getChildren().map(i, this::onlyExtras));
    }

    /** The tree simplified on the assumption that {@code python_full_version} lies within the bounds. */
    int simplifyPythonVersions(int i, Ranges.Bound<PythonVersion> lower, Ranges.Bound<PythonVersion> upper) {
        if (i == TRUE || i == FALSE || lower.isUnbounded() && upper.isUnbounded()) {
            return i;
        }
        Node node = node(i);
        if (!(node.getVar().getKind() == Variable.Kind.VERSION
                && node.getVar().getVersion() == CanonicalVersion.PYTHON_FULL_VERSION &&
                node.getChildren().getKind() == Edges.Kind.VERSION)) {
            return createNode(node.getVar(), node.getChildren().map(i, n -> simplifyPythonVersions(n, lower, upper)));
        }
        Ranges<PythonVersion> pyRange = Ranges.fromRangeBounds(lower, upper);
        if (pyRange.isEmpty()) {
            return FALSE;
        }
        List<Edge<PythonVersion>> kept = new ArrayList<>();
        for (Edge<PythonVersion> e : node.getChildren().getVersions()) {
            Ranges<PythonVersion> overlap = e.getRange().intersection(pyRange);
            if (!overlap.isEmpty()) {
                kept.add(new Edge<>(overlap, e.getNode()));
            }
        }
        // the surviving edges extend to infinity on both sides, where the bounds are known to hold
        Edge<PythonVersion> first = kept.get(0);
        kept.set(0, new Edge<>(Ranges.fromRangeBounds(Ranges.Bound.unbounded(),
                first.getRange().boundingRange().getEnd()), first.getNode()));
        Edge<PythonVersion> last = kept.get(kept.size() - 1);
        kept.set(kept.size() - 1, new Edge<>(Ranges.fromRangeBounds(last.getRange().boundingRange().getStart(),
                Ranges.Bound.unbounded()), last.getNode()));
        return negate(createNode(node.getVar(), Edges.version(kept)), i);
    }

    /** The tree conjoined with the Python version bounds, the reverse of {@link #simplifyPythonVersions}. */
    int complexifyPythonVersions(int i, Ranges.Bound<PythonVersion> lower, Ranges.Bound<PythonVersion> upper) {
        if (i == FALSE || lower.isUnbounded() && upper.isUnbounded()) {
            return i;
        }
        Ranges<PythonVersion> pyRange = Ranges.fromRangeBounds(lower, upper);
        int versions = createNode(Variable.version(CanonicalVersion.PYTHON_FULL_VERSION),
                Edges.version(fromRange(pyRange)));
        return and(i, versions);
    }

    boolean isUnreachable(int marker) {
        return and(marker, not(exclusions())) == FALSE;
    }

    /**
     * The disjunction of pairs of values known never to hold together, such as {@code os_name == 'nt'} with
     * {@code sys_platform == 'linux'}.
     */
    private int exclusions() {
        if (exclusions != null) {
            return exclusions;
        }
        Map<Long, Integer> cache = new HashMap<>();
        int osNameNt = expression(new MarkerExpression.Str(StringKey.OS_NAME, Operator.EQUAL, "nt"));
        int osNamePosix = expression(new MarkerExpression.Str(StringKey.OS_NAME, Operator.EQUAL, "posix"));
        int linux = sys("linux");
        int darwin = sys("darwin");
        int ios = sys("ios");
        int win32 = sys("win32");
        List<int[]> pairs = new ArrayList<>();
        pairs.add(new int[]{osNameNt, linux});
        pairs.add(new int[]{osNameNt, darwin});
        pairs.add(new int[]{osNameNt, ios});
        pairs.add(new int[]{osNamePosix, win32});
        int platformIos = platformSystem("iOS");
        int platformIpados = platformSystem("iPadOS");
        int[] platformSystems = {platformSystem("FreeBSD"), platformSystem("NetBSD"), platformSystem("OpenBSD"),
                platformSystem("SunOS"), platformIos, platformIpados};
        int[] sysPlatforms = {sys("aix"), sys("android"), sys("emscripten"), ios, linux, darwin, win32, sys("cygwin"),
                sys("wasi")};
        for (int platform : platformSystems) {
            for (int sysPlatform : sysPlatforms) {
                if (sysPlatform == ios && (platform == platformIos || platform == platformIpados)) {
                    continue;
                }
                pairs.add(new int[]{platform, sysPlatform});
            }
        }
        int tree = FALSE;
        for (int[] pair : pairs) {
            int both = conjunction(cache, pair[0], pair[1]);
            tree = not(conjunction(cache, not(tree), not(both)));
        }
        exclusions = tree;
        return tree;
    }

    private int sys(String value) {
        return expression(new MarkerExpression.Str(StringKey.SYS_PLATFORM, Operator.EQUAL, value));
    }

    private int platformSystem(String value) {
        return expression(new MarkerExpression.Str(StringKey.PLATFORM_SYSTEM, Operator.EQUAL, value));
    }

    // conjunction without the known-incompatibility check, whose results never enter the regular cache
    private int conjunction(Map<Long, Integer> cache, int xi, int yi) {
        Integer trivial = andTrivial(xi, yi);
        if (trivial != null) {
            return trivial;
        }
        long key = ((long) xi << 32) | (yi & 0xffffffffL);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Node x = node(xi);
        Node y = node(yi);
        int cmp = x.getVar().compareTo(y.getVar());
        int node;
        if (cmp < 0) {
            node = createNode(x.getVar(), x.getChildren().map(xi, n -> conjunction(cache, n, yi)));
        } else if (cmp > 0) {
            node = createNode(y.getVar(), y.getChildren().map(yi, n -> conjunction(cache, n, xi)));
        } else {
            node = createNode(x.getVar(), x.getChildren().apply(xi, y.getChildren(), yi, (a, b) -> conjunction(cache,
                    a, b)));
        }
        cache.put(key, node);
        return node;
    }

    // ---- edges of an expression

    private static Edges fromString(CanonicalString key, Operator operator, String value) {
        Ranges<String> range;
        boolean versionLike = key == CanonicalString.PLATFORM_RELEASE || key == CanonicalString.PLATFORM_VERSION;
        switch (operator) {
            case GREATER_THAN:
                range = versionLike ? Ranges.strictlyHigherThan(value) : Ranges.<String>empty();
                break;
            case GREATER_EQUAL:
                range = versionLike ? Ranges.higherThan(value) : Ranges.singleton(value);
                break;
            case LESS_THAN:
                range = versionLike ? Ranges.strictlyLowerThan(value) : Ranges.<String>empty();
                break;
            case LESS_EQUAL:
                range = versionLike ? Ranges.lowerThan(value) : Ranges.singleton(value);
                break;
            case EQUAL:
                range = Ranges.singleton(value);
                break;
            case NOT_EQUAL:
                range = Ranges.singleton(value).complement();
                break;
            default:
                throw new IllegalStateException("string comparison with " + operator + " is not a range");
        }
        return Edges.string(fromRange(range));
    }

    private static Edges fromSpecifier(Specifier specifier) {
        return Edges.version(fromRange(releaseSpecifierToRange(specifier.onlyRelease())));
    }

    // a python_version list matches each version's python_full_version range
    private static Object fromPythonVersions(List<PythonVersion> versions, boolean notIn) {
        Ranges<PythonVersion> range = Ranges.empty();
        for (PythonVersion version : versions) {
            Specifier equals = new Specifier(Specifier.Op.EQUAL, releaseVersion(version.getRelease(),
                    version.getRelease().length));
            Object full = pythonVersionToFullVersion(equals);
            if (full instanceof Integer) {
                return full;
            }
            range = range.union(releaseSpecifierToRange((Specifier) full));
        }
        if (notIn) {
            range = range.complement();
        }
        return Edges.version(fromRange(range));
    }

    private static Edges fromVersions(List<PythonVersion> versions, boolean notIn) {
        Ranges<PythonVersion> range = Ranges.empty();
        for (PythonVersion version : versions) {
            range = range.union(Ranges.singleton(version));
        }
        if (notIn) {
            range = range.complement();
        }
        return Edges.version(fromRange(range));
    }

    /** Edges true on the range's segments and false on its complement's, sorted by start. */
    static <T extends Comparable<T>> List<Edge<T>> fromRange(Ranges<T> range) {
        List<Edge<T>> edges = new ArrayList<>();
        for (Ranges.Segment<T> s : range.segments()) {
            edges.add(new Edge<>(Ranges.fromRangeBounds(s.getStart(), s.getEnd()), TRUE));
        }
        for (Ranges.Segment<T> s : range.complement().segments()) {
            edges.add(new Edge<>(Ranges.fromRangeBounds(s.getStart(), s.getEnd()), FALSE));
        }
        edges.sort((a, b) -> Ranges.compareStarts(a.getRange().boundingRange().getStart(),
                b.getRange().boundingRange().getStart()));
        return edges;
    }

    // two disjoint ranges meet without a gap when one includes the position the other excludes
    static <T extends Comparable<T>> boolean canConjoin(Ranges<T> a, Ranges<T> b) {
        Ranges.Segment<T> r1 = a.boundingRange();
        Ranges.Segment<T> r2 = b.boundingRange();
        if (r1 == null || r2 == null) {
            return false;
        }
        Ranges.Bound<T> end = r1.getEnd();
        Ranges.Bound<T> start = r2.getStart();
        if (end.isUnbounded() || start.isUnbounded() || end.getValue().compareTo(start.getValue()) != 0) {
            return false;
        }
        return end.isIncluded() && start.isExcluded() || end.isExcluded() && start.isIncluded();
    }

    /** uv-pep440's {@code release_specifier_to_range} with trailing zeros trimmed, as markers use it. */
    static Ranges<PythonVersion> releaseSpecifierToRange(Specifier specifier) {
        PythonVersion version = specifier.getVersion();
        PythonVersion trimmed = onlyReleaseTrimmed(version);
        long[] release = version.getRelease();
        switch (specifier.getOp()) {
            case EQUAL:
            case EXACT_EQUAL:
                return Ranges.singleton(trimmed);
            case NOT_EQUAL:
                return Ranges.singleton(trimmed).complement();
            case LESS_THAN:
                return Ranges.strictlyLowerThan(trimmed);
            case LESS_THAN_EQUAL:
                return Ranges.lowerThan(trimmed);
            case GREATER_THAN:
                return Ranges.strictlyHigherThan(trimmed);
            case GREATER_THAN_EQUAL:
                return Ranges.higherThan(trimmed);
            case TILDE_EQUAL: {
                long[] upper = Arrays.copyOf(release, release.length - 1);
                upper[upper.length - 1]++;
                return Ranges.fromRangeBounds(Ranges.Bound.included(trimmed),
                        Ranges.Bound.excluded(releaseVersion(upper, upper.length)));
            }
            default: {
                // trailing zeros stay significant before a star
                long[] high = Arrays.copyOf(release, release.length);
                high[high.length - 1]++;
                Ranges<PythonVersion> range = Ranges.fromRangeBounds(Ranges.Bound.included(version),
                        Ranges.Bound.excluded(releaseVersion(high, high.length)));
                return specifier.getOp() == Specifier.Op.NOT_EQUAL_STAR ? range.complement() : range;
            }
        }
    }

    /** The python_full_version specifier a python_version one denotes, or the constant node it always evaluates to. */
    static Object pythonVersionToFullVersion(Specifier specifier) {
        long[] release = specifier.getVersion().getRelease();
        Specifier.Op op = specifier.getOp();
        if (op.isStar()) {
            if (release.length == 1) {
                return specifier;
            }
            for (int i = 2; i < release.length; i++) {
                if (release[i] != 0) {
                    return FALSE;
                }
            }
            return new Specifier(op, releaseVersion(new long[]{release[0], release[1]}, 2));
        }
        if (op == Specifier.Op.TILDE_EQUAL) {
            if (release.length == 2) {
                return specifier;
            }
            for (int i = 2; i < release.length; i++) {
                if (release[i] != 0) {
                    return FALSE;
                }
            }
            return new Specifier(Specifier.Op.EQUAL_STAR, releaseVersion(new long[]{release[0], release[1]}, 2));
        }
        long[] trimmed = onlyReleaseTrimmed(specifier.getVersion()).getRelease();
        if (trimmed.length <= 2) {
            long major = trimmed[0];
            long minor = trimmed.length == 2 ? trimmed[1] : 0;
            PythonVersion version = releaseVersion(new long[]{major, minor}, 2);
            switch (op) {
                case EQUAL:
                case EXACT_EQUAL:
                    return new Specifier(Specifier.Op.EQUAL_STAR, version);
                case NOT_EQUAL:
                    return new Specifier(Specifier.Op.NOT_EQUAL_STAR, version);
                case GREATER_THAN:
                    return new Specifier(Specifier.Op.GREATER_THAN_EQUAL, releaseVersion(new long[]{major, minor + 1},
                            2));
                case LESS_THAN:
                    return major == 0 && minor == 0 ? FALSE : specifier;
                case GREATER_THAN_EQUAL:
                    return major == 0 && minor == 0 ? TRUE : specifier;
                default:
                    return new Specifier(Specifier.Op.LESS_THAN, releaseVersion(new long[]{major, minor + 1}, 2));
            }
        }
        long major = release[0];
        long minor = release[1];
        switch (op) {
            case EQUAL:
            case EXACT_EQUAL:
                return FALSE;
            case NOT_EQUAL:
                return TRUE;
            case LESS_THAN:
            case LESS_THAN_EQUAL:
                return new Specifier(Specifier.Op.LESS_THAN, releaseVersion(new long[]{major, minor + 1}, 2));
            default:
                return new Specifier(Specifier.Op.GREATER_THAN_EQUAL, releaseVersion(new long[]{major, minor + 1}, 2));
        }
    }
}
