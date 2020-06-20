package org.openrewrite.maven;

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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class MavenParser {
    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);

    private final XmlParser xmlParser = new XmlParser();
    private final File localRepository;

    private MavenParser(File localRepository) {
        this.localRepository = localRepository;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Maven.Pom> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        return sourceFiles.stream().map(source -> parse(source, relativeTo)).collect(toList());
    }

    public Maven.Pom parse(Path sourceFile, @Nullable Path relativeTo) {
        return new Maven.Pom(buildMavenModel(sourceFile),
                xmlParser.parse(sourceFile, relativeTo));
    }

    public MavenModel buildMavenModel(Path sourceFile) {
        try {
            RepositorySystem repositorySystem = getRepositorySystem();
            RepositorySystemSession repositorySystemSession = getRepositorySystemSession(repositorySystem);

            DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
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

        logger.debug("Maven model resolved: {}, parsing its dependencies...", model);
        List<MavenModel.Dependency> dependencies = model.getDependencies().stream().map(dependency -> {
            logger.debug("processing dependency: {}", dependency);
            Artifact artifact = new DefaultArtifact(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getType(),
                    dependency.getVersion());
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(singletonList(new RemoteRepository.Builder("central",
                    "default", "http://central.maven.org/maven2/").build()));

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
                        dependency.getScope());
            } catch (ArtifactResolutionException e) {
                logger.warn("error resolving artifact: {}", e.getMessage());
                return new MavenModel.Dependency(
                        new MavenModel.ModuleVersionId(
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion()),
                        dependency.getScope());
            }
        }).collect(toList());

        return new MavenModel(parent,
                new MavenModel.ModuleVersionId(model.getGroupId(), model.getArtifactId(), model.getVersion()),
                dependencies);
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

    public static class Builder {
        File localRepository = new File(System.getProperty("user.home") + "/.m2");

        public Builder localRepository(File localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(localRepository);
        }
    }
}
