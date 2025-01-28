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
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.internal.StringUtils.formatUriForPropertiesFile;

@Value
public class GradleWrapper {
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

    public static GradleWrapper create(@Nullable String distributionTypeName, @Nullable String version, @Nullable String repositoryUrl, ExecutionContext ctx) {
        DistributionType distributionType = Arrays.stream(DistributionType.values())
                .filter(dt -> dt.name().equalsIgnoreCase(distributionTypeName))
                .findAny()
                .orElse(DistributionType.Bin);
        VersionComparator versionComparator = StringUtils.isBlank(version) ?
                new LatestRelease(null) :
                requireNonNull(Semver.validate(version, null).getValue());

        List<GradleVersion> allVersions = listAllVersions(repositoryUrl, ctx);
        GradleVersion gradleVersion = allVersions.stream()
                .filter(v -> versionComparator.isValid(null, v.version))
                .max((v1, v2) -> versionComparator.compare(null, v1.version, v2.version))
                .orElseThrow(() -> new IllegalStateException("Expected to find at least one Gradle wrapper version to select from."));

        try {
            DistributionInfos infos = DistributionInfos.fetch(distributionType, gradleVersion, ctx);
            return new GradleWrapper(gradleVersion.version, infos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<GradleVersion> listAllVersions(@Nullable String repositoryUrl, ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        String gradleVersionsUrl = StringUtils.isBlank(repositoryUrl) ? "https://services.gradle.org/versions/all" : repositoryUrl;
        try (HttpSender.Response resp = httpSender.send(httpSender.get(gradleVersionsUrl).build())) {
            if (resp.isSuccessful()) {
                return new ObjectMapper()
                        .registerModule(new ParameterNamesModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(resp.getBody(), new TypeReference<List<GradleVersion>>() {
                        });
            }
            throw new IOException("Could not get Gradle versions. HTTP " + resp.getCode());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile("gradle-([0-9.]+)");

    /**
     * Construct a Gradle wrapper from a URI.
     * Can be used in contexts where services.gradle.org, normally used for version lookups, is unavailable.
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
                .build(URI.create(distributionInfos.getDownloadUrl()), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-(plugins|wrapper)-(?!shared).*\\.jar", "gradle-wrapper.jar");
    }

    public RemoteArchive wrapperJar(SourceFile before) {
        return Remote.builder(before)
                .build(URI.create(distributionInfos.getDownloadUrl()), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-(plugins|wrapper)-(?!shared).*\\.jar", "gradle-wrapper.jar");
    }

    public RemoteResource gradlew() {
        return new GradleWrapperScriptLoader().findNearest(version).gradlew();
    }

    public RemoteResource gradlewBat() {
        return new GradleWrapperScriptLoader().findNearest(version).gradlewBat();
    }

    public enum DistributionType {
        Bin,
        All
    }

    @Value
    public static class GradleVersion {
        String version;
        String downloadUrl;
        String checksumUrl;

        /**
         * Is null for every non-release version (e.g. RCs and milestones).
         */
        @Nullable
        String wrapperChecksumUrl;
    }
}
