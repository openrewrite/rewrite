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

import lombok.Value;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Value
public class MavenWrapper {
    public static final String ASF_LICENSE_HEADER = "# Licensed to the Apache Software Foundation (ASF) under one\n" +
                                                    "# or more contributor license agreements.  See the NOTICE file\n" +
                                                    "# distributed with this work for additional information\n" +
                                                    "# regarding copyright ownership.  The ASF licenses this file\n" +
                                                    "# to you under the Apache License, Version 2.0 (the\n" +
                                                    "# \"License\"); you may not use this file except in compliance\n" +
                                                    "# with the License.  You may obtain a copy of the License at\n" +
                                                    "# \n" +
                                                    "#   http://www.apache.org/licenses/LICENSE-2.0\n" +
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

    String wrapperVersion;
    String wrapperUri;
    Checksum wrapperChecksum;
    String wrapperDistributionUri;
    DistributionType wrapperDistributionType;
    String distributionVersion;
    String distributionUri;
    Checksum distributionChecksum;

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

        MavenPomDownloader pomDownloader = new MavenPomDownloader(Collections.emptyMap(), ctx, null, null);

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

        List<MavenRepository> repositories = Collections.singletonList(repository);
        try {
            GroupArtifact wrapperDistributionGroupArtifact = new GroupArtifact("org.apache.maven.wrapper", "maven-wrapper-distribution");
            MavenMetadata wrapperMetadata = pomDownloader.downloadMetadata(wrapperDistributionGroupArtifact, null, repositories);
            String resolvedWrapperVersion = wrapperMetadata.getVersioning()
                    .getVersions()
                    .stream()
                    .filter(v -> wrapperVersionComparator.isValid(null, v))
                    .max((v1, v2) -> wrapperVersionComparator.compare(null, v1, v2))
                    .orElseThrow(() -> new IllegalStateException("Expected to find at least one Maven wrapper version to select from."));
            String resolvedWrapperUri = getDownloadUriFor(repository, new GroupArtifact("org.apache.maven.wrapper", "maven-wrapper"), resolvedWrapperVersion, null, "jar");
            String resolvedWrapperDistributionUri = getDownloadUriFor(repository, wrapperDistributionGroupArtifact, resolvedWrapperVersion, wrapperDistributionType.classifier, "zip");

            GroupArtifact distributionGroupArtifact = new GroupArtifact("org.apache.maven", "apache-maven");
            MavenMetadata distributionMetadata = pomDownloader.downloadMetadata(distributionGroupArtifact, null, repositories);
            String resolvedDistributionVersion = distributionMetadata.getVersioning()
                    .getVersions()
                    .stream()
                    .filter(v -> distributionVersionComparator.isValid(null, v))
                    .max((v1, v2) -> distributionVersionComparator.compare(null, v1, v2))
                    .orElseThrow(() -> new IllegalStateException("Expected to find at least one Maven distribution version to select from."));
            String resolvedDistributionUri = getDownloadUriFor(repository, distributionGroupArtifact, resolvedDistributionVersion, "bin", "zip");

            Remote wrapperJar = (Remote) Checksum.sha256(Remote.builder(
                    WRAPPER_JAR_LOCATION,
                    URI.create(resolvedWrapperUri)
            ).build(), ctx);

            Remote mavenDistribution = (Remote) Checksum.sha256(Remote.builder(
                    Paths.get(""),
                    URI.create(resolvedDistributionUri)
            ).build(), ctx);

            return new MavenWrapper(
                    resolvedWrapperVersion,
                    resolvedWrapperUri,
                    wrapperJar.getChecksum(),
                    resolvedWrapperDistributionUri,
                    wrapperDistributionType,
                    resolvedDistributionVersion,
                    resolvedDistributionUri,
                    mavenDistribution.getChecksum()
            );
        } catch (MavenDownloadingException e) {
            throw new RuntimeException("Could not get Maven versions at: " + repository.getUri(), e);
        }
    }

    public String getWrapperUrl() {
        return wrapperUri;
    }

    public String getDistributionUrl() {
        return distributionUri;
    }

    public Remote wrapperJar() {
        return Remote.builder(
                WRAPPER_JAR_LOCATION,
                URI.create(wrapperUri)
        ).build();
    }

    public Remote wrapperJar(SourceFile previous) {
        return Remote.builder(
                previous,
                URI.create(wrapperUri)
        ).build();
    }

    public Remote wrapperDownloader() {
        return Remote.builder(
                WRAPPER_DOWNLOADER_LOCATION,
                URI.create(wrapperDistributionUri)
        ).build(".mvn/wrapper/MavenWrapperDownloader.java");
    }

    public Remote wrapperDownloader(SourceFile previous) {
        return Remote.builder(
                previous,
                URI.create(wrapperDistributionUri)
        ).build(".mvn/wrapper/MavenWrapperDownloader.java");
    }

    public Remote mvnw() {
        return Remote.builder(
                WRAPPER_SCRIPT_LOCATION,
                URI.create(wrapperDistributionUri)
        ).build("mvnw");
    }

    public Remote mvnwCmd() {
        return Remote.builder(
                WRAPPER_BATCH_LOCATION,
                URI.create(wrapperDistributionUri)
        ).build("mvnw.cmd");
    }

    private static String getDownloadUriFor(MavenRepository repository, GroupArtifact ga, String version, @Nullable String classifier, String extension) {
        return repository.getUri() + "/" +
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
