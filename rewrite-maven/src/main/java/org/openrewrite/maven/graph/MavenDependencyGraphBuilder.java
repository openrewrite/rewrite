/*
 * Copyright 2025 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.maven.graph;

import org.openrewrite.maven.tree.ResolvedDependency;

/**
 * Maven-specific implementation of DependencyGraphBuilder that creates standard DependencyNode instances.
 */
public class MavenDependencyGraphBuilder extends AbstractDependencyGraphBuilder<DependencyGraph.DependencyNode> {

    @Override
    public DependencyGraph.DependencyNode createNode(ResolvedDependency dependency, String scope) {
        return new DependencyGraph.DependencyNode(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                scope
        );
    }
}
