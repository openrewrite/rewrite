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
import org.jetbrains.annotations.NotNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

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
    String name;

    @Nullable
    String description;

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

    public static RawPom parse(InputStream inputStream, @Nullable String snapshotVersion) {
        try {
            RawPom pom = MavenXmlMapper.readMapper().readValue(inputStream, RawPom.class);
            if (snapshotVersion != null) {
                pom.setSnapshotVersion(snapshotVersion);
            }
            return pom;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse pom", e);
        }
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
        @JacksonXmlElementWrapper
        List<GroupArtifact> exclusions;
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
    }

    @Nullable
    public String getGroupId() {
        return groupId == null && parent != null ? parent.getGroupId() : groupId;
    }

    @Nullable
    public String getVersion() {
        return version == null && parent != null ? parent.getVersion() : version;
    }

    public Pom toPom(@Nullable Path inputPath, @Nullable MavenRepository repo) {
        org.openrewrite.maven.tree.Parent parent = getParent() == null ? null : new org.openrewrite.maven.tree.Parent(new GroupArtifactVersion(
                getParent().getGroupId(), getParent().getArtifactId(),
                getParent().getVersion()), getParent().getRelativePath());

        return new Pom(
                inputPath,
                repo,
                parent,
                new ResolvedGroupArtifactVersion(repo == null ? null : repo.getUri(), groupId, artifactId, version, null),
                name,
                getPackaging(),
                getProperties() == null ? emptyMap() : getProperties(),
                mapDependencyManagement(getDependencyManagement()),
                mapRequestedDependencies(getDependencies()),
                mapRepositories(getRepositories()),
                mapLicenses(getLicenses()),
                mapProfiles(getProfiles())
        );
    }

    private List<org.openrewrite.maven.tree.License> mapLicenses(@Nullable Licenses rawLicenses) {
        List<org.openrewrite.maven.tree.License> licenses = emptyList();
        if (rawLicenses != null) {
            List<License> unmappedLicenses = rawLicenses.getLicenses();
            if (unmappedLicenses != null) {
                licenses = new ArrayList<>(unmappedLicenses.size());
                for (License l : unmappedLicenses) {
                    licenses.add(org.openrewrite.maven.tree.License.fromName(l.getName()));
                }
            }
        }
        return licenses;
    }

    private List<org.openrewrite.maven.tree.Profile> mapProfiles(@Nullable Profiles rawProfiles) {
        List<org.openrewrite.maven.tree.Profile> profiles = emptyList();
        if (rawProfiles != null) {
            List<Profile> unmappedProfiles = rawProfiles.getProfiles();
            if (unmappedProfiles != null) {
                profiles = new ArrayList<>(unmappedProfiles.size());

                // profiles are mapped in reverse order to put them in precedence order left to right
                for (int i = unmappedProfiles.size() - 1; i >= 0; i--) {
                    Profile p = unmappedProfiles.get(i);
                    profiles.add(new org.openrewrite.maven.tree.Profile(
                            p.getId(),
                            p.getActivation(),
                            p.getProperties() == null ? emptyMap() : p.getProperties(),
                            mapRequestedDependencies(p.getDependencies()),
                            mapDependencyManagement(p.getDependencyManagement()),
                            mapRepositories(p.getRepositories())
                    ));
                }

            }
        }
        return profiles;
    }

    @NotNull
    private List<MavenRepository> mapRepositories(@Nullable RawRepositories rawRepositories) {
        List<MavenRepository> pomRepositories = emptyList();
        if (rawRepositories != null) {
            List<RawRepositories.Repository> unmappedRepos = rawRepositories.getRepositories();
            if (unmappedRepos != null) {
                pomRepositories = new ArrayList<>(unmappedRepos.size());
                for (RawRepositories.Repository r : unmappedRepos) {
                    pomRepositories.add(new MavenRepository(r.getId(), URI.create(r.getUrl()),
                            r.getReleases() == null || r.getReleases().isEnabled(),
                            r.getSnapshots() != null && r.getSnapshots().isEnabled(),
                            false, null, null));
                }

            }
        }
        return pomRepositories;
    }

    private List<DependencyManagementDependency> mapDependencyManagement(@Nullable DependencyManagement rawDependencyManagement) {
        List<DependencyManagementDependency> dependencyManagementDependencies = emptyList();
        if (rawDependencyManagement != null && rawDependencyManagement.getDependencies() != null) {
            List<Dependency> unmappedDependencies = rawDependencyManagement.getDependencies().getDependencies();
            if (unmappedDependencies != null) {
                dependencyManagementDependencies = new ArrayList<>(unmappedDependencies.size());
                for (Dependency d : unmappedDependencies) {
                    GroupArtifactVersion dGav = new GroupArtifactVersion(d.getGroupId(), d.getArtifactId(), d.getVersion());
                    if ("import".equals(d.getScope())) {
                        dependencyManagementDependencies.add(new DependencyManagementDependency.Imported(dGav));
                    } else {
                        dependencyManagementDependencies.add(new DependencyManagementDependency.Defined(dGav, d.getScope(), d.getType(), d.getClassifier(), d.getExclusions()));
                    }
                }
            }
        }
        return dependencyManagementDependencies;
    }

    private List<org.openrewrite.maven.tree.Dependency> mapRequestedDependencies(@Nullable Dependencies rawDependencies) {
        List<org.openrewrite.maven.tree.Dependency> dependencies = emptyList();
        if (rawDependencies != null && rawDependencies.getDependencies() != null) {
            List<Dependency> unmappedDependencies = rawDependencies.getDependencies();
            if (unmappedDependencies != null) {
                dependencies = new ArrayList<>(unmappedDependencies.size());
                for (Dependency d : unmappedDependencies) {
                    GroupArtifactVersion dGav = new GroupArtifactVersion(d.getGroupId(), d.getArtifactId(), d.getVersion());
                    dependencies.add(new org.openrewrite.maven.tree.Dependency(dGav, d.getClassifier(), d.getType(), d.getScope(), d.getExclusions(),
                            d.getOptional() != null && d.getOptional()));
                }
            }
        }
        return dependencies;
    }
}
