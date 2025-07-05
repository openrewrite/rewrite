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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.tree.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.maven.tree.Plugin.PLUGIN_DEFAULT_GROUPID;

/**
 * A value object deserialized directly from POM XML
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@XmlRootElement(name = "project")
@SuppressWarnings("unused")
public class RawPom {

    // Obsolete field supplanted by the "modelVersion" field in modern poms
    @Nullable
    String pomVersion;

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

    // Obsolete field supplanted by the "version" field in modern poms
    @Nullable
    String currentVersion;

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
    Prerequisites prerequisites;

    @Nullable
    String packaging;

    Dependencies dependencies;

    @Nullable
    DependencyManagement dependencyManagement;

    @Nullable
    Map<String, String> properties;

    @Nullable
    Build build;

    @Nullable
    RawRepositories repositories;

    @Nullable
    RawPluginRepositories pluginRepositories;

    @Nullable
    Licenses licenses;

    @Nullable
    Profiles profiles;

    @Nullable
    Modules modules;

    @Nullable
    SubProjects subprojects;

    public RawPom(@Nullable String pomVersion, @Nullable Parent parent, @Nullable String groupId, String artifactId, @Nullable String version, @Nullable String currentVersion, @Nullable String name, @Nullable String description, @Nullable Prerequisites prerequisites, @Nullable String packaging, @Nullable Dependencies dependencies, @Nullable DependencyManagement dependencyManagement, @Nullable Map<String, String> properties, @Nullable Build build, @Nullable RawRepositories repositories, @Nullable RawPluginRepositories pluginRepositories, @Nullable Licenses licenses, @Nullable Profiles profiles, @Nullable Modules modules, @Nullable SubProjects subprojects) {
        this.pomVersion = pomVersion;
        this.parent = parent;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.currentVersion = currentVersion;
        this.name = name;
        this.description = description;
        this.prerequisites = prerequisites;
        this.packaging = packaging;
        this.dependencies = dependencies == null ? new Dependencies() : dependencies;
        this.dependencyManagement = dependencyManagement;
        this.properties = properties;
        this.build = build;
        this.repositories = repositories;
        this.pluginRepositories = pluginRepositories;
        this.licenses = licenses;
        this.profiles = profiles;
        this.modules = modules;
        this.subprojects = subprojects;
    }

    public static RawPom parse(InputStream inputStream, @Nullable String snapshotVersion) {
        try {
            RawPom pom = MavenXmlMapper.readMapper().readValue(inputStream, RawPom.class);
            if (snapshotVersion != null) {
                pom.setSnapshotVersion(snapshotVersion);
            }
            return pom;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse pom: " + e.getMessage(), e);
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
        String optional;

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
            this.dependencies = new ArrayList<>();
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
    public static class Prerequisites {

        @JacksonXmlProperty(localName = "maven")
        @Nullable
        public String maven;
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

    @Getter
    public static class Modules {
        private final List<String> modules;

        public Modules() {
            this.modules = emptyList();
        }

        public Modules(@JacksonXmlProperty(localName = "module") List<String> modules) {
            this.modules = modules;
        }
    }

    @Getter
    public static class SubProjects {
        private final List<String> subprojects;

        public SubProjects() {
            this.subprojects = emptyList();
        }

        public SubProjects(@JacksonXmlProperty(localName = "subproject") List<String> subprojects) {
            this.subprojects = subprojects;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Data
    public static class Build {

        @Nullable
        @JacksonXmlElementWrapper(localName = "plugins")
        @JacksonXmlProperty(localName = "plugin")
        List<Plugin> plugins;

        @Nullable
        @JacksonXmlProperty(localName = "pluginManagement")
        PluginManagement pluginManagement;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Data
    public static class PluginManagement {

        @Nullable
        @JacksonXmlElementWrapper(localName = "plugins")
        @JacksonXmlProperty(localName = "plugin")
        List<Plugin> plugins;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Plugin {
        @Nullable
        String groupId;
        String artifactId;

        @Nullable
        String version;

        @Nullable
        String extensions;

        @Nullable
        String inherited;

        @Nullable
        JsonNode configuration;

        @NonFinal
        @Nullable
        @JacksonXmlElementWrapper(localName = "dependencies")
        @JacksonXmlProperty(localName = "dependency")
        List<Dependency> dependencies;

        @NonFinal
        @Nullable
        @JacksonXmlElementWrapper(localName = "executions")
        @JacksonXmlProperty(localName = "execution")
        List<Execution> executions;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Execution {
        String id;

        @NonFinal
        @JacksonXmlElementWrapper(localName = "goals")
        @JacksonXmlProperty(localName = "goal")
        List<String> goals;

        String phase;

        @Nullable
        String inherited;

        @Nullable
        JsonNode configuration;
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

        @Nullable
        RawPluginRepositories pluginRepositories;
    }

    public @Nullable String getGroupId() {
        return groupId == null && parent != null ? parent.getGroupId() : groupId;
    }

    public @Nullable String getVersion() {
        if (version == null) {
            if (currentVersion == null) {
                if (parent == null) {
                    return null;
                } else {
                    return parent.getVersion();
                }
            } else {
                return currentVersion;
            }
        }
        return version;
    }


    public Pom toPom(@Nullable Path inputPath, @Nullable MavenRepository repo) {
        org.openrewrite.maven.tree.Parent parent = getParent() == null ? null : new org.openrewrite.maven.tree.Parent(new GroupArtifactVersion(
                getParent().getGroupId(), getParent().getArtifactId(),
                getParent().getVersion()), getParent().getRelativePath());

        Pom.PomBuilder builder = Pom.builder()
                .sourcePath(inputPath)
                .repository(repo)
                .parent(parent)
                .gav(new ResolvedGroupArtifactVersion(
                        repo == null ? null : repo.getUri(),
                        Objects.requireNonNull(getGroupId()),
                        artifactId,
                        Objects.requireNonNull(getVersion()),
                        null))
                .name(name)
                .obsoletePomVersion(pomVersion)
                .prerequisites(prerequisites == null ? null : new org.openrewrite.maven.tree.Prerequisites(prerequisites.getMaven()))
                .packaging(packaging)
                .properties(getProperties() == null ? emptyMap() : getProperties())
                .licenses(mapLicenses(getLicenses()))
                .profiles(mapProfiles(getProfiles()))
                .subprojects(mapSubProjects(getModules(), getSubprojects()));
        if (StringUtils.isBlank(pomVersion)) {
            builder.dependencies(mapRequestedDependencies(getDependencies()))
                    .dependencyManagement(mapDependencyManagement(getDependencyManagement()))
                    .repositories(mapRepositories(getRepositories()))
                    .pluginRepositories(mapPluginRepositories(getPluginRepositories()))
                    .plugins(mapPlugins((build != null) ? build.getPlugins() : null))
                    .pluginManagement(mapPlugins((build != null && build.getPluginManagement() != null) ? build.getPluginManagement().getPlugins() : null));
        }
        return builder.build();
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
                            mapRepositories(p.getRepositories()),
                            mapPluginRepositories(p.getPluginRepositories()),
                            mapPlugins((build != null) ? build.getPlugins() : null),
                            mapPlugins((build != null && build.getPluginManagement() != null) ? build.getPluginManagement().getPlugins() : null)
                    ));
                }

            }
        }
        return profiles;
    }

    @NonNull
    private List<MavenRepository> mapRepositories(@Nullable RawRepositories rawRepositories) {
        List<MavenRepository> pomRepositories = emptyList();
        if (rawRepositories != null) {
            List<RawRepositories.Repository> unmappedRepos = rawRepositories.getRepositories();
            if (unmappedRepos != null) {
                pomRepositories = new ArrayList<>(unmappedRepos.size());
                for (RawRepositories.Repository r : unmappedRepos) {
                    pomRepositories.add(new MavenRepository(r.getId(), r.getUrl(),
                            r.getReleases() == null ? null : r.getReleases().getEnabled(),
                            r.getSnapshots() == null ? null : r.getSnapshots().getEnabled(),
                            false, null, null, null, null));
                }

            }
        }
        return pomRepositories;
    }

    @NonNull
    private List<MavenRepository> mapPluginRepositories(@Nullable RawPluginRepositories rawRepositories) {
        List<MavenRepository> pomRepositories = emptyList();
        if (rawRepositories != null) {
            List<RawPluginRepositories.PluginRepository> unmappedRepos = rawRepositories.getPluginRepositories();
            if (unmappedRepos != null) {
                pomRepositories = new ArrayList<>(unmappedRepos.size());
                for (RawPluginRepositories.PluginRepository r : unmappedRepos) {
                    pomRepositories.add(new MavenRepository(r.getId(), r.getUrl(),
                            r.getReleases() == null ? null : r.getReleases().getEnabled(),
                            r.getSnapshots() == null ? null : r.getSnapshots().getEnabled(),
                            false, null, null, null, null));
                }

            }
        }
        return pomRepositories;
    }

    private List<ManagedDependency> mapDependencyManagement(@Nullable DependencyManagement rawDependencyManagement) {
        List<ManagedDependency> dependencyManagementDependencies = emptyList();
        if (rawDependencyManagement != null && rawDependencyManagement.getDependencies() != null) {
            List<Dependency> unmappedDependencies = rawDependencyManagement.getDependencies().getDependencies();
            if (unmappedDependencies != null) {
                dependencyManagementDependencies = new ArrayList<>(unmappedDependencies.size());
                for (Dependency d : unmappedDependencies) {
                    GroupArtifactVersion dGav = new GroupArtifactVersion(d.getGroupId(), d.getArtifactId(), d.getVersion());
                    if ("import".equals(d.getScope())) {
                        dependencyManagementDependencies.add(new ManagedDependency.Imported(dGav));
                    } else {
                        dependencyManagementDependencies.add(new ManagedDependency.Defined(dGav, d.getScope(), d.getType(), d.getClassifier(), d.getExclusions()));
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
                            d.getOptional()));
                }
            }
        }
        return dependencies;
    }

    private List<org.openrewrite.maven.tree.Dependency> mapRequestedDependencies(@Nullable List<Dependency> rawDependencies) {
        List<org.openrewrite.maven.tree.Dependency> dependencies = emptyList();
        if (rawDependencies != null) {
            dependencies = new ArrayList<>(rawDependencies.size());
            for (Dependency d : rawDependencies) {
                GroupArtifactVersion dGav = new GroupArtifactVersion(d.getGroupId(), d.getArtifactId(), d.getVersion());
                dependencies.add(new org.openrewrite.maven.tree.Dependency(dGav, d.getClassifier(), d.getType(), d.getScope(), d.getExclusions(),
                        d.getOptional()));
            }
        }
        return dependencies;
    }

    private List<org.openrewrite.maven.tree.Plugin> mapPlugins(@Nullable List<Plugin> rawPlugins) {
        List<org.openrewrite.maven.tree.Plugin> plugins = emptyList();
        if (rawPlugins != null) {
            plugins = new ArrayList<>(rawPlugins.size());
            for (Plugin rawPlugin : rawPlugins) {
                String pluginGroupId = rawPlugin.getGroupId();
                plugins.add(new org.openrewrite.maven.tree.Plugin(
                        pluginGroupId == null ? PLUGIN_DEFAULT_GROUPID : pluginGroupId,
                        rawPlugin.getArtifactId(),
                        rawPlugin.getVersion(),
                        rawPlugin.getExtensions(),
                        rawPlugin.getInherited(),
                        rawPlugin.getConfiguration(),
                        mapRequestedDependencies(rawPlugin.getDependencies()),
                        mapPluginExecutions(rawPlugin.getExecutions())
                ));
            }
        }
        return plugins;
    }

    private Map<String, Object> mapPluginConfiguration(@Nullable JsonNode configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return emptyMap();
        }
        return MavenXmlMapper.readMapper().convertValue(configuration, new TypeReference<Map<String, Object>>() {
        });
    }

    private List<org.openrewrite.maven.tree.Plugin.Execution> mapPluginExecutions(@Nullable List<Execution> rawExecutions) {
        List<org.openrewrite.maven.tree.Plugin.Execution> executions = emptyList();
        if (rawExecutions != null) {
            executions = new ArrayList<>(rawExecutions.size());
            for (Execution rawExecution : rawExecutions) {
                executions.add(new org.openrewrite.maven.tree.Plugin.Execution(
                        rawExecution.getId(),
                        rawExecution.getGoals(),
                        rawExecution.getPhase(),
                        rawExecution.getInherited(),
                        rawExecution.getConfiguration()
                ));
            }
        }
        return executions;
    }

    private List<String> mapSubProjects(@Nullable Modules modules, @Nullable SubProjects subprojects) {
        if (modules == null && subprojects != null) {
            return subprojects.getSubprojects();
        }
        if (subprojects == null && modules != null) {
            return modules.getModules();
        }
        if (modules != null && subprojects != null) {
            return ListUtils.concatAll(modules.getModules(), subprojects.getSubprojects());
        }
        return emptyList();
    }
}
