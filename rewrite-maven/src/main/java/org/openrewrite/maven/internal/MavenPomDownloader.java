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

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
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

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("OptionalAssignedToNull")
public class MavenPomDownloader {
    private static final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
            .handle(SocketTimeoutException.class, TimeoutException.class)
            .handleIf(throwable -> throwable instanceof UncheckedIOException && throwable.getCause() instanceof SocketTimeoutException)
            .withDelay(Duration.ofMillis(500))
            .withJitter(0.1)
            .withMaxRetries(5)
            .build();

    private static final Pattern SNAPSHOT_TIMESTAMP = Pattern.compile("^(.*-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

    private static final String SNAPSHOT = "SNAPSHOT";


    private final MavenPomCache mavenCache;
    private final Map<Path, Pom> projectPoms;
    private final Map<GroupArtifactVersion, Pom> projectPomsByGav;
    private final MavenExecutionContextView ctx;
    private final HttpSender httpSender;

    @Nullable
    private MavenSettings mavenSettings;

    @Nullable
    private List<String> activeProfiles;

    private boolean addCentralRepository;
    private boolean addLocalRepository;

    /**
     * @param projectPoms    Other POMs in this project.
     * @param ctx            The execution context, which potentially contain Maven settings customization
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
     * A MavenPomDownloader for non-maven contexts where there are no project poms or assumption that maven central
     * is implicitly added as a repository. In a Maven contexts, a non-empty projectPoms should be specified to
     * {@link #MavenPomDownloader(Map, ExecutionContext)} for accurate results.
     *
     * @param ctx The execution context, which potentially contain Maven settings customization and {@link HttpSender} customization.
     */
    public MavenPomDownloader(ExecutionContext ctx) {
        this(emptyMap(), HttpSenderExecutionContextView.view(ctx).getHttpSender(), ctx);
        this.addCentralRepository = Boolean.TRUE.equals(MavenExecutionContextView.view(ctx).getAddCentralRepository());
        this.addLocalRepository = Boolean.TRUE.equals(MavenExecutionContextView.view(ctx).getAddLocalRepository());
    }

    /**
     * @param projectPoms Other POMs in this project.
     * @param ctx         The execution context, which potentially contain Maven settings customization
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
        this.ctx = MavenExecutionContextView.view(ctx);
        this.mavenCache = this.ctx.getPomCache();
        this.addCentralRepository = !Boolean.FALSE.equals(MavenExecutionContextView.view(ctx).getAddCentralRepository());
        this.addLocalRepository = !Boolean.FALSE.equals(MavenExecutionContextView.view(ctx).getAddLocalRepository());
    }

    byte[] sendRequest(HttpSender.Request request) throws IOException, HttpSenderResponseException {
        long start = System.nanoTime();
        try {
            return Failsafe.with(retryPolicy).get(() -> {
                try (HttpSender.Response response = httpSender.send(request)) {
                    if (!response.isSuccessful()) {
                        throw new HttpSenderResponseException(null, response.getCode(),
                                new String(response.getBodyAsBytes()));
                    }
                    return response.getBodyAsBytes();
                }
            });
        } catch (FailsafeException failsafeException) {
            if (failsafeException.getCause() instanceof HttpSenderResponseException) {
                throw (HttpSenderResponseException) failsafeException.getCause();
            }
            throw failsafeException;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.ctx.recordResolutionTime(Duration.ofNanos(System.nanoTime() - start));
        }
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
        Pom parentPom = projectPoms.get(parentPath);
        return parentPom != null && parentPom.getGav().getGroupId().equals(parent.getGav().getGroupId()) &&
               parentPom.getGav().getArtifactId().equals(parent.getGav().getArtifactId()) ? parentPom : null;
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

        if (containingPom != null) {
            gav = containingPom.getValues(gav);
        }

        ctx.getResolutionListener().downloadMetadata(gav);

        Timer.Sample sample = Timer.start();
        Timer.Builder timer = Timer.builder("rewrite.maven.download").tag("type", "metadata");

        MavenMetadata mavenMetadata = null;
        Collection<MavenRepository> normalizedRepos = distinctNormalizedRepositories(repositories, containingPom, null);
        Map<MavenRepository, String> repositoryResponses = new LinkedHashMap<>();
        List<String> attemptedUris = new ArrayList<>(normalizedRepos.size());
        for (MavenRepository repo : normalizedRepos) {
            ctx.getResolutionListener().repository(repo, containingPom);
            if (gav.getVersion() != null && !repositoryAcceptsVersion(repo, gav.getVersion(), containingPom)) {
                continue;
            }
            attemptedUris.add(repo.getUri());
            Optional<MavenMetadata> result = mavenCache.getMavenMetadata(URI.create(repo.getUri()), gav);
            if (result == null) {
                // Not in the cache, attempt to download it.
                boolean cacheEmptyResult = false;
                try {
                    String scheme = URI.create(repo.getUri()).getScheme();
                    String uri = repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                                 requireNonNull(gav.getGroupId()).replace('.', '/') + '/' +
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
                            Counter.builder("rewrite.maven.derived.metadata")
                                    .tag("repositoryUri", repo.getUri())
                                    .tag("group", gav.getGroupId())
                                    .tag("artifact", gav.getArtifactId())
                                    .register(Metrics.globalRegistry)
                                    .increment();
                            result = Optional.of(derivedMeta);
                        }
                    } catch (HttpSenderResponseException | MavenDownloadingException | IOException e) {
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
            ctx.getResolutionListener().downloadError(gav, attemptedUris, null);
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
    private MavenMetadata deriveMetadata(GroupArtifactVersion gav, MavenRepository repo) throws HttpSenderResponseException, IOException, MavenDownloadingException {
        if ((repo.getDeriveMetadataIfMissing() != null && !repo.getDeriveMetadataIfMissing()) || gav.getVersion() != null) {
            // Do not derive metadata if we cannot navigate/browse the artifacts.
            // Do not derive metadata if a specific version has been defined.
            return null;
        }

        String scheme = URI.create(repo.getUri()).getScheme();
        String uri = repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                     requireNonNull(gav.getGroupId()).replace('.', '/') + '/' +
                     gav.getArtifactId() + '/';

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
                    if (Files.isDirectory(path) && hasPomFile(dir, path, gav)) {
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
        // apparently the snapshot timestamp is not always present in the metadata
        if (s1 == null || s1.getTimestamp() == null) {
            return s2;
        } else if (s2 == null || s2.getTimestamp() == null) {
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
                ctx.getResolutionListener().downloadError(gav, emptyList(), containingPom.getRequested());
            }
            throw new MavenDownloadingException("Group id, artifact id, or version are missing.", null, gav);
        }

        ctx.getResolutionListener().download(gav);

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
            !StringUtils.isBlank(relativePath) && !relativePath.contains(":")) {
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
        Timer.Builder timer = Timer.builder("rewrite.maven.download").tag("type", "pom");

        Map<MavenRepository, String> repositoryResponses = new LinkedHashMap<>();
        String versionMaybeDatedSnapshot = datedSnapshotVersion(gav, containingPom, repositories, ctx);
        GroupArtifactVersion originalGav = gav;
        gav = handleSnapshotTimestampVersion(gav);
        List<String> uris = new ArrayList<>();
        for (MavenRepository repo : normalizedRepos) {
            ctx.getResolutionListener().repository(repo, containingPom);
            //noinspection DataFlowIssue
            if (!repositoryAcceptsVersion(repo, gav.getVersion(), containingPom)) {
                continue;
            }

            ResolvedGroupArtifactVersion resolvedGav = new ResolvedGroupArtifactVersion(
                    repo.getUri(), gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), versionMaybeDatedSnapshot);
            Optional<Pom> result = mavenCache.getPom(resolvedGav);

            if (result == null) {
                URI uri = URI.create(repo.getUri() + (repo.getUri().endsWith("/") ? "" : "/") +
                                     requireNonNull(gav.getGroupId()).replace('.', '/') + '/' +
                                     gav.getArtifactId() + '/' +
                                     gav.getVersion() + '/' +
                                     gav.getArtifactId() + '-' + versionMaybeDatedSnapshot + ".pom");
                uris.add(uri.toString());
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

                            if (pom.getPackaging() == null || "jar".equals(pom.getPackaging())) {
                                File jar = f.toPath().resolveSibling(gav.getArtifactId() + '-' + versionMaybeDatedSnapshot + ".jar").toFile();
                                if (!jar.exists() || jar.length() == 0) {
                                    // The jar has not been downloaded, making this dependency unusable.
                                    continue;
                                }
                            }

                            if (repo.getUri().equals(MavenRepository.MAVEN_LOCAL_DEFAULT.getUri())) {
                                // so that the repository path is the same regardless of username
                                pom = pom.withRepository(MavenRepository.MAVEN_LOCAL_USER_NEUTRAL);
                            }

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
                    } catch (IOException e) {
                        repositoryResponses.put(repo, e.getMessage());
                    }
                }
            } else if (result.isPresent()) {
                sample.stop(timer.tags("outcome", "cached").register(Metrics.globalRegistry));
                return result.get();
            } else {
                repositoryResponses.put(repo, "Did not attempt to download because of a previous failure to retrieve from this repository.");
            }
        }
        ctx.getResolutionListener().downloadError(gav, uris, (containingPom == null) ? null : containingPom.getRequested());
        sample.stop(timer.tags("outcome", "unavailable").register(Metrics.globalRegistry));
        throw new MavenDownloadingException("Unable to download POM: " + gav + '.', null, originalGav)
                .setRepositoryResponses(repositoryResponses);
    }

    /**
     * Gets the base version from snapshot timestamp version.
     */
    private GroupArtifactVersion handleSnapshotTimestampVersion(GroupArtifactVersion gav) {
        Matcher m = SNAPSHOT_TIMESTAMP.matcher(requireNonNull(gav.getVersion()));
        if (m.matches()) {
            final String baseVersion;
            if (m.group(1) != null) {
                baseVersion = m.group(1) + SNAPSHOT;
            } else {
                baseVersion = SNAPSHOT;
            }
            return gav.withVersion(baseVersion);
        }
        return gav;
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

    Collection<MavenRepository> distinctNormalizedRepositories(
            List<MavenRepository> repositories,
            @Nullable ResolvedPom containingPom,
            @Nullable String acceptsVersion) {
        LinkedHashMap<String, MavenRepository> normalizedRepositories = new LinkedHashMap<>();
        if (addLocalRepository) {
            normalizedRepositories.put(ctx.getLocalRepository().getId(), ctx.getLocalRepository());
        }

        for (MavenRepository repo : repositories) {
            MavenRepository normalizedRepo = normalizeRepository(repo, ctx, containingPom);
            if (normalizedRepo != null && (acceptsVersion == null || repositoryAcceptsVersion(normalizedRepo, acceptsVersion, containingPom))) {
                normalizedRepositories.put(normalizedRepo.getId(), normalizedRepo);
            }
        }

        // repositories from maven settings
        for (MavenRepository repo : ctx.getRepositories(mavenSettings, activeProfiles)) {
            MavenRepository normalizedRepo = normalizeRepository(repo, ctx, containingPom);
            if (normalizedRepo != null && (acceptsVersion == null || repositoryAcceptsVersion(normalizedRepo, acceptsVersion, containingPom))) {
                normalizedRepositories.put(normalizedRepo.getId(), normalizedRepo);
            }
        }
        if (!normalizedRepositories.containsKey(MavenRepository.MAVEN_CENTRAL.getId()) && addCentralRepository) {
            MavenRepository normalizedRepo = normalizeRepository(MavenRepository.MAVEN_CENTRAL, ctx, containingPom);
            if (normalizedRepo != null) {
                normalizedRepositories.put(normalizedRepo.getId(), normalizedRepo);
            }
        }
        return normalizedRepositories.values();
    }

    @Nullable
    public MavenRepository normalizeRepository(MavenRepository originalRepository, MavenExecutionContextView ctx, @Nullable ResolvedPom containingPom) {
        Optional<MavenRepository> result = null;
        MavenRepository repository = originalRepository;
        if (containingPom != null) {
            // Parameter substitution in case a repository is defined like "${ARTIFACTORY_URL}/repo"
            repository = repository.withUri(containingPom.getValue(repository.getUri()));
        }
        repository = applyAuthenticationToRepository(applyMirrors(repository));
        try {
            if (repository.isKnownToExist()) {
                return repository;
            }

            // If a repository URI contains an unresolved property placeholder, do not continue.
            // There is also an edge case in which this condition is transient during `resolveParentPropertiesAndRepositoriesRecursively()`
            // and therefore, we do not want to cache a null normalization result.
            if (repository.getUri().contains("${")) {
                ctx.getResolutionListener().repositoryAccessFailed(repository.getUri(),
                        new IllegalArgumentException("Repository " + repository.getUri() + " contains an unresolved property placeholder."));
                return null;
            }

            // Skip blocked repositories
            // https://github.com/openrewrite/rewrite/issues/3141
            if (repository.getUri().contains("0.0.0.0")) {
                ctx.getResolutionListener().repositoryAccessFailed(repository.getUri(),
                        new IllegalArgumentException("Repository " + repository.getUri() + " has invalid IP address."));
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
                    ctx.getResolutionListener().repositoryAccessFailed(repository.getUri(), new IllegalArgumentException("Repository " + repository.getUri() + " is not HTTP(S)."));
                    return null;
                }

                // Always prefer to use https, fallback to http only if https isn't available
                // URLs are case-sensitive after the domain name, so it can be incorrect to lowerCase() a whole URL
                // This regex accepts any capitalization of the letters in "http"
                String httpsUri = repository.getUri().toLowerCase().startsWith("http:") ?
                        repository.getUri().replaceFirst("[hH][tT][tT][pP]://", "https://") :
                        repository.getUri();
                if (!httpsUri.endsWith("/")) {
                    httpsUri += "/";
                }

                HttpSender.Request.Builder request = applyAuthenticationToRequest(repository, httpSender.get(httpsUri));
                MavenRepository normalized = null;
                try {
                    sendRequest(request.build());
                    normalized = repository.withUri(httpsUri).withKnownToExist(true);
                } catch (Throwable t) {
                    if (t instanceof HttpSenderResponseException) {
                        HttpSenderResponseException e = (HttpSenderResponseException) t;
                        // response was returned from the server, but it was not a 200 OK. The server therefore exists.
                        if (e.isServerReached()) {
                            normalized = repository.withUri(httpsUri);
                        }
                    }
                    if (normalized == null) {
                        if (!httpsUri.equals(originalUrl)) {
                            try {
                                sendRequest(request.url(originalUrl).build());
                                normalized = new MavenRepository(
                                        repository.getId(),
                                        originalUrl,
                                        repository.getReleases(),
                                        repository.getSnapshots(),
                                        repository.getUsername(),
                                        repository.getPassword());
                            } catch (HttpSenderResponseException e) {
                                //Response was returned from the server, but it was not a 200 OK. The server therefore exists.
                                if (e.isServerReached()) {
                                    normalized = new MavenRepository(
                                            repository.getId(),
                                            originalUrl,
                                            repository.getReleases(),
                                            repository.getSnapshots(),
                                            repository.getUsername(),
                                            repository.getPassword());
                                }
                            } catch (Throwable e) {
                                // ok to fall through here and cache a null
                            }
                        }
                    }
                    if (normalized == null && !(t instanceof HttpSenderResponseException &&
                                                ((HttpSenderResponseException) t).getBody().contains("Directory listing forbidden"))) {
                        ctx.getResolutionListener().repositoryAccessFailed(repository.getUri(), t);
                    }
                }
                mavenCache.putNormalizedRepository(repository, normalized);
                result = Optional.ofNullable(normalized);
            }
        } catch (Exception e) {
            ctx.getResolutionListener().repositoryAccessFailed(repository.getUri(), e);
            ctx.getOnError().accept(e);
            mavenCache.putNormalizedRepository(repository, null);
        }

        return result == null || !result.isPresent() ? null : applyAuthenticationToRepository(result.get());
    }

    /**
     * Replicates Apache Maven's behavior to attempt anonymous download if repository credentials prove invalid
     */
    private byte[] requestAsAuthenticatedOrAnonymous(MavenRepository repo, String uriString) throws HttpSenderResponseException, IOException {
        try {
            return sendRequest(applyAuthenticationToRequest(repo, httpSender.get(uriString)).build());
        } catch (HttpSenderResponseException e) {
            if (hasCredentials(repo) && e.isClientSideException()) {
                return retryRequestAnonymously(uriString, e);
            } else {
                throw e;
            }
        }
    }

    private byte[] retryRequestAnonymously(String uriString, HttpSenderResponseException originalException) throws HttpSenderResponseException, IOException {
        try {
            return sendRequest(httpSender.get(uriString).build());
        } catch (HttpSenderResponseException retryException) {
            if (retryException.isAccessDenied()) {
                throw originalException;
            } else {
                throw retryException;
            }
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
        if (ctx.getSettings() != null && ctx.getSettings().getServers() != null) {
            for (MavenSettings.Server server : ctx.getSettings().getServers().getServers()) {
                if (server.getId().equals(repository.getId()) && server.getConfiguration() != null && server.getConfiguration().getHttpHeaders() != null) {
                    for (MavenSettings.HttpHeader header : server.getConfiguration().getHttpHeaders()) {
                        request.withHeader(header.getName(), header.getValue());
                    }
                }
            }
        }
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
    public static class HttpSenderResponseException extends Exception {
        @Nullable
        private final Integer responseCode;

        private final String body;

        public HttpSenderResponseException(@Nullable Throwable cause, @Nullable Integer responseCode,
                                           String body) {
            super(cause);
            this.responseCode = responseCode;
            this.body = body;
        }

        /**
         * All 400s are considered client-side exceptions, but we only want to cache ones that are unlikely to change
         * if requested again in order to save on time spent making HTTP calls.
         * <br/>
         * For 408 TIMEOUT, 425 TOO_EARLY, and 429 TOO_MANY_REQUESTS, these are likely to change if not cached.
         */
        public boolean isClientSideException() {
            if (responseCode == null || responseCode < 400 || responseCode > 499) {
                return false;
            }
            return responseCode != 408 && responseCode != 425 && responseCode != 429;
        }

        public String getMessage() {
            return responseCode == null ?
                    requireNonNull(getCause()).getMessage() :
                    "HTTP " + responseCode;
        }

        public boolean isAccessDenied() {
            return responseCode != null && 400 < responseCode && responseCode <= 403;
        }

        // Any response code below 100 implies that no connection was made. Sometimes 0 or -1 is used for connection failures.
        public boolean isServerReached() {
            return responseCode != null && responseCode >= 100;
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

    /**
     * Check if <tt>dir</tt> has a folder named <tt>versionPath</tt> and a pom file for the artifact.
     * Maven might have tried to download the artifact before and failed, so the folder might exist but contains only
     * a <tt>artifact-version.pom.lastUpdated</tt> file, indicating last time maven attempted to download the artifact.
     *
     * @param dir         root directory of the artifact
     * @param versionPath version path of the artifact
     * @param gav         the artifact
     * @return <tt>true</tt> if the artifact has a pom file, <tt>false</tt> otherwise.
     */
    private static boolean hasPomFile(Path dir, Path versionPath, GroupArtifactVersion gav) {
        String artifactPomFile = gav.getArtifactId() + "-" + versionPath.getFileName() + ".pom";
        return Files.exists(dir.resolve(versionPath.resolve(artifactPomFile)));
    }
}
