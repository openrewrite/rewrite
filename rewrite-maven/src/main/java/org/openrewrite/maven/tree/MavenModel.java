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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.CachingWorkspaceReader;
import org.openrewrite.maven.internal.MavenRepositorySystemUtils;

import java.io.File;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * A minified, serializable form of {@link org.apache.maven.model.Model}
 * for inclusion as a data element of {@link Maven.Pom}.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class MavenModel {
    @ToString.Exclude
    @With
    @Nullable
    MavenModel parent;

    @EqualsAndHashCode.Include
    @With
    ModuleVersionId moduleVersion;

    @ToString.Exclude
    @Nullable
    @With
    DependencyManagement dependencyManagement;

    @ToString.Exclude
    @With
    List<Dependency> dependencies;

    @ToString.Exclude
    @With
    List<License> licenses;

    @With
    Map<String, Set<ModuleVersionId>> transitiveDependenciesByScope;

    @ToString.Exclude
    @With
    Map<String, String> properties;

    @ToString.Exclude
    @With
    Collection<Repository> repositories;

    /**
     * Modules inheriting from the POM this model represents. To cut the
     * object cycle, the parent of all of these modules will be null.
     */
    Collection<MavenModel> inheriting;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        @With
        ModuleVersionId moduleVersion;

        /**
         * The version written into the POM file, which may be a dynamic constraint or property reference.
         */
        @With
        String requestedVersion;

        @With
        String scope;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class License {
        @With
        String name;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ModuleVersionId implements Comparable<ModuleVersionId> {
        @With
        String groupId;

        @With
        String artifactId;

        @With
        @Nullable
        String classifier;

        @With
        String version;

        @With
        @Nullable
        String extension;

        @JsonIgnore
        public List<String> getNewerVersions(Maven.Pom pom, File localRepository,
                                             File workspaceDir) {
            RepositorySystem repositorySystem = MavenRepositorySystemUtils.getRepositorySystem();
            RepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
                    .getRepositorySystemSession(repositorySystem, localRepository,
                            CachingWorkspaceReader.forWorkspaceDir(workspaceDir));

            Artifact newerArtifacts = new DefaultArtifact(groupId, artifactId, classifier, extension,
                    "(" + version + ",)");
            VersionRangeRequest newerArtifactsRequest = new VersionRangeRequest();
            newerArtifactsRequest.setArtifact(newerArtifacts);

            List<RemoteRepository> remotes = new ArrayList<>(pom.getModel().getRepositories().size());

            for (Repository repo : pom.getModel().getRepositories()) {
                remotes.add(new RemoteRepository.Builder(repo.getId(), "default",
                        repo.getUrl()).build());

                if(repo.getUrl().contains("http://")) {
                    remotes.add(
                            new RemoteRepository.Builder(repo.getId(), "default",
                                    repo.getUrl().replace("http:", "https:")).build());
                }
            }

            newerArtifactsRequest.setRepositories(remotes);

            try {
                VersionRangeResult newerArtifactsResult = repositorySystem.resolveVersionRange(repositorySystemSession, newerArtifactsRequest);
                return newerArtifactsResult.getVersions().stream()
                        .map(Version::toString)
                        .collect(toList());
            } catch (VersionRangeResolutionException e) {
                return emptyList();
            }
        }

        @Override
        public int compareTo(ModuleVersionId v) {
            if (!groupId.equals(v.groupId)) {
                return comparePartByPart(groupId, v.groupId);
            } else if (!artifactId.equals(v.artifactId)) {
                return comparePartByPart(artifactId, v.artifactId);
            } else if (classifier == null && v.classifier != null) {
                return -1;
            } else if (classifier != null) {
                if (v.classifier == null) {
                    return 1;
                }
                if (!classifier.equals(v.classifier)) {
                    return classifier.compareTo(v.classifier);
                }
            }

            // in every case imagined so far, group and artifact comparison are enough,
            // so this is just for completeness
            return version.compareTo(v.version);
        }

        private int comparePartByPart(String d1, String d2) {
            String[] d1Parts = d1.split("[.-]");
            String[] d2Parts = d2.split("[.-]");

            for (int i = 0; i < Math.min(d1Parts.length, d2Parts.length); i++) {
                if (!d1Parts[i].equals(d2Parts[i])) {
                    return d1Parts[i].compareTo(d2Parts[i]);
                }
            }

            return d1Parts.length - d2Parts.length;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        @With
        List<Dependency> dependencies;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Repository {
        @With
        String id;

        @With
        String url;
    }

    public String valueOf(String value) {
        value = value.trim();
        return value.startsWith("${") && value.endsWith("}") ?
                properties.get(value.substring(2, value.length() - 1)) :
                value;
    }
}
