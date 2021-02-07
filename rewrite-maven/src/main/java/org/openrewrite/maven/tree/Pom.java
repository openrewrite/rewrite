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
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class Pom implements Marker {
    @Nullable
    String groupId;

    String artifactId;

    @Nullable
    String version;

    /**
     * The timestamp and build numbered version number (the latest snapshot at time dependencies were resolved).
     */
    @Nullable
    String snapshotVersion;

    @Nullable
    String packaging;

    @Nullable
    String classifier;

    @Nullable
    Pom parent;

    @With
    Collection<Dependency> dependencies;

    DependencyManagement dependencyManagement;

    @With
    Collection<License> licenses;

    Collection<Repository> repositories;
    Map<String, String> properties;

    public Pom(@Nullable String groupId,
               String artifactId,
               @Nullable String version,
               @Nullable String snapshotVersion,
               @Nullable String packaging,
               @Nullable String classifier,
               @Nullable Pom parent,
               Collection<Dependency> dependencies,
               DependencyManagement dependencyManagement,
               Collection<License> licenses,
               Collection<Repository> repositories,
               Map<String, String> properties) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.snapshotVersion = snapshotVersion;
        this.packaging = packaging;
        this.classifier = classifier;
        this.parent = parent;
        this.dependencies = dependencies;
        this.dependencyManagement = dependencyManagement;
        this.licenses = licenses;
        this.repositories = repositories;
        this.properties = properties;
    }

    public Set<Dependency> getDependencies(Scope scope) {
        Set<Dependency> dependenciesForScope = new TreeSet<>(Comparator.comparing(Dependency::getCoordinates));
        for (Dependency dependency : dependencies) {
            addDependenciesFromScope(scope, dependency, dependenciesForScope);
        }
        return dependenciesForScope;
    }

    private void addDependenciesFromScope(Scope scope, Dependency dep, Set<Dependency> found) {
        if (dep.getScope().isInClasspathOf(scope)) {
            found.add(dep);
            for (Dependency child : dep.getModel().getDependencies()) {
                addDependenciesFromScope(scope, child, found);
            }
        }
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

    public Collection<Pom.Dependency> findDependencies(String groupId, String artifactId) {
        return dependencies.stream()
                .flatMap(d -> d.findDependencies(groupId, artifactId).stream())
                .collect(Collectors.toList());
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
    public String getPackaging() {
        return packaging;
    }

    @Nullable
    public String getClassifier() {
        return classifier;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Data
    public static class Repository {
        String id;

        @EqualsAndHashCode.Include
        URI uri;

        boolean releases;
        boolean snapshots;
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

            switch (license) {
                case "Apache License, Version 2.0":
                case "The Apache Software License, Version 2.0":
                    return new Pom.License(license, Pom.LicenseType.Apache2);
                case "GNU Lesser General Public License":
                case "GNU Library General Public License":
                    // example Lanterna
                    return new Pom.License(license, Pom.LicenseType.LGPL);
                case "Public Domain":
                    return new Pom.License(license, LicenseType.PublicDomain);
                default:
                    if (license.contains("LGPL")) {
                        // example Checkstyle
                        return new Pom.License(license, Pom.LicenseType.LGPL);
                    } else if (license.contains("GPL") || license.contains("GNU General Public License")) {
                        // example com.buschmais.jqassistant:jqassistant-maven-plugin
                        // example com.github.mtakaki:dropwizard-circuitbreaker
                        return new Pom.License(license, Pom.LicenseType.GPL);
                    } else if (license.contains("CDDL")) {
                        return new Pom.License(license, LicenseType.CDDL);
                    } else if (license.contains("Creative Commons") || license.contains("CC0")) {
                        return new Pom.License(license, LicenseType.CreativeCommons);
                    } else if (license.contains("BSD")) {
                        return new Pom.License(license, LicenseType.BSD);
                    } else if (license.contains("MIT")) {
                        return new Pom.License(license, LicenseType.MIT);
                    } else if (license.contains("Eclipse") || license.contains("EPL")) {
                        return new Pom.License(license, LicenseType.Eclipse);
                    } else if (license.contains("Apache") || license.contains("ASF")) {
                        return new Pom.License(license, LicenseType.Apache2);
                    } else if (license.contains("Mozilla")) {
                        return new Pom.License(license, LicenseType.Mozilla);
                    } else if (license.toLowerCase().contains("GNU Lesser General Public License".toLowerCase()) ||
                            license.contains("GNU Library General Public License")) {
                        return new Pom.License(license, LicenseType.LGPL);
                    }
                    return new Pom.License(license, Pom.LicenseType.Unknown);
            }
        }
    }

    public enum LicenseType {
        Apache2,
        BSD,
        CDDL,
        CreativeCommons,
        Eclipse,
        GPL,
        LGPL,
        MIT,
        Mozilla,
        PublicDomain,
        Unknown
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency implements DependencyDescriptor {
        Repository repository;

        Scope scope;

        @Nullable
        String classifier;

        @Nullable
        String type;

        boolean optional;
        Pom model;

        @Nullable
        String requestedVersion;

        @Nullable
        String datedSnapshotVersion;

        Set<GroupArtifact> exclusions;

        public String getGroupId() {
            return model.getGroupId();
        }

        public String getArtifactId() {
            return model.getArtifactId();
        }

        public String getVersion() {
            return model.getVersion();
        }

        public String getCoordinates() {
            return model.getGroupId() + ':' + model.getArtifactId() + ':' + model.getVersion() +
                    (classifier == null ? "" : ':' + classifier);
        }

        @Override
        public String toString() {
            return "Dependency {" + getCoordinates() +
                    (optional ? ", optional" : "") +
                    (!getVersion().equals(requestedVersion) ? ", requested=" + requestedVersion : "") +
                    '}';
        }

        /**
         * Finds transitive dependencies of this dependency that match the provided group and artifact ids.
         *
         * @param groupId    The groupId to match
         * @param artifactId The artifactId to match.
         * @return Transitive dependencies with any version matching the provided group and artifact id, if any.
         */
        public Collection<Pom.Dependency> findDependencies(String groupId, String artifactId) {
            return findDependencies(d -> d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId));
        }

        /**
         * Finds transitive dependencies of this dependency that match the given predicate.
         *
         * @param matcher A dependency test.
         * @return Transitive dependencies with any version matching the given predicate.
         */
        public Collection<Pom.Dependency> findDependencies(Predicate<Dependency> matcher) {
            List<Pom.Dependency> matches = new ArrayList<>();
            if (matcher.test(this)) {
                matches.add(this);
            }
            for (Dependency d : model.getDependencies()) {
                matches.addAll(d.findDependencies(matcher));
            }
            return matches;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        Collection<DependencyManagementDependency> dependencies;
    }

    @Override
    public String toString() {
        return "Pom{" +
                groupId + ':' + artifactId + ':' + version +
                '}';
    }
}
