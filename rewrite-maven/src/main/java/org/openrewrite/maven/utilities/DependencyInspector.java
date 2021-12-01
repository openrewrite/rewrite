package org.openrewrite.maven.utilities;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.*;
import java.util.stream.Collectors;

public class DependencyInspector {

    public static DependencyInspectorDependency resolveDependencyTree(Maven maven) {
        return resolveDependencyTree(maven.getModel());
    }

    public static DependencyInspectorDependency resolveDependencyTree(@Nullable Pom pom) {
        if (pom == null) {
            return null;
        }
        return new DependencyInspectorDependency(
                pom.getGroupId(),
                pom.getArtifactId(),
                pom.getVersion(),
                pom.getVersion(),
                "",
                pom.getDependencies().stream().map(d -> DependencyInspector.resolveDependency(d, null, new HashSet<>())).collect(Collectors.toList()));
    }

    private static DependencyInspectorDependency resolveDependency(Pom.Dependency dependency, Scope parentScope, Set<String> seenArtifacts) {
        String gav = gav(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());

        Scope dependencyScope = parentScope == Scope.Test ? Scope.Test : dependency.getScope();
        List<DependencyInspectorDependency> childDependencies;
        if (seenArtifacts.add(gav)) {

            childDependencies = new ArrayList<>();
            for (Pom.Dependency child : dependency.getModel().getDependencies()) {
                if (dependency.getExclusions().contains(new GroupArtifact(child.getGroupId(), child.getArtifactId()))) {
                    //Do not add the child if it's in the exclusions of the containing dependency.
                    continue;
                }

                if (dependencyScope == null
                        || (dependencyScope == Scope.Test && child.getScope() == Scope.Compile)
                        || (dependencyScope == Scope.Compile && (child.getScope() == Scope.Compile || child.getScope() == Scope.Runtime))) {

                    childDependencies.add(resolveDependency(child, dependencyScope, seenArtifacts));
                }
            }
        } else {
            childDependencies = Collections.emptyList();
        }

        return new DependencyInspectorDependency(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getRequestedVersion(),
                dependencyScope.name().toLowerCase(),
                childDependencies);
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

        buffer.append(dependency.getId());
        if (!dependency.getScope().isEmpty()) {
            buffer.append('[').append(dependency.scope).append(']');
        }
        buffer.append('\n');
        int index = 0;
        for (DependencyInspectorDependency child : dependency.getDependencies()) {
            printDependencyTree(buffer, child, indent, index == dependency.getDependencies().size() -1);
            index++;
        }
    }

    @Value
    public static class DependencyInspectorDependency {

        private String id;
        private String groupId;
        private String artifactId;
        private String version;
        private String originalVersion;
        private String scope;
        private List<DependencyInspectorDependency> dependencies;

        public DependencyInspectorDependency(String groupId, String artifactId, String version, String originalVersion, String scope, List<DependencyInspectorDependency> dependencies) {
            this.id = gav(groupId, artifactId, version);
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.originalVersion = originalVersion;
            this.scope = scope;
            this.dependencies = dependencies;
        }
    }

    private static String gav(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }
}
