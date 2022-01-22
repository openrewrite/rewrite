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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.internal.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;

import java.util.*;

import static java.util.Collections.*;

@Getter
public class ResolvedPom implements DependencyManagementDependency {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", null);

    // https://maven.apache.org/ref/3.6.3/maven-model-builder/super-pom.html
    private static final ResolvedPom SUPER_POM = new ResolvedPom(
            new Pom(null, null, null, null, null, null, emptyMap(), emptyList(), emptyList(), singletonList(MavenRepository.MAVEN_CENTRAL), emptyList(), emptyList()),
            emptyList()
    );

    @With
    Pom requested;

    @With
    Iterable<String> activeProfiles;

    public ResolvedPom(Pom requested, Iterable<String> activeProfiles) {
        this(requested, activeProfiles, emptyMap(), emptyList(), emptyList(), emptyList());
    }

    @JsonCreator
    ResolvedPom(Pom requested, Iterable<String> activeProfiles, Map<String, String> properties, List<DependencyManagementDependency> dependencyManagement, List<MavenRepository> repositories, List<Dependency> requestedDependencies) {
        this.requested = requested;
        this.activeProfiles = activeProfiles;
        this.properties = properties;
        this.dependencyManagement = dependencyManagement;
        this.repositories = repositories;
        this.requestedDependencies = requestedDependencies;
    }

    @NonFinal
    Map<String, String> properties;

    @NonFinal
    List<DependencyManagementDependency> dependencyManagement;

    @NonFinal
    List<MavenRepository> repositories;

    @NonFinal
    List<Dependency> requestedDependencies;

    /**
     * Whenever a change is made that may affect the effective properties, dependency management,
     * dependencies, etc. of a POM, this can be called to re-resolve the POM.
     *
     * @param ctx        An execution context containing any maven-specific requirements.
     * @param downloader A POM downloader to download dependencies and parents.
     * @return A new instance with dependencies re-resolved or the same instance if no resolved dependencies have changed.
     * @throws MavenDownloadingException When problems are encountered downloading dependencies or parents.
     */
    public ResolvedPom resolve(ExecutionContext ctx, MavenPomDownloader downloader) throws MavenDownloadingException {
        ResolvedPom resolved = new ResolvedPom(requested, activeProfiles).resolver(ctx, downloader).resolve();

        for (Map.Entry<String, String> property : resolved.getProperties().entrySet()) {
            if (!property.getValue().equals(properties.get(property.getKey()))) {
                return resolved;
            }
        }

        List<Dependency> resolvedRequestedDependencies = resolved.getRequestedDependencies();
        if (requestedDependencies.size() != resolvedRequestedDependencies.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedRequestedDependencies.size(); i++) {
            if (!requestedDependencies.get(i).equals(resolvedRequestedDependencies.get(i))) {
                return resolved;
            }
        }

        List<DependencyManagementDependency> resolvedDependencyManagement = resolved.getDependencyManagement();
        if (dependencyManagement.size() != resolvedDependencyManagement.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedDependencyManagement.size(); i++) {
            // TODO does ResolvedPom's equals work well enough to match on BOM imports?
            if (!dependencyManagement.get(i).equals(resolvedDependencyManagement.get(i))) {
                return resolved;
            }
        }

        List<MavenRepository> resolvedRepositories = resolved.getRepositories();
        if (repositories.size() != resolvedRepositories.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedRepositories.size(); i++) {
            if (!repositories.get(i).equals(resolvedRepositories.get(i))) {
                return resolved;
            }
        }

        return this;
    }

    Resolver resolver(ExecutionContext ctx, MavenPomDownloader downloader) {
        return new Resolver(ctx, downloader);
    }

    public ResolvedGroupArtifactVersion getGav() {
        return requested.getGav();
    }

    public String getGroupId() {
        return requested.getGroupId();
    }

    public String getArtifactId() {
        return requested.getArtifactId();
    }

    public String getVersion() {
        return requested.getVersion();
    }

    @Nullable
    public String getDatedSnapshotVersion() {
        return requested.getDatedSnapshotVersion();
    }

    @Override
    public <D extends DependencyManagementDependency> D withVersion(String version) {
        throw new UnsupportedOperationException("Call withVersion on the requested DependencyManagement.Import instead.");
    }

    @Nullable
    public String getPackaging() {
        return requested.getPackaging();
    }

