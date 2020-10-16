
package org.openrewrite.maven.internal;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.*;

class ResolutionContext {
    private final Set<Pom.Repository> repositories;
    private final NavigableMap<Scope, Set<String>> nearerGroupArtifactsByScope;
    private final Map<String, String> dependencyManagement;

    public ResolutionContext() {
        this(new LinkedHashSet<>(), new TreeMap<>(), new HashMap<>());
        for (Scope scope : Scope.values()) {
            nearerGroupArtifactsByScope.put(scope, new HashSet<>());
        }
    }

    public ResolutionContext(Set<Pom.Repository> repositories, NavigableMap<Scope, Set<String>> nearerGroupArtifactsByScope, Map<String, String> dependencyManagement) {
        this.repositories = repositories;
        this.nearerGroupArtifactsByScope = nearerGroupArtifactsByScope;
        this.dependencyManagement = dependencyManagement;
    }

    public ResolutionContext withRepositories(List<Pom.Repository> repositories) {
        Set<Pom.Repository> moreRepositories = new LinkedHashSet<>(repositories);
        moreRepositories.addAll(repositories);
        return new ResolutionContext(moreRepositories, nearerGroupArtifactsByScope, dependencyManagement);
    }

    public ResolutionContext withNearerGroupArtifacts(Collection<RawPom.Dependency> dependencies) {
        NavigableMap<Scope, Set<String>> moreNearerGroupArtifacts = new TreeMap<>(nearerGroupArtifactsByScope);
        dependencies.forEach(dep -> moreNearerGroupArtifacts.headMap(Scope.fromName(dep.getScope()))
                .forEach((scope, gas) -> gas.add(dep.getGroupId() + ':' + dep.getArtifactId())));
        return new ResolutionContext(repositories, moreNearerGroupArtifacts, dependencyManagement);
    }

    public ResolutionContext withDependencyManagement(@Nullable RawPom.DependencyManagement rawDependencyManagement) {
        if (rawDependencyManagement == null) {
            return this;
        }
        Map<String, String> moreDependencyManagement = new HashMap<>(dependencyManagement);
        rawDependencyManagement.getDependencies().forEach(dep -> moreDependencyManagement
                .put(dep.getGroupId() + ':' + dep.getArtifactId(), dep.getVersion()));
        return new ResolutionContext(repositories, nearerGroupArtifactsByScope, moreDependencyManagement);
    }

    public boolean evict(RawPom.Dependency dependency) {
        return nearerGroupArtifactsByScope.get(Scope.fromName(dependency.getScope()))
                .contains(dependency.getGroupId() + ':' + dependency.getArtifactId());
    }
}
