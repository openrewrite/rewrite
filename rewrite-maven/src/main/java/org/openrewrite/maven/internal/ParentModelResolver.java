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
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.util.List;

/**
 * Because {@link org.apache.maven.repository.internal.DefaultModelResolver} is package private.
 */
@SuppressWarnings("JavadocReference")
public class ParentModelResolver implements ModelResolver {
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> remoteRepositories;

    public ParentModelResolver(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) {
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteRepositories = remoteRepositories;
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(pomArtifact);
            artifactRequest.setRepositories(remoteRepositories);

            pomArtifact = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        return new FileModelSource(pomArtifact.getFile());
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}
