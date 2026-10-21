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
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.pep440.PythonVersion;

import java.util.*;

/**
 * The marker under which each lock entry is reachable from the root, and the edge simplification uv
 * derives from it since 0.11.25: a clause the parent's reachability implies leaves the edge marker,
 * and an edge the reachability contradicts leaves the lock.
 */
final class UvLockReachability {

    private static final int MAX_TERMS = 32;
    private static final Set<String> PYTHON_VARS = new HashSet<>(Arrays.asList("python_version", "python_full_version"));

    private final List<UvLockPackage> packages;
    private final PyRange requiresPython;
    private final Map<String, List<Term>> reach = new HashMap<>();

    private UvLockReachability(List<UvLockPackage> packages, PyRange requiresPython) {
        this.packages = packages;
        this.requiresPython = requiresPython;
    }

    static List<UvLockPackage> simplify(List<UvLockPackage> packages, int rootIndex, PyRange requiresPython) {
        UvLockReachability r = new UvLockReachability(packages, requiresPython);
        r.propagate(rootIndex);
        List<UvLockPackage> simplified = new ArrayList<>(packages.size());
        for (int i = 0; i < packages.size(); i++) {
            simplified.add(r.simplifyEntry(i));
        }
        return simplified;
    }

    /** One conjunction of Python version clauses and equality atoms over the other marker variables. */
    private static final class Term {
        final List<String[]> pythonClauses = new ArrayList<>();
        final Set<String> atoms = new LinkedHashSet<>();

        @Nullable PyRange range(PyRange base) {
            PyRange r = base;
            for (String[] c : pythonClauses) {
                PyRange clause = clauseRange(c);
                r = clause == null ? null : r.intersect(clause);
                if (r == null) {
                    return null;
                }
            }
            return r;
        }

        @Nullable Term and(Term o) {
            Term t = new Term();
            t.pythonClauses.addAll(pythonClauses);
            t.pythonClauses.addAll(o.pythonClauses);
            t.atoms.addAll(atoms);
            for (String atom : o.atoms) {
                if (contradicts(atom)) {
                    return null;
                }
                t.atoms.add(atom);
            }
            return t;
        }

        boolean contradicts(String atom) {
            String var = atom.substring(0, atom.indexOf(' '));
            boolean equality = atom.startsWith(var + " == ");
            for (String mine : atoms) {
                if (!mine.startsWith(var + " ")) {
                    continue;
                }
                boolean mineEquality = mine.startsWith(var + " == ");
                String value = atom.substring(atom.indexOf("'"));
                String mineValue = mine.substring(mine.indexOf("'"));
                if (equality && mineEquality && !value.equals(mineValue)) {
                    return true;
                }
                if (equality != mineEquality && value.equals(mineValue)) {
                    return true;
                }
            }
            return false;
        }

        String key() {
            StringBuilder b = new StringBuilder();
            for (String[] c : pythonClauses) {
                b.append(c[0]).append(c[1]).append(c[2]).append('|');
            }
            return b + String.join("|", atoms);
        }
    }

    // ---- parsing

    private static @Nullable PyRange clauseRange(String[] clause) {
        String var = clause[0];
        String op = clause[1];
        String value = clause[2];
        if ("python_version".equals(var)) {
            PythonVersion v = PythonVersion.parse(value);
            if (v == null || v.getRelease().length != 2) {
                throw unsupported("python_version clause " + op + " '" + value + "'");
            }
            PythonVersion next = PyRange.bumpLastRelease(v);
            switch (op) {
                case "<": return PyRange.parse("<" + value);
                case "<=": return PyRange.parse("<" + next);
                case ">=": return PyRange.parse(">=" + value);
                case ">": return PyRange.parse(">=" + next);
                case "==": return PyRange.parse(">=" + value + ",<" + next);
                default: throw unsupported("python_version clause " + op);
            }
        }
        switch (op) {
            case "<": case "<=": case ">=": case ">": case "~=":
                return PyRange.parse(op + value);
            case "==":
                return value.endsWith(".*") ? PyRange.parse("==" + value) : PyRange.parse(">=" + value + ",<=" + value);
            default:
                throw unsupported("python_full_version clause " + op);
        }
    }

