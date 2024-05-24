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
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.Objects;

@Value
@With
public class ResolvedManagedDependency {
    GroupArtifactVersion gav;

    Scope scope;

    @Nullable
    String type;

    @Nullable
    String classifier;

    List<GroupArtifact> exclusions;

    ManagedDependency requested;

    @Nullable
    ManagedDependency requestedBom;

    @Nullable
    ResolvedGroupArtifactVersion bomGav;

    public String getType() {
        return type == null ? "jar" : type;
    }

    public String getGroupId() {
        assert gav.getGroupId() != null;
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    /**
     * Return the version of the managed dependency, this can be null if a managed dependency is used
     * to exclude a transitive dependency vs managed a version of a group/artifact
     *
     * @return the version of the dependency
     */
    @Nullable
    public String getVersion() {
        return gav.getVersion();
    }

    public boolean matches(@Nullable String groupId, String artifactId,
                           @Nullable String type, @Nullable String classifier) {
        return Objects.equals(groupId, gav.getGroupId()) && artifactId.equals(gav.getArtifactId()) &&
                (type == null ? "jar" : type).equals(this.type == null ? "jar" : this.type) &&
                Objects.equals(classifier, this.classifier);
    }
}
