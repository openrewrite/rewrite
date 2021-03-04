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

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class RawMavenResolver {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", null);

    // This is used to keep track of what versions have been seen further up the tree so we don't unnecessarily
    // resolve subtrees that have no chance of being selected by conflict resolution.
    private final NavigableMap<Scope, Map<GroupArtifact, RequestedVersion>> versionSelection;

    // The breadth-first queue of resolution tasks.
    private final Queue<ResolutionTask> workQueue = new LinkedList<>();

    private final Map<PartialTreeKey, Optional<Pom>> resolved = new HashMap<>();
    private final Map<ResolutionTask, PartialMaven> partialResults = new HashMap<>();

    private final MavenPomDownloader downloader;

    private final Collection<String> activeProfiles;
    private final boolean resolveOptional;

    private final MavenExecutionContextView ctx;

    @Nullable
    private final Path projectDir;

    public RawMavenResolver(MavenPomDownloader downloader, Collection<String> activeProfiles,
                            boolean resolveOptional, ExecutionContext ctx, @Nullable Path projectDir) {
        this.versionSelection = new TreeMap<>();
        for (Scope scope : Scope.values()) {
            versionSelection.putIfAbsent(scope, new HashMap<>());
        }
        this.downloader = downloader;
        this.activeProfiles = activeProfiles;
        this.resolveOptional = resolveOptional;
        this.ctx = new MavenExecutionContextView(ctx);
        this.projectDir = projectDir;
    }

    @Nullable
    public Xml.Document resolve(RawMaven rawMaven) {
        Pom pom = resolve(rawMaven, Scope.None, rawMaven.getPom().getVersion(), ctx.getRepositories());
        assert pom != null;
        return rawMaven.getDocument().withMarkers(rawMaven.getDocument().getMarkers()
                .compute(pom, (old, n) -> n));
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
    public Pom resolve(RawMaven rawMaven, Scope scope, @Nullable String requestedVersion, Collection<MavenRepository> repositories) {
        return resolve(rawMaven, scope, requestedVersion, repositories, null);
    }

    @Nullable
    private Pom resolve(RawMaven rawMaven, Scope scope, @Nullable String requestedVersion, Collection<MavenRepository> repositories,
                        @Nullable LinkedHashSet<PartialTreeKey> seenParentPoms) {
        ResolutionTask rootTask = new ResolutionTask(scope, rawMaven, emptySet(),
                false, null, null, requestedVersion, repositories, seenParentPoms);

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

        PartialMaven partialMaven = new PartialMaven(rawMaven.getPom());
        processProperties(task, partialMaven);
        processRepositories(partialMaven, task);
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

        for (RawPom.Dependency d : pom.getActiveDependencyManagementDependencies(activeProfiles)) {
            if (d.getVersion() == null) {
                ctx.getOnError().accept(new MavenParsingException(
                        "Problem with dependencyManagement section of %s:%s:%s. Unable to determine version of managed dependency %s:%s",
                        pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), d.getGroupId(), d.getArtifactId()));
            }
            assert d.getVersion() != null;

            String groupId = partialMaven.getValue(d.getGroupId());
            String artifactId = partialMaven.getValue(d.getArtifactId());
            String version = partialMaven.getValue(d.getVersion());

            if (groupId == null || artifactId == null || version == null) {
                ctx.getOnError().accept(new MavenParsingException(
                        "Problem with dependencyManagement section of %s:%s:%s. Unable to determine groupId, " +
                                "artifactId, or version of managed dependency %s:%s.",
                        pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), d.getGroupId(), d.getArtifactId()));
            }
            assert groupId != null;
            assert artifactId != null;
            assert version != null;

            // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#importing-dependencies
            if (Objects.equals(d.getType(), "pom") && Objects.equals(d.getScope(), "import")) {
                RawMaven rawMaven = downloader.download(groupId, artifactId, version, null, null,
                        partialMaven.getRepositories(), ctx);
                if (rawMaven != null) {
                    Pom maven = new RawMavenResolver(downloader, activeProfiles, resolveOptional, ctx, projectDir)
                            .resolve(rawMaven, Scope.Compile, d.getVersion(), partialMaven.getRepositories());

                    if (maven != null) {
                        managedDependencies.add(new DependencyManagementDependency.Imported(groupId, artifactId,
                                version, d.getVersion(), maven));
                    }
                }
            } else {
                Scope scope = d.getScope() == null ? null : Scope.fromName(d.getScope());
                if (!Scope.Invalid.equals(scope)) {
                    managedDependencies.add(new DependencyManagementDependency.Defined(
                            groupId, artifactId, version, d.getVersion(),
                            scope,
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
                .filter(dep -> resolveOptional || dep.getOptional() == null || !dep.getOptional())
                .map(dep -> {
                    // replace property references, source versions from dependency management sections, etc.
                    String groupId = partialMaven.getValue(dep.getGroupId());
                    String artifactId = partialMaven.getValue(dep.getArtifactId());

                    RawPom includingPom = rawMaven.getPom();
                    if (groupId == null) {
                        ctx.getOnError().accept(new MavenParsingException(
                                "Problem resolving dependency of %s:%s:%s. Unable to determine groupId.",
                                includingPom.getGroupId(), includingPom.getArtifactId(), includingPom.getVersion()));
                        return null;
                    }
                    if (artifactId == null) {
                        ctx.getOnError().accept(new MavenParsingException(
                                "Problem resolving dependency of %s:%s:%s. Unable to determine artifactId.",
                                includingPom.getGroupId(), includingPom.getArtifactId(), includingPom.getVersion()));
                        return null;
                    }

                    // Handle dependency exclusions
                    for (GroupArtifact e : task.getExclusions()) {
                        try {
                            if (dep.getGroupId().matches(e.getGroupId()) &&
                                    dep.getArtifactId().matches(e.getArtifactId())) {
                                return null;
                            }
                        } catch (Exception exception) {
                            ctx.getOnError().accept(exception);
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
                            String partialMavenVersion = partialMaven.getValue(last);
                            if (partialMavenVersion != null) {
                                result = partialMavenVersion;
                            }
                        }
                        if (result == null) {
                            OUTER:
                            for (DependencyManagementDependency managed : partialMaven.getDependencyManagement().getDependencies()) {
                                for (DependencyDescriptor dependencyDescriptor : managed.getDependencies()) {
                                    if (groupId.equals(partialMaven.getValue(dependencyDescriptor.getGroupId())) &&
                                            artifactId.equals(partialMaven.getValue(dependencyDescriptor.getArtifactId()))) {
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
                            version = partialMaven.getValue(depVersion);
                        }
                    }

                    if (version == null) {
                        ctx.getOnError().accept(new MavenParsingException("Failed to determine version for %s:%s. Initial value was %s. Including POM is at %s",
                                groupId, artifactId, dep.getVersion(), rawMaven));
                        return null;
                    }

                    String scope = null;
                    // loop so that when dependencyManagement refers to a property that we take another pass to resolve the property.
                    i = 0;
                    do {
                        last = scope;
                        String result = null;
                        if (last != null) {
                            String partialMavenScope = partialMaven.getValue(last);
                            if (partialMavenScope != null) {
                                result = partialMavenScope;
                            }
                        }
                        if (result == null) {
                            OUTER:
                            for (DependencyManagementDependency managed : partialMaven.getDependencyManagement().getDependencies()) {
                                for (DependencyDescriptor dependencyDescriptor : managed.getDependencies()) {
                                    if (groupId.equals(partialMaven.getValue(dependencyDescriptor.getGroupId())) &&
                                            artifactId.equals(partialMaven.getValue(dependencyDescriptor.getArtifactId())) &&
                                            dependencyDescriptor.getScope() != null) {
                                        result = dependencyDescriptor.getScope().name().toLowerCase();
                                        break OUTER;
                                    }
                                }
                            }

                            if (result == null && partialMaven.getParent() != null) {
                                result = partialMaven.getParent().getManagedScope(groupId, artifactId);
                            }
                        }
                        scope = result;
                    } while (i++ < 2 || !Objects.equals(scope, last));

                    // dependencyManagement takes precedence over the scope specified on the dependency
                    if (scope == null) {
                        String depScope = dep.getScope();
                        if (depScope != null) {
                            scope = partialMaven.getValue(depScope);
                        }
                    }

                    Scope requestedScope = Scope.fromName(scope);
                    Scope effectiveScope = requestedScope.transitiveOf(task.getScope());

                    if (effectiveScope == null || Scope.Invalid.equals(effectiveScope)) {
                        return null;
                    }

                    RequestedVersion requestedVersion = selectVersion(effectiveScope, groupId, artifactId, version);
                    versionSelection.get(effectiveScope).put(new GroupArtifact(groupId, artifactId), requestedVersion);
                    version = requestedVersion.resolve(downloader, partialMaven.getRepositories());

                    if (version == null || version.contains("${")) {
                        ctx.getOnError().accept(new MavenParsingException("Unable to download %s:%s:%s. Including POM is at %s",
                                groupId, artifactId, version, rawMaven));
                        return null;
                    }

                    RawMaven download = downloader.download(groupId, artifactId,
                            version, null, rawMaven,
                            partialMaven.getRepositories(), ctx);

                    if (download == null) {
                        ctx.getOnError().accept(new MavenParsingException("Unable to download %s:%s:%s. Including POM is at %s",
                                groupId, artifactId, version, rawMaven.getSourcePath()));
                        return null;
                    }

                    ResolutionTask resolutionTask = new ResolutionTask(
                            requestedScope,
                            download,
                            dep.getExclusions() == null ?
                                    emptySet() :
                                    dep.getExclusions().stream()
                                            .map(ex -> new GroupArtifact(
                                                    partialMaven.getValue(ex.getGroupId()),
                                                    partialMaven.getValue(ex.getArtifactId())
                                            ))
                                            .map(ex -> new GroupArtifact(
                                                    ex.getGroupId() == null ? ".*" : ex.getGroupId().replace("*", ".*"),
                                                    ex.getArtifactId() == null ? ".*" : ex.getArtifactId().replace("*", ".*")
                                            ))
                                            .collect(Collectors.toSet()),
                            dep.getOptional() != null && dep.getOptional(),
                            dep.getClassifier(),
                            dep.getType(),
                            dep.getVersion(),
                            partialMaven.getRepositories(),
                            null
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
        if (pom.getParent() != null) {
            RawPom.Parent rawParent = pom.getParent();
            // With "->" indicating a "has parent" relationship, parentPomSightings is used to detect cycles like
            // A -> B -> A
            // And cut them off early with a clearer, more actionable error than a stack overflow
            LinkedHashSet<PartialTreeKey> parentPomSightings;
            if (task.getSeenParentPoms() == null) {
                parentPomSightings = new LinkedHashSet<>();
            } else {
                parentPomSightings = new LinkedHashSet<>(task.getSeenParentPoms());
            }

            PartialTreeKey gav = new PartialTreeKey(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion());
            if (parentPomSightings.contains(gav)) {
                ctx.getOnError().accept(new MavenParsingException("Cycle in parent poms detected: " + gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion() + " is its own parent by way of these poms:\n" + parentPomSightings.stream()
                        .map(it -> it.groupId + ":" + it.getArtifactId() + ":" + it.getVersion())
                        .collect(joining("\n"))));
                return;
            }
            parentPomSightings.add(gav);

            Pom parent = null;
            RawMaven rawParentModel = downloader.download(rawParent.getGroupId(), rawParent.getArtifactId(),
                    rawParent.getVersion(), rawParent.getRelativePath(), rawMaven,
                    partialMaven.getRepositories(), ctx);
            if (rawParentModel != null) {
                PartialTreeKey parentKey = new PartialTreeKey(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion());
                Optional<Pom> maybeParent = resolved.get(parentKey);

                //noinspection OptionalAssignedToNull
                if (maybeParent == null) {
                    parent = new RawMavenResolver(downloader, activeProfiles, resolveOptional, ctx, projectDir)
                            .resolve(rawParentModel, Scope.Compile, rawParent.getVersion(), partialMaven.getRepositories(), parentPomSightings);
                    resolved.put(parentKey, Optional.ofNullable(parent));
                } else {
                    parent = maybeParent.orElse(null);
                }
            }
            partialMaven.setParent(parent);
        }
    }

    private void processRepositories(PartialMaven partialMaven, ResolutionTask task) {
        Set<MavenRepository> repositories = new LinkedHashSet<>();
        for (RawRepositories.Repository repo : task.getRawMaven().getPom().getActiveRepositories(activeProfiles)) {
            MavenRepository mapped = processRepository(partialMaven, repo);
            if (mapped == null) {
                continue;
            }
            mapped = MavenRepositoryMirror.apply(ctx.getMirrors(), mapped);
            mapped = MavenRepositoryCredentials.apply(ctx.getCredentials(), mapped);
            repositories.add(mapped);
        }

        repositories.addAll(task.getRepositories());
        partialMaven.setRepositories(repositories);
    }

    private void processLicenses(ResolutionTask task, PartialMaven partialMaven) {
        List<RawPom.License> licenses = task.getRawMaven().getPom().getInnerLicenses();
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

                            Pom resolved = assembleResults(depTask, nextAssemblyStack);
                            if (resolved == null) {
                                return null;
                            }

                            return new Pom.Dependency(
                                    depTask.getRawMaven().getRepository(),
                                    depTask.getScope(),
                                    depTask.getClassifier(),
                                    depTask.getType(),
                                    optional,
                                    resolved,
                                    depTask.getRequestedVersion(),
                                    depTask.getRawMaven().getPom().getSnapshotVersion(),
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

                        if (!ancestorDep.getVersion().equals(conflictResolvedVersion)) {
                            assert conflictResolvedVersion != null;
                            RawMaven conflictResolvedRaw = downloader.download(groupId, artifactId, conflictResolvedVersion,
                                    null, null, task.getRepositories(), ctx);

                            Pom conflictResolved = conflictResolvedRaw == null ? null : assembleResults(new ResolutionTask(scope, conflictResolvedRaw,
                                    ancestorDep.getExclusions(), ancestorDep.isOptional(), ancestorDep.getRequestedVersion(),
                                    ancestorDep.getClassifier(), ancestorDep.getType(), task.getRepositories(), null), nextAssemblyStack);

                            if (conflictResolved == null) {
                                dependencies.add(ancestorDep);
                            } else {
                                dependencies.add(new Pom.Dependency(
                                        rawMaven.getRepository(),
                                        scope,
                                        ancestorDep.getClassifier(),
                                        ancestorDep.getType(),
                                        ancestorDep.isOptional(),
                                        conflictResolved,
                                        ancestorDep.getRequestedVersion(),
                                        ancestorDep.getDatedSnapshotVersion(),
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

                result = Optional.of(
                        new Pom(
                                groupId,
                                rawPom.getArtifactId(),
                                version,
                                rawPom.getName(),
                                rawPom.getDescription(),
                                rawPom.getSnapshotVersion(),
                                rawPom.getPackaging(),
                                null,
                                partial.getParent(),
                                dependencies,
                                partial.getDependencyManagement(),
                                partial.getLicenses(),
                                partial.getRepositories(),
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

    @Nullable
    private MavenRepository processRepository(PartialMaven partialMaven, @Nullable RawRepositories.Repository repo) {
        if (repo == null) {
            return null;
        }
        String url = partialMaven.getValue(repo.getUrl());
        return url == null ? null : processRepository(repo.withUrl(url));
    }

    @Nullable
    private MavenRepository processRepository(@Nullable RawRepositories.Repository repo) {
        if (repo == null) {
            return null;
        }
        try {
            // Prevent malformed URLs from being used
            return new MavenRepository(repo.getId(), URI.create(repo.getUrl().trim()),
                    repo.getReleases() == null || repo.getReleases().isEnabled(),
                    repo.getSnapshots() != null && repo.getSnapshots().isEnabled(),
                    null, null);
        } catch (Throwable t) {
            ctx.getOnError().accept(new MavenParsingException("Invalid repository URL %s", t, repo.getUrl()));
            return null;
        }
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
        String type;

        @EqualsAndHashCode.Include
        @Nullable
        String requestedVersion;

        Collection<MavenRepository> repositories;

        @Nullable
        LinkedHashSet<PartialTreeKey> seenParentPoms;

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
        final RawPom rawPom;

        Pom parent;
        Pom.DependencyManagement dependencyManagement;
        Collection<ResolutionTask> dependencyTasks = emptyList();
        Collection<Pom.License> licenses = emptyList();
        Collection<MavenRepository> repositories = emptyList();
        Map<String, String> properties = emptyMap();

        /**
         * The order of repositories should be:
         * 1. repos in profiles
         * 2. my repos repos
         * 3. parent pom repos
         */
        public List<MavenRepository> getRepositories() {
            List<MavenRepository> allRepositories = new ArrayList<>(repositories);
            Pom ancestor = parent;
            while(ancestor != null) {
                allRepositories.addAll(ancestor.getRepositories());
                ancestor = ancestor.getParent();
            }
            return allRepositories;
        }

        /**
         * Recursively substitutes properties for their values until the value is no longer
         * a property reference.
         *
         * @param v The starting value, which may or may not be a property reference.
         * @return A fixed value or <code>null</code> if the referenced property cannot be found.
         */
        @Nullable
        public String getValue(@Nullable String v) {
            if (v == null) {
                return null;
            }

            try {
                return placeholderHelper.replacePlaceholders(v, key -> {
                    switch (key) {
                        case "groupId":
                        case "project.groupId":
                        case "pom.groupId":
                            String groupId = rawPom.getGroupId();
                            if (groupId != null) {
                                return groupId;
                            }
                            return parent == null ? null : parent.getGroupId();
                        case "project.parent.groupId":
                            return parent != null ? parent.getGroupId() : null;
                        case "artifactId":
                        case "project.artifactId":
                        case "pom.artifactId":
                            return rawPom.getArtifactId(); // cannot be inherited from parent
                        case "project.parent.artifactId":
                            return parent == null ? null : parent.getArtifactId();
                        case "version":
                        case "project.version":
                        case "pom.version":
                            String rawVersion = rawPom.getVersion();
                            if (rawVersion != null) {
                                return rawVersion;
                            }
                            return parent == null ? null : parent.getVersion();
                        case "project.parent.version":
                            return parent != null ? parent.getVersion() : null;
                        case "project.basedir":
                        case "basedir":
                            return projectDir == null ? null : projectDir.toString();
                    }

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
                        value = ancestor.getValue("${" + key + "}");
                        if (value != null) {
                            return value;
                        }
                    }

                    value = System.getProperty(key);
                    if (value != null) {
                        return value;
                    }

                    ctx.getOnError().accept(new MavenParsingException("Unable to resolve property %s. Including POM is at %s", v, rawPom));

                    return null;
                });
            } catch (Throwable t) {
                ctx.getOnError().accept(new MavenParsingException("Unable to resolve property %s. Including POM is at %s", v, rawPom));
                return null;
            }
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
