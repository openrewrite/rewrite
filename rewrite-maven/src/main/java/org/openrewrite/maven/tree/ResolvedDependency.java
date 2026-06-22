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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@Value
@With
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class ResolvedDependency implements Serializable {

    /**
     * This will be {@code null} when this is a project dependency.
     */
    @Nullable
    MavenRepository repository;

    ResolvedGroupArtifactVersion gav;

    Dependency requested;

    /**
     * Direct dependencies only that survived conflict resolution and exclusion.
     */
    @NonFinal
    @EqualsAndHashCode.Exclude
    @Builder.Default
    List<ResolvedDependency> dependencies = emptyList();

    @Builder.Default
    List<License> licenses = emptyList();

    @Nullable
    String type;

    @Nullable
    String classifier;

    @Nullable
    Boolean optional;

    int depth;

    @Nullable
    @NonFinal
    List<GroupArtifact> effectiveExclusions;

    public List<GroupArtifact> getEffectiveExclusions() {
        return effectiveExclusions == null ? emptyList() : effectiveExclusions;
    }

    /**
     * Only used by dependency resolution to avoid unnecessary empty list allocations for leaf dependencies.
     * @param dependencies A dependency list
     */
    void unsafeSetDependencies(List<ResolvedDependency> dependencies) {
        this.dependencies = dependencies;
    }

    void unsafeSetEffectiveExclusions(List<GroupArtifact> effectiveExclusions) {
        this.effectiveExclusions = effectiveExclusions;
    }

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    public String getType() {
        return type == null ? "jar" : type;
    }

    public boolean isDirect() {
        return depth == 0;
    }

    @SuppressWarnings("unused")
    public boolean isTransitive() {
        return depth != 0;
    }

    public @Nullable String getDatedSnapshotVersion() {
        return gav.getDatedSnapshotVersion();
    }

    public List<ResolvedDependency> findDependencies(String groupId, String artifactId) {
        List<ResolvedDependency> found = new ArrayList<>();
        findDependencies0(groupId, artifactId, isExact(groupId), isExact(artifactId), null, found);
        return found;
    }

    private void findDependencies0(@Nullable String groupId, @Nullable String artifactId, boolean groupExact,
                                   boolean artifactExact, @Nullable Set<ResolvedDependency> visited,
                                   List<ResolvedDependency> found) {
        if (matches(getGroupId(), groupId, groupExact) && matches(getArtifactId(), artifactId, artifactExact)) {
            found.add(this);
        } else {
            if (visited == null) {
                visited = newSetFromMap(new IdentityHashMap<>());
            }
            if (!visited.add(this)) {
                return;
            }
        }
        List<ResolvedDependency> dependencies = this.dependencies;
        if (dependencies.isEmpty()) {
            return;
        }
        if (visited == null) {
            // Reached only when this dependency matched (and so was not added above); allocate for the recursion.
            visited = newSetFromMap(new IdentityHashMap<>());
        }
        List<GroupArtifact> exclusions = requested == null ? null : requested.getExclusions();
        boolean hasExclusions = exclusions != null && !exclusions.isEmpty();
        for (int i = 0, size = dependencies.size(); i < size; i++) {
            ResolvedDependency dependency = dependencies.get(i);
            if (hasExclusions) {
                int start = found.size();
                dependency.findDependencies0(groupId, artifactId, groupExact, artifactExact, visited, found);
                for (int j = found.size() - 1; j >= start; j--) {
                    if (isExcluded(found.get(j), exclusions)) {
                        found.remove(j);
                    }
                }
            } else {
                dependency.findDependencies0(groupId, artifactId, groupExact, artifactExact, visited, found);
            }
        }
    }

    public @Nullable ResolvedDependency findDependency(@Nullable String groupId, @Nullable String artifactId) {
        return findDependency0(groupId, artifactId, isExact(groupId), isExact(artifactId), null);
    }

    private @Nullable ResolvedDependency findDependency0(@Nullable String groupId, @Nullable String artifactId,
                                                         boolean groupExact, boolean artifactExact,
                                                         @Nullable Set<ResolvedDependency> visited) {
        if (matches(getGroupId(), groupId, groupExact) && matches(getArtifactId(), artifactId, artifactExact)) {
            return this;
        }
        List<ResolvedDependency> dependencies = this.dependencies;
        if (dependencies.isEmpty()) {
            return null;
        }
        if (visited == null) {
            visited = newSetFromMap(new IdentityHashMap<>());
        }
        if (!visited.add(this)) {
            return null;
        }
        List<GroupArtifact> exclusions = requested == null ? null : requested.getExclusions();
        for (int i = 0, size = dependencies.size(); i < size; i++) {
            ResolvedDependency found = dependencies.get(i).findDependency0(groupId, artifactId, groupExact, artifactExact, visited);
            if (found != null && !isExcluded(found, exclusions)) {
                return found;
            }
        }
        return null;
    }

    /**
     * @param pattern a groupId or artifactId glob pattern
     * @return {@code true} when {@code pattern} contains no glob metacharacters and so can be compared with a
     * case-insensitive {@link String#equalsIgnoreCase} rather than the more expensive
     * {@link org.openrewrite.internal.StringUtils#matchesGlob}.
     * {@code null} (matches everything) and patterns containing {@code *}/{@code ?} return {@code false}.
     */
    private static boolean isExact(@Nullable String pattern) {
        return pattern != null && pattern.indexOf('*') < 0 && pattern.indexOf('?') < 0;
    }

    private static boolean matches(String value, @Nullable String pattern, boolean exact) {
        return exact ? value.equalsIgnoreCase(pattern) : matchesGlob(value, pattern);
    }

    private static boolean isExcluded(ResolvedDependency found, @Nullable List<GroupArtifact> exclusions) {
        if (exclusions != null) {
            for (int i = 0, size = exclusions.size(); i < size; i++) {
                GroupArtifact exclusion = exclusions.get(i);
                if (matchesGlob(found.getGroupId(), exclusion.getGroupId()) &&
                        matchesGlob(found.getArtifactId(), exclusion.getArtifactId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return (repository == null ? "" : repository.getUri() + "/") +
                gav.getGroupId().replace('.', '/') + "/" +
                gav.getArtifactId() + "/" + gav.getVersion() + "/" +
                gav.getArtifactId() + "-" +
                gav.getVersion() + ".pom";
    }
}
