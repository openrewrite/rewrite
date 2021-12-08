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
package org.openrewrite.maven.utilities;

import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.*;

public class DependencyInspector {

    /**
     * Given a Maven source file, this method returns a dependency tree of the file by navigating the pom model and
     * its transitive dependencies. This method produces results that have close parity with those found in Eclipse's pom
     * editor (Dependency Hierarchy). Each returned dependency reflects the original version, defined in the raw pom, along
     * with the version that was resolved. Any duplicate dependencies that are encountered deeper in the dependency tree
     * will have their children pruned to reduce the size of the returned hierarchy.
     *
     * @param maven A maven source file whose model that will be traversed to collect transitive dependencies.
     * @return A dependency hierarchy representing the resolved, transitive dependencies of the maven file.
     */
    public static DependencyInspectorDependency resolveDependencyTree(Maven maven) {
        return resolveDependencyTree(maven.getModel());
    }

    /**
     * Given a Pom model this method returns a dependency tree of the pom by navigating the model and its transitive
     * dependencies. This method produces results that have close parity with those found in Eclipse's pom editor
     * (Dependency Hierarchy). Each returned dependency reflects the original version, defined in the raw pom, along with
     * the version that was resolved. Any duplicate dependencies that are encountered deeper in the dependency tree will
     * have their children pruned to reduce the size of the returned hierarchy.
     *
     * @param pom A root pom model that will be traversed to collect transitive dependencies.
     * @return A dependency hierarchy representing the resolved, transitive dependencies of the root pom.
     */
    public static DependencyInspectorDependency resolveDependencyTree(@Nullable Pom pom) {
        if (pom == null) {
            return null;
        }

        DependencyInspectorDependency root = new DependencyInspectorDependency(
                pom.getGroupId(),
                pom.getArtifactId(),
                pom.getVersion(),
                pom.getVersion(),
                "");

        Map<GroupArtifact, String> seenArtifacts = new HashMap<>();
        Deque<DependencyTask> workQueue = new ArrayDeque<>();

        // Breadth first navigation of the dependencies.
        workQueue.addFirst(new DependencyTask(root, pom, null, Collections.emptySet()));
        while (!workQueue.isEmpty()) {
            DependencyTask task = workQueue.removeFirst();
            GroupArtifact ga = new GroupArtifact(task.dependency.groupId, task.dependency.artifactId);

            String resolvedVersion = seenArtifacts.get(ga);
            if (resolvedVersion == null) {
                // The children of an artifact are only traversed the first time the artifact is encountered, this
                // matches how Eclipse's pom editor prunes the children of duplicate artifacts when they are encountered
                // deeper in the dependency tree.
                resolvedVersion = task.dependency.resolvedVersion;
                seenArtifacts.putIfAbsent(ga, resolvedVersion);

                List<DependencyInspectorDependency> children = new ArrayList<>(task.pom.getDependencies().size());
                for (Pom.Dependency dependency : task.pom.getDependencies()) {
                    GroupArtifact childGav = new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId());
                    if (task.exclusions.contains(childGav)) {
                        //Do not add the child if it's in the exclusions of the containing dependency.
                        continue;
                    }
                    Scope childScope = dependency.getScope() == null ? Scope.Compile : dependency.getScope();

                    if (task.scope == null || ((task.scope == Scope.Test || task.scope == Scope.Compile) &&
                            (dependency.getScope() == Scope.Compile || dependency.getScope() == Scope.Runtime))) {
                        if (task.scope == Scope.Test) {
                            childScope = Scope.Test;
                        }
                        String childVersion = dependency.getRequestedVersion() != null ? dependency.getRequestedVersion() : dependency.getVersion();
                        DependencyInspectorDependency child = new DependencyInspectorDependency(
                                task.pom.getValue(dependency.getGroupId()),
                                task.pom.getValue(dependency.getArtifactId()),
                                task.pom.getValue(childVersion),
                                task.pom.getValue(childVersion),
                                childScope.name().toLowerCase());
                        children.add(child);
                        workQueue.addLast(new DependencyTask(child, dependency.getModel(), childScope, dependency.getExclusions()));
                    }
                }
                if (!children.isEmpty()) {
                    task.dependency.dependencies = children;
                }
            } else {
                task.dependency.resolvedVersion = resolvedVersion;
            }
        }
        return root;
    }

    /**
     * Given a maven source file, this method returns a string representation of the dependency hierarchy by navigating
     * the file's model and its transitive dependencies. This method produces results that have close parity with those
     * found in Eclipse's pom editor (Dependency Hierarchy). Each returned dependency reflects the resolved version and notes
     * any conflicts between the resolved and original version. Any duplicate dependencies that are encountered deeper
     * in the dependency tree will have their children pruned to reduce the size of the returned hierarchy.
     *
     * @param maven A maven source file whose model that will be traversed to collect transitive dependencies.
     * @return A dependency hierarchy representing the resolved, transitive dependencies of the maven file.
     */
    public static String printDependencyTree(Maven maven) {
        return printDependencyTree(maven.getModel());
    }

    /**
     * Given a Pom model, this method returns a string representation of the dependency hierarchy by navigating the
     * model and its transitive dependencies. This method produces results that have close parity with those found in
     * Eclipse's pom editor (Dependency Hierarchy). Each returned dependency reflects the resolved version and notes
     * any conflicts between the resolved and original version. Any duplicate dependencies that are encountered deeper
     * in the dependency tree will have their children pruned to reduce the size of the returned hierarchy.
     *
     * @param pom A root pom model that will be traversed to collect transitive dependencies.
     * @return A dependency hierarchy representing the resolved, transitive dependencies of the root pom.
     */
    public static String printDependencyTree(@Nullable Pom pom) {

        DependencyInspectorDependency dependencyTree = resolveDependencyTree(pom);

        StringBuilder buffer = new StringBuilder(1024);
        printDependencyTree(buffer, dependencyTree, "", true);
        return buffer.toString();
    }

    private static void printDependencyTree(StringBuilder buffer, DependencyInspectorDependency dependency, String indent, boolean isLast) {
        buffer.append(indent);
        if (!indent.isEmpty()) {
            buffer.append("|-");
        }
        if (!isLast) {
            indent = indent + "|   ";
        } else {
            indent = indent + "    ";
        }

        buffer.append(dependency.getGroupId()).append(':').append(dependency.getArtifactId()).append(':').append(dependency.getOriginalVersion());
        if (!dependency.getResolvedVersion().equals(dependency.getOriginalVersion())) {
            buffer.append(" (omitted for conflict with ").append(dependency.getResolvedVersion()).append (')');
        }
        if (!dependency.getScope().isEmpty()) {
            buffer.append(" [").append(dependency.scope).append(']');
        }
        buffer.append('\n');
        int index = 0;
        for (DependencyInspectorDependency child : dependency.getDependencies()) {
            printDependencyTree(buffer, child, indent, index == dependency.getDependencies().size() -1);
            index++;
        }
    }

    private static class DependencyTask {

        private DependencyInspectorDependency dependency;
        private Pom pom;
        @Nullable
        private Scope scope;
        private Set<GroupArtifact> exclusions;

        private DependencyTask(DependencyInspectorDependency dependency, Pom pom, @Nullable Scope scope, Set<GroupArtifact> exclusions) {
            this.dependency = dependency;
            this.pom = pom;
            this.scope = scope;
            this.exclusions = exclusions;
        }
    }

    @Getter
    public static class DependencyInspectorDependency {

        private final String groupId;
        private final String artifactId;
        private String resolvedVersion;
        private final String originalVersion;
        private final String scope;
        private List<DependencyInspectorDependency> dependencies = Collections.emptyList();

        public DependencyInspectorDependency(String groupId, String artifactId, String resolvedVersion, String originalVersion, String scope) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.resolvedVersion = resolvedVersion;
            this.originalVersion = originalVersion;
            this.scope = scope;
        }
    }

    private static String gav(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }
}
