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

import org.eclipse.aether.*;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

import static java.util.Collections.emptyList;

public final class MavenRepositorySystemUtils {
    private static final Logger logger = LoggerFactory.getLogger(MavenRepositorySystemUtils.class);

    private MavenRepositorySystemUtils() {
    }

    public static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = org.apache.maven.repository.internal.MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return serviceLocator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system,
                                                                             @Nullable File localRepositoryDir) {
        DefaultRepositorySystemSession repositorySystemSession = org.apache.maven.repository.internal.MavenRepositorySystemUtils
                .newSession();

        LocalRepository localRepository = new LocalRepository(localRepositoryDir);
        repositorySystemSession.setLocalRepositoryManager(
                system.newLocalRepositoryManager(repositorySystemSession, localRepository));

        repositorySystemSession.setConfigProperty("aether.conflictResolver.verbose", true);
        repositorySystemSession.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        repositorySystemSession.setCache(new DefaultRepositoryCache());
        repositorySystemSession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(ArtifactDescriptorPolicy.IGNORE_ERRORS));
        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryEventListener());
        repositorySystemSession.setDependencySelector(
                new AndDependencySelector(
                        new ExclusionDependencySelector(),
                        new ScopeDependencySelector(emptyList(), Arrays.asList("provided", "test")),
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
}
