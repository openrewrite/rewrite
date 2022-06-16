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
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Files;
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

    private static final Retry mavenDownloaderRetry = retryRegistry.retry("MavenDownloader");

    private final MavenPomCache mavenCache;
    private final Map<Path, Pom> projectPoms;
    private final MavenExecutionContextView ctx;
    private final HttpSender httpSender;

    private final CheckedFunction1<HttpSender.Request, byte[]> sendRequest;

    public MavenPomDownloader(Map<Path, Pom> projectPoms, ExecutionContext ctx) {
        this(projectPoms, new HttpUrlConnectionSender(), ctx);
    }

    public MavenPomDownloader(Map<Path, Pom> projectPoms, HttpSender httpSender, ExecutionContext ctx) {
        this.projectPoms = projectPoms;
        this.httpSender = httpSender;
        this.sendRequest = Retry.decorateCheckedFunction(
                mavenDownloaderRetry,
                request -> {
                    HttpSender.Response response = httpSender.send(request);
                    if (response.isSuccessful()) {
                        return response.getBodyAsBytes();
                    } else if (response.getCode() >= 400 && response.getCode() <= 404) {
                        //Throw a different exception for client-side failures to allow downstream callers to handle those
                        //differently.
                        throw new MavenClientSideException("Failed to download " + request.getUrl() + ": " + response.getCode());
                    }
                    throw new MavenDownloadingException("Failed to download " + request.getUrl() + ": " + response.getCode());
                });
        this.ctx = MavenExecutionContextView.view(ctx);
        this.mavenCache = this.ctx.getPomCache();
    }

    public MavenMetadata downloadMetadata(GroupArtifact groupArtifact, @Nullable ResolvedPom containingPom, List<MavenRepository> repositories) {
        return downloadMetadata(new GroupArtifactVersion(groupArtifact.getGroupId(), groupArtifact.getArtifactId(), null),
                containingPom,
                repositories);
    }

    public MavenMetadata downloadMetadata(GroupArtifactVersion gav, @Nullable ResolvedPom containingPom, List<MavenRepository> repositories) {
        if (gav.getGroupId() == null) {
            throw new MavenDownloadingException("Unable to download maven metadata because of a missing groupId.");
        }

        Timer.Sample sample = Timer.start();
        Timer.Builder timer = Timer.builder("rewrite.maven.download")
                .tag("group.id", gav.getGroupId())
                .tag("artifact.id", gav.getArtifactId())
                .tag("type", "metadata");

        MavenMetadata mavenMetadata = null;
        for (MavenRepository repo : distinctNormalizedRepositories(repositories, containingPom, null)) {
            String version = gav.getVersion();
            if (version != null) {
                if (version.endsWith("-SNAPSHOT") && !repo.isSnapshots()) {
                    continue;
                } else if ("RELEASE".equals(version) && !repo.isReleases()) {
                    continue;
                }
            }

            try {
                Optional<MavenMetadata> result = mavenCache.getMavenMetadata(URI.create(repo.getUri()), gav);
                if (result == null) {

                    String scheme = URI.create(repo.getUri()).getScheme();
                    String uri = repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                            gav.getGroupId().replace('.', '/') + '/' +
                            gav.getArtifactId() + '/' +
                            (gav.getVersion() == null ? "" : gav.getVersion() + '/') +
                            "maven-metadata.xml";

                    if ("file".equals(scheme)) {
                        // A maven repository can be expressed as a URI with a file scheme
                        Path path = Paths.get(URI.create(uri));
                        if (Files.exists(path)) {
                            result = Optional.of(MavenMetadata.parse(Files.readAllBytes(path)));
                        }
                    } else {
                        HttpSender.Request.Builder request = applyAuthenticationToRequest(repo, httpSender.get(uri));
                        try {
                            byte[] responseBody = sendRequest.apply(request.build());
                            result = Optional.of(MavenMetadata.parse(responseBody));
                            mavenCache.putMavenMetadata(URI.create(repo.getUri()), gav, result.get());
                        } catch (MavenClientSideException exception) {
                            // If the request resulted in a common client-side error, cache a null result, as this is
                            // likely to continue to occur.
                            mavenCache.putMavenMetadata(URI.create(repo.getUri()), gav, null);
                        } catch (Throwable ignored) {
                            // various kinds of connection failures
                        }
                    }
                }

                if (result != null && result.isPresent()) {
                    if (mavenMetadata == null) {
                        mavenMetadata = result.get();
                    } else {
                        mavenMetadata = mergeMetadata(mavenMetadata, result.get());
                    }
                }
            } catch (Exception ignored) {
                // any download or marshaling failure
            }
        }

        if (mavenMetadata == null) {
            sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));
            throw new MavenDownloadingException("Unable to download metadata " + gav);
        }

        sample.stop(timer.tags("outcome", "success").register(Metrics.globalRegistry));
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

    public Pom download(GroupArtifactVersion gav,
                        @Nullable String relativePath,
                        @Nullable ResolvedPom containingPom,
                        List<MavenRepository> repositories) throws MavenDownloadingException {
        if (gav.getGroupId() == null || gav.getArtifactId() == null || gav.getVersion() == null) {
            String errorText = "Unable to download dependency " + gav;
            if (containingPom != null) {
                ctx.getResolutionListener().downloadError(gav, containingPom.getRequested());
            }
            throw new MavenDownloadingException(errorText);
        }

        // The pom being examined might be from a remote repository or a local filesystem.
        // First try to match the requested download with one of the project POMs.
        for (Pom projectPom : projectPoms.values()) {
            if (gav.getGroupId().equals(projectPom.getGroupId()) &&
                    gav.getArtifactId().equals(projectPom.getArtifactId())) {
                // In a real project you'd never expect there to be more than one project pom with the same group/artifact but different version numbers
                // But in unit tests that supply all of the poms as "project" poms like these, there might be more than one entry
                if (gav.getVersion().equals(projectPom.getVersion())) {
                    return projectPom;
                }
                return projectPom;
            }
        }

        if (containingPom != null && containingPom.getRequested().getSourcePath() != null &&
                !StringUtils.isBlank(relativePath)) {
            Path folderContainingPom = containingPom.getRequested().getSourcePath().getParent();
            if (folderContainingPom != null) {
                Pom maybeLocalPom = projectPoms.get(folderContainingPom.resolve(Paths.get(relativePath, "pom.xml"))
                        .normalize());
                // Even poms published to remote repositories still contain relative paths to their parent poms
                // So double check that the GAV coordinates match so that we don't get a relative path from a remote
                // pom like ".." or "../.." which coincidentally _happens_ to have led to an unrelated pom on the local filesystem
                if (maybeLocalPom != null
                        && gav.getGroupId().equals(maybeLocalPom.getGroupId())
                        && gav.getArtifactId().equals(maybeLocalPom.getArtifactId())
                        && gav.getVersion().equals(maybeLocalPom.getVersion())) {
                    return maybeLocalPom;
                }
            }
        }

        Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, containingPom, gav.getVersion());

        Timer.Sample sample = Timer.start();
        Timer.Builder timer = Timer.builder("rewrite.maven.download")
                .tag("group.id", gav.getGroupId())
                .tag("artifact.id", gav.getArtifactId())
                .tag("type", "pom");

        String versionMaybeDatedSnapshot = datedSnapshotVersion(gav, containingPom, repositories, ctx);
        for (MavenRepository repo : normalizedRepos) {
            ResolvedGroupArtifactVersion resolvedGav = new ResolvedGroupArtifactVersion(
                    repo.getUri(), gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), versionMaybeDatedSnapshot);
            Optional<Pom> result = mavenCache.getPom(resolvedGav);
            if (result == null) {
                URI uri = URI.create(repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                        gav.getGroupId().replace('.', '/') + '/' +
                        gav.getArtifactId() + '/' +
                        gav.getVersion() + '/' +
                        gav.getArtifactId() + '-' + versionMaybeDatedSnapshot + ".pom");

                if ("file".equals(uri.getScheme())) {
                    Path inputPath = Paths.get(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());

                    try {
                        File f = new File(uri);

                        //NOTE: The pom may exist without a .jar artifact if the pom packaging is "pom"
                        if (!f.exists()) {
                            continue;
                        }

                        try (FileInputStream fis = new FileInputStream(f)) {
                            RawPom rawPom = RawPom.parse(fis, Objects.equals(versionMaybeDatedSnapshot, gav.getVersion()) ? null : versionMaybeDatedSnapshot);
                            Pom pom = rawPom.toPom(inputPath, repo).withGav(resolvedGav);

                            // so that the repository path is the same regardless of user name
                            pom = pom.withRepository(MavenRepository.MAVEN_LOCAL_USER_NEUTRAL);

                            if (!Objects.equals(versionMaybeDatedSnapshot, pom.getVersion())) {
                                pom = pom.withGav(pom.getGav().withDatedSnapshotVersion(versionMaybeDatedSnapshot));
                            }
                            mavenCache.putPom(resolvedGav, pom);
                            sample.stop(timer.tags("outcome", "from maven local").register(Metrics.globalRegistry));
                            return pom;
                        }
                    } catch (IOException e) {
                        // unable to read from maven local
                        throw new UncheckedIOException(e);
                    }
                }

                HttpSender.Request.Builder request = applyAuthenticationToRequest(repo, httpSender.get(uri.toString()));
                try {
                    byte[] responseBody = sendRequest.apply(request.build());

                    Path inputPath = Paths.get(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
                    RawPom rawPom = RawPom.parse(
                            new ByteArrayInputStream(responseBody),
                            Objects.equals(versionMaybeDatedSnapshot, gav.getVersion()) ? null : versionMaybeDatedSnapshot
                    );
                    Pom pom = rawPom.toPom(inputPath, repo).withGav(resolvedGav);
                    if (!Objects.equals(versionMaybeDatedSnapshot, pom.getVersion())) {
                        pom = pom.withGav(pom.getGav().withDatedSnapshotVersion(versionMaybeDatedSnapshot));
                    }
                    mavenCache.putPom(resolvedGav, pom);
                    sample.stop(timer.tags("outcome", "downloaded").register(Metrics.globalRegistry));
                    return pom;
                } catch (MavenClientSideException exception) {
                    mavenCache.putPom(resolvedGav, null);
                } catch (Throwable ignored) {
                    // various kinds of connection failures
                }
            } else if (result.isPresent()) {
                sample.stop(timer.tags("outcome", "cached").register(Metrics.globalRegistry));
                return result.get();
            }
        }

        sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));
        if (containingPom != null) {
            ctx.getResolutionListener().downloadError(gav, containingPom.getRequested());
        }

        throw new MavenDownloadingException("Unable to download dependency " + gav + " from the following repositories :\n  - "
                + normalizedRepos.stream().map(MavenRepository::getUri).collect(Collectors.joining("\n  - ")));
    }

    @Nullable
    private String datedSnapshotVersion(GroupArtifactVersion gav, @Nullable ResolvedPom containingPom, List<MavenRepository> repositories, ExecutionContext ctx) {
        if (gav.getVersion() != null && gav.getVersion().endsWith("-SNAPSHOT")) {
            for (ResolvedGroupArtifactVersion pinnedSnapshotVersion : new MavenExecutionContextView(ctx).getPinnedSnapshotVersions()) {
                if (pinnedSnapshotVersion.getDatedSnapshotVersion() != null &&
                        pinnedSnapshotVersion.getGroupId().equals(gav.getGroupId()) &&
                        pinnedSnapshotVersion.getArtifactId().equals(gav.getArtifactId()) &&
                        pinnedSnapshotVersion.getVersion().equals(gav.getVersion())) {
                    return pinnedSnapshotVersion.getDatedSnapshotVersion();
                }
            }

            MavenMetadata mavenMetadata;
            try {
                mavenMetadata = downloadMetadata(gav, containingPom, repositories);
            } catch (MavenDownloadingException e) {
                //This can happen if the artifact only exists in the local maven cache. In this case, just return the original
                return gav.getVersion();
            }
            MavenMetadata.Snapshot snapshot = mavenMetadata.getVersioning().getSnapshot();
            if (snapshot != null) {
                return gav.getVersion().replaceFirst("SNAPSHOT$", snapshot.getTimestamp() + "-" + snapshot.getBuildNumber());
            }
        }

        return gav.getVersion();
    }

    private Collection<MavenRepository> distinctNormalizedRepositories(List<MavenRepository> repositories,
                                                                       @Nullable ResolvedPom containingPom,
                                                                       @Nullable String acceptsVersion) {
        Set<MavenRepository> normalizedRepositories = new LinkedHashSet<>();
        normalizedRepositories.add(MavenRepository.MAVEN_LOCAL);

        for (MavenRepository repo : repositories) {
            MavenRepository normalizedRepo = normalizeRepository(repo, containingPom);
            if (normalizedRepo != null && (acceptsVersion == null || normalizedRepo.acceptsVersion(acceptsVersion))) {
                normalizedRepositories.add(normalizedRepo);
            }
        }

        // repositories from maven settings
        for (MavenRepository repo : ctx.getRepositories()) {
            MavenRepository normalizedRepo = normalizeRepository(repo, containingPom);
            if (normalizedRepo != null && (acceptsVersion == null || normalizedRepo.acceptsVersion(acceptsVersion))) {
                normalizedRepositories.add(normalizedRepo);
            }
        }

        normalizedRepositories.add(normalizeRepository(MavenRepository.MAVEN_CENTRAL, containingPom));
        return normalizedRepositories;
    }

    @Nullable
    protected MavenRepository normalizeRepository(MavenRepository originalRepository, @Nullable ResolvedPom containingPom) {
        Optional<MavenRepository> result = null;
        MavenRepository repository = applyAuthenticationToRepository(applyMirrors(originalRepository));
        if (containingPom != null) {
            repository = repository.withUri(containingPom.getValue(repository.getUri()));
        }
        try {
            if (repository.isKnownToExist()) {
                return repository;
            }
            String originalUrl = repository.getUri();
            if ("file".equals(URI.create(originalUrl).getScheme())) {
                return repository;
            }
            result = mavenCache.getNormalizedRepository(repository);
            if (result == null) {
                if (!repository.getUri().toLowerCase().startsWith("http")) {
                    // can be s3 among potentially other types for which there is a maven wagon implementation
                    return null;
                }

                // Always prefer to use https, fallback to http only if https isn't available
                // URLs are case-sensitive after the domain name, so it can be incorrect to lowerCase() a whole URL
                // This regex accepts any capitalization of the letters in "http"
                String httpsUri = repository.getUri().toLowerCase().startsWith("http:") ?
                        repository.getUri().replaceFirst("[hH][tT][tT][pP]://", "https://") :
                        repository.getUri();

                HttpSender.Request.Builder request = applyAuthenticationToRequest(repository, httpSender.get(httpsUri));
                MavenRepository normalized = null;
                try {
                    sendRequest.apply(request.build());
                    normalized = repository.withUri(httpsUri);
                } catch (MavenDownloadingException exception) {
                    //Response was returned from the server, but it was not a 200 OK. The server therefore exists.
                    normalized = repository.withUri(httpsUri);
                } catch (Throwable t) {
                    if (!httpsUri.equals(originalUrl)) {
                        try {
                            sendRequest.apply(request.url(originalUrl).build());
                            normalized = new MavenRepository(
                                    repository.getId(),
                                    originalUrl,
                                    repository.isReleases(),
                                    repository.isSnapshots(),
                                    repository.getUsername(),
                                    repository.getPassword());
                        } catch (MavenDownloadingException exception) {
                            //Response was returned from the server, but it was not a 200 OK. The server therefore exists.
                            normalized = new MavenRepository(
                                    repository.getId(),
                                    originalUrl,
                                    repository.isReleases(),
                                    repository.isSnapshots(),
                                    repository.getUsername(),
                                    repository.getPassword());
                        } catch (Throwable ignored) {
                            // ok to fall through here and cache a null
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
    private HttpSender.Request.Builder applyAuthenticationToRequest(MavenRepository repository, HttpSender.Request.Builder request) {
        if (isViableCredential(repository.getUsername()) && isViableCredential(repository.getPassword())) {
            return request.withBasicAuthentication(repository.getUsername(), repository.getPassword());
        }
        return request;
    }

    private boolean isViableCredential(@Nullable String value) {
        return value != null && !isUnresolvedProperty(value);
    }

    private boolean isUnresolvedProperty(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    private MavenRepository applyMirrors(MavenRepository repository) {
        return MavenRepositoryMirror.apply(ctx.getMirrors(), repository);
    }
}
