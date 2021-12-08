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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.*;

import java.net.URI;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public class RawMavenResolver {
    private static final Counter parentsResolved = Counter.builder("rewrite.maven.resolve.parents").register(Metrics.globalRegistry);
    private static final Counter dependencyManagementResolved = Counter.builder("rewrite.maven.resolve.dependency.management").register(Metrics.globalRegistry);

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

    public RawMavenResolver(MavenPomDownloader downloader, Collection<String> activeProfiles,
                            boolean resolveOptional, ExecutionContext ctx) {
        this.versionSelection = new TreeMap<>();
        for (Scope scope : Scope.values()) {
            versionSelection.putIfAbsent(scope, new HashMap<>());
        }
        this.downloader = downloader;
        this.activeProfiles = activeProfiles;
        this.resolveOptional = resolveOptional;
        this.ctx = new MavenExecutionContextView(ctx);
    }

    @Nullable
    public MavenModel resolve(RawMaven rawMaven, Map<String, String> effectiveProperties) {
        Pom pom = resolve(rawMaven, Scope.None, rawMaven.getPom().getVersion(), effectiveProperties, ctx.getRepositories());
        assert pom != null;
        rawMaven.getSample().stop(MetricsHelper.successTags(Timer.builder("rewrite.parse")
                        .description("The time spent parsing a Maven POM file")
                        .tag("file.type", "Maven"))
                .register(Metrics.globalRegistry));
        return new MavenModel(randomId(), pom);
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
    public Pom resolve(RawMaven rawMaven, Scope scope, @Nullable String requestedVersion,
                       Map<String, String> effectiveProperties, List<MavenRepository> repositories) {
        return resolve(rawMaven, scope, requestedVersion, effectiveProperties, null, repositories, null);
    }

    @Nullable
    private Pom resolve(RawMaven rawMaven, Scope scope, @Nullable String requestedVersion,
                        Map<String, String> effectiveProperties, @Nullable PartialMaven projectPom,
                        List<MavenRepository> repositories, @Nullable Set<PartialTreeKey> seenParentPoms) {

        ResolutionTask rootTask = new ResolutionTask(scope, rawMaven, emptySet(),
                false, null, null, requestedVersion, effectiveProperties,
                repositories, projectPom, seenParentPoms);

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
        task.getRawMaven().getActiveProperties(activeProfiles).forEach(task.getEffectiveProperties()::putIfAbsent);
        if (task.getRawMaven().getPom().getProperties() != null) {
            partialMaven.setProperties(task.getRawMaven().getPom().getProperties());
        }

        partialMaven.setEffectiveProperties(task.getEffectiveProperties());
    }

    private void processDependencyManagement(ResolutionTask task, PartialMaven partialMaven) {
        RawPom pom = task.getRawMaven().getPom();
        List<DependencyManagementDependency> managedDependencies = new ArrayList<>();

        for (RawPom.Dependency d : pom.getActiveDependencyManagementDependencies(activeProfiles)) {
            if (d.getVersion() == null) {
                continue;
            }

            String groupId = partialMaven.getRequiredValue(d.getGroupId());
            String artifactId = partialMaven.getRequiredValue(d.getArtifactId());
            String version = partialMaven.getValue(d.getVersion(), false);

            if (groupId == null || artifactId == null) {
                continue;
            }

            // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#importing-dependencies
            if (Objects.equals(d.getType(), "pom") && Objects.equals(d.getScope(), "import")) {
                if (version == null) {
                    ctx.getOnError().accept(new MavenParsingException(
                            "Problem with dependencyManagement section of %s:%s:%s. Unable to determine version of " +
                                    "managed dependency %s:%s.",
                            pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), d.getGroupId(), d.getArtifactId()));
                } else {
                    RawMaven rawMaven = downloader.download(groupId, artifactId, version, null, null,
                            partialMaven.getRepositories(), ctx);
                    if (rawMaven != null) {
                        dependencyManagementResolved.increment();
                        Pom maven = resolve(rawMaven, Scope.Compile, d.getVersion(), new HashMap<>(), partialMaven.getRepositories());
                        if (maven != null) {
                            managedDependencies.add(new DependencyManagementDependency.Imported(groupId, artifactId,
                                    version, d.getVersion(), maven));
                        }
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

        // Parent dependencies wind up being part of the subtree rooted at "task", so affect conflict resolution further down tree.ls
        if (partialMaven.getParent() != null) {
            for (Pom.Dependency dependency : partialMaven.getParent().getDependencies()) {
                RequestedVersion requestedVersion = selectVersion(dependency.getScope(), dependency.getGroupId(),
                        dependency.getArtifactId(), dependency.getVersion());
                versionSelection.get(dependency.getScope()).put(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()), requestedVersion);
            }
        }

        partialMaven.setDependencyTasks(rawMaven.getActiveDependencies(activeProfiles).stream()
                .filter(dep -> rawMaven.isProjectPom() || (resolveOptional || dep.getOptional() == null || !dep.getOptional()))
                .map(dep -> {
                    // replace property references, source versions from dependency management sections, etc.
                    String groupId = partialMaven.getRequiredValue(dep.getGroupId());
                    String artifactId = partialMaven.getRequiredValue(dep.getArtifactId());

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

                    if (dep.getOptional() != null && dep.getOptional() && task.getProjectPom() != null) {
                        return null;
                    }

                    int i = 0;
                    String last;

                    //Resolve the dependency scope. if the scope is set in the pom, it take precedence over the
                    //managed scope. This allows a downstream pom to override the managed scope.
                    String scope = dep.getScope() != null ? partialMaven.getRequiredValue(dep.getScope()) : null;

                    if (scope == null) {
                        // loop so that when dependencyManagement refers to a property that we take another pass to resolve the property.
                        do {
                            last = scope;
                            String result = null;
                            if (last != null) {
                                String partialMavenScope = partialMaven.getRequiredValue(last);
                                if (partialMavenScope != null) {
                                    result = partialMavenScope;
                                }
                            }
                            if (result == null) {
                                result = partialMaven.getDependencyManagement().getManagedScope(groupId, artifactId);
                                if (result == null && partialMaven.getParent() != null) {
                                    result = partialMaven.getParent().getManagedScope(groupId, artifactId);
                                }
                            }
                            scope = result;
                        } while (i++ < 2 || !Objects.equals(scope, last));
                    }
                    Scope requestedScope = Scope.fromName(scope);
                    Scope effectiveScope = requestedScope.transitiveOf(task.getScope());

                    if (effectiveScope == null || Scope.Invalid.equals(effectiveScope)) {
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

                    //Determine if there is a managed version of the artifact.
                    String managedVersion = null;

                    PartialMaven projectPom = task.getProjectPom();
                    if (projectPom != null) {
                        // prefer dependency management from the project over the dependency's dependency management
                        managedVersion = projectPom.getDependencyManagement().getManagedVersion(groupId, artifactId);
                        if (managedVersion == null && projectPom.getParent() != null) {
                            managedVersion = projectPom.getParent().getManagedVersion(groupId, artifactId);
                        }
                    }

                    // loop so that when dependencyManagement refers to a property that we take another pass to resolve the property.
                    do {
                        last = managedVersion;
                        String result = null;
                        if (last != null) {
                            String partialMavenVersion = partialMaven.getRequiredValue(last);
                            if (partialMavenVersion != null) {
                                result = partialMavenVersion;
                            }
                        }
                        if (result == null) {
                            OUTER:
                            for (DependencyManagementDependency managed : partialMaven.getDependencyManagement().getDependencies()) {
                                for (DependencyDescriptor dependencyDescriptor : managed.getDependencies()) {
                                    if (groupId.equals(partialMaven.getRequiredValue(dependencyDescriptor.getGroupId())) &&
                                            artifactId.equals(partialMaven.getRequiredValue(dependencyDescriptor.getArtifactId()))) {
                                        result = dependencyDescriptor.getVersion();
                                        break OUTER;
                                    }
                                }
                            }

                            if (result == null && partialMaven.getParent() != null) {
                                result = partialMaven.getParent().getManagedVersion(groupId, artifactId);
                            }
                        }
                        managedVersion = result;
                    } while (i++ < 2 || !Objects.equals(managedVersion, last));

                    //Figure out which version should be used. If the artifact has an explicitly defined
                    //version and it is not a transitive dependency of a managed dependency, the artifact
                    //version "wins" over the managed dependency.

                    String version = partialMaven.getValue(dep.getVersion(), false);
                    managedVersion = partialMaven.getValue(managedVersion, false);

                    if ((task.getProjectPom() != null || version == null) && managedVersion != null) {
                        version = managedVersion;
                    }

                    if (version == null) {
                        ctx.getOnError().accept(new MavenParsingException("Failed to determine version for %s:%s. Initial value was %s. Including POM is at %s",
                                groupId, artifactId, dep.getVersion(), rawMaven));
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
                                groupId, artifactId, version, rawMaven));
                        return null;
                    }

                    Set<GroupArtifact> exclusions;
                    if (dep.getExclusions() == null) {
                        exclusions = task.getExclusions();
                    } else {
                        exclusions = new HashSet<>(task.getExclusions());
                        for (GroupArtifact ex : dep.getExclusions()) {
                            GroupArtifact groupArtifact = new GroupArtifact(
                                    partialMaven.getRequiredValue(ex.getGroupId()),
                                    partialMaven.getRequiredValue(ex.getArtifactId())
                            );
                            GroupArtifact artifact = new GroupArtifact(
                                    groupArtifact.getGroupId() == null ? ".*" : groupArtifact.getGroupId().replace("*", ".*"),
                                    groupArtifact.getArtifactId() == null ? ".*" : groupArtifact.getArtifactId().replace("*", ".*")
                            );
                            exclusions.add(artifact);
                        }
                    }

                    ResolutionTask resolutionTask = new ResolutionTask(
                            requestedScope,
                            download,
                            exclusions,
                            dep.getOptional() != null && dep.getOptional(),
                            dep.getClassifier(),
                            dep.getType(),
                            dep.getVersion(),
                            new HashMap<>(),
                            partialMaven.getRepositories(),
                            task.getProjectPom() == null ? partialMaven : task.getProjectPom(),
                            task.getSeenParentPoms()
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

            PartialTreeKey gav = new PartialTreeKey(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion(),
                    task.getExclusions());
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
                PartialTreeKey parentKey = new PartialTreeKey(rawParent.getGroupId(), rawParent.getArtifactId(), rawParent.getVersion(),
                        task.getExclusions());
                Optional<Pom> maybeParent = resolved.get(parentKey);

                //noinspection OptionalAssignedToNull
                if (maybeParent == null) {
                    parentsResolved.increment();
                    parent = resolve(rawParentModel, Scope.None, rawParent.getVersion(), partialMaven.getEffectiveProperties(), task.getProjectPom(), partialMaven.getRepositories(), parentPomSightings);
                    resolved.put(parentKey, Optional.ofNullable(parent));
                } else {
                    parent = maybeParent.orElse(null);
                    if (parent != null) {
                        //Need to populate the effective properties using the cached values from the pom.
                        parent.getPropertyOverrides().forEach(partialMaven.getEffectiveProperties()::putIfAbsent);
                        parent.getProperties().forEach(partialMaven.getEffectiveProperties()::putIfAbsent);
                    }
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
        PartialTreeKey taskKey = new PartialTreeKey(rawPom.getGroupId(), rawPom.getArtifactId(), rawPom.getVersion(),
                task.getExclusions());

        Optional<Pom> result = resolved.get(taskKey);

        Stack<ResolutionTask> nextAssemblyStack = new Stack<>();
        nextAssemblyStack.addAll(assemblyStack);
        nextAssemblyStack.push(task);

        //noinspection OptionalAssignedToNull
        if (result == null) {
            PartialMaven partial = partialResults.get(task);
            if (partial != null) {
                Set<GroupArtifact> exclusions = new HashSet<>();
//                exclusions.addAll(task.getExclusions()); // is this necessary?
                for (ResolutionTask assembly : assemblyStack) {
                    PartialMaven assemblyPartial = partialResults.get(assembly);
                    if (assemblyPartial != null) {
                        for (ResolutionTask depTask : assemblyPartial.getDependencyTasks()) {
                            RawPom depTaskPom = depTask.getRawMaven().getPom();
                            RawPom taskPom = task.getRawMaven().getPom();
                            if (!depTask.getExclusions().isEmpty() &&
                                    depTaskPom.getGroupId() != null &&
                                    depTaskPom.getGroupId().equals(taskPom.getGroupId()) &&
                                    depTaskPom.getArtifactId().equals(taskPom.getArtifactId())) {
                                exclusions.addAll(depTask.getExclusions());
                            }
                        }
                    }
                }

                List<Pom.Dependency> dependencies = new ArrayList<>(partial.getDependencyTasks().size());
                nextDep:
                for (ResolutionTask depTask : partial.getDependencyTasks()) {
                    RawPom depTaskPom = depTask.getRawMaven().getPom();
                    for (GroupArtifact exclusion : exclusions) {
                        if (exclusion.getGroupId().equals(depTaskPom.getGroupId()) &&
                                exclusion.getArtifactId().equals(depTaskPom.getArtifactId())) {
                            continue nextDep;
                        }
                    }

                    boolean optional = depTask.isOptional();

                    Pom resolved = assembleResults(depTask, nextAssemblyStack);
                    if (resolved == null) {
                        continue;
                    }

                    dependencies.add(new Pom.Dependency(
                            depTask.getRawMaven().getRepository(),
                            depTask.getScope(),
                            depTask.getClassifier(),
                            depTask.getType(),
                            optional,
                            resolved,
                            depTask.getRequestedVersion(),
                            depTask.getExclusions()
                    ));
                }

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

                            Pom conflictResolved = conflictResolvedRaw == null ? null :
                                    assembleResults(
                                            new ResolutionTask(
                                                    scope,
                                                    conflictResolvedRaw,
                                                    ancestorDep.getExclusions(),
                                                    ancestorDep.isOptional(),
                                                    ancestorDep.getClassifier(),
                                                    ancestorDep.getType(),
                                                    ancestorDep.getRequestedVersion(),
                                                    partial.effectiveProperties,
                                                    task.getRepositories(),
                                                    task.getProjectPom(),
                                                    null), nextAssemblyStack);

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

                Map<String, String> propertyOverrides = new HashMap<>();

                if (partial.getProperties() != null) {
                    partial.getProperties().forEach((k, v) -> {
                        if (!Objects.equals(v, partial.getEffectiveProperties().get(k))) {
                            propertyOverrides.put(k, v);
                        }
                    });
                }

                result = Optional.of(
                        Pom.build(
                                groupId,
                                rawPom.getArtifactId(),
                                version,
                                rawPom.getSnapshotVersion(),
                                rawPom.getName(),
                                rawPom.getDescription(),
                                partial.getRequiredValue(rawPom.getPackaging()),
                                null,
                                partial.getParent(),
                                dependencies,
                                partial.getDependencyManagement(),
                                partial.getLicenses(),
                                partial.getRepositories(),
                                partial.getProperties(),
                                propertyOverrides,
                                false
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
        String url = partialMaven.getRequiredValue(repo.getUrl());
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

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Value
    public static class ResolutionTask {
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

        /**
         * This is used to keep track of all properties encountered as raw maven poms are resolved into their partial forms.
         * A property key may exist in multiple places as the resolver walks the dependencies and the value of the property
         * is set the first time the property is encountered.
         * <p>
         * A property in the root (the first pom to be resolved) will take precedence over a property defined in the parent.
         * A property in a parent will take precedence over the same property defined in a transitive pom file.
         * An effective property may have a value that, itself contains a property place holder to a second property and
         * resolution of placeholders is done on-demand when a value is requested from the pom.
         */
        @EqualsAndHashCode.Include
        Map<String, String> effectiveProperties;

        @EqualsAndHashCode.Include
        List<MavenRepository> repositories;

        @EqualsAndHashCode.Include
        PartialMaven projectPom;

        @Nullable
        Set<PartialTreeKey> seenParentPoms;

        public Set<GroupArtifact> getExclusions() {
            return exclusions == null ? emptySet() : exclusions;
        }
    }

    @Value
    static class PartialTreeKey {
        String groupId;
        String artifactId;
        String version;
        Set<GroupArtifact> exclusions;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @Getter
    @Setter
    public class PartialMaven {
        @EqualsAndHashCode.Include
        final RawPom rawPom;

        Pom parent;
        Pom.DependencyManagement dependencyManagement;
        Collection<ResolutionTask> dependencyTasks = emptyList();
        Collection<Pom.License> licenses = emptyList();
        Collection<MavenRepository> repositories = emptyList();

        //The properties parsed direction from this pom's XML file. The values may be different than the effective property
        //values.
        Map<String, String> properties = emptyMap();

        //Effective properties are collected across all pom.xml files that were resolved during a parse. These properties
        //reflect what the value should be in the context of the entire maven tree and account for property precedence
        //when the same property key is encountered multiple times.
        Map<String, String> effectiveProperties = emptyMap();

        /**
         * The order of repositories should be:
         * 1. repos in profiles
         * 2. my repos repos
         * 3. parent pom repos
         */
        public List<MavenRepository> getRepositories() {
            List<MavenRepository> allRepositories = new ArrayList<>(repositories);
            Pom ancestor = parent;
            while (ancestor != null) {
                allRepositories.addAll(ancestor.getRepositories());
                ancestor = ancestor.getParent();
            }
            return allRepositories;
        }

        @Nullable
        public String getValue(@Nullable String v, boolean required) {
            if (v == null) {
                return null;
            }

            try {
                return placeholderHelper.replacePlaceholders(v, key -> {
                    if (key == null) {
                        return null;
                    }
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
                    }

                    String value = System.getProperty(key);
                    if (value != null) {
                        return value;
                    }

                    value = effectiveProperties.get(key);
                    if (value != null) {
                        return value;
                    }

                    if (required) {
                        ctx.getOnError().accept(new MavenParsingException("Unable to resolve property %s. Including POM is at %s", v, rawPom));
                    }
                    return null;
                });
            } catch (Throwable t) {
                if (required) {
                    ctx.getOnError().accept(new MavenParsingException("Unable to resolve property %s. Including POM is at %s", v, rawPom));
                }
                return null;
            }
        }

        /**
         * Recursively substitutes properties for their values until the value is no longer
         * a property reference.
         *
         * @param v The starting value, which may or may not be a property reference.
         * @return A fixed value or <code>null</code> if the referenced property cannot be found.
         */
        @Nullable
        public String getRequiredValue(@Nullable String v) {
            return getValue(v, true);
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
