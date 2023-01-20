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

import lombok.*;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Dependency implements Serializable {
    GroupArtifactVersion gav;

    @With
    @Nullable
    String classifier;

    @With
    @Nullable
    String type;

    @With
    String scope;

    @With
    List<GroupArtifact> exclusions;

    @With
    @Nullable
    String optional;

    @Nullable
    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    @Nullable
    public String getVersion() {
        return gav.getVersion();
    }

    public Dependency withGav(GroupArtifactVersion gav) {
        if(gav == this.gav) {
            return this;
        }
        return new Dependency(gav, classifier, type, scope, exclusions, optional);
    }

    @Override
    public String toString() {
        return gav.toString();
    }
}
