package org.openrewrite.maven.utilities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.*;

public class DependencyInspector {

    public static DependencyInspectorDependency resolveDependencyTree(Maven maven) {
        return resolveDependencyTree(maven.getModel());
    }

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

        workQueue.addFirst(new DependencyTask(root, pom, null, Collections.emptySet()));
        while (!workQueue.isEmpty()) {
            DependencyTask task = workQueue.removeFirst();
            GroupArtifact ga = new GroupArtifact(task.dependency.groupId, task.dependency.artifactId);

            String resolvedVersion = seenArtifacts.get(ga);
            if (resolvedVersion == null) {
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

    public static String printDependencyTree(Maven maven) {
        return printDependencyTree(maven.getModel());
    }

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

        buffer/*.append(dependency.getGroupId()).append(':')*/.append(dependency.getArtifactId()).append(':').append(dependency.getOriginalVersion());
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
