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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Metadata;
import org.openrewrite.internal.lang.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class Pom implements Metadata {
    @Nullable
    String groupId;

    String artifactId;

    @Nullable
    String version;

    @Nullable
    String type;

    @Nullable
    String classifier;

    @Nullable
    Pom parent;

    Collection<Dependency> dependencies;
    DependencyManagement dependencyManagement;

    @With
    Collection<License> licenses;

    Collection<Repository> repositories;
    Map<String, String> properties;

    public Pom(@Nullable @JsonProperty("groupId") String groupId,
               @JsonProperty("artifactId") String artifactId,
               @Nullable @JsonProperty("version") String version,
               @Nullable @JsonProperty("type") String type,
               @Nullable @JsonProperty("classifier") String classifier,
               @Nullable @JsonProperty("parent") Pom parent,
               @JsonProperty("dependencies") Collection<Dependency> dependencies,
               @JsonProperty("dependencyManagement") DependencyManagement dependencyManagement,
               @JsonProperty("licenses") Collection<License> licenses,
               @JsonProperty("repositories") Collection<Repository> repositories,
               @JsonProperty("properties") Map<String, String> properties) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
        this.parent = parent;
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
        this.licenses = licenses;
        this.repositories = repositories;
        this.properties = properties;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Data
    public static class Repository {
        @EqualsAndHashCode.Include
        URL url;

        boolean releases;
        boolean snapshots;
    }

    @Nullable
    public String getProperty(@Nullable String property) {
        if (property == null) {
            return null;
        }

        String key = property.replace("${", "").replace("}", "");
        String value = properties.get(key);
        if (value == null) {
            return parent == null ? null : parent.getProperty(key);
        }
        return value;
    }

    public String getGroupId() {
        if (groupId == null) {
            if (parent == null) {
                throw new IllegalStateException("groupId must be defined");
            }
            return parent.getGroupId();
        }
        return groupId;
    }

    public DependencyManagement getEffectiveDependencyManagement() {
        if (parent == null) {
            return dependencyManagement;
        }
        return new DependencyManagement(Stream.concat(dependencyManagement.getDependencies().stream(),
                parent.getEffectiveDependencyManagement().getDependencies().stream()).collect(Collectors.toList())
        );
    }

    @Nullable
    public String getManagedVersion(String groupId, String artifactId) {
        DependencyManagement effectiveDependencyManagement = getEffectiveDependencyManagement();
        for (DependencyManagementDependency dep : effectiveDependencyManagement.getDependencies()) {
            for (DependencyDescriptor dependencyDescriptor : dep.getDependencies()) {
                if (groupId.equals(dependencyDescriptor.getGroupId()) && artifactId.equals(dependencyDescriptor.getArtifactId())) {
                    return dependencyDescriptor.getVersion();
                }
            }
        }
        return null;
    }

    /**
     * Cannot be inherited from a parent POM.
     */
    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        if (version == null) {
            if (parent == null) {
                throw new IllegalStateException("version must be defined");
            }
            return parent.getVersion();
        }
        return version;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getClassifier() {
        return classifier;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class License {
        String name;
        LicenseType type;

        public static Pom.License fromName(@Nullable String license) {
            if (license == null) {
                return new Pom.License("", Pom.LicenseType.Unknown);
            }

            // FIXME add others
            switch (license) {
                case "Eclipse Public License v2.0":
                    return new Pom.License(license, Pom.LicenseType.Eclipse2);
                case "Apache License, Version 2.0":
                    return new Pom.License(license, Pom.LicenseType.Apache2);
                case "GNU Lesser General Public License":
                    // example Lanterna
                    return new Pom.License(license, Pom.LicenseType.LGPL);
                default:
                    if (license.contains("LGPL")) {
                        // example Checkstyle
                        return new Pom.License(license, Pom.LicenseType.LGPL);
                    } else if (license.contains("GPL") || license.contains("GNU General Public License")) {
                        // example com.buschmais.jqassistant:jqassistant-maven-plugin
                        // example com.github.mtakaki:dropwizard-circuitbreaker
                        return new Pom.License(license, Pom.LicenseType.GPL);
                    }
                    return new Pom.License(license, Pom.LicenseType.Unknown);
            }
        }
    }

    public enum LicenseType {
        Apache2,
        Eclipse2,
        MIT,
        GPL,
        LGPL,
        Other,
        Unknown
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency implements DependencyDescriptor {
        Scope scope;

        @Nullable
        String classifier;

        boolean optional;
        Pom model;

        @Nullable
        String requestedVersion;

        Set<GroupArtifact> exclusions;

        @JsonIgnore
        public String getGroupId() {
            return model.getGroupId();
        }

        @JsonIgnore
        public String getArtifactId() {
            return model.getArtifactId();
        }

        @JsonIgnore
        public String getVersion() {
            return model.getVersion();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        Collection<DependencyManagementDependency> dependencies;
    }
}
