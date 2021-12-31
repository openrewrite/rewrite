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
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.CacheResult;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

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
            request -> httpClient.newCall(request).execute());

    // https://maven.apache.org/ref/3.6.3/maven-model-builder/super-pom.html
    private static final MavenRepository SUPER_POM_REPOSITORY = new MavenRepository("central",
            URI.create("https://repo.maven.apache.org/maven2"), true, false, true, null, null);

    private static final Set<String> unresolvablePoms = new HashSet<>();
    private static final CacheResult<RawMaven> UNAVAILABLE_POM = new CacheResult<>(CacheResult.State.Unavailable, null, -1);
    private static final CacheResult<MavenMetadata> UNAVAILABLE_METADATA = new CacheResult<>(CacheResult.State.Unavailable, null, -1);
    private static final CacheResult<MavenRepository> UNAVAILABLE_REPOSITORY = new CacheResult<>(CacheResult.State.Unavailable, null, -1);

    static {
        //noinspection ConstantConditions
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(MavenPomDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))) {
            reader.lines()
                    .filter(line -> !line.isEmpty())
                    .forEach(unresolvablePoms::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final MavenPomCache mavenPomCache;
    private final Map<Path, RawMaven> projectPoms;
    private final MavenExecutionContextView ctx;

    public MavenPomDownloader(MavenPomCache mavenPomCache, Map<Path, RawMaven> projectPoms, ExecutionContext ctx) {
        this.mavenPomCache = mavenPomCache;
        this.projectPoms = projectPoms;
        this.ctx = new MavenExecutionContextView(ctx);
    }

    @Nullable
    public MavenMetadata downloadMetadata(String groupId, String artifactId, Collection<MavenRepository> repositories) {
        return downloadMetadata(groupId, artifactId, null, repositories);
    }

    @Nullable
    public MavenMetadata downloadMetadata(String groupId, String artifactId, @Nullable String version,
                                          Collection<MavenRepository> repositories) {
        Timer.Sample sample = Timer.start();

        MavenMetadata mavenMetadata = MavenMetadata.EMPTY;
        Set<MavenRepository> repos = getDistinctNormalizedRepositories(repositories);
        for (MavenRepository repo : repos) {
            Timer.Builder timer = Timer.builder("rewrite.maven.download")
                    .tag("repo.id", repo.getUri().toString())
                    .tag("group.id", groupId)
                    .tag("artifact.id", artifactId)
                    .tag("type", "metadata");

            try {
                MavenPomCache.MetadataKey metadataKey = new MavenPomCache.MetadataKey(repo.getUri(), groupId, artifactId, version);
                MavenMetadata currentMetadata;
                CacheResult<MavenMetadata> result = mavenPomCache.getMavenMetadata(metadataKey);
                if (result == null) {
                    //Go download this bad boy.
                    currentMetadata = forceDownloadMetadata(groupId, artifactId, version, repo);
                    result = mavenPomCache.setMavenMetadata(metadataKey, currentMetadata, version != null && version.endsWith("SNAPSHOT"));
                } else {
                    currentMetadata = result.getData();
                }
                sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));

                if (currentMetadata != null) {
                    if (mavenMetadata == MavenMetadata.EMPTY) {
                        if (currentMetadata != MavenMetadata.EMPTY) {
                            mavenMetadata = currentMetadata;
                        }
                    } else if (currentMetadata != MavenMetadata.EMPTY) {
                        mavenMetadata = mergeMetadata(mavenMetadata, currentMetadata);
                    }
                }
            } catch (Exception e) {
                sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                        .register(Metrics.globalRegistry));
            }
        }
        return mavenMetadata;
    }

    @NonNull
    private MavenMetadata mergeMetadata(MavenMetadata m1, MavenMetadata m2) {
        return new MavenMetadata(new MavenMetadata.Versioning(
                Stream.concat(m1.getVersioning().getVersions().stream(), m2.getVersioning().getVersions().stream()).collect(toList()),
                Stream.concat(m1.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m1.getVersioning().getSnapshotVersions().stream(),
                        m2.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m2.getVersioning().getSnapshotVersions().stream()).collect(toList()),
                null
        ));
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

        if (containingPom != null && containingPom.getSourcePath() != null &&
                !StringUtils.isBlank(relativePath)) {
            Path folderContainingPom = containingPom.getSourcePath().getParent();
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

        Set<MavenRepository> normalizedRepos = getDistinctNormalizedRepositories(repositories);
        Set<MavenRepository> repos = normalizedRepos
                .stream()
                .filter(repo -> repo.acceptsVersion(version))
                .collect(toSet());
        for (MavenRepository repo : repos) {
            Timer.Builder timer = Timer.builder("rewrite.maven.download")
                    .tag("repo.id", repo.getUri().toString())
                    .tag("group.id", groupId)
                    .tag("artifact.id", artifactId)
                    .tag("type", "pom");

            try {
                //There are a few exceptional artifacts that will never be resolved by the repositories. This will always
                //result in an Unavailable response from the cache.
                String artifactCoordinates = groupId + ':' + artifactId + ':' + version;
                if (unresolvablePoms.contains(artifactCoordinates)) {
                    return null;
                }

                MavenPomCache.PomKey pomKey = new MavenPomCache.PomKey(repo.getUri(), groupId, artifactId, versionMaybeDatedSnapshot);
                CacheResult<RawMaven> result = mavenPomCache.getMaven(pomKey);

                if (result == null) {
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
                            RawMaven rawMaven = RawMaven.parse(
                                    new Parser.Input(inputPath, () -> new ByteArrayInputStream(responseBody), true),
                                    null,
                                    versionMaybeDatedSnapshot.equals(version) ? null : versionMaybeDatedSnapshot,
                                    ctx
                            ).withRepository(repo);
                            result = mavenPomCache.setMaven(pomKey, rawMaven, version.endsWith("-SNAPSHOT"));
                        } else {
                            throw new MavenDownloadingException("Download failure. Response code is [" + responseCode + "]. URI = " + uri);
                        }
                    } catch (Throwable throwable) {
                        mavenPomCache.setMaven(pomKey, null, version.endsWith("-SNAPSHOT"));
                        throw new MavenDownloadingException(throwable);
                    }
                }

                sample.stop(addTagsByResult(timer, result).register(Metrics.globalRegistry));
                return result.getData();
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

            MavenMetadata mavenMetadata;
            Set<MavenRepository> normalizedRepos = getDistinctNormalizedRepositories(repositories);
            Set<MavenRepository> repos = normalizedRepos
                    .stream()
                    .filter(repo -> repo.acceptsVersion(version))
                    .collect(toSet());

            mavenMetadata = downloadMetadata(groupId, artifactId, version, repos);
            if (mavenMetadata != null) {
                MavenMetadata.Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
                if (snapshot != null) {
                    return version.replaceFirst("SNAPSHOT$", snapshot.getTimestamp() + "-" + snapshot.getBuildNumber());
                }
            }
        }

        return version;
    }

    @NonNull
    private Set<MavenRepository> getDistinctNormalizedRepositories(Collection<MavenRepository> repositories) {
        List<MavenRepository> candidates = new ArrayList<>(repositories);
        candidates.add(SUPER_POM_REPOSITORY);
        Set<MavenRepository> normalizedRepositories = new HashSet<>();
        for (MavenRepository repo : candidates) {
            MavenRepository normalizedRepo = normalizeRepository(repo);
            if (normalizedRepo != null) {
                normalizedRepositories.add(normalizedRepo);
            }
        }
        return normalizedRepositories;
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
            result = mavenPomCache.getNormalizedRepository(repository);
            if (result == null) {
                // Always prefer to use https, fallback to http only if https isn't available
                // URLs are case-sensitive after the domain name, so it can be incorrect to lowerCase() a whole URL
                // This regex accepts any capitalization of the letters in "http"
                String httpsUri = repository.getUri().getScheme().equalsIgnoreCase("http") ?
                        repository.getUri().toString().replaceFirst("[hH][tT][tT][pP]://", "https://") :
                        repository.getUri().toString();

                Request.Builder request = applyAuthenticationToRequest(repository, new Request.Builder()
                        .url(httpsUri).get());
                MavenRepository normalized = null;
                try (Response ignored = sendRequest.apply(request.build())) {
                    normalized = repository.withUri(URI.create(httpsUri));
                } catch (Throwable t) {
                    if (!httpsUri.equals(originalUrl)) {
                        try (Response ignored = sendRequest.apply(request.url(originalUrl).build())) {
                            normalized = new MavenRepository(
                                    repository.getId(),
                                    URI.create(originalUrl),
                                    repository.isReleases(),
                                    repository.isSnapshots(),
                                    repository.getUsername(),
                                    repository.getPassword());
                        } catch (Throwable t2) {
                            //Fall through, normalized will be null.
                        }
                    }
                }
                if (normalized != null) {
                    result = mavenPomCache.setNormalizedRepository(repository, normalized);
                } else {
                    return null;
                }
            }
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
