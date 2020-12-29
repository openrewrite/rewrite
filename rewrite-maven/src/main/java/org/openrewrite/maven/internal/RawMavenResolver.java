/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

public class RawMavenResolver {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", null);
    private static final Logger logger = LoggerFactory.getLogger(RawMavenResolver.class);

    // This is used to keep track of what versions have been seen further up the tree so we don't unnecessarily
    // resolve subtrees that have no chance of being selected by conflict resolution.
    private final NavigableMap<Scope, Map<GroupArtifact, RequestedVersion>> versionSelection;

    // The breadth-first queue of resolution tasks.
    private final Queue<ResolutionTask> workQueue = new LinkedList<>();

    private final Map<PartialTreeKey, Optional<Pom>> resolved = new HashMap<>();
    private final Map<ResolutionTask, PartialMaven> partialResults = new HashMap<>();

    private final MavenDownloader downloader;

    /**
     * Is this resolver being used for resolving parents or import BOMs? Just a flag for logging purposes alone...
     */
    private final boolean forParent;

    private final Collection<String> activeProfiles;
    private final boolean resolveOptional;
    @Nullable private final MavenSettings mavenSettings;

    public RawMavenResolver(MavenDownloader downloader, boolean forParent, Collection<String> activeProfiles,
                            @Nullable MavenSettings mavenSettings, boolean resolveOptional) {
        this.versionSelection = new TreeMap<>();
        for (Scope scope : Scope.values()) {
            versionSelection.putIfAbsent(scope, new HashMap<>());
        }
        this.downloader = downloader;
        this.forParent = forParent;
        this.activeProfiles = activeProfiles;
        this.mavenSettings = mavenSettings;
        this.resolveOptional = resolveOptional;
    }

    @Nullable
    public Xml.Document resolve(RawMaven rawMaven) {
        Pom pom = resolve(rawMaven, Scope.None, rawMaven.getPom().getVersion(),
                mavenSettings == null ? emptyList() : mavenSettings.getActiveRepositories(activeProfiles));
        assert pom != null;
        return rawMaven.getDocument().withMetadata(singletonList(pom));
    }

    /**
     * Resolution is performed breadth-first because the default conflict resolution algorithm
     * for Maven prefers nearer versions. By proceeding breadth-first we can avoid even attempting
     * to resolve subtrees that have no chance of being selected by conflict resolution in the end.
     *
     * @param rawMaven     The shell of the POM to resolve.
     * @param repositories The set of repositories to resolve with.
     * @return A transitively resolved POM model.
     */
    @Nullable
    public Pom resolve(RawMaven rawMaven, Scope scope, @Nullable String requestedVersion, List<RawRepositories.Repository> repositories) {
        ResolutionTask rootTask = new ResolutionTask(scope, rawMaven, emptySet(),
                false, null, requestedVersion, repositories);

        workQueue.add(rootTask);

        while (!workQueue.isEmpty()) {
            processTask(workQueue.poll());
        }

        return assembleResults(rootTask, new Stack<>());
    }

    private void processTask(ResolutionTask task) {
        RawMaven rawMaven = task.getRawMaven();

        if (partialResults.containsKey(task)) {
            return; // already processed
        }

        PartialMaven partialMaven = new PartialMaven(rawMaven.getDocument().getSourcePath(), rawMaven.getPom());
        processProperties(task, partialMaven);
        processRepositories(task, partialMaven);
        processParent(task, partialMaven);
        processDependencyManagement(task, partialMaven);
        processLicenses(task, partialMaven);
        processDependencies(task, partialMaven);

        partialResults.put(task, partialMaven);
    }

    private void processProperties(ResolutionTask task, PartialMaven partialMaven) {
        partialMaven.setProperties(task.getRawMaven().getActiveProperties(activeProfiles));
    }

