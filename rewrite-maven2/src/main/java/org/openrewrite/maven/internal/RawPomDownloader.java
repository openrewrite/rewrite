package org.openrewrite.maven.internal;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.CacheResult;
import org.openrewrite.maven.cache.MavenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class RawPomDownloader {
    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build();

    private final MavenCache mavenCache;
    private final Map<URI, RawMaven> projectPoms;

    public RawPomDownloader(MavenCache mavenCache) {
        this(mavenCache, emptyMap());
    }

    public RawPomDownloader(MavenCache mavenCache, Map<URI, RawMaven> projectPoms) {
        this.mavenCache = mavenCache;
        this.projectPoms = projectPoms;
    }

    public RawMavenMetadata downloadMetadata(String groupId, String artifactId, List<RawPom.Repository> repositories) {
        Timer.Sample sample = Timer.start();

        return repositories.stream()
                .distinct()
                .map(this::normalizeRepository)
                .filter(Objects::nonNull)
                .map(repo -> {
                    Timer.Builder timer = Timer.builder("rewrite.maven.download")
                            .tag("group.id", groupId)
                            .tag("artifact.id", artifactId)
                            .tag("type", "metadata");

                    try {
                        CacheResult<RawMavenMetadata> result = mavenCache.computeMavenMetadata(URI.create(repo.getUrl()).toURL(), groupId, artifactId, () -> {
                            logger.debug("Resolving {}:{} metadata from {}", groupId, artifactId, repo.getUrl());

                            String uri = repo.getUrl() + "/" +
                                    groupId.replace('.', '/') + '/' +
                                    artifactId + '/' +
                                    "maven-metadata.xml";

                            Request request = new Request.Builder().url(uri).get().build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                if (response.isSuccessful() && response.body() != null) {
                                    @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body()
                                            .bytes();

                                    return RawMavenMetadata.parse(responseBody);
                                }
                            }

                            return null;
                        });

                        sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
                        return result.getData();
                    } catch (Exception e) {
                        sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                                .register(Metrics.globalRegistry));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
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

    private Timer.Builder addTagsByResult(Timer.Builder timer, CacheResult<?> result) {
        switch (result.getState()) {
            case Cached:
                timer = timer.tags("outcome", "cached", "exception", "none");
                break;
            case Unavailable:
                timer = timer.tags("outcome", "unavailable", "exception", "none");
                break;
            case Updated:
                timer = timer.tags("outcome", "downloaded", "exception", "none");
                break;
        }
        return timer;
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

        if (!StringUtils.isBlank(relativePath) && (containingPom == null || !containingPom.getURI().getScheme().contains("http"))) {
            return Optional.ofNullable(containingPom)
                    .map(pom -> projectPoms.get(pom.getURI()
                            .relativize(URI.create(relativePath))
                            .resolve("pom.xml")
                            .normalize()
                    ))
                    .orElse(null);
        }

        return repositories.stream()
                .distinct()
                .map(this::normalizeRepository)
                .filter(Objects::nonNull)
                .filter(repo -> repo.acceptsVersion(version))
                .map(repo -> {
                    Timer.Builder timer = Timer.builder("rewrite.maven.download")
                            .tag("group.id", groupId)
                            .tag("artifact.id", artifactId)
                            .tag("type", "pom");

                    try {
                        CacheResult<RawMaven> result = mavenCache.computeMaven(URI.create(repo.getUrl()).toURL(), groupId, artifactId, version, () -> {
                            String uri = repo.getUrl() + "/" +
                                    groupId.replace('.', '/') + '/' +
                                    artifactId + '/' +
                                    version + '/' +
                                    artifactId + '-' + version + ".pom";

                            Request request = new Request.Builder().url(uri).get().build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                if (response.isSuccessful() && response.body() != null) {
                                    @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body()
                                            .bytes();

                                    return RawMaven.parse(
                                            new Parser.Input(URI.create(uri), () -> new ByteArrayInputStream(responseBody)),
                                            null
                                    );
                                }
                            }

                            return null;
                        });

                        sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
                        return result.getData();
                    } catch (Exception e) {
                        logger.error("Failed to download {}:{}:{}:{}", groupId, artifactId, version, classifier, e);
                        sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                                .register(Metrics.globalRegistry));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private RawPom.Repository normalizeRepository(RawPom.Repository repository) {
        try {
            CacheResult<RawPom.Repository> result = mavenCache.computeRepository(repository, () -> {
                // FIXME add retry logic
                String url = repository.getUrl();
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
                        return new RawPom.Repository(
                                url,
                                repository.getReleases(),
                                repository.getSnapshots()
                        );
                    }

                    return null;
                }
            });

            return result.getData();
        } catch (Exception e) {
            return null;
        }
    }
}
