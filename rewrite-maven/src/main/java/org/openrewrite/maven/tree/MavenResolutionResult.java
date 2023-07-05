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

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.internal.MavenPomDownloader;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class MavenResolutionResult implements Marker {
    @EqualsAndHashCode.Include
    @With
    UUID id;

    @Nullable
    Integer managedReference;

    @With
    ResolvedPom pom;

    /**
     * Resolution results of POMs in this repository that hold this POM as a parent.
     */
    @With
    List<MavenResolutionResult> modules;

    @Nullable
    MavenResolutionResult parent;

    @With
    Map<Scope, List<ResolvedDependency>> dependencies;

    @With
    @Nullable
    MavenSettings mavenSettings;

    @With
    List<String> activeProfiles;

    public List<String> getActiveProfiles() {
        // for backwards compatibility with ASTs that were serialized before activeProfiles was added
        return activeProfiles == null ? emptyList() : activeProfiles;
    }

    public MavenResolutionResult unsafeSetManagedReference(Integer id) {
        this.managedReference = id;
        return this;
    }

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

    /**
     * Finds dependencies (including any transitive dependencies) in the model that match the provided group and
     * artifact ids. The search can optionally be limited to a given scope.
     * <p>
     * Note: It is possible for the same dependency to be returned multiple times if it is present in multiple scopes.
     *
     * @param groupId    The groupId as a glob expression
     * @param artifactId The artifactId as a glob expression
     * @param scope      The scope to limit the search to, or null to search all scopes
     * @return A list of matching dependencies
     */
    public List<ResolvedDependency> findDependencies(String groupId, String artifactId, @Nullable Scope scope) {
        return findDependencies(d -> matchesGlob(d.getGroupId(), groupId) && matchesGlob(d.getArtifactId(), artifactId), scope);
    }

    /**
     * Finds dependencies (including any transitive dependencies) in the model that match the predicate. The search can
     * optionally be limited to a given scope.
     * <p>
     * Note: It is possible for the same dependency to be returned multiple times if it is present in multiple scopes.
     *
     * @param matcher The predicate to match the dependency
     * @param scope   A scope to limit the search to, or null to search all scopes
     * @return A list of matching dependencies
     */
    public List<ResolvedDependency> findDependencies(Predicate<ResolvedDependency> matcher, @Nullable Scope scope) {
        List<ResolvedDependency> found = null;
        for (Map.Entry<Scope, List<ResolvedDependency>> entry : dependencies.entrySet()) {
            if (scope != null && entry.getKey() != scope) {
                continue;
            }

            for (ResolvedDependency d : entry.getValue()) {
                if (matcher.test(d)) {
                    if (found == null) {
                        found = new ArrayList<>();
                    }
                    found.add(d);
                }
            }
        }
        return found == null ? emptyList() : found;
    }

    public void unsafeSet(UUID id, ResolvedPom pom, List<MavenResolutionResult> modules,
                          @Nullable MavenResolutionResult parent,
                          Map<Scope, List<ResolvedDependency>> dependencies) {
        this.id = id;
        this.pom = pom;
        this.modules = modules;
        this.parent = parent;
        this.dependencies = dependencies;
    }

    public void unsafeSetParent(MavenResolutionResult parent) {
        this.parent = parent;
    }

    public void unsafeSetModules(@Nullable List<MavenResolutionResult> modules) {
        this.modules = modules == null ? emptyList() : new ArrayList<>(modules);
    }

    @Nullable
    public ResolvedManagedDependency getResolvedManagedDependency(ManagedDependency dependency) {
        for (ResolvedManagedDependency dm : pom.getDependencyManagement()) {
            if (dm.getRequested() == dependency || dm.getRequestedBom() == dependency) {
                return dm;
            }
        }
        return null;
    }

    private static final Scope[] RESOLVE_SCOPES = new Scope[]{Scope.Compile, Scope.Runtime, Scope.Test, Scope.Provided};

    public MavenResolutionResult resolveDependencies(MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingExceptions {
        Map<Scope, List<ResolvedDependency>> dependencies = new HashMap<>();
        MavenDownloadingExceptions exceptions = null;

        Map<GroupArtifact, Set<GroupArtifactVersion>> exceptionsInLowerScopes = new HashMap<>();
        for (Scope scope : RESOLVE_SCOPES) {
            try {
                dependencies.put(scope, pom.resolveDependencies(scope, downloader, ctx));
            } catch (MavenDownloadingExceptions e) {
                for (MavenDownloadingException exception : e.getExceptions()) {
                    if (exceptionsInLowerScopes.computeIfAbsent(new GroupArtifact(exception.getRoot().getGroupId(),
                            exception.getRoot().getArtifactId()), ga -> new HashSet<>()).add(exception.getFailedOn())) {
                        exceptions = MavenDownloadingExceptions.append(exceptions, exception);
                    }
                }
            }
        }
        if (exceptions != null) {
            throw exceptions;
        }
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
