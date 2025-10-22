/*
 * Copyright 2025 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.maven.graph;

import org.openrewrite.maven.tree.ResolvedDependency;

/**
 * Gradle-specific implementation of DependencyGraphBuilder that creates GradleDependencyNode instances
 * which preserve the full ResolvedDependency object.
 */
public class GradleDependencyGraphBuilder extends AbstractDependencyGraphBuilder<DependencyGraph.GradleDependencyNode> {

    @Override
    public DependencyGraph.GradleDependencyNode createNode(ResolvedDependency dependency, String scope) {
        return new DependencyGraph.GradleDependencyNode(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                scope,
                dependency
        );
    }
}
