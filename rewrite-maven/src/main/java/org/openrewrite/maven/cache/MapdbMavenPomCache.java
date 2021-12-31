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
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.*;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.File;

public class MapdbMavenPomCache implements MavenPomCache {

    private static final Serializer<MavenRepository> REPOSITORY_SERIALIZER = new JacksonMapdbSerializer<>(MavenRepository.class);
    private static final Serializer<PomKey> POM_KEY_SERIALIZER = new JacksonMapdbSerializer<>(PomKey.class);
    private static final Serializer<MetadataKey> METADATA_KEY_SERIALIZER = new JacksonMapdbSerializer<>(MetadataKey.class);

    private static final Serializer<CacheResult<RawMaven>> MAVEN_SERIALIZER = new CacheResultJacksonMapdbSerializer<>(RawMaven.class);
    private static final Serializer<CacheResult<MavenRepository>> OPTIONAL_REPOSITORY_SERIALIZER = new CacheResultJacksonMapdbSerializer<>(MavenRepository.class);
    private static final Serializer<CacheResult<MavenMetadata>> MAVEN_METADATA_SERIALIZER = new CacheResultJacksonMapdbSerializer<>(MavenMetadata.class);

    private final HTreeMap<PomKey, CacheResult<RawMaven>> pomCache;
    private final HTreeMap<MetadataKey, CacheResult<MavenMetadata>> mavenMetadataCache;
    private final HTreeMap<MavenRepository, CacheResult<MavenRepository>> normalizedRepositoryUrls;
    private final long releaseTimeToLiveMilliseconds;
    private final long snapshotTimeToLiveMilliseconds;

    public MapdbMavenPomCache(@Nullable File workspace, @Nullable Long maxCacheStoreSize,
                              long releaseTimeToLiveMilliseconds, long snapshotTimeToLiveMilliseconds) {

        this.releaseTimeToLiveMilliseconds = releaseTimeToLiveMilliseconds;
        this.snapshotTimeToLiveMilliseconds = snapshotTimeToLiveMilliseconds;

        if (workspace != null) {
            if(!workspace.exists() && !workspace.mkdirs()) {
                throw new IllegalStateException("Unable to find or create maven pom cache at " + workspace);
            }

            if(workspace.isDirectory()) {
                workspace = new File(workspace, "db");
            }

            DB localRepositoryDiskDb = DBMaker
                    .fileDB(workspace)
                    .fileMmapEnableIfSupported()
                    .fileLockWait(10_000)
                    .checksumHeaderBypass()
                    .closeOnJvmShutdown()
                    .make();

            pomCache = localRepositoryDiskDb
                    .hashMap("pom.disk")
                    .keySerializer(POM_KEY_SERIALIZER)
                    .valueSerializer(MAVEN_SERIALIZER)
                    .createOrOpen();

            mavenMetadataCache = localRepositoryDiskDb
                    .hashMap("metadata.disk")
                    .keySerializer(METADATA_KEY_SERIALIZER)
                    .valueSerializer(MAVEN_METADATA_SERIALIZER)
                    .createOrOpen();

            normalizedRepositoryUrls = localRepositoryDiskDb
                    .hashMap("repository.urls")
                    .keySerializer(REPOSITORY_SERIALIZER)
                    .valueSerializer(OPTIONAL_REPOSITORY_SERIALIZER)
                    .createOrOpen();

            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "disk", "content", "poms"), pomCache);
            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "disk", "content", "metadata"), mavenMetadataCache);
            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "disk", "content", "repository urls"), normalizedRepositoryUrls);
        } else {
            DB inMemoryDb = DBMaker
                    .heapDB()
                    .make();

            // fast in-memory collection with limited size
            pomCache = inMemoryDb
                    .hashMap("pom.inmem")
                    .keySerializer(POM_KEY_SERIALIZER)
                    .valueSerializer(MAVEN_SERIALIZER)
                    .expireStoreSize(maxCacheStoreSize == null ? 0 : maxCacheStoreSize)
                    .create();

            mavenMetadataCache = inMemoryDb
                    .hashMap("metadata.inmem")
                    .keySerializer(METADATA_KEY_SERIALIZER)
                    .valueSerializer(MAVEN_METADATA_SERIALIZER)
                    .expireStoreSize(maxCacheStoreSize == null ? 0 : maxCacheStoreSize)
                    .create();

            normalizedRepositoryUrls = inMemoryDb
                    .hashMap("repository.urls")
                    .keySerializer(REPOSITORY_SERIALIZER)
                    .valueSerializer(OPTIONAL_REPOSITORY_SERIALIZER)
                    .create();

            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "memory", "content", "pom"), pomCache);
            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "memory", "content", "metadata"), mavenMetadataCache);
            Metrics.gaugeMapSize("rewrite.maven.cache.size", Tags.of("type", "mapdb", "layer", "memory", "content", "repository urls"), normalizedRepositoryUrls);
        }
    }

    @Override
    public CacheResult<MavenMetadata> getMavenMetadata(MetadataKey key) {
        CacheResult<MavenMetadata> metadata = mavenMetadataCache.get(key);
        if (metadata != null && metadata.getTtl() > 0 && metadata.getTtl() < System.currentTimeMillis()) {
            //If current time is greater than time to live, return null (a cache miss)
            return null;
        } else {
            return metadata;
        }
    }

    @Override
    public CacheResult<MavenMetadata> setMavenMetadata(MetadataKey key, MavenMetadata metadata, boolean isSnapshot) {
        long ttl = System.currentTimeMillis() + (isSnapshot ? snapshotTimeToLiveMilliseconds : releaseTimeToLiveMilliseconds);
        mavenMetadataCache.put(key, new CacheResult<>(CacheResult.State.Cached, metadata, ttl));
        return new CacheResult<>(CacheResult.State.Updated, metadata, ttl);
    }

    @Override
    public CacheResult<RawMaven> getMaven(PomKey key) {
        CacheResult<RawMaven> rawMaven = pomCache.get(key);
        if (rawMaven != null && rawMaven.getTtl() > 0 && rawMaven.getTtl() < System.currentTimeMillis()) {
            //If current time is greater than time to live, return null (a cache miss)
            return null;
        } else {
            return rawMaven;
        }
    }

    @Override
    public CacheResult<RawMaven> setMaven(PomKey key, RawMaven maven, boolean isSnapshot) {
        long ttl = System.currentTimeMillis() + (isSnapshot ? snapshotTimeToLiveMilliseconds : releaseTimeToLiveMilliseconds);
        pomCache.put(key, new CacheResult<>(CacheResult.State.Cached, maven, ttl));
        return new CacheResult<>(CacheResult.State.Updated, maven, ttl);
    }

    @Override
    public CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        CacheResult<MavenRepository> normalized = normalizedRepositoryUrls.get(repository);
        if (normalized != null && normalized.getTtl() > 0 && normalized.getTtl() < System.currentTimeMillis()) {
            //If current time is greater than time to live, return null (a cache miss)
            return null;
        } else {
            return normalized;
        }
    }

    @Override
    public CacheResult<MavenRepository> setNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
        normalizedRepositoryUrls.put(repository, new CacheResult<>(CacheResult.State.Cached, normalized, -1));
        return new CacheResult<>(CacheResult.State.Updated, normalized, -1);
    }

    @Override
    public void close() {
        pomCache.close();
        mavenMetadataCache.close();
        normalizedRepositoryUrls.close();
    }

    @Override
    public void clear() {
        pomCache.clear();
        mavenMetadataCache.clear();
        normalizedRepositoryUrls.clear();
    }
}
