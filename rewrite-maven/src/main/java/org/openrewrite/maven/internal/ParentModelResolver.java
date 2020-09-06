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

import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Because {@link org.apache.maven.repository.internal.DefaultModelResolver} is package private.
 */
@SuppressWarnings("JavadocReference")
public class ParentModelResolver implements ModelResolver {
    private static final Logger logger = LoggerFactory.getLogger(ParentModelResolver.class);

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> remoteRepositories;

    public ParentModelResolver(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession,
                               List<RemoteRepository> remoteRepositories) {
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteRepositories = remoteRepositories;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) {
        logger.trace("resolving model for: {}:{}", groupId, artifactId);
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(pomArtifact);
            artifactRequest.setRepositories(remoteRepositories);

            pomArtifact = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest).getArtifact();
        } catch (ArtifactResolutionException e) {
            logger.error("unable to resolve model", e);
            return new UnresolvedModelSource(groupId, artifactId, version);
        }

        return new FileModelSource(pomArtifact.getFile());
    }

    @SuppressWarnings("deprecation")
    @Override
    public ModelSource resolveModel(Parent parent) {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }

    private static class UnresolvedModelSource implements ModelSource2 {
        private final String groupId;
        private final String artifactId;
        private final String version;

        private UnresolvedModelSource(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            return null;
        }

        @Override
        public URI getLocationURI() {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            String syntheticPom = "<project>\n" +
                    "<modelVersion>4.0.0</modelVersion>\n" +
                    "<packaging>pom</packaging>\n" +
                    "<groupId>" + groupId + "</groupId>\n" +
                    "<artifactId>" + artifactId + "</artifactId>\n" +
                    "<version>" + version + "</version>\n" +
                    "</project>";

            return new ByteArrayInputStream(syntheticPom.getBytes());
        }

        @Override
        public String getLocation() {
            return null;
        }
    }
}
