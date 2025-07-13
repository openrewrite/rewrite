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
package org.openrewrite.gradle.internal;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;

@Value
@With
@EqualsAndHashCode
public class Dependency {
    @Nullable
    String groupId;

    String artifactId;

    @Nullable
    String version;

    @Nullable
    String classifier;

    @Nullable
    String ext;

    public GroupArtifactVersion getGav() {
        return new GroupArtifactVersion(groupId, artifactId, version);
    }

    public String toStringNotation() {
        StringBuilder sb = new StringBuilder();
        //Build against spec from gradle docs, all options are optional apart from name
        //configurationName "group:name:version:classifier@extension"
        if (groupId != null) {
            sb.append(groupId);
        }
        sb.append(":").append(artifactId);

        if (version != null) {
            sb.append(":").append(version);
        } else if (classifier != null) {
            sb.append(":");
        }

        if (classifier != null) {
            sb.append(":").append(classifier);
        }

        if (ext != null) {
            sb.append("@").append(ext);
        }

        return sb.toString();
    }
}
