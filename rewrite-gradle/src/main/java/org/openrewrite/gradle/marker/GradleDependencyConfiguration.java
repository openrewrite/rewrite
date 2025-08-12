/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.gradle.attributes.Category;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.attributes.Attributed;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Version;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("unused")
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class GradleDependencyConfiguration implements Serializable, Attributed {
    /**
     * The name of the dependency configuration. Unique within a given project.
     */
    String name;

    @Nullable
    String description;

    boolean isTransitive;

    /**
     * Indicates that this configuration is intended for resolving a set of dependencies into a dependency graph. A resolvable configuration should not be declarable or consumable.
     * See <a href="https://docs.gradle.org/current/userguide/declaring_configurations.html#sec:configuration-flags-roles">Configuration flag roles</a>
     */
    boolean isCanBeResolved;

    /**
     * Indicates that this configuration is intended for exposing artifacts outside this project. A consumable configuration should not be declarable or resolvable.
     * See <a href="https://docs.gradle.org/current/userguide/declaring_configurations.html#sec:configuration-flags-roles">Configuration flag roles</a>
     */
    boolean isCanBeConsumed;

    /**
     * Indicates that this configuration is intended for declaring dependencies. A declarable configuration should not be resolvable or consumable.
     * See <a href="https://docs.gradle.org/current/userguide/declaring_configurations.html#sec:configuration-flags-roles">Configuration flag roles</a>
     */
    boolean isCanBeDeclared;

    /**
     * The list of zero or more configurations this configuration extends from.
     * The extended configuration's dependencies are all requested as part of this configuration, but different versions
     * may be resolved.
     */
    @NonFinal
    List<GradleDependencyConfiguration> extendsFrom;

    List<Dependency> requested;

    /**
     * The list of direct dependencies resolved for this configuration.
     */
    List<ResolvedDependency> directResolved;

    public List<ResolvedDependency> getDirectResolved() {
        return directResolved == null ? emptyList() : directResolved;
    }

    /**
     * The list of all dependencies resolved for this configuration, including transitive dependencies.
     */
    public List<ResolvedDependency> getResolved() {
        List<ResolvedDependency> resolved = new ArrayList<>(getDirectResolved());
        Map<GroupArtifact, ResolvedDependency> alreadyResolved = new HashMap<>();
        resolveTransitiveDependencies(resolved, alreadyResolved);
        return new ArrayList<>(alreadyResolved.values());
    }

    /**
     * The type of exception thrown when attempting to resolve this configuration. null if no exception was thrown.
     */
    @Nullable
    String exceptionType;

    /**
     * The message of the exception thrown when attempting to resolve this configuration. null if no exception was thrown.
     */
    @Nullable
    String message;

    /**
     * Lists the constraints applied to manage the versions of transitive dependencies.
     * Produces a list of _only_ those constraints applied directly to this configuration.
     * But configurations inherit the constraints of the configurations they extend, so to get all the constraints
     * actually in effect for a given configuration call getAllConstraints()
     */
    @NonFinal
    List<GradleDependencyConstraint> constraints;

    Map<String, String> attributes;

    /**
     * Lists all the constraints in effect for the current configuration, including those constraints inherited from
     * parent configurations.
     */
    List<GradleDependencyConstraint> getAllConstraints() {
        Set<GradleDependencyConstraint> constraintSet = new LinkedHashSet<>(constraints);
        for (GradleDependencyConfiguration parentConfiguration : allExtendsFrom()) {
            constraintSet.addAll(parentConfiguration.getConstraints());
        }
        return new ArrayList<>(constraintSet);
    }

    @Deprecated
    public GradleDependencyConfiguration(
            String name,
            @Nullable String description,
            boolean isTransitive,
            boolean isCanBeResolved,
            boolean isCanBeConsumed,
            List<GradleDependencyConfiguration> extendsFrom,
            List<Dependency> requested,
            List<ResolvedDependency> directResolved,
            @Nullable String exceptionType,
            @Nullable String message
    ) {
        this(name, description, isTransitive, isCanBeResolved, isCanBeConsumed,
                // Introduced in Gradle 8.2, but the concept is relevant for earlier versions as well.
                // Most of the time this means excluding "runtimeClasspath" and "compileClasspath", but not just the buildscript's "classpath"
                !name.endsWith("Classpath"),
                extendsFrom, requested, directResolved, exceptionType, message);

    }

    @Deprecated
    public GradleDependencyConfiguration(
            String name,
            @Nullable String description,
            boolean isTransitive,
            boolean isCanBeResolved,
            boolean isCanBeConsumed,
            boolean isCanBeDeclared,
            List<GradleDependencyConfiguration> extendsFrom,
            List<Dependency> requested,
            List<ResolvedDependency> directResolved,
            @Nullable String exceptionType,
            @Nullable String message
    ) {
        this.name = name;
        this.description = description;
        this.isTransitive = isTransitive;
        this.isCanBeResolved = isCanBeResolved;
        this.isCanBeConsumed = isCanBeConsumed;
        this.isCanBeDeclared = isCanBeDeclared;
        this.extendsFrom = extendsFrom;
        this.requested = requested;
        this.directResolved = directResolved;
        this.exceptionType = exceptionType;
        this.message = message;
        this.constraints = emptyList();
        this.attributes = emptyMap();
    }

    /**
     * List the configurations which are extended by the given configuration.
     * Assuming a hierarchy like:
     * <pre>
     *     implementation
     *     |> compileClasspath
     *     |> runtimeClasspath
     *     |> testImplementation
     *        |> testCompileClasspath
     *        |> testRuntimeClasspath
     * </pre>
     * <p>
     * When querying "testCompileClasspath" this function will return [testImplementation, implementation].
     */
    public List<GradleDependencyConfiguration> allExtendsFrom() {
        Set<GradleDependencyConfiguration> result = new LinkedHashSet<>();
        for (GradleDependencyConfiguration parentConfiguration : getExtendsFrom()) {
            result.add(parentConfiguration);
            result.addAll(parentConfiguration.allExtendsFrom());
        }
        return new ArrayList<>(result);
    }

    public @Nullable Dependency findRequestedDependency(@Nullable String groupId, String artifactId) {
        for (Dependency d : requested) {
            if (StringUtils.matchesGlob(d.getGav().getGroupId(), groupId) &&
                StringUtils.matchesGlob(d.getGav().getArtifactId(), artifactId)) {
                return d;
            }
        }
        return null;
    }

    public @Nullable ResolvedDependency findResolvedDependency(@Nullable String groupId, String artifactId) {
        for (ResolvedDependency d : directResolved) {
            ResolvedDependency dependency = d.findDependency(groupId == null ? "" : groupId, artifactId);
            if (dependency != null) {
                return dependency;
            }
        }
        return null;
    }

    public void unsafeSetExtendsFrom(List<GradleDependencyConfiguration> extendsFrom) {
        this.extendsFrom = extendsFrom;
    }

    public void unsafeSetConstraints(List<GradleDependencyConstraint> constraints) {
        this.constraints = constraints;
    }

    private static void resolveTransitiveDependencies(List<ResolvedDependency> resolved, Map<GroupArtifact, ResolvedDependency> alreadyResolved) {
        for (ResolvedDependency dependency : resolved) {
            GroupArtifact ga = dependency.getGav().asGroupArtifact();
            if (alreadyResolved.containsKey(ga)) {
                ResolvedDependency alreadyPresent = alreadyResolved.get(ga);
                Version newVersion = new Version(dependency.getVersion());
                Version presentVersion = new Version(alreadyPresent.getVersion());
                int compared = presentVersion.compareTo(newVersion);
                if (compared > 0 || (compared == 0 && alreadyPresent.getDependencies().size() == dependency.getDependencies().size())) {
                    continue;
                }
            }
            alreadyResolved.put(ga, dependency);
            resolveTransitiveDependencies(dependency.getDependencies(), alreadyResolved);
        }
    }

    /**
     * Merge two collections of constraints, giving precedence to the constraints in the preferred collection.
     * The preferred set are populated from resolved version numbers that cannot be explained only by things we can observe from Gradle's public API.
     * The others are the constraints reported from Gradle's public API.
     * When the preferred collection is null or empty, returns the others collection.
     * When the others collection is null or empty, returns the preferred collection.
     * If both are null or empty, returns an empty list.
     */
    public static List<GradleDependencyConstraint> merge(@Nullable Collection<GradleDependencyConstraint> preferred, @Nullable Collection<GradleDependencyConstraint> others) {
        if ((preferred == null || preferred.isEmpty()) && (others == null || others.isEmpty())) {
            return emptyList();
        }
        if (preferred == null || preferred.isEmpty()) {
            return new ArrayList<>(others);
        }
        if (others == null || others.isEmpty()) {
            return new ArrayList<>(preferred);
        }
        Map<GroupArtifact, GradleDependencyConstraint> results = preferred.stream().collect(toMap(it -> new GroupArtifact(it.getGroupId(), it.getArtifactId()), it -> it));
        for (GradleDependencyConstraint lowerPrecedenceConstraint : others) {
            results.putIfAbsent(new GroupArtifact(lowerPrecedenceConstraint.getGroupId(), lowerPrecedenceConstraint.getArtifactId()), lowerPrecedenceConstraint);
        }
        return new ArrayList<>(results.values());
    }

    public GradleDependencyConfiguration removeDirectDependencies(
            Collection<GroupArtifact> gas,
            List<MavenRepository> repositories,
            ExecutionContext ctx
    ) throws MavenDownloadingException {
        return mapDependencies(d -> {
            for (GroupArtifact gav : gas) {
                if (Objects.equals(d.getGroupId(), gav.getGroupId()) &&
                    Objects.equals(d.getArtifactId(), gav.getArtifactId())) {
                    return null;
                }
            }
            return d;
        }, repositories, ctx);
    }

    public GradleDependencyConfiguration upgradeDirectDependencies(
            Collection<GroupArtifactVersion> gavs,
            List<MavenRepository> repositories,
            ExecutionContext ctx
    ) throws MavenDownloadingException {
        return mapDependencies(d -> {
            for (GroupArtifactVersion gav : gavs) {
                if (Objects.equals(d.getGroupId(), gav.getGroupId()) &&
                    Objects.equals(d.getArtifactId(), gav.getArtifactId()) &&
                    !Objects.equals(d.getVersion(), Semver.max(d.getVersion(), gav.getVersion()))) {
                        return d.withGav(new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()));
                }
            }
            return d;
        }, repositories, ctx);
    }

    /**
     * Produces a GradleDependencyConfiguration where dependencies with the specified GroupId and ArtifactId have been
     * updated to use a new version number.
     */
    public GradleDependencyConfiguration upgradeDirectDependency(
            GroupArtifactVersion gav,
            List<MavenRepository> repositories,
            ExecutionContext ctx
    ) throws MavenDownloadingException {
        return mapDependencies(d -> {
            if ( Objects.equals(d.getGroupId(), gav.getGroupId()) &&
                 Objects.equals(d.getArtifactId(), gav.getArtifactId()) &&
                 !Objects.equals(d.getVersion(), Semver.max(d.getVersion(), gav.getVersion()))) {
                return d.withGav(gav);
            }
            return d;
        }, repositories, ctx);
    }

    /**
     * Produce a new GradleDependencyConfiguration according to the supplied mapping function.
     *
     * @param mapping An arbitrary mapping function which is applied to the requested dependencies of the named configuration.
     * @param repositories The repositories to resolve the new requested dependencies from.
     * @param ctx the ExecutionContext of a recipe run suitable for retrieving HTTP communication and dependency caching configuration.
     * @return A GradleDependencyConfiguration with its requested and resolved dependencies upgraded according to the mapping function.
     *         Or the original GradleDependencyConfiguration if the mapping function made no changes.
     * @throws MavenDownloadingException if there was a problem resolving the new requested dependencies.
     */
    public GradleDependencyConfiguration mapDependencies(
            Function<Dependency, @Nullable Dependency> mapping,
            List<MavenRepository> repositories,
            ExecutionContext ctx
    ) throws MavenDownloadingException {
        List<Dependency> newRequested = ListUtils.map(requested, mapping::apply);
        if(requested == newRequested) {
            return this;
        }

        GradleDependencyConfiguration result = withRequested(newRequested);
        if (isCanBeResolved) {
            try {
                MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                List<ResolvedDependency> newResolved = new ArrayList<>(newRequested.size());
                for(Dependency dep : newRequested) {
                    if(dep.findAttribute(ProjectAttribute.class).isPresent()) {
                        // Dependencies representing another project in the same multi-project build can't be resolved by mav be trivially resolvable
                        ResolvedDependency resolved = ResolvedDependency.builder()
                                .gav(dep.getGav().asResolved())
                                .requested(dep)
                                .classifier(dep.getClassifier())
                                .build();
                        newResolved.add(resolved);
                    } else {
                        Pom singlePom = singleDependencyPom(dep, newRequested, repositories, ctx);
                        ResolvedPom singleDependencyResolved = singlePom.resolve(emptyList(), mpd, ctx);
                        ResolvedDependency resolved = singleDependencyResolved.resolveDependencies(Scope.Compile, mpd, ctx).get(0);
                        newResolved.add(resolved);
                    }
                }
                result = result.withDirectResolved(newResolved);
            } catch (MavenDownloadingExceptions e) {
                throw e.getExceptions().get(0);
            }
        }

        return result;
    }

    /**
     * Combines the constraints applicable to this configuration with any BOMs found in the provided list of dependencies
     * to produce a list of managed dependencies which approximately represents all the relevant versions.
     *
     * @param maybeContainsBoms a list of dependencies. Those that are marked with an org.gradle.category indicating
     *                          they are to be treated as BOMs are considered and the rest ignored.
     */
    private List<ManagedDependency> managedFrom(List<Dependency> maybeContainsBoms) {
        List<GradleDependencyConstraint> allConstraints = getAllConstraints();
        List<ManagedDependency> managed = new ArrayList<>(allConstraints.size() + maybeContainsBoms.size());
        for (Dependency maybeBom : maybeContainsBoms) {
            maybeBom.findAttribute(Category.class).ifPresent(category -> {
                if(category == Category.REGULAR_PLATFORM || category == Category.ENFORCED_PLATFORM ) {
                    managed.add(new ManagedDependency.Imported(maybeBom.getGav()));
                }
            });
        }

        for (GradleDependencyConstraint constraint : allConstraints) {
            String version = null;
            if (StringUtils.isNotEmpty(constraint.getStrictVersion())) {
                version = constraint.getStrictVersion();
            } else if(StringUtils.isNotEmpty(constraint.getRequiredVersion())) {
                version = constraint.getRequiredVersion();
            } else if(StringUtils.isNotEmpty(constraint.getPreferredVersion())) {
                version = constraint.getPreferredVersion();
            }
            if(version == null) {
                continue;
            }
            managed.add(new ManagedDependency.Defined(
                    new GroupArtifactVersion(constraint.getGroupId(), constraint.getArtifactId(), version),
                    null,
                    null,
                    null,
                    null
            ));
        }

        return managed;
    }

    /**
     * Produce a
     */
    private Pom singleDependencyPom(Dependency requested, List<Dependency> maybeContainsBoms, List<MavenRepository> repositories, ExecutionContext ctx) {
        // Gradle Dependency tend to list their "scope" as the name of the gradle configuration they are listed in
        Dependency mavenCompatibleRequested = requested.withScope("compile");
        if (requested.findAttribute(Category.class).isPresent()) {
            mavenCompatibleRequested = mavenCompatibleRequested.withType("pom");
        }
        GroupArtifactVersion requestedGav = requested.getGav();
        List<Dependency> bomsOnly = ListUtils.filter(maybeContainsBoms, it -> it.findAttribute(Category.class).isPresent());
        return Pom.builder()
                .gav(requestedGav.asResolved()
                        .withGroupId("sdp-" + requestedGav.getGroupId())
                        .withArtifactId("sdp-" + requestedGav.getArtifactId())
                        // Only if all of these things are identical should this be retrieved from a cache
                        .withVersion(String.valueOf(Objects.hash(requested, getAllConstraints(), bomsOnly, repositories)))
                )
                .repositories(repositories)
                .dependencyManagement(managedFrom(bomsOnly))
                .dependencies(Collections.singletonList(mavenCompatibleRequested))
                .sourcePath(Paths.get("pom.xml"))
                .build();
    }
}
