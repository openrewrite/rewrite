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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Overlays resolved-dependency information from a parsed lock file onto a
 * {@link PythonResolutionResult} marker. The pyproject and pipfile entry points
 * differ only in package-manager handling.
 */
public final class PythonResolutionLinker {

    private PythonResolutionLinker() {
    }

    /**
     * Apply pyproject-shaped resolution and set the package manager to {@code pm},
     * the resolver whose lock this overlay came from.
     */
    public static PythonResolutionResult applyPyproject(PythonResolutionResult marker,
                                                        List<ResolvedDependency> resolvedDeps,
                                                        PackageManager pm) {
        return relink(marker, resolvedDeps).withPackageManager(pm);
    }

    /**
     * Apply pipfile-shaped resolution. The package manager is left unchanged
     * ({@code createMarker} already sets it to {@link PackageManager#Pipenv}).
     */
    public static PythonResolutionResult applyPipfile(PythonResolutionResult marker,
                                                      List<ResolvedDependency> resolvedDeps) {
        return relink(marker, resolvedDeps);
    }

    /**
     * Replace the marker's resolved graph and relink every declared-dependency
     * field against it; linking an empty field is a no-op, so this covers all
     * marker shapes.
     */
    private static PythonResolutionResult relink(PythonResolutionResult marker,
                                                 List<ResolvedDependency> resolvedDeps) {
        marker = marker.withResolvedDependencies(resolvedDeps);
        marker = marker.withDependencies(link(marker.getDependencies(), resolvedDeps));
        marker = marker.withBuildRequires(link(marker.getBuildRequires(), resolvedDeps));
        marker = marker.withOptionalDependencies(linkMap(marker.getOptionalDependencies(), resolvedDeps));
        marker = marker.withDependencyGroups(linkMap(marker.getDependencyGroups(), resolvedDeps));
        marker = marker.withConstraintDependencies(link(marker.getConstraintDependencies(), resolvedDeps));
        marker = marker.withOverrideDependencies(link(marker.getOverrideDependencies(), resolvedDeps));
        return marker;
    }

    /**
     * Rebuild the resolved graph with updated versions via {@link #buildGraph} and relink
     * all declared-dependency {@code resolved} pointers. Keys of {@code versionUpdates} are
     * normalized package names. Returns the same marker when no version changes.
     */
    public static PythonResolutionResult updateResolvedVersions(PythonResolutionResult marker,
                                                                Map<String, String> versionUpdates) {
        List<ResolvedDependency> resolved = marker.getResolvedDependencies();
        boolean changed = resolved.stream().anyMatch(dep -> {
            String newVersion = versionUpdates.get(PythonResolutionResult.normalizeName(dep.getName()));
            return newVersion != null && !newVersion.equals(dep.getVersion());
        });
        if (!changed) {
            return marker;
        }
        List<UnlinkedPackage> packages = new ArrayList<>(resolved.size());
        for (ResolvedDependency dep : resolved) {
            String newVersion = versionUpdates.get(PythonResolutionResult.normalizeName(dep.getName()));
            List<String> depNames = dep.getDependencies() == null ? emptyList() :
                    dep.getDependencies().stream().map(ResolvedDependency::getName).collect(Collectors.toList());
            packages.add(new UnlinkedPackage(dep.getName(),
                    newVersion != null ? newVersion : dep.getVersion(), dep.getSource(), depNames));
        }
        return relink(marker, buildGraph(packages));
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

    /**
     * Flat per-package data extracted from a lock file or installed-package
     * metadata, before graph linking.
     */
    @Value
    public static class UnlinkedPackage {
        String name;
        @Nullable String version;
        @Nullable String source;
        List<String> dependencyNames;
    }

    /**
     * Builds the linked graph from flat package data, upholding the aliasing contract
     * documented on {@link ResolvedDependency}. Dependency names that resolve to no
     * package are dropped, leaving a null dependencies list when none resolve. Where the
     * same normalized name appears more than once, dependents link to the first occurrence.
     */
    public static List<ResolvedDependency> buildGraph(List<UnlinkedPackage> packages) {
        List<String> normalizedNames = new ArrayList<>(packages.size());
        List<List<String>> normalizedDepNames = new ArrayList<>(packages.size());
        Set<String> knownNames = new HashSet<>();
        for (UnlinkedPackage pkg : packages) {
            String normalized = PythonResolutionResult.normalizeName(pkg.getName());
            normalizedNames.add(normalized);
            knownNames.add(normalized);
            List<String> depNames = new ArrayList<>(pkg.getDependencyNames().size());
            for (String depName : pkg.getDependencyNames()) {
                depNames.add(PythonResolutionResult.normalizeName(depName));
            }
            normalizedDepNames.add(depNames);
        }

        List<ResolvedDependency> resolved = new ArrayList<>(packages.size());
        Map<String, ResolvedDependency> byName = new LinkedHashMap<>();
        for (int i = 0; i < packages.size(); i++) {
            UnlinkedPackage pkg = packages.get(i);
            boolean anyResolvable = normalizedDepNames.get(i).stream().anyMatch(knownNames::contains);
            ResolvedDependency entry = new ResolvedDependency(pkg.getName(), pkg.getVersion(),
                    pkg.getSource(), anyResolvable ? new ArrayList<>() : null);
            resolved.add(entry);
            byName.putIfAbsent(normalizedNames.get(i), entry);
        }

        for (int i = 0; i < resolved.size(); i++) {
            ResolvedDependency entry = resolved.get(i);
            if (entry.getDependencies() == null) {
                continue;
            }
            for (String depName : normalizedDepNames.get(i)) {
                ResolvedDependency child = byName.get(depName);
                if (child != null) {
                    entry.getDependencies().add(child);
                }
            }
        }
        return resolved;
    }
}
