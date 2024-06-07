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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Dependency management sections contain a combination of single dependency definitions and imports of
 * BOMs and their dependency management sections/properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface ManagedDependency {
    @Value
    @With
    class Defined implements ManagedDependency {
        GroupArtifactVersion gav;

        @Nullable
        String scope;

        @Nullable
        String type;

        @Nullable
        String classifier;

        List<GroupArtifact> exclusions;

        @Override
        public String getGroupId() {
            return requireNonNull(gav.getGroupId());
        }

        @Override
        public String getArtifactId() {
            return gav.getArtifactId();
        }

        @Nullable
        @Override
        public String getVersion() {
            return gav.getVersion();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Defined withVersion(String version) {
            return withGav(gav.withVersion(version));
        }
    }

    @Value
    @With
    class Imported implements ManagedDependency {
        GroupArtifactVersion gav;

        @Override
        public String getGroupId() {
            return requireNonNull(gav.getGroupId());
        }

        @Override
        public String getArtifactId() {
            return gav.getArtifactId();
        }

        @Nullable
        @Override
        public String getVersion() {
            return gav.getVersion();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Imported withVersion(String version) {
            return withGav(gav.withVersion(version));
        }
    }

    String getGroupId();

    String getArtifactId();

    @Nullable
    String getVersion();

    <D extends ManagedDependency> D withVersion(String version);
}
