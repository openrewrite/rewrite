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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

@Value
@With
public class GroupArtifactVersion implements Serializable {
    @Nullable
    String groupId;

    String artifactId;

    @Nullable
    String version;

    public GroupArtifactVersion(@Nullable String groupId, String artifactId, @Nullable String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public String toString() {
        return (groupId == null ? "" : groupId) + ':' + artifactId +
                (version == null ? "" : ":" + version);
    }

    public GroupArtifact asGroupArtifact() {
        return new GroupArtifact(groupId == null ? "" : groupId, artifactId);
    }

    /**
     * Cast to a ResolvedGroupArtifactVersion.
     * Usable when repository of resolution or dated snapshot version are irrelevant.
     */
    public ResolvedGroupArtifactVersion asResolved() {
        return new ResolvedGroupArtifactVersion(null, groupId == null ? "" : groupId, artifactId, version == null ? "" : version, null);
    }

    public GroupArtifactVersion withGroupArtifact(GroupArtifact ga) {
        if(Objects.equals(ga.getGroupId(), groupId) && Objects.equals(ga.getArtifactId(), artifactId)) {
            return this;
        }
        return new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), version);
    }
}
