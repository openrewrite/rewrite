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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;

import java.util.*;

/**
 * The marker under which each lock entry is reachable from the root, and the edge simplification uv
 * derives from it since 0.11.25: a clause the parent's reachability implies leaves the edge marker,
 * and an edge the reachability contradicts leaves the lock.
 */
final class UvLockReachability {

    /** The most disjuncts a node's reachability may hold before the pass fails loud. */
    private static final int MAX_TERMS = 32;

    private final List<UvLockPackage> packages;
    private final PyRange requiresPython;
    private final Map<String, List<Integer>> byName = new HashMap<>();
    private final Map<Node, List<Term>> reach = new HashMap<>();

    private UvLockReachability(List<UvLockPackage> packages, PyRange requiresPython) {
        this.packages = packages;
        this.requiresPython = requiresPython;
        for (int i = 0; i < packages.size(); i++) {
            byName.computeIfAbsent(packages.get(i).getName(), k -> new ArrayList<>()).add(i);
        }
    }

    static List<UvLockPackage> simplify(List<UvLockPackage> packages, int rootIndex, PyRange requiresPython) {
        UvLockReachability r = new UvLockReachability(packages, requiresPython);
        r.propagate(rootIndex);
        List<UvLockPackage> simplified = new ArrayList<>(packages.size());
        for (int i = 0; i < packages.size(); i++) {
            simplified.add(r.simplifyEntry(i, i == rootIndex));
        }
        return simplified;
    }

    /** A lock entry, or one of its extras, as a node of the reachability graph. */
    @Value
    private static class Node {
        int index;
        @Nullable String extra;
    }

    /** A clause over a marker variable other than the Python version, which only {@code ==} and {@code !=} compare. */
    @Value
    private static class Atom {
        String var;
        boolean equality;
        String value;

        boolean contradicts(Atom o) {
            return var.equals(o.var) && (equality && o.equality ? !value.equals(o.value) :
                    equality != o.equality && value.equals(o.value));
        }

        // var == 'x' implies var != 'y' for every other y
        boolean implies(Atom o) {
            return equals(o) || (var.equals(o.var) && equality && !o.equality && !value.equals(o.value));
        }

        @Override
        public String toString() {
            return var + (equality ? " == '" : " != '") + value + "'";
        }
    }

    /** One conjunction of Python version clauses and atoms. */
    @Value
    private static class Term {
        List<String[]> pythonClauses;
        Set<Atom> atoms;

        @Nullable PyRange range(PyRange base) {
            PyRange r = base;
            for (String[] c : pythonClauses) {
                r = r.intersect(clauseRange(c));
                if (r == null) {
                    return null;
                }
            }
            return r;
        }

        @Nullable Term and(Term o) {
            Set<Atom> atoms = new LinkedHashSet<>(this.atoms);
            for (Atom atom : o.atoms) {
                for (Atom mine : this.atoms) {
                    if (mine.contradicts(atom)) {
                        return null;
                    }
                }
                atoms.add(atom);
            }
            List<String[]> clauses = new ArrayList<>(pythonClauses);
            clauses.addAll(o.pythonClauses);
            return new Term(clauses, atoms);
        }

        String key() {
            StringBuilder b = new StringBuilder();
            for (String[] c : pythonClauses) {
                b.append(c[0]).append(c[1]).append(c[2]).append('|');
            }
            return b.append(atoms).toString();
        }
    }

    // ---- parsing

    // != only removes points from a range, so leaving the range whole for it stays sound
    private static PyRange clauseRange(String[] clause) {
        String[] lowered = UvLockMarkers.lowerVersionClause(clause[0], clause[1], clause[2]);
        if (lowered == null) {
            throw unsupported("Python version clause " + String.join(" ", clause));
        }
        String op = lowered[1];
        String value = lowered[2];
        if ("!=".equals(op)) {
            return new PyRange();
        }
        if ("==".equals(op) && !value.endsWith(".*")) {
            return PyRange.parse(">=" + value + ",<=" + value);
        }
        return PyRange.parse(op + value);
    }

