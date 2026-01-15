/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.graph;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;

import java.util.*;

/**
 * Walks a dependency tree lazily, finding dependencies that match a given matcher.
 * Uses depth-first pre-order traversal with cycle detection to avoid infinite loops.
 * Pre-order means parent dependencies are visited before their children.
 * <p>
 * This class encapsulates the common pattern used by dependency insight recipes
 * to traverse dependency trees without creating intermediate objects for
 * non-matching dependencies, reducing GC pressure on large dependency graphs.
 */
public class DependencyTreeWalker {
    /**
     * Callback interface for handling dependencies during tree traversal.
     */
    @FunctionalInterface
    public interface Callback {
        /**
         * Called when a dependency is visited or matches a pattern.
         * <p>
         * The path is in <b>leaf-to-root order</b>: {@code path.getFirst()} returns the current
         * dependency, {@code path.getLast()} returns the direct (root) dependency.
         *
         * @param dependency the current dependency being visited/matched
         * @param path       the path from this dependency to direct dependency (leaf-to-root order);
         *                   use {@code getLast()} for the direct dependency, iterate normally for leaf-to-root
         */
        void accept(ResolvedDependency dependency, Deque<ResolvedDependency> path);
    }

    /**
     * Walks the dependency tree starting from the given roots, calling the callback
     * for each dependency that matches the matcher (or all dependencies if matcher is null).
     * Uses caching to avoid re-traversing duplicate dependencies.
     *
     * @param roots    the direct dependencies to start traversal from
     * @param matcher  the matcher to test dependencies against, or null to visit all
     * @param callback called for each matching dependency with its path
     */
    public static void walk(List<ResolvedDependency> roots, @Nullable DependencyMatcher matcher, Callback callback) {
        Deque<ResolvedDependency> path = new ArrayDeque<>();
        for (ResolvedDependency root : roots) {
            walkRecursive(root, matcher, path, callback);
        }
    }

    /**
     * Walks a single dependency tree, calling the callback for each match.
     *
     * @param root     the direct dependency to start traversal from
     * @param matcher  the matcher to test dependencies against, or null to visit all
     * @param callback called for each matching dependency with its path
     */
    public static void walk(ResolvedDependency root, @Nullable DependencyMatcher matcher, Callback callback) {
        Deque<ResolvedDependency> path = new ArrayDeque<>();
        walkRecursive(root, matcher, path,  callback);
    }

    private static void walkRecursive(
            ResolvedDependency dependency,
            @Nullable DependencyMatcher matcher,
            Deque<ResolvedDependency> path,
            Callback callback
    ) {
        // Cycle detection - check if we've already visited this dependency in the current path
        if (containsDependency(path, dependency)) {
            return;
        }

        path.addFirst(dependency);
        if (matcher == null || matcher.matches(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
            callback.accept(dependency, new ArrayDeque<>(path));
        }

        for (ResolvedDependency child : dependency.getDependencies()) {
            walkRecursive(child, matcher, path, callback);
        }
        path.removeFirst();
    }

    private static boolean containsDependency(Deque<ResolvedDependency> path, ResolvedDependency dependency) {
        for (ResolvedDependency dep : path) {
            if (dep.getGroupId().equals(dependency.getGroupId()) &&
                dep.getArtifactId().equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders a dependency path as an inverse tree string for display.
     * Shows the matched dependency at the top, with parents indented below,
     * ending with the scope/configuration.
     * <p>
     * Example output for a transitive dependency:
     * <pre>
     * matched:artifact:1.0
     * \--- parent:artifact:2.0
     *      \--- direct:artifact:3.0
     *           \--- compile
     * </pre>
     *
     * @param scopeOrConfig  the scope (Maven) or configuration name (Gradle)
     * @param dependencyPath the path in leaf-to-root order (matched dependency first, direct dependency last)
     * @return formatted inverse dependency tree string
     */
    public static String renderPath(String scopeOrConfig, Deque<ResolvedDependency> dependencyPath) {
        if (dependencyPath.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (ResolvedDependency dep : dependencyPath) {
            if (depth == 0) {
                sb.append(formatDependency(dep.getGav()));
            } else {
                appendIndentedLine(sb, depth - 1, formatDependency(dep.getGav()));
            }
            depth++;
        }
        appendIndentedLine(sb, depth - 1, scopeOrConfig);
        return sb.toString();
    }

    private static void appendIndentedLine(StringBuilder tree, int depth, String content) {
        tree.append("\n");
        for (int i = 0; i < depth; i++) {
            tree.append("     ");
        }
        tree.append("\\--- ").append(content);
    }

    private static String formatDependency(ResolvedGroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    /**
     * Collects dependency match information while walking the dependency tree.
     * This collector builds two maps that track:
     * <ul>
     *   <li>Which scopes/configurations contain direct dependencies that match or transitively depend on matches</li>
     *   <li>Which target dependencies are reachable from each direct dependency</li>
     * </ul>
     * <p>
     * This is the pattern used by dependency insight recipes to mark direct dependencies
     * based on their transitive dependencies.
     *
     * @param <S> the scope type (e.g., Maven Scope or Gradle configuration name String)
     */
    public static class Matches<S> {
        private final Map<S, Set<GroupArtifactVersion>> scopeToDirectDependency = new HashMap<>();
        private final Map<GroupArtifactVersion, Set<GroupArtifactVersion>> directDependencyToTargetDependency = new HashMap<>();

        /**
         * Walks the dependency tree for a given scope, collecting match mappings.
         *
         * @param scope      the scope or configuration name
         * @param root       the direct dependency to walk from
         * @param matcher    the matcher to test dependencies against
         * @param callback   optional callback for additional per-match processing (may be null)
         */
        public void collect(S scope, ResolvedDependency root, DependencyMatcher matcher, @Nullable Callback callback) {
            walk(root, matcher, (matched, path) -> {
                // path is in leaf-to-root order: getLast() gives direct dependency
                ResolvedDependency directDependency = path.getLast();
                GroupArtifactVersion directGav = directDependency.getGav().asGroupArtifactVersion();
                GroupArtifactVersion matchedGav = matched.getGav().asGroupArtifactVersion();

                scopeToDirectDependency.computeIfAbsent(scope, __ -> new HashSet<>()).add(directGav);
                directDependencyToTargetDependency.computeIfAbsent(directGav, __ -> new HashSet<>()).add(matchedGav);

                if (callback != null) {
                    callback.accept(matched, path);
                }
            });
        }

        /**
         * Returns the mapping from scope to direct dependencies that have matching transitives.
         */
        public Map<S, Set<GroupArtifactVersion>> byScope() {
            return scopeToDirectDependency;
        }

        /**
         * Returns the mapping from direct dependency to the transitive dependencies that matched.
         */
        public Map<GroupArtifactVersion, Set<GroupArtifactVersion>> byDirectDependency() {
            return directDependencyToTargetDependency;
        }

        /**
         * Returns true if no matches were found.
         */
        public boolean isEmpty() {
            return directDependencyToTargetDependency.isEmpty();
        }
    }
}
