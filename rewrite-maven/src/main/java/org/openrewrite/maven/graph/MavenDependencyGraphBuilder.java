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
