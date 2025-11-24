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
        List<DependencyGraph.DependencyNode> pathBuffer = new ArrayList<>();
        for (ResolvedDependency dependency : dependencies) {
            collectDependencyPathsRecursive(dependency, pathBuffer, paths, scope);
        }
    }

    private void collectDependencyPathsRecursive(ResolvedDependency dependency,
                                                 List<DependencyGraph.DependencyNode> pathBuffer,
                                                 Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> paths,
                                                 String scope) {
        ResolvedGroupArtifactVersion gav = dependency.getGav();

        T node = createNode(dependency, scope);
        pathBuffer.add(node); // Add at end for O(1) performance

        // Reverse pathBuffer to get correct order (current -> parents)
        // We add at end during traversal for O(1) performance, but need to reverse for storage
        List<DependencyGraph.DependencyNode> pathSnapshot = new ArrayList<>(pathBuffer.size());
        for (int i = pathBuffer.size() - 1; i >= 0; i--) {
            pathSnapshot.add(pathBuffer.get(i));
        }
        DependencyGraph.DependencyPath path = new DependencyGraph.DependencyPath(pathSnapshot, scope);
        paths.computeIfAbsent(gav, k -> new ArrayList<>()).add(path);

        for (ResolvedDependency child : dependency.getDependencies()) {
            boolean hasCycle = false;
            for (int i = 0; i < pathBuffer.size(); i++) {
                DependencyGraph.DependencyNode n = pathBuffer.get(i);
                if (n.getGroupId().equals(child.getGroupId()) &&
                    n.getArtifactId().equals(child.getArtifactId())) {
                    hasCycle = true;
                    break;
                }
            }
            if (!hasCycle) {
                collectDependencyPathsRecursive(child, pathBuffer, paths, scope);
            }
        }

        pathBuffer.remove(pathBuffer.size() - 1); // Backtrack for next iteration (O(1))
    }
}
