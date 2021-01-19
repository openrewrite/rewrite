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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.CheckedFunction1;
import io.vavr.Function1;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.CacheResult;
import org.openrewrite.maven.cache.MavenCache;
import org.openrewrite.maven.tree.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MavenDownloader {
    private static final Logger logger = LoggerFactory.getLogger(MavenDownloader.class);

    private static final RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(SocketTimeoutException.class, TimeoutException.class)
            .build();
    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(1, TimeUnit.MINUTES)
            .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build();

    private static final Retry mavenDownloaderRetry = retryRegistry.retry("MavenDownloader");

    private static final CheckedFunction1<Request, Response> sendRequest = Retry.decorateCheckedFunction(
            mavenDownloaderRetry,
            (request) -> httpClient.newCall(request).execute());

    private static final Function1<String, Response> sendHeadFallbackToGet = Retry.decorateCheckedFunction(
            mavenDownloaderRetry,
            (String url) ->  httpClient.newCall(new Request.Builder().url(url).head().build()).execute()
    ).recover((throwable) -> (String url) -> {
        try {
            return httpClient.newCall(new Request.Builder().url(url).get().build()).execute();
        } catch (IOException e) {
            return null;
        }
    });

    // https://maven.apache.org/ref/3.6.3/maven-model-builder/super-pom.html
    private static final RawRepositories.Repository SUPER_POM_REPOSITORY = new RawRepositories.Repository("central", "https://repo.maven.apache.org/maven2",
            new RawRepositories.ArtifactPolicy(true), new RawRepositories.ArtifactPolicy(false));

    private final MavenCache mavenCache;
    private final Map<Path, RawMaven> projectPoms;
    @Nullable
    private final MavenSettings settings;

    /**
     * Any visitor constructing a MavenDownloader should provide the MavenSettings from {@link Maven#getSettings()}.
     * Failing to do this will result in being unable to download dependencies or their metadata when information from settings.xml is required to access artifact
     * repositories.
     */
    public MavenDownloader(MavenCache mavenCache, Map<Path, RawMaven> projectPoms, @Nullable MavenSettings settings) {
        this.mavenCache = mavenCache;
        this.projectPoms = projectPoms;
        this.settings = settings;
    }

    public MavenMetadata downloadMetadata(String groupId, String artifactId,
                                          List<RawRepositories.Repository> repositories) {
        Timer.Sample sample = Timer.start();

        return Stream.concat(repositories.stream().distinct(), Stream.of(SUPER_POM_REPOSITORY))
                .map(this::normalizeRepository)
                .distinct()
                .filter(Objects::nonNull)
                .map(repo -> {
                    Timer.Builder timer = Timer.builder("rewrite.maven.download")
                            .tag("group.id", groupId)
                            .tag("artifact.id", artifactId)
                            .tag("type", "metadata");

                    try {
                        CacheResult<MavenMetadata> result = mavenCache.computeMavenMetadata(URI.create(repo.getUrl()), groupId, artifactId,
                                () -> forceDownloadMetadata(groupId, artifactId, null, repo));

                        sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
                        return result.getData();
                    } catch (Exception e) {
                        sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                                .register(Metrics.globalRegistry));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .reduce(MavenMetadata.EMPTY, (m1, m2) -> {
                    if (m1 == MavenMetadata.EMPTY) {
                        if (m2 == MavenMetadata.EMPTY) {
                            return m1;
                        } else {
                            return m2;
                        }
                    } else if (m2 == MavenMetadata.EMPTY) {
                        return m1;
                    } else {
                        return new MavenMetadata(new MavenMetadata.Versioning(
                                Stream.concat(m1.getVersioning().getVersions().stream(),
                                        m2.getVersioning().getVersions().stream()).collect(toList()),
                                null
                        ));
                    }
                });
    }

    @Nullable
    private MavenMetadata forceDownloadMetadata(String groupId, String artifactId,
                                                @Nullable String version, RawRepositories.Repository repo) throws IOException {
        logger.debug("Resolving {}:{} metadata from {}", groupId, artifactId, repo.getUrl());

        String uri = repo.getUrl() + "/" +
                groupId.replace('.', '/') + '/' +
                artifactId + '/' +
                (version == null ? "" : version + '/') +
                "maven-metadata.xml";

        Request request = new Request.Builder().url(uri).get().build();
        try (Response response =  sendRequest.apply(request)) {
            if (response.isSuccessful() && response.body() != null) {
                @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body()
                        .bytes();

                return MavenMetadata.parse(responseBody);
            }
        } catch (Throwable throwable) {
            return null;
        }

        return null;
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
                             List<RawRepositories.Repository> repositories) {

        String versionMaybeDatedSnapshot = findDatedSnapshotVersionIfNecessary(groupId, artifactId, version, repositories);
        if (versionMaybeDatedSnapshot == null) {
            return null;
        }

        Timer.Sample sample = Timer.start();

        // The pom being examined might be from a remote repository or a local filesystem.
        // First try to match the requested download with one of the project poms so we don't needlessly ping remote repos
        for (RawMaven projectPom : projectPoms.values()) {
            if (groupId.equals(projectPom.getPom().getGroupId()) &&
                    artifactId.equals(projectPom.getPom().getArtifactId())) {
                return projectPom;
            }
        }
        if (containingPom != null && !StringUtils.isBlank(relativePath)) {
            Path folderContainingPom = containingPom.getSourcePath()
                    .getParent();
            if (folderContainingPom != null) {
                RawMaven maybeLocalPom = projectPoms.get(folderContainingPom.resolve(Paths.get(relativePath, "pom.xml"))
                        .normalize());
                // Even poms published to remote repositories still contain relative paths to their parent poms
                // So double check that the GAV coordinates match so that we don't get a relative path from a remote
                // pom like ".." or "../.." which coincidentally _happens_ to have led to an unrelated pom on the local filesystem
                if (maybeLocalPom != null
                        && groupId.equals(maybeLocalPom.getPom().getGroupId())
                        && artifactId.equals(maybeLocalPom.getPom().getArtifactId())
                        && version.equals(maybeLocalPom.getPom().getVersion())) {
                    return maybeLocalPom;
                }
            }
        }

        final Set<RawRepositories.Repository> validRepositories = Stream.concat(repositories.stream(), Stream.of(SUPER_POM_REPOSITORY))
                .map(this::normalizeRepository)
                .filter(Objects::nonNull)
                .distinct()
                .filter(repo -> repo.acceptsVersion(version)).collect(Collectors.toSet());

        Timer.Builder timer = Timer.builder("rewrite.maven.download")
                .tag("group.id", groupId)
                .tag("artifact.id", artifactId)
                .tag("type", "pom");

        try {

            CacheResult<RawMaven> result = mavenCache.computeMaven(null, groupId, artifactId,
                    versionMaybeDatedSnapshot, () -> {

                        for (RawRepositories.Repository repo : validRepositories) {
                            String uri = URI.create(repo.getUrl()) + "/" +
                                    groupId.replace('.', '/') + '/' +
                                    artifactId + '/' +
                                    version + '/' +
                                    artifactId + '-' + versionMaybeDatedSnapshot + ".pom";

                            Request request = new Request.Builder().url(uri).get().build();
                            try (Response response = sendRequest.apply(request)) {
                                if (response.isSuccessful() && response.body() != null) {
                                    @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body()
                                            .bytes();

                                    // This path doesn't matter except for debugging/error logs where it might get displayed
                                    Path inputPath = Paths.get(groupId, artifactId, version);
                                    return RawMaven.parse(
                                            new Parser.Input(inputPath, () -> new ByteArrayInputStream(responseBody), true),
                                            null,
                                            versionMaybeDatedSnapshot.equals(version) ? null : versionMaybeDatedSnapshot
                                    );
                                }
                            } catch (Throwable throwable) {
                                //If an exception happens, just skip to the next repo.
                            }
                        }
                        return null;
                    });
            sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
            return result.getData();
        } catch (Exception e) {
            logger.debug("Failed to download {}:{}:{}:{}", groupId, artifactId, version, classifier, e);
            sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                    .register(Metrics.globalRegistry));
        }
        return null;
    }

    @Nullable
    private String findDatedSnapshotVersionIfNecessary(String groupId, String artifactId, String version, List<RawRepositories.Repository> repositories) {
        if (version.endsWith("-SNAPSHOT")) {
            MavenMetadata mavenMetadata = repositories.stream()
                    .map(this::normalizeRepository)
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(repo -> repo.acceptsVersion(version))
                    .map(repo -> {
                        try {
                            return forceDownloadMetadata(groupId, artifactId, version, repo);
                        } catch (IOException e) {
                            logger.debug("Failed to download snapshot metadata for {}:{}:{}", groupId, artifactId, version);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (mavenMetadata != null) {
                MavenMetadata.Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
                if (snapshot == null) {
                    return null;
                }
                return version.replaceFirst("SNAPSHOT$", snapshot.getTimestamp() + "-" + snapshot.getBuildNumber());
            }
        }

        return version;
    }

    @Nullable
    private RawRepositories.Repository normalizeRepository(RawRepositories.Repository repository) {

        RawRepositories.Repository repoWithMirrors = applyMirrors(repository);
        CacheResult<RawRepositories.Repository> result;
        try {
            result = mavenCache.computeRepository(repoWithMirrors, () -> {
                String url = repoWithMirrors.getUrl();
                try (Response response = sendHeadFallbackToGet.apply(url)) {
                    if (url.toLowerCase().contains("http://")) {
                        return normalizeRepository(
                                new RawRepositories.Repository(
                                        repoWithMirrors.getId(),
                                        url.toLowerCase().replace("http://", "https://"),
                                        repoWithMirrors.getReleases(),
                                        repoWithMirrors.getSnapshots()
                                )
                        );
                    } else if (response.isSuccessful()) {
                        return new RawRepositories.Repository(
                                repoWithMirrors.getId(),
                                url,
                                repoWithMirrors.getReleases(),
                                repoWithMirrors.getSnapshots()
                        );
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            return null;
        }

        return result.getData();
    }

    private RawRepositories.Repository applyMirrors(RawRepositories.Repository repository) {
        if (settings == null) {
            return repository;
        } else {
            return settings.applyMirrors(repository);
        }
    }
}
