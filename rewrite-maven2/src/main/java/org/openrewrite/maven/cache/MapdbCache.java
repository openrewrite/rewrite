package org.openrewrite.maven.cache;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerString;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.*;
import org.openrewrite.maven.tree.GroupArtifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Callable;

public class MapdbCache implements MavenCache {
    private static final Serializer<Optional<RawMaven>> MAVEN_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawMaven.class);
    private static final Serializer<RawPom.Repository> REPOSITORY_SERIALIZER = new JacksonMapdbSerializer<>(RawPom.Repository.class);
    private static final Serializer<Optional<RawPom.Repository>> OPTIONAL_REPOSITORY_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawPom.Repository.class);
    private static final Serializer<Optional<RawMavenMetadata>> MAVEN_METADATA_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawMavenMetadata.class);
    private static final Serializer<GroupArtifactRepository> GROUP_ARTIFACT_SERIALIZER = new JacksonMapdbSerializer<>(GroupArtifactRepository.class);

    private final HTreeMap<String, Optional<RawMaven>> pomCache;
    private final HTreeMap<GroupArtifactRepository, Optional<RawMavenMetadata>> mavenMetadataCache;
    private final HTreeMap<RawPom.Repository, Optional<RawPom.Repository>> normalizedRepositoryUrls;

    CacheResult<RawMaven> UNAVAILABLE_POM = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<RawMavenMetadata> UNAVAILABLE_METADATA = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<RawPom.Repository> UNAVAILABLE_REPOSITORY = new CacheResult<>(CacheResult.State.Unavailable, null);

    public MapdbCache(@Nullable File workspace,
                      @Nullable Long maxCacheStoreSize) {
        if (workspace != null) {
            DB localRepositoryDiskDb = DBMaker
                    .fileDB(workspace)
                    .fileMmapEnableIfSupported()
                    .fileLockWait(10_000)
                    .checksumHeaderBypass()
                    .closeOnJvmShutdown()
                    .make();

            pomCache = localRepositoryDiskDb
                    .hashMap("pom.disk")
                    .keySerializer(new SerializerString())
                    .valueSerializer(MAVEN_SERIALIZER)
                    .createOrOpen();

            fillUnresolvablePoms();

            mavenMetadataCache = localRepositoryDiskDb
                    .hashMap("metadata.disk")
                    .keySerializer(GROUP_ARTIFACT_SERIALIZER)
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
                    .keySerializer(new SerializerString())
                    .valueSerializer(MAVEN_SERIALIZER)
                    .expireStoreSize(maxCacheStoreSize == null ? 0 : maxCacheStoreSize)
                    .create();

            fillUnresolvablePoms();

            mavenMetadataCache = inMemoryDb
                    .hashMap("metadata.inmem")
                    .keySerializer(GROUP_ARTIFACT_SERIALIZER)
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

    private void fillUnresolvablePoms() {
        new BufferedReader(new InputStreamReader(RawPomDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))
                .lines()
                .filter(line -> !line.isEmpty())
                .forEach(gav -> pomCache.put(gav, Optional.empty()));
    }

    @Override
    public CacheResult<RawMavenMetadata> computeMavenMetadata(URL repo, String groupId, String artifactId, Callable<RawMavenMetadata> orElseGet) throws Exception {
        GroupArtifactRepository gar = new GroupArtifactRepository(repo, new GroupArtifact(groupId, artifactId));
        Optional<RawMavenMetadata> rawMavenMetadata = mavenMetadataCache.get(gar);

        //noinspection OptionalAssignedToNull
        if (rawMavenMetadata == null) {
            try {
                RawMavenMetadata metadata = orElseGet.call();
                mavenMetadataCache.put(gar, Optional.ofNullable(metadata));
                return new CacheResult<>(CacheResult.State.Updated, metadata);
            } catch (Exception e) {
                mavenMetadataCache.put(gar, Optional.empty());
                throw e;
            }
        }

        return rawMavenMetadata
                .map(metadata -> new CacheResult<>(CacheResult.State.Cached, metadata))
                .orElse(UNAVAILABLE_METADATA);
    }

    @Override
    public CacheResult<RawMaven> computeMaven(URL repo, String groupId, String artifactId, String version,
                                                      Callable<RawMaven> orElseGet) throws Exception {
        // FIXME key be repo as well, because different repos may have different versions of the same POM
        String cacheKey = groupId + ':' + artifactId + ':' + version;
        Optional<RawMaven> rawMaven = pomCache.get(cacheKey);

        //noinspection OptionalAssignedToNull
        if (rawMaven == null) {
            try {
                RawMaven maven = orElseGet.call();
                pomCache.put(cacheKey, Optional.ofNullable(maven));
                return new CacheResult<>(CacheResult.State.Updated, maven);
            } catch (Exception e) {
                pomCache.put(cacheKey, Optional.empty());
                throw e;
            }
        }

        return rawMaven
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_POM);
    }

    @Override
    public CacheResult<RawPom.Repository> computeRepository(RawPom.Repository repository,
                                                            Callable<RawPom.Repository> orElseGet) throws Exception {
        Optional<RawPom.Repository> normalizedRepository = normalizedRepositoryUrls.get(repository);

        //noinspection OptionalAssignedToNull
        if (normalizedRepository == null) {
            try {
                RawPom.Repository repo = orElseGet.call();
                normalizedRepositoryUrls.put(repository, Optional.of(repo));
                return new CacheResult<>(CacheResult.State.Updated, repo);
            } catch (Exception e) {
                normalizedRepositoryUrls.put(repository, Optional.empty());
                throw e;
            }
        }

        return normalizedRepository
                .map(pom -> new CacheResult<>(CacheResult.State.Cached, pom))
                .orElse(UNAVAILABLE_REPOSITORY);
    }

    @Override
    public void close() {
        pomCache.close();
        mavenMetadataCache.close();
        normalizedRepositoryUrls.close();
    }
}
