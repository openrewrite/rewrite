package org.openrewrite.maven.internal;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerString;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.GroupArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class RawPomDownloader {
    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);

    private static final Serializer<Optional<RawMaven>> MAVEN_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawMaven.class);
    private static final Serializer<RawPom.Repository> REPOSITORY_SERIALIZER = new JacksonMapdbSerializer<>(RawPom.Repository.class);
    private static final Serializer<Optional<RawPom.Repository>> OPTIONAL_REPOSITORY_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawPom.Repository.class);
    private static final Serializer<Optional<RawMavenMetadata>> MAVEN_METADATA_SERIALIZER = new OptionalJacksonMapdbSerializer<>(RawMavenMetadata.class);
    private static final Serializer<GroupArtifactRepository> GROUP_ARTIFACT_SERIALIZER = new JacksonMapdbSerializer<>(GroupArtifactRepository.class);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build();

    private final HTreeMap<String, Optional<RawMaven>> pomCache;
    private final HTreeMap<GroupArtifactRepository, Optional<RawMavenMetadata>> mavenMetadataCache;
    private final HTreeMap<RawPom.Repository, Optional<RawPom.Repository>> normalizedRepositoryUrls;

    private final Map<URI, RawMaven> projectPoms;

    public RawPomDownloader(@Nullable File workspace,
                            @Nullable Long maxCacheStoreSize) {
        if (workspace != null) {
            DB localRepositoryDiskDb = DBMaker
                    .fileDB(workspace)
                    .fileMmapEnableIfSupported()
                    .fileLockWait()
                    .checksumStoreEnable()
                    .closeOnJvmShutdown()
                    .make();

            pomCache = localRepositoryDiskDb
                    .hashMap("pom.disk")
                    .keySerializer(new SerializerString())
                    .valueSerializer(MAVEN_SERIALIZER)
                    .createOrOpen();

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

            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "disk", "content", "poms"), pomCache);
            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "disk", "content", "metadata"), mavenMetadataCache);
            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "disk", "content", "repository urls"), normalizedRepositoryUrls);
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

            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "memory", "content", "pom"), pomCache);
            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "memory", "content", "metadata"), mavenMetadataCache);
            Metrics.gaugeMapSize("rewrite.maven.workspace.cache.size", Tags.of("layer", "memory", "content", "repository urls"), normalizedRepositoryUrls);
        }

        this.projectPoms = emptyMap();
    }

    private RawPomDownloader(HTreeMap<String, Optional<RawMaven>> pomCache,
                             HTreeMap<GroupArtifactRepository, Optional<RawMavenMetadata>> mavenMetadataCache,
                             HTreeMap<RawPom.Repository, Optional<RawPom.Repository>> normalizedRepositoryUrls,
                             Map<URI, RawMaven> projectPoms) {
        this.pomCache = pomCache;
        this.mavenMetadataCache = mavenMetadataCache;
        this.normalizedRepositoryUrls = normalizedRepositoryUrls;
        this.projectPoms = projectPoms;
    }

    private void fillUnresolvablePoms() {
        new BufferedReader(new InputStreamReader(RawPomDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))
                .lines()
                .filter(line -> !line.isEmpty())
                .forEach(gav -> pomCache.put(gav, Optional.empty()));
    }

    public RawPomDownloader withProjectPoms(Collection<RawMaven> projectPoms) {
        return new RawPomDownloader(pomCache, mavenMetadataCache, normalizedRepositoryUrls,
                projectPoms.stream().collect(toMap(RawMaven::getURI, Function.identity())));
    }

    public RawMavenMetadata downloadMetadata(String groupId, String artifactId, List<RawPom.Repository> repositories) {
        GroupArtifact ga = new GroupArtifact(groupId, artifactId);
        Timer.Sample sample = Timer.start();

        return repositories.stream()
                .distinct()
                .map(this::normalizeRepository)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(repo -> mavenMetadataCache.compute(new GroupArtifactRepository(repo.getUrl(), ga), (gar, cachedMetadata) -> {
                    if (cachedMetadata != null && cachedMetadata.isPresent()) {
                        sample.stop(Metrics.timer("rewrite.maven.download",
                                "outcome", "cached",
                                "exception", "none",
                                "type", "metadata",
                                "group.id", groupId,
                                "artifact.id", artifactId)
                        );
                        return cachedMetadata;
                    } else {
                        logger.debug("Resolving {}:{} metadata from {}", groupId, artifactId, repo.getUrl());

                        String uri = repo.getUrl() + "/" +
                                groupId.replace('.', '/') + '/' +
                                artifactId + '/' +
                                "maven-metadata.xml";

                        Exception ex = null;

                        Request request = new Request.Builder().url(uri).get().build();
                        try (Response response = httpClient.newCall(request).execute()) {
                            if (response.isSuccessful() && response.body() != null) {
                                sample.stop(Metrics.timer("rewrite.maven.download",
                                        "outcome", "success",
                                        "exception", "none",
                                        "type", "metadata",
                                        "group.id", groupId,
                                        "artifact.id", artifactId));

                                @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body().bytes();

                                return Optional.of(RawMavenMetadata.parse(responseBody));
                            }
                        } catch (IOException e) {
                            ex = e;
                        }

                        sample.stop(Metrics.timer("rewrite.maven.download",
                                "outcome", "error",
                                "exception", ex == null ? "none" : ex.getClass().getName(),
                                "type", "metadata",
                                "group.id", groupId,
                                "artifact.id", artifactId));

                        return Optional.empty();
                    }
                }))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(RawMavenMetadata.EMPTY, (m1, m2) -> {
                    if (m1 == RawMavenMetadata.EMPTY) {
                        if (m2 == RawMavenMetadata.EMPTY) {
                            return m1;
                        } else {
                            return m2;
                        }
                    } else if (m2 == RawMavenMetadata.EMPTY) {
                        return m1;
                    } else {
                        return new RawMavenMetadata(new RawMavenMetadata.Versioning(
                                Stream.concat(m1.getVersioning().getVersions().stream(),
                                        m2.getVersioning().getVersions().stream()).collect(toList())
                        ));
                    }
                });
    }

    @Nullable
    public RawMaven download(String groupId,
                             String artifactId,
                             String version,
                             @Nullable String classifier,
                             @Nullable String relativePath,
                             @Nullable RawMaven containingPom,
                             List<RawPom.Repository> repositories) {

        Timer.Sample sample = Timer.start();

        try {
            String cacheKey = groupId + ':' + artifactId + ':' + version + (classifier == null ? "" : ':' + classifier);

            return pomCache.compute(cacheKey, (key, cachedPom) -> {
                if (cachedPom != null && cachedPom.isPresent()) {
                    sample.stop(Metrics.timer("rewrite.maven.download",
                            "outcome", "cached",
                            "exception", "none",
                            "type", "pom",
                            "group.id", groupId,
                            "artifact.id", artifactId));
                    return cachedPom;
                } else {
                    if (!StringUtils.isBlank(relativePath) && (containingPom == null || !containingPom.getURI().getScheme().contains("http"))) {
                        return Optional.ofNullable(containingPom)
                                .map(pom -> projectPoms.get(pom.getURI()
                                        .relativize(URI.create(relativePath))
                                        .resolve("pom.xml")
                                        .normalize()
                                ));
                    }

                    return repositories.stream()
                            .distinct()
                            .map(this::normalizeRepository)
                            .filter(repo -> repo.map(r -> r.acceptsVersion(version)).orElse(false))
                            .map(Optional::get)
                            .map(repo -> {
                                String uri = repo.getUrl() + "/" +
                                        groupId.replace('.', '/') + '/' +
                                        artifactId + '/' +
                                        version + '/' +
                                        artifactId + '-' + version + ".pom";

                                Exception ex = null;

                                Request request = new Request.Builder().url(uri).get().build();
                                try (Response response = httpClient.newCall(request).execute()) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        sample.stop(Metrics.timer("rewrite.maven.download",
                                                "outcome", "success",
                                                "exception", "none",
                                                "type", "pom",
                                                "group.id", groupId,
                                                "artifact.id", artifactId));

                                        @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body().bytes();

                                        return RawMaven.parse(
                                                new Parser.Input(URI.create(uri), () -> new ByteArrayInputStream(responseBody)),
                                                null
                                        );
                                    }
                                } catch (IOException e) {
                                    ex = e;
                                }

                                sample.stop(Metrics.timer("rewrite.maven.download",
                                        "outcome", "error",
                                        "exception", ex == null ? "none" : ex.getClass().getName(),
                                        "type", "pom",
                                        "group.id", groupId,
                                        "artifact.id", artifactId));

                                return null;
                            })
                            .filter(Objects::nonNull)
                            .findFirst();
                }
            }).orElse(null);
        } catch (Throwable t) {
            logger.error("Failed to download {}:{}:{}:{}", groupId, artifactId, version, classifier, t);
            return null;
        }
    }

    private Optional<RawPom.Repository> normalizeRepository(RawPom.Repository repository) {
        return normalizedRepositoryUrls.computeIfAbsent(repository, repo -> {
            try {
                // FIXME add retry logic
                String url = repo.getUrl();
                Request request = new Request.Builder().url(url).head().build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (url.toLowerCase().contains("http://")) {
                        return normalizeRepository(
                                new RawPom.Repository(
                                        url.toLowerCase().replace("http://", "https://"),
                                        repository.getReleases(),
                                        repository.getSnapshots()
                                )
                        );
                    } else if (response.isSuccessful()) {
                        return Optional.of(new RawPom.Repository(
                                url,
                                repository.getReleases(),
                                repository.getSnapshots()
                        ));
                    }

                    return Optional.empty();
                }
            } catch (IOException e) {
                return Optional.empty();
            }
        });
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    private static class GroupArtifactRepository {
        String repository;
        GroupArtifact groupArtifact;
    }
}
