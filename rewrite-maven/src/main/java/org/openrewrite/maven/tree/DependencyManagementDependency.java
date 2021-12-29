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
import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

/**
 * Dependency management sections contain a combination of single dependency definitions and imports of
 * BOMs and their dependency management sections/properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface DependencyManagementDependency {
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    class Defined implements DependencyManagementDependency,
            DependencyDescriptor {
        String groupId;
        String artifactId;

        @Nullable
        @With
        String version;

        String requestedVersion;

        @Nullable
        Scope scope;

        @Nullable
        String classifier;

        Set<GroupArtifact> exclusions;

        @Override
        public List<DependencyDescriptor> getDependencies() {
            return Collections.singletonList(this);
        }

        @Override
        public Map<String, String> getProperties() {
            return emptyMap();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    class Imported implements DependencyManagementDependency {
        String groupId;
        String artifactId;

        @With
        String version;

        String requestedVersion;
        Pom maven;

        @Override
        public List<DependencyDescriptor> getDependencies() {
            return maven.getEffectiveDependencyManagement().getDependencies().stream()
                    .flatMap(dep -> dep.getDependencies().stream())
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getProperties() {
            // FIXME should be active properties by profile as well? also parent properties?
            return maven.getProperties();
        }

        public boolean deepEquals(@Nullable Object other) {
            if (other instanceof Imported) {
                Imported i = (Imported) other;
                return this == other || (
                        Objects.equals(this.groupId, i.groupId)
                                && Objects.equals(this.artifactId, i.artifactId)
                                && Objects.equals(this.version, i.version)
                                && Objects.equals(this.requestedVersion, i.requestedVersion)
                                && (this.maven == i.maven || (this.maven != null && this.maven.deepEquals(i.maven))
                        )
                );
            }
            return false;
        }
    }

    /**
     * @return A list of managed dependencies in order of precedence.
     */
    List<DependencyDescriptor> getDependencies();

    /**
     * @return A map of properties inherited from import-scope BOMs defined as
     * dependencyManagement dependencies.
     */
    Map<String, String> getProperties();

    String getGroupId();

    String getArtifactId();

    String getVersion();

    <D extends DependencyManagementDependency> D withVersion(String version);

    String getRequestedVersion();
}
