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
package org.openrewrite.maven.cache;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;

import java.net.URI;
import java.util.Optional;

public interface MavenPomCache {

    @Nullable
    ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency);

    void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved);

    @Nullable
    Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav);

    void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata);

    /**
     * Return a possibly-stale cached metadata entry together with its HTTP validators (ETag /
     * Last-Modified) so the caller can revalidate it with a conditional GET instead of a full
     * re-download. This is consulted only after {@link #getMavenMetadata} has reported a miss
     * (e.g. because the entry's freshness window has elapsed), giving caches a chance to retain a
     * validator past the point at which they stop serving the value directly.
     * <p>
     * The default implementation returns {@code null}, meaning "no validator available"; callers
     * then fall back to an unconditional download, preserving pre-existing behavior.
     *
     * @param repo the repository the metadata would be downloaded from
     * @param gav  the group/artifact (and optionally version) the metadata describes
     * @return a validator-carrying entry, or {@code null} if none is retained
     */
    default @Nullable MavenMetadataValidation getMavenMetadataForRevalidation(URI repo, GroupArtifactVersion gav) {
        return null;
    }

    /**
     * Store metadata along with the HTTP validators returned by the origin, so that a later expiry
     * can be revalidated cheaply via {@link #getMavenMetadataForRevalidation}. Re-storing a value
     * that was confirmed unchanged by a {@code 304} response also refreshes the entry's freshness.
     * <p>
     * The default implementation delegates to {@link #putMavenMetadata(URI, GroupArtifactVersion,
     * MavenMetadata)}, discarding the validators; caches that do not support conditional requests
     * therefore behave exactly as before.
     *
     * @param repo         the repository the metadata was downloaded from
     * @param gav          the group/artifact (and optionally version) the metadata describes
     * @param metadata     the metadata value, or {@code null} for a negative result
     * @param etag         the {@code ETag} response header, or {@code null}
     * @param lastModified the {@code Last-Modified} response header, or {@code null}
     */
    default void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata,
                                  @Nullable String etag, @Nullable String lastModified) {
        putMavenMetadata(repo, gav, metadata);
    }

    @Nullable
    Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) throws MavenDownloadingException;

    void putPom(ResolvedGroupArtifactVersion gav, @Nullable Pom pom);

    @Nullable
    Optional<MavenRepository> getNormalizedRepository(MavenRepository repository);

    void putNormalizedRepository(MavenRepository repository, MavenRepository normalized);
}
