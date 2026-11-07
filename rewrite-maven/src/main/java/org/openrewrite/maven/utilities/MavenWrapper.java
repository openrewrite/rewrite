/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.utilities;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.MavenRepositoryRemoteAuthenticator;
import org.openrewrite.maven.tree.*;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteExecutionContextView;
import org.openrewrite.remote.RemoteFile;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * The Maven wrapper and distribution to use. The URLs written to {@code maven-wrapper.properties} are always those of
 * the canonical repository, while every {@link Remote} is downloaded from the repository's mirror from the Maven
 * settings, if any. Credentials for the mirror are registered on the {@link ExecutionContext} rather than stored on
 * the {@link Remote}, so a {@link Remote} read with another {@link ExecutionContext} is only served from the
 * {@link org.openrewrite.remote.RemoteArtifactCache} or downloaded anonymously.
 */
@Value
@AllArgsConstructor
public class MavenWrapper {
    public static final String ASF_LICENSE_HEADER = "# Licensed to the Apache Software Foundation (ASF) under one\n" +
                                                    "# or more contributor license agreements.  See the NOTICE file\n" +
                                                    "# distributed with this work for additional information\n" +
                                                    "# regarding copyright ownership.  The ASF licenses this file\n" +
                                                    "# to you under the Apache License, Version 2.0 (the\n" +
                                                    "# \"License\"); you may not use this file except in compliance\n" +
                                                    "# with the License.  You may obtain a copy of the License at\n" +
                                                    "# \n" +
                                                    "#   https://www.apache.org/licenses/LICENSE-2.0\n" +
                                                    "# \n" +
                                                    "# Unless required by applicable law or agreed to in writing,\n" +
                                                    "# software distributed under the License is distributed on an\n" +
                                                    "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                                                    "# KIND, either express or implied.  See the License for the\n" +
                                                    "# specific language governing permissions and limitations\n" +
                                                    "# under the License.\n";
    public static final String WRAPPER_DOWNLOADER_LOCATION_RELATIVE_PATH = ".mvn/wrapper/MavenWrapperDownloader.java";
    public static final String WRAPPER_JAR_LOCATION_RELATIVE_PATH = ".mvn/wrapper/maven-wrapper.jar";
    public static final String WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH = ".mvn/wrapper/maven-wrapper.properties";
    public static final String WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH = "mvnw";
    public static final String WRAPPER_BATCH_LOCATION_RELATIVE_PATH = "mvnw.cmd";

