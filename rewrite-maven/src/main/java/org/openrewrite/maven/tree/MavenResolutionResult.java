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
package org.openrewrite.maven.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.internal.MavenPomDownloader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
public class MavenResolutionResult implements Marker {
    @EqualsAndHashCode.Include
    @With
    UUID id;

    @With
    ResolvedPom pom;

    /**
     * Resolution results of POMs in this repository that hold this POM as a parent.
     */
    @With
    List<MavenResolutionResult> modules;

    @Nullable
    @NonFinal
    MavenResolutionResult parent;

    @With
    Map<Scope, List<ResolvedDependency>> dependencies;

    @Incubating(since = "7.18.0")
    @Nullable
    public ResolvedDependency getResolvedDependency(Dependency dependency) {
        for (int i = Scope.values().length - 1; i >= 0; i--) {
            Scope scope = Scope.values()[i];
            if (dependencies.containsKey(scope)) {
                for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                    if (resolvedDependency.getRequested() == dependency) {
                        return resolvedDependency;
                    }
                }
            }
        }
        return null;
    }

    public void unsafeSetParent(MavenResolutionResult parent) {
        this.parent = parent;
    }

    @Incubating(since = "7.18.0")
    @Nullable
    public ResolvedManagedDependency getResolvedManagedDependency(ManagedDependency dependency) {
        for (ResolvedManagedDependency dm : pom.getDependencyManagement()) {
            if (dm.getRequested() == dependency || dm.getRequestedBom() == dependency) {
                return dm;
            }
        }
        return null;
    }

    public MavenResolutionResult resolveDependencies(MavenPomDownloader downloader, ExecutionContext ctx) {
        Map<Scope, List<ResolvedDependency>> dependencies = new HashMap<>();
        dependencies.put(Scope.Compile, pom.resolveDependencies(Scope.Compile, downloader, ctx));
        dependencies.put(Scope.Test, pom.resolveDependencies(Scope.Test, downloader, ctx));
        dependencies.put(Scope.Runtime, pom.resolveDependencies(Scope.Runtime, downloader, ctx));
        dependencies.put(Scope.Provided, pom.resolveDependencies(Scope.Provided, downloader, ctx));
        return withDependencies(dependencies);
    }

    public Map<Path, Pom> getProjectPoms() {
        return getProjectPomsRecursive(new HashMap<>());
    }

    private Map<Path, Pom> getProjectPomsRecursive(Map<Path, Pom> projectPoms) {
        projectPoms.put(pom.getRequested().getSourcePath(), pom.getRequested());
        if (parent != null) {
            parent.getProjectPomsRecursive(projectPoms);
        }
        for (MavenResolutionResult module : modules) {
            if (!projectPoms.containsKey(module.getPom().getRequested().getSourcePath())) {
                module.getProjectPomsRecursive(projectPoms);
            }
        }
        return projectPoms;
    }
}
