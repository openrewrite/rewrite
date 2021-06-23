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
import okhttp3.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.CacheResult;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryCredentials;
import org.openrewrite.maven.tree.MavenRepositoryMirror;

import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class MavenPomDownloader {
    private static final RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(throwable -> throwable instanceof SocketTimeoutException ||
                    throwable instanceof TimeoutException)
            .build();

    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build();

    private static final Retry mavenDownloaderRetry = retryRegistry.retry("MavenDownloader");

    private static final CheckedFunction1<Request, Response> sendRequest = Retry.decorateCheckedFunction(
            mavenDownloaderRetry,
            (request) -> httpClient.newCall(request).execute());

    // https://maven.apache.org/ref/3.6.3/maven-model-builder/super-pom.html
    private static final MavenRepository SUPER_POM_REPOSITORY = new MavenRepository("central",
            URI.create("https://repo.maven.apache.org/maven2"), true, false, true, null, null);

    private final MavenPomCache mavenPomCache;
    private final Map<Path, RawMaven> projectPoms;
    private final MavenExecutionContextView ctx;

    public MavenPomDownloader(MavenPomCache mavenPomCache, Map<Path, RawMaven> projectPoms, ExecutionContext ctx) {
        this.mavenPomCache = mavenPomCache;
        this.projectPoms = projectPoms;
        this.ctx = new MavenExecutionContextView(ctx);
    }

    public MavenMetadata downloadMetadata(String groupId, String artifactId, Collection<MavenRepository> repositories) {
        Timer.Sample sample = Timer.start();

        return Stream.concat(repositories.stream().distinct(), Stream.of(SUPER_POM_REPOSITORY))
                .map(this::normalizeRepository)
                .distinct()
                .filter(Objects::nonNull)
                .map(repo -> {
                    Timer.Builder timer = Timer.builder("rewrite.maven.download")
                            .tag("repo.id", repo.getUri().toString())
                            .tag("group.id", groupId)
                            .tag("artifact.id", artifactId)
                            .tag("type", "metadata");

                    try {
                        CacheResult<MavenMetadata> result = mavenPomCache.computeMavenMetadata(repo.getUri(), groupId, artifactId,
                                () -> downloadMetadata(groupId, artifactId, null, singletonList(repo)));

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
                                emptyList(), // there will never be snapshot versions in metadata at the group:artifact level
                                null
                        ));
                    }
                });
    }

    @Nullable
    public MavenMetadata downloadMetadata(String groupId, String artifactId, @Nullable String version,
                                          Collection<MavenRepository> repositories) {
        return repositories.stream()
                .map(this::normalizeRepository)
                .distinct()
                .filter(Objects::nonNull)
                .map(repo -> forceDownloadMetadata(groupId, artifactId, version, repo))
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
                                emptyList(),
                                Stream.concat(m1.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m1.getVersioning().getSnapshotVersions().stream(),
                                        m2.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m2.getVersioning().getSnapshotVersions().stream()).collect(toList()),
                                null
                        ));
                    }
                });
    }

    @Nullable
    private MavenMetadata forceDownloadMetadata(String groupId, String artifactId, @Nullable String version, MavenRepository repo) {
        String uri = repo.getUri().toString() + "/" +
                groupId.replace('.', '/') + '/' +
                artifactId + '/' +
                (version == null ? "" : version + '/') +
                "maven-metadata.xml";

        Request.Builder request = applyAuthenticationToRequest(repo, new Request.Builder().url(uri).get());
        try (Response response = sendRequest.apply(request.build())) {
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
                             @Nullable String relativePath,
                             @Nullable RawMaven containingPom,
                             Collection<MavenRepository> repositories,
                             ExecutionContext ctx) {
        Map<MavenRepository, Exception> errors = new HashMap<>();

        String versionMaybeDatedSnapshot = datedSnapshotVersion(groupId, artifactId, version, repositories, ctx);
        if (versionMaybeDatedSnapshot == null) {
            return null;
        }

        Timer.Sample sample = Timer.start();

        // The pom being examined might be from a remote repository or a local filesystem.
        // First try to match the requested download with one of the project poms so we don't needlessly ping remote repos
        RawMaven goodEnoughMatch = null;
        for (RawMaven projectPom : projectPoms.values()) {
            if (groupId.equals(projectPom.getPom().getGroupId()) &&
                    artifactId.equals(projectPom.getPom().getArtifactId())) {
                // In a real project you'd never expect there to be more than one project pom with the same group/artifact but different version numbers
                // But in unit tests that supply all of the poms as "project" poms like these, there might be more than one entry
                if (version.equals(projectPom.getPom().getVersion())) {
                    return projectPom;
                }
                goodEnoughMatch = projectPom;
            }
        }
        if (goodEnoughMatch != null) {
            return goodEnoughMatch;
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

        List<MavenRepository> repos = Stream.concat(repositories.stream(), Stream.of(SUPER_POM_REPOSITORY))
                .map(this::normalizeRepository)
                .filter(Objects::nonNull)
                .distinct()
                .filter(repo -> repo.acceptsVersion(version))
                .collect(toList());
        for (MavenRepository repo : repos) {
            Timer.Builder timer = Timer.builder("rewrite.maven.download")
                    .tag("repo.id", repo.getUri().toString())
                    .tag("group.id", groupId)
                    .tag("artifact.id", artifactId)
                    .tag("type", "pom");

            try {
                CacheResult<RawMaven> result = mavenPomCache.computeMaven(repo.getUri(), groupId, artifactId,
                        versionMaybeDatedSnapshot, () -> {
                            String uri = URI.create(repo.getUri().toString()) + "/" +
                                    groupId.replace('.', '/') + '/' +
                                    artifactId + '/' +
                                    version + '/' +
                                    artifactId + '-' + versionMaybeDatedSnapshot + ".pom";

                            Request.Builder request = applyAuthenticationToRequest(repo, new Request.Builder().url(uri).get());
                            int responseCode;
                            try (Response response = sendRequest.apply(request.build())) {
                                responseCode = response.code();
                                if (response.isSuccessful() && response.body() != null) {
                                    @SuppressWarnings("ConstantConditions") byte[] responseBody = response.body()
                                            .bytes();

                                    // This path doesn't matter except for debugging/error logs where it might get displayed
                                    Path inputPath = Paths.get(groupId, artifactId, version);
                                    return RawMaven.parse(
                                            new Parser.Input(inputPath, () -> new ByteArrayInputStream(responseBody), true),
                                            null,
                                            versionMaybeDatedSnapshot.equals(version) ? null : versionMaybeDatedSnapshot,
                                            ctx
                                    ).withRepository(repo);
                                }
                            } catch (Throwable throwable) {
                                throw new MavenDownloadingException(throwable);
                            }
                            throw new MavenDownloadingException("HTTP response code: %d", responseCode, uri);
                        });

                sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
                if (result.getState() != CacheResult.State.Unavailable) {
                    return result.getData();
                }
            } catch (Exception e) {
                sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                        .register(Metrics.globalRegistry));
                errors.put(repo, e);
            }
        }

        if (!errors.isEmpty()) {
            String errorText = "Unable to download dependency " + groupId + ":" + artifactId + ":" + version + " from any of these repositories: \n" +
                    errors.entrySet().stream()
                            .map(entry -> "    Id: " + entry.getKey().getId() + ", URL: " + entry.getKey().getUri().toString() + ", cause: " + entry.getValue())
                            .collect(Collectors.joining("\n"));
            ctx.getOnError().accept(new MavenDownloadingException(errorText));
        }

        return null;
    }

    @Nullable
    private String datedSnapshotVersion(String groupId, String artifactId, String version, Collection<MavenRepository> repositories, ExecutionContext ctx) {
        if (version.endsWith("-SNAPSHOT")) {
            for (GroupArtifactVersion pinnedSnapshotVersion : new MavenExecutionContextView(ctx).getPinnedSnapshotVersions()) {
                if (pinnedSnapshotVersion.getDatedSnapshotVersion() != null &&
                        pinnedSnapshotVersion.getGroupId().equals(groupId) &&
                        pinnedSnapshotVersion.getArtifactId().equals(artifactId) &&
                        pinnedSnapshotVersion.getVersion().equals(version)) {
                    return pinnedSnapshotVersion.getDatedSnapshotVersion();
                }
            }

            MavenMetadata mavenMetadata = repositories.stream()
                    .map(this::normalizeRepository)
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(repo -> repo.acceptsVersion(version))
                    .map(repo -> downloadMetadata(groupId, artifactId, version, singletonList(repo)))
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
    protected MavenRepository normalizeRepository(MavenRepository originalRepository) {
        CacheResult<MavenRepository> result;
        try {
            MavenRepository repository = applyAuthenticationToRepository(applyMirrors(originalRepository));
            if (repository.isKnownToExist()) {
                return repository;
            }
            String originalUrl = repository.getUri().toString();
            result = mavenPomCache.computeRepository(repository, () -> {
                // Always prefer to use https, fallback to http only if https isn't available
                // URLs are case-sensitive after the domain name, so it can be incorrect to lowerCase() a whole URL
                // This regex accepts any capitalization of the letters in "http"
                String httpsUri = repository.getUri().getScheme().equalsIgnoreCase("http") ?
                        repository.getUri().toString().replaceFirst("[hH][tT][tT][pP]://", "https://") :
                        repository.getUri().toString();

                Request.Builder request = applyAuthenticationToRequest(repository, new Request.Builder()
                        .url(httpsUri).get());
                try (Response ignored = sendRequest.apply(request.build())) {
                    return repository.withUri(URI.create(httpsUri));
                } catch (Throwable t) {
                    // Fallback to http if https is unavailable and the original URL was an http URL
                    if (httpsUri.equals(originalUrl)) {
                        return null;
                    }
                    try (Response ignored = sendRequest.apply(request.url(originalUrl).build())) {
                        return new MavenRepository(
                                repository.getId(),
                                URI.create(originalUrl),
                                repository.isReleases(),
                                repository.isSnapshots(),
                                repository.getUsername(),
                                repository.getPassword());
                    } catch (Throwable t2) {
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            return null;
        }

        MavenRepository repo = result.getData();
        return repo == null ? null : applyAuthenticationToRepository(repo);
    }

    /**
     * Returns a Maven Repository with any applicable credentials as sourced from the ExecutionContext
     */
    private MavenRepository applyAuthenticationToRepository(MavenRepository repository) {
        return MavenRepositoryCredentials.apply(ctx.getCredentials(), repository);
    }

    /**
     * Returns a request builder with Authorization header set if the provided repository specifies credentials
     */
    private Request.Builder applyAuthenticationToRequest(MavenRepository repository, Request.Builder request) {
        if (repository.getUsername() != null && repository.getPassword() != null) {
            String credentials = Credentials.basic(repository.getUsername(), repository.getPassword());
            request.header("Authorization", credentials);
        }
        return request;
    }

    private MavenRepository applyMirrors(MavenRepository repository) {
        return MavenRepositoryMirror.apply(ctx.getMirrors(), repository);
    }
}
