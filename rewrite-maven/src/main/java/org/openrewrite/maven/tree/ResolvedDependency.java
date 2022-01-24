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

import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@With
public class ResolvedDependency {
    MavenRepository repository;

    ResolvedGroupArtifactVersion gav;

    Dependency requested;

    /**
     * Direct dependencies only that survived conflict resolution and exclusion.
     */
    @NonFinal
    List<ResolvedDependency> dependencies;

    List<License> licenses;

    /**
     * Only used by dependency resolution to avoid unnecessary empty list allocations for leaf dependencies.
     * @param dependencies A dependency list
     */
    void unsafeSetDependencies(List<ResolvedDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public ResolvedGroupArtifactVersion getGav() {
        return gav;
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
        return requested.getType() == null ? "jar" : requested.getType();
    }

    @Nullable
    public String getClassifier() {
        return requested.getClassifier();
    }

    public boolean isOptional() {
        return requested.isOptional();
    }

    @Nullable
    public String getDatedSnapshotVersion() {
        return gav.getDatedSnapshotVersion();
    }

    @Nullable
    public ResolvedDependency findDependency(String groupId, String artifactId) {
        if (matchesGlob(getGroupId(), groupId) && matchesGlob(getArtifactId(), artifactId)) {
            return this;
        }
        for (ResolvedDependency dependency : dependencies) {
            ResolvedDependency found = findDependency(groupId, artifactId);
            if (found != null) {
                return found;
            }
        }
        return null;
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
