package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class Pom {
    private static final Map<ModuleVersionId, Pom> flyweights = HashObjObjMaps.newMutableMap();

    @Getter(AccessLevel.NONE)
    ModuleVersionId moduleVersionId;

    @Nullable
    Maven parent;

    Collection<Dependency> dependencies;
    DependencyManagement dependencyManagement;
    Collection<License> licenses;
    Collection<Repository> repositories;
    Map<String, String> properties;

    private Pom(ModuleVersionId moduleVersionId,
                @Nullable Maven parent,
                Collection<Dependency> dependencies,
                DependencyManagement dependencyManagement,
                Collection<License> licenses,
                Collection<Repository> repositories,
                Map<String, String> properties) {
        this.moduleVersionId = moduleVersionId;
        this.parent = parent;
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
        this.licenses = licenses;
        this.repositories = repositories;
        this.properties = properties;
    }

    @JsonCreator
    public static Pom build(@JsonProperty("groupId") String groupId,
                            @JsonProperty("artifactId") String artifactId,
                            @JsonProperty("version") String version,
                            @JsonProperty("type") String type,
                            @Nullable @JsonProperty("classifier") String classifier,
                            @Nullable @JsonProperty("parent") Maven parent,
                            @JsonProperty("dependencies") Collection<Dependency> dependencies,
                            @JsonProperty("dependencyManagement") DependencyManagement dependencyManagement,
                            @JsonProperty("licenses") Collection<License> licenses,
                            @JsonProperty("repositories") Collection<Repository> repositories,
                            @JsonProperty("properties") Map<String, String> properties) {
        ModuleVersionId mvid = new ModuleVersionId(groupId, artifactId, version, type, classifier);
        return flyweights.computeIfAbsent(mvid, m ->
                new Pom(m, parent, dependencies, dependencyManagement, licenses, repositories, properties));
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    private static class ModuleVersionId {
        String groupId;
        String artifactId;
        String version;
        String type;

        @Nullable
        String classifier;
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
    public String getProperty(String property) {
        String key = property.replace("${", "").replace("}", "");
        return ofNullable(properties.get(key))
                .orElseGet(() -> parent == null ? null : parent.getModel().getProperty(property));
    }

    public String getGroupId() {
        return ofNullable(moduleVersionId.getGroupId())
                .orElseGet(() -> ofNullable(parent)
                        .map(p -> p.getModel().getGroupId())
                        .orElseThrow(() -> new IllegalStateException("groupId must be defined")));
    }

    public DependencyManagement getEffectiveDependencyManagement() {
        if(parent == null) {
            return dependencyManagement;
        }
        return new DependencyManagement(Stream.concat(dependencyManagement.getDependencies().stream(),
                parent.getModel().getEffectiveDependencyManagement().getDependencies().stream()).collect(Collectors.toList())
        );
    }

    @Nullable
    public String getManagedVersion(String groupId, String artifactId) {
        DependencyManagement effectiveDependencyManagement = getEffectiveDependencyManagement();
        return effectiveDependencyManagement.getDependencies().stream()
                .flatMap(dep -> dep.getDependencies().stream())
                .filter(dep -> groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId()))
                .findAny()
                .map(DependencyDescriptor::getVersion)
                .orElse(null);
    }

    /**
     * Cannot be inherited from a parent POM.
     */
    public String getArtifactId() {
        return moduleVersionId.getArtifactId();
    }

    public String getVersion() {
        return ofNullable(moduleVersionId.getVersion())
                .orElseGet(() -> ofNullable(parent)
                        .map(p -> p.getModel().getVersion())
                        .orElseThrow(() -> new IllegalStateException("version must be defined")));
    }

    public String getType() {
        return moduleVersionId.getType();
    }

    @Nullable
    public String getClassifier() {
        return moduleVersionId.getClassifier();
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
        Maven maven;

        Set<GroupArtifact> exclusions;

        @JsonIgnore
        public String getGroupId() {
            return maven.getModel().getGroupId();
        }

        @JsonIgnore
        public String getArtifactId() {
            return maven.getModel().getArtifactId();
        }

        @JsonIgnore
        public String getVersion() {
            return maven.getModel().getVersion();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        Collection<DependencyManagementDependency> dependencies;
    }
}
