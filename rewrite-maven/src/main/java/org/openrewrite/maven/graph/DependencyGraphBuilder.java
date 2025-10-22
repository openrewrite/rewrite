/*
 * Copyright 2025 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.maven.graph;

import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.List;
import java.util.Map;

/**
 * Interface for building dependency graphs with type-safe node creation.
 *
 * @param <T> The type of dependency node this builder creates
 */
public interface DependencyGraphBuilder<T extends DependencyGraph.DependencyNode> {

    /**
     * Collects dependency paths for the given dependencies.
     *
     * @param dependencies The list of resolved dependencies to process
     * @param paths The map to store the collected paths
     * @param scope The scope or configuration name
     */
    void collectDependencyPaths(List<ResolvedDependency> dependencies,
                                Map<ResolvedGroupArtifactVersion, List<DependencyGraph.DependencyPath>> paths,
                                String scope);

    /**
     * Creates a dependency node from the given dependency.
     *
     * @param dependency The resolved dependency
     * @param scope The scope or configuration name
     * @return A new dependency node of type T
     */
    T createNode(ResolvedDependency dependency, String scope);
}