    private static List<Term> dnf(@Nullable String marker) {
        List<Term> terms = new ArrayList<>();
        if (marker == null) {
            terms.add(new Term());
            return terms;
        }
        List<List<String[]>> parsed = UvLockMarkers.parseDisjunction(marker);
        if (parsed == null) {
            throw unsupported("marker \"" + marker + "\"");
        }
        for (List<String[]> chain : parsed) {
            Term term = new Term();
            for (String[] clause : chain) {
                if (PYTHON_VARS.contains(clause[0])) {
                    term.pythonClauses.add(clause);
                } else if ("==".equals(clause[1]) || "!=".equals(clause[1])) {
                    term.atoms.add(clause[0] + " " + clause[1] + " '" + clause[2] + "'");
                } else {
                    throw unsupported("marker \"" + marker + "\"");
                }
            }
            terms.add(term);
        }
        return terms;
    }

    private static EngineFailure unsupported(String what) {
        return new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                "Simplifying dependency markers under reachability is not supported for " + what);
    }

    // ---- propagation

    private void propagate(int rootIndex) {
        Deque<String> queue = new ArrayDeque<>();
        reach.put(key(rootIndex, null), Collections.singletonList(new Term()));
        queue.add(key(rootIndex, null));
        while (!queue.isEmpty()) {
            String node = queue.poll();
            int index = Integer.parseInt(node.substring(0, node.indexOf('|')));
            String extra = node.substring(node.indexOf('|') + 1);
            UvLockPackage pkg = packages.get(index);
            List<List<UvLockDependency>> lists = new ArrayList<>();
            if (extra.isEmpty()) {
                if (pkg.getDependencies() != null) {
                    lists.add(pkg.getDependencies());
                }
                if (pkg.getDevDependencies() != null) {
                    lists.addAll(pkg.getDevDependencies().values());
                }
                // the root's extras are all locked, so its groups are reachable with it
                if (index == rootIndex && pkg.getOptionalDependencies() != null) {
                    lists.addAll(pkg.getOptionalDependencies().values());
                }
            } else if (pkg.getOptionalDependencies() != null && pkg.getOptionalDependencies().get(extra) != null) {
                lists.add(pkg.getOptionalDependencies().get(extra));
            }
            List<Term> parent = reach.get(node);
            for (List<UvLockDependency> edges : lists) {
                for (UvLockDependency edge : edges) {
                    List<Term> via = and(parent, dnf(edge.getMarker()));
                    int target = target(edge);
                    List<String> children = new ArrayList<>();
                    children.add(key(target, null));
                    if (edge.getExtra() != null) {
                        for (String e : edge.getExtra()) {
                            children.add(key(target, e));
                        }
                    }
                    for (String child : children) {
                        if (or(child, via)) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
    }

    private static String key(int index, @Nullable String extra) {
        return index + "|" + (extra == null ? "" : extra);
    }

    private int target(UvLockDependency edge) {
        int found = -1;
        for (int i = 0; i < packages.size(); i++) {
            UvLockPackage pkg = packages.get(i);
            if (!pkg.getName().equals(edge.getName()) ||
                    (edge.getVersion() != null && !edge.getVersion().equals(pkg.getVersion())) ||
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
    private boolean or(String node, List<Term> terms) {
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

    private UvLockPackage simplifyEntry(int index) {
        UvLockPackage pkg = packages.get(index);
        UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder();
        List<Term> own = reach.get(key(index, null));
        if (own == null) {
            return pkg;
        }
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
                List<Term> parent = reach.get(key(index, e.getKey()));
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
     * The edge with its marker restricted to what the parent's reachability leaves undecided.
     * A clause true under every reachable term goes, a term false under every one goes, and an
     * edge with no term left is null. A multi-term marker only survives untouched.
     */
    private @Nullable UvLockDependency restrict(UvLockDependency edge, List<Term> parent) {
        List<Term> terms = dnf(edge.getMarker());
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
        for (Term term : terms) {
            List<String> clauses = new ArrayList<>();
            boolean falseTerm = false;
            for (String[] c : term.pythonClauses) {
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
            for (String atom : term.atoms) {
                boolean allPresent = true;
                boolean allContradicted = true;
                for (Term t : parent) {
                    allPresent &= t.atoms.contains(atom);
                    allContradicted &= t.contradicts(atom);
                }
                if (allContradicted) {
                    falseTerm = true;
                } else if (!allPresent) {
                    clauses.add(atom);
                }
                changed |= allPresent || allContradicted;
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