    private static List<Term> dnf(@Nullable String marker) {
        List<Term> terms = new ArrayList<>();
        if (marker == null) {
            terms.add(new Term(new ArrayList<>(), new LinkedHashSet<>()));
            return terms;
        }
        List<List<String[]>> parsed = UvLockMarkers.parseDisjunction(marker);
        if (parsed == null) {
            throw unsupported("marker \"" + marker + "\"");
        }
        for (List<String[]> chain : parsed) {
            List<String[]> python = new ArrayList<>();
            Set<Atom> atoms = new LinkedHashSet<>();
            for (String[] clause : chain) {
                if ("python_version".equals(clause[0]) || "python_full_version".equals(clause[0])) {
                    python.add(clause);
                } else if ("==".equals(clause[1]) || "!=".equals(clause[1])) {
                    atoms.add(new Atom(clause[0], "==".equals(clause[1]), clause[2]));
                } else {
                    throw unsupported("marker \"" + marker + "\"");
                }
            }
            terms.add(new Term(python, atoms));
        }
        return terms;
    }

    private static EngineFailure unsupported(String what) {
        return new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                "Simplifying dependency markers under reachability is not supported for " + what);
    }

    // ---- propagation

    private void propagate(int rootIndex) {
        Deque<Node> queue = new ArrayDeque<>();
        Node root = new Node(rootIndex, null);
        reach.put(root, new ArrayList<>(dnf(null)));
        queue.add(root);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            List<Term> parent = reach.get(node);
            for (List<UvLockDependency> edges : edgeLists(node, rootIndex)) {
                for (UvLockDependency edge : edges) {
                    List<Term> via = and(parent, dnf(edge.getMarker()));
                    int target = target(edge);
                    List<Node> children = new ArrayList<>();
                    children.add(new Node(target, null));
                    if (edge.getExtra() != null) {
                        for (String e : edge.getExtra()) {
                            children.add(new Node(target, e));
                        }
                    }
                    for (Node child : children) {
                        if (or(child, via)) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
    }

    private List<List<UvLockDependency>> edgeLists(Node node, int rootIndex) {
        UvLockPackage pkg = packages.get(node.getIndex());
        List<List<UvLockDependency>> lists = new ArrayList<>();
        if (node.getExtra() != null) {
            if (pkg.getOptionalDependencies() != null && pkg.getOptionalDependencies().get(node.getExtra()) != null) {
                lists.add(pkg.getOptionalDependencies().get(node.getExtra()));
            }
            return lists;
        }
        if (pkg.getDependencies() != null) {
            lists.add(pkg.getDependencies());
        }
        if (pkg.getDevDependencies() != null) {
            lists.addAll(pkg.getDevDependencies().values());
        }
        // the root's extras are all locked, so its groups are reachable with it
        if (node.getIndex() == rootIndex && pkg.getOptionalDependencies() != null) {
            lists.addAll(pkg.getOptionalDependencies().values());
        }
        return lists;
    }

    private int target(UvLockDependency edge) {
        int found = -1;
        for (int i : byName.getOrDefault(edge.getName(), Collections.emptyList())) {
            UvLockPackage pkg = packages.get(i);
            if ((edge.getVersion() != null && !edge.getVersion().equals(pkg.getVersion())) ||
                    (edge.getSource() != null && !edge.getSource().equals(pkg.getSource()))) {
                continue;
            }
            if (found >= 0) {
                throw unsupported("an edge to " + edge.getName() + " that several entries match");
            }
            found = i;
        }
        if (found < 0) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, edge.getName(),
                    "Dependency edge to " + edge.getName() + " has no [[package]] entry");
        }
        return found;
    }

    private List<Term> and(List<Term> a, List<Term> b) {
        List<Term> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Term x : a) {
            for (Term y : b) {
                Term t = x.and(y);
                if (t != null && t.range(requiresPython) != null && seen.add(t.key())) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    /** Adds the terms to the node's reachability and reports whether it grew. */
    private boolean or(Node node, List<Term> terms) {
        List<Term> existing = reach.computeIfAbsent(node, k -> new ArrayList<>());
        Set<String> keys = new HashSet<>();
        for (Term t : existing) {
            keys.add(t.key());
        }
        boolean grew = false;
        for (Term t : terms) {
            if (keys.add(t.key())) {
                existing.add(t);
                grew = true;
            }
        }
        if (existing.size() > MAX_TERMS) {
            throw unsupported("a reachability marker with more than " + MAX_TERMS + " disjuncts");
        }
        return grew;
    }

    // ---- simplification

    private UvLockPackage simplifyEntry(int index, boolean root) {
        UvLockPackage pkg = packages.get(index);
        List<Term> own = reach.get(new Node(index, null));
        if (own == null) {
            return pkg;
        }
        UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder();
        if (pkg.getDependencies() != null) {
            b.dependencies(simplifyList(pkg.getDependencies(), own));
        }
        if (pkg.getDevDependencies() != null) {
            Map<String, List<UvLockDependency>> dev = new LinkedHashMap<>();
            for (Map.Entry<String, List<UvLockDependency>> e : pkg.getDevDependencies().entrySet()) {
                dev.put(e.getKey(), simplifyList(e.getValue(), own));
            }
            b.devDependencies(dev);
        }
        if (pkg.getOptionalDependencies() != null) {
            Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
            for (Map.Entry<String, List<UvLockDependency>> e : pkg.getOptionalDependencies().entrySet()) {
                List<Term> parent = root ? own : reach.get(new Node(index, e.getKey()));
                groups.put(e.getKey(), parent == null ? e.getValue() : simplifyList(e.getValue(), parent));
            }
            b.optionalDependencies(groups);
        }
        return b.build();
    }

    private List<UvLockDependency> simplifyList(List<UvLockDependency> edges, List<Term> parent) {
        List<UvLockDependency> out = new ArrayList<>(edges.size());
        for (UvLockDependency edge : edges) {
            UvLockDependency simplified = edge.getMarker() == null ? edge : restrict(edge, parent);
            if (simplified != null && !out.contains(simplified)) {
                out.add(simplified);
            }
        }
        return out;
    }

    /**
     * The edge with its marker restricted to what the parent's reachability leaves undecided, or null
     * when no term survives. A clause every reachable term decides goes, and a multi-term marker only
     * survives untouched.
     */
    private @Nullable UvLockDependency restrict(UvLockDependency edge, List<Term> parent) {
        List<PyRange> ranges = new ArrayList<>();
        for (Term t : parent) {
            PyRange r = t.range(requiresPython);
            if (r != null) {
                ranges.add(r);
            }
        }
        if (ranges.isEmpty()) {
            return edge;
        }
        List<String> kept = new ArrayList<>();
        boolean changed = false;
        for (Term term : dnf(edge.getMarker())) {
            List<String> clauses = new ArrayList<>();
            boolean falseTerm = false;
            for (String[] c : term.getPythonClauses()) {
                boolean allTrue = true;
                boolean allFalse = true;
                for (PyRange r : ranges) {
                    Boolean truth = UvLockMarkers.clauseTruth(r, c[0], c[1], c[2]);
                    allTrue &= Boolean.TRUE.equals(truth);
                    allFalse &= Boolean.FALSE.equals(truth);
                }
                if (allFalse) {
                    falseTerm = true;
                } else if (!allTrue) {
                    clauses.add(c[0] + " " + c[1] + " '" + c[2] + "'");
                }
                changed |= allTrue || allFalse;
            }
            for (Atom atom : term.getAtoms()) {
                boolean allImplied = true;
                boolean allContradicted = true;
                for (Term t : parent) {
                    boolean implied = false;
                    boolean contradicted = false;
                    for (Atom given : t.getAtoms()) {
                        implied |= given.implies(atom);
                        contradicted |= given.contradicts(atom);
                    }
                    allImplied &= implied;
                    allContradicted &= contradicted;
                }
                if (allContradicted) {
                    falseTerm = true;
                } else if (!allImplied) {
                    clauses.add(atom.toString());
                }
                changed |= allImplied || allContradicted;
            }
            if (falseTerm) {
                continue;
            }
            if (clauses.isEmpty()) {
                return edge.withMarker(null);
            }
            kept.add(String.join(" and ", clauses));
        }
        if (!changed) {
            return edge;
        }
        if (kept.isEmpty()) {
            return null;
        }
        if (kept.size() > 1) {
            throw unsupported("the disjunction \"" + edge.getMarker() + "\" that reachability narrows");
        }
        return edge.withMarker(kept.get(0));
    }
}
