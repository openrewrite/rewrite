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
package org.openrewrite.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.openrewrite.maven.internal.ParentModelResolver;
import org.openrewrite.maven.tree.MavenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class MavenModuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(MavenModuleLoader.class);

    private final boolean resolveDependencies;
    private final File localRepository;
    private final List<RemoteRepository> remoteRepositories;

    public MavenModuleLoader(boolean resolveDependencies, File localRepository, List<RemoteRepository> remoteRepositories) {
        this.resolveDependencies = resolveDependencies;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    public Map<Path, MavenModel> load(List<Path> sourceFiles) {
        Map<Path, MavenModel> modelsByPath = new HashMap<>();
        Map<MavenModel.ModuleVersionId, Collection<MavenModel>> inheriting = new HashMap<>();

        for (Path sourceFile : sourceFiles) {
            MavenModel model = load(sourceFile);
            modelsByPath.put(sourceFile, model);

            MavenModel descendent = model;
            MavenModel ancestor = model.getParent();
            for (; ancestor != null; descendent = ancestor, ancestor = ancestor.getParent()) {
                inheriting.computeIfAbsent(ancestor.getModuleVersion(), m -> new ArrayList<>())
                        .add(descendent.withParent(null)); // null the parent to cut the object cycle
            }
        }

        return modelsByPath.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, modelByPath -> {
                    MavenModel m = modelByPath.getValue();
                    return new MavenModel(m.getParent(), m.getModuleVersion(),
                            m.getDependencyManagement(), m.getDependencies(), m.getProperties(),
                            inheriting.getOrDefault(m.getModuleVersion(), emptyList()));
                }));
    }

    private MavenModel load(Path sourceFile) {
        try {
            RepositorySystem repositorySystem = getRepositorySystem();
            RepositorySystemSession repositorySystemSession = getRepositorySystemSession(repositorySystem);

            DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                    .setModelResolver(new ParentModelResolver(repositorySystem, repositorySystemSession, remoteRepositories))
                    .setPomFile(sourceFile.toFile());

            ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

            return buildMavenModelRecursive(repositorySystem, repositorySystemSession,
                    modelBuildingResult, modelBuildingResult.getEffectiveModel());
        } catch (ModelBuildingException e) {
            throw new RuntimeException(e);
        }
    }

    private MavenModel buildMavenModelRecursive(RepositorySystem repositorySystem,
                                                RepositorySystemSession repositorySystemSession,
                                                ModelBuildingResult result,
                                                Model model) {
        MavenModel parent = null;
        if (model.getParent() != null) {
            parent = buildMavenModelRecursive(repositorySystem, repositorySystemSession,
                    result, result.getRawModel(model.getParent().getId().replace(":pom", "")));
        }

        Model rawModel = result.getRawModel(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());

        List<MavenModel.Dependency> dependencies = model.getDependencies().stream()
                .map(dependency -> resolveDependency(repositorySystem, repositorySystemSession, dependency, rawModel))
                .collect(toList());

        MavenModel.DependencyManagement dependencyManagement = model.getDependencyManagement() == null ?
                null :
                new MavenModel.DependencyManagement(model
                        .getDependencyManagement()
                        .getDependencies()
                        .stream()
                        .map(dependency -> resolveDependency(repositorySystem, repositorySystemSession, dependency, rawModel))
                        .collect(toList())
                );

        Map<String, String> properties = model.getProperties().entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

        return new MavenModel(parent,
                new MavenModel.ModuleVersionId(model.getGroupId(), model.getArtifactId(), model.getVersion()),
                dependencyManagement,
                dependencies,
                properties,
                emptyList());
    }

    private MavenModel.Dependency resolveDependency(RepositorySystem repositorySystem,
                                                    RepositorySystemSession repositorySystemSession,
                                                    Dependency dependency, Model rawModel) {
        String requestedVersion = dependency.getVersion();

        if (rawModel != null) {
            requestedVersion = rawModel.getDependencies().stream()
                    .filter(d -> d.getGroupId().equals(dependency.getGroupId()) &&
                            d.getArtifactId().equals(dependency.getArtifactId()) &&
                            (d.getScope() == null || d.getScope().equals(dependency.getScope())))
                    .map(Dependency::getVersion)
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(dependency.getVersion());

            if (resolveDependencies) {
                Artifact artifact = new DefaultArtifact(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getType(),
                        dependency.getVersion());
                ArtifactRequest artifactRequest = new ArtifactRequest();
                artifactRequest.setArtifact(artifact);
                artifactRequest.setRepositories(remoteRepositories);

                try {
                    ArtifactResult artifactResult = repositorySystem
                            .resolveArtifact(repositorySystemSession, artifactRequest);
                    artifact = artifactResult.getArtifact();
                    logger.debug("artifact {} resolved to {}", artifact, artifact.getFile());

                    return new MavenModel.Dependency(
                            new MavenModel.ModuleVersionId(
                                    artifact.getGroupId(),
                                    artifact.getArtifactId(),
                                    artifact.getVersion()),
                            requestedVersion,
                            dependency.getScope());
                } catch (ArtifactResolutionException e) {
                    logger.warn("error resolving artifact: {}", e.getMessage());
                }
            }
        }

        return new MavenModel.Dependency(
                new MavenModel.ModuleVersionId(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion()),
                requestedVersion,
                dependency.getScope());
    }

    private RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator
                .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return serviceLocator.getService(RepositorySystem.class);
    }

    private DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
                .newSession();

        LocalRepository localRepository = new LocalRepository(this.localRepository.getPath());
        repositorySystemSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(repositorySystemSession, localRepository));

        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryEventListener());

        return repositorySystemSession;
    }

    private static class ConsoleRepositoryEventListener
            extends AbstractRepositoryListener {

        @Override
        public void artifactInstalled(RepositoryEvent event) {
            logger.debug("artifact {} installed to file {}", event.getArtifact(), event.getFile());
        }

        @Override
        public void artifactInstalling(RepositoryEvent event) {
            logger.debug("installing artifact {} to file {}", event.getArtifact(), event.getFile());
        }

        @Override
        public void artifactResolved(RepositoryEvent event) {
            logger.debug("artifact {} resolved from repository {}", event.getArtifact(),
                    event.getRepository());
        }

        @Override
        public void artifactDownloading(RepositoryEvent event) {
            logger.debug("downloading artifact {} from repository {}", event.getArtifact(),
                    event.getRepository());
        }

        @Override
        public void artifactDownloaded(RepositoryEvent event) {
            logger.debug("downloaded artifact {} from repository {}", event.getArtifact(),
                    event.getRepository());
        }

        @Override
        public void artifactResolving(RepositoryEvent event) {
            logger.debug("resolving artifact {}", event.getArtifact());
        }
    }
}
