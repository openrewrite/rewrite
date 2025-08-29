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

@Value
public class GroupArtifact implements Serializable {
    private static final LinkedHashMap<String, WeakReference<GroupArtifact>> CACHE = new LinkedHashMap<String, WeakReference<GroupArtifact>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10_000;
        }
    };

    String groupId;
    String artifactId;

    /**
     * Static factory method of() should be used instead. This is temporarily still public for minimally-disruptive deprecation.
     */
    @Deprecated
    @InlineMe(replacement = "GroupArtifact.of(groupId, artifactId)", imports = "org.openrewrite.maven.tree.GroupArtifact")
    public GroupArtifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    @JsonCreator
    public static GroupArtifact of(
            @Nullable String groupId,
            String artifactId) {
        String finalGroup = groupId == null ? "" : groupId;
        String key = finalGroup + ":" + artifactId;

        synchronized (CACHE) {
            WeakReference<GroupArtifact> ref = CACHE.get(key);
            GroupArtifact instance = ref != null ? ref.get() : null;

            if (instance == null) {
                instance = new GroupArtifact(finalGroup, artifactId);
                CACHE.put(key, new WeakReference<>(instance));
            }

            return instance;
        }
    }

    public GroupArtifact withGroupId(String groupId) {
        return groupId.equals(this.groupId) ? this : of(groupId, artifactId);
    }

    public GroupArtifact withArtifactId(String artifactId) {
        return artifactId.equals(this.artifactId) ? this : of(groupId, artifactId);
    }
}
