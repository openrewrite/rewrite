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
import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
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
    private final Map<GroupArtifactVersion, Pom> projectPomsByGav;
    private final MavenExecutionContextView ctx;
    private final HttpSender httpSender;

    @Nullable
    private MavenSettings mavenSettings;

    @Nullable
    private List<String> activeProfiles;

    private final CheckedFunction1<HttpSender.Request, byte[]> sendRequest;

    /**
     * @param projectPoms    Other POMs in this project.
     * @param ctx            The execution context, which potentially contain Maven settings customization and
     *                       and {@link HttpSender} customization.
     * @param mavenSettings  The Maven settings to use, if any. This argument overrides any Maven settings
     *                       set on the execution context.
     * @param activeProfiles The active profiles to use, if any. This argument overrides any active profiles
     *                       set on the execution context.
     */
    public MavenPomDownloader(Map<Path, Pom> projectPoms, ExecutionContext ctx,
                              @Nullable MavenSettings mavenSettings,
                              @Nullable List<String> activeProfiles) {
        this(projectPoms, HttpSenderExecutionContextView.view(ctx).getHttpSender(), ctx);
        this.mavenSettings = mavenSettings;
        this.activeProfiles = activeProfiles;
    }

    /**
     * @param projectPoms Other POMs in this project.
     * @param ctx         The execution context, which potentially contain Maven settings customization and
     *                    and {@link HttpSender} customization.
     */
    public MavenPomDownloader(Map<Path, Pom> projectPoms, ExecutionContext ctx) {
        this(projectPoms, HttpSenderExecutionContextView.view(ctx).getHttpSender(), ctx);
    }

    /**
     * @param projectPoms Project poms on disk.
     * @param httpSender  The HTTP sender.
     * @param ctx         The execution context.
     * @deprecated Use {@link #MavenPomDownloader(Map, ExecutionContext)} instead.
     */
    @Deprecated
    public MavenPomDownloader(Map<Path, Pom> projectPoms, HttpSender httpSender, ExecutionContext ctx) {
        this.projectPoms = projectPoms;
        this.projectPomsByGav = projectPomsByGav(projectPoms);
        this.httpSender = httpSender;
        this.sendRequest = Retry.decorateCheckedFunction(
                mavenDownloaderRetry,
                request -> {
                    int responseCode;
                    try (HttpSender.Response response = httpSender.send(request)) {
                        if (response.isSuccessful()) {
                            return response.getBodyAsBytes();
                        }
                        responseCode = response.getCode();
                    } catch (Throwable t) {
                        throw new HttpSenderResponseException(t, null);
                    }
                    throw new HttpSenderResponseException(null, responseCode);
                });
        this.ctx = MavenExecutionContextView.view(ctx);
        this.mavenCache = this.ctx.getPomCache();
    }

    private Map<GroupArtifactVersion, Pom> projectPomsByGav(Map<Path, Pom> projectPoms) {
        Map<GroupArtifactVersion, Pom> result = new HashMap<>();
        for (final Pom projectPom : projectPoms.values()) {
            final List<Pom> ancestryWithinProject = getAncestryWithinProject(projectPom, projectPoms);
            final Map<String, String> mergedProperties = mergeProperties(ancestryWithinProject);
            final GroupArtifactVersion gav = new GroupArtifactVersion(
                    projectPom.getGroupId(),
                    projectPom.getArtifactId(),
                    ResolvedPom.placeholderHelper.replacePlaceholders(projectPom.getVersion(), mergedProperties::get)
            );
            result.put(gav, projectPom);
        }
        return result;
    }

    private Map<String, String> mergeProperties(final List<Pom> pomAncestry) {
        Map<String, String> mergedProperties = new HashMap<>();
        for (final Pom pom : pomAncestry) {
            for (final Map.Entry<String, String> property : pom.getProperties().entrySet()) {
                mergedProperties.putIfAbsent(property.getKey(), property.getValue());
            }
        }
        return mergedProperties;
    }

    private List<Pom> getAncestryWithinProject(Pom projectPom, Map<Path, Pom> projectPoms) {
        Pom parentPom = getParentWithinProject(projectPom, projectPoms);
        if (parentPom == null) {
            return Collections.singletonList(projectPom);
        } else {
            return ListUtils.concat(projectPom, getAncestryWithinProject(parentPom, projectPoms));
        }
    }

    @Nullable
    private Pom getParentWithinProject(Pom projectPom, Map<Path, Pom> projectPoms) {
        final Parent parent = projectPom.getParent();
        if (parent == null || projectPom.getSourcePath() == null) {
            return null;
        }
        String relativePath = parent.getRelativePath();
        if (StringUtils.isBlank(relativePath)) {
            relativePath = "../pom.xml";
        }
        Path parentPath = projectPom.getSourcePath()
                .resolve("..")
                .resolve(Paths.get(relativePath))
                .normalize();
        return projectPoms.get(parentPath);
    }

    public MavenMetadata downloadMetadata(GroupArtifact groupArtifact, @Nullable ResolvedPom containingPom, List<MavenRepository> repositories) throws MavenDownloadingException {
        return downloadMetadata(new GroupArtifactVersion(groupArtifact.getGroupId(), groupArtifact.getArtifactId(), null),
                containingPom,
                repositories);
    }

    public MavenMetadata downloadMetadata(GroupArtifactVersion gav, @Nullable ResolvedPom containingPom, List<MavenRepository> repositories) throws MavenDownloadingException {
        if (gav.getGroupId() == null) {
            throw new MavenDownloadingException("Missing group id.", null, gav);
        }

        Timer.Sample sample = Timer.start();
        Timer.Builder timer = Timer.builder("rewrite.maven.download")
                .tag("group.id", gav.getGroupId())
                .tag("artifact.id", gav.getArtifactId())
                .tag("type", "metadata");

        MavenMetadata mavenMetadata = null;
        Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, containingPom, null);
        Map<MavenRepository, String> repositoryResponses = new LinkedHashMap<>();
        for (MavenRepository repo : normalizedRepos) {
            if (gav.getVersion() != null && !repositoryAcceptsVersion(repo, gav.getVersion(), containingPom)) {
                continue;
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
                } catch (HttpSenderResponseException e) {
                    repositoryResponses.put(repo, e.getMessage());
                    if (e.isClientSideException()) {
                        //If we have a 400-404, cache an empty result.
                        cacheEmptyResult = true;
                    }
                } catch (IOException e) {
                    repositoryResponses.put(repo, e.getMessage());
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
                    } catch (HttpSenderResponseException | MavenDownloadingException e) {
                        repositoryResponses.put(repo, e.getMessage());
                    }
                }
                if (result == null && cacheEmptyResult) {
                    // If there was no fatal failure while attempting to find metadata and there was no metadata retrieved
                    // from the current repo, cache an empty result.
                    mavenCache.putMavenMetadata(URI.create(repo.getUri()), gav, null);
                }
            } else if (!result.isPresent()) {
                repositoryResponses.put(repo, "Did not attempt to download because of a previous failure to retrieve from this repository.");
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
            throw new MavenDownloadingException("Unable to download metadata.", null, gav)
                    .setRepositoryResponses(repositoryResponses);
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
     * @param gav  The artifact coordinates that will be derived.
     * @param repo The repository that will be queried for directory results
     * @return Metadata or null if the metadata cannot be derived.
     */
    @Nullable
    private MavenMetadata deriveMetadata(GroupArtifactVersion gav, MavenRepository repo) throws HttpSenderResponseException, MavenDownloadingException {
        if ((repo.getDeriveMetadataIfMissing() != null && !repo.getDeriveMetadataIfMissing()) || gav.getVersion() != null) {
            // Do not derive metadata if we cannot navigate/browse the artifacts.
            // Do not derive metadata if a specific version has been defined.
            return null;
        }

        String scheme = URI.create(repo.getUri()).getScheme();
        String uri = repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                     requireNonNull(gav.getGroupId()).replace('.', '/') + '/' +
                     gav.getArtifactId();

        try {
            MavenMetadata.Versioning versioning;
            if ("file".equals(scheme)) {
                versioning = directoryToVersioning(uri, gav);
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
        } catch (HttpSenderResponseException e) {
            if (e.isClientSideException() && e.getResponseCode() != null && e.getResponseCode() != 404) {
                // If access was denied, do not attempt to derive metadata from this repository in the future.
                repo.setDeriveMetadataIfMissing(false);
            }
            throw e;
        }
    }

    @Nullable
    private MavenMetadata.Versioning directoryToVersioning(String uri, GroupArtifactVersion gav) throws MavenDownloadingException {
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
                throw new MavenDownloadingException("Unable to derive metadata from file repository. " + e.getMessage(), null, gav);
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

    protected MavenMetadata mergeMetadata(MavenMetadata m1, MavenMetadata m2) {
        return new MavenMetadata(new MavenMetadata.Versioning(
                mergeVersions(m1.getVersioning().getVersions(), m2.getVersioning().getVersions()),
                Stream.concat(m1.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m1.getVersioning().getSnapshotVersions().stream(),
                        m2.getVersioning().getSnapshotVersions() == null ? Stream.empty() : m2.getVersioning().getSnapshotVersions().stream()).collect(toList()),
                maxSnapshot(m1.getVersioning().getSnapshot(), m2.getVersioning().getSnapshot())
        ));
    }

    private List<String> mergeVersions(List<String> versions1, List<String> versions2) {
        Set<String> merged = new HashSet<>(versions1);
        merged.addAll(versions2);
        return new ArrayList<>(merged);
    }

    @Nullable
    private MavenMetadata.Snapshot maxSnapshot(@Nullable MavenMetadata.Snapshot s1, @Nullable MavenMetadata.Snapshot s2) {
        if (s1 == null) {
            return s2;
        } else if (s2 == null) {
            return s1;
        } else {
            return (s1.getTimestamp().compareTo(s2.getTimestamp())) >= 0 ? s1 : s2;
        }
    }

    public Pom download(GroupArtifactVersion gav,
                        @Nullable String relativePath,
                        @Nullable ResolvedPom containingPom,
                        List<MavenRepository> repositories) throws MavenDownloadingException {
        if (gav.getGroupId() == null || gav.getArtifactId() == null || gav.getVersion() == null) {
            if (containingPom != null) {
                ctx.getResolutionListener().downloadError(gav, containingPom.getRequested());
            }
            throw new MavenDownloadingException("Group id, artifact id, or version are missing.", null, gav);
        }

        // The pom being examined might be from a remote repository or a local filesystem.
        // First try to match the requested download with one of the project POMs.
        final Pom projectPomWithResolvedVersion = projectPomsByGav.get(gav);
        if (projectPomWithResolvedVersion != null) {
            return projectPomWithResolvedVersion;
        }

        // The requested gav might itself have an unresolved placeholder in the version, so also check raw values
        for (Pom projectPom : projectPoms.values()) {
            if (gav.getGroupId().equals(projectPom.getGroupId()) &&
                    gav.getArtifactId().equals(projectPom.getArtifactId()) &&
                    (gav.getVersion().equals(projectPom.getVersion()) || projectPom.getVersion().equals(projectPom.getValue(gav.getVersion())))) {
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

        Map<MavenRepository, String> repositoryResponses = new LinkedHashMap<>();
        String versionMaybeDatedSnapshot = datedSnapshotVersion(gav, containingPom, repositories, ctx);
        for (MavenRepository repo : normalizedRepos) {
            if (!repositoryAcceptsVersion(repo, gav.getVersion(), containingPom)) {
                continue;
            }

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

                            // so that the repository path is the same regardless of username
                            pom = pom.withRepository(MavenRepository.MAVEN_LOCAL_USER_NEUTRAL);

                            if (!Objects.equals(versionMaybeDatedSnapshot, pom.getVersion())) {
                                pom = pom.withGav(pom.getGav().withDatedSnapshotVersion(versionMaybeDatedSnapshot));
                            }
                            mavenCache.putPom(resolvedGav, pom);
                            ctx.getResolutionListener().downloadSuccess(resolvedGav, containingPom);
                            sample.stop(timer.tags("outcome", "from maven local").register(Metrics.globalRegistry));
                            return pom;
                        }
                    } catch (IOException e) {
                        // unable to read the pom from a file-based repository.
                        repositoryResponses.put(repo, e.getMessage());
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
                        ctx.getResolutionListener().downloadSuccess(resolvedGav, containingPom);
                        sample.stop(timer.tags("outcome", "downloaded").register(Metrics.globalRegistry));
                        return pom;
                    } catch (HttpSenderResponseException e) {
                        repositoryResponses.put(repo, e.getMessage());
                        if (e.isClientSideException()) {
                            //If the exception is a common, client-side exception, cache an empty result.
                            mavenCache.putPom(resolvedGav, null);
                        }
                    }
                }
            } else if (result.isPresent()) {
                sample.stop(timer.tags("outcome", "cached").register(Metrics.globalRegistry));
                return result.get();
            } else {
                repositoryResponses.put(repo, "Did not attempt to download because of a previous failure to retrieve from this repository.");
            }
        }

        sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));
        if (containingPom != null) {
            ctx.getResolutionListener().downloadError(gav, containingPom.getRequested());
        }

        throw new MavenDownloadingException("Unable to download POM.", null, gav)
                .setRepositoryResponses(repositoryResponses);
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
            if (snapshot != null && snapshot.getTimestamp() != null) {
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
            if (normalizedRepo != null && (acceptsVersion == null || repositoryAcceptsVersion(normalizedRepo, acceptsVersion, containingPom))) {
                normalizedRepositories.add(normalizedRepo);
            }
        }

        // repositories from maven settings
        for (MavenRepository repo : ctx.getRepositories(mavenSettings, activeProfiles)) {
            MavenRepository normalizedRepo = normalizeRepository(repo, containingPom);
            if (normalizedRepo != null && (acceptsVersion == null || repositoryAcceptsVersion(normalizedRepo, acceptsVersion, containingPom))) {
                normalizedRepositories.add(normalizedRepo);
            }
        }

        normalizedRepositories.add(normalizeRepository(MavenRepository.MAVEN_CENTRAL, containingPom));
        return normalizedRepositories;
    }

    @Nullable
    public MavenRepository normalizeRepository(MavenRepository originalRepository, @Nullable ResolvedPom containingPom) {
        return normalizeRepository(originalRepository, containingPom, null);
    }

    @Nullable
    public MavenRepository normalizeRepository(MavenRepository originalRepository, @Nullable ResolvedPom containingPom, @Nullable Consumer<Throwable> nullReasonConsumer) {
        Optional<MavenRepository> result = null;
        MavenRepository repository = applyAuthenticationToRepository(applyMirrors(originalRepository));
        if (containingPom != null) {
            repository = repository.withUri(containingPom.getValue(repository.getUri()));
        }
        try {
            if (repository.isKnownToExist()) {
                return repository;
            }

            // If a repository URI contains an unresolved property placeholder, do not continue.
            // There is also an edge case in which this condition is transient during `resolveParentPropertiesAndRepositoriesRecursively()`
            // and therefore, we do not want to cache a null normalization result.
            if (repository.getUri().contains("${")) {
                return null;
            }

            String originalUrl = repository.getUri();
            if ("file".equals(URI.create(originalUrl).getScheme())) {
                return repository;
            }
            result = mavenCache.getNormalizedRepository(repository);
            if (result == null) {
                if (!repository.getUri().toLowerCase().startsWith("http")) {
                    // can be s3 among potentially other types for which there is a maven wagon implementation
                    if (nullReasonConsumer != null) {
                        nullReasonConsumer.accept(new RuntimeException("Repository " + repository.getUri() + " is not HTTP(S)."));
                    }
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
                } catch (Throwable t) {
                    if (t instanceof HttpSenderResponseException) {
                        HttpSenderResponseException e = (HttpSenderResponseException) t;
                        // response was returned from the server, but it was not a 200 OK. The server therefore exists.
                        if (e.getResponseCode() != null) {
                            normalized = repository.withUri(httpsUri);
                        }
                    }
                    if (normalized == null) {
                        if (!httpsUri.equals(originalUrl)) {
                            try {
                                sendRequest.apply(request.url(originalUrl).build());
                                normalized = new MavenRepository(
                                        repository.getId(),
                                        originalUrl,
                                        repository.getReleases(),
                                        repository.getSnapshots(),
                                        repository.getUsername(),
                                        repository.getPassword());
                            } catch (HttpSenderResponseException e) {
                                //Response was returned from the server, but it was not a 200 OK. The server therefore exists.
                                if (e.getResponseCode() != null) {
                                    normalized = new MavenRepository(
                                            repository.getId(),
                                            originalUrl,
                                            repository.getReleases(),
                                            repository.getSnapshots(),
                                            repository.getUsername(),
                                            repository.getPassword());
                                } else if (!e.isClientSideException()) {
                                    return originalRepository;
                                }
                            } catch (Throwable e) {
                                // ok to fall through here and cache a null
                                if (nullReasonConsumer != null) {
                                    nullReasonConsumer.accept(t);
                                }
                            }
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
    private byte[] requestAsAuthenticatedOrAnonymous(MavenRepository repo, String uriString) throws HttpSenderResponseException {
        try {
            try {
                return sendRequest.apply(applyAuthenticationToRequest(repo, httpSender.get(uriString)).build());
            } catch (HttpSenderResponseException e) {
                if (hasCredentials(repo) && e.isClientSideException()) {
                    return sendRequest.apply(httpSender.get(uriString).build());
                } else {
                    throw e;
                }
            }
        } catch (HttpSenderResponseException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t); // unreachable
        }
    }

    /**
     * Returns a Maven Repository with any applicable credentials as sourced from the ExecutionContext
     */
    private MavenRepository applyAuthenticationToRepository(MavenRepository repository) {
        return MavenRepositoryCredentials.apply(ctx.getCredentials(mavenSettings), repository);
    }

    /**
     * Returns a request builder with Authorization header set if the provided repository specifies credentials
     */
    private HttpSender.Request.Builder applyAuthenticationToRequest(MavenRepository repository, HttpSender.Request.Builder request) {
        if (hasCredentials(repository)) {
            return request.withBasicAuthentication(repository.getUsername(), repository.getPassword());
        }
        return request;
    }

    private static boolean hasCredentials(MavenRepository repository) {
        return repository.getUsername() != null && repository.getPassword() != null;
    }

    private MavenRepository applyMirrors(MavenRepository repository) {
        return MavenRepositoryMirror.apply(ctx.getMirrors(mavenSettings), repository);
    }

    @Getter
    private static class HttpSenderResponseException extends Exception {
        @Nullable
        private final Integer responseCode;

        public HttpSenderResponseException(@Nullable Throwable cause, @Nullable Integer responseCode) {
            super(cause);
            this.responseCode = responseCode;
        }

        public boolean isClientSideException() {
            return responseCode != null && responseCode >= 400 && responseCode <= 404;
        }

        public String getMessage() {
            return responseCode == null ?
                    requireNonNull(getCause()).getMessage() :
                    "HTTP " + responseCode;
        }
    }

    public boolean repositoryAcceptsVersion(MavenRepository repo, String version, @Nullable ResolvedPom containingPom) {

        if (version.endsWith("-SNAPSHOT")) {
            String snapshotsRaw = containingPom == null ? repo.getSnapshots() : containingPom.getValue(repo.getSnapshots());
            return snapshotsRaw == null || Boolean.parseBoolean(snapshotsRaw.trim());
        } else if ("https://repo.spring.io/milestone".equalsIgnoreCase(repo.getUri())) {
            // special case this repository since it will be so commonly used
            return version.matches(".*(M|RC)\\d+$");
        }

        String releasesRaw = containingPom == null ? repo.getReleases() : containingPom.getValue(repo.getReleases());
        return releasesRaw == null || Boolean.parseBoolean(releasesRaw.trim());
    }

}
