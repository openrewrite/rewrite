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

import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for dependency graph builders that provides the common implementation
 * for collecting dependency paths.
 *
 * @param <T> The type of dependency node this builder creates
 */
public abstract class AbstractDependencyGraphBuilder<T extends DependencyGraph.DependencyNode>
        implements DependencyGraphBuilder<T> {

    @Override
    public void collectDependencyPaths(List<ResolvedDependency> dependencies,
                                       Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> paths,
                                       String scope) {
        for (ResolvedDependency dependency : dependencies) {
            List<DependencyGraph.DependencyNode> parentPath = new ArrayList<>();
            collectDependencyPathsRecursive(dependency, parentPath, paths, scope);
        }
    }

    private void collectDependencyPathsRecursive(ResolvedDependency dependency,
                                                 List<DependencyGraph.DependencyNode> parentPath,
                                                 Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> paths,
                                                 String scope) {
        ResolvedGroupArtifactVersion gav = dependency.getGav();

        // Create path for this dependency including all parents
        List<DependencyGraph.DependencyNode> pathNodes = new ArrayList<>();

        // Add the current dependency using the specific node type created by subclass
        T node = createNode(dependency, scope);
        pathNodes.add(node);

        // Add all parents from the parent path
        pathNodes.addAll(parentPath);

        DependencyGraph.DependencyPath path = new DependencyGraph.DependencyPath(pathNodes, scope);
        paths.computeIfAbsent(gav, k -> new ArrayList<>()).add(path);

        // Create new parent path for children that includes this dependency
        List<DependencyGraph.DependencyNode> newParentPath = new ArrayList<>();
        newParentPath.add(node);
        newParentPath.addAll(parentPath);

        // Recursively process child dependencies
        for (ResolvedDependency child : dependency.getDependencies()) {
            if (newParentPath.stream().noneMatch(childPath -> childPath.getGroupId().equals(child.getGroupId()) && childPath.getArtifactId().equals(child.getArtifactId()))) {
                collectDependencyPathsRecursive(child, newParentPath, paths, scope);
            }
        }
    }
}