    private void processDependencyManagement(ResolutionTask task, PartialMaven partialMaven) {
        RawPom pom = task.getRawMaven().getPom();
        List<DependencyManagementDependency> managedDependencies = new ArrayList<>();

        RawPom.DependencyManagement dependencyManagement = pom.getDependencyManagement();
        if (dependencyManagement != null && dependencyManagement.getDependencies() != null) {
            for (RawPom.Dependency d : dependencyManagement.getDependencies().getDependencies()) {
                assert d.getVersion() != null;

                String groupId = partialMaven.getGroupId(d.getGroupId());
                String artifactId = partialMaven.getArtifactId(d.getArtifactId());
                String version = partialMaven.getVersion(d.getVersion());

                // for debugging...
                if (groupId == null || artifactId == null || version == null) {
                    assert groupId != null;
                    assert artifactId != null;
                    //noinspection ConstantConditions
                    assert version != null;
                }

                // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#importing-dependencies
                if (Objects.equals(d.getType(), "pom") && Objects.equals(d.getScope(), "import")) {
                    RawMaven rawMaven = downloader.download(groupId, artifactId, version, null, null, null,
                            partialMaven.getRepositories());
                    if (rawMaven != null) {
                        Pom maven = new RawMavenResolver(downloader, true, activeProfiles, mavenSettings, resolveOptional)
                                .resolve(rawMaven, Scope.Compile, d.getVersion(), partialMaven.getRepositories());

                        if (maven != null) {
                            managedDependencies.add(new DependencyManagementDependency.Imported(groupId, artifactId,
                                    version, d.getVersion(), maven));
                        }
                    }
                } else {
                    managedDependencies.add(new DependencyManagementDependency.Defined(
                            groupId, artifactId, version, d.getVersion(),
                            d.getScope() == null ? null : Scope.fromName(d.getScope()),
                            d.getClassifier(), d.getExclusions()));
                }
            }
        }

        partialMaven.setDependencyManagement(new Pom.DependencyManagement(managedDependencies));
    }

    private void processDependencies(ResolutionTask task, PartialMaven partialMaven) {
        RawMaven rawMaven = task.getRawMaven();

        // Parent dependencies wind up being part of the subtree rooted at "task", so affect conflict resolution further down tree.
        if (partialMaven.getParent() != null) {
            for (Pom.Dependency dependency : partialMaven.getParent().getDependencies()) {
                RequestedVersion requestedVersion = selectVersion(dependency.getScope(), dependency.getGroupId(),
                        dependency.getArtifactId(), dependency.getVersion());
                versionSelection.get(dependency.getScope()).put(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()), requestedVersion);
            }
        }

