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
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.MavenMetadata;

/**
 * A previously cached {@link MavenMetadata} together with the HTTP validators returned by the
 * origin when it was downloaded. A {@link MavenPomCache} can hand one of these back from
 * {@link MavenPomCache#getMavenMetadataForRevalidation} so that, rather than re-downloading and
 * re-parsing an expired {@code maven-metadata.xml}, the downloader can issue a conditional GET
 * ({@code If-None-Match} / {@code If-Modified-Since}). On a {@code 304 Not Modified} the cached
 * {@link #metadata} is reused as-is.
 */
@Value
public class MavenMetadataValidation {

    /**
     * The cached metadata value. May be {@code null} for a previously cached negative result.
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
     * The {@code Last-Modified} response header (raw HTTP-date string) captured when the metadata
     * was downloaded, replayed verbatim as {@code If-Modified-Since}. {@code null} if the origin
     * did not provide one.
     */
    @Nullable
    String lastModified;
}
