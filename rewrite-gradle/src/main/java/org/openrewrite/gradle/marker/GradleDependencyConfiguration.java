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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Version;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class GradleDependencyConfiguration implements Serializable {
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
     *  Indicates that this configuration is intended for exposing artifacts outside this project. A consumable configuration should not be declarable or resolvable.
     *  See <a href="https://docs.gradle.org/current/userguide/declaring_configurations.html#sec:configuration-flags-roles">Configuration flag roles</a>
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
     */
    List<GradleDependencyConstraint> constraints;

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

    public @Nullable Dependency findRequestedDependency(String groupId, String artifactId) {
        for (Dependency d : requested) {
            if (StringUtils.matchesGlob(d.getGav().getGroupId(), groupId) &&
                StringUtils.matchesGlob(d.getGav().getArtifactId(), artifactId)) {
                return d;
            }
        }
        return null;
    }

    public @Nullable ResolvedDependency findResolvedDependency(String groupId, String artifactId) {
        for (ResolvedDependency d : directResolved) {
            ResolvedDependency dependency = d.findDependency(groupId, artifactId);
            if (dependency != null) {
                return dependency;
            }
        }
        return null;
    }

    public void unsafeSetExtendsFrom(List<GradleDependencyConfiguration> extendsFrom) {
        this.extendsFrom = extendsFrom;
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
}
