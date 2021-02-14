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
package org.openrewrite.maven.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

import static java.util.Collections.emptyList;

/**
 * A value object deserialized directly from POM XML
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@XmlRootElement(name = "project")
public class RawPom {
    @Nullable
    Parent parent;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String groupId;

    @EqualsAndHashCode.Include
    @ToString.Include
    String artifactId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String version;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    String snapshotVersion;

    @Nullable
    String packaging;

    @Nullable
    Dependencies dependencies;

    @Nullable
    DependencyManagement dependencyManagement;

    @Nullable
    Map<String, String> properties;

    @Nullable
    RawRepositories repositories;

    @Nullable
    Licenses licenses;

    @Nullable
    Profiles profiles;

    public Map<String, String> getActiveProperties(Iterable<String> activeProfiles) {
        Map<String, String> activeProperties = new HashMap<>();

        if (properties != null) {
            activeProperties.putAll(properties);
        }

        if (profiles != null) {
            for (RawPom.Profile profile : getInnerProfiles()) {
                if (profile.isActive(activeProfiles) && profile.getProperties() != null) {
                    activeProperties.putAll(profile.getProperties());
                }
            }
        }

        return activeProperties;
    }

    public Collection<Dependency> getActiveDependencyManagementDependencies(Iterable<String> activeProfiles) {
        Collection<Dependency> activeDependencies = new ArrayList<>();

        if (dependencyManagement != null && dependencyManagement.dependencies != null) {
            activeDependencies.addAll(dependencyManagement.dependencies.getDependencies());
        }

        if (profiles != null) {
            for (RawPom.Profile profile : getInnerProfiles()) {
                if (profile.isActive(activeProfiles) && profile.getDependencyManagement() != null &&
                        profile.getDependencyManagement().dependencies != null) {
                    activeDependencies.addAll(profile.getDependencyManagement().dependencies.getDependencies());
                }
            }
        }

        return activeDependencies;
    }

    public List<Dependency> getActiveDependencies(Collection<String> activeProfiles) {
        List<Dependency> activeDependencies = new ArrayList<>();

        if (dependencies != null) {
            activeDependencies.addAll(dependencies.getDependencies());
        }

        if (profiles != null) {
            for (Profile profile : getInnerProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    if (profile.dependencies != null) {
                        activeDependencies.addAll(profile.dependencies.dependencies);
                    }
                }
            }
        }

        return activeDependencies;
    }

    public List<RawRepositories.Repository> getActiveRepositories(Collection<String> activeProfiles) {
        List<RawRepositories.Repository> activeRepositories = new ArrayList<>();

        if (repositories != null) {
            activeRepositories.addAll(repositories.getRepositories());
        }

        if (profiles != null) {
            for (Profile profile : getInnerProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    if (profile.repositories != null) {
                        activeRepositories.addAll(profile.repositories.getRepositories());
                    }
                }
            }
        }

        return activeRepositories;
    }

    public List<License> getInnerLicenses() {
        return licenses == null ? emptyList() : licenses.getLicenses();
    }

    public List<Profile> getInnerProfiles() {
        return profiles == null ? emptyList() : profiles.getProfiles();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        String groupId;
        String artifactId;

        @Nullable
        String version;

        @Nullable
        String scope;

        @Nullable
        String type;

        @Nullable
        String classifier;

        @Nullable
        Boolean optional;

        @Nullable
        @JacksonXmlElementWrapper(useWrapping = true)
        Set<GroupArtifact> exclusions;
    }

    @Getter
    public static class DependencyManagement {
        @Nullable
        private final Dependencies dependencies;

        public DependencyManagement() {
            this.dependencies = null;
        }

        public DependencyManagement(@JsonProperty("dependencies") @Nullable Dependencies dependencies) {
            this.dependencies = dependencies;
        }
    }

    @Getter
    public static class Dependencies {
        private final List<Dependency> dependencies;

        public Dependencies() {
            this.dependencies = emptyList();
        }

        public Dependencies(@JacksonXmlProperty(localName = "dependency") List<Dependency> dependencies) {
            this.dependencies = dependencies;
        }
    }

    @Getter
    public static class Licenses {
        private final List<License> licenses;

        public Licenses() {
            this.licenses = emptyList();
        }

        public Licenses(@JacksonXmlProperty(localName = "license") List<License> licenses) {
            this.licenses = licenses;
        }
    }

    @Getter
    public static class Profiles {
        private final List<Profile> profiles;

        public Profiles() {
            this.profiles = emptyList();
        }

        public Profiles(@JacksonXmlProperty(localName = "profile") List<Profile> profiles) {
            this.profiles = profiles;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Parent {
        String groupId;
        String artifactId;
        String version;

        @Nullable
        String relativePath;
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    @Getter
    public static class License {
        String name;

        public License() {
            this.name = "";
        }

        public License(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        String id;

        @Nullable
        ProfileActivation activation;

        @Nullable
        Map<String, String> properties;

        @Nullable
        Dependencies dependencies;

        @Nullable
        DependencyManagement dependencyManagement;

        @Nullable
        RawRepositories repositories;

        public boolean isActive(Iterable<String> activeProfiles) {
            return ProfileActivation.isActive(id, activeProfiles, activation);
        }
    }

    @Nullable
    public String getGroupId() {
        return groupId == null && parent != null ? parent.getGroupId() : groupId;
    }

    @Nullable
    public String getVersion() {
        return version == null && parent != null ? parent.getVersion() : version;
    }
}
