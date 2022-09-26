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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.vavr.CheckedFunction1;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
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
        this(projectPoms, HttpSenderExecutionContextView.view(ctx).getHttpSender(), ctx);
    }

    /**
     * @param projectPoms Project poms on disk.
     * @param httpSender The HTTP sender.
     * @param ctx The execution context.
     * @deprecated Use {@link #MavenPomDownloader(Map, ExecutionContext)} instead.
     */
    @Deprecated
    public MavenPomDownloader(Map<Path, Pom> projectPoms, HttpSender httpSender, ExecutionContext ctx) {
        this.projectPoms = projectPoms;
        this.httpSender = httpSender;
        this.sendRequest = Retry.decorateCheckedFunction(
                mavenDownloaderRetry,
                request -> {
                    try (HttpSender.Response response = httpSender.send(request)) {
                        if (response.isSuccessful()) {
                            return response.getBodyAsBytes();
                        } else if (response.getCode() >= 400 && response.getCode() <= 404) {
                            //Throw a different exception for client-side failures to allow downstream callers to handle those
                            //differently.
                            throw new MavenClientSideException("Failed to download " + request.getUrl(), response.getCode());
                        }
                        throw new MavenDownloadingException("Failed to download " + request.getUrl() + ": " + response.getCode());
                    }
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
        Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, containingPom, null);
        List<String> downloadFailures = new ArrayList<>();
        for (MavenRepository repo : normalizedRepos) {
            String version = gav.getVersion();
            if (version != null) {
                if (version.endsWith("-SNAPSHOT") && !repo.isSnapshots()) {
                    downloadFailures.add("[" + repo.getUri() + "] Version is a snapshot but the repository does not support snapshots.");
                    continue;
                } else if (!version.endsWith("-SNAPSHOT") && !repo.isReleases()) {
                    downloadFailures.add("[" + repo.getUri() + "] Version is a release but the repository does not support releases.");
                    continue;
                }
            }

            Optional<MavenMetadata> result = mavenCache.getMavenMetadata(URI.create(repo.getUri()), gav);
            if (result == null) {
                // Not in the cache, attempt to download it.
                boolean cacheEmptyResult = false;
                try {
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
                        byte[] responseBody = requestAsAuthenticatedOrAnonymous(repo, uri);
                        result = Optional.of(MavenMetadata.parse(responseBody));
                    }
                } catch (Throwable exception) {
                    downloadFailures.add("[" + repo.getUri() + "] Unable to download metadata. " + exception.getMessage());
                    if (exception instanceof MavenClientSideException) {
                        //If we have a 400-404, cache an empty result.
                        cacheEmptyResult = true;
                    }
                }

                if (result == null) {
                    // If no result was found in the repository, attempt to derive the metadata from the repository.
                    try {
                        MavenMetadata derivedMeta = deriveMetadata(gav, repo);
                        if (derivedMeta != null) {
                            Counter.builder("rewrite.maven.derived.metatdata")
                                    .tag("repositoryUri", repo.getUri())
                                    .tag("group", gav.getGroupId())
                                    .tag("artifact", gav.getArtifactId())
                                    .register(Metrics.globalRegistry);
                            result = Optional.of(derivedMeta);
                        }
                    } catch (Throwable exception) {
                        downloadFailures.add("[" + repo.getUri() + "] Unable to derive metadata. " + exception.getMessage());
                    }
                }
                if (result == null && cacheEmptyResult) {
                    // If there was no fatal failure while attempting to find metadata and there was no metadata retrieved
                    // from the current repo, cache an empty result.
                    mavenCache.putMavenMetadata(URI.create(repo.getUri()), gav, null);
                }
            } else if (!result.isPresent()) {
                downloadFailures.add("[" + repo.getUri() + "] Cached empty result.");
            }

            // Merge metadata from repository and cache metadata result.
            if (result != null && result.isPresent()) {
                if (mavenMetadata == null) {
                    mavenMetadata = result.get();
                } else {
                    mavenMetadata = mergeMetadata(mavenMetadata, result.get());
                }
                mavenCache.putMavenMetadata(URI.create(repo.getUri()), gav, result.get());
            }
        }

        if (mavenMetadata == null) {
            sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));

            StringBuilder message = new StringBuilder("Unable to Download metadata for [").append(gav).append("] from the following repositories :");

            for (MavenRepository repository : normalizedRepos) {
                message.append("\n  ").append(repository.getUri());
            }

            message.append("\nMetadata download failures:");
            for (String failure : downloadFailures) {
                message.append("\n  ").append(failure);
            }
            throw new MavenDownloadingException(message.toString());
        }

        sample.stop(timer.tags("outcome", "success").register(Metrics.globalRegistry));
        return mavenMetadata;
    }

    /**
     * This method will attempt to generate the metadata by navigating the repository's directory structure.
     * Currently, the only repository I can find that has missing maven-metadata.xml is Nexus. Both Artifactory
     * and JitPack appear to always publish the metadata. So this method is currently tailored towards Nexus and how
     * it publishes html index pages.
     *
     * @param gav The artifact coordinates that will be derived.
     * @param repo The repository that will be queried for directory results
     * @return Metadata or null if the metadata cannot be derived.
     */
    @Nullable
    private MavenMetadata deriveMetadata(GroupArtifactVersion gav, MavenRepository repo) throws Throwable {
        if ((repo.getDeriveMetadataIfMissing() != null && !repo.getDeriveMetadataIfMissing()) || gav.getVersion() != null) {
            // Do not derive metadata if we cannot navigate/browse the artifacts.
            // Do not derive metadata if a specific version has been defined.
            return null;
        }

        String scheme = URI.create(repo.getUri()).getScheme();
        String uri = repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                     gav.getGroupId().replace('.', '/') + '/' +
                     gav.getArtifactId();

        try {
            MavenMetadata.Versioning versioning;
            if ("file".equals(scheme)) {
                versioning = directoryToVersioning(uri);
            } else {
                // Only compute the metadata for http-based repositories.
                String responseBody = new String(requestAsAuthenticatedOrAnonymous(repo, uri));
                versioning = htmlIndexToVersioning(responseBody, uri);
            }

            if (versioning == null) {
                return null;
            } else {
                return new MavenMetadata(versioning);
            }
        } catch (MavenClientSideException exception) {
            if (exception.getResponseCode() != null && exception.getResponseCode() != 404) {
                // If access was denied, do not attempt to derive metadata from this repository in the future.
                repo.setDeriveMetadataIfMissing(false);
            }
            throw exception;
        }
    }

    @Nullable
    private MavenMetadata.Versioning directoryToVersioning(String uri) {
        Path dir = Paths.get(URI.create(uri));
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                List<String> versions = new ArrayList<>();
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        versions.add(path.getFileName().toString());
                    }
                }
                return new MavenMetadata.Versioning(versions, null, null);
            } catch (IOException e) {
                throw new MavenDownloadingException("Unable to derive metadata from file repository. " + e.getMessage());
            }
        }
        return null;
    }

    @Nullable
    private MavenMetadata.Versioning htmlIndexToVersioning(String responseBody, String uri) {

        // A very primitive approach, this just finds hrefs with trailing "/",
        List<String> versions = new ArrayList<>();
        int start = responseBody.indexOf("<a href=\"");
        while (start > 0) {
            start = start + 9;
            int end = responseBody.indexOf("\">", start);
            if (end < 0) {
                break;
            }
            String href = responseBody.substring(start, end).trim();
            if (href.endsWith("/")) {
                //Only look for hrefs that have directories (the directory names are the versions)
                versions.add(hrefToVersion(href, uri));
            }

            start = responseBody.indexOf("<a href=\"", end);
        }
        if (versions.isEmpty()) {
            return null;
        }

        return new MavenMetadata.Versioning(versions, null, null);
    }

    String hrefToVersion(String href, String rootUri) {
        String version;
        if (href.startsWith(rootUri)) {
            //intentionally length + 1 to exclude "/"
            version = href.substring(rootUri.length());
        } else {
            version = href;
        }

        if (version.endsWith("/")) {
            return version.substring(0, version.length() - 1);
        } else {
            return version;
        }
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
                    gav.getArtifactId().equals(projectPom.getArtifactId()) &&
                    Objects.equals(projectPom.getValue(projectPom.getVersion()), projectPom.getValue(gav.getVersion()))) {
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

        List<String> downloadFailures = new ArrayList<>();
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
                        // unable to read the pom from a file-based repository.
                        downloadFailures.add("[" + repo.getUri() + "] Unable to download dependency. " + e.getMessage());
                    }
                } else {

                    try {
                        byte[] responseBody = requestAsAuthenticatedOrAnonymous(repo, uri.toString());

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
                    } catch (Throwable exception) {
                        downloadFailures.add("[" + repo.getUri() + "] Unable to download dependency. " + exception.getMessage());
                        if (exception instanceof MavenClientSideException) {
                            //If the exception is a common, client-side exception, cache an empty result.
                            mavenCache.putPom(resolvedGav, null);
                        }
                    }
                }
            } else if (result.isPresent()) {
                sample.stop(timer.tags("outcome", "cached").register(Metrics.globalRegistry));
                return result.get();
            } else {
                downloadFailures.add("[" + repo.getUri() + "] Cached empty result.");
            }
        }

        sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));
        if (containingPom != null) {
            ctx.getResolutionListener().downloadError(gav, containingPom.getRequested());
        }

        StringBuilder message = new StringBuilder("Unable to download dependency [" + gav + "] from the following repositories :");

        for (MavenRepository repository : normalizedRepos) {
            message.append("\n  ").append(repository.getUri());
        }

        message.append("\nDownload failures:");
        for (String failure : downloadFailures) {
            message.append("\n  ").append(failure);
        }
        throw new MavenDownloadingException(message.toString());
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
        normalizedRepositories.add(ctx.getLocalRepository());

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
     * Replicates Apache Maven's behavior to attempt anonymous download if repository credentials prove invalid
     */
    private byte[] requestAsAuthenticatedOrAnonymous(MavenRepository repo, String uriString) throws Throwable {
        try {
            return sendRequest.apply(applyAuthenticationToRequest(repo, httpSender.get(uriString)).build());
        } catch (MavenClientSideException e) {
            return sendRequest.apply(httpSender.get(uriString).build());
        }
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
        if (repository.getUsername() != null && repository.getPassword() != null) {
            return request.withBasicAuthentication(repository.getUsername(), repository.getPassword());
        }
        return request;
    }

    private MavenRepository applyMirrors(MavenRepository repository) {
        return MavenRepositoryMirror.apply(ctx.getMirrors(), repository);
    }
}
