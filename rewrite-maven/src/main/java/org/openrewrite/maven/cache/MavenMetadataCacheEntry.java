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
package org.openrewrite.maven.cache;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.MavenMetadata;

/**
 * A cached {@link MavenMetadata} value together with the HTTP validators (ETag / Last-Modified)
 * returned by the origin when it was downloaded. This is the single payload a {@link MavenPomCache}
 * exchanges for {@code maven-metadata.xml}: it is handed back from
 * {@link MavenPomCache#getMavenMetadata} and accepted by {@link MavenPomCache#putMavenMetadata}.
 * <p>
 * Carrying the validators on every read lets the downloader, when an entry's freshness window has
 * elapsed ({@link #expired}), revalidate it with a conditional GET ({@code If-None-Match} /
 * {@code If-Modified-Since}) rather than re-downloading and re-parsing the body. On a {@code 304 Not
 * Modified} the cached {@link #metadata} is reused as-is.
 */
@Value
public class MavenMetadataCacheEntry {

    /**
     * The cached metadata value. {@code null} represents a previously cached negative result (a
     * download that failed with a client-side error), distinct from a cache miss, which is signalled
     * by {@link MavenPomCache#getMavenMetadata} returning {@code null} for the whole entry.
     */
    @Nullable
    MavenMetadata metadata;

    /**
     * The {@code ETag} response header captured when the metadata was downloaded, replayed as
     * {@code If-None-Match}. {@code null} if the origin did not provide one.
     */
    @Nullable
    String etag;

    /**
     * The {@code Last-Modified} response header (raw HTTP-date string) captured when the metadata was
     * downloaded, replayed verbatim as {@code If-Modified-Since}. {@code null} if the origin did not
     * provide one.
     */
    @Nullable
    String lastModified;

    /**
     * The cache's freshness verdict, populated when the entry is read back. {@code true} means the
     * entry's freshness window has elapsed and the caller should revalidate it with a conditional GET
     * rather than serving {@link #metadata} directly. It is always {@code false} on the write path
     * (see {@link #fresh}); caches that expire entries recompute it on read from their own retention
     * policy, e.g. via {@link #withExpired(boolean)}.
     */
    @With
    boolean expired;

    /**
     * Create an entry for storing a just-fetched value and its validators. The entry is not
     * {@linkplain #expired}; a cache decides expiry when the entry is later read back.
     */
    public static MavenMetadataCacheEntry fresh(@Nullable MavenMetadata metadata, @Nullable String etag, @Nullable String lastModified) {
        return new MavenMetadataCacheEntry(metadata, etag, lastModified, false);
    }
}
