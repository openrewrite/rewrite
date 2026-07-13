/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.VersionResolver;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionResolutionException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.VersionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

/**
 * Decorates the engine's {@code DefaultVersionResolver}: a {@code -SNAPSHOT} coordinate the caller pinned via
 * {@code MavenExecutionContextView.getPinnedSnapshotVersions()} resolves to its recorded dated build with no metadata
 * read, exactly as {@code MavenPomDownloader.datedSnapshotVersion} short-circuits today. Because the resolver is wired
 * into the {@code RepositorySystem}, the pin wins for transitive snapshots too — every {@code resolveVersion} the
 * descriptor reader and artifact resolver drive routes through here. Any other coordinate delegates to the wrapped
 * resolver, whose metadata reads keep flowing through the resolver's own {@code MetadataResolver}/LRM path.
 */
class PinnedVersionResolver implements VersionResolver {

    private final VersionResolver delegate;

    PinnedVersionResolver(VersionResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request)
            throws VersionResolutionException {
        Artifact artifact = request.getArtifact();
        String version = artifact.getVersion();
        if (version != null && version.endsWith("-SNAPSHOT")) {
            for (ResolvedGroupArtifactVersion pinned : CollectContext.from(session).getPinnedSnapshotVersions()) {
                if (pinned.getDatedSnapshotVersion() != null &&
                    pinned.getGroupId().equals(artifact.getGroupId()) &&
                    pinned.getArtifactId().equals(artifact.getArtifactId()) &&
                    pinned.getVersion().equals(version)) {
                    return new VersionResult(request).setVersion(pinned.getDatedSnapshotVersion());
                }
            }
        }
        return delegate.resolveVersion(session, request);
    }
}
