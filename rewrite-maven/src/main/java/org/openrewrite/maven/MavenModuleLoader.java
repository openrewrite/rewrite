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
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.version.Version;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.CachingWorkspaceReader;
import org.openrewrite.maven.internal.ParentModelResolver;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class MavenModuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(MavenModuleLoader.class);

    private final Map<MavenModel.ModuleVersionId, MavenModel.Dependency> dependencyCache = new HashMap<>();

    private final CachingWorkspaceReader workspaceReader;

    private final boolean resolveDependencies;
    private boolean resolveNonProjectParents;
    private final File localRepository;
    private final List<RemoteRepository> remoteRepositories;

    public MavenModuleLoader(boolean resolveDependencies,
                             boolean resolveNonProjectParents,
                             File localRepository,
                             @Nullable File workspaceDir,
                             List<RemoteRepository> remoteRepositories) {
        this.resolveDependencies = resolveDependencies;
        this.resolveNonProjectParents = resolveNonProjectParents;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.workspaceReader = new CachingWorkspaceReader(workspaceDir);
    }

    public List<MavenModel> load(Iterable<Parser.Input> inputs) {
        return load(InputStreamModelSource.fromInputs(inputs));
    }

    private List<MavenModel> load(Collection<InputStreamModelSource> modelSources) {
        List<MavenModel> models = new ArrayList<>();
        Map<MavenModel.ModuleVersionId, Collection<MavenModel>> inheriting = new HashMap<>();

        for (InputStreamModelSource modelSource : modelSources) {
            MavenModel model = load(modelSource);
            models.add(model);

            MavenModel descendent = model;
            MavenModel ancestor = model.getParent();
            for (; ancestor != null; descendent = ancestor, ancestor = ancestor.getParent()) {
                inheriting.computeIfAbsent(ancestor.getModuleVersion(), m -> new ArrayList<>())
                        .add(descendent.withParent(null)); // null the parent to cut the object cycle
            }
        }

        return models.stream()
                .map(m -> new MavenModel(m.getParent(), m.getModuleVersion(),
                        m.getDependencyManagement(), m.getDependencies(),
                        m.getTransitiveDependenciesByScope(), m.getProperties(),
                        inheriting.getOrDefault(m.getModuleVersion(), emptyList())))
                .collect(toList());
    }

    private MavenModel load(InputStreamModelSource modelSource) {
        try {
            RepositorySystem repositorySystem = getRepositorySystem();
            RepositorySystemSession repositorySystemSession = getRepositorySystemSession(repositorySystem);

            DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                    .setModelResolver(new ParentModelResolver(repositorySystem,
                            repositorySystemSession, remoteRepositories, resolveNonProjectParents))
                    .setModelSource(modelSource)
                    .setSystemProperties(System.getProperties());

            DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory() {
                @Override
                protected SuperPomProvider newSuperPomProvider() {
                    NoMavenCentralSuperPomProvider superPomProvider = new NoMavenCentralSuperPomProvider();
                    superPomProvider.setModelProcessor(newModelProcessor());
                    return superPomProvider;
                }
            }.newInstance();

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
            Model rawParentModel = result.getRawModel(model.getParent().getId().replace(":pom", ""));
            if (rawParentModel.getGroupId() == null) {
                rawParentModel.setGroupId(model.getParent().getGroupId());
            }
            if (rawParentModel.getVersion() == null) {
                rawParentModel.setVersion(model.getParent().getVersion());
            }

            parent = buildMavenModelRecursive(repositorySystem, repositorySystemSession,
                    result, rawParentModel);
        }

        Model rawModel = result.getRawModel(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());

        Map<String, Set<MavenModel.ModuleVersionId>> transitiveDepenenciesByScope = new HashMap<>();

        List<MavenModel.Dependency> dependencies = model.getDependencies().stream()
                .map(dependency -> resolveDependency(repositorySystem, repositorySystemSession, dependency,
                        model, rawModel, transitiveDepenenciesByScope))
                .collect(toList());

        MavenModel.DependencyManagement dependencyManagement = model.getDependencyManagement() == null ?
                null :
                new MavenModel.DependencyManagement(model
                        .getDependencyManagement()
                        .getDependencies()
                        .stream()
                        .map(dependency -> resolveDependency(
                                repositorySystem,
                                repositorySystemSession,
                                dependency,
                                model,
                                rawModel,
                                transitiveDepenenciesByScope)
                        )
                        .collect(toList())
                );

        Map<String, String> properties = model.getProperties().entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

        return new MavenModel(parent,
                new MavenModel.ModuleVersionId(
                        model.getGroupId(),
                        model.getArtifactId(),
                        null,
                        model.getVersion(),
                        newerVersions(repositorySystem, repositorySystemSession, model,
                                model.getGroupId(), model.getArtifactId(), model.getVersion(),
                                "", "pom")
                ),
                dependencyManagement,
                dependencies,
                transitiveDepenenciesByScope,
                properties,
                emptyList());
    }

    private List<String> newerVersions(RepositorySystem repositorySystem,
                                       RepositorySystemSession repositorySystemSession,
                                       Model model,
                                       String groupId, String artifactId, String version,
                                       String classifier, String extension) {
        Artifact newerArtifacts = new DefaultArtifact(groupId, artifactId, classifier, extension,
                "(" + version + ",)");
        VersionRangeRequest newerArtifactsRequest = new VersionRangeRequest();
        newerArtifactsRequest.setArtifact(newerArtifacts);
        newerArtifactsRequest.setRepositories(remoteRepositoriesFromModel(model));

        try {
            VersionRangeResult newerArtifactsResult = repositorySystem.resolveVersionRange(repositorySystemSession, newerArtifactsRequest);
            List<String> versions = newerArtifactsResult.getVersions().stream()
                    .map(Version::toString)
                    .collect(toList());
            workspaceReader.cacheVersions(newerArtifacts, versions);
            return versions;
        } catch (VersionRangeResolutionException e) {
            return emptyList();
        }
    }

    private List<RemoteRepository> remoteRepositoriesFromModel(Model model) {
        List<RemoteRepository> remotes = new ArrayList<>(remoteRepositories);
        model.getRepositories().forEach(repo -> remotes.add(
                new RemoteRepository.Builder(repo.getId(), "default",
                        repo.getUrl().replace("http:", "https:")).build())
        );
        return remotes;
    }

    private MavenModel.Dependency resolveDependency(RepositorySystem repositorySystem,
                                                    RepositorySystemSession repositorySystemSession,
                                                    Dependency dependency, Model model, Model rawModel,
                                                    Map<String, Set<MavenModel.ModuleVersionId>> transitiveDepenenciesByScope) {

        // This may not be the EFFECTIVE mvid, e.g. a version here could be a property reference. So two distinct
        // "dependency" instances could ultimately refer to the same MavenModel.Dependency, and that's ok.
        MavenModel.ModuleVersionId mvidFromDependency = new MavenModel.ModuleVersionId(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                dependency.getVersion(),
                emptyList());

        return dependencyCache.computeIfAbsent(mvidFromDependency, mvid -> {
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
                    Artifact artifact = collectIfNecessary(repositorySystem, repositorySystemSession,
                            model, dependency, transitiveDepenenciesByScope);

                    return new MavenModel.Dependency(
                            new MavenModel.ModuleVersionId(
                                    artifact.getGroupId(),
                                    artifact.getArtifactId(),
                                    artifact.getClassifier(),
                                    artifact.getVersion(),
                                    newerVersions(repositorySystem, repositorySystemSession, model,
                                            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                            artifact.getClassifier(), artifact.getExtension())
                            ),
                            requestedVersion,
                            dependency.getScope());

                }
            }

            return new MavenModel.Dependency(
                    mvid,
                    requestedVersion,
                    dependency.getScope());
        });
    }

    private Artifact collectIfNecessary(RepositorySystem repositorySystem,
                                        RepositorySystemSession repositorySystemSession,
                                        Model model,
                                        Dependency dependency,
                                        Map<String, Set<MavenModel.ModuleVersionId>> transitiveDepenenciesByScope) {
        Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                dependency.getType(),
                Maven.getPropertyKey(dependency.getVersion())
                        .map(versionProperty -> (String) model.getProperties().get(versionProperty))
                        .orElse(dependency.getVersion()));

        if (resolveDependencies) {
            try {
                CollectRequest collectRequest = new CollectRequest();
                collectRequest.setRepositories(remoteRepositoriesFromModel(model));
                collectRequest.addDependency(
                        new org.eclipse.aether.graph.Dependency(
                                artifact,
                                dependency.getScope(),
                                dependency.getOptional() != null && dependency.getOptional().equals("true"),
                                dependency.getExclusions().stream()
                                        .map(exclusion -> new Exclusion(
                                                exclusion.getGroupId(),
                                                exclusion.getArtifactId(),
                                                null,
                                                null
                                        ))
                                        .collect(toList())
                        )
                );

                CollectResult collectResult = repositorySystem
                        .collectDependencies(repositorySystemSession, collectRequest);

                DependencyNode dependencyNode = collectResult.getRoot().getChildren().iterator().next();
                artifact = dependencyNode.getArtifact();
                collectTransitiveDependencies(dependencyNode, transitiveDepenenciesByScope);

                logger.debug("artifact {} resolved to {}", artifact, artifact.getFile());
            } catch (DependencyCollectionException e) {
                logger.warn("error collecting dependencies", e);
            }
        }

        return artifact;
    }

    public void collectTransitiveDependencies(DependencyNode node, Map<String, Set<MavenModel.ModuleVersionId>> transitiveDepenenciesByScope) {
        Artifact dep = node.getDependency().getArtifact();
        transitiveDepenenciesByScope.computeIfAbsent(node.getDependency().getScope(), scope -> new HashSet<>())
                .add(new MavenModel.ModuleVersionId(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getVersion(), emptyList()));
        node.getChildren().forEach(child -> collectTransitiveDependencies(child, transitiveDepenenciesByScope));
    }

    private RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
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

        repositorySystemSession.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        repositorySystemSession.setCache(new DefaultRepositoryCache());
        repositorySystemSession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(ArtifactDescriptorPolicy.IGNORE_ERRORS));
        repositorySystemSession.setWorkspaceReader(workspaceReader);
        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryEventListener());
        repositorySystemSession.setDependencySelector(
                new AndDependencySelector(
                        new ScopeDependencySelector(emptyList(),
                                Arrays.asList("provided", "test", "runtime")),
                        new OptionalDependencySelector()
                )
        );

        repositorySystemSession.setReadOnly();

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

    static class InputStreamModelSource implements ModelSource2 {
        private final Path path;
        private final Supplier<InputStream> inputStream;

        private Map<Path, InputStreamModelSource> allModelSources;

        public InputStreamModelSource(Path path, Supplier<InputStream> inputStream) {
            this.path = path;
            this.inputStream = inputStream;
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            return allModelSources.get(path.getParent().resolve(relPath).normalize());
        }

        @Override
        public URI getLocationURI() {
            return path.toUri();
        }

        @Override
        public InputStream getInputStream() {
            return inputStream.get();
        }

        @Override
        public String getLocation() {
            return path.toString();
        }

        public static Collection<InputStreamModelSource> fromInputs(Iterable<Parser.Input> inputs) {
            Map<Path, InputStreamModelSource> modelSources = StreamSupport.stream(inputs.spliterator(), false)
                    .map(input -> new InputStreamModelSource(input.getPath(), input::getSource))
                    .collect(Collectors.toMap(
                            modelSource -> modelSource.path,
                            Function.identity(),
                            (s1, s2) -> s1,
                            LinkedHashMap::new));

            modelSources.values().forEach(modelSource -> modelSource.allModelSources = modelSources);
            return modelSources.values();
        }
    }

    private static class NoMavenCentralSuperPomProvider extends DefaultSuperPomProvider {
        @Override
        public Model getSuperModel(String version) {
            Model superModel = super.getSuperModel(version);
            superModel.getRepositories().clear();
            return superModel;
        }
    }
}
