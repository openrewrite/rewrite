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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.tree.MavenRepository;
import java.util.*;

public class InMemoryMavenPomCache implements MavenPomCache {

    private final Map<PomKey, CacheResult<RawMaven>> pomCache = new HashMap<>();
    private final Map<MetadataKey, CacheResult<MavenMetadata>> mavenMetadataCache = new HashMap<>();
    private final Map<MavenRepository, CacheResult<MavenRepository>> repositoryCache = new HashMap<>();
    private final long releaseTimeToLiveMilliseconds;
    private final long snapshotTimeToLiveMilliseconds;

    public InMemoryMavenPomCache() {
        this(60_000 * 60, 60_000);
    }

    public InMemoryMavenPomCache(long releaseTimeToLiveMilliseconds, long snapshotTimeToLiveMilliseconds) {
        this.releaseTimeToLiveMilliseconds = releaseTimeToLiveMilliseconds;
        this.snapshotTimeToLiveMilliseconds = snapshotTimeToLiveMilliseconds;

        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "poms"), pomCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "metadata"), mavenMetadataCache);
        Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "inmem", "content", "repository urls"), repositoryCache);
    }

    @Override
    @Nullable
    public CacheResult<MavenMetadata> getMavenMetadata(MetadataKey key) {
        return filterExpired(mavenMetadataCache.get(key));
    }

    @Override
    public CacheResult<MavenMetadata> setMavenMetadata(MetadataKey key, MavenMetadata metadata, boolean isSnapshot) {
        long ttl = System.currentTimeMillis() + (isSnapshot ? snapshotTimeToLiveMilliseconds : releaseTimeToLiveMilliseconds);
        mavenMetadataCache.put(key, new CacheResult<>(CacheResult.State.Cached, metadata, ttl));
        return new CacheResult<>(CacheResult.State.Updated, metadata, ttl);
    }

    @Override
    @Nullable
    public CacheResult<RawMaven> getMaven(PomKey key) {
        return filterExpired(pomCache.get(key));
    }

    @Override
    public CacheResult<RawMaven> setMaven(PomKey key, RawMaven maven, boolean isSnapshot) {
        long ttl = System.currentTimeMillis() + (isSnapshot ? snapshotTimeToLiveMilliseconds : releaseTimeToLiveMilliseconds);
        pomCache.put(key, new CacheResult<>(CacheResult.State.Cached, maven, ttl));
        return new CacheResult<>(CacheResult.State.Updated, maven, ttl);
    }

    @Override
    @Nullable
    public CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        return filterExpired(repositoryCache.get(repository));
    }

    @Override
    public CacheResult<MavenRepository> setNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
        long ttl = calculateExpiration(normalized == null ? 60_000 : 60_000 * 60);
        repositoryCache.put(repository, new CacheResult<>(CacheResult.State.Cached, normalized, ttl));
        return new CacheResult<>(CacheResult.State.Updated, normalized, ttl);
    }

    @Override
    public void clear() {
        pomCache.clear();
        mavenMetadataCache.clear();
        repositoryCache.clear();
    }

    @Override
    public void close() throws Exception {
    }
}
