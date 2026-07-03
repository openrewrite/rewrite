/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.GradleWrapperScriptLoader;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteArchive;
import org.openrewrite.remote.RemoteResource;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.formatUriForPropertiesFile;

@Value
public class GradleWrapper {
    private static final String GRADLE_DOWNLOADS_URL = "https://downloads.gradle.org";
    private static final String GRADLE_DISTRIBUTIONS_URL = GRADLE_DOWNLOADS_URL + "/distributions";
    private static final String GRADLE_SERVICES_URL = "https://services.gradle.org";
    private static final String GRADLE_VERSIONS_ALL_URL = GRADLE_SERVICES_URL + "/versions/all";
    public static final String WRAPPER_JAR_LOCATION_RELATIVE_PATH = "gradle/wrapper/gradle-wrapper.jar";
    public static final String WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH = "gradle/wrapper/gradle-wrapper.properties";
    public static final String WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH = "gradlew";
    public static final String WRAPPER_BATCH_LOCATION_RELATIVE_PATH = "gradlew.bat";

    public static final Path WRAPPER_JAR_LOCATION = Paths.get(WRAPPER_JAR_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get(WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_SCRIPT_LOCATION = Paths.get(WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH);
    public static final Path WRAPPER_BATCH_LOCATION = Paths.get(WRAPPER_BATCH_LOCATION_RELATIVE_PATH);

    String version;
    DistributionInfos distributionInfos;

    /**
     * Construct a Gradle wrapper from a distribution type and version.
     * Used in contexts where downloads.gradle.org is available.
     */
    public static GradleWrapper create(@Nullable String distributionTypeName, @Nullable String version, ExecutionContext ctx) {
        return create(null, distributionTypeName, version, ctx);
    }

    public static GradleWrapper create(@Nullable String currentDistributionUrl, @Nullable String distributionTypeName, @Nullable String version, ExecutionContext ctx) {
        String normalizedCurrentDistributionUrl = currentDistributionUrl == null ? null : currentDistributionUrl.replace("\\", "");
        DistributionType distributionType;
        if (distributionTypeName != null) {
            distributionType = Arrays.stream(DistributionType.values())
                    .filter(dt -> dt.name().equalsIgnoreCase(distributionTypeName))
                    .findAny()
                    .orElse(DistributionType.Bin);
        } else if (normalizedCurrentDistributionUrl != null && normalizedCurrentDistributionUrl.endsWith("-all.zip")) {
            distributionType = DistributionType.All;
        } else {
            distributionType = DistributionType.Bin;
        }
        VersionComparator versionComparator = StringUtils.isBlank(version) ?
                new LatestRelease(null) :
                requireNonNull(Semver.validate(version, null).getValue());

        try {
            GradleVersion gradleVersion = determineGradleVersion(normalizedCurrentDistributionUrl, version, versionComparator, distributionType, ctx);
            DistributionInfos infos = DistributionInfos.fetch(gradleVersion, ctx);
            return new GradleWrapper(gradleVersion.version, infos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static GradleVersion determineGradleVersion(@Nullable String currentDistributionUrl, @Nullable String version, VersionComparator versionComparator,
                                                        DistributionType distributionType, ExecutionContext ctx) {
        if (currentDistributionUrl == null || currentDistributionUrl.startsWith(GRADLE_DOWNLOADS_URL) || currentDistributionUrl.startsWith(GRADLE_SERVICES_URL)) {
            // Only list all versions via services endpoint if a wildcard notation was requested or null, e.g. 8.x
            if (!(versionComparator instanceof ExactVersion)) {
                List<GradleVersion> allVersions = listAllPublicVersions(ctx);
                return allVersions.stream()
                        .filter(v -> versionComparator.isValid(null, v.version))
                        .filter(v -> v.distributionType == distributionType)
                        .max((v1, v2) -> versionComparator.compare(null, v1.version, v2.version))
                        .orElseThrow(() -> new IllegalStateException(String.format("Expected to find at least one Gradle wrapper version to select from %s.", GRADLE_DOWNLOADS_URL)));
            }

            return new GradleVersion(version,
                    GRADLE_DISTRIBUTIONS_URL + "/gradle-" + version + "-" + distributionType.getFileSuffix() +".zip",
                    distributionType,
                    GRADLE_DISTRIBUTIONS_URL + "/gradle-" + version + "-" + distributionType.getFileSuffix() +".zip.sha256",
                    GRADLE_DISTRIBUTIONS_URL + "/gradle-" + version + "-wrapper.jar.sha256"
            );
        }

        Optional<GradleVersion> privateArtifactoryVersion = selectPrivateArtifactoryVersion(currentDistributionUrl, versionComparator, distributionType, ctx);
        if (privateArtifactoryVersion.isPresent()) {
            return privateArtifactoryVersion.get();
        }

        return resolveCustomDistributionVersion(currentDistributionUrl, version, versionComparator, distributionType);
    }

    /**
     * Opportunistically ask Artifactory which Gradle versions the repository actually hosts, so we don't suggest a
     * version it doesn't contain. This is best-effort: it only works when Artifactory is served under the conventional
     * "/artifactory" context path and exposes the storage API. Returns empty when the URL isn't a recognizable
     * Artifactory storage layout, the listing fails, or no hosted version satisfies the requested selector, in which
     * case the caller falls back to {@link #resolveCustomDistributionVersion}.
     */
    private static Optional<GradleVersion> selectPrivateArtifactoryVersion(String currentDistributionUrl, VersionComparator versionComparator,
                                                                           DistributionType distributionType, ExecutionContext ctx) {
        URI currentDistributionUri = URI.create(currentDistributionUrl);
        if (!currentDistributionUri.getScheme().startsWith("http") || !currentDistributionUri.getPath().startsWith("/artifactory")) {
            return Optional.empty();
        }
        try {
            String artifactoryUrl = currentDistributionUrl.substring(0, currentDistributionUrl.lastIndexOf("/"));
            return listAllPrivateArtifactoryVersions(artifactoryUrl, ctx).stream()
                    .filter(v -> versionComparator.isValid(null, v.version))
                    .filter(v -> v.distributionType == distributionType)
                    .max((v1, v2) -> versionComparator.compare(null, v1.version, v2.version));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Resolve the target version for a Gradle distribution hosted at a non-standard URL (an arbitrary Maven repository
     * in Artifactory or Nexus, an S3 bucket, a CDN, etc.) that can't be queried for its available versions.
     * <p>
     * An exact version is substituted into the existing URL, preserving the custom host and path layout already present
     * in the wrapper properties. A dynamic selector (e.g. {@code 7.x}) can't be resolved without querying an external
     * service such as {@code services.gradle.org}, which is frequently unavailable in the enterprise environments that
     * host distributions at non-standard URLs. Rather than failing the (often deeply chained) recipe run, the version
     * already present in the URL is returned so the caller leaves the wrapper unchanged.
     */
    private static GradleVersion resolveCustomDistributionVersion(String currentDistributionUrl, @Nullable String version, VersionComparator versionComparator,
                                                                  DistributionType distributionType) {
        if (!(versionComparator instanceof ExactVersion)) {
            Matcher matcher = GRADLE_VERSION_PATTERN.matcher(currentDistributionUrl);
            String existingVersion = matcher.find() ? matcher.group(1) : version;
            return new GradleVersion(existingVersion, currentDistributionUrl, distributionType, null, null);
        }

        return new GradleVersion(
                version,
                currentDistributionUrl.replaceAll("(.*gradle-)(\\d+\\.\\d+(?:\\.\\d+)?)(.*-)(?:bin|all).zip", "$1" + version + "$3" + distributionType.getFileSuffix() + ".zip"),
                distributionType,
                null,
                null
        );
    }

    public static List<GradleVersion> listAllPublicVersions(ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        try (HttpSender.Response resp = httpSender.send(httpSender.get(GRADLE_VERSIONS_ALL_URL).build())) {
            if (resp.isSuccessful()) {
                List<GradleVersion> gradleVersions = new ObjectMapper()
                        .registerModule(new ParameterNamesModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(resp.getBody(), new TypeReference<List<GradleVersion>>() {
                        });
                List<GradleVersion> allGradleVersions = new ArrayList<>(gradleVersions.size() * 2);
                for (GradleVersion gradleVersion : gradleVersions) {
                    String downloadUrl = migrateToDownloadsUrl(gradleVersion.downloadUrl);
                    String checksumUrl = migrateToDownloadsUrl(gradleVersion.checksumUrl);
                    String wrapperChecksumUrl = migrateToDownloadsUrl(gradleVersion.wrapperChecksumUrl);
                    allGradleVersions.add(new GradleVersion(
                            gradleVersion.version,
                            downloadUrl,
                            DistributionType.Bin,
                            checksumUrl,
                            wrapperChecksumUrl
                    ));
                    allGradleVersions.add(new GradleVersion(
                            gradleVersion.version,
                            downloadUrl.replace("-bin.zip", "-all.zip"),
                            DistributionType.All,
                            checksumUrl == null ? null : checksumUrl.replace("-bin.zip", "-all.zip"),
                            wrapperChecksumUrl
                    ));
                }
                return allGradleVersions;
            }
            throw new IOException("Could not get Gradle versions from " + GRADLE_VERSIONS_ALL_URL + ": HTTP " + resp.getCode());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<GradleVersion> listAllPrivateArtifactoryVersions(String artifactoryUrl, ExecutionContext ctx) {
        URI artifactoryUri = URI.create(artifactoryUrl);
        String path = artifactoryUri.getPath();
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        // Maven-layout repos store each version in its own subdirectory (e.g. .../gradle/8.10.1/gradle-8.10.1-all.zip).
        // When distributionUrl points inside such a version directory, crawl up one level to discover sibling versions.
        boolean mavenLayout = MAVEN_VERSION_FOLDER_PATTERN.matcher(lastSegment).matches();
        String listingPath = mavenLayout ? path.substring(0, path.lastIndexOf('/')) : path;
        String downloadBaseUrl = mavenLayout ? artifactoryUrl.substring(0, artifactoryUrl.lastIndexOf('/')) : artifactoryUrl;
        String artifactoryApiUrl = String.format("%s://%s%s", artifactoryUri.getScheme(), artifactoryUri.getHost(), listingPath.replace("/artifactory", "/artifactory/api/storage"));
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        try (HttpSender.Response resp = httpSender.send(httpSender.get(artifactoryApiUrl).build())) {
            if (resp.isSuccessful()) {
                JsonNode node = new ObjectMapper().readTree(resp.getBody());
                List<GradleVersion> gradleVersions = new ArrayList<>();
                for (JsonNode child : node.get("children")) {
                    boolean folder = child.get("folder").asBoolean();
                    String uri = child.get("uri").asText();
                    if (mavenLayout && folder) {
                        String version = uri.startsWith("/") ? uri.substring(1) : uri;
                        if (MAVEN_VERSION_FOLDER_PATTERN.matcher(version).matches()) {
                            String folderUrl = downloadBaseUrl + uri;
                            gradleVersions.add(new GradleVersion(version, folderUrl + "/gradle-" + version + "-bin.zip", DistributionType.Bin, null, null));
                            gradleVersions.add(new GradleVersion(version, folderUrl + "/gradle-" + version + "-all.zip", DistributionType.All, null, null));
                        }
                    } else if (!folder) {
                        Matcher matcher = GRADLE_VERSION_PATTERN.matcher(uri);
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            gradleVersions.add(new GradleVersion(version, downloadBaseUrl + uri, uri.endsWith("-all.zip") ? DistributionType.All : DistributionType.Bin, null, null));
                        }
                    }
                }
                return gradleVersions;
            }
            throw new IOException("Could not get Gradle versions from " + artifactoryApiUrl + ": HTTP " + resp.getCode());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @Nullable String migrateToDownloadsUrl(@Nullable String url) {
        return url == null ? null : url.replace(GRADLE_SERVICES_URL, GRADLE_DOWNLOADS_URL);
    }

    private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile("gradle-([0-9.]+)");
    private static final Pattern MAVEN_VERSION_FOLDER_PATTERN = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?(?:-[\\w.-]+)?");

    /**
     * Construct a Gradle wrapper from a URI.
     * Can be used in contexts where downloads.gradle.org, normally used for version lookups, is unavailable.
     */
    public static GradleWrapper create(URI fullDistributionUri, @SuppressWarnings("unused") ExecutionContext ctx) {
        String version = "";
        Matcher matcher = GRADLE_VERSION_PATTERN.matcher(fullDistributionUri.toString());
        if (matcher.find()) {
            version = matcher.group(1);
        }
        return new GradleWrapper(version, new DistributionInfos(fullDistributionUri.toString(), null, null));
    }

    public String getDistributionUrl() {
        return distributionInfos.getDownloadUrl();
    }

    public String getPropertiesFormattedUrl() {
        return formatUriForPropertiesFile(getDistributionUrl());
    }

    public @Nullable Checksum getDistributionChecksum() {
        return distributionInfos.getChecksum();
    }

    static final FileAttributes WRAPPER_JAR_FILE_ATTRIBUTES = new FileAttributes(null, null, null, true, true, false, 0);

    public RemoteArchive wrapperJar() {
        return Remote.builder(WRAPPER_JAR_LOCATION)
                .charset(StandardCharsets.ISO_8859_1)
                .build(URI.create(distributionInfos.getDownloadUrl()), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-(plugins|wrapper)-(?!shared).*\\.jar", "gradle-wrapper.jar");
    }

    public RemoteArchive wrapperJar(SourceFile before) {
        return Remote.builder(before)
                .charset(StandardCharsets.ISO_8859_1)
                .build(URI.create(distributionInfos.getDownloadUrl()), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-(plugins|wrapper)-(?!shared).*\\.jar", "gradle-wrapper.jar");
    }

    public RemoteResource gradlew() {
        return new GradleWrapperScriptLoader().findNearest(version).gradlew();
    }

    public RemoteResource gradlewBat() {
        return new GradleWrapperScriptLoader().findNearest(version).gradlewBat();
    }

    public enum DistributionType {
        Bin("bin"),
        All("all");

        private final String fileSuffix;

        DistributionType(String fileSuffix) {
            this.fileSuffix = fileSuffix;
        }

        public String getFileSuffix() {
            return fileSuffix;
        }
    }

    @Value
    public static class GradleVersion {
        String version;
        String downloadUrl;
        DistributionType distributionType;

        @Nullable
        String checksumUrl;

        /**
         * Is null for every non-release version (e.g. RCs and milestones).
         */
        @Nullable
        String wrapperChecksumUrl;
    }
}