    @Nullable
    public String getValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return placeholderHelper.replacePlaceholders(value, this::getProperty);
    }

    @Nullable
    private String getProperty(@Nullable String property) {
        if (property == null) {
            return null;
        }
        switch (property) {
            case "groupId":
            case "project.groupId":
            case "pom.groupId":
                return requested.getGroupId();
            case "project.parent.groupId":
                return requested.getParent() != null ? requested.getParent().getGroupId() : null;
            case "artifactId":
            case "project.artifactId":
            case "pom.artifactId":
                return requested.getArtifactId(); // cannot be inherited from parent
            case "project.parent.artifactId":
                return requested.getParent() == null ? null : requested.getParent().getArtifactId();
            case "version":
            case "project.version":
            case "pom.version":
                return requested.getVersion();
            case "project.parent.version":
                return requested.getParent() != null ? requested.getParent().getVersion() : null;
        }

        return System.getProperty(property, properties.get(property));
    }

    @Nullable
    public String getManagedVersion(String groupId, String artifactId, @Nullable String type, @Nullable String classifier) {
        for (DependencyManagementDependency dep : dependencyManagement) {
            if (dep instanceof ResolvedPom) {
                String version = ((ResolvedPom) dep).getManagedVersion(groupId, artifactId, type, classifier);
                if (version != null) {
                    return version;
                }
            } else if (dep instanceof DependencyManagementDependency.Defined) {
                DependencyManagementDependency.Defined dm = (DependencyManagementDependency.Defined) dep;
                if (dm.matches(groupId, artifactId, type, classifier)) {
                    return getValue(dm.getVersion());
                }
            }
        }

        return null;
    }

    @Nullable
    public String getManagedScope(String groupId, String artifactId, @Nullable String type, @Nullable String classifier) {
        for (DependencyManagementDependency dep : dependencyManagement) {
            if (dep instanceof ResolvedPom) {
                String scope = ((ResolvedPom) dep).getManagedScope(groupId, artifactId, type, classifier);
                if (scope != null) {
                    return scope;
                }
            } else if (dep instanceof DependencyManagementDependency.Defined) {
                DependencyManagementDependency.Defined dm = (DependencyManagementDependency.Defined) dep;
                if (dm.matches(groupId, artifactId, type, classifier)) {
                    return getValue(dm.getScope());
                }
            }
        }

        return null;
    }

    public GroupArtifactVersion getValues(GroupArtifactVersion gav) {
        return gav.withGroupId(getValue(gav.getGroupId()))
                .withArtifactId(getValue(gav.getArtifactId()))
                .withVersion(getValue(gav.getVersion()));
    }

    @Value
    class Resolver {
        ExecutionContext ctx;
        MavenPomDownloader downloader;

        public ResolvedPom resolve() throws MavenDownloadingException {
            List<Pom> pomAncestry = new ArrayList<>();
            pomAncestry.add(requested);
            resolveParentsRecursively(pomAncestry);
            return ResolvedPom.this;
        }

        void resolveParentsRecursively(List<Pom> pomAncestry) {
            Pom pom = pomAncestry.get(0);
            mergeRepositories(pom.getRepositories());

            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergeProperties(profile.getProperties());
                }
            }
            mergeProperties(pom.getProperties());

            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergeDependencyManagement(profile.getDependencyManagement(), pom);
                    mergeRequestedDependencies(profile.getDependencies());
                }
            }
            mergeDependencyManagement(pom.getDependencyManagement(), pom);
            mergeRequestedDependencies(pom.getDependencies());

            if (pom.getParent() != null) {
                Pom parentPom = downloader.download(getValues(pom.getParent().getGav()),
                        pom.getParent().getRelativePath(), ResolvedPom.this, repositories);

                for (Pom ancestor : pomAncestry) {
                    if (ancestor.getGav().equals(parentPom.getGav())) {
                        // parent cycle
                        return;
                    }
                }

                MavenExecutionContextView.view(ctx)
                        .getResolutionListener()
                        .parent(parentPom, pom);

                pomAncestry.add(0, parentPom);
                resolveParentsRecursively(pomAncestry);
            }
        }

        private void mergeRequestedDependencies(List<Dependency> incomingRequestedDependencies) {
            if (!incomingRequestedDependencies.isEmpty()) {
                if (requestedDependencies == null || requestedDependencies.isEmpty()) {
                    requestedDependencies = new ArrayList<>(incomingRequestedDependencies);
                }
                requestedDependencies.addAll(incomingRequestedDependencies);
            }
        }

        private void mergeRepositories(List<MavenRepository> incomingRepositories) {
            if (!incomingRepositories.isEmpty()) {
                if (repositories == null || repositories.isEmpty()) {
                    repositories = new ArrayList<>(incomingRepositories);
                }

                nextRepository:
                for (MavenRepository incomingRepository : incomingRepositories) {
                    if (incomingRepository.getId() != null) {
                        for (MavenRepository repository : repositories) {
                            if (incomingRepository.getId().equals(repository.getId())) {
                                continue nextRepository;
                            }
                        }
                    }
                    repositories.add(incomingRepository);
                }
            }
        }

        private void mergeProperties(Map<String, String> incomingProperties) {
            if (!incomingProperties.isEmpty()) {
                if (properties == null || properties.isEmpty()) {
                    properties = new HashMap<>(incomingProperties);
                }
                for (Map.Entry<String, String> property : incomingProperties.entrySet()) {
                    if (!properties.containsKey(property.getKey())) {
                        properties.put(property.getKey(), property.getValue());
                    }
                }
            }
        }

        private void mergeDependencyManagement(List<DependencyManagementDependency> incomingDependencyManagement, Pom pom) {
            if (!incomingDependencyManagement.isEmpty()) {
                if (dependencyManagement == null || dependencyManagement.isEmpty()) {
                    dependencyManagement = new ArrayList<>(incomingDependencyManagement);
                }
                for (DependencyManagementDependency d : incomingDependencyManagement) {
                    if (d instanceof Imported) {
                        ResolvedPom bom = downloader.download(getValues(((Imported) d).getGav()), null, null, emptyList())
                                .resolve(emptyList(), downloader, ctx);
                        MavenExecutionContextView.view(ctx)
                                .getResolutionListener()
                                .bomImport(bom.getGav(), pom);
                        dependencyManagement.addAll(bom.getDependencyManagement());
                    } else if (d instanceof Defined) {
                        Defined defined = (Defined) d;
                        defined = defined.withGav(getValues(defined.getGav()));
                        MavenExecutionContextView.view(ctx)
                                .getResolutionListener()
                                .dependencyManagement(defined, pom);
                        dependencyManagement.add(defined);
                    }
                }
            }
        }
    }

    public List<ResolvedDependency> resolveDependencies(Scope scope, MavenPomDownloader downloader, ExecutionContext ctx) {
        Set<GroupArtifact> nearer = new HashSet<>();
        List<ResolvedDependency> dependencies = new ArrayList<>();

        List<DependencyAndDependent> dependenciesAtDepth = new ArrayList<>();
        for (Dependency requestedDependency : getRequestedDependencies()) {
            Scope dScope = Scope.fromName(getValue(requestedDependency.getScope()));
            if (dScope == scope || dScope.isInClasspathOf(scope)) {
                dependenciesAtDepth.add(new DependencyAndDependent(requestedDependency, null, this));
            }
        }

        int depth = 0;
        while (!dependenciesAtDepth.isEmpty()) {
            List<DependencyAndDependent> dependenciesAtNextDepth = new ArrayList<>();

            for (DependencyAndDependent dd : dependenciesAtDepth) {
                Dependency d = dd.getDependency();
                d = dd.getDefinedIn().getValues(d, depth);

                if (!nearer.add(new GroupArtifact(d.getGroupId(), d.getArtifactId()))) {
                    // there was a nearer dependency with the same group and artifact that
                    // has higher precedence in conflict resolution
                    continue;
                }

                if (d.getType() != null && !"jar".equals(d.getType())) {
                    continue;
                }

                try {
                    Pom dPom = downloader.download(d.getGav(), null, dd.definedIn, getRepositories());
                    Scope dScope = Scope.fromName(d.getScope());

                    ResolvedPom resolvedPom = new ResolvedPom(dPom, getActiveProfiles(), getProperties(),
                            getDependencyManagement(), getRepositories(), emptyList());
                    List<Pom> dPomAncestry = new ArrayList<>();
                    dPomAncestry.add(dPom);
                    resolvedPom.resolver(ctx, downloader).resolveParentsRecursively(dPomAncestry);

                    ResolvedDependency resolved = new ResolvedDependency(dPom.getRepository(),
                            resolvedPom.getGav(), d, emptyList(),
                            resolvedPom.getRequested().getLicenses());

                    MavenExecutionContextView.view(ctx)
                            .getResolutionListener()
                            .dependency(scope, resolved, dd.getDefinedIn());

                    // build link between the including dependency and this one
                    ResolvedDependency includedBy = dd.getDependent();
                    if (includedBy != null) {
                        if (includedBy.getDependencies().isEmpty()) {
                            includedBy.unsafeSetDependencies(new ArrayList<>());
                        }
                        includedBy.getDependencies().add(resolved);
                    }

                    dependencies.add(resolved);

                    for (Dependency d2 : resolvedPom.getRequestedDependencies()) {
                        if (!d2.isOptional()) { // only zero depth optional dependencies resolved
                            if (Scope.fromName(d2.getScope()).isInClasspathOf(dScope)) {
                                dependenciesAtNextDepth.add(new DependencyAndDependent(d2, resolved, resolvedPom));
                            }
                        }
                    }
                } catch (MavenDownloadingException e) {
                    ctx.getOnError().accept(e);
                }
            }

            dependenciesAtDepth = dependenciesAtNextDepth;
            depth++;
        }

        return dependencies;
    }

    private Dependency getValues(Dependency dep, int depth) {
        Dependency d = dep.withGav(getValues(dep.getGav()))
                .withScope(getValue(dep.getScope()));
        assert d.getGroupId() != null;

        String version = d.getVersion();
        if (d.getVersion() == null || depth > 0) {
            // dependency management overrides transitive dependency versions
            version = getManagedVersion(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier());
            if (version == null) {
                version = d.getVersion();
            }
        }

        String scope = d.getScope() == null ?
                getManagedScope(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier()) :
                getValue(d.getScope());

        return d
                .withGav(d.getGav().withVersion(version))
                .withScope(scope);
    }

    @Value
    private static class DependencyAndDependent {
        Dependency dependency;
        ResolvedDependency dependent;
        ResolvedPom definedIn;
    }
}
