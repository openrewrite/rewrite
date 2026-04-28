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
package org.openrewrite.java.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.TypeNameMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypesInUse {
    private final JavaSourceFile cu;
    private final Set<JavaType> typesInUse;
    private final Set<JavaType.Method> declaredMethods;
    private final Set<JavaType.Method> usedMethods;
    private final Set<JavaType.Variable> variables;

    /**
     * Lazily-built prefix tree over every fully qualified name reachable via
     * {@link TypeUtils#isAssignableTo(String, JavaType)} starting from any type referenced in this
     * compilation unit. Each terminating node carries two pairs of bits per visibility:
     * <ul>
     *   <li>{@code leaf*} — a real FQN ends here in raw form ({@code $} preserved as part of an
     *       inner-class segment). {@link #hasTypeInPackage} and {@link #hasTypeInPackageOrSubpackage}
     *       check these.</li>
     *   <li>{@code alias*} — the canonical form of an inner-class FQN ({@code $} replaced with
     *       {@code .}) ends here. Lets {@link #hasType} match either form against
     *       {@link TypeUtils#fullyQualifiedNamesAreEqual} semantics without confusing package-shape
     *       queries.</li>
     * </ul>
     * Visibility splits each pair into explicit (reachable from types-in-use or imports — visible
     * without {@code includeImplicit}) and implicit (reachable only through used-method types).
     */
    @Nullable
    private volatile FqnTrie trie;

    /**
     * Per-{@link TypeNameMatcher} memo for {@link #hasTypeMatching}. Lazily allocated; keyed by
     * the matcher's pattern combined with the {@code includeImplicit} dimension.
     */
    @Nullable
    private volatile Map<String, Boolean> typeMatchingCache;

    public static TypesInUse build(JavaSourceFile cu) {
        FindTypesInUse findTypesInUse = new FindTypesInUse();
        findTypesInUse.visit(cu, 0);
        return new TypesInUse(cu,
                findTypesInUse.getTypes(),
                findTypesInUse.getDeclaredMethods(),
                findTypesInUse.getUsedMethods(),
                findTypesInUse.getVariables());
    }

    /**
     * Whether any type referenced in this compilation unit is assignable to {@code fullyQualifiedType}.
     * Mirrors the loop in {@code UsesType.visit}: types-in-use, imports, and (when {@code includeImplicit})
     * the declaring/return/parameter types of used methods.
     * <p>
     * The first call materializes the closure for the file and caches it; subsequent calls are O(1).
     * {@code $} and {@code .} are treated as equivalent in inner-class FQNs, matching
     * {@link TypeUtils#fullyQualifiedNamesAreEqual(String, String)}.
     */
    public boolean hasType(String fullyQualifiedType, boolean includeImplicit) {
        // Canonicalize the query so $-form lookups land on the alias path that mirrors the .-form.
        String canonical = fullyQualifiedType.indexOf('$') < 0 ? fullyQualifiedType : fullyQualifiedType.replace('$', '.');
        return getOrBuildTrie().hasFqn(canonical, includeImplicit);
    }

    /**
     * Whether this compilation unit references any type whose package equals {@code packageName}
     * exactly (i.e., the {@code com.foo.*} pattern). Navigation to the package node is O(depth);
     * matching is a constant-time check for any real-leaf child (alias children are deliberately
     * ignored, so {@code com.foo.Outer.Inner} from a canonicalized inner-class FQN does not
     * register {@code com.foo.Outer} as a package).
     */
    public boolean hasTypeInPackage(String packageName, boolean includeImplicit) {
        return getOrBuildTrie().hasLeafAtDepth(packageName, includeImplicit);
    }

    /**
     * Whether this compilation unit references any type whose package equals {@code packageName}
     * or starts with {@code packageName + '.'} (i.e., the {@code com.foo..*} pattern). The
     * descendant-leaf rollup tracks only real leaves, so the answer is O(depth) regardless of
     * trie size.
     */
    public boolean hasTypeInPackageOrSubpackage(String packageName, boolean includeImplicit) {
        return getOrBuildTrie().hasAnyLeafBelow(packageName, includeImplicit);
    }

    /**
     * Whether any FQN reachable through the assignability closure of this compilation unit
     * matches {@code matcher}. Walks every raw-leaf path in the trie and tests the matcher
     * against the reconstructed FQN — equivalent to the per-element loop in {@code UsesType}'s
     * pre-cache implementation, but iterated once per (file, matcher) pair regardless of how
     * many recipe instances ask. Alias paths are excluded so canonical inner-class FQNs only
     * match through their raw form, mirroring the semantics of {@link #hasTypeInPackage}.
     */
    public boolean hasTypeMatching(TypeNameMatcher matcher, boolean includeImplicit) {
        Map<String, Boolean> cache = typeMatchingCache;
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            typeMatchingCache = cache;
        }
        String key = (includeImplicit ? "+" : "-") + matcher;
        return cache.computeIfAbsent(key, k -> getOrBuildTrie().anyLeafFqn(matcher, includeImplicit));
    }

    /**
     * Whether this compilation unit invokes any method matching {@code matcher}.
     */
    public boolean hasMethodUse(MethodMatcher matcher) {
        for (JavaType.Method m : usedMethods) {
            if (matcher.matches(m)) {
                return true;
            }
        }
        return false;
    }

    /** Whether this compilation unit declares any method matching {@code matcher}. */
    public boolean declaresMethod(MethodMatcher matcher) {
        for (JavaType.Method m : declaredMethods) {
            if (matcher.matches(m)) {
                return true;
            }
        }
        return false;
    }

    private FqnTrie getOrBuildTrie() {
        FqnTrie t = trie;
        if (t == null) {
            t = buildTrie();
            trie = t;
        }
        return t;
    }

    private FqnTrie buildTrie() {
        FqnTrie t = new FqnTrie();
        // Explicit pass first: types-in-use and imports. Reachable here ⇒ visible without includeImplicit.
        Set<String> visited = new HashSet<>();
        for (JavaType type : typesInUse) {
            JavaType checkType = type instanceof JavaType.Primitive ? type : TypeUtils.asFullyQualified(type);
            walkAssignableTo(checkType, t, true, visited);
        }
        for (J.Import anImport : cu.getImports()) {
            JavaType target = anImport.isStatic()
                    ? anImport.getQualid().getTarget().getType()
                    : anImport.getQualid().getType();
            walkAssignableTo(TypeUtils.asFullyQualified(target), t, true, visited);
        }
        // Implicit pass: used methods' declaring/return/parameter types. Only adds FQNs the
        // explicit pass didn't already reach; if the explicit pass already added an FQN with
        // explicit=true, the trie's per-bit promotion rules keep that visibility.
        for (JavaType.Method method : usedMethods) {
            walkAssignableTo(method.getDeclaringType(), t, false, visited);
            walkAssignableTo(method.getReturnType(), t, false, visited);
            for (JavaType pt : method.getParameterTypes()) {
                walkAssignableTo(pt, t, false, visited);
            }
        }
        return t;
    }

    /**
     * Walks the assignability chain of {@code from} and inserts each reached FQN into the trie.
     * Each FQN is inserted in raw form (with {@code $} preserved as part of an inner-class
     * segment) so package-shape queries see the correct package/class boundary; in addition,
     * inner-class FQNs are also inserted as canonical-form aliases so exact lookups by either
     * {@code .} or {@code $} form land on the same node. Mirrors the cases in
     * {@link TypeUtils#isAssignableTo(String, JavaType)}.
     */
    private static void walkAssignableTo(@Nullable JavaType from, FqnTrie trie, boolean explicit, Set<String> visited) {
        if (from == null) {
            return;
        }
        try {
            if (from instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fq = (JavaType.FullyQualified) from;
                if (from instanceof JavaType.Parameterized) {
                    String parameterized = from.toString();
                    //noinspection ConstantConditions
                    if (parameterized != null && visited.add(parameterized)) {
                        trie.insert(parameterized, explicit);
                    }
                }
                String fqn = fq.getFullyQualifiedName();
                if (visited.add(fqn)) {
                    trie.insert(fqn, explicit);
                    walkAssignableTo(fq.getSupertype(), trie, explicit, visited);
                    for (JavaType.FullyQualified i : fq.getInterfaces()) {
                        walkAssignableTo(i, trie, explicit, visited);
                    }
                }
            } else if (from instanceof JavaType.GenericTypeVariable) {
                for (JavaType bound : ((JavaType.GenericTypeVariable) from).getBounds()) {
                    walkAssignableTo(bound, trie, explicit, visited);
                }
                if (visited.add("java.lang.Object")) {
                    trie.insert("java.lang.Object", explicit);
                }
            } else if (from instanceof JavaType.Primitive) {
                JavaType.Primitive p = (JavaType.Primitive) from;
                String keyword = p.getKeyword();
                if (keyword != null && visited.add(keyword)) {
                    trie.insert(keyword, explicit);
                }
                if (p == JavaType.Primitive.String && visited.add("java.lang.String")) {
                    trie.insert("java.lang.String", explicit);
                }
                if (visited.add("java.lang.Object")) {
                    trie.insert("java.lang.Object", explicit);
                }
            } else if (from instanceof JavaType.Variable) {
                walkAssignableTo(((JavaType.Variable) from).getType(), trie, explicit, visited);
            } else if (from instanceof JavaType.Method) {
                walkAssignableTo(((JavaType.Method) from).getReturnType(), trie, explicit, visited);
            } else if (from instanceof JavaType.Intersection) {
                for (JavaType bound : ((JavaType.Intersection) from).getBounds()) {
                    walkAssignableTo(bound, trie, explicit, visited);
                }
                if (visited.add("java.lang.Object")) {
                    trie.insert("java.lang.Object", explicit);
                }
            } else if (visited.add("java.lang.Object")) {
                trie.insert("java.lang.Object", explicit);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Prefix tree over fully qualified names, tokenized on {@code .} only. Each FQN is inserted
     * twice when it contains {@code $}: once raw (so {@code com.foo.Outer$Inner} is the three
     * segments {@code [com, foo, Outer$Inner]}, with the inner-class boundary preserved for
     * package-shape queries) and once as a canonical alias ({@code [com, foo, Outer, Inner]}, so
     * exact-FQN lookups by either form land on the same terminating node).
     * <p>
     * Each node tracks four visibility bits — leaf vs alias, each with explicit vs implicit —
     * plus a descendant-explicit/-implicit rollup that propagates only on the raw insertion path,
     * so {@code com.foo..*} queries don't get false positives from canonical alias paths.
     */
    private static final class FqnTrie {
        private final Node root = new Node();

        /**
         * Insert {@code fqn} as a real leaf at its raw path. If it contains {@code $}, additionally
         * insert the canonicalized path as an alias leaf so {@link #hasFqn} answers either form.
         */
        void insert(String fqn, boolean explicit) {
            if (fqn.isEmpty()) {
                return;
            }
            insertPath(fqn, explicit, false);
            if (fqn.indexOf('$') >= 0) {
                insertPath(fqn.replace('$', '.'), explicit, true);
            }
        }

        private void insertPath(String path, boolean explicit, boolean alias) {
            Node n = root;
            int start = 0;
            int len = path.length();
            for (int i = 0; i <= len; i++) {
                if (i == len || path.charAt(i) == '.') {
                    // Descendant rollup propagates only for raw-leaf insertions; alias paths do
                    // not contribute to "this package has any class beneath it" queries.
                    if (!alias) {
                        if (explicit) {
                            n.descendantExplicit = true;
                        } else {
                            n.descendantImplicit = true;
                        }
                    }
                    n = n.findOrAddChild(path, start, i);
                    start = i + 1;
                }
            }
            if (alias) {
                if (explicit) {
                    n.aliasExplicit = true;
                } else if (!n.aliasExplicit) {
                    n.aliasImplicit = true;
                }
            } else {
                if (explicit) {
                    n.leafExplicit = true;
                } else if (!n.leafExplicit) {
                    n.leafImplicit = true;
                }
            }
        }

        /** Exact-FQN lookup. The query is expected in canonical form (no {@code $}). */
        boolean hasFqn(String canonicalFqn, boolean includeImplicit) {
            Node n = navigate(canonicalFqn);
            if (n == null) {
                return false;
            }
            if (n.leafExplicit || n.aliasExplicit) {
                return true;
            }
            return includeImplicit && (n.leafImplicit || n.aliasImplicit);
        }

        /** {@code com.foo.*}: any FQN whose package is exactly {@code pkg}. */
        boolean hasLeafAtDepth(String pkg, boolean includeImplicit) {
            Node n = navigate(pkg);
            if (n == null) {
                return false;
            }
            for (int i = 0; i < n.childCount; i++) {
                Node child = n.childNodes[i];
                // Deliberately ignore alias bits: a canonicalized inner-class path like
                // [com, foo, Outer, Inner] must not let `com.foo.Outer.*` match.
                if (child.leafExplicit || (includeImplicit && child.leafImplicit)) {
                    return true;
                }
            }
            return false;
        }

        /** {@code com.foo..*}: any FQN whose package starts with {@code pkg + '.'} (or equals {@code pkg}). */
        boolean hasAnyLeafBelow(String pkg, boolean includeImplicit) {
            Node n = navigate(pkg);
            if (n == null) {
                return false;
            }
            // Deliberately exclude n.leaf*/n.alias* — `pkg..*` matches strict subpackages of pkg,
            // not pkg itself. Descendant flags only track raw-leaf insertions.
            return n.descendantExplicit || (includeImplicit && n.descendantImplicit);
        }

        /**
         * Walks every raw-leaf FQN reachable in this trie and returns {@code true} on the first
         * one that {@code matcher} accepts. Alias paths are skipped so canonicalized inner-class
         * FQNs only match through their raw form, matching {@link TypeUtils#isAssignableTo}'s
         * semantics for the legacy {@code GenericPattern} iteration.
         */
        boolean anyLeafFqn(TypeNameMatcher matcher, boolean includeImplicit) {
            return anyLeafFqn(root, new StringBuilder(), matcher, includeImplicit);
        }

        private static boolean anyLeafFqn(Node node, StringBuilder fqn, TypeNameMatcher matcher, boolean includeImplicit) {
            if ((node.leafExplicit || (includeImplicit && node.leafImplicit)) && matcher.matches(fqn.toString())) {
                return true;
            }
            for (int i = 0; i < node.childCount; i++) {
                int len = fqn.length();
                if (len > 0) {
                    fqn.append('.');
                }
                fqn.append(node.childSegments[i]);
                if (anyLeafFqn(node.childNodes[i], fqn, matcher, includeImplicit)) {
                    return true;
                }
                fqn.setLength(len);
            }
            return false;
        }

        private @Nullable Node navigate(String pkg) {
            if (pkg.isEmpty()) {
                return root;
            }
            Node n = root;
            int start = 0;
            int len = pkg.length();
            for (int i = 0; i <= len; i++) {
                if (i == len || pkg.charAt(i) == '.') {
                    n = n.findChild(pkg, start, i);
                    if (n == null) {
                        return null;
                    }
                    start = i + 1;
                }
            }
            return n;
        }

        /**
         * Trie node with parallel arrays for children instead of a {@link java.util.HashMap}, so
         * {@link #findChild} can locate a child via {@link String#regionMatches} without
         * allocating a substring of the query string. Average branching for FQN tries is small
         * (typically 1–10), so the linear scan is faster than a hashed lookup once you account
         * for the avoided allocation.
         */
        private static final class Node {
            private static final String[] EMPTY_SEGMENTS = new String[0];
            private static final Node[] EMPTY_NODES = new Node[0];

            String[] childSegments = EMPTY_SEGMENTS;
            Node[] childNodes = EMPTY_NODES;
            int childCount;

            boolean leafExplicit;
            boolean leafImplicit;
            boolean aliasExplicit;
            boolean aliasImplicit;
            boolean descendantExplicit;
            boolean descendantImplicit;

            /** Locate a child by the segment {@code path[start, end)}; allocation-free. */
            @Nullable Node findChild(String path, int start, int end) {
                int len = end - start;
                for (int i = 0; i < childCount; i++) {
                    String seg = childSegments[i];
                    if (seg.length() == len && path.regionMatches(start, seg, 0, len)) {
                        return childNodes[i];
                    }
                }
                return null;
            }

            /** Insertion-time helper: find or grow. Allocates the segment string once. */
            Node findOrAddChild(String path, int start, int end) {
                int len = end - start;
                for (int i = 0; i < childCount; i++) {
                    String seg = childSegments[i];
                    if (seg.length() == len && path.regionMatches(start, seg, 0, len)) {
                        return childNodes[i];
                    }
                }
                if (childCount == childSegments.length) {
                    int newCap = childSegments.length == 0 ? 4 : childSegments.length * 2;
                    childSegments = childSegments.length == 0 ? new String[newCap] : Arrays.copyOf(childSegments, newCap);
                    childNodes = childNodes.length == 0 ? new Node[newCap] : Arrays.copyOf(childNodes, newCap);
                }
                Node child = new Node();
                childSegments[childCount] = path.substring(start, end);
                childNodes[childCount] = child;
                childCount++;
                return child;
            }
        }
    }

    @Getter
    public static class FindTypesInUse extends JavaIsoVisitor<Integer> {
        private final Set<JavaType> types = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> declaredMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> usedMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Variable> variables = newSetFromMap(new IdentityHashMap<>());

        @Override
        public J.Import visitImport(J.Import _import, Integer p) {
            return _import;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            Object parent = Objects.requireNonNull(getCursor().getParent()).getValue();
            if (parent instanceof J.ClassDeclaration) {
                // skip type of class
                return identifier;
            } else if (parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == identifier) {
                // skip method name
                return identifier;
            }
            return super.visitIdentifier(identifier, p);
        }

        @Override
        public J.Lambda.Parameters visitLambdaParameters(J.Lambda.Parameters parameters, Integer integer) {
            for (J j : parameters.getParameters()) {
                if (j instanceof J.VariableDeclarations && ((J.VariableDeclarations) j).getTypeExpression() == null) {
                    // Type is inferred, so no need to visit type to retain import statements
                    return parameters;
                }
                // We only need to check the first parameter, as the rest will be using the same
                break;
            }
            return super.visitLambdaParameters(parameters, integer);
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, Integer p) {
            if (javaType != null && !(javaType instanceof JavaType.Unknown)) {
                Cursor cursor = getCursor();
                if (javaType instanceof JavaType.Variable) {
                    variables.add((JavaType.Variable) javaType);
                } else if (javaType instanceof JavaType.Method) {
                    if (cursor.getValue() instanceof J.MethodDeclaration) {
                        declaredMethods.add((JavaType.Method) javaType);
                    } else {
                        usedMethods.add((JavaType.Method) javaType);
                    }
                } else if (!(cursor.getValue() instanceof J.ClassDeclaration) &&
                        !(cursor.getValue() instanceof J.Lambda) &&
                        !isFullyQualifiedJavaDocReference(cursor)) {
                    types.add(javaType);
                }
            }
            return javaType;
        }

        private boolean isFullyQualifiedJavaDocReference(Cursor cursor) {
            // Fully qualified Javadoc references are _using_ those types as much as they are just references;
            // TypesInUse entries determines what imports are retained, and for fully qualified these can be dropped
            if (cursor.getValue() instanceof J.FieldAccess) {
                Iterator<Object> path = cursor.getPath();
                while (path.hasNext()) {
                    Object o = path.next();
                    if (o instanceof Javadoc.Reference) {
                        return true;
                    }
                    if (o instanceof J.Block) {
                        return false;
                    }
                }
            }
            return false;
        }
    }
}
