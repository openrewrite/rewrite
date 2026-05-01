/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Overlays resolved-dependency information from a parsed lock file onto a
 * {@link PythonResolutionResult} marker. Pyproject and Pipfile have different
 * sets of declared-dependency fields, so two entry points are exposed.
 */
public final class PythonResolutionLinker {

    private PythonResolutionLinker() {
    }

    /**
     * Apply pyproject-shaped resolution: link dependencies, build-requires,
     * optional-dependencies, dependency-groups, constraint-dependencies, and
     * override-dependencies. Sets the package manager to {@link PackageManager#Uv}
     * since uv is the resolver this overlay covers.
     */
    public static PythonResolutionResult applyPyproject(PythonResolutionResult marker,
                                                        List<ResolvedDependency> resolvedDeps) {
        marker = marker.withResolvedDependencies(resolvedDeps);
        marker = marker.withPackageManager(PackageManager.Uv);
        marker = marker.withDependencies(link(marker.getDependencies(), resolvedDeps));
        marker = marker.withBuildRequires(link(marker.getBuildRequires(), resolvedDeps));
        marker = marker.withOptionalDependencies(linkMap(marker.getOptionalDependencies(), resolvedDeps));
        marker = marker.withDependencyGroups(linkMap(marker.getDependencyGroups(), resolvedDeps));
        marker = marker.withConstraintDependencies(link(marker.getConstraintDependencies(), resolvedDeps));
        marker = marker.withOverrideDependencies(link(marker.getOverrideDependencies(), resolvedDeps));
        return marker;
    }

    /**
     * Apply pipfile-shaped resolution: link {@code [packages]} and
     * {@code [dev-packages]}. The package manager is left unchanged ({@code createMarker}
     * already sets it to {@link PackageManager#Pipenv}).
     */
    public static PythonResolutionResult applyPipfile(PythonResolutionResult marker,
                                                      List<ResolvedDependency> resolvedDeps) {
        marker = marker.withResolvedDependencies(resolvedDeps);
        marker = marker.withDependencies(link(marker.getDependencies(), resolvedDeps));
        marker = marker.withOptionalDependencies(linkMap(marker.getOptionalDependencies(), resolvedDeps));
        return marker;
    }

    public static List<Dependency> link(List<Dependency> deps, List<ResolvedDependency> resolved) {
        return deps.stream().map(dep -> {
            String normalizedName = PythonResolutionResult.normalizeName(dep.getName());
            ResolvedDependency found = resolved.stream()
                    .filter(r -> PythonResolutionResult.normalizeName(r.getName()).equals(normalizedName))
                    .findFirst()
                    .orElse(null);
            return found != null ? dep.withResolved(found) : dep;
        }).collect(Collectors.toList());
    }

    public static Map<String, List<Dependency>> linkMap(Map<String, List<Dependency>> depMap,
                                                        List<ResolvedDependency> resolved) {
        Map<String, List<Dependency>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Dependency>> entry : depMap.entrySet()) {
            result.put(entry.getKey(), link(entry.getValue(), resolved));
        }
        return result;
    }
}
