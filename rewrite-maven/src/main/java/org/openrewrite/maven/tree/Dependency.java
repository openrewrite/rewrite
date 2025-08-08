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
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@With
public class Dependency implements Serializable {
    GroupArtifactVersion gav;

    @Nullable
    String classifier;

    @Nullable
    String type;

    @With
    @Nullable
    String scope;

    @Builder.Default
    @Nullable
    List<GroupArtifact> exclusions = emptyList();

    @Nullable
    String optional;

    @Builder.Default
    Set<DependencyAttribute> attributes = emptySet();

    /**
     * Lookup an attribute by its class. There should only ever be one attribute of a given type in the set, although
     * this is enforced only by convention.
     */
    <T extends DependencyAttribute> Optional<T> findAttribute(Class<T> clazz) {
        return attributes.stream()
                .filter(clazz::isInstance)
                .findAny()
                .map(clazz::cast);
    }

    public @Nullable String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public @Nullable String getVersion() {
        return gav.getVersion();
    }

    @Override
    public String toString() {
        return gav.toString();
    }
}
