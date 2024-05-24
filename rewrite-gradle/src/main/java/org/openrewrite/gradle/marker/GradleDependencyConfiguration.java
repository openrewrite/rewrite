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

import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
@Value
@With
public class GradleDependencyConfiguration implements Serializable {
    /**
     * The name of the dependency configuration. Unique within a given project.
     */
    String name;

    @Nullable
    String description;

    boolean isTransitive;

    boolean isCanBeResolved;

    boolean isCanBeConsumed;

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
        Set<ResolvedDependency> alreadyResolved = new HashSet<>();
        return resolveTransitiveDependencies(resolved, alreadyResolved);
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

    @Nullable
    public Dependency findRequestedDependency(String groupId, String artifactId) {
        for (Dependency d : requested) {
            if (StringUtils.matchesGlob(d.getGav().getGroupId(), groupId) &&
                StringUtils.matchesGlob(d.getGav().getArtifactId(), artifactId)) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    public ResolvedDependency findResolvedDependency(String groupId, String artifactId) {
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

    private static List<ResolvedDependency> resolveTransitiveDependencies(List<ResolvedDependency> resolved, Set<ResolvedDependency> alreadyResolved) {
        for (ResolvedDependency dependency : resolved) {
            if (alreadyResolved.add(dependency)) {
                alreadyResolved.addAll(resolveTransitiveDependencies(dependency.getDependencies(), alreadyResolved));
            }
        }
        return new ArrayList<>(alreadyResolved);
    }
}
