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

package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.errorprone.annotations.InlineMe;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Value
public class ResolvedGroupArtifactVersion implements Serializable {
    private static final LinkedHashMap<String, WeakReference<ResolvedGroupArtifactVersion>> CACHE = new LinkedHashMap<String, WeakReference<ResolvedGroupArtifactVersion>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10_000;
        }
    };

    @Nullable
    String repository;

    String groupId;
    String artifactId;
    String version;

    /**
     * In the form "${version}-${timestamp}-${buildNumber}", e.g. for the artifact rewrite-testing-frameworks-1.7.0-20210614.172805-1.jar,
     * the dated snapshot version is "1.7.0-20210614.172805-1".
     */
    @Nullable
    String datedSnapshotVersion;

    @InlineMe(
            replacement = "ResolvedGroupArtifactVersion.of(repository, groupId, artifactId, version, datedSnapshotVersion)",
            imports = "org.openrewrite.maven.tree.ResolvedGroupArtifactVersion")
    public ResolvedGroupArtifactVersion(@Nullable String repository, String groupId, String artifactId, String version, @Nullable String datedSnapshotVersion) {
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.datedSnapshotVersion = datedSnapshotVersion;
    }

    @JsonCreator
    public static ResolvedGroupArtifactVersion of(
            @Nullable String repository,
            String groupId,
            String artifactId,
            String version,
            @Nullable String datedSnapshotVersion) {
        String key = (repository == null ? "" : repository) + "::" + groupId + ":" + artifactId + ":" + version + ":" + (datedSnapshotVersion == null ? "" : datedSnapshotVersion);

        synchronized (CACHE) {
            WeakReference<ResolvedGroupArtifactVersion> ref = CACHE.get(key);
            ResolvedGroupArtifactVersion instance = ref != null ? ref.get() : null;

            if (instance == null) {
                instance = new ResolvedGroupArtifactVersion(repository, groupId, artifactId, version, datedSnapshotVersion);
                CACHE.put(key, new WeakReference<>(instance));
            }

            return instance;
        }
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + (datedSnapshotVersion == null ? version : datedSnapshotVersion);
    }

    public GroupArtifact asGroupArtifact() {
        return GroupArtifact.of(groupId, artifactId);
    }

    public GroupArtifactVersion asGroupArtifactVersion() {
        return GroupArtifactVersion.of(groupId, artifactId, version);
    }

    public ResolvedGroupArtifactVersion withRepository(@Nullable String repository) {
        return Objects.equals(repository, this.repository) ? this : of(repository, groupId, artifactId, version, datedSnapshotVersion);
    }

    public ResolvedGroupArtifactVersion withGroupId(String groupId) {
        return groupId.equals(this.groupId) ? this : of(repository, groupId, artifactId, version, datedSnapshotVersion);
    }

    public ResolvedGroupArtifactVersion withArtifactId(String artifactId) {
        return artifactId.equals(this.artifactId) ? this : of(repository, groupId, artifactId, version, datedSnapshotVersion);
    }

    public ResolvedGroupArtifactVersion withVersion(String version) {
        return version.equals(this.version) ? this : of(repository, groupId, artifactId, version, datedSnapshotVersion);
    }

    public ResolvedGroupArtifactVersion withDatedSnapshotVersion(@Nullable String datedSnapshotVersion) {
        return Objects.equals(datedSnapshotVersion, this.datedSnapshotVersion) ? this : of(repository, groupId, artifactId, version, datedSnapshotVersion);
    }

    public ResolvedGroupArtifactVersion withGroupArtifact(GroupArtifact ga) {
        if (Objects.equals(ga.getGroupId(), groupId) && Objects.equals(ga.getArtifactId(), artifactId)) {
            return this;
        }
        return ResolvedGroupArtifactVersion.of(repository, ga.getGroupId(), ga.getArtifactId(), version, datedSnapshotVersion);
    }
}