    public static final Path WRAPPER_DOWNLOADER_LOCATION = Paths.get(WRAPPER_DOWNLOADER_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_JAR_LOCATION = Paths.get(WRAPPER_JAR_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get(WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_SCRIPT_LOCATION = Paths.get(WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_BATCH_LOCATION = Paths.get(WRAPPER_BATCH_LOCATION_RELATIVE_PATH);

    private static final GroupArtifact WRAPPER_GROUP_ARTIFACT = new GroupArtifact("org.apache.maven.wrapper", "maven-wrapper");
    private static final GroupArtifact WRAPPER_DISTRIBUTION_GROUP_ARTIFACT = new GroupArtifact("org.apache.maven.wrapper", "maven-wrapper-distribution");
    private static final GroupArtifact DISTRIBUTION_GROUP_ARTIFACT = new GroupArtifact("org.apache.maven", "apache-maven");

    String wrapperVersion;
    String wrapperUri;
    Checksum wrapperChecksum;
    String wrapperDistributionUri;
    DistributionType wrapperDistributionType;
    String distributionVersion;
    String distributionUri;
    Checksum distributionChecksum;

    /**
     * The URI of the repository that wrapper files are downloaded from, or {@code null} to download them from the
     * canonical URIs.
     */
    @Nullable
    String fetchRepositoryUri;

    private final static Cache<URI, Checksum> artifactChecksumCache = Caffeine.newBuilder().maximumSize(20).build();

    /**
     * @deprecated Use the constructor that also takes the fetch repository URI.
     */
    @Deprecated
    public MavenWrapper(String wrapperVersion, String wrapperUri, Checksum wrapperChecksum, String wrapperDistributionUri,
                        DistributionType wrapperDistributionType, String distributionVersion, String distributionUri,
                        Checksum distributionChecksum) {
        this(wrapperVersion, wrapperUri, wrapperChecksum, wrapperDistributionUri, wrapperDistributionType,
                distributionVersion, distributionUri, distributionChecksum, null);
    }

    public static MavenWrapper create(
            @Nullable String wrapperVersion,
            @Nullable String wrapperDistributionTypeName,
            @Nullable String distributionVersion,
            @Nullable String repositoryUrl,
            ExecutionContext ctx
    ) {
        DistributionType wrapperDistributionType = Arrays.stream(DistributionType.values())
                .filter(dt -> dt.classifier.equalsIgnoreCase(wrapperDistributionTypeName))
                .findAny()
                .orElse(DistributionType.Bin);

        MavenPomDownloader pomDownloader = new MavenPomDownloader(emptyMap(), ctx, null, null);

        VersionComparator wrapperVersionComparator = StringUtils.isBlank(wrapperVersion) ?
                new LatestRelease(null) :
                requireNonNull(Semver.validate(wrapperVersion, null).getValue());
        VersionComparator distributionVersionComparator = StringUtils.isBlank(distributionVersion) ?
                new LatestRelease(null) :
                requireNonNull(Semver.validate(distributionVersion, null).getValue());

        MavenRepository repository = StringUtils.isBlank(repositoryUrl) ?
                MavenRepository.MAVEN_CENTRAL :
                MavenRepository.builder()
                        .uri(repositoryUrl)
                        .releases(true)
                        .snapshots(true)
                        .build();

        List<MavenRepository> repositories = singletonList(repository);
        try {
            MavenMetadata wrapperMetadata = pomDownloader.downloadMetadata(WRAPPER_DISTRIBUTION_GROUP_ARTIFACT, null, repositories);
            String resolvedWrapperVersion = wrapperMetadata.getVersioning()
                    .getVersions()
                    .stream()
                    .filter(v -> wrapperVersionComparator.isValid(null, v))
                    .max((v1, v2) -> wrapperVersionComparator.compare(null, v1, v2))
                    .orElseThrow(() -> new IllegalStateException("Expected to find at least one Maven wrapper version to select from."));
            String resolvedWrapperUri = getDownloadUriFor(repository.getUri(), WRAPPER_GROUP_ARTIFACT, resolvedWrapperVersion, null, "jar");
            String resolvedWrapperDistributionUri = getDownloadUriFor(repository.getUri(), WRAPPER_DISTRIBUTION_GROUP_ARTIFACT, resolvedWrapperVersion, wrapperDistributionType.classifier, "zip");

            MavenMetadata distributionMetadata = pomDownloader.downloadMetadata(DISTRIBUTION_GROUP_ARTIFACT, null, repositories);
            String resolvedDistributionVersion = distributionMetadata.getVersioning()
                    .getVersions()
                    .stream()
                    .filter(v -> distributionVersionComparator.isValid(null, v))
                    .max((v1, v2) -> distributionVersionComparator.compare(null, v1, v2))
                    .orElseThrow(() -> new IllegalStateException("Expected to find at least one Maven distribution version to select from."));
            String resolvedDistributionUri = getDownloadUriFor(repository.getUri(), DISTRIBUTION_GROUP_ARTIFACT, resolvedDistributionVersion, "bin", "zip");

            MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
            MavenSettings settings = mctx.getSettings();
            MavenRepository fetchRepository = pomDownloader.normalizeRepository(repository, mctx, null);
            if (fetchRepository == null) {
                fetchRepository = MavenRepositoryCredentials.apply(mctx.getCredentials(settings),
                        MavenRepositoryMirror.apply(mctx.getMirrors(settings), repository));
            }
            MavenRepositoryRemoteAuthenticator authenticator = MavenRepositoryRemoteAuthenticator.forRepository(fetchRepository, settings);
            if (authenticator != null) {
                RemoteExecutionContextView.view(ctx).addAuthenticator(authenticator);
            }
            String mirrorUri = withoutTrailingSlash(fetchRepository.getUri());
            String fetchRepositoryUri = mirrorUri.equals(withoutTrailingSlash(repository.getUri())) ? null : mirrorUri;
            Checksum wrapperJarChecksum = retrieveChecksumUsingCache(URI.create(resolvedWrapperUri),
                    Remote.builder(WRAPPER_JAR_LOCATION).build(fetchUri(fetchRepositoryUri, resolvedWrapperUri,
                            WRAPPER_GROUP_ARTIFACT, resolvedWrapperVersion, null, "jar")), ctx);
            Checksum mavenDistributionChecksum = retrieveChecksumUsingCache(URI.create(resolvedDistributionUri),
                    Remote.builder(Paths.get("")).build(fetchUri(fetchRepositoryUri, resolvedDistributionUri,
                            DISTRIBUTION_GROUP_ARTIFACT, resolvedDistributionVersion, "bin", "zip")), ctx);
            return new MavenWrapper(
                    resolvedWrapperVersion,
                    resolvedWrapperUri,
                    requireNonNull(wrapperJarChecksum),
                    resolvedWrapperDistributionUri,
                    wrapperDistributionType,
                    resolvedDistributionVersion,
                    resolvedDistributionUri,
                    requireNonNull(mavenDistributionChecksum),
                    fetchRepositoryUri
            );
        } catch (MavenDownloadingException e) {
            throw new RuntimeException("Could not get Maven versions at: " + repository.getUri(), e);
        }
    }

    private static Checksum retrieveChecksumUsingCache(URI canonicalUri, RemoteFile remote, ExecutionContext ctx) {
        Checksum ret = artifactChecksumCache.getIfPresent(canonicalUri);
        if (ret != null) {
            return ret;
        }
        ret = Checksum.sha256(remote, ctx).getChecksum();
        artifactChecksumCache.put(canonicalUri, ret);
        return ret;
    }

    public String getWrapperUrl() {
        return wrapperUri;
    }

    public String getDistributionUrl() {
        return distributionUri;
    }

    public Remote wrapperJar() {
        return Remote.builder(WRAPPER_JAR_LOCATION)
                .charset(StandardCharsets.ISO_8859_1)
                .build(getWrapperFetchUri());
    }

    public Remote wrapperJar(SourceFile previous) {
        return Remote.builder(previous)
                .charset(StandardCharsets.ISO_8859_1)
                .build(getWrapperFetchUri());
    }

    public Remote wrapperDownloader() {
        return Remote.builder(WRAPPER_DOWNLOADER_LOCATION)
                .charset(StandardCharsets.UTF_8)
                .build(getWrapperDistributionFetchUri(), ".mvn/wrapper/MavenWrapperDownloader.java");
    }

    public Remote wrapperDownloader(SourceFile previous) {
        return Remote.builder(previous)
                .charset(StandardCharsets.UTF_8)
                .build(getWrapperDistributionFetchUri(), ".mvn/wrapper/MavenWrapperDownloader.java");
    }

    public Remote mvnw() {
        return Remote.builder(WRAPPER_SCRIPT_LOCATION)
                .charset(StandardCharsets.UTF_8)
                .build(getWrapperDistributionFetchUri(), "mvnw");
    }

    public Remote mvnwCmd() {
        return Remote.builder(WRAPPER_BATCH_LOCATION)
                .charset(Charset.forName("Windows-1252"))
                .build(getWrapperDistributionFetchUri(), "mvnw.cmd");
    }

    private URI getWrapperFetchUri() {
        return fetchUri(fetchRepositoryUri, wrapperUri, WRAPPER_GROUP_ARTIFACT, wrapperVersion, null, "jar");
    }

    private URI getWrapperDistributionFetchUri() {
        return fetchUri(fetchRepositoryUri, wrapperDistributionUri, WRAPPER_DISTRIBUTION_GROUP_ARTIFACT, wrapperVersion, wrapperDistributionType.classifier, "zip");
    }

    private static URI fetchUri(@Nullable String fetchRepositoryUri, String canonicalUri, GroupArtifact ga, String version,
                                @Nullable String classifier, String extension) {
        if (fetchRepositoryUri == null) {
            return URI.create(canonicalUri);
        }
        return URI.create(getDownloadUriFor(withoutTrailingSlash(fetchRepositoryUri), ga, version, classifier, extension));
    }

    private static String withoutTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    private static String getDownloadUriFor(String repositoryUri, GroupArtifact ga, String version, @Nullable String classifier, String extension) {
        return repositoryUri + "/" +
               ga.getGroupId().replace(".", "/") + "/" +
               ga.getArtifactId() + "/" +
               version + "/" +
               ga.getArtifactId() + "-" + version + (classifier == null ? "" : "-" + classifier) + "." + extension;
    }

    public enum DistributionType {
        Bin("bin"),
        OnlyScript("only-script"),
        Script("script"),
        Source("source");

        private final String classifier;

        DistributionType(String classifier) {
            this.classifier = classifier;
        }
    }
}