        partialMaven.setDependencyTasks(rawMaven.getActiveDependencies(activeProfiles).stream()
                .filter(dep -> {
                    // we don't care about test-jar, etc.
                    return dep.getType() == null || dep.getType().equals("jar");
                })
                .filter(dep -> resolveOptional || dep.getOptional() == null || !dep.getOptional())
                .map(dep -> {
                    // replace property references, source versions from dependency management sections, etc.
                    String groupId = partialMaven.getGroupId(dep.getGroupId());
                    String artifactId = partialMaven.getArtifactId(dep.getArtifactId());

                    // for debugging...
                    if (groupId == null || artifactId == null) {
                        assert groupId != null;
                        //noinspection ConstantConditions
                        assert artifactId != null;
                    }

                    // excluded
                    for (GroupArtifact e : task.getExclusions()) {
                        if (dep.getGroupId().matches(e.getGroupId()) &&
                                dep.getArtifactId().matches(e.getArtifactId())) {
                            return null;
                        }
                    }

                    String version = null;
                    String last;

                    // loop so that when dependencyManagement refers to a property that we take another pass to resolve the property.
                    int i = 0;
                    do {
                        last = version;
                        String result = null;
                        if (last != null) {
                            String partialMavenVersion = partialMaven.getVersion(last);
                            if (partialMavenVersion != null) {
                                result = partialMavenVersion;
                            }
                        }
                        if (result == null) {
                            OUTER:
                            for (DependencyManagementDependency managed : partialMaven.getDependencyManagement().getDependencies()) {
                                for (DependencyDescriptor dependencyDescriptor : managed.getDependencies()) {
                                    if (groupId.equals(partialMaven.getGroupId(dependencyDescriptor.getGroupId())) &&
                                            artifactId.equals(partialMaven.getArtifactId(dependencyDescriptor.getArtifactId()))) {
                                        result = dependencyDescriptor.getVersion();
                                        break OUTER;
                                    }
                                }
                            }

                            if (result == null && partialMaven.getParent() != null) {
                                result = partialMaven.getParent().getManagedVersion(groupId, artifactId);
                            }
                        }
                        version = result;
                    } while (i++ < 2 || !Objects.equals(version, last));

                    // dependencyManagement takes precedence over the version specified on the dependency
                    if (version == null) {
                        String depVersion = dep.getVersion();
                        if (depVersion != null) {
                            version = partialMaven.getVersion(depVersion);
                        }
                    }

                    // for debugging...
                    if (version == null) {
                        logger.error("Failed to determine version for {}:{}. Initial value was {}. Including POM is at {}",
                                groupId, artifactId, dep.getVersion(),
                                rawMaven.getSourcePath());

                        //noinspection ConstantConditions
                        assert version != null;
                    }

                    Scope requestedScope = Scope.fromName(partialMaven.getScope(dep.getScope()));
                    Scope effectiveScope = requestedScope.transitiveOf(task.getScope());

                    if (effectiveScope == null) {
                        return null;
                    }

                    RequestedVersion requestedVersion = selectVersion(effectiveScope, groupId, artifactId, version);
                    versionSelection.get(effectiveScope).put(new GroupArtifact(groupId, artifactId), requestedVersion);

                    version = requestedVersion.resolve(downloader, partialMaven.getRepositories());

                    if (version.contains("${")) {
                        logger.debug("Unable to download {}:{}:{}. Including POM is at {}",
                                groupId, artifactId, version, rawMaven.getSourcePath());
                        return null;
                    }

                    RawMaven download = downloader.download(groupId, artifactId,
                            version, dep.getClassifier(), null, rawMaven,
                            partialMaven.getRepositories());

                    if (download == null) {
                        logger.debug("Unable to download {}:{}:{}. Including POM is at {}",
                                groupId, artifactId, version, rawMaven.getSourcePath());
                        return null;
                    }

                    ResolutionTask resolutionTask = new ResolutionTask(
                            requestedScope,
                            download,
                            dep.getExclusions() == null ?
                                    emptySet() :
                                    dep.getExclusions().stream()
                                            .map(ex -> new GroupArtifact(
                                                    ex.getGroupId().replace("*", ".*"),
                                                    ex.getArtifactId().replace("*", ".*")
                                            ))
                                            .collect(Collectors.toSet()),
                            dep.getOptional() != null && dep.getOptional(),
                            dep.getClassifier(),
                            dep.getVersion(),
                            partialMaven.getRepositories()
                    );

                    if (!partialResults.containsKey(resolutionTask)) {
                        // otherwise we've already resolved this subtree previously!
                        workQueue.add(resolutionTask);
                    }

                    return resolutionTask;
                })
                .filter(Objects::nonNull)
                .collect(toList()));
    }

    private void processParent(ResolutionTask task, PartialMaven partialMaven) {
        RawMaven rawMaven = task.getRawMaven();
        RawPom pom = rawMaven.getPom();
        Pom parent = null;
        if (pom.getParent() != null) {
            // TODO would it help to limit lookups of parents here by pre-caching RawMaven by RawPom.Parent?
            RawPom.Parent rawParent = pom.getParent();
            RawMaven rawParentModel = downloader.download(rawParent.getGroupId(), rawParent.getArtifactId(),
                    rawParent.getVersion(), null, rawParent.getRelativePath(), rawMaven,
                    partialMaven.getRepositories());
            if (rawParentModel != null) {
                PartialTreeKey parentKey = new PartialTreeKey(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion());
                Optional<Pom> maybeParent = resolved.get(parentKey);

                //noinspection OptionalAssignedToNull
                if (maybeParent == null) {
                    parent = new RawMavenResolver(downloader, true, activeProfiles, mavenSettings, resolveOptional)
                            .resolve(rawParentModel, Scope.Compile, rawParent.getVersion(), partialMaven.getRepositories());
                    resolved.put(parentKey, Optional.ofNullable(parent));
                } else {
                    parent = maybeParent.orElse(null);
                }
            }
        }

        partialMaven.setParent(parent);
    }

    private void processRepositories(ResolutionTask task, PartialMaven partialMaven) {
        List<RawRepositories.Repository> repositories = new ArrayList<>();
        List<RawRepositories.Repository> repositoriesFromPom = task.getRawMaven().getPom().getActiveRepositories(activeProfiles);
        if(mavenSettings != null) {
            repositoriesFromPom = mavenSettings.applyMirrors(repositoriesFromPom);
        }

        for (RawRepositories.Repository repository : repositoriesFromPom) {
            String url = repository.getUrl().trim();
            if (repository.getUrl().contains("${")) {
                url = placeholderHelper.replacePlaceholders(url, k -> partialMaven.getProperties().get(k));
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                URI.create(url);
                repositories.add(new RawRepositories.Repository(repository.getId(), url, repository.getReleases(), repository.getSnapshots()));
            } catch (Throwable t) {
                logger.debug("Unable to make a URI out of repositoriy url {}", url);
            }
        }

        repositories.addAll(task.getRepositories());
        partialMaven.setRepositories(repositories);
    }

    private void processLicenses(ResolutionTask task, PartialMaven partialMaven) {
        List<RawPom.License> licenses = task.getRawMaven().getPom().getLicenses();
        List<Pom.License> list = new ArrayList<>();
        for (RawPom.License license : licenses) {
            Pom.License fromName = Pom.License.fromName(license.getName());
            list.add(fromName);
        }
        partialMaven.setLicenses(list);
    }

    @Nullable
    private Pom assembleResults(ResolutionTask task, Stack<ResolutionTask> assemblyStack) {
        if (assemblyStack.contains(task)) {
            return null; // cut cycles
        }

        RawMaven rawMaven = task.getRawMaven();
        RawPom rawPom = rawMaven.getPom();
        PartialTreeKey taskKey = new PartialTreeKey(rawPom.getGroupId(), rawPom.getArtifactId(), rawPom.getVersion());

        Optional<Pom> result = resolved.get(taskKey);

        Stack<ResolutionTask> nextAssemblyStack = new Stack<>();
        nextAssemblyStack.addAll(assemblyStack);
        nextAssemblyStack.push(task);

        //noinspection OptionalAssignedToNull
        if (result == null) {
            PartialMaven partial = partialResults.get(task);
            if (partial != null) {
                List<Pom.Dependency> dependencies = partial.getDependencyTasks().stream()
                        .map(depTask -> {
                            boolean optional = depTask.isOptional() ||
                                    assemblyStack.stream().anyMatch(ResolutionTask::isOptional);

                            if (logger.isDebugEnabled() && !forParent) {
                                String indent = CharBuffer.allocate(assemblyStack.size()).toString().replace('\0', ' ');
                                RawPom depPom = depTask.getRawMaven().getPom();
                                logger.debug(
                                        "{}{}:{}:{}{} {}",
                                        indent,
                                        depPom.getGroupId(),
                                        depPom.getArtifactId(),
                                        depPom.getVersion(),
                                        optional ? " (optional) " : "",
                                        depTask.getRawMaven().getSourcePath()
                                );
                            }

                            Pom resolved = assembleResults(depTask, nextAssemblyStack);
                            if (resolved == null) {
                                return null;
                            }

                            return new Pom.Dependency(
                                    depTask.getScope(),
                                    depTask.getClassifier(),
                                    optional,
                                    resolved,
                                    depTask.getRequestedVersion(),
                                    depTask.getExclusions()
                            );
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(ArrayList::new));

                for (Pom ancestor = partial.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
                    for (Pom.Dependency ancestorDep : ancestor.getDependencies()) {
                        // the ancestor's dependency might be overridden by another version by conflict resolution
                        Scope scope = ancestorDep.getScope();
                        String groupId = ancestorDep.getGroupId();
                        String artifactId = ancestorDep.getArtifactId();

                        String conflictResolvedVersion = selectVersion(scope, groupId, artifactId, ancestorDep.getVersion())
                                .resolve(downloader, task.getRepositories());

                        if (!conflictResolvedVersion.equals(ancestorDep.getVersion())) {
                            RawMaven conflictResolvedRaw = downloader.download(groupId, artifactId, conflictResolvedVersion,
                                    ancestorDep.getClassifier(), null, null, task.getRepositories());

                            Pom conflictResolved = assembleResults(new ResolutionTask(scope, conflictResolvedRaw,
                                    ancestorDep.getExclusions(), ancestorDep.isOptional(), ancestorDep.getRequestedVersion(),
                                    ancestorDep.getClassifier(), task.getRepositories()), nextAssemblyStack);

                            if (conflictResolved == null) {
                                logger.debug(
                                        "Unable to conflict resolve {}:{}:{} {}",
                                        ancestorDep.getGroupId(),
                                        ancestorDep.getArtifactId(),
                                        ancestorDep.getVersion(),
                                        conflictResolvedRaw == null ? "unknown URI" : conflictResolvedRaw.getSourcePath()
                                );
                                dependencies.add(ancestorDep);
                            } else {
                                dependencies.add(new Pom.Dependency(
                                        scope,
                                        ancestorDep.getClassifier(),
                                        ancestorDep.isOptional(),
                                        conflictResolved,
                                        ancestorDep.getRequestedVersion(),
                                        ancestorDep.getExclusions()
                                ));
                            }
                        } else {
                            dependencies.add(ancestorDep);
                        }
                    }
                }

                String groupId = rawPom.getGroupId();
                if (groupId == null) {
                    groupId = partial.getParent().getGroupId();
                }

                String version = rawPom.getVersion();
                if (version == null) {
                    version = partial.getParent().getVersion();
                }

                List<Pom.Repository> repositories = new ArrayList<>();
                for (RawRepositories.Repository repo : partial.getRepositories()) {
                    try {
                        repositories.add(new Pom.Repository(URI.create(repo.getUrl()).toURL(),
                                repo.getReleases() == null || repo.getReleases().isEnabled(),
                                repo.getSnapshots() == null || repo.getSnapshots().isEnabled()));
                    } catch (MalformedURLException e) {
                        logger.debug("Malformed repository URL '{}'", repo.getUrl());
                    }
                }

                result = Optional.of(
                        new Pom(
                                partial.getSourcePath(),
                                groupId,
                                rawPom.getArtifactId(),
                                version,
                                rawPom.getSnapshotVersion(),
                                null,
                                null,
                                partial.getParent(),
                                dependencies,
                                partial.getDependencyManagement(),
                                partial.getLicenses(),
                                repositories,
                                partial.getProperties()
                        )
                );
            } else {
                result = Optional.empty();
            }

            resolved.put(taskKey, result);
        }

        return result.orElse(null);
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Data
    private static class ResolutionTask {
        @EqualsAndHashCode.Include
        Scope scope;

        @EqualsAndHashCode.Include
        RawMaven rawMaven;

        @EqualsAndHashCode.Include
        @Nullable
        Set<GroupArtifact> exclusions;

        @EqualsAndHashCode.Include
        boolean optional;

        @EqualsAndHashCode.Include
        @Nullable
        String classifier;

        @EqualsAndHashCode.Include
        @Nullable
        String requestedVersion;

        List<RawRepositories.Repository> repositories;

        @JsonIgnore
        public Set<GroupArtifact> getExclusions() {
            return exclusions == null ? emptySet() : exclusions;
        }
    }

    // FIXME may be able to eliminate this and go straight to ResolutionTask as the key
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    static class PartialTreeKey {
        String groupId;
        String artifactId;
        String version;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @Getter
    @Setter
    class PartialMaven {
        @EqualsAndHashCode.Include
        final Path sourcePath;

        @EqualsAndHashCode.Include
        final RawPom rawPom;

        Pom parent;
        Pom.DependencyManagement dependencyManagement;
        Collection<ResolutionTask> dependencyTasks = emptyList();
        Collection<Pom.License> licenses = emptyList();
        List<RawRepositories.Repository> repositories = emptyList();
        Map<String, String> properties = emptyMap();

        @Nullable
        String getGroupId(String g) {
            if (g.equals("${project.groupId}") || g.equals("${pom.groupId}")) {
                String groupId = rawPom.getGroupId();
                if (groupId != null) {
                    return groupId;
                }
                return parent == null ? null : parent.getGroupId();
            } else if (g.equals("${project.parent.groupId}")) {
                return parent != null ? parent.getGroupId() : null;
            }
            return getValue(g);
        }

        @Nullable
        String getArtifactId(String a) {
            if (a.equals("${project.artifactId}") || a.equals("${pom.artifactId}")) {
                return rawPom.getArtifactId(); // cannot be inherited from parent
            } else if (a.equals("${project.parent.artifactId}")) {
                return parent != null ? parent.getArtifactId() : null;
            }
            return getValue(a);
        }

        @Nullable
        String getVersion(@Nullable String v) {
            String last = null;
            String version;
            for (version = v; version != null && !version.equals(last);) {
                last = version;
                if (version.equals("${project.version}") || version.equals("${pom.version}")) {
                    String rawVersion = rawPom.getVersion();
                    if (rawVersion != null) {
                        version = rawVersion;
                        continue;
                    }
                    version = parent == null ? null : parent.getVersion();
                } else if (v.equals("${project.parent.version}")) {
                    version = parent != null ? parent.getVersion() : null;
                } else {
                    version = getValue(version);
                }
            }
            return version;
        }

        @Nullable
        String getScope(@Nullable String s) {
            return s == null ? null : getValue(s);
        }

        private String getValue(String v) {
            if (v.startsWith("${") && v.endsWith("}")) {
                String key = v.replace("${", "").replace("}", "");

                String value = rawPom.getActiveProperties(activeProfiles).get(key);
                if (value != null) {
                    return value;
                }

                // will be null when processing dependencyManagement itself...
                if (dependencyManagement != null) {
                    for (DependencyManagementDependency managedDependency : dependencyManagement.getDependencies()) {
                        value = managedDependency.getProperties().get(key);
                        if (value != null) {
                            return value;
                        }
                    }
                }

                for (Pom ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
                    value = ancestor.getProperty(key);
                    if (value != null) {
                        return value;
                    }
                }

                value = System.getProperty(key);
                if (value != null) {
                    return value;
                }

                return v;
            }
            return v;
        }
    }

    /**
     * Perform version conflict resolution on a dependency
     *
     * @param scope      The dependency's scope.
     * @param groupId    The dependency's group.
     * @param artifactId The dependency's artifact id.
     * @param version    The dependency's recommended version, if any.
     * @return The version selected by conflict resolution.
     */
    private RequestedVersion selectVersion(@Nullable Scope scope, String groupId, String artifactId, String version) {
        GroupArtifact groupArtifact = new GroupArtifact(groupId, artifactId);

        if (scope == null) {
            return new RequestedVersion(groupArtifact, null, version);
        }

        RequestedVersion nearer = null;
        for (Map<GroupArtifact, RequestedVersion> nearerInScope : versionSelection.headMap(scope, true).values()) {
            RequestedVersion requestedVersion = nearerInScope.get(groupArtifact);
            if (requestedVersion != null) {
                nearer = requestedVersion;
                break;
            }
        }

        return versionSelection.get(scope)
                .getOrDefault(groupArtifact, new RequestedVersion(groupArtifact, nearer, version));
    }
}
