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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("OptionalAssignedToNull")
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

    private final MavenPomCache mavenCache;
    private final Map<Path, Pom> projectPoms;
    private final MavenExecutionContextView ctx;

    public MavenPomDownloader(Map<Path, Pom> projectPoms, ExecutionContext ctx) {
        this.projectPoms = projectPoms;
        if (ctx instanceof MavenExecutionContextView) {
            this.ctx = (MavenExecutionContextView) ctx;
        } else {
            this.ctx = new MavenExecutionContextView(ctx);
        }
        this.mavenCache = this.ctx.getPomCache();
    }

    @Nullable
    public MavenMetadata downloadMetadata(GroupArtifact groupArtifact, List<MavenRepository> repositories) {
        return downloadMetadata(new GroupArtifactVersion(groupArtifact.getGroupId(), groupArtifact.getArtifactId(), null),
                repositories);
    }

    @Nullable
    public MavenMetadata downloadMetadata(GroupArtifactVersion gav, List<MavenRepository> repositories) {
        Timer.Sample sample = Timer.start();

        MavenMetadata mavenMetadata = MavenMetadata.EMPTY;
        Collection<MavenRepository> repos = distinctNormalizedRepositories(repositories, null);
        for (MavenRepository repo : repos) {
            Timer.Builder timer = Timer.builder("rewrite.maven.download")
                    .tag("repo.id", repo.getUri().toString())
                    .tag("group.id", gav.getGroupId())
                    .tag("artifact.id", gav.getArtifactId())
                    .tag("type", "metadata");

            try {
                Optional<MavenMetadata> result = mavenCache.getMavenMetadata(repo.getUri(), gav);
                if (result == null) {
                    result = Optional.ofNullable(forceDownloadMetadata(gav, repo));
                    timer = timer.tags("outcome", result.isPresent() ? "unavailable" : "downloaded", "exception", "none");
                    mavenCache.putMavenMetadata(repo.getUri(), gav, result.orElse(null));
                } else {
                    timer = timer.tags("outcome", "cached", "exception", "none");
                }
                sample.stop(timer.register(Metrics.globalRegistry));

                if (result.isPresent()) {
                    if (mavenMetadata == MavenMetadata.EMPTY) {
                        if (result.get() != MavenMetadata.EMPTY) {
                            mavenMetadata = result.get();
                        }
                    } else {
                        mavenMetadata = mergeMetadata(mavenMetadata, result.get());
                    }
                }
            } catch (Exception e) {
                sample.stop(timer.tags("outcome", "error", "exception", e.getClass().getName())
                        .register(Metrics.globalRegistry));
                ctx.getOnError().accept(e);
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
    private MavenMetadata forceDownloadMetadata(GroupArtifactVersion gav, MavenRepository repo) {
        String uri = repo.getUri().toString() + "/" +
                gav.getGroupId().replace('.', '/') + '/' +
                gav.getArtifactId() + '/' +
                (gav.getVersion() == null ? "" : gav.getVersion() + '/') +
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

    public Pom download(GroupArtifactVersion gav,
                        @Nullable String relativePath,
                        @Nullable Pom containingPom,
                        List<MavenRepository> repositories,
                        ExecutionContext ctx) throws MavenDownloadingException {

        Map<MavenRepository, String> errors = new HashMap<>();

        String versionMaybeDatedSnapshot = datedSnapshotVersion(gav, repositories, ctx);
        Timer.Sample sample = Timer.start();

        // The pom being examined might be from a remote repository or a local filesystem.
        // First try to match the requested download with one of the project poms so we don't needlessly ping remote repos
        Pom projectMatch = null;
        for (Pom projectPom : projectPoms.values()) {
            if (gav.getGroupId().equals(projectPom.getGroupId()) &&
                    gav.getArtifactId().equals(projectPom.getArtifactId())) {
                // In a real project you'd never expect there to be more than one project pom with the same group/artifact but different version numbers
                // But in unit tests that supply all of the poms as "project" poms like these, there might be more than one entry
                assert gav.getVersion() != null;
                if (gav.getVersion().equals(projectPom.getVersion())) {
                    return projectPom;
                }
                projectMatch = projectPom;
            }
        }
        if (projectMatch != null) {
            return projectMatch;
        }

        if (containingPom != null && containingPom.getSourcePath() != null &&
                !StringUtils.isBlank(relativePath)) {
            Path folderContainingPom = containingPom.getSourcePath().getParent();
            if (folderContainingPom != null) {
                Pom maybeLocalPom = projectPoms.get(folderContainingPom.resolve(Paths.get(relativePath, "pom.xml"))
                        .normalize());
                // Even poms published to remote repositories still contain relative paths to their parent poms
                // So double check that the GAV coordinates match so that we don't get a relative path from a remote
                // pom like ".." or "../.." which coincidentally _happens_ to have led to an unrelated pom on the local filesystem
                assert gav.getVersion() != null;
                if (maybeLocalPom != null
                        && gav.getGroupId().equals(maybeLocalPom.getGroupId())
                        && gav.getArtifactId().equals(maybeLocalPom.getArtifactId())
                        && gav.getVersion().equals(maybeLocalPom.getVersion())) {
                    return maybeLocalPom;
                }
            }
        }

        Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, gav.getVersion());
        for (MavenRepository repo : normalizedRepos) {
            Timer.Builder timer = Timer.builder("rewrite.maven.download")
                    .tag("repo.id", repo.getUri().toString())
                    .tag("group.id", gav.getGroupId())
                    .tag("artifact.id", gav.getArtifactId())
                    .tag("type", "pom");

            ResolvedGroupArtifactVersion resolvedGav = new ResolvedGroupArtifactVersion(
                    repo.getUri(), gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), versionMaybeDatedSnapshot);
            Optional<Pom> result = mavenCache.getPom(resolvedGav);

            if (result == null) {
                String uri = URI.create(repo.getUri().toString()) + "/" +
                        gav.getGroupId().replace('.', '/') + '/' +
                        gav.getArtifactId() + '/' +
                        gav.getVersion() + '/' +
                        gav.getArtifactId() + '-' + versionMaybeDatedSnapshot + ".pom";

                Request.Builder request = applyAuthenticationToRequest(repo, new Request.Builder().url(uri).get());
                int responseCode;
                try (Response response = sendRequest.apply(request.build())) {
                    responseCode = response.code();
                    if (response.isSuccessful() && response.body() != null) {
                        //noinspection ConstantConditions
                        byte[] responseBody = response.body().bytes();

                        // This path doesn't matter except for debugging/error logs where it might get displayed
                        Path inputPath = Paths.get(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());

                        RawPom rawPom = RawPom.parse(
                                new ByteArrayInputStream(responseBody),
                                versionMaybeDatedSnapshot.equals(gav.getVersion()) ? null : versionMaybeDatedSnapshot
                        );

                        Pom pom = rawPom.toPom(inputPath, repo);
                        if (!versionMaybeDatedSnapshot.equals(pom.getVersion())) {
                            pom = pom.withGav(pom.getGav().withDatedSnapshotVersion(versionMaybeDatedSnapshot));
                        }
                        mavenCache.putPom(resolvedGav, pom);
                        timer = timer.tags("outcome", pom == null ? "unavailable" : "downloaded", "exception", "none");
                        result = Optional.ofNullable(pom);
                    } else {
                        errors.put(repo, "Download failure. Response code is [" + responseCode + "].");
                        mavenCache.putPom(resolvedGav, null);
                        timer = timer.tags("outcome", "unavailable", "exception", "none");
                    }
                } catch (Throwable throwable) {
                    errors.put(repo, "Download failure. " + throwable.getMessage());
                    timer = timer.tags("outcome", "unavailable", "exception", "none");
                }
            }
            sample.stop(timer.register(Metrics.globalRegistry));
            if (result != null && result.isPresent()) {
                return result.get();
            }
        }

        String errorText = "Unable to download dependency " + gav + " from any of these repositories: \n" +
                errors.entrySet().stream()
                        .map(entry -> "    Id: " + entry.getKey().getId() + ", URL: " + entry.getKey().getUri().toString() + ", cause: " + entry.getValue())
                        .collect(Collectors.joining("\n"));
        throw new MavenDownloadingException(errorText);
    }

    private String datedSnapshotVersion(GroupArtifactVersion gav, List<MavenRepository> repositories, ExecutionContext ctx) {
        assert gav.getVersion() != null;
        if (gav.getVersion().endsWith("-SNAPSHOT")) {
            for (ResolvedGroupArtifactVersion pinnedSnapshotVersion : new MavenExecutionContextView(ctx).getPinnedSnapshotVersions()) {
                if (pinnedSnapshotVersion.getDatedSnapshotVersion() != null &&
                        pinnedSnapshotVersion.getGroupId().equals(gav.getGroupId()) &&
                        pinnedSnapshotVersion.getArtifactId().equals(gav.getArtifactId()) &&
                        pinnedSnapshotVersion.getVersion().equals(gav.getVersion())) {
                    return pinnedSnapshotVersion.getDatedSnapshotVersion();
                }
            }

            MavenMetadata mavenMetadata;
            Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, gav.getVersion());
            mavenMetadata = downloadMetadata(gav, repositories);
            if (mavenMetadata != null) {
                MavenMetadata.Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
                if (snapshot != null) {
                    return gav.getVersion().replaceFirst("SNAPSHOT$", snapshot.getTimestamp() + "-" + snapshot.getBuildNumber());
                }
            }
        }

        return gav.getVersion();
    }

    private Collection<MavenRepository> distinctNormalizedRepositories(List<MavenRepository> repositories, @Nullable String acceptsVersion) {
        Set<MavenRepository> normalizedRepositories = new LinkedHashSet<>();
        for (MavenRepository repo : repositories) {
            MavenRepository normalizedRepo = normalizeRepository(repo);
            if (normalizedRepo != null && (acceptsVersion == null || normalizedRepo.acceptsVersion(acceptsVersion))) {
                normalizedRepositories.add(normalizedRepo);
            }
        }
        normalizedRepositories.add(normalizeRepository(MavenRepository.MAVEN_CENTRAL));
        return normalizedRepositories;
    }

    @Nullable
    protected MavenRepository normalizeRepository(MavenRepository originalRepository) {
        Optional<MavenRepository> result = null;
        MavenRepository repository = applyAuthenticationToRepository(applyMirrors(originalRepository));
        try {
            if (repository.isKnownToExist()) {
                return repository;
            }
            String originalUrl = repository.getUri().toString();
            result = mavenCache.getNormalizedRepository(repository);
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
                            ctx.getOnError().accept(t2);
                            //ok to fall through here and cache a null. the TTL for null repositories can be set to
                            //have it periodically retry the repo.
                        }
                    }
                }
                mavenCache.putNormalizedRepository(repository, normalized);
                result = Optional.ofNullable(normalized);
            }
        } catch (Exception e) {
            ctx.getOnError().accept(e);
            mavenCache.putNormalizedRepository(repository, null);
        }

        return result == null || !result.isPresent() ? null : applyAuthenticationToRepository(result.get());
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
