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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A value object deserialized directly from POM XML
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RawPom {
    @Getter
    @Nullable
    Parent parent;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String groupId;

    @Getter
    @EqualsAndHashCode.Include
    @ToString.Include
    String artifactId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String version;

    @Getter
    @Nullable
    String packaging;

    @Nullable
    Dependencies dependencies;

    @Getter
    @Nullable
    DependencyManagement dependencyManagement;

    @Getter
    @Nullable
    Map<String, String> properties;

    @Nullable
    Repositories repositories;

    @Nullable
    Licenses licenses;

    @Nullable
    Profiles profiles;

    @JsonCreator
    public RawPom(@JsonProperty("parent") @Nullable Parent parent,
                  @JsonProperty("groupId") @Nullable String groupId,
                  @JsonProperty("artifactId") String artifactId,
                  @JsonProperty("version") @Nullable String version,
                  @JsonProperty("packaging") @Nullable String packaging,
                  @JsonProperty("dependencies") @Nullable Dependencies dependencies,
                  @JsonProperty("dependencyManagement") @Nullable DependencyManagement dependencyManagement,
                  @JsonProperty("properties") @Nullable Map<String, String> properties,
                  @JsonProperty("repositories") @Nullable Repositories repositories,
                  @JsonProperty("licenses") @Nullable Licenses licenses,
                  @JsonProperty("profiles") @Nullable Profiles profiles) {
        this.parent = parent;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
        this.properties = properties;
        this.repositories = repositories;
        this.licenses = licenses;
        this.profiles = profiles;
    }

    @JsonIgnore
    public Map<String, String> getActiveProperties() {
        Map<String, String> activeProperties = new HashMap<>();

        if (properties != null) {
            activeProperties.putAll(properties);
        }

        if (profiles != null) {
            for (RawPom.Profile profile : getProfiles()) {
                if (profile.isActive() && profile.getProperties() != null) {
                    activeProperties.putAll(profile.getProperties());
                }
            }
        }

        return activeProperties;
    }

    @JsonIgnore
    public List<Dependency> getActiveDependencies() {
        List<Dependency> activeDependencies = new ArrayList<>();

        if (dependencies != null) {
            activeDependencies.addAll(dependencies.getDependencies());
        }

        if (profiles != null) {
            for (Profile profile : getProfiles()) {
                if (profile.isActive()) {
                    if (profile.dependencies != null) {
                        activeDependencies.addAll(profile.dependencies);
                    }
                }
            }
        }

        return activeDependencies;
    }

    @JsonIgnore
    public List<Repository> getRepositories() {
        return repositories == null ? emptyList() : repositories.getRepositories();
    }

    @JsonIgnore
    public List<License> getLicenses() {
        return licenses == null ? emptyList() : licenses.getLicenses();
    }

    @JsonIgnore
    public List<Profile> getProfiles() {
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
        Set<GroupArtifact> exclusions;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class DependencyManagement {
        @Nullable
        Dependencies dependencies;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Dependencies {
        @JacksonXmlProperty(localName = "dependency")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Dependency> dependencies = emptyList();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Repositories {
        @JacksonXmlProperty(localName = "repository")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Repository> repositories = emptyList();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Licenses {
        @JacksonXmlProperty(localName = "license")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<License> licenses = emptyList();
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class Profiles {
        @JacksonXmlProperty(localName = "profile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Profile> profiles = emptyList();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Repository {
        @With
        String url;

        @Nullable
        ArtifactPolicy releases;

        @Nullable
        ArtifactPolicy snapshots;

        public boolean acceptsVersion(String version) {
            if (version.endsWith("-SNAPSHOT")) {
                return snapshots != null && snapshots.isEnabled();
            } else if (url.equals("https://repo.spring.io/milestone")) {
                // special case this repository since it will be so commonly used
                return version.matches(".*(M|RC)\\d+$");
            }
            return releases != null && releases.isEnabled();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    @Getter
    public static class ArtifactPolicy {
        boolean enabled;

        @JsonCreator
        public ArtifactPolicy(@JsonProperty("enabled") @Nullable Boolean enabled) {
            this.enabled = enabled == null || enabled;
        }

        /**
         * Used by Jackson in the event there is an empty tag in the POM.
         */
        @SuppressWarnings("unused")
        public ArtifactPolicy() {
            this(true);
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

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class License {
        String name;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        ProfileActivation activation;

        @Nullable
        Map<String, String> properties;

        @Nullable
        List<Dependency> dependencies;

        @JsonIgnore
        public boolean isActive() {
            return activation != null && activation.isActive();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ProfileActivation {
        @Nullable
        String jdk;

        @Nullable
        Map<String, String> property;

        @JsonIgnore
        public boolean isActive() {
            return isActiveByJdk() || isActiveByProperty();
        }

        @JsonIgnore
        private boolean isActiveByJdk() {
            if (jdk == null) {
                return false;
            }

            String version = System.getProperty("java.version");
            RequestedVersion requestedVersion = new RequestedVersion(new GroupArtifact("", ""),
                    null, jdk);

            if (requestedVersion.isDynamic() || requestedVersion.isRange()) {
                return requestedVersion.selectFrom(singletonList(version)) != null;
            }

            //noinspection ConstantConditions
            return version.startsWith(requestedVersion.nearestVersion());
        }

        @JsonIgnore
        private boolean isActiveByProperty() {
            if (property == null || property.isEmpty()) {
                return false;
            }

            for (Map.Entry<String, String> prop : property.entrySet()) {
                if (!prop.getValue().equals(System.getenv(prop.getKey()))) {
                    return false;
                }
            }

            return true;
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
