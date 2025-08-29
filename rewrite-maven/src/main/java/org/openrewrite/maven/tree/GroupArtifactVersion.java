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
public class GroupArtifactVersion implements Serializable {
    private static final LinkedHashMap<String, WeakReference<GroupArtifactVersion>> CACHE = new LinkedHashMap<String, WeakReference<GroupArtifactVersion>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10_000;
        }
    };

    @Nullable
    String groupId;

    String artifactId;

    @Nullable
    String version;

    @InlineMe(replacement = "GroupArtifactVersion.of(groupId, artifactId, version)", imports = "org.openrewrite.maven.tree.GroupArtifactVersion")
    public GroupArtifactVersion(@Nullable String groupId, String artifactId, @Nullable String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @JsonCreator
    public static GroupArtifactVersion of(
            @Nullable String groupId,
            String artifactId,
            @Nullable String version) {
        String key = (groupId == null ? "" : groupId) + ":" + artifactId + ":" + (version == null ? "" : version);

        synchronized (CACHE) {
            WeakReference<GroupArtifactVersion> ref = CACHE.get(key);
            GroupArtifactVersion instance = ref != null ? ref.get() : null;

            if (instance == null) {
                instance = new GroupArtifactVersion(groupId, artifactId, version);
                CACHE.put(key, new WeakReference<>(instance));
            }

            return instance;
        }
    }

    @Override
    public String toString() {
        return (groupId == null ? "" : groupId) + ':' + artifactId +
                (version == null ? "" : ":" + version);
    }

    public GroupArtifact asGroupArtifact() {
        return GroupArtifact.of(groupId, artifactId);
    }

    /**
     * Cast to a ResolvedGroupArtifactVersion.
     * Usable when repository of resolution or dated snapshot version are irrelevant.
     */
    public ResolvedGroupArtifactVersion asResolved() {
        return ResolvedGroupArtifactVersion.of(null, groupId == null ? "" : groupId, artifactId, version == null ? "" : version, null);
    }

    public GroupArtifactVersion withGroupId(@Nullable String groupId) {
        return Objects.equals(groupId, this.groupId) ? this : of(groupId, artifactId, version);
    }

    public GroupArtifactVersion withArtifactId(String artifactId) {
        return Objects.equals(artifactId, this.artifactId) ? this : of(groupId, artifactId, version);
    }

    public GroupArtifactVersion withVersion(@Nullable String version) {
        return Objects.equals(version, this.version) ? this : of(groupId, artifactId, version);
    }

    public GroupArtifactVersion withGroupArtifact(GroupArtifact ga) {
        return GroupArtifactVersion.of(ga.getGroupId(), ga.getArtifactId(), version);
    }
}
